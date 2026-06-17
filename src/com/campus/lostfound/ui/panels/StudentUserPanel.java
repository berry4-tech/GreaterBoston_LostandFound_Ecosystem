package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.components.*;
import com.campus.lostfound.ui.dialogs.RequestDetailDialog;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main panel for Student users in the Lost & Found system.
 * 
 * Features:
 * - Report lost items with detailed information
 * - View "My Lost Items" with status tracking
 * - View match notifications for lost items
 * - Submit claims for found items with verification
 * - Track claim request status
 * 
 * @author Developer 2 - UI Panels
 */
public class StudentUserPanel extends JPanel {
    
    // Data
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private WorkRequestService workRequestService;
    private ItemMatcher itemMatcher;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JLabel welcomeLabel;
    private JLabel statsLabel;
    
    // Tab panels
    private JPanel reportLostTab;
    private JPanel myItemsTab;
    private JPanel matchesTab;
    private JPanel claimsTab;
    private JPanel searchTab;
    
    // Report Lost Item components
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<Item.ItemCategory> categoryCombo;
    private JComboBox<Building> buildingCombo;
    private JTextField roomField;
    private JTextField colorField;
    private JTextField brandField;
    private JLabel imagePreviewLabel;
    private List<String> imagePaths;
    private JButton submitButton;
    
    // My Items components
    private JPanel myItemsListPanel;
    private JScrollPane myItemsScrollPane;
    private JComboBox<String> myItemsFilterCombo;
    
    // Matches components
    private JPanel matchesContainerPanel;
    private JComboBox<ItemOption> lostItemSelector;
    private ItemMatchResultsPanel matchResultsPanel;
    
    // Claims components
    private WorkQueueTablePanel claimsQueuePanel;
    
    // Pending Pickups components
    private JPanel pendingPickupsPanel;
    private JPanel pickupsListPanel;
    
