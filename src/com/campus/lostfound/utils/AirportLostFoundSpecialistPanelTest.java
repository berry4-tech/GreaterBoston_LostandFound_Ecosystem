package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.panels.AirportLostFoundSpecialistPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Standalone test for AirportLostFoundSpecialistPanel.
 * Creates a test user with AIRPORT_LOST_FOUND_SPECIALIST role and displays the panel.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class AirportLostFoundSpecialistPanelTest {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("AirportLostFoundSpecialistPanel Test");
        System.out.println("=".repeat(60));
        
        // Apply global emoji font support
        UIConstants.applyGlobalEmojiFont();
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set system look and feel");
        }
        
        // Find or create test user
        User testUser = findOrCreateTestUser();
        
        if (testUser == null) {
            System.err.println("ERROR: Could not find or create test user!");
            return;
        }
        
        System.out.println("\nTest User Details:");
        System.out.println("  Name: " + testUser.getFullName());
        System.out.println("  Email: " + testUser.getEmail());
        System.out.println("  Role: " + testUser.getRole());
        System.out.println("  Enterprise ID: " + testUser.getEnterpriseId());
        System.out.println("  Organization ID: " + testUser.getOrganizationId());
        
        // Create and display the panel
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Airport Lost & Found Specialist Panel Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1400, 900);
                frame.setLocationRelativeTo(null);
                
                AirportLostFoundSpecialistPanel panel = new AirportLostFoundSpecialistPanel(testUser);
                frame.add(panel);
                
                frame.setVisible(true);
                System.out.println("\n✅ Panel displayed successfully!");
                System.out.println("\nTest the following features:");
                System.out.println("  1. Dashboard - Check statistics and alerts");
                System.out.println("  2. Item Intake - Register items with flight info");
                System.out.println("  3. Traveler Search - Search by name/flight/confirmation");
                System.out.println("  4. Delivery Coordination - Create delivery requests");
                System.out.println("  5. Cross-Enterprise Transfer - Send to universities/MBTA");
                System.out.println("  6. Reports - View unclaimed items and export");
                
            } catch (Exception e) {
                System.err.println("ERROR displaying panel: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private static User findOrCreateTestUser() {
        MongoUserDAO userDAO = new MongoUserDAO();
        MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
        MongoOrganizationDAO organizationDAO = new MongoOrganizationDAO();
        
        // Try to find existing airport specialist
        List<User> allUsers = userDAO.findAll();
        for (User user : allUsers) {
            if (user.getRole() == User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST) {
                System.out.println("Found existing airport specialist: " + user.getEmail());
                return user;
            }
        }
        
        System.out.println("No existing airport specialist found. Creating test user...");
        
        // Find Logan Airport enterprise
        String airportEnterpriseId = null;
        String airportOrgId = null;
        
        List<Enterprise> enterprises = enterpriseDAO.findAll();
        for (Enterprise e : enterprises) {
            if (e.getType() == Enterprise.EnterpriseType.AIRPORT || 
                e.getName().toLowerCase().contains("airport") ||
                e.getName().toLowerCase().contains("logan")) {
                airportEnterpriseId = e.getEnterpriseId();
                System.out.println("Found airport enterprise: " + e.getName());
                break;
            }
        }
        
        // If no airport enterprise, try to use any available enterprise
        if (airportEnterpriseId == null && !enterprises.isEmpty()) {
            Enterprise firstEnt = enterprises.get(0);
            airportEnterpriseId = firstEnt.getEnterpriseId();
            System.out.println("Using enterprise: " + firstEnt.getName());
        }
        
        // Find organization within enterprise
        if (airportEnterpriseId != null) {
            List<Organization> orgs = organizationDAO.findAll();
            for (Organization org : orgs) {
                if (airportEnterpriseId.equals(org.getEnterpriseId())) {
                    airportOrgId = org.getOrganizationId();
                    System.out.println("Using organization: " + org.getName());
                    break;
                }
            }
        }
        
        // Create test user - User constructor: (email, firstName, lastName, role)
        User testUser = new User(
            "airport.specialist@logan.com",
            "Airport",
            "Specialist",
            User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST
        );
        
        testUser.setEnterpriseId(airportEnterpriseId);
        testUser.setOrganizationId(airportOrgId);
        
        // Try to save the user - create method requires password as second argument
        try {
            String userId = userDAO.create(testUser, "password123");
            if (userId != null && !userId.isEmpty()) {
                Optional<User> savedUser = userDAO.findByEmail("airport.specialist@logan.com");
                if (savedUser.isPresent()) {
                    System.out.println("Created test user with ID: " + userId);
                    return savedUser.get();
                }
            }
        } catch (Exception e) {
            System.out.println("Could not save test user to database: " + e.getMessage());
        }
        
        // Return unsaved user for display testing
        System.out.println("Returning unsaved test user for display testing");
        return testUser;
    }
}
