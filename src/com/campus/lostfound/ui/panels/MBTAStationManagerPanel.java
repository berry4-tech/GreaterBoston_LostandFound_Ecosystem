package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.components.*;
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
 * Main panel for MBTA Station Manager role.
 * 
 * Features:
 * - Dashboard with transit-specific metrics
 * - Bulk item intake for high-volume processing (20+ items/day)
 * - Auto-matching with university student reports
 * - "Send to University" cross-enterprise workflow
 * - Station inventory management
 * - Theft pattern reporting to police
 * 
 * @author Developer 2 - UI Panels
 */
public class MBTAStationManagerPanel extends JPanel {
    
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
    private JLabel todayIntakeLabel;
    private JLabel pendingMatchesLabel;
    private JLabel pendingTransfersLabel;
    private JLabel totalInventoryLabel;
    private JPanel alertsPanel;
    
    // Work Queue tab
    private WorkQueueTablePanel workQueuePanel;
    
    // Bulk Intake tab
    private JTable bulkIntakeTable;
    private DefaultTableModel bulkIntakeModel;
    private JComboBox<String> transitTypeCombo;
    private JComboBox<String> routeCombo;
    private JTextField stationField;
    private JButton addRowButton;
    private JButton registerAllButton;
    private int bulkRowCount = 0;
    
    // Auto-Match tab
    private ItemMatchResultsPanel matchResultsPanel;
    private JComboBox<ItemOption> matchSourceItemCombo;
    private JButton runMatchButton;
    private JPanel matchSummaryPanel;
    
    // Send to University tab
    private JComboBox<ItemOption> transferItemCombo;
    
    // Airport Emergency tab
    private JComboBox<ItemOption> emergencyItemCombo;
    private JTextField emergencyTravelerNameField;
    private JTextField emergencyTravelerPhoneField;
    private JTextField emergencyTravelerEmailField;
    private JTextField emergencyFlightNumberField;
    private JTextField emergencyDepartureTimeField;
    private JComboBox<String> emergencyAirlineCombo;
    private JComboBox<String> emergencyTerminalCombo;
    private JTextField emergencyGateField;
    private JTextField emergencyDestinationCityField;
    private JComboBox<String> emergencyTransitLineCombo;
    private JTextField emergencyFoundLocationField;
    private JComboBox<String> emergencyCourierCombo;
    private JCheckBox emergencyPoliceEscortCheckBox;
    private JCheckBox emergencyGateHoldCheckBox;
    private JTextArea emergencyNotesArea;
    private JComboBox<EnterpriseOption> destUniversityCombo;
    private JComboBox<OrganizationOption> destOrgCombo;
    private JTextField studentNameField;
    private JTextField studentEmailField;
    private JTextField studentIdField;
    private JTextField pickupLocationField;
    private JTextArea transferNotesArea;
    private String matchedLostItemId;  // Store matched lost item ID for auto-closure
    
