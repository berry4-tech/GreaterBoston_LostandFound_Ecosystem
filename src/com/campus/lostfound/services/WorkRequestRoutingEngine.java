package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.workrequest.WorkRequest;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced routing engine for work requests.
 * Handles priority-based routing, load balancing, and SLA tracking.
 */
public class WorkRequestRoutingEngine {
    
    private static final Logger LOGGER = Logger.getLogger(WorkRequestRoutingEngine.class.getName());
    private final MongoUserDAO userDAO;
    
    // Track workload per approver for load balancing
    private final Map<String, Integer> approverWorkload = new HashMap<>();
    
    public WorkRequestRoutingEngine() {
        this.userDAO = new MongoUserDAO();
    }
    
    public WorkRequestRoutingEngine(MongoUserDAO userDAO) {
        this.userDAO = userDAO;
    }
    
    /**
     * Find the best approver for a request using advanced routing logic.
     * 
     * @param role Required role
     * @param organizationId Preferred organization
     * @param priority Request priority (affects selection)
     * @return Best available approver, or null if none found
     */
    public User findBestApprover(String role, String organizationId, WorkRequest.RequestPriority priority) {
        List<User> candidates = findCandidateApprovers(role, organizationId);
        
        if (candidates.isEmpty()) {
            LOGGER.warning("No approvers found for role: " + role);
            return null;
        }
        
        // For urgent requests, prefer less busy approvers
        if (priority == WorkRequest.RequestPriority.URGENT) {
            return selectLeastBusyApprover(candidates);
        }
        
        // For normal requests, use round-robin load balancing
        return selectBalancedApprover(candidates);
    }
    
    /**
     * Find all candidate approvers for a role.
     */
    private List<User> findCandidateApprovers(String role, String organizationId) {
        List<User> allUsers = userDAO.findAll();
        
        // First try: exact organization match
        List<User> exactMatch = allUsers.stream()
            .filter(u -> u.getRole().name().equals(role))
            .filter(u -> organizationId == null || organizationId.equals(u.getOrganizationId()))
            .collect(Collectors.toList());
        
        if (!exactMatch.isEmpty()) {
            return exactMatch;
        }
        
        // Fallback: any user with the role
        return allUsers.stream()
            .filter(u -> u.getRole().name().equals(role))
            .collect(Collectors.toList());
    }
    
    /**
     * Select the least busy approver (for urgent requests).
     */
    private User selectLeastBusyApprover(List<User> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        
        // Find approver with lowest workload
        User leastBusy = candidates.get(0);
        int minWorkload = getWorkload(leastBusy);
        
        for (User candidate : candidates) {
            int workload = getWorkload(candidate);
            if (workload < minWorkload) {
                minWorkload = workload;
                leastBusy = candidate;
            }
        }
        
        LOGGER.info("Selected least busy approver: " + leastBusy.getFullName() + 
                   " (workload: " + minWorkload + ")");
        
        // Increment workload
        incrementWorkload(leastBusy);
        
        return leastBusy;
    }
    
    /**
     * Select approver using round-robin load balancing.
     */
    private User selectBalancedApprover(List<User> candidates) {
        if (candidates.size() == 1) {
            incrementWorkload(candidates.get(0));
            return candidates.get(0);
        }
        
        // Sort by current workload (ascending)
        List<User> sorted = candidates.stream()
            .sorted(Comparator.comparingInt(this::getWorkload))
            .collect(Collectors.toList());
        
        User selected = sorted.get(0);
        incrementWorkload(selected);
        
        LOGGER.info("Load-balanced selection: " + selected.getFullName() + 
                   " (new workload: " + getWorkload(selected) + ")");
        
        return selected;
    }
    
    /**
     * Get current workload for an approver.
     */
    private int getWorkload(User user) {
        String userId = String.valueOf(user.getUserId());
        return approverWorkload.getOrDefault(userId, 0);
    }
    
    /**
     * Increment workload for an approver.
     */
    private void incrementWorkload(User user) {
        String userId = String.valueOf(user.getUserId());
        approverWorkload.put(userId, getWorkload(user) + 1);
    }
    
    /**
     * Decrement workload when a request is completed/rejected.
     */
    public void releaseWorkload(String userId) {
        int current = approverWorkload.getOrDefault(userId, 0);
        if (current > 0) {
            approverWorkload.put(userId, current - 1);
        }
    }
    
