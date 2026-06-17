package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.Enterprise.EnterpriseType;
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
 * Enhanced Admin Panel with comprehensive analytics, management, and reporting
 * capabilities.
 *
 * Features: - Dashboard: Quick stats and recent activity - System Analytics:
 * Charts and metrics visualization - Enterprise Management: CRUD for
 * enterprises and organizations - User Management: Search, role management,
 * trust scores, ban/suspend - Advanced Reports: Custom report generation with
 * filters and export - System Health: Database status, performance metrics
 *
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class AdminPanel extends JPanel {

    // ==================== DEPENDENCIES ====================
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoUserDAO userDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private MongoWorkRequestDAO workRequestDAO;
    private MongoTrustScoreDAO trustScoreDAO;
    private AnalyticsService analyticsService;
    private ReportExportService reportExportService;
    private TrustScoreService trustScoreService;

    // ==================== UI COMPONENTS ====================
    private JTabbedPane mainTabbedPane;

    // Dashboard tab components
    private JLabel totalItemsLabel, lostItemsLabel, foundItemsLabel, claimedItemsLabel;
    private JLabel totalUsersLabel, activeUsersLabel, avgTrustScoreLabel, totalBuildingsLabel;
    private JTable recentItemsTable, topUsersTable, buildingStatsTable;

    // Analytics tab components
    private JPanel analyticsChartsPanel;
    private JComboBox<String> chartTypeCombo;
    private JLabel recoveryRateGauge, slaComplianceGauge, networkEffectGauge;
    private JTable trendsTable;

    // Enterprise Management tab components
    private JTable enterpriseTable, organizationTable;
    private DefaultTableModel enterpriseTableModel, organizationTableModel;

    // User Management tab components
    private JTextField userSearchField;
    private JComboBox<String> userRoleFilter, userEnterpriseFilter;
    private JTable userTable;
    private DefaultTableModel userTableModel;

    // Reports tab components
    private JComboBox<String> reportTypeCombo;
    private JTextField reportFromDate, reportToDate;
    private JComboBox<String> reportEnterpriseFilter;
    private JTextArea reportPreviewArea;

    // System Health tab components
    private JLabel dbStatusLabel, dbConnectionLabel, totalDocumentsLabel;
    private JTable collectionStatsTable;
    private JTextArea systemLogArea;

    // ==================== COLORS ====================
    private static final Color PRIMARY_BLUE = new Color(52, 152, 219);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color WARNING_YELLOW = new Color(241, 196, 15);
    private static final Color DANGER_RED = new Color(231, 76, 60);
    private static final Color INFO_PURPLE = new Color(155, 89, 182);
    private static final Color DARK_GRAY = new Color(52, 73, 94);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);

    // ==================== CONSTRUCTOR ====================
    public AdminPanel(User currentUser) {
        this.currentUser = currentUser;
        initializeDAOs();
        initComponents();
        loadAllData();
    }

    private void initializeDAOs() {
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.analyticsService = new AnalyticsService();
        this.reportExportService = new ReportExportService();
        this.trustScoreService = new TrustScoreService();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY);

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main tabbed pane
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Add tabs
        mainTabbedPane.addTab("Dashboard", createDashboardTab());
        mainTabbedPane.addTab("System Analytics", createAnalyticsTab());
        mainTabbedPane.addTab("Enterprise Management", createEnterpriseManagementTab());
        mainTabbedPane.addTab("User Management", createUserManagementTab());
        mainTabbedPane.addTab("Advanced Reports", createAdvancedReportsTab());
        mainTabbedPane.addTab("System Health", createSystemHealthTab());

        // Tab change listener to refresh data
        mainTabbedPane.addChangeListener(e -> {
            int selectedIndex = mainTabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case 0:
                    loadDashboardData();
                    break;
                case 1:
                    loadAnalyticsData();
                    break;
                case 2:
                    loadEnterpriseData();
                    break;
                case 3:
                    loadUserData();
                    break;
                case 5:
                    loadSystemHealthData();
                    break;
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

        JLabel titleLabel = new JLabel("Admin Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(DARK_GRAY);
        panel.add(titleLabel, BorderLayout.WEST);

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

        // Statistics cards
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
        totalBuildingsLabel = new JLabel("0");

        panel.add(createStatCard("Total Items", totalItemsLabel, PRIMARY_BLUE));
        panel.add(createStatCard("Lost Items", lostItemsLabel, DANGER_RED));
        panel.add(createStatCard("Found Items", foundItemsLabel, SUCCESS_GREEN));
        panel.add(createStatCard("Claimed", claimedItemsLabel, INFO_PURPLE));
        panel.add(createStatCard("Total Users", totalUsersLabel, DARK_GRAY));
        panel.add(createStatCard("Active Users", activeUsersLabel, new Color(22, 160, 133)));
        panel.add(createStatCard("Avg Trust", avgTrustScoreLabel, WARNING_YELLOW));
        panel.add(createStatCard("Buildings", totalBuildingsLabel, new Color(149, 165, 166)));

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
        panel.add(createTableSection("Building Activity", createBuildingStatsTable()));

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

    private JScrollPane createBuildingStatsTable() {
        String[] columns = {"Building", "Items", "Activity"};
        buildingStatsTable = new JTable(new Object[0][3], columns);
        styleTable(buildingStatsTable);
        return new JScrollPane(buildingStatsTable);
    }

    // ==================== TAB 2: SYSTEM ANALYTICS ====================
    private JPanel createAnalyticsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        controlsPanel.setOpaque(false);

        controlsPanel.add(new JLabel("Chart Type:"));
        chartTypeCombo = new JComboBox<>(new String[]{
            "Recovery Rate Trend", "Items by Category", "Items by Status",
            "Enterprise Comparison", "Request Volume", "Trust Score Distribution"
        });
        chartTypeCombo.addActionListener(e -> updateAnalyticsChart());
        controlsPanel.add(chartTypeCombo);

        JButton refreshChartBtn = createStyledButton("Refresh", PRIMARY_BLUE);
        refreshChartBtn.addActionListener(e -> loadAnalyticsData());
        controlsPanel.add(refreshChartBtn);

        panel.add(controlsPanel, BorderLayout.NORTH);

        // Main content - split into gauges and chart area
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setOpaque(false);

        // Left side - KPI Gauges
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

        JLabel kpiTitle = new JLabel("Key Metrics");
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

        // Network Effect Gauge
        panel.add(createGaugePanel("Network Effect", networkEffectGauge = new JLabel("0%"), INFO_PURPLE));
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

        // Progress bar
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

        JLabel title = new JLabel("Weekly Trends (Last 8 Weeks)");
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

        // Create simple bar chart visualization
        JPanel barsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart(g, chartType);
            }
        };
        barsPanel.setBackground(Color.WHITE);
        chartPanel.add(barsPanel, BorderLayout.CENTER);

        // Legend
        JPanel legendPanel = createChartLegend(chartType);
        chartPanel.add(legendPanel, BorderLayout.SOUTH);

        analyticsChartsPanel.add(chartPanel, BorderLayout.CENTER);
        analyticsChartsPanel.revalidate();
        analyticsChartsPanel.repaint();
    }

    private void drawChart(Graphics g, String chartType) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<ChartDataPoint> data = getChartData(chartType);
        if (data.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("No data available", 100, 100);
            return;
        }

        int width = analyticsChartsPanel.getWidth() - 100;
        int height = analyticsChartsPanel.getHeight() - 100;
        int barWidth = Math.max(30, (width - 50) / data.size() - 10);
        int maxValue = (int) data.stream().mapToDouble(d -> d.value).max().orElse(100);
        if (maxValue == 0) {
            maxValue = 1;
        }

        int x = 50;
        for (ChartDataPoint point : data) {
            int barHeight = (int) ((point.value / maxValue) * (height - 50));

            g2.setColor(point.color);
            g2.fillRect(x, height - barHeight, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            // Value on top
            String valueStr = point.getFormattedValue();
            g2.drawString(valueStr, x + barWidth / 4, height - barHeight - 5);

            // Label at bottom (truncated)
            String label = point.label.length() > 10 ? point.label.substring(0, 8) + ".." : point.label;
            g2.drawString(label, x, height + 15);

            x += barWidth + 10;
        }
    }

    private List<ChartDataPoint> getChartData(String chartType) {
        try {
            switch (chartType) {
                case "Items by Category":
                    return analyticsService.getCategoryPieData();
                case "Items by Status":
                    return analyticsService.getItemStatusPieData();
                case "Enterprise Comparison":
                    return analyticsService.getEnterpriseComparisonData();
                case "Trust Score Distribution":
                    return analyticsService.getTrustScorePieData();
                case "Recovery Rate Trend":
                case "Request Volume":
                default:
                    // Convert time series to bar chart
                    List<TimeSeriesPoint> timeSeries = chartType.equals("Request Volume")
                            ? analyticsService.getRequestVolumeData(14)
                            : analyticsService.getRecoveryTrendData(14);
                    List<ChartDataPoint> result = new ArrayList<>();
                    for (TimeSeriesPoint ts : timeSeries) {
                        result.add(new ChartDataPoint(ts.getShortDateLabel(), ts.value, PRIMARY_BLUE));
                    }
                    return result;
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private JPanel createChartLegend(String chartType) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 30));

        List<ChartDataPoint> data = getChartData(chartType);
        int maxItems = Math.min(6, data.size());

        for (int i = 0; i < maxItems; i++) {
            ChartDataPoint point = data.get(i);
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
            item.setOpaque(false);

            JLabel colorBox = new JLabel("■");
            colorBox.setForeground(point.color);
            item.add(colorBox);

            JLabel label = new JLabel(point.label.length() > 15
                    ? point.label.substring(0, 12) + "..." : point.label);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            item.add(label);

            panel.add(item);
        }

        return panel;
    }

    // ==================== TAB 3: ENTERPRISE MANAGEMENT ====================
    private JPanel createEnterpriseManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Split into enterprises and organizations
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        // Top - Enterprises
        JPanel enterprisesPanel = createEnterprisesPanel();
        splitPane.setTopComponent(enterprisesPanel);

        // Bottom - Organizations
        JPanel organizationsPanel = createOrganizationsPanel();
        splitPane.setBottomComponent(organizationsPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEnterprisesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Header with buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Enterprises");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(title, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonsPanel.setOpaque(false);

        JButton addBtn = createStyledButton("Add Enterprise", SUCCESS_GREEN);
        addBtn.addActionListener(e -> showAddEnterpriseDialog());
        buttonsPanel.add(addBtn);

        JButton editBtn = createStyledButton("Edit", PRIMARY_BLUE);
        editBtn.addActionListener(e -> showEditEnterpriseDialog());
        buttonsPanel.add(editBtn);

        JButton deleteBtn = createStyledButton("Delete", DANGER_RED);
        deleteBtn.addActionListener(e -> deleteSelectedEnterprise());
        buttonsPanel.add(deleteBtn);

        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Name", "Type", "Items", "Users", "Active"};
        enterpriseTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        enterpriseTable = new JTable(enterpriseTableModel);
        styleTable(enterpriseTable);
        enterpriseTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        enterpriseTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        panel.add(new JScrollPane(enterpriseTable), BorderLayout.CENTER);
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

        JLabel title = new JLabel("Organizations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(title, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonsPanel.setOpaque(false);

        JButton addBtn = createStyledButton("Add Organization", SUCCESS_GREEN);
        addBtn.addActionListener(e -> showAddOrganizationDialog());
        buttonsPanel.add(addBtn);

        JButton editBtn = createStyledButton("Edit", PRIMARY_BLUE);
        editBtn.addActionListener(e -> showEditOrganizationDialog());
        buttonsPanel.add(editBtn);

        JButton deleteBtn = createStyledButton("Delete", DANGER_RED);
        deleteBtn.addActionListener(e -> deleteSelectedOrganization());
        buttonsPanel.add(deleteBtn);

        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Name", "Type", "Enterprise", "Items", "Active"};
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

        searchPanel.add(new JLabel("Enterprise:"));
        userEnterpriseFilter = new JComboBox<>();
        userEnterpriseFilter.addItem("All Enterprises");
        userEnterpriseFilter.addActionListener(e -> searchUsers());
        searchPanel.add(userEnterpriseFilter);

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

        String[] columns = {"Email", "Name", "Role", "Enterprise", "Trust Score", "Items", "Status"};
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

        JButton banUserBtn = createStyledButton("Ban/Suspend", DANGER_RED);
        banUserBtn.addActionListener(e -> banOrSuspendUser());
        actionsPanel.add(banUserBtn);

        tablePanel.add(actionsPanel, BorderLayout.SOUTH);
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TAB 5: ADVANCED REPORTS ====================
    private JPanel createAdvancedReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Report configuration panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(Color.WHITE);
        configPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Configuration"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Report Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("Report Type:"), gbc);
        gbc.gridx = 1;
        reportTypeCombo = new JComboBox<>(new String[]{
            "Executive Summary", "Weekly Activity", "Monthly Summary",
            "Enterprise Report", "User Analytics", "Item Recovery Report",
            "SLA Compliance Report", "Network Effect Analysis"
        });
        configPanel.add(reportTypeCombo, gbc);

        // Date Range
        gbc.gridx = 0;
        gbc.gridy = 1;
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

        // Enterprise Filter
        gbc.gridx = 0;
        gbc.gridy = 2;
        configPanel.add(new JLabel("Enterprise:"), gbc);
        gbc.gridx = 1;
        reportEnterpriseFilter = new JComboBox<>();
        reportEnterpriseFilter.addItem("All Enterprises");
        configPanel.add(reportEnterpriseFilter, gbc);

        // Generate buttons
        gbc.gridx = 2;
        gbc.gridy = 2;
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

    // ==================== TAB 6: SYSTEM HEALTH ====================
    private JPanel createSystemHealthTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top - Status indicators
        JPanel statusPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statusPanel.setOpaque(false);

        dbStatusLabel = new JLabel("Unknown");
        statusPanel.add(createHealthCard("Database Status", dbStatusLabel, PRIMARY_BLUE));

        dbConnectionLabel = new JLabel("Checking...");
        statusPanel.add(createHealthCard("Connection", dbConnectionLabel, SUCCESS_GREEN));

        totalDocumentsLabel = new JLabel("0");
        statusPanel.add(createHealthCard("Total Documents", totalDocumentsLabel, INFO_PURPLE));

        JLabel uptimeLabel = new JLabel("Active");
        statusPanel.add(createHealthCard("System Status", uptimeLabel, SUCCESS_GREEN));

        panel.add(statusPanel, BorderLayout.NORTH);

        // Center - Split into collection stats and logs
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        // Left - Collection statistics
        JPanel collectionsPanel = new JPanel(new BorderLayout(5, 5));
        collectionsPanel.setBackground(Color.WHITE);
        collectionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Collection Statistics"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String[] columns = {"Collection", "Documents", "Indexes", "Size (KB)"};
        collectionStatsTable = new JTable(new Object[0][4], columns);
        styleTable(collectionStatsTable);
        collectionsPanel.add(new JScrollPane(collectionStatsTable), BorderLayout.CENTER);

        // Refresh button
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setOpaque(false);
        JButton refreshStatsBtn = createStyledButton("Refresh Stats", PRIMARY_BLUE);
        refreshStatsBtn.addActionListener(e -> loadSystemHealthData());
        refreshPanel.add(refreshStatsBtn);
        collectionsPanel.add(refreshPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(collectionsPanel);

        // Right - System log
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBackground(Color.WHITE);
        logPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("System Log / Alerts"),
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
        JButton checkAlertsBtn = createStyledButton("Check Alerts", WARNING_YELLOW);
        checkAlertsBtn.addActionListener(e -> checkSystemAlerts());
        logButtonsPanel.add(checkAlertsBtn);
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
        loadEnterpriseData();
        loadUserData();
        loadSystemHealthData();
    }

    private void loadDashboardData() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> stats = new HashMap<>();
                try {
                    List<Item> allItems = itemDAO.findAll();
                    stats.put("totalItems", allItems.size());
                    stats.put("lostItems", allItems.stream().filter(i -> i.getType() == ItemType.LOST).count());
                    stats.put("foundItems", allItems.stream().filter(i -> i.getType() == ItemType.FOUND).count());
                    stats.put("claimedItems", allItems.stream().filter(i -> i.getStatus() == ItemStatus.CLAIMED).count());

                    List<User> allUsers = userDAO.findAll();
                    stats.put("totalUsers", allUsers.size());
                    stats.put("avgTrust", allUsers.stream().mapToDouble(User::getTrustScore).average().orElse(0.0));

                    Set<Integer> activeUserIds = new HashSet<>();
                    for (Item item : allItems) {
                        if (item.getReportedBy() != null) {
                            activeUserIds.add(item.getReportedBy().getUserId());
                        }
                    }
                    stats.put("activeUsers", activeUserIds.size());
                    stats.put("totalBuildings", buildingDAO.count());

                    // Recent items
                    List<Item> recentItems = allItems.stream()
                            .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                            .limit(10).toList();
                    stats.put("recentItems", recentItems);

                    // Top users
                    Map<User, Integer> userItemCounts = new HashMap<>();
                    for (Item item : allItems) {
                        User reporter = item.getReportedBy();
                        if (reporter != null) {
                            userItemCounts.put(reporter, userItemCounts.getOrDefault(reporter, 0) + 1);
                        }
                    }
                    List<Map.Entry<User, Integer>> topUsers = userItemCounts.entrySet().stream()
                            .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                            .limit(10).toList();
                    stats.put("topUsers", topUsers);

                    // Building stats
                    Map<String, Integer> buildingCounts = new HashMap<>();
                    for (Item item : allItems) {
                        if (item.getLocation() != null && item.getLocation().getBuilding() != null) {
                            String code = item.getLocation().getBuilding().getCode();
                            buildingCounts.put(code, buildingCounts.getOrDefault(code, 0) + 1);
                        }
                    }
                    stats.put("buildingStats", buildingCounts);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return stats;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> stats = get();
                    updateDashboardUI(stats);
                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load dashboard data - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateDashboardUI(Map<String, Object> stats) {
        totalItemsLabel.setText(String.valueOf(stats.getOrDefault("totalItems", 0)));
        lostItemsLabel.setText(String.valueOf(stats.getOrDefault("lostItems", 0L)));
        foundItemsLabel.setText(String.valueOf(stats.getOrDefault("foundItems", 0L)));
        claimedItemsLabel.setText(String.valueOf(stats.getOrDefault("claimedItems", 0L)));
        totalUsersLabel.setText(String.valueOf(stats.getOrDefault("totalUsers", 0)));
        activeUsersLabel.setText(String.valueOf(stats.getOrDefault("activeUsers", 0)));
        avgTrustScoreLabel.setText(String.format("%.0f%%", (Double) stats.getOrDefault("avgTrust", 0.0)));
        totalBuildingsLabel.setText(String.valueOf(stats.getOrDefault("totalBuildings", 0L)));

        // Update recent items table
        List<Item> recentItems = (List<Item>) stats.getOrDefault("recentItems", new ArrayList<>());
        Object[][] itemData = new Object[recentItems.size()][3];
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
        for (int i = 0; i < recentItems.size(); i++) {
            Item item = recentItems.get(i);
            itemData[i][0] = item.getTitle().length() > 20 ? item.getTitle().substring(0, 17) + "..." : item.getTitle();
            itemData[i][1] = item.getType() == ItemType.LOST ? "Lost" : "Found";
            itemData[i][2] = dateFormat.format(item.getReportedDate());
        }
        recentItemsTable.setModel(new DefaultTableModel(itemData, new String[]{"Title", "Type", "Date"}));

        // Update top users table
        List<Map.Entry<User, Integer>> topUsers = (List<Map.Entry<User, Integer>>) stats.getOrDefault("topUsers", new ArrayList<>());
        Object[][] userData = new Object[topUsers.size()][3];
        for (int i = 0; i < topUsers.size(); i++) {
            Map.Entry<User, Integer> entry = topUsers.get(i);
            userData[i][0] = entry.getKey().getFirstName();
            userData[i][1] = entry.getValue();
            userData[i][2] = String.format("%.0f%%", entry.getKey().getTrustScore());
        }
        topUsersTable.setModel(new DefaultTableModel(userData, new String[]{"Name", "Items", "Trust"}));

        // Update building stats table
        Map<String, Integer> buildingStats = (Map<String, Integer>) stats.getOrDefault("buildingStats", new HashMap<>());
        Object[][] buildingData = new Object[buildingStats.size()][3];
        int index = 0;
        for (Map.Entry<String, Integer> entry : buildingStats.entrySet()) {
            buildingData[index][0] = entry.getKey();
            buildingData[index][1] = entry.getValue();
            buildingData[index][2] = entry.getValue() > 5 ? "High" : entry.getValue() > 2 ? "Medium" : "Low";
            index++;
        }
        buildingStatsTable.setModel(new DefaultTableModel(buildingData, new String[]{"Building", "Items", "Activity"}));
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
                    double recoveryRate = analyticsService.getRecoveryRate() * 100;
                    recoveryRateGauge.setText(String.format("%.0f%%", recoveryRate));

                    double slaCompliance = analyticsService.getSLAComplianceRate() * 100;
                    slaComplianceGauge.setText(String.format("%.0f%%", slaCompliance));

                    NetworkEffectStats networkStats = analyticsService.getNetworkEffectMetrics();
                    networkEffectGauge.setText(String.format("%.0f%%", networkStats.getImprovementPercentage()));

                    // Update trends table
                    List<WeeklyTrend> trends = analyticsService.getWeeklyTrends(8);
                    Object[][] trendsData = new Object[trends.size()][6];
                    for (int i = 0; i < trends.size(); i++) {
                        WeeklyTrend trend = trends.get(i);
                        trendsData[i] = trend.toTableRow();
                    }
                    trendsTable.setModel(new DefaultTableModel(trendsData, WeeklyTrend.getTableColumns()));

                    // Update chart
                    updateAnalyticsChart();
                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load analytics - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadEnterpriseData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Load enterprises
                    List<Enterprise> enterprises = enterpriseDAO.findAll();
                    enterpriseTableModel.setRowCount(0);

                    for (Enterprise ent : enterprises) {
                        long itemCount = itemDAO.findAll().stream()
                                .filter(i -> ent.getEnterpriseId().equals(i.getEnterpriseId())).count();
                        long userCount = userDAO.findAll().stream()
                                .filter(u -> ent.getEnterpriseId().equals(u.getEnterpriseId())).count();

                        enterpriseTableModel.addRow(new Object[]{
                            ent.getEnterpriseId(),
                            ent.getName(),
                            ent.getType() != null ? ent.getType().name() : "N/A",
                            itemCount,
                            userCount,
                            ent.isActive() ? "Active" : "Inactive"
                        });
                    }

                    // Load organizations
                    List<Organization> organizations = organizationDAO.findAll();
                    organizationTableModel.setRowCount(0);

                    for (Organization org : organizations) {
                        String entName = enterprises.stream()
                                .filter(e -> e.getEnterpriseId().equals(org.getEnterpriseId()))
                                .map(Enterprise::getName)
                                .findFirst().orElse("Unknown");

                        long itemCount = itemDAO.findAll().stream()
                                .filter(i -> org.getOrganizationId().equals(i.getOrganizationId())).count();

                        organizationTableModel.addRow(new Object[]{
                            org.getOrganizationId(),
                            org.getName(),
                            org.getType() != null ? org.getType().name() : "N/A",
                            entName,
                            itemCount,
                            org.isActive() ? "Active" : "Inactive"
                        });
                    }

                    // Populate enterprise filters
                    userEnterpriseFilter.removeAllItems();
                    userEnterpriseFilter.addItem("All Enterprises");
                    reportEnterpriseFilter.removeAllItems();
                    reportEnterpriseFilter.addItem("All Enterprises");
                    for (Enterprise ent : enterprises) {
                        userEnterpriseFilter.addItem(ent.getName());
                        reportEnterpriseFilter.addItem(ent.getName());
                    }
                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load enterprise data - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadUserData() {
        searchUsers();
    }

    private void loadSystemHealthData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Test database connection
                    boolean connected = testDatabaseConnection();
                    dbStatusLabel.setText(connected ? "Connected" : "Disconnected");
                    dbStatusLabel.setForeground(connected ? SUCCESS_GREEN : DANGER_RED);
                    dbConnectionLabel.setText(connected ? "OK" : "Failed");
                    dbConnectionLabel.setForeground(connected ? SUCCESS_GREEN : DANGER_RED);

                    // Get collection statistics
                    Object[][] collectionData = getCollectionStats();
                    collectionStatsTable.setModel(new DefaultTableModel(
                            collectionData, new String[]{"Collection", "Documents", "Indexes", "Size (KB)"}
                    ));

                    // Calculate total documents
                    long totalDocs = 0;
                    for (Object[] row : collectionData) {
                        totalDocs += (long) row[1];
                    }
                    totalDocumentsLabel.setText(String.format("%,d", totalDocs));

                    // Check for alerts
                    checkSystemAlerts();

                } catch (Exception e) {
                    logToSystem("ERROR: Failed to load system health - " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private boolean testDatabaseConnection() {
        try {
            itemDAO.findAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object[][] getCollectionStats() {
        try {
            List<Object[]> stats = new ArrayList<>();

            long itemCount = itemDAO.findAll().size();
            stats.add(new Object[]{"items", itemCount, 3L, itemCount * 2L});

            long userCount = userDAO.findAll().size();
            stats.add(new Object[]{"users", userCount, 2L, userCount * 1L});

            long enterpriseCount = enterpriseDAO.findAll().size();
            stats.add(new Object[]{"enterprises", enterpriseCount, 1L, enterpriseCount * 1L});

            long orgCount = organizationDAO.findAll().size();
            stats.add(new Object[]{"organizations", orgCount, 2L, orgCount * 1L});

            long requestCount = workRequestDAO.findAll().size();
            stats.add(new Object[]{"work_requests", requestCount, 4L, requestCount * 3L});

            long buildingCount = buildingDAO.count();
            stats.add(new Object[]{"buildings", buildingCount, 1L, buildingCount * 1L});

            return stats.toArray(new Object[0][]);
        } catch (Exception e) {
            return new Object[0][];
        }
    }

    private void checkSystemAlerts() {
        StringBuilder log = new StringBuilder();
        log.append("=== System Health Check ===\n");
        log.append("Time: ").append(new Date()).append("\n\n");

        try {
            List<SystemAlert> alerts = analyticsService.getAlerts();

            if (alerts.isEmpty()) {
                log.append("[INFO] No active alerts. System operating normally.\n");
            } else {
                for (SystemAlert alert : alerts) {
                    String prefix = alert.level == AlertLevel.CRITICAL ? "[CRITICAL]"
                            : alert.level == AlertLevel.WARNING ? "[WARNING]" : "[INFO]";
                    log.append(prefix).append(" ").append(alert.title).append("\n");
                    log.append("  -> ").append(alert.message).append("\n\n");
                }
            }

            // Add performance metrics
            log.append("\n=== Performance Metrics ===\n");
            log.append("Recovery Rate: ").append(String.format("%.1f%%", analyticsService.getRecoveryRate() * 100)).append("\n");
            log.append("SLA Compliance: ").append(String.format("%.1f%%", analyticsService.getSLAComplianceRate() * 100)).append("\n");
            log.append("Avg Recovery Time: ").append(String.format("%.1f hours", analyticsService.getAverageRecoveryTime())).append("\n");

        } catch (Exception e) {
            log.append("[ERROR] Failed to check alerts: ").append(e.getMessage()).append("\n");
        }

        systemLogArea.setText(log.toString());
    }

    // ==================== ACTION METHODS ====================
    private void searchUsers() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() {
                List<User> users = userDAO.findAll();
                String searchText = userSearchField.getText().toLowerCase().trim();
                String roleFilter = (String) userRoleFilter.getSelectedItem();
                String entFilter = (String) userEnterpriseFilter.getSelectedItem();

                return users.stream()
                        .filter(u -> searchText.isEmpty()
                        || u.getEmail().toLowerCase().contains(searchText)
                        || u.getFullName().toLowerCase().contains(searchText))
                        .filter(u -> "All Roles".equals(roleFilter)
                        || (u.getRole() != null && u.getRole().name().equals(roleFilter)))
                        .filter(u -> "All Enterprises".equals(entFilter)
                        || getEnterpriseName(u.getEnterpriseId()).equals(entFilter))
                        .collect(Collectors.toList());
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
                            getEnterpriseName(user.getEnterpriseId()),
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
        userEnterpriseFilter.setSelectedIndex(0);
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
        if (user == null) {
            return;
        }

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
        if (user == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                "Current trust score: " + String.format("%.0f", user.getTrustScore())
                + "\nEnter new trust score (0-100):",
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
        if (user == null) {
            return;
        }

        // Get user's items
        List<Item> userItems = itemDAO.findAll().stream()
                .filter(i -> i.getReportedBy() != null && email.equals(i.getReportedBy().getEmail()))
                .collect(Collectors.toList());

        StringBuilder activity = new StringBuilder();
        activity.append("=== Activity Log for ").append(user.getFullName()).append(" ===\n\n");
        activity.append("Email: ").append(email).append("\n");
        activity.append("Role: ").append(user.getRole()).append("\n");
        activity.append("Trust Score: ").append(String.format("%.0f", user.getTrustScore())).append("\n");
        activity.append("Items Reported: ").append(user.getItemsReported()).append("\n");
        activity.append("Items Returned: ").append(user.getItemsReturned()).append("\n\n");

        activity.append("Recent Items:\n");
        int count = 0;
        for (Item item : userItems) {
            if (count++ >= 10) {
                break;
            }
            activity.append("  - ").append(item.getTitle()).append(" (").append(item.getType()).append(")\n");
        }

        JTextArea textArea = new JTextArea(activity.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "User Activity", JOptionPane.INFORMATION_MESSAGE);
    }

    private void banOrSuspendUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = (String) userTableModel.getValueAt(selectedRow, 0);
        User user = userDAO.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        String[] options = {"Suspend", "Reactivate", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "User: " + user.getFullName() + "\nCurrent Status: " + (user.isActive() ? "Active" : "Suspended"),
                "Ban/Suspend User",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]
        );

        if (choice == 0) { // Suspend
            user.setActive(false);
            userDAO.update(user);
            searchUsers();
            JOptionPane.showMessageDialog(this, "User suspended successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("WARNING: Suspended user " + email);
        } else if (choice == 1) { // Reactivate
            user.setActive(true);
            userDAO.update(user);
            searchUsers();
            JOptionPane.showMessageDialog(this, "User reactivated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Reactivated user " + email);
        }
    }

    private void showAddEnterpriseDialog() {
        JTextField nameField = new JTextField(20);
        JComboBox<EnterpriseType> typeCombo = new JComboBox<>(EnterpriseType.values());
        JTextField descField = new JTextField(30);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Enterprise",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            Enterprise enterprise = new Enterprise();
            enterprise.setEnterpriseId("ENT-" + System.currentTimeMillis());
            enterprise.setName(nameField.getText().trim());
            enterprise.setType((EnterpriseType) typeCombo.getSelectedItem());
            enterprise.setDescription(descField.getText().trim());
            enterprise.setActive(true);
            enterprise.setJoinedDate(new Date());

            enterpriseDAO.create(enterprise);
            loadEnterpriseData();
            JOptionPane.showMessageDialog(this, "Enterprise added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Added new enterprise - " + enterprise.getName());
        }
    }

    private void showEditEnterpriseDialog() {
        int selectedRow = enterpriseTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an enterprise first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String entId = (String) enterpriseTableModel.getValueAt(selectedRow, 0);
        Enterprise enterprise = enterpriseDAO.findAll().stream()
                .filter(e -> e.getEnterpriseId().equals(entId))
                .findFirst().orElse(null);

        if (enterprise == null) {
            return;
        }

        JTextField nameField = new JTextField(enterprise.getName(), 20);
        JComboBox<EnterpriseType> typeCombo = new JComboBox<>(EnterpriseType.values());
        typeCombo.setSelectedItem(enterprise.getType());
        JCheckBox activeCheck = new JCheckBox("Active", enterprise.isActive());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Status:"));
        panel.add(activeCheck);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Enterprise",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            enterprise.setName(nameField.getText().trim());
            enterprise.setType((EnterpriseType) typeCombo.getSelectedItem());
            enterprise.setActive(activeCheck.isSelected());

            enterpriseDAO.update(enterprise);
            loadEnterpriseData();
            JOptionPane.showMessageDialog(this, "Enterprise updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("INFO: Updated enterprise - " + enterprise.getName());
        }
    }

    private void deleteSelectedEnterprise() {
        int selectedRow = enterpriseTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an enterprise first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String entId = (String) enterpriseTableModel.getValueAt(selectedRow, 0);
        String entName = (String) enterpriseTableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete enterprise: " + entName + "?\nThis action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            enterpriseDAO.delete(entId);
            loadEnterpriseData();
            JOptionPane.showMessageDialog(this, "Enterprise deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("WARNING: Deleted enterprise - " + entName);
        }
    }

    private void showAddOrganizationDialog() {
        JTextField nameField = new JTextField(20);
        JComboBox<OrganizationType> typeCombo = new JComboBox<>(OrganizationType.values());
        JComboBox<String> enterpriseCombo = new JComboBox<>();
        enterpriseDAO.findAll().forEach(e -> enterpriseCombo.addItem(e.getName()));

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Enterprise:"));
        panel.add(enterpriseCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Organization",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            String entName = (String) enterpriseCombo.getSelectedItem();
            String entId = enterpriseDAO.findAll().stream()
                    .filter(e -> e.getName().equals(entName))
                    .map(Enterprise::getEnterpriseId)
                    .findFirst().orElse(null);

            Organization org = new Organization();
            org.setOrganizationId("ORG-" + System.currentTimeMillis());
            org.setName(nameField.getText().trim());
            org.setType((OrganizationType) typeCombo.getSelectedItem());
            org.setEnterpriseId(entId);
            org.setActive(true);
            org.setCreatedDate(new Date());

            organizationDAO.create(org);
            loadEnterpriseData();
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
        Organization org = organizationDAO.findAll().stream()
                .filter(o -> o.getOrganizationId().equals(orgId))
                .findFirst().orElse(null);

        if (org == null) {
            return;
        }

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
            loadEnterpriseData();
            JOptionPane.showMessageDialog(this, "Organization updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
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
            loadEnterpriseData();
            JOptionPane.showMessageDialog(this, "Organization deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            logToSystem("WARNING: Deleted organization - " + orgName);
        }
    }

    private void generateReportPreview() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        StringBuilder preview = new StringBuilder();

        preview.append("=== ").append(reportType).append(" ===\n");
        preview.append("Generated: ").append(new Date()).append("\n");
        preview.append("Date Range: ").append(reportFromDate.getText()).append(" to ").append(reportToDate.getText()).append("\n");
        preview.append("Enterprise Filter: ").append(reportEnterpriseFilter.getSelectedItem()).append("\n\n");

        try {
            switch (reportType) {
                case "Executive Summary":
                    ExecutiveSummary summary = analyticsService.getExecutiveSummary();
                    preview.append(summary.getHighlightsSummary());
                    break;

                case "Weekly Activity":
                    List<WeeklyTrend> weekly = analyticsService.getWeeklyTrends(4);
                    preview.append("Weekly Activity Report\n\n");
                    for (WeeklyTrend trend : weekly) {
                        preview.append(trend.getWeekLabel()).append(":\n");
                        preview.append("  Items: ").append(trend.itemsReported);
                        preview.append(" | Recovered: ").append(trend.recovered);
                        preview.append(" | Rate: ").append(String.format("%.0f%%", trend.recoveryRate * 100)).append("\n");
                    }
                    break;

                case "Enterprise Report":
                    List<EnterpriseStats> entStats = analyticsService.getEnterpriseStats();
                    preview.append("Enterprise Performance Report\n\n");
                    for (EnterpriseStats stat : entStats) {
                        preview.append(stat.enterpriseName).append(":\n");
                        preview.append("  Items: ").append(stat.totalItems);
                        preview.append(" | Users: ").append(stat.userCount);
                        preview.append(" | Recovery: ").append(String.format("%.0f%%", stat.recoveryRate * 100)).append("\n");
                    }
                    break;

                case "Network Effect Analysis":
                    NetworkEffectStats networkStats = analyticsService.getNetworkEffectMetrics();
                    preview.append(networkStats.getSummary());
                    break;

                default:
                    QuickStats quick = analyticsService.getQuickStats();
                    preview.append("Quick Statistics\n\n");
                    preview.append("Total Items: ").append(quick.totalItems).append("\n");
                    preview.append("Open Items: ").append(quick.openItems).append("\n");
                    preview.append("Pending Claims: ").append(quick.pendingClaims).append("\n");
                    preview.append("Recovery Rate: ").append(String.format("%.0f%%", quick.recoveryRate * 100)).append("\n");
                    preview.append("Active Users: ").append(quick.activeUsers).append("\n");
            }
        } catch (Exception e) {
            preview.append("\nError generating report: ").append(e.getMessage());
        }

        reportPreviewArea.setText(preview.toString());
    }

    private void exportGeneratedReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(reportType.replace(" ", "_") + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));

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

    private String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null) {
            return "N/A";
        }
        return enterpriseDAO.findAll().stream()
                .filter(e -> e.getEnterpriseId().equals(enterpriseId))
                .map(Enterprise::getName)
                .findFirst().orElse("Unknown");
    }

    private void logToSystem(String message) {
        if (systemLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                systemLogArea.append(new Date() + " - " + message + "\n");
            });
        }
    }
}
