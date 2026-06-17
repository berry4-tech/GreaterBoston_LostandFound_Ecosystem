/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui;

import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.panels.*;
import com.campus.lostfound.dao.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;
import java.util.List;

/**
 *
 * @author aksha
 */
public class MainDashboard extends JFrame {

    private User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel userLabel;
    private JLabel statsLabel;
    private JProgressBar trustBar; // Store reference for updates
    private JPanel sidebar; // Store reference to rebuild if needed

    // Panels
    private SearchBrowsePanel searchPanel;
    private ReportItemPanel reportPanel;
    private MyItemsPanel myItemsPanel;
    private ReviewClaimsPanel reviewClaimsPanel;
    private AdminPanel adminPanel;
    private EnterpriseAdminPanel enterpriseAdminPanel;
    
    // Role-specific panels
    private JPanel roleSpecificPanel;

    // DAOs
    private MongoItemDAO itemDAO;
    private MongoUserDAO userDAO;

    public MainDashboard(User user) {
        this.currentUser = user;
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();

        initComponents();
        loadStats();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setTitle("Campus Lost & Found - " + currentUser.getFullName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);

        // Main layout
        setLayout(new BorderLayout());

        // Create sidebar
        sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // Create content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(245, 245, 245));

        // Initialize panels
        searchPanel = new SearchBrowsePanel(currentUser);
        reportPanel = new ReportItemPanel(currentUser);
        myItemsPanel = new MyItemsPanel(currentUser);
        reviewClaimsPanel = new ReviewClaimsPanel(currentUser);
        MessagesPanel messagesPanel = new MessagesPanel(currentUser);

        // Add panels to card layout
        contentPanel.add(searchPanel, "search");
        contentPanel.add(reportPanel, "report");
        contentPanel.add(myItemsPanel, "myitems");
        contentPanel.add(reviewClaimsPanel, "reviewclaims");
        contentPanel.add(messagesPanel, "messages");

        // Add admin panel based on user role
        // SYSTEM_ADMIN gets network-wide AdminPanel
        // Enterprise admins get EnterpriseAdminPanel (scoped to their enterprise)
        if (currentUser.getRole() == User.UserRole.SYSTEM_ADMIN) {
            adminPanel = new AdminPanel(currentUser);
            contentPanel.add(adminPanel, "admin");
        } else if (isEnterpriseAdmin(currentUser.getRole())) {
            enterpriseAdminPanel = new EnterpriseAdminPanel(currentUser);
            contentPanel.add(enterpriseAdminPanel, "admin");
        }
        
        // Add role-specific panel based on user role
        addRoleSpecificPanel();

        add(contentPanel, BorderLayout.CENTER);

        // Show search panel by default
        cardLayout.show(contentPanel, "search");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // Logo/Title
        JLabel logoLabel = new JLabel("Lost & Found");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logoLabel);

        sidebar.add(Box.createVerticalStrut(10));

        // User info
        userLabel = new JLabel(currentUser.getFirstName());
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userLabel.setForeground(new Color(189, 195, 199));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(userLabel);

        JLabel roleLabel = new JLabel(currentUser.getRole().getDisplayName());
        roleLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        roleLabel.setForeground(new Color(149, 165, 166));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(roleLabel);

        // Trust Score - Store reference for updates
        trustBar = new JProgressBar(0, 100);
        trustBar.setValue((int) currentUser.getTrustScore());
        trustBar.setString("Trust: " + (int) currentUser.getTrustScore() + "%");
        trustBar.setStringPainted(true);
        trustBar.setMaximumSize(new Dimension(220, 20));
        trustBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(trustBar);

        sidebar.add(Box.createVerticalStrut(10));

        // Add warning banner if trust score is low
        if (currentUser.getTrustScore() < 75) {
            JPanel warningPanel = createTrustWarningBanner();
            sidebar.add(warningPanel);
            sidebar.add(Box.createVerticalStrut(10));
        }

        sidebar.add(Box.createVerticalStrut(20));

