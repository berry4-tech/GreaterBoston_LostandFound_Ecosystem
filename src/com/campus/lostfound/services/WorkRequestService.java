package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoWorkRequestDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Service layer for WorkRequest business logic.
 * Handles request creation, routing, approval, and querying.
 */
public class WorkRequestService {
    
    private static final Logger LOGGER = Logger.getLogger(WorkRequestService.class.getName());
    private final MongoWorkRequestDAO requestDAO;
    private final MongoUserDAO userDAO;
    private final MongoItemDAO itemDAO;
    private final WorkRequestRoutingEngine routingEngine;
    private final TrustScoreService trustScoreService;
    
    public WorkRequestService() {
        this.requestDAO = new MongoWorkRequestDAO();
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        this.routingEngine = new WorkRequestRoutingEngine(userDAO);
        this.trustScoreService = new TrustScoreService();
    }
    
    // For testing with mock DAOs
    public WorkRequestService(MongoWorkRequestDAO requestDAO, MongoUserDAO userDAO) {
        this.requestDAO = requestDAO;
        this.userDAO = userDAO;
        this.itemDAO = new MongoItemDAO();
        this.routingEngine = new WorkRequestRoutingEngine(userDAO);
        this.trustScoreService = new TrustScoreService();
    }
    
    /**
     * Create a new work request.
     * Validates the request, determines first approver, and saves to database.
     * For ItemClaimRequests, also checks for duplicate claims and creates disputes.
     * 
     * @param request The work request to create
     * @return The request ID if successful, null if validation fails
     */
    public String createRequest(WorkRequest request) {
        try {
            // Validate request
            if (!request.isValid()) {
                LOGGER.warning("Invalid work request: " + request.getRequestSummary());
                return null;
            }
            
            // ========== DUPLICATE CLAIM DETECTION ==========
            // Check if this is an ItemClaimRequest and if there's already a claim for this item
            if (request instanceof ItemClaimRequest) {
                ItemClaimRequest claimRequest = (ItemClaimRequest) request;
                DisputeCheckResult disputeResult = checkForDuplicateClaim(claimRequest);
                
                if (disputeResult.hasExistingClaim) {
                    LOGGER.info("Duplicate claim detected for item: " + claimRequest.getItemId());
                    
                    // Check if a dispute already exists for this item
                    if (disputeResult.existingDispute != null) {
                        // Add this claimant to the existing dispute
                        addClaimantToExistingDispute(disputeResult.existingDispute, claimRequest);
                        LOGGER.info("Added new claimant to existing dispute: " + disputeResult.existingDispute.getRequestId());
                        return disputeResult.existingDispute.getRequestId();
                    } else {
                        // Create a new MultiEnterpriseDisputeResolution
                        String disputeId = createDisputeFromDuplicateClaims(
                            disputeResult.existingClaim, claimRequest);
                        LOGGER.info("Created new dispute for item: " + claimRequest.getItemId() + 
                                   " with dispute ID: " + disputeId);
                        return disputeId;
                    }
                }
            }
            
            // Set initial status
            request.setStatus(WorkRequest.RequestStatus.PENDING);
            request.setApprovalStep(0);
            
            // Check requester's trust score and flag if low
            TrustScoreCheckResult trustCheck = checkRequesterTrustScore(request);
            if (trustCheck.requiresFlag) {
                request.setNotes((request.getNotes() != null ? request.getNotes() + "\n" : "") + 
                    "⚠️ LOW TRUST SCORE: " + trustCheck.message);
                // Escalate priority for low-trust requesters
                if (trustCheck.score < 30 && request.getPriority() == WorkRequest.RequestPriority.NORMAL) {
                    request.setPriority(WorkRequest.RequestPriority.HIGH);
                    LOGGER.info("Escalated priority due to low trust score");
                }
            }
            
            // Determine priority if not already set
            if (request.getPriority() == null || request.getPriority() == WorkRequest.RequestPriority.NORMAL) {
                WorkRequest.RequestPriority autoPriority = routingEngine.determinePriority(request);
                request.setPriority(autoPriority);
                LOGGER.info("Auto-assigned priority: " + autoPriority);
            }
            
            // Auto-advance approval if requester matches the first required role
            // This handles cases like Campus Coordinator creating a transfer request
            // where they ARE the source coordinator approval step
            autoAdvanceIfRequesterMatchesFirstApprover(request);
            
            // Determine next approver using routing engine
            String nextApproverRole = request.getNextRequiredRole();
            if (nextApproverRole != null) {
                User approver = routingEngine.findBestApprover(
                    nextApproverRole, 
                    request.getTargetOrganizationId(),
                    request.getPriority()
                );
                if (approver != null) {
                    request.setCurrentApproverId(String.valueOf(approver.getUserId()));
                    LOGGER.info("Assigned to approver: " + approver.getFullName() + 
                               " (" + nextApproverRole + ", priority: " + request.getPriority() + ")");
                }
            }
            
            // Save to database
            String requestId = requestDAO.save(request);
            
            if (requestId != null) {
                LOGGER.info("Created work request: " + requestId + " of type " + request.getRequestType());
            }
            
            return requestId;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating work request", e);
            return null;
        }
    }
    
    /**
     * Auto-advance approval if the requester's role matches the first required role.
     * This handles cases like:
     * - Campus Coordinator creating a transfer request (they ARE the source approval)
     * - MBTA Station Manager sending to University (they ARE the source approval)
     * 
     * This prevents the requester from having to approve their own initiated request.
     */
    private void autoAdvanceIfRequesterMatchesFirstApprover(WorkRequest request) {
        String firstRequiredRole = request.getNextRequiredRole();
        if (firstRequiredRole == null) {
            return; // No approval needed
        }
        
        // Try to get requester's role
        String requesterEmail = request.getRequesterEmail();
        if (requesterEmail == null) {
            requesterEmail = request.getRequesterId();
        }
        
        if (requesterEmail == null) {
            return;
        }
        
        Optional<User> requesterOpt = userDAO.findByEmail(requesterEmail);
        if (!requesterOpt.isPresent()) {
            return;
        }
        
        User requester = requesterOpt.get();
        String requesterRole = requester.getRole().name();
        
        // If requester's role matches the first required role, auto-advance
        if (requesterRole.equals(firstRequiredRole)) {
            LOGGER.info("Auto-advancing approval: requester " + requester.getFullName() + 
                       " matches first approval role " + firstRequiredRole);
            request.advanceApproval(requesterEmail, requester.getFullName() + " (Auto-approved as initiator)");
        }
    }
    
    /**
     * Approve a work request and route to next approver.
     * 
     * @param requestId The request to approve
     * @param approverId The user approving
     * @return true if approval successful, false otherwise
     */
    public boolean approveRequest(String requestId, String approverId) {
        try {
            // Load request
            WorkRequest request = requestDAO.findById(requestId);
            if (request == null) {
                LOGGER.warning("Request not found: " + requestId);
                return false;
            }
            
            // Verify request is pending
            if (!request.isPending()) {
                LOGGER.warning("Request not pending: " + requestId + " (status: " + request.getStatus() + ")");
                return false;
            }
            
            // Verify approver has authority
            if (!canUserApprove(approverId, request)) {
                LOGGER.warning("User " + approverId + " cannot approve request " + requestId);
                return false;
            }
            
            // Get approver details (use email for lookup)
            Optional<User> approverOpt = userDAO.findByEmail(approverId);
            if (!approverOpt.isPresent()) {
                LOGGER.warning("Approver not found by email: " + approverId);
                return false;
            }
            User approver = approverOpt.get();
            
            // Advance approval
            request.advanceApproval(approverId, approver.getFullName());
            
            // Route to next approver if needed
            if (request.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS) {
                routeToNextApprover(request);
            }
            
            // Save updated request
            requestDAO.save(request);
            
            // If fully approved and it's an ItemClaimRequest, update the Item status
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED && 
                request instanceof ItemClaimRequest) {
                updateItemStatusOnClaimApproval((ItemClaimRequest) request);
            }
            
            // If fully approved and it's a TransitToUniversityTransferRequest, update both items
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED && 
                request instanceof TransitToUniversityTransferRequest) {
                updateItemStatusOnTransferApproval((TransitToUniversityTransferRequest) request);
            }
            
