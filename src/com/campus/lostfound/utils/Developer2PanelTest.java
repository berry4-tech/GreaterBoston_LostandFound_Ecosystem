package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.panels.*;
import javax.swing.*;
import java.awt.*;

/**
 * Quick test harness for Developer 2's role-specific panels.
 * Run this to test each panel individually without going through login.
 * 
 * Usage: Run main() and select which panel to test from the menu.
 */
public class Developer2PanelTest {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Apply global emoji font fix
            UIConstants.applyGlobalEmojiFont();
            
            // Show selection dialog
            String[] options = {
                "CampusCoordinatorPanel",
                "MBTAStationManagerPanel", 
                "StudentUserPanel",
                "PublicTravelerPanel"
            };
            
            String selected = (String) JOptionPane.showInputDialog(
                null,
                "Select panel to test:",
                "Developer 2 Panel Test",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (selected == null) {
                System.exit(0);
            }
            
            // Create test user with appropriate role
            User testUser = createTestUser(selected);
            
            // Create and show panel
            JPanel panel = createPanel(selected, testUser);
            
            if (panel != null) {
                showInFrame(panel, selected, testUser);
            }
        });
    }
    
    private static User createTestUser(String panelName) {
        User user;
        
        switch (panelName) {
            case "CampusCoordinatorPanel" -> {
                user = new User("coordinator@neu.edu", "Campus", "Coordinator", User.UserRole.CAMPUS_COORDINATOR);
                user.setUserId(100);
                user.setEnterpriseId("ENT001"); // Higher Education
                user.setOrganizationId("ORG001"); // NEU Campus Operations
            }
            case "MBTAStationManagerPanel" -> {
                user = new User("manager@mbta.com", "Station", "Manager", User.UserRole.STATION_MANAGER);
                user.setUserId(200);
                user.setEnterpriseId("ENT002"); // MBTA
                user.setOrganizationId("ORG003"); // Station Operations
            }
            case "StudentUserPanel" -> {
                user = new User("student@neu.edu", "Test", "Student", User.UserRole.STUDENT);
                user.setUserId(300);
                user.setEnterpriseId("ENT001");
                user.setOrganizationId("ORG001");
            }
            case "PublicTravelerPanel" -> {
                user = new User("traveler@email.com", "Public", "Traveler", User.UserRole.PUBLIC_TRAVELER);
                user.setUserId(400);
                user.setPhoneNumber("617-555-1234");
            }
            default -> {
                user = new User("test@test.com", "Test", "User", User.UserRole.STUDENT);
                user.setUserId(999);
            }
        }
        
        user.setTrustScore(85.0);
        return user;
    }
    
    private static JPanel createPanel(String panelName, User user) {
        try {
            return switch (panelName) {
                case "CampusCoordinatorPanel" -> new CampusCoordinatorPanel(user);
                case "MBTAStationManagerPanel" -> new MBTAStationManagerPanel(user);
                case "StudentUserPanel" -> new StudentUserPanel(user);
                case "PublicTravelerPanel" -> new PublicTravelerPanel(user);
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error creating panel: " + e.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    private static void showInFrame(JPanel panel, String title, User user) {
        JFrame frame = new JFrame("TEST: " + title + " - " + user.getFullName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        
        // Add info bar at top
        JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        infoBar.setBackground(new Color(52, 73, 94));
        infoBar.add(createInfoLabel("User: " + user.getFullName()));
        infoBar.add(createInfoLabel("Role: " + user.getRole().getDisplayName()));
        infoBar.add(createInfoLabel("Enterprise: " + (user.getEnterpriseId() != null ? user.getEnterpriseId() : "N/A")));
        infoBar.add(createInfoLabel("Org: " + (user.getOrganizationId() != null ? user.getOrganizationId() : "N/A")));
        infoBar.add(createInfoLabel("Trust: " + (int) user.getTrustScore() + "%"));
        
        frame.add(infoBar, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        
        frame.setVisible(true);
        
        System.out.println("=".repeat(60));
        System.out.println("TESTING: " + title);
        System.out.println("User: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
        System.out.println("Enterprise ID: " + user.getEnterpriseId());
        System.out.println("Organization ID: " + user.getOrganizationId());
        System.out.println("=".repeat(60));
    }
    
    private static JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }
}
