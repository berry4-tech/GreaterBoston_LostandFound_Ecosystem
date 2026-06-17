package com.campus.lostfound.models.workrequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all work requests in the Lost & Found ecosystem.
 * Work requests represent formal workflows that require approval across organizations/enterprises.
 */
public abstract class WorkRequest {
    
    // Enums
    public enum RequestStatus {
        PENDING,           // Just created, waiting for first approval
        IN_PROGRESS,       // Approved by at least one person, needs more approvals
        APPROVED,          // Fully approved, ready for action
        REJECTED,          // Rejected by an approver
        COMPLETED,         // Action completed successfully
        CANCELLED          // Cancelled by requester
    }
    
    public enum RequestPriority {
        URGENT,    // Requires immediate attention (< 4 hours)
        HIGH,      // Important (< 24 hours)
        NORMAL,    // Standard processing (< 72 hours)
        LOW        // No rush (< 1 week)
    }
    
    public enum RequestType {
        ITEM_CLAIM,                      // Student claiming an item
        CROSS_CAMPUS_TRANSFER,           // Transfer between university campuses
        TRANSIT_TO_UNIVERSITY_TRANSFER,  // MBTA to University transfer
        AIRPORT_TO_UNIVERSITY_TRANSFER,  // Airport to University transfer
        POLICE_EVIDENCE_REQUEST,         // Police evidence verification
        MBTA_TO_AIRPORT_EMERGENCY,       // Emergency transfer from MBTA to Airport
        MULTI_ENTERPRISE_DISPUTE         // Dispute resolution across multiple enterprises
    }
    
    // Core fields
    protected String requestId;
    protected RequestType requestType;
    protected RequestStatus status;
    protected RequestPriority priority;
    
    // User and organization context
    protected String requesterId;           // User who created the request
    protected String requesterEmail;        // Email for trust score lookups
    protected String requesterName;         // For display purposes
    protected String requesterEnterpriseId;
    protected String requesterOrganizationId;
    
    // Target context (where request is being sent to)
    protected String targetEnterpriseId;
    protected String targetOrganizationId;
    
    // Approval tracking
    protected List<String> approverIds;     // Users who have approved (in order)
    protected List<String> approverNames;   // For display
    protected String currentApproverId;     // Who needs to approve next
    protected int approvalStep;             // Which step in chain (0-indexed)
    
    // Request details
    protected String description;
    protected String notes;                 // Additional notes/comments
    
    // Timestamps
    protected LocalDateTime createdAt;
    protected LocalDateTime lastUpdatedAt;
    protected LocalDateTime completedAt;
    
