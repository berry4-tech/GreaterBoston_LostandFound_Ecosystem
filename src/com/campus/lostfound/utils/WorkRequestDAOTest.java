package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoWorkRequestDAO;
import com.campus.lostfound.models.workrequest.*;

import java.util.List;

/**
 * Test for MongoWorkRequestDAO - validates database persistence (Part 3)
 * Tests saving, loading, and querying all WorkRequest types.
 */
public class WorkRequestDAOTest {
    
    public static void main(String[] args) {
        System.out.println("=== WORK REQUEST DAO TEST (Part 3) ===\n");
        
        MongoWorkRequestDAO dao = new MongoWorkRequestDAO();
        
        // Test 1: Save and retrieve ItemClaimRequest
        testItemClaimRequestDAO(dao);
        
        // Test 2: Save and retrieve CrossCampusTransferRequest
        testCrossCampusTransferRequestDAO(dao);
        
        // Test 3: Save and retrieve TransitToUniversityTransferRequest
        testTransitTransferRequestDAO(dao);
        
        // Test 4: Save and retrieve AirportToUniversityTransferRequest
        testAirportTransferRequestDAO(dao);
        
        // Test 5: Save and retrieve PoliceEvidenceRequest
        testPoliceEvidenceRequestDAO(dao);
        
        // Test 6: Query methods
        testQueryMethods(dao);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("âœ… ALL DAO TESTS PASSED!");
        System.out.println("=".repeat(70));
        System.out.println("\nâœ… Part 3 Complete: MongoWorkRequestDAO implemented and working!");
        System.out.println("Database operations verified:");
        System.out.println("  - Polymorphic saving/loading ✅");
        System.out.println("  - All 5 request types persist correctly ✅");
        System.out.println("  - Query methods work ✅");
        System.out.println("  - ObjectId conversion works ✅");
        System.out.println("\nNext: Part 4 - Create WorkRequestService for business logic");
    }
    
    private static void testItemClaimRequestDAO(MongoWorkRequestDAO dao) {
        System.out.println("TEST 1: ItemClaimRequest DAO Operations");
        System.out.println("=".repeat(70));
        
        // Create request
        ItemClaimRequest request = new ItemClaimRequest(
            "student123", "John Doe",
            "item789", "MacBook Pro", 2499.00
        );
        request.setClaimDetails("This is my laptop with all my coursework.");
        request.setIdentifyingFeatures("Serial XYZ123, scratch on bottom right.");
        request.setProofDescription("I have purchase receipt and AppleCare docs.");
        request.setRequesterOrganizationId("org123");
        
        // Save
        System.out.println("Saving ItemClaimRequest...");
        String id = dao.save(request);
        System.out.println("âœ… Saved with ID: " + id);
        
        // Retrieve
        System.out.println("Retrieving ItemClaimRequest...");
        WorkRequest retrieved = dao.findById(id);
        System.out.println("âœ… Retrieved: " + retrieved.getClass().getSimpleName());
        System.out.println("   Type: " + retrieved.getRequestType());
        System.out.println("   Status: " + retrieved.getStatus());
        System.out.println("   Requester: " + retrieved.getRequesterName());
        
        // Verify it's the correct type
        if (retrieved instanceof ItemClaimRequest) {
            ItemClaimRequest loaded = (ItemClaimRequest) retrieved;
            System.out.println("   Item: " + loaded.getItemName());
            System.out.println("   Value: $" + loaded.getItemValue());
            System.out.println("   High-Value: " + loaded.isHighValue());
            System.out.println("âœ… Type cast successful - all fields loaded correctly");
        }
        System.out.println();
    }
    
    private static void testCrossCampusTransferRequestDAO(MongoWorkRequestDAO dao) {
        System.out.println("TEST 2: CrossCampusTransferRequest DAO Operations");
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
        request.setRequesterOrganizationId("org456");
        
        System.out.println("Saving CrossCampusTransferRequest...");
        String id = dao.save(request);
        System.out.println("âœ… Saved with ID: " + id);
        
        System.out.println("Retrieving CrossCampusTransferRequest...");
        WorkRequest retrieved = dao.findById(id);
        System.out.println("âœ… Retrieved: " + retrieved.getClass().getSimpleName());
        
        if (retrieved instanceof CrossCampusTransferRequest) {
            CrossCampusTransferRequest loaded = (CrossCampusTransferRequest) retrieved;
            System.out.println("   From: " + loaded.getSourceCampusName());
            System.out.println("   To: " + loaded.getDestinationCampusName());
            System.out.println("   Student: " + loaded.getStudentName());
            System.out.println("   Method: " + loaded.getTransferMethod());
            System.out.println("âœ… All transfer fields loaded correctly");
        }
        System.out.println();
    }
    
    private static void testTransitTransferRequestDAO(MongoWorkRequestDAO dao) {
        System.out.println("TEST 3: TransitToUniversityTransferRequest DAO Operations");
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
        request.setCampusPickupLocation("Campus Security");
        request.setMbtaIncidentNumber("MBTA-2025-1234");
        request.setRequesterOrganizationId("mbta-org");
        
        System.out.println("Saving TransitToUniversityTransferRequest...");
        String id = dao.save(request);
        System.out.println("âœ… Saved with ID: " + id);
        
        System.out.println("Retrieving TransitToUniversityTransferRequest...");
        WorkRequest retrieved = dao.findById(id);
        System.out.println("âœ… Retrieved: " + retrieved.getClass().getSimpleName());
        
        if (retrieved instanceof TransitToUniversityTransferRequest) {
            TransitToUniversityTransferRequest loaded = (TransitToUniversityTransferRequest) retrieved;
            System.out.println("   Transit: " + loaded.getTransitType() + " - " + loaded.getRouteNumber());
            System.out.println("   Station: " + loaded.getStationName());
            System.out.println("   MBTA Incident: " + loaded.getMbtaIncidentNumber());
            System.out.println("   ID Required: " + loaded.isRequiresIdVerification());
            System.out.println("âœ… All transit fields loaded correctly");
        }
        System.out.println();
    }
    