    // Inventory tab
    private JPanel inventoryListPanel;
    private JScrollPane inventoryScrollPane;
    private JComboBox<String> inventoryFilterCombo;
    private JComboBox<String> inventoryStatusCombo;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Color PRIMARY_COLOR = new Color(0, 102, 153);      // MBTA Blue
    private static final Color MBTA_ORANGE = new Color(237, 139, 0);        // Orange Line
    private static final Color MBTA_RED = new Color(218, 41, 28);           // Red Line
    private static final Color MBTA_GREEN = new Color(0, 132, 61);          // Green Line
    private static final Color MBTA_BLUE = new Color(0, 61, 165);           // Blue Line
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 32);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);
    private static final Font EMOJI_FONT_SMALL = UIConstants.getEmojiFont(Font.PLAIN, 12);
    
    // Transit types and routes
    private static final String[] TRANSIT_TYPES = {"Subway", "Bus", "Commuter Rail", "Ferry", "Station Platform"};
    private static final Map<String, String[]> ROUTES_BY_TYPE = new HashMap<>();
    
    static {
        ROUTES_BY_TYPE.put("Subway", new String[]{"Red Line", "Orange Line", "Blue Line", "Green Line B", "Green Line C", "Green Line D", "Green Line E"});
        ROUTES_BY_TYPE.put("Bus", new String[]{"Bus 1", "Bus 39", "Bus 66", "Bus 77", "Bus 111", "SL1", "SL2", "SL4", "SL5"});
        ROUTES_BY_TYPE.put("Commuter Rail", new String[]{"Framingham/Worcester", "Fitchburg", "Haverhill", "Lowell", "Newburyport/Rockport", "Providence/Stoughton", "Franklin/Foxboro", "Middleborough/Lakeville"});
        ROUTES_BY_TYPE.put("Ferry", new String[]{"F1 Hingham/Hull", "F2 Charlestown", "F4 East Boston"});
        ROUTES_BY_TYPE.put("Station Platform", new String[]{"N/A - Station Area"});
    }
    
    /**
     * Create a new MBTAStationManagerPanel.
     * 
     * @param currentUser The logged-in MBTA station manager
     */
    public MBTAStationManagerPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.itemMatcher = new ItemMatcher();
        
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
        tabbedPane.addTab("📊 Dashboard", createDashboardTab());
        tabbedPane.addTab("📋 Work Queue", createWorkQueueTab());
        tabbedPane.addTab("📦 Bulk Intake", createBulkIntakeTab());
        tabbedPane.addTab("🔍 Auto-Match", createAutoMatchTab());
        tabbedPane.addTab("🎓 Send to University", createSendToUniversityTab());
        tabbedPane.addTab("🚨 Airport Emergency", createAirportEmergencyTab());
        tabbedPane.addTab("📋 Inventory", createInventoryTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> workQueuePanel.loadRequestsForRole("STATION_MANAGER");
                case 3 -> loadMatchSourceItems();
                case 4 -> loadTransferData();
                case 5 -> loadEmergencyTabData();
                case 6 -> refreshInventory();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Welcome
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        welcomeLabel = new JLabel("🚇 MBTA Station Manager Dashboard");
        welcomeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(welcomeLabel);
        
        JLabel subtitleLabel = new JLabel("👤 " + currentUser.getFullName() + " • " + 
            getOrganizationName());
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 220, 240));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("🔄 Refresh All");
        refreshBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(MBTA_BLUE);
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
        
        // Today's Intake card
        JPanel intakeCard = createStatCard("📦", "Today's Intake", "0", MBTA_ORANGE);
        todayIntakeLabel = (JLabel) ((JPanel) intakeCard.getComponent(1)).getComponent(0);
        statsRow.add(intakeCard);
        
        // Pending Matches card
        JPanel matchesCard = createStatCard("🔍", "Potential Matches", "0", MBTA_GREEN);
        pendingMatchesLabel = (JLabel) ((JPanel) matchesCard.getComponent(1)).getComponent(0);
        statsRow.add(matchesCard);
        
        // Pending Transfers card
        JPanel transfersCard = createStatCard("🎓", "Pending Transfers", "0", MBTA_BLUE);
        pendingTransfersLabel = (JLabel) ((JPanel) transfersCard.getComponent(1)).getComponent(0);
        statsRow.add(transfersCard);
        
        // Total Inventory card
        JPanel inventoryCard = createStatCard("📋", "Station Inventory", "0", PRIMARY_COLOR);
        totalInventoryLabel = (JLabel) ((JPanel) inventoryCard.getComponent(1)).getComponent(0);
        statsRow.add(inventoryCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - split into alerts and quick actions
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = new JPanel(new BorderLayout());
        alertsContainer.setOpaque(false);
        alertsContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "🚨 Alerts & Notifications",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
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
        actionsContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "⚡ Quick Actions",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        JPanel actionsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        actionsPanel.setBackground(Color.WHITE);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        actionsPanel.add(createQuickActionButton("📦 Bulk Intake", () -> tabbedPane.setSelectedIndex(2)));
        actionsPanel.add(createQuickActionButton("🔍 Run Auto-Match", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("🎓 Send to University", () -> tabbedPane.setSelectedIndex(4)));
        actionsPanel.add(createQuickActionButton("🚨 Airport Emergency", () -> { tabbedPane.setSelectedIndex(5); loadEmergencyTabData(); }));
        actionsPanel.add(createQuickActionButton("📋 Process Queue", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("🚔 Report to Police", this::openReportToPoliceDialog));
        
        actionsContainer.add(actionsPanel, BorderLayout.CENTER);
        contentPanel.add(actionsContainer);
        
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
    
    // ==================== TAB 2: WORK QUEUE ====================
    
    private JPanel createWorkQueueTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        
        JLabel titleLabel = new JLabel("📋 Transfer Requests & Claims Work Queue");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Review and process incoming transfer requests and item claims");
        helpLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(helpLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Work queue panel
        workQueuePanel = new WorkQueueTablePanel(currentUser);
        workQueuePanel.setOnRequestDoubleClicked(this::handleWorkRequest);
        
        panel.add(workQueuePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 3: BULK INTAKE ====================
    
    private JPanel createBulkIntakeTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header with transit context
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("📦 Bulk Item Intake");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel("Register multiple found items efficiently (designed for 20+ items/day)");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        // Transit context selection
        JPanel contextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        contextPanel.setOpaque(false);
        contextPanel.setBorder(BorderFactory.createTitledBorder("Transit Context"));
        
        contextPanel.add(new JLabel("Type:"));
        transitTypeCombo = new JComboBox<>(TRANSIT_TYPES);
        transitTypeCombo.setPreferredSize(new Dimension(150, 28));
        transitTypeCombo.addActionListener(e -> updateRouteCombo());
        contextPanel.add(transitTypeCombo);
        
        contextPanel.add(new JLabel("Route:"));
        routeCombo = new JComboBox<>();
        routeCombo.setPreferredSize(new Dimension(200, 28));
        updateRouteCombo();
        contextPanel.add(routeCombo);
        
        contextPanel.add(new JLabel("Station:"));
        stationField = new JTextField(15);
        stationField.setText(getDefaultStationName());
        contextPanel.add(stationField);
        
        headerPanel.add(contextPanel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Bulk intake table
        String[] columns = {"#", "Category", "Description", "Color", "Brand", "Found Location", "Special Notes"};
        bulkIntakeModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Row number not editable
            }
        };
        
        bulkIntakeTable = new JTable(bulkIntakeModel);
        bulkIntakeTable.setRowHeight(35);
        bulkIntakeTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bulkIntakeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bulkIntakeTable.getTableHeader().setBackground(PRIMARY_COLOR);
        bulkIntakeTable.getTableHeader().setForeground(Color.BLACK);
        
        // Set column widths
        TableColumnModel columnModel = bulkIntakeTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(0).setMaxWidth(50);
        columnModel.getColumn(1).setPreferredWidth(150);
        columnModel.getColumn(2).setPreferredWidth(250);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(150);
        columnModel.getColumn(6).setPreferredWidth(150);
        
        // Category dropdown in table
        JComboBox<String> categoryCombo = new JComboBox<>();
        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            categoryCombo.addItem(cat.getEmoji() + " " + cat.getDisplayName());
        }
        columnModel.getColumn(1).setCellEditor(new DefaultCellEditor(categoryCombo));
        
        // Add initial rows
        for (int i = 0; i < 5; i++) {
            addBulkIntakeRow();
        }
        
        JScrollPane tableScroll = new JScrollPane(bulkIntakeTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        
        panel.add(tableScroll, BorderLayout.CENTER);
        
        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);
        
        addRowButton = new JButton("➕ Add Row");
        addRowButton.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        addRowButton.addActionListener(e -> addBulkIntakeRow());
        leftButtons.add(addRowButton);
        
        JButton addFiveRowsBtn = new JButton("➕➕ Add 5 Rows");
        addFiveRowsBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        addFiveRowsBtn.addActionListener(e -> {
            for (int i = 0; i < 5; i++) addBulkIntakeRow();
        });
        leftButtons.add(addFiveRowsBtn);
        
        JButton removeRowBtn = new JButton("➖ Remove Selected");
        removeRowBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        removeRowBtn.addActionListener(e -> removeSelectedBulkRow());
        leftButtons.add(removeRowBtn);
        
        JButton clearAllBtn = new JButton("🗑️ Clear All");
        clearAllBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        clearAllBtn.addActionListener(e -> clearBulkIntakeTable());
        leftButtons.add(clearAllBtn);
        
        bottomPanel.add(leftButtons, BorderLayout.WEST);
        
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);
        
        registerAllButton = new JButton("📦 Register All Items");
        registerAllButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        registerAllButton.setBackground(SUCCESS_COLOR);
        registerAllButton.setForeground(Color.WHITE);
        registerAllButton.setFocusPainted(false);
        registerAllButton.setBorderPainted(false);
        registerAllButton.setPreferredSize(new Dimension(180, 40));
        registerAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerAllButton.addActionListener(e -> registerBulkItems());
        rightButtons.add(registerAllButton);
        
        JButton registerAndMatchBtn = new JButton("📦🔍 Register & Auto-Match");
        registerAndMatchBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        registerAndMatchBtn.setBackground(MBTA_BLUE);
        registerAndMatchBtn.setForeground(Color.WHITE);
        registerAndMatchBtn.setFocusPainted(false);
        registerAndMatchBtn.setBorderPainted(false);
        registerAndMatchBtn.setPreferredSize(new Dimension(200, 40));
        registerAndMatchBtn.addActionListener(e -> registerBulkItemsAndMatch());
        rightButtons.add(registerAndMatchBtn);
        
        bottomPanel.add(rightButtons, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateRouteCombo() {
        String type = (String) transitTypeCombo.getSelectedItem();
        routeCombo.removeAllItems();
        if (type != null && ROUTES_BY_TYPE.containsKey(type)) {
            for (String route : ROUTES_BY_TYPE.get(type)) {
                routeCombo.addItem(route);
            }
        }
    }
    
    private void addBulkIntakeRow() {
        bulkRowCount++;
        bulkIntakeModel.addRow(new Object[]{bulkRowCount, "", "", "", "", "", ""});
    }
    
    private void removeSelectedBulkRow() {
        int row = bulkIntakeTable.getSelectedRow();
        if (row >= 0) {
            bulkIntakeModel.removeRow(row);
            renumberBulkRows();
        }
    }
    
    private void renumberBulkRows() {
        for (int i = 0; i < bulkIntakeModel.getRowCount(); i++) {
            bulkIntakeModel.setValueAt(i + 1, i, 0);
        }
        bulkRowCount = bulkIntakeModel.getRowCount();
    }
    
    private void clearBulkIntakeTable() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear all rows? This cannot be undone.",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            bulkIntakeModel.setRowCount(0);
            bulkRowCount = 0;
            for (int i = 0; i < 5; i++) addBulkIntakeRow();
        }
    }
    
    // ==================== TAB 4: AUTO-MATCH ====================
    
    private JPanel createAutoMatchTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("🔍 Auto-Match Found Items with Lost Reports");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel("Match transit found items with university student lost item reports");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        // Source item selection
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sourcePanel.setOpaque(false);
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Select Found Item to Match"));
        
        sourcePanel.add(new JLabel("Found Item:"));
        matchSourceItemCombo = new JComboBox<>();
        matchSourceItemCombo.setPreferredSize(new Dimension(400, 28));
        sourcePanel.add(matchSourceItemCombo);
        
        runMatchButton = new JButton("🔍 Find Matches");
        runMatchButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        runMatchButton.setBackground(MBTA_GREEN);
        runMatchButton.setForeground(Color.WHITE);
        runMatchButton.setFocusPainted(false);
        runMatchButton.setBorderPainted(false);
        runMatchButton.addActionListener(e -> runAutoMatch());
        sourcePanel.add(runMatchButton);
        
        JButton runAllMatchesBtn = new JButton("🔍 Match All Station Items");
        runAllMatchesBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        runAllMatchesBtn.addActionListener(e -> runBatchAutoMatch());
        sourcePanel.add(runAllMatchesBtn);
        
        headerPanel.add(sourcePanel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Match results panel
        matchResultsPanel = new ItemMatchResultsPanel(currentUser);
        matchResultsPanel.setOnClaimRequested((sourceItem, match) -> {
            // Open send to university tab with pre-filled data
            prefillSendToUniversity(sourceItem, match);
            tabbedPane.setSelectedIndex(4);
        });
        
        panel.add(matchResultsPanel, BorderLayout.CENTER);
        
        // Summary panel at bottom
        matchSummaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        matchSummaryPanel.setOpaque(false);
        matchSummaryPanel.setBorder(BorderFactory.createTitledBorder("Match Summary"));
        matchSummaryPanel.add(new JLabel("Run auto-match to see summary"));
        
        panel.add(matchSummaryPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // ==================== TAB 5: SEND TO UNIVERSITY ====================
    
    private JPanel createSendToUniversityTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(800, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("🎓 Send Item to University");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Create a transit-to-university transfer request for matched items");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Item selection section
        formContainer.add(createSectionLabel("Select Item"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Found Item *"));
        transferItemCombo = new JComboBox<>();
        transferItemCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        transferItemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(transferItemCombo);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Destination section
        formContainer.add(createSectionLabel("Destination University"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel destRow = new JPanel(new GridLayout(1, 2, 15, 0));
        destRow.setOpaque(false);
        destRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        destRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel destEntPanel = createFieldPanel("University *");
        destUniversityCombo = new JComboBox<>();
        destUniversityCombo.addActionListener(e -> updateDestinationOrganizations());
        destEntPanel.add(destUniversityCombo);
        destRow.add(destEntPanel);
        
        JPanel destOrgPanel = createFieldPanel("Campus/Department *");
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
        
        JPanel pickupPanel = createFieldPanel("Campus Pickup Location *");
        pickupLocationField = new JTextField();
        pickupLocationField.setText("Campus Lost & Found Office");
        pickupPanel.add(pickupLocationField);
        studentRow2.add(pickupPanel);
        
        formContainer.add(studentRow2);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Transfer notes
        formContainer.add(createSectionLabel("Transfer Details"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Notes / Special Instructions"));
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
        
        JButton submitTransferButton = new JButton("🎓 Create Transfer Request");
        submitTransferButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        submitTransferButton.setBackground(MBTA_BLUE);
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
    
    // ==================== TAB 6: AIRPORT EMERGENCY ====================
    
    private JPanel createAirportEmergencyTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Scrollable form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));
        
        // Header with emergency styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DANGER_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel("\ud83d\udea8 MBTA to Airport Emergency Transfer");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel("For time-critical items (passports, IDs) when traveler is at airport gate");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(255, 220, 220));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        formContainer.add(headerPanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SECTION 1: Item Selection =====
        formContainer.add(createSectionLabel("\ud83d\udce6 Item Information"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Select Found Item (Passport/Documents) *"));
        emergencyItemCombo = new JComboBox<>();
        emergencyItemCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        emergencyItemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(emergencyItemCombo);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Transit context row
        JPanel transitRow = new JPanel(new GridLayout(1, 2, 15, 0));
        transitRow.setOpaque(false);
        transitRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        transitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel linePanel = createFieldPanel("Transit Line *");
        emergencyTransitLineCombo = new JComboBox<>(new String[]{
            "Blue Line", "Silver Line SL1", "Silver Line SL2", "Red Line", "Orange Line", "Green Line"
        });
        linePanel.add(emergencyTransitLineCombo);
        transitRow.add(linePanel);
        
        JPanel foundLocPanel = createFieldPanel("Found Location");
        emergencyFoundLocationField = new JTextField();
        emergencyFoundLocationField.setText("Platform / Train Car");
        foundLocPanel.add(emergencyFoundLocationField);
        transitRow.add(foundLocPanel);
        
        formContainer.add(transitRow);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SECTION 2: Traveler Information =====
        formContainer.add(createSectionLabel("\ud83d\udc64 Traveler Information"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel travelerRow1 = new JPanel(new GridLayout(1, 2, 15, 0));
        travelerRow1.setOpaque(false);
        travelerRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        travelerRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel namePanel = createFieldPanel("Traveler Name *");
        emergencyTravelerNameField = new JTextField();
        namePanel.add(emergencyTravelerNameField);
        travelerRow1.add(namePanel);
        
        JPanel phonePanel = createFieldPanel("Phone Number *");
        emergencyTravelerPhoneField = new JTextField();
        phonePanel.add(emergencyTravelerPhoneField);
        travelerRow1.add(phonePanel);
        
        formContainer.add(travelerRow1);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Traveler Email"));
        emergencyTravelerEmailField = new JTextField();
        emergencyTravelerEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        emergencyTravelerEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(emergencyTravelerEmailField);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SECTION 3: Flight Information =====
        formContainer.add(createSectionLabel("\u2708\ufe0f Flight Information"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel flightRow1 = new JPanel(new GridLayout(1, 3, 15, 0));
        flightRow1.setOpaque(false);
        flightRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        flightRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel airlinePanel = createFieldPanel("Airline *");
        emergencyAirlineCombo = new JComboBox<>(new String[]{
            "Delta", "JetBlue", "American Airlines", "United", "Southwest", 
            "Spirit", "Frontier", "Alaska Airlines", "Other"
        });
        airlinePanel.add(emergencyAirlineCombo);
        flightRow1.add(airlinePanel);
        
        JPanel flightNumPanel = createFieldPanel("Flight Number *");
        emergencyFlightNumberField = new JTextField();
        emergencyFlightNumberField.setToolTipText("e.g., DL1234, B6789");
        flightNumPanel.add(emergencyFlightNumberField);
        flightRow1.add(flightNumPanel);
        
        JPanel departurePanel = createFieldPanel("Departure Time *");
        emergencyDepartureTimeField = new JTextField();
        emergencyDepartureTimeField.setToolTipText("e.g., 10:30 AM or 14:45");
        departurePanel.add(emergencyDepartureTimeField);
        flightRow1.add(departurePanel);
        
        formContainer.add(flightRow1);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Destination City"));
        emergencyDestinationCityField = new JTextField();
        emergencyDestinationCityField.setMaximumSize(new Dimension(400, 30));
        emergencyDestinationCityField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(emergencyDestinationCityField);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SECTION 4: Airport Destination =====
        formContainer.add(createSectionLabel("\ud83c\udfeb Logan Airport Destination"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel airportRow = new JPanel(new GridLayout(1, 2, 15, 0));
        airportRow.setOpaque(false);
        airportRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        airportRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel terminalPanel = createFieldPanel("Terminal *");
        emergencyTerminalCombo = new JComboBox<>(new String[]{
            "Terminal A", "Terminal B", "Terminal C", "Terminal E (International)"
        });
        terminalPanel.add(emergencyTerminalCombo);
        airportRow.add(terminalPanel);
        
        JPanel gatePanel = createFieldPanel("Gate Number");
        emergencyGateField = new JTextField();
        emergencyGateField.setToolTipText("e.g., B22, A15");
        gatePanel.add(emergencyGateField);
        airportRow.add(gatePanel);
        
        formContainer.add(airportRow);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SECTION 5: Emergency Coordination =====
        formContainer.add(createSectionLabel("\ud83d\ude91 Emergency Coordination Options"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel coordRow = new JPanel(new GridLayout(1, 2, 15, 0));
        coordRow.setOpaque(false);
        coordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        coordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel courierPanel = createFieldPanel("Courier Method *");
        emergencyCourierCombo = new JComboBox<>(new String[]{
            "MBTA Shuttle", "Taxi/Rideshare", "Police Escort", "Airport Pickup Team"
        });
        courierPanel.add(emergencyCourierCombo);
        coordRow.add(courierPanel);
        
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setOpaque(false);
        
        emergencyPoliceEscortCheckBox = new JCheckBox("\ud83d\ude94 Request Police Escort (fastest)");
        emergencyPoliceEscortCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        emergencyPoliceEscortCheckBox.setOpaque(false);
        checkboxPanel.add(emergencyPoliceEscortCheckBox);
        
        emergencyGateHoldCheckBox = new JCheckBox("\u2708\ufe0f Request Gate Hold from Airline");
        emergencyGateHoldCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        emergencyGateHoldCheckBox.setOpaque(false);
        checkboxPanel.add(emergencyGateHoldCheckBox);
        
        coordRow.add(checkboxPanel);
        
        formContainer.add(coordRow);
        formContainer.add(Box.createVerticalStrut(15));
        
        formContainer.add(createFormLabel("Emergency Notes / Special Instructions"));
        emergencyNotesArea = new JTextArea(3, 30);
        emergencyNotesArea.setLineWrap(true);
        emergencyNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(emergencyNotesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(notesScroll);
        formContainer.add(Box.createVerticalStrut(25));
        
        // ===== SUBMIT BUTTONS =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton submitEmergencyBtn = new JButton("\ud83d\udea8 CREATE EMERGENCY REQUEST");
        submitEmergencyBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 16));
        submitEmergencyBtn.setBackground(DANGER_COLOR);
        submitEmergencyBtn.setForeground(Color.WHITE);
        submitEmergencyBtn.setFocusPainted(false);
        submitEmergencyBtn.setBorderPainted(false);
        submitEmergencyBtn.setPreferredSize(new Dimension(300, 50));
        submitEmergencyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitEmergencyBtn.addActionListener(e -> submitAirportEmergencyRequest());
        buttonPanel.add(submitEmergencyBtn);
        
        JButton clearEmergencyBtn = new JButton("Clear Form");
        clearEmergencyBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        clearEmergencyBtn.setPreferredSize(new Dimension(120, 50));
        clearEmergencyBtn.addActionListener(e -> clearEmergencyForm());
        buttonPanel.add(clearEmergencyBtn);
        
        formContainer.add(buttonPanel);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Info box at bottom
        JPanel infoBox = new JPanel(new BorderLayout());
        infoBox.setBackground(new Color(255, 243, 205));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel infoLabel = new JLabel("<html><b>\u26a0\ufe0f Important:</b> This creates an URGENT priority request that will immediately " +
            "alert Airport Lost & Found staff. Only use for genuine time-critical situations " +
            "(flight departing within 2-3 hours).</html>");
        infoLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        infoBox.add(infoLabel, BorderLayout.CENTER);
        
        formContainer.add(infoBox);
        
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
    
    // ==================== TAB 7: INVENTORY ====================
    
    private JPanel createInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with filters
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("📋 Station Inventory");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
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
            "All Statuses", "Open", "Pending Transfer", "Matched"
        });
        inventoryStatusCombo.setPreferredSize(new Dimension(140, 28));
        inventoryStatusCombo.addActionListener(e -> refreshInventory());
        filterPanel.add(inventoryStatusCombo);
        
        JButton refreshInvBtn = new JButton("🔄 Refresh");
        refreshInvBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshInvBtn.addActionListener(e -> refreshInventory());
        filterPanel.add(refreshInvBtn);
        
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
        label.setForeground(PRIMARY_COLOR);
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
        return "MBTA Station Operations";
    }
    
    private String getDefaultStationName() {
        // Try to get from user's organization
        return "Park Street Station";
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                // Get today's intake count
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                
                List<Item> myItems = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                int todayIntake = (int) myItems.stream()
                    .filter(i -> i.getReportedDate().after(today.getTime()))
                    .count();
                
                // Count potential matches (items with matches to university lost items)
                int potentialMatches = 0;
                List<Item> allLostItems = itemDAO.findAll().stream()
                    .filter(i -> i.getType() == Item.ItemType.LOST)
                    .collect(Collectors.toList());
                
                for (Item foundItem : myItems) {
                    if (foundItem.getStatus() == Item.ItemStatus.OPEN) {
                        List<PotentialMatch> matches = itemMatcher.findMatches(foundItem, allLostItems);
                        if (!matches.isEmpty()) {
                            potentialMatches++;
                        }
                    }
                }
                
                // Get pending transfers
                List<WorkRequest> requests = workRequestService.getRequestsForRole(
                    "MBTA_STATION_MANAGER", currentUser.getOrganizationId());
                int pendingTransfers = (int) requests.stream()
                    .filter(r -> r.getRequestType() == WorkRequest.RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER)
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                // Total inventory
                int totalInventory = (int) myItems.stream()
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN || 
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM)
                    .count();
                
                return new int[]{todayIntake, potentialMatches, pendingTransfers, totalInventory};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    todayIntakeLabel.setText(String.valueOf(stats[0]));
                    pendingMatchesLabel.setText(String.valueOf(stats[1]));
                    pendingTransfersLabel.setText(String.valueOf(stats[2]));
                    totalInventoryLabel.setText(String.valueOf(stats[3]));
                    
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
        
        if (stats[1] > 0) {
            alertsPanel.add(createAlertItem("🔍", stats[1] + " items have potential matches with university reports!", MBTA_GREEN));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[2] > 0) {
            alertsPanel.add(createAlertItem("🎓", stats[2] + " transfer request(s) pending approval", MBTA_BLUE));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[3] > 50) {
            alertsPanel.add(createAlertItem("📦", "High inventory (" + stats[3] + " items) - consider running batch transfers", WARNING_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (alertsPanel.getComponentCount() == 0) {
            JLabel noAlerts = new JLabel("✅ No alerts - station operations running smoothly!");
            noAlerts.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
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
        iconLabel.setFont(EMOJI_FONT_MEDIUM);
        panel.add(iconLabel, BorderLayout.WEST);
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadMatchSourceItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    matchSourceItemCombo.removeAllItems();
                    matchSourceItemCombo.addItem(new ItemOption(null, "-- Select a found item --"));
                    for (Item item : items) {
                        matchSourceItemCombo.addItem(new ItemOption(item, 
                            item.getCategory().getEmoji() + " " + item.getTitle()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadTransferData() {
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
                
                // Populate university combo (only Higher Education enterprises)
                destUniversityCombo.removeAllItems();
                destUniversityCombo.addItem(new EnterpriseOption(null, "-- Select university --"));
                for (Enterprise e : enterprises) {
                    if (e.getType() == Enterprise.EnterpriseType.HIGHER_EDUCATION) {
                        destUniversityCombo.addItem(new EnterpriseOption(e, "🎓 " + e.getName()));
                    }
                }
            }
        };
        worker.execute();
    }
    
    private void updateDestinationOrganizations() {
        destOrgCombo.removeAllItems();
        destOrgCombo.addItem(new OrganizationOption(null, "-- Select department --"));
        
        EnterpriseOption selected = (EnterpriseOption) destUniversityCombo.getSelectedItem();
        if (selected != null && selected.enterprise != null) {
            for (Organization org : organizationDAO.findAll()) {
                if (selected.enterprise.getEnterpriseId().equals(org.getEnterpriseId())) {
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
                    case "Pending Transfer" -> item.getStatus() == Item.ItemStatus.PENDING_CLAIM;
                    case "Matched" -> item.getStatus() == Item.ItemStatus.VERIFIED;
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
        
        String locationText = item.getLocation() != null ? item.getLocation().getBuilding().getName() : "Unknown";
        JLabel detailsLabel = new JLabel(item.getCategory().getDisplayName() + " • 📍 " + locationText);
        detailsLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        detailsLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(detailsLabel);
        
        JLabel dateLabel = new JLabel("Found: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Status and actions
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(150, 0));
        
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(statusLabel);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonRow.setOpaque(false);
        
        JButton matchBtn = new JButton("🔍");
        matchBtn.setToolTipText("Find Matches");
        matchBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 10));
        matchBtn.addActionListener(e -> {
            selectItemForMatch(item);
            tabbedPane.setSelectedIndex(3);
        });
        buttonRow.add(matchBtn);
        
        JButton transferBtn = new JButton("🎓");
        transferBtn.setToolTipText("Send to University");
        transferBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 10));
        transferBtn.addActionListener(e -> {
            selectItemForTransfer(item);
            tabbedPane.setSelectedIndex(4);
        });
        buttonRow.add(transferBtn);
        
        rightPanel.add(buttonRow);
        
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
            workQueuePanel.loadRequestsForRole("STATION_MANAGER");
            loadDashboardData();
        });
        dialog.setVisible(true);
    }
    
    private void registerBulkItems() {
        List<Object[]> validRows = getValidBulkRows();
        
        if (validRows.isEmpty()) {
            showError("No valid items to register. Please fill in at least Category and Description.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Register " + validRows.size() + " item(s)?",
            "Confirm Bulk Registration",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        registerAllButton.setEnabled(false);
        registerAllButton.setText("Registering...");
        
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                int successCount = 0;
                String transitType = (String) transitTypeCombo.getSelectedItem();
                String route = (String) routeCombo.getSelectedItem();
                String station = stationField.getText().trim();
                
                for (Object[] row : validRows) {
                    try {
                        String categoryStr = (String) row[1];
                        Item.ItemCategory category = parseCategoryFromString(categoryStr);
                        
                        // Create transit location from context
                        Building transitBuilding = new Building(
                            station.isEmpty() ? "MBTA Station" : station,
                            "MBTA-" + route.replaceAll("\\s+", "-").toUpperCase(),
                            Building.BuildingType.TRANSIT_HUB
                        );
                        transitBuilding.setBuildingId((station + route).hashCode());
                        
                        // Get found location from row if provided
                        String foundLocation = (row[5] != null) ? (String) row[5] : "";
                        Location transitLocation = new Location(
                            transitBuilding,
                            transitType + " - " + route,
                            foundLocation.isEmpty() ? "Found on " + transitType : foundLocation
                        );
                        
                        Item item = new Item(
                            generateItemTitle(category, (String) row[2]),
                            (String) row[2],
                            category,
                            Item.ItemType.FOUND,
                            transitLocation,
                            currentUser
                        );
                        
                        item.setStatus(Item.ItemStatus.OPEN);
                        item.setEnterpriseId(currentUser.getEnterpriseId());
                        item.setOrganizationId(currentUser.getOrganizationId());
                        
                        if (row[3] != null && !((String) row[3]).isEmpty()) {
                            item.setPrimaryColor((String) row[3]);
                        }
                        if (row[4] != null && !((String) row[4]).isEmpty()) {
                            item.setBrand((String) row[4]);
                        }
                        
                        // Add transit context to keywords
                        List<String> keywords = item.getKeywords();
                        if (keywords == null) {
                            keywords = new ArrayList<>();
                        }
                        keywords.add(transitType);
                        keywords.add(route);
                        keywords.add(station);
                        item.setKeywords(keywords);
                        
                        String itemId = itemDAO.create(item);
                        if (itemId != null && !itemId.isEmpty()) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return successCount;
            }
            
            @Override
            protected void done() {
                registerAllButton.setEnabled(true);
                registerAllButton.setText("📦 Register All Items");
                
                try {
                    int count = get();
                    JOptionPane.showMessageDialog(MBTAStationManagerPanel.this,
                        count + " item(s) registered successfully!",
                        "Bulk Registration Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    clearBulkIntakeTable();
                    loadDashboardData();
                } catch (Exception e) {
                    showError("Error during registration: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void registerBulkItemsAndMatch() {
        registerBulkItems();
        // After registration, switch to auto-match tab
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1000); // Wait for registration
                tabbedPane.setSelectedIndex(3);
                runBatchAutoMatch();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private List<Object[]> getValidBulkRows() {
        List<Object[]> validRows = new ArrayList<>();
        
        for (int i = 0; i < bulkIntakeModel.getRowCount(); i++) {
            String category = (String) bulkIntakeModel.getValueAt(i, 1);
            String description = (String) bulkIntakeModel.getValueAt(i, 2);
            
            if (category != null && !category.isEmpty() && 
                description != null && !description.isEmpty()) {
                Object[] row = new Object[7];
                for (int j = 0; j < 7; j++) {
                    row[j] = bulkIntakeModel.getValueAt(i, j);
                }
                validRows.add(row);
            }
        }
        
        return validRows;
    }
    
    private Item.ItemCategory parseCategoryFromString(String categoryStr) {
        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            if (categoryStr.contains(cat.getDisplayName())) {
                return cat;
            }
        }
        return Item.ItemCategory.OTHER;
    }
    
    private String generateItemTitle(Item.ItemCategory category, String description) {
        String prefix = category.getDisplayName();
        if (description.length() > 30) {
            return prefix + " - " + description.substring(0, 27) + "...";
        }
        return prefix + " - " + description;
    }
    
    private void runAutoMatch() {
        ItemOption selected = (ItemOption) matchSourceItemCombo.getSelectedItem();
        if (selected == null || selected.item == null) {
            showError("Please select a found item to match.");
            return;
        }
        
        matchResultsPanel.findMatchesFor(selected.item);
    }
    
    private void runBatchAutoMatch() {
        SwingWorker<Map<Item, List<PotentialMatch>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<Item, List<PotentialMatch>> doInBackground() {
                Map<Item, List<PotentialMatch>> allMatches = new LinkedHashMap<>();
                
                List<Item> myFoundItems = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
                
                List<Item> allLostItems = itemDAO.findAll().stream()
                    .filter(i -> i.getType() == Item.ItemType.LOST)
                    .collect(Collectors.toList());
                
                for (Item foundItem : myFoundItems) {
                    List<PotentialMatch> matches = itemMatcher.findMatches(foundItem, allLostItems);
                    if (!matches.isEmpty()) {
                        allMatches.put(foundItem, matches);
                    }
                }
                
                return allMatches;
            }
            
            @Override
            protected void done() {
                try {
                    Map<Item, List<PotentialMatch>> results = get();
                    updateMatchSummary(results);
                    
                    if (results.isEmpty()) {
                        JOptionPane.showMessageDialog(MBTAStationManagerPanel.this,
                            "No matches found for any station items.",
                            "Batch Match Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MBTAStationManagerPanel.this,
                            "Found matches for " + results.size() + " item(s)!\nSee summary below.",
                            "Batch Match Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    showError("Error during batch matching: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void updateMatchSummary(Map<Item, List<PotentialMatch>> results) {
        matchSummaryPanel.removeAll();
        
        if (results.isEmpty()) {
            matchSummaryPanel.add(new JLabel("No matches found in batch scan"));
        } else {
            int totalMatches = results.values().stream().mapToInt(List::size).sum();
            
            matchSummaryPanel.add(new JLabel("📊 " + results.size() + " items with matches"));
            matchSummaryPanel.add(new JLabel("🔗 " + totalMatches + " total potential matches"));
            
            JButton viewAllBtn = new JButton("View All Matches");
            viewAllBtn.addActionListener(e -> showAllMatchesDialog(results));
            matchSummaryPanel.add(viewAllBtn);
        }
        
        matchSummaryPanel.revalidate();
        matchSummaryPanel.repaint();
    }
    
    private void showAllMatchesDialog(Map<Item, List<PotentialMatch>> results) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "All Matches Summary",
            true
        );
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        for (Map.Entry<Item, List<PotentialMatch>> entry : results.entrySet()) {
            Item foundItem = entry.getKey();
            List<PotentialMatch> matches = entry.getValue();
            
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createTitledBorder(
                foundItem.getCategory().getEmoji() + " " + foundItem.getTitle()
            ));
            
            StringBuilder sb = new StringBuilder("<html>");
            for (PotentialMatch match : matches) {
                int confidence = (int) (match.getScore() * 100);
                sb.append("• ").append(match.getItem().getTitle())
                  .append(" (").append(confidence).append("% match)<br>");
            }
            sb.append("</html>");
            
            JLabel matchesLabel = new JLabel(sb.toString());
            itemPanel.add(matchesLabel, BorderLayout.CENTER);
            
            JButton sendBtn = new JButton("Send to University");
            sendBtn.addActionListener(e -> {
                dialog.dispose();
                prefillSendToUniversity(foundItem, matches.get(0));
                tabbedPane.setSelectedIndex(4);
            });
            itemPanel.add(sendBtn, BorderLayout.EAST);
            
            panel.add(itemPanel);
            panel.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scroll = new JScrollPane(panel);
        dialog.add(scroll);
        dialog.setVisible(true);
    }
    
    private void prefillSendToUniversity(Item sourceItem, PotentialMatch match) {
        // Select the found item
        for (int i = 0; i < transferItemCombo.getItemCount(); i++) {
            ItemOption opt = transferItemCombo.getItemAt(i);
            if (opt.item != null && opt.item.getMongoId() != null &&
                opt.item.getMongoId().equals(sourceItem.getMongoId())) {
                transferItemCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Try to get student info from the matched lost item
        Item lostItem = match.getItem();
        if (lostItem.getReportedBy() != null) {
            studentNameField.setText(lostItem.getReportedBy().getFullName());
            studentEmailField.setText(lostItem.getReportedBy().getEmail());
        }
        
        // Store the matched lost item ID for auto-closure when student confirms pickup
        matchedLostItemId = lostItem.getMongoId();
        
        transferNotesArea.setText("Auto-matched with " + (int)(match.getScore() * 100) + 
            "% confidence. Lost item: " + lostItem.getTitle() + 
            " (ID: " + lostItem.getMongoId() + ")");
    }
    
    private void submitTransferRequest() {
        // Validate
        ItemOption selectedItem = (ItemOption) transferItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item to transfer.");
            return;
        }
        
        EnterpriseOption selectedUniversity = (EnterpriseOption) destUniversityCombo.getSelectedItem();
        if (selectedUniversity == null || selectedUniversity.enterprise == null) {
            showError("Please select a destination university.");
            return;
        }
        
        OrganizationOption selectedOrg = (OrganizationOption) destOrgCombo.getSelectedItem();
        if (selectedOrg == null || selectedOrg.organization == null) {
            showError("Please select a destination department.");
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
                    
                    // Generate a studentId if not provided (use email-based ID)
                    String studentIdValue = studentIdField.getText().trim();
                    if (studentIdValue.isEmpty()) {
                        // Use email prefix as fallback ID
                        String email = studentEmailField.getText().trim();
                        studentIdValue = "STU-" + email.split("@")[0].toUpperCase();
                    }
                    
                    TransitToUniversityTransferRequest transfer = new TransitToUniversityTransferRequest(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        item.getMongoId(),
                        item.getTitle(),
                        studentIdValue,
                        studentNameField.getText().trim()
                    );
                    
                    transfer.setItemCategory(item.getCategory().name());
                    transfer.setItemDescription(item.getDescription());
                    
                    // MBTA context
                    transfer.setTransitType((String) transitTypeCombo.getSelectedItem());
                    transfer.setRouteNumber((String) routeCombo.getSelectedItem());
                    transfer.setStationName(stationField.getText().trim());
                    transfer.setMbtaStationManagerId(currentUser.getEmail());
                    transfer.setMbtaStationManagerName(currentUser.getFullName());
                    transfer.setMbtaLocationId(currentUser.getOrganizationId());
                    
                    // University context
                    transfer.setUniversityName(selectedUniversity.enterprise.getName());
                    transfer.setTargetEnterpriseId(selectedUniversity.enterprise.getEnterpriseId());
                    transfer.setTargetOrganizationId(selectedOrg.organization.getOrganizationId());
                    transfer.setCampusPickupLocation(pickupLocationField.getText().trim());
                    
                    // Find a Campus Coordinator for the destination organization
                    String destOrgId = selectedOrg.organization.getOrganizationId();
                    Optional<User> coordinator = userDAO.findAll().stream()
                        .filter(u -> u.getRole() == User.UserRole.CAMPUS_COORDINATOR)
                        .filter(u -> destOrgId.equals(u.getOrganizationId()))
                        .findFirst();
                    
                    if (coordinator.isPresent()) {
                        transfer.setCampusCoordinatorId(coordinator.get().getEmail());
                        transfer.setCampusCoordinatorName(coordinator.get().getFullName());
                    } else {
                        // Fallback: use any campus coordinator from that enterprise
                        String destEntId = selectedUniversity.enterprise.getEnterpriseId();
                        Optional<User> anyCoordinator = userDAO.findAll().stream()
                            .filter(u -> u.getRole() == User.UserRole.CAMPUS_COORDINATOR)
                            .filter(u -> destEntId.equals(u.getEnterpriseId()))
                            .findFirst();
                        
                        if (anyCoordinator.isPresent()) {
                            transfer.setCampusCoordinatorId(anyCoordinator.get().getEmail());
                            transfer.setCampusCoordinatorName(anyCoordinator.get().getFullName());
                        } else {
                            // Last fallback: use a placeholder that will route properly
                            transfer.setCampusCoordinatorId("coordinator@" + 
                                selectedUniversity.enterprise.getName().toLowerCase().replace(" ", "") + ".edu");
                            transfer.setCampusCoordinatorName("Campus Coordinator");
                        }
                    }
                    
                    // Student details
                    transfer.setStudentEmail(studentEmailField.getText().trim());
                    transfer.setStudentIdNumber(studentIdField.getText().trim());
                    
                    // Set matched lost item ID if this came from auto-match
                    if (matchedLostItemId != null && !matchedLostItemId.isEmpty()) {
                        transfer.setLostItemId(matchedLostItemId);
                    }
                    
                    // Transfer notes
                    transfer.setTransferNotes(transferNotesArea.getText().trim());
                    transfer.setRequiresIdVerification(true);
                    
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
                        JOptionPane.showMessageDialog(MBTAStationManagerPanel.this,
                            "Transfer request created successfully!\n" +
                            "The university will be notified to arrange pickup.",
                            "Request Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearTransferForm();
                        loadDashboardData();
                    } else {
                        showError("Failed to create transfer request.");
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
        if (destUniversityCombo.getItemCount() > 0) {
            destUniversityCombo.setSelectedIndex(0);
        }
        studentNameField.setText("");
        studentEmailField.setText("");
        studentIdField.setText("");
        pickupLocationField.setText("Campus Lost & Found Office");
        transferNotesArea.setText("");
        matchedLostItemId = null;  // Clear matched lost item
    }
    
    // ==================== AIRPORT EMERGENCY METHODS ====================
    
    private void loadEmergencyTabData() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                // Get found items that are likely travel documents
                return itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    emergencyItemCombo.removeAllItems();
                    emergencyItemCombo.addItem(new ItemOption(null, "-- Select an item --"));
                    
                    // Prioritize documents/IDs at the top
                    List<Item> priorityItems = items.stream()
                        .filter(i -> i.getCategory() == Item.ItemCategory.IDS_CARDS || 
                                    (i.getTitle() != null && 
                                     (i.getTitle().toLowerCase().contains("passport") ||
                                      i.getTitle().toLowerCase().contains("license") ||
                                      i.getTitle().toLowerCase().contains("document") ||
                                      i.getTitle().toLowerCase().contains("id"))))
                        .collect(Collectors.toList());
                    
                    List<Item> otherItems = items.stream()
                        .filter(i -> !priorityItems.contains(i))
                        .collect(Collectors.toList());
                    
                    // Add priority items first with indicator
                    for (Item item : priorityItems) {
                        emergencyItemCombo.addItem(new ItemOption(item, 
                            "\u2b50 " + item.getCategory().getEmoji() + " " + item.getTitle()));
                    }
                    
                    // Add separator if both lists have items
                    if (!priorityItems.isEmpty() && !otherItems.isEmpty()) {
                        emergencyItemCombo.addItem(new ItemOption(null, "--- Other Items ---"));
                    }
                    
                    // Add other items
                    for (Item item : otherItems) {
                        emergencyItemCombo.addItem(new ItemOption(item, 
                            item.getCategory().getEmoji() + " " + item.getTitle()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void submitAirportEmergencyRequest() {
        // Validate required fields
        ItemOption selectedItem = (ItemOption) emergencyItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item to transfer.");
            return;
        }
        
        if (emergencyTravelerNameField.getText().trim().isEmpty()) {
            showError("Please enter the traveler's name.");
            emergencyTravelerNameField.requestFocus();
            return;
        }
        
        if (emergencyTravelerPhoneField.getText().trim().isEmpty()) {
            showError("Please enter the traveler's phone number.");
            emergencyTravelerPhoneField.requestFocus();
            return;
        }
        
        if (emergencyFlightNumberField.getText().trim().isEmpty()) {
            showError("Please enter the flight number.");
            emergencyFlightNumberField.requestFocus();
            return;
        }
        
        if (emergencyDepartureTimeField.getText().trim().isEmpty()) {
            showError("Please enter the departure time.");
            emergencyDepartureTimeField.requestFocus();
            return;
        }
        
        // Confirm emergency creation
        int confirm = JOptionPane.showConfirmDialog(this,
            "\ud83d\udea8 CREATE EMERGENCY REQUEST?\n\n" +
            "This will immediately alert Airport Lost & Found staff.\n" +
            "Item: " + selectedItem.item.getTitle() + "\n" +
            "Traveler: " + emergencyTravelerNameField.getText().trim() + "\n" +
            "Flight: " + emergencyFlightNumberField.getText().trim() + " at " + 
                emergencyDepartureTimeField.getText().trim() + "\n\n" +
            "Continue?",
            "Confirm Emergency Request",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = selectedItem.item;
                    
                    // Create the emergency request
                    MBTAToAirportEmergencyRequest emergency = new MBTAToAirportEmergencyRequest(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        item.getMongoId(),
                        item.getTitle(),
                        emergencyTravelerNameField.getText().trim(),
                        emergencyFlightNumberField.getText().trim().toUpperCase()
                    );
                    
                    // Item details
                    emergency.setItemDescription(item.getDescription());
                    emergency.setItemCategory(item.getCategory().name());
                    
                    // MBTA context
                    emergency.setMbtaStationId(currentUser.getOrganizationId());
                    emergency.setMbtaStationName(stationField.getText().trim());
                    emergency.setTransitLine((String) emergencyTransitLineCombo.getSelectedItem());
                    emergency.setFoundLocation(emergencyFoundLocationField.getText().trim());
                    emergency.setFoundDateTime(java.time.LocalDateTime.now().toString());
                    emergency.setMbtaIncidentNumber("MBTA-EMG-" + System.currentTimeMillis() % 1000000);
                    
                    // Set requester enterprise/org
                    emergency.setRequesterEnterpriseId(currentUser.getEnterpriseId());
                    emergency.setRequesterOrganizationId(currentUser.getOrganizationId());
                    
                    // Airport destination - find airport enterprise and organization
                    Optional<Enterprise> airportEnterprise = enterpriseDAO.findAll().stream()
                        .filter(e -> e.getType() == Enterprise.EnterpriseType.AIRPORT)
                        .findFirst();
                    
                    if (airportEnterprise.isPresent()) {
                        emergency.setTargetEnterpriseId(airportEnterprise.get().getEnterpriseId());
                        
                        // Find Airport Lost & Found organization
                        Optional<Organization> airportOrg = organizationDAO.findAll().stream()
                            .filter(o -> airportEnterprise.get().getEnterpriseId().equals(o.getEnterpriseId()))
                            .filter(o -> o.getName().toLowerCase().contains("lost") || 
                                        o.getName().toLowerCase().contains("found") ||
                                        o.getName().toLowerCase().contains("l&f"))
                            .findFirst();
                        
                        if (airportOrg.isPresent()) {
                            emergency.setTargetOrganizationId(airportOrg.get().getOrganizationId());
                        } else {
                            // Use first airport org as fallback
                            organizationDAO.findAll().stream()
                                .filter(o -> airportEnterprise.get().getEnterpriseId().equals(o.getEnterpriseId()))
                                .findFirst()
                                .ifPresent(o -> emergency.setTargetOrganizationId(o.getOrganizationId()));
                        }
                        
                        // Find an Airport Specialist to assign
                        Optional<User> specialist = userDAO.findAll().stream()
                            .filter(u -> u.getRole() == User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST)
                            .filter(u -> airportEnterprise.get().getEnterpriseId().equals(u.getEnterpriseId()))
                            .findFirst();
                        
                        if (specialist.isPresent()) {
                            emergency.setAirportSpecialistId(specialist.get().getEmail());
                            emergency.setAirportSpecialistName(specialist.get().getFullName());
                        }
                    }
                    
                    emergency.setAirportTerminal((String) emergencyTerminalCombo.getSelectedItem());
                    emergency.setAirportGate(emergencyGateField.getText().trim());
                    emergency.setAirportContactPhone("617-561-1714"); // Logan Lost & Found
                    
                    // Traveler details
                    emergency.setTravelerName(emergencyTravelerNameField.getText().trim());
                    emergency.setTravelerPhone(emergencyTravelerPhoneField.getText().trim());
                    emergency.setTravelerEmail(emergencyTravelerEmailField.getText().trim());
                    
                    // Flight details
                    emergency.setAirline((String) emergencyAirlineCombo.getSelectedItem());
                    emergency.setFlightDepartureTime(emergencyDepartureTimeField.getText().trim());
                    emergency.setDestinationCity(emergencyDestinationCityField.getText().trim());
                    
                    // Emergency coordination
                    String courierMethod = (String) emergencyCourierCombo.getSelectedItem();
                    emergency.setCourierMethod(courierMethod.toUpperCase().replace(" ", "_").replace("/", "_"));
                    
                    if (emergencyPoliceEscortCheckBox.isSelected()) {
                        emergency.requestPoliceEscort();
                    }
                    
                    if (emergencyGateHoldCheckBox.isSelected()) {
                        emergency.requestGateHold();
                    }
                    
                    // Add any notes
                    String notes = emergencyNotesArea.getText().trim();
                    if (!notes.isEmpty()) {
                        emergency.addDeliveryNote("Initial notes: " + notes);
                    }
                    
                    // Create the request via WorkRequestService
                    return workRequestService.createRequest(emergency);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    String requestId = get();
                    if (requestId != null && !requestId.isEmpty()) {
                        // Show success with emergency reference number
                        String emergencyCode = "EMR-MBTA-LOG-" + requestId.substring(Math.max(0, requestId.length() - 6));
                        
                        JOptionPane.showMessageDialog(MBTAStationManagerPanel.this,
                            "\ud83d\udea8 EMERGENCY REQUEST CREATED!\n\n" +
                            "Emergency Code: " + emergencyCode + "\n\n" +
                            "Airport Lost & Found has been notified.\n" +
                            "Item: " + selectedItem.item.getTitle() + "\n" +
                            "Flight: " + emergencyFlightNumberField.getText().trim() + "\n\n" +
                            "Direct Airport L&F Line: 617-561-1714\n" +
                            "Massport Operations: 617-568-5000",
                            "Emergency Request Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        clearEmergencyForm();
                        loadDashboardData();
                        
                        // Update item status
                        Item item = selectedItem.item;
                        item.setStatus(Item.ItemStatus.PENDING_CLAIM);
                        itemDAO.update(item);
                        
                    } else {
                        showError("Failed to create emergency request. Please try again or call Airport L&F directly.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void clearEmergencyForm() {
        if (emergencyItemCombo.getItemCount() > 0) {
            emergencyItemCombo.setSelectedIndex(0);
        }
        emergencyTravelerNameField.setText("");
        emergencyTravelerPhoneField.setText("");
        emergencyTravelerEmailField.setText("");
        emergencyFlightNumberField.setText("");
        emergencyDepartureTimeField.setText("");
        emergencyDestinationCityField.setText("");
        emergencyGateField.setText("");
        emergencyFoundLocationField.setText("Platform / Train Car");
        emergencyPoliceEscortCheckBox.setSelected(false);
        emergencyGateHoldCheckBox.setSelected(false);
        emergencyNotesArea.setText("");
        
        if (emergencyAirlineCombo.getItemCount() > 0) {
            emergencyAirlineCombo.setSelectedIndex(0);
        }
        if (emergencyTerminalCombo.getItemCount() > 0) {
            emergencyTerminalCombo.setSelectedIndex(0);
        }
        if (emergencyTransitLineCombo.getItemCount() > 0) {
            emergencyTransitLineCombo.setSelectedIndex(0);
        }
        if (emergencyCourierCombo.getItemCount() > 0) {
            emergencyCourierCombo.setSelectedIndex(0);
        }
    }
    
    private void selectItemForMatch(Item item) {
        for (int i = 0; i < matchSourceItemCombo.getItemCount(); i++) {
            ItemOption opt = matchSourceItemCombo.getItemAt(i);
            if (opt.item != null && opt.item.getMongoId() != null &&
                opt.item.getMongoId().equals(item.getMongoId())) {
                matchSourceItemCombo.setSelectedIndex(i);
                runAutoMatch();
                break;
            }
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
    
    private void openReportToPoliceDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "🚔 Report Suspicious Activity to Police",
            true
        );
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        panel.add(new JLabel("Report theft patterns or suspicious activity to Boston Police"));
        panel.add(Box.createVerticalStrut(15));
        
        panel.add(new JLabel("Incident Type:"));
        JComboBox<String> incidentCombo = new JComboBox<>(new String[]{
            "Multiple high-value items from same location",
            "Suspected theft ring activity",
            "Suspicious person behavior",
            "Stolen property suspected",
            "Other"
        });
        incidentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(incidentCombo);
        panel.add(Box.createVerticalStrut(10));
        
        panel.add(new JLabel("Description:"));
        JTextArea descArea = new JTextArea(5, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(descScroll);
        panel.add(Box.createVerticalStrut(10));
        
        panel.add(new JLabel("Location/Station:"));
        JTextField locationField = new JTextField(stationField.getText());
        locationField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(locationField);
        panel.add(Box.createVerticalStrut(20));
        
        JButton submitBtn = new JButton("🚔 Submit Report");
        submitBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        submitBtn.setBackground(DANGER_COLOR);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog,
                "Report submitted to Boston Police Department.\n" +
                "Reference number: BPD-" + System.currentTimeMillis() % 100000,
                "Report Submitted",
                JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        panel.add(submitBtn);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void refreshAll() {
        loadDashboardData();
        workQueuePanel.loadRequestsForRole("STATION_MANAGER");
        loadMatchSourceItems();
        loadTransferData();
        refreshInventory();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
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
}
