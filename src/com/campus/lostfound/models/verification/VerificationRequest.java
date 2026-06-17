package com.campus.lostfound.models.verification;

import java.time.LocalDateTime;

/**
 * Represents a verification request in the Lost & Found ecosystem.
 * 
 * Verification requests are created when:
 * - High-value items need ownership verification
 * - Cross-enterprise transfers require multi-party approval
 * - Users need identity verification
 * - Items need serial number or stolen property checks
 * - Students need enrollment verification
 * 
 * The verification workflow:
 * 1. Request created (PENDING)
 * 2. Assigned to verifier (IN_PROGRESS)
 * 3. Verification completed (VERIFIED/FAILED)
 * 4. Or request expires (EXPIRED)
 */
public class VerificationRequest {
    
    // ==================== ENUMS ====================
    
    /**
     * Types of verification that can be requested
     */
    public enum VerificationType {
        IDENTITY_VERIFICATION("Identity Verification", "Verify user's identity via government ID"),
        HIGH_VALUE_ITEM_CLAIM("High-Value Item Claim", "Verify ownership of item valued over $500"),
        CROSS_ENTERPRISE_TRANSFER("Cross-Enterprise Transfer", "Verify transfer between organizations"),
        SERIAL_NUMBER_CHECK("Serial Number Check", "Check item serial number against databases"),
        STOLEN_PROPERTY_CHECK("Stolen Property Check", "Check if item is reported stolen"),
        STUDENT_ENROLLMENT("Student Enrollment", "Verify student is enrolled at university"),
        POLICE_BACKGROUND_CHECK("Police Background Check", "Background check via law enforcement"),
        OWNERSHIP_DOCUMENTATION("Ownership Documentation", "Verify proof of ownership documents"),
        MULTI_PARTY_APPROVAL("Multi-Party Approval", "Requires approval from multiple parties");
        
        private final String displayName;
        private final String description;
        
        VerificationType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Get default expiration hours for this verification type
         */
        public int getDefaultExpirationHours() {
            switch (this) {
                case IDENTITY_VERIFICATION:
                    return 24;      // 1 day
                case HIGH_VALUE_ITEM_CLAIM:
                    return 72;      // 3 days
                case CROSS_ENTERPRISE_TRANSFER:
                    return 168;     // 1 week
                case SERIAL_NUMBER_CHECK:
                    return 48;      // 2 days
                case STOLEN_PROPERTY_CHECK:
                    return 48;      // 2 days
                case STUDENT_ENROLLMENT:
                    return 24;      // 1 day
                case POLICE_BACKGROUND_CHECK:
                    return 168;     // 1 week
                case OWNERSHIP_DOCUMENTATION:
                    return 72;      // 3 days
                case MULTI_PARTY_APPROVAL:
                    return 120;     // 5 days
                default:
                    return 72;
            }
        }
        
        /**
         * Check if this type requires police involvement
         */
        public boolean requiresPolice() {
            return this == STOLEN_PROPERTY_CHECK || 
                   this == POLICE_BACKGROUND_CHECK ||
                   this == SERIAL_NUMBER_CHECK;
        }
    }
    
    /**
     * Status of the verification request
     */
    public enum VerificationStatus {
        PENDING("Pending", "Awaiting assignment to a verifier"),
        IN_PROGRESS("In Progress", "Being processed by a verifier"),
        AWAITING_DOCUMENTS("Awaiting Documents", "Waiting for additional documentation"),
        AWAITING_RESPONSE("Awaiting Response", "Waiting for external response (e.g., police)"),
        VERIFIED("Verified", "Verification completed successfully"),
        FAILED("Failed", "Verification failed"),
        EXPIRED("Expired", "Request expired before completion"),
        CANCELLED("Cancelled", "Request was cancelled");
        
        private final String displayName;
        private final String description;
        
        VerificationStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Check if this is a terminal status (no more changes expected)
         */
        public boolean isTerminal() {
            return this == VERIFIED || this == FAILED || this == EXPIRED || this == CANCELLED;
        }
        
        /**
         * Check if this status represents a successful outcome
         */
        public boolean isSuccessful() {
            return this == VERIFIED;
        }
    }
    
    /**
     * Priority of the verification request
     */
    public enum VerificationPriority {
        URGENT(4, "Urgent", "Requires immediate attention (< 4 hours)", 4),
        HIGH(3, "High", "Important verification (< 24 hours)", 24),
        NORMAL(2, "Normal", "Standard processing (< 72 hours)", 72),
        LOW(1, "Low", "No rush (< 1 week)", 168);
        
        private final int level;
        private final String displayName;
        private final String description;
        private final int targetHours;
        
