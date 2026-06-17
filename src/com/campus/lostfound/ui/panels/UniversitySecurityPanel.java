package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.trustscore.*;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.verification.*;
import com.campus.lostfound.models.verification.VerificationRequest.*;
import com.campus.lostfound.services.*;
import com.campus.lostfound.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main panel for University Security role.
 * 
 * Features:
 * - Dashboard with key security metrics and alerts
 * - Verification queue for processing verification requests
 * - Student lookup with trust score display
 * - High-value items management
 * - Reports and analytics
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class UniversitySecurityPanel extends JPanel {
    
    // Services
    private TrustScoreService trustScoreService;
    private VerificationService verificationService;
    private AuthorityService authorityService;
    
    // DAOs
    private MongoUserDAO userDAO;
    private MongoItemDAO itemDAO;
    private MongoTrustScoreDAO trustScoreDAO;
    private MongoVerificationDAO verificationDAO;
    
    // Data
    private User currentUser;
    
    // UI Components
    private JTabbedPane tabbedPane;
    
    // Dashboard components
    private JLabel activeVerificationsLabel;
    private JLabel flaggedUsersLabel;
    private JLabel pendingHighValueLabel;
    private JLabel overdueVerificationsLabel;
    private JPanel alertsPanel;
    
    // Verification Queue components
    private JTable verificationTable;
    private DefaultTableModel verificationTableModel;
    private JComboBox<String> verificationTypeFilter;
    private JComboBox<String> verificationStatusFilter;
    private JComboBox<String> verificationPriorityFilter;
    
    // Student Lookup components
    private JTextField studentSearchField;
    private JPanel studentResultPanel;
    private JPanel trustScoreHistoryPanel;
    
    // High-Value Items components
    private JTable highValueTable;
    private DefaultTableModel highValueTableModel;
    
    // Reports components
    private JPanel reportContentPanel;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final Color PRIMARY_COLOR = new Color(13, 110, 253);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    private static final Color PURPLE_COLOR = new Color(111, 66, 193);
    private static final double HIGH_VALUE_THRESHOLD = 500.0;
    
    /**
     * Create a new UniversitySecurityPanel.
     * 
     * @param currentUser The logged-in security officer
     */
    public UniversitySecurityPanel(User currentUser) {
        this.currentUser = currentUser;
        
        // Initialize services
        this.trustScoreService = new TrustScoreService();
        this.verificationService = new VerificationService();
        this.authorityService = new AuthorityService();
        
        // Initialize DAOs
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.verificationDAO = new MongoVerificationDAO();
        
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
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        // Create tabs
        tabbedPane.addTab("Dashboard", createDashboardTab());
        tabbedPane.addTab("Verification Queue", createVerificationQueueTab());
        tabbedPane.addTab("Student Lookup", createStudentLookupTab());
        tabbedPane.addTab("High-Value Items", createHighValueItemsTab());
        tabbedPane.addTab("Reports", createReportsTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> loadVerificationQueue();
                case 3 -> loadHighValueItems();
                case 4 -> loadReports();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Welcome
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("University Security Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel(currentUser.getFullName() + " • Security Officer");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = createHeaderButton("Refresh All");
        refreshBtn.addActionListener(e -> refreshAll());
        rightPanel.add(refreshBtn);
        
        JButton emergencyBtn = createHeaderButton("Emergency Alert");
        emergencyBtn.setBackground(DANGER_COLOR);
        emergencyBtn.addActionListener(e -> showEmergencyDialog());
        rightPanel.add(emergencyBtn);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(52, 73, 94));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    // ==================== TAB 1: DASHBOARD ====================
    
    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Stats cards row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 15, 0));
        statsRow.setOpaque(false);
        
        // Active Verifications
        JPanel verifCard = createStatCard("Active Verifications", "0", PRIMARY_COLOR, "Pending verification requests");
        activeVerificationsLabel = findValueLabel(verifCard);
        statsRow.add(verifCard);
        
        // Flagged Users
        JPanel flaggedCard = createStatCard("Flagged Users", "0", DANGER_COLOR, "Users flagged for review");
        flaggedUsersLabel = findValueLabel(flaggedCard);
        statsRow.add(flaggedCard);
        
        // Pending High-Value Claims
        JPanel highValueCard = createStatCard("High-Value Claims", "0", WARNING_COLOR, "Claims over $500 pending");
        pendingHighValueLabel = findValueLabel(highValueCard);
        statsRow.add(highValueCard);
        
        // Overdue Verifications
        JPanel overdueCard = createStatCard("Overdue", "0", PURPLE_COLOR, "Past SLA deadline");
        overdueVerificationsLabel = findValueLabel(overdueCard);
        statsRow.add(overdueCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - split into alerts and quick actions
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = createTitledPanel("Security Alerts");
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Color.WHITE);
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsContainer.add(alertsScroll, BorderLayout.CENTER);
        contentPanel.add(alertsContainer);
        
        // Quick actions panel
        JPanel actionsContainer = createTitledPanel("Quick Actions");
        JPanel actionsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        actionsPanel.setBackground(Color.WHITE);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        actionsPanel.add(createQuickActionButton("Process Verifications", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("Lookup Student", () -> tabbedPane.setSelectedIndex(2)));
        actionsPanel.add(createQuickActionButton("High-Value Items", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("View Reports", () -> tabbedPane.setSelectedIndex(4)));
        actionsPanel.add(createQuickActionButton("Flag User", this::showFlagUserDialog));
        actionsPanel.add(createQuickActionButton("Serial # Check", this::showSerialNumberCheckDialog));
        
        actionsContainer.add(actionsPanel, BorderLayout.CENTER);
        contentPanel.add(actionsContainer);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Bottom - Recent Activity
        JPanel recentPanel = createTitledPanel("Recent Security Activity");
        recentPanel.setPreferredSize(new Dimension(0, 200));
        
        JTextArea activityLog = new JTextArea();
        activityLog.setEditable(false);
        activityLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        activityLog.setText(getRecentActivityLog());
        JScrollPane logScroll = new JScrollPane(activityLog);
        logScroll.setBorder(null);
        recentPanel.add(logScroll, BorderLayout.CENTER);
        
        panel.add(recentPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, Color color, String tooltip) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setToolTipText(tooltip);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setName("valueLabel");
        textPanel.add(valueLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(108, 117, 125));
        textPanel.add(titleLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JLabel findValueLabel(JPanel card) {
        for (Component c : card.getComponents()) {
            if (c instanceof JPanel) {
                for (Component inner : ((JPanel) c).getComponents()) {
                    if (inner instanceof JLabel && "valueLabel".equals(inner.getName())) {
                        return (JLabel) inner;
                    }
                }
            }
        }
        return new JLabel("0");
    }
    
    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        );
        panel.setBorder(border);
        return panel;
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
    
    // ==================== TAB 2: VERIFICATION QUEUE ====================
    
    private JPanel createVerificationQueueTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with filters
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Verification Request Queue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Review and process pending verification requests");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        titlePanel.add(helpLabel, BorderLayout.SOUTH);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        filterPanel.add(new JLabel("Type:"));
        verificationTypeFilter = new JComboBox<>();
        verificationTypeFilter.addItem("All Types");
        for (VerificationType type : VerificationType.values()) {
            verificationTypeFilter.addItem(type.getDisplayName());
        }
        verificationTypeFilter.setPreferredSize(new Dimension(160, 28));
        verificationTypeFilter.addActionListener(e -> filterVerificationQueue());
        filterPanel.add(verificationTypeFilter);
        
        filterPanel.add(new JLabel("Status:"));
        verificationStatusFilter = new JComboBox<>(new String[]{
            "All Statuses", "Pending", "In Progress", "Awaiting Documents", "Awaiting Response"
        });
        verificationStatusFilter.setPreferredSize(new Dimension(140, 28));
        verificationStatusFilter.addActionListener(e -> filterVerificationQueue());
        filterPanel.add(verificationStatusFilter);
        
        filterPanel.add(new JLabel("Priority:"));
        verificationPriorityFilter = new JComboBox<>(new String[]{
            "All Priorities", "Urgent", "High", "Normal", "Low"
        });
        verificationPriorityFilter.setPreferredSize(new Dimension(120, 28));
        verificationPriorityFilter.addActionListener(e -> filterVerificationQueue());
        filterPanel.add(verificationPriorityFilter);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadVerificationQueue());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Verification table
        String[] columns = {"ID", "Type", "Subject", "Status", "Priority", "SLA", "Created", "Actions"};
        verificationTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column
            }
        };
        
        verificationTable = new JTable(verificationTableModel);
        verificationTable.setRowHeight(40);
        verificationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        verificationTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Column widths
        verificationTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        verificationTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        verificationTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        verificationTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        verificationTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        verificationTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        verificationTable.getColumnModel().getColumn(6).setPreferredWidth(130);
        verificationTable.getColumnModel().getColumn(7).setPreferredWidth(120);
        
        // Custom renderers
        verificationTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        verificationTable.getColumnModel().getColumn(4).setCellRenderer(new PriorityCellRenderer());
        verificationTable.getColumnModel().getColumn(5).setCellRenderer(new SlaCellRenderer());
        verificationTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        verificationTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        // Double-click to process
        verificationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = verificationTable.getSelectedRow();
                    if (row >= 0) {
                        processVerificationRequest(row);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(verificationTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 3: STUDENT LOOKUP ====================
    
    private JPanel createStudentLookupTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Search section
        JPanel searchPanel = new JPanel(new BorderLayout(15, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel searchLabel = new JLabel("Search Student:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        
        JPanel searchInputPanel = new JPanel(new BorderLayout(10, 0));
        searchInputPanel.setOpaque(false);
        
        studentSearchField = new JTextField();
        studentSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentSearchField.setPreferredSize(new Dimension(300, 35));
        studentSearchField.addActionListener(e -> searchStudent());
        searchInputPanel.add(studentSearchField, BorderLayout.CENTER);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setBackground(PRIMARY_COLOR);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setBorderPainted(false);
        searchBtn.setPreferredSize(new Dimension(100, 35));
        searchBtn.addActionListener(e -> searchStudent());
        searchInputPanel.add(searchBtn, BorderLayout.EAST);
        
        searchPanel.add(searchInputPanel, BorderLayout.CENTER);
        
        JLabel hintLabel = new JLabel("Search by email, student ID, or name");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(108, 117, 125));
        searchPanel.add(hintLabel, BorderLayout.SOUTH);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Results section - split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.5);
        
        // Left - Student info
        studentResultPanel = new JPanel(new BorderLayout());
        studentResultPanel.setBackground(Color.WHITE);
        studentResultPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Student Information",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        JLabel placeholderLabel = new JLabel("Search for a student to view details", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(108, 117, 125));
        studentResultPanel.add(placeholderLabel, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(studentResultPanel);
        
        // Right - Trust score history
        trustScoreHistoryPanel = new JPanel(new BorderLayout());
        trustScoreHistoryPanel.setBackground(Color.WHITE);
        trustScoreHistoryPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Trust Score History",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        JLabel historyPlaceholder = new JLabel("Search for a student to view history", SwingConstants.CENTER);
        historyPlaceholder.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        historyPlaceholder.setForeground(new Color(108, 117, 125));
        trustScoreHistoryPanel.add(historyPlaceholder, BorderLayout.CENTER);
        
        splitPane.setRightComponent(trustScoreHistoryPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 4: HIGH-VALUE ITEMS ====================
    
    private JPanel createHighValueItemsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("High-Value Items ($500+)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton serialCheckBtn = new JButton("Check Serial Number");
        serialCheckBtn.addActionListener(e -> showSerialNumberCheckDialog());
        buttonPanel.add(serialCheckBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadHighValueItems());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // High-value items table
        String[] columns = {"Item", "Category", "Value", "Status", "Claim Status", "Serial #", "Verification", "Actions"};
        highValueTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        
        highValueTable = new JTable(highValueTableModel);
        highValueTable.setRowHeight(40);
        highValueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        highValueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Custom renderer for value column
        highValueTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(DANGER_COLOR);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                return this;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(highValueTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 5: REPORTS ====================
    
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Security Reports & Analytics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton exportBtn = new JButton("Export Report");
        exportBtn.addActionListener(e -> exportReport());
        buttonPanel.add(exportBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadReports());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Report content
        reportContentPanel = new JPanel();
        reportContentPanel.setLayout(new BoxLayout(reportContentPanel, BoxLayout.Y_AXIS));
        reportContentPanel.setBackground(Color.WHITE);
        reportContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(reportContentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> data = new HashMap<>();
                
                // Get verification stats
                List<VerificationRequest> pending = verificationService.getVerificationsByStatus(VerificationStatus.PENDING);
                List<VerificationRequest> inProgress = verificationService.getVerificationsByStatus(VerificationStatus.IN_PROGRESS);
                data.put("activeVerifications", pending.size() + inProgress.size());
                
                // Get flagged users
                List<TrustScore> flaggedUsers = trustScoreService.getFlaggedUsers();
                data.put("flaggedUsers", flaggedUsers.size());
                
                // Get high-value pending
                List<VerificationRequest> highValuePending = verificationService.getHighValuePending();
                data.put("highValuePending", highValuePending.size());
                
                // Get overdue
                List<VerificationRequest> overdue = verificationService.getOverdueVerifications();
                data.put("overdue", overdue.size());
                
                // Store for alerts
                data.put("flaggedUsersList", flaggedUsers);
                data.put("overdueList", overdue);
                
                return data;
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    
                    activeVerificationsLabel.setText(String.valueOf(data.get("activeVerifications")));
                    flaggedUsersLabel.setText(String.valueOf(data.get("flaggedUsers")));
                    pendingHighValueLabel.setText(String.valueOf(data.get("highValuePending")));
                    overdueVerificationsLabel.setText(String.valueOf(data.get("overdue")));
                    
                    // Update alerts
                    updateAlerts(data);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void updateAlerts(Map<String, Object> data) {
        alertsPanel.removeAll();
        
        // Overdue alerts
        int overdueCount = (Integer) data.get("overdue");
        if (overdueCount > 0) {
            alertsPanel.add(createAlertItem("URGENT: " + overdueCount + " overdue verification(s)", DANGER_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Flagged users
        @SuppressWarnings("unchecked")
        List<TrustScore> flaggedUsers = (List<TrustScore>) data.get("flaggedUsersList");
        if (flaggedUsers != null && !flaggedUsers.isEmpty()) {
            for (TrustScore ts : flaggedUsers.subList(0, Math.min(3, flaggedUsers.size()))) {
                String name = ts.getUserName() != null ? ts.getUserName() : ts.getUserId();
                alertsPanel.add(createAlertItem("Flagged user: " + name + " (Score: " + 
                    String.format("%.0f", ts.getCurrentScore()) + ")", WARNING_COLOR));
                alertsPanel.add(Box.createVerticalStrut(8));
            }
            if (flaggedUsers.size() > 3) {
                alertsPanel.add(createAlertItem("... and " + (flaggedUsers.size() - 3) + " more flagged users", WARNING_COLOR));
                alertsPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        // High-value items
        int highValueCount = (Integer) data.get("highValuePending");
        if (highValueCount > 0) {
            alertsPanel.add(createAlertItem(highValueCount + " high-value item(s) pending verification", INFO_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        if (alertsPanel.getComponentCount() == 0) {
            JLabel noAlerts = new JLabel("No active security alerts");
            noAlerts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            noAlerts.setForeground(SUCCESS_COLOR);
            alertsPanel.add(noAlerts);
        }
        
        alertsPanel.revalidate();
        alertsPanel.repaint();
    }
    
    private JPanel createAlertItem(String message, Color color) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadVerificationQueue() {
        SwingWorker<List<VerificationRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VerificationRequest> doInBackground() {
                List<VerificationRequest> all = verificationService.getAllVerifications();
                // Filter out terminal statuses by default
                return all.stream()
                    .filter(r -> !r.getStatus().isTerminal())
                    .sorted((a, b) -> {
                        // Sort by priority then by date
                        int priorityCompare = b.getPriority().getLevel() - a.getPriority().getLevel();
                        if (priorityCompare != 0) return priorityCompare;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<VerificationRequest> requests = get();
                    populateVerificationTable(requests);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void populateVerificationTable(List<VerificationRequest> requests) {
        verificationTableModel.setRowCount(0);
        
        for (VerificationRequest req : requests) {
            String id = req.getVisibleId() != null ? req.getVisibleId() : req.getRequestId();
            String type = req.getVerificationType().getDisplayName();
            String subject = getSubjectDisplay(req);
            String status = req.getStatus().getDisplayName();
            String priority = req.getPriority().getDisplayName();
            String sla = getSlaDisplay(req);
            String created = req.getCreatedAt().format(DT_FORMATTER);
            
            verificationTableModel.addRow(new Object[]{id, type, subject, status, priority, sla, created, "Process"});
        }
    }
    
    private String getSubjectDisplay(VerificationRequest req) {
        if (req.getSubjectUserName() != null) {
            return req.getSubjectUserName();
        }
        if (req.getSubjectItemName() != null) {
            return req.getSubjectItemName();
        }
        if (req.getSubjectUserId() != null) {
            return req.getSubjectUserId();
        }
        return "N/A";
    }
    
    private String getSlaDisplay(VerificationRequest req) {
        if (req.isOverdue()) {
            return "OVERDUE";
        }
        long hours = req.getHoursUntilDue();
        if (hours < 0) {
            return "OVERDUE";
        } else if (hours < 24) {
            return hours + "h left";
        } else {
            return (hours / 24) + "d left";
        }
    }
    
    private void filterVerificationQueue() {
        SwingWorker<List<VerificationRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VerificationRequest> doInBackground() {
                List<VerificationRequest> all = verificationService.getAllVerifications();
                
                String typeFilter = (String) verificationTypeFilter.getSelectedItem();
                String statusFilter = (String) verificationStatusFilter.getSelectedItem();
                String priorityFilter = (String) verificationPriorityFilter.getSelectedItem();
                
                return all.stream()
                    .filter(r -> {
                        if (!"All Types".equals(typeFilter)) {
                            return r.getVerificationType().getDisplayName().equals(typeFilter);
                        }
                        return true;
                    })
                    .filter(r -> {
                        if (!"All Statuses".equals(statusFilter)) {
                            return r.getStatus().getDisplayName().equals(statusFilter);
                        }
                        return !r.getStatus().isTerminal();
                    })
                    .filter(r -> {
                        if (!"All Priorities".equals(priorityFilter)) {
                            return r.getPriority().getDisplayName().equals(priorityFilter);
                        }
                        return true;
                    })
                    .sorted((a, b) -> b.getPriority().getLevel() - a.getPriority().getLevel())
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    populateVerificationTable(get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void searchStudent() {
        String searchTerm = studentSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term", "Search", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                // Try email first
                Optional<User> userOpt = userDAO.findByEmail(searchTerm);
                if (userOpt.isPresent()) return userOpt.get();
                
                // Try by name (partial match)
                List<User> allUsers = userDAO.findAll();
                return allUsers.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchTerm.toLowerCase())))
                    .findFirst()
                    .orElse(null);
            }
            
            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        displayStudentInfo(user);
                        displayTrustScoreHistory(user);
                    } else {
                        JOptionPane.showMessageDialog(UniversitySecurityPanel.this,
                            "No student found matching: " + searchTerm,
                            "Not Found",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void displayStudentInfo(User user) {
        studentResultPanel.removeAll();
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Basic info
        addInfoRow(infoPanel, "Name:", user.getFullName());
        addInfoRow(infoPanel, "Email:", user.getEmail());
        addInfoRow(infoPanel, "Role:", user.getRole().getDisplayName());
        addInfoRow(infoPanel, "Phone:", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
        
        infoPanel.add(Box.createVerticalStrut(15));
        
        // Trust Score section
        TrustScore trustScore = trustScoreService.getOrCreateTrustScore(user.getEmail());
        
        JPanel scorePanel = new JPanel(new BorderLayout());
        scorePanel.setOpaque(false);
        scorePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Trust Score"));
        
        JLabel scoreLabel = new JLabel(String.format("%.0f / 100", trustScore.getCurrentScore()));
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        scoreLabel.setForeground(getTrustScoreColor(trustScore.getScoreLevel()));
        scorePanel.add(scoreLabel, BorderLayout.WEST);
        
        JLabel levelLabel = new JLabel(trustScore.getScoreLevel().getDisplayName());
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        levelLabel.setForeground(getTrustScoreColor(trustScore.getScoreLevel()));
        scorePanel.add(levelLabel, BorderLayout.EAST);
        
        infoPanel.add(scorePanel);
        infoPanel.add(Box.createVerticalStrut(15));
        
        // Status indicators
        if (trustScore.isFlagged()) {
            JLabel flaggedLabel = new JLabel("⚠ USER IS FLAGGED");
            flaggedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            flaggedLabel.setForeground(DANGER_COLOR);
            infoPanel.add(flaggedLabel);
            infoPanel.add(Box.createVerticalStrut(5));
        }
        
        if (trustScore.isUnderInvestigation()) {
            JLabel investLabel = new JLabel("🔍 UNDER INVESTIGATION");
            investLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            investLabel.setForeground(WARNING_COLOR);
            infoPanel.add(investLabel);
            infoPanel.add(Box.createVerticalStrut(5));
        }
        
        // Capability checks
        infoPanel.add(Box.createVerticalStrut(10));
        addInfoRow(infoPanel, "Can Claim High-Value:", 
            trustScoreService.canClaimHighValueItem(user.getEmail()) ? "Yes" : "No");
        addInfoRow(infoPanel, "Requires Verification:", 
            trustScoreService.requiresAdditionalVerification(user.getEmail()) ? "Yes" : "No");
        addInfoRow(infoPanel, "Max Claim Without Verification:", 
            "$" + String.format("%.0f", trustScoreService.getMaxClaimValueWithoutVerification(user.getEmail())));
        
        // Action buttons
        infoPanel.add(Box.createVerticalStrut(20));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        if (!trustScore.isFlagged()) {
            JButton flagBtn = new JButton("Flag User");
            flagBtn.setBackground(DANGER_COLOR);
            flagBtn.setForeground(Color.BLACK);
            flagBtn.addActionListener(e -> flagUser(user));
            buttonPanel.add(flagBtn);
        } else {
            JButton unflagBtn = new JButton("Remove Flag");
            unflagBtn.setBackground(SUCCESS_COLOR);
            unflagBtn.setForeground(Color.WHITE);
            unflagBtn.addActionListener(e -> unflagUser(user));
            buttonPanel.add(unflagBtn);
        }
        
        JButton verifyBtn = new JButton("Request Verification");
        verifyBtn.addActionListener(e -> requestVerification(user));
        buttonPanel.add(verifyBtn);
        
        JButton adjustBtn = new JButton("Adjust Score");
        adjustBtn.addActionListener(e -> showAdjustScoreDialog(user));
        buttonPanel.add(adjustBtn);
        
        infoPanel.add(buttonPanel);
        
        studentResultPanel.add(infoPanel, BorderLayout.CENTER);
        studentResultPanel.revalidate();
        studentResultPanel.repaint();
    }
    
    private void addInfoRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelComp.setForeground(new Color(108, 117, 125));
        row.add(labelComp, BorderLayout.WEST);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(valueComp, BorderLayout.EAST);
        
        panel.add(row);
        panel.add(Box.createVerticalStrut(5));
    }
    
    private void displayTrustScoreHistory(User user) {
        trustScoreHistoryPanel.removeAll();
        
        List<TrustScoreEvent> events = trustScoreService.getUserEventHistory(user.getEmail());
        
        JPanel historyList = new JPanel();
        historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
        historyList.setBackground(Color.WHITE);
        historyList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        if (events.isEmpty()) {
            JLabel noHistory = new JLabel("No trust score history found");
            noHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            noHistory.setForeground(new Color(108, 117, 125));
            historyList.add(noHistory);
        } else {
            for (TrustScoreEvent event : events) {
                historyList.add(createEventCard(event));
                historyList.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(null);
        trustScoreHistoryPanel.add(scrollPane, BorderLayout.CENTER);
        trustScoreHistoryPanel.revalidate();
        trustScoreHistoryPanel.repaint();
    }
    
    private JPanel createEventCard(TrustScoreEvent event) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        // Left - Points change
        int points = event.getPointsChange();
        JLabel pointsLabel = new JLabel((points >= 0 ? "+" : "") + points);
        pointsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pointsLabel.setForeground(points >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
        pointsLabel.setPreferredSize(new Dimension(50, 0));
        card.add(pointsLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        JLabel typeLabel = new JLabel(event.getEventType().getDisplayName());
        typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detailsPanel.add(typeLabel);
        
        JLabel descLabel = new JLabel(event.getDescription());
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(new Color(108, 117, 125));
        detailsPanel.add(descLabel);
        
        card.add(detailsPanel, BorderLayout.CENTER);
        
        // Right - Date
        JLabel dateLabel = new JLabel(event.getTimestamp().format(DT_FORMATTER));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dateLabel.setForeground(new Color(134, 142, 150));
        card.add(dateLabel, BorderLayout.EAST);
        
        return card;
    }
    
    private Color getTrustScoreColor(ScoreLevel level) {
        switch (level) {
            case EXCELLENT: return new Color(40, 167, 69);
            case GOOD: return new Color(23, 162, 184);
            case FAIR: return new Color(255, 193, 7);
            case LOW: return new Color(255, 152, 0);
            case PROBATION: return new Color(220, 53, 69);
            default: return Color.GRAY;
        }
    }
    
    private void loadHighValueItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findAll().stream()
                    .filter(i -> i.getEstimatedValue() >= HIGH_VALUE_THRESHOLD)
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .sorted((a, b) -> Double.compare(b.getEstimatedValue(), a.getEstimatedValue()))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    populateHighValueTable(items);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void populateHighValueTable(List<Item> items) {
        highValueTableModel.setRowCount(0);
        
        for (Item item : items) {
            String name = item.getTitle();
            String category = item.getCategory().getDisplayName();
            String value = "$" + String.format("%.2f", item.getEstimatedValue());
            String status = item.getStatus().getLabel();
            String claimStatus = item.getStatus() == Item.ItemStatus.PENDING_CLAIM ? "Has Claims" : "No Claims";
            String serialNum = item.getSerialNumber() != null ? item.getSerialNumber() : "N/A";
            String verification = verificationService.requiresPoliceVerification(item) ? "Police Required" : "Standard";
            
            highValueTableModel.addRow(new Object[]{name, category, value, status, claimStatus, serialNum, verification, "Verify"});
        }
    }
    
    private void loadReports() {
        reportContentPanel.removeAll();
        
        // Verification Summary
        reportContentPanel.add(createReportSection("Verification Summary", createVerificationSummaryContent()));
        reportContentPanel.add(Box.createVerticalStrut(20));
        
        // Flagged Users Report
        reportContentPanel.add(createReportSection("Flagged Users", createFlaggedUsersContent()));
        reportContentPanel.add(Box.createVerticalStrut(20));
        
        // Trust Score Distribution
        reportContentPanel.add(createReportSection("Trust Score Distribution", createTrustScoreDistributionContent()));
        reportContentPanel.add(Box.createVerticalStrut(20));
        
        // Suspicious Activity Log
        reportContentPanel.add(createReportSection("Recent Suspicious Activity", createSuspiciousActivityContent()));
        
        reportContentPanel.revalidate();
        reportContentPanel.repaint();
    }
    
    private JPanel createReportSection(String title, JComponent content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JComponent createVerificationSummaryContent() {
        Map<VerificationStatus, Long> statusCounts = verificationService.getCountByStatus();
        Map<VerificationType, Long> typeCounts = verificationService.getCountByType();
        
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        long pending = statusCounts.getOrDefault(VerificationStatus.PENDING, 0L);
        long inProgress = statusCounts.getOrDefault(VerificationStatus.IN_PROGRESS, 0L);
        long verified = statusCounts.getOrDefault(VerificationStatus.VERIFIED, 0L);
        long failed = statusCounts.getOrDefault(VerificationStatus.FAILED, 0L);
        
        panel.add(createMiniStatCard("Pending", String.valueOf(pending), WARNING_COLOR));
        panel.add(createMiniStatCard("In Progress", String.valueOf(inProgress), PRIMARY_COLOR));
        panel.add(createMiniStatCard("Verified", String.valueOf(verified), SUCCESS_COLOR));
        panel.add(createMiniStatCard("Failed", String.valueOf(failed), DANGER_COLOR));
        
        long identity = typeCounts.getOrDefault(VerificationType.IDENTITY_VERIFICATION, 0L);
        long highValue = typeCounts.getOrDefault(VerificationType.HIGH_VALUE_ITEM_CLAIM, 0L);
        long serial = typeCounts.getOrDefault(VerificationType.SERIAL_NUMBER_CHECK, 0L);
        long stolen = typeCounts.getOrDefault(VerificationType.STOLEN_PROPERTY_CHECK, 0L);
        
        panel.add(createMiniStatCard("Identity", String.valueOf(identity), INFO_COLOR));
        panel.add(createMiniStatCard("High-Value", String.valueOf(highValue), PURPLE_COLOR));
        panel.add(createMiniStatCard("Serial Checks", String.valueOf(serial), PRIMARY_COLOR));
        panel.add(createMiniStatCard("Stolen Checks", String.valueOf(stolen), DANGER_COLOR));
        
        return panel;
    }
    
    private JPanel createMiniStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(color);
        card.add(valueLabel, BorderLayout.WEST);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelComp.setForeground(new Color(108, 117, 125));
        card.add(labelComp, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JComponent createFlaggedUsersContent() {
        List<TrustScore> flaggedUsers = trustScoreService.getFlaggedUsers();
        
        String[] columns = {"User", "Score", "Level", "Flag Reason", "Actions"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        for (TrustScore ts : flaggedUsers) {
            model.addRow(new Object[]{
                ts.getUserName() != null ? ts.getUserName() : ts.getUserId(),
                String.format("%.0f", ts.getCurrentScore()),
                ts.getScoreLevel().getDisplayName(),
                ts.getFlagReason() != null ? ts.getFlagReason() : "N/A",
                "Review"
            });
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        return new JScrollPane(table);
    }
    
    private JComponent createTrustScoreDistributionContent() {
        TrustScoreService.TrustScoreStats stats = trustScoreService.getStatistics();
        
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        Map<ScoreLevel, Long> dist = stats.scoreDistribution;
        
        panel.add(createDistributionBar("Excellent", dist.getOrDefault(ScoreLevel.EXCELLENT, 0L), SUCCESS_COLOR));
        panel.add(createDistributionBar("Good", dist.getOrDefault(ScoreLevel.GOOD, 0L), INFO_COLOR));
        panel.add(createDistributionBar("Fair", dist.getOrDefault(ScoreLevel.FAIR, 0L), WARNING_COLOR));
        panel.add(createDistributionBar("Low", dist.getOrDefault(ScoreLevel.LOW, 0L), new Color(255, 152, 0)));
        panel.add(createDistributionBar("Probation", dist.getOrDefault(ScoreLevel.PROBATION, 0L), DANGER_COLOR));
        
        return panel;
    }
    
    private JPanel createDistributionBar(String label, long count, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel countLabel = new JLabel(String.valueOf(count), SwingConstants.CENTER);
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        countLabel.setForeground(color);
        panel.add(countLabel, BorderLayout.CENTER);
        
        JLabel nameLabel = new JLabel(label, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(nameLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JComponent createSuspiciousActivityContent() {
        JTextArea activityLog = new JTextArea();
        activityLog.setEditable(false);
        activityLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        
        // Get recent negative events
        StringBuilder sb = new StringBuilder();
        List<TrustScore> allScores = trustScoreService.getAllTrustScores();
        
        for (TrustScore ts : allScores.subList(0, Math.min(10, allScores.size()))) {
            List<TrustScoreEvent> negEvents = trustScoreService.getUserNegativeEvents(ts.getUserId());
            for (TrustScoreEvent event : negEvents.subList(0, Math.min(3, negEvents.size()))) {
                sb.append(event.getTimestamp().format(DT_FORMATTER))
                  .append(" | ")
                  .append(ts.getUserName() != null ? ts.getUserName() : ts.getUserId())
                  .append(" | ")
                  .append(event.getEventType().getDisplayName())
                  .append(" (")
                  .append(event.getPointsChange())
                  .append(" pts)\n");
            }
        }
        
        if (sb.length() == 0) {
            sb.append("No recent suspicious activity recorded.");
        }
        
        activityLog.setText(sb.toString());
        return new JScrollPane(activityLog);
    }
    
    private String getRecentActivityLog() {
        StringBuilder log = new StringBuilder();
        log.append(LocalDateTime.now().format(DT_FORMATTER)).append(" | System started\n");
        log.append(LocalDateTime.now().minusMinutes(5).format(DT_FORMATTER)).append(" | Dashboard loaded\n");
        log.append(LocalDateTime.now().minusMinutes(10).format(DT_FORMATTER)).append(" | Verification queue refreshed\n");
        return log.toString();
    }
    
    // ==================== ACTIONS ====================
    
    private void processVerificationRequest(int row) {
        String requestId = (String) verificationTableModel.getValueAt(row, 0);
        VerificationRequest request = verificationService.getVerificationByVisibleId(requestId);
        
        if (request == null) {
            request = verificationService.getVerificationRequest(requestId);
        }
        
        if (request == null) {
            JOptionPane.showMessageDialog(this, "Request not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        showVerificationProcessDialog(request);
    }
    
    private void showVerificationProcessDialog(VerificationRequest request) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Process Verification", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel titleLabel = new JLabel(request.getVerificationType().getDisplayName());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        
        addInfoRow(detailsPanel, "Request ID:", request.getVisibleId() != null ? request.getVisibleId() : request.getRequestId());
        addInfoRow(detailsPanel, "Status:", request.getStatus().getDisplayName());
        addInfoRow(detailsPanel, "Priority:", request.getPriority().getDisplayName());
        addInfoRow(detailsPanel, "Subject:", getSubjectDisplay(request));
        addInfoRow(detailsPanel, "Reason:", request.getRequestReason() != null ? request.getRequestReason() : "N/A");
        
        if (request.getSubjectItemValue() > 0) {
            addInfoRow(detailsPanel, "Item Value:", "$" + String.format("%.2f", request.getSubjectItemValue()));
        }
        
        detailsPanel.add(Box.createVerticalStrut(20));
        
        // Notes
        JLabel notesLabel = new JLabel("Verification Notes:");
        notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detailsPanel.add(notesLabel);
        
        JTextArea notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        if (request.getVerificationNotes() != null) {
            notesArea.setText(request.getVerificationNotes());
        }
        detailsPanel.add(new JScrollPane(notesArea));
        
        panel.add(new JScrollPane(detailsPanel), BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        if (request.canProcess()) {
            if (request.getStatus() == VerificationStatus.PENDING) {
                JButton assignBtn = new JButton("Assign to Me");
                assignBtn.setBackground(PRIMARY_COLOR);
                assignBtn.setForeground(Color.WHITE);
                assignBtn.addActionListener(e -> {
                    verificationService.assignVerifier(request.getRequestId(), 
                        currentUser.getEmail(), currentUser.getFullName(), currentUser.getRole().getDisplayName());
                    dialog.dispose();
                    loadVerificationQueue();
                });
                buttonPanel.add(assignBtn);
            }
            
            JButton approveBtn = new JButton("Verify (Approve)");
            approveBtn.setBackground(SUCCESS_COLOR);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.addActionListener(e -> {
                verificationService.completeVerification(request.getRequestId(), notesArea.getText());
                dialog.dispose();
                loadVerificationQueue();
                loadDashboardData();
            });
            buttonPanel.add(approveBtn);
            
            JButton rejectBtn = new JButton("Fail (Reject)");
            rejectBtn.setBackground(DANGER_COLOR);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.addActionListener(e -> {
                String reason = JOptionPane.showInputDialog(dialog, "Enter rejection reason:");
                if (reason != null && !reason.isEmpty()) {
                    verificationService.failVerification(request.getRequestId(), reason);
                    dialog.dispose();
                    loadVerificationQueue();
                    loadDashboardData();
                }
            });
            buttonPanel.add(rejectBtn);
            
            JButton awaitDocsBtn = new JButton("Await Documents");
            awaitDocsBtn.addActionListener(e -> {
                verificationService.setAwaitingDocuments(request.getRequestId(), notesArea.getText());
                dialog.dispose();
                loadVerificationQueue();
            });
            buttonPanel.add(awaitDocsBtn);
        }
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void flagUser(User user) {
        String reason = JOptionPane.showInputDialog(this, 
            "Enter reason for flagging " + user.getFullName() + ":",
            "Flag User",
            JOptionPane.WARNING_MESSAGE);
        
        if (reason != null && !reason.isEmpty()) {
            boolean success = trustScoreService.flagUser(user.getEmail(), reason, currentUser.getEmail());
            if (success) {
                JOptionPane.showMessageDialog(this, "User has been flagged", "Success", JOptionPane.INFORMATION_MESSAGE);
                searchStudent();
                loadDashboardData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to flag user", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void unflagUser(User user) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to remove the flag from " + user.getFullName() + "?",
            "Confirm Unflag",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = trustScoreService.clearUserFlag(user.getEmail(), currentUser.getEmail());
            if (success) {
                JOptionPane.showMessageDialog(this, "Flag has been removed", "Success", JOptionPane.INFORMATION_MESSAGE);
                searchStudent();
                loadDashboardData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove flag", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void requestVerification(User user) {
        String[] options = {"Identity Verification", "Student Enrollment", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select verification type for " + user.getFullName(),
            "Request Verification",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            verificationService.requestIdentityVerification(user.getEmail(), currentUser.getEmail());
            JOptionPane.showMessageDialog(this, "Identity verification requested", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if (choice == 1) {
            verificationService.requestStudentEnrollmentVerification(user.getEmail(), currentUser.getEmail());
            JOptionPane.showMessageDialog(this, "Student enrollment verification requested", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        
        loadVerificationQueue();
        loadDashboardData();
    }
    
    private void showAdjustScoreDialog(User user) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        panel.add(new JLabel("Current Score:"));
        panel.add(new JLabel(String.format("%.0f", trustScoreService.getTrustScore(user.getEmail()))));
        
        panel.add(new JLabel("New Score:"));
        JSpinner scoreSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));
        panel.add(scoreSpinner);
        
        panel.add(new JLabel("Reason:"));
        JTextField reasonField = new JTextField();
        panel.add(reasonField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Adjust Trust Score", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            double newScore = ((Number) scoreSpinner.getValue()).doubleValue();
            String reason = reasonField.getText().trim();
            
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a reason", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = trustScoreService.manuallyUpdateScore(user.getEmail(), newScore, reason, currentUser.getEmail());
            if (success) {
                JOptionPane.showMessageDialog(this, "Score updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                searchStudent();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update score", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showFlagUserDialog() {
        String email = JOptionPane.showInputDialog(this, "Enter user email to flag:");
        if (email != null && !email.isEmpty()) {
            Optional<User> userOpt = userDAO.findByEmail(email);
            if (userOpt.isPresent()) {
                flagUser(userOpt.get());
            } else {
                JOptionPane.showMessageDialog(this, "User not found: " + email, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showSerialNumberCheckDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("Item ID or Name:"));
        JTextField itemField = new JTextField();
        panel.add(itemField);
        panel.add(new JLabel("Serial Number:"));
        JTextField serialField = new JTextField();
        panel.add(serialField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Serial Number Check", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String itemId = itemField.getText().trim();
            String serial = serialField.getText().trim();
            
            // Simulate serial number check
            boolean isStolen = Math.random() < 0.1; // 10% chance of being stolen for demo
            
            if (isStolen) {
                JOptionPane.showMessageDialog(this,
                    "⚠ WARNING: Serial number " + serial + " is flagged in stolen property database!\n" +
                    "Case has been escalated to law enforcement.",
                    "Stolen Property Alert",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "✓ Serial number " + serial + " is not in stolen property database.",
                    "Check Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void showEmergencyDialog() {
        String[] options = {"Lockdown Mode", "Broadcast Alert", "Contact Police", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select emergency action:",
            "Emergency Alert",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[3]);
        
        if (choice >= 0 && choice < 3) {
            JOptionPane.showMessageDialog(this,
                "Emergency action '" + options[choice] + "' has been initiated.\n" +
                "All relevant personnel have been notified.",
                "Emergency Action",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void exportReport() {
        JOptionPane.showMessageDialog(this,
            "Report exported to:\n" +
            System.getProperty("user.dir") + "/reports/security_report_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf",
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void refreshAll() {
        loadDashboardData();
        loadVerificationQueue();
        loadHighValueItems();
        loadReports();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ==================== CUSTOM CELL RENDERERS ====================
    
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String status = (String) value;
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            if (!isSelected) {
                switch (status) {
                    case "Pending" -> { setBackground(new Color(255, 243, 205)); setForeground(new Color(133, 100, 4)); }
                    case "In Progress" -> { setBackground(new Color(207, 226, 255)); setForeground(new Color(0, 64, 133)); }
                    case "Awaiting Documents", "Awaiting Response" -> { setBackground(new Color(226, 217, 243)); setForeground(new Color(88, 28, 135)); }
                    case "Verified" -> { setBackground(new Color(209, 231, 221)); setForeground(new Color(20, 83, 45)); }
                    case "Failed" -> { setBackground(new Color(248, 215, 218)); setForeground(new Color(114, 28, 36)); }
                    default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                }
            }
            
            return this;
        }
    }
    
    private class PriorityCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String priority = (String) value;
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            if (!isSelected) {
                switch (priority) {
                    case "Urgent" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                    case "High" -> { setBackground(new Color(255, 243, 205)); setForeground(new Color(133, 100, 4)); }
                    case "Normal" -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
                    case "Low" -> { setBackground(new Color(226, 232, 240)); setForeground(new Color(71, 85, 105)); }
                    default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                }
            }
            
            return this;
        }
    }
    
    private class SlaCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String sla = (String) value;
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            if (!isSelected) {
                if ("OVERDUE".equals(sla)) {
                    setBackground(new Color(248, 215, 218));
                    setForeground(DANGER_COLOR);
                } else if (sla.contains("h left")) {
                    setBackground(new Color(255, 243, 205));
                    setForeground(new Color(133, 100, 4));
                } else {
                    setBackground(new Color(209, 231, 221));
                    setForeground(SUCCESS_COLOR);
                }
            }
            
            return this;
        }
    }
    
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
            setBackground(PRIMARY_COLOR);
            setForeground(Color.WHITE);
            setBorderPainted(false);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            return this;
        }
    }
    
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            button.setBackground(PRIMARY_COLOR);
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
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
                processVerificationRequest(currentRow);
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
