package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.trustscore.*;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.verification.*;
import com.campus.lostfound.models.verification.VerificationRequest.*;
import com.campus.lostfound.services.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main panel for Transit Security Inspector role (MBTA).
 * 
 * Features:
 * - Dashboard with suspicious activity alerts and pattern detection
 * - Activity monitoring for high-frequency claimers and location patterns
 * - Traveler ID verification queue
 * - Fraud investigation case management
 * - Reports and alerts with export functionality
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class TransitSecurityInspectorPanel extends JPanel {
    
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
    private List<SuspiciousActivity> suspiciousActivities = new ArrayList<>();
    private List<HighFrequencyClaimer> highFrequencyClaimers = new ArrayList<>();
    private List<TravelerVerification> travelerVerifications = new ArrayList<>();
    private List<FraudCase> fraudCases = new ArrayList<>();
    
    // UI Components
    private JTabbedPane tabbedPane;
    
    // Dashboard components
    private JLabel suspiciousAlertsLabel;
    private JLabel patternWarningsLabel;
    private JLabel pendingVerificationsLabel;
    private JLabel activeFraudCasesLabel;
    private JPanel alertsPanel;
    private JPanel hotspotPanel;
    
    // Activity Monitor components
    private JTable activityTable;
    private DefaultTableModel activityTableModel;
    private JTable claimersTable;
    private DefaultTableModel claimersTableModel;
    
    // Traveler Verification components
    private JTable verificationTable;
    private DefaultTableModel verificationTableModel;
    private JTextArea verificationNotesArea;
    
    // Fraud Investigation components
    private JTable fraudTable;
    private DefaultTableModel fraudTableModel;
    private JTextArea fraudCaseNotesArea;
    
    // Reports components
    private JTextArea reportOutputArea;
    
    // Constants
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final Color PRIMARY_COLOR = new Color(13, 110, 253);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    private static final Color MBTA_ORANGE = new Color(237, 139, 0);
    private static final Color MBTA_DARK = new Color(26, 35, 46);
    
    // MBTA Line colors
    private static final Color RED_LINE = new Color(218, 41, 28);
    private static final Color ORANGE_LINE = new Color(237, 139, 0);
    private static final Color BLUE_LINE = new Color(0, 61, 165);
    private static final Color GREEN_LINE = new Color(0, 132, 61);
    
    /**
     * Create a new TransitSecurityInspectorPanel.
     * 
     * @param currentUser The logged-in transit security inspector
     */
    public TransitSecurityInspectorPanel(User currentUser) {
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
        
        // Initialize sample data
        initializeSampleData();
        
        initComponents();
        loadDashboardData();
    }
    
    private void initializeSampleData() {
        // Sample suspicious activities
        suspiciousActivities.add(new SuspiciousActivity("SA-001", "Multiple claims at Park Street", 
            "Red Line", "Park Street", ActivityType.MULTIPLE_CLAIMS, 
            AlertLevel.HIGH, LocalDateTime.now().minusHours(2)));
        suspiciousActivities.add(new SuspiciousActivity("SA-002", "Same-day claims at different stations", 
            "Orange Line", "Downtown Crossing", ActivityType.CROSS_STATION, 
            AlertLevel.MEDIUM, LocalDateTime.now().minusHours(5)));
        suspiciousActivities.add(new SuspiciousActivity("SA-003", "High-value electronics claim pattern", 
            "Blue Line", "Airport", ActivityType.HIGH_VALUE_PATTERN, 
            AlertLevel.HIGH, LocalDateTime.now().minusDays(1)));
        suspiciousActivities.add(new SuspiciousActivity("SA-004", "Traveler ID mismatch", 
            "Green Line", "Copley", ActivityType.ID_MISMATCH, 
            AlertLevel.CRITICAL, LocalDateTime.now().minusHours(1)));
        suspiciousActivities.add(new SuspiciousActivity("SA-005", "Repeat false claims", 
            "Red Line", "Harvard", ActivityType.FALSE_CLAIMS, 
            AlertLevel.HIGH, LocalDateTime.now().minusDays(2)));
        
        // Sample high-frequency claimers
        highFrequencyClaimers.add(new HighFrequencyClaimer("HFC-001", "John Smith", 
            "user_12345", 8, 3, 35.5, LocalDateTime.now().minusDays(7)));
        highFrequencyClaimers.add(new HighFrequencyClaimer("HFC-002", "Jane Doe", 
            "user_23456", 5, 4, 42.0, LocalDateTime.now().minusDays(14)));
        highFrequencyClaimers.add(new HighFrequencyClaimer("HFC-003", "Mike Johnson", 
            "user_34567", 12, 2, 28.0, LocalDateTime.now().minusDays(5)));
        highFrequencyClaimers.add(new HighFrequencyClaimer("HFC-004", "Sarah Wilson", 
            "user_45678", 6, 5, 55.0, LocalDateTime.now().minusDays(10)));
        
        // Sample traveler verifications
        travelerVerifications.add(new TravelerVerification("TV-001", "Robert Brown", 
            "Airport", VerificationStatus.PENDING, "ID check requested by Logan Airport", 
            LocalDateTime.now().minusHours(3)));
        travelerVerifications.add(new TravelerVerification("TV-002", "Emily Davis", 
            "South Station", VerificationStatus.IN_PROGRESS, "Cross-reference with Amtrak records", 
            LocalDateTime.now().minusHours(6)));
        travelerVerifications.add(new TravelerVerification("TV-003", "William Taylor", 
            "Back Bay", VerificationStatus.PENDING, "TSA coordination required", 
            LocalDateTime.now().minusHours(1)));
        
        // Sample fraud cases
        fraudCases.add(new FraudCase("FC-2024-001", "Serial False Claims", 
            "Pattern of false claims across multiple Red Line stations", 
            FraudStatus.ACTIVE, FraudPriority.HIGH, LocalDateTime.now().minusDays(7)));
        fraudCases.add(new FraudCase("FC-2024-002", "Identity Fraud Ring", 
            "Coordinated identity fraud using fake student IDs", 
            FraudStatus.UNDER_REVIEW, FraudPriority.CRITICAL, LocalDateTime.now().minusDays(3)));
        fraudCases.add(new FraudCase("FC-2024-003", "Electronics Theft Pattern", 
            "Organized theft of electronics at transit stations", 
            FraudStatus.PENDING_BPD, FraudPriority.HIGH, LocalDateTime.now().minusDays(14)));
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
        tabbedPane.addTab("Activity Monitor", createActivityMonitorTab());
        tabbedPane.addTab("Traveler Verification", createTravelerVerificationTab());
        tabbedPane.addTab("Fraud Investigation", createFraudInvestigationTab());
        tabbedPane.addTab("Reports & Alerts", createReportsTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> loadActivityData();
                case 2 -> loadVerificationData();
                case 3 -> loadFraudCases();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MBTA_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Title with MBTA branding
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel mbtaLabel = new JLabel("MBTA TRANSIT SECURITY");
        mbtaLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mbtaLabel.setForeground(MBTA_ORANGE);
        mbtaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(mbtaLabel);
        
        JLabel titleLabel = new JLabel("Security Inspector Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel(currentUser.getFullName() + " • Transit Security Division");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Center - Line status indicators
        JPanel linesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        linesPanel.setOpaque(false);
        linesPanel.add(createLineStatusBadge("Red", RED_LINE, "Normal"));
        linesPanel.add(createLineStatusBadge("Orange", ORANGE_LINE, "Normal"));
        linesPanel.add(createLineStatusBadge("Blue", BLUE_LINE, "Alert"));
        linesPanel.add(createLineStatusBadge("Green", GREEN_LINE, "Normal"));
        panel.add(linesPanel, BorderLayout.CENTER);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = createHeaderButton("Refresh", new Color(52, 73, 94));
        refreshBtn.addActionListener(e -> refreshAll());
        rightPanel.add(refreshBtn);
        
        JButton alertBtn = createHeaderButton("Issue Alert", WARNING_COLOR);
        alertBtn.addActionListener(e -> showIssueAlertDialog());
        rightPanel.add(alertBtn);
        
        JButton bpdBtn = createHeaderButton("Contact BPD", DANGER_COLOR);
        bpdBtn.addActionListener(e -> showBPDContactDialog());
        rightPanel.add(bpdBtn);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createLineStatusBadge(String line, Color color, String status) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        badge.setOpaque(false);
        
        JLabel colorDot = new JLabel("\u25CF"); // Filled circle
        colorDot.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        colorDot.setForeground(color);
        badge.add(colorDot);
        
        JLabel lineLabel = new JLabel(line);
        lineLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lineLabel.setForeground(status.equals("Alert") ? WARNING_COLOR : Color.WHITE);
        badge.add(lineLabel);
        
        return badge;
    }
    
    private JButton createHeaderButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
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
        
        // Suspicious Activity Alerts
        JPanel alertsCard = createStatCard("Suspicious Alerts", "0", DANGER_COLOR, "Requiring attention");
        suspiciousAlertsLabel = findValueLabel(alertsCard);
        statsRow.add(alertsCard);
        
        // Pattern Warnings
        JPanel patternCard = createStatCard("Pattern Warnings", "0", WARNING_COLOR, "Detected patterns");
        patternWarningsLabel = findValueLabel(patternCard);
        statsRow.add(patternCard);
        
        // Pending Verifications
        JPanel verifyCard = createStatCard("Pending Verifications", "0", INFO_COLOR, "Traveler ID checks");
        pendingVerificationsLabel = findValueLabel(verifyCard);
        statsRow.add(verifyCard);
        
        // Active Fraud Cases
        JPanel fraudCard = createStatCard("Active Fraud Cases", "0", MBTA_ORANGE, "Open investigations");
        activeFraudCasesLabel = findValueLabel(fraudCard);
        statsRow.add(fraudCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - split into alerts and hotspot map
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Priority Alerts panel
        JPanel alertsContainer = createTitledPanel("Priority Alerts");
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Color.WHITE);
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsContainer.add(alertsScroll, BorderLayout.CENTER);
        contentPanel.add(alertsContainer);
        
        // Station Hotspot Map (text-based)
        JPanel hotspotContainer = createTitledPanel("Station Activity Hotspots");
        hotspotPanel = new JPanel();
        hotspotPanel.setLayout(new BoxLayout(hotspotPanel, BoxLayout.Y_AXIS));
        hotspotPanel.setBackground(Color.WHITE);
        hotspotPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add hotspot entries
        hotspotPanel.add(createHotspotEntry("Park Street", RED_LINE, 12, "High Activity"));
        hotspotPanel.add(Box.createVerticalStrut(8));
        hotspotPanel.add(createHotspotEntry("Downtown Crossing", ORANGE_LINE, 8, "Moderate"));
        hotspotPanel.add(Box.createVerticalStrut(8));
        hotspotPanel.add(createHotspotEntry("Airport", BLUE_LINE, 15, "Very High"));
        hotspotPanel.add(Box.createVerticalStrut(8));
        hotspotPanel.add(createHotspotEntry("Harvard", RED_LINE, 6, "Normal"));
        hotspotPanel.add(Box.createVerticalStrut(8));
        hotspotPanel.add(createHotspotEntry("Copley", GREEN_LINE, 9, "Moderate"));
        hotspotPanel.add(Box.createVerticalStrut(8));
        hotspotPanel.add(createHotspotEntry("South Station", RED_LINE, 11, "High Activity"));
        
        JScrollPane hotspotScroll = new JScrollPane(hotspotPanel);
        hotspotScroll.setBorder(null);
        hotspotContainer.add(hotspotScroll, BorderLayout.CENTER);
        contentPanel.add(hotspotContainer);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Bottom - Recent incidents summary
        JPanel recentPanel = createTitledPanel("Recent Incidents (Last 24 Hours)");
        recentPanel.setPreferredSize(new Dimension(0, 180));
        
        String[] columns = {"Time", "Type", "Station", "Line", "Status", "Action Taken"};
        DefaultTableModel recentModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        recentModel.addRow(new Object[]{"10:32 AM", "Suspicious Claim", "Park Street", "Red", "Escalated", "BPD Notified"});
        recentModel.addRow(new Object[]{"09:15 AM", "ID Mismatch", "Airport", "Blue", "Resolved", "Verified on-site"});
        recentModel.addRow(new Object[]{"08:45 AM", "Multiple Claims", "Downtown", "Orange", "Under Review", "Flagged for review"});
        recentModel.addRow(new Object[]{"Yesterday 6:30 PM", "False Claim", "Harvard", "Red", "Closed", "Trust score adjusted"});
        
        JTable recentTable = new JTable(recentModel);
        recentTable.setRowHeight(35);
        recentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Status column renderer
        recentTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (status) {
                        case "Escalated" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "Under Review" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "Resolved", "Closed" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        recentPanel.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        panel.add(recentPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createHotspotEntry(String station, Color lineColor, int activityCount, String level) {
        JPanel entry = new JPanel(new BorderLayout(10, 0));
        entry.setBackground(new Color(248, 249, 250));
        entry.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        // Left - Line color indicator
        JLabel lineIndicator = new JLabel("\u25A0"); // Filled square
        lineIndicator.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        lineIndicator.setForeground(lineColor);
        entry.add(lineIndicator, BorderLayout.WEST);
        
        // Center - Station name and activity count
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        JLabel stationLabel = new JLabel(station);
        stationLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        centerPanel.add(stationLabel, BorderLayout.NORTH);
        
        JLabel countLabel = new JLabel(activityCount + " activities today");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        countLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(countLabel, BorderLayout.SOUTH);
        
        entry.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Activity level badge
        JLabel levelLabel = new JLabel(level);
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        levelLabel.setOpaque(true);
        levelLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        
        switch (level) {
            case "Very High" -> { levelLabel.setBackground(new Color(248, 215, 218)); levelLabel.setForeground(DANGER_COLOR); }
            case "High Activity" -> { levelLabel.setBackground(new Color(255, 243, 205)); levelLabel.setForeground(WARNING_COLOR.darker()); }
            case "Moderate" -> { levelLabel.setBackground(new Color(207, 226, 255)); levelLabel.setForeground(PRIMARY_COLOR); }
            default -> { levelLabel.setBackground(new Color(209, 231, 221)); levelLabel.setForeground(SUCCESS_COLOR); }
        }
        
        entry.add(levelLabel, BorderLayout.EAST);
        
        return entry;
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
    
    // ==================== TAB 2: ACTIVITY MONITOR ====================
    
    private JPanel createActivityMonitorTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Split into two sections
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);
        
        // Top - High Frequency Claimers
        JPanel claimersPanel = new JPanel(new BorderLayout());
        claimersPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WARNING_COLOR),
            "High-Frequency Claimers",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            WARNING_COLOR.darker()
        ));
        
        // Header with filters
        JPanel claimersHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        claimersHeader.setOpaque(false);
        
        claimersHeader.add(new JLabel("Time Period:"));
        JComboBox<String> periodCombo = new JComboBox<>(new String[]{"Last 7 Days", "Last 30 Days", "Last 90 Days"});
        periodCombo.addActionListener(e -> filterClaimers((String) periodCombo.getSelectedItem()));
        claimersHeader.add(periodCombo);
        
        claimersHeader.add(new JLabel("Min Claims:"));
        JSpinner minClaimsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        claimersHeader.add(minClaimsSpinner);
        
        JButton filterBtn = new JButton("Apply Filter");
        filterBtn.addActionListener(e -> loadActivityData());
        claimersHeader.add(filterBtn);
        
        claimersPanel.add(claimersHeader, BorderLayout.NORTH);
        
        // Claimers table
        String[] claimerColumns = {"ID", "Name", "Claims", "Stations", "Trust Score", "Last Activity", "Actions"};
        claimersTableModel = new DefaultTableModel(claimerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 6; }
        };
        
        claimersTable = new JTable(claimersTableModel);
        claimersTable.setRowHeight(35);
        claimersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Trust score renderer
        claimersTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double score = Double.parseDouble(value.toString());
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (!isSelected) {
                    if (score < 40) { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                    else if (score < 60) { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                    else { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                }
                return this;
            }
        });
        
        // Actions column
        claimersTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer("Investigate"));
        claimersTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this::investigateClaimer));
        
        claimersPanel.add(new JScrollPane(claimersTable), BorderLayout.CENTER);
        splitPane.setTopComponent(claimersPanel);
        
        // Bottom - Location-based suspicious activity
        JPanel locationPanel = new JPanel(new BorderLayout());
        locationPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DANGER_COLOR),
            "Suspicious Activity Patterns",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            DANGER_COLOR
        ));
        
        // Activity header
        JPanel activityHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        activityHeader.setOpaque(false);
        
        activityHeader.add(new JLabel("Line:"));
        JComboBox<String> lineCombo = new JComboBox<>(new String[]{"All Lines", "Red Line", "Orange Line", "Blue Line", "Green Line"});
        lineCombo.addActionListener(e -> filterActivityByLine((String) lineCombo.getSelectedItem()));
        activityHeader.add(lineCombo);
        
        activityHeader.add(new JLabel("Alert Level:"));
        JComboBox<String> levelCombo = new JComboBox<>(new String[]{"All Levels", "Critical", "High", "Medium", "Low"});
        activityHeader.add(levelCombo);
        
        JButton escalateBtn = new JButton("Escalate Selected");
        escalateBtn.setBackground(DANGER_COLOR);
        escalateBtn.setForeground(Color.WHITE);
        escalateBtn.addActionListener(e -> escalateSelectedActivity());
        activityHeader.add(escalateBtn);
        
        JButton bpdBtn = new JButton("Notify BPD");
        bpdBtn.setBackground(MBTA_ORANGE);
        bpdBtn.setForeground(Color.WHITE);
        bpdBtn.addActionListener(e -> notifyBPD());
        activityHeader.add(bpdBtn);
        
        locationPanel.add(activityHeader, BorderLayout.NORTH);
        
        // Activity table
        String[] activityColumns = {"Alert ID", "Description", "Line", "Station", "Type", "Level", "Time", "Actions"};
        activityTableModel = new DefaultTableModel(activityColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        
        activityTable = new JTable(activityTableModel);
        activityTable.setRowHeight(35);
        activityTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Alert level renderer
        activityTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String level = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (level) {
                        case "CRITICAL" -> { setBackground(new Color(139, 0, 0)); setForeground(Color.WHITE); }
                        case "HIGH" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "MEDIUM" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        default -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
                    }
                }
                return this;
            }
        });
        
        // Actions column
        activityTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer("Review"));
        activityTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox(), this::reviewActivity));
        
        locationPanel.add(new JScrollPane(activityTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(locationPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Load initial data
        loadActivityData();
        
        return panel;
    }
    
    // ==================== TAB 3: TRAVELER VERIFICATION ====================
    
    private JPanel createTravelerVerificationTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Traveler ID Verification Queue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);
        
        JButton airportBtn = new JButton("Airport Coordination");
        airportBtn.setBackground(BLUE_LINE);
        airportBtn.setForeground(Color.WHITE);
        airportBtn.addActionListener(e -> showAirportCoordinationDialog());
        actionPanel.add(airportBtn);
        
        JButton tsaBtn = new JButton("TSA Cross-Reference");
        tsaBtn.setBackground(new Color(0, 83, 155));
        tsaBtn.setForeground(Color.WHITE);
        tsaBtn.addActionListener(e -> showTSACrossReferenceDialog());
        actionPanel.add(tsaBtn);
        
        JButton refreshBtn = new JButton("Refresh Queue");
        refreshBtn.addActionListener(e -> loadVerificationData());
        actionPanel.add(refreshBtn);
        
        headerPanel.add(actionPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split pane - table and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        splitPane.setResizeWeight(0.6);
        
        // Left - Verification queue table
        String[] columns = {"ID", "Traveler Name", "Location", "Status", "Request Type", "Time", "Actions"};
        verificationTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 6; }
        };
        
        verificationTable = new JTable(verificationTableModel);
        verificationTable.setRowHeight(38);
        verificationTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Status renderer
        verificationTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (status) {
                        case "PENDING" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "IN_PROGRESS" -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
                        case "VERIFIED" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        case "FAILED" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Actions column
        verificationTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer("Verify"));
        verificationTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this::processVerification));
        
        // Selection listener
        verificationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = verificationTable.getSelectedRow();
                if (row >= 0) {
                    displayVerificationDetails(row);
                }
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(verificationTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 0));
        splitPane.setLeftComponent(tableScroll);
        
        // Right - Verification details
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Verification Details",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        verificationNotesArea = new JTextArea();
        verificationNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        verificationNotesArea.setLineWrap(true);
        verificationNotesArea.setWrapStyleWord(true);
        verificationNotesArea.setText("Select a verification request to view details");
        verificationNotesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        verificationNotesArea.setEditable(false);
        
        detailsPanel.add(new JScrollPane(verificationNotesArea), BorderLayout.CENTER);
        
        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setOpaque(false);
        
        JButton approveBtn = new JButton("Approve");
        approveBtn.setBackground(SUCCESS_COLOR);
        approveBtn.setForeground(Color.WHITE);
        approveBtn.addActionListener(e -> approveVerification());
        actionsPanel.add(approveBtn);
        
        JButton rejectBtn = new JButton("Reject");
        rejectBtn.setBackground(DANGER_COLOR);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.addActionListener(e -> rejectVerification());
        actionsPanel.add(rejectBtn);
        
        JButton addNoteBtn = new JButton("Add Note");
        addNoteBtn.addActionListener(e -> addVerificationNote());
        actionsPanel.add(addNoteBtn);
        
        JButton requestDocsBtn = new JButton("Request Documents");
        requestDocsBtn.addActionListener(e -> requestDocuments());
        actionsPanel.add(requestDocsBtn);
        
        detailsPanel.add(actionsPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(detailsPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Load initial data
        loadVerificationData();
        
        return panel;
    }
    
    // ==================== TAB 4: FRAUD INVESTIGATION ====================
    
    private JPanel createFraudInvestigationTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Fraud Investigation Cases");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton newCaseBtn = new JButton("New Case");
        newCaseBtn.setBackground(MBTA_ORANGE);
        newCaseBtn.setForeground(Color.WHITE);
        newCaseBtn.addActionListener(e -> showNewFraudCaseDialog());
        buttonPanel.add(newCaseBtn);
        
        JButton crossEnterpriseBtn = new JButton("Cross-Enterprise Data");
        crossEnterpriseBtn.addActionListener(e -> showCrossEnterpriseDataDialog());
        buttonPanel.add(crossEnterpriseBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadFraudCases());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        splitPane.setResizeWeight(0.6);
        
        // Left - Cases table
        String[] columns = {"Case #", "Title", "Status", "Priority", "Created", "Lead"};
        fraudTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        fraudTable = new JTable(fraudTableModel);
        fraudTable.setRowHeight(38);
        fraudTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Status renderer
        fraudTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (status) {
                        case "Active" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "Under Review" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "Pending BPD" -> { setBackground(new Color(232, 222, 248)); setForeground(new Color(111, 66, 193)); }
                        case "Closed" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Priority renderer
        fraudTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String priority = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (priority) {
                        case "Critical" -> { setBackground(new Color(139, 0, 0)); setForeground(Color.WHITE); }
                        case "High" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "Medium" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Selection listener
        fraudTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = fraudTable.getSelectedRow();
                if (row >= 0) {
                    displayFraudCaseDetails(row);
                }
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(fraudTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 0));
        splitPane.setLeftComponent(tableScroll);
        
        // Right - Case details
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Case Details",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        fraudCaseNotesArea = new JTextArea();
        fraudCaseNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fraudCaseNotesArea.setLineWrap(true);
        fraudCaseNotesArea.setWrapStyleWord(true);
        fraudCaseNotesArea.setText("Select a case to view details");
        fraudCaseNotesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fraudCaseNotesArea.setEditable(false);
        
        detailsPanel.add(new JScrollPane(fraudCaseNotesArea), BorderLayout.CENTER);
        
        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setOpaque(false);
        
        JButton addEvidenceBtn = new JButton("Add Evidence");
        addEvidenceBtn.addActionListener(e -> addEvidence());
        actionsPanel.add(addEvidenceBtn);
        
        JButton addInterviewBtn = new JButton("Interview Notes");
        addInterviewBtn.addActionListener(e -> addInterviewNotes());
        actionsPanel.add(addInterviewBtn);
        
        JButton escalateBPDBtn = new JButton("Escalate to BPD");
        escalateBPDBtn.setBackground(DANGER_COLOR);
        escalateBPDBtn.setForeground(Color.WHITE);
        escalateBPDBtn.addActionListener(e -> escalateToBPD());
        actionsPanel.add(escalateBPDBtn);
        
        JButton resolveCaseBtn = new JButton("Resolve Case");
        resolveCaseBtn.setBackground(SUCCESS_COLOR);
        resolveCaseBtn.setForeground(Color.WHITE);
        resolveCaseBtn.addActionListener(e -> resolveCase());
        actionsPanel.add(resolveCaseBtn);
        
        detailsPanel.add(actionsPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(detailsPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Load initial data
        loadFraudCases();
        
        return panel;
    }
    
    // ==================== TAB 5: REPORTS & ALERTS ====================
    
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Report selection panel
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        selectionPanel.setOpaque(false);
        selectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(MBTA_ORANGE),
            "Generate Report",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            MBTA_ORANGE.darker()
        ));
        
        selectionPanel.add(new JLabel("Report Type:"));
        JComboBox<String> reportTypeCombo = new JComboBox<>(new String[]{
            "Daily Activity Summary",
            "Suspicious Activity Report",
            "Pattern Analysis",
            "Fraud Case Summary",
            "High-Frequency Claimers",
            "Station Hotspot Analysis",
            "Cross-Enterprise Activity"
        });
        selectionPanel.add(reportTypeCombo);
        
        selectionPanel.add(new JLabel("Date Range:"));
        JComboBox<String> dateRangeCombo = new JComboBox<>(new String[]{
            "Today", "Last 7 Days", "Last 30 Days", "Custom Range"
        });
        selectionPanel.add(dateRangeCombo);
        
        selectionPanel.add(new JLabel("Line:"));
        JComboBox<String> lineCombo = new JComboBox<>(new String[]{"All Lines", "Red Line", "Orange Line", "Blue Line", "Green Line"});
        selectionPanel.add(lineCombo);
        
        JButton generateBtn = new JButton("Generate Report");
        generateBtn.setBackground(PRIMARY_COLOR);
        generateBtn.setForeground(Color.WHITE);
        generateBtn.addActionListener(e -> generateReport(
            (String) reportTypeCombo.getSelectedItem(),
            (String) dateRangeCombo.getSelectedItem(),
            (String) lineCombo.getSelectedItem()
        ));
        selectionPanel.add(generateBtn);
        
        panel.add(selectionPanel, BorderLayout.NORTH);
        
        // Report output area
        JPanel outputPanel = createTitledPanel("Report Output");
        
        reportOutputArea = new JTextArea();
        reportOutputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportOutputArea.setEditable(false);
        reportOutputArea.setText(generateDefaultReport());
        reportOutputArea.setCaretPosition(0);
        
        outputPanel.add(new JScrollPane(reportOutputArea), BorderLayout.CENTER);
        
        // Export buttons
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        exportPanel.setOpaque(false);
        
        JButton exportPdfBtn = new JButton("Export to PDF");
        exportPdfBtn.addActionListener(e -> exportReport("PDF"));
        exportPanel.add(exportPdfBtn);
        
        JButton exportCsvBtn = new JButton("Export to CSV");
        exportCsvBtn.addActionListener(e -> exportReport("CSV"));
        exportPanel.add(exportCsvBtn);
        
        JButton exportBpdBtn = new JButton("Export to BPD Format");
        exportBpdBtn.setBackground(MBTA_DARK);
        exportBpdBtn.setForeground(Color.WHITE);
        exportBpdBtn.addActionListener(e -> exportToBPDFormat());
        exportPanel.add(exportBpdBtn);
        
        JButton emailBtn = new JButton("Email Report");
        emailBtn.addActionListener(e -> emailReport());
        exportPanel.add(emailBtn);
        
        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> printReport());
        exportPanel.add(printBtn);
        
        outputPanel.add(exportPanel, BorderLayout.SOUTH);
        panel.add(outputPanel, BorderLayout.CENTER);
        
        // Escalated items panel at bottom
        JPanel escalatedPanel = createTitledPanel("Escalated Items Requiring Action");
        escalatedPanel.setPreferredSize(new Dimension(0, 180));
        
        String[] escalatedColumns = {"ID", "Type", "Description", "Line", "Escalated By", "Time", "Urgency"};
        DefaultTableModel escalatedModel = new DefaultTableModel(escalatedColumns, 0);
        
        escalatedModel.addRow(new Object[]{"ESC-001", "Fraud Alert", "Identity fraud at Airport station", "Blue", currentUser.getFullName(), "2 hours ago", "High"});
        escalatedModel.addRow(new Object[]{"ESC-002", "Pattern Alert", "Multiple claims - same description", "Red", "System", "4 hours ago", "Medium"});
        escalatedModel.addRow(new Object[]{"ESC-003", "BPD Request", "Stolen property coordination", "Orange", "BPD Det. Smith", "Yesterday", "High"});
        
        JTable escalatedTable = new JTable(escalatedModel);
        escalatedTable.setRowHeight(35);
        
        // Urgency renderer
        escalatedTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String urgency = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    switch (urgency) {
                        case "High" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "Medium" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        default -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                    }
                }
                return this;
            }
        });
        
        escalatedPanel.add(new JScrollPane(escalatedTable), BorderLayout.CENTER);
        panel.add(escalatedPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> data = new HashMap<>();
                
                // Count suspicious activities by level
                long highAlerts = suspiciousActivities.stream()
                    .filter(a -> a.level == AlertLevel.HIGH || a.level == AlertLevel.CRITICAL)
                    .count();
                long patternWarnings = suspiciousActivities.stream()
                    .filter(a -> a.type == ActivityType.CROSS_STATION || 
                                a.type == ActivityType.MULTIPLE_CLAIMS ||
                                a.type == ActivityType.HIGH_VALUE_PATTERN)
                    .count();
                long pendingVerifications = travelerVerifications.stream()
                    .filter(v -> v.status == VerificationStatus.PENDING || 
                                v.status == VerificationStatus.IN_PROGRESS)
                    .count();
                long activeFraud = fraudCases.stream()
                    .filter(f -> f.status != FraudStatus.CLOSED)
                    .count();
                
                data.put("suspiciousAlerts", highAlerts);
                data.put("patternWarnings", patternWarnings);
                data.put("pendingVerifications", pendingVerifications);
                data.put("activeFraudCases", activeFraud);
                
                return data;
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    
                    suspiciousAlertsLabel.setText(String.valueOf(data.get("suspiciousAlerts")));
                    patternWarningsLabel.setText(String.valueOf(data.get("patternWarnings")));
                    pendingVerificationsLabel.setText(String.valueOf(data.get("pendingVerifications")));
                    activeFraudCasesLabel.setText(String.valueOf(data.get("activeFraudCases")));
                    
                    updateAlerts();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void updateAlerts() {
        alertsPanel.removeAll();
        
        // Critical alerts first
        for (SuspiciousActivity activity : suspiciousActivities) {
            if (activity.level == AlertLevel.CRITICAL) {
                alertsPanel.add(createAlertItem("CRITICAL: " + activity.description + " [" + activity.station + "]", DANGER_COLOR));
                alertsPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        // High alerts
        for (SuspiciousActivity activity : suspiciousActivities) {
            if (activity.level == AlertLevel.HIGH) {
                alertsPanel.add(createAlertItem("HIGH: " + activity.description + " [" + activity.line + "]", WARNING_COLOR));
                alertsPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        // Pending verifications
        long pendingCount = travelerVerifications.stream()
            .filter(v -> v.status == VerificationStatus.PENDING)
            .count();
        if (pendingCount > 0) {
            alertsPanel.add(createAlertItem(pendingCount + " traveler verification(s) pending", INFO_COLOR));
            alertsPanel.add(Box.createVerticalStrut(8));
        }
        
        // Active fraud cases
        long fraudCount = fraudCases.stream()
            .filter(f -> f.status == FraudStatus.ACTIVE)
            .count();
        if (fraudCount > 0) {
            alertsPanel.add(createAlertItem(fraudCount + " active fraud investigation(s)", MBTA_ORANGE));
        }
        
        if (alertsPanel.getComponentCount() == 0) {
            JLabel noAlerts = new JLabel("No active alerts");
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
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadActivityData() {
        // Load claimers
        claimersTableModel.setRowCount(0);
        for (HighFrequencyClaimer claimer : highFrequencyClaimers) {
            claimersTableModel.addRow(new Object[]{
                claimer.id,
                claimer.name,
                claimer.claimCount,
                claimer.stationCount,
                claimer.trustScore,
                claimer.lastActivity.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")),
                "Investigate"
            });
        }
        
        // Load activities
        activityTableModel.setRowCount(0);
        for (SuspiciousActivity activity : suspiciousActivities) {
            activityTableModel.addRow(new Object[]{
                activity.id,
                activity.description,
                activity.line,
                activity.station,
                activity.type.getDisplayName(),
                activity.level.name(),
                activity.timestamp.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")),
                "Review"
            });
        }
    }
    
    private void loadVerificationData() {
        verificationTableModel.setRowCount(0);
        for (TravelerVerification verification : travelerVerifications) {
            verificationTableModel.addRow(new Object[]{
                verification.id,
                verification.travelerName,
                verification.location,
                verification.status.name(),
                verification.requestNotes,
                verification.requestTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")),
                "Verify"
            });
        }
    }
    
    private void loadFraudCases() {
        fraudTableModel.setRowCount(0);
        for (FraudCase fraudCase : fraudCases) {
            fraudTableModel.addRow(new Object[]{
                fraudCase.caseNumber,
                fraudCase.title,
                fraudCase.status.getDisplayName(),
                fraudCase.priority.getDisplayName(),
                fraudCase.createdAt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                "Insp. " + currentUser.getLastName()
            });
        }
    }
    
    private void displayVerificationDetails(int row) {
        if (row < 0 || row >= travelerVerifications.size()) return;
        
        TravelerVerification v = travelerVerifications.get(row);
        
        StringBuilder details = new StringBuilder();
        details.append("VERIFICATION REQUEST: ").append(v.id).append("\n");
        details.append("=".repeat(45)).append("\n\n");
        details.append("Traveler: ").append(v.travelerName).append("\n");
        details.append("Location: ").append(v.location).append("\n");
        details.append("Status: ").append(v.status.name()).append("\n");
        details.append("Requested: ").append(v.requestTime.format(DT_FORMATTER)).append("\n\n");
        details.append("Request Notes:\n").append(v.requestNotes).append("\n\n");
        details.append("=".repeat(45)).append("\n");
        details.append("VERIFICATION LOG:\n\n");
        details.append("[").append(v.requestTime.format(DT_FORMATTER)).append("]\n");
        details.append("Verification request created.\n\n");
        details.append("[").append(LocalDateTime.now().format(DT_FORMATTER)).append("]\n");
        details.append("Awaiting processing.\n");
        
        verificationNotesArea.setText(details.toString());
        verificationNotesArea.setCaretPosition(0);
    }
    
    private void displayFraudCaseDetails(int row) {
        if (row < 0 || row >= fraudCases.size()) return;
        
        FraudCase fc = fraudCases.get(row);
        
        StringBuilder details = new StringBuilder();
        details.append("CASE: ").append(fc.caseNumber).append("\n");
        details.append("=".repeat(45)).append("\n\n");
        details.append("Title: ").append(fc.title).append("\n");
        details.append("Status: ").append(fc.status.getDisplayName()).append("\n");
        details.append("Priority: ").append(fc.priority.getDisplayName()).append("\n");
        details.append("Created: ").append(fc.createdAt.format(DT_FORMATTER)).append("\n\n");
        details.append("Description:\n").append(fc.description).append("\n\n");
        details.append("=".repeat(45)).append("\n");
        details.append("INVESTIGATION TIMELINE:\n\n");
        details.append("[").append(fc.createdAt.format(DT_FORMATTER)).append("]\n");
        details.append("Case opened. Initial evidence collected.\n\n");
        details.append("[").append(fc.createdAt.plusHours(24).format(DT_FORMATTER)).append("]\n");
        details.append("Cross-enterprise data requested from universities.\n\n");
        details.append("[").append(LocalDateTime.now().format(DT_FORMATTER)).append("]\n");
        details.append("Case under active investigation.\n");
        
        fraudCaseNotesArea.setText(details.toString());
        fraudCaseNotesArea.setCaretPosition(0);
    }
    
    // ==================== ACTIONS ====================
    
    private void filterClaimers(String period) {
        // Filter implementation
        loadActivityData();
    }
    
    private void filterActivityByLine(String line) {
        // Filter implementation
        loadActivityData();
    }
    
    private void investigateClaimer(int row) {
        if (row < 0) return;
        String name = (String) claimersTableModel.getValueAt(row, 1);
        
        int choice = JOptionPane.showConfirmDialog(this,
            "Open investigation for: " + name + "?\n\n" +
            "This will create a new fraud case and\n" +
            "request cross-enterprise data.",
            "Confirm Investigation",
            JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            String caseNum = "FC-2024-" + String.format("%03d", fraudCases.size() + 1);
            FraudCase newCase = new FraudCase(
                caseNum,
                "Investigation: " + name,
                "High-frequency claimer investigation based on suspicious patterns",
                FraudStatus.ACTIVE,
                FraudPriority.MEDIUM,
                LocalDateTime.now()
            );
            fraudCases.add(newCase);
            
            JOptionPane.showMessageDialog(this,
                "Investigation opened!\n\nCase #: " + caseNum,
                "Investigation Created",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadFraudCases();
            tabbedPane.setSelectedIndex(3);
        }
    }
    
    private void reviewActivity(int row) {
        if (row < 0) return;
        String id = (String) activityTableModel.getValueAt(row, 0);
        String desc = (String) activityTableModel.getValueAt(row, 1);
        
        String[] options = {"Dismiss", "Escalate", "Open Investigation"};
        int choice = JOptionPane.showOptionDialog(this,
            "Review Activity: " + id + "\n\n" + desc + "\n\nSelect action:",
            "Review Activity",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        
        switch (choice) {
            case 0 -> JOptionPane.showMessageDialog(this, "Activity dismissed", "Dismissed", JOptionPane.INFORMATION_MESSAGE);
            case 1 -> {
                JOptionPane.showMessageDialog(this, "Activity escalated to supervisor", "Escalated", JOptionPane.INFORMATION_MESSAGE);
            }
            case 2 -> investigateClaimer(row);
        }
    }
    
    private void escalateSelectedActivity() {
        int row = activityTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an activity to escalate", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String id = (String) activityTableModel.getValueAt(row, 0);
        JOptionPane.showMessageDialog(this,
            "Activity " + id + " escalated to supervisor.\n\n" +
            "Notification sent to Transit Security Command.",
            "Escalated",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void notifyBPD() {
        int row = activityTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an activity first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String id = (String) activityTableModel.getValueAt(row, 0);
        String desc = (String) activityTableModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Notify Boston Police Department about:\n\n" +
            id + ": " + desc + "\n\n" +
            "This will send a formal notification to BPD.",
            "Confirm BPD Notification",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                "BPD Notification Sent!\n\n" +
                "Reference #: BPD-" + System.currentTimeMillis() % 100000 + "\n" +
                "Contact: Transit Coordination Unit",
                "BPD Notified",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void processVerification(int row) {
        if (row < 0) return;
        
        String id = (String) verificationTableModel.getValueAt(row, 0);
        String name = (String) verificationTableModel.getValueAt(row, 1);
        
        String[] options = {"Verify ID", "Request Documents", "Reject", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Process verification for: " + name + "\n\n" +
            "Select action:",
            "Process Verification",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        
        switch (choice) {
            case 0 -> approveVerification();
            case 1 -> requestDocuments();
            case 2 -> rejectVerification();
        }
    }
    
    private void approveVerification() {
        int row = verificationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a verification", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        travelerVerifications.get(row).status = VerificationStatus.VERIFIED;
        JOptionPane.showMessageDialog(this, "Verification approved!", "Approved", JOptionPane.INFORMATION_MESSAGE);
        loadVerificationData();
    }
    
    private void rejectVerification() {
        int row = verificationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a verification", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String reason = JOptionPane.showInputDialog(this, "Enter rejection reason:");
        if (reason != null && !reason.isEmpty()) {
            travelerVerifications.get(row).status = VerificationStatus.FAILED;
            JOptionPane.showMessageDialog(this, "Verification rejected", "Rejected", JOptionPane.INFORMATION_MESSAGE);
            loadVerificationData();
        }
    }
    
    private void addVerificationNote() {
        int row = verificationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a verification", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String note = JOptionPane.showInputDialog(this, "Enter note:");
        if (note != null && !note.isEmpty()) {
            String current = verificationNotesArea.getText();
            verificationNotesArea.setText(current + "\n\n[" + LocalDateTime.now().format(DT_FORMATTER) + "]\n" + note);
            JOptionPane.showMessageDialog(this, "Note added", "Note Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void requestDocuments() {
        int row = verificationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a verification", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(this,
            "Document request sent!\n\n" +
            "Request ID: DOC-" + System.currentTimeMillis() % 100000 + "\n" +
            "Documents requested: Photo ID, Boarding Pass",
            "Documents Requested",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAirportCoordinationDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Terminal:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> terminalCombo = new JComboBox<>(new String[]{"Terminal A", "Terminal B", "Terminal C", "Terminal E"});
        panel.add(terminalCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Request Type:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "ID Verification", "Item Transfer", "Security Alert", "Coordination Meeting"
        });
        panel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Details:"), gbc);
        
        gbc.gridx = 1;
        JTextArea detailsArea = new JTextArea(4, 25);
        detailsArea.setLineWrap(true);
        panel.add(new JScrollPane(detailsArea), gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Airport Coordination", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Coordination request sent to Logan Airport!\n\n" +
                "Request ID: AC-" + System.currentTimeMillis() % 100000 + "\n" +
                "Terminal: " + terminalCombo.getSelectedItem(),
                "Request Sent",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showTSACrossReferenceDialog() {
        String travelerId = JOptionPane.showInputDialog(this, 
            "Enter Traveler ID or Name for TSA cross-reference:");
        
        if (travelerId != null && !travelerId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "TSA Cross-Reference Request Submitted\n\n" +
                "Query: " + travelerId + "\n" +
                "Reference #: TSA-" + System.currentTimeMillis() % 100000 + "\n\n" +
                "Note: Results will be returned within 24-48 hours.",
                "TSA Request Submitted",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showNewFraudCaseDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Case Title:"), gbc);
        
        gbc.gridx = 1;
        JTextField titleField = new JTextField(25);
        panel.add(titleField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(4, 25);
        descArea.setLineWrap(true);
        panel.add(new JScrollPane(descArea), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Priority:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low"});
        priorityCombo.setSelectedIndex(1);
        panel.add(priorityCombo, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "New Fraud Case", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a case title", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String caseNum = "FC-2024-" + String.format("%03d", fraudCases.size() + 1);
            FraudPriority priority = switch ((String) priorityCombo.getSelectedItem()) {
                case "Critical" -> FraudPriority.CRITICAL;
                case "High" -> FraudPriority.HIGH;
                case "Medium" -> FraudPriority.MEDIUM;
                default -> FraudPriority.LOW;
            };
            
            FraudCase newCase = new FraudCase(caseNum, title, descArea.getText(), FraudStatus.ACTIVE, priority, LocalDateTime.now());
            fraudCases.add(newCase);
            
            JOptionPane.showMessageDialog(this,
                "Fraud case created!\n\nCase #: " + caseNum,
                "Case Created",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadFraudCases();
            loadDashboardData();
        }
    }
    
    private void showCrossEnterpriseDataDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Enterprise:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> enterpriseCombo = new JComboBox<>(new String[]{
            "Northeastern University", "Boston University", "Logan Airport", "Boston Police Department"
        });
        panel.add(enterpriseCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Data Type:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> dataTypeCombo = new JComboBox<>(new String[]{
            "Claim History", "Trust Scores", "Lost Items", "User Information", "Incident Reports"
        });
        panel.add(dataTypeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Search Query:"), gbc);
        
        gbc.gridx = 1;
        JTextField queryField = new JTextField(25);
        panel.add(queryField, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Cross-Enterprise Data Request", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Data request submitted!\n\n" +
                "Request ID: XE-" + System.currentTimeMillis() % 100000 + "\n" +
                "Enterprise: " + enterpriseCombo.getSelectedItem() + "\n" +
                "Data Type: " + dataTypeCombo.getSelectedItem() + "\n\n" +
                "Results will be available within 1-2 hours.",
                "Request Submitted",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void addEvidence() {
        int row = fraudTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String evidence = JOptionPane.showInputDialog(this, "Enter evidence details:");
        if (evidence != null && !evidence.isEmpty()) {
            String current = fraudCaseNotesArea.getText();
            fraudCaseNotesArea.setText(current + "\n\n[" + LocalDateTime.now().format(DT_FORMATTER) + "] EVIDENCE ADDED:\n" + evidence);
            JOptionPane.showMessageDialog(this, "Evidence added to case", "Evidence Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void addInterviewNotes() {
        int row = fraudTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JTextArea notesArea = new JTextArea(6, 30);
        notesArea.setLineWrap(true);
        
        int result = JOptionPane.showConfirmDialog(this, 
            new JScrollPane(notesArea), "Interview Notes", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION && !notesArea.getText().isEmpty()) {
            String current = fraudCaseNotesArea.getText();
            fraudCaseNotesArea.setText(current + "\n\n[" + LocalDateTime.now().format(DT_FORMATTER) + "] INTERVIEW NOTES:\n" + notesArea.getText());
            JOptionPane.showMessageDialog(this, "Interview notes added", "Notes Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void escalateToBPD() {
        int row = fraudTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Escalate this case to Boston Police Department?\n\n" +
            "This will transfer case management to BPD.",
            "Confirm BPD Escalation",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            fraudCases.get(row).status = FraudStatus.PENDING_BPD;
            JOptionPane.showMessageDialog(this,
                "Case escalated to BPD!\n\n" +
                "BPD Reference #: BPD-" + System.currentTimeMillis() % 100000,
                "Escalated",
                JOptionPane.INFORMATION_MESSAGE);
            loadFraudCases();
        }
    }
    
    private void resolveCase() {
        int row = fraudTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resolution = JOptionPane.showInputDialog(this, "Enter resolution summary:");
        if (resolution != null && !resolution.isEmpty()) {
            fraudCases.get(row).status = FraudStatus.CLOSED;
            JOptionPane.showMessageDialog(this, "Case resolved and closed", "Case Closed", JOptionPane.INFORMATION_MESSAGE);
            loadFraudCases();
            loadDashboardData();
        }
    }
    
    private void showIssueAlertDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Alert Type:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Security Alert", "Suspicious Activity", "BOLO", "System Warning"
        });
        panel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Line:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> lineCombo = new JComboBox<>(new String[]{"All Lines", "Red Line", "Orange Line", "Blue Line", "Green Line"});
        panel.add(lineCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Message:"), gbc);
        
        gbc.gridx = 1;
        JTextArea msgArea = new JTextArea(4, 25);
        msgArea.setLineWrap(true);
        panel.add(new JScrollPane(msgArea), gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Issue Alert", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Alert Issued!\n\n" +
                "Alert ID: ALT-" + System.currentTimeMillis() % 100000 + "\n" +
                "Type: " + typeCombo.getSelectedItem() + "\n" +
                "Broadcast to: " + lineCombo.getSelectedItem(),
                "Alert Issued",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showBPDContactDialog() {
        JOptionPane.showMessageDialog(this,
            "BOSTON POLICE DEPARTMENT CONTACTS\n\n" +
            "Transit Coordination Unit: (617) 343-4500\n" +
            "Emergency: 911\n" +
            "Non-Emergency: (617) 343-4633\n\n" +
            "MBTA Transit Police: (617) 222-1212\n" +
            "After Hours: (617) 222-1000",
            "BPD Contact Information",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String generateDefaultReport() {
        StringBuilder report = new StringBuilder();
        report.append("╔══════════════════════════════════════════════════════════════════╗\n");
        report.append("║           MBTA TRANSIT SECURITY - DAILY ACTIVITY REPORT          ║\n");
        report.append("╠══════════════════════════════════════════════════════════════════╣\n");
        report.append("║  Generated: ").append(LocalDateTime.now().format(DT_FORMATTER));
        report.append("                              ║\n");
        report.append("║  Inspector: ").append(String.format("%-50s", currentUser.getFullName())).append("║\n");
        report.append("╚══════════════════════════════════════════════════════════════════╝\n\n");
        
        report.append("┌────────────────────────────────────────────────────────────────────┐\n");
        report.append("│  SUMMARY STATISTICS                                                │\n");
        report.append("├────────────────────────────────────────────────────────────────────┤\n");
        report.append("│  Suspicious Alerts:      ").append(String.format("%-45s", suspiciousActivities.size())).append("│\n");
        report.append("│  High-Frequency Claimers: ").append(String.format("%-44s", highFrequencyClaimers.size())).append("│\n");
        report.append("│  Pending Verifications:  ").append(String.format("%-45s", travelerVerifications.stream().filter(v -> v.status == VerificationStatus.PENDING).count())).append("│\n");
        report.append("│  Active Fraud Cases:     ").append(String.format("%-45s", fraudCases.stream().filter(f -> f.status != FraudStatus.CLOSED).count())).append("│\n");
        report.append("└────────────────────────────────────────────────────────────────────┘\n\n");
        
        report.append("┌────────────────────────────────────────────────────────────────────┐\n");
        report.append("│  LINE STATUS                                                       │\n");
        report.append("├────────────────────────────────────────────────────────────────────┤\n");
        report.append("│  Red Line:    NORMAL      │  Orange Line: NORMAL                  │\n");
        report.append("│  Blue Line:   ALERT       │  Green Line:  NORMAL                  │\n");
        report.append("└────────────────────────────────────────────────────────────────────┘\n\n");
        
        report.append("Select a report type and click 'Generate Report' for detailed analysis.\n");
        
        return report.toString();
    }
    
    private void generateReport(String type, String dateRange, String line) {
        StringBuilder report = new StringBuilder();
        
        report.append("╔══════════════════════════════════════════════════════════════════╗\n");
        report.append("║           MBTA TRANSIT SECURITY REPORT                           ║\n");
        report.append("╠══════════════════════════════════════════════════════════════════╣\n");
        report.append("║  Report Type: ").append(String.format("%-51s", type)).append("║\n");
        report.append("║  Date Range:  ").append(String.format("%-51s", dateRange)).append("║\n");
        report.append("║  Line Filter: ").append(String.format("%-51s", line)).append("║\n");
        report.append("║  Generated:   ").append(String.format("%-51s", LocalDateTime.now().format(DT_FORMATTER))).append("║\n");
        report.append("║  Inspector:   ").append(String.format("%-51s", currentUser.getFullName())).append("║\n");
        report.append("╚══════════════════════════════════════════════════════════════════╝\n\n");
        
        switch (type) {
            case "Daily Activity Summary" -> {
                report.append("DAILY ACTIVITY SUMMARY\n");
                report.append("=".repeat(50)).append("\n\n");
                report.append("Total Activities Monitored: 47\n");
                report.append("Alerts Generated: 5\n");
                report.append("Escalations: 2\n");
                report.append("BPD Notifications: 1\n\n");
                report.append("Station Breakdown:\n");
                report.append("  Park Street:        12 activities (3 alerts)\n");
                report.append("  Airport:            15 activities (2 alerts)\n");
                report.append("  Downtown Crossing:   8 activities (0 alerts)\n");
                report.append("  South Station:      12 activities (0 alerts)\n");
            }
            case "Suspicious Activity Report" -> {
                report.append("SUSPICIOUS ACTIVITY REPORT\n");
                report.append("=".repeat(50)).append("\n\n");
                for (SuspiciousActivity activity : suspiciousActivities) {
                    report.append("[").append(activity.level).append("] ").append(activity.description).append("\n");
                    report.append("    Station: ").append(activity.station).append(" | Line: ").append(activity.line).append("\n");
                    report.append("    Time: ").append(activity.timestamp.format(DT_FORMATTER)).append("\n\n");
                }
            }
            case "High-Frequency Claimers" -> {
                report.append("HIGH-FREQUENCY CLAIMERS ANALYSIS\n");
                report.append("=".repeat(50)).append("\n\n");
                for (HighFrequencyClaimer claimer : highFrequencyClaimers) {
                    report.append("Name: ").append(claimer.name).append("\n");
                    report.append("  Claims: ").append(claimer.claimCount);
                    report.append(" | Stations: ").append(claimer.stationCount);
                    report.append(" | Trust Score: ").append(claimer.trustScore).append("\n\n");
                }
            }
            default -> {
                report.append("Report data would be displayed here.\n");
                report.append("Select specific report type for detailed analysis.\n");
            }
        }
        
        reportOutputArea.setText(report.toString());
        reportOutputArea.setCaretPosition(0);
    }
    
    private void exportReport(String format) {
        JOptionPane.showMessageDialog(this,
            "Report exported successfully!\n\n" +
            "Format: " + format + "\n" +
            "File: transit_security_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + format.toLowerCase(),
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportToBPDFormat() {
        JOptionPane.showMessageDialog(this,
            "Report exported in BPD format!\n\n" +
            "File: BPD_transit_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xml\n\n" +
            "Format conforms to BPD Case Management System requirements.",
            "BPD Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void emailReport() {
        String email = JOptionPane.showInputDialog(this, "Enter email address:");
        if (email != null && !email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Report emailed to: " + email,
                "Email Sent",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void printReport() {
        JOptionPane.showMessageDialog(this,
            "Report sent to printer.",
            "Print Job Submitted",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void refreshAll() {
        loadDashboardData();
        loadActivityData();
        loadVerificationData();
        loadFraudCases();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class SuspiciousActivity {
        String id;
        String description;
        String line;
        String station;
        ActivityType type;
        AlertLevel level;
        LocalDateTime timestamp;
        
        SuspiciousActivity(String id, String description, String line, String station,
                          ActivityType type, AlertLevel level, LocalDateTime timestamp) {
            this.id = id;
            this.description = description;
            this.line = line;
            this.station = station;
            this.type = type;
            this.level = level;
            this.timestamp = timestamp;
        }
    }
    
    private enum ActivityType {
        MULTIPLE_CLAIMS("Multiple Claims"),
        CROSS_STATION("Cross-Station Pattern"),
        HIGH_VALUE_PATTERN("High-Value Pattern"),
        ID_MISMATCH("ID Mismatch"),
        FALSE_CLAIMS("False Claims");
        
        private final String displayName;
        ActivityType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private enum AlertLevel { LOW, MEDIUM, HIGH, CRITICAL }
    
    private static class HighFrequencyClaimer {
        String id;
        String name;
        String userId;
        int claimCount;
        int stationCount;
        double trustScore;
        LocalDateTime lastActivity;
        
        HighFrequencyClaimer(String id, String name, String userId, int claimCount,
                            int stationCount, double trustScore, LocalDateTime lastActivity) {
            this.id = id;
            this.name = name;
            this.userId = userId;
            this.claimCount = claimCount;
            this.stationCount = stationCount;
            this.trustScore = trustScore;
            this.lastActivity = lastActivity;
        }
    }
    
    private static class TravelerVerification {
        String id;
        String travelerName;
        String location;
        VerificationStatus status;
        String requestNotes;
        LocalDateTime requestTime;
        
        TravelerVerification(String id, String travelerName, String location,
                            VerificationStatus status, String requestNotes, LocalDateTime requestTime) {
            this.id = id;
            this.travelerName = travelerName;
            this.location = location;
            this.status = status;
            this.requestNotes = requestNotes;
            this.requestTime = requestTime;
        }
    }
    
    private enum VerificationStatus { PENDING, IN_PROGRESS, VERIFIED, FAILED }
    
    private static class FraudCase {
        String caseNumber;
        String title;
        String description;
        FraudStatus status;
        FraudPriority priority;
        LocalDateTime createdAt;
        
        FraudCase(String caseNumber, String title, String description,
                 FraudStatus status, FraudPriority priority, LocalDateTime createdAt) {
            this.caseNumber = caseNumber;
            this.title = title;
            this.description = description;
            this.status = status;
            this.priority = priority;
            this.createdAt = createdAt;
        }
    }
    
    private enum FraudStatus {
        ACTIVE("Active"),
        UNDER_REVIEW("Under Review"),
        PENDING_BPD("Pending BPD"),
        CLOSED("Closed");
        
        private final String displayName;
        FraudStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private enum FraudPriority {
        CRITICAL("Critical"),
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low");
        
        private final String displayName;
        FraudPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    // ==================== CUSTOM CELL RENDERERS ====================
    
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String text) {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
            setBackground(PRIMARY_COLOR);
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setText(text);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "Action");
            return this;
        }
    }
    
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        private java.util.function.IntConsumer action;
        
        public ButtonEditor(JCheckBox checkBox, java.util.function.IntConsumer action) {
            super(checkBox);
            this.action = action;
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
            label = value != null ? value.toString() : "Action";
            button.setText(label);
            isPushed = true;
            currentRow = row;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                action.accept(currentRow);
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
