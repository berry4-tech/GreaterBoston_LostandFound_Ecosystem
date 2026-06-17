package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.ui.MainDashboard;
import com.campus.lostfound.ui.UIConstants;
import javax.swing.*;
import java.util.List;

/**
 * Quick launcher to test Enterprise Admin Panel.
 * Creates a mock enterprise admin user and opens the MainDashboard.
 * 
 * Run this to see the Enterprise Admin Panel without going through login.
 */
public class TestEnterpriseAdminLauncher {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIConstants.applyGlobalEmojiFont();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Find an enterprise to test with
            MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            
            if (enterprises.isEmpty()) {
                JOptionPane.showMessageDialog(null, 
                    "No enterprises found in database!\n\n" +
                    "Please run your test data generator first to create enterprises.",
                    "No Data", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Let user pick which enterprise to test
            String[] options = enterprises.stream()
                .map(e -> e.getName() + " (" + e.getType() + ")")
                .toArray(String[]::new);
            
            String selected = (String) JOptionPane.showInputDialog(
                null,
                "Select an enterprise to test as admin:",
                "Select Enterprise",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (selected == null) {
                System.out.println("No enterprise selected. Exiting.");
                return;
            }
            
            // Find the selected enterprise
            int selectedIndex = java.util.Arrays.asList(options).indexOf(selected);
            Enterprise testEnterprise = enterprises.get(selectedIndex);
            
            System.out.println("=== Testing Enterprise Admin ===");
            System.out.println("Enterprise: " + testEnterprise.getName());
            System.out.println("Enterprise ID: " + testEnterprise.getEnterpriseId());
            System.out.println("Type: " + testEnterprise.getType());
            System.out.println();
            
            // Let user pick which admin role to test with
            UserRole[] adminRoles = {
                UserRole.UNIVERSITY_ADMIN,
                UserRole.MBTA_ADMIN,
                UserRole.AIRPORT_ADMIN,
                UserRole.POLICE_ADMIN
            };
            
            String[] roleOptions = {
                "University Administrator",
                "MBTA Administrator", 
                "Airport Administrator",
                "Police Administrator"
            };
            
            String selectedRole = (String) JOptionPane.showInputDialog(
                null,
                "Select admin role to test with:",
                "Select Role",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roleOptions,
                roleOptions[0]
            );
            
            if (selectedRole == null) {
                System.out.println("No role selected. Exiting.");
                return;
            }
            
            int roleIndex = java.util.Arrays.asList(roleOptions).indexOf(selectedRole);
            UserRole testRole = adminRoles[roleIndex];
            
            // Create a mock enterprise admin user
            User enterpriseAdmin = new User(
                "admin@" + testEnterprise.getName().toLowerCase().replace(" ", "") + ".edu",
                "Enterprise",
                "Administrator",
                testRole  // Use selected role
            );
            enterpriseAdmin.setEnterpriseId(testEnterprise.getEnterpriseId());
            enterpriseAdmin.setTrustScore(100.0);
            
            System.out.println("Created test user:");
            System.out.println("  Email: " + enterpriseAdmin.getEmail());
            System.out.println("  Role: " + enterpriseAdmin.getRole());
            System.out.println("  Enterprise ID: " + enterpriseAdmin.getEnterpriseId());
            System.out.println();
            System.out.println("Opening MainDashboard with Enterprise Admin Panel...");
            
            // Open the main dashboard - it will show EnterpriseAdminPanel for this user
            MainDashboard dashboard = new MainDashboard(enterpriseAdmin);
            dashboard.setVisible(true);
            
            // Auto-navigate to admin panel
            SwingUtilities.invokeLater(() -> {
                dashboard.showPanel("admin");
            });
        });
    }
}
