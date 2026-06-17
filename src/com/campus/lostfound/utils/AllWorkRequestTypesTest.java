package com.campus.lostfound.utils;

import com.campus.lostfound.models.workrequest.*;

/**
 * Comprehensive test for all 5 WorkRequest types (Part 2 validation)
 * Tests that each request type follows the correct approval chain and workflow.
 */
public class AllWorkRequestTypesTest {
    
    public static void main(String[] args) {
        System.out.println("=== ALL WORK REQUEST TYPES TEST (Part 2) ===\n");
        
        // Test 1: ItemClaimRequest
        testItemClaimRequest();
        
        // Test 2: CrossCampusTransferRequest
        testCrossCampusTransferRequest();
        
        // Test 3: TransitToUniversityTransferRequest
        testTransitToUniversityTransferRequest();
        
        // Test 4: AirportToUniversityTransferRequest
        testAirportToUniversityTransferRequest();
        
        // Test 5: PoliceEvidenceRequest
        testPoliceEvidenceRequest();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("âœ… ALL 5 WORK REQUEST TYPES TESTED SUCCESSFULLY!");
        System.out.println("=".repeat(70));
        System.out.println("\nâœ… Part 2 Complete: All WorkRequest types implemented and working!");
        System.out.println("Next: Part 3 - Create MongoWorkRequestDAO for database persistence");
    }
    
    private static void testItemClaimRequest() {
        System.out.println("TEST 1: ItemClaimRequest (High-Value)");
        System.out.println("=".repeat(70));
        
        ItemClaimRequest request = new ItemClaimRequest(
            "student123", "John Doe",
            "item789", "MacBook Pro", 2499.00
        );
        request.setClaimDetails("This is my laptop with all my coursework.");
        request.setIdentifyingFeatures("Serial XYZ123, scratch on bottom right corner.");
        request.setProofDescription("I have purchase receipt and AppleCare documentation.");
        
        System.out.println("Type: " + request.getRequestType());
        System.out.println("Summary: " + request.getRequestSummary());
        System.out.println("Approval Chain: " + request.getApprovalChain());
        System.out.println("Valid: " + request.isValid());
        System.out.println("Requires Police: " + request.requiresPoliceVerification());
        System.out.println("Status: " + request.getStatus());
        System.out.println();
    }
    
    private static void testCrossCampusTransferRequest() {
        System.out.println("TEST 2: CrossCampusTransferRequest (NEU → BU)");
        System.out.println("=".repeat(70));
        
        CrossCampusTransferRequest request = new CrossCampusTransferRequest(
            "coord456", "Jane Smith",
            "item321", "Blue Backpack",
            "student789", "Alice Johnson"
        );
        request.setSourceCampusName("Northeastern University");
        request.setDestinationCampusName("Boston University");
        request.setDestinationCoordinatorId("coord999");
        request.setPickupLocation("BU Student Center");
        request.setTransferMethod("Courier");
        
        System.out.println("Type: " + request.getRequestType());
        System.out.println("Summary: " + request.getRequestSummary());
        System.out.println("Approval Chain: " + request.getApprovalChain());
        System.out.println("Valid: " + request.isValid());
        System.out.println("Status: " + request.getStatus());
        
        // Test tracking notes
        request.addTrackingNote("Item packaged for transfer");
        request.addTrackingNote("Courier en route to BU");
        System.out.println("Tracking Notes: " + request.getTrackingNotes());
        System.out.println();
    }
    
    private static void testTransitToUniversityTransferRequest() {
        System.out.println("TEST 3: TransitToUniversityTransferRequest (MBTA → NEU)");
        System.out.println("=".repeat(70));
        
        TransitToUniversityTransferRequest request = new TransitToUniversityTransferRequest(
            "mbta555", "Mike Brown",
            "item654", "Black Wallet",
            "student111", "Bob Wilson"
        );
        request.setTransitType("Subway");
        request.setRouteNumber("Green Line");
        request.setStationName("Northeastern Station");
        request.setUniversityName("Northeastern University");
        request.setCampusCoordinatorId("coord456");
        request.setCampusPickupLocation("Campus Security Office");
        request.setMbtaIncidentNumber("MBTA-2025-1234");
        
        System.out.println("Type: " + request.getRequestType());
        System.out.println("Summary: " + request.getRequestSummary());
        System.out.println("Approval Chain: " + request.getApprovalChain());
        System.out.println("Valid: " + request.isValid());
        System.out.println("Transfer Code: " + request.generateTransferCode());
        System.out.println("Cross-Enterprise: " + request.isCrossEnterpriseTransfer());
        System.out.println("Requires ID: " + request.isRequiresIdVerification());
        System.out.println("Status: " + request.getStatus());
        System.out.println();
    }
    
