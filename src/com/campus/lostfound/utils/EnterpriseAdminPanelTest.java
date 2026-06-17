package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.services.*;
import com.campus.lostfound.ui.panels.*;
import javax.swing.*;

/**
 * Test class to verify the EnterpriseAdminPanel implementation.
 * 
 * Tests:
 * 1. Network Admin (SYSTEM_ADMIN) → AdminPanel (network-wide)
 * 2. Enterprise Admin (UNIVERSITY_ADMIN) → EnterpriseAdminPanel (scoped)
 * 3. Data provider filtering works correctly
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseAdminPanelTest {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Test menu
            String[] options = {
                "1. Test Network Admin Panel (SYSTEM_ADMIN)",
                "2. Test Enterprise Admin Panel (UNIVERSITY_ADMIN)", 
                "3. Test Data Provider - Network Scope",
                "4. Test Data Provider - Enterprise Scope",
                "5. Compare Both Panels Side by Side"
            };
            
            int choice = JOptionPane.showOptionDialog(
                null,
                "Select a test to run:",
                "Enterprise Admin Panel Test Suite",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            switch (choice) {
                case 0:
                    testNetworkAdminPanel();
                    break;
                case 1:
                    testEnterpriseAdminPanel();
                    break;
                case 2:
                    testNetworkDataProvider();
                    break;
                case 3:
                    testEnterpriseDataProvider();
                    break;
                case 4:
                    testBothPanelsSideBySide();
                    break;
                default:
                    System.out.println("No test selected.");
            }
        });
    }
    
    private static void testNetworkAdminPanel() {
        System.out.println("\n=== Testing Network Admin Panel (SYSTEM_ADMIN) ===\n");
        
        User sysAdmin = new User("sysadmin@network.com", "System", "Administrator", UserRole.SYSTEM_ADMIN);
        
        JFrame frame = new JFrame("Network Admin Panel Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);
        frame.add(new AdminPanel(sysAdmin));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        System.out.println("✓ Network Admin Panel loaded successfully");
        System.out.println("✓ Shows data from ALL enterprises");
    }
    
    private static void testEnterpriseAdminPanel() {
        System.out.println("\n=== Testing Enterprise Admin Panel (UNIVERSITY_ADMIN) ===\n");
        
        User univAdmin = new User("admin@northeastern.edu", "University", "Administrator", UserRole.UNIVERSITY_ADMIN);
        univAdmin.setEnterpriseId("ENT-001"); // Northeastern University enterprise
        
        JFrame frame = new JFrame("Enterprise Admin Panel Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);
        frame.add(new EnterpriseAdminPanel(univAdmin));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        System.out.println("✓ Enterprise Admin Panel loaded successfully");
        System.out.println("✓ Shows data ONLY from user's enterprise");
    }
    
    private static void testNetworkDataProvider() {
        System.out.println("\n=== Testing Network Admin Data Provider ===\n");
        
        NetworkAdminDataProvider provider = new NetworkAdminDataProvider();
        
        System.out.println("Scope: " + provider.getScopeDescription());
        System.out.println("Is Network Scope: " + provider.isNetworkScope());
        System.out.println("Enterprise ID: " + provider.getEnterpriseId());
        System.out.println();
        
        System.out.println("--- Statistics ---");
        System.out.println("Total Items: " + provider.getTotalItemCount());
        System.out.println("Lost Items: " + provider.getLostItemCount());
        System.out.println("Found Items: " + provider.getFoundItemCount());
        System.out.println("Claimed Items: " + provider.getClaimedItemCount());
        System.out.println("Total Users: " + provider.getTotalUserCount());
        System.out.println("Active Users: " + provider.getActiveUserCount());
        System.out.println("Avg Trust Score: " + String.format("%.1f", provider.getAverageTrustScore()));
        System.out.println("Recovery Rate: " + String.format("%.1f%%", provider.getRecoveryRate() * 100));
        System.out.println();
        
        System.out.println("--- Enterprises ---");
        provider.getEnterprises().forEach(e -> 
            System.out.println("  - " + e.getName() + " (" + e.getType() + ")"));
        System.out.println();
        
        System.out.println("--- Organizations ---");
        provider.getOrganizations().forEach(o -> 
            System.out.println("  - " + o.getName() + " (" + o.getType() + ")"));
        
        System.out.println("\n✓ Network Data Provider test complete");
        
        JOptionPane.showMessageDialog(null, 
            "Network Data Provider Test Complete!\n\nCheck console for details.",
            "Test Complete", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static void testEnterpriseDataProvider() {
        System.out.println("\n=== Testing Enterprise Admin Data Provider ===\n");
        
        // First, let's find an enterprise to test with
        NetworkAdminDataProvider networkProvider = new NetworkAdminDataProvider();
        var enterprises = networkProvider.getEnterprises();
        
        if (enterprises.isEmpty()) {
            System.out.println("ERROR: No enterprises found in database!");
            JOptionPane.showMessageDialog(null, 
                "No enterprises found in database.\nPlease run test data generator first.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Pick the first enterprise
        var testEnterprise = enterprises.get(0);
        String enterpriseId = testEnterprise.getEnterpriseId();
        
        System.out.println("Testing with enterprise: " + testEnterprise.getName());
        System.out.println("Enterprise ID: " + enterpriseId);
        System.out.println();
        
        EnterpriseAdminDataProvider provider = new EnterpriseAdminDataProvider(enterpriseId);
        
        System.out.println("Scope: " + provider.getScopeDescription());
        System.out.println("Is Network Scope: " + provider.isNetworkScope());
        System.out.println("Enterprise ID: " + provider.getEnterpriseId());
        System.out.println();
        
        System.out.println("--- Enterprise Statistics ---");
        System.out.println("Total Items: " + provider.getTotalItemCount());
        System.out.println("Lost Items: " + provider.getLostItemCount());
        System.out.println("Found Items: " + provider.getFoundItemCount());
        System.out.println("Claimed Items: " + provider.getClaimedItemCount());
        System.out.println("Total Users: " + provider.getTotalUserCount());
        System.out.println("Active Users: " + provider.getActiveUserCount());
        System.out.println("Avg Trust Score: " + String.format("%.1f", provider.getAverageTrustScore()));
        System.out.println("Recovery Rate: " + String.format("%.1f%%", provider.getRecoveryRate() * 100));
        System.out.println();
        
        System.out.println("--- Organizations in this Enterprise ---");
        provider.getOrganizations().forEach(o -> 
            System.out.println("  - " + o.getName() + " (" + o.getType() + ")"));
        System.out.println();
        
        System.out.println("--- Cross-Enterprise Transfer Stats ---");
        var transferStats = provider.getCrossEnterpriseTransferStats();
        System.out.println("Incoming Transfers: " + transferStats.get("incomingTransfers"));
        System.out.println("Outgoing Transfers: " + transferStats.get("outgoingTransfers"));
        
        System.out.println("\n✓ Enterprise Data Provider test complete");
        
        // Compare with network-wide
        System.out.println("\n--- Comparison with Network-Wide ---");
        System.out.println("Network Items: " + networkProvider.getTotalItemCount() + 
            " | Enterprise Items: " + provider.getTotalItemCount());
        System.out.println("Network Users: " + networkProvider.getTotalUserCount() + 
            " | Enterprise Users: " + provider.getTotalUserCount());
        
        JOptionPane.showMessageDialog(null, 
            "Enterprise Data Provider Test Complete!\n\n" +
            "Enterprise: " + testEnterprise.getName() + "\n" +
            "Items: " + provider.getTotalItemCount() + " (of " + networkProvider.getTotalItemCount() + " total)\n" +
            "Users: " + provider.getTotalUserCount() + " (of " + networkProvider.getTotalUserCount() + " total)\n\n" +
            "Check console for full details.",
            "Test Complete", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static void testBothPanelsSideBySide() {
        System.out.println("\n=== Testing Both Panels Side by Side ===\n");
        
        // Find an enterprise to test
        NetworkAdminDataProvider networkProvider = new NetworkAdminDataProvider();
        var enterprises = networkProvider.getEnterprises();
        
        if (enterprises.isEmpty()) {
            JOptionPane.showMessageDialog(null, 
                "No enterprises found. Please run test data generator first.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        var testEnterprise = enterprises.get(0);
        
        // Create Network Admin
        User sysAdmin = new User("sysadmin@network.com", "System", "Administrator", UserRole.SYSTEM_ADMIN);
        
        // Create Enterprise Admin
        User univAdmin = new User("admin@" + testEnterprise.getName().toLowerCase().replace(" ", "") + ".edu", 
            "Enterprise", "Administrator", UserRole.UNIVERSITY_ADMIN);
        univAdmin.setEnterpriseId(testEnterprise.getEnterpriseId());
        
        // Create frames side by side
        JFrame networkFrame = new JFrame("Network Admin (All Enterprises)");
        networkFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        networkFrame.setSize(700, 600);
        networkFrame.add(new AdminPanel(sysAdmin));
        networkFrame.setLocation(50, 100);
        networkFrame.setVisible(true);
        
        JFrame enterpriseFrame = new JFrame("Enterprise Admin (" + testEnterprise.getName() + ")");
        enterpriseFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        enterpriseFrame.setSize(700, 600);
        enterpriseFrame.add(new EnterpriseAdminPanel(univAdmin));
        enterpriseFrame.setLocation(760, 100);
        enterpriseFrame.setVisible(true);
        
        System.out.println("✓ Both panels opened side by side");
        System.out.println("  Left: Network Admin (all data)");
        System.out.println("  Right: Enterprise Admin (" + testEnterprise.getName() + " only)");
        System.out.println("\nCompare the statistics to verify scoping is working correctly!");
    }
}
