package com.campus.lostfound.models.workrequest;

import java.util.Arrays;
import java.util.List;

/**
 * Emergency work request for transferring time-critical items from MBTA to Logan Airport.
 * 
 * Use Case: Passport/travel documents found on MBTA, traveler is at airport gate about to miss flight.
 * This is a high-priority, time-sensitive transfer that requires immediate coordination.
 * 
 * Flow: MBTA Station Manager → Airport Specialist → Emergency Delivery
 * 
 * Per PDF Use Case 3.2:
 * - Passport holder found on Red Line during morning rush
 * - System detects traveler pattern (airport-bound route, early morning, boarding pass inside)
 * - Station Manager creates urgent request to Logan Airport for time-critical coordination
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class MBTAToAirportEmergencyRequest extends WorkRequest {
    
    // Item details
    private String itemId;
    private String itemName;
    private String itemDescription;
    private String itemCategory;          // Usually "IDS_CARDS", "DOCUMENTS"
    
    // MBTA context (source)
    private String mbtaStationId;
    private String mbtaStationName;
    private String mbtaStationManagerId;
    private String mbtaStationManagerName;
    private String transitLine;            // e.g., "Red Line", "Blue Line"
    private String foundLocation;          // Specific location on train/station
    private String foundDateTime;          // When item was found
    private String mbtaIncidentNumber;     // MBTA tracking number
    
    // Airport context (destination)
    private String airportTerminal;        // e.g., "Terminal A", "Terminal B"
    private String airportGate;            // e.g., "Gate B22"
    private String airportSpecialistId;
    private String airportSpecialistName;
    private String airportContactPhone;    // Direct line for emergency
    
    // Traveler details
    private String travelerId;             // If known
    private String travelerName;
    private String travelerPhone;
    private String travelerEmail;
    private String flightNumber;           // e.g., "DL1234"
    private String flightDepartureTime;    // e.g., "10:30 AM"
    private String airline;                // e.g., "Delta", "JetBlue"
    private String destinationCity;        // Where traveler is flying to
    
    // Emergency coordination
    private String emergencyContactNumber; // Airport operations emergency line
    private String courierMethod;          // "MBTA_SHUTTLE", "TAXI", "POLICE_ESCORT"
    private String estimatedDeliveryTime;  // ETA to airport
    private boolean policeEscortRequested; // If police escort needed for speed
    private boolean gateHoldRequested;     // If airline should hold gate
    private String gateHoldStatus;         // "REQUESTED", "APPROVED", "DENIED"
    
    // Document verification (for passports/IDs)
    private String documentType;           // "PASSPORT", "DRIVERS_LICENSE", "BOARDING_PASS"
    private String documentNumber;         // Last 4 digits only for security
    private String documentIssuingCountry; // For passports
    private boolean documentPhotoMatch;    // If photo matches traveler description
    
    // Status tracking
    private String pickupConfirmationCode;
    private String deliveryConfirmationCode;
    private String currentLocationStatus;  // "AT_MBTA", "IN_TRANSIT", "AT_AIRPORT", "DELIVERED"
    private String deliveryNotes;
    
    // Constructor
    public MBTAToAirportEmergencyRequest() {
        super();
        this.requestType = RequestType.MBTA_TO_AIRPORT_EMERGENCY;
        this.priority = RequestPriority.URGENT; // Always urgent for emergency transfers
        this.currentLocationStatus = "AT_MBTA";
    }
    
    // Convenience constructor
    public MBTAToAirportEmergencyRequest(String mbtaManagerId, String mbtaManagerName,
                                          String itemId, String itemName,
                                          String travelerName, String flightNumber) {
        this();
        this.mbtaStationManagerId = mbtaManagerId;
        this.mbtaStationManagerName = mbtaManagerName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.travelerName = travelerName;
        this.flightNumber = flightNumber;
        this.requesterId = mbtaManagerId;
        this.requesterName = mbtaManagerName;
        this.description = String.format("EMERGENCY: %s found at MBTA for traveler %s on flight %s", 
                                        itemName, travelerName, flightNumber);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // Fast-track approval for emergencies
        return Arrays.asList(
            "STATION_MANAGER",              // MBTA releases item
            "AIRPORT_LOST_FOUND_SPECIALIST" // Airport confirms receipt and delivery
        );
    }
    
    @Override
    public String getRequestSummary() {
        return String.format("🚨 EMERGENCY: %s from MBTA %s to Logan %s for flight %s (%s)", 
                            itemName, 
                            mbtaStationName != null ? mbtaStationName : "Station",
                            airportTerminal != null ? airportTerminal : "Airport",
                            flightNumber != null ? flightNumber : "Unknown",
                            travelerName != null ? travelerName : "Traveler");
    }
    
    @Override
    public boolean isValid() {
        // Must have item, MBTA source, airport destination, and traveler info
        if (itemId == null || itemId.isEmpty()) return false;
        if (mbtaStationManagerId == null || mbtaStationManagerId.isEmpty()) return false;
        if (mbtaStationName == null || mbtaStationName.isEmpty()) return false;
        if (flightNumber == null || flightNumber.isEmpty()) return false;
        
        return true;
    }
    
    /**
     * Check if this is truly time-critical (flight within 2 hours)
     */
    public boolean isTimeCritical() {
        // If we have departure time, check if it's within 2 hours
        // For now, all MBTA to Airport emergencies are considered time-critical
        return true;
    }
    
    /**
     * Calculate urgency level based on flight time
     */
    public String getUrgencyLevel() {
        if (flightDepartureTime == null) return "CRITICAL";
        // In a real implementation, parse time and calculate
        return "CRITICAL"; // Default to critical for emergency requests
    }
    
    /**
     * Request gate hold from airline
     */
    public void requestGateHold() {
        this.gateHoldRequested = true;
        this.gateHoldStatus = "REQUESTED";
        addDeliveryNote("Gate hold requested for flight " + flightNumber);
    }
    
    /**
     * Request police escort for faster delivery
     */
    public void requestPoliceEscort() {
        this.policeEscortRequested = true;
        addDeliveryNote("Police escort requested for emergency delivery");
    }
    
    /**
     * Update current location status
     */
    public void updateLocationStatus(String status) {
        this.currentLocationStatus = status;
        this.lastUpdatedAt = java.time.LocalDateTime.now();
        addDeliveryNote("Status updated: " + status);
    }
    
    /**
     * Confirm pickup from MBTA
     */
    public void confirmPickup(String confirmationCode) {
        this.pickupConfirmationCode = confirmationCode;
        this.currentLocationStatus = "IN_TRANSIT";
        addDeliveryNote("Picked up from MBTA. Code: " + confirmationCode);
    }
    
    /**
     * Confirm delivery at airport
     */
    public void confirmDelivery(String confirmationCode) {
        this.deliveryConfirmationCode = confirmationCode;
        this.currentLocationStatus = "DELIVERED";
        addDeliveryNote("Delivered at airport. Code: " + confirmationCode);
        this.complete();
    }
    
    /**
     * Add delivery tracking note
     */
    public void addDeliveryNote(String note) {
        String timestamp = java.time.LocalDateTime.now().toString();
        if (deliveryNotes == null) {
            deliveryNotes = "[" + timestamp + "] " + note;
        } else {
            deliveryNotes += "\n[" + timestamp + "] " + note;
        }
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Generate emergency tracking code
     */
    public String generateEmergencyCode() {
        if (requestId == null) return "EMR-" + System.currentTimeMillis();
        return "EMR-MBTA-LOG-" + requestId.substring(Math.max(0, requestId.length() - 6));
    }
    
    /**
     * Check if this involves a passport (higher priority)
     */
    public boolean isPassportEmergency() {
        return "PASSPORT".equalsIgnoreCase(documentType) || 
               (itemName != null && itemName.toLowerCase().contains("passport"));
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
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public String getItemCategory() {
        return itemCategory;
    }
    
    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }
    
    public String getMbtaStationId() {
        return mbtaStationId;
    }
    
    public void setMbtaStationId(String mbtaStationId) {
        this.mbtaStationId = mbtaStationId;
    }
    
    public String getMbtaStationName() {
        return mbtaStationName;
    }
    
    public void setMbtaStationName(String mbtaStationName) {
        this.mbtaStationName = mbtaStationName;
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
    
    public String getTransitLine() {
        return transitLine;
    }
    
    public void setTransitLine(String transitLine) {
        this.transitLine = transitLine;
    }
    
    public String getFoundLocation() {
        return foundLocation;
    }
    
    public void setFoundLocation(String foundLocation) {
        this.foundLocation = foundLocation;
    }
    
    public String getFoundDateTime() {
        return foundDateTime;
    }
    
    public void setFoundDateTime(String foundDateTime) {
        this.foundDateTime = foundDateTime;
    }
    
    public String getMbtaIncidentNumber() {
        return mbtaIncidentNumber;
    }
    
    public void setMbtaIncidentNumber(String mbtaIncidentNumber) {
        this.mbtaIncidentNumber = mbtaIncidentNumber;
    }
    
    public String getAirportTerminal() {
        return airportTerminal;
    }
    
    public void setAirportTerminal(String airportTerminal) {
        this.airportTerminal = airportTerminal;
    }
    
    public String getAirportGate() {
        return airportGate;
    }
    
    public void setAirportGate(String airportGate) {
        this.airportGate = airportGate;
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
    
    public String getAirportContactPhone() {
        return airportContactPhone;
    }
    
    public void setAirportContactPhone(String airportContactPhone) {
        this.airportContactPhone = airportContactPhone;
    }
    
    public String getTravelerId() {
        return travelerId;
    }
    
    public void setTravelerId(String travelerId) {
        this.travelerId = travelerId;
    }
    
    public String getTravelerName() {
        return travelerName;
    }
    
    public void setTravelerName(String travelerName) {
        this.travelerName = travelerName;
    }
    
    public String getTravelerPhone() {
        return travelerPhone;
    }
    
    public void setTravelerPhone(String travelerPhone) {
        this.travelerPhone = travelerPhone;
    }
    
    public String getTravelerEmail() {
        return travelerEmail;
    }
    
    public void setTravelerEmail(String travelerEmail) {
        this.travelerEmail = travelerEmail;
    }
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
    
    public String getFlightDepartureTime() {
        return flightDepartureTime;
    }
    
    public void setFlightDepartureTime(String flightDepartureTime) {
        this.flightDepartureTime = flightDepartureTime;
    }
    
    public String getAirline() {
        return airline;
    }
    
    public void setAirline(String airline) {
        this.airline = airline;
    }
    
    public String getDestinationCity() {
        return destinationCity;
    }
    
    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }
    
    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }
    
    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }
    
    public String getCourierMethod() {
        return courierMethod;
    }
    
    public void setCourierMethod(String courierMethod) {
        this.courierMethod = courierMethod;
    }
    
    public String getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }
    
    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }
    
    public boolean isPoliceEscortRequested() {
        return policeEscortRequested;
    }
    
    public void setPoliceEscortRequested(boolean policeEscortRequested) {
        this.policeEscortRequested = policeEscortRequested;
    }
    
    public boolean isGateHoldRequested() {
        return gateHoldRequested;
    }
    
    public void setGateHoldRequested(boolean gateHoldRequested) {
        this.gateHoldRequested = gateHoldRequested;
    }
    
    public String getGateHoldStatus() {
        return gateHoldStatus;
    }
    
    public void setGateHoldStatus(String gateHoldStatus) {
        this.gateHoldStatus = gateHoldStatus;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getDocumentNumber() {
        return documentNumber;
    }
    
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
    
    public String getDocumentIssuingCountry() {
        return documentIssuingCountry;
    }
    
    public void setDocumentIssuingCountry(String documentIssuingCountry) {
        this.documentIssuingCountry = documentIssuingCountry;
    }
    
    public boolean isDocumentPhotoMatch() {
        return documentPhotoMatch;
    }
    
    public void setDocumentPhotoMatch(boolean documentPhotoMatch) {
        this.documentPhotoMatch = documentPhotoMatch;
    }
    
    public String getPickupConfirmationCode() {
        return pickupConfirmationCode;
    }
    
    public void setPickupConfirmationCode(String pickupConfirmationCode) {
        this.pickupConfirmationCode = pickupConfirmationCode;
    }
    
    public String getDeliveryConfirmationCode() {
        return deliveryConfirmationCode;
    }
    
    public void setDeliveryConfirmationCode(String deliveryConfirmationCode) {
        this.deliveryConfirmationCode = deliveryConfirmationCode;
    }
    
    public String getCurrentLocationStatus() {
        return currentLocationStatus;
    }
    
    public void setCurrentLocationStatus(String currentLocationStatus) {
        this.currentLocationStatus = currentLocationStatus;
    }
    
    public String getDeliveryNotes() {
        return deliveryNotes;
    }
    
    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }
    
    @Override
    public String toString() {
        return String.format("MBTAToAirportEmergencyRequest{id=%s, item=%s, from=%s, flight=%s, traveler=%s, status=%s, location=%s}",
                requestId, itemName, mbtaStationName, flightNumber, travelerName, status, currentLocationStatus);
    }
}