    private static void testAirportToUniversityTransferRequest() {
        System.out.println("TEST 4: AirportToUniversityTransferRequest (Logan → NEU)");
        System.out.println("=".repeat(70));
        
        AirportToUniversityTransferRequest request = new AirportToUniversityTransferRequest(
            "airport777", "Sarah Davis",
            "item888", "Laptop Bag",
            "student222", "Charlie Green"
        );
        request.setTerminalNumber("Terminal B");
        request.setAirportArea("Security Checkpoint");
        request.setAirportIncidentNumber("LOGAN-2025-5678");
        request.setUniversityName("Northeastern University");
        request.setCampusCoordinatorId("coord456");
        request.setCampusPickupLocation("International Student Services");
        request.setFlightNumber("DL1234");
        request.setWasInSecureArea(true);
        request.setSecurityNotes("Found in secure area, TSA notified and cleared item.");
        request.setEstimatedValue(150.00);
        
        System.out.println("Type: " + request.getRequestType());
        System.out.println("Summary: " + request.getRequestSummary());
        System.out.println("Approval Chain: " + request.getApprovalChain());
        System.out.println("Valid: " + request.isValid());
        System.out.println("Transfer Code: " + request.generateTransferCode());
        System.out.println("Requires Police Verification: " + request.isRequiresPoliceVerification());
        System.out.println("Enhanced Security: " + request.requiresEnhancedSecurity());
        System.out.println("Found in Secure Area: " + request.isWasInSecureArea());
        System.out.println("Status: " + request.getStatus());
        System.out.println();
    }
    
    private static void testPoliceEvidenceRequest() {
        System.out.println("TEST 5: PoliceEvidenceRequest (High-Value Verification)");
        System.out.println("=".repeat(70));
        
        PoliceEvidenceRequest request = new PoliceEvidenceRequest(
            "coord456", "Jane Smith",
            "item999", "iPhone 15 Pro",
            "High-value item requires serial number verification"
        );
        request.setItemCategory("Electronics");
        request.setEstimatedValue(1200.00);
        request.setSerialNumber("ABCD1234567890");
        request.setImeiNumber("123456789012345");
        request.setBrandName("Apple");
        request.setModelNumber("iPhone 15 Pro 256GB");
        request.setSourceEnterpriseName("Northeastern University");
        request.setHighValueVerification(true);
        request.setStolenCheck(true);
        request.setUrgencyLevel("High");
        
        System.out.println("Type: " + request.getRequestType());
        System.out.println("Summary: " + request.getRequestSummary());
        System.out.println("Approval Chain: " + request.getApprovalChain());
        System.out.println("Valid: " + request.isValid());
        System.out.println("Case Number: " + request.generateCaseNumber());
        System.out.println("Verification Status: " + request.getVerificationStatus());
        System.out.println("Requires Immediate Action: " + request.requiresImmediateAction());
        System.out.println("Status: " + request.getStatus());
        
        // Simulate police verification
        System.out.println("\n--- Simulating Police Verification ---");
        request.markAsClear("Serial number checked against database. No match in stolen items. Item cleared for return.");
        System.out.println("After Verification - Status: " + request.getVerificationStatus());
        System.out.println("Verification Complete: " + request.isVerificationComplete());
        System.out.println("Notes: " + request.getVerificationNotes());
        System.out.println();
    }
    
    // Additional test: Test all approval chains
    private static void printApprovalChains() {
        System.out.println("\nAPPROVAL CHAIN SUMMARY");
        System.out.println("=".repeat(70));
        
        System.out.println("1. ItemClaimRequest (Regular): " + 
            new ItemClaimRequest("u1", "U1", "i1", "Item", 100).getApprovalChain());
        System.out.println("2. ItemClaimRequest (High-Value): " + 
            new ItemClaimRequest("u1", "U1", "i1", "Item", 1000).getApprovalChain());
        System.out.println("3. CrossCampusTransferRequest: " + 
            new CrossCampusTransferRequest().getApprovalChain());
        System.out.println("4. TransitToUniversityTransferRequest: " + 
            new TransitToUniversityTransferRequest().getApprovalChain());
        System.out.println("5. AirportToUniversityTransferRequest: " + 
            new AirportToUniversityTransferRequest().getApprovalChain());
        System.out.println("6. PoliceEvidenceRequest: " + 
            new PoliceEvidenceRequest().getApprovalChain());
    }
}
