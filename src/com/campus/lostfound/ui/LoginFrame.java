/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.panels.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

/**
 *
 * @author aksha
 */
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private MongoUserDAO userDAO;

    public LoginFrame() {
        userDAO = new MongoUserDAO();
        initComponents();
        setLocationRelativeTo(null);

        // Set Enter key to trigger login
        getRootPane().setDefaultButton(loginButton);
    }

    private void initComponents() {
        setTitle("Greater Boston Lost & Found - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Create gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(41, 128, 185),
                        0, getHeight(), new Color(109, 89, 122)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new GridBagLayout());

        // Create login panel
        JPanel loginPanel = createLoginPanel();
        mainPanel.add(loginPanel);

        add(mainPanel);
        setSize(900, 600);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Logo/Title
        JLabel logoLabel = new JLabel("📦 Greater Boston Lost & Found");
        logoLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 28));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(logoLabel);

        JLabel subtitleLabel = new JLabel("Boston Lost & Found Network");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitleLabel);

        panel.add(Box.createVerticalStrut(30));

        // Email field
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 12));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailLabel);
        panel.add(Box.createVerticalStrut(5));

        emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(300, 40));
        emailField.setMaximumSize(new Dimension(300, 40));
        emailField.setFont(new Font("Arial", Font.PLAIN, 14));
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailField);

        panel.add(Box.createVerticalStrut(20));

        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 12));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(5));

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 40));
        passwordField.setMaximumSize(new Dimension(300, 40));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordField);

        panel.add(Box.createVerticalStrut(25));

        // Login button
        loginButton = new JButton("Sign In");
        loginButton.setPreferredSize(new Dimension(300, 45));
        loginButton.setMaximumSize(new Dimension(300, 45));
        loginButton.setBackground(new Color(52, 152, 219));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(true);
        loginButton.setOpaque(true);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());
        panel.add(loginButton);

        panel.add(Box.createVerticalStrut(10));

        // Register button
        registerButton = new JButton("Create New Account");
        registerButton.setPreferredSize(new Dimension(300, 45));
        registerButton.setMaximumSize(new Dimension(300, 45));
        registerButton.setBackground(new Color(46, 204, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(true);
        registerButton.setOpaque(true);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> showRegistrationDialog());
        panel.add(registerButton);

        panel.add(Box.createVerticalStrut(20));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);

        panel.add(Box.createVerticalStrut(10));

        // Demo credentials hint
        JLabel hintLabel = new JLabel("<html><center>Create a new account to get started<br>Select your enterprise and organization</center></html>");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hintLabel);

        return panel;
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }

        // Disable button during login
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(new Color(52, 152, 219));

        // Perform login in background thread
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private User authenticatedUser;

            @Override
            protected Boolean doInBackground() throws Exception {
                boolean authenticated = userDAO.authenticate(email, password);
                if (authenticated) {
                    Optional<User> userOpt = userDAO.findByEmail(email);
                    if (userOpt.isPresent()) {
                        authenticatedUser = userOpt.get();
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        statusLabel.setText("Login successful!");
                        statusLabel.setForeground(new Color(46, 204, 113));

                        // Open appropriate panel based on role
                        SwingUtilities.invokeLater(() -> {
                            openRoleBasedPanel(authenticatedUser);
                            dispose();
                        });
                    } else {
                        showError("Invalid email or password");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception e) {
                    showError("Login failed: " + e.getMessage());
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                }
            }
        };

        worker.execute();
    }

    private void showRegistrationDialog() {
        JDialog dialog = new JDialog(this, "Create New Account", true);
        dialog.setSize(450, 650);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        // Form fields
        JTextField firstNameField = createField(panel, "First Name");
        JTextField lastNameField = createField(panel, "Last Name");
        JTextField emailFieldReg = createField(panel, "Email");
        JTextField phoneField = createField(panel, "Phone Number (optional)");
        JPasswordField passwordFieldReg = createPasswordField(panel, "Password");
        JPasswordField confirmPasswordField = createPasswordField(panel, "Confirm Password");

        // Enterprise selection
        panel.add(new JLabel("Enterprise"));
        panel.add(Box.createVerticalStrut(5));
        JComboBox<com.campus.lostfound.models.Enterprise> enterpriseCombo = new JComboBox<>();
        enterpriseCombo.setMaximumSize(new Dimension(350, 35));
        enterpriseCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Load enterprises
        com.campus.lostfound.dao.MongoEnterpriseDAO enterpriseDAO = new com.campus.lostfound.dao.MongoEnterpriseDAO();
        for (com.campus.lostfound.models.Enterprise enterprise : enterpriseDAO.findAll()) {
            enterpriseCombo.addItem(enterprise);
        }
        panel.add(enterpriseCombo);
        panel.add(Box.createVerticalStrut(15));

        // Organization selection
        panel.add(new JLabel("Organization"));
        panel.add(Box.createVerticalStrut(5));
        JComboBox<com.campus.lostfound.models.Organization> organizationCombo = new JComboBox<>();
        organizationCombo.setMaximumSize(new Dimension(350, 35));
        organizationCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(organizationCombo);
        panel.add(Box.createVerticalStrut(15));

        // Role selection - with cascading filter
        panel.add(new JLabel("Role"));
        panel.add(Box.createVerticalStrut(5));
        JComboBox<User.UserRole> roleCombo = new JComboBox<>();
        roleCombo.setMaximumSize(new Dimension(350, 35));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(roleCombo);
        panel.add(Box.createVerticalStrut(20));
        
        // Organization DAO for loading organizations
        com.campus.lostfound.dao.MongoOrganizationDAO organizationDAO = new com.campus.lostfound.dao.MongoOrganizationDAO();
        
        // Helper to update roles based on enterprise and organization selection
        Runnable updateRoles = () -> {
            roleCombo.removeAllItems();
            
            com.campus.lostfound.models.Enterprise selectedEnterprise = 
                (com.campus.lostfound.models.Enterprise) enterpriseCombo.getSelectedItem();
            com.campus.lostfound.models.Organization selectedOrg = 
                (com.campus.lostfound.models.Organization) organizationCombo.getSelectedItem();
            
            if (selectedEnterprise != null) {
                java.util.List<User.UserRole> applicableRoles;
                
                if (selectedOrg != null && selectedOrg.getType() != null) {
                    // More specific filtering based on organization type
                    applicableRoles = getRolesForOrganization(
                        selectedEnterprise.getType(), 
                        selectedOrg.getType()
                    );
                } else {
                    // Enterprise-level filtering
                    applicableRoles = getRolesForEnterprise(selectedEnterprise.getType());
                }
                
                for (User.UserRole role : applicableRoles) {
                    roleCombo.addItem(role);
                }
            }
        };
        
        // Update organizations when enterprise changes
        enterpriseCombo.addActionListener(e -> {
            organizationCombo.removeAllItems();
            com.campus.lostfound.models.Enterprise selectedEnterprise = 
                (com.campus.lostfound.models.Enterprise) enterpriseCombo.getSelectedItem();
            if (selectedEnterprise != null) {
                for (com.campus.lostfound.models.Organization org : organizationDAO.findByEnterpriseId(selectedEnterprise.getEnterpriseId())) {
                    organizationCombo.addItem(org);
                }
            }
            // Update roles when enterprise changes
            updateRoles.run();
        });
        
        // Update roles when organization changes
        organizationCombo.addActionListener(e -> {
            updateRoles.run();
        });
        
        // Trigger initial load
        if (enterpriseCombo.getItemCount() > 0) {
            enterpriseCombo.setSelectedIndex(0);
        }

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton createButton = new JButton("Create Account");
        createButton.setBackground(new Color(46, 204, 113));
        createButton.setForeground(Color.WHITE);
        createButton.setFont(new Font("Arial", Font.BOLD, 13));
        createButton.setFocusPainted(false);
        createButton.setBorderPainted(false);
        createButton.setContentAreaFilled(true);
        createButton.setOpaque(true);
        createButton.setPreferredSize(new Dimension(140, 35));
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailFieldReg.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordFieldReg.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            // Validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid email address", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if user already exists
            Optional<User> existing = userDAO.findByEmail(email);
            if (existing.isPresent()) {
                JOptionPane.showMessageDialog(dialog, "An account with this email already exists", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get selected enterprise and organization
            com.campus.lostfound.models.Enterprise selectedEnterprise = 
                (com.campus.lostfound.models.Enterprise) enterpriseCombo.getSelectedItem();
            com.campus.lostfound.models.Organization selectedOrg = 
                (com.campus.lostfound.models.Organization) organizationCombo.getSelectedItem();
            
            if (selectedEnterprise == null || selectedOrg == null) {
                JOptionPane.showMessageDialog(dialog, "Please select both Enterprise and Organization", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create user
            User newUser = new User(email, firstName, lastName, (User.UserRole) roleCombo.getSelectedItem());
            newUser.setPhoneNumber(phone.isEmpty() ? null : phone);
            newUser.setEnterpriseId(selectedEnterprise.getEnterpriseId());
            newUser.setOrganizationId(selectedOrg.getOrganizationId());

            String userId = userDAO.create(newUser, password);
            if (userId != null) {
                JOptionPane.showMessageDialog(dialog, "Account created successfully! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                emailField.setText(email);
                passwordField.setText("");
                passwordField.requestFocus();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to create account", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(149, 165, 166));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 13));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setContentAreaFilled(true);
        cancelButton.setOpaque(true);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);

        JScrollPane scrollPane = new JScrollPane(panel);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private JTextField createField(JPanel parent, String label) {
        parent.add(new JLabel(label));
        parent.add(Box.createVerticalStrut(5));
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(350, 35));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(field);
        parent.add(Box.createVerticalStrut(15));
        return field;
    }

    private JPasswordField createPasswordField(JPanel parent, String label) {
        parent.add(new JLabel(label));
        parent.add(Box.createVerticalStrut(5));
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(350, 35));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(field);
        parent.add(Box.createVerticalStrut(15));
        return field;
    }

    /**
     * Get applicable roles based on Enterprise type
     */
    private java.util.List<User.UserRole> getRolesForEnterprise(com.campus.lostfound.models.Enterprise.EnterpriseType enterpriseType) {
        java.util.List<User.UserRole> roles = new java.util.ArrayList<>();
        
        if (enterpriseType == null) {
            return roles;
        }
        
        switch (enterpriseType) {
            case HIGHER_EDUCATION:
                roles.add(User.UserRole.STUDENT);
                roles.add(User.UserRole.STAFF);
                roles.add(User.UserRole.CAMPUS_COORDINATOR);
                roles.add(User.UserRole.BUILDING_MANAGER);
                roles.add(User.UserRole.CAMPUS_SECURITY);
                roles.add(User.UserRole.UNIVERSITY_ADMIN);
                break;
                
            case PUBLIC_TRANSIT:
                roles.add(User.UserRole.PUBLIC_TRAVELER);
                roles.add(User.UserRole.STATION_MANAGER);
                roles.add(User.UserRole.LOST_FOUND_CLERK);
                roles.add(User.UserRole.TRANSIT_SECURITY_INSPECTOR);
                roles.add(User.UserRole.TRANSIT_OFFICER);
                break;
                
            case AIRPORT:
                roles.add(User.UserRole.PUBLIC_TRAVELER);
                roles.add(User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
                roles.add(User.UserRole.TSA_SECURITY_COORDINATOR);
                roles.add(User.UserRole.AIRLINE_REPRESENTATIVE);
                break;
                
            case LAW_ENFORCEMENT:
                roles.add(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
                roles.add(User.UserRole.DETECTIVE);
                roles.add(User.UserRole.POLICE_ADMIN);
                break;
        }
        
        return roles;
    }

    /**
     * Get applicable roles based on Organization type (more specific filtering)
     */
    private java.util.List<User.UserRole> getRolesForOrganization(
            com.campus.lostfound.models.Enterprise.EnterpriseType enterpriseType,
            com.campus.lostfound.models.Organization.OrganizationType orgType) {
        
        // Start with enterprise-level roles
        java.util.List<User.UserRole> roles = getRolesForEnterprise(enterpriseType);
        
        if (orgType == null) {
            return roles;
        }
        
        // Further filter based on organization type
        java.util.List<User.UserRole> filteredRoles = new java.util.ArrayList<>();
        
        switch (orgType) {
            // Higher Education Organizations - Students can register at any university org
            case CAMPUS_OPERATIONS:
                filteredRoles.add(User.UserRole.STUDENT);  // Students can register here
                filteredRoles.add(User.UserRole.CAMPUS_COORDINATOR);
                filteredRoles.add(User.UserRole.BUILDING_MANAGER);
                filteredRoles.add(User.UserRole.STAFF);
                filteredRoles.add(User.UserRole.UNIVERSITY_ADMIN);  // Admins can register here
                break;
            case STUDENT_SERVICES:
                filteredRoles.add(User.UserRole.STUDENT);
                filteredRoles.add(User.UserRole.STAFF);
                filteredRoles.add(User.UserRole.CAMPUS_COORDINATOR);
                break;
            case CAMPUS_SECURITY:
                filteredRoles.add(User.UserRole.STUDENT);  // Students can register here
                filteredRoles.add(User.UserRole.CAMPUS_SECURITY);
                filteredRoles.add(User.UserRole.STAFF);
                break;
                
            // MBTA Organizations
            case STATION_OPERATIONS:
                filteredRoles.add(User.UserRole.STATION_MANAGER);
                filteredRoles.add(User.UserRole.LOST_FOUND_CLERK);
                filteredRoles.add(User.UserRole.PUBLIC_TRAVELER);
                break;
            case TRANSIT_POLICE:
                filteredRoles.add(User.UserRole.TRANSIT_SECURITY_INSPECTOR);
                filteredRoles.add(User.UserRole.TRANSIT_OFFICER);
                break;
            case CENTRAL_LOST_FOUND:
                filteredRoles.add(User.UserRole.LOST_FOUND_CLERK);
                filteredRoles.add(User.UserRole.STATION_MANAGER);
                break;
                
            // Airport Organizations
            case AIRPORT_OPERATIONS:
                filteredRoles.add(User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
                filteredRoles.add(User.UserRole.PUBLIC_TRAVELER);
                break;
            case TSA_SECURITY:
                filteredRoles.add(User.UserRole.TSA_SECURITY_COORDINATOR);
                break;
            case AIRLINE_SERVICES:
                filteredRoles.add(User.UserRole.AIRLINE_REPRESENTATIVE);
                filteredRoles.add(User.UserRole.PUBLIC_TRAVELER);
                break;
                
            // Law Enforcement Organizations
            case POLICE_DEPARTMENT:
                filteredRoles.add(User.UserRole.POLICE_ADMIN);
                filteredRoles.add(User.UserRole.DETECTIVE);
                break;
            case EVIDENCE_MANAGEMENT:
                filteredRoles.add(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
                break;
            case DETECTIVE_BUREAU:
                filteredRoles.add(User.UserRole.DETECTIVE);
                filteredRoles.add(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
                break;
                
            default:
                return roles; // Return all enterprise roles if org type not matched
        }
        
        return filteredRoles;
    }

    /**
     * Open the appropriate panel based on user role
     */
    private void openRoleBasedPanel(User user) {
        JFrame frame;
        
        switch (user.getRole()) {
            case CAMPUS_COORDINATOR:
                frame = createRoleFrame("Campus Coordinator", new CampusCoordinatorPanel(user));
                break;
            case CAMPUS_SECURITY:
                frame = createRoleFrame("University Security", new UniversitySecurityPanel(user));
                break;
            case STATION_MANAGER:
                frame = createRoleFrame("Station Manager", new MBTAStationManagerPanel(user));
                break;
            case STUDENT:
                frame = createRoleFrame("Student Portal", new StudentUserPanel(user));
                break;
            case PUBLIC_TRAVELER:
                frame = createRoleFrame("Traveler Portal", new PublicTravelerPanel(user));
                break;
            case AIRPORT_LOST_FOUND_SPECIALIST:
                frame = createRoleFrame("Airport Lost & Found", new AirportLostFoundSpecialistPanel(user));
                break;
            case TSA_SECURITY_COORDINATOR:
                frame = createRoleFrame("TSA Security Operations", new TSASecurityCoordinatorPanel(user));
                break;
            case POLICE_EVIDENCE_CUSTODIAN:
                frame = createRoleFrame("Police Evidence Custodian", new PoliceEvidenceCustodianPanel(user));
                break;
            case TRANSIT_SECURITY_INSPECTOR:
                frame = createRoleFrame("Transit Security Inspector", new TransitSecurityInspectorPanel(user));
                break;
            default:
                // For other roles, use default dashboard
                frame = new MainDashboard(user);
                break;
        }
        
        frame.setVisible(true);
    }
    
    /**
     * Create a JFrame wrapper for role-specific panels
     */
    private JFrame createRoleFrame(String title, JPanel panel) {
        JFrame frame = new JFrame("Lost & Found - " + title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        
        // Add menu bar with logout option
        JMenuBar menuBar = new JMenuBar();
        JMenu logoutMenu = new JMenu("Logout");
        logoutMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int confirm = JOptionPane.showConfirmDialog(frame, 
                    "Are you sure you want to logout?", 
                    "Confirm Logout", 
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    new LoginFrame().setVisible(true);
                }
            }
        });
        menuBar.add(logoutMenu);
        frame.setJMenuBar(menuBar);
        
        frame.add(panel);
        return frame;
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Apply global emoji font to all Swing components
        UIConstants.applyGlobalEmojiFont();

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
