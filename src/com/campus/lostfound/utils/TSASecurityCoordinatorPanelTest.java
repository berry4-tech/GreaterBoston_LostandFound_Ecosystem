package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.panels.TSASecurityCoordinatorPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Standalone test for TSASecurityCoordinatorPanel.
 * Tests the complete TSA Security Coordinator interface.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class TSASecurityCoordinatorPanelTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TSASecurityCoordinatorPanel Standalone Test");
        System.out.println("========================================\n");
        
        // Apply emoji font globally
        UIConstants.applyGlobalEmojiFont();
        
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set system look and feel: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Find or create a TSA Security Coordinator user
                User tsaCoordinator = findOrCreateTSACoordinator();
                
                if (tsaCoordinator == null) {
                    JOptionPane.showMessageDialog(null,
                        "Could not find or create TSA Security Coordinator user.\n" +
                        "Please ensure the database has airport enterprise data.",
                        "Test Setup Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                System.out.println("\n✅ Using TSA Security Coordinator: " + tsaCoordinator.getFullName());
                System.out.println("   Email: " + tsaCoordinator.getEmail());
                System.out.println("   Role: " + tsaCoordinator.getRole().getDisplayName());
                System.out.println("   Enterprise ID: " + tsaCoordinator.getEnterpriseId());
                System.out.println("   Organization ID: " + tsaCoordinator.getOrganizationId());
                
                // Create the panel
                TSASecurityCoordinatorPanel panel = new TSASecurityCoordinatorPanel(tsaCoordinator);
                
                // Create frame
                JFrame frame = new JFrame("TSA Security Coordinator Panel - Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1400, 900);
                frame.setLocationRelativeTo(null);
                
                frame.add(panel);
                frame.setVisible(true);
                
                System.out.println("\n✅ TSASecurityCoordinatorPanel launched successfully!");
                System.out.println("\n========================================");
                System.out.println("Test Interface Features:");
                System.out.println("========================================");
                System.out.println("Tab 1: Dashboard");
                System.out.println("  - Security checkpoint item metrics");
                System.out.println("  - Federal compliance status");
                System.out.println("  - Security alerts");
                System.out.println("  - Quick action buttons");
                System.out.println("");
                System.out.println("Tab 2: Checkpoint Items");
                System.out.println("  - Register items from TSA checkpoints");
                System.out.println("  - Prohibited item classification");
                System.out.println("  - Chain of custody documentation");
                System.out.println("  - Transfer to Airport L&F option");
                System.out.println("");
                System.out.println("Tab 3: High-Value Security");
                System.out.println("  - Items over $500 threshold");
                System.out.println("  - Serial number verification");
                System.out.println("  - Police database check (simulated)");
                System.out.println("  - Secure storage assignment");
                System.out.println("");
                System.out.println("Tab 4: Federal Compliance");
                System.out.println("  - Daily compliance checklist");
                System.out.println("  - Prohibited items log");
                System.out.println("  - Disposal documentation");
                System.out.println("  - Audit trail viewer");
                System.out.println("");
                System.out.println("Tab 5: Owner Identification");
                System.out.println("  - Passenger/owner search");
                System.out.println("  - Flight manifest access (simulated)");
                System.out.println("  - ID verification process");
                System.out.println("  - Contact attempt log");
                System.out.println("");
                System.out.println("Tab 6: Coordination");
                System.out.println("  - Police coordination queue");
                System.out.println("  - Airport L&F transfers");
                System.out.println("  - Incident reports management");
                System.out.println("  - Emergency contacts");
                System.out.println("========================================\n");
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Error launching panel: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private static User findOrCreateTSACoordinator() {
        MongoUserDAO userDAO = new MongoUserDAO();
        MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
        MongoOrganizationDAO organizationDAO = new MongoOrganizationDAO();
        
        // Try to find existing TSA Security Coordinator
        List<User> users = userDAO.findAll();
        for (User user : users) {
            if (user.getRole() == User.UserRole.TSA_SECURITY_COORDINATOR) {
                System.out.println("Found existing TSA Security Coordinator: " + user.getFullName());
                return user;
            }
        }
        
        System.out.println("No TSA Security Coordinator found, creating test user...");
        
        // Find Logan Airport enterprise
        Enterprise airportEnterprise = null;
        for (Enterprise e : enterpriseDAO.findAll()) {
            if (e.getType() == Enterprise.EnterpriseType.AIRPORT ||
                e.getName().toLowerCase().contains("logan") ||
                e.getName().toLowerCase().contains("airport")) {
                airportEnterprise = e;
                break;
            }
        }
        
        if (airportEnterprise == null) {
            System.out.println("Warning: No airport enterprise found, using first available");
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            if (!enterprises.isEmpty()) {
                airportEnterprise = enterprises.get(0);
            }
        }
        
        // Find TSA organization
        Organization tsaOrg = null;
        if (airportEnterprise != null) {
            for (Organization org : organizationDAO.findAll()) {
                if (org.getEnterpriseId().equals(airportEnterprise.getEnterpriseId())) {
                    if (org.getName().toLowerCase().contains("tsa") ||
                        org.getName().toLowerCase().contains("security")) {
                        tsaOrg = org;
                        break;
                    }
                }
            }
            
            // If no TSA org found, use first org for this enterprise
            if (tsaOrg == null) {
                for (Organization org : organizationDAO.findAll()) {
                    if (org.getEnterpriseId().equals(airportEnterprise.getEnterpriseId())) {
                        tsaOrg = org;
                        break;
                    }
                }
            }
        }
        
        // Create test TSA user
        User tsaCoordinator = new User(
            "tsa.coordinator.test@logan.airport.gov",
            "Thomas",
            "SecurityOfficer",
            User.UserRole.TSA_SECURITY_COORDINATOR
        );
        
        if (airportEnterprise != null) {
            tsaCoordinator.setEnterpriseId(airportEnterprise.getEnterpriseId());
        }
        
        if (tsaOrg != null) {
            tsaCoordinator.setOrganizationId(tsaOrg.getOrganizationId());
        }
        
        // Save to database
        try {
            String visibleId = userDAO.create(tsaCoordinator, "password123");
            System.out.println("Created test TSA Security Coordinator with ID: " + visibleId);
            
            // Retrieve the saved user to get the userId
            Optional<User> savedUser = userDAO.findByEmail(tsaCoordinator.getEmail());
            if (savedUser.isPresent()) {
                return savedUser.get();
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not save user to database: " + e.getMessage());
        }
        
        return tsaCoordinator;
    }
}