        VerificationPriority(int level, String displayName, String description, int targetHours) {
            this.level = level;
            this.displayName = displayName;
            this.description = description;
            this.targetHours = targetHours;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getTargetHours() {
            return targetHours;
        }
    }
    
    // ==================== FIELDS ====================
    
    // Identifiers
    private String requestId;           // MongoDB ObjectId as string
    private String visibleId;           // Human-readable ID (e.g., "VR-001234")
    
    // Request type and status
    private VerificationType verificationType;
    private VerificationStatus status;
    private VerificationPriority priority;
    
    // Subject of verification (what/who is being verified)
    private String subjectUserId;       // User being verified (if applicable)
    private String subjectUserName;
    private String subjectUserEmail;
    private String subjectItemId;       // Item being verified (if applicable)
    private String subjectItemName;
    private double subjectItemValue;    // For high-value threshold checks
    
    // Requester information (who requested verification)
    private String requesterId;
    private String requesterName;
    private String requesterEmail;
    private String requesterOrganizationId;
    private String requesterEnterpriseName;
    
    // Verifier information (who performs verification)
    private String verifierId;
    private String verifierName;
    private String verifierRole;
    private String verifierOrganizationId;
    
    // Related entities
    private String relatedWorkRequestId;    // If triggered by a work request
    private String relatedClaimId;          // If triggered by a claim
    
    // Verification details
    private String verificationNotes;       // Notes from verifier
    private String requestReason;           // Why verification was requested
    private String failureReason;           // Why verification failed (if applicable)
    
    // External verification results
    private String policeCheckResult;           // Result from police database check
    private String policeCheckCaseNumber;       // Police case number if applicable
    private String serialNumberCheckResult;     // Result from serial number check
    private String stolenPropertyCheckResult;   // Result from stolen property database
    private boolean isReportedStolen;           // Flag if item found in stolen database
    
    // Document verification
    private String documentType;            // Type of document provided (ID, receipt, etc.)
    private String documentVerificationResult;
    private boolean documentsVerified;
    
    // Multi-party approval tracking
    private int requiredApprovals;          // Number of approvals needed
    private int currentApprovals;           // Number received so far
    private String approverIds;             // Comma-separated list of approver IDs
    private String approverNames;           // Comma-separated list of approver names
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime assignedAt;       // When verifier was assigned
    private LocalDateTime completedAt;      // When verification completed
    private LocalDateTime expiresAt;        // When request expires
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Default constructor
     */
    public VerificationRequest() {
        this.status = VerificationStatus.PENDING;
        this.priority = VerificationPriority.NORMAL;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requiredApprovals = 1;
        this.currentApprovals = 0;
        this.isReportedStolen = false;
        this.documentsVerified = false;
    }
    
    /**
     * Constructor with type
     */
    public VerificationRequest(VerificationType type) {
        this();
        this.verificationType = type;
        // Set expiration based on type
        this.expiresAt = LocalDateTime.now().plusHours(type.getDefaultExpirationHours());
    }
    
    /**
     * Constructor for user verification
     */
    public VerificationRequest(VerificationType type, String subjectUserId, String requesterId) {
        this(type);
        this.subjectUserId = subjectUserId;
        this.requesterId = requesterId;
    }
    
