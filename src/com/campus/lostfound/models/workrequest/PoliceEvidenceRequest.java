package com.campus.lostfound.models.workrequest;

import java.util.Arrays;
import java.util.List;

/**
 * Work request for police evidence verification of found items.
 * Approval chain: Any Coordinator → Police Evidence Custodian → Serial Number Check
 * 
 * This handles verification requests for potentially stolen items, high-value items,
 * or items that need to be checked against police databases.
 */
public class PoliceEvidenceRequest extends WorkRequest {
    
    // Item details
    private String itemId;
    private String itemName;
    private String itemCategory;
    private String itemDescription;
    private double estimatedValue;
    
    // Serial/identification numbers
    private String serialNumber;           // Electronics serial number
    private String modelNumber;            // Device model
    private String brandName;              // Brand/manufacturer
    private String imeiNumber;             // For phones
    private String otherIdentifiers;       // Other unique IDs
    
    // Source enterprise context
    private String sourceEnterpriseName;   // Where item was found
    private String sourceOrganizationName;
    private String coordinatorId;          // Who submitted the request
    private String coordinatorName;
    private String foundLocationId;
    private String foundLocationName;
    
    // Police context
    private String policeOfficerId;
    private String policeOfficerName;
    private String policeDepartment;
    private String caseNumber;             // Police case number if created
    
    // Verification details
    private String verificationReason;     // Why checking with police
    private boolean isStolenCheck;         // Check if reported stolen
    private boolean isHighValueVerification; // High-value item verification
    private boolean requiresSerialCheck;   // Check serial in database
    
    // Verification results
    private String verificationStatus;     // "Pending", "Clear", "Flagged", "Stolen"
    private String verificationNotes;      // Police notes about verification
    private boolean matchesStoredReport;   // Found in stolen items database?
    private String stolenReportId;         // If matches a report
    
    // Submission details
    private String submittedDateTime;
    private String urgencyLevel;           // "Standard", "High", "Urgent"
    private String evidencePhotoUrl;       // Photo of serial number/item
    
    // Constructor
    public PoliceEvidenceRequest() {
        super();
        this.requestType = RequestType.POLICE_EVIDENCE_REQUEST;
        this.verificationStatus = "Pending";
        this.urgencyLevel = "Standard";
        this.requiresSerialCheck = true;
    }
    
    // Convenience constructor
    public PoliceEvidenceRequest(String coordinatorId, String coordinatorName,
                                 String itemId, String itemName,
                                 String verificationReason) {
        this();
        this.coordinatorId = coordinatorId;
        this.coordinatorName = coordinatorName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.verificationReason = verificationReason;
        this.requesterId = coordinatorId;
        this.requesterName = coordinatorName;
        this.description = String.format("Police verification request for %s - %s", 
                                        itemName, verificationReason);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // Coordinator submits, Police custodian reviews and verifies
        return Arrays.asList(
            "CAMPUS_COORDINATOR",          // Or any coordinator submits
            "POLICE_EVIDENCE_CUSTODIAN"    // Police verifies and checks databases
        );
    }
    
    @Override
    public String getRequestSummary() {
        String urgencyTag = "Urgent".equals(urgencyLevel) ? " [URGENT]" : "";
        return String.format("Police Verification: %s from %s - %s%s", 
                            itemName, sourceEnterpriseName, verificationReason, urgencyTag);
    }
    
