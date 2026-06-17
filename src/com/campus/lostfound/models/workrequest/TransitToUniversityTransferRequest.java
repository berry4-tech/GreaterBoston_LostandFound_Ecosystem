package com.campus.lostfound.models.workrequest;

import java.util.Arrays;
import java.util.List;

/**
 * Work request for transferring an item from MBTA to a university campus.
 * Approval chain: MBTA Station Manager → Campus Coordinator → Student Pickup
 * 
 * This handles items found on MBTA (trains, buses, stations) that belong to university students.
 * MBTA and the university must coordinate the transfer.
 */
public class TransitToUniversityTransferRequest extends WorkRequest {
    
    // Item details
    private String itemId;                  // Found item ID
    private String lostItemId;              // Matched lost item ID (for auto-closure)
    private String itemName;
    private String itemCategory;
    private String itemDescription;
    
    // MBTA context (where item was found)
    private String transitType;             // "Subway", "Bus", "Commuter Rail"
    private String routeNumber;             // e.g., "Green Line", "Bus 39"
    private String stationName;             // Where found
    private String mbtaStationManagerId;
    private String mbtaStationManagerName;
    private String mbtaLocationId;
    
    // University context (destination)
    private String universityName;          // e.g., "Northeastern University"
    private String campusCoordinatorId;
    private String campusCoordinatorName;
    private String campusPickupLocation;    // Where student will get it
    
    // Student details
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentIdNumber;         // Student ID for verification
    
    // Transfer details
    private String transferDate;            // When transfer will happen
    private String transferNotes;           // Special instructions
    private boolean requiresIdVerification; // If student must show ID
    
    // MBTA-specific tracking
    private String mbtaIncidentNumber;      // MBTA's tracking number
    private String foundDate;               // When item was found on transit
    
    // Constructor
    public TransitToUniversityTransferRequest() {
        super();
        this.requestType = RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER;
        this.requiresIdVerification = true; // Always require ID for transit items
    }
    
    // Convenience constructor
    public TransitToUniversityTransferRequest(String mbtaManagerId, String mbtaManagerName,
                                              String itemId, String itemName,
                                              String studentId, String studentName) {
        this();
        this.mbtaStationManagerId = mbtaManagerId;
        this.mbtaStationManagerName = mbtaManagerName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.requesterId = mbtaManagerId;
        this.requesterName = mbtaManagerName;
        this.description = String.format("MBTA to University transfer of %s for student %s", 
                                        itemName, studentName);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // MBTA releases, Campus receives, Student confirms
        return Arrays.asList(
            "STATION_MANAGER",         // MBTA Station Manager approves release
            "CAMPUS_COORDINATOR",      // University approves receipt
            "STUDENT"                  // Student confirms pickup
        );
    }
    
    @Override
    public String getRequestSummary() {
        return String.format("Transit Transfer: %s from MBTA %s to %s for student %s", 
                            itemName, stationName, universityName, studentName);
    }
    
    @Override
    public boolean isValid() {
        // Must have item, MBTA context, university context, and student info
        if (itemId == null || itemId.isEmpty()) return false;
        if (mbtaStationManagerId == null || mbtaStationManagerId.isEmpty()) return false;
        if (campusCoordinatorId == null || campusCoordinatorId.isEmpty()) return false;
        if (studentId == null || studentId.isEmpty()) return false;
        if (stationName == null || stationName.isEmpty()) return false;
        if (campusPickupLocation == null || campusPickupLocation.isEmpty()) return false;
        
        return true;
    }
    
    /**
     * Check if this is a cross-enterprise transfer (always true for transit to university)
     */
    public boolean isCrossEnterpriseTransfer() {
        return true; // MBTA and University are always different enterprises
    }
    
    /**
     * Generate a transfer confirmation code for tracking
     */
    public String generateTransferCode() {
        if (requestId == null) return null;
        return "MBTA-UNI-" + requestId.substring(Math.max(0, requestId.length() - 6));
    }
    
    /**
     * Add transfer tracking note
     */
    public void addTransferNote(String note) {
        if (transferNotes == null) {
            transferNotes = note;
        } else {
            transferNotes += "\n" + note;
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
    
    public String getLostItemId() {
        return lostItemId;
    }
    
    public void setLostItemId(String lostItemId) {
        this.lostItemId = lostItemId;
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
    
    public String getTransitType() {
        return transitType;
    }
    
    public void setTransitType(String transitType) {
        this.transitType = transitType;
    }
    
    public String getRouteNumber() {
        return routeNumber;
    }
    
    public void setRouteNumber(String routeNumber) {
        this.routeNumber = routeNumber;
    }
    
    public String getStationName() {
        return stationName;
    }
    
    public void setStationName(String stationName) {
        this.stationName = stationName;
    }
    
    public String getMbtaStationManagerId() {
        return mbtaStationManagerId;
    }
    
    public void setMbtaStationManagerId(String mbtaStationManagerId) {
        this.mbtaStationManagerId = mbtaStationManagerId;
    }
    
    public String getMbtaStationManagerName() {
        return mbtaStationManagerName;
    }
    
    public void setMbtaStationManagerName(String mbtaStationManagerName) {
        this.mbtaStationManagerName = mbtaStationManagerName;
    }
    
    public String getMbtaLocationId() {
        return mbtaLocationId;
    }
    
    public void setMbtaLocationId(String mbtaLocationId) {
        this.mbtaLocationId = mbtaLocationId;
    }
    
    public String getUniversityName() {
        return universityName;
    }
    
    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }
    
    public String getCampusCoordinatorId() {
        return campusCoordinatorId;
    }
    
    public void setCampusCoordinatorId(String campusCoordinatorId) {
        this.campusCoordinatorId = campusCoordinatorId;
    }
    
    public String getCampusCoordinatorName() {
        return campusCoordinatorName;
    }
    
    public void setCampusCoordinatorName(String campusCoordinatorName) {
        this.campusCoordinatorName = campusCoordinatorName;
    }
    
    public String getCampusPickupLocation() {
        return campusPickupLocation;
    }
    
    public void setCampusPickupLocation(String campusPickupLocation) {
        this.campusPickupLocation = campusPickupLocation;
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
    
    public String getStudentIdNumber() {
        return studentIdNumber;
    }
    
    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }
    
    public String getTransferDate() {
        return transferDate;
    }
    
    public void setTransferDate(String transferDate) {
        this.transferDate = transferDate;
    }
    
    public String getTransferNotes() {
        return transferNotes;
    }
    
    public void setTransferNotes(String transferNotes) {
        this.transferNotes = transferNotes;
    }
    
    public boolean isRequiresIdVerification() {
        return requiresIdVerification;
    }
    
    public void setRequiresIdVerification(boolean requiresIdVerification) {
        this.requiresIdVerification = requiresIdVerification;
    }
    
    public String getMbtaIncidentNumber() {
        return mbtaIncidentNumber;
    }
    
    public void setMbtaIncidentNumber(String mbtaIncidentNumber) {
        this.mbtaIncidentNumber = mbtaIncidentNumber;
    }
    
    public String getFoundDate() {
        return foundDate;
    }
    
    public void setFoundDate(String foundDate) {
        this.foundDate = foundDate;
    }
    
    @Override
    public String toString() {
        return String.format("TransitToUniversityTransferRequest{id=%s, item=%s, from=%s, to=%s, student=%s, status=%s}",
                requestId, itemName, stationName, universityName, studentName, status);
    }
}
