package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoVerificationDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.verification.VerificationRequest;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationType;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationStatus;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationPriority;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for Verification business logic.
 * 
 * Provides a clean API for:
 * - Creating verification requests
 * - Processing verifications (assign, complete, fail)
 * - Business rule checks (requires police, multi-party approval)
 * - Query operations
 * - Integration with TrustScoreService
 * 
 * This service coordinates between verification requests and other systems
 * to ensure security protocols are followed consistently.
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class VerificationService {
    
    private static final Logger LOGGER = Logger.getLogger(VerificationService.class.getName());
    
    // Thresholds
    public static final double HIGH_VALUE_THRESHOLD = 500.0;      // Items over $500 need verification
    public static final double VERY_HIGH_VALUE_THRESHOLD = 2000.0; // Items over $2000 need police
    public static final double LOW_TRUST_THRESHOLD = 50.0;        // Users below 50 need extra verification
    public static final double HIGH_TRUST_THRESHOLD = 85.0;       // Users above 85 can skip some verification
    
    private final MongoVerificationDAO verificationDAO;
    private final MongoUserDAO userDAO;
    private final MongoItemDAO itemDAO;
    private TrustScoreService trustScoreService;
    
    // ==================== CONSTRUCTORS ====================
    
    public VerificationService() {
        this.verificationDAO = new MongoVerificationDAO();
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        
        // Lazy load TrustScoreService to avoid circular dependency
        try {
            this.trustScoreService = new TrustScoreService();
        } catch (Exception e) {
            LOGGER.warning("Could not initialize TrustScoreService: " + e.getMessage());
        }
    }
    
    /**
     * Constructor for testing with mock DAOs
     */
    public VerificationService(MongoVerificationDAO verificationDAO, 
                               MongoUserDAO userDAO, 
                               MongoItemDAO itemDAO,
                               TrustScoreService trustScoreService) {
        this.verificationDAO = verificationDAO;
        this.userDAO = userDAO;
        this.itemDAO = itemDAO;
        this.trustScoreService = trustScoreService;
    }
    
    // ==================== REQUEST CREATION ====================
    
    /**
     * Create a generic verification request
     * 
     * @param type The type of verification needed
     * @param subjectUserId The user being verified (can be null for item-only)
     * @param requesterId The user requesting verification
     * @return The created VerificationRequest
     */
    public VerificationRequest createVerificationRequest(VerificationType type, 
                                                          String subjectUserId, 
                                                          String requesterId) {
        try {
            VerificationRequest request = new VerificationRequest(type, subjectUserId, requesterId);
            
            // Set priority based on type
            request.setPriority(determinePriority(type, null));
            
            // Populate user details if available
            populateUserDetails(request, subjectUserId, requesterId);
            
            // Save to database
            String requestId = verificationDAO.save(request);
            request.setRequestId(requestId);
            
            LOGGER.info("Created verification request: " + request.getVisibleId() + 
                       " type: " + type + " for user: " + subjectUserId);
            
            return request;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating verification request", e);
            return null;
        }
    }
    
    /**
     * Create verification request with item context
     */
    public VerificationRequest createVerificationRequest(VerificationType type,
                                                          String subjectUserId,
                                                          String subjectItemId,
                                                          String requesterId) {
        try {
            VerificationRequest request = new VerificationRequest(type, subjectUserId, 
                                                                   subjectItemId, requesterId);
            
            // Populate user details
            populateUserDetails(request, subjectUserId, requesterId);
            
            // Populate item details
            populateItemDetails(request, subjectItemId);
            
            // Set priority based on type and item value
            request.setPriority(determinePriority(type, request.getSubjectItemValue()));
            
            // Set multi-party approval requirements if needed
            if (requiresMultiPartyApproval(request)) {
                request.setRequiredApprovals(getRequiredApprovalCount(request));
            }
            
            // Save to database
            String requestId = verificationDAO.save(request);
            request.setRequestId(requestId);
            
            LOGGER.info("Created verification request: " + request.getVisibleId() + 
                       " type: " + type + " for item: " + subjectItemId);
            
            return request;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating verification request", e);
            return null;
        }
    }
    
    /**
     * Request identity verification for a user
     */
    public VerificationRequest requestIdentityVerification(String userId, String requesterId) {
        // Check if already has pending identity verification
        if (verificationDAO.hasPendingVerification(userId, VerificationType.IDENTITY_VERIFICATION)) {
            LOGGER.info("User " + userId + " already has pending identity verification");
            List<VerificationRequest> pending = verificationDAO.findByUserId(userId);
            return pending.stream()
                .filter(r -> r.getVerificationType() == VerificationType.IDENTITY_VERIFICATION)
                .filter(r -> !r.getStatus().isTerminal())
                .findFirst()
                .orElse(null);
        }
        
        VerificationRequest request = createVerificationRequest(
            VerificationType.IDENTITY_VERIFICATION, userId, requesterId);
        
        if (request != null) {
            request.setRequestReason("Identity verification required for account security");
        }
        
        return request;
    }
    
    /**
     * Request high-value item verification
     */
    public VerificationRequest requestHighValueItemVerification(String itemId, 
                                                                  String claimerId,
                                                                  String requesterId) {
        try {
            // Get item details
            Optional<Item> itemOpt = itemDAO.findById(itemId);
            if (itemOpt.isEmpty()) {
                LOGGER.warning("Item not found: " + itemId);
                return null;
            }
            
            Item item = itemOpt.get();
            
            // Check if truly high value
            if (item.getEstimatedValue() < HIGH_VALUE_THRESHOLD) {
                LOGGER.info("Item " + itemId + " value below threshold, no verification needed");
                return null;
            }
            
            VerificationRequest request = createVerificationRequest(
                VerificationType.HIGH_VALUE_ITEM_CLAIM,
                claimerId,
                itemId,
                requesterId
            );
            
            if (request != null) {
                request.setRequestReason("High-value item claim verification. " +
                    "Item value: $" + String.format("%.2f", item.getEstimatedValue()));
                
                // If very high value, set urgent priority
                if (item.getEstimatedValue() >= VERY_HIGH_VALUE_THRESHOLD) {
                    request.setPriority(VerificationPriority.HIGH);
                }
                
                verificationDAO.update(request);
            }
            
            return request;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating high-value verification", e);
            return null;
        }
    }
    
    /**
     * Request serial number check against police databases
     */
    public VerificationRequest requestSerialNumberCheck(String itemId, String requesterId) {
        try {
            Optional<Item> itemOpt = itemDAO.findById(itemId);
            if (itemOpt.isEmpty()) {
                LOGGER.warning("Item not found: " + itemId);
                return null;
            }
            
            Item item = itemOpt.get();
            
            // Check if item has serial number
            if (item.getSerialNumber() == null || item.getSerialNumber().isEmpty()) {
                LOGGER.info("Item " + itemId + " has no serial number");
                return null;
            }
            
            VerificationRequest request = createVerificationRequest(
                VerificationType.SERIAL_NUMBER_CHECK,
                null, // No subject user
                itemId,
                requesterId
            );
            
            if (request != null) {
                request.setRequestReason("Serial number verification for item: " + 
                    item.getName() + " (S/N: " + item.getSerialNumber() + ")");
                request.setPriority(VerificationPriority.HIGH); // Police checks are high priority
                verificationDAO.update(request);
            }
            
            return request;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating serial number check", e);
            return null;
        }
    }
    
    /**
     * Request stolen property check
     */
    public VerificationRequest requestStolenPropertyCheck(String itemId, String requesterId) {
        VerificationRequest request = createVerificationRequest(
            VerificationType.STOLEN_PROPERTY_CHECK,
            null,
            itemId,
            requesterId
        );
        
        if (request != null) {
            request.setRequestReason("Stolen property database check requested");
            request.setPriority(VerificationPriority.URGENT); // Always urgent
            verificationDAO.update(request);
        }
        
        return request;
    }
    
    /**
     * Request cross-enterprise transfer verification
     */
    public VerificationRequest requestCrossEnterpriseVerification(String itemId,
                                                                    String fromUserId,
                                                                    String toUserId,
                                                                    String requesterId) {
        VerificationRequest request = createVerificationRequest(
            VerificationType.CROSS_ENTERPRISE_TRANSFER,
            fromUserId,
            itemId,
            requesterId
        );
        
        if (request != null) {
            request.setRequestReason("Cross-enterprise item transfer verification");
            request.setRequiredApprovals(2); // Requires approval from both sides
            verificationDAO.update(request);
        }
        
        return request;
    }
    
    /**
     * Request student enrollment verification
     */
    public VerificationRequest requestStudentEnrollmentVerification(String studentId, 
                                                                      String requesterId) {
        // Check if already verified
        if (verificationDAO.isUserVerified(studentId, VerificationType.STUDENT_ENROLLMENT)) {
            LOGGER.info("Student " + studentId + " already has verified enrollment");
            return null;
        }
        
        VerificationRequest request = createVerificationRequest(
            VerificationType.STUDENT_ENROLLMENT,
            studentId,
            requesterId
        );
        
        if (request != null) {
            request.setRequestReason("Verify student enrollment status");
        }
        
        return request;
    }
    
    // ==================== PROCESSING ====================
    
    /**
     * Assign a verification request to a verifier
     */
    public boolean assignVerifier(String requestId, String verifierId, 
                                   String verifierName, String verifierRole) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                LOGGER.warning("Request not found: " + requestId);
                return false;
            }
            
            if (!request.canProcess()) {
                LOGGER.warning("Request cannot be processed: " + request.getStatus());
                return false;
            }
            
            request.assignTo(verifierId, verifierName, verifierRole);
            verificationDAO.update(request);
            
            LOGGER.info("Assigned verification " + request.getVisibleId() + 
                       " to " + verifierName);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error assigning verifier", e);
            return false;
        }
    }
    
    /**
     * Complete verification successfully
     */
    public boolean completeVerification(String requestId, String notes) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                LOGGER.warning("Request not found: " + requestId);
                return false;
            }
            
            if (!request.canProcess()) {
                LOGGER.warning("Request cannot be completed: " + request.getStatus());
                return false;
            }
            
            request.complete(notes);
            verificationDAO.update(request);
            
            // Update trust score for verified user
            if (request.getSubjectUserId() != null && trustScoreService != null) {
                // Successful verification is a positive event
                // (This would integrate with TrustScoreService)
                LOGGER.info("Verification completed - trust score update pending for: " + 
                           request.getSubjectUserId());
            }
            
            LOGGER.info("Completed verification: " + request.getVisibleId());
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error completing verification", e);
            return false;
        }
    }
    
    /**
     * Fail a verification
     */
    public boolean failVerification(String requestId, String reason) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                LOGGER.warning("Request not found: " + requestId);
                return false;
            }
            
            if (!request.canProcess()) {
                LOGGER.warning("Request cannot be failed: " + request.getStatus());
                return false;
            }
            
            request.fail(reason);
            verificationDAO.update(request);
            
            // Failed verification may affect trust score
            if (request.getSubjectUserId() != null && trustScoreService != null) {
                LOGGER.info("Verification failed - trust score impact pending for: " + 
                           request.getSubjectUserId());
            }
            
            LOGGER.info("Failed verification: " + request.getVisibleId() + 
                       " reason: " + reason);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error failing verification", e);
            return false;
        }
    }
    
    /**
     * Cancel a verification request
     */
    public boolean cancelVerification(String requestId, String reason) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            request.cancel(reason);
            verificationDAO.update(request);
            
            LOGGER.info("Cancelled verification: " + request.getVisibleId());
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling verification", e);
            return false;
        }
    }
    
    /**
     * Record police check result
     */
    public boolean recordPoliceCheckResult(String requestId, String result, 
                                            String caseNumber, boolean isStolen) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            request.recordPoliceCheck(result, caseNumber, isStolen);
            
            if (isStolen) {
                // If stolen, fail the verification immediately
                request.fail("Item reported as stolen property. Case #: " + caseNumber);
            }
            
            verificationDAO.update(request);
            
            LOGGER.info("Recorded police check for " + request.getVisibleId() + 
                       " stolen: " + isStolen);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording police check", e);
            return false;
        }
    }
    
    /**
     * Record serial number check result
     */
    public boolean recordSerialNumberResult(String requestId, String result, boolean isStolen) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            request.recordSerialCheck(result, isStolen);
            
            if (isStolen) {
                request.fail("Serial number flagged in stolen property database");
            }
            
            verificationDAO.update(request);
            
            LOGGER.info("Recorded serial number check for " + request.getVisibleId());
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording serial number check", e);
            return false;
        }
    }
    
    /**
     * Record multi-party approval
     */
    public boolean recordApproval(String requestId, String approverId, String approverName) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            boolean allApproved = request.recordApproval(approverId, approverName);
            verificationDAO.update(request);
            
            LOGGER.info("Recorded approval for " + request.getVisibleId() + 
                       " from " + approverName + 
                       " (" + request.getCurrentApprovals() + "/" + 
                       request.getRequiredApprovals() + ")");
            
            return allApproved;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording approval", e);
            return false;
        }
    }
    
    /**
     * Expire all old requests
     */
    public int expireOldRequests() {
        return verificationDAO.markExpiredRequests();
    }
    
    /**
     * Update request to awaiting documents
     */
    public boolean setAwaitingDocuments(String requestId, String notes) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            request.awaitDocuments(notes);
            verificationDAO.update(request);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting awaiting documents", e);
            return false;
        }
    }
    
    /**
     * Update request to awaiting external response
     */
    public boolean setAwaitingResponse(String requestId, String notes) {
        try {
            VerificationRequest request = verificationDAO.findById(requestId);
            if (request == null) {
                return false;
            }
            
            request.awaitResponse(notes);
            verificationDAO.update(request);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting awaiting response", e);
            return false;
        }
    }
    
    // ==================== QUERIES ====================
    
    /**
     * Get pending verifications for a verifier
     */
    public List<VerificationRequest> getPendingVerifications(String verifierId) {
        return verificationDAO.findPendingForVerifier(verifierId);
    }
    
    /**
     * Get all unassigned pending requests
     */
    public List<VerificationRequest> getUnassignedRequests() {
        return verificationDAO.findUnassignedPending();
    }
    
    /**
     * Get verification status
     */
    public VerificationStatus getVerificationStatus(String requestId) {
        VerificationRequest request = verificationDAO.findById(requestId);
        return request != null ? request.getStatus() : null;
    }
    
    /**
     * Get full verification request
     */
    public VerificationRequest getVerificationRequest(String requestId) {
        return verificationDAO.findById(requestId);
    }
    
    /**
     * Get verification request by visible ID
     */
    public VerificationRequest getVerificationByVisibleId(String visibleId) {
        return verificationDAO.findByVisibleId(visibleId);
    }
    
    /**
     * Check if user has been verified for a type
     */
    public boolean isUserVerified(String userId, VerificationType type) {
        return verificationDAO.isUserVerified(userId, type);
    }
    
    /**
     * Check if user has pending verification
     */
    public boolean hasPendingVerification(String userId, VerificationType type) {
        return verificationDAO.hasPendingVerification(userId, type);
    }
    
    /**
     * Get verification history for a user
     */
    public List<VerificationRequest> getVerificationHistory(String userId) {
        return verificationDAO.getVerificationHistory(userId);
    }
    
    /**
     * Get verifications for an item
     */
    public List<VerificationRequest> getVerificationsForItem(String itemId) {
        return verificationDAO.findByItemId(itemId);
    }
    
    /**
     * Get requests requiring police involvement
     */
    public List<VerificationRequest> getPoliceRequiredRequests() {
        return verificationDAO.findRequiringPolice();
    }
    
    /**
     * Get flagged stolen items
     */
    public List<VerificationRequest> getStolenItemFlags() {
        return verificationDAO.findStolenFlags();
    }
    
    /**
     * Get high-value pending verifications
     */
    public List<VerificationRequest> getHighValuePending() {
        return verificationDAO.findHighValuePending(HIGH_VALUE_THRESHOLD);
    }
    
    /**
     * Get overdue verifications
     */
    public List<VerificationRequest> getOverdueVerifications() {
        return verificationDAO.findOverdue();
    }
    
    /**
     * Get verifications by status
     */
    public List<VerificationRequest> getVerificationsByStatus(VerificationStatus status) {
        return verificationDAO.findByStatus(status);
    }
    
    /**
     * Get verifications by type
     */
    public List<VerificationRequest> getVerificationsByType(VerificationType type) {
        return verificationDAO.findByType(type);
    }
    
    /**
     * Get all verifications (admin)
     */
    public List<VerificationRequest> getAllVerifications() {
        return verificationDAO.findAll();
    }
    
    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Check if an item requires police verification
     */
    public boolean requiresPoliceVerification(Item item) {
        if (item == null) return false;
        
        // Very high value items require police
        if (item.getEstimatedValue() >= VERY_HIGH_VALUE_THRESHOLD) {
            return true;
        }
        
        // Items with serial numbers should be checked
        if (item.getSerialNumber() != null && !item.getSerialNumber().isEmpty()) {
            // Electronics, jewelry over certain value
            if (item.getEstimatedValue() >= HIGH_VALUE_THRESHOLD) {
                return true;
            }
        }
        
        // Check category for typically stolen items
        if (item.getCategory() != null) {
            String categoryName = item.getCategory().getDisplayName().toLowerCase();
            if (categoryName.contains("electronics") ||
                categoryName.contains("jewelry")) {
                return item.getEstimatedValue() >= HIGH_VALUE_THRESHOLD;
            }
        }
        
        return false;
    }
    
    /**
     * Check if an item requires police verification (by ID)
     */
    public boolean requiresPoliceVerification(String itemId) {
        Optional<Item> itemOpt = itemDAO.findById(itemId);
        return itemOpt.map(this::requiresPoliceVerification).orElse(false);
    }
    
    /**
     * Check if request requires multi-party approval
     */
    public boolean requiresMultiPartyApproval(VerificationRequest request) {
        if (request == null) return false;
        
        // Cross-enterprise transfers always need multi-party
        if (request.getVerificationType() == VerificationType.CROSS_ENTERPRISE_TRANSFER) {
            return true;
        }
        
        // Very high value items need multi-party
        if (request.getSubjectItemValue() >= VERY_HIGH_VALUE_THRESHOLD) {
            return true;
        }
        
        // Items flagged as potentially stolen
        if (request.isReportedStolen()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get required verifications for an item claim based on value and user trust
     */
    public List<VerificationType> getRequiredVerifications(double itemValue, double userTrustScore) {
        List<VerificationType> required = new ArrayList<>();
        
        // Low trust users always need identity verification
        if (userTrustScore < LOW_TRUST_THRESHOLD) {
            required.add(VerificationType.IDENTITY_VERIFICATION);
        }
        
        // High value items
        if (itemValue >= HIGH_VALUE_THRESHOLD) {
            required.add(VerificationType.HIGH_VALUE_ITEM_CLAIM);
            required.add(VerificationType.OWNERSHIP_DOCUMENTATION);
            
            // Unless very high trust
            if (userTrustScore < HIGH_TRUST_THRESHOLD) {
                required.add(VerificationType.IDENTITY_VERIFICATION);
            }
        }
        
        // Very high value - additional checks
        if (itemValue >= VERY_HIGH_VALUE_THRESHOLD) {
            required.add(VerificationType.SERIAL_NUMBER_CHECK);
            required.add(VerificationType.STOLEN_PROPERTY_CHECK);
        }
        
        return required;
    }
    
    /**
     * Get required verifications using user ID
     */
    public List<VerificationType> getRequiredVerifications(double itemValue, String userId) {
        double trustScore = 50.0; // Default
        
        if (trustScoreService != null) {
            trustScore = trustScoreService.getTrustScore(userId);
        }
        
        return getRequiredVerifications(itemValue, trustScore);
    }
    
    /**
     * Check if user can skip verification for an item
     */
    public boolean canSkipVerification(String userId, double itemValue) {
        if (itemValue >= HIGH_VALUE_THRESHOLD) {
            return false; // High value always needs verification
        }
        
        if (trustScoreService != null) {
            double trustScore = trustScoreService.getTrustScore(userId);
            return trustScore >= HIGH_TRUST_THRESHOLD;
        }
        
        return false;
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("totalRequests", verificationDAO.getTotalCount());
        stats.put("pendingCount", verificationDAO.getPendingCount());
        stats.put("todayCompleted", verificationDAO.getTodayCompletions());
        stats.put("avgProcessingHours", verificationDAO.getAverageProcessingTimeHours());
        stats.put("statusCounts", verificationDAO.getCountByStatus());
        stats.put("typeCounts", verificationDAO.getCountByType());
        stats.put("overdueCount", verificationDAO.findOverdue().size());
        stats.put("policeRequiredCount", verificationDAO.findRequiringPolice().size());
        stats.put("stolenFlagsCount", verificationDAO.findStolenFlags().size());
        
        return stats;
    }
    
    /**
     * Get count by status
     */
    public Map<VerificationStatus, Long> getCountByStatus() {
        return verificationDAO.getCountByStatus();
    }
    
    /**
     * Get count by type
     */
    public Map<VerificationType, Long> getCountByType() {
        return verificationDAO.getCountByType();
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Populate user details on a request
     */
    private void populateUserDetails(VerificationRequest request, 
                                      String subjectUserId, 
                                      String requesterId) {
        try {
            // Subject user
            if (subjectUserId != null) {
                Optional<User> subjectOpt = userDAO.findByEmail(subjectUserId);
                if (subjectOpt.isEmpty()) {
                    try {
                        subjectOpt = userDAO.findById(subjectUserId);
                    } catch (Exception e) {
                        // Not a valid ObjectId
                    }
                }
                if (subjectOpt.isPresent()) {
                    User subject = subjectOpt.get();
                    request.setSubjectUserName(subject.getFullName());
                    request.setSubjectUserEmail(subject.getEmail());
                }
            }
            
            // Requester
            if (requesterId != null) {
                Optional<User> requesterOpt = userDAO.findByEmail(requesterId);
                if (requesterOpt.isEmpty()) {
                    try {
                        requesterOpt = userDAO.findById(requesterId);
                    } catch (Exception e) {
                        // Not a valid ObjectId
                    }
                }
                if (requesterOpt.isPresent()) {
                    User requester = requesterOpt.get();
                    request.setRequesterName(requester.getFullName());
                    request.setRequesterEmail(requester.getEmail());
                    request.setRequesterOrganizationId(requester.getOrganizationId());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error populating user details", e);
        }
    }
    
    /**
     * Populate item details on a request
     */
    private void populateItemDetails(VerificationRequest request, String itemId) {
        try {
            if (itemId != null) {
                Optional<Item> itemOpt = itemDAO.findById(itemId);
                if (itemOpt.isPresent()) {
                    Item item = itemOpt.get();
                    request.setSubjectItemName(item.getName());
                    request.setSubjectItemValue(item.getEstimatedValue());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error populating item details", e);
        }
    }
    
    /**
     * Determine priority based on verification type and item value
     */
    private VerificationPriority determinePriority(VerificationType type, Double itemValue) {
        // Stolen property checks are always urgent
        if (type == VerificationType.STOLEN_PROPERTY_CHECK) {
            return VerificationPriority.URGENT;
        }
        
        // Police-related checks are high priority
        if (type.requiresPolice()) {
            return VerificationPriority.HIGH;
        }
        
        // Very high value items are high priority
        if (itemValue != null && itemValue >= VERY_HIGH_VALUE_THRESHOLD) {
            return VerificationPriority.HIGH;
        }
        
        // High value items are normal-high
        if (itemValue != null && itemValue >= HIGH_VALUE_THRESHOLD) {
            return VerificationPriority.NORMAL;
        }
        
        return VerificationPriority.NORMAL;
    }
    
    /**
     * Get required approval count for multi-party requests
     */
    private int getRequiredApprovalCount(VerificationRequest request) {
        if (request.getVerificationType() == VerificationType.CROSS_ENTERPRISE_TRANSFER) {
            return 2; // Both enterprises must approve
        }
        
        if (request.getSubjectItemValue() >= VERY_HIGH_VALUE_THRESHOLD) {
            return 2; // Manager + security
        }
        
        if (request.isReportedStolen()) {
            return 3; // Security + police + manager
        }
        
        return 1;
    }
}