    // Constructor
    public WorkRequest() {
        this.requestId = null;  // Will be set by MongoDB
        this.status = RequestStatus.PENDING;
        this.priority = RequestPriority.NORMAL; // Default priority
        this.approverIds = new ArrayList<>();
        this.approverNames = new ArrayList<>();
        this.approvalStep = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    // Abstract methods - must be implemented by subclasses
    
    /**
     * Returns the approval chain for this request type.
     * Each entry is a role that must approve, in order.
     * Example: ["CAMPUS_COORDINATOR", "POLICE_EVIDENCE_CUSTODIAN"]
     */
    public abstract List<String> getApprovalChain();
    
    /**
     * Returns a human-readable description of what this request is for.
     * Example: "Item Claim Request for Blue Backpack"
     */
    public abstract String getRequestSummary();
    
    /**
     * Validates that the request has all required information.
     * Returns true if valid, false otherwise.
     */
    public abstract boolean isValid();
    
    // Business logic methods
    
    /**
     * Move to next approval step
     */
    public void advanceApproval(String approverId, String approverName) {
        this.approverIds.add(approverId);
        this.approverNames.add(approverName);
        this.approvalStep++;
        this.lastUpdatedAt = LocalDateTime.now();
        
        List<String> chain = getApprovalChain();
        if (approvalStep >= chain.size()) {
            // All approvals received
            this.status = RequestStatus.APPROVED;
        } else {
            this.status = RequestStatus.IN_PROGRESS;
        }
    }
    
    /**
     * Reject the request
     */
    public void reject(String reason) {
        this.status = RequestStatus.REJECTED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "REJECTED: " + reason;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark request as completed
     */
    public void complete() {
        this.status = RequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel the request
     */
    public void cancel() {
        this.status = RequestStatus.CANCELLED;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this request needs approval from a specific role
     */
    public boolean needsApprovalFromRole(String role) {
        List<String> chain = getApprovalChain();
        if (approvalStep >= chain.size()) {
            return false; // Already fully approved
        }
        return chain.get(approvalStep).equals(role);
    }
    
    /**
     * Get the role that needs to approve next
     */
    public String getNextRequiredRole() {
        List<String> chain = getApprovalChain();
        if (approvalStep >= chain.size()) {
            return null; // Fully approved
        }
        return chain.get(approvalStep);
    }
    
    /**
     * Check if request is awaiting any approvals
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING || status == RequestStatus.IN_PROGRESS;
    }
    
    // Getters and Setters
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public RequestType getRequestType() {
        return requestType;
    }
    
    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
    
    public RequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(RequestStatus status) {
        this.status = status;
    }
    
    public RequestPriority getPriority() {
        return priority;
    }
    
    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }
    
    public String getRequesterId() {
        return requesterId;
    }
    
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
    
    public String getRequesterEmail() {
        return requesterEmail;
    }
    
    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }
    
    public String getRequesterEnterpriseId() {
        return requesterEnterpriseId;
    }
    
    public void setRequesterEnterpriseId(String requesterEnterpriseId) {
        this.requesterEnterpriseId = requesterEnterpriseId;
    }
    
    public String getRequesterOrganizationId() {
        return requesterOrganizationId;
    }
    
    public void setRequesterOrganizationId(String requesterOrganizationId) {
        this.requesterOrganizationId = requesterOrganizationId;
    }
    
    public String getTargetEnterpriseId() {
        return targetEnterpriseId;
    }
    
    public void setTargetEnterpriseId(String targetEnterpriseId) {
        this.targetEnterpriseId = targetEnterpriseId;
    }
    
    public String getTargetOrganizationId() {
        return targetOrganizationId;
    }
    
    public void setTargetOrganizationId(String targetOrganizationId) {
        this.targetOrganizationId = targetOrganizationId;
    }
    
    public List<String> getApproverIds() {
        return approverIds;
    }
    
    public void setApproverIds(List<String> approverIds) {
        this.approverIds = approverIds;
    }
    
    public List<String> getApproverNames() {
        return approverNames;
    }
    
    public void setApproverNames(List<String> approverNames) {
        this.approverNames = approverNames;
    }
    
    public String getCurrentApproverId() {
        return currentApproverId;
    }
    
    public void setCurrentApproverId(String currentApproverId) {
        this.currentApproverId = currentApproverId;
    }
    
    public int getApprovalStep() {
        return approvalStep;
    }
    
    public void setApprovalStep(int approvalStep) {
        this.approvalStep = approvalStep;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Get SLA target time in hours based on priority
     */
    public long getSlaTargetHours() {
        switch (priority) {
            case URGENT: return 4;
            case HIGH: return 24;
            case NORMAL: return 72;
            case LOW: return 168; // 1 week
            default: return 72;
        }
    }
    
    /**
     * Check if request is overdue based on SLA
     */
    public boolean isOverdue() {
        if (status != RequestStatus.PENDING && status != RequestStatus.IN_PROGRESS) {
            return false; // Completed/rejected requests aren't overdue
        }
        
        long hoursSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
        return hoursSinceCreation > getSlaTargetHours();
    }
    
    /**
     * Get hours remaining until SLA breach (negative if overdue)
     */
    public long getHoursUntilSla() {
        long hoursSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
        return getSlaTargetHours() - hoursSinceCreation;
    }
    
    @Override
    public String toString() {
        return String.format("WorkRequest{id=%s, type=%s, status=%s, priority=%s, requester=%s, step=%d/%d}",
                requestId, requestType, status, priority, requesterName, approvalStep, getApprovalChain().size());
    }
}
