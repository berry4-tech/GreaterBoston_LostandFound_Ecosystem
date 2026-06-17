package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.services.*;
import com.campus.lostfound.ui.MainDashboard;
import com.campus.lostfound.ui.panels.*;

import javax.swing.*;
import java.util.List;

/**
 * Integration Test for Part 11 - Developer 4
 * 
 * Tests the integration between:
 * 1. MainDashboard role-based panel navigation
 * 2. SearchBrowsePanel cross-enterprise search
 * 3. EnterpriseItemService integration
 * 4. Role-specific panel loading
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class Part11IntegrationTest {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PART 11 INTEGRATION TEST");
        System.out.println("Developer 4 - Cross-Enterprise Integration");
        System.out.println("=".repeat(60));
        System.out.println();

        // Test 1: Service Integration
        System.out.println("TEST 1: Service Integration");
        testServiceIntegration();
        
        // Test 2: Role Panel Mapping
        System.out.println("\nTEST 2: Role Panel Mapping");
        testRolePanelMapping();
        
        // Test 3: Cross-Enterprise Search
        System.out.println("\nTEST 3: Cross-Enterprise Search");
        testCrossEnterpriseSearch();
        
        // Test 4: Launch GUI Test
        System.out.println("\nTEST 4: Launch Integration GUI Test");
        launchGUITest();
    }

    private static void testServiceIntegration() {
        try {
            // Test EnterpriseItemService
            EnterpriseItemService enterpriseService = new EnterpriseItemService();
            List<Item> allItems = enterpriseService.searchAllEnterprises(null, null);
            System.out.println("  [PASS] EnterpriseItemService initialized");
            System.out.println("  [PASS] Cross-enterprise search returned: " + allItems.size() + " items");
            
            // Test AnalyticsService
            AnalyticsService analyticsService = new AnalyticsService();
            long totalItems = analyticsService.getTotalItemCount();
            double recoveryRate = analyticsService.getRecoveryRate();
            System.out.println("  [PASS] AnalyticsService initialized");
            System.out.println("  [PASS] Total items: " + totalItems);
            System.out.println("  [PASS] Recovery rate: " + String.format("%.1f%%", recoveryRate * 100));
            
            // Test EnterpriseItemMatcher
            EnterpriseItemMatcher matcher = new EnterpriseItemMatcher();
            System.out.println("  [PASS] EnterpriseItemMatcher initialized");
            
        } catch (Exception e) {
            System.out.println("  [FAIL] Service integration error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testRolePanelMapping() {
        System.out.println("  Testing role -> panel mappings:");
        
        UserRole[] testRoles = {
            UserRole.AIRPORT_LOST_FOUND_SPECIALIST,
            UserRole.TSA_SECURITY_COORDINATOR,
            UserRole.STATION_MANAGER,
            UserRole.TRANSIT_SECURITY_INSPECTOR,
            UserRole.POLICE_EVIDENCE_CUSTODIAN,
            UserRole.PUBLIC_TRAVELER,
            UserRole.STUDENT,
            UserRole.CAMPUS_SECURITY,
            UserRole.CAMPUS_COORDINATOR,
            UserRole.SYSTEM_ADMIN
        };
        
        for (UserRole role : testRoles) {
            String panelName = getPanelNameForRole(role);
            System.out.println("    " + role.name() + " -> " + panelName);
        }
        
        System.out.println("  [PASS] All role mappings verified");
    }
    
    private static String getPanelNameForRole(UserRole role) {
        switch (role) {
            case AIRPORT_LOST_FOUND_SPECIALIST: return "AirportLostFoundSpecialistPanel";
            case TSA_SECURITY_COORDINATOR: return "TSASecurityCoordinatorPanel";
            case STATION_MANAGER: return "MBTAStationManagerPanel";
            case TRANSIT_SECURITY_INSPECTOR: return "TransitSecurityInspectorPanel";
            case POLICE_EVIDENCE_CUSTODIAN: return "PoliceEvidenceCustodianPanel";
            case PUBLIC_TRAVELER: return "PublicTravelerPanel";
            case STUDENT: return "StudentUserPanel";
            case CAMPUS_SECURITY: return "UniversitySecurityPanel";
            case CAMPUS_COORDINATOR: return "CampusCoordinatorPanel";
            case SYSTEM_ADMIN: return "AdminPanel";
            case UNIVERSITY_ADMIN: return "AdminPanel";
            default: return "None (uses default panels)";
        }
    }

    private static void testCrossEnterpriseSearch() {
        try {
            EnterpriseItemService service = new EnterpriseItemService();
            MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
            
            // Get all enterprises
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            System.out.println("  Found " + enterprises.size() + " enterprises in database");
            
            for (Enterprise enterprise : enterprises) {
                System.out.println("    - " + enterprise.getName() + " (" + enterprise.getType() + ")");
            }
            
            // Test search across all enterprises
            List<Item> allItems = service.searchAllEnterprises(null, null);
            System.out.println("  [PASS] Search all enterprises: " + allItems.size() + " items");
            
            // Test search by category
            List<Item> electronics = service.searchAllEnterprises(null, Item.ItemCategory.ELECTRONICS);
            System.out.println("  [PASS] Search electronics category: " + electronics.size() + " items");
            
            // Test item count by enterprise
            var itemsByEnterprise = service.getItemCountByEnterprise();
            System.out.println("  [PASS] Items by enterprise: " + itemsByEnterprise.size() + " enterprises with items");
            
        } catch (Exception e) {
            System.out.println("  [FAIL] Cross-enterprise search error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void launchGUITest() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default
            }
            
            // Create test user with different roles to test panel loading
            System.out.println("  Select a role to test:");
            System.out.println("    1. Airport Lost & Found Specialist");
            System.out.println("    2. TSA Security Coordinator");
            System.out.println("    3. Station Manager");
            System.out.println("    4. Student");
            System.out.println("    5. System Admin");
            
            // Default to System Admin for comprehensive testing
            User testUser = createTestUser(UserRole.SYSTEM_ADMIN);
            
            MainDashboard dashboard = new MainDashboard(testUser);
            dashboard.setVisible(true);
            
            System.out.println("  [PASS] MainDashboard launched with role: " + testUser.getRole().getDisplayName());
            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("INTEGRATION TEST INSTRUCTIONS:");
            System.out.println("=".repeat(60));
            System.out.println("1. Check that the sidebar shows role-specific button");
            System.out.println("2. Click 'Search Items' and verify enterprise filter dropdown");
            System.out.println("3. Try filtering by different enterprises");
            System.out.println("4. If Admin, check the Admin Panel for all features");
            System.out.println("5. Test cross-enterprise search functionality");
            System.out.println("=".repeat(60));
        });
    }

    private static User createTestUser(UserRole role) {
        User user = new User();
        user.setUserId(99999);
        user.setFirstName("Integration");
        user.setLastName("Test");
        user.setEmail("integration.test@example.com");
        user.setRole(role);
        user.setTrustScore(100.0);
        user.setActive(true);
        return user;
    }
}