    @Override
    public boolean isValid() {
        // Must have essential fields for police verification
        if (itemId == null || itemId.isEmpty()) return false;
        if (coordinatorId == null || coordinatorId.isEmpty()) return false;
        if (verificationReason == null || verificationReason.isEmpty()) return false;
        
        // High-value items must have serial number
        if (isHighValueVerification && estimatedValue > 500.0) {
            if (serialNumber == null || serialNumber.isEmpty()) {
                return false;
            }
        }
        
        // Stolen checks require at least one identifier
        if (isStolenCheck) {
            if ((serialNumber == null || serialNumber.isEmpty()) &&
                (imeiNumber == null || imeiNumber.isEmpty()) &&
                (otherIdentifiers == null || otherIdentifiers.isEmpty())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Mark item as flagged (potential stolen item)
     */
    public void flagAsStolen(String reportId, String notes) {
        this.verificationStatus = "Stolen";
        this.matchesStoredReport = true;
        this.stolenReportId = reportId;
        this.verificationNotes = notes;
        this.urgencyLevel = "Urgent";
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Mark item as clear (not stolen, verified legitimate)
     */
    public void markAsClear(String notes) {
        this.verificationStatus = "Clear";
        this.matchesStoredReport = false;
        this.verificationNotes = notes;
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Mark item as flagged for investigation (suspicious but not confirmed)
     */
    public void flagForInvestigation(String reason) {
        this.verificationStatus = "Flagged";
        this.verificationNotes = "FLAGGED FOR INVESTIGATION: " + reason;
        this.urgencyLevel = "High";
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Check if verification is complete
     */
    public boolean isVerificationComplete() {
        return "Clear".equals(verificationStatus) || 
               "Stolen".equals(verificationStatus);
    }
    
    /**
     * Check if item requires immediate police action
     */
    public boolean requiresImmediateAction() {
        return "Stolen".equals(verificationStatus) || 
               "Flagged".equals(verificationStatus) ||
               "Urgent".equals(urgencyLevel);
    }
    
    /**
     * Generate police case number if needed
     */
    public String generateCaseNumber() {
        if (caseNumber != null) return caseNumber;
        if (requestId == null) return null;
        
        this.caseNumber = "BPD-EVD-" + java.time.LocalDateTime.now().getYear() + 
                         "-" + requestId.substring(Math.max(0, requestId.length() - 6));
        return this.caseNumber;
    }
    
    // Getters and Setters
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemCategory() {
        return itemCategory;
    }
    
    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public double getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getModelNumber() {
        return modelNumber;
    }
    
    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }
    
    public String getBrandName() {
        return brandName;
    }
    
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
    
    public String getImeiNumber() {
        return imeiNumber;
    }
    
    public void setImeiNumber(String imeiNumber) {
        this.imeiNumber = imeiNumber;
    }
    
    public String getOtherIdentifiers() {
        return otherIdentifiers;
    }
    
    public void setOtherIdentifiers(String otherIdentifiers) {
        this.otherIdentifiers = otherIdentifiers;
    }
    
    public String getSourceEnterpriseName() {
        return sourceEnterpriseName;
    }
    
    public void setSourceEnterpriseName(String sourceEnterpriseName) {
        this.sourceEnterpriseName = sourceEnterpriseName;
    }
    
    public String getSourceOrganizationName() {
        return sourceOrganizationName;
    }
    
    public void setSourceOrganizationName(String sourceOrganizationName) {
        this.sourceOrganizationName = sourceOrganizationName;
    }
    
    public String getCoordinatorId() {
        return coordinatorId;
    }
    
    public void setCoordinatorId(String coordinatorId) {
        this.coordinatorId = coordinatorId;
    }
    
    public String getCoordinatorName() {
        return coordinatorName;
    }
    
    public void setCoordinatorName(String coordinatorName) {
        this.coordinatorName = coordinatorName;
    }
    
    public String getFoundLocationId() {
        return foundLocationId;
    }
    
    public void setFoundLocationId(String foundLocationId) {
        this.foundLocationId = foundLocationId;
    }
    
    public String getFoundLocationName() {
        return foundLocationName;
    }
    
    public void setFoundLocationName(String foundLocationName) {
        this.foundLocationName = foundLocationName;
    }
    
    public String getPoliceOfficerId() {
        return policeOfficerId;
    }
    
    public void setPoliceOfficerId(String policeOfficerId) {
        this.policeOfficerId = policeOfficerId;
    }
    
    public String getPoliceOfficerName() {
        return policeOfficerName;
    }
    
    public void setPoliceOfficerName(String policeOfficerName) {
        this.policeOfficerName = policeOfficerName;
    }
    
    public String getPoliceDepartment() {
        return policeDepartment;
    }
    
    public void setPoliceDepartment(String policeDepartment) {
        this.policeDepartment = policeDepartment;
    }
    
    public String getCaseNumber() {
        return caseNumber;
    }
    
    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }
    
    public String getVerificationReason() {
        return verificationReason;
    }
    
    public void setVerificationReason(String verificationReason) {
        this.verificationReason = verificationReason;
    }
    
    public boolean isStolenCheck() {
        return isStolenCheck;
    }
    
    public void setStolenCheck(boolean stolenCheck) {
        isStolenCheck = stolenCheck;
    }
    
    public boolean isHighValueVerification() {
        return isHighValueVerification;
    }
    
    public void setHighValueVerification(boolean highValueVerification) {
        isHighValueVerification = highValueVerification;
    }
    
    public boolean isRequiresSerialCheck() {
        return requiresSerialCheck;
    }
    
    public void setRequiresSerialCheck(boolean requiresSerialCheck) {
        this.requiresSerialCheck = requiresSerialCheck;
    }
    
    public String getVerificationStatus() {
        return verificationStatus;
    }
    
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    
    public String getVerificationNotes() {
        return verificationNotes;
    }
    
    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }
    
    public boolean isMatchesStoredReport() {
        return matchesStoredReport;
    }
    
    public void setMatchesStoredReport(boolean matchesStoredReport) {
        this.matchesStoredReport = matchesStoredReport;
    }
    
    public String getStolenReportId() {
        return stolenReportId;
    }
    
    public void setStolenReportId(String stolenReportId) {
        this.stolenReportId = stolenReportId;
    }
    
    public String getSubmittedDateTime() {
        return submittedDateTime;
    }
    
    public void setSubmittedDateTime(String submittedDateTime) {
        this.submittedDateTime = submittedDateTime;
    }
    
    public String getUrgencyLevel() {
        return urgencyLevel;
    }
    
    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }
    
    public String getEvidencePhotoUrl() {
        return evidencePhotoUrl;
    }
    
    public void setEvidencePhotoUrl(String evidencePhotoUrl) {
        this.evidencePhotoUrl = evidencePhotoUrl;
    }
    
    @Override
    public String toString() {
        return String.format("PoliceEvidenceRequest{id=%s, item=%s, serial=%s, status=%s, verification=%s, urgency=%s}",
                requestId, itemName, serialNumber, status, verificationStatus, urgencyLevel);
    }
}
