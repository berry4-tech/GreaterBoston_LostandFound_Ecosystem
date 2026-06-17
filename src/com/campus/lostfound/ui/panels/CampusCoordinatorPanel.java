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
 * Main panel for Campus Coordinator role.
 * 
 * Features:
 * - Dashboard with key metrics and alerts
 * - Work queue for processing item claims
 * - Register incoming found items
 * - Create cross-enterprise transfer requests
 * - View and manage item inventory
 * 
 * @author Developer 2 - UI Panels
 */
public class CampusCoordinatorPanel extends JPanel {
    
    // Data
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoUserDAO userDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private WorkRequestService workRequestService;
    private ItemMatcher itemMatcher;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JLabel welcomeLabel;
    
    // Dashboard components
    private JLabel pendingClaimsLabel;
    private JLabel pendingTransfersLabel;
    private JLabel totalItemsLabel;
    private JLabel overdueLabel;
    private JPanel alertsPanel;
    
    // Work Queue tab
    private WorkQueueTablePanel workQueuePanel;
    
    // Register Found Item tab
    private JTextField foundTitleField;
    private JTextArea foundDescriptionArea;
    private JComboBox<Item.ItemCategory> foundCategoryCombo;
    private JComboBox<Building> foundBuildingCombo;
    private JTextField foundRoomField;
    private JTextField foundColorField;
    private JTextField foundBrandField;
    private JTextField finderNameField;
    private JTextField finderContactField;
    private JLabel foundImagePreviewLabel;
    private List<String> foundImagePaths;
    private JButton registerButton;
    
    // Transfer Request tab
    private JComboBox<ItemOption> transferItemCombo;
    private JComboBox<EnterpriseOption> destEnterpriseCombo;
    private JComboBox<OrganizationOption> destOrgCombo;
    private JTextField studentNameField;
    private JTextField studentEmailField;
    private JTextField studentIdField;
    private JTextField pickupLocationField;
    private JComboBox<String> transferMethodCombo;
    private JTextArea transferNotesArea;
    private String matchedLostItemId;  // Store matched lost item ID for auto-closure
    private JPanel matchesPanel;  // Panel to display auto-match results
    private JList<PotentialMatch> matchesList;  // List of matches
    private DefaultListModel<PotentialMatch> matchesListModel;  // Model for matches list
    
