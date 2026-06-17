package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Building;
import com.campus.lostfound.ui.panels.StudentUserPanel;
import com.campus.lostfound.dao.MongoUserDAO;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * Test class for StudentUserPanel.
 * Run this directly to test the panel without going through the full login flow.
 */
public class StudentUserPanelTest {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Create test frame
            JFrame frame = new JFrame("StudentUserPanel Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 850);
            frame.setLocationRelativeTo(null);
            
            // Try to load a real student user from database, or create a mock one
            User testUser = loadOrCreateTestUser();
            
            System.out.println("Testing with user: " + testUser.getFullName());
            System.out.println("Role: " + testUser.getRole().getDisplayName());
            System.out.println("Trust Score: " + testUser.getTrustScore());
            System.out.println("Enterprise ID: " + testUser.getEnterpriseId());
            System.out.println("Organization ID: " + testUser.getOrganizationId());
            
            // Create and add the StudentUserPanel
            StudentUserPanel panel = new StudentUserPanel(testUser);
            frame.add(panel);
            
            frame.setVisible(true);
        });
    }
    
    private static User loadOrCreateTestUser() {
        // Try to load an existing student from database
        try {
            MongoUserDAO userDAO = new MongoUserDAO();
            
            // Try to find any student user
            for (User user : userDAO.findAll()) {
                if (user.getRole() == User.UserRole.STUDENT) {
                    System.out.println("Found existing student user in database.");
                    return user;
                }
            }
            
            // If no student found, try to find any user
            Optional<User> anyUser = userDAO.findAll().stream().findFirst();
            if (anyUser.isPresent()) {
                System.out.println("No student found, using first available user.");
                return anyUser.get();
            }
            
        } catch (Exception e) {
            System.out.println("Could not load user from database: " + e.getMessage());
        }
        
        // Create a mock user for testing
        System.out.println("Creating mock student user for testing...");
        User mockUser = new User(
            "test.student@northeastern.edu",
            "Test",
            "Student",
            User.UserRole.STUDENT
        );
        mockUser.setUserId(999);
        mockUser.setTrustScore(85.0);
        mockUser.setEnterpriseId("1"); // Adjust based on your data
        mockUser.setOrganizationId("1"); // Adjust based on your data
        
        return mockUser;
    }
}
