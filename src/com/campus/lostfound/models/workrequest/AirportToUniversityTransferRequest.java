package com.campus.lostfound.models.workrequest;

import java.util.Arrays;
import java.util.List;

/**
 * Work request for transferring an item from Logan Airport to a university,
 * with police verification for high-security items.
 * Approval chain: Airport Specialist → Campus Coordinator → Police Verification
 * 
 * This handles items found at the airport that belong to university students.
 * Requires police verification due to airport security protocols.
 */
public class AirportToUniversityTransferRequest extends WorkRequest {
    
    // Item details
    private String itemId;                  // Found item ID
    private String lostItemId;              // Matched lost item ID (for auto-closure, if applicable)
    private String itemName;
    private String itemCategory;
    private String itemDescription;
    private double estimatedValue;
    
    // Airport context (where item was found)
    private String terminalNumber;          // e.g., "Terminal A", "Terminal E"
    private String airportArea;             // "Baggage Claim", "Security", "Gate"
    private String airportSpecialistId;
    private String airportSpecialistName;
    private String foundLocation;           // Specific location details
    
    // University context (destination)
    private String universityName;
    private String campusCoordinatorId;
    private String campusCoordinatorName;
    private String campusPickupLocation;
    
    // Student details
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentIdNumber;
    private String flightNumber;            // Flight student was on (if known)
    
    // Police verification (required for airport items)
    private String policeOfficerId;
    private String policeOfficerName;
    private boolean requiresPoliceVerification;
    private String securityClearanceLevel;  // "Standard", "Enhanced", "TSA"
    
    // Airport-specific tracking
    private String airportIncidentNumber;   // Airport's tracking number
    private String tsamanagerInvolved;      // If TSA was involved
    private String foundDateTime;           // Exact date/time found
    private boolean wasInSecureArea;        // Found in secure area?
    
    // Transfer details
    private String transferDate;
    private String transferNotes;
    private String securityNotes;           // Special security considerations
    
    // Constructor
    public AirportToUniversityTransferRequest() {
        super();
        this.requestType = RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER;
        this.requiresPoliceVerification = true; // Always requires police for airport items
        this.securityClearanceLevel = "Standard";
    }
    
    // Convenience constructor
    public AirportToUniversityTransferRequest(String airportSpecialistId, String airportSpecialistName,
                                              String itemId, String itemName,
                                              String studentId, String studentName) {
        this();
        this.airportSpecialistId = airportSpecialistId;
        this.airportSpecialistName = airportSpecialistName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.requesterId = airportSpecialistId;
        this.requesterName = airportSpecialistName;
        this.description = String.format("Airport to University transfer of %s for student %s", 
                                        itemName, studentName);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // Airport releases, Campus receives, Police verifies, Student confirms pickup
        return Arrays.asList(
            "AIRPORT_LOST_FOUND_SPECIALIST",  // Airport approves release
            "CAMPUS_COORDINATOR",              // University approves receipt
            "POLICE_EVIDENCE_CUSTODIAN",       // Police verifies (security requirement)
            "STUDENT"                          // Student confirms pickup
        );
    }
    
    @Override
    public String getRequestSummary() {
        String secureTag = wasInSecureArea ? " [SECURE AREA]" : "";
        return String.format("Airport Transfer: %s from %s to %s for student %s%s", 
                            itemName, terminalNumber, universityName, studentName, secureTag);
    }
    