            // If fully approved and it's an AirportToUniversityTransferRequest, update both items
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED && 
                request instanceof AirportToUniversityTransferRequest) {
                updateItemStatusOnAirportTransferApproval((AirportToUniversityTransferRequest) request);
            }
            
            // If fully approved and it's a CrossCampusTransferRequest, update both items
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED && 
                request instanceof CrossCampusTransferRequest) {
                updateItemStatusOnCrossCampusTransferApproval((CrossCampusTransferRequest) request);
            }
            
            // If fully approved and it's an MBTAToAirportEmergencyRequest, update the Item status
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED && 
                request instanceof MBTAToAirportEmergencyRequest) {
                updateItemStatusOnEmergencyTransfer((MBTAToAirportEmergencyRequest) request);
            }
            
            LOGGER.info("Request " + requestId + " approved by " + approver.getFullName() + 
                       " (step " + request.getApprovalStep() + "/" + request.getApprovalChain().size() + 
                       ", priority: " + request.getPriority() + ")");
            
            // Record trust score events
            recordApprovalTrustEvents(request, approverId);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error approving request: " + requestId, e);
            return false;
        }
    }
    
    /**
     * Reject a work request.
     * 
     * @param requestId The request to reject
     * @param approverId The user rejecting
     * @param reason Reason for rejection
     * @return true if rejection successful, false otherwise
     */
    public boolean rejectRequest(String requestId, String approverId, String reason) {
        try {
            // Load request
            WorkRequest request = requestDAO.findById(requestId);
            if (request == null) {
                LOGGER.warning("Request not found: " + requestId);
                return false;
            }
            
            // Verify request is pending
            if (!request.isPending()) {
                LOGGER.warning("Request not pending: " + requestId);
                return false;
            }
            
            // Verify approver has authority
            if (!canUserApprove(approverId, request)) {
                LOGGER.warning("User " + approverId + " cannot reject request " + requestId);
                return false;
            }
            
            // Get approver details (use email for lookup)
            Optional<User> approverOpt = userDAO.findByEmail(approverId);
            String approverName = approverOpt.isPresent() ? approverOpt.get().getFullName() : "Unknown";
            
            // Reject request
            request.reject(reason + " - Rejected by " + approverName);
            
            // Release workload
            if (request.getCurrentApproverId() != null) {
                routingEngine.releaseWorkload(request.getCurrentApproverId());
            }
            
            // Save updated request
            requestDAO.save(request);
            
            LOGGER.info("Request " + requestId + " rejected by " + approverName + ": " + reason);
            
            // Record trust score events for rejection
            recordRejectionTrustEvents(request, approverId, reason);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rejecting request: " + requestId, e);
            return false;
        }
    }
    
    /**
     * Complete a work request (final step after all approvals).
     * 
     * @param requestId The request to complete
     * @return true if successful, false otherwise
     */
    public boolean completeRequest(String requestId) {
        try {
            WorkRequest request = requestDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            if (request.getStatus() != WorkRequest.RequestStatus.APPROVED) {
                LOGGER.warning("Cannot complete request that is not approved: " + requestId);
                return false;
            }
            
            request.complete();
            
            // Release workload for all approvers
            for (String approverId : request.getApproverIds()) {
                routingEngine.releaseWorkload(approverId);
            }
            
            requestDAO.save(request);
            
            LOGGER.info("Request " + requestId + " marked as completed");
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error completing request: " + requestId, e);
            return false;
        }
    }
    
    /**
     * Cancel a work request (by requester).
     * 
     * @param requestId The request to cancel
     * @param requesterId The user requesting cancellation
     * @return true if successful, false otherwise
     */
    public boolean cancelRequest(String requestId, String requesterId) {
        try {
            WorkRequest request = requestDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            // Only requester can cancel
            if (!request.getRequesterId().equals(requesterId)) {
                LOGGER.warning("User " + requesterId + " cannot cancel request " + requestId);
                return false;
            }
            
            // Cannot cancel completed or already cancelled requests
            if (request.getStatus() == WorkRequest.RequestStatus.COMPLETED ||
                request.getStatus() == WorkRequest.RequestStatus.CANCELLED) {
                return false;
            }
            
            request.cancel();
            
            // Release workload
            if (request.getCurrentApproverId() != null) {
                routingEngine.releaseWorkload(request.getCurrentApproverId());
            }
            
            requestDAO.save(request);
            
            LOGGER.info("Request " + requestId + " cancelled by requester");
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling request: " + requestId, e);
            return false;
        }
    }
    
    /**
     * Get all work requests for a specific user.
     * Returns requests they created or need to approve.
     * 
     * @param userId The user ID
     * @param role The user's role (for filtering approval queue)
     * @return List of relevant work requests
     */
    public List<WorkRequest> getRequestsForUser(String userId, String role) {
        try {
            List<WorkRequest> allRequests = new ArrayList<>();
            
            // Get requests created by user
            List<WorkRequest> created = requestDAO.findByRequesterId(userId);
            allRequests.addAll(created);
            
            // Get requests awaiting approval by this user
            List<WorkRequest> toApprove = requestDAO.findPendingForApprover(userId);
            allRequests.addAll(toApprove);
            
            // Get requests where user might need to approve (by role)
            // This helps populate work queues
            List<WorkRequest> byRole = getRequestsForRole(role, userId);
            allRequests.addAll(byRole);
            
            // Remove duplicates (using requestId)
            return allRequests.stream()
                             .distinct()
                             .collect(Collectors.toList());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting requests for user: " + userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get pending requests for a specific role.
     * Useful for work queue displays.
     * 
     * @param role The role (e.g., "CAMPUS_COORDINATOR")
     * @param organizationId Optional organization ID to filter
     * @return List of pending requests needing this role
     */
    public List<WorkRequest> getRequestsForRole(String role, String organizationId) {
        try {
            List<WorkRequest> pending = requestDAO.findByStatus(WorkRequest.RequestStatus.PENDING);
            List<WorkRequest> inProgress = requestDAO.findByStatus(WorkRequest.RequestStatus.IN_PROGRESS);
            
            List<WorkRequest> all = new ArrayList<>();
            all.addAll(pending);
            all.addAll(inProgress);
            
            // Filter by role and organization (for CrossCampusTransferRequest)
            return all.stream()
                     .filter(r -> r.needsApprovalFromRole(role))
                     .filter(r -> canViewRequest(r, role, organizationId))
                     .collect(Collectors.toList());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting requests for role: " + role, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if a user from a specific organization can view/access a request.
     * For CrossCampusTransferRequest, applies organization-level filtering.
     * For other request types, allows access based on role alone.
     * 
     * @param request The work request
     * @param role The user's role
     * @param organizationId The user's organization ID
     * @return true if the user can view this request
     */
    private boolean canViewRequest(WorkRequest request, String role, String organizationId) {
        // For CrossCampusTransferRequest, apply organization-level filtering
        if (request instanceof CrossCampusTransferRequest) {
            CrossCampusTransferRequest cctr = (CrossCampusTransferRequest) request;
            int currentStep = cctr.getApprovalStep();
            
            if (organizationId == null) {
                return false; // Can't verify organization
            }
            
            String sourceOrgId = cctr.getRequesterOrganizationId();
            String targetOrgId = cctr.getTargetOrganizationId();
            
            // Source organization coordinators can always see requests they initiated
            // (for tracking purposes)
            if (sourceOrgId != null && sourceOrgId.equals(organizationId)) {
                return true;
            }
            
            // Destination organization coordinators can see requests awaiting their approval
            // (Step 1 = destination coordinator approval step)
            if (currentStep == 1 && targetOrgId != null && targetOrgId.equals(organizationId)) {
                return true;
            }
            
            // Step 2 = Student confirmation step
            // Students can see requests where they are the recipient
            // The role-based check (needsApprovalFromRole) already verified the STUDENT role,
            // and the student-specific matching is done in StudentUserPanel.isTransferForThisStudent()
            if (currentStep == 2 && "STUDENT".equals(role)) {
                return true;
            }
            
            return false;
        }
        
        // For other request types, allow access based on role alone
        return true;
    }
    
    /**
     * Get request by ID.
     */
    public WorkRequest getRequestById(String requestId) {
        return requestDAO.findById(requestId);
    }
    
    /**
     * Update an existing work request.
     * Saves the current state of the request to the database.
     * 
     * @param request The work request to update
     * @return true if update successful, false otherwise
     */
    public boolean updateRequest(WorkRequest request) {
        try {
            if (request == null || request.getRequestId() == null) {
                LOGGER.warning("Cannot update null request or request without ID");
                return false;
            }
            
            request.setLastUpdatedAt(java.time.LocalDateTime.now());
            String savedId = requestDAO.save(request);
            
            if (savedId != null) {
                LOGGER.info("Updated work request: " + request.getRequestId());
                return true;
            }
            return false;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating work request: " + request.getRequestId(), e);
            return false;
        }
    }
    
    /**
     * Get all requests (admin function).
     */
    public List<WorkRequest> getAllRequests() {
        return requestDAO.findAll();
    }
    
    /**
     * Get requests by status.
     */
    public List<WorkRequest> getRequestsByStatus(WorkRequest.RequestStatus status) {
        return requestDAO.findByStatus(status);
    }
    
    /**
     * Get requests by type.
     */
    public List<WorkRequest> getRequestsByType(WorkRequest.RequestType type) {
        return requestDAO.findByType(type);
    }
    
    /**
     * Get count of pending requests for a user.
     */
    public long getPendingCountForUser(String userId) {
        try {
            List<WorkRequest> pending = requestDAO.findPendingForApprover(userId);
            return pending.size();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting pending count", e);
            return 0;
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Check if a user can approve a specific request.
     * @param userEmail The email of the user attempting to approve
     * @param request The work request to check
     */
    private boolean canUserApprove(String userEmail, WorkRequest request) {
        // Check if user has the required role (use email for lookup)
        Optional<User> userOpt = userDAO.findByEmail(userEmail);
        if (!userOpt.isPresent()) {
            return false;
        }
        User user = userOpt.get();
        
        // Check if user is the assigned approver
        if (userEmail.equals(request.getCurrentApproverId())) {
            // For CrossCampusTransferRequest, still need to verify organization
            if (request instanceof CrossCampusTransferRequest) {
                return canUserApproveCrossCampusTransfer(user, (CrossCampusTransferRequest) request);
            }
            return true;
        }
        
        String requiredRole = request.getNextRequiredRole();
        if (requiredRole == null) {
            return false; // Already fully approved
        }
        
        // Check role match first
        if (!user.getRole().name().equals(requiredRole)) {
            return false;
        }
        
        // For CrossCampusTransferRequest, also check organization-level access
        if (request instanceof CrossCampusTransferRequest) {
            return canUserApproveCrossCampusTransfer(user, (CrossCampusTransferRequest) request);
        }
        
        // For other request types, role match is sufficient
        return true;
    }
    
    /**
     * Check if a user can approve a CrossCampusTransferRequest based on organization.
     * Step 1 (source approval): User must be from the SOURCE organization
     * Step 2 (destination approval): User must be from the TARGET organization
     * 
     * @param user The user attempting to approve
     * @param request The cross-campus transfer request
     * @return true if user can approve this step
     */
    private boolean canUserApproveCrossCampusTransfer(User user, CrossCampusTransferRequest request) {
        int currentStep = request.getApprovalStep();
        String userOrgId = user.getOrganizationId();
        
        if (userOrgId == null) {
            LOGGER.warning("User " + user.getEmail() + " has no organization ID");
            return false;
        }
        
        if (currentStep == 0) {
            // Step 1: Source campus coordinator must approve
            // User must be from the requester's (source) organization
            String sourceOrgId = request.getRequesterOrganizationId();
            if (sourceOrgId == null) {
                LOGGER.warning("CrossCampusTransferRequest has no requester organization ID");
                return false;
            }
            boolean canApprove = userOrgId.equals(sourceOrgId);
            if (!canApprove) {
                LOGGER.info("User " + user.getEmail() + " (org: " + userOrgId + 
                           ") cannot approve Step 1 - requires source org: " + sourceOrgId);
            }
            return canApprove;
            
        } else if (currentStep == 1) {
            // Step 2: Destination campus coordinator must approve
            // User must be from the target organization
            String targetOrgId = request.getTargetOrganizationId();
            if (targetOrgId == null) {
                LOGGER.warning("CrossCampusTransferRequest has no target organization ID");
                return false;
            }
            boolean canApprove = userOrgId.equals(targetOrgId);
            if (!canApprove) {
                LOGGER.info("User " + user.getEmail() + " (org: " + userOrgId + 
                           ") cannot approve Step 2 - requires target org: " + targetOrgId);
            }
            return canApprove;
            
        } else if (currentStep == 2) {
            // Step 3: Student confirmation - handled by student role check
            return user.getRole().name().equals("STUDENT");
        }
        
        return false;
    }
    
    /**
     * Route request to next approver in the chain using routing engine.
     */
    private void routeToNextApprover(WorkRequest request) {
        String nextRole = request.getNextRequiredRole();
        if (nextRole == null) {
            return; // Fully approved
        }
        
        // Use routing engine for smart selection
        User nextApprover = routingEngine.findBestApprover(
            nextRole, 
            request.getTargetOrganizationId(),
            request.getPriority()
        );
        
        if (nextApprover != null) {
            request.setCurrentApproverId(String.valueOf(nextApprover.getUserId()));
            LOGGER.info("Routed to next approver: " + nextApprover.getFullName() + 
                       " (" + nextRole + ", priority: " + request.getPriority() + ")");
        } else {
            LOGGER.warning("Could not find approver for role: " + nextRole);
        }
    }
    
    // Note: findApproverByRole is now handled by WorkRequestRoutingEngine
    
    /**
     * Get statistics about work requests.
     */
    public WorkRequestStats getStatistics() {
        try {
            long total = requestDAO.count();
            long pending = requestDAO.countByStatus(WorkRequest.RequestStatus.PENDING);
            long inProgress = requestDAO.countByStatus(WorkRequest.RequestStatus.IN_PROGRESS);
            long approved = requestDAO.countByStatus(WorkRequest.RequestStatus.APPROVED);
            long rejected = requestDAO.countByStatus(WorkRequest.RequestStatus.REJECTED);
            long completed = requestDAO.countByStatus(WorkRequest.RequestStatus.COMPLETED);
            long cancelled = requestDAO.countByStatus(WorkRequest.RequestStatus.CANCELLED);
            
            return new WorkRequestStats(total, pending, inProgress, approved, 
                                       rejected, completed, cancelled);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting statistics", e);
            return new WorkRequestStats(0, 0, 0, 0, 0, 0, 0);
        }
    }
    
    /**
     * Get overdue requests (SLA monitoring).
     */
    public List<WorkRequest> getOverdueRequests() {
        List<WorkRequest> allRequests = getAllRequests();
        return routingEngine.getOverdueRequests(allRequests);
    }
    
    /**
     * Get requests approaching SLA breach.
     */
    public List<WorkRequest> getApproachingSlaRequests() {
        List<WorkRequest> allRequests = getAllRequests();
        return routingEngine.getApproachingSlaRequests(allRequests);
    }
    
    /**
     * Get routing engine (for advanced operations).
     */
    public WorkRequestRoutingEngine getRoutingEngine() {
        return routingEngine;
    }
    
    // ==================== DUPLICATE CLAIM DETECTION ====================
    
    /**
     * Result of checking for duplicate claims on an item.
     */
    public static class DisputeCheckResult {
        public boolean hasExistingClaim;
        public ItemClaimRequest existingClaim;
        public MultiEnterpriseDisputeResolution existingDispute;
        
        public DisputeCheckResult(boolean hasExistingClaim, ItemClaimRequest existingClaim, 
                                  MultiEnterpriseDisputeResolution existingDispute) {
            this.hasExistingClaim = hasExistingClaim;
            this.existingClaim = existingClaim;
            this.existingDispute = existingDispute;
        }
    }
    
    /**
     * Check if there's already a claim or dispute for the given item.
     * 
     * @param newClaim The new claim being submitted
     * @return DisputeCheckResult containing existing claim/dispute info
     */
    private DisputeCheckResult checkForDuplicateClaim(ItemClaimRequest newClaim) {
        String itemId = newClaim.getItemId();
        if (itemId == null || itemId.isEmpty()) {
            return new DisputeCheckResult(false, null, null);
        }
        
        try {
            // First, check if there's already a dispute for this item
            List<WorkRequest> disputes = requestDAO.findByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE);
            for (WorkRequest wr : disputes) {
                if (wr instanceof MultiEnterpriseDisputeResolution) {
                    MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
                    if (itemId.equals(dispute.getItemId()) && 
                        !"RESOLVED".equals(dispute.getResolutionStatus()) &&
                        !"CLOSED".equals(dispute.getResolutionStatus())) {
                        // Check if this claimant is already part of the dispute
                        boolean alreadyClaimant = dispute.getClaimants().stream()
                            .anyMatch(c -> c.claimantId != null && 
                                          c.claimantId.equals(newClaim.getRequesterId()));
                        if (!alreadyClaimant) {
                            return new DisputeCheckResult(true, null, dispute);
                        } else {
                            LOGGER.warning("User " + newClaim.getRequesterId() + 
                                          " already has a claim in dispute " + dispute.getRequestId());
                            return new DisputeCheckResult(false, null, null);
                        }
                    }
                }
            }
            
            // Check for existing pending/in-progress claims on this item
            List<WorkRequest> allClaims = requestDAO.findByType(WorkRequest.RequestType.ITEM_CLAIM);
            for (WorkRequest wr : allClaims) {
                if (wr instanceof ItemClaimRequest) {
                    ItemClaimRequest existingClaim = (ItemClaimRequest) wr;
                    
                    // Must be for the same item
                    if (!itemId.equals(existingClaim.getItemId())) {
                        continue;
                    }
                    
                    // Must be from a different person
                    if (existingClaim.getRequesterId() != null && 
                        existingClaim.getRequesterId().equals(newClaim.getRequesterId())) {
                        continue;
                    }
                    
                    // Must be in an active status (PENDING, IN_PROGRESS, or APPROVED but not completed)
                    WorkRequest.RequestStatus status = existingClaim.getStatus();
                    if (status == WorkRequest.RequestStatus.PENDING || 
                        status == WorkRequest.RequestStatus.IN_PROGRESS ||
                        status == WorkRequest.RequestStatus.APPROVED) {
                        LOGGER.info("Found existing claim " + existingClaim.getRequestId() + 
                                   " for item " + itemId + " from " + existingClaim.getRequesterName());
                        return new DisputeCheckResult(true, existingClaim, null);
                    }
                }
            }
            
            return new DisputeCheckResult(false, null, null);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking for duplicate claims", e);
            return new DisputeCheckResult(false, null, null);
        }
    }
    
    /**
     * Create a MultiEnterpriseDisputeResolution from two conflicting claims.
     * This is called when a second claim comes in for an item that already has a claim.
     * 
     * @param existingClaim The first claim that was already submitted
     * @param newClaim The new conflicting claim
     * @return The ID of the created dispute
     */
    private String createDisputeFromDuplicateClaims(ItemClaimRequest existingClaim, ItemClaimRequest newClaim) {
        // Create the dispute
        MultiEnterpriseDisputeResolution dispute = new MultiEnterpriseDisputeResolution();
        
        // Set basic info from the item
        dispute.setItemId(existingClaim.getItemId());
        dispute.setItemName(existingClaim.getItemName());
        dispute.setItemCategory(existingClaim.getItemCategory());
        dispute.setItemDescription(existingClaim.getClaimDetails());
        dispute.setEstimatedValue(existingClaim.getItemValue());
        dispute.setHoldingEnterpriseName(existingClaim.getItemHoldingEnterpriseName());
        
        // Set dispute metadata
        dispute.setDisputeType("OWNERSHIP");
        dispute.setDisputeReason("Multiple claimants for the same item");
        dispute.setDisputeInitiatedBy("SYSTEM");
        dispute.setDisputeInitiatedByName("Automatic Dispute Detection");
        dispute.setDisputeStartDate(LocalDateTime.now().toString());
        dispute.setResolutionStatus("PENDING");
        
        // Set requester info (use system as requester for auto-created disputes)
        dispute.setRequesterId("SYSTEM");
        dispute.setRequesterName("Automatic Dispute Detection");
        dispute.setDescription(String.format("Ownership dispute for %s - Multiple claimants detected", 
                                            existingClaim.getItemName()));
        
        // Add the first claimant (from existing claim)
        MultiEnterpriseDisputeResolution.Claimant claimant1 = new MultiEnterpriseDisputeResolution.Claimant();
        claimant1.claimantId = existingClaim.getRequesterId();
        claimant1.claimantName = existingClaim.getRequesterName();
        claimant1.claimantEmail = existingClaim.getRequesterEmail();
        claimant1.enterpriseId = existingClaim.getRequesterEnterpriseId();
        claimant1.enterpriseName = getEnterpriseName(existingClaim.getRequesterEnterpriseId());
        claimant1.organizationId = existingClaim.getRequesterOrganizationId();
        claimant1.organizationName = getOrganizationName(existingClaim.getRequesterOrganizationId());
        claimant1.claimDescription = existingClaim.getClaimDetails();
        claimant1.proofDescription = existingClaim.getProofDescription();
        claimant1.claimSubmittedDate = existingClaim.getCreatedAt() != null ? 
                                       existingClaim.getCreatedAt().toString() : LocalDateTime.now().toString();
        claimant1.claimStatus = "SUBMITTED";
        
        // Get trust score for claimant1
        try {
            claimant1.trustScore = trustScoreService.getTrustScore(existingClaim.getRequesterId());
        } catch (Exception e) {
            claimant1.trustScore = 50.0; // Default
        }
        dispute.addClaimant(claimant1);
        
        // Add the second claimant (from new claim)
        MultiEnterpriseDisputeResolution.Claimant claimant2 = new MultiEnterpriseDisputeResolution.Claimant();
        claimant2.claimantId = newClaim.getRequesterId();
        claimant2.claimantName = newClaim.getRequesterName();
        claimant2.claimantEmail = newClaim.getRequesterEmail();
        claimant2.enterpriseId = newClaim.getRequesterEnterpriseId();
        claimant2.enterpriseName = getEnterpriseName(newClaim.getRequesterEnterpriseId());
        claimant2.organizationId = newClaim.getRequesterOrganizationId();
        claimant2.organizationName = getOrganizationName(newClaim.getRequesterOrganizationId());
        claimant2.claimDescription = newClaim.getClaimDetails();
        claimant2.proofDescription = newClaim.getProofDescription();
        claimant2.claimSubmittedDate = LocalDateTime.now().toString();
        claimant2.claimStatus = "SUBMITTED";
        
        // Get trust score for claimant2
        try {
            claimant2.trustScore = trustScoreService.getTrustScore(newClaim.getRequesterId());
        } catch (Exception e) {
            claimant2.trustScore = 50.0; // Default
        }
        dispute.addClaimant(claimant2);
        
        // Set priority based on item value
        if (existingClaim.isHighValue() || existingClaim.getItemValue() > 500) {
            dispute.setPriority(WorkRequest.RequestPriority.URGENT);
            dispute.setPoliceInvolved(true);
        } else {
            dispute.setPriority(WorkRequest.RequestPriority.HIGH);
        }
        
        // Update the existing claim status to indicate it's now part of a dispute
        existingClaim.setStatus(WorkRequest.RequestStatus.IN_PROGRESS);
        existingClaim.setNotes((existingClaim.getNotes() != null ? existingClaim.getNotes() + "\n" : "") +
            "⚖️ DISPUTE CREATED: This claim is now part of a multi-claimant dispute.");
        requestDAO.save(existingClaim);
        
        // Save and return the dispute
        String disputeId = requestDAO.save(dispute);
        
        // Log the dispute creation
        LOGGER.info(String.format("Created dispute %s for item %s with claimants: %s and %s",
            disputeId, existingClaim.getItemId(), 
            claimant1.claimantName, claimant2.claimantName));
        
        return disputeId;
    }
    
    /**
     * Add a new claimant to an existing dispute.
     * 
     * @param dispute The existing dispute
     * @param newClaim The new claim to add as a claimant
     */
    private void addClaimantToExistingDispute(MultiEnterpriseDisputeResolution dispute, ItemClaimRequest newClaim) {
        MultiEnterpriseDisputeResolution.Claimant claimant = new MultiEnterpriseDisputeResolution.Claimant();
        claimant.claimantId = newClaim.getRequesterId();
        claimant.claimantName = newClaim.getRequesterName();
        claimant.claimantEmail = newClaim.getRequesterEmail();
        claimant.enterpriseId = newClaim.getRequesterEnterpriseId();
        claimant.enterpriseName = getEnterpriseName(newClaim.getRequesterEnterpriseId());
        claimant.organizationId = newClaim.getRequesterOrganizationId();
        claimant.organizationName = getOrganizationName(newClaim.getRequesterOrganizationId());
        claimant.claimDescription = newClaim.getClaimDetails();
        claimant.proofDescription = newClaim.getProofDescription();
        claimant.claimSubmittedDate = LocalDateTime.now().toString();
        claimant.claimStatus = "SUBMITTED";
        
        try {
            claimant.trustScore = trustScoreService.getTrustScore(newClaim.getRequesterId());
        } catch (Exception e) {
            claimant.trustScore = 50.0;
        }
        
        dispute.addClaimant(claimant);
        dispute.addNote("New claimant added: " + claimant.claimantName + " from " + claimant.enterpriseName);
        
        // If we now have 3+ claimants, escalate priority
        if (dispute.getClaimantCount() >= 3 && dispute.getPriority() != WorkRequest.RequestPriority.URGENT) {
            dispute.setPriority(WorkRequest.RequestPriority.URGENT);
            dispute.setPoliceInvolved(true);
            dispute.addNote("ESCALATED: 3+ claimants detected. Police involvement required.");
        }
        
        requestDAO.save(dispute);
        
        LOGGER.info(String.format("Added claimant %s to existing dispute %s. Total claimants: %d",
            claimant.claimantName, dispute.getRequestId(), dispute.getClaimantCount()));
    }
    
    /**
     * Get enterprise name from ID (helper method).
     */
    private String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null) return "Unknown Enterprise";
        // Map common enterprise IDs to names
        return switch (enterpriseId.toUpperCase()) {
            case "UNIVERSITY", "UNI" -> "University";
            case "MBTA", "TRANSIT" -> "MBTA Transit";
            case "AIRPORT", "LOGAN" -> "Logan Airport";
            case "POLICE", "BPD" -> "Boston Police";
            default -> enterpriseId;
        };
    }
    
    /**
     * Get organization name from ID (helper method).
     */
    private String getOrganizationName(String organizationId) {
        if (organizationId == null) return "Unknown Organization";
        try {
            // Try to look up the organization
            // For now, return the ID as the name
            return organizationId;
        } catch (Exception e) {
            return organizationId;
        }
    }
    
    /**
     * Get all disputes for a specific item.
     * 
     * @param itemId The item ID
     * @return List of disputes for this item
     */
    public List<MultiEnterpriseDisputeResolution> getDisputesForItem(String itemId) {
        List<MultiEnterpriseDisputeResolution> disputes = new ArrayList<>();
        try {
            List<WorkRequest> allDisputes = requestDAO.findByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE);
            for (WorkRequest wr : allDisputes) {
                if (wr instanceof MultiEnterpriseDisputeResolution) {
                    MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
                    if (itemId.equals(dispute.getItemId())) {
                        disputes.add(dispute);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting disputes for item: " + itemId, e);
        }
        return disputes;
    }
    
    /**
     * Get all disputes that involve a specific user (as a claimant).
     * 
     * @param userId The user ID
     * @return List of disputes involving this user
     */
    public List<MultiEnterpriseDisputeResolution> getDisputesForUser(String userId) {
        List<MultiEnterpriseDisputeResolution> disputes = new ArrayList<>();
        try {
            List<WorkRequest> allDisputes = requestDAO.findByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE);
            for (WorkRequest wr : allDisputes) {
                if (wr instanceof MultiEnterpriseDisputeResolution) {
                    MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
                    boolean isInvolved = dispute.getClaimants().stream()
                        .anyMatch(c -> userId.equals(c.claimantId));
                    if (isInvolved) {
                        disputes.add(dispute);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting disputes for user: " + userId, e);
        }
        return disputes;
    }
    
    /**
     * Get all pending disputes that require police involvement.
     * 
     * @return List of disputes needing police action
     */
    public List<MultiEnterpriseDisputeResolution> getDisputesRequiringPolice() {
        List<MultiEnterpriseDisputeResolution> disputes = new ArrayList<>();
        try {
            List<WorkRequest> allDisputes = requestDAO.findByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE);
            for (WorkRequest wr : allDisputes) {
                if (wr instanceof MultiEnterpriseDisputeResolution) {
                    MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
                    if (dispute.isPoliceInvolved() && 
                        !"RESOLVED".equals(dispute.getResolutionStatus()) &&
                        !"CLOSED".equals(dispute.getResolutionStatus())) {
                        disputes.add(dispute);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting disputes requiring police", e);
        }
        return disputes;
    }
    
    /**
     * Record a panel vote on a dispute.
     * 
     * @param disputeId The dispute ID
     * @param voterId The voter's user ID
     * @param voterName The voter's name
     * @param voterRole The voter's role
     * @param votedForClaimantId The claimant they're voting for
     * @param reason The reason for their vote
     * @return true if vote recorded successfully
     */
    public boolean recordDisputeVote(String disputeId, String voterId, String voterName,
                                     String voterRole, String votedForClaimantId, String reason) {
        try {
            WorkRequest wr = requestDAO.findById(disputeId);
            if (!(wr instanceof MultiEnterpriseDisputeResolution)) {
                LOGGER.warning("Not a dispute: " + disputeId);
                return false;
            }
            
            MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
            
            // Check if this voter is already a panel member
            boolean found = false;
            for (MultiEnterpriseDisputeResolution.PanelMember pm : dispute.getVerificationPanel()) {
                if (pm.memberId.equals(voterId)) {
                    found = true;
                    break;
                }
            }
            
            // If not already a panel member, add them
            if (!found) {
                dispute.addPanelMember(voterId, voterName, voterRole, getEnterpriseName(voterRole));
            }
            
            // Record the vote
            dispute.recordPanelVote(voterId, votedForClaimantId, reason);
            
            requestDAO.save(dispute);
            
            LOGGER.info(String.format("Recorded vote in dispute %s: %s voted for claimant %s",
                disputeId, voterName, votedForClaimantId));
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording dispute vote", e);
            return false;
        }
    }
    
    /**
     * Record police findings for a dispute.
     * 
     * @param disputeId The dispute ID
     * @param officerId The police officer's ID
     * @param officerName The police officer's name
     * @param reportNumber The police report number
     * @param findings The police findings/report
     * @return true if findings recorded successfully
     */
    public boolean recordPoliceFindingsForDispute(String disputeId, String officerId, String officerName,
                                                   String reportNumber, String findings) {
        try {
            WorkRequest wr = requestDAO.findById(disputeId);
            if (!(wr instanceof MultiEnterpriseDisputeResolution)) {
                LOGGER.warning("Not a dispute: " + disputeId);
                return false;
            }
            
            MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
            
            dispute.setPoliceOfficerId(officerId);
            dispute.setPoliceOfficerName(officerName);
            dispute.setPoliceReportNumber(reportNumber);
            dispute.setPoliceFindingsReport(findings);
            dispute.setPoliceInvolved(true);
            dispute.addNote("Police findings recorded by " + officerName + " (Report #" + reportNumber + ")");
            
            requestDAO.save(dispute);
            
            LOGGER.info(String.format("Recorded police findings for dispute %s by officer %s",
                disputeId, officerName));
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording police findings", e);
            return false;
        }
    }
    
    /**
     * Resolve a dispute with a final decision.
     * 
     * @param disputeId The dispute ID
     * @param winningClaimantId The ID of the claimant who wins
     * @param reason The reason for the decision
     * @param decidedBy Who made the decision
     * @return true if resolved successfully
     */
    public boolean resolveDispute(String disputeId, String winningClaimantId, 
                                  String reason, String decidedBy) {
        try {
            WorkRequest wr = requestDAO.findById(disputeId);
            if (!(wr instanceof MultiEnterpriseDisputeResolution)) {
                LOGGER.warning("Not a dispute: " + disputeId);
                return false;
            }
            
            MultiEnterpriseDisputeResolution dispute = (MultiEnterpriseDisputeResolution) wr;
            
            // Find the winning claimant's name
            String winnerName = "Unknown";
            for (MultiEnterpriseDisputeResolution.Claimant c : dispute.getClaimants()) {
                if (c.claimantId.equals(winningClaimantId)) {
                    winnerName = c.claimantName;
                    c.claimStatus = "APPROVED";
                } else {
                    c.claimStatus = "REJECTED";
                }
            }
            
            dispute.setWinningClaimantId(winningClaimantId);
            dispute.setWinningClaimantName(winnerName);
            dispute.setResolutionStatus("RESOLVED");
            dispute.setResolutionDecision("AWARDED");
            dispute.setResolutionReason(reason);
            dispute.addNote("Dispute resolved by " + decidedBy + ". Item awarded to " + winnerName + ". Reason: " + reason);
            dispute.complete();
            
            requestDAO.save(dispute);
            
            // Record trust score events
            try {
                // Reward the winner
                trustScoreService.recordEvent(winningClaimantId, 
                    EventType.SUCCESSFUL_CLAIM,
                    "Won ownership dispute for item: " + dispute.getItemName(),
                    dispute.getItemId(), disputeId, null);
                
                // Penalize losing claimants (minor penalty, they may have been honest but wrong)
                for (MultiEnterpriseDisputeResolution.Claimant c : dispute.getClaimants()) {
                    if (!c.claimantId.equals(winningClaimantId)) {
                        trustScoreService.recordEvent(c.claimantId,
                            EventType.CLAIM_REJECTED,
                            "Lost ownership dispute for item: " + dispute.getItemName(),
                            dispute.getItemId(), disputeId, null);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error recording trust score events for dispute resolution", e);
            }
            
            LOGGER.info(String.format("Resolved dispute %s: %s wins", disputeId, winnerName));
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resolving dispute", e);
            return false;
        }
    }
    
    // ==================== INNER CLASS ====================
    
    /**
     * Statistics about work requests.
     */
    public static class WorkRequestStats {
        private final long total;
        private final long pending;
        private final long inProgress;
        private final long approved;
        private final long rejected;
        private final long completed;
        private final long cancelled;
        
        public WorkRequestStats(long total, long pending, long inProgress, 
                               long approved, long rejected, long completed, long cancelled) {
            this.total = total;
            this.pending = pending;
            this.inProgress = inProgress;
            this.approved = approved;
            this.rejected = rejected;
            this.completed = completed;
            this.cancelled = cancelled;
        }
        
        public long getTotal() { return total; }
        public long getPending() { return pending; }
        public long getInProgress() { return inProgress; }
        public long getApproved() { return approved; }
        public long getRejected() { return rejected; }
        public long getCompleted() { return completed; }
        public long getCancelled() { return cancelled; }
        
        @Override
        public String toString() {
            return String.format("WorkRequestStats{total=%d, pending=%d, inProgress=%d, " +
                               "approved=%d, rejected=%d, completed=%d, cancelled=%d}",
                               total, pending, inProgress, approved, rejected, completed, cancelled);
        }
    }
    
    /**
     * Result of trust score check for a requester.
     */
    public static class TrustScoreCheckResult {
        public final double score;
        public final boolean requiresFlag;
        public final String message;
        
        public TrustScoreCheckResult(double score, boolean requiresFlag, String message) {
            this.score = score;
            this.requiresFlag = requiresFlag;
            this.message = message;
        }
    }
    
    /**
     * Format RequestType enum for display.
     */
    private String formatRequestType(WorkRequest.RequestType type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case ITEM_CLAIM -> "Item Claim";
            case CROSS_CAMPUS_TRANSFER -> "Cross-Campus Transfer";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "Transit Transfer";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "Airport Transfer";
            case POLICE_EVIDENCE_REQUEST -> "Police Evidence";
            case MBTA_TO_AIRPORT_EMERGENCY -> "MBTA to Airport Emergency";
            case MULTI_ENTERPRISE_DISPUTE -> "Multi-Enterprise Dispute";
        };
    }
    
    // ==================== TRUST SCORE INTEGRATION ====================
    
    /**
     * Check requester's trust score and determine if request needs flagging.
     * Returns a result object with score, flag status, and message.
     * 
     * @param request The work request to check
     * @return TrustScoreCheckResult with score details
     */
    private TrustScoreCheckResult checkRequesterTrustScore(WorkRequest request) {
        try {
            String requesterId = request.getRequesterId();
            double score = trustScoreService.getTrustScore(requesterId);
            
            // Check various trust conditions
            if (trustScoreService.isFlagged(requesterId)) {
                return new TrustScoreCheckResult(score, true, 
                    "User is flagged for review (Score: " + String.format("%.0f", score) + ")");
            }
            
            if (trustScoreService.isUnderInvestigation(requesterId)) {
                return new TrustScoreCheckResult(score, true, 
                    "User is under active investigation (Score: " + String.format("%.0f", score) + ")");
            }
            
            if (score < 30) {
                return new TrustScoreCheckResult(score, true, 
                    "User on PROBATION - requires extra scrutiny (Score: " + String.format("%.0f", score) + ")");
            }
            
            if (score < 50) {
                return new TrustScoreCheckResult(score, true, 
                    "Low trust user - verify carefully (Score: " + String.format("%.0f", score) + ")");
            }
            
            // Score is acceptable
            return new TrustScoreCheckResult(score, false, 
                "Trust score acceptable (" + String.format("%.0f", score) + ")");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking trust score for request: " + request.getRequestId(), e);
            // Default to cautious approach
            return new TrustScoreCheckResult(50, false, "Unable to verify trust score");
        }
    }
    
    /**
     * Record trust score events when a request is approved.
     * - Rewards the approver for processing the request
     * - May reward the requester if the request was successful
     * 
     * @param request The approved request
     * @param approverId The user who approved
     */
    private void recordApprovalTrustEvents(WorkRequest request, String approverId) {
        try {
            // Reward the approver for processing
            trustScoreService.recordEvent(
                approverId, 
                EventType.APPROVE_REQUEST,
                "Approved " + formatRequestType(request.getRequestType()) + " request",
                null, request.getRequestId(), null
            );
            
            // If fully approved (not just one step), consider rewarding requester
            if (request.getStatus() == WorkRequest.RequestStatus.APPROVED) {
                // Record successful request for requester (small bonus)
                trustScoreService.recordEvent(
                    request.getRequesterId(),
                    EventType.REQUEST_COMPLETED,
                    "Work request completed successfully",
                    null, request.getRequestId(), null
                );
            }
            
            LOGGER.fine("Recorded approval trust events for request: " + request.getRequestId());
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error recording approval trust events", e);
            // Don't fail the approval just because trust recording failed
        }
    }
    
    /**
     * Record trust score events when a request is rejected.
     * - May penalize the requester if the rejection indicates bad behavior
     * 
     * @param request The rejected request
     * @param approverId The user who rejected
     * @param reason The rejection reason
     */
    private void recordRejectionTrustEvents(WorkRequest request, String approverId, String reason) {
        try {
            String lowerReason = reason.toLowerCase();
            
            // Check if rejection indicates fraud or bad behavior
            if (lowerReason.contains("fraud") || lowerReason.contains("false") || 
                lowerReason.contains("fake") || lowerReason.contains("stolen")) {
                // Severe penalty for fraudulent requests
                trustScoreService.recordEvent(
                    request.getRequesterId(),
                    EventType.FALSE_CLAIM,
                    "Request rejected as fraudulent: " + reason,
                    null, request.getRequestId(), null
                );
                LOGGER.info("Recorded fraud penalty for user: " + request.getRequesterId());
                
            } else if (lowerReason.contains("invalid") || lowerReason.contains("incomplete") ||
                       lowerReason.contains("insufficient")) {
                // Minor penalty for invalid/incomplete requests
                trustScoreService.recordEvent(
                    request.getRequesterId(),
                    EventType.CLAIM_REJECTED,
                    "Request rejected: " + reason,
                    null, request.getRequestId(), null
                );
                
            } else {
                // No penalty for normal rejections (policy reasons, etc.)
                LOGGER.fine("Request rejected without trust penalty (policy rejection)");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error recording rejection trust events", e);
            // Don't fail the rejection just because trust recording failed
        }
    }
    
    /**
     * Update item status when an ItemClaimRequest is fully approved.
     * Sets the found item to CLAIMED status and records the resolved date.
     * 
     * @param claimRequest The approved claim request
     */
    private void updateItemStatusOnClaimApproval(ItemClaimRequest claimRequest) {
        try {
            // Update the FOUND item (the item being claimed)
            String foundItemId = claimRequest.getItemId();
            if (foundItemId != null && !foundItemId.isEmpty()) {
                var foundItemOpt = itemDAO.findById(foundItemId);
                if (foundItemOpt.isPresent()) {
                    Item foundItem = foundItemOpt.get();
                    foundItem.setStatus(Item.ItemStatus.CLAIMED);
                    foundItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(foundItem)) {
                        LOGGER.info("Found item " + foundItemId + " marked as CLAIMED after claim approval");
                    } else {
                        LOGGER.warning("Failed to update found item status for: " + foundItemId);
                    }
                } else {
                    LOGGER.warning("Found item not found for claim approval: " + foundItemId);
                }
            }
            
            // Update the LOST item (the student's original lost item) if available
            String lostItemId = claimRequest.getLostItemId();
            if (lostItemId != null && !lostItemId.isEmpty()) {
                var lostItemOpt = itemDAO.findById(lostItemId);
                if (lostItemOpt.isPresent()) {
                    Item lostItem = lostItemOpt.get();
                    lostItem.setStatus(Item.ItemStatus.CLAIMED);
                    lostItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(lostItem)) {
                        LOGGER.info("Lost item " + lostItemId + " marked as CLAIMED after claim approval");
                    } else {
                        LOGGER.warning("Failed to update lost item status for: " + lostItemId);
                    }
                } else {
                    LOGGER.warning("Lost item not found for claim approval: " + lostItemId);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating item status on claim approval", e);
            // Don't fail the approval just because item update failed
        }
    }
    
    /**
     * Update item status when a TransitToUniversityTransferRequest is fully approved (student pickup confirmed).
     * Sets both the found item and matched lost item to CLAIMED status.
     * 
     * @param transferRequest The approved transfer request
     */
    private void updateItemStatusOnTransferApproval(TransitToUniversityTransferRequest transferRequest) {
        try {
            // Update the FOUND item (the item transferred from MBTA)
            String foundItemId = transferRequest.getItemId();
            if (foundItemId != null && !foundItemId.isEmpty()) {
                var foundItemOpt = itemDAO.findById(foundItemId);
                if (foundItemOpt.isPresent()) {
                    Item foundItem = foundItemOpt.get();
                    foundItem.setStatus(Item.ItemStatus.CLAIMED);
                    foundItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(foundItem)) {
                        LOGGER.info("Found item " + foundItemId + " marked as CLAIMED after transfer pickup");
                    } else {
                        LOGGER.warning("Failed to update found item status for: " + foundItemId);
                    }
                } else {
                    LOGGER.warning("Found item not found for transfer: " + foundItemId);
                }
            }
            
            // Update the LOST item (the student's original lost report) if it was auto-matched
            String lostItemId = transferRequest.getLostItemId();
            if (lostItemId != null && !lostItemId.isEmpty()) {
                var lostItemOpt = itemDAO.findById(lostItemId);
                if (lostItemOpt.isPresent()) {
                    Item lostItem = lostItemOpt.get();
                    lostItem.setStatus(Item.ItemStatus.CLAIMED);
                    lostItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(lostItem)) {
                        LOGGER.info("Lost item " + lostItemId + " marked as CLAIMED after transfer pickup (auto-matched)");
                    } else {
                        LOGGER.warning("Failed to update lost item status for: " + lostItemId);
                    }
                } else {
                    LOGGER.warning("Lost item not found for transfer: " + lostItemId);
                }
            } else {
                LOGGER.info("No matched lost item ID for transfer - student can manually resolve their lost item");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating item status on transfer approval", e);
            // Don't fail the approval just because item update failed
        }
    }
    
    /**
     * Update item status when an AirportToUniversityTransferRequest is fully approved (student pickup confirmed).
     * Sets both the found item and matched lost item to CLAIMED status.
     * 
     * @param transferRequest The approved airport transfer request
     */
    private void updateItemStatusOnAirportTransferApproval(AirportToUniversityTransferRequest transferRequest) {
        try {
            // Update the FOUND item (the item transferred from Airport)
            String foundItemId = transferRequest.getItemId();
            if (foundItemId != null && !foundItemId.isEmpty()) {
                var foundItemOpt = itemDAO.findById(foundItemId);
                if (foundItemOpt.isPresent()) {
                    Item foundItem = foundItemOpt.get();
                    foundItem.setStatus(Item.ItemStatus.CLAIMED);
                    foundItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(foundItem)) {
                        LOGGER.info("Found item " + foundItemId + " marked as CLAIMED after airport transfer pickup");
                    } else {
                        LOGGER.warning("Failed to update found item status for: " + foundItemId);
                    }
                } else {
                    LOGGER.warning("Found item not found for airport transfer: " + foundItemId);
                }
            }
            
            // Update the LOST item (the student's original lost report) if it was auto-matched
            String lostItemId = transferRequest.getLostItemId();
            if (lostItemId != null && !lostItemId.isEmpty()) {
                var lostItemOpt = itemDAO.findById(lostItemId);
                if (lostItemOpt.isPresent()) {
                    Item lostItem = lostItemOpt.get();
                    lostItem.setStatus(Item.ItemStatus.CLAIMED);
                    lostItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(lostItem)) {
                        LOGGER.info("Lost item " + lostItemId + " marked as CLAIMED after airport transfer pickup (auto-matched)");
                    } else {
                        LOGGER.warning("Failed to update lost item status for: " + lostItemId);
                    }
                } else {
                    LOGGER.warning("Lost item not found for airport transfer: " + lostItemId);
                }
            } else {
                LOGGER.info("No matched lost item ID for airport transfer - student can manually resolve their lost item");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating item status on airport transfer approval", e);
            // Don't fail the approval just because item update failed
        }
    }
    
    /**
     * Update item status when a CrossCampusTransferRequest is fully approved (student pickup confirmed).
     * Sets both the found item and matched lost item to CLAIMED status.
     * 
     * @param transferRequest The approved cross-campus transfer request
     */
    private void updateItemStatusOnCrossCampusTransferApproval(CrossCampusTransferRequest transferRequest) {
        try {
            // Update the FOUND item (the item transferred between campuses)
            String foundItemId = transferRequest.getItemId();
            if (foundItemId != null && !foundItemId.isEmpty()) {
                var foundItemOpt = itemDAO.findById(foundItemId);
                if (foundItemOpt.isPresent()) {
                    Item foundItem = foundItemOpt.get();
                    foundItem.setStatus(Item.ItemStatus.CLAIMED);
                    foundItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(foundItem)) {
                        LOGGER.info("Found item " + foundItemId + " marked as CLAIMED after cross-campus transfer pickup");
                    } else {
                        LOGGER.warning("Failed to update found item status for: " + foundItemId);
                    }
                } else {
                    LOGGER.warning("Found item not found for cross-campus transfer: " + foundItemId);
                }
            }
            
            // Update the LOST item (the student's original lost report) if it was auto-matched
            String lostItemId = transferRequest.getLostItemId();
            if (lostItemId != null && !lostItemId.isEmpty()) {
                var lostItemOpt = itemDAO.findById(lostItemId);
                if (lostItemOpt.isPresent()) {
                    Item lostItem = lostItemOpt.get();
                    lostItem.setStatus(Item.ItemStatus.CLAIMED);
                    lostItem.setResolvedDate(new java.util.Date());
                    
                    if (itemDAO.update(lostItem)) {
                        LOGGER.info("Lost item " + lostItemId + " marked as CLAIMED after cross-campus transfer pickup (auto-matched)");
                    } else {
                        LOGGER.warning("Failed to update lost item status for: " + lostItemId);
                    }
                } else {
                    LOGGER.warning("Lost item not found for cross-campus transfer: " + lostItemId);
                }
            } else {
                LOGGER.info("No matched lost item ID for cross-campus transfer - student can manually resolve their lost item");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating item status on cross-campus transfer approval", e);
            // Don't fail the approval just because item update failed
        }
    }
    
    /**
     * Update item status when an MBTAToAirportEmergencyRequest is fully approved.
     * Sets the item to PENDING_CLAIM status since it's now at the airport awaiting traveler pickup.
     * 
     * @param emergencyRequest The approved emergency transfer request
     */
    private void updateItemStatusOnEmergencyTransfer(MBTAToAirportEmergencyRequest emergencyRequest) {
        try {
            // Update the FOUND item (the item transferred from MBTA to Airport)
            String itemId = emergencyRequest.getItemId();
            if (itemId != null && !itemId.isEmpty()) {
                var itemOpt = itemDAO.findById(itemId);
                if (itemOpt.isPresent()) {
                    Item item = itemOpt.get();
                    item.setStatus(Item.ItemStatus.CLAIMED);
                    item.setResolvedDate(new java.util.Date());
                    
                    // Add tracking info to keywords
                    List<String> keywords = item.getKeywords();
                    if (keywords == null) keywords = new java.util.ArrayList<>();
                    keywords.add("Emergency Transfer from MBTA");
                    keywords.add("Traveler: " + emergencyRequest.getTravelerName());
                    keywords.add("Flight: " + emergencyRequest.getFlightNumber());
                    if (emergencyRequest.getAirportTerminal() != null) {
                        keywords.add(emergencyRequest.getAirportTerminal());
                    }
                    item.setKeywords(keywords);
                    
                    if (itemDAO.update(item)) {
                        LOGGER.info("Item " + itemId + " marked as PENDING_CLAIM after emergency transfer approval");
                    } else {
                        LOGGER.warning("Failed to update item status for emergency transfer: " + itemId);
                    }
                } else {
                    LOGGER.warning("Item not found for emergency transfer: " + itemId);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating item status on emergency transfer approval", e);
            // Don't fail the approval just because item update failed
        }
    }
}
