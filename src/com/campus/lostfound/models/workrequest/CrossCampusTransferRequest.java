package com.campus.lostfound.models.workrequest;

import java.util.Arrays;
import java.util.List;

/**
 * Work request for transferring an item between university campuses (e.g., NEU to BU).
 * Approval chain: Source Campus Coordinator → Destination Campus Coordinator → Student Pickup
 * 
 * This handles items found at one university that belong to a student from another university.
 * Both campuses must coordinate the transfer.
 */
public class CrossCampusTransferRequest extends WorkRequest {
    
    // Item details
    private String itemId;
    private String lostItemId;  // ID of matched lost item for auto-closure
    private String itemName;
    private String itemCategory;
    
    // Source campus (where item was found)
    private String sourceCampusName;        // e.g., "Northeastern University"
    private String sourceCoordinatorId;
    private String sourceCoordinatorName;
    private String sourceLocationId;
    private String sourceLocationName;
    
    // Destination campus (where student is)
    private String destinationCampusName;   // e.g., "Boston University"
    private String destinationCoordinatorId;
    private String destinationCoordinatorName;
    
    // Student details
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    
    // Transfer logistics
    private String pickupLocation;          // Where student will pick up
    private String estimatedPickupDate;     // When transfer is expected
    private String transferMethod;          // "In-person", "Courier", "Student pickup"
    private String trackingNotes;           // Status updates during transfer
    
    // Constructor
    public CrossCampusTransferRequest() {
        super();
        this.requestType = RequestType.CROSS_CAMPUS_TRANSFER;
    }
    
    // Convenience constructor
    public CrossCampusTransferRequest(String sourceCoordinatorId, String sourceCoordinatorName,
                                     String itemId, String itemName,
                                     String studentId, String studentName) {
        this();
        this.sourceCoordinatorId = sourceCoordinatorId;
        this.sourceCoordinatorName = sourceCoordinatorName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.requesterId = sourceCoordinatorId;
        this.requesterName = sourceCoordinatorName;
        this.description = String.format("Cross-campus transfer of %s for student %s", 
                                        itemName, studentName);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // Source approver releases, destination approver receives, student confirms
        // Dynamically determine destination approver based on enterprise type
        String destinationApproverRole = getDestinationApproverRole();
        
        return Arrays.asList(
            "CAMPUS_COORDINATOR",      // Source campus approves release
            destinationApproverRole,   // Destination org approves receipt (role varies by enterprise)
            "STUDENT"                  // Student confirms pickup
        );
    }
    
    /**
     * Determine the appropriate approver role at the destination enterprise.
     * Different enterprise types have different coordinator roles.
     * These role names MUST match the actual UserRole enum values in User.java!
     */
    private String getDestinationApproverRole() {
        if (destinationCampusName == null) {
            return "CAMPUS_COORDINATOR"; // Default fallback
        }
        
        String destLower = destinationCampusName.toLowerCase();
        
        // MBTA / Transit - matches MBTA_STATION_MANAGER role
        if (destLower.contains("mbta") || destLower.contains("transit") || 
            destLower.contains("transportation") || destLower.contains("station")) {
            return "MBTA_STATION_MANAGER";
        }
        
        // Airport - matches AIRPORT_SPECIALIST role
        if (destLower.contains("airport") || destLower.contains("logan")) {
            return "AIRPORT_SPECIALIST";
        }
        
        // Police - matches POLICE_EVIDENCE_CUSTODIAN role
        if (destLower.contains("police") || destLower.contains("law enforcement") ||
            destLower.contains("nupd") || destLower.contains("bpd")) {
            return "POLICE_EVIDENCE_CUSTODIAN";
        }
        
        // Default: Higher Education / Campus
        return "CAMPUS_COORDINATOR";
    }
    
    @Override
    public String getRequestSummary() {
        return String.format("Cross-Campus Transfer: %s from %s to %s for student %s", 
                            itemName, sourceCampusName, destinationCampusName, studentName);
    }
    