    @Override
    public boolean isValid() {
        // Must have all critical fields for airport security protocols
        if (itemId == null || itemId.isEmpty()) return false;
        if (airportSpecialistId == null || airportSpecialistId.isEmpty()) return false;
        if (campusCoordinatorId == null || campusCoordinatorId.isEmpty()) return false;
        if (studentId == null || studentId.isEmpty()) return false;
        if (terminalNumber == null || terminalNumber.isEmpty()) return false;
        if (airportIncidentNumber == null || airportIncidentNumber.isEmpty()) return false;
        if (campusPickupLocation == null || campusPickupLocation.isEmpty()) return false;
        
        // Items from secure areas require additional documentation
        if (wasInSecureArea && (securityNotes == null || securityNotes.length() < 20)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if this transfer requires enhanced security clearance
     */
    public boolean requiresEnhancedSecurity() {
        return wasInSecureArea || 
               estimatedValue > 1000.0 || 
               "Enhanced".equals(securityClearanceLevel) ||
               "TSA".equals(securityClearanceLevel);
    }
    
    /**
     * Generate airport-university transfer code
     */
    public String generateTransferCode() {
        if (requestId == null) return null;
        return "LOGAN-UNI-" + requestId.substring(Math.max(0, requestId.length() - 6));
    }
    
    /**
     * Add security note about the transfer
     */
    public void addSecurityNote(String note) {
        if (securityNotes == null) {
            securityNotes = note;
        } else {
            securityNotes += "\n" + note;
        }
        this.lastUpdatedAt = java.time.LocalDateTime.now();
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
    
    public double getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    public String getTerminalNumber() {
        return terminalNumber;
    }
    
    public void setTerminalNumber(String terminalNumber) {
        this.terminalNumber = terminalNumber;
    }
    
    public String getAirportArea() {
        return airportArea;
    }
    
    public void setAirportArea(String airportArea) {
        this.airportArea = airportArea;
    }
    
    public String getAirportSpecialistId() {
        return airportSpecialistId;
    }
    
    public void setAirportSpecialistId(String airportSpecialistId) {
        this.airportSpecialistId = airportSpecialistId;
    }
    
    public String getAirportSpecialistName() {
        return airportSpecialistName;
    }
    
    public void setAirportSpecialistName(String airportSpecialistName) {
        this.airportSpecialistName = airportSpecialistName;
    }
    
    public String getFoundLocation() {
        return foundLocation;
    }
    
    public void setFoundLocation(String foundLocation) {
        this.foundLocation = foundLocation;
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
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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
    
    public boolean isRequiresPoliceVerification() {
        return requiresPoliceVerification;
    }
    
    public void setRequiresPoliceVerification(boolean requiresPoliceVerification) {
        this.requiresPoliceVerification = requiresPoliceVerification;
    }
    
    public String getSecurityClearanceLevel() {
        return securityClearanceLevel;
    }
    
    public void setSecurityClearanceLevel(String securityClearanceLevel) {
        this.securityClearanceLevel = securityClearanceLevel;
    }
    
    public String getAirportIncidentNumber() {
        return airportIncidentNumber;
    }
    
    public void setAirportIncidentNumber(String airportIncidentNumber) {
        this.airportIncidentNumber = airportIncidentNumber;
    }
    
    public String getTsamanagerInvolved() {
        return tsamanagerInvolved;
    }
    
    public void setTsamanagerInvolved(String tsamanagerInvolved) {
        this.tsamanagerInvolved = tsamanagerInvolved;
    }
    
    public String getFoundDateTime() {
        return foundDateTime;
    }
    
    public void setFoundDateTime(String foundDateTime) {
        this.foundDateTime = foundDateTime;
    }
    
    public boolean isWasInSecureArea() {
        return wasInSecureArea;
    }
    
    public void setWasInSecureArea(boolean wasInSecureArea) {
        this.wasInSecureArea = wasInSecureArea;
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
    
    public String getSecurityNotes() {
        return securityNotes;
    }
    
    public void setSecurityNotes(String securityNotes) {
        this.securityNotes = securityNotes;
    }
    
    @Override
    public String toString() {
        return String.format("AirportToUniversityTransferRequest{id=%s, item=%s, terminal=%s, student=%s, secure=%s, status=%s}",
                requestId, itemName, terminalNumber, studentName, wasInSecureArea, status);
    }
}
