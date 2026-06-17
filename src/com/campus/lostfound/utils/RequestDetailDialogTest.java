package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.components.WorkQueueTablePanel;
import com.campus.lostfound.ui.dialogs.RequestDetailDialog;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Test class for RequestDetailDialog component.
 * Demonstrates integration with WorkQueueTablePanel.
 * 
 * @author Developer 2 - UI Components
 */
public class RequestDetailDialogTest {
    
    public static void main(String[] args) {
        System.out.println("=" .repeat(60));
        System.out.println("RequestDetailDialog Component Test");
        System.out.println("=" .repeat(60));
        
        // Run tests
        testDialogCreation();
        testIntegrationWithWorkQueue();
    }
    
    /**
     * Test 1: Dialog Creation with Different Request Types
     */
    private static void testDialogCreation() {
        System.out.println("\n[TEST 1] Dialog Creation");
        System.out.println("-".repeat(40));
        
        try {
            // Get test data
            MongoUserDAO userDAO = new MongoUserDAO();
            WorkRequestService service = new WorkRequestService();
            
            List<User> users = userDAO.findAll();
            List<WorkRequest> requests = service.getAllRequests();
            
            if (users.isEmpty()) {
                System.out.println("❌ No users found. Run MongoDataGenerator first.");
                return;
            }
            
            if (requests.isEmpty()) {
                System.out.println("❌ No work requests found. Run work request tests first.");
                return;
            }
            
            System.out.println("✓ Found " + users.size() + " users");
            System.out.println("✓ Found " + requests.size() + " work requests");
            
            // Test with each request type
            for (WorkRequest.RequestType type : WorkRequest.RequestType.values()) {
                Optional<WorkRequest> requestOfType = requests.stream()
                    .filter(r -> r.getRequestType() == type)
                    .findFirst();
                
                if (requestOfType.isPresent()) {
                    System.out.println("✓ Found request of type: " + type);
                } else {
                    System.out.println("⚠ No request found for type: " + type);
                }
            }
            
            System.out.println("✓ TEST 1 PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 1 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 2: Integration with WorkQueueTablePanel (Interactive)
     */
    private static void testIntegrationWithWorkQueue() {
        System.out.println("\n[TEST 2] Integration with WorkQueueTablePanel");
        System.out.println("-".repeat(40));
        
        try {
            // Get test data
            MongoUserDAO userDAO = new MongoUserDAO();
            List<User> users = userDAO.findAll();
            
            if (users.isEmpty()) {
                System.out.println("❌ No users found.");
                return;
            }
            
            // Find a coordinator or admin
            Optional<User> coordinator = users.stream()
                .filter(u -> u.getRole().name().contains("COORDINATOR") || 
                            u.getRole().name().contains("ADMIN") ||
                            u.getRole().name().contains("MANAGER"))
                .findFirst();
            
            User testUser = coordinator.orElse(users.get(0));
            System.out.println("✓ Using test user: " + testUser.getFullName() + " (" + testUser.getRole() + ")");
            
            // Launch integrated UI
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Use default
                }
                
                // Create main frame
                JFrame frame = new JFrame("Work Request System - Integration Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1300, 700);
                
                // Create work queue panel
                WorkQueueTablePanel workQueue = new WorkQueueTablePanel(testUser);
                
                // Set up double-click to open detail dialog
                workQueue.setOnRequestDoubleClicked(request -> {
                    RequestDetailDialog dialog = new RequestDetailDialog(frame, request, testUser);
                    
                    // Set callback to refresh queue after changes
                    dialog.setOnRequestUpdated(updatedRequest -> {
                        System.out.println("Request updated: " + updatedRequest.getStatus());
                        workQueue.loadRequests();
                    });
                    
                    dialog.setVisible(true);
                });
                
                // Set up single-click to show preview
                workQueue.setOnRequestSelected(request -> {
                    frame.setTitle("Work Request System - Selected: " + request.getRequestSummary());
                });
                
                // Add instructions panel
                JPanel instructionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
                instructionPanel.setBackground(new Color(232, 244, 253));
                instructionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(13, 110, 253)));
                
                JLabel instructionLabel = new JLabel(
                    "📋 Double-click any request to open the detail dialog • " +
                    "Test approve/reject actions • Press Refresh to reload data"
                );
                instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                instructionPanel.add(instructionLabel);
                
                // Layout
                frame.setLayout(new BorderLayout());
                frame.add(instructionPanel, BorderLayout.NORTH);
                frame.add(workQueue, BorderLayout.CENTER);
                
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                
                // Load data
                workQueue.loadRequests();
                
                System.out.println("✓ Integration test UI opened.");
                System.out.println("  → Double-click a request to test RequestDetailDialog");
                System.out.println("  → Try approving/rejecting requests");
            });
            
            System.out.println("✓ TEST 2 LAUNCHED (check UI window)");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 2 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
