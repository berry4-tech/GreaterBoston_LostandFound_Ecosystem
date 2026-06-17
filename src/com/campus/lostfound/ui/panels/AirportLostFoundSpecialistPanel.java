package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.services.EnterpriseItemService;
import com.campus.lostfound.services.EnterpriseItemMatcher;
import com.campus.lostfound.services.AnalyticsService;
import com.campus.lostfound.services.ReportExportService;
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
 * Main panel for Airport Lost & Found Specialist role at Logan Airport.
 * 
 * Features:
 * - Dashboard with airport-specific metrics and flight alerts
 * - Item intake with flight info, terminal, gate, TSA checkpoint origin
 * - Traveler search by name, flight, confirmation number
 * - Delivery coordination for shipping items to travelers
 * - Cross-enterprise transfers with MBTA and universities
 * - Comprehensive reporting and export
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class AirportLostFoundSpecialistPanel extends JPanel {
    
    // Data
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoUserDAO userDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private WorkRequestService workRequestService;
    private ItemMatcher itemMatcher;
    private EnterpriseItemService enterpriseItemService;
    private EnterpriseItemMatcher enterpriseItemMatcher;
    private AnalyticsService analyticsService;
    private ReportExportService reportExportService;
    
    // UI Components
    private JTabbedPane tabbedPane;
    private JLabel welcomeLabel;
    
    // Dashboard components
    private JLabel todayReceivedLabel;
    private JLabel pendingClaimsLabel;
    private JLabel internationalQueueLabel;
    private JLabel emergencyDeliveryLabel;
    private JPanel alertsPanel;
    
    // Item Intake tab
    private JTextField intakeTitleField;
    private JTextArea intakeDescriptionArea;
    private JComboBox<Item.ItemCategory> intakeCategoryCombo;
    private JComboBox<String> intakeTerminalCombo;
    private JTextField intakeGateField;
    private JComboBox<String> intakeAreaCombo;
    private JTextField intakeAirlineField;
    private JTextField intakeFlightNumberField;
    private JTextField intakePassengerNameField;
    private JTextField intakeConfirmationField;
    private JCheckBox intakeTsaCheckpointCheckBox;
    private JCheckBox intakeInternationalCheckBox;
    private JTextField intakeColorField;
    private JTextField intakeBrandField;
    private JTextField intakeValueField;
    private JLabel intakeImagePreviewLabel;
    private List<String> intakeImagePaths;
    
    // Traveler Search tab
    private JTextField searchNameField;
    private JTextField searchFlightField;
    private JTextField searchConfirmationField;
    private JComboBox<String> searchTerminalFilter;
    private JPanel searchResultsPanel;
    private JCheckBox searchCrossEnterpriseCheckBox;
    
    // Delivery Coordination tab
    private JComboBox<ItemOption> deliveryItemCombo;
    private JTextField deliveryRecipientNameField;
    private JTextField deliveryAddressField;
    private JTextField deliveryCityField;
    private JTextField deliveryStateField;
    private JTextField deliveryZipField;
    private JTextField deliveryCountryField;
    private JTextField deliveryPhoneField;
    private JTextField deliveryEmailField;
    private JComboBox<String> deliveryCarrierCombo;
    private JTextField deliveryTrackingField;
    private JCheckBox deliveryEmergencyCheckBox;
    private JTextArea deliveryNotesArea;
    private JPanel pendingDeliveriesPanel;
    
    // Auto-Match tab
    private ItemMatchResultsPanel matchResultsPanel;
    private JComboBox<ItemOption> matchSourceItemCombo;
    private JButton runMatchButton;
    private JPanel matchSummaryPanel;
    private String matchedLostItemId;  // Store matched lost item ID for auto-closure
    
    // Cross-Enterprise Transfer tab
    private JPanel incomingTransfersPanel;
    private JPanel outgoingTransfersPanel;
    private JComboBox<ItemOption> transferItemCombo;
    private JComboBox<EnterpriseOption> transferDestCombo;
    private JComboBox<OrganizationOption> transferOrgCombo;
    private JTextField transferStudentNameField;
    private JTextField transferStudentEmailField;
    private JTextField transferPickupLocationField;
    private JTextArea transferNotesArea;
    
    // Reports tab
    private JPanel reportSummaryPanel;
    private JTable unclaimedItemsTable;
    private DefaultTableModel unclaimedItemsModel;
    
    // Pending Approvals tab
    private JTable pendingApprovalsTable;
    private DefaultTableModel pendingApprovalsTableModel;
    
    // Inventory tab
    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;
    private JPanel tsaIncomingPanel;
    private JLabel inventoryCountLabel;
    private JLabel tsaPendingCountLabel;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    private static final Color PRIMARY_COLOR = new Color(0, 51, 102);       // Logan Airport Blue
    private static final Color SECONDARY_COLOR = new Color(0, 102, 153);    // Lighter Blue
    private static final Color ACCENT_COLOR = new Color(255, 193, 7);       // Gold/Yellow
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 32);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);
    private static final Font EMOJI_FONT_SMALL = UIConstants.getEmojiFont(Font.PLAIN, 12);
    
    // Airport-specific data
    private static final String[] TERMINALS = {"Terminal A", "Terminal B", "Terminal C", "Terminal E"};
    private static final String[] AIRPORT_AREAS = {
        "Baggage Claim", "Security Checkpoint", "Gate Area", "Ticketing/Check-in", 
        "Customs", "Arrivals Hall", "Departures Hall", "Restroom", "Food Court", 
        "Parking Garage", "Shuttle/Bus Area", "Other"
    };
    private static final String[] CARRIERS = {
        "FedEx", "UPS", "USPS Priority", "USPS Express", "DHL", "Airport Courier"
    };
    
    /**
     * Create a new AirportLostFoundSpecialistPanel.
     * 
     * @param currentUser The logged-in airport lost & found specialist
     */
    public AirportLostFoundSpecialistPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.itemMatcher = new ItemMatcher();
        this.enterpriseItemService = new EnterpriseItemService();
        this.enterpriseItemMatcher = new EnterpriseItemMatcher();
        this.analyticsService = new AnalyticsService();
        this.reportExportService = new ReportExportService();
        this.intakeImagePaths = new ArrayList<>();
        
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
        tabbedPane.addTab("✈️ Dashboard", createDashboardTab());
        tabbedPane.addTab("📋 Pending Approvals", createPendingApprovalsTab());
        tabbedPane.addTab("📦 Inventory", createInventoryTab());
        tabbedPane.addTab("📥 Item Intake", createItemIntakeTab());
        tabbedPane.addTab("🔍 Traveler Search", createTravelerSearchTab());
        tabbedPane.addTab("🚚 Delivery", createDeliveryCoordinationTab());
        tabbedPane.addTab("🔎 Auto-Match", createAutoMatchTab());
        tabbedPane.addTab("🔄 Transfers", createCrossEnterpriseTransferTab());
        tabbedPane.addTab("📊 Reports", createReportsTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> loadPendingApprovals();
                case 2 -> loadInventoryData();
                case 5 -> loadDeliveryData();
                case 6 -> loadMatchSourceItems();
                case 7 -> loadTransferData();
                case 8 -> loadReportData();
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
        
        welcomeLabel = new JLabel("✈️ Logan Airport Lost & Found");
        welcomeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(welcomeLabel);
        
        JLabel subtitleLabel = new JLabel("👤 " + currentUser.getFullName() + " • Lost & Found Specialist");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 220, 240));
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
        
        JButton emergencyBtn = new JButton("🚨 Emergency");
        emergencyBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        emergencyBtn.setForeground(Color.WHITE);
        emergencyBtn.setBackground(DANGER_COLOR);
        emergencyBtn.setBorderPainted(false);
        emergencyBtn.setFocusPainted(false);
        emergencyBtn.addActionListener(e -> openEmergencyDialog());
        rightPanel.add(emergencyBtn);
        
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
        
        // Today's Received card
        JPanel receivedCard = createStatCard("📦", "Today's Items", "0", SECONDARY_COLOR);
        todayReceivedLabel = (JLabel) ((JPanel) receivedCard.getComponent(1)).getComponent(0);
        statsRow.add(receivedCard);
        
        // Pending Claims card
        JPanel claimsCard = createStatCard("📋", "Pending Claims", "0", SUCCESS_COLOR);
        pendingClaimsLabel = (JLabel) ((JPanel) claimsCard.getComponent(1)).getComponent(0);
        statsRow.add(claimsCard);
        
        // International Queue card
        JPanel internationalCard = createStatCard("🌍", "International Queue", "0", INFO_COLOR);
        internationalQueueLabel = (JLabel) ((JPanel) internationalCard.getComponent(1)).getComponent(0);
        statsRow.add(internationalCard);
        
        // Emergency Deliveries card
        JPanel emergencyCard = createStatCard("🚨", "Emergency Deliveries", "0", DANGER_COLOR);
        emergencyDeliveryLabel = (JLabel) ((JPanel) emergencyCard.getComponent(1)).getComponent(0);
        statsRow.add(emergencyCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - alerts and quick actions
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = new JPanel(new BorderLayout());
        alertsContainer.setOpaque(false);
        alertsContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "🚨 Flight Alerts & Notifications",
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
        
        actionsPanel.add(createQuickActionButton("📋 Pending Approvals", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("📦 Register Item", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("🔎 Run Auto-Match", () -> tabbedPane.setSelectedIndex(6)));
        actionsPanel.add(createQuickActionButton("🔄 Transfer to Univ.", () -> tabbedPane.setSelectedIndex(7)));
        actionsPanel.add(createQuickActionButton("🚚 Process Delivery", () -> tabbedPane.setSelectedIndex(5)));
        actionsPanel.add(createQuickActionButton("🚔 Contact Massport PD", this::contactPoliceDialog));
        
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
    
    // ==================== TAB 2: PENDING APPROVALS ====================
    
    /**
     * Creates the Pending Approvals tab that shows ALL work requests
     * requiring AIRPORT_LOST_FOUND_SPECIALIST approval.
     * This includes ItemClaimRequest, TransferRequests, etc.
     */
    private JPanel createPendingApprovalsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("📋 Pending Approvals - Airport Lost & Found");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> loadPendingApprovals());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        JLabel subtitleLabel = new JLabel("Item claims and transfers requiring Airport L&F verification before release");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Request ID", "Type", "Summary", "Requester", "Status", "Created", "Priority", "Actions"};
        pendingApprovalsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column
            }
        };
        
        pendingApprovalsTable = new JTable(pendingApprovalsTableModel);
        pendingApprovalsTable.setRowHeight(45);
        pendingApprovalsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingApprovalsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pendingApprovalsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        pendingApprovalsTable.getTableHeader().setBackground(PRIMARY_COLOR);
        pendingApprovalsTable.getTableHeader().setForeground(Color.WHITE);
        
        // Hide request ID column but keep for reference
        pendingApprovalsTable.getColumnModel().getColumn(0).setMinWidth(0);
        pendingApprovalsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        pendingApprovalsTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Column widths
        pendingApprovalsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        pendingApprovalsTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        pendingApprovalsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        pendingApprovalsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        pendingApprovalsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        pendingApprovalsTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        pendingApprovalsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        
        // Priority renderer with color coding
        pendingApprovalsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String priority = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                
                if (!isSelected) {
                    switch (priority) {
                        case "URGENT" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "HIGH" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "NORMAL" -> { setBackground(new Color(207, 226, 255)); setForeground(SECONDARY_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Double-click to view details
        pendingApprovalsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = pendingApprovalsTable.getSelectedRow();
                    if (row >= 0) {
                        viewPendingRequestDetails(row);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(pendingApprovalsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Action buttons panel at bottom
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setOpaque(false);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton approveBtn = new JButton("✅ Approve Selected");
        approveBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        approveBtn.setBackground(SUCCESS_COLOR);
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setBorderPainted(false);
        approveBtn.setFocusPainted(false);
        approveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        approveBtn.addActionListener(e -> approveSelectedPendingRequest());
        actionsPanel.add(approveBtn);
        
        JButton rejectBtn = new JButton("❌ Reject Selected");
        rejectBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        rejectBtn.setBackground(DANGER_COLOR);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setBorderPainted(false);
        rejectBtn.setFocusPainted(false);
        rejectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rejectBtn.addActionListener(e -> rejectSelectedPendingRequest());
        actionsPanel.add(rejectBtn);
        
        JButton viewBtn = new JButton("👁️ View Details");
        viewBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        viewBtn.addActionListener(e -> {
            int row = pendingApprovalsTable.getSelectedRow();
            if (row >= 0) viewPendingRequestDetails(row);
            else showError("Please select a request first.");
        });
        actionsPanel.add(viewBtn);
        
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Load ALL pending work requests that need AIRPORT_LOST_FOUND_SPECIALIST approval.
     * This includes ItemClaimRequest (when item is at airport) and transfer requests.
     */
    private void loadPendingApprovals() {
        SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WorkRequest> doInBackground() {
                // Get ALL requests that need AIRPORT_LOST_FOUND_SPECIALIST approval
                // This includes ItemClaimRequest, AirportToUniversityTransfer, etc.
                return workRequestService.getRequestsForRole(
                    "AIRPORT_LOST_FOUND_SPECIALIST", 
                    currentUser.getOrganizationId()
                ).stream()
                    .filter(r -> r.needsApprovalFromRole("AIRPORT_LOST_FOUND_SPECIALIST"))
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .sorted((a, b) -> {
                        // Sort by priority (URGENT first) then by date
                        int priorityCompare = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
                        if (priorityCompare != 0) return priorityCompare;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<WorkRequest> requests = get();
                    pendingApprovalsTableModel.setRowCount(0);
                    
                    if (requests.isEmpty()) {
                        // Show empty state
                        pendingApprovalsTableModel.addRow(new Object[]{
                            "", "", "✅ No pending requests requiring your approval", "", "", "", "", ""
                        });
                    } else {
                        for (WorkRequest request : requests) {
                            String type = formatRequestTypeForTable(request.getRequestType());
                            String created = request.getCreatedAt() != null ? 
                                DATE_FORMAT.format(java.util.Date.from(
                                    request.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())) 
                                : "Unknown";
                            
                            pendingApprovalsTableModel.addRow(new Object[]{
                                request.getRequestId(),
                                type,
                                request.getRequestSummary(),
                                request.getRequesterName(),
                                request.getStatus().name(),
                                created,
                                request.getPriority() != null ? request.getPriority().name() : "NORMAL",
                                "Review"
                            });
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error loading pending approvals: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private int getPriorityOrder(WorkRequest.RequestPriority priority) {
        if (priority == null) return 0;
        return switch (priority) {
            case URGENT -> 3;
            case HIGH -> 2;
            case NORMAL -> 1;
            case LOW -> 0;
        };
    }
    
    private String formatRequestTypeForTable(WorkRequest.RequestType type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case ITEM_CLAIM -> "📋 Item Claim";
            case CROSS_CAMPUS_TRANSFER -> "🏫 Cross-Campus";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "🚇 Transit Transfer";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "✈️ Airport Transfer";
            case POLICE_EVIDENCE_REQUEST -> "🚔 Police Evidence";
            case MBTA_TO_AIRPORT_EMERGENCY -> "🚨 MBTA Emergency";
            case MULTI_ENTERPRISE_DISPUTE -> "⚖️ Dispute";
        };
    }
    
    private void viewPendingRequestDetails(int row) {
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        WorkRequest request = workRequestService.getRequestById(requestId);
        if (request == null) {
            showError("Request not found.");
            return;
        }
        
        RequestDetailDialog dialog = new RequestDetailDialog(
            SwingUtilities.getWindowAncestor(this),
            request,
            currentUser
        );
        dialog.setOnRequestUpdated(r -> {
            loadPendingApprovals();
            loadDashboardData();
        });
        dialog.setVisible(true);
    }
    
    private void approveSelectedPendingRequest() {
        int row = pendingApprovalsTable.getSelectedRow();
        if (row < 0) {
            showError("Please select a request to approve.");
            return;
        }
        
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        String summary = (String) pendingApprovalsTableModel.getValueAt(row, 2);
        String type = (String) pendingApprovalsTableModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Approve this request?\n\n" +
            "Type: " + type + "\n" +
            "Summary: " + summary + "\n\n" +
            "This confirms Airport L&F verification for this item.",
            "Confirm Approval",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return workRequestService.approveRequest(requestId, currentUser.getEmail());
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                                "✅ Request approved successfully!\n\n" +
                                "The item has been verified by Airport Lost & Found.",
                                "Approved", JOptionPane.INFORMATION_MESSAGE);
                            loadPendingApprovals();
                            loadDashboardData();
                        } else {
                            showError("Failed to approve request. Please try again.");
                        }
                    } catch (Exception e) {
                        showError("Error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void rejectSelectedPendingRequest() {
        int row = pendingApprovalsTable.getSelectedRow();
        if (row < 0) {
            showError("Please select a request to reject.");
            return;
        }
        
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        String reason = JOptionPane.showInputDialog(this,
            "Enter rejection reason:\n(e.g., Item not found, Insufficient proof, etc.)",
            "Reject Request",
            JOptionPane.WARNING_MESSAGE);
        
        if (reason != null && !reason.trim().isEmpty()) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return workRequestService.rejectRequest(requestId, currentUser.getEmail(), reason.trim());
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                                "Request rejected.\nReason: " + reason.trim(),
                                "Rejected", JOptionPane.INFORMATION_MESSAGE);
                            loadPendingApprovals();
                            loadDashboardData();
                        } else {
                            showError("Failed to reject request.");
                        }
                    } catch (Exception e) {
                        showError("Error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
    
    // ==================== TAB 3: INVENTORY ====================
    
    private JPanel createInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header with stats
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📋 Airport Lost & Found Inventory");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setOpaque(false);
        
        inventoryCountLabel = new JLabel("📦 Total Items: 0");
        inventoryCountLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        inventoryCountLabel.setForeground(PRIMARY_COLOR);
        statsPanel.add(inventoryCountLabel);
        
        tsaPendingCountLabel = new JLabel("🔒 TSA Pending: 0");
        tsaPendingCountLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        tsaPendingCountLabel.setForeground(WARNING_COLOR);
        statsPanel.add(tsaPendingCountLabel);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> loadInventoryData());
        statsPanel.add(refreshBtn);
        
        headerPanel.add(statsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Main content - split between TSA incoming and main inventory
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.3);
        
        // Top - TSA Incoming Items
        JPanel tsaContainer = new JPanel(new BorderLayout());
        tsaContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WARNING_COLOR, 2),
            "🔒 Incoming from TSA Checkpoints",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        tsaIncomingPanel = new JPanel();
        tsaIncomingPanel.setLayout(new BoxLayout(tsaIncomingPanel, BoxLayout.Y_AXIS));
        tsaIncomingPanel.setBackground(new Color(255, 253, 240));
        tsaIncomingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane tsaScroll = new JScrollPane(tsaIncomingPanel);
        tsaScroll.setBorder(null);
        tsaScroll.getVerticalScrollBar().setUnitIncrement(16);
        tsaContainer.add(tsaScroll, BorderLayout.CENTER);
        
        splitPane.setTopComponent(tsaContainer);
        
        // Bottom - Main Inventory Table
        JPanel inventoryContainer = new JPanel(new BorderLayout());
        inventoryContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR),
            "📦 Current Inventory",
            TitledBorder.LEFT, TitledBorder.TOP,
            UIConstants.getEmojiFont(Font.BOLD, 14)
        ));
        
        String[] columns = {"ID", "Item", "Category", "Terminal", "Source", "Found Date", "Status", "Actions"};
        inventoryModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column is editable
            }
        };
        
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setRowHeight(40);
        inventoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inventoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        inventoryTable.getTableHeader().setBackground(PRIMARY_COLOR);
        inventoryTable.getTableHeader().setForeground(Color.BLACK);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Column widths
        TableColumnModel tcm = inventoryTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(80);  // ID
        tcm.getColumn(1).setPreferredWidth(200); // Item
        tcm.getColumn(2).setPreferredWidth(100); // Category
        tcm.getColumn(3).setPreferredWidth(100); // Terminal
        tcm.getColumn(4).setPreferredWidth(100); // Source
        tcm.getColumn(5).setPreferredWidth(100); // Date
        tcm.getColumn(6).setPreferredWidth(80);  // Status
        tcm.getColumn(7).setPreferredWidth(100); // Actions
        
        // Hide ID column but keep data
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setWidth(0);
        
        JScrollPane tableScroll = new JScrollPane(inventoryTable);
        inventoryContainer.add(tableScroll, BorderLayout.CENTER);
        
        // Action buttons panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionPanel.setOpaque(false);
        
        JButton viewItemBtn = new JButton("👁️ View Details");
        viewItemBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        viewItemBtn.addActionListener(e -> viewSelectedInventoryItem());
        actionPanel.add(viewItemBtn);
        
        JButton processClaimBtn = new JButton("📋 Process Claim");
        processClaimBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        processClaimBtn.addActionListener(e -> processInventoryClaim());
        actionPanel.add(processClaimBtn);
        
        JButton transferBtn = new JButton("🔄 Transfer to University");
        transferBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        transferBtn.addActionListener(e -> {
            tabbedPane.setSelectedIndex(6); // Go to transfers tab
        });
        actionPanel.add(transferBtn);
        
        JButton shipBtn = new JButton("🚚 Arrange Shipping");
        shipBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        shipBtn.addActionListener(e -> {
            tabbedPane.setSelectedIndex(4); // Go to delivery tab (unchanged)
        });
        actionPanel.add(shipBtn);
        
        inventoryContainer.add(actionPanel, BorderLayout.SOUTH);
        
        splitPane.setBottomComponent(inventoryContainer);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTsaItemCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WARNING_COLOR),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Left - Category icon
        JLabel catLabel = new JLabel(item.getCategory().getEmoji());
        catLabel.setFont(EMOJI_FONT_LARGE);
        catLabel.setPreferredSize(new Dimension(50, 0));
        card.add(catLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🔒 " + item.getTitle());
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        centerPanel.add(titleLabel);
        
        // Extract terminal from keywords
        String terminal = "Unknown";
        String checkpoint = "";
        if (item.getKeywords() != null) {
            for (String kw : item.getKeywords()) {
                for (String t : TERMINALS) {
                    if (kw.contains(t)) {
                        terminal = t;
                        break;
                    }
                }
                if (kw.contains("TSA Checkpoint")) {
                    checkpoint = " • TSA Checkpoint";
                }
            }
        }
        
        JLabel detailsLabel = new JLabel(item.getCategory().getDisplayName() + " • 📍 " + terminal + checkpoint);
        detailsLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        detailsLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(detailsLabel);
        
        JLabel dateLabel = new JLabel("Found: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Accept button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 20));
        rightPanel.setOpaque(false);
        
        JButton acceptBtn = new JButton("✅ Accept into Inventory");
        acceptBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        acceptBtn.setBackground(SUCCESS_COLOR);
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setBorderPainted(false);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        acceptBtn.addActionListener(e -> acceptTsaItem(item));
        rightPanel.add(acceptBtn);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void loadInventoryData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Item> inventoryItems;
            List<Item> tsaPendingItems;
            
            @Override
            protected Void doInBackground() {
                // Get all items from the same enterprise (Logan Airport)
                List<Item> allEnterpriseItems = itemDAO.findAll().stream()
                    .filter(i -> i.getEnterpriseId() != null && 
                                i.getEnterpriseId().equals(currentUser.getEnterpriseId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // TSA items pending transfer (have "Transfer to Airport L&F" keyword but not yet accepted)
                tsaPendingItems = allEnterpriseItems.stream()
                    .filter(i -> i.getKeywords() != null && 
                                i.getKeywords().contains("Transfer to Airport L&F"))
                    .filter(i -> i.getKeywords() == null || 
                                !i.getKeywords().contains("Accepted by Airport L&F"))
                    .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                    .collect(Collectors.toList());
                
                // Main inventory - items from Airport L&F org OR accepted TSA items
                inventoryItems = allEnterpriseItems.stream()
                    .filter(i -> {
                        boolean isOwnOrg = i.getOrganizationId() != null && 
                                          i.getOrganizationId().equals(currentUser.getOrganizationId());
                        boolean isAcceptedTsa = i.getKeywords() != null && 
                                               i.getKeywords().contains("Accepted by Airport L&F");
                        return isOwnOrg || isAcceptedTsa;
                    })
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN || 
                                i.getStatus() == Item.ItemStatus.PENDING_CLAIM ||
                                i.getStatus() == Item.ItemStatus.VERIFIED)
                    .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                    .collect(Collectors.toList());
                
                return null;
            }
            
            @Override
            protected void done() {
                // Update counts
                inventoryCountLabel.setText("📦 Total Items: " + inventoryItems.size());
                tsaPendingCountLabel.setText("🔒 TSA Pending: " + tsaPendingItems.size());
                
                // Update TSA incoming panel
                tsaIncomingPanel.removeAll();
                if (tsaPendingItems.isEmpty()) {
                    JLabel noItemsLabel = new JLabel("✅ No pending items from TSA checkpoints");
                    noItemsLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
                    noItemsLabel.setForeground(SUCCESS_COLOR);
                    noItemsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    tsaIncomingPanel.add(Box.createVerticalStrut(20));
                    tsaIncomingPanel.add(noItemsLabel);
                } else {
                    for (Item item : tsaPendingItems) {
                        tsaIncomingPanel.add(createTsaItemCard(item));
                        tsaIncomingPanel.add(Box.createVerticalStrut(10));
                    }
                }
                tsaIncomingPanel.revalidate();
                tsaIncomingPanel.repaint();
                
                // Update inventory table
                inventoryModel.setRowCount(0);
                for (Item item : inventoryItems) {
                    String terminal = "Unknown";
                    String source = "Direct";
                    
                    if (item.getKeywords() != null) {
                        for (String kw : item.getKeywords()) {
                            for (String t : TERMINALS) {
                                if (kw.contains(t)) {
                                    terminal = t;
                                    break;
                                }
                            }
                            if (kw.contains("TSA Checkpoint") || kw.contains("Accepted by Airport L&F")) {
                                source = "TSA";
                            }
                        }
                    }
                    
                    inventoryModel.addRow(new Object[]{
                        item.getMongoId(),
                        item.getCategory().getEmoji() + " " + item.getTitle(),
                        item.getCategory().getDisplayName(),
                        terminal,
                        source,
                        DATE_FORMAT.format(item.getReportedDate()),
                        item.getStatus().getLabel(),
                        "View"
                    });
                }
            }
        };
        worker.execute();
    }
    
    private void acceptTsaItem(Item item) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Accept this item from TSA into your inventory?\n\n" +
            "Item: " + item.getTitle() + "\n" +
            "Category: " + item.getCategory().getDisplayName() + "\n\n" +
            "This will add the item to your Lost & Found inventory.",
            "Confirm Accept from TSA",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        // Add "Accepted by Airport L&F" keyword
                        List<String> keywords = item.getKeywords();
                        if (keywords == null) keywords = new ArrayList<>();
                        keywords.add("Accepted by Airport L&F");
                        keywords.add("Accepted on: " + DATETIME_FORMAT.format(new Date()));
                        keywords.add("Accepted by: " + currentUser.getFullName());
                        item.setKeywords(keywords);
                        
                        // Update the item in database
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
                            JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                                "✅ Item accepted into inventory!\n\n" +
                                "Item: " + item.getTitle() + "\n\n" +
                                "The item is now part of your Lost & Found inventory.",
                                "Item Accepted",
                                JOptionPane.INFORMATION_MESSAGE);
                            loadInventoryData();
                            loadDashboardData();
                        } else {
                            showError("Failed to accept item. Please try again.");
                        }
                    } catch (Exception e) {
                        showError("Error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void viewSelectedInventoryItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an item to view.");
            return;
        }
        
        String itemId = (String) inventoryModel.getValueAt(selectedRow, 0);
        Optional<Item> itemOpt = itemDAO.findById(itemId);
        
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            StringBuilder details = new StringBuilder();
            details.append("📦 Item Details\n\n");
            details.append("Title: ").append(item.getTitle()).append("\n");
            details.append("Category: ").append(item.getCategory().getDisplayName()).append("\n");
            details.append("Description: ").append(item.getDescription()).append("\n");
            details.append("Status: ").append(item.getStatus().getLabel()).append("\n");
            details.append("Found Date: ").append(DATE_FORMAT.format(item.getReportedDate())).append("\n");
            
            if (item.getEstimatedValue() > 0) {
                details.append("Estimated Value: $").append(String.format("%.2f", item.getEstimatedValue())).append("\n");
            }
            
            if (item.getKeywords() != null && !item.getKeywords().isEmpty()) {
                details.append("\nKeywords/Tags:\n");
                for (String kw : item.getKeywords()) {
                    details.append("  • ").append(kw).append("\n");
                }
            }
            
            JOptionPane.showMessageDialog(this, details.toString(), "Item Details", JOptionPane.INFORMATION_MESSAGE);
        } else {
            showError("Item not found.");
        }
    }
    
    private void processInventoryClaim() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Please select an item to process claim.");
            return;
        }
        
        String itemTitle = (String) inventoryModel.getValueAt(selectedRow, 1);
        
        JOptionPane.showMessageDialog(this,
            "📋 Claim Process for: " + itemTitle + "\n\n" +
            "To process a claim:\n" +
            "1. Verify claimant's identity (Government ID)\n" +
            "2. Verify flight information (Boarding pass/Itinerary)\n" +
            "3. Have claimant describe item contents\n" +
            "4. Complete claim form\n\n" +
            "For high-value items ($500+), police verification is required.",
            "Process Claim",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ==================== TAB 3: ITEM INTAKE ====================
    
    private JPanel createItemIntakeTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));
        
        // Title
        JLabel titleLabel = new JLabel("📦 Register Found Item");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Log items found at Logan Airport with flight and location details");
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
        intakeTitleField = new JTextField();
        titlePanel.add(intakeTitleField);
        row1.add(titlePanel);
        
        JPanel categoryPanel = createFieldPanel("Category *");
        intakeCategoryCombo = new JComboBox<>(Item.ItemCategory.values());
        intakeCategoryCombo.setRenderer(new CategoryComboRenderer());
        categoryPanel.add(intakeCategoryCombo);
        row1.add(categoryPanel);
        
        formContainer.add(row1);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Description
        formContainer.add(createFormLabel("Description *"));
        intakeDescriptionArea = new JTextArea(3, 30);
        intakeDescriptionArea.setLineWrap(true);
        intakeDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(intakeDescriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Row 2: Color, Brand, Value
        JPanel row2 = new JPanel(new GridLayout(1, 3, 15, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel colorPanel = createFieldPanel("Primary Color");
        intakeColorField = new JTextField();
        colorPanel.add(intakeColorField);
        row2.add(colorPanel);
        
        JPanel brandPanel = createFieldPanel("Brand");
        intakeBrandField = new JTextField();
        brandPanel.add(intakeBrandField);
        row2.add(brandPanel);
        
        JPanel valuePanel = createFieldPanel("Estimated Value ($)");
        intakeValueField = new JTextField();
        valuePanel.add(intakeValueField);
        row2.add(valuePanel);
        
        formContainer.add(row2);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Airport Location section
        formContainer.add(createSectionLabel("Airport Location"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 3: Terminal, Gate, Area
        JPanel row3 = new JPanel(new GridLayout(1, 3, 15, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel terminalPanel = createFieldPanel("Terminal *");
        intakeTerminalCombo = new JComboBox<>(TERMINALS);
        terminalPanel.add(intakeTerminalCombo);
        row3.add(terminalPanel);
        
        JPanel gatePanel = createFieldPanel("Gate/Area Code");
        intakeGateField = new JTextField();
        intakeGateField.setToolTipText("e.g., B12, C42");
        gatePanel.add(intakeGateField);
        row3.add(gatePanel);
        
        JPanel areaPanel = createFieldPanel("Found Area *");
        intakeAreaCombo = new JComboBox<>(AIRPORT_AREAS);
        areaPanel.add(intakeAreaCombo);
        row3.add(areaPanel);
        
        formContainer.add(row3);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Checkboxes row
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        checkboxPanel.setOpaque(false);
        checkboxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        intakeTsaCheckpointCheckBox = new JCheckBox("🔒 Found at TSA Checkpoint");
        intakeTsaCheckpointCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        intakeTsaCheckpointCheckBox.setOpaque(false);
        checkboxPanel.add(intakeTsaCheckpointCheckBox);
        
        intakeInternationalCheckBox = new JCheckBox("🌍 International Terminal");
        intakeInternationalCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        intakeInternationalCheckBox.setOpaque(false);
        checkboxPanel.add(intakeInternationalCheckBox);
        
        formContainer.add(checkboxPanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Flight Info section
        formContainer.add(createSectionLabel("Flight Information (if known)"));
        formContainer.add(Box.createVerticalStrut(10));
        
        // Row 4: Airline, Flight Number
        JPanel row4 = new JPanel(new GridLayout(1, 2, 15, 0));
        row4.setOpaque(false);
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row4.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel airlinePanel = createFieldPanel("Airline");
        intakeAirlineField = new JTextField();
        intakeAirlineField.setToolTipText("e.g., Delta, JetBlue, United");
        airlinePanel.add(intakeAirlineField);
        row4.add(airlinePanel);
        
        JPanel flightPanel = createFieldPanel("Flight Number");
        intakeFlightNumberField = new JTextField();
        intakeFlightNumberField.setToolTipText("e.g., DL1234, B6789");
        flightPanel.add(intakeFlightNumberField);
        row4.add(flightPanel);
        
        formContainer.add(row4);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Row 5: Passenger Name, Confirmation
        JPanel row5 = new JPanel(new GridLayout(1, 2, 15, 0));
        row5.setOpaque(false);
        row5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row5.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel passengerPanel = createFieldPanel("Passenger Name (if found)");
        intakePassengerNameField = new JTextField();
        passengerPanel.add(intakePassengerNameField);
        row5.add(passengerPanel);
        
        JPanel confirmPanel = createFieldPanel("Confirmation/PNR Number");
        intakeConfirmationField = new JTextField();
        confirmPanel.add(intakeConfirmationField);
        row5.add(confirmPanel);
        
        formContainer.add(row5);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Image upload
        formContainer.add(createFormLabel("Photo (Recommended)"));
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imagePanel.setOpaque(false);
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        intakeImagePreviewLabel = new JLabel("No image");
        intakeImagePreviewLabel.setPreferredSize(new Dimension(100, 100));
        intakeImagePreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        intakeImagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        intakeImagePreviewLabel.setBackground(new Color(248, 249, 250));
        intakeImagePreviewLabel.setOpaque(true);
        imagePanel.add(intakeImagePreviewLabel);
        
        imagePanel.add(Box.createHorizontalStrut(15));
        
        JButton uploadButton = new JButton("📷 Upload Photo");
        uploadButton.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        uploadButton.addActionListener(e -> selectIntakeImage());
        imagePanel.add(uploadButton);
        
        JButton clearImageButton = new JButton("Clear");
        clearImageButton.addActionListener(e -> clearIntakeImage());
        imagePanel.add(Box.createHorizontalStrut(10));
        imagePanel.add(clearImageButton);
        
        formContainer.add(imagePanel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton registerButton = new JButton("📦 Register Item");
        registerButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        registerButton.setBackground(SUCCESS_COLOR);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setPreferredSize(new Dimension(160, 45));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> registerItem());
        buttonPanel.add(registerButton);
        
        JButton registerAndMatchButton = new JButton("📦🔍 Register & Find Owner");
        registerAndMatchButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        registerAndMatchButton.setBackground(PRIMARY_COLOR);
        registerAndMatchButton.setForeground(Color.WHITE);
        registerAndMatchButton.setFocusPainted(false);
        registerAndMatchButton.setBorderPainted(false);
        registerAndMatchButton.setPreferredSize(new Dimension(200, 45));
        registerAndMatchButton.addActionListener(e -> registerAndMatch());
        buttonPanel.add(registerAndMatchButton);
        
        JButton clearButton = new JButton("Clear Form");
        clearButton.setPreferredSize(new Dimension(120, 45));
        clearButton.addActionListener(e -> clearIntakeForm());
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
    
    // ==================== TAB 3: TRAVELER SEARCH ====================
    
    private JPanel createTravelerSearchTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("🔍 Traveler Item Search");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel("Search for items by traveler name, flight number, or confirmation code");
        subtitleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        // Search criteria panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(new JLabel("Traveler Name:"), gbc);
        
        gbc.gridx = 1;
        searchNameField = new JTextField(15);
        searchPanel.add(searchNameField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("Flight Number:"), gbc);
        
        gbc.gridx = 3;
        searchFlightField = new JTextField(10);
        searchPanel.add(searchFlightField, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(new JLabel("Confirmation #:"), gbc);
        
        gbc.gridx = 1;
        searchConfirmationField = new JTextField(15);
        searchPanel.add(searchConfirmationField, gbc);
        
        gbc.gridx = 2;
        searchPanel.add(new JLabel("Terminal:"), gbc);
        
        gbc.gridx = 3;
        searchTerminalFilter = new JComboBox<>();
        searchTerminalFilter.addItem("All Terminals");
        for (String t : TERMINALS) {
            searchTerminalFilter.addItem(t);
        }
        searchPanel.add(searchTerminalFilter, gbc);
        
        // Row 3 - Cross-enterprise checkbox and buttons
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        searchCrossEnterpriseCheckBox = new JCheckBox("🔗 Include results from MBTA & Universities");
        searchCrossEnterpriseCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        searchCrossEnterpriseCheckBox.setOpaque(false);
        searchPanel.add(searchCrossEnterpriseCheckBox, gbc);
        
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        JPanel searchBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchBtnPanel.setOpaque(false);
        
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        searchBtn.setBackground(PRIMARY_COLOR);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setBorderPainted(false);
        searchBtn.addActionListener(e -> performTravelerSearch());
        searchBtnPanel.add(searchBtn);
        
        JButton clearSearchBtn = new JButton("Clear");
        clearSearchBtn.addActionListener(e -> clearTravelerSearch());
        searchBtnPanel.add(clearSearchBtn);
        
        searchPanel.add(searchBtnPanel, gbc);
        
        headerPanel.add(searchPanel, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Results panel
        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(Color.WHITE);
        searchResultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel instructionLabel = new JLabel("Enter search criteria and click Search");
        instructionLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        instructionLabel.setForeground(new Color(108, 117, 125));
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchResultsPanel.add(instructionLabel);
        
        JScrollPane resultsScroll = new JScrollPane(searchResultsPanel);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Search Results"));
        resultsScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(resultsScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 4: DELIVERY COORDINATION ====================
    
    private JPanel createDeliveryCoordinationTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Left - Delivery form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setPreferredSize(new Dimension(450, 0));
        
        JLabel titleLabel = new JLabel("🚚 Create Delivery Request");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(titleLabel);
        formPanel.add(Box.createVerticalStrut(20));
        
        // Item selection
        formPanel.add(createSectionLabel("Select Item"));
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Item to Ship *"));
        deliveryItemCombo = new JComboBox<>();
        deliveryItemCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        deliveryItemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryItemCombo);
        formPanel.add(Box.createVerticalStrut(15));
        
        // Recipient info
        formPanel.add(createSectionLabel("Recipient Information"));
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Recipient Name *"));
        deliveryRecipientNameField = new JTextField();
        deliveryRecipientNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deliveryRecipientNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryRecipientNameField);
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Street Address *"));
        deliveryAddressField = new JTextField();
        deliveryAddressField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deliveryAddressField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryAddressField);
        formPanel.add(Box.createVerticalStrut(10));
        
        // City, State, Zip row
        JPanel cityStateRow = new JPanel(new GridLayout(1, 3, 10, 0));
        cityStateRow.setOpaque(false);
        cityStateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        cityStateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel cityPanel = createFieldPanel("City *");
        deliveryCityField = new JTextField();
        cityPanel.add(deliveryCityField);
        cityStateRow.add(cityPanel);
        
        JPanel statePanel = createFieldPanel("State *");
        deliveryStateField = new JTextField();
        statePanel.add(deliveryStateField);
        cityStateRow.add(statePanel);
        
        JPanel zipPanel = createFieldPanel("ZIP *");
        deliveryZipField = new JTextField();
        zipPanel.add(deliveryZipField);
        cityStateRow.add(zipPanel);
        
        formPanel.add(cityStateRow);
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Country"));
        deliveryCountryField = new JTextField("USA");
        deliveryCountryField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deliveryCountryField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryCountryField);
        formPanel.add(Box.createVerticalStrut(10));
        
        // Phone and Email row
        JPanel contactRow = new JPanel(new GridLayout(1, 2, 10, 0));
        contactRow.setOpaque(false);
        contactRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        contactRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel phonePanel = createFieldPanel("Phone *");
        deliveryPhoneField = new JTextField();
        phonePanel.add(deliveryPhoneField);
        contactRow.add(phonePanel);
        
        JPanel emailPanel = createFieldPanel("Email");
        deliveryEmailField = new JTextField();
        emailPanel.add(deliveryEmailField);
        contactRow.add(emailPanel);
        
        formPanel.add(contactRow);
        formPanel.add(Box.createVerticalStrut(15));
        
        // Shipping info
        formPanel.add(createSectionLabel("Shipping Details"));
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Carrier *"));
        deliveryCarrierCombo = new JComboBox<>(CARRIERS);
        deliveryCarrierCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deliveryCarrierCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryCarrierCombo);
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Tracking Number (after shipping)"));
        deliveryTrackingField = new JTextField();
        deliveryTrackingField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        deliveryTrackingField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryTrackingField);
        formPanel.add(Box.createVerticalStrut(10));
        
        deliveryEmergencyCheckBox = new JCheckBox("🚨 Emergency/Priority Delivery");
        deliveryEmergencyCheckBox.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        deliveryEmergencyCheckBox.setOpaque(false);
        deliveryEmergencyCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(deliveryEmergencyCheckBox);
        formPanel.add(Box.createVerticalStrut(10));
        
        formPanel.add(createFormLabel("Notes"));
        deliveryNotesArea = new JTextArea(3, 20);
        deliveryNotesArea.setLineWrap(true);
        deliveryNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(deliveryNotesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(notesScroll);
        formPanel.add(Box.createVerticalStrut(15));
        
        // Submit button
        JButton createDeliveryBtn = new JButton("🚚 Create Delivery Request");
        createDeliveryBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        createDeliveryBtn.setBackground(PRIMARY_COLOR);
        createDeliveryBtn.setForeground(Color.WHITE);
        createDeliveryBtn.setBorderPainted(false);
        createDeliveryBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createDeliveryBtn.setMaximumSize(new Dimension(250, 40));
        createDeliveryBtn.addActionListener(e -> createDeliveryRequest());
        formPanel.add(createDeliveryBtn);
        
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        panel.add(formScroll, BorderLayout.WEST);
        
        // Right - Pending deliveries
        JPanel pendingPanel = new JPanel(new BorderLayout());
        pendingPanel.setOpaque(false);
        pendingPanel.setBorder(BorderFactory.createTitledBorder("📋 Pending Deliveries"));
        
        pendingDeliveriesPanel = new JPanel();
        pendingDeliveriesPanel.setLayout(new BoxLayout(pendingDeliveriesPanel, BoxLayout.Y_AXIS));
        pendingDeliveriesPanel.setBackground(Color.WHITE);
        
        JScrollPane pendingScroll = new JScrollPane(pendingDeliveriesPanel);
        pendingScroll.setBorder(null);
        pendingPanel.add(pendingScroll, BorderLayout.CENTER);
        
        panel.add(pendingPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 5: AUTO-MATCH ====================
    
    private JPanel createAutoMatchTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("🔎 Auto-Match Found Items with Lost Reports");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel("Match airport found items with university student lost item reports for cross-enterprise recovery");
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
        runMatchButton.setBackground(SUCCESS_COLOR);
        runMatchButton.setForeground(Color.WHITE);
        runMatchButton.setFocusPainted(false);
        runMatchButton.setBorderPainted(false);
        runMatchButton.addActionListener(e -> runAutoMatch());
        sourcePanel.add(runMatchButton);
        
        JButton runAllMatchesBtn = new JButton("🔍 Match All Airport Items");
        runAllMatchesBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        runAllMatchesBtn.addActionListener(e -> runBatchAutoMatch());
        sourcePanel.add(runAllMatchesBtn);
        
        headerPanel.add(sourcePanel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Match results panel
        matchResultsPanel = new ItemMatchResultsPanel(currentUser);
        matchResultsPanel.setOnClaimRequested((sourceItem, match) -> {
            // Open transfer tab with pre-filled data
            prefillTransferForm(sourceItem, match);
            tabbedPane.setSelectedIndex(7); // Transfer tab
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
    
    // ==================== TAB 6: CROSS-ENTERPRISE TRANSFER ====================
    
    private JPanel createCrossEnterpriseTransferTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Left - Outgoing transfers / Create new
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(450, 0));
        
        JLabel titleLabel = new JLabel("🎓 Send to University / MBTA");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // Item selection
        leftPanel.add(createSectionLabel("Select Item"));
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Item to Transfer *"));
        transferItemCombo = new JComboBox<>();
        transferItemCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        transferItemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(transferItemCombo);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // Destination
        leftPanel.add(createSectionLabel("Destination"));
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Destination Enterprise *"));
        transferDestCombo = new JComboBox<>();
        transferDestCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        transferDestCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferDestCombo.addActionListener(e -> updateTransferOrganizations());
        leftPanel.add(transferDestCombo);
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Department/Station *"));
        transferOrgCombo = new JComboBox<>();
        transferOrgCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        transferOrgCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(transferOrgCombo);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // Student info
        leftPanel.add(createSectionLabel("Owner Information"));
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Student/Owner Name *"));
        transferStudentNameField = new JTextField();
        transferStudentNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        transferStudentNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(transferStudentNameField);
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Email *"));
        transferStudentEmailField = new JTextField();
        transferStudentEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        transferStudentEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(transferStudentEmailField);
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Pickup Location *"));
        transferPickupLocationField = new JTextField("Campus Lost & Found Office");
        transferPickupLocationField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        transferPickupLocationField.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(transferPickupLocationField);
        leftPanel.add(Box.createVerticalStrut(10));
        
        leftPanel.add(createFormLabel("Notes"));
        transferNotesArea = new JTextArea(3, 20);
        transferNotesArea.setLineWrap(true);
        transferNotesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(transferNotesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(notesScroll);
        leftPanel.add(Box.createVerticalStrut(15));
        
        JButton createTransferBtn = new JButton("🔄 Create Transfer Request");
        createTransferBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        createTransferBtn.setBackground(PRIMARY_COLOR);
        createTransferBtn.setForeground(Color.WHITE);
        createTransferBtn.setBorderPainted(false);
        createTransferBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createTransferBtn.setMaximumSize(new Dimension(250, 40));
        createTransferBtn.addActionListener(e -> createTransferRequest());
        leftPanel.add(createTransferBtn);
        
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(null);
        panel.add(leftScroll, BorderLayout.WEST);
        
        // Right - Incoming and outgoing transfers
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        rightPanel.setOpaque(false);
        
        // Incoming transfers
        JPanel incomingContainer = new JPanel(new BorderLayout());
        incomingContainer.setOpaque(false);
        incomingContainer.setBorder(BorderFactory.createTitledBorder("📥 Incoming Transfers"));
        
        incomingTransfersPanel = new JPanel();
        incomingTransfersPanel.setLayout(new BoxLayout(incomingTransfersPanel, BoxLayout.Y_AXIS));
        incomingTransfersPanel.setBackground(Color.WHITE);
        
        JScrollPane incomingScroll = new JScrollPane(incomingTransfersPanel);
        incomingScroll.setBorder(null);
        incomingContainer.add(incomingScroll, BorderLayout.CENTER);
        rightPanel.add(incomingContainer);
        
        // Outgoing transfers
        JPanel outgoingContainer = new JPanel(new BorderLayout());
        outgoingContainer.setOpaque(false);
        outgoingContainer.setBorder(BorderFactory.createTitledBorder("📤 Outgoing Transfers"));
        
        outgoingTransfersPanel = new JPanel();
        outgoingTransfersPanel.setLayout(new BoxLayout(outgoingTransfersPanel, BoxLayout.Y_AXIS));
        outgoingTransfersPanel.setBackground(Color.WHITE);
        
        JScrollPane outgoingScroll = new JScrollPane(outgoingTransfersPanel);
        outgoingScroll.setBorder(null);
        outgoingContainer.add(outgoingScroll, BorderLayout.CENTER);
        rightPanel.add(outgoingContainer);
        
        panel.add(rightPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 6: REPORTS ====================
    
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📊 Reports & Analytics");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        exportPanel.setOpaque(false);
        
        JButton exportCsvBtn = new JButton("📄 Export to CSV");
        exportCsvBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        exportCsvBtn.addActionListener(e -> exportReportToCSV());
        exportPanel.add(exportCsvBtn);
        
        JButton refreshReportBtn = new JButton("🔄 Refresh");
        refreshReportBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshReportBtn.addActionListener(e -> loadReportData());
        exportPanel.add(refreshReportBtn);
        
        headerPanel.add(exportPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Summary cards
        reportSummaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        reportSummaryPanel.setOpaque(false);
        reportSummaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        panel.add(reportSummaryPanel, BorderLayout.NORTH);
        
        // Split - Summary stats and unclaimed items table
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setOpaque(false);
        
        // Summary stats at top
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        contentPanel.add(statsPanel, BorderLayout.NORTH);
        
        // Unclaimed items table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder("📋 Unclaimed Items (30+ Days)"));
        
        String[] columns = {"Item", "Category", "Terminal", "Found Date", "Days", "Status", "Actions"};
        unclaimedItemsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        
        unclaimedItemsTable = new JTable(unclaimedItemsModel);
        unclaimedItemsTable.setRowHeight(35);
        unclaimedItemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        unclaimedItemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        unclaimedItemsTable.getTableHeader().setBackground(PRIMARY_COLOR);
        unclaimedItemsTable.getTableHeader().setForeground(Color.BLACK);
        
        // Column widths
        TableColumnModel tcm = unclaimedItemsTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(200);
        tcm.getColumn(1).setPreferredWidth(120);
        tcm.getColumn(2).setPreferredWidth(100);
        tcm.getColumn(3).setPreferredWidth(100);
        tcm.getColumn(4).setPreferredWidth(60);
        tcm.getColumn(5).setPreferredWidth(100);
        tcm.getColumn(6).setPreferredWidth(100);
        
        // Button renderer for actions column
        tcm.getColumn(6).setCellRenderer(new ButtonRenderer());
        tcm.getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane tableScroll = new JScrollPane(unclaimedItemsTable);
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        
        contentPanel.add(tablePanel, BorderLayout.CENTER);
        
        // Summary panel at bottom
        JPanel dailySummaryPanel = new JPanel(new BorderLayout());
        dailySummaryPanel.setOpaque(false);
        dailySummaryPanel.setBorder(BorderFactory.createTitledBorder("📈 Daily Activity Summary"));
        dailySummaryPanel.setPreferredSize(new Dimension(0, 150));
        
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summaryArea.setText("Loading daily summary...");
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(null);
        dailySummaryPanel.add(summaryScroll, BorderLayout.CENTER);
        
        contentPanel.add(dailySummaryPanel, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
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
        return "Logan Airport Lost & Found";
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                
                List<Item> myItems = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // Today's items
                int todayReceived = (int) myItems.stream()
                    .filter(i -> i.getReportedDate().after(today.getTime()))
                    .count();
                
                // Pending claims (use work requests)
                List<WorkRequest> requests = workRequestService.getRequestsForRole(
                    "AIRPORT_LOST_FOUND_SPECIALIST", currentUser.getOrganizationId());
                int pendingClaims = (int) requests.stream()
                    .filter(r -> r.getRequestType() == WorkRequest.RequestType.ITEM_CLAIM ||
                                r.getRequestType() == WorkRequest.RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER)
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING ||
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                // International items (items with international flag - simulated)
                int internationalQueue = (int) myItems.stream()
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .filter(i -> i.getKeywords() != null && 
                                i.getKeywords().stream().anyMatch(k -> k.toLowerCase().contains("international")))
                    .count();
                
                // Emergency deliveries (simulated - high priority requests)
                int emergencyDeliveries = (int) requests.stream()
                    .filter(r -> r.getPriority() == WorkRequest.RequestPriority.URGENT ||
                                r.getPriority() == WorkRequest.RequestPriority.HIGH)
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING ||
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                return new int[]{todayReceived, pendingClaims, internationalQueue, emergencyDeliveries};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    todayReceivedLabel.setText(String.valueOf(stats[0]));
                    pendingClaimsLabel.setText(String.valueOf(stats[1]));
                    internationalQueueLabel.setText(String.valueOf(stats[2]));
                    emergencyDeliveryLabel.setText(String.valueOf(stats[3]));
                    
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
            alertsPanel.add(createAlertItem("🚨", stats[3] + " emergency delivery request(s) pending!", DANGER_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[2] > 0) {
            alertsPanel.add(createAlertItem("🌍", stats[2] + " international traveler item(s) awaiting processing", INFO_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (stats[1] > 5) {
            alertsPanel.add(createAlertItem("📋", stats[1] + " pending claims/transfers - review queue", WARNING_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Check for items from TSA checkpoints
        alertsPanel.add(createAlertItem("🔒", "TSA checkpoint items require extra verification", new Color(128, 0, 128)));
        alertsPanel.add(Box.createVerticalStrut(10));
        
        if (alertsPanel.getComponentCount() == 0) {
            JLabel noAlerts = new JLabel("✅ No critical alerts - operations running smoothly!");
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
    
    private void loadDeliveryData() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN ||
                                i.getStatus() == Item.ItemStatus.VERIFIED)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    deliveryItemCombo.removeAllItems();
                    deliveryItemCombo.addItem(new ItemOption(null, "-- Select an item --"));
                    for (Item item : items) {
                        deliveryItemCombo.addItem(new ItemOption(item, 
                            item.getCategory().getEmoji() + " " + item.getTitle()));
                    }
                    
                    loadPendingDeliveries();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void loadPendingDeliveries() {
        pendingDeliveriesPanel.removeAll();
        
        // Simulated pending deliveries - in real implementation, would query a delivery table
        JLabel placeholder = new JLabel("No pending deliveries");
        placeholder.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        placeholder.setForeground(new Color(108, 117, 125));
        pendingDeliveriesPanel.add(placeholder);
        
        pendingDeliveriesPanel.revalidate();
        pendingDeliveriesPanel.repaint();
    }
    
    private void loadMatchSourceItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                // Get items from same enterprise (to include TSA accepted items)
                List<Item> allEnterpriseItems = itemDAO.findAll().stream()
                    .filter(i -> i.getEnterpriseId() != null && 
                                i.getEnterpriseId().equals(currentUser.getEnterpriseId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // Include items from own org OR accepted TSA items (same logic as inventory)
                return allEnterpriseItems.stream()
                    .filter(i -> {
                        boolean isOwnOrg = i.getOrganizationId() != null && 
                                          i.getOrganizationId().equals(currentUser.getOrganizationId());
                        boolean isAcceptedTsa = i.getKeywords() != null && 
                                               i.getKeywords().contains("Accepted by Airport L&F");
                        return isOwnOrg || isAcceptedTsa;
                    })
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
            List<WorkRequest> transfers;
            
            @Override
            protected Void doInBackground() {
                // Get items from same enterprise first (to include TSA items)
                List<Item> allEnterpriseItems = itemDAO.findAll().stream()
                    .filter(i -> i.getEnterpriseId() != null && 
                                i.getEnterpriseId().equals(currentUser.getEnterpriseId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // Include items from own org OR accepted TSA items (same logic as loadInventoryData)
                items = allEnterpriseItems.stream()
                    .filter(i -> {
                        boolean isOwnOrg = i.getOrganizationId() != null && 
                                          i.getOrganizationId().equals(currentUser.getOrganizationId());
                        boolean isAcceptedTsa = i.getKeywords() != null && 
                                               i.getKeywords().contains("Accepted by Airport L&F");
                        return isOwnOrg || isAcceptedTsa;
                    })
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
                
                enterprises = enterpriseDAO.findAll();
                
                transfers = workRequestService.getRequestsForRole(
                    "AIRPORT_LOST_FOUND_SPECIALIST", currentUser.getOrganizationId());
                
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
                
                // Populate enterprise combo (universities and MBTA)
                transferDestCombo.removeAllItems();
                transferDestCombo.addItem(new EnterpriseOption(null, "-- Select destination --"));
                for (Enterprise e : enterprises) {
                    if (e.getType() == Enterprise.EnterpriseType.HIGHER_EDUCATION) {
                        transferDestCombo.addItem(new EnterpriseOption(e, "🎓 " + e.getName()));
                    } else if (e.getType() == Enterprise.EnterpriseType.PUBLIC_TRANSIT) {
                        transferDestCombo.addItem(new EnterpriseOption(e, "🚇 " + e.getName()));
                    }
                }
                
                // Load transfer lists
                loadTransferLists(transfers);
            }
        };
        worker.execute();
    }
    
    private void loadTransferLists(List<WorkRequest> transfers) {
        incomingTransfersPanel.removeAll();
        outgoingTransfersPanel.removeAll();
        
        int incomingCount = 0;
        int outgoingCount = 0;
        
        for (WorkRequest request : transfers) {
            // Handle regular transfers
            if (request.getRequestType() == WorkRequest.RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER ||
                request.getRequestType() == WorkRequest.RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER) {
                
                // Check if this is incoming or outgoing based on requester
                if (request.getRequesterId().equals(currentUser.getEmail())) {
                    outgoingTransfersPanel.add(createTransferCard(request, false));
                    outgoingTransfersPanel.add(Box.createVerticalStrut(10));
                    outgoingCount++;
                } else {
                    incomingTransfersPanel.add(createTransferCard(request, true));
                    incomingTransfersPanel.add(Box.createVerticalStrut(10));
                    incomingCount++;
                }
            }
            // Handle MBTA to Airport Emergency requests - these are ALWAYS incoming for Airport
            else if (request.getRequestType() == WorkRequest.RequestType.MBTA_TO_AIRPORT_EMERGENCY) {
                incomingTransfersPanel.add(createEmergencyTransferCard(request));
                incomingTransfersPanel.add(Box.createVerticalStrut(10));
                incomingCount++;
            }
        }
        
        if (incomingCount == 0) {
            JLabel noIncoming = new JLabel("No incoming transfers");
            noIncoming.setForeground(new Color(108, 117, 125));
            incomingTransfersPanel.add(noIncoming);
        }
        
        if (outgoingCount == 0) {
            JLabel noOutgoing = new JLabel("No outgoing transfers");
            noOutgoing.setForeground(new Color(108, 117, 125));
            outgoingTransfersPanel.add(noOutgoing);
        }
        
        incomingTransfersPanel.revalidate();
        incomingTransfersPanel.repaint();
        outgoingTransfersPanel.revalidate();
        outgoingTransfersPanel.repaint();
    }
    
    private JPanel createEmergencyTransferCard(WorkRequest request) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(new Color(255, 240, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DANGER_COLOR, 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        JLabel iconLabel = new JLabel("\ud83d\udea8");
        iconLabel.setFont(EMOJI_FONT_LARGE);
        card.add(iconLabel, BorderLayout.WEST);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel urgentLabel = new JLabel("\ud83d\udea8 EMERGENCY - " + request.getPriority().name());
        urgentLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        urgentLabel.setForeground(DANGER_COLOR);
        centerPanel.add(urgentLabel);
        
        JLabel summaryLabel = new JLabel(request.getRequestSummary());
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        centerPanel.add(summaryLabel);
        
        // Show flight info if available
        if (request instanceof MBTAToAirportEmergencyRequest) {
            MBTAToAirportEmergencyRequest emg = (MBTAToAirportEmergencyRequest) request;
            String flightInfo = "";
            if (emg.getFlightNumber() != null) {
                flightInfo = "\u2708\ufe0f Flight: " + emg.getFlightNumber();
                if (emg.getFlightDepartureTime() != null) {
                    flightInfo += " at " + emg.getFlightDepartureTime();
                }
            }
            if (!flightInfo.isEmpty()) {
                JLabel flightLabel = new JLabel(flightInfo);
                flightLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 11));
                flightLabel.setForeground(new Color(0, 100, 0));
                centerPanel.add(flightLabel);
            }
        }
        
        JLabel statusLabel = new JLabel("Status: " + request.getStatus().name() + 
            " \u2022 From: " + request.getRequesterName());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(statusLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        
        JButton viewBtn = new JButton("View Details");
        viewBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        viewBtn.addActionListener(e -> showRequestDetails(request));
        buttonPanel.add(viewBtn);
        
        buttonPanel.add(Box.createVerticalStrut(5));
        
        JButton processBtn = new JButton("\ud83d\udea8 Process");
        processBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 10));
        processBtn.setBackground(DANGER_COLOR);
        processBtn.setForeground(Color.WHITE);
        processBtn.setBorderPainted(false);
        processBtn.addActionListener(e -> processEmergencyRequest(request));
        buttonPanel.add(processBtn);
        
        card.add(buttonPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void processEmergencyRequest(WorkRequest request) {
        if (!(request instanceof MBTAToAirportEmergencyRequest)) {
            showRequestDetails(request);
            return;
        }
        
        MBTAToAirportEmergencyRequest emg = (MBTAToAirportEmergencyRequest) request;
        
        StringBuilder info = new StringBuilder();
        info.append("\ud83d\udea8 EMERGENCY TRANSFER DETAILS\n\n");
        info.append("Item: ").append(emg.getItemName()).append("\n");
        info.append("From: ").append(emg.getMbtaStationName()).append(" (").append(emg.getTransitLine()).append(")\n");
        info.append("\n--- TRAVELER ---\n");
        info.append("Name: ").append(emg.getTravelerName()).append("\n");
        info.append("Phone: ").append(emg.getTravelerPhone()).append("\n");
        if (emg.getTravelerEmail() != null && !emg.getTravelerEmail().isEmpty()) {
            info.append("Email: ").append(emg.getTravelerEmail()).append("\n");
        }
        info.append("\n--- FLIGHT ---\n");
        info.append("Flight: ").append(emg.getFlightNumber()).append("\n");
        info.append("Departure: ").append(emg.getFlightDepartureTime()).append("\n");
        info.append("Terminal: ").append(emg.getAirportTerminal()).append("\n");
        if (emg.getAirportGate() != null && !emg.getAirportGate().isEmpty()) {
            info.append("Gate: ").append(emg.getAirportGate()).append("\n");
        }
        info.append("\n--- STATUS ---\n");
        info.append("Current Location: ").append(emg.getCurrentLocationStatus()).append("\n");
        if (emg.isPoliceEscortRequested()) {
            info.append("\ud83d\ude94 POLICE ESCORT REQUESTED\n");
        }
        if (emg.isGateHoldRequested()) {
            info.append("\u2708\ufe0f GATE HOLD REQUESTED\n");
        }
        
        String[] options = {"\u2705 Confirm Pickup", "\ud83d\ude95 Dispatch Courier", "\ud83d\udcde Contact Traveler", "Cancel"};
        
        int choice = JOptionPane.showOptionDialog(this,
            info.toString(),
            "Process Emergency Request",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        
        switch (choice) {
            case 0 -> confirmEmergencyPickup(emg);
            case 1 -> dispatchCourier(emg);
            case 2 -> contactTraveler(emg);
        }
    }
    
    private void confirmEmergencyPickup(MBTAToAirportEmergencyRequest emg) {
        String code = JOptionPane.showInputDialog(this,
            "Enter pickup confirmation code from MBTA:",
            "Confirm Pickup",
            JOptionPane.QUESTION_MESSAGE);
        
        if (code != null && !code.trim().isEmpty()) {
            emg.confirmPickup(code.trim());
            emg.setStatus(WorkRequest.RequestStatus.IN_PROGRESS);
            
            // Save to database
            workRequestService.updateRequest(emg);
            
            JOptionPane.showMessageDialog(this,
                "\u2705 Pickup confirmed!\n\n" +
                "Item is now IN TRANSIT to the airport.\n" +
                "Confirmation Code: " + code.trim() + "\n\n" +
                "Next: Deliver to traveler at " + emg.getAirportTerminal() + 
                (emg.getAirportGate() != null ? " " + emg.getAirportGate() : ""),
                "Pickup Confirmed",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadTransferData();
            loadDashboardData();
        }
    }
    
    private void dispatchCourier(MBTAToAirportEmergencyRequest emg) {
        String[] couriers = {"Airport Shuttle", "Taxi/Rideshare", "Police Escort", "Airport Operations Vehicle"};
        String courier = (String) JOptionPane.showInputDialog(this,
            "Select courier method:",
            "Dispatch Courier",
            JOptionPane.QUESTION_MESSAGE,
            null,
            couriers,
            couriers[0]);
        
        if (courier != null) {
            emg.setCourierMethod(courier.toUpperCase().replace(" ", "_"));
            emg.addDeliveryNote("Courier dispatched: " + courier);
            
            workRequestService.updateRequest(emg);
            
            JOptionPane.showMessageDialog(this,
                "\ud83d\ude95 Courier dispatched!\n\n" +
                "Method: " + courier + "\n" +
                "Pickup from: " + emg.getMbtaStationName() + "\n" +
                "Deliver to: " + emg.getAirportTerminal() + 
                (emg.getAirportGate() != null ? " " + emg.getAirportGate() : ""),
                "Courier Dispatched",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadTransferData();
        }
    }
    
    private void contactTraveler(MBTAToAirportEmergencyRequest emg) {
        JOptionPane.showMessageDialog(this,
            "\ud83d\udcde Traveler Contact Information\n\n" +
            "Name: " + emg.getTravelerName() + "\n" +
            "Phone: " + emg.getTravelerPhone() + "\n" +
            (emg.getTravelerEmail() != null && !emg.getTravelerEmail().isEmpty() ? 
                "Email: " + emg.getTravelerEmail() + "\n" : "") +
            "\nFlight: " + emg.getFlightNumber() + "\n" +
            "Departure: " + emg.getFlightDepartureTime() + "\n" +
            "Terminal: " + emg.getAirportTerminal() +
            (emg.getAirportGate() != null ? "\nGate: " + emg.getAirportGate() : ""),
            "Contact Traveler",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private JPanel createTransferCard(WorkRequest request, boolean isIncoming) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isIncoming ? INFO_COLOR : PRIMARY_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel iconLabel = new JLabel(isIncoming ? "📥" : "📤");
        iconLabel.setFont(EMOJI_FONT_LARGE);
        card.add(iconLabel, BorderLayout.WEST);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel summaryLabel = new JLabel(request.getRequestSummary());
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(summaryLabel);
        
        JLabel statusLabel = new JLabel("Status: " + request.getStatus().name() + 
            " • " + DATE_FORMAT.format(Date.from(request.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(statusLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        JButton viewBtn = new JButton("View");
        viewBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        viewBtn.addActionListener(e -> showRequestDetails(request));
        card.add(viewBtn, BorderLayout.EAST);
        
        return card;
    }
    
    private void updateTransferOrganizations() {
        transferOrgCombo.removeAllItems();
        transferOrgCombo.addItem(new OrganizationOption(null, "-- Select department --"));
        
        EnterpriseOption selected = (EnterpriseOption) transferDestCombo.getSelectedItem();
        if (selected != null && selected.enterprise != null) {
            for (Organization org : organizationDAO.findAll()) {
                if (selected.enterprise.getEnterpriseId().equals(org.getEnterpriseId())) {
                    transferOrgCombo.addItem(new OrganizationOption(org, org.getName()));
                }
            }
        }
    }
    
    private void loadReportData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Item> unclaimedItems;
            
            @Override
            protected Void doInBackground() {
                Calendar thirtyDaysAgo = Calendar.getInstance();
                thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -30);
                
                unclaimedItems = itemDAO.findAll().stream()
                    .filter(i -> i.getOrganizationId() != null && 
                                i.getOrganizationId().equals(currentUser.getOrganizationId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .filter(i -> i.getReportedDate().before(thirtyDaysAgo.getTime()))
                    .sorted((a, b) -> a.getReportedDate().compareTo(b.getReportedDate()))
                    .collect(Collectors.toList());
                
                return null;
            }
            
            @Override
            protected void done() {
                // Update unclaimed items table
                unclaimedItemsModel.setRowCount(0);
                
                for (Item item : unclaimedItems) {
                    long daysSinceFound = (new Date().getTime() - item.getReportedDate().getTime()) 
                                          / (1000 * 60 * 60 * 24);
                    
                    String terminal = "Unknown";
                    if (item.getKeywords() != null) {
                        for (String kw : item.getKeywords()) {
                            for (String t : TERMINALS) {
                                if (kw.contains(t)) {
                                    terminal = t;
                                    break;
                                }
                            }
                        }
                    }
                    
                    unclaimedItemsModel.addRow(new Object[]{
                        item.getTitle(),
                        item.getCategory().getDisplayName(),
                        terminal,
                        DATE_FORMAT.format(item.getReportedDate()),
                        daysSinceFound,
                        item.getStatus().getLabel(),
                        "Dispose"
                    });
                }
            }
        };
        worker.execute();
    }
    
    // ==================== ACTIONS ====================
    
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
                
                // Get items from same enterprise (to include TSA accepted items)
                List<Item> allEnterpriseItems = itemDAO.findAll().stream()
                    .filter(i -> i.getEnterpriseId() != null && 
                                i.getEnterpriseId().equals(currentUser.getEnterpriseId()))
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .collect(Collectors.toList());
                
                // Filter to own org OR accepted TSA items
                List<Item> myFoundItems = allEnterpriseItems.stream()
                    .filter(i -> {
                        boolean isOwnOrg = i.getOrganizationId() != null && 
                                          i.getOrganizationId().equals(currentUser.getOrganizationId());
                        boolean isAcceptedTsa = i.getKeywords() != null && 
                                               i.getKeywords().contains("Accepted by Airport L&F");
                        return isOwnOrg || isAcceptedTsa;
                    })
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
                        JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                            "No matches found for any airport items.",
                            "Batch Match Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
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
            
            JLabel itemsLabel = new JLabel("📊 " + results.size() + " items with matches");
            itemsLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
            matchSummaryPanel.add(itemsLabel);
            
            JLabel matchesLabel = new JLabel("🔗 " + totalMatches + " total potential matches");
            matchesLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
            matchSummaryPanel.add(matchesLabel);
            
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
            
            JButton sendBtn = new JButton("🎓 Send to University");
            sendBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
            sendBtn.addActionListener(e -> {
                dialog.dispose();
                prefillTransferForm(foundItem, matches.get(0));
                tabbedPane.setSelectedIndex(6); // Transfer tab
            });
            itemPanel.add(sendBtn, BorderLayout.EAST);
            
            panel.add(itemPanel);
            panel.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scroll = new JScrollPane(panel);
        dialog.add(scroll);
        dialog.setVisible(true);
    }
    
    private void prefillTransferForm(Item sourceItem, PotentialMatch match) {
        // Make sure transfer data is loaded
        loadTransferData();
        
        // Use SwingUtilities.invokeLater to ensure combo boxes are populated
        SwingUtilities.invokeLater(() -> {
            // Select the found item in transfer combo
            for (int i = 0; i < transferItemCombo.getItemCount(); i++) {
                ItemOption opt = transferItemCombo.getItemAt(i);
                if (opt.item != null && opt.item.getMongoId() != null &&
                    opt.item.getMongoId().equals(sourceItem.getMongoId())) {
                    transferItemCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            // Get student info from the matched lost item
            Item lostItem = match.getItem();
            if (lostItem.getReportedBy() != null) {
                transferStudentNameField.setText(lostItem.getReportedBy().getFullName());
                transferStudentEmailField.setText(lostItem.getReportedBy().getEmail());
            }
            
            // Store the matched lost item ID for auto-closure when student confirms pickup
            matchedLostItemId = lostItem.getMongoId();
            
            // Add match notes
            int confidence = (int) (match.getScore() * 100);
            transferNotesArea.setText("Auto-matched with " + confidence + 
                "% confidence.\nLost item: " + lostItem.getTitle() + 
                "\nLost Item ID: " + lostItem.getMongoId() +
                "\nReported by: " + (lostItem.getReportedBy() != null ? lostItem.getReportedBy().getFullName() : "Unknown"));
            
            // Try to auto-select the university based on the lost item's enterprise
            if (lostItem.getEnterpriseId() != null) {
                for (int i = 0; i < transferDestCombo.getItemCount(); i++) {
                    EnterpriseOption opt = transferDestCombo.getItemAt(i);
                    if (opt.enterprise != null && 
                        opt.enterprise.getEnterpriseId().equals(lostItem.getEnterpriseId())) {
                        transferDestCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
    }
    
    private void registerItem() {
        if (!validateIntakeForm()) return;
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = createItemFromForm();
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
                        JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                            "Item registered successfully!\nItem ID: " + itemId,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearIntakeForm();
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
    
    private void registerAndMatch() {
        if (!validateIntakeForm()) return;
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = createItemFromForm();
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
                        clearIntakeForm();
                        loadDashboardData();
                        
                        // Find matches using cross-enterprise matcher
                        findMatchesForItem(itemId);
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
    
    private Item createItemFromForm() {
        Item item = new Item(
            intakeTitleField.getText().trim(),
            intakeDescriptionArea.getText().trim(),
            (Item.ItemCategory) intakeCategoryCombo.getSelectedItem(),
            Item.ItemType.FOUND,
            null,
            currentUser
        );
        
        item.setStatus(Item.ItemStatus.OPEN);
        item.setEnterpriseId(currentUser.getEnterpriseId());
        item.setOrganizationId(currentUser.getOrganizationId());
        
        if (!intakeColorField.getText().trim().isEmpty()) {
            item.setPrimaryColor(intakeColorField.getText().trim());
        }
        if (!intakeBrandField.getText().trim().isEmpty()) {
            item.setBrand(intakeBrandField.getText().trim());
        }
        
        try {
            if (!intakeValueField.getText().trim().isEmpty()) {
                item.setEstimatedValue(Double.parseDouble(intakeValueField.getText().trim()));
            }
        } catch (NumberFormatException ignored) {}
        
        // Add airport context to keywords
        List<String> keywords = item.getKeywords();
        if (keywords == null) keywords = new ArrayList<>();
        
        keywords.add((String) intakeTerminalCombo.getSelectedItem());
        keywords.add((String) intakeAreaCombo.getSelectedItem());
        
        if (!intakeGateField.getText().trim().isEmpty()) {
            keywords.add("Gate " + intakeGateField.getText().trim());
        }
        if (!intakeAirlineField.getText().trim().isEmpty()) {
            keywords.add(intakeAirlineField.getText().trim());
        }
        if (!intakeFlightNumberField.getText().trim().isEmpty()) {
            keywords.add(intakeFlightNumberField.getText().trim());
        }
        if (intakeTsaCheckpointCheckBox.isSelected()) {
            keywords.add("TSA Checkpoint");
        }
        if (intakeInternationalCheckBox.isSelected()) {
            keywords.add("International Terminal");
        }
        
        item.setKeywords(keywords);
        
        // Add images
        for (String path : intakeImagePaths) {
            item.addImagePath(path);
        }
        
        return item;
    }
    
    private boolean validateIntakeForm() {
        if (intakeTitleField.getText().trim().isEmpty()) {
            showError("Please enter an item title.");
            intakeTitleField.requestFocus();
            return false;
        }
        if (intakeDescriptionArea.getText().trim().isEmpty()) {
            showError("Please enter a description.");
            intakeDescriptionArea.requestFocus();
            return false;
        }
        return true;
    }
    
    private void findMatchesForItem(String itemId) {
        Optional<Item> itemOpt = itemDAO.findById(itemId);
        if (itemOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Item registered but could not find it for matching.",
                "Partial Success",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Item foundItem = itemOpt.get();
        
        // Use cross-enterprise matching
        List<com.campus.lostfound.models.EnterpriseMatchResult> matches = 
            enterpriseItemMatcher.matchAcrossEnterprises(foundItem);
        
        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Item registered successfully!\nNo potential matches found across all enterprises.",
                "No Matches",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Item registered! Found ").append(matches.size()).append(" potential match(es):\n\n");
            
            for (int i = 0; i < Math.min(5, matches.size()); i++) {
                com.campus.lostfound.models.EnterpriseMatchResult match = matches.get(i);
                sb.append("• ").append(match.getMatchedItem().getTitle())
                  .append(" (").append(match.getScoreLabel()).append(")")
                  .append("\n  📍 ").append(match.getMatchedEnterpriseName())
                  .append("\n");
            }
            
            if (matches.size() > 5) {
                sb.append("\n... and ").append(matches.size() - 5).append(" more");
            }
            
            int result = JOptionPane.showConfirmDialog(this,
                sb.toString() + "\n\nWould you like to initiate a transfer?",
                "Potential Matches Found",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                tabbedPane.setSelectedIndex(7); // Transfer tab
                selectItemForTransfer(foundItem);
            }
        }
    }
    
    private void performTravelerSearch() {
        String name = searchNameField.getText().trim();
        String flight = searchFlightField.getText().trim();
        String confirmation = searchConfirmationField.getText().trim();
        String terminal = (String) searchTerminalFilter.getSelectedItem();
        boolean crossEnterprise = searchCrossEnterpriseCheckBox.isSelected();
        
        if (name.isEmpty() && flight.isEmpty() && confirmation.isEmpty()) {
            showError("Please enter at least one search criterion.");
            return;
        }
        
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                List<Item> results;
                
                if (crossEnterprise) {
                    // Search across all enterprises
                    results = enterpriseItemService.searchAllEnterprises(
                        name + " " + flight + " " + confirmation, null);
                } else {
                    // Search local items only
                    results = itemDAO.findAll().stream()
                        .filter(i -> i.getOrganizationId() != null && 
                                    i.getOrganizationId().equals(currentUser.getOrganizationId()))
                        .collect(Collectors.toList());
                }
                
                // Apply filters
                return results.stream()
                    .filter(i -> {
                        if (!name.isEmpty()) {
                            boolean match = (i.getReportedBy() != null && 
                                           i.getReportedBy().getFullName().toLowerCase().contains(name.toLowerCase()));
                            if (i.getKeywords() != null) {
                                match = match || i.getKeywords().stream()
                                    .anyMatch(k -> k.toLowerCase().contains(name.toLowerCase()));
                            }
                            if (!match) return false;
                        }
                        if (!flight.isEmpty() && i.getKeywords() != null) {
                            boolean hasFlght = i.getKeywords().stream()
                                .anyMatch(k -> k.toLowerCase().contains(flight.toLowerCase()));
                            if (!hasFlght) return false;
                        }
                        if (!confirmation.isEmpty() && i.getKeywords() != null) {
                            boolean hasConf = i.getKeywords().stream()
                                .anyMatch(k -> k.toLowerCase().contains(confirmation.toLowerCase()));
                            if (!hasConf) return false;
                        }
                        if (terminal != null && !terminal.equals("All Terminals") && i.getKeywords() != null) {
                            boolean hasTerm = i.getKeywords().stream()
                                .anyMatch(k -> k.contains(terminal));
                            if (!hasTerm) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> results = get();
                    displaySearchResults(results);
                } catch (Exception e) {
                    showError("Search failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void displaySearchResults(List<Item> results) {
        searchResultsPanel.removeAll();
        
        if (results.isEmpty()) {
            JLabel noResults = new JLabel("No items found matching your criteria.");
            noResults.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
            noResults.setForeground(new Color(108, 117, 125));
            noResults.setAlignmentX(Component.CENTER_ALIGNMENT);
            searchResultsPanel.add(noResults);
        } else {
            JLabel countLabel = new JLabel("Found " + results.size() + " item(s)");
            countLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
            countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchResultsPanel.add(countLabel);
            searchResultsPanel.add(Box.createVerticalStrut(10));
            
            for (Item item : results) {
                searchResultsPanel.add(createSearchResultCard(item));
                searchResultsPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }
    
    private JPanel createSearchResultCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        
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
        
        String locationText = "";
        if (item.getKeywords() != null) {
            for (String kw : item.getKeywords()) {
                for (String t : TERMINALS) {
                    if (kw.contains(t)) {
                        locationText = t;
                        break;
                    }
                }
                if (!locationText.isEmpty()) break;
            }
        }
        
        JLabel detailsLabel = new JLabel(item.getCategory().getDisplayName() + 
            (locationText.isEmpty() ? "" : " • 📍 " + locationText));
        detailsLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        detailsLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(detailsLabel);
        
        JLabel dateLabel = new JLabel("Found: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Actions
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
        
        JButton claimBtn = new JButton("📋 Claim");
        claimBtn.setToolTipText("Initiate Claim");
        claimBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 10));
        claimBtn.addActionListener(e -> initiateClaim(item));
        buttonRow.add(claimBtn);
        
        JButton deliverBtn = new JButton("🚚 Ship");
        deliverBtn.setToolTipText("Arrange Delivery");
        deliverBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 10));
        deliverBtn.addActionListener(e -> {
            tabbedPane.setSelectedIndex(5); // Delivery tab
            selectItemForDelivery(item);
        });
        buttonRow.add(deliverBtn);
        
        rightPanel.add(buttonRow);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void clearTravelerSearch() {
        searchNameField.setText("");
        searchFlightField.setText("");
        searchConfirmationField.setText("");
        searchTerminalFilter.setSelectedIndex(0);
        searchCrossEnterpriseCheckBox.setSelected(false);
        
        searchResultsPanel.removeAll();
        JLabel instructionLabel = new JLabel("Enter search criteria and click Search");
        instructionLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        instructionLabel.setForeground(new Color(108, 117, 125));
        searchResultsPanel.add(instructionLabel);
        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }
    
    private void createDeliveryRequest() {
        ItemOption selectedItem = (ItemOption) deliveryItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item to ship.");
            return;
        }
        
        if (deliveryRecipientNameField.getText().trim().isEmpty() ||
            deliveryAddressField.getText().trim().isEmpty() ||
            deliveryCityField.getText().trim().isEmpty() ||
            deliveryStateField.getText().trim().isEmpty() ||
            deliveryZipField.getText().trim().isEmpty() ||
            deliveryPhoneField.getText().trim().isEmpty()) {
            showError("Please fill in all required shipping fields.");
            return;
        }
        
        // Create delivery request (simplified - would integrate with shipping API)
        StringBuilder address = new StringBuilder();
        address.append(deliveryRecipientNameField.getText().trim()).append("\n");
        address.append(deliveryAddressField.getText().trim()).append("\n");
        address.append(deliveryCityField.getText().trim()).append(", ");
        address.append(deliveryStateField.getText().trim()).append(" ");
        address.append(deliveryZipField.getText().trim()).append("\n");
        address.append(deliveryCountryField.getText().trim());
        
        String carrier = (String) deliveryCarrierCombo.getSelectedItem();
        boolean isEmergency = deliveryEmergencyCheckBox.isSelected();
        
        JOptionPane.showMessageDialog(this,
            "Delivery request created!\n\n" +
            "Item: " + selectedItem.item.getTitle() + "\n" +
            "Carrier: " + carrier + "\n" +
            "Priority: " + (isEmergency ? "EMERGENCY" : "Standard") + "\n\n" +
            "Ship to:\n" + address.toString() + "\n\n" +
            "Please prepare the item for shipping and enter the tracking number when available.",
            "Delivery Request Created",
            JOptionPane.INFORMATION_MESSAGE);
        
        clearDeliveryForm();
        loadDeliveryData();
    }
    
    private void clearDeliveryForm() {
        if (deliveryItemCombo.getItemCount() > 0) {
            deliveryItemCombo.setSelectedIndex(0);
        }
        deliveryRecipientNameField.setText("");
        deliveryAddressField.setText("");
        deliveryCityField.setText("");
        deliveryStateField.setText("");
        deliveryZipField.setText("");
        deliveryCountryField.setText("USA");
        deliveryPhoneField.setText("");
        deliveryEmailField.setText("");
        deliveryCarrierCombo.setSelectedIndex(0);
        deliveryTrackingField.setText("");
        deliveryEmergencyCheckBox.setSelected(false);
        deliveryNotesArea.setText("");
    }
    
    private void createTransferRequest() {
        ItemOption selectedItem = (ItemOption) transferItemCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.item == null) {
            showError("Please select an item to transfer.");
            return;
        }
        
        EnterpriseOption selectedDest = (EnterpriseOption) transferDestCombo.getSelectedItem();
        if (selectedDest == null || selectedDest.enterprise == null) {
            showError("Please select a destination enterprise.");
            return;
        }
        
        OrganizationOption selectedOrg = (OrganizationOption) transferOrgCombo.getSelectedItem();
        if (selectedOrg == null || selectedOrg.organization == null) {
            showError("Please select a destination department/station.");
            return;
        }
        
        if (transferStudentNameField.getText().trim().isEmpty() ||
            transferStudentEmailField.getText().trim().isEmpty() ||
            transferPickupLocationField.getText().trim().isEmpty()) {
            showError("Please fill in all required owner information fields.");
            return;
        }
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Item item = selectedItem.item;
                    
                    // Generate a student ID from email (use email prefix or hash)
                    String studentEmail = transferStudentEmailField.getText().trim();
                    String studentId = studentEmail.contains("@") 
                        ? studentEmail.substring(0, studentEmail.indexOf("@"))
                        : "STU-" + System.currentTimeMillis() % 100000;
                    
                    AirportToUniversityTransferRequest transfer = new AirportToUniversityTransferRequest(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        item.getMongoId(),
                        item.getTitle(),
                        studentId,  // Use generated student ID
                        transferStudentNameField.getText().trim()
                    );
                    
                    transfer.setItemCategory(item.getCategory().name());
                    transfer.setItemDescription(item.getDescription());
                    transfer.setEstimatedValue(item.getEstimatedValue());
                    
                    // Airport context
                    String terminal = "Unknown";
                    if (item.getKeywords() != null) {
                        for (String kw : item.getKeywords()) {
                            for (String t : TERMINALS) {
                                if (kw.contains(t)) {
                                    terminal = t;
                                    break;
                                }
                            }
                        }
                    }
                    transfer.setTerminalNumber(terminal);
                    transfer.setAirportSpecialistId(currentUser.getEmail());
                    transfer.setAirportSpecialistName(currentUser.getFullName());
                    transfer.setAirportIncidentNumber("LOGAN-" + System.currentTimeMillis() % 100000);
                    
                    // University context
                    transfer.setUniversityName(selectedDest.enterprise.getName());
                    transfer.setTargetEnterpriseId(selectedDest.enterprise.getEnterpriseId());
                    transfer.setTargetOrganizationId(selectedOrg.organization.getOrganizationId());
                    transfer.setCampusPickupLocation(transferPickupLocationField.getText().trim());
                    
                    // CRITICAL: Set campus coordinator ID (use target org ID as placeholder)
                    // The routing engine will find the actual coordinator
                    transfer.setCampusCoordinatorId(selectedOrg.organization.getOrganizationId());
                    transfer.setCampusCoordinatorName(selectedOrg.organization.getName() + " Coordinator");
                    
                    // Student details
                    transfer.setStudentId(studentId);
                    transfer.setStudentName(transferStudentNameField.getText().trim());
                    transfer.setStudentEmail(studentEmail);
                    
                    // Set matched lost item ID if this came from auto-match
                    // This enables auto-closure of the student's LOST item when pickup is confirmed
                    if (matchedLostItemId != null && !matchedLostItemId.isEmpty()) {
                        transfer.setLostItemId(matchedLostItemId);
                    }
                    
                    // Notes
                    transfer.setTransferNotes(transferNotesArea.getText().trim());
                    
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
                        JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                            "Transfer request created successfully!\n" +
                            "The destination will be notified to arrange pickup.",
                            "Request Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearTransferForm();
                        loadDashboardData();
                        loadTransferData();
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
        if (transferDestCombo.getItemCount() > 0) {
            transferDestCombo.setSelectedIndex(0);
        }
        transferStudentNameField.setText("");
        transferStudentEmailField.setText("");
        transferPickupLocationField.setText("Campus Lost & Found Office");
        transferNotesArea.setText("");
        matchedLostItemId = null;  // Clear matched lost item reference
    }
    
    private void exportReportToCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("logan_airport_items_report_" + 
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    List<Item> items = itemDAO.findAll().stream()
                        .filter(i -> i.getOrganizationId() != null && 
                                    i.getOrganizationId().equals(currentUser.getOrganizationId()))
                        .collect(Collectors.toList());
                    
                    return reportExportService.exportItemsToCSV(items, file.getAbsolutePath());
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(AirportLostFoundSpecialistPanel.this,
                                "Report exported successfully!\n" + file.getAbsolutePath(),
                                "Export Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showError("Failed to export report.");
                        }
                    } catch (Exception e) {
                        showError("Export error: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void clearIntakeForm() {
        intakeTitleField.setText("");
        intakeDescriptionArea.setText("");
        intakeCategoryCombo.setSelectedIndex(0);
        intakeTerminalCombo.setSelectedIndex(0);
        intakeGateField.setText("");
        intakeAreaCombo.setSelectedIndex(0);
        intakeAirlineField.setText("");
        intakeFlightNumberField.setText("");
        intakePassengerNameField.setText("");
        intakeConfirmationField.setText("");
        intakeTsaCheckpointCheckBox.setSelected(false);
        intakeInternationalCheckBox.setSelected(false);
        intakeColorField.setText("");
        intakeBrandField.setText("");
        intakeValueField.setText("");
        clearIntakeImage();
    }
    
    private void selectIntakeImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String destPath = "items/images/" + System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(System.getProperty("user.dir"), destPath);
                destFile.getParentFile().mkdirs();
                java.nio.file.Files.copy(file.toPath(), destFile.toPath());
                
                intakeImagePaths.clear();
                intakeImagePaths.add(destPath);
                
                ImageIcon icon = new ImageIcon(destFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                intakeImagePreviewLabel.setIcon(new ImageIcon(img));
                intakeImagePreviewLabel.setText("");
            } catch (Exception e) {
                showError("Failed to upload image: " + e.getMessage());
            }
        }
    }
    
    private void clearIntakeImage() {
        intakeImagePaths.clear();
        intakeImagePreviewLabel.setIcon(null);
        intakeImagePreviewLabel.setText("No image");
    }
    
    private void selectItemForDelivery(Item item) {
        for (int i = 0; i < deliveryItemCombo.getItemCount(); i++) {
            ItemOption opt = deliveryItemCombo.getItemAt(i);
            if (opt.item != null && opt.item.getMongoId() != null &&
                opt.item.getMongoId().equals(item.getMongoId())) {
                deliveryItemCombo.setSelectedIndex(i);
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
    
    private void initiateClaim(Item item) {
        JOptionPane.showMessageDialog(this,
            "To claim this item, please verify the traveler's identity and have them complete a claim form.\n\n" +
            "Item: " + item.getTitle() + "\n" +
            "Category: " + item.getCategory().getDisplayName() + "\n\n" +
            "Required verification:\n• Government-issued ID\n• Flight itinerary or boarding pass\n• Description of item contents",
            "Initiate Claim Process",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showRequestDetails(WorkRequest request) {
        RequestDetailDialog dialog = new RequestDetailDialog(
            SwingUtilities.getWindowAncestor(this),
            request,
            currentUser
        );
        dialog.setOnRequestUpdated(r -> {
            loadDashboardData();
            loadTransferData();
        });
        dialog.setVisible(true);
    }
    
    private void refreshAll() {
        loadDashboardData();
        loadPendingApprovals();
        loadInventoryData();
        loadDeliveryData();
        loadMatchSourceItems();
        loadTransferData();
        loadReportData();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openEmergencyDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "🚨 Emergency Procedures",
            true
        );
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("Emergency Contact & Procedures");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        
        String[] contacts = {
            "🚔 Massport Police: 617-568-5000",
            "🚨 TSA Command Center: 617-568-4500",
            "🏥 Logan Medical: 617-561-1600",
            "🔥 Fire Emergency: 911",
            "📞 Operations Center: 617-568-5050"
        };
        
        for (String contact : contacts) {
            JLabel contactLabel = new JLabel(contact);
            contactLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
            contactLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(contactLabel);
            panel.add(Box.createVerticalStrut(10));
        }
        
        panel.add(Box.createVerticalStrut(20));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void contactPoliceDialog() {
        JOptionPane.showMessageDialog(this,
            "🚔 Massport Police Contact Information\n\n" +
            "Emergency: 617-568-5000\n" +
            "Non-Emergency: 617-568-5010\n" +
            "Lost & Found Coordination: 617-568-5020\n\n" +
            "For suspicious items or security concerns,\ncontact TSA immediately: 617-568-4500",
            "Police Contact",
            JOptionPane.INFORMATION_MESSAGE);
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
    
    // Button renderer for table actions
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 10));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            return this;
        }
    }
    
    // Button editor for table actions
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = value != null ? value.toString() : "";
            button.setText(label);
            isPushed = true;
            currentRow = row;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Handle dispose action
                String itemTitle = (String) unclaimedItemsModel.getValueAt(currentRow, 0);
                int confirm = JOptionPane.showConfirmDialog(
                    AirportLostFoundSpecialistPanel.this,
                    "Dispose of item: " + itemTitle + "?",
                    "Confirm Disposal",
                    JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(
                        AirportLostFoundSpecialistPanel.this,
                        "Item marked for disposal. Follow Massport disposal procedures.",
                        "Item Disposed",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