        // Navigation buttons
        sidebar.add(createNavButton("Search Items", "search"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Report Item", "report"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("My Items", "myitems"));
        sidebar.add(Box.createVerticalStrut(10));

        // Add Review Claims button with notification badge
        JButton reviewClaimsButton = createNavButtonWithBadge("Review Claims", "reviewclaims");
        sidebar.add(reviewClaimsButton);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createNavButton("Messages", "messages"));
        sidebar.add(Box.createVerticalStrut(10));

        if (currentUser.getRole() == User.UserRole.SYSTEM_ADMIN) {
            sidebar.add(createNavButton("Network Admin", "admin"));
            sidebar.add(Box.createVerticalStrut(10));
        } else if (isEnterpriseAdmin(currentUser.getRole())) {
            sidebar.add(createNavButton("Enterprise Admin", "admin"));
            sidebar.add(Box.createVerticalStrut(10));
        }
        
        // Add role-specific navigation button
        String roleButtonText = getRoleSpecificButtonText();
        if (roleButtonText != null) {
            sidebar.add(createNavButton(roleButtonText, "rolespecific"));
            sidebar.add(Box.createVerticalStrut(10));
        }

        sidebar.add(Box.createVerticalGlue());

        // Stats
        statsLabel = new JLabel("<html>Loading stats...</html>");
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsLabel.setForeground(new Color(189, 195, 199));
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(statsLabel);

        sidebar.add(Box.createVerticalStrut(20));

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setMaximumSize(new Dimension(220, 35));
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.setBackground(new Color(192, 57, 43));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(220, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBackground(new Color(52, 73, 94));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        button.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            // Refresh panel if needed
            if (cardName.equals("myitems") && myItemsPanel != null) {
                myItemsPanel.refreshItems();
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(41, 128, 185));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(52, 73, 94));
            }
        });

        return button;
    }

    private JButton createNavButtonWithBadge(String text, String cardName) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(220, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBackground(new Color(52, 73, 94));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        button.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            // Refresh panel when shown
            if (cardName.equals("reviewclaims") && reviewClaimsPanel != null) {
                reviewClaimsPanel.loadItemsWithClaims();
            }
            if (cardName.equals("myitems") && myItemsPanel != null) {
                myItemsPanel.refreshItems();
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(41, 128, 185));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(52, 73, 94));
            }
        });

        // Load badge count asynchronously
        SwingWorker<Integer, Void> badgeWorker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                // 🔥 FIX: Use the same logic as the panel to count claims
                List<Item> userItems = new MongoItemDAO().findByUser(currentUser.getEmail());
                int totalPendingClaims = 0;

                for (Item item : userItems) {
                    if (item.getType() == Item.ItemType.FOUND) {
                        MongoClaimDAO claimDAO = new MongoClaimDAO();
                        totalPendingClaims += claimDAO.countPendingClaims(item.getItemId());
                    }
                }

                return totalPendingClaims;
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    if (count > 0) {
                        button.setText(text + " (" + count + ")");
                        // Make it more prominent if there are claims
                        button.setBackground(new Color(255, 152, 0));
                    }
                } catch (Exception e) {
                    // Ignore errors - badge just won't show
                }
            }
        };

        // Load badge after a short delay to let panel initialize
        Timer badgeTimer = new Timer(500, e -> badgeWorker.execute());
        badgeTimer.setRepeats(false);
        badgeTimer.start();

        return button;
    }

    private void loadStats() {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                int totalItems = itemDAO.findAll().size();
                int myItems = itemDAO.findByUser(currentUser.getEmail()).size();
                int totalUsers = userDAO.findAll().size();

                return String.format("<html>Statistics<br>"
                        + "Total Items: %d<br>"
                        + "My Items: %d<br>"
                        + "Active Users: %d</html>",
                        totalItems, myItems, totalUsers);
            }

            @Override
            protected void done() {
                try {
                    statsLabel.setText(get());
                } catch (Exception e) {
                    statsLabel.setText("<html>Stats unavailable</html>");
                }
            }
        };
        worker.execute();
    }

    private JPanel createTrustWarningBanner() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMaximumSize(new Dimension(220, 150));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Color based on severity
        Color bgColor, borderColor;
        String icon, title;

        if (currentUser.getTrustScore() < 25) {
            // SUSPENDED
            bgColor = new Color(255, 235, 235);
            borderColor = new Color(244, 67, 54);
            icon = "X";
            title = "SUSPENDED";
        } else if (currentUser.getTrustScore() < 50) {
            // POOR
            bgColor = new Color(255, 243, 224);
            borderColor = new Color(255, 152, 0);
            icon = "!";
            title = "RESTRICTED";
        } else {
            // GOOD (but not excellent)
            bgColor = new Color(255, 249, 196);
            borderColor = new Color(255, 193, 7);
            icon = "!";
            title = "LIMITED";
        }

        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 20));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(borderColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(5));

        // Message
        String message = getTrustWarningMessage(currentUser.getTrustScore());
        JTextArea messageArea = new JTextArea(message);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 10));
        messageArea.setForeground(new Color(80, 80, 80));
        messageArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageArea);

        panel.add(Box.createVerticalStrut(5));

        // Improve score hint
        JLabel hintLabel = new JLabel("<html><center>Report items<br>to improve</center></html>");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 9));
        hintLabel.setForeground(new Color(94, 242, 118));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hintLabel);

        return panel;
    }

    private String getTrustWarningMessage(double score) {
        if (score < 25) {
            return "Account suspended due to multiple false claims. Cannot submit claims. Contact admin to appeal.";
        } else if (score < 50) {
            return "Restricted access. Can only claim low-value items. All claims need admin approval.";
        } else {
            return "Limited access. Cannot claim high-value items (electronics, jewelry, IDs).";
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }

    public void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Update the Review Claims button badge with current count
     */
    public void updateReviewClaimsBadge() {
        if (reviewClaimsPanel != null) {
            SwingUtilities.invokeLater(() -> {
                reviewClaimsPanel.loadItemsWithClaims();
            });
        }
    }

    /**
     * Refresh user's trust score from database and update all UI components
     * Call this after any action that changes trust score
     */
    public void refreshUserTrustScore() {
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                // Reload user from database to get updated trust score
                Optional<User> userOpt = userDAO.findByEmail(currentUser.getEmail());
                return userOpt.orElse(currentUser);
            }

            @Override
            protected void done() {
                try {
                    User updatedUser = get();
                    double oldScore = currentUser.getTrustScore();
                    double newScore = updatedUser.getTrustScore();

                    // Update current user reference
                    currentUser = updatedUser;

                    // Update trust bar in sidebar
                    if (trustBar != null) {
                        trustBar.setValue((int) newScore);
                        trustBar.setString("Trust: " + (int) newScore + "%");
                        trustBar.repaint();
                    }

                    // If trust level changed, rebuild sidebar to show/hide warning banner
                    if (getTrustLevelCategory(oldScore) != getTrustLevelCategory(newScore)) {
                        rebuildSidebar();
                    }

                    System.out.println("✓ UI refreshed: Trust score "
                            + String.format("%.0f", oldScore) + " → " + String.format("%.0f", newScore));

                } catch (Exception e) {
                    System.err.println("Error refreshing user trust score: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Get trust level category (0-3) for comparison
     */
    private int getTrustLevelCategory(double score) {
        if (score >= 75) {
            return 3; // EXCELLENT
        }
        if (score >= 50) {
            return 2; // GOOD
        }
        if (score >= 25) {
            return 1; // POOR
        }
        return 0; // SUSPENDED
    }

    /**
     * Rebuild sidebar when trust level changes (to update warning banner)
     */
    private void rebuildSidebar() {
        remove(sidebar);
        sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);
        revalidate();
        repaint();
    }
    
    /**
     * Check if a role is an enterprise admin (can access enterprise-scoped admin panel)
     */
    private boolean isEnterpriseAdmin(User.UserRole role) {
        if (role == null) return false;
        switch (role) {
            case UNIVERSITY_ADMIN:      // University/Higher Education enterprise admin
            case MBTA_ADMIN:            // MBTA Transit enterprise admin
            case AIRPORT_ADMIN:         // Logan Airport enterprise admin
            case POLICE_ADMIN:          // Boston Police enterprise admin
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Add role-specific panel based on user's role
     */
    private void addRoleSpecificPanel() {
        User.UserRole role = currentUser.getRole();
        
        switch (role) {
            case AIRPORT_LOST_FOUND_SPECIALIST:
                roleSpecificPanel = new AirportLostFoundSpecialistPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case TSA_SECURITY_COORDINATOR:
                roleSpecificPanel = new TSASecurityCoordinatorPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case STATION_MANAGER:
                roleSpecificPanel = new MBTAStationManagerPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case TRANSIT_SECURITY_INSPECTOR:
                roleSpecificPanel = new TransitSecurityInspectorPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case POLICE_EVIDENCE_CUSTODIAN:
                roleSpecificPanel = new PoliceEvidenceCustodianPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case PUBLIC_TRAVELER:
                roleSpecificPanel = new PublicTravelerPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case STUDENT:
                roleSpecificPanel = new StudentUserPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case CAMPUS_SECURITY:
                roleSpecificPanel = new UniversitySecurityPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            case CAMPUS_COORDINATOR:
                roleSpecificPanel = new CampusCoordinatorPanel(currentUser);
                contentPanel.add(roleSpecificPanel, "rolespecific");
                break;
                
            default:
                // No special panel for this role
                roleSpecificPanel = null;
                break;
        }
    }
    
    /**
     * Get the button text for role-specific panel navigation
     */
    private String getRoleSpecificButtonText() {
        User.UserRole role = currentUser.getRole();
        
        switch (role) {
            case AIRPORT_LOST_FOUND_SPECIALIST:
                return "Airport L&F";
            case TSA_SECURITY_COORDINATOR:
                return "TSA Security";
            case STATION_MANAGER:
                return "Station Manager";
            case TRANSIT_SECURITY_INSPECTOR:
                return "Transit Security";
            case POLICE_EVIDENCE_CUSTODIAN:
                return "Evidence Custody";
            case PUBLIC_TRAVELER:
                return "Traveler Portal";
            case STUDENT:
                return "Student Portal";
            case CAMPUS_SECURITY:
                return "Security Panel";
            case CAMPUS_COORDINATOR:
                return "Coordinator Panel";
            default:
                return null;
        }
    }
}