    /**
     * Set priority based on request characteristics.
     */
    public WorkRequest.RequestPriority determinePriority(WorkRequest request) {
        // High-value items are HIGH priority
        if (request instanceof com.campus.lostfound.models.workrequest.ItemClaimRequest) {
            com.campus.lostfound.models.workrequest.ItemClaimRequest claim = 
                (com.campus.lostfound.models.workrequest.ItemClaimRequest) request;
            
            if (claim.isHighValue() && claim.getItemValue() > 1000) {
                return WorkRequest.RequestPriority.URGENT; // Very high value
            } else if (claim.isHighValue()) {
                return WorkRequest.RequestPriority.HIGH; // High value
            }
        }
        
        // Police evidence requests are HIGH priority
        if (request instanceof com.campus.lostfound.models.workrequest.PoliceEvidenceRequest) {
            com.campus.lostfound.models.workrequest.PoliceEvidenceRequest evidence = 
                (com.campus.lostfound.models.workrequest.PoliceEvidenceRequest) request;
            
            if (evidence.isStolenCheck()) {
                return WorkRequest.RequestPriority.URGENT; // Stolen items
            }
            return WorkRequest.RequestPriority.HIGH; // Other police verification
        }
        
        // Airport items need quick processing (security protocols)
        if (request instanceof com.campus.lostfound.models.workrequest.AirportToUniversityTransferRequest) {
            com.campus.lostfound.models.workrequest.AirportToUniversityTransferRequest airport = 
                (com.campus.lostfound.models.workrequest.AirportToUniversityTransferRequest) request;
            
            if (airport.isWasInSecureArea()) {
                return WorkRequest.RequestPriority.HIGH; // Secure area items
            }
        }
        
        // Default priority
        return WorkRequest.RequestPriority.NORMAL;
    }
    
    /**
     * Check if any approvers are available for a role.
     */
    public boolean hasAvailableApprovers(String role, String organizationId) {
        List<User> candidates = findCandidateApprovers(role, organizationId);
        return !candidates.isEmpty();
    }
    
    /**
     * Get all overdue requests (for SLA monitoring).
     */
    public List<WorkRequest> getOverdueRequests(List<WorkRequest> allRequests) {
        return allRequests.stream()
            .filter(WorkRequest::isOverdue)
            .sorted(Comparator.comparing(WorkRequest::getCreatedAt))
            .collect(Collectors.toList());
    }
    
    /**
     * Get requests approaching SLA breach (within 20% of limit).
     */
    public List<WorkRequest> getApproachingSlaRequests(List<WorkRequest> allRequests) {
        return allRequests.stream()
            .filter(r -> r.isPending() || r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
            .filter(r -> {
                long remaining = r.getHoursUntilSla();
                long total = r.getSlaTargetHours();
                double percentRemaining = (double) remaining / total;
                return percentRemaining < 0.2 && percentRemaining > 0; // Less than 20% time left
            })
            .sorted(Comparator.comparingLong(WorkRequest::getHoursUntilSla))
            .collect(Collectors.toList());
    }
    
    /**
     * Get routing recommendation for a request.
     */
    public RoutingRecommendation getRoutingRecommendation(WorkRequest request) {
        String nextRole = request.getNextRequiredRole();
        if (nextRole == null) {
            return new RoutingRecommendation(false, "Request fully approved", null);
        }
        
        List<User> candidates = findCandidateApprovers(nextRole, request.getTargetOrganizationId());
        
        if (candidates.isEmpty()) {
            return new RoutingRecommendation(false, 
                "No approvers available for role: " + nextRole, null);
        }
        
        User recommended = findBestApprover(nextRole, 
            request.getTargetOrganizationId(), request.getPriority());
        
        String reason = String.format("Best match: %s (workload: %d, role: %s)",
            recommended.getFullName(), getWorkload(recommended), nextRole);
        
        return new RoutingRecommendation(true, reason, recommended);
    }
    
    /**
     * Get workload statistics for monitoring.
     */
    public Map<String, Integer> getWorkloadStatistics() {
        return new HashMap<>(approverWorkload);
    }
    
    /**
     * Reset workload tracking (for testing or periodic reset).
     */
    public void resetWorkloadTracking() {
        approverWorkload.clear();
        LOGGER.info("Workload tracking reset");
    }
    
    // ==================== INNER CLASS ====================
    
    /**
     * Routing recommendation result.
     */
    public static class RoutingRecommendation {
        private final boolean canRoute;
        private final String reason;
        private final User recommendedApprover;
        
        public RoutingRecommendation(boolean canRoute, String reason, User recommendedApprover) {
            this.canRoute = canRoute;
            this.reason = reason;
            this.recommendedApprover = recommendedApprover;
        }
        
        public boolean canRoute() { return canRoute; }
        public String getReason() { return reason; }
        public User getRecommendedApprover() { return recommendedApprover; }
        
        @Override
        public String toString() {
            return String.format("RoutingRecommendation{canRoute=%s, reason='%s', approver=%s}",
                canRoute, reason, recommendedApprover != null ? recommendedApprover.getFullName() : "none");
        }
    }
}
