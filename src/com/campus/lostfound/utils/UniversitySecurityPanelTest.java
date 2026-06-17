package com.campus.lostfound.utils;

import com.campus.lostfound.models.User;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.ui.panels.UniversitySecurityPanel;
import com.campus.lostfound.ui.UIConstants;
import javax.swing.*;
import java.awt.*;

/**
 * Test class for UniversitySecurityPanel.
 * Launches the security panel with a test user for manual testing.
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class UniversitySecurityPanelTest {
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Apply global emoji font
        UIConstants.applyGlobalEmojiFont();
        
        SwingUtilities.invokeLater(() -> {
            // Create test user with CAMPUS_SECURITY role
            User testUser = new User(
                "security@northeastern.edu",
                "Security",
                "Officer",
                UserRole.CAMPUS_SECURITY
            );
            testUser.setUserId(999);
            testUser.setEnterpriseId("ENT-HIGHER-ED");
            testUser.setOrganizationId("ORG-NEU-MAIN");
            
            // Create frame
            JFrame frame = new JFrame("University Security Panel Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            
            // Add panel
            UniversitySecurityPanel panel = new UniversitySecurityPanel(testUser);
            frame.add(panel);
            
            frame.setVisible(true);
            
            System.out.println("===========================================");
            System.out.println("University Security Panel Test");
            System.out.println("===========================================");
            System.out.println("User: " + testUser.getFullName());
            System.out.println("Role: " + testUser.getRole().getDisplayName());
            System.out.println("Enterprise: " + testUser.getEnterpriseId());
            System.out.println("Organization: " + testUser.getOrganizationId());
            System.out.println("===========================================");
            System.out.println("\nPanel Features:");
            System.out.println("  Tab 1: Dashboard - Security metrics and alerts");
            System.out.println("  Tab 2: Verification Queue - Process verification requests");
            System.out.println("  Tab 3: Student Lookup - Search students, view trust scores");
            System.out.println("  Tab 4: High-Value Items - Items over $500");
            System.out.println("  Tab 5: Reports - Security analytics");
            System.out.println("===========================================");
        });
    }
}