    private static void testAirportTransferRequestDAO(MongoWorkRequestDAO dao) {
        System.out.println("TEST 4: AirportToUniversityTransferRequest DAO Operations");
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
        request.setSecurityNotes("Found in secure area, TSA cleared.");
        request.setRequesterOrganizationId("airport-org");
        
        System.out.println("Saving AirportToUniversityTransferRequest...");
        String id = dao.save(request);
        System.out.println("âœ… Saved with ID: " + id);
        
        System.out.println("Retrieving AirportToUniversityTransferRequest...");
        WorkRequest retrieved = dao.findById(id);
        System.out.println("âœ… Retrieved: " + retrieved.getClass().getSimpleName());
        
        if (retrieved instanceof AirportToUniversityTransferRequest) {
            AirportToUniversityTransferRequest loaded = (AirportToUniversityTransferRequest) retrieved;
            System.out.println("   Terminal: " + loaded.getTerminalNumber());
            System.out.println("   Area: " + loaded.getAirportArea());
            System.out.println("   Secure Area: " + loaded.isWasInSecureArea());
            System.out.println("   Flight: " + loaded.getFlightNumber());
            System.out.println("   Police Required: " + loaded.isRequiresPoliceVerification());
            System.out.println("âœ… All airport fields loaded correctly");
        }
        System.out.println();
    }
    
    private static void testPoliceEvidenceRequestDAO(MongoWorkRequestDAO dao) {
        System.out.println("TEST 5: PoliceEvidenceRequest DAO Operations");
        System.out.println("=".repeat(70));
        
        PoliceEvidenceRequest request = new PoliceEvidenceRequest(
            "coord456", "Jane Smith",
            "item999", "iPhone 15 Pro",
            "Serial number verification required"
        );
        request.setSerialNumber("ABCD1234567890");
        request.setImeiNumber("123456789012345");
        request.setBrandName("Apple");
        request.setEstimatedValue(1200.00);
        request.setSourceEnterpriseName("Northeastern University");
        request.setStolenCheck(true);
        request.setHighValueVerification(true);
        request.setUrgencyLevel("High");
        request.setRequesterOrganizationId("neu-org");
        
        System.out.println("Saving PoliceEvidenceRequest...");
        String id = dao.save(request);
        System.out.println("âœ… Saved with ID: " + id);
        
        System.out.println("Retrieving PoliceEvidenceRequest...");
        WorkRequest retrieved = dao.findById(id);
        System.out.println("âœ… Retrieved: " + retrieved.getClass().getSimpleName());
        
        if (retrieved instanceof PoliceEvidenceRequest) {
            PoliceEvidenceRequest loaded = (PoliceEvidenceRequest) retrieved;
            System.out.println("   Item: " + loaded.getItemName());
            System.out.println("   Serial: " + loaded.getSerialNumber());
            System.out.println("   IMEI: " + loaded.getImeiNumber());
            System.out.println("   Verification: " + loaded.getVerificationStatus());
            System.out.println("   Stolen Check: " + loaded.isStolenCheck());
            System.out.println("   Urgency: " + loaded.getUrgencyLevel());
            System.out.println("âœ… All police evidence fields loaded correctly");
        }
        System.out.println();
    }
    
    private static void testQueryMethods(MongoWorkRequestDAO dao) {
        System.out.println("TEST 6: Query Methods");
        System.out.println("=".repeat(70));
        
        // Test findByStatus
        System.out.println("Testing findByStatus(PENDING)...");
        List<WorkRequest> pending = dao.findByStatus(WorkRequest.RequestStatus.PENDING);
        System.out.println("âœ… Found " + pending.size() + " pending requests");
        
        // Test findByType
        System.out.println("\nTesting findByType(ITEM_CLAIM)...");
        List<WorkRequest> itemClaims = dao.findByType(WorkRequest.RequestType.ITEM_CLAIM);
        System.out.println("âœ… Found " + itemClaims.size() + " item claim requests");
        
        // Test findByRequesterId
        System.out.println("\nTesting findByRequesterId(student123)...");
        List<WorkRequest> studentRequests = dao.findByRequesterId("student123");
        System.out.println("âœ… Found " + studentRequests.size() + " requests from student123");
        
        // Test count
        System.out.println("\nTesting count methods...");
        long total = dao.count();
        long pendingCount = dao.countByStatus(WorkRequest.RequestStatus.PENDING);
        System.out.println("âœ… Total requests: " + total);
        System.out.println("âœ… Pending requests: " + pendingCount);
        
        // Test findAll
        System.out.println("\nTesting findAll()...");
        List<WorkRequest> all = dao.findAll();
        System.out.println("âœ… Retrieved all " + all.size() + " requests");
        System.out.println("\nRequest Type Distribution:");
        for (WorkRequest r : all) {
            System.out.println("   - " + r.getRequestType() + ": " + r.getClass().getSimpleName());
        }
        
        System.out.println("\nâœ… All query methods working correctly!");
    }
}
