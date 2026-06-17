package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.components.WorkQueueTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Test class for WorkQueueTablePanel component.
 * Demonstrates usage and verifies functionality.
 * 
 * @author Developer 2 - UI Components
 */
public class WorkQueueTablePanelTest {
    
    public static void main(String[] args) {
        System.out.println("=" .repeat(60));
        System.out.println("WorkQueueTablePanel Component Test");
        System.out.println("=" .repeat(60));
        
        // Run tests
        testComponentCreation();
        testDataLoading();
        testUIDisplay();
    }
    
    /**
     * Test 1: Component Creation
     */
    private static void testComponentCreation() {
        System.out.println("\n[TEST 1] Component Creation");
        System.out.println("-".repeat(40));
        
        try {
            // Get a test user
            MongoUserDAO userDAO = new MongoUserDAO();
            List<User> users = userDAO.findAll();
            
            if (users.isEmpty()) {
                System.out.println("❌ No users found in database. Run MongoDataGenerator first.");
                return;
            }
            
            User testUser = users.get(0);
            System.out.println("✓ Found test user: " + testUser.getFullName() + " (" + testUser.getRole() + ")");
            
            // Create component
            WorkQueueTablePanel panel = new WorkQueueTablePanel(testUser);
            System.out.println("✓ WorkQueueTablePanel created successfully");
            
            // Verify component structure
            if (panel.getLayout() instanceof BorderLayout) {
                System.out.println("✓ Uses BorderLayout as expected");
            }
            
            System.out.println("✓ TEST 1 PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 1 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 2: Data Loading
     */
    private static void testDataLoading() {
        System.out.println("\n[TEST 2] Data Loading");
        System.out.println("-".repeat(40));
        
        try {
            // Get work requests from service
            WorkRequestService service = new WorkRequestService();
            List<WorkRequest> allRequests = service.getAllRequests();
            
            System.out.println("✓ Total work requests in database: " + allRequests.size());
            
            // Count by status
            long pending = allRequests.stream()
                .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING)
                .count();
            long inProgress = allRequests.stream()
                .filter(r -> r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                .count();
            long completed = allRequests.stream()
                .filter(r -> r.getStatus() == WorkRequest.RequestStatus.COMPLETED)
                .count();
            
            System.out.println("  - Pending: " + pending);
            System.out.println("  - In Progress: " + inProgress);
            System.out.println("  - Completed: " + completed);
            
            // Count by priority
            long urgent = allRequests.stream()
                .filter(r -> r.getPriority() == WorkRequest.RequestPriority.URGENT)
                .count();
            long high = allRequests.stream()
                .filter(r -> r.getPriority() == WorkRequest.RequestPriority.HIGH)
                .count();
            
            System.out.println("  - Urgent priority: " + urgent);
            System.out.println("  - High priority: " + high);
            
            // Count overdue
            long overdue = allRequests.stream()
                .filter(WorkRequest::isOverdue)
                .count();
            System.out.println("  - Overdue: " + overdue);
            
            System.out.println("✓ TEST 2 PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 2 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 3: UI Display (Interactive)
     */
    private static void testUIDisplay() {
        System.out.println("\n[TEST 3] UI Display (Interactive)");
        System.out.println("-".repeat(40));
        
        try {
            // Get a test user
            MongoUserDAO userDAO = new MongoUserDAO();
            List<User> users = userDAO.findAll();
            
            if (users.isEmpty()) {
                System.out.println("❌ No users found. Skipping UI test.");
                return;
            }
            
            // Find a coordinator or admin for more interesting data
            Optional<User> coordinator = users.stream()
                .filter(u -> u.getRole().name().contains("COORDINATOR") || 
                            u.getRole().name().contains("ADMIN"))
                .findFirst();
            
            User testUser = coordinator.orElse(users.get(0));
            System.out.println("✓ Using test user: " + testUser.getFullName());
            
            // Launch UI on EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    // Set look and feel
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Use default
                }
                
                // Create frame
                JFrame frame = new JFrame("WorkQueueTablePanel Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 600);
                
                // Create panel
                WorkQueueTablePanel panel = new WorkQueueTablePanel(testUser);
                
                // Set callbacks
                panel.setOnRequestSelected(request -> {
                    System.out.println("Selected: " + request.getRequestSummary());
                });
                
                panel.setOnRequestDoubleClicked(request -> {
                    JOptionPane.showMessageDialog(frame,
                        "Request Details:\n\n" +
                        "ID: " + request.getRequestId() + "\n" +
                        "Type: " + request.getRequestType() + "\n" +
                        "Status: " + request.getStatus() + "\n" +
                        "Priority: " + request.getPriority() + "\n" +
                        "Requester: " + request.getRequesterName() + "\n" +
                        "Summary: " + request.getRequestSummary() + "\n" +
                        "SLA: " + request.getHoursUntilSla() + " hours remaining",
                        "Work Request Details",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                frame.add(panel);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                
                // Load data
                panel.loadRequests();
                
                System.out.println("✓ UI window opened. Close window to exit test.");
            });
            
            System.out.println("✓ TEST 3 LAUNCHED (check UI window)");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 3 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
