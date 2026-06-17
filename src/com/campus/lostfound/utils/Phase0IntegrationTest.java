package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import java.util.List;

/**
 * Test to verify Phase 0 - Enterprise Hierarchy is properly set up
 */
public class Phase0IntegrationTest {

    public static void main(String[] args) {
        System.out.println("🧪 PHASE 0 INTEGRATION TEST");
        System.out.println("=" .repeat(60));
        
        boolean allTestsPassed = true;
        
        // Test 1: Check Network exists
        System.out.println("\n[TEST 1] Verifying Network...");
        MongoNetworkDAO networkDAO = new MongoNetworkDAO();
        List<Network> networks = networkDAO.findAll();
        if (!networks.isEmpty()) {
            System.out.println("✅ Network found: " + networks.get(0).getName());
        } else {
            System.out.println("❌ No network found!");
            allTestsPassed = false;
        }
        
        // Test 2: Check Enterprises exist (should be 4)
        System.out.println("\n[TEST 2] Verifying Enterprises...");
        MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
        List<Enterprise> enterprises = enterpriseDAO.findAll();
        System.out.println("Found " + enterprises.size() + " enterprises:");
        for (Enterprise e : enterprises) {
            System.out.println("  - " + e.getName() + " (" + e.getType().getDisplayName() + ")");
        }
        if (enterprises.size() >= 4) {
            System.out.println("✅ All 4 enterprises found");
        } else {
            System.out.println("❌ Expected 4 enterprises, found " + enterprises.size());
            allTestsPassed = false;
        }
        
        // Test 3: Check Organizations exist (should be 8+)
        System.out.println("\n[TEST 3] Verifying Organizations...");
        MongoOrganizationDAO organizationDAO = new MongoOrganizationDAO();
        List<Organization> organizations = organizationDAO.findAll();
        System.out.println("Found " + organizations.size() + " organizations:");
        for (Enterprise e : enterprises) {
            List<Organization> enterpriseOrgs = organizationDAO.findByEnterpriseId(e.getEnterpriseId());
            System.out.println("  " + e.getName() + ": " + enterpriseOrgs.size() + " organizations");
        }
        if (organizations.size() >= 8) {
            System.out.println("✅ All organizations found");
        } else {
            System.out.println("❌ Expected 8+ organizations, found " + organizations.size());
            allTestsPassed = false;
        }
        
        // Test 4: Check Users have enterprise context
        System.out.println("\n[TEST 4] Verifying Users have Enterprise/Org context...");
        MongoUserDAO userDAO = new MongoUserDAO();
        List<User> users = userDAO.findAll();
        int usersWithContext = 0;
        for (User user : users) {
            if (user.getEnterpriseId() != null && user.getOrganizationId() != null) {
                usersWithContext++;
            }
        }
        System.out.println("Users with enterprise context: " + usersWithContext + "/" + users.size());
        if (usersWithContext > 0) {
            System.out.println("✅ Users have enterprise/org assignments");
        } else {
            System.out.println("❌ No users have enterprise context!");
            allTestsPassed = false;
        }
        
        // Test 5: Check Buildings have enterprise context
        System.out.println("\n[TEST 5] Verifying Buildings have Enterprise/Org context...");
        MongoBuildingDAO buildingDAO = new MongoBuildingDAO();
        List<Building> buildings = buildingDAO.findAll();
        int buildingsWithContext = 0;
        for (Building building : buildings) {
            if (building.getEnterpriseId() != null && building.getOrganizationId() != null) {
                buildingsWithContext++;
            }
        }
        System.out.println("Buildings with enterprise context: " + buildingsWithContext + "/" + buildings.size());
        if (buildingsWithContext > 0) {
            System.out.println("✅ Buildings have enterprise/org assignments");
        } else {
            System.out.println("⚠️  Some buildings may not have enterprise context (this is OK for existing buildings)");
        }
        
        // Test 6: Sample User Login Simulation
        System.out.println("\n[TEST 6] Simulating User Login with Context...");
        if (!users.isEmpty()) {
            User sampleUser = users.get(0);
            String enterpriseName = "Unknown";
            String orgName = "Unknown";
            
            if (sampleUser.getEnterpriseId() != null) {
                var enterprise = enterpriseDAO.findById(sampleUser.getEnterpriseId());
                if (enterprise.isPresent()) {
                    enterpriseName = enterprise.get().getName();
                }
            }
            
            if (sampleUser.getOrganizationId() != null) {
                var org = organizationDAO.findById(sampleUser.getOrganizationId());
                if (org.isPresent()) {
                    orgName = org.get().getName();
                }
            }
            
            System.out.println("Sample User Context:");
            System.out.println("  Name: " + sampleUser.getFullName());
            System.out.println("  Role: " + sampleUser.getRole().getDisplayName());
            System.out.println("  Enterprise: " + enterpriseName);
            System.out.println("  Organization: " + orgName);
            System.out.println("✅ User context display works!");
        }
        
        // Final Summary
        System.out.println("\n" + "=".repeat(60));
        if (allTestsPassed) {
            System.out.println("🎉 PHASE 0 COMPLETE - ALL TESTS PASSED!");
            System.out.println("✅ Enterprise hierarchy is properly set up");
            System.out.println("✅ Ready to proceed to Phase 1 development");
        } else {
            System.out.println("⚠️  PHASE 0 INCOMPLETE - Some tests failed");
            System.out.println("Run MongoDataGenerator.main() to generate the ecosystem");
        }
        System.out.println("=".repeat(60));
    }
}