    // Inventory tab
    private JPanel inventoryListPanel;
    private JScrollPane inventoryScrollPane;
    private JComboBox<String> inventoryFilterCombo;
    private JComboBox<String> inventoryStatusCombo;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Color PRIMARY_COLOR = new Color(13, 110, 253);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 32);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);
    
    /**
     * Create a new CampusCoordinatorPanel.
     * 
     * @param currentUser The logged-in campus coordinator
     */
    public CampusCoordinatorPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.itemMatcher = new ItemMatcher();
        this.foundImagePaths = new ArrayList<>();
        
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
        tabbedPane.addTab("Dashboard", createDashboardTab());
        tabbedPane.addTab("Work Queue", createWorkQueueTab());
        tabbedPane.addTab("Register Found Item", createRegisterFoundTab());
        tabbedPane.addTab("Transfer Request", createTransferRequestTab());
        tabbedPane.addTab("Inventory", createInventoryTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> workQueuePanel.loadRequestsForRole("CAMPUS_COORDINATOR");
                case 3 -> loadTransferData();
                case 4 -> refreshInventory();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(52, 73, 94));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Welcome
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        welcomeLabel = new JLabel("Campus Coordinator Dashboard");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(welcomeLabel);
        
        JLabel subtitleLabel = new JLabel(currentUser.getFullName() + " • " + 
            getOrganizationName());
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("Refresh All");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(41, 128, 185));
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshAll());
        rightPanel.add(refreshBtn);
        
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
        
        // Pending Claims card
        JPanel claimsCard = createStatCard("Pending Claims", "0", PRIMARY_COLOR);
        pendingClaimsLabel = (JLabel) ((JPanel) claimsCard.getComponent(0)).getComponent(0);
        statsRow.add(claimsCard);
        
        // Pending Transfers card
        JPanel transfersCard = createStatCard("Pending Transfers", "0", INFO_COLOR);
        pendingTransfersLabel = (JLabel) ((JPanel) transfersCard.getComponent(0)).getComponent(0);
        statsRow.add(transfersCard);
        
        // Total Items card
        JPanel itemsCard = createStatCard("Items in Inventory", "0", SUCCESS_COLOR);
        totalItemsLabel = (JLabel) ((JPanel) itemsCard.getComponent(0)).getComponent(0);
        statsRow.add(itemsCard);
        
        // Overdue card
        JPanel overdueCard = createStatCard("Overdue Requests", "0", DANGER_COLOR);
        overdueLabel = (JLabel) ((JPanel) overdueCard.getComponent(0)).getComponent(0);
        statsRow.add(overdueCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - split into alerts and recent activity
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = new JPanel(new BorderLayout());
        alertsContainer.setOpaque(false);
        TitledBorder alertsBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Alerts & Notifications",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        );
        alertsContainer.setBorder(alertsBorder);
        
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Color.WHITE);
        
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsContainer.add(alertsScroll, BorderLayout.CENTER);
        
        contentPanel.add(alertsContainer);
        
        // Quick actions panel
        JPanel actionsContainer = new JPanel(new BorderLayout());
        actionsContainer.setOpaque(false);
        TitledBorder actionsBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Quick Actions",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        );
        actionsContainer.setBorder(actionsBorder);
        
        JPanel actionsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        actionsPanel.setBackground(Color.WHITE);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        actionsPanel.add(createQuickActionButton("Process Claims", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("Register Item", () -> tabbedPane.setSelectedIndex(2)));
        actionsPanel.add(createQuickActionButton("New Transfer", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("View Inventory", () -> tabbedPane.setSelectedIndex(4)));
        actionsPanel.add(createQuickActionButton("Search Items", this::openSearchDialog));
        actionsPanel.add(createQuickActionButton("Run Matching", this::runAutoMatching));
        
        actionsContainer.add(actionsPanel, BorderLayout.CENTER);
        contentPanel.add(actionsContainer);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        textPanel.add(valueLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(108, 117, 125));
        textPanel.add(titleLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JButton createQuickActionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
    
    // ==================== TAB 2: WORK QUEUE ====================
    
    private JPanel createWorkQueueTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        
        JLabel titleLabel = new JLabel("Claims & Requests Work Queue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Review and process pending item claims and transfer requests");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(helpLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Work queue panel
        workQueuePanel = new WorkQueueTablePanel(currentUser);
        workQueuePanel.setOnRequestDoubleClicked(this::handleWorkRequest);
        
        panel.add(workQueuePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 3: REGISTER FOUND ITEM ====================
    
    private JPanel createRegisterFoundTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(800, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("Register Incoming Found Item");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Log items turned in by finders or transferred from other locations");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Item details section
        formContainer.add(createSectionLabel("Item Details"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Title and Category row
        JPanel row1 = new JPanel(new GridLayout(1, 2, 15, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel titlePanel = createFieldPanel("Item Title *");
        foundTitleField = new JTextField();
        titlePanel.add(foundTitleField);
        row1.add(titlePanel);
        
        JPanel categoryPanel = createFieldPanel("Category *");
        foundCategoryCombo = new JComboBox<>(Item.ItemCategory.values());
        foundCategoryCombo.setRenderer(new CategoryComboRenderer());
        categoryPanel.add(foundCategoryCombo);
        row1.add(categoryPanel);
        
        formContainer.add(row1);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Description
        formContainer.add(createFormLabel("Description *"));
        foundDescriptionArea = new JTextArea(3, 30);
        foundDescriptionArea.setLineWrap(true);
        foundDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(foundDescriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Location row
        JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel buildingPanel = createFieldPanel("Building/Location *");
        foundBuildingCombo = new JComboBox<>();
        loadBuildings();
        buildingPanel.add(foundBuildingCombo);
        row2.add(buildingPanel);
        
        JPanel roomPanel = createFieldPanel("Room/Area");
        foundRoomField = new JTextField();
        roomPanel.add(foundRoomField);
        row2.add(roomPanel);
        
        formContainer.add(row2);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Color and Brand row
        JPanel row3 = new JPanel(new GridLayout(1, 2, 15, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel colorPanel = createFieldPanel("Primary Color");
        foundColorField = new JTextField();
        colorPanel.add(foundColorField);
        row3.add(colorPanel);
        
        JPanel brandPanel = createFieldPanel("Brand");
        foundBrandField = new JTextField();
        brandPanel.add(foundBrandField);
        row3.add(brandPanel);
        
        formContainer.add(row3);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Finder info section
        formContainer.add(createSectionLabel("Finder Information (if known)"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel row4 = new JPanel(new GridLayout(1, 2, 15, 0));
        row4.setOpaque(false);
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row4.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel finderNamePanel = createFieldPanel("Finder Name");
        finderNameField = new JTextField();
        finderNamePanel.add(finderNameField);
        row4.add(finderNamePanel);
        
        JPanel finderContactPanel = createFieldPanel("Finder Contact (email/phone)");
        finderContactField = new JTextField();
        finderContactPanel.add(finderContactField);
        row4.add(finderContactPanel);
        
        formContainer.add(row4);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Image upload
        formContainer.add(createFormLabel("Photo (Recommended)"));
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imagePanel.setOpaque(false);
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        foundImagePreviewLabel = new JLabel("No image");
        foundImagePreviewLabel.setPreferredSize(new Dimension(100, 100));
        foundImagePreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        foundImagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        foundImagePreviewLabel.setBackground(new Color(248, 249, 250));
        foundImagePreviewLabel.setOpaque(true);
        imagePanel.add(foundImagePreviewLabel);
        
        imagePanel.add(Box.createHorizontalStrut(15));
        
        JButton uploadButton = new JButton("Upload Photo");
        uploadButton.addActionListener(e -> selectFoundItemImage());
        imagePanel.add(uploadButton);
        
        JButton clearImageButton = new JButton("Clear");
        clearImageButton.addActionListener(e -> clearFoundItemImage());
        imagePanel.add(Box.createHorizontalStrut(10));
        imagePanel.add(clearImageButton);
        
        formContainer.add(imagePanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        registerButton = new JButton("Register Item");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setBackground(SUCCESS_COLOR);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setPreferredSize(new Dimension(160, 45));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> registerFoundItem());
        buttonPanel.add(registerButton);
        
        JButton registerAndMatchButton = new JButton("Register & Find Matches");
        registerAndMatchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerAndMatchButton.setBackground(PRIMARY_COLOR);
        registerAndMatchButton.setForeground(Color.WHITE);
        registerAndMatchButton.setFocusPainted(false);
        registerAndMatchButton.setBorderPainted(false);
        registerAndMatchButton.setPreferredSize(new Dimension(200, 45));
        registerAndMatchButton.addActionListener(e -> registerFoundItemAndMatch());
        buttonPanel.add(registerAndMatchButton);
        
        JButton clearButton = new JButton("Clear Form");
        clearButton.setPreferredSize(new Dimension(120, 45));
        clearButton.addActionListener(e -> clearFoundItemForm());
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
    
    // ==================== TAB 4: TRANSFER REQUEST ====================
    
    private JPanel createTransferRequestTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(800, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("Create Cross-Enterprise Transfer Request");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Request transfer of a found item to another campus or organization");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Item selection section
        formContainer.add(createSectionLabel("Select Item to Transfer"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Item *"));
        JPanel itemSelectRow = new JPanel(new BorderLayout(10, 0));
        itemSelectRow.setOpaque(false);
        itemSelectRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        itemSelectRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        transferItemCombo = new JComboBox<>();
        itemSelectRow.add(transferItemCombo, BorderLayout.CENTER);
        
        JButton findMatchesBtn = new JButton("🔍 Find Matches");
        findMatchesBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        findMatchesBtn.setBackground(new Color(40, 167, 69));
        findMatchesBtn.setForeground(Color.WHITE);
        findMatchesBtn.setFocusPainted(false);
        findMatchesBtn.setBorderPainted(false);
        findMatchesBtn.setPreferredSize(new Dimension(140, 35));
        findMatchesBtn.addActionListener(e -> findMatchesForSelectedItem());
        itemSelectRow.add(findMatchesBtn, BorderLayout.EAST);
        
        formContainer.add(itemSelectRow);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Matches panel (initially hidden, shown when matches are found)
        matchesPanel = new JPanel();
        matchesPanel.setLayout(new BorderLayout());
        matchesPanel.setOpaque(false);
        matchesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        matchesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        matchesPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(40, 167, 69)),
            "🔍 Potential Lost Item Matches (Click to select)",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 12),
            new Color(40, 167, 69)
        ));
        matchesPanel.setVisible(false);
        
        matchesListModel = new DefaultListModel<>();
        matchesList = new JList<>(matchesListModel);
        matchesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matchesList.setCellRenderer(new MatchListCellRenderer());
        matchesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMatchSelected();
            }
        });
        
        JScrollPane matchesScroll = new JScrollPane(matchesList);
        matchesScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 120));
        matchesPanel.add(matchesScroll, BorderLayout.CENTER);
        
        formContainer.add(matchesPanel);
        formContainer.add(Box.createVerticalStrut(10));
        
        // Destination section
        formContainer.add(createSectionLabel("Destination"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel destRow = new JPanel(new GridLayout(1, 2, 15, 0));
        destRow.setOpaque(false);
        destRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        destRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel destEntPanel = createFieldPanel("Destination Enterprise *");
        destEnterpriseCombo = new JComboBox<>();
        destEnterpriseCombo.addActionListener(e -> updateDestinationOrganizations());
        destEntPanel.add(destEnterpriseCombo);
        destRow.add(destEntPanel);
        
        JPanel destOrgPanel = createFieldPanel("Destination Organization *");
        destOrgCombo = new JComboBox<>();
        destOrgPanel.add(destOrgCombo);
        destRow.add(destOrgPanel);
        
        formContainer.add(destRow);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Student info section
        formContainer.add(createSectionLabel("Student Information"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel studentRow1 = new JPanel(new GridLayout(1, 2, 15, 0));
        studentRow1.setOpaque(false);
        studentRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        studentRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel namePanel = createFieldPanel("Student Name *");
        studentNameField = new JTextField();
        namePanel.add(studentNameField);
        studentRow1.add(namePanel);
        
        JPanel emailPanel = createFieldPanel("Student Email *");
        studentEmailField = new JTextField();
        emailPanel.add(studentEmailField);
        studentRow1.add(emailPanel);
        
        formContainer.add(studentRow1);
        formContainer.add(Box.createVerticalStrut(15));
        
        JPanel studentRow2 = new JPanel(new GridLayout(1, 2, 15, 0));
        studentRow2.setOpaque(false);
        studentRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        studentRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel idPanel = createFieldPanel("Student ID");
        studentIdField = new JTextField();
        idPanel.add(studentIdField);
        studentRow2.add(idPanel);
        
        JPanel pickupPanel = createFieldPanel("Pickup Location *");
        pickupLocationField = new JTextField();
        pickupPanel.add(pickupLocationField);
        studentRow2.add(pickupPanel);
        
        formContainer.add(studentRow2);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Transfer details section
        formContainer.add(createSectionLabel("Transfer Details"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Transfer Method *"));
        transferMethodCombo = new JComboBox<>(new String[]{
            "Student Pickup at Source", "Inter-campus Courier", "Student Pickup at Destination"
        });
        transferMethodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        transferMethodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(transferMethodCombo);
        formContainer.add(Box.createVerticalStrut(15));
        
        formContainer.add(createFormLabel("Notes"));
        transferNotesArea = new JTextArea(3, 30);
        transferNotesArea.setLineWrap(true);
        transferNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(transferNotesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(notesScroll);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton submitTransferButton = new JButton("Submit Transfer Request");
        submitTransferButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitTransferButton.setBackground(PRIMARY_COLOR);
        submitTransferButton.setForeground(Color.WHITE);
        submitTransferButton.setFocusPainted(false);
        submitTransferButton.setBorderPainted(false);
        submitTransferButton.setPreferredSize(new Dimension(220, 45));
        submitTransferButton.addActionListener(e -> submitTransferRequest());
        buttonPanel.add(submitTransferButton);
        
        JButton clearTransferButton = new JButton("Clear Form");
        clearTransferButton.setPreferredSize(new Dimension(120, 45));
        clearTransferButton.addActionListener(e -> clearTransferForm());
        buttonPanel.add(clearTransferButton);
        
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
    
    // ==================== TAB 5: INVENTORY ====================
    
    private JPanel createInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with filters
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Item Inventory");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        JLabel filterLabel = new JLabel("Category:");
        filterPanel.add(filterLabel);
        
        inventoryFilterCombo = new JComboBox<>();
        inventoryFilterCombo.addItem("All Categories");
        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            inventoryFilterCombo.addItem(cat.getEmoji() + " " + cat.getDisplayName());
        }
        inventoryFilterCombo.setPreferredSize(new Dimension(180, 28));
        inventoryFilterCombo.addActionListener(e -> refreshInventory());
        filterPanel.add(inventoryFilterCombo);
        
        JLabel statusLabel = new JLabel("Status:");
        filterPanel.add(statusLabel);
        
        inventoryStatusCombo = new JComboBox<>(new String[]{
            "All Statuses", "Open", "Pending Claim", "Verified"
        });
        inventoryStatusCombo.setPreferredSize(new Dimension(140, 28));
        inventoryStatusCombo.addActionListener(e -> refreshInventory());
        filterPanel.add(inventoryStatusCombo);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshInventory());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Inventory list
        inventoryListPanel = new JPanel();
        inventoryListPanel.setLayout(new BoxLayout(inventoryListPanel, BoxLayout.Y_AXIS));
        inventoryListPanel.setBackground(Color.WHITE);
        inventoryListPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        inventoryScrollPane = new JScrollPane(inventoryListPanel);
        inventoryScrollPane.setBorder(null);
        inventoryScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(inventoryScrollPane, BorderLayout.CENTER);
        
        return panel;
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
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(52, 73, 94));
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
    
    private String getOrganizationName() {
        if (currentUser.getOrganizationId() != null) {
            for (Organization org : organizationDAO.findAll()) {
                if (org.getOrganizationId().equals(currentUser.getOrganizationId())) {
                    return org.getName();
                }
            }
        }
        return "Unknown Organization";
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                // Get pending claims
                List<WorkRequest> claims = workRequestService.getRequestsForRole(
                    "CAMPUS_COORDINATOR", currentUser.getOrganizationId());
                int pendingClaims = (int) claims.stream()
                    .filter(r -> r.getRequestType() == WorkRequest.RequestType.ITEM_CLAIM)
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                // Get pending transfers
                int pendingTransfers = (int) claims.stream()
                    .filter(r -> r.getRequestType() == WorkRequest.RequestType.CROSS_CAMPUS_TRANSFER ||
                                r.getRequestType() == WorkRequest.RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER)
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                // Get total items in inventory (for this organization)
                List<Item> items = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN || 
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM)
                    .collect(Collectors.toList());
                int totalItems = items.size();
                
                // Get overdue count
                int overdue = (int) claims.stream()
                    .filter(WorkRequest::isOverdue)
                    .count();
                
                return new int[]{pendingClaims, pendingTransfers, totalItems, overdue};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    pendingClaimsLabel.setText(String.valueOf(stats[0]));
                    pendingTransfersLabel.setText(String.valueOf(stats[1]));
                    totalItemsLabel.setText(String.valueOf(stats[2]));
                    overdueLabel.setText(String.valueOf(stats[3]));
                    
                    // Update alerts
                    loadAlerts(stats);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadAlerts(int[] stats) {
        alertsPanel.removeAll();
        
        if (stats[3] > 0) {
            alertsPanel.add(createAlertItem("!", "You have " + stats[3] + " overdue request(s)!", DANGER_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[0] > 5) {
            alertsPanel.add(createAlertItem("i", stats[0] + " claims waiting for review", WARNING_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[1] > 0) {
            alertsPanel.add(createAlertItem(">", stats[1] + " transfer request(s) pending", INFO_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (alertsPanel.getComponentCount() == 0) {
            JLabel noAlerts = new JLabel("No alerts - you're all caught up!");
            noAlerts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            noAlerts.setForeground(SUCCESS_COLOR);
            alertsPanel.add(noAlerts);
        }
        
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
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        iconLabel.setForeground(color);
        panel.add(iconLabel, BorderLayout.WEST);
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
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
                    foundBuildingCombo.removeAllItems();
                    for (Building b : buildings) {
                        foundBuildingCombo.addItem(b);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadTransferData() {
        // Load items available for transfer
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Item> items;
            List<Enterprise> enterprises;
            
            @Override
            protected Void doInBackground() {
                items = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
                
                enterprises = enterpriseDAO.findAll();
                return null;
            }
            
            @Override
            protected void done() {
                // Populate item combo
                transferItemCombo.removeAllItems();
                transferItemCombo.addItem(new ItemOption(null, "-- Select an item --"));
                for (Item item : items) {
                    transferItemCombo.addItem(new ItemOption(item, 
                        item.getCategory().getEmoji() + " " + item.getTitle()));
                }
                
                // Populate enterprise combo - include ALL enterprises
                // (same enterprise for cross-campus transfers, different for cross-enterprise)
                destEnterpriseCombo.removeAllItems();
                destEnterpriseCombo.addItem(new EnterpriseOption(null, "-- Select enterprise --"));
                for (Enterprise e : enterprises) {
                    // Include all enterprises - filtering happens at organization level
                    destEnterpriseCombo.addItem(new EnterpriseOption(e, e.getName()));
                }
            }
        };
        worker.execute();
    }
    
    private void updateDestinationOrganizations() {
        destOrgCombo.removeAllItems();
        destOrgCombo.addItem(new OrganizationOption(null, "-- Select organization --"));
        
        EnterpriseOption selected = (EnterpriseOption) destEnterpriseCombo.getSelectedItem();
        if (selected != null && selected.enterprise != null) {
            // Check if this is the same enterprise as the current user
            boolean isSameEnterprise = selected.enterprise.getEnterpriseId()
                .equals(currentUser.getEnterpriseId());
            
            for (Organization org : organizationDAO.findAll()) {
                if (selected.enterprise.getEnterpriseId().equals(org.getEnterpriseId())) {
                    // If same enterprise, exclude current user's organization
                    // (can't transfer to yourself)
                    if (isSameEnterprise && org.getOrganizationId().equals(currentUser.getOrganizationId())) {
                        continue; // Skip own organization
                    }
                    destOrgCombo.addItem(new OrganizationOption(org, org.getName()));
                }
            }
        }
    }
    
    private void refreshInventory() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    displayInventory(filterInventory(items));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private List<Item> filterInventory(List<Item> items) {
        String categoryFilter = (String) inventoryFilterCombo.getSelectedItem();
        String statusFilter = (String) inventoryStatusCombo.getSelectedItem();
        
        return items.stream()
            .filter(item -> {
                if (categoryFilter == null || categoryFilter.equals("All Categories")) return true;
                for (Item.ItemCategory cat : Item.ItemCategory.values()) {
                    if (categoryFilter.contains(cat.getDisplayName())) {
                        return item.getCategory() == cat;
                    }
                }
                return true;
            })
            .filter(item -> {
                if (statusFilter == null || statusFilter.equals("All Statuses")) return true;
                return switch (statusFilter) {
                    case "Open" -> item.getStatus() == Item.ItemStatus.OPEN;
                    case "Pending Claim" -> item.getStatus() == Item.ItemStatus.PENDING_CLAIM;
                    case "Verified" -> item.getStatus() == Item.ItemStatus.VERIFIED;
                    default -> true;
                };
            })
            .collect(Collectors.toList());
    }
    
    private void displayInventory(List<Item> items) {
        inventoryListPanel.removeAll();
        
        if (items.isEmpty()) {
            JLabel emptyLabel = new JLabel("No items in inventory matching filters");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            inventoryListPanel.add(emptyLabel);
        } else {
            for (Item item : items) {
                inventoryListPanel.add(createInventoryItemCard(item));
                inventoryListPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        inventoryListPanel.revalidate();
        inventoryListPanel.repaint();
    }
    
    private JPanel createInventoryItemCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // Left - Category icon
        JLabel catLabel = new JLabel(item.getCategory().getEmoji());
        catLabel.setFont(EMOJI_FONT_LARGE);
        catLabel.setPreferredSize(new Dimension(50, 0));
        card.add(catLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        centerPanel.add(titleLabel);
        
        JLabel detailsLabel = new JLabel(item.getCategory().getDisplayName() + " • " +
            (item.getLocation() != null ? item.getLocation().getBuilding().getName() : "Unknown"));
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailsLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(detailsLabel);
        
        JLabel dateLabel = new JLabel("Registered: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Status and actions
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(120, 0));
        
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(statusLabel);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        JButton transferBtn = new JButton("Transfer");
        transferBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        transferBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        transferBtn.addActionListener(e -> {
            tabbedPane.setSelectedIndex(3);
            selectItemForTransfer(item);
        });
        rightPanel.add(transferBtn);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    // ==================== ACTIONS ====================
    
    private void handleWorkRequest(WorkRequest request) {
        RequestDetailDialog dialog = new RequestDetailDialog(
            SwingUtilities.getWindowAncestor(this),
            request,
            currentUser
        );
        dialog.setOnRequestUpdated(r -> {
            workQueuePanel.loadRequestsForRole("CAMPUS_COORDINATOR");
            loadDashboardData();
        });
        dialog.setVisible(true);
    }
    
    private void registerFoundItem() {
        if (!validateFoundItemForm()) return;
        
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Building building = (Building) foundBuildingCombo.getSelectedItem();
                    Location location = new Location(building, foundRoomField.getText().trim(), "");
                    
                    Item item = new Item(
                        foundTitleField.getText().trim(),
                        foundDescriptionArea.getText().trim(),
                        (Item.ItemCategory) foundCategoryCombo.getSelectedItem(),
                        Item.ItemType.FOUND,
                        location,
                        currentUser
                    );
                    
                    item.setStatus(Item.ItemStatus.OPEN);
                    
                    if (!foundColorField.getText().trim().isEmpty()) {
                        item.setPrimaryColor(foundColorField.getText().trim());
                    }
                    if (!foundBrandField.getText().trim().isEmpty()) {
                        item.setBrand(foundBrandField.getText().trim());
                    }
                    
                    for (String path : foundImagePaths) {
                        item.addImagePath(path);
                    }
                    
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
                registerButton.setEnabled(true);
                registerButton.setText("Register Item");
                
                try {
                    String itemId = get();
                    if (itemId != null) {
                        JOptionPane.showMessageDialog(CampusCoordinatorPanel.this,
                            "Item registered successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearFoundItemForm();
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
    
    private void registerFoundItemAndMatch() {
        if (!validateFoundItemForm()) return;
        
        registerButton.setEnabled(false);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Building building = (Building) foundBuildingCombo.getSelectedItem();
                    Location location = new Location(building, foundRoomField.getText().trim(), "");
                    
                    Item item = new Item(
                        foundTitleField.getText().trim(),
                        foundDescriptionArea.getText().trim(),
                        (Item.ItemCategory) foundCategoryCombo.getSelectedItem(),
                        Item.ItemType.FOUND,
                        location,
                        currentUser
                    );
                    
                    item.setStatus(Item.ItemStatus.OPEN);
                    item.setEnterpriseId(currentUser.getEnterpriseId());
                    item.setOrganizationId(currentUser.getOrganizationId());
                    
                    if (!foundColorField.getText().trim().isEmpty()) {
                        item.setPrimaryColor(foundColorField.getText().trim());
                    }
                    if (!foundBrandField.getText().trim().isEmpty()) {
                        item.setBrand(foundBrandField.getText().trim());
                    }
                    
                    return itemDAO.create(item);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                registerButton.setEnabled(true);
                
                try {
                    String itemId = get();
                    if (itemId != null) {
                        clearFoundItemForm();
                        loadDashboardData();
                        
                        // Show matches
                        showMatchesForNewItem(itemId);
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
    
    private void showMatchesForNewItem(String itemId) {
        Optional<Item> itemOpt = itemDAO.findById(itemId);
        if (itemOpt.isEmpty()) return;
        
        Item foundItem = itemOpt.get();
        List<Item> allItems = itemDAO.findAll();
        List<ItemMatcher.PotentialMatch> matches = itemMatcher.findMatches(foundItem, allItems);
        
        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Item registered. No potential matches found for lost items.",
                "No Matches",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Show matches in a dialog
            StringBuilder sb = new StringBuilder();
            sb.append("Item registered! Found ").append(matches.size()).append(" potential match(es):\n\n");
            
            for (int i = 0; i < Math.min(5, matches.size()); i++) {
                ItemMatcher.PotentialMatch match = matches.get(i);
                int confidence = (int) (match.getScore() * 100);
                sb.append("• ").append(match.getItem().getTitle())
                  .append(" (").append(confidence).append("% match)\n");
            }
            
            if (matches.size() > 5) {
                sb.append("\n... and ").append(matches.size() - 5).append(" more");
            }
            
            JOptionPane.showMessageDialog(this,
                sb.toString(),
                "Potential Matches Found",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private boolean validateFoundItemForm() {
        if (foundTitleField.getText().trim().isEmpty()) {
            showError("Please enter an item title.");
            foundTitleField.requestFocus();
            return false;
        }
        if (foundDescriptionArea.getText().trim().isEmpty()) {
            showError("Please enter a description.");
            foundDescriptionArea.requestFocus();
            return false;
        }
        if (foundBuildingCombo.getSelectedItem() == null) {
            showError("Please select a building/location.");
            return false;
        }
        return true;
    }
    
    private void clearFoundItemForm() {
        foundTitleField.setText("");
        foundDescriptionArea.setText("");
        foundCategoryCombo.setSelectedIndex(0);
        if (foundBuildingCombo.getItemCount() > 0) {
            foundBuildingCombo.setSelectedIndex(0);
        }
        foundRoomField.setText("");
        foundColorField.setText("");
        foundBrandField.setText("");
        finderNameField.setText("");
        finderContactField.setText("");
        clearFoundItemImage();
    }
    
    private void selectFoundItemImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String destPath = "items/images/" + System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(System.getProperty("user.dir"), destPath);
                destFile.getParentFile().mkdirs();
                java.nio.file.Files.copy(file.toPath(), destFile.toPath());
                
                foundImagePaths.clear();
                foundImagePaths.add(destPath);
                
                ImageIcon icon = new ImageIcon(destFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                foundImagePreviewLabel.setIcon(new ImageIcon(img));
                foundImagePreviewLabel.setText("");
            } catch (Exception e) {
                showError("Failed to upload image: " + e.getMessage());
            }
        }
    }
    
    private void clearFoundItemImage() {
        foundImagePaths.clear();
        foundImagePreviewLabel.setIcon(null);
        foundImagePreviewLabel.setText("No image");
    }
    
    private void submitTransferRequest() {
        // Validate
        ItemOption selectedItem = (ItemOption) transferItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item to transfer.");
            return;
        }
        
        OrganizationOption selectedOrg = (OrganizationOption) destOrgCombo.getSelectedItem();
        if (selectedOrg == null || selectedOrg.organization == null) {
            showError("Please select a destination organization.");
            return;
        }
        
        if (studentNameField.getText().trim().isEmpty()) {
            showError("Please enter the student name.");
            return;
        }
        
        if (studentEmailField.getText().trim().isEmpty()) {
            showError("Please enter the student email.");
            return;
        }
        
        if (pickupLocationField.getText().trim().isEmpty()) {
            showError("Please enter the pickup location.");
            return;
        }
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Item item = selectedItem.item;
                    
                    CrossCampusTransferRequest transfer = new CrossCampusTransferRequest(
                        currentUser.getEmail(),  // Use email as consistent identifier
                        currentUser.getFullName(),
                        item.getMongoId(),
                        item.getTitle(),
                        studentIdField.getText().trim(),
                        studentNameField.getText().trim()
                    );
                    
                    transfer.setItemCategory(item.getCategory().name());
                    transfer.setSourceCampusName(getOrganizationName());
                    transfer.setSourceCoordinatorId(currentUser.getEmail());  // Use email
                    transfer.setSourceCoordinatorName(currentUser.getFullName());
                    
                    // Set requester's organization context (critical for org-level access control)
                    transfer.setRequesterOrganizationId(currentUser.getOrganizationId());
                    transfer.setRequesterEnterpriseId(currentUser.getEnterpriseId());
                    
                    EnterpriseOption destEnt = (EnterpriseOption) destEnterpriseCombo.getSelectedItem();
                    transfer.setDestinationCampusName(destEnt.enterprise.getName());
                    transfer.setTargetEnterpriseId(destEnt.enterprise.getEnterpriseId());
                    transfer.setTargetOrganizationId(selectedOrg.organization.getOrganizationId());
                    
                    transfer.setStudentEmail(studentEmailField.getText().trim());
                    transfer.setPickupLocation(pickupLocationField.getText().trim());
                    transfer.setTransferMethod((String) transferMethodCombo.getSelectedItem());
                    transfer.setTrackingNotes(transferNotesArea.getText().trim());
                    
                    // Set matched lost item ID if this came from auto-match
                    if (matchedLostItemId != null && !matchedLostItemId.isEmpty()) {
                        transfer.setLostItemId(matchedLostItemId);
                    }
                    
                    String requestId = workRequestService.createRequest(transfer);
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
                        JOptionPane.showMessageDialog(CampusCoordinatorPanel.this,
                            "Transfer request submitted successfully!\n" +
                            "The destination organization will be notified.",
                            "Request Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearTransferForm();
                        loadDashboardData();
                    } else {
                        showError("Failed to submit transfer request.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void clearTransferForm() {
        if (transferItemCombo.getItemCount() > 0) {
            transferItemCombo.setSelectedIndex(0);
        }
        if (destEnterpriseCombo.getItemCount() > 0) {
            destEnterpriseCombo.setSelectedIndex(0);
        }
        studentNameField.setText("");
        studentEmailField.setText("");
        studentIdField.setText("");
        pickupLocationField.setText("");
        transferMethodCombo.setSelectedIndex(0);
        transferNotesArea.setText("");
        
        // Clear auto-match data
        matchedLostItemId = null;
        if (matchesListModel != null) {
            matchesListModel.clear();
        }
        if (matchesPanel != null) {
            matchesPanel.setVisible(false);
        }
    }
    
    private void selectItemForTransfer(Item item) {
        for (int i = 0; i < transferItemCombo.getItemCount(); i++) {
            ItemOption opt = transferItemCombo.getItemAt(i);
            if (opt.item != null && opt.item.getMongoId() != null &&
                opt.item.getMongoId().equals(item.getMongoId())) {
                transferItemCombo.setSelectedIndex(i);
                break;
            }
        }
    }
    
    /**
     * Find potential matches for the selected found item.
     * Searches LOST items across ALL organizations to find matches.
     */
    private void findMatchesForSelectedItem() {
        ItemOption selectedItem = (ItemOption) transferItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item first.");
            return;
        }
        
        SwingWorker<List<PotentialMatch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PotentialMatch> doInBackground() {
                // Get all LOST items from all organizations (cross-org matching)
                List<Item> allLostItems = itemDAO.findAll().stream()
                    .filter(i -> i.getType() == Item.ItemType.LOST)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN || 
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM)
                    .collect(Collectors.toList());
                
                return itemMatcher.findMatches(selectedItem.item, allLostItems);
            }
            
            @Override
            protected void done() {
                try {
                    List<PotentialMatch> matches = get();
                    displayMatches(matches);
                } catch (Exception e) {
                    showError("Error finding matches: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Display the potential matches in the matches panel.
     */
    private void displayMatches(List<PotentialMatch> matches) {
        matchesListModel.clear();
        matchedLostItemId = null;  // Clear any previous selection
        
        if (matches.isEmpty()) {
            matchesPanel.setVisible(false);
            JOptionPane.showMessageDialog(this,
                "No potential matches found for lost item reports.\n" +
                "You can still create the transfer manually.",
                "No Matches",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (PotentialMatch match : matches) {
                matchesListModel.addElement(match);
            }
            matchesPanel.setVisible(true);
            
            // Revalidate to show the panel
            matchesPanel.getParent().revalidate();
            matchesPanel.getParent().repaint();
        }
    }
    
    /**
     * Handle when a match is selected from the list.
     * Auto-fills the student information from the lost item reporter.
     */
    private void onMatchSelected() {
        PotentialMatch selectedMatch = matchesList.getSelectedValue();
        if (selectedMatch == null) return;
        
        Item lostItem = selectedMatch.getItem();
        
        // Store the matched lost item ID for auto-closure
        matchedLostItemId = lostItem.getMongoId();
        
        // Get the student (reporter) information from the lost item
        User reporter = lostItem.getReportedBy();
        if (reporter != null) {
            studentNameField.setText(reporter.getFullName());
            studentEmailField.setText(reporter.getEmail());
            
            // Try to find the student's university/enterprise
            if (reporter.getEnterpriseId() != null) {
                // Select the destination enterprise
                for (int i = 0; i < destEnterpriseCombo.getItemCount(); i++) {
                    EnterpriseOption opt = destEnterpriseCombo.getItemAt(i);
                    if (opt.enterprise != null && 
                        opt.enterprise.getEnterpriseId().equals(reporter.getEnterpriseId())) {
                        destEnterpriseCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            // Try to select the reporter's organization
            if (reporter.getOrganizationId() != null) {
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < destOrgCombo.getItemCount(); i++) {
                        OrganizationOption opt = destOrgCombo.getItemAt(i);
                        if (opt.organization != null && 
                            opt.organization.getOrganizationId().equals(reporter.getOrganizationId())) {
                            destOrgCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                });
            }
        }
        
        // Set default pickup location
        pickupLocationField.setText("Campus Lost & Found Office");
        
        // Add match info to notes
        int confidence = (int) (selectedMatch.getScore() * 100);
        transferNotesArea.setText("Auto-matched with " + confidence + 
            "% confidence.\nLost item: " + lostItem.getTitle() + 
            " (ID: " + lostItem.getMongoId() + ")");
    }
    
    private void refreshAll() {
        loadDashboardData();
        workQueuePanel.loadRequestsForRole("CAMPUS_COORDINATOR");
        loadTransferData();
        refreshInventory();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openSearchDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Search Items",
            true
        );
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);
        
        EnterpriseItemSearchPanel searchPanel = new EnterpriseItemSearchPanel(currentUser);
        searchPanel.setOnItemDoubleClicked(item -> {
            // Could open item details or allow actions
            dialog.dispose();
        });
        
        dialog.add(searchPanel);
        dialog.setVisible(true);
    }
    
    private void runAutoMatching() {
        JOptionPane.showMessageDialog(this,
            "Auto-matching will scan all found items and notify students with potential matches.\n" +
            "This feature runs automatically but can also be triggered manually.",
            "Auto-Matching",
            JOptionPane.INFORMATION_MESSAGE);
        
        // Could implement batch matching here
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== INNER CLASSES ====================
    
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
    
    private static class EnterpriseOption {
        Enterprise enterprise;
        String displayName;
        
        EnterpriseOption(Enterprise enterprise, String displayName) {
            this.enterprise = enterprise;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private static class OrganizationOption {
        Organization organization;
        String displayName;
        
        OrganizationOption(Organization organization, String displayName) {
            this.organization = organization;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
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
    
    /**
     * Cell renderer for the matches list that displays match info nicely.
     */
    private class MatchListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(EMOJI_FONT);
            
            if (value instanceof PotentialMatch match) {
                Item lostItem = match.getItem();
                int confidence = (int) (match.getScore() * 100);
                
                String reporterName = "Unknown";
                String reporterOrg = "";
                if (lostItem.getReportedBy() != null) {
                    reporterName = lostItem.getReportedBy().getFullName();
                    // Try to get organization name
                    if (lostItem.getReportedBy().getOrganizationId() != null) {
                        for (Organization org : organizationDAO.findAll()) {
                            if (org.getOrganizationId().equals(lostItem.getReportedBy().getOrganizationId())) {
                                reporterOrg = " @ " + org.getName();
                                break;
                            }
                        }
                    }
                }
                
                String confidenceEmoji = confidence >= 80 ? "✅" : (confidence >= 50 ? "🟡" : "🟠");
                setText(confidenceEmoji + " " + confidence + "% - " + lostItem.getTitle() + 
                       " (" + reporterName + reporterOrg + ")");
                
                // Color based on confidence
                if (!isSelected) {
                    if (confidence >= 80) {
                        setBackground(new Color(212, 237, 218));
                    } else if (confidence >= 50) {
                        setBackground(new Color(255, 243, 205));
                    } else {
                        setBackground(Color.WHITE);
                    }
                }
            }
            
            return this;
        }
    }
}