    // Search components
    private EnterpriseItemSearchPanel searchPanel;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Color PRIMARY_COLOR = new Color(13, 110, 253);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 32);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);
    
    /**
     * Create a new StudentUserPanel.
     * 
     * @param currentUser The logged-in student user
     */
    public StudentUserPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.itemMatcher = new ItemMatcher();
        this.imagePaths = new ArrayList<>();
        
        initComponents();
        loadInitialData();
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
        reportLostTab = createReportLostTab();
        myItemsTab = createMyItemsTab();
        matchesTab = createMatchesTab();
        claimsTab = createClaimsTab();
        searchTab = createSearchTab();
        
        tabbedPane.addTab("📝 Report Lost Item", reportLostTab);
        tabbedPane.addTab("📋 My Items", myItemsTab);
        tabbedPane.addTab("🔔 Matches", matchesTab);
        tabbedPane.addTab("📦 My Claims", claimsTab);
        tabbedPane.addTab("🎁 Pending Pickups", createPendingPickupsTab());
        tabbedPane.addTab("🔍 Search All", searchTab);
        
        // Tab change listener to refresh data
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 1 -> refreshMyItems();
                case 2 -> refreshMatches();
                case 3 -> refreshClaims();
                case 4 -> refreshPendingPickups();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        // Left side - Welcome message
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        welcomeLabel = new JLabel("👋 Welcome, " + currentUser.getFullName());
        welcomeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 22));
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(welcomeLabel);
        
        JLabel roleLabel = new JLabel("Student Portal • Lost & Found Recovery System");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(new Color(108, 117, 125));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(roleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Trust Score and Quick stats
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);
        
        // Trust Score Panel
        JPanel trustPanel = createTrustScorePanel();
        rightPanel.add(trustPanel);
        
        // Separator
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 40));
        rightPanel.add(sep);
        
        statsLabel = new JLabel("Loading stats...");
        statsLabel.setFont(EMOJI_FONT);
        statsLabel.setForeground(new Color(108, 117, 125));
        rightPanel.add(statsLabel);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Creates a Trust Score display panel with visual indicator
     */
    private JPanel createTrustScorePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        // Trust score value
        double trustScore = currentUser.getTrustScore();
        String trustLevel = currentUser.getTrustLevel();
        Color trustColor = currentUser.getTrustLevelColor();
        
        // Trust label
        JLabel trustLabel = new JLabel("🛡️ Trust Score");
        trustLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
        trustLabel.setForeground(new Color(108, 117, 125));
        trustLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(trustLabel);
        
        // Score value with color
        JLabel scoreLabel = new JLabel(String.format("%.0f%%", trustScore));
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        scoreLabel.setForeground(trustColor);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(scoreLabel);
        
        // Trust level badge
        JLabel levelLabel = new JLabel(trustLevel);
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setOpaque(true);
        levelLabel.setBackground(trustColor);
        levelLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(levelLabel);
        
        // Tooltip with restrictions
        panel.setToolTipText("<html><b>Trust Level: " + trustLevel + "</b><br/>" + 
            currentUser.getRestrictionMessage().replace("\n", "<br/>") + "</html>");
        
        return panel;
    }
    
    // ==================== TAB 1: REPORT LOST ITEM ====================
    
    private JPanel createReportLostTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("Report a Lost Item");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Provide details about your lost item to help us find it");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Item Title
        formContainer.add(createFormLabel("Item Title *"));
        titleField = new JTextField();
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formContainer.add(titleField);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Category
        formContainer.add(createFormLabel("Category *"));
        categoryCombo = new JComboBox<>(Item.ItemCategory.values());
        categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        categoryCombo.setRenderer(new CategoryComboRenderer());
        formContainer.add(categoryCombo);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Description
        formContainer.add(createFormLabel("Description *"));
        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Location row
        JPanel locationRow = new JPanel(new GridLayout(1, 2, 15, 0));
        locationRow.setOpaque(false);
        locationRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        locationRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Building
        JPanel buildingPanel = new JPanel();
        buildingPanel.setLayout(new BoxLayout(buildingPanel, BoxLayout.Y_AXIS));
        buildingPanel.setOpaque(false);
        buildingPanel.add(createFormLabel("Building/Location *"));
        buildingCombo = new JComboBox<>();
        buildingCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        loadBuildings();
        buildingPanel.add(buildingCombo);
        locationRow.add(buildingPanel);
        
        // Room
        JPanel roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));
        roomPanel.setOpaque(false);
        roomPanel.add(createFormLabel("Room/Area"));
        roomField = new JTextField();
        roomField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        roomPanel.add(roomField);
        locationRow.add(roomPanel);
        
        formContainer.add(locationRow);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Details row
        JPanel detailsRow = new JPanel(new GridLayout(1, 2, 15, 0));
        detailsRow.setOpaque(false);
        detailsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        detailsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Color
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
        colorPanel.setOpaque(false);
        colorPanel.add(createFormLabel("Primary Color"));
        colorField = new JTextField();
        colorField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        colorPanel.add(colorField);
        detailsRow.add(colorPanel);
        
        // Brand
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.add(createFormLabel("Brand"));
        brandField = new JTextField();
        brandField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        brandPanel.add(brandField);
        detailsRow.add(brandPanel);
        
        formContainer.add(detailsRow);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Image upload
        formContainer.add(createFormLabel("Photo (Optional)"));
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imagePanel.setOpaque(false);
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        imagePreviewLabel = new JLabel("No image selected");
        imagePreviewLabel.setPreferredSize(new Dimension(100, 100));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setBackground(new Color(248, 249, 250));
        imagePreviewLabel.setOpaque(true);
        imagePanel.add(imagePreviewLabel);
        
        imagePanel.add(Box.createHorizontalStrut(15));
        
        JButton uploadButton = new JButton("📷 Upload Photo");
        uploadButton.setFont(EMOJI_FONT);
        uploadButton.addActionListener(e -> selectImage());
        imagePanel.add(uploadButton);
        
        JButton clearImageButton = new JButton("Clear");
        clearImageButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearImageButton.addActionListener(e -> clearImage());
        imagePanel.add(Box.createHorizontalStrut(10));
        imagePanel.add(clearImageButton);
        
        formContainer.add(imagePanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        submitButton = new JButton("📤 Submit Report");
        submitButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        submitButton.setBackground(PRIMARY_COLOR);
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setPreferredSize(new Dimension(180, 45));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> submitLostItemReport());
        buttonPanel.add(submitButton);
        
        buttonPanel.add(Box.createHorizontalStrut(15));
        
        JButton clearButton = new JButton("Clear Form");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clearButton.setPreferredSize(new Dimension(120, 45));
        clearButton.addActionListener(e -> clearReportForm());
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
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(73, 80, 87));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return label;
    }
    
    // ==================== TAB 2: MY ITEMS ====================
    
    private JPanel createMyItemsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with filter
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("📋 My Reported Items");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterPanel.add(filterLabel);
        
        myItemsFilterCombo = new JComboBox<>(new String[]{
            "All Items", "Lost Items", "Found Items", "Open", "Pending Claim", "Resolved", "Cancelled"
        });
        myItemsFilterCombo.setPreferredSize(new Dimension(150, 28));
        myItemsFilterCombo.addActionListener(e -> refreshMyItems());
        filterPanel.add(myItemsFilterCombo);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(EMOJI_FONT);
        refreshBtn.addActionListener(e -> refreshMyItems());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Items list
        myItemsListPanel = new JPanel();
        myItemsListPanel.setLayout(new BoxLayout(myItemsListPanel, BoxLayout.Y_AXIS));
        myItemsListPanel.setBackground(Color.WHITE);
        myItemsListPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        myItemsScrollPane = new JScrollPane(myItemsListPanel);
        myItemsScrollPane.setBorder(null);
        myItemsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(myItemsScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 3: MATCHES ====================
    
    private JPanel createMatchesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with item selector
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🔔 Match Notifications");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        leftHeader.add(titleLabel);
        
        headerPanel.add(leftHeader, BorderLayout.WEST);
        
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        selectorPanel.setOpaque(false);
        
        JLabel selectLabel = new JLabel("Select your lost item:");
        selectLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectorPanel.add(selectLabel);
        
        lostItemSelector = new JComboBox<>();
        lostItemSelector.setPreferredSize(new Dimension(300, 30));
        lostItemSelector.addActionListener(e -> onLostItemSelected());
        selectorPanel.add(lostItemSelector);
        
        headerPanel.add(selectorPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Match results panel
        matchResultsPanel = new ItemMatchResultsPanel(currentUser);
        matchResultsPanel.setOnClaimRequested(this::showClaimDialog);
        
        panel.add(matchResultsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 4: MY CLAIMS ====================
    
    private JPanel createClaimsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        
        JLabel titleLabel = new JLabel("📦 My Claim Requests");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Track the status of your item claim requests");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(helpLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Work queue panel
        claimsQueuePanel = new WorkQueueTablePanel(currentUser);
        claimsQueuePanel.setOnRequestDoubleClicked(this::showClaimDetails);
        
        panel.add(claimsQueuePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 5: PENDING PICKUPS ====================
    
    private JPanel createPendingPickupsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("🎁 Items Ready for Pickup");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Items transferred from MBTA/Airport awaiting your pickup confirmation");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(helpLabel, BorderLayout.SOUTH);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(EMOJI_FONT);
        refreshBtn.addActionListener(e -> refreshPendingPickups());
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Pickups list
        pickupsListPanel = new JPanel();
        pickupsListPanel.setLayout(new BoxLayout(pickupsListPanel, BoxLayout.Y_AXIS));
        pickupsListPanel.setBackground(Color.WHITE);
        pickupsListPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(pickupsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshPendingPickups() {
        SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WorkRequest> doInBackground() {
                // Get all transfer requests that need STUDENT approval
                List<WorkRequest> allRequests = workRequestService.getRequestsForRole(
                    "STUDENT", currentUser.getOrganizationId());
                
                // Filter to transfers where this student is the recipient
                return allRequests.stream()
                    .filter(r -> r.getRequestType() == WorkRequest.RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER ||
                                r.getRequestType() == WorkRequest.RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER ||
                                r.getRequestType() == WorkRequest.RequestType.CROSS_CAMPUS_TRANSFER)
                    .filter(r -> r.isPending())
                    .filter(r -> isTransferForThisStudent(r))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<WorkRequest> pickups = get();
                    displayPendingPickups(pickups);
                } catch (Exception e) {
                    e.printStackTrace();
                    pickupsListPanel.removeAll();
                    JLabel errorLabel = new JLabel("Error loading pickups: " + e.getMessage());
                    errorLabel.setForeground(DANGER_COLOR);
                    pickupsListPanel.add(errorLabel);
                    pickupsListPanel.revalidate();
                    pickupsListPanel.repaint();
                }
            }
        };
        worker.execute();
    }
    
    private boolean isTransferForThisStudent(WorkRequest request) {
        // Check if this transfer is meant for the current student
        if (request instanceof TransitToUniversityTransferRequest transfer) {
            String studentEmail = transfer.getStudentEmail();
            String studentName = transfer.getStudentName();
            
            // Match by email or name
            if (studentEmail != null && studentEmail.equalsIgnoreCase(currentUser.getEmail())) {
                return true;
            }
            if (studentName != null && studentName.equalsIgnoreCase(currentUser.getFullName())) {
                return true;
            }
        } else if (request instanceof AirportToUniversityTransferRequest transfer) {
            String studentEmail = transfer.getStudentEmail();
            String studentName = transfer.getStudentName();
            
            if (studentEmail != null && studentEmail.equalsIgnoreCase(currentUser.getEmail())) {
                return true;
            }
            if (studentName != null && studentName.equalsIgnoreCase(currentUser.getFullName())) {
                return true;
            }
        } else if (request instanceof CrossCampusTransferRequest transfer) {
            String studentEmail = transfer.getStudentEmail();
            String studentName = transfer.getStudentName();
            
            // Match by email or name
            if (studentEmail != null && studentEmail.equalsIgnoreCase(currentUser.getEmail())) {
                return true;
            }
            if (studentName != null && studentName.equalsIgnoreCase(currentUser.getFullName())) {
                return true;
            }
        }
        return false;
    }
    
    private void displayPendingPickups(List<WorkRequest> pickups) {
        pickupsListPanel.removeAll();
        
        if (pickups.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
            emptyPanel.setOpaque(false);
            emptyPanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            
            JLabel emptyIcon = new JLabel("📭");
            emptyIcon.setFont(UIConstants.getEmojiFont(Font.PLAIN, 48));
            emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(emptyIcon);
            
            emptyPanel.add(Box.createVerticalStrut(15));
            
            JLabel emptyLabel = new JLabel("No items pending pickup");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(emptyLabel);
            
            JLabel helpLabel = new JLabel("Items transferred from MBTA or Airport will appear here");
            helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            helpLabel.setForeground(new Color(160, 160, 160));
            helpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(helpLabel);
            
            pickupsListPanel.add(emptyPanel);
        } else {
            for (WorkRequest request : pickups) {
                pickupsListPanel.add(createPickupCard(request));
                pickupsListPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        pickupsListPanel.revalidate();
        pickupsListPanel.repaint();
    }
    
    private JPanel createPickupCard(WorkRequest request) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(232, 245, 233)); // Light green background
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SUCCESS_COLOR, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Left - Icon
        JLabel iconLabel = new JLabel();
        iconLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 40));
        iconLabel.setPreferredSize(new Dimension(60, 0));
        
        String itemName = "Unknown Item";
        String pickupLocation = "Campus Lost & Found Office";
        String sourceLocation = "Unknown";
        
        if (request instanceof TransitToUniversityTransferRequest transfer) {
            iconLabel.setText("🚇");
            itemName = transfer.getItemName() != null ? transfer.getItemName() : "Transit Item";
            pickupLocation = transfer.getCampusPickupLocation() != null ? 
                transfer.getCampusPickupLocation() : "Campus Lost & Found Office";
            sourceLocation = "MBTA " + (transfer.getStationName() != null ? transfer.getStationName() : "Station");
        } else if (request instanceof AirportToUniversityTransferRequest transfer) {
            iconLabel.setText("✈️");
            itemName = transfer.getItemName() != null ? transfer.getItemName() : "Airport Item";
            pickupLocation = transfer.getCampusPickupLocation() != null ? 
                transfer.getCampusPickupLocation() : "Campus Lost & Found Office";
            sourceLocation = "Logan Airport " + (transfer.getTerminalNumber() != null ? 
                "Terminal " + transfer.getTerminalNumber() : "");
        } else if (request instanceof CrossCampusTransferRequest transfer) {
            iconLabel.setText("🎓");
            itemName = transfer.getItemName() != null ? transfer.getItemName() : "Campus Item";
            pickupLocation = transfer.getPickupLocation() != null ? 
                transfer.getPickupLocation() : "Campus Lost & Found Office";
            sourceLocation = transfer.getSourceCampusName() != null ? 
                transfer.getSourceCampusName() : "Partner Campus";
        }
        
        card.add(iconLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🎉 Your item is ready for pickup!");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 16));
        titleLabel.setForeground(new Color(27, 94, 32));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(titleLabel);
        
        centerPanel.add(Box.createVerticalStrut(8));
        
        JLabel itemLabel = new JLabel("Item: " + itemName);
        itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(itemLabel);
        
        JLabel sourceLabel = new JLabel("From: " + sourceLocation);
        sourceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sourceLabel.setForeground(new Color(66, 66, 66));
        sourceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(sourceLabel);
        
        JLabel pickupLabel = new JLabel("📍 Pickup at: " + pickupLocation);
        pickupLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        pickupLabel.setForeground(PRIMARY_COLOR);
        pickupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(pickupLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Action button
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(150, 0));
        
        JButton confirmBtn = new JButton("✅ Confirm Pickup");
        confirmBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        confirmBtn.setBackground(SUCCESS_COLOR);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> confirmPickup(request));
        rightPanel.add(confirmBtn);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        JButton detailsBtn = new JButton("View Details");
        detailsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsBtn.addActionListener(e -> showTransferDetails(request));
        rightPanel.add(detailsBtn);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void confirmPickup(WorkRequest request) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Have you picked up your item?\n\n" +
            "By confirming, you acknowledge that you have received the item.",
            "Confirm Pickup",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return workRequestService.approveRequest(
                    request.getRequestId(),
                    currentUser.getEmail()
                );
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(StudentUserPanel.this,
                            "🎉 Pickup confirmed!\n\n" +
                            "Thank you for using the Lost & Found Recovery System.\n" +
                            "Your item has been marked as CLAIMED.",
                            "Pickup Confirmed",
                            JOptionPane.INFORMATION_MESSAGE);
                        // Refresh all relevant tabs to show updated item status
                        refreshPendingPickups();
                        refreshMyItems();      // Refresh My Items tab to show CLAIMED status
                        loadInitialData();     // Refresh stats in header
                        refreshMatches();      // Refresh matches in case item was matched
                    } else {
                        showError("Failed to confirm pickup. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void showTransferDetails(WorkRequest request) {
        RequestDetailDialog dialog = new RequestDetailDialog(
            SwingUtilities.getWindowAncestor(this),
            request,
            currentUser
        );
        dialog.setOnRequestUpdated(r -> refreshPendingPickups());
        dialog.setVisible(true);
    }
    
    // ==================== TAB 6: SEARCH ====================
    
    private JPanel createSearchTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Use the EnterpriseItemSearchPanel
        searchPanel = new EnterpriseItemSearchPanel(currentUser);
        searchPanel.setOnItemDoubleClicked(this::showItemDetails);
        
        panel.add(searchPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadInitialData() {
        // Load stats in background
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                List<Item> userItems = itemDAO.findByUser(currentUser.getEmail());
                int lost = (int) userItems.stream().filter(i -> i.getType() == Item.ItemType.LOST).count();
                int found = (int) userItems.stream().filter(i -> i.getType() == Item.ItemType.FOUND).count();
                int open = (int) userItems.stream().filter(i -> i.getStatus() == Item.ItemStatus.OPEN).count();
                return new int[]{lost, found, open};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    statsLabel.setText(String.format("📊 %d Lost • %d Found • %d Open", 
                        stats[0], stats[1], stats[2]));
                } catch (Exception e) {
                    statsLabel.setText("Unable to load stats");
                }
            }
        };
        worker.execute();
    }
    
    private void loadBuildings() {
        SwingWorker<List<Building>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Building> doInBackground() {
                return buildingDAO.findAll();
            }
            
            @Override
            protected void done() {
                try {
                    List<Building> buildings = get();
                    buildingCombo.removeAllItems();
                    for (Building b : buildings) {
                        buildingCombo.addItem(b);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void refreshMyItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findByUser(currentUser.getEmail());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    displayMyItems(filterMyItems(items));
                    loadInitialData(); // Refresh stats
                } catch (Exception e) {
                    showError("Failed to load items: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private List<Item> filterMyItems(List<Item> items) {
        String filter = (String) myItemsFilterCombo.getSelectedItem();
        if (filter == null || filter.equals("All Items")) return items;
        
        return items.stream().filter(item -> switch (filter) {
            case "Lost Items" -> item.getType() == Item.ItemType.LOST;
            case "Found Items" -> item.getType() == Item.ItemType.FOUND;
            case "Open" -> item.getStatus() == Item.ItemStatus.OPEN;
            case "Pending Claim" -> item.getStatus() == Item.ItemStatus.PENDING_CLAIM;
            case "Resolved" -> item.getStatus() == Item.ItemStatus.CLAIMED;
            case "Cancelled" -> item.getStatus() == Item.ItemStatus.CANCELLED;
            default -> true;
        }).collect(Collectors.toList());
    }
    
    private void displayMyItems(List<Item> items) {
        myItemsListPanel.removeAll();
        
        if (items.isEmpty()) {
            JLabel emptyLabel = new JLabel("No items found. Report a lost item to get started!");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            myItemsListPanel.add(Box.createVerticalGlue());
            myItemsListPanel.add(emptyLabel);
            myItemsListPanel.add(Box.createVerticalGlue());
        } else {
            for (Item item : items) {
                myItemsListPanel.add(createMyItemCard(item));
                myItemsListPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        myItemsListPanel.revalidate();
        myItemsListPanel.repaint();
    }
    
    private JPanel createMyItemCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Left - Type badge and category icon
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(80, 0));
        
        JLabel typeLabel = new JLabel(item.getType().getIcon() + " " + item.getType().getLabel());
        typeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 10));
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setOpaque(true);
        typeLabel.setBackground(item.getType() == Item.ItemType.LOST ? DANGER_COLOR : SUCCESS_COLOR);
        typeLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(typeLabel);
        
        leftPanel.add(Box.createVerticalStrut(10));
        
        JLabel catLabel = new JLabel(item.getCategory().getEmoji());
        catLabel.setFont(EMOJI_FONT_LARGE);
        catLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(catLabel);
        
        card.add(leftPanel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(titleLabel);
        
        centerPanel.add(Box.createVerticalStrut(5));
        
        JLabel categoryLabel = new JLabel(item.getCategory().getDisplayName() + " • " + 
            (item.getLocation() != null ? item.getLocation().getBuilding().getName() : "Unknown"));
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(108, 117, 125));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(categoryLabel);
        
        centerPanel.add(Box.createVerticalStrut(5));
        
        JLabel dateLabel = new JLabel("Reported: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Status and actions
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(130, 0));
        
        // Status badge
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(statusLabel);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Find Matches button (only for lost items that are open)
        if (item.getType() == Item.ItemType.LOST && item.getStatus() == Item.ItemStatus.OPEN) {
            JButton matchBtn = new JButton("Find Matches");
            matchBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            matchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            matchBtn.addActionListener(e -> {
                tabbedPane.setSelectedIndex(2); // Switch to Matches tab
                selectLostItemForMatching(item);
            });
            rightPanel.add(matchBtn);
            
            rightPanel.add(Box.createVerticalStrut(5));
            
            // Mark as Resolved button for LOST items that are still OPEN
            JButton resolveBtn = new JButton("✓ Resolved");
            resolveBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
            resolveBtn.setForeground(SUCCESS_COLOR);
            resolveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            resolveBtn.setToolTipText("Mark this lost item as found/resolved");
            resolveBtn.addActionListener(e -> showResolveItemDialog(item));
            rightPanel.add(resolveBtn);
        }
        
        rightPanel.add(Box.createVerticalStrut(5));
        
        // Delete button
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        deleteBtn.setForeground(DANGER_COLOR);
        deleteBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteBtn.addActionListener(e -> deleteItem(item));
        rightPanel.add(deleteBtn);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void refreshMatches() {
        // Populate lost item selector
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findByUser(currentUser.getEmail())
                    .stream()
                    .filter(i -> i.getType() == Item.ItemType.LOST)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> lostItems = get();
                    lostItemSelector.removeAllItems();
                    lostItemSelector.addItem(new ItemOption(null, "-- Select a lost item --"));
                    for (Item item : lostItems) {
                        lostItemSelector.addItem(new ItemOption(item, 
                            item.getCategory().getEmoji() + " " + item.getTitle()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void selectLostItemForMatching(Item item) {
        // Find and select the item in the combo box
        for (int i = 0; i < lostItemSelector.getItemCount(); i++) {
            ItemOption opt = lostItemSelector.getItemAt(i);
            if (opt.item != null && opt.item.getMongoId() != null && 
                opt.item.getMongoId().equals(item.getMongoId())) {
                lostItemSelector.setSelectedIndex(i);
                break;
            }
        }
    }
    
    private void onLostItemSelected() {
        ItemOption selected = (ItemOption) lostItemSelector.getSelectedItem();
        if (selected != null && selected.item != null) {
            matchResultsPanel.findMatchesFor(selected.item);
        } else {
            matchResultsPanel.clear();
        }
    }
    
    private void refreshClaims() {
        // Load user's claim requests using getRequestsForUser method
        SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WorkRequest> doInBackground() {
                return workRequestService.getRequestsForUser(
                    currentUser.getEmail(),
                    currentUser.getRole().name());
            }
            
            @Override
            protected void done() {
                try {
                    List<WorkRequest> requests = get();
                    // Filter to only show item claims submitted by this user
                    List<WorkRequest> claims = requests.stream()
                        .filter(r -> r.getRequestType() == WorkRequest.RequestType.ITEM_CLAIM)
                        .filter(r -> r.getRequesterId().equals(currentUser.getEmail()))
                        .collect(Collectors.toList());
                    claimsQueuePanel.loadRequests(claims);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    // ==================== ACTIONS ====================
    
    private void submitLostItemReport() {
        // Validate form
        if (titleField.getText().trim().isEmpty()) {
            showError("Please enter an item title.");
            titleField.requestFocus();
            return;
        }
        
        if (descriptionArea.getText().trim().isEmpty()) {
            showError("Please enter a description.");
            descriptionArea.requestFocus();
            return;
        }
        
        if (buildingCombo.getSelectedItem() == null) {
            showError("Please select a building/location.");
            return;
        }
        
        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    // Get selected values
                    Building building = (Building) buildingCombo.getSelectedItem();
                    Item.ItemCategory category = (Item.ItemCategory) categoryCombo.getSelectedItem();
                    
                    // Create location
                    Location location = new Location(building, roomField.getText().trim(), "");
                    
                    // Create item using proper constructor
                    Item item = new Item(
                        titleField.getText().trim(),
                        descriptionArea.getText().trim(),
                        category,
                        Item.ItemType.LOST,
                        location,
                        currentUser
                    );
                    
                    item.setStatus(Item.ItemStatus.OPEN);
                    
                    // Optional fields
                    if (!colorField.getText().trim().isEmpty()) {
                        item.setPrimaryColor(colorField.getText().trim());
                    }
                    if (!brandField.getText().trim().isEmpty()) {
                        item.setBrand(brandField.getText().trim());
                    }
                    
                    // Images
                    for (String path : imagePaths) {
                        item.addImagePath(path);
                    }
                    
                    // Set enterprise/org from user
                    item.setEnterpriseId(currentUser.getEnterpriseId());
                    item.setOrganizationId(currentUser.getOrganizationId());
                    
                    return itemDAO.create(item);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                submitButton.setEnabled(true);
                submitButton.setText("📤 Submit Report");
                
                try {
                    String itemId = get();
                    if (itemId != null) {
                        JOptionPane.showMessageDialog(StudentUserPanel.this,
                            "Your lost item report has been submitted successfully!\n" +
                            "We'll notify you if potential matches are found.",
                            "Report Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearReportForm();
                        loadInitialData(); // Refresh stats
                    } else {
                        showError("Failed to submit report. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void clearReportForm() {
        titleField.setText("");
        descriptionArea.setText("");
        categoryCombo.setSelectedIndex(0);
        if (buildingCombo.getItemCount() > 0) {
            buildingCombo.setSelectedIndex(0);
        }
        roomField.setText("");
        colorField.setText("");
        brandField.setText("");
        clearImage();
    }
    
    private void selectImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                // Copy to items folder
                String destPath = "items/images/" + System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(System.getProperty("user.dir"), destPath);
                destFile.getParentFile().mkdirs();
                java.nio.file.Files.copy(file.toPath(), destFile.toPath());
                
                imagePaths.clear();
                imagePaths.add(destPath);
                
                // Show preview
                ImageIcon icon = new ImageIcon(destFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                imagePreviewLabel.setIcon(new ImageIcon(img));
                imagePreviewLabel.setText("");
            } catch (Exception e) {
                showError("Failed to upload image: " + e.getMessage());
            }
        }
    }
    
    private void clearImage() {
        imagePaths.clear();
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText("No image selected");
    }
    
    /**
     * Show dialog to mark a lost item as resolved.
     * Allows user to select resolution reason and updates status accordingly.
     */
    private void showResolveItemDialog(Item item) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Mark Item as Resolved",
            true
        );
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("✅ Mark Item as Resolved");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        JLabel itemLabel = new JLabel("Item: " + item.getTitle());
        itemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemLabel.setForeground(new Color(108, 117, 125));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(itemLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Resolution reason selection
        JLabel reasonLabel = new JLabel("How was this item resolved?");
        reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        reasonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(reasonLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Radio buttons for resolution options
        ButtonGroup resolutionGroup = new ButtonGroup();
        
        JRadioButton foundMyselfBtn = new JRadioButton("🔍 Found it myself");
        foundMyselfBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        foundMyselfBtn.setOpaque(false);
        foundMyselfBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        foundMyselfBtn.setActionCommand("FOUND_MYSELF");
        resolutionGroup.add(foundMyselfBtn);
        contentPanel.add(foundMyselfBtn);
        contentPanel.add(Box.createVerticalStrut(8));
        
        JRadioButton pickedUpBtn = new JRadioButton("🏢 Picked up from Lost & Found");
        pickedUpBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        pickedUpBtn.setOpaque(false);
        pickedUpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickedUpBtn.setActionCommand("PICKED_UP");
        resolutionGroup.add(pickedUpBtn);
        contentPanel.add(pickedUpBtn);
        contentPanel.add(Box.createVerticalStrut(8));
        
        JRadioButton noLongerLookingBtn = new JRadioButton("❌ No longer looking (cancel report)");
        noLongerLookingBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        noLongerLookingBtn.setOpaque(false);
        noLongerLookingBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        noLongerLookingBtn.setActionCommand("CANCELLED");
        resolutionGroup.add(noLongerLookingBtn);
        contentPanel.add(noLongerLookingBtn);
        
        // Select first option by default
        foundMyselfBtn.setSelected(true);
        
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        JButton confirmBtn = new JButton("✓ Mark as Resolved");
        confirmBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        confirmBtn.setBackground(SUCCESS_COLOR);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setBorderPainted(false);
        confirmBtn.addActionListener(e -> {
            String selectedAction = resolutionGroup.getSelection().getActionCommand();
            resolveItem(item, selectedAction);
            dialog.dispose();
        });
        buttonPanel.add(confirmBtn);
        
        contentPanel.add(buttonPanel);
        
        dialog.add(contentPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Resolve a lost item with the specified resolution reason.
     * Updates the item status to CLAIMED or CANCELLED based on selection.
     */
    private void resolveItem(Item item, String resolutionAction) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // Determine new status based on resolution action
                    Item.ItemStatus newStatus;
                    String resolutionNote;
                    
                    switch (resolutionAction) {
                        case "FOUND_MYSELF" -> {
                            newStatus = Item.ItemStatus.CLAIMED;
                            resolutionNote = "Resolved: Found by owner";
                        }
                        case "PICKED_UP" -> {
                            newStatus = Item.ItemStatus.CLAIMED;
                            resolutionNote = "Resolved: Picked up from Lost & Found";
                        }
                        case "CANCELLED" -> {
                            newStatus = Item.ItemStatus.CANCELLED;
                            resolutionNote = "Cancelled: Owner no longer looking";
                        }
                        default -> {
                            newStatus = Item.ItemStatus.CLAIMED;
                            resolutionNote = "Resolved by owner";
                        }
                    }
                    
                    // Update the item
                    item.setStatus(newStatus);
                    item.setResolvedDate(new java.util.Date());
                    
                    // Add resolution note to description if needed
                    String currentDesc = item.getDescription() != null ? item.getDescription() : "";
                    if (!currentDesc.contains(resolutionNote)) {
                        item.setDescription(currentDesc + "\n[" + resolutionNote + "]");
                    }
                    
                    return itemDAO.update(item);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        String message = switch (resolutionAction) {
                            case "FOUND_MYSELF" -> "Great news! Your item has been marked as found.";
                            case "PICKED_UP" -> "Your item pickup has been confirmed.";
                            case "CANCELLED" -> "Your lost item report has been cancelled.";
                            default -> "Item has been resolved.";
                        };
                        
                        JOptionPane.showMessageDialog(StudentUserPanel.this,
                            message,
                            "Item Resolved",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        refreshMyItems();
                        loadInitialData(); // Refresh stats
                    } else {
                        showError("Failed to update item. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void deleteItem(Item item) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete \"" + item.getTitle() + "\"?\n" +
            "This action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    // Use mongoId for deletion
                    return itemDAO.delete(item.getMongoId());
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(StudentUserPanel.this,
                                "Item deleted successfully.",
                                "Deleted",
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshMyItems();
                        } else {
                            showError("Failed to delete item.");
                        }
                    } catch (Exception e) {
                        showError("Error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void showClaimDialog(Item sourceItem, PotentialMatch match) {
        Item foundItem = match.getItem();
        
        // Create claim dialog
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Submit Claim Request",
            true
        );
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("📦 Claim Found Item");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        JLabel itemLabel = new JLabel("Item: " + foundItem.getTitle());
        itemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemLabel.setForeground(new Color(108, 117, 125));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(itemLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Match confidence
        int confidence = (int) (match.getScore() * 100);
        JLabel confLabel = new JLabel("Match Confidence: " + confidence + "%");
        confLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        confLabel.setForeground(confidence >= 70 ? SUCCESS_COLOR : WARNING_COLOR);
        confLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(confLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Claim details
        JLabel detailsLabel = new JLabel("Why do you believe this is your item? *");
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(detailsLabel);
        
        JTextArea claimDetailsArea = new JTextArea(3, 30);
        claimDetailsArea.setLineWrap(true);
        claimDetailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(claimDetailsArea);
        detailsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        detailsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(detailsScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Identifying features
        JLabel featuresLabel = new JLabel("Identifying features only you would know *");
        featuresLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        featuresLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresLabel);
        
        JTextArea featuresArea = new JTextArea(3, 30);
        featuresArea.setLineWrap(true);
        featuresArea.setWrapStyleWord(true);
        JScrollPane featuresScroll = new JScrollPane(featuresArea);
        featuresScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        featuresScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Proof description
        JLabel proofLabel = new JLabel("What proof can you provide? (receipt, photos, etc.)");
        proofLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        proofLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(proofLabel);
        
        JTextArea proofArea = new JTextArea(2, 30);
        proofArea.setLineWrap(true);
        proofArea.setWrapStyleWord(true);
        JScrollPane proofScroll = new JScrollPane(proofArea);
        proofScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        proofScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(proofScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Estimated Value field
        JLabel valueLabel = new JLabel("Estimated Value ($) - Items over $500 require police verification");
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(valueLabel);
        
        JTextField valueField = new JTextField("0.00");
        valueField.setMaximumSize(new Dimension(150, 30));
        valueField.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(valueField);
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        JButton submitBtn = new JButton("Submit Claim");
        submitBtn.setBackground(PRIMARY_COLOR);
        submitBtn.setForeground(Color.BLACK);
        submitBtn.addActionListener(e -> {
            // Validate
            if (claimDetailsArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please explain why this is your item.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (featuresArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please provide identifying features.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Parse estimated value
            double estimatedValue = 0.0;
            try {
                estimatedValue = Double.parseDouble(valueField.getText().trim());
                if (estimatedValue < 0) estimatedValue = 0.0;
            } catch (NumberFormatException ex) {
                // Keep default 0.0
            }
            
            // Submit claim - pass both the source (lost) item and found item
            submitClaim(sourceItem, foundItem, claimDetailsArea.getText().trim(), 
                featuresArea.getText().trim(), proofArea.getText().trim(), estimatedValue);
            dialog.dispose();
        });
        buttonPanel.add(submitBtn);
        
        contentPanel.add(buttonPanel);
        
        dialog.add(new JScrollPane(contentPanel));
        dialog.setVisible(true);
    }
    
    private void submitClaim(Item lostItem, Item foundItem, String claimDetails, String features, String proof, double estimatedValue) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // Use mongoId as the item identifier
                    ItemClaimRequest claim = new ItemClaimRequest(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        foundItem.getMongoId(),
                        foundItem.getTitle(),
                        estimatedValue  // Use the value entered by user
                    );
                    
                    // Set requesterEmail explicitly for trust score lookups
                    claim.setRequesterEmail(currentUser.getEmail());
                    
                    // Set requester's enterprise/organization for comparison
                    claim.setRequesterEnterpriseId(currentUser.getEnterpriseId());
                    claim.setRequesterOrganizationId(currentUser.getOrganizationId());
                    
                    // Set the lost item ID if available (for updating both items on approval)
                    if (lostItem != null && lostItem.getMongoId() != null) {
                        claim.setLostItemId(lostItem.getMongoId());
                    }
                    
                    claim.setClaimDetails(claimDetails);
                    claim.setIdentifyingFeatures(features);
                    claim.setProofDescription(proof);
                    claim.setItemCategory(foundItem.getCategory().name());
                    
                    if (foundItem.getLocation() != null && foundItem.getLocation().getBuilding() != null) {
                        claim.setFoundLocationId(String.valueOf(foundItem.getLocation().getBuilding().getBuildingId()));
                        claim.setFoundLocationName(foundItem.getLocation().getBuilding().getName());
                    }
                    
                    // Set target based on item's organization (where item is held)
                    claim.setTargetOrganizationId(foundItem.getOrganizationId());
                    claim.setTargetEnterpriseId(foundItem.getEnterpriseId());
                    
                    // ==================== NEW: Set holding enterprise type ====================
                    // This enables dynamic approval chain routing
                    setHoldingEnterpriseInfo(claim, foundItem);
                    
                    String requestId = workRequestService.createRequest(claim);
                    return requestId != null && !requestId.isEmpty();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Build approval workflow message
                        String workflowInfo = "";
                        // Note: We can't access the claim here, but we can show generic message
                        
                        JOptionPane.showMessageDialog(StudentUserPanel.this,
                            "Your claim request has been submitted!\n\n" +
                            "Approval workflow:\n" +
                            "• Campus Coordinator will verify your identity\n" +
                            "• If item is held by MBTA/Airport, they will also approve\n" +
                            "• High-value items require police verification\n\n" +
                            "Track the status in the 'My Claims' tab.",
                            "Claim Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Switch to claims tab
                        tabbedPane.setSelectedIndex(3);
                        refreshClaims();
                    } else {
                        showError("Failed to submit claim. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    /**
     * NEW HELPER METHOD: Sets the holding enterprise information on the claim.
     * This enables dynamic approval chain routing based on where the item is held.
     * 
     * @param claim The claim request to update
     * @param foundItem The found item being claimed
     */
    private void setHoldingEnterpriseInfo(ItemClaimRequest claim, Item foundItem) {
        String itemEnterpriseId = foundItem.getEnterpriseId();
        
        // If no enterprise ID on item, skip (will use default approval chain)
        if (itemEnterpriseId == null || itemEnterpriseId.isEmpty()) {
            return;
        }
        
        // Check if item is at a different enterprise than the student
        if (itemEnterpriseId.equals(currentUser.getEnterpriseId())) {
            // Same enterprise - no external routing needed
            // But check if different organization (cross-campus)
            if (!foundItem.getOrganizationId().equals(currentUser.getOrganizationId())) {
                // Cross-campus within same enterprise
                claim.setItemHoldingEnterpriseType("HIGHER_EDUCATION");
                
                // Try to get org name for display
                try {
                    var org = organizationDAO.findById(foundItem.getOrganizationId());
                    if (org.isPresent()) {
                        claim.setItemHoldingEnterpriseName(org.get().getName());
                    }
                } catch (Exception e) {
                    // Ignore - display name is optional
                }
            }
            return;
        }
        
        // Different enterprise - look up the enterprise type
        try {
            var enterpriseOpt = enterpriseDAO.findById(itemEnterpriseId);
            if (enterpriseOpt.isPresent()) {
                Enterprise holdingEnterprise = enterpriseOpt.get();
                
                // Set the enterprise type for approval chain routing
                if (holdingEnterprise.getType() != null) {
                    claim.setItemHoldingEnterpriseType(holdingEnterprise.getType().name());
                }
                
                // Set name for display
                claim.setItemHoldingEnterpriseName(holdingEnterprise.getName());
            }
        } catch (Exception e) {
            // If lookup fails, try to infer from enterprise ID
            inferEnterpriseTypeFromId(claim, itemEnterpriseId);
        }
    }
    
    /**
     * Fallback method to infer enterprise type from ID if lookup fails.
     * Uses naming conventions to determine type.
     */
    private void inferEnterpriseTypeFromId(ItemClaimRequest claim, String enterpriseId) {
        if (enterpriseId == null) return;
        
        String idLower = enterpriseId.toLowerCase();
        
        if (idLower.contains("mbta") || idLower.contains("transit")) {
            claim.setItemHoldingEnterpriseType("PUBLIC_TRANSIT");
            claim.setItemHoldingEnterpriseName("MBTA");
        } else if (idLower.contains("airport") || idLower.contains("logan")) {
            claim.setItemHoldingEnterpriseType("AIRPORT");
            claim.setItemHoldingEnterpriseName("Logan Airport");
        } else if (idLower.contains("police") || idLower.contains("bpd") || idLower.contains("nupd")) {
            claim.setItemHoldingEnterpriseType("LAW_ENFORCEMENT");
            claim.setItemHoldingEnterpriseName("Police Department");
        }
        // If can't infer, leave null - will use default approval chain
    }
    
    private void showClaimDetails(WorkRequest request) {
        if (request instanceof ItemClaimRequest) {
            RequestDetailDialog dialog = new RequestDetailDialog(
                SwingUtilities.getWindowAncestor(this),
                request,
                currentUser
            );
            dialog.setVisible(true);
        }
    }
    
    private void showItemDetails(Item item) {
        // Show item details dialog
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Item Details",
            true
        );
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Type and category
        JLabel typeLabel = new JLabel(item.getType().getIcon() + " " + item.getType().getLabel() + 
            " • " + item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName());
        typeLabel.setFont(EMOJI_FONT);
        typeLabel.setForeground(new Color(108, 117, 125));
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(typeLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Description
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            JLabel descLabel = new JLabel("<html><b>Description:</b> " + item.getDescription() + "</html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(descLabel);
            contentPanel.add(Box.createVerticalStrut(10));
        }
        
        // Location
        if (item.getLocation() != null) {
            JLabel locLabel = new JLabel("📍 " + item.getLocation().getBuilding().getName());
            locLabel.setFont(EMOJI_FONT);
            locLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(locLabel);
            contentPanel.add(Box.createVerticalStrut(10));
        }
        
        // Date
        JLabel dateLabel = new JLabel("Reported: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        dateLabel.setForeground(new Color(134, 142, 150));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(dateLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // If it's a found item, show claim button
        if (item.getType() == Item.ItemType.FOUND && item.getStatus() == Item.ItemStatus.OPEN) {
            JButton claimBtn = new JButton("📋 Submit Claim for This Item");
            claimBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
            claimBtn.setBackground(PRIMARY_COLOR);
            claimBtn.setForeground(Color.BLACK);
            claimBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            claimBtn.addActionListener(e -> {
                dialog.dispose();
                // Create a PotentialMatch with default score
                // Note: sourceItem is null when claiming from Search tab (no matched lost item)
                PotentialMatch match = new PotentialMatch(item, 0.5);
                showClaimDialog(null, match);
            });
            contentPanel.add(claimBtn);
        }
        
        // Close button
        contentPanel.add(Box.createVerticalGlue());
        JButton closeBtn = new JButton("Close");
        closeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeBtn.addActionListener(e -> dialog.dispose());
        contentPanel.add(closeBtn);
        
        dialog.add(new JScrollPane(contentPanel));
        dialog.setVisible(true);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Wrapper for item combo box options.
     */
    private static class ItemOption {
        Item item;
        String displayName;
        
        ItemOption(Item item, String displayName) {
            this.item = item;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    /**
     * Custom renderer for category combo box.
     */
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