    @Override
    public boolean isValid() {
        // Must have item, source, and pickup info
        if (itemId == null || itemId.isEmpty()) return false;
        if (sourceCoordinatorId == null || sourceCoordinatorId.isEmpty()) return false;
        // destinationCoordinatorId not required - routing engine finds appropriate approver
        // based on targetOrganizationId (works across different enterprise types)
        if (pickupLocation == null || pickupLocation.isEmpty()) return false;
        // studentId is optional, but studentName is required
        if (studentName == null || studentName.isEmpty()) return false;
        
        return true;
    }
    
    /**
     * Check if transfer is within the same enterprise (same university system)
     */
    public boolean isSameEnterprise() {
        return requesterEnterpriseId != null && 
               requesterEnterpriseId.equals(targetEnterpriseId);
    }
    
    /**
     * Add a tracking note about transfer status
     */
    public void addTrackingNote(String note) {
        if (trackingNotes == null) {
            trackingNotes = note;
        } else {
            trackingNotes += "\n" + note;
        }
        this.lastUpdatedAt = java.time.LocalDateTime.now();
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
    
    public String getSourceCampusName() {
        return sourceCampusName;
    }
    
    public void setSourceCampusName(String sourceCampusName) {
        this.sourceCampusName = sourceCampusName;
    }
    
    public String getSourceCoordinatorId() {
        return sourceCoordinatorId;
    }
    
    public void setSourceCoordinatorId(String sourceCoordinatorId) {
        this.sourceCoordinatorId = sourceCoordinatorId;
    }
    
    public String getSourceCoordinatorName() {
        return sourceCoordinatorName;
    }
    
    public void setSourceCoordinatorName(String sourceCoordinatorName) {
        this.sourceCoordinatorName = sourceCoordinatorName;
    }
    
    public String getSourceLocationId() {
        return sourceLocationId;
    }
    
    public void setSourceLocationId(String sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }
    
    public String getSourceLocationName() {
        return sourceLocationName;
    }
    
    public void setSourceLocationName(String sourceLocationName) {
        this.sourceLocationName = sourceLocationName;
    }
    
    public String getDestinationCampusName() {
        return destinationCampusName;
    }
    
    public void setDestinationCampusName(String destinationCampusName) {
        this.destinationCampusName = destinationCampusName;
    }
    
    public String getDestinationCoordinatorId() {
        return destinationCoordinatorId;
    }
    
    public void setDestinationCoordinatorId(String destinationCoordinatorId) {
        this.destinationCoordinatorId = destinationCoordinatorId;
    }
    
    public String getDestinationCoordinatorName() {
        return destinationCoordinatorName;
    }
    
    public void setDestinationCoordinatorName(String destinationCoordinatorName) {
        this.destinationCoordinatorName = destinationCoordinatorName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public String getStudentPhone() {
        return studentPhone;
    }
    
    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }
    
    public String getPickupLocation() {
        return pickupLocation;
    }
    
    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
    
    public String getEstimatedPickupDate() {
        return estimatedPickupDate;
    }
    
    public void setEstimatedPickupDate(String estimatedPickupDate) {
        this.estimatedPickupDate = estimatedPickupDate;
    }
    
    public String getTransferMethod() {
        return transferMethod;
    }
    
    public void setTransferMethod(String transferMethod) {
        this.transferMethod = transferMethod;
    }
    
    public String getTrackingNotes() {
        return trackingNotes;
    }
    
    public void setTrackingNotes(String trackingNotes) {
        this.trackingNotes = trackingNotes;
    }
    
    public String getLostItemId() {
        return lostItemId;
    }
    
    public void setLostItemId(String lostItemId) {
        this.lostItemId = lostItemId;
    }
    
    @Override
    public String toString() {
        return String.format("CrossCampusTransferRequest{id=%s, item=%s, from=%s, to=%s, student=%s, status=%s}",
                requestId, itemName, sourceCampusName, destinationCampusName, studentName, status);
    }
}
