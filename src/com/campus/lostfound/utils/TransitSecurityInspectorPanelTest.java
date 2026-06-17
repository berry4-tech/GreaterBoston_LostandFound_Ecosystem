package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.panels.TransitSecurityInspectorPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Standalone test for TransitSecurityInspectorPanel.
 * 
 * This test creates a mock Transit Security Inspector user and displays
 * the panel for visual verification and interaction testing.
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class TransitSecurityInspectorPanelTest {
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            System.out.println("=".repeat(60));
            System.out.println("TransitSecurityInspectorPanel Test");
            System.out.println("=".repeat(60));
            
            // Create test user - Transit Security Inspector
            User testUser = createTestUser();
            System.out.println("Created test user: " + testUser.getFullName());
            System.out.println("Role: " + testUser.getRole());
            System.out.println("Email: " + testUser.getEmail());
            
            // Create and display frame
            JFrame frame = new JFrame("MBTA Transit Security Inspector - Test Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            
            // Create the panel
            TransitSecurityInspectorPanel panel = new TransitSecurityInspectorPanel(testUser);
            frame.add(panel);
            
            frame.setVisible(true);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Panel Features to Test:");
            System.out.println("=".repeat(60));
            System.out.println("1. Dashboard Tab:");
            System.out.println("   - View stat cards (Alerts, Patterns, Verifications, Cases)");
            System.out.println("   - Check Priority Alerts section");
            System.out.println("   - View Station Activity Hotspots");
            System.out.println("   - Review Recent Incidents table");
            System.out.println();
            System.out.println("2. Activity Monitor Tab:");
            System.out.println("   - View High-Frequency Claimers table");
            System.out.println("   - Click 'Investigate' button on a claimer");
            System.out.println("   - View Suspicious Activity Patterns table");
            System.out.println("   - Click 'Review' button on an activity");
            System.out.println("   - Test 'Escalate Selected' button");
            System.out.println("   - Test 'Notify BPD' button");
            System.out.println();
            System.out.println("3. Traveler Verification Tab:");
            System.out.println("   - View verification queue");
            System.out.println("   - Select a verification to see details");
            System.out.println("   - Test 'Verify' button");
            System.out.println("   - Test 'Approve'/'Reject' buttons");
            System.out.println("   - Test 'Airport Coordination' button");
            System.out.println("   - Test 'TSA Cross-Reference' button");
            System.out.println();
            System.out.println("4. Fraud Investigation Tab:");
            System.out.println("   - View fraud cases list");
            System.out.println("   - Select a case to see details");
            System.out.println("   - Test 'New Case' button");
            System.out.println("   - Test 'Add Evidence' button");
            System.out.println("   - Test 'Escalate to BPD' button");
            System.out.println("   - Test 'Cross-Enterprise Data' button");
            System.out.println();
            System.out.println("5. Reports & Alerts Tab:");
            System.out.println("   - Generate different report types");
            System.out.println("   - Test export buttons (PDF, CSV, BPD Format)");
            System.out.println("   - View escalated items table");
            System.out.println();
            System.out.println("Header Buttons:");
            System.out.println("   - Refresh button");
            System.out.println("   - Issue Alert button");
            System.out.println("   - Contact BPD button");
            System.out.println("   - Line status indicators (Red, Orange, Blue, Green)");
            System.out.println("=".repeat(60));
        });
    }
    
    /**
     * Create a test Transit Security Inspector user.
     */
    private static User createTestUser() {
        // User constructor: (String email, String firstName, String lastName, UserRole role)
        User user = new User(
            "j.murphy@mbta.com",
            "James",
            "Murphy",
            User.UserRole.TRANSIT_SECURITY_INSPECTOR
        );
        user.setUserId(90001);
        user.setEnterpriseId("ENT-MBTA");
        user.setOrganizationId("ORG-MBTA-SECURITY");
        return user;
    }
}
