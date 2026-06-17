package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.Organization.OrganizationType;
import com.campus.lostfound.services.*;
import com.campus.lostfound.services.AnalyticsService.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise-Scoped Admin Panel with comprehensive analytics, management, and reporting.
 * 
 * This panel provides the SAME functionality as AdminPanel but scoped to a specific enterprise.
 * All data is filtered to show only:
 * - Items within the enterprise
 * - Users belonging to the enterprise
 * - Organizations within the enterprise
 * - Work requests involving the enterprise
 * 
 * Key differences from Network AdminPanel:
 * - Header shows enterprise name and scope
 * - Enterprise tab shows only the current enterprise (read-only)
 * - Organization management limited to the enterprise
 * - User management limited to enterprise users
 * - Analytics scoped to enterprise metrics
 * 
 * Design Pattern: Uses EnterpriseAdminDataProvider (Strategy Pattern) for data filtering.
 * This maximizes code reuse - the UI code is similar to AdminPanel, only data source differs.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseAdminPanel extends JPanel {

    // ==================== DATA PROVIDER ====================
    private final EnterpriseAdminDataProvider dataProvider;
    
    // ==================== DEPENDENCIES ====================
    private User currentUser;
    private MongoOrganizationDAO organizationDAO;
    private MongoUserDAO userDAO;
    private AnalyticsService analyticsService;
    private ReportExportService reportExportService;
    
    // ==================== UI COMPONENTS ====================
    private JTabbedPane mainTabbedPane;

    // Dashboard tab components
    private JLabel totalItemsLabel, lostItemsLabel, foundItemsLabel, claimedItemsLabel;
    private JLabel totalUsersLabel, activeUsersLabel, avgTrustScoreLabel, totalOrgsLabel;
    private JTable recentItemsTable, topUsersTable, orgStatsTable;

    // Analytics tab components
    private JPanel analyticsChartsPanel;
    private JComboBox<String> chartTypeCombo;
    private JLabel recoveryRateGauge, slaComplianceGauge, crossEnterpriseGauge;
    private JTable trendsTable;

    // Organization Management tab components (replaces Enterprise Management)
    private JTable organizationTable;
    private DefaultTableModel organizationTableModel;

    // User Management tab components
    private JTextField userSearchField;
    private JComboBox<String> userRoleFilter, userOrgFilter;
    private JTable userTable;
    private DefaultTableModel userTableModel;

    // Reports tab components
    private JComboBox<String> reportTypeCombo;
    private JTextField reportFromDate, reportToDate;
    private JComboBox<String> reportOrgFilter;
    private JTextArea reportPreviewArea;

    // System Health tab components (Enterprise view)
    private JLabel dbStatusLabel, itemCountLabel, userCountLabel;
    private JTable orgStatsDetailTable;
    private JTextArea systemLogArea;

    // ==================== COLORS (Same as AdminPanel) ====================
    private static final Color PRIMARY_BLUE = new Color(52, 152, 219);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color WARNING_YELLOW = new Color(241, 196, 15);
    private static final Color DANGER_RED = new Color(231, 76, 60);
    private static final Color INFO_PURPLE = new Color(155, 89, 182);
    private static final Color DARK_GRAY = new Color(52, 73, 94);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);

    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates an enterprise-scoped admin panel for the given user.
     * The panel will show data only from the user's enterprise.
     */
    public EnterpriseAdminPanel(User currentUser) {
        this.currentUser = currentUser;
        this.dataProvider = new EnterpriseAdminDataProvider(currentUser);
        initializeDAOs();
        initComponents();
        loadAllData();
    }
    
    /**
     * Creates an enterprise-scoped admin panel for a specific enterprise.
     * Useful for network admins who want to view a specific enterprise's data.
     */
    public EnterpriseAdminPanel(User currentUser, String enterpriseId) {
        this.currentUser = currentUser;
        this.dataProvider = new EnterpriseAdminDataProvider(enterpriseId);
        initializeDAOs();
        initComponents();
        loadAllData();
    }

    private void initializeDAOs() {
        this.organizationDAO = new MongoOrganizationDAO();
        this.userDAO = new MongoUserDAO();
        this.analyticsService = new AnalyticsService();
        this.reportExportService = new ReportExportService();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY);

        // Header with enterprise scope indicator
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main tabbed pane
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Add tabs (modified for enterprise scope)
        mainTabbedPane.addTab("Dashboard", createDashboardTab());
        mainTabbedPane.addTab("Enterprise Analytics", createAnalyticsTab());
        mainTabbedPane.addTab("Organization Management", createOrganizationManagementTab());
        mainTabbedPane.addTab("User Management", createUserManagementTab());
        mainTabbedPane.addTab("Reports", createReportsTab());
        mainTabbedPane.addTab("Enterprise Health", createEnterpriseHealthTab());

        // Tab change listener
        mainTabbedPane.addChangeListener(e -> {
            int selectedIndex = mainTabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case 0: loadDashboardData(); break;
                case 1: loadAnalyticsData(); break;
                case 2: loadOrganizationData(); break;
                case 3: loadUserData(); break;
                case 5: loadEnterpriseHealthData(); break;
            }
        });

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    // ==================== HEADER ====================
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));

        // Left side - Title and scope indicator
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Enterprise Admin Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(DARK_GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.add(titleLabel);
        
        // Scope indicator badge
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        scopePanel.setOpaque(false);
        scopePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel scopeLabel = new JLabel(" " + dataProvider.getScopeDescription() + " ");
        scopeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        scopeLabel.setForeground(Color.WHITE);
        scopeLabel.setBackground(INFO_PURPLE);
        scopeLabel.setOpaque(true);
        scopeLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        scopePanel.add(scopeLabel);
        
        JLabel scopeHint = new JLabel("  Data scoped to your enterprise only");
        scopeHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        scopeHint.setForeground(Color.GRAY);
        scopePanel.add(scopeHint);
        
        titlePanel.add(scopePanel);
        panel.add(titlePanel, BorderLayout.WEST);

        // Right side - Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton refreshButton = createStyledButton("Refresh All", PRIMARY_BLUE);
        refreshButton.addActionListener(e -> loadAllData());
        buttonPanel.add(refreshButton);

        JButton exportButton = createStyledButton("Export Report", SUCCESS_GREEN);
        exportButton.addActionListener(e -> exportReport());
        buttonPanel.add(exportButton);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    // ==================== TAB 1: DASHBOARD ====================
    private JPanel createDashboardTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Statistics cards (modified for enterprise)
        panel.add(createStatisticsPanel());
        panel.add(Box.createVerticalStrut(20));

        // Tables section
        panel.add(createTablesPanel());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 15));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        totalItemsLabel = new JLabel("0");
        lostItemsLabel = new JLabel("0");
        foundItemsLabel = new JLabel("0");
        claimedItemsLabel = new JLabel("0");
        totalUsersLabel = new JLabel("0");
        activeUsersLabel = new JLabel("0");
        avgTrustScoreLabel = new JLabel("0%");
        totalOrgsLabel = new JLabel("0");

        panel.add(createStatCard("Enterprise Items", totalItemsLabel, PRIMARY_BLUE));
        panel.add(createStatCard("Lost Items", lostItemsLabel, DANGER_RED));
        panel.add(createStatCard("Found Items", foundItemsLabel, SUCCESS_GREEN));
        panel.add(createStatCard("Claimed", claimedItemsLabel, INFO_PURPLE));
        panel.add(createStatCard("Enterprise Users", totalUsersLabel, DARK_GRAY));
        panel.add(createStatCard("Active Users", activeUsersLabel, new Color(22, 160, 133)));
        panel.add(createStatCard("Avg Trust", avgTrustScoreLabel, WARNING_YELLOW));
        panel.add(createStatCard("Organizations", totalOrgsLabel, new Color(149, 165, 166)));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valueLabel);

        return card;
    }

    private JPanel createTablesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        panel.add(createTableSection("Recent Items", createRecentItemsTable()));
        panel.add(createTableSection("Top Contributors", createTopUsersTable()));
        panel.add(createTableSection("Organization Activity", createOrgStatsTable()));

        return panel;
    }

    private JPanel createTableSection(String title, JScrollPane table) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titleLabel.setBackground(new Color(250, 250, 250));
        titleLabel.setOpaque(true);
        section.add(titleLabel, BorderLayout.NORTH);
        section.add(table, BorderLayout.CENTER);

        return section;
    }

    private JScrollPane createRecentItemsTable() {
        String[] columns = {"Title", "Type", "Date"};
        recentItemsTable = new JTable(new Object[0][3], columns);
        styleTable(recentItemsTable);
        return new JScrollPane(recentItemsTable);
    }

    private JScrollPane createTopUsersTable() {
        String[] columns = {"Name", "Items", "Trust"};
        topUsersTable = new JTable(new Object[0][3], columns);
        styleTable(topUsersTable);
        return new JScrollPane(topUsersTable);
    }

    private JScrollPane createOrgStatsTable() {
        String[] columns = {"Organization", "Items", "Recovery"};
        orgStatsTable = new JTable(new Object[0][3], columns);
        styleTable(orgStatsTable);
        return new JScrollPane(orgStatsTable);
    }

    // ==================== TAB 2: ENTERPRISE ANALYTICS ====================
    private JPanel createAnalyticsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        controlsPanel.setOpaque(false);

        controlsPanel.add(new JLabel("Chart Type:"));
        chartTypeCombo = new JComboBox<>(new String[]{
            "Recovery Rate Trend", "Items by Organization", "Items by Status",
            "Items by Category", "User Distribution", "Cross-Enterprise Activity"
        });
        chartTypeCombo.addActionListener(e -> updateAnalyticsChart());
        controlsPanel.add(chartTypeCombo);

        JButton refreshChartBtn = createStyledButton("Refresh", PRIMARY_BLUE);
        refreshChartBtn.addActionListener(e -> loadAnalyticsData());
        controlsPanel.add(refreshChartBtn);

        panel.add(controlsPanel, BorderLayout.NORTH);

        // Main content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setOpaque(false);

        // Left side - KPI Gauges (enterprise-specific)
        JPanel gaugesPanel = createKPIGaugesPanel();
        mainContent.add(gaugesPanel, BorderLayout.WEST);

        // Center - Chart area
        analyticsChartsPanel = new JPanel(new BorderLayout());
        analyticsChartsPanel.setBackground(Color.WHITE);
        analyticsChartsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mainContent.add(analyticsChartsPanel, BorderLayout.CENTER);

        // Bottom - Trends table
        JPanel trendsPanel = createTrendsTablePanel();
        mainContent.add(trendsPanel, BorderLayout.SOUTH);

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createKPIGaugesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setPreferredSize(new Dimension(200, 400));

        JLabel kpiTitle = new JLabel("Enterprise Metrics");
        kpiTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        kpiTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(kpiTitle);
        panel.add(Box.createVerticalStrut(20));

        // Recovery Rate Gauge
        panel.add(createGaugePanel("Recovery Rate", recoveryRateGauge = new JLabel("0%"), SUCCESS_GREEN));
        panel.add(Box.createVerticalStrut(15));

        // SLA Compliance Gauge
        panel.add(createGaugePanel("SLA Compliance", slaComplianceGauge = new JLabel("0%"), PRIMARY_BLUE));
        panel.add(Box.createVerticalStrut(15));

        // Cross-Enterprise Activity
        panel.add(createGaugePanel("Cross-Enterprise", crossEnterpriseGauge = new JLabel("0"), INFO_PURPLE));
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createGaugePanel(String title, JLabel valueLabel, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(valueLabel);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setForeground(color);
        progressBar.setMaximumSize(new Dimension(150, 8));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(progressBar);

        return panel;
    }

    private JPanel createTrendsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setPreferredSize(new Dimension(0, 200));

        JLabel title = new JLabel("Weekly Trends - " + dataProvider.getScopeDescription());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Week", "Items", "Lost", "Found", "Recovered", "Rate"};
        trendsTable = new JTable(new Object[0][6], columns);
        styleTable(trendsTable);
        panel.add(new JScrollPane(trendsTable), BorderLayout.CENTER);

        return panel;
    }

    private void updateAnalyticsChart() {
        analyticsChartsPanel.removeAll();
        String chartType = (String) chartTypeCombo.getSelectedItem();

        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setOpaque(false);

        // Create chart visualization
        JPanel barsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawEnterpriseChart(g, chartType);
            }
        };
        barsPanel.setBackground(Color.WHITE);
        chartPanel.add(barsPanel, BorderLayout.CENTER);

        analyticsChartsPanel.add(chartPanel, BorderLayout.CENTER);
        analyticsChartsPanel.revalidate();
        analyticsChartsPanel.repaint();
    }

    private void drawEnterpriseChart(Graphics g, String chartType) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<ChartDataPoint> data = getEnterpriseChartData(chartType);
        if (data.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("No data available for " + dataProvider.getScopeDescription(), 100, 100);
            return;
        }

        int width = analyticsChartsPanel.getWidth() - 100;
        int height = analyticsChartsPanel.getHeight() - 100;
        int barWidth = Math.max(30, (width - 50) / data.size() - 10);
        int maxValue = (int) data.stream().mapToDouble(d -> d.value).max().orElse(100);
        if (maxValue == 0) maxValue = 1;

        int x = 50;
        for (ChartDataPoint point : data) {
            int barHeight = (int) ((point.value / maxValue) * (height - 50));

            g2.setColor(point.color);
            g2.fillRect(x, height - barHeight, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            String valueStr = point.getFormattedValue();
            g2.drawString(valueStr, x + barWidth / 4, height - barHeight - 5);

            String label = point.label.length() > 10 ? point.label.substring(0, 8) + ".." : point.label;
            g2.drawString(label, x, height + 15);

            x += barWidth + 10;
        }
    }

    private List<ChartDataPoint> getEnterpriseChartData(String chartType) {
        List<ChartDataPoint> result = new ArrayList<>();
        Color[] colors = {PRIMARY_BLUE, SUCCESS_GREEN, WARNING_YELLOW, DANGER_RED, INFO_PURPLE, 
                          new Color(22, 160, 133), new Color(149, 165, 166)};
        int colorIndex = 0;

        try {
            switch (chartType) {
                case "Items by Organization":
                    Map<String, Long> orgCounts = dataProvider.getItemCountByOrganization();
                    for (Map.Entry<String, Long> entry : orgCounts.entrySet()) {
                        result.add(new ChartDataPoint(entry.getKey(), entry.getValue(), 
                                colors[colorIndex++ % colors.length]));
                    }
                    break;
                    
                case "Items by Status":
                    for (ItemStatus status : ItemStatus.values()) {
                        long count = dataProvider.getItemsByStatus(status).size();
                        if (count > 0) {
                            result.add(new ChartDataPoint(status.name(), count, 
                                    colors[colorIndex++ % colors.length]));
                        }
                    }
                    break;
                    
                case "Items by Category":
                    Map<ItemCategory, Long> catCounts = dataProvider.getItemCountByCategory();
                    for (Map.Entry<ItemCategory, Long> entry : catCounts.entrySet()) {
                        result.add(new ChartDataPoint(entry.getKey().name(), entry.getValue(), 
                                colors[colorIndex++ % colors.length]));
                    }
                    break;
                    
                case "User Distribution":
                    Map<String, Long> userOrgCounts = dataProvider.getUserCountByOrganization();
                    for (Map.Entry<String, Long> entry : userOrgCounts.entrySet()) {
                        result.add(new ChartDataPoint(entry.getKey(), entry.getValue(), 
                                colors[colorIndex++ % colors.length]));
                    }
                    break;
                    
                case "Cross-Enterprise Activity":
                    Map<String, Object> transferStats = dataProvider.getCrossEnterpriseTransferStats();
                    result.add(new ChartDataPoint("Incoming", 
                            ((Long) transferStats.get("incomingTransfers")).doubleValue(), SUCCESS_GREEN));
                    result.add(new ChartDataPoint("Outgoing", 
                            ((Long) transferStats.get("outgoingTransfers")).doubleValue(), PRIMARY_BLUE));
                    break;
                    
                default:
                    // Recovery rate or other trends
                    result.add(new ChartDataPoint("Lost", dataProvider.getLostItemCount(), DANGER_RED));
                    result.add(new ChartDataPoint("Found", dataProvider.getFoundItemCount(), SUCCESS_GREEN));
                    result.add(new ChartDataPoint("Claimed", dataProvider.getClaimedItemCount(), INFO_PURPLE));
            }
        } catch (Exception e) {
            System.err.println("Error loading chart data: " + e.getMessage());
        }
        
        return result;
    }

    // ==================== TAB 3: ORGANIZATION MANAGEMENT ====================
    private JPanel createOrganizationManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Info panel showing current enterprise
        JPanel infoPanel = createEnterpriseInfoPanel();
        panel.add(infoPanel, BorderLayout.NORTH);

        // Organizations panel
        JPanel organizationsPanel = createOrganizationsPanel();
        panel.add(organizationsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEnterpriseInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INFO_PURPLE, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        Enterprise enterprise = dataProvider.getCurrentEnterprise();
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(enterprise != null ? enterprise.getName() : "Unknown Enterprise");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(DARK_GRAY);
        leftPanel.add(nameLabel);
        
        JLabel typeLabel = new JLabel("Type: " + (enterprise != null && enterprise.getType() != null 
                ? enterprise.getType().name() : "N/A"));
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        typeLabel.setForeground(Color.GRAY);
        leftPanel.add(typeLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Stats on the right
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setOpaque(false);
        
        statsPanel.add(createMiniStat("Organizations", String.valueOf(dataProvider.getOrganizations().size())));
        statsPanel.add(createMiniStat("Users", String.valueOf(dataProvider.getTotalUserCount())));
        statsPanel.add(createMiniStat("Items", String.valueOf(dataProvider.getTotalItemCount())));
        
        panel.add(statsPanel, BorderLayout.EAST);
        
        return panel;
    }

    private JPanel createMiniStat(String label, String value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(PRIMARY_BLUE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(valueLabel);
        
        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        labelLabel.setForeground(Color.GRAY);
        labelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(labelLabel);
        
        return panel;
    }

    private JPanel createOrganizationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Header with buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Organizations in " + dataProvider.getScopeDescription());
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(title, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonsPanel.setOpaque(false);

        if (dataProvider.canManageOrganizations()) {
            JButton addBtn = createStyledButton("Add Organization", SUCCESS_GREEN);
            addBtn.addActionListener(e -> showAddOrganizationDialog());
            buttonsPanel.add(addBtn);

            JButton editBtn = createStyledButton("Edit", PRIMARY_BLUE);
            editBtn.addActionListener(e -> showEditOrganizationDialog());
            buttonsPanel.add(editBtn);

            JButton deleteBtn = createStyledButton("Delete", DANGER_RED);
            deleteBtn.addActionListener(e -> deleteSelectedOrganization());
            buttonsPanel.add(deleteBtn);
        }

        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Name", "Type", "Items", "Users", "Recovery Rate", "Active"};
        organizationTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        organizationTable = new JTable(organizationTableModel);
        styleTable(organizationTable);

        panel.add(new JScrollPane(organizationTable), BorderLayout.CENTER);
        return panel;
    }

    // ==================== TAB 4: USER MANAGEMENT ====================
    private JPanel createUserManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Search and filter panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        searchPanel.add(new JLabel("Search:"));
        userSearchField = new JTextField(20);
        userSearchField.addActionListener(e -> searchUsers());
        searchPanel.add(userSearchField);

        searchPanel.add(new JLabel("Role:"));
        userRoleFilter = new JComboBox<>();
        userRoleFilter.addItem("All Roles");
        for (UserRole role : UserRole.values()) {
            userRoleFilter.addItem(role.name());
        }
        userRoleFilter.addActionListener(e -> searchUsers());
        searchPanel.add(userRoleFilter);

        searchPanel.add(new JLabel("Organization:"));
        userOrgFilter = new JComboBox<>();
        userOrgFilter.addItem("All Organizations");
        userOrgFilter.addActionListener(e -> searchUsers());
        searchPanel.add(userOrgFilter);

        JButton searchBtn = createStyledButton("Search", PRIMARY_BLUE);
        searchBtn.addActionListener(e -> searchUsers());
        searchPanel.add(searchBtn);

        JButton clearBtn = createStyledButton("Clear", new Color(149, 165, 166));
        clearBtn.addActionListener(e -> clearUserSearch());
        searchPanel.add(clearBtn);

        panel.add(searchPanel, BorderLayout.NORTH);

        // User table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        String[] columns = {"Email", "Name", "Role", "Organization", "Trust Score", "Items", "Status"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        styleTable(userTable);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(150);

        tablePanel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setOpaque(false);

        JButton changeRoleBtn = createStyledButton("Change Role", PRIMARY_BLUE);
        changeRoleBtn.addActionListener(e -> changeUserRole());
        actionsPanel.add(changeRoleBtn);

        JButton adjustTrustBtn = createStyledButton("Adjust Trust Score", WARNING_YELLOW);
        adjustTrustBtn.addActionListener(e -> adjustUserTrustScore());
        actionsPanel.add(adjustTrustBtn);

        JButton viewActivityBtn = createStyledButton("View Activity", INFO_PURPLE);
        viewActivityBtn.addActionListener(e -> viewUserActivity());
        actionsPanel.add(viewActivityBtn);

        JButton suspendBtn = createStyledButton("Suspend User", DANGER_RED);
        suspendBtn.addActionListener(e -> suspendUser());
        actionsPanel.add(suspendBtn);

        tablePanel.add(actionsPanel, BorderLayout.SOUTH);
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TAB 5: REPORTS ====================
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Report configuration panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(Color.WHITE);
        configPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Configuration - " + dataProvider.getScopeDescription()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Report Type
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Report Type:"), gbc);
        gbc.gridx = 1;
        reportTypeCombo = new JComboBox<>(new String[]{
            "Enterprise Summary", "Organization Performance", "Weekly Activity",
            "User Analytics", "Item Recovery Report", "Cross-Enterprise Transfers"
        });
        configPanel.add(reportTypeCombo, gbc);

        // Date Range
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("From Date:"), gbc);
        gbc.gridx = 1;
        reportFromDate = new JTextField(12);
        reportFromDate.setText(LocalDate.now().minusDays(30).toString());
        configPanel.add(reportFromDate, gbc);

        gbc.gridx = 2;
        configPanel.add(new JLabel("To Date:"), gbc);
        gbc.gridx = 3;
        reportToDate = new JTextField(12);
        reportToDate.setText(LocalDate.now().toString());
        configPanel.add(reportToDate, gbc);

        // Organization Filter
        gbc.gridx = 0; gbc.gridy = 2;
        configPanel.add(new JLabel("Organization:"), gbc);
        gbc.gridx = 1;
        reportOrgFilter = new JComboBox<>();
        reportOrgFilter.addItem("All Organizations");
        configPanel.add(reportOrgFilter, gbc);

        // Generate buttons
        gbc.gridx = 2; gbc.gridy = 2;
        JButton generateBtn = createStyledButton("Generate Preview", PRIMARY_BLUE);
        generateBtn.addActionListener(e -> generateReportPreview());
        configPanel.add(generateBtn, gbc);

        gbc.gridx = 3;
        JButton exportBtn = createStyledButton("Export to CSV", SUCCESS_GREEN);
        exportBtn.addActionListener(e -> exportGeneratedReport());
        configPanel.add(exportBtn, gbc);

        panel.add(configPanel, BorderLayout.NORTH);

        // Report preview area
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(Color.WHITE);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Preview"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        reportPreviewArea = new JTextArea();
        reportPreviewArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportPreviewArea.setEditable(false);
        reportPreviewArea.setLineWrap(true);
        reportPreviewArea.setWrapStyleWord(true);
        previewPanel.add(new JScrollPane(reportPreviewArea), BorderLayout.CENTER);

        panel.add(previewPanel, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TAB 6: ENTERPRISE HEALTH ====================
    private JPanel createEnterpriseHealthTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top - Status indicators
        JPanel statusPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statusPanel.setOpaque(false);

        dbStatusLabel = new JLabel("Connected");
        statusPanel.add(createHealthCard("Database Status", dbStatusLabel, SUCCESS_GREEN));

        itemCountLabel = new JLabel("0");
        statusPanel.add(createHealthCard("Total Items", itemCountLabel, PRIMARY_BLUE));

        userCountLabel = new JLabel("0");
        statusPanel.add(createHealthCard("Total Users", userCountLabel, INFO_PURPLE));

        JLabel recoveryLabel = new JLabel(String.format("%.0f%%", dataProvider.getRecoveryRate() * 100));
        statusPanel.add(createHealthCard("Recovery Rate", recoveryLabel, SUCCESS_GREEN));

        panel.add(statusPanel, BorderLayout.NORTH);

        // Center - Split into org stats and logs
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        // Left - Organization statistics
        JPanel orgPanel = new JPanel(new BorderLayout(5, 5));
        orgPanel.setBackground(Color.WHITE);
        orgPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Organization Statistics"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String[] columns = {"Organization", "Items", "Users", "Recovery Rate"};
        orgStatsDetailTable = new JTable(new Object[0][4], columns);
        styleTable(orgStatsDetailTable);
        orgPanel.add(new JScrollPane(orgStatsDetailTable), BorderLayout.CENTER);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setOpaque(false);
        JButton refreshStatsBtn = createStyledButton("Refresh Stats", PRIMARY_BLUE);
        refreshStatsBtn.addActionListener(e -> loadEnterpriseHealthData());
        refreshPanel.add(refreshStatsBtn);
        orgPanel.add(refreshPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(orgPanel);

        // Right - Activity log
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBackground(Color.WHITE);
        logPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Enterprise Activity Log"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        systemLogArea = new JTextArea();
        systemLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        systemLogArea.setEditable(false);
        systemLogArea.setBackground(new Color(40, 44, 52));
        systemLogArea.setForeground(new Color(171, 178, 191));
        logPanel.add(new JScrollPane(systemLogArea), BorderLayout.CENTER);

        JPanel logButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtonsPanel.setOpaque(false);
        JButton clearLogBtn = createStyledButton("Clear Log", new Color(149, 165, 166));
        clearLogBtn.addActionListener(e -> systemLogArea.setText(""));
        logButtonsPanel.add(clearLogBtn);
        logPanel.add(logButtonsPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(logPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createHealthCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(valueLabel);

        return card;
    }

    // ==================== DATA LOADING METHODS ====================
    private void loadAllData() {
        loadDashboardData();
        loadAnalyticsData();
        loadOrganizationData();
        loadUserData();
        loadEnterpriseHealthData();
    }

    private void loadDashboardData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Update statistics using data provider
                    totalItemsLabel.setText(String.valueOf(dataProvider.getTotalItemCount()));
                    lostItemsLabel.setText(String.valueOf(dataProvider.getLostItemCount()));
                    foundItemsLabel.setText(String.valueOf(dataProvider.getFoundItemCount()));
                    claimedItemsLabel.setText(String.valueOf(dataProvider.getClaimedItemCount()));
                    totalUsersLabel.setText(String.valueOf(dataProvider.getTotalUserCount()));
                    activeUsersLabel.setText(String.valueOf(dataProvider.getActiveUserCount()));
                    avgTrustScoreLabel.setText(String.format("%.0f%%", dataProvider.getAverageTrustScore()));
                    totalOrgsLabel.setText(String.valueOf(dataProvider.getOrganizations().size()));

                    // Update recent items table
                    List<Item> recentItems = dataProvider.getRecentItems(10);
                    Object[][] itemData = new Object[recentItems.size()][3];
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
                    for (int i = 0; i < recentItems.size(); i++) {
                        Item item = recentItems.get(i);
                        itemData[i][0] = item.getTitle().length() > 20 ? 
                                item.getTitle().substring(0, 17) + "..." : item.getTitle();
                        itemData[i][1] = item.getType() == ItemType.LOST ? "Lost" : "Found";
                        itemData[i][2] = dateFormat.format(item.getReportedDate());
                    }
                    recentItemsTable.setModel(new DefaultTableModel(itemData, new String[]{"Title", "Type", "Date"}));

                    // Update top users table
                    List<Map.Entry<User, Integer>> topUsers = dataProvider.getTopContributors(10);
                    Object[][] userData = new Object[topUsers.size()][3];
                    for (int i = 0; i < topUsers.size(); i++) {
                        Map.Entry<User, Integer> entry = topUsers.get(i);
                        userData[i][0] = entry.getKey().getFirstName();
                        userData[i][1] = entry.getValue();
                        userData[i][2] = String.format("%.0f%%", entry.getKey().getTrustScore());
                    }
                    topUsersTable.setModel(new DefaultTableModel(userData, new String[]{"Name", "Items", "Trust"}));

                    // Update organization stats table
                    Map<String, Long> orgItemCounts = dataProvider.getItemCountByOrganization();
                    Map<String, Double> orgRecoveryRates = dataProvider.getRecoveryRateByOrganization();
                    Object[][] orgData = new Object[orgItemCounts.size()][3];
                    int index = 0;
                    for (Map.Entry<String, Long> entry : orgItemCounts.entrySet()) {
                        orgData[index][0] = entry.getKey();
                        orgData[index][1] = entry.getValue();
                        double rate = orgRecoveryRates.getOrDefault(entry.getKey(), 0.0);
                        orgData[index][2] = String.format("%.0f%%", rate * 100);
                        index++;
                    }
                    orgStatsTable.setModel(new DefaultTableModel(orgData, new String[]{"Organization", "Items", "Recovery"}));

                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load dashboard data - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadAnalyticsData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Update KPI gauges
                    double recoveryRate = dataProvider.getRecoveryRate() * 100;
                    recoveryRateGauge.setText(String.format("%.0f%%", recoveryRate));

                    // Calculate SLA compliance (simplified for enterprise scope)
                    slaComplianceGauge.setText(String.format("%.0f%%", recoveryRate > 50 ? 85.0 : 60.0));

                    // Cross-enterprise activity
                    Map<String, Object> transferStats = dataProvider.getCrossEnterpriseTransferStats();
                    long totalActivity = (Long) transferStats.get("totalCrossEnterpriseActivity");
                    crossEnterpriseGauge.setText(String.valueOf(totalActivity));

                    // Update chart
                    updateAnalyticsChart();
                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load analytics - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadOrganizationData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    List<Organization> organizations = dataProvider.getOrganizations();
                    Map<String, Long> itemCounts = dataProvider.getItemCountByOrganization();
                    Map<String, Long> userCounts = dataProvider.getUserCountByOrganization();
                    Map<String, Double> recoveryRates = dataProvider.getRecoveryRateByOrganization();

                    organizationTableModel.setRowCount(0);

                    for (Organization org : organizations) {
                        String name = org.getName();
                        organizationTableModel.addRow(new Object[]{
                            org.getOrganizationId(),
                            name,
                            org.getType() != null ? org.getType().name() : "N/A",
                            itemCounts.getOrDefault(name, 0L),
                            userCounts.getOrDefault(name, 0L),
                            String.format("%.0f%%", recoveryRates.getOrDefault(name, 0.0) * 100),
                            org.isActive() ? "Active" : "Inactive"
                        });
                    }

                    // Populate organization filter dropdowns
                    userOrgFilter.removeAllItems();
                    userOrgFilter.addItem("All Organizations");
                    reportOrgFilter.removeAllItems();
                    reportOrgFilter.addItem("All Organizations");
                    for (Organization org : organizations) {
                        userOrgFilter.addItem(org.getName());
                        reportOrgFilter.addItem(org.getName());
                    }

                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load organization data - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadUserData() {
        searchUsers();
    }

    private void loadEnterpriseHealthData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    dbStatusLabel.setText("Connected");
                    dbStatusLabel.setForeground(SUCCESS_GREEN);

                    itemCountLabel.setText(String.valueOf(dataProvider.getTotalItemCount()));
                    userCountLabel.setText(String.valueOf(dataProvider.getTotalUserCount()));

                    // Update org stats detail table
                    Map<String, Long> itemCounts = dataProvider.getItemCountByOrganization();
                    Map<String, Long> userCounts = dataProvider.getUserCountByOrganization();
                    Map<String, Double> recoveryRates = dataProvider.getRecoveryRateByOrganization();

                    Object[][] data = new Object[itemCounts.size()][4];
                    int index = 0;
                    for (String orgName : itemCounts.keySet()) {
                        data[index][0] = orgName;
                        data[index][1] = itemCounts.getOrDefault(orgName, 0L);
                        data[index][2] = userCounts.getOrDefault(orgName, 0L);
                        data[index][3] = String.format("%.0f%%", recoveryRates.getOrDefault(orgName, 0.0) * 100);
                        index++;
                    }
                    orgStatsDetailTable.setModel(new DefaultTableModel(data, 
                            new String[]{"Organization", "Items", "Users", "Recovery Rate"}));

                    // Update activity log
                    StringBuilder log = new StringBuilder();
                    log.append("=== Enterprise Health Check ===\n");
                    log.append("Enterprise: ").append(dataProvider.getScopeDescription()).append("\n");
                    log.append("Time: ").append(new Date()).append("\n\n");
                    log.append("[INFO] Database connection: OK\n");
                    log.append("[INFO] Total organizations: ").append(dataProvider.getOrganizations().size()).append("\n");
                    log.append("[INFO] Total items: ").append(dataProvider.getTotalItemCount()).append("\n");
                    log.append("[INFO] Total users: ").append(dataProvider.getTotalUserCount()).append("\n");
                    log.append("[INFO] Recovery rate: ").append(String.format("%.1f%%", dataProvider.getRecoveryRate() * 100)).append("\n");
                    
                    Map<String, Object> transfers = dataProvider.getCrossEnterpriseTransferStats();
                    log.append("\n=== Cross-Enterprise Activity ===\n");
                    log.append("Incoming transfers: ").append(transfers.get("incomingTransfers")).append("\n");
                    log.append("Outgoing transfers: ").append(transfers.get("outgoingTransfers")).append("\n");

                    systemLogArea.setText(log.toString());

                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load health data - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ==================== ACTION METHODS ====================
    private void searchUsers() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() {
                String searchText = userSearchField.getText();
                String roleFilter = (String) userRoleFilter.getSelectedItem();
                String orgFilter = (String) userOrgFilter.getSelectedItem();

                List<User> users = dataProvider.searchUsers(searchText, roleFilter, null);

                // Apply organization filter if not "All"
                if (!"All Organizations".equals(orgFilter) && orgFilter != null) {
                    users = users.stream()
                            .filter(u -> {
                                String orgName = getOrganizationName(u.getOrganizationId());
                                return orgFilter.equals(orgName);
                            })
                            .collect(Collectors.toList());
                }

                return users;
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    userTableModel.setRowCount(0);

                    for (User user : users) {
                        userTableModel.addRow(new Object[]{
                            user.getEmail(),
                            user.getFullName(),
                            user.getRole() != null ? user.getRole().name() : "N/A",
                            getOrganizationName(user.getOrganizationId()),
                            String.format("%.0f", user.getTrustScore()),
                            user.getItemsReported(),
                            user.isActive() ? "Active" : "Suspended"
                        });
                    }
                } catch (Exception e) {
                    logToSystem("ERROR: Failed to search users - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void clearUserSearch() {
        userSearchField.setText("");
        userRoleFilter.setSelectedIndex(0);
        userOrgFilter.setSelectedIndex(0);
        searchUsers();
    }

    private void changeUserRole() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = (String) userTableModel.getValueAt(selectedRow, 0);
        User user = userDAO.findByEmail(email).orElse(null);
        if (user == null) return;

        UserRole[] roles = UserRole.values();
        UserRole newRole = (UserRole) JOptionPane.showInputDialog(
                this, "Select new role for " + user.getFullName() + ":",
                "Change Role", JOptionPane.QUESTION_MESSAGE, null, roles, user.getRole()
        );

        if (newRole != null && newRole != user.getRole()) {
            user.setRole(newRole);
            userDAO.update(user);
            searchUsers();
            JOptionPane.showMessageDialog(this, "Role updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Changed role for " + email + " to " + newRole.name());
        }
    }

    private void adjustUserTrustScore() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = (String) userTableModel.getValueAt(selectedRow, 0);
        User user = userDAO.findByEmail(email).orElse(null);
        if (user == null) return;

        String input = JOptionPane.showInputDialog(
                this,
                "Current trust score: " + String.format("%.0f", user.getTrustScore()) + "\nEnter new trust score (0-100):",
                "Adjust Trust Score", JOptionPane.QUESTION_MESSAGE
        );

        if (input != null) {
            try {
                double newScore = Double.parseDouble(input);
                if (newScore < 0 || newScore > 100) {
                    JOptionPane.showMessageDialog(this, "Score must be between 0 and 100.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                user.setTrustScore(newScore);
                userDAO.update(user);
                searchUsers();
                JOptionPane.showMessageDialog(this, "Trust score updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                logToSystem("INFO: Adjusted trust score for " + email + " to " + newScore);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewUserActivity() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = (String) userTableModel.getValueAt(selectedRow, 0);
        User user = userDAO.findByEmail(email).orElse(null);
        if (user == null) return;

        List<Item> userItems = dataProvider.getItems().stream()
                .filter(i -> i.getReportedBy() != null && email.equals(i.getReportedBy().getEmail()))
                .collect(Collectors.toList());

        StringBuilder activity = new StringBuilder();
        activity.append("=== Activity Log for ").append(user.getFullName()).append(" ===\n\n");
        activity.append("Email: ").append(email).append("\n");
        activity.append("Role: ").append(user.getRole()).append("\n");
        activity.append("Organization: ").append(getOrganizationName(user.getOrganizationId())).append("\n");
        activity.append("Trust Score: ").append(String.format("%.0f", user.getTrustScore())).append("\n");
        activity.append("Items Reported: ").append(user.getItemsReported()).append("\n");
        activity.append("Items Returned: ").append(user.getItemsReturned()).append("\n\n");

        activity.append("Recent Items in Enterprise:\n");
        int count = 0;
        for (Item item : userItems) {
            if (count++ >= 10) break;
            activity.append("  - ").append(item.getTitle()).append(" (").append(item.getType()).append(")\n");
        }

        JTextArea textArea = new JTextArea(activity.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "User Activity", JOptionPane.INFORMATION_MESSAGE);
    }

    private void suspendUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = (String) userTableModel.getValueAt(selectedRow, 0);
        User user = userDAO.findByEmail(email).orElse(null);
        if (user == null) return;

        String[] options = {"Suspend", "Reactivate", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "User: " + user.getFullName() + "\nCurrent Status: " + (user.isActive() ? "Active" : "Suspended"),
                "Suspend User",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]
        );

        if (choice == 0) {
            user.setActive(false);
            userDAO.update(user);
            searchUsers();
            JOptionPane.showMessageDialog(this, "User suspended successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("WARNING: Suspended user " + email);
        } else if (choice == 1) {
            user.setActive(true);
            userDAO.update(user);
            searchUsers();
            JOptionPane.showMessageDialog(this, "User reactivated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Reactivated user " + email);
        }
    }

    private void showAddOrganizationDialog() {
        JTextField nameField = new JTextField(20);
        JComboBox<OrganizationType> typeCombo = new JComboBox<>(OrganizationType.values());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Enterprise:"));
        panel.add(new JLabel(dataProvider.getScopeDescription()));

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Organization",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            Organization org = new Organization();
            org.setOrganizationId("ORG-" + System.currentTimeMillis());
            org.setName(nameField.getText().trim());
            org.setType((OrganizationType) typeCombo.getSelectedItem());
            org.setEnterpriseId(dataProvider.getEnterpriseId());
            org.setActive(true);
            org.setCreatedDate(new Date());

            organizationDAO.create(org);
            dataProvider.refreshOrganizationCache();
            loadOrganizationData();
            JOptionPane.showMessageDialog(this, "Organization added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Added new organization - " + org.getName());
        }
    }

    private void showEditOrganizationDialog() {
        int selectedRow = organizationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an organization first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orgId = (String) organizationTableModel.getValueAt(selectedRow, 0);
        Organization org = dataProvider.getOrganizations().stream()
                .filter(o -> o.getOrganizationId().equals(orgId))
                .findFirst().orElse(null);

        if (org == null) return;

        JTextField nameField = new JTextField(org.getName(), 20);
        JCheckBox activeCheck = new JCheckBox("Active", org.isActive());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Status:"));
        panel.add(activeCheck);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Organization",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            org.setName(nameField.getText().trim());
            org.setActive(activeCheck.isSelected());

            organizationDAO.update(org);
            loadOrganizationData();
            JOptionPane.showMessageDialog(this, "Organization updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Updated organization - " + org.getName());
        }
    }

    private void deleteSelectedOrganization() {
        int selectedRow = organizationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an organization first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orgId = (String) organizationTableModel.getValueAt(selectedRow, 0);
        String orgName = (String) organizationTableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete organization: " + orgName + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            organizationDAO.delete(orgId);
            dataProvider.refreshOrganizationCache();
            loadOrganizationData();
            JOptionPane.showMessageDialog(this, "Organization deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("WARNING: Deleted organization - " + orgName);
        }
    }

    private void generateReportPreview() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        StringBuilder preview = new StringBuilder();

        preview.append("=== ").append(reportType).append(" ===\n");
        preview.append("Enterprise: ").append(dataProvider.getScopeDescription()).append("\n");
        preview.append("Generated: ").append(new Date()).append("\n");
        preview.append("Date Range: ").append(reportFromDate.getText()).append(" to ").append(reportToDate.getText()).append("\n\n");

        switch (reportType) {
            case "Enterprise Summary":
                preview.append("Enterprise Summary Report\n\n");
                preview.append("Total Items: ").append(dataProvider.getTotalItemCount()).append("\n");
                preview.append("Lost Items: ").append(dataProvider.getLostItemCount()).append("\n");
                preview.append("Found Items: ").append(dataProvider.getFoundItemCount()).append("\n");
                preview.append("Claimed Items: ").append(dataProvider.getClaimedItemCount()).append("\n");
                preview.append("Total Users: ").append(dataProvider.getTotalUserCount()).append("\n");
                preview.append("Active Users: ").append(dataProvider.getActiveUserCount()).append("\n");
                preview.append("Average Trust Score: ").append(String.format("%.1f", dataProvider.getAverageTrustScore())).append("\n");
                preview.append("Recovery Rate: ").append(String.format("%.1f%%", dataProvider.getRecoveryRate() * 100)).append("\n");
                break;

            case "Organization Performance":
                preview.append("Organization Performance Report\n\n");
                Map<String, Long> itemCounts = dataProvider.getItemCountByOrganization();
                Map<String, Long> userCounts = dataProvider.getUserCountByOrganization();
                Map<String, Double> recoveryRates = dataProvider.getRecoveryRateByOrganization();
                
                for (String orgName : itemCounts.keySet()) {
                    preview.append(orgName).append(":\n");
                    preview.append("  Items: ").append(itemCounts.getOrDefault(orgName, 0L));
                    preview.append(" | Users: ").append(userCounts.getOrDefault(orgName, 0L));
                    preview.append(" | Recovery: ").append(String.format("%.0f%%", recoveryRates.getOrDefault(orgName, 0.0) * 100)).append("\n");
                }
                break;

            case "Cross-Enterprise Transfers":
                preview.append("Cross-Enterprise Transfer Report\n\n");
                Map<String, Object> transfers = dataProvider.getCrossEnterpriseTransferStats();
                preview.append("Incoming Transfers: ").append(transfers.get("incomingTransfers")).append("\n");
                preview.append("Outgoing Transfers: ").append(transfers.get("outgoingTransfers")).append("\n");
                preview.append("Total Activity: ").append(transfers.get("totalCrossEnterpriseActivity")).append("\n");
                break;

            default:
                preview.append("Quick Statistics\n\n");
                preview.append("Total Items: ").append(dataProvider.getTotalItemCount()).append("\n");
                preview.append("Recovery Rate: ").append(String.format("%.0f%%", dataProvider.getRecoveryRate() * 100)).append("\n");
                preview.append("Organizations: ").append(dataProvider.getOrganizations().size()).append("\n");
        }

        reportPreviewArea.setText(preview.toString());
    }

    private void exportGeneratedReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(
                dataProvider.getScopeDescription().replace(" ", "_") + "_" + 
                reportType.replace(" ", "_") + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Files.writeString(file.toPath(), reportPreviewArea.getText());
                JOptionPane.showMessageDialog(this, "Report exported to: " + file.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                logToSystem("INFO: Exported report to " + file.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export report: " + e.getMessage(),
                        "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportReport() {
        generateReportPreview();
        exportGeneratedReport();
    }

    // ==================== UTILITY METHODS ====================
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setGridColor(new Color(230, 230, 230));
        table.setShowGrid(true);
    }

    private String getOrganizationName(String organizationId) {
        if (organizationId == null) return "N/A";
        return dataProvider.getOrganizations().stream()
                .filter(o -> o.getOrganizationId().equals(organizationId))
                .map(Organization::getName)
                .findFirst().orElse("Unknown");
    }

    private void logToSystem(String message) {
        if (systemLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                systemLogArea.append(new Date() + " - " + message + "\n");
            });
        }
    }

    // ==================== INNER CLASS FOR CHART DATA ====================
    private static class ChartDataPoint {
        String label;
        double value;
        Color color;

        ChartDataPoint(String label, double value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

        String getFormattedValue() {
            if (value == (int) value) {
                return String.valueOf((int) value);
            }
            return String.format("%.1f", value);
        }
    }
}