    /**
     * Constructor for item verification
     */
    public VerificationRequest(VerificationType type, String subjectUserId, 
                                String subjectItemId, String requesterId) {
        this(type, subjectUserId, requesterId);
        this.subjectItemId = subjectItemId;
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Generate human-readable visible ID
     */
    public void generateVisibleId(int sequenceNumber) {
        this.visibleId = String.format("VR-%06d", sequenceNumber);
    }
    
    /**
     * Check if request is expired
     */
    public boolean isExpired() {
        if (expiresAt == null) return false;
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if request can still be processed
     */
    public boolean canProcess() {
        return !status.isTerminal() && !isExpired();
    }
    
    /**
     * Check if request is overdue based on priority SLA
     */
    public boolean isOverdue() {
        if (status.isTerminal()) return false;
        LocalDateTime dueTime = createdAt.plusHours(priority.getTargetHours());
        return LocalDateTime.now().isAfter(dueTime);
    }
    
    /**
     * Get hours remaining until SLA deadline
     */
    public long getHoursUntilDue() {
        LocalDateTime dueTime = createdAt.plusHours(priority.getTargetHours());
        return java.time.Duration.between(LocalDateTime.now(), dueTime).toHours();
    }
    
    /**
     * Assign to a verifier
     */
    public void assignTo(String verifierId, String verifierName, String verifierRole) {
        this.verifierId = verifierId;
        this.verifierName = verifierName;
        this.verifierRole = verifierRole;
        this.status = VerificationStatus.IN_PROGRESS;
        this.assignedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as awaiting documents
     */
    public void awaitDocuments(String notes) {
        this.status = VerificationStatus.AWAITING_DOCUMENTS;
        this.verificationNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as awaiting external response
     */
    public void awaitResponse(String notes) {
        this.status = VerificationStatus.AWAITING_RESPONSE;
        this.verificationNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Complete verification successfully
     */
    public void complete(String notes) {
        this.status = VerificationStatus.VERIFIED;
        this.verificationNotes = notes;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Fail verification
     */
    public void fail(String reason) {
        this.status = VerificationStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel verification
     */
    public void cancel(String reason) {
        this.status = VerificationStatus.CANCELLED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as expired
     */
    public void expire() {
        this.status = VerificationStatus.EXPIRED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record approval (for multi-party)
     */
    public boolean recordApproval(String approverId, String approverName) {
        this.currentApprovals++;
        
        if (this.approverIds == null || this.approverIds.isEmpty()) {
            this.approverIds = approverId;
            this.approverNames = approverName;
        } else {
            this.approverIds += "," + approverId;
            this.approverNames += "," + approverName;
        }
        
        this.updatedAt = LocalDateTime.now();
        
        // Check if all approvals received
        if (this.currentApprovals >= this.requiredApprovals) {
            complete("All required approvals received");
            return true;
        }
        return false;
    }
    
    /**
     * Record police check result
     */
    public void recordPoliceCheck(String result, String caseNumber, boolean isStolen) {
        this.policeCheckResult = result;
        this.policeCheckCaseNumber = caseNumber;
        this.isReportedStolen = isStolen;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record serial number check result
     */
    public void recordSerialCheck(String result, boolean isStolen) {
        this.serialNumberCheckResult = result;
        this.isReportedStolen = isStolen;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get a summary string for display
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(verificationType.getDisplayName());
        
        if (subjectUserName != null) {
            sb.append(" for ").append(subjectUserName);
        }
        if (subjectItemName != null) {
            sb.append(" - ").append(subjectItemName);
        }
        
        return sb.toString();
    }
    
    /**
     * Get status with color for UI
     */
    public String getStatusColorHex() {
        switch (status) {
            case PENDING:
                return "#F59E0B";      // Amber
            case IN_PROGRESS:
                return "#3B82F6";      // Blue
            case AWAITING_DOCUMENTS:
            case AWAITING_RESPONSE:
                return "#8B5CF6";      // Purple
            case VERIFIED:
                return "#22C55E";      // Green
            case FAILED:
                return "#EF4444";      // Red
            case EXPIRED:
            case CANCELLED:
                return "#6B7280";      // Gray
            default:
                return "#6B7280";
        }
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getVisibleId() {
        return visibleId;
    }
    
    public void setVisibleId(String visibleId) {
        this.visibleId = visibleId;
    }
    
    public VerificationType getVerificationType() {
        return verificationType;
    }
    
    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }
    
    public VerificationStatus getStatus() {
        return status;
    }
    
    public void setStatus(VerificationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public VerificationPriority getPriority() {
        return priority;
    }
    
    public void setPriority(VerificationPriority priority) {
        this.priority = priority;
    }
    
    public String getSubjectUserId() {
        return subjectUserId;
    }
    
    public void setSubjectUserId(String subjectUserId) {
        this.subjectUserId = subjectUserId;
    }
    
    public String getSubjectUserName() {
        return subjectUserName;
    }
    
    public void setSubjectUserName(String subjectUserName) {
        this.subjectUserName = subjectUserName;
    }
    
    public String getSubjectUserEmail() {
        return subjectUserEmail;
    }
    
    public void setSubjectUserEmail(String subjectUserEmail) {
        this.subjectUserEmail = subjectUserEmail;
    }
    
    public String getSubjectItemId() {
        return subjectItemId;
    }
    
    public void setSubjectItemId(String subjectItemId) {
        this.subjectItemId = subjectItemId;
    }
    
    public String getSubjectItemName() {
        return subjectItemName;
    }
    
    public void setSubjectItemName(String subjectItemName) {
        this.subjectItemName = subjectItemName;
    }
    
    public double getSubjectItemValue() {
        return subjectItemValue;
    }
    
    public void setSubjectItemValue(double subjectItemValue) {
        this.subjectItemValue = subjectItemValue;
    }
    
    public String getRequesterId() {
        return requesterId;
    }
    
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }
    
    public String getRequesterEmail() {
        return requesterEmail;
    }
    
    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }
    
    public String getRequesterOrganizationId() {
        return requesterOrganizationId;
    }
    
    public void setRequesterOrganizationId(String requesterOrganizationId) {
        this.requesterOrganizationId = requesterOrganizationId;
    }
    
    public String getRequesterEnterpriseName() {
        return requesterEnterpriseName;
    }
    
    public void setRequesterEnterpriseName(String requesterEnterpriseName) {
        this.requesterEnterpriseName = requesterEnterpriseName;
    }
    
    public String getVerifierId() {
        return verifierId;
    }
    
    public void setVerifierId(String verifierId) {
        this.verifierId = verifierId;
    }
    
    public String getVerifierName() {
        return verifierName;
    }
    
    public void setVerifierName(String verifierName) {
        this.verifierName = verifierName;
    }
    
    public String getVerifierRole() {
        return verifierRole;
    }
    
    public void setVerifierRole(String verifierRole) {
        this.verifierRole = verifierRole;
    }
    
    public String getVerifierOrganizationId() {
        return verifierOrganizationId;
    }
    
    public void setVerifierOrganizationId(String verifierOrganizationId) {
        this.verifierOrganizationId = verifierOrganizationId;
    }
    
    public String getRelatedWorkRequestId() {
        return relatedWorkRequestId;
    }
    
    public void setRelatedWorkRequestId(String relatedWorkRequestId) {
        this.relatedWorkRequestId = relatedWorkRequestId;
    }
    
    public String getRelatedClaimId() {
        return relatedClaimId;
    }
    
    public void setRelatedClaimId(String relatedClaimId) {
        this.relatedClaimId = relatedClaimId;
    }
    
    public String getVerificationNotes() {
        return verificationNotes;
    }
    
    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }
    
    public String getRequestReason() {
        return requestReason;
    }
    
    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public String getPoliceCheckResult() {
        return policeCheckResult;
    }
    
    public void setPoliceCheckResult(String policeCheckResult) {
        this.policeCheckResult = policeCheckResult;
    }
    
    public String getPoliceCheckCaseNumber() {
        return policeCheckCaseNumber;
    }
    
    public void setPoliceCheckCaseNumber(String policeCheckCaseNumber) {
        this.policeCheckCaseNumber = policeCheckCaseNumber;
    }
    
    public String getSerialNumberCheckResult() {
        return serialNumberCheckResult;
    }
    
    public void setSerialNumberCheckResult(String serialNumberCheckResult) {
        this.serialNumberCheckResult = serialNumberCheckResult;
    }
    
    public String getStolenPropertyCheckResult() {
        return stolenPropertyCheckResult;
    }
    
    public void setStolenPropertyCheckResult(String stolenPropertyCheckResult) {
        this.stolenPropertyCheckResult = stolenPropertyCheckResult;
    }
    
    public boolean isReportedStolen() {
        return isReportedStolen;
    }
    
    public void setReportedStolen(boolean reportedStolen) {
        isReportedStolen = reportedStolen;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getDocumentVerificationResult() {
        return documentVerificationResult;
    }
    
    public void setDocumentVerificationResult(String documentVerificationResult) {
        this.documentVerificationResult = documentVerificationResult;
    }
    
    public boolean isDocumentsVerified() {
        return documentsVerified;
    }
    
    public void setDocumentsVerified(boolean documentsVerified) {
        this.documentsVerified = documentsVerified;
    }
    
    public int getRequiredApprovals() {
        return requiredApprovals;
    }
    
    public void setRequiredApprovals(int requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }
    
    public int getCurrentApprovals() {
        return currentApprovals;
    }
    
    public void setCurrentApprovals(int currentApprovals) {
        this.currentApprovals = currentApprovals;
    }
    
    public String getApproverIds() {
        return approverIds;
    }
    
    public void setApproverIds(String approverIds) {
        this.approverIds = approverIds;
    }
    
    public String getApproverNames() {
        return approverNames;
    }
    
    public void setApproverNames(String approverNames) {
        this.approverNames = approverNames;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    // ==================== OBJECT METHODS ====================
    
    @Override
    public String toString() {
        return "VerificationRequest{" +
                "requestId='" + requestId + '\'' +
                ", visibleId='" + visibleId + '\'' +
                ", verificationType=" + verificationType +
                ", status=" + status +
                ", priority=" + priority +
                ", subjectUserId='" + subjectUserId + '\'' +
                ", subjectItemId='" + subjectItemId + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerificationRequest that = (VerificationRequest) o;
        return requestId != null && requestId.equals(that.requestId);
    }
    
    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}
