package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.ui.panels.AdminPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Test class for the Enhanced AdminPanel (Part 10 of Developer 4)
 * 
 * Tests all new tabs:
 * 1. Dashboard - Basic stats and recent activity
 * 2. System Analytics - Charts and KPI gauges
 * 3. Enterprise Management - CRUD operations
 * 4. User Management - Search, role management, trust scores
 * 5. Advanced Reports - Report generation and export
 * 6. System Health - Database status and alerts
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnhancedAdminPanelTest {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("ENHANCED ADMIN PANEL TEST - PART 10");
        System.out.println("Developer 4 - Cross-Enterprise Integration Lead");
        System.out.println("=".repeat(60));
        System.out.println();

        // Test 1: Verify DAOs are accessible
        System.out.println("TEST 1: Verifying DAO Connectivity...");
        testDAOConnectivity();
        
        // Test 2: Verify Analytics Service
        System.out.println("\nTEST 2: Verifying Analytics Service...");
        testAnalyticsService();
        
        // Test 3: Launch GUI Test
        System.out.println("\nTEST 3: Launching Enhanced Admin Panel GUI...");
        launchGUITest();
    }

    private static void testDAOConnectivity() {
        try {
            MongoItemDAO itemDAO = new MongoItemDAO();
            MongoUserDAO userDAO = new MongoUserDAO();
            MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
            MongoOrganizationDAO organizationDAO = new MongoOrganizationDAO();
            MongoWorkRequestDAO workRequestDAO = new MongoWorkRequestDAO();
            MongoBuildingDAO buildingDAO = new MongoBuildingDAO();
            
            List<Item> items = itemDAO.findAll();
            List<User> users = userDAO.findAll();
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            List<Organization> organizations = organizationDAO.findAll();
            
            System.out.println("  [PASS] Items in database: " + items.size());
            System.out.println("  [PASS] Users in database: " + users.size());
            System.out.println("  [PASS] Enterprises in database: " + enterprises.size());
            System.out.println("  [PASS] Organizations in database: " + organizations.size());
            System.out.println("  [PASS] Buildings in database: " + buildingDAO.count());
            System.out.println("  [PASS] Work Requests in database: " + workRequestDAO.findAll().size());
            
            // Print enterprise names
            System.out.println("\n  Enterprises:");
            for (Enterprise ent : enterprises) {
                System.out.println("    - " + ent.getName() + " (" + ent.getType() + ")");
            }
            
        } catch (Exception e) {
            System.out.println("  [FAIL] DAO connectivity error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAnalyticsService() {
        try {
            com.campus.lostfound.services.AnalyticsService analyticsService = 
                new com.campus.lostfound.services.AnalyticsService();
            
            // Test basic analytics
            long totalItems = analyticsService.getTotalItemCount();
            long totalUsers = analyticsService.getTotalUserCount();
            double recoveryRate = analyticsService.getRecoveryRate();
            double slaCompliance = analyticsService.getSLAComplianceRate();
            
            System.out.println("  [PASS] Total Items: " + totalItems);
            System.out.println("  [PASS] Total Users: " + totalUsers);
            System.out.println("  [PASS] Recovery Rate: " + String.format("%.1f%%", recoveryRate * 100));
            System.out.println("  [PASS] SLA Compliance: " + String.format("%.1f%%", slaCompliance * 100));
            
            // Test chart data
            var categoryData = analyticsService.getCategoryPieData();
            var statusData = analyticsService.getItemStatusPieData();
            var enterpriseData = analyticsService.getEnterpriseComparisonData();
            
            System.out.println("  [PASS] Category chart data points: " + categoryData.size());
            System.out.println("  [PASS] Status chart data points: " + statusData.size());
            System.out.println("  [PASS] Enterprise comparison data points: " + enterpriseData.size());
            
            // Test weekly trends
            var weeklyTrends = analyticsService.getWeeklyTrends(8);
            System.out.println("  [PASS] Weekly trends: " + weeklyTrends.size() + " weeks");
            
            // Test enterprise stats
            var enterpriseStats = analyticsService.getEnterpriseStats();
            System.out.println("  [PASS] Enterprise stats: " + enterpriseStats.size() + " enterprises");
            
            // Test network effect
            var networkEffect = analyticsService.getNetworkEffectMetrics();
            System.out.println("  [PASS] Network effect improvement: " + 
                String.format("%.1f%%", networkEffect.getImprovementPercentage()));
            
            // Test alerts
            var alerts = analyticsService.getAlerts();
            System.out.println("  [PASS] System alerts: " + alerts.size());
            for (var alert : alerts) {
                System.out.println("    - [" + alert.level + "] " + alert.title);
            }
            
            // Test executive summary
            var summary = analyticsService.getExecutiveSummary();
            System.out.println("  [PASS] Executive Summary generated successfully");
            
        } catch (Exception e) {
            System.out.println("  [FAIL] Analytics service error: " + e.getMessage());
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
            
            // Create test user with ADMIN role
            User adminUser = createTestAdminUser();
            
            // Create and show the enhanced admin panel
            JFrame frame = new JFrame("Enhanced Admin Panel Test - Part 10");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            
            // Add the enhanced admin panel
            AdminPanel adminPanel = new AdminPanel(adminUser);
            frame.add(adminPanel);
            
            frame.setVisible(true);
            
            System.out.println("  [PASS] Enhanced Admin Panel launched successfully!");
            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("GUI TEST INSTRUCTIONS:");
            System.out.println("=".repeat(60));
            System.out.println("1. Dashboard Tab:");
            System.out.println("   - Verify statistics cards show data");
            System.out.println("   - Check Recent Items, Top Contributors, Building Activity tables");
            System.out.println();
            System.out.println("2. System Analytics Tab:");
            System.out.println("   - Check KPI gauges (Recovery Rate, SLA Compliance, Network Effect)");
            System.out.println("   - Test different chart types in dropdown");
            System.out.println("   - Verify Weekly Trends table");
            System.out.println();
            System.out.println("3. Enterprise Management Tab:");
            System.out.println("   - View enterprises and organizations");
            System.out.println("   - Test Add/Edit/Delete buttons");
            System.out.println();
            System.out.println("4. User Management Tab:");
            System.out.println("   - Search users by name or email");
            System.out.println("   - Filter by role and enterprise");
            System.out.println("   - Test Change Role, Adjust Trust Score, View Activity buttons");
            System.out.println();
            System.out.println("5. Advanced Reports Tab:");
            System.out.println("   - Select different report types");
            System.out.println("   - Generate preview");
            System.out.println("   - Test export to CSV");
            System.out.println();
            System.out.println("6. System Health Tab:");
            System.out.println("   - Verify database connection status");
            System.out.println("   - Check collection statistics");
            System.out.println("   - Review system log and alerts");
            System.out.println("=".repeat(60));
        });
    }

    private static User createTestAdminUser() {
        User admin = new User();
        admin.setUserId(99999);
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setEmail("admin@test.com");
        admin.setRole(UserRole.SYSTEM_ADMIN);
        admin.setTrustScore(100.0);
        admin.setActive(true);
        return admin;
    }
}
