package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.services.EnterpriseItemService;
import com.campus.lostfound.services.AnalyticsService;
import com.campus.lostfound.services.ReportExportService;
import com.campus.lostfound.ui.dialogs.RequestDetailDialog;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main panel for TSA Security Coordinator role at Logan Airport.
 * 
 * Features:
 * - Dashboard with checkpoint item metrics and federal compliance status
 * - Checkpoint item registration with prohibited item classification
 * - High-value item security with verification workflows
 * - Federal compliance documentation and audit trail
 * - Passenger/owner identification tools
 * - Multi-agency coordination (Police, Airport L&F)
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class TSASecurityCoordinatorPanel extends JPanel {
    
    // Data
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoUserDAO userDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private WorkRequestService workRequestService;
    private EnterpriseItemService enterpriseItemService;
    private AnalyticsService analyticsService;
    private ReportExportService reportExportService;
    
    // UI Components
    private JTabbedPane tabbedPane;
    
    // Dashboard components
    private JLabel checkpointItemsLabel;
    private JLabel prohibitedItemsLabel;
    private JLabel highValueItemsLabel;
    private JLabel pendingVerificationLabel;
    private JPanel complianceStatusPanel;
    private JPanel alertsPanel;
    
    // Checkpoint Items tab
    private JTextField checkpointTitleField;
    private JTextArea checkpointDescriptionArea;
    private JComboBox<Item.ItemCategory> checkpointCategoryCombo;
    private JComboBox<String> checkpointTerminalCombo;
    private JTextField checkpointLaneField;
    private JComboBox<String> checkpointTypeCombo;
    private JComboBox<String> prohibitedTypeCombo;
    private JCheckBox isProhibitedCheckBox;
    private JCheckBox requiresDisposalCheckBox;
    private JTextField checkpointOfficerIdField;
    private JTextField checkpointOfficerNameField;
    private JTextField passengerNameField;
    private JTextField flightNumberField;
    private JTextArea chainOfCustodyArea;
    private JLabel checkpointImagePreviewLabel;
    private List<String> checkpointImagePaths;
    
    // High-Value Security tab
    private JTable highValueItemsTable;
    private DefaultTableModel highValueItemsModel;
    private JTextField serialNumberField;
    private JTextArea verificationNotesArea;
    private JComboBox<String> verificationStatusCombo;
    private JPanel policeCheckPanel;
    
    // Federal Compliance tab
    private JTable auditLogTable;
    private DefaultTableModel auditLogModel;
    private JTable prohibitedItemsLogTable;
    private DefaultTableModel prohibitedItemsLogModel;
    private JPanel complianceChecklistPanel;
    private JTextArea disposalDocArea;
    
    // Owner Identification tab
    private JTextField ownerSearchNameField;
    private JTextField ownerSearchFlightField;
    private JTextField ownerSearchIdField;
    private JPanel passengerResultsPanel;
    private JTable contactAttemptsTable;
    private DefaultTableModel contactAttemptsModel;
    
    // Coordination tab
    private JPanel policeCoordinationPanel;
    private JPanel airportTransferPanel;
    private JTable incidentReportsTable;
    private DefaultTableModel incidentReportsModel;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    private static final Color PRIMARY_COLOR = new Color(0, 51, 102);       // TSA Blue
    private static final Color SECONDARY_COLOR = new Color(0, 82, 147);     // Lighter Blue
    private static final Color ACCENT_COLOR = new Color(192, 0, 0);         // TSA Red/Maroon
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    private static final Color FEDERAL_BLUE = new Color(0, 40, 104);        // Federal Blue
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 32);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);
    
    // TSA-specific data
    private static final String[] TERMINALS = {"Terminal A", "Terminal B", "Terminal C", "Terminal E"};
    private static final String[] CHECKPOINT_TYPES = {
        "Standard Screening", "PreCheck Lane", "CLEAR Lane", 
        "Random Selection", "Secondary Screening", "Exit Lane"
    };
    private static final String[] PROHIBITED_TYPES = {
        "Not Prohibited",
        "Sharp Objects (knives, box cutters)",
        "Sporting Goods (bats, golf clubs)",
        "Firearms/Ammunition",
        "Tools (over 7 inches)",
        "Martial Arts/Self Defense",
        "Explosive/Flammable",
        "Liquids (over 3.4oz)",
        "Other Prohibited Item"
    };
    private static final String[] VERIFICATION_STATUSES = {
        "Pending Verification", "Verified - Clear", "Verified - Stolen", 
        "Verification Failed", "Owner Identified", "Unclaimed"
    };
    private static final double HIGH_VALUE_THRESHOLD = 500.0;
    
    /**
     * Create a new TSASecurityCoordinatorPanel.
     * 
     * @param currentUser The logged-in TSA Security Coordinator
     */
    public TSASecurityCoordinatorPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.enterpriseItemService = new EnterpriseItemService();
        this.analyticsService = new AnalyticsService();
        this.reportExportService = new ReportExportService();
        this.checkpointImagePaths = new ArrayList<>();
        
        initComponents();
        loadDashboardData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Tabbed content
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        // Create tabs
        tabbedPane.addTab("🛡️ Dashboard", createDashboardTab());
        tabbedPane.addTab("🔒 Checkpoint Items", createCheckpointItemsTab());
        tabbedPane.addTab("💎 High-Value Security", createHighValueSecurityTab());
        tabbedPane.addTab("📜 Federal Compliance", createFederalComplianceTab());
        tabbedPane.addTab("🔍 Owner Identification", createOwnerIdentificationTab());
        tabbedPane.addTab("🤝 Coordination", createCoordinationTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 2 -> loadHighValueItems();
                case 3 -> loadComplianceData();
                case 4 -> loadContactAttempts();
                case 5 -> loadCoordinationData();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FEDERAL_BLUE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Welcome
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🛡️ TSA Security Operations");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("👤 " + currentUser.getFullName() + " • Security Coordinator");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 210, 230));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(SECONDARY_COLOR);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshAll());
        rightPanel.add(refreshBtn);
        
        JButton alertBtn = new JButton("🚨 Security Alert");
        alertBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        alertBtn.setForeground(Color.WHITE);
        alertBtn.setBackground(DANGER_COLOR);
        alertBtn.setBorderPainted(false);
        alertBtn.setFocusPainted(false);
        alertBtn.addActionListener(e -> openSecurityAlertDialog());
        rightPanel.add(alertBtn);
        
        JButton federalBtn = new JButton("🏛️ Federal Hotline");
        federalBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        federalBtn.setForeground(Color.WHITE);
        federalBtn.setBackground(ACCENT_COLOR);
        federalBtn.setBorderPainted(false);
        federalBtn.setFocusPainted(false);
        federalBtn.addActionListener(e -> showFederalContacts());
        rightPanel.add(federalBtn);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    // ==================== TAB 1: DASHBOARD ====================
    
    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Stats cards row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 15, 0));
        statsRow.setOpaque(false);
        
        // Checkpoint Items card
        JPanel checkpointCard = createStatCard("🔒", "Checkpoint Items", "0", SECONDARY_COLOR);
        checkpointItemsLabel = (JLabel) ((JPanel) checkpointCard.getComponent(1)).getComponent(0);
        statsRow.add(checkpointCard);
        
        // Prohibited Items card
        JPanel prohibitedCard = createStatCard("⛔", "Prohibited Items", "0", DANGER_COLOR);
        prohibitedItemsLabel = (JLabel) ((JPanel) prohibitedCard.getComponent(1)).getComponent(0);
        statsRow.add(prohibitedCard);
        
        // High-Value Items card
        JPanel highValueCard = createStatCard("💎", "High-Value ($500+)", "0", WARNING_COLOR);
        highValueItemsLabel = (JLabel) ((JPanel) highValueCard.getComponent(1)).getComponent(0);
        statsRow.add(highValueCard);
        
        // Pending Verification card
        JPanel verificationCard = createStatCard("🔍", "Pending Verification", "0", INFO_COLOR);
        pendingVerificationLabel = (JLabel) ((JPanel) verificationCard.getComponent(1)).getComponent(0);
        statsRow.add(verificationCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Left - Federal Compliance Status
        JPanel complianceContainer = new JPanel(new BorderLayout());
        complianceContainer.setOpaque(false);
        complianceContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(FEDERAL_BLUE, 2),
            "🏛️ Federal Compliance Status",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        complianceStatusPanel = new JPanel();
        complianceStatusPanel.setLayout(new BoxLayout(complianceStatusPanel, BoxLayout.Y_AXIS));
        complianceStatusPanel.setBackground(Color.WHITE);
        complianceStatusPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JScrollPane complianceScroll = new JScrollPane(complianceStatusPanel);
        complianceScroll.setBorder(null);
        complianceContainer.add(complianceScroll, BorderLayout.CENTER);
        
        contentPanel.add(complianceContainer);
        
        // Right - Alerts and Quick Actions
        JPanel rightSide = new JPanel(new GridLayout(2, 1, 0, 15));
        rightSide.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = new JPanel(new BorderLayout());
        alertsContainer.setOpaque(false);
        alertsContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DANGER_COLOR),
            "🚨 Security Alerts",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Color.WHITE);
        
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsContainer.add(alertsScroll, BorderLayout.CENTER);
        
        rightSide.add(alertsContainer);
        
        // Quick actions
        JPanel actionsContainer = new JPanel(new BorderLayout());
        actionsContainer.setOpaque(false);
        actionsContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "⚡ Quick Actions",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        JPanel actionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        actionsPanel.setBackground(Color.WHITE);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        actionsPanel.add(createQuickActionButton("🔒 Register Item", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("💎 High-Value Review", () -> tabbedPane.setSelectedIndex(2)));
        actionsPanel.add(createQuickActionButton("📜 Compliance Report", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("🚔 Contact Police", this::contactPoliceDialog));
        
        actionsContainer.add(actionsPanel, BorderLayout.CENTER);
        rightSide.add(actionsContainer);
        
        contentPanel.add(rightSide);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(EMOJI_FONT_LARGE);
        card.add(iconLabel, BorderLayout.WEST);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 28));
        valueLabel.setForeground(color);
        textPanel.add(valueLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        titleLabel.setForeground(new Color(108, 117, 125));
        textPanel.add(titleLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JButton createQuickActionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        button.setBackground(new Color(248, 249, 250));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(232, 244, 253));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(248, 249, 250));
            }
        });
        
        return button;
    }
    
    // ==================== TAB 2: CHECKPOINT ITEMS ====================
    
    private JPanel createCheckpointItemsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("🔒 Register Checkpoint Item");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Log items collected at TSA security checkpoints");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Item details section
        formContainer.add(createSectionLabel("Item Details"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 1: Title and Category
        JPanel row1 = new JPanel(new GridLayout(1, 2, 15, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel titlePanel = createFieldPanel("Item Title *");
        checkpointTitleField = new JTextField();
        titlePanel.add(checkpointTitleField);
        row1.add(titlePanel);
        
        JPanel categoryPanel = createFieldPanel("Category *");
        checkpointCategoryCombo = new JComboBox<>(Item.ItemCategory.values());
        checkpointCategoryCombo.setRenderer(new CategoryComboRenderer());
        categoryPanel.add(checkpointCategoryCombo);
        row1.add(categoryPanel);
        
        formContainer.add(row1);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Description
        formContainer.add(createFormLabel("Description *"));
        checkpointDescriptionArea = new JTextArea(3, 30);
        checkpointDescriptionArea.setLineWrap(true);
        checkpointDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(checkpointDescriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Checkpoint Location section
        formContainer.add(createSectionLabel("Checkpoint Location"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 2: Terminal, Lane, Checkpoint Type
        JPanel row2 = new JPanel(new GridLayout(1, 3, 15, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel terminalPanel = createFieldPanel("Terminal *");
        checkpointTerminalCombo = new JComboBox<>(TERMINALS);
        terminalPanel.add(checkpointTerminalCombo);
        row2.add(terminalPanel);
        
        JPanel lanePanel = createFieldPanel("Lane Number");
        checkpointLaneField = new JTextField();
        checkpointLaneField.setToolTipText("e.g., Lane 1, Lane 2A");
        lanePanel.add(checkpointLaneField);
        row2.add(lanePanel);
        
        JPanel typePanel = createFieldPanel("Checkpoint Type *");
        checkpointTypeCombo = new JComboBox<>(CHECKPOINT_TYPES);
        typePanel.add(checkpointTypeCombo);
        row2.add(typePanel);
        
        formContainer.add(row2);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Prohibited Item Classification
        formContainer.add(createSectionLabel("⛔ Prohibited Item Classification"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Prohibited item row
        JPanel prohibitedRow = new JPanel(new GridLayout(1, 2, 15, 0));
        prohibitedRow.setOpaque(false);
        prohibitedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        prohibitedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel prohibitedTypePanel = createFieldPanel("Prohibited Type");
        prohibitedTypeCombo = new JComboBox<>(PROHIBITED_TYPES);
        prohibitedTypeCombo.addActionListener(e -> {
            String selected = (String) prohibitedTypeCombo.getSelectedItem();
            isProhibitedCheckBox.setSelected(!selected.equals("Not Prohibited"));
        });
        prohibitedTypePanel.add(prohibitedTypeCombo);
        prohibitedRow.add(prohibitedTypePanel);
        
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setOpaque(false);
        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        isProhibitedCheckBox = new JCheckBox("⛔ Confirmed Prohibited Item");
        isProhibitedCheckBox.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        isProhibitedCheckBox.setForeground(DANGER_COLOR);
        isProhibitedCheckBox.setOpaque(false);
        checkboxPanel.add(isProhibitedCheckBox);
        
        requiresDisposalCheckBox = new JCheckBox("🗑️ Requires Immediate Disposal");
        requiresDisposalCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        requiresDisposalCheckBox.setOpaque(false);
        checkboxPanel.add(requiresDisposalCheckBox);
        
        prohibitedRow.add(checkboxPanel);
        
        formContainer.add(prohibitedRow);
        formContainer.add(Box.createVerticalStrut(20));
        
        // TSA Officer Information
        formContainer.add(createSectionLabel("TSA Officer Information"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 3: Officer ID and Name
        JPanel row3 = new JPanel(new GridLayout(1, 2, 15, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel officerIdPanel = createFieldPanel("Officer Badge ID *");
        checkpointOfficerIdField = new JTextField();
        officerIdPanel.add(checkpointOfficerIdField);
        row3.add(officerIdPanel);
        
        JPanel officerNamePanel = createFieldPanel("Officer Name *");
        checkpointOfficerNameField = new JTextField(currentUser.getFullName());
        officerNamePanel.add(checkpointOfficerNameField);
        row3.add(officerNamePanel);
        
        formContainer.add(row3);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Passenger Information (if available)
        formContainer.add(createSectionLabel("Passenger Information (if available)"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 4: Passenger Name and Flight
        JPanel row4 = new JPanel(new GridLayout(1, 2, 15, 0));
        row4.setOpaque(false);
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row4.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel passengerPanel = createFieldPanel("Passenger Name");
        passengerNameField = new JTextField();
        passengerPanel.add(passengerNameField);
        row4.add(passengerPanel);
        
        JPanel flightPanel = createFieldPanel("Flight Number");
        flightNumberField = new JTextField();
        flightNumberField.setToolTipText("e.g., DL1234, B6789");
        flightPanel.add(flightNumberField);
        row4.add(flightPanel);
        
        formContainer.add(row4);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Chain of Custody
        formContainer.add(createSectionLabel("🔗 Chain of Custody Notes"));
        formContainer.add(Box.createVerticalStrut(10));
        
        chainOfCustodyArea = new JTextArea(3, 30);
        chainOfCustodyArea.setLineWrap(true);
        chainOfCustodyArea.setWrapStyleWord(true);
        chainOfCustodyArea.setText("Item collected at checkpoint by [Officer Name] on [Date/Time].\nWitness: [If applicable]");
        JScrollPane custodyScroll = new JScrollPane(chainOfCustodyArea);
        custodyScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        custodyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(custodyScroll);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Image upload
        formContainer.add(createFormLabel("Photo Documentation"));
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imagePanel.setOpaque(false);
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        checkpointImagePreviewLabel = new JLabel("No image");
        checkpointImagePreviewLabel.setPreferredSize(new Dimension(100, 100));
        checkpointImagePreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        checkpointImagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        checkpointImagePreviewLabel.setBackground(new Color(248, 249, 250));
        checkpointImagePreviewLabel.setOpaque(true);
        imagePanel.add(checkpointImagePreviewLabel);
        
        imagePanel.add(Box.createHorizontalStrut(15));
        
        JButton uploadButton = new JButton("📷 Upload Photo");
        uploadButton.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        uploadButton.addActionListener(e -> selectCheckpointImage());
        imagePanel.add(uploadButton);
        
        JButton clearImageButton = new JButton("Clear");
        clearImageButton.addActionListener(e -> clearCheckpointImage());
        imagePanel.add(Box.createHorizontalStrut(10));
        imagePanel.add(clearImageButton);
        
        formContainer.add(imagePanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton registerButton = new JButton("🔒 Register Item");
        registerButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        registerButton.setBackground(SUCCESS_COLOR);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setPreferredSize(new Dimension(160, 45));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> registerCheckpointItem());
        buttonPanel.add(registerButton);
        
        JButton transferToLFButton = new JButton("✈️ Register & Transfer to L&F");
        transferToLFButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        transferToLFButton.setBackground(PRIMARY_COLOR);
        transferToLFButton.setForeground(Color.WHITE);
        transferToLFButton.setFocusPainted(false);
        transferToLFButton.setBorderPainted(false);
        transferToLFButton.setPreferredSize(new Dimension(240, 45));
        transferToLFButton.addActionListener(e -> registerAndTransferToLF());
        buttonPanel.add(transferToLFButton);
        
        JButton clearButton = new JButton("Clear Form");
        clearButton.setPreferredSize(new Dimension(120, 45));
        clearButton.addActionListener(e -> clearCheckpointForm());
        buttonPanel.add(clearButton);
        
        formContainer.add(buttonPanel);
        
        // Center the form
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrapper.setOpaque(false);
        centerWrapper.add(formContainer);
        
        JScrollPane scrollPane = new JScrollPane(centerWrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 3: HIGH-VALUE SECURITY ====================
    
    private JPanel createHighValueSecurityTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("💎 High-Value Items Security ($" + (int)HIGH_VALUE_THRESHOLD + "+)");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> loadHighValueItems());
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        splitPane.setBorder(null);
        
        // Left - High-value items table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder("High-Value Items Queue"));
        
        String[] columns = {"Item", "Category", "Est. Value", "Status", "Serial #", "Date"};
        highValueItemsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        highValueItemsTable = new JTable(highValueItemsModel);
        highValueItemsTable.setRowHeight(35);
        highValueItemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        highValueItemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        highValueItemsTable.getTableHeader().setBackground(FEDERAL_BLUE);
        highValueItemsTable.getTableHeader().setForeground(Color.BLACK);
        highValueItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        highValueItemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedItemVerification();
            }
        });
        
        // Column widths
        TableColumnModel tcm = highValueItemsTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(150);
        tcm.getColumn(1).setPreferredWidth(100);
        tcm.getColumn(2).setPreferredWidth(80);
        tcm.getColumn(3).setPreferredWidth(120);
        tcm.getColumn(4).setPreferredWidth(100);
        tcm.getColumn(5).setPreferredWidth(90);
        
        JScrollPane tableScroll = new JScrollPane(highValueItemsTable);
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(tablePanel);
        
        // Right - Verification panel
        JPanel verificationPanel = new JPanel();
        verificationPanel.setLayout(new BoxLayout(verificationPanel, BoxLayout.Y_AXIS));
        verificationPanel.setBorder(BorderFactory.createTitledBorder("🔍 Verification & Police Check"));
        verificationPanel.setBackground(Color.WHITE);
        
        verificationPanel.add(Box.createVerticalStrut(15));
        verificationPanel.add(createFormLabel("Serial Number / Identifier"));
        serialNumberField = new JTextField();
        serialNumberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        serialNumberField.setAlignmentX(Component.LEFT_ALIGNMENT);
        verificationPanel.add(serialNumberField);
        verificationPanel.add(Box.createVerticalStrut(15));
        
        verificationPanel.add(createFormLabel("Verification Status"));
        verificationStatusCombo = new JComboBox<>(VERIFICATION_STATUSES);
        verificationStatusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        verificationStatusCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        verificationPanel.add(verificationStatusCombo);
        verificationPanel.add(Box.createVerticalStrut(15));
        
        verificationPanel.add(createFormLabel("Verification Notes"));
        verificationNotesArea = new JTextArea(5, 20);
        verificationNotesArea.setLineWrap(true);
        verificationNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(verificationNotesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        verificationPanel.add(notesScroll);
        verificationPanel.add(Box.createVerticalStrut(15));
        
        // Police database check panel
        policeCheckPanel = new JPanel();
        policeCheckPanel.setLayout(new BoxLayout(policeCheckPanel, BoxLayout.Y_AXIS));
        policeCheckPanel.setOpaque(false);
        policeCheckPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        policeCheckPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(FEDERAL_BLUE),
                "🚔 Police Database Check"
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JButton checkPoliceDbBtn = new JButton("🔍 Check Stolen Property Database");
        checkPoliceDbBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        checkPoliceDbBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkPoliceDbBtn.addActionListener(e -> performPoliceDbCheck());
        policeCheckPanel.add(checkPoliceDbBtn);
        
        JLabel policeResultLabel = new JLabel("No check performed");
        policeResultLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        policeResultLabel.setForeground(new Color(108, 117, 125));
        policeResultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        policeCheckPanel.add(Box.createVerticalStrut(10));
        policeCheckPanel.add(policeResultLabel);
        
        verificationPanel.add(policeCheckPanel);
        verificationPanel.add(Box.createVerticalStrut(15));
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton saveVerificationBtn = new JButton("💾 Save Verification");
        saveVerificationBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        saveVerificationBtn.setBackground(SUCCESS_COLOR);
        saveVerificationBtn.setForeground(Color.WHITE);
        saveVerificationBtn.setBorderPainted(false);
        saveVerificationBtn.addActionListener(e -> saveVerification());
        actionPanel.add(saveVerificationBtn);
        
        JButton secureStorageBtn = new JButton("🔐 Secure Storage");
        secureStorageBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        secureStorageBtn.addActionListener(e -> assignSecureStorage());
        actionPanel.add(secureStorageBtn);
        
        JButton releaseBtn = new JButton("✅ Authorize Release");
        releaseBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        releaseBtn.addActionListener(e -> authorizeRelease());
        actionPanel.add(releaseBtn);
        
        verificationPanel.add(actionPanel);
        
        JScrollPane verificationScroll = new JScrollPane(verificationPanel);
        verificationScroll.setBorder(null);
        splitPane.setRightComponent(verificationScroll);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 4: FEDERAL COMPLIANCE ====================
    
    private JPanel createFederalComplianceTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header with federal styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(FEDERAL_BLUE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🏛️ Federal Compliance & Audit Documentation");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        exportPanel.setOpaque(false);
        
        JButton exportBtn = new JButton("📄 Export Compliance Report");
        exportBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        exportBtn.setForeground(FEDERAL_BLUE);
        exportBtn.addActionListener(e -> exportComplianceReport());
        exportPanel.add(exportBtn);
        
        headerPanel.add(exportPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Main content - split into sections
        JPanel contentPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        contentPanel.setOpaque(false);
        
        // Section 1: Compliance Checklist
        JPanel checklistPanel = new JPanel(new BorderLayout());
        checklistPanel.setOpaque(false);
        checklistPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SUCCESS_COLOR),
            "✅ Daily Compliance Checklist"
        ));
        
        complianceChecklistPanel = new JPanel();
        complianceChecklistPanel.setLayout(new BoxLayout(complianceChecklistPanel, BoxLayout.Y_AXIS));
        complianceChecklistPanel.setBackground(Color.WHITE);
        complianceChecklistPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] checklistItems = {
            "All checkpoint items logged within 1 hour",
            "Prohibited items properly classified",
            "High-value items placed in secure storage",
            "Chain of custody documented",
            "Photo documentation completed",
            "Daily report submitted to supervisor"
        };
        
        for (String item : checklistItems) {
            JCheckBox checkBox = new JCheckBox(item);
            checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            checkBox.setOpaque(false);
            complianceChecklistPanel.add(checkBox);
            complianceChecklistPanel.add(Box.createVerticalStrut(5));
        }
        
        JScrollPane checklistScroll = new JScrollPane(complianceChecklistPanel);
        checklistScroll.setBorder(null);
        checklistPanel.add(checklistScroll, BorderLayout.CENTER);
        
        contentPanel.add(checklistPanel);
        
        // Section 2: Prohibited Items Log
        JPanel prohibitedLogPanel = new JPanel(new BorderLayout());
        prohibitedLogPanel.setOpaque(false);
        prohibitedLogPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DANGER_COLOR),
            "⛔ Prohibited Items Log"
        ));
        
        String[] prohibitedColumns = {"Date", "Item", "Type", "Disposition", "Officer"};
        prohibitedItemsLogModel = new DefaultTableModel(prohibitedColumns, 0);
        
        prohibitedItemsLogTable = new JTable(prohibitedItemsLogModel);
        prohibitedItemsLogTable.setRowHeight(30);
        prohibitedItemsLogTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        prohibitedItemsLogTable.getTableHeader().setBackground(DANGER_COLOR);
        prohibitedItemsLogTable.getTableHeader().setForeground(Color.BLACK);
        
        JScrollPane prohibitedScroll = new JScrollPane(prohibitedItemsLogTable);
        prohibitedLogPanel.add(prohibitedScroll, BorderLayout.CENTER);
        
        contentPanel.add(prohibitedLogPanel);
        
        // Section 3: Disposal Documentation
        JPanel disposalPanel = new JPanel(new BorderLayout());
        disposalPanel.setOpaque(false);
        disposalPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WARNING_COLOR),
            "🗑️ Disposal Documentation"
        ));
        
        disposalDocArea = new JTextArea();
        disposalDocArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        disposalDocArea.setLineWrap(true);
        disposalDocArea.setWrapStyleWord(true);
        disposalDocArea.setText("Disposal Record:\n\n" +
            "Item ID: _____________\n" +
            "Disposal Method: _____________\n" +
            "Witness 1: _____________\n" +
            "Witness 2: _____________\n" +
            "Date/Time: _____________\n" +
            "Supervisor Approval: _____________\n\n" +
            "Notes: _____________");
        
        JScrollPane disposalScroll = new JScrollPane(disposalDocArea);
        
        JPanel disposalBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        disposalBtnPanel.setOpaque(false);
        JButton saveDisposalBtn = new JButton("💾 Save Record");
        saveDisposalBtn.addActionListener(e -> saveDisposalRecord());
        disposalBtnPanel.add(saveDisposalBtn);
        
        disposalPanel.add(disposalScroll, BorderLayout.CENTER);
        disposalPanel.add(disposalBtnPanel, BorderLayout.SOUTH);
        
        contentPanel.add(disposalPanel);
        
        // Section 4: Audit Trail
        JPanel auditPanel = new JPanel(new BorderLayout());
        auditPanel.setOpaque(false);
        auditPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(INFO_COLOR),
            "📋 Audit Trail"
        ));
        
        String[] auditColumns = {"Timestamp", "Action", "User", "Details"};
        auditLogModel = new DefaultTableModel(auditColumns, 0);
        
        auditLogTable = new JTable(auditLogModel);
        auditLogTable.setRowHeight(28);
        auditLogTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        auditLogTable.getTableHeader().setBackground(INFO_COLOR);
        auditLogTable.getTableHeader().setForeground(Color.BLACK);
        
        JScrollPane auditScroll = new JScrollPane(auditLogTable);
        auditPanel.add(auditScroll, BorderLayout.CENTER);
        
        contentPanel.add(auditPanel);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 5: OWNER IDENTIFICATION ====================
    
    private JPanel createOwnerIdentificationTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header with search
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🔍 Passenger/Owner Identification");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Search panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(new JLabel("Passenger Name:"), gbc);
        
        gbc.gridx = 1;
        ownerSearchNameField = new JTextField(15);
        searchPanel.add(ownerSearchNameField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("Flight Number:"), gbc);
        
        gbc.gridx = 3;
        ownerSearchFlightField = new JTextField(10);
        searchPanel.add(ownerSearchFlightField, gbc);
        
        gbc.gridx = 4;
        searchPanel.add(new JLabel("ID/Badge #:"), gbc);
        
        gbc.gridx = 5;
        ownerSearchIdField = new JTextField(10);
        searchPanel.add(ownerSearchIdField, gbc);
        
        gbc.gridx = 6;
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        searchBtn.setBackground(PRIMARY_COLOR);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setBorderPainted(false);
        searchBtn.addActionListener(e -> performOwnerSearch());
        searchPanel.add(searchBtn, gbc);
        
        headerPanel.add(searchPanel, BorderLayout.CENTER);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split panel for results and contact log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setBorder(null);
        
        // Top - Passenger results
        JPanel resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setBorder(BorderFactory.createTitledBorder("Search Results / Flight Manifest"));
        
        passengerResultsPanel = new JPanel();
        passengerResultsPanel.setLayout(new BoxLayout(passengerResultsPanel, BoxLayout.Y_AXIS));
        passengerResultsPanel.setBackground(Color.WHITE);
        
        JLabel placeholder = new JLabel("Enter search criteria to find passenger information");
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        placeholder.setForeground(new Color(108, 117, 125));
        placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
        passengerResultsPanel.add(Box.createVerticalStrut(50));
        passengerResultsPanel.add(placeholder);
        
        JScrollPane resultsScroll = new JScrollPane(passengerResultsPanel);
        resultsScroll.setBorder(null);
        resultsContainer.add(resultsScroll, BorderLayout.CENTER);
        
        // ID Verification buttons
        JPanel idVerifyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        idVerifyPanel.setOpaque(false);
        
        JButton verifyIdBtn = new JButton("🆔 Verify ID");
        verifyIdBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        verifyIdBtn.addActionListener(e -> showIdVerificationDialog());
        idVerifyPanel.add(verifyIdBtn);
        
        JButton manifestBtn = new JButton("📋 View Manifest");
        manifestBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        manifestBtn.addActionListener(e -> showFlightManifest());
        idVerifyPanel.add(manifestBtn);
        
        JButton contactBtn = new JButton("📞 Log Contact Attempt");
        contactBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        contactBtn.addActionListener(e -> logContactAttempt());
        idVerifyPanel.add(contactBtn);
        
        resultsContainer.add(idVerifyPanel, BorderLayout.SOUTH);
        
        splitPane.setTopComponent(resultsContainer);
        
        // Bottom - Contact attempts log
        JPanel contactLogPanel = new JPanel(new BorderLayout());
        contactLogPanel.setBorder(BorderFactory.createTitledBorder("📞 Contact Attempt Log"));
        
        String[] contactColumns = {"Date/Time", "Method", "Passenger", "Result", "Notes", "Officer"};
        contactAttemptsModel = new DefaultTableModel(contactColumns, 0);
        
        contactAttemptsTable = new JTable(contactAttemptsModel);
        contactAttemptsTable.setRowHeight(30);
        contactAttemptsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        contactAttemptsTable.getTableHeader().setBackground(PRIMARY_COLOR);
        contactAttemptsTable.getTableHeader().setForeground(Color.BLACK);
        
        JScrollPane contactScroll = new JScrollPane(contactAttemptsTable);
        contactLogPanel.add(contactScroll, BorderLayout.CENTER);
        
        splitPane.setBottomComponent(contactLogPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 6: COORDINATION ====================
    
    private JPanel createCoordinationTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel titleLabel = new JLabel("🤝 Multi-Agency Coordination");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Main content - 3 sections
        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        contentPanel.setOpaque(false);
        
        // Section 1: Police Coordination
        JPanel policePanel = new JPanel(new BorderLayout());
        policePanel.setOpaque(false);
        policePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(FEDERAL_BLUE, 2),
            "🚔 Police Coordination"
        ));
        
        policeCoordinationPanel = new JPanel();
        policeCoordinationPanel.setLayout(new BoxLayout(policeCoordinationPanel, BoxLayout.Y_AXIS));
        policeCoordinationPanel.setBackground(Color.WHITE);
        policeCoordinationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add police queue items
        JLabel policeQueueLabel = new JLabel("Items Pending Police Review: 0");
        policeQueueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        policeQueueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        policeCoordinationPanel.add(policeQueueLabel);
        policeCoordinationPanel.add(Box.createVerticalStrut(15));
        
        JButton escalateBtn = new JButton("🚨 Escalate to Massport PD");
        escalateBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        escalateBtn.setBackground(DANGER_COLOR);
        escalateBtn.setForeground(Color.WHITE);
        escalateBtn.setBorderPainted(false);
        escalateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        escalateBtn.addActionListener(e -> escalateToPolice());
        policeCoordinationPanel.add(escalateBtn);
        policeCoordinationPanel.add(Box.createVerticalStrut(10));
        
        JButton requestPickupBtn = new JButton("📋 Request Evidence Pickup");
        requestPickupBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        requestPickupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        requestPickupBtn.addActionListener(e -> requestEvidencePickup());
        policeCoordinationPanel.add(requestPickupBtn);
        policeCoordinationPanel.add(Box.createVerticalStrut(10));
        
        JButton callPoliceBtn = new JButton("📞 Contact Massport Police");
        callPoliceBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        callPoliceBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        callPoliceBtn.addActionListener(e -> contactPoliceDialog());
        policeCoordinationPanel.add(callPoliceBtn);
        
        JScrollPane policeScroll = new JScrollPane(policeCoordinationPanel);
        policeScroll.setBorder(null);
        policePanel.add(policeScroll, BorderLayout.CENTER);
        
        contentPanel.add(policePanel);
        
        // Section 2: Airport L&F Transfer
        JPanel transferPanel = new JPanel(new BorderLayout());
        transferPanel.setOpaque(false);
        transferPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
            "✈️ Airport Lost & Found Transfer"
        ));
        
        airportTransferPanel = new JPanel();
        airportTransferPanel.setLayout(new BoxLayout(airportTransferPanel, BoxLayout.Y_AXIS));
        airportTransferPanel.setBackground(Color.WHITE);
        airportTransferPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel transferQueueLabel = new JLabel("Pending Transfers to L&F: 0");
        transferQueueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        transferQueueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        airportTransferPanel.add(transferQueueLabel);
        airportTransferPanel.add(Box.createVerticalStrut(15));
        
        JButton newTransferBtn = new JButton("📤 Create Transfer Request");
        newTransferBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        newTransferBtn.setBackground(PRIMARY_COLOR);
        newTransferBtn.setForeground(Color.WHITE);
        newTransferBtn.setBorderPainted(false);
        newTransferBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        newTransferBtn.addActionListener(e -> createAirportTransfer());
        airportTransferPanel.add(newTransferBtn);
        airportTransferPanel.add(Box.createVerticalStrut(10));
        
        JButton viewTransfersBtn = new JButton("📋 View Pending Transfers");
        viewTransfersBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        viewTransfersBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        viewTransfersBtn.addActionListener(e -> viewPendingTransfers());
        airportTransferPanel.add(viewTransfersBtn);
        airportTransferPanel.add(Box.createVerticalStrut(10));
        
        JButton contactLFBtn = new JButton("📞 Contact Airport L&F");
        contactLFBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        contactLFBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        contactLFBtn.addActionListener(e -> contactAirportLF());
        airportTransferPanel.add(contactLFBtn);
        
        JScrollPane transferScroll = new JScrollPane(airportTransferPanel);
        transferScroll.setBorder(null);
        transferPanel.add(transferScroll, BorderLayout.CENTER);
        
        contentPanel.add(transferPanel);
        
        // Section 3: Incident Reports
        JPanel incidentPanel = new JPanel(new BorderLayout());
        incidentPanel.setOpaque(false);
        incidentPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WARNING_COLOR, 2),
            "📝 Incident Reports"
        ));
        
        String[] incidentColumns = {"ID", "Date", "Type", "Status"};
        incidentReportsModel = new DefaultTableModel(incidentColumns, 0);
        
        incidentReportsTable = new JTable(incidentReportsModel);
        incidentReportsTable.setRowHeight(28);
        incidentReportsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        incidentReportsTable.getTableHeader().setBackground(WARNING_COLOR);
        incidentReportsTable.getTableHeader().setForeground(Color.BLACK);
        
        JScrollPane incidentScroll = new JScrollPane(incidentReportsTable);
        
        JPanel incidentBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        incidentBtnPanel.setOpaque(false);
        
        JButton newIncidentBtn = new JButton("➕ New Report");
        newIncidentBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
        newIncidentBtn.addActionListener(e -> createIncidentReport());
        incidentBtnPanel.add(newIncidentBtn);
        
        JButton viewIncidentBtn = new JButton("👁️ View");
        viewIncidentBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
        viewIncidentBtn.addActionListener(e -> viewSelectedIncident());
        incidentBtnPanel.add(viewIncidentBtn);
        
        incidentPanel.add(incidentScroll, BorderLayout.CENTER);
        incidentPanel.add(incidentBtnPanel, BorderLayout.SOUTH);
        
        contentPanel.add(incidentPanel);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Emergency contacts footer
        JPanel emergencyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        emergencyPanel.setBackground(new Color(248, 249, 250));
        emergencyPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        emergencyPanel.add(createEmergencyContactLabel("🚔 Massport PD", "617-568-5000"));
        emergencyPanel.add(createEmergencyContactLabel("🛡️ TSA Command", "617-568-4500"));
        emergencyPanel.add(createEmergencyContactLabel("✈️ Airport L&F", "617-561-1700"));
        emergencyPanel.add(createEmergencyContactLabel("🚨 Emergency", "911"));
        
        panel.add(emergencyPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JLabel createEmergencyContactLabel(String name, String number) {
        JLabel label = new JLabel(name + ": " + number);
        label.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        label.setForeground(FEDERAL_BLUE);
        return label;
    }
    
    // ==================== HELPER METHODS ====================
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(73, 80, 87));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return label;
    }
    
    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        label.setForeground(FEDERAL_BLUE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(0, 0, 8, 0)
        ));
        return label;
    }
    
    private JPanel createFieldPanel(String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(createFormLabel(labelText));
        return panel;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                List<Item> myItems = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // Checkpoint items (items with TSA keywords)
                int checkpointItems = (int) myItems.stream()
                    .filter(i -> i.getKeywords() != null && 
                                i.getKeywords().stream().anyMatch(k -> 
                                    k.toLowerCase().contains("checkpoint") || 
                                    k.toLowerCase().contains("tsa") ||
                                    k.toLowerCase().contains("security")))
                    .count();
                
                // Prohibited items
                int prohibitedItems = (int) myItems.stream()
                    .filter(i -> i.getKeywords() != null && 
                                i.getKeywords().stream().anyMatch(k -> 
                                    k.toLowerCase().contains("prohibited")))
                    .count();
                
                // High-value items
                int highValueItems = (int) myItems.stream()
                    .filter(i -> i.getEstimatedValue() >= HIGH_VALUE_THRESHOLD)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .count();
                
                // Pending verification (items needing verification)
                int pendingVerification = (int) myItems.stream()
                    .filter(i -> i.getEstimatedValue() >= HIGH_VALUE_THRESHOLD)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN ||
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM)
                    .count();
                
                return new int[]{checkpointItems, prohibitedItems, highValueItems, pendingVerification};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    checkpointItemsLabel.setText(String.valueOf(stats[0]));
                    prohibitedItemsLabel.setText(String.valueOf(stats[1]));
                    highValueItemsLabel.setText(String.valueOf(stats[2]));
                    pendingVerificationLabel.setText(String.valueOf(stats[3]));
                    
                    loadComplianceStatus();
                    loadAlerts(stats);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadComplianceStatus() {
        complianceStatusPanel.removeAll();
        
        // Add compliance status items
        complianceStatusPanel.add(createComplianceItem("✅", "Daily Log Submission", "Compliant", SUCCESS_COLOR));
        complianceStatusPanel.add(Box.createVerticalStrut(10));
        complianceStatusPanel.add(createComplianceItem("✅", "Prohibited Item Documentation", "Compliant", SUCCESS_COLOR));
        complianceStatusPanel.add(Box.createVerticalStrut(10));
        complianceStatusPanel.add(createComplianceItem("⚠️", "High-Value Item Verification", "3 Pending", WARNING_COLOR));
        complianceStatusPanel.add(Box.createVerticalStrut(10));
        complianceStatusPanel.add(createComplianceItem("✅", "Chain of Custody Records", "Complete", SUCCESS_COLOR));
        complianceStatusPanel.add(Box.createVerticalStrut(10));
        complianceStatusPanel.add(createComplianceItem("✅", "TSA Reporting Requirements", "Up to Date", SUCCESS_COLOR));
        
        complianceStatusPanel.revalidate();
        complianceStatusPanel.repaint();
    }
    
    private JPanel createComplianceItem(String icon, String label, String status, Color statusColor) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(EMOJI_FONT_MEDIUM);
        panel.add(iconLabel, BorderLayout.WEST);
        
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(nameLabel, BorderLayout.CENTER);
        
        JLabel statusLabel = new JLabel(status);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(statusColor);
        panel.add(statusLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void loadAlerts(int[] stats) {
        alertsPanel.removeAll();
        
        if (stats[3] > 0) {
            alertsPanel.add(createAlertItem("🔍", stats[3] + " high-value item(s) pending verification", WARNING_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[1] > 0) {
            alertsPanel.add(createAlertItem("⛔", stats[1] + " prohibited item(s) require documentation", DANGER_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Standard reminder
        alertsPanel.add(createAlertItem("📋", "Remember: Document all checkpoint items within 1 hour", INFO_COLOR));
        
        alertsPanel.revalidate();
        alertsPanel.repaint();
    }
    
    private JPanel createAlertItem(String icon, String message, Color color) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(EMOJI_FONT_MEDIUM);
        panel.add(iconLabel, BorderLayout.WEST);
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadHighValueItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getEstimatedValue() >= HIGH_VALUE_THRESHOLD)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN ||
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM)
                    .sorted((a, b) -> Double.compare(b.getEstimatedValue(), a.getEstimatedValue()))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    highValueItemsModel.setRowCount(0);
                    
                    for (Item item : items) {
                        String serialNum = "";
                        if (item.getKeywords() != null) {
                            for (String kw : item.getKeywords()) {
                                if (kw.startsWith("SN:") || kw.startsWith("Serial:")) {
                                    serialNum = kw.substring(kw.indexOf(":") + 1).trim();
                                    break;
                                }
                            }
                        }
                        
                        highValueItemsModel.addRow(new Object[]{
                            item.getTitle(),
                            item.getCategory().getDisplayName(),
                            String.format("$%.2f", item.getEstimatedValue()),
                            item.getStatus().getLabel(),
                            serialNum.isEmpty() ? "Not recorded" : serialNum,
                            DATE_FORMAT.format(item.getReportedDate())
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadSelectedItemVerification() {
        int selectedRow = highValueItemsTable.getSelectedRow();
        if (selectedRow < 0) return;
        
        String serialNum = (String) highValueItemsModel.getValueAt(selectedRow, 4);
        if (!serialNum.equals("Not recorded")) {
            serialNumberField.setText(serialNum);
        } else {
            serialNumberField.setText("");
        }
        
        verificationNotesArea.setText("Item: " + highValueItemsModel.getValueAt(selectedRow, 0) + "\n" +
            "Value: " + highValueItemsModel.getValueAt(selectedRow, 2) + "\n\n" +
            "Verification Notes:\n");
        verificationStatusCombo.setSelectedIndex(0);
    }
    
    private void loadComplianceData() {
        // Load prohibited items log
        prohibitedItemsLogModel.setRowCount(0);
        
        List<Item> prohibitedItems = itemDAO.findAll().stream()
            .filter(i -> i.getOrganizationId() != null && 
                        i.getOrganizationId().equals(currentUser.getOrganizationId()))
            .filter(i -> i.getKeywords() != null && 
                        i.getKeywords().stream().anyMatch(k -> k.toLowerCase().contains("prohibited")))
            .collect(Collectors.toList());
        
        for (Item item : prohibitedItems) {
            String prohibitedType = "Unknown";
            if (item.getKeywords() != null) {
                for (String kw : item.getKeywords()) {
                    if (kw.contains("Sharp") || kw.contains("Sporting") || 
                        kw.contains("Firearm") || kw.contains("Liquid")) {
                        prohibitedType = kw;
                        break;
                    }
                }
            }
            
            prohibitedItemsLogModel.addRow(new Object[]{
                DATE_FORMAT.format(item.getReportedDate()),
                item.getTitle(),
                prohibitedType,
                item.getStatus().getLabel(),
                currentUser.getFullName()
            });
        }
        
        // Load audit log
        auditLogModel.setRowCount(0);
        
        // Add sample audit entries
        auditLogModel.addRow(new Object[]{
            DATETIME_FORMAT.format(new Date()),
            "Item Registered",
            currentUser.getFullName(),
            "New checkpoint item logged"
        });
        auditLogModel.addRow(new Object[]{
            DATETIME_FORMAT.format(new Date(System.currentTimeMillis() - 3600000)),
            "Verification Complete",
            currentUser.getFullName(),
            "High-value item verified"
        });
    }
    
    private void loadContactAttempts() {
        contactAttemptsModel.setRowCount(0);
        
        // Sample contact attempts
        contactAttemptsModel.addRow(new Object[]{
            DATETIME_FORMAT.format(new Date()),
            "Phone Call",
            "John Smith",
            "No Answer",
            "Left voicemail",
            currentUser.getFullName()
        });
    }
    
    private void loadCoordinationData() {
        // Load incident reports
        incidentReportsModel.setRowCount(0);
        
        incidentReportsModel.addRow(new Object[]{
            "TSA-" + (System.currentTimeMillis() % 10000),
            DATE_FORMAT.format(new Date()),
            "Prohibited Item",
            "Documented"
        });
    }
    
    // ==================== ACTIONS ====================
    
    private void registerCheckpointItem() {
        if (!validateCheckpointForm()) return;
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = createItemFromCheckpointForm();
                    return itemDAO.create(item);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    String itemId = get();
                    if (itemId != null && !itemId.isEmpty()) {
                        // Log audit entry
                        addAuditEntry("Item Registered", "Checkpoint item: " + checkpointTitleField.getText());
                        
                        JOptionPane.showMessageDialog(TSASecurityCoordinatorPanel.this,
                            "Item registered successfully!\n" +
                            "Item ID: " + itemId + "\n\n" +
                            (isProhibitedCheckBox.isSelected() ? 
                                "⛔ PROHIBITED ITEM - Please complete disposal documentation" : 
                                "Item logged in TSA checkpoint inventory"),
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        clearCheckpointForm();
                        loadDashboardData();
                    } else {
                        showError("Failed to register item. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void registerAndTransferToLF() {
        if (!validateCheckpointForm()) return;
        
        if (isProhibitedCheckBox.isSelected()) {
            showError("Prohibited items cannot be transferred to Lost & Found.\n" +
                     "Please process through disposal documentation.");
            return;
        }
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = createItemFromCheckpointForm();
                    List<String> kw = item.getKeywords();
                    if (kw == null) kw = new ArrayList<>();
                    kw.add("Transfer to Airport L&F");
                    item.setKeywords(kw);
                    return itemDAO.create(item);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    String itemId = get();
                    if (itemId != null && !itemId.isEmpty()) {
                        addAuditEntry("Item Registered & Transferred", 
                            "Item transferred to Airport L&F: " + checkpointTitleField.getText());
                        
                        JOptionPane.showMessageDialog(TSASecurityCoordinatorPanel.this,
                            "Item registered and marked for transfer!\n" +
                            "Item ID: " + itemId + "\n\n" +
                            "Please coordinate with Airport Lost & Found for pickup.",
                            "Transfer Initiated",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        clearCheckpointForm();
                        loadDashboardData();
                    } else {
                        showError("Failed to register item.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private Item createItemFromCheckpointForm() {
        Item item = new Item(
            checkpointTitleField.getText().trim(),
            checkpointDescriptionArea.getText().trim(),
            (Item.ItemCategory) checkpointCategoryCombo.getSelectedItem(),
            Item.ItemType.FOUND,
            null,
            currentUser
        );
        
        item.setStatus(Item.ItemStatus.OPEN);
        item.setEnterpriseId(currentUser.getEnterpriseId());
        item.setOrganizationId(currentUser.getOrganizationId());
        
        // Add checkpoint context to keywords
        List<String> keywords = new ArrayList<>();
        keywords.add("TSA Checkpoint");
        keywords.add((String) checkpointTerminalCombo.getSelectedItem());
        keywords.add((String) checkpointTypeCombo.getSelectedItem());
        
        if (!checkpointLaneField.getText().trim().isEmpty()) {
            keywords.add("Lane " + checkpointLaneField.getText().trim());
        }
        
        // Prohibited item classification
        String prohibitedType = (String) prohibitedTypeCombo.getSelectedItem();
        if (!prohibitedType.equals("Not Prohibited")) {
            keywords.add("Prohibited: " + prohibitedType);
        }
        
        if (isProhibitedCheckBox.isSelected()) {
            keywords.add("PROHIBITED ITEM");
        }
        
        if (requiresDisposalCheckBox.isSelected()) {
            keywords.add("Requires Disposal");
        }
        
        // Officer info
        keywords.add("Officer: " + checkpointOfficerNameField.getText().trim());
        keywords.add("Badge: " + checkpointOfficerIdField.getText().trim());
        
        // Passenger info
        if (!passengerNameField.getText().trim().isEmpty()) {
            keywords.add("Passenger: " + passengerNameField.getText().trim());
        }
        if (!flightNumberField.getText().trim().isEmpty()) {
            keywords.add("Flight: " + flightNumberField.getText().trim());
        }
        
        // Chain of custody
        if (!chainOfCustodyArea.getText().trim().isEmpty()) {
            keywords.add("Custody: " + chainOfCustodyArea.getText().trim().substring(0, 
                Math.min(100, chainOfCustodyArea.getText().trim().length())));
        }
        
        item.setKeywords(keywords);
        
        // Add images
        for (String path : checkpointImagePaths) {
            item.addImagePath(path);
        }
        
        return item;
    }
    
    private boolean validateCheckpointForm() {
        if (checkpointTitleField.getText().trim().isEmpty()) {
            showError("Please enter an item title.");
            checkpointTitleField.requestFocus();
            return false;
        }
        if (checkpointDescriptionArea.getText().trim().isEmpty()) {
            showError("Please enter a description.");
            checkpointDescriptionArea.requestFocus();
            return false;
        }
        if (checkpointOfficerIdField.getText().trim().isEmpty()) {
            showError("Please enter officer badge ID.");
            checkpointOfficerIdField.requestFocus();
            return false;
        }
        if (checkpointOfficerNameField.getText().trim().isEmpty()) {
            showError("Please enter officer name.");
            checkpointOfficerNameField.requestFocus();
            return false;
        }
        return true;
    }
    
    private void clearCheckpointForm() {
        checkpointTitleField.setText("");
        checkpointDescriptionArea.setText("");
        checkpointCategoryCombo.setSelectedIndex(0);
        checkpointTerminalCombo.setSelectedIndex(0);
        checkpointLaneField.setText("");
        checkpointTypeCombo.setSelectedIndex(0);
        prohibitedTypeCombo.setSelectedIndex(0);
        isProhibitedCheckBox.setSelected(false);
        requiresDisposalCheckBox.setSelected(false);
        checkpointOfficerIdField.setText("");
        checkpointOfficerNameField.setText(currentUser.getFullName());
        passengerNameField.setText("");
        flightNumberField.setText("");
        chainOfCustodyArea.setText("Item collected at checkpoint by [Officer Name] on [Date/Time].\nWitness: [If applicable]");
        clearCheckpointImage();
    }
    
    private void selectCheckpointImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String destPath = "items/images/tsa_" + System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(System.getProperty("user.dir"), destPath);
                destFile.getParentFile().mkdirs();
                java.nio.file.Files.copy(file.toPath(), destFile.toPath());
                
                checkpointImagePaths.clear();
                checkpointImagePaths.add(destPath);
                
                ImageIcon icon = new ImageIcon(destFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                checkpointImagePreviewLabel.setIcon(new ImageIcon(img));
                checkpointImagePreviewLabel.setText("");
            } catch (Exception e) {
                showError("Failed to upload image: " + e.getMessage());
            }
        }
    }
    
    private void clearCheckpointImage() {
        checkpointImagePaths.clear();
        checkpointImagePreviewLabel.setIcon(null);
        checkpointImagePreviewLabel.setText("No image");
    }
    
    private void performPoliceDbCheck() {
        String serialNum = serialNumberField.getText().trim();
        if (serialNum.isEmpty()) {
            showError("Please enter a serial number to check.");
            return;
        }
        
        // Simulate police database check
        JDialog progressDialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Checking Database...",
            true
        );
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("🔍 Checking police stolen property database..."), BorderLayout.CENTER);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.add(progress, BorderLayout.SOUTH);
        progressDialog.add(panel);
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Thread.sleep(2000); // Simulate check
                return Math.random() > 0.9; // 10% chance of match
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    boolean isStolen = get();
                    if (isStolen) {
                        JOptionPane.showMessageDialog(TSASecurityCoordinatorPanel.this,
                            "⚠️ ALERT: Item may match stolen property report!\n\n" +
                            "Serial Number: " + serialNum + "\n\n" +
                            "Please contact Massport Police immediately.\n" +
                            "Do NOT release item to anyone.",
                            "STOLEN PROPERTY ALERT",
                            JOptionPane.WARNING_MESSAGE);
                        
                        verificationStatusCombo.setSelectedItem("Verified - Stolen");
                        addAuditEntry("Police DB Match", "Serial: " + serialNum + " - POTENTIAL STOLEN PROPERTY");
                    } else {
                        JOptionPane.showMessageDialog(TSASecurityCoordinatorPanel.this,
                            "✅ No matches found in stolen property database.\n\n" +
                            "Serial Number: " + serialNum + "\n\n" +
                            "Item appears clear for normal processing.",
                            "Database Check Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        verificationStatusCombo.setSelectedItem("Verified - Clear");
                        addAuditEntry("Police DB Check", "Serial: " + serialNum + " - Clear");
                    }
                } catch (Exception e) {
                    showError("Database check failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void saveVerification() {
        int selectedRow = highValueItemsTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an item to verify.");
            return;
        }
        
        String itemTitle = (String) highValueItemsModel.getValueAt(selectedRow, 0);
        String status = (String) verificationStatusCombo.getSelectedItem();
        
        addAuditEntry("Verification Updated", 
            "Item: " + itemTitle + " - Status: " + status);
        
        JOptionPane.showMessageDialog(this,
            "Verification saved for: " + itemTitle + "\n" +
            "Status: " + status,
            "Verification Saved",
            JOptionPane.INFORMATION_MESSAGE);
        
        loadHighValueItems();
    }
    
    private void assignSecureStorage() {
        int selectedRow = highValueItemsTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an item.");
            return;
        }
        
        String itemTitle = (String) highValueItemsModel.getValueAt(selectedRow, 0);
        String location = JOptionPane.showInputDialog(this,
            "Enter secure storage location for:\n" + itemTitle,
            "Assign Secure Storage",
            JOptionPane.QUESTION_MESSAGE);
        
        if (location != null && !location.trim().isEmpty()) {
            addAuditEntry("Secure Storage Assigned", 
                "Item: " + itemTitle + " - Location: " + location);
            
            JOptionPane.showMessageDialog(this,
                "Item assigned to secure storage:\n" +
                "Location: " + location,
                "Storage Assigned",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void authorizeRelease() {
        int selectedRow = highValueItemsTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an item.");
            return;
        }
        
        String itemTitle = (String) highValueItemsModel.getValueAt(selectedRow, 0);
        String status = (String) verificationStatusCombo.getSelectedItem();
        
        if (status.contains("Stolen")) {
            showError("Cannot authorize release of potentially stolen property.\n" +
                     "Contact Massport Police for further instructions.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Authorize release of:\n" + itemTitle + "?\n\n" +
            "Verification Status: " + status + "\n\n" +
            "This action will mark the item for release.",
            "Confirm Release Authorization",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            addAuditEntry("Release Authorized", 
                "Item: " + itemTitle + " - Authorized by: " + currentUser.getFullName());
            
            JOptionPane.showMessageDialog(this,
                "Release authorized for: " + itemTitle + "\n\n" +
                "Please ensure proper ID verification before release.",
                "Release Authorized",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadHighValueItems();
        }
    }
    
    private void exportComplianceReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("tsa_compliance_report_" + 
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            // Export compliance data
            try {
                List<Item> items = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getKeywords() != null && 
                                i.getKeywords().stream().anyMatch(k -> k.toLowerCase().contains("tsa")))
                    .collect(Collectors.toList());
                
                boolean success = reportExportService.exportItemsToCSV(items, file.getAbsolutePath());
                
                if (success) {
                    JOptionPane.showMessageDialog(this,
                        "Compliance report exported successfully!\n" + file.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    addAuditEntry("Report Exported", "Compliance report: " + file.getName());
                } else {
                    showError("Failed to export report.");
                }
            } catch (Exception e) {
                showError("Export error: " + e.getMessage());
            }
        }
    }
    
    private void saveDisposalRecord() {
        String record = disposalDocArea.getText().trim();
        if (record.isEmpty() || record.contains("_____________")) {
            showError("Please complete all disposal documentation fields.");
            return;
        }
        
        addAuditEntry("Disposal Documented", "Disposal record created");
        
        JOptionPane.showMessageDialog(this,
            "Disposal record saved successfully.\n\n" +
            "Please retain a printed copy for physical records.",
            "Record Saved",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void performOwnerSearch() {
        String name = ownerSearchNameField.getText().trim();
        String flight = ownerSearchFlightField.getText().trim();
        String id = ownerSearchIdField.getText().trim();
        
        if (name.isEmpty() && flight.isEmpty() && id.isEmpty()) {
            showError("Please enter at least one search criterion.");
            return;
        }
        
        // Simulate passenger search
        passengerResultsPanel.removeAll();
        
        // Add simulated results
        passengerResultsPanel.add(createPassengerResultCard(
            name.isEmpty() ? "John Smith" : name,
            flight.isEmpty() ? "DL1234" : flight,
            "Gate B12",
            "2 items at checkpoint"
        ));
        
        passengerResultsPanel.revalidate();
        passengerResultsPanel.repaint();
        
        addAuditEntry("Passenger Search", "Name: " + name + ", Flight: " + flight);
    }
    
    private JPanel createPassengerResultCard(String name, String flight, String gate, String items) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(EMOJI_FONT_LARGE);
        card.add(iconLabel, BorderLayout.WEST);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        centerPanel.add(nameLabel);
        
        JLabel detailsLabel = new JLabel("Flight: " + flight + " • " + gate + " • " + items);
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailsLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(detailsLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void showIdVerificationDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "🆔 ID Verification Process",
            true
        );
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("ID Verification Checklist");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        
        String[] checks = {
            "Government-issued photo ID presented",
            "Photo matches passenger",
            "Name matches checkpoint records",
            "ID not expired",
            "Boarding pass or flight confirmation verified"
        };
        
        JCheckBox[] checkBoxes = new JCheckBox[checks.length];
        for (int i = 0; i < checks.length; i++) {
            checkBoxes[i] = new JCheckBox(checks[i]);
            checkBoxes[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(checkBoxes[i]);
            panel.add(Box.createVerticalStrut(5));
        }
        
        panel.add(Box.createVerticalStrut(20));
        
        JButton verifyBtn = new JButton("✅ Complete Verification");
        verifyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        verifyBtn.addActionListener(e -> {
            boolean allChecked = true;
            for (JCheckBox cb : checkBoxes) {
                if (!cb.isSelected()) {
                    allChecked = false;
                    break;
                }
            }
            
            if (allChecked) {
                addAuditEntry("ID Verified", "Passenger ID verification completed");
                JOptionPane.showMessageDialog(dialog, "ID verification complete!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else {
                showError("Please complete all verification steps.");
            }
        });
        panel.add(verifyBtn);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showFlightManifest() {
        JOptionPane.showMessageDialog(this,
            "📋 Flight Manifest Access\n\n" +
            "This feature requires integration with airline systems.\n\n" +
            "For immediate assistance, contact:\n" +
            "• Airline Operations: Check airline desk\n" +
            "• Massport Operations: 617-568-5050\n\n" +
            "Note: Flight manifests contain sensitive PII and\n" +
            "access must comply with TSA privacy regulations.",
            "Flight Manifest",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void logContactAttempt() {
        String[] methods = {"Phone Call", "Email", "In-Person", "PA Announcement", "Other"};
        String method = (String) JOptionPane.showInputDialog(this,
            "Select contact method:",
            "Log Contact Attempt",
            JOptionPane.QUESTION_MESSAGE,
            null, methods, methods[0]);
        
        if (method != null) {
            String[] results = {"No Answer", "Left Message", "Spoke Directly", "Wrong Number", "Email Sent"};
            String result = (String) JOptionPane.showInputDialog(this,
                "Result of contact attempt:",
                "Contact Result",
                JOptionPane.QUESTION_MESSAGE,
                null, results, results[0]);
            
            if (result != null) {
                contactAttemptsModel.addRow(new Object[]{
                    DATETIME_FORMAT.format(new Date()),
                    method,
                    ownerSearchNameField.getText().trim().isEmpty() ? "Unknown" : ownerSearchNameField.getText(),
                    result,
                    "",
                    currentUser.getFullName()
                });
                
                addAuditEntry("Contact Logged", "Method: " + method + ", Result: " + result);
            }
        }
    }
    
    private void escalateToPolice() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "🚨 Escalate to Massport Police Department?\n\n" +
            "This will create an urgent notification for police response.\n\n" +
            "Use only for:\n" +
            "• Suspected stolen property\n" +
            "• Security threats\n" +
            "• Evidence preservation\n\n" +
            "Continue with escalation?",
            "Confirm Police Escalation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            addAuditEntry("Police Escalation", "Urgent escalation to Massport PD");
            
            JOptionPane.showMessageDialog(this,
                "🚔 Escalation submitted to Massport Police.\n\n" +
                "Reference Number: MPD-" + (System.currentTimeMillis() % 100000) + "\n\n" +
                "An officer will respond shortly.\n" +
                "For immediate assistance: 617-568-5000",
                "Escalation Submitted",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void requestEvidencePickup() {
        String description = JOptionPane.showInputDialog(this,
            "Describe items for evidence pickup:",
            "Request Evidence Pickup",
            JOptionPane.QUESTION_MESSAGE);
        
        if (description != null && !description.trim().isEmpty()) {
            addAuditEntry("Evidence Pickup Requested", description);
            
            JOptionPane.showMessageDialog(this,
                "📋 Evidence pickup request submitted.\n\n" +
                "Request ID: EVD-" + (System.currentTimeMillis() % 10000) + "\n\n" +
                "Massport Police Evidence Unit will coordinate pickup.",
                "Request Submitted",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void createAirportTransfer() {
        JOptionPane.showMessageDialog(this,
            "✈️ Transfer to Airport Lost & Found\n\n" +
            "To create a transfer:\n" +
            "1. Go to Checkpoint Items tab\n" +
            "2. Register the item\n" +
            "3. Click 'Register & Transfer to L&F'\n\n" +
            "Or contact Airport L&F directly:\n" +
            "📞 617-561-1700",
            "Create Transfer",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void viewPendingTransfers() {
        JOptionPane.showMessageDialog(this,
            "📋 Pending Transfers to Airport L&F\n\n" +
            "No pending transfers at this time.\n\n" +
            "All checkpoint items requiring transfer\n" +
            "will appear in this list.",
            "Pending Transfers",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void contactAirportLF() {
        JOptionPane.showMessageDialog(this,
            "✈️ Logan Airport Lost & Found\n\n" +
            "Main Office: 617-561-1700\n" +
            "Terminal A: 617-561-1701\n" +
            "Terminal B: 617-561-1702\n" +
            "Terminal C: 617-561-1703\n" +
            "Terminal E: 617-561-1704\n\n" +
            "Hours: 24/7",
            "Airport Lost & Found Contact",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void createIncidentReport() {
        String[] types = {"Prohibited Item", "Security Concern", "Passenger Dispute", 
                         "Equipment Issue", "Procedural Violation", "Other"};
        String type = (String) JOptionPane.showInputDialog(this,
            "Select incident type:",
            "Create Incident Report",
            JOptionPane.QUESTION_MESSAGE,
            null, types, types[0]);
        
        if (type != null) {
            String reportId = "TSA-" + (System.currentTimeMillis() % 10000);
            
            incidentReportsModel.addRow(new Object[]{
                reportId,
                DATE_FORMAT.format(new Date()),
                type,
                "Draft"
            });
            
            addAuditEntry("Incident Report Created", "Type: " + type + ", ID: " + reportId);
            
            JOptionPane.showMessageDialog(this,
                "📝 Incident Report Created\n\n" +
                "Report ID: " + reportId + "\n" +
                "Type: " + type + "\n" +
                "Status: Draft\n\n" +
                "Please complete the full report within 24 hours.",
                "Report Created",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void viewSelectedIncident() {
        int selectedRow = incidentReportsTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an incident report to view.");
            return;
        }
        
        String reportId = (String) incidentReportsModel.getValueAt(selectedRow, 0);
        String type = (String) incidentReportsModel.getValueAt(selectedRow, 2);
        
        JOptionPane.showMessageDialog(this,
            "📋 Incident Report: " + reportId + "\n\n" +
            "Type: " + type + "\n" +
            "Date: " + incidentReportsModel.getValueAt(selectedRow, 1) + "\n" +
            "Status: " + incidentReportsModel.getValueAt(selectedRow, 3) + "\n\n" +
            "Full report details would be displayed here.",
            "Incident Report Details",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void addAuditEntry(String action, String details) {
        if (auditLogModel != null) {
            auditLogModel.insertRow(0, new Object[]{
                DATETIME_FORMAT.format(new Date()),
                action,
                currentUser.getFullName(),
                details
            });
        }
    }
    
    private void refreshAll() {
        loadDashboardData();
        loadHighValueItems();
        loadComplianceData();
        loadContactAttempts();
        loadCoordinationData();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openSecurityAlertDialog() {
        String[] alertTypes = {
            "Suspicious Item Detected",
            "Unattended Bag",
            "Security Breach",
            "Passenger Behavior Concern",
            "Equipment Malfunction",
            "Evacuation Required"
        };
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "🚨 Security Alert Type:",
            "Security Alert",
            JOptionPane.WARNING_MESSAGE,
            null, alertTypes, alertTypes[0]);
        
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Issue security alert: " + selected + "?\n\n" +
                "This will notify:\n" +
                "• TSA Supervisors\n" +
                "• Massport Police\n" +
                "• Terminal Operations\n\n" +
                "Proceed?",
                "Confirm Alert",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                addAuditEntry("Security Alert Issued", selected);
                
                JOptionPane.showMessageDialog(this,
                    "🚨 SECURITY ALERT ISSUED\n\n" +
                    "Type: " + selected + "\n" +
                    "Alert ID: SEC-" + (System.currentTimeMillis() % 10000) + "\n\n" +
                    "Notifications sent to all relevant parties.\n" +
                    "Stand by for response.",
                    "Alert Issued",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    private void showFederalContacts() {
        JOptionPane.showMessageDialog(this,
            "🏛️ Federal Agency Contacts\n\n" +
            "TSA Federal Security Director:\n" +
            "  617-568-4500\n\n" +
            "TSA Operations Center:\n" +
            "  617-568-4510\n\n" +
            "FBI Boston Field Office:\n" +
            "  617-742-5533\n\n" +
            "CBP (Customs & Border Protection):\n" +
            "  617-568-1800\n\n" +
            "DHS Tip Line:\n" +
            "  1-866-347-2423",
            "Federal Agency Contacts",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void contactPoliceDialog() {
        JOptionPane.showMessageDialog(this,
            "🚔 Massport Police Department\n\n" +
            "Emergency: 617-568-5000\n" +
            "Non-Emergency: 617-568-5010\n" +
            "Evidence Unit: 617-568-5020\n" +
            "Investigations: 617-568-5030\n\n" +
            "Station Location: Terminal C, Ground Level\n\n" +
            "For life-threatening emergencies: 911",
            "Police Contact",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class CategoryComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(EMOJI_FONT);
            
            if (value instanceof Item.ItemCategory category) {
                setText(category.getEmoji() + " " + category.getDisplayName());
            }
            
            return this;
        }
    }
}
