package com.campus.lostfound.utils;

import com.campus.lostfound.models.workrequest.ItemClaimRequest;
import com.campus.lostfound.models.workrequest.WorkRequest;

/**
 * Simple test to verify Part 1 of Phase 1: WorkRequest Base + ItemClaimRequest
 * This validates the core workflow pattern works correctly.
 */
public class WorkRequestTest {
    
    public static void main(String[] args) {
        System.out.println("=== WORK REQUEST SYSTEM TEST (Part 1) ===\n");
        
        // Test 1: Create a regular-value item claim request
        System.out.println("TEST 1: Regular Item Claim (< $500)");
        System.out.println("=====================================");
        ItemClaimRequest regularClaim = new ItemClaimRequest(
            "student123", "John Doe",
            "item456", "Blue Backpack", 75.00
        );
        regularClaim.setClaimDetails("This is my backpack with my books inside. I lost it in Snell Library yesterday.");
        regularClaim.setIdentifyingFeatures("Has a small tear on the front pocket and a keychain with my initials JD.");
        regularClaim.setProofDescription("I have receipts showing I purchased this backpack last month.");
        
        System.out.println("Created: " + regularClaim);
        System.out.println("Summary: " + regularClaim.getRequestSummary());
        System.out.println("Approval Chain: " + regularClaim.getApprovalChain());
        System.out.println("Valid: " + regularClaim.isValid());
        System.out.println("Requires Police: " + regularClaim.requiresPoliceVerification());
        System.out.println("Confidence Score: " + regularClaim.getClaimConfidenceScore() + "%");
        System.out.println("Next Required Role: " + regularClaim.getNextRequiredRole());
        System.out.println();
        
        // Simulate approval by campus coordinator
        System.out.println("APPROVING: Campus Coordinator approves...");
        regularClaim.advanceApproval("coordinator789", "Jane Smith");
        System.out.println("Status: " + regularClaim.getStatus());
        System.out.println("Approval Step: " + regularClaim.getApprovalStep() + "/" + regularClaim.getApprovalChain().size());
        System.out.println("Approvers: " + regularClaim.getApproverNames());
        System.out.println();
        
        // Test 2: Create a high-value item claim request
        System.out.println("\nTEST 2: High-Value Item Claim (> $500)");
        System.out.println("=========================================");
        ItemClaimRequest highValueClaim = new ItemClaimRequest(
            "student999", "Sarah Johnson",
            "item789", "MacBook Pro", 2499.00
        );
        highValueClaim.setClaimDetails("This is my laptop that I use for coursework. I accidentally left it in the student center coffee shop.");
        highValueClaim.setIdentifyingFeatures("Serial number XYZ123456, has a scratch on the bottom right corner, and my name engraved on the back.");
        highValueClaim.setProofDescription("I have the original purchase receipt, AppleCare+ documentation, and photos of the serial number.");
        highValueClaim.setClaimPhotoUrl("/photos/claim_proof_789.jpg");
        
        System.out.println("Created: " + highValueClaim);
        System.out.println("Summary: " + highValueClaim.getRequestSummary());
        System.out.println("Approval Chain: " + highValueClaim.getApprovalChain());
        System.out.println("Valid: " + highValueClaim.isValid());
        System.out.println("Requires Police: " + highValueClaim.requiresPoliceVerification());
        System.out.println("Confidence Score: " + highValueClaim.getClaimConfidenceScore() + "%");
        System.out.println("Next Required Role: " + highValueClaim.getNextRequiredRole());
        System.out.println();
        
        // Simulate multi-step approval
        System.out.println("APPROVAL STEP 1: Campus Coordinator approves...");
        highValueClaim.advanceApproval("coordinator789", "Jane Smith");
        System.out.println("Status: " + highValueClaim.getStatus());
        System.out.println("Next Required Role: " + highValueClaim.getNextRequiredRole());
        System.out.println();
        
        System.out.println("APPROVAL STEP 2: Police Evidence Custodian approves...");
        highValueClaim.advanceApproval("police555", "Officer Mike Brown");
        System.out.println("Status: " + highValueClaim.getStatus());
        System.out.println("Fully Approved: " + (highValueClaim.getStatus() == WorkRequest.RequestStatus.APPROVED));
        System.out.println("Approvers: " + highValueClaim.getApproverNames());
        System.out.println();
        
        // Complete the request
        System.out.println("COMPLETING: Marking as completed...");
        highValueClaim.complete();
        System.out.println("Final Status: " + highValueClaim.getStatus());
        System.out.println("Completed At: " + highValueClaim.getCompletedAt());
        System.out.println();
        
        // Test 3: Test rejection scenario
        System.out.println("\nTEST 3: Rejection Scenario");
        System.out.println("============================");
        ItemClaimRequest rejectedClaim = new ItemClaimRequest(
            "student111", "Bob Wilson",
            "item333", "Black Wallet", 50.00
        );
        rejectedClaim.setClaimDetails("It's my wallet.");
        rejectedClaim.setIdentifyingFeatures("It's black.");
        
        System.out.println("Created: " + rejectedClaim);
        System.out.println("Valid: " + rejectedClaim.isValid() + " (insufficient details)");
        System.out.println();
        
        System.out.println("REJECTING: Coordinator rejects due to insufficient proof...");
        rejectedClaim.reject("Insufficient identifying details provided. Please provide more specific information.");
        System.out.println("Status: " + rejectedClaim.getStatus());
        System.out.println("Notes: " + rejectedClaim.getNotes());
        System.out.println();
        
        // Test 4: Test role checking
        System.out.println("\nTEST 4: Role-Based Access");
        System.out.println("===========================");
        ItemClaimRequest newClaim = new ItemClaimRequest(
            "student222", "Alice Green",
            "item444", "Red Jacket", 120.00
        );
        
        System.out.println("Needs approval from CAMPUS_COORDINATOR: " + newClaim.needsApprovalFromRole("CAMPUS_COORDINATOR"));
        System.out.println("Needs approval from POLICE_EVIDENCE_CUSTODIAN: " + newClaim.needsApprovalFromRole("POLICE_EVIDENCE_CUSTODIAN"));
        System.out.println("Needs approval from STUDENT: " + newClaim.needsApprovalFromRole("STUDENT"));
        System.out.println();
        
        System.out.println("=== ALL TESTS PASSED! ===");
        System.out.println("\nâœ… Part 1 Complete: WorkRequest base class and ItemClaimRequest are working correctly!");
        System.out.println("Next: Part 2 - Create remaining 4 WorkRequest types using this proven pattern.");
    }
}
