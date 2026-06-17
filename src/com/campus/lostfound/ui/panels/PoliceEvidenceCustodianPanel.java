package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.trustscore.*;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.verification.*;
import com.campus.lostfound.models.verification.VerificationRequest.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.*;
import com.campus.lostfound.ui.dialogs.RequestDetailDialog;

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
 * Main panel for Police Evidence Custodian role.
 * 
 * Features:
 * - Dashboard with evidence custody metrics
 * - Evidence intake and chain of custody management
 * - Serial number lookup against "police databases"
 * - Release queue for approved item releases
 * - Fraud investigation case management
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class PoliceEvidenceCustodianPanel extends JPanel {
    
    // Services
    private TrustScoreService trustScoreService;
    private VerificationService verificationService;
    private AuthorityService authorityService;
    private WorkRequestService workRequestService;
    
    // DAOs
    private MongoUserDAO userDAO;
    private MongoItemDAO itemDAO;
    private MongoTrustScoreDAO trustScoreDAO;
    private MongoVerificationDAO verificationDAO;
    
    // Data
    private User currentUser;
    private List<EvidenceItem> evidenceItems = new ArrayList<>();
    private List<Investigation> activeInvestigations = new ArrayList<>();
    
    // UI Components
    private JTabbedPane tabbedPane;
    
    // Dashboard components
    private JLabel evidenceInCustodyLabel;
    private JLabel pendingReleasesLabel;
    private JLabel activeInvestigationsLabel;
    private JLabel stolenMatchesLabel;
    private JPanel alertsPanel;
    
    // Evidence Intake components
    private JTextField intakeItemIdField;
    private JTextField intakeSourceEnterpriseField;
    private JTextField intakeCaseNumberField;
    private JTextField intakeSerialNumberField;
    private JTextArea intakeDescriptionArea;
    private JComboBox<String> intakeSourceTypeCombo;
    
    // Serial Number Lookup components
    private JTextField serialSearchField;
    private JPanel serialResultsPanel;
    
    // Release Queue components
    private JTable releaseQueueTable;
    private DefaultTableModel releaseQueueTableModel;
    
    // Investigations components
    private JTable investigationsTable;
    private DefaultTableModel investigationsTableModel;
    private JTextArea caseNotesArea;
    
    // Constants
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final Color PRIMARY_COLOR = new Color(13, 110, 253);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color INFO_COLOR = new Color(23, 162, 184);
    private static final Color PURPLE_COLOR = new Color(111, 66, 193);
    private static final Color POLICE_BLUE = new Color(0, 51, 102);
    private static final Color POLICE_GOLD = new Color(218, 165, 32);
    
    // Emoji-capable fonts (same pattern as WorkQueueTablePanel)
    private static final Font EMOJI_FONT = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.PLAIN, 12);
    private static final Font EMOJI_FONT_BOLD = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.BOLD, 12);
    private static final Font EMOJI_FONT_BOLD_14 = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.BOLD, 14);
    private static final Font EMOJI_FONT_BOLD_18 = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.BOLD, 18);
    private static final Font EMOJI_FONT_BOLD_24 = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.BOLD, 24);
    private static final Font EMOJI_FONT_ITALIC = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.ITALIC, 11);
    private static final Font EMOJI_FONT_TAB = com.campus.lostfound.ui.UIConstants.getEmojiFont(Font.PLAIN, 14);
    
    /**
     * Create a new PoliceEvidenceCustodianPanel.
     * 
     * @param currentUser The logged-in evidence custodian
     */
    public PoliceEvidenceCustodianPanel(User currentUser) {
        this.currentUser = currentUser;
        
        // Initialize services
        this.trustScoreService = new TrustScoreService();
        this.verificationService = new VerificationService();
        this.authorityService = new AuthorityService();
        this.workRequestService = new WorkRequestService();
        
        // Initialize DAOs
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.verificationDAO = new MongoVerificationDAO();
        
        // Initialize sample data
        initializeSampleData();
        
        initComponents();
        loadDashboardData();
        
        // Pre-load pending approvals data
        SwingUtilities.invokeLater(() -> loadPendingApprovals());
    }
    
    private void initializeSampleData() {
        // Sample evidence items
        evidenceItems.add(new EvidenceItem("EV-001", "MacBook Pro 16\"", "Northeastern University", 
            "2024-001234", "C02G8KXNML7H", "High-value laptop found in library", 
            EvidenceStatus.IN_CUSTODY, LocalDateTime.now().minusDays(3)));
        evidenceItems.add(new EvidenceItem("EV-002", "iPhone 15 Pro", "MBTA - Park Street", 
            "2024-001235", "DNPVT2J1KX", "Phone found on Red Line train", 
            EvidenceStatus.PENDING_RELEASE, LocalDateTime.now().minusDays(5)));
        evidenceItems.add(new EvidenceItem("EV-003", "Diamond Ring", "Logan Airport - Terminal E", 
            "2024-001236", null, "Ring found at security checkpoint", 
            EvidenceStatus.UNDER_INVESTIGATION, LocalDateTime.now().minusDays(7)));
        evidenceItems.add(new EvidenceItem("EV-004", "Rolex Watch", "BU Campus", 
            "2024-001237", "M28571937", "Watch flagged as potentially stolen", 
            EvidenceStatus.FLAGGED_STOLEN, LocalDateTime.now().minusDays(2)));
        evidenceItems.add(new EvidenceItem("EV-005", "Camera Equipment", "MIT", 
            null, "8429145", "Professional camera equipment, no case number yet", 
            EvidenceStatus.IN_CUSTODY, LocalDateTime.now().minusDays(1)));
        
        // Sample investigations
        activeInvestigations.add(new Investigation("INV-2024-001", "Serial Fraud Ring", 
            "Multiple false claims across universities", InvestigationStatus.ACTIVE, 
            LocalDateTime.now().minusDays(14)));
        activeInvestigations.add(new Investigation("INV-2024-002", "Stolen Electronics", 
            "Pattern of stolen electronics at MBTA stations", InvestigationStatus.ACTIVE, 
            LocalDateTime.now().minusDays(7)));
        activeInvestigations.add(new Investigation("INV-2024-003", "Identity Fraud", 
            "Claimant using false identification", InvestigationStatus.PENDING_EVIDENCE, 
            LocalDateTime.now().minusDays(3)));
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Tabbed content
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(EMOJI_FONT_TAB);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        // Create tabs
        tabbedPane.addTab("Dashboard", createDashboardTab());
        tabbedPane.addTab("📋 Pending Approvals", createPendingApprovalsTab());
        tabbedPane.addTab("⚖️ Disputes", createDisputesTab());
        tabbedPane.addTab("Evidence Intake", createEvidenceIntakeTab());
        tabbedPane.addTab("Serial # Lookup", createSerialLookupTab());
        tabbedPane.addTab("Release Queue", createReleaseQueueTab());
        tabbedPane.addTab("Investigations", createInvestigationsTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 1 -> loadPendingApprovals();
                case 2 -> loadDisputes();
                case 5 -> loadReleaseQueue();
                case 6 -> loadInvestigations();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(POLICE_BLUE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left - Title with badge icon
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel badgeLabel = new JLabel("⭐ BOSTON POLICE DEPARTMENT");
        badgeLabel.setFont(EMOJI_FONT_BOLD);
        badgeLabel.setForeground(POLICE_GOLD);
        badgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(badgeLabel);
        
        JLabel titleLabel = new JLabel("Evidence Custodian Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel(currentUser.getFullName() + " • Evidence Management Division");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - Quick actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshBtn = createHeaderButton("Refresh All", new Color(52, 73, 94));
        refreshBtn.addActionListener(e -> refreshAll());
        rightPanel.add(refreshBtn);
        
        JButton ncicBtn = createHeaderButton("NCIC Lookup", PRIMARY_COLOR);
        ncicBtn.addActionListener(e -> showNCICLookupDialog());
        rightPanel.add(ncicBtn);
        
        JButton alertBtn = createHeaderButton("Issue BOLO", DANGER_COLOR);
        alertBtn.addActionListener(e -> showBOLODialog());
        rightPanel.add(alertBtn);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
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
        
        // Evidence in Custody
        JPanel custodyCard = createStatCard("Evidence in Custody", "0", POLICE_BLUE, "Items currently held");
        evidenceInCustodyLabel = findValueLabel(custodyCard);
        statsRow.add(custodyCard);
        
        // Pending Releases
        JPanel releaseCard = createStatCard("Pending Releases", "0", SUCCESS_COLOR, "Approved for release");
        pendingReleasesLabel = findValueLabel(releaseCard);
        statsRow.add(releaseCard);
        
        // Active Investigations
        JPanel investCard = createStatCard("Active Investigations", "0", WARNING_COLOR, "Open cases");
        activeInvestigationsLabel = findValueLabel(investCard);
        statsRow.add(investCard);
        
        // Stolen Property Matches
        JPanel stolenCard = createStatCard("Stolen Matches", "0", DANGER_COLOR, "Flagged as stolen");
        stolenMatchesLabel = findValueLabel(stolenCard);
        statsRow.add(stolenCard);
        
        panel.add(statsRow, BorderLayout.NORTH);
        
        // Main content - split into alerts and recent activity
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        
        // Alerts panel
        JPanel alertsContainer = createTitledPanel("Priority Alerts");
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(Color.WHITE);
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsContainer.add(alertsScroll, BorderLayout.CENTER);
        contentPanel.add(alertsContainer);
        
        // Quick actions panel
        JPanel actionsContainer = createTitledPanel("Evidence Actions");
        JPanel actionsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        actionsPanel.setBackground(Color.WHITE);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        actionsPanel.add(createQuickActionButton("New Evidence Intake", () -> tabbedPane.setSelectedIndex(1)));
        actionsPanel.add(createQuickActionButton("Serial # Lookup", () -> tabbedPane.setSelectedIndex(2)));
        actionsPanel.add(createQuickActionButton("Process Release", () -> tabbedPane.setSelectedIndex(3)));
        actionsPanel.add(createQuickActionButton("View Investigations", () -> tabbedPane.setSelectedIndex(4)));
        actionsPanel.add(createQuickActionButton("NCIC Database", this::showNCICLookupDialog));
        actionsPanel.add(createQuickActionButton("Generate Report", this::generateEvidenceReport));
        
        actionsContainer.add(actionsPanel, BorderLayout.CENTER);
        contentPanel.add(actionsContainer);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Bottom - Daily stats
        JPanel dailyPanel = createTitledPanel("Today's Activity");
        dailyPanel.setPreferredSize(new Dimension(0, 180));
        
        JPanel dailyContent = new JPanel(new GridLayout(1, 4, 15, 0));
        dailyContent.setBackground(Color.WHITE);
        dailyContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        dailyContent.add(createDailyStatCard("Intake Today", "3", "items received"));
        dailyContent.add(createDailyStatCard("Released Today", "2", "items returned"));
        dailyContent.add(createDailyStatCard("Serial Checks", "12", "lookups performed"));
        dailyContent.add(createDailyStatCard("Enterprise Requests", "5", "pending coordination"));
        
        dailyPanel.add(dailyContent, BorderLayout.CENTER);
        panel.add(dailyPanel, BorderLayout.SOUTH);
        
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
    
    private JPanel createDailyStatCard(String title, String value, String subtitle) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 249, 250));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(POLICE_BLUE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(valueLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLabel.setForeground(new Color(108, 117, 125));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subLabel);
        
        return card;
    }
    
    // ==================== TAB 2: PENDING APPROVALS ====================
    
    // Pending approvals table components
    private JTable pendingApprovalsTable;
    private DefaultTableModel pendingApprovalsTableModel;
    private JPanel pendingApprovalsPanel;
    
    private JPanel createPendingApprovalsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("📋 Work Requests Pending Police Verification");
        titleLabel.setFont(EMOJI_FONT_BOLD_18);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(EMOJI_FONT);
        refreshBtn.addActionListener(e -> loadPendingApprovals());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        JLabel subtitleLabel = new JLabel("Cross-enterprise transfers requiring police verification for security compliance");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
        pendingApprovalsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        pendingApprovalsTable.getTableHeader().setBackground(POLICE_BLUE);
        pendingApprovalsTable.getTableHeader().setForeground(Color.WHITE);
        
        // Hide request ID column but keep for reference
        pendingApprovalsTable.getColumnModel().getColumn(0).setMinWidth(0);
        pendingApprovalsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        pendingApprovalsTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Column widths
        pendingApprovalsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        pendingApprovalsTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        pendingApprovalsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        pendingApprovalsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        pendingApprovalsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        pendingApprovalsTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        pendingApprovalsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        
        // Priority renderer
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
                        case "NORMAL" -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
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
                        viewWorkRequestDetails(row);
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
        approveBtn.setFont(EMOJI_FONT_BOLD);
        approveBtn.setBackground(SUCCESS_COLOR);
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setBorderPainted(false);
        approveBtn.addActionListener(e -> approveSelectedRequest());
        actionsPanel.add(approveBtn);
        
        JButton rejectBtn = new JButton("❌ Reject Selected");
        rejectBtn.setFont(EMOJI_FONT_BOLD);
        rejectBtn.setBackground(DANGER_COLOR);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setBorderPainted(false);
        rejectBtn.addActionListener(e -> rejectSelectedRequest());
        actionsPanel.add(rejectBtn);
        
        JButton viewBtn = new JButton("👁 View Details");
        viewBtn.setFont(EMOJI_FONT);
        viewBtn.addActionListener(e -> {
            int row = pendingApprovalsTable.getSelectedRow();
            if (row >= 0) viewWorkRequestDetails(row);
            else JOptionPane.showMessageDialog(this, "Please select a request first", "No Selection", JOptionPane.WARNING_MESSAGE);
        });
        actionsPanel.add(viewBtn);
        
        panel.add(actionsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadPendingApprovals() {
        SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WorkRequest> doInBackground() {
                // Get all requests that need POLICE_EVIDENCE_CUSTODIAN approval
                return workRequestService.getRequestsForRole(
                    "POLICE_EVIDENCE_CUSTODIAN", 
                    currentUser.getOrganizationId()
                ).stream()
                    .filter(r -> r.needsApprovalFromRole("POLICE_EVIDENCE_CUSTODIAN"))
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void done() {
                try {
                    List<WorkRequest> requests = get();
                    pendingApprovalsTableModel.setRowCount(0);
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    
                    for (WorkRequest request : requests) {
                        String type = formatRequestType(request.getRequestType());
                        String created = request.getCreatedAt() != null ? 
                            request.getCreatedAt().format(formatter) : "Unknown";
                        
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
                    
                    if (requests.isEmpty()) {
                        // Show empty state message
                        pendingApprovalsTableModel.addRow(new Object[]{
                            "", "", "No pending requests requiring police verification", "", "", "", "", ""
                        });
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                        "Error loading pending approvals: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private String formatRequestType(WorkRequest.RequestType type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case ITEM_CLAIM -> "📋 Item Claim";
            case CROSS_CAMPUS_TRANSFER -> "🏫 Cross-Campus";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "🚇 Transit Transfer";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "✈️ Airport Transfer";
            case POLICE_EVIDENCE_REQUEST -> "👮 Police Evidence";
            case MBTA_TO_AIRPORT_EMERGENCY -> "🚨 MBTA Emergency";
            case MULTI_ENTERPRISE_DISPUTE -> "⚖️ Dispute";
        };
    }
    
    private void viewWorkRequestDetails(int row) {
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        WorkRequest request = workRequestService.getRequestById(requestId);
        if (request == null) {
            JOptionPane.showMessageDialog(this, "Request not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        RequestDetailDialog dialog = new RequestDetailDialog(
            SwingUtilities.getWindowAncestor(this),
            request,
            currentUser
        );
        dialog.setOnRequestUpdated(r -> loadPendingApprovals());
        dialog.setVisible(true);
    }
    
    private void approveSelectedRequest() {
        int row = pendingApprovalsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a request to approve", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        String summary = (String) pendingApprovalsTableModel.getValueAt(row, 2);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Approve this request?\n\n" + summary + "\n\n" +
            "This confirms police verification for the cross-enterprise transfer.",
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
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "✅ Request approved successfully!\n\n" +
                                "The transfer has been verified by police.",
                                "Approved", JOptionPane.INFORMATION_MESSAGE);
                            loadPendingApprovals();
                            loadDashboardData();
                        } else {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "Failed to approve request. Please try again.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void rejectSelectedRequest() {
        int row = pendingApprovalsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a request to reject", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String requestId = (String) pendingApprovalsTableModel.getValueAt(row, 0);
        if (requestId == null || requestId.isEmpty()) return;
        
        String reason = JOptionPane.showInputDialog(this,
            "Enter rejection reason:",
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
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "Request rejected.",
                                "Rejected", JOptionPane.INFORMATION_MESSAGE);
                            loadPendingApprovals();
                            loadDashboardData();
                        } else {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "Failed to reject request.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    // ==================== TAB 3: DISPUTES ====================
    
    // Disputes table components
    private JTable disputesTable;
    private DefaultTableModel disputesTableModel;
    private JPanel disputeDetailPanel;
    private JTextArea disputeNotesArea;
    private JComboBox<String> voteClaimantCombo;
    private JTextArea voteFindingsArea;
    private JTextField policeReportNumberField;
    private List<MultiEnterpriseDisputeResolution> loadedDisputes = new ArrayList<>();
    
    private JPanel createDisputesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("⚖️ Multi-Claimant Dispute Resolution");
        titleLabel.setFont(EMOJI_FONT_BOLD_18);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(EMOJI_FONT);
        refreshBtn.addActionListener(e -> loadDisputes());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        JLabel subtitleLabel = new JLabel("Review disputes involving multiple claimants from different enterprises");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split pane - disputes list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.4);
        
        // Left - Disputes table
        String[] columns = {"Dispute ID", "Item", "Claimants", "Status", "Priority", "Police Report"};
        disputesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        disputesTable = new JTable(disputesTableModel);
        disputesTable.setRowHeight(40);
        disputesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        disputesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        disputesTable.getTableHeader().setBackground(POLICE_BLUE);
        disputesTable.getTableHeader().setForeground(Color.WHITE);
        
        // Hide dispute ID column but keep for reference
        disputesTable.getColumnModel().getColumn(0).setMinWidth(0);
        disputesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        disputesTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Status renderer
        disputesTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
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
                        case "UNDER_REVIEW" -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
                        case "ESCALATED" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "RESOLVED" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Priority renderer
        disputesTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String priority = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 10));
                
                if (!isSelected) {
                    switch (priority) {
                        case "URGENT" -> { setBackground(new Color(248, 215, 218)); setForeground(DANGER_COLOR); }
                        case "HIGH" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Selection listener
        disputesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = disputesTable.getSelectedRow();
                if (row >= 0) {
                    displayDisputeDetails(row);
                }
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(disputesTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        splitPane.setLeftComponent(tableScroll);
        
        // Right - Dispute details panel
        disputeDetailPanel = new JPanel(new BorderLayout());
        disputeDetailPanel.setBackground(Color.WHITE);
        disputeDetailPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(POLICE_BLUE),
            "Dispute Details & Police Actions",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            POLICE_BLUE
        ));
        
        // Create tabbed detail panel
        JTabbedPane detailTabs = new JTabbedPane(JTabbedPane.TOP);
        detailTabs.setFont(EMOJI_FONT);
        
        // Tab 1: Claimants & Evidence
        JPanel claimantsPanel = createClaimantsPanel();
        detailTabs.addTab("👥 Claimants", claimantsPanel);
        
        // Tab 2: Police Actions
        JPanel policeActionsPanel = createPoliceActionsPanel();
        detailTabs.addTab("👮 Police Actions", policeActionsPanel);
        
        // Tab 3: Resolution
        JPanel resolutionPanel = createResolutionPanel();
        detailTabs.addTab("⚖️ Resolution", resolutionPanel);
        
        // Tab 4: Notes/History
        disputeNotesArea = new JTextArea();
        disputeNotesArea.setEditable(false);
        disputeNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        disputeNotesArea.setLineWrap(true);
        disputeNotesArea.setWrapStyleWord(true);
        disputeNotesArea.setText("Select a dispute to view details");
        disputeNotesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailTabs.addTab("📝 Notes", new JScrollPane(disputeNotesArea));
        
        disputeDetailPanel.add(detailTabs, BorderLayout.CENTER);
        
        splitPane.setRightComponent(disputeDetailPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createClaimantsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // This will be populated when a dispute is selected
        JLabel placeholder = new JLabel("Select a dispute to view claimants", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        placeholder.setForeground(new Color(108, 117, 125));
        panel.add(placeholder, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPoliceActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Police Report Number
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel reportLabel = new JLabel("Police Report #:");
        reportLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(reportLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        policeReportNumberField = new JTextField(20);
        policeReportNumberField.setFont(new Font("Consolas", Font.PLAIN, 13));
        formPanel.add(policeReportNumberField, gbc);
        
        // Vote for Claimant
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel voteLabel = new JLabel("Vote for Claimant:");
        voteLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(voteLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        voteClaimantCombo = new JComboBox<>();
        voteClaimantCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(voteClaimantCombo, gbc);
        
        // Police Findings
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel findingsLabel = new JLabel("Police Findings:");
        findingsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(findingsLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        voteFindingsArea = new JTextArea(6, 30);
        voteFindingsArea.setLineWrap(true);
        voteFindingsArea.setWrapStyleWord(true);
        voteFindingsArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        voteFindingsArea.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
        formPanel.add(new JScrollPane(voteFindingsArea), gbc);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);
        
        JButton submitFindingsBtn = new JButton("📋 Submit Findings");
        submitFindingsBtn.setFont(EMOJI_FONT_BOLD);
        submitFindingsBtn.setBackground(PRIMARY_COLOR);
        submitFindingsBtn.setForeground(Color.WHITE);
        submitFindingsBtn.setBorderPainted(false);
        submitFindingsBtn.addActionListener(e -> submitPoliceFindings());
        buttonPanel.add(submitFindingsBtn);
        
        JButton castVoteBtn = new JButton("🗳️ Cast Vote");
        castVoteBtn.setFont(EMOJI_FONT_BOLD);
        castVoteBtn.setBackground(SUCCESS_COLOR);
        castVoteBtn.setForeground(Color.WHITE);
        castVoteBtn.setBorderPainted(false);
        castVoteBtn.addActionListener(e -> castDisputeVote());
        buttonPanel.add(castVoteBtn);
        
        JButton requestEvidenceBtn = new JButton("📎 Request More Evidence");
        requestEvidenceBtn.setFont(EMOJI_FONT);
        requestEvidenceBtn.addActionListener(e -> requestAdditionalEvidence());
        buttonPanel.add(requestEvidenceBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createResolutionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Award to Claimant
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel awardLabel = new JLabel("Award Item To:");
        awardLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(awardLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        JComboBox<String> awardCombo = new JComboBox<>();
        awardCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(awardCombo, gbc);
        
        // Reason
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel reasonLabel = new JLabel("Resolution Reason:");
        reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(reasonLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JTextArea reasonArea = new JTextArea(5, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reasonArea.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
        formPanel.add(new JScrollPane(reasonArea), gbc);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        // Resolution buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setOpaque(false);
        
        JButton resolveBtn = new JButton("✅ Resolve Dispute");
        resolveBtn.setFont(EMOJI_FONT_BOLD_14);
        resolveBtn.setBackground(SUCCESS_COLOR);
        resolveBtn.setForeground(Color.WHITE);
        resolveBtn.setBorderPainted(false);
        resolveBtn.setPreferredSize(new Dimension(180, 40));
        resolveBtn.addActionListener(e -> {
            int row = disputesTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a dispute first", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String selectedClaimant = (String) awardCombo.getSelectedItem();
            String reason = reasonArea.getText().trim();
            
            if (selectedClaimant == null || reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a claimant and provide a reason", 
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            resolveSelectedDispute(selectedClaimant, reason);
        });
        buttonPanel.add(resolveBtn);
        
        JButton escalateBtn = new JButton("⬆️ Escalate to Court");
        escalateBtn.setFont(EMOJI_FONT_BOLD_14);
        escalateBtn.setBackground(DANGER_COLOR);
        escalateBtn.setForeground(Color.WHITE);
        escalateBtn.setBorderPainted(false);
        escalateBtn.setPreferredSize(new Dimension(180, 40));
        escalateBtn.addActionListener(e -> escalateToLegalSystem());
        buttonPanel.add(escalateBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Store reference for updating
        panel.putClientProperty("awardCombo", awardCombo);
        panel.putClientProperty("reasonArea", reasonArea);
        
        return panel;
    }
    
    private void loadDisputes() {
        SwingWorker<List<MultiEnterpriseDisputeResolution>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MultiEnterpriseDisputeResolution> doInBackground() {
                // Get all disputes that require police involvement
                return workRequestService.getDisputesRequiringPolice();
            }
            
            @Override
            protected void done() {
                try {
                    loadedDisputes = get();
                    disputesTableModel.setRowCount(0);
                    
                    for (MultiEnterpriseDisputeResolution dispute : loadedDisputes) {
                        String claimantCount = dispute.getClaimantCount() + " claimants";
                        String policeReport = dispute.getPoliceReportNumber() != null ? 
                            dispute.getPoliceReportNumber() : "Not assigned";
                        
                        disputesTableModel.addRow(new Object[]{
                            dispute.getRequestId(),
                            dispute.getItemName(),
                            claimantCount,
                            dispute.getResolutionStatus(),
                            dispute.getPriority() != null ? dispute.getPriority().name() : "HIGH",
                            policeReport
                        });
                    }
                    
                    if (loadedDisputes.isEmpty()) {
                        disputesTableModel.addRow(new Object[]{
                            "", "No disputes requiring police action", "", "", "", ""
                        });
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                        "Error loading disputes: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displayDisputeDetails(int row) {
        if (row < 0 || row >= loadedDisputes.size()) return;
        
        MultiEnterpriseDisputeResolution dispute = loadedDisputes.get(row);
        
        // Update claimant combo boxes
        voteClaimantCombo.removeAllItems();
        voteClaimantCombo.addItem("-- Select Claimant --");
        
        for (MultiEnterpriseDisputeResolution.Claimant c : dispute.getClaimants()) {
            String display = c.claimantName + " (" + c.enterpriseName + ")";
            voteClaimantCombo.addItem(display);
        }
        
        // Update police report field
        policeReportNumberField.setText(dispute.getPoliceReportNumber() != null ? 
            dispute.getPoliceReportNumber() : "");
        
        // Update findings area with existing findings
        if (dispute.getPoliceFindingsReport() != null && !dispute.getPoliceFindingsReport().isEmpty()) {
            voteFindingsArea.setText(dispute.getPoliceFindingsReport());
        } else {
            voteFindingsArea.setText("");
        }
        
        // Update notes area
        StringBuilder notes = new StringBuilder();
        notes.append("DISPUTE: ").append(dispute.getRequestId()).append("\n");
        notes.append("━".repeat(50)).append("\n\n");
        notes.append("Item: ").append(dispute.getItemName()).append("\n");
        notes.append("Category: ").append(dispute.getItemCategory() != null ? dispute.getItemCategory() : "N/A").append("\n");
        notes.append("Estimated Value: $").append(String.format("%.2f", dispute.getEstimatedValue())).append("\n");
        notes.append("Holding Enterprise: ").append(dispute.getHoldingEnterpriseName() != null ? dispute.getHoldingEnterpriseName() : "N/A").append("\n\n");
        notes.append("Status: ").append(dispute.getResolutionStatus()).append("\n");
        notes.append("Dispute Type: ").append(dispute.getDisputeType() != null ? dispute.getDisputeType() : "OWNERSHIP").append("\n");
        notes.append("Reason: ").append(dispute.getDisputeReason() != null ? dispute.getDisputeReason() : "Multiple claimants").append("\n\n");
        
        notes.append("━".repeat(50)).append("\n");
        notes.append("CLAIMANTS (" + dispute.getClaimantCount() + "):\n\n");
        
        int i = 1;
        for (MultiEnterpriseDisputeResolution.Claimant c : dispute.getClaimants()) {
            notes.append(i++).append(". ").append(c.claimantName).append("\n");
            notes.append("   Enterprise: ").append(c.enterpriseName != null ? c.enterpriseName : "N/A").append("\n");
            notes.append("   Email: ").append(c.claimantEmail != null ? c.claimantEmail : "N/A").append("\n");
            notes.append("   Trust Score: ").append(String.format("%.0f", c.trustScore)).append("\n");
            notes.append("   Claim: ").append(c.claimDescription != null ? c.claimDescription : "N/A").append("\n");
            notes.append("   Proof: ").append(c.proofDescription != null ? c.proofDescription : "N/A").append("\n");
            notes.append("   Status: ").append(c.claimStatus != null ? c.claimStatus : "SUBMITTED").append("\n\n");
        }
        
        notes.append("━".repeat(50)).append("\n");
        notes.append("PANEL VOTES (" + dispute.getPanelVotesReceived() + "/" + dispute.getPanelVotesRequired() + "):\n\n");
        
        for (MultiEnterpriseDisputeResolution.PanelMember pm : dispute.getVerificationPanel()) {
            notes.append("• ").append(pm.memberName).append(" (").append(pm.role).append(")\n");
            notes.append("  Voted: ").append(pm.hasVoted ? "Yes" : "No").append("\n");
            if (pm.hasVoted && pm.votedForClaimantId != null) {
                notes.append("  Vote For: ").append(pm.votedForClaimantId).append("\n");
                notes.append("  Reason: ").append(pm.voteReason != null ? pm.voteReason : "N/A").append("\n");
            }
            notes.append("\n");
        }
        
        if (dispute.getPoliceReportNumber() != null) {
            notes.append("━".repeat(50)).append("\n");
            notes.append("POLICE INVOLVEMENT:\n\n");
            notes.append("Report #: ").append(dispute.getPoliceReportNumber()).append("\n");
            notes.append("Officer: ").append(dispute.getPoliceOfficerName() != null ? dispute.getPoliceOfficerName() : "N/A").append("\n");
            if (dispute.getPoliceFindingsReport() != null) {
                notes.append("Findings:\n").append(dispute.getPoliceFindingsReport()).append("\n");
            }
        }
        
        if (dispute.getResolutionNotes() != null && !dispute.getResolutionNotes().isEmpty()) {
            notes.append("\n━".repeat(50)).append("\n");
            notes.append("NOTES/HISTORY:\n\n");
            notes.append(dispute.getResolutionNotes());
        }
        
        disputeNotesArea.setText(notes.toString());
        disputeNotesArea.setCaretPosition(0);
        
        // Update the claimants panel with detailed view
        updateClaimantsPanel(dispute);
        
        // Update resolution panel combo
        updateResolutionPanel(dispute);
    }
    
    private void updateClaimantsPanel(MultiEnterpriseDisputeResolution dispute) {
        // Find the claimants panel in the detail tabs
        Component[] components = disputeDetailPanel.getComponents();
        for (Component c : components) {
            if (c instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) c;
                JPanel claimantsPanel = (JPanel) tabs.getComponentAt(0);
                claimantsPanel.removeAll();
                
                claimantsPanel.setLayout(new BoxLayout(claimantsPanel, BoxLayout.Y_AXIS));
                claimantsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                for (MultiEnterpriseDisputeResolution.Claimant claimant : dispute.getClaimants()) {
                    JPanel cardPanel = createClaimantCard(claimant);
                    claimantsPanel.add(cardPanel);
                    claimantsPanel.add(Box.createVerticalStrut(10));
                }
                
                claimantsPanel.revalidate();
                claimantsPanel.repaint();
                break;
            }
        }
    }
    
    private JPanel createClaimantCard(MultiEnterpriseDisputeResolution.Claimant claimant) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(new Color(248, 249, 250));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Header with name and enterprise
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel("👤 " + claimant.claimantName);
        nameLabel.setFont(EMOJI_FONT_BOLD_14);
        headerPanel.add(nameLabel, BorderLayout.WEST);
        
        JLabel enterpriseLabel = new JLabel(claimant.enterpriseName != null ? claimant.enterpriseName : "Unknown");
        enterpriseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        enterpriseLabel.setForeground(PRIMARY_COLOR);
        headerPanel.add(enterpriseLabel, BorderLayout.EAST);
        
        card.add(headerPanel, BorderLayout.NORTH);
        
        // Details
        JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 10, 3));
        detailsPanel.setOpaque(false);
        
        detailsPanel.add(createDetailLabel("Trust Score:"));
        detailsPanel.add(createValueLabel(String.format("%.0f/100", claimant.trustScore), getTrustScoreColor(claimant.trustScore)));
        
        detailsPanel.add(createDetailLabel("Status:"));
        detailsPanel.add(createValueLabel(claimant.claimStatus != null ? claimant.claimStatus : "SUBMITTED", null));
        
        detailsPanel.add(createDetailLabel("Claim:"));
        String claimText = claimant.claimDescription != null ? 
            (claimant.claimDescription.length() > 50 ? claimant.claimDescription.substring(0, 50) + "..." : claimant.claimDescription) : "N/A";
        detailsPanel.add(createValueLabel(claimText, null));
        
        card.add(detailsPanel, BorderLayout.CENTER);
        
        // Action button
        JButton viewEvidenceBtn = new JButton("View Evidence");
        viewEvidenceBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        viewEvidenceBtn.addActionListener(e -> viewClaimantEvidence(claimant));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(viewEvidenceBtn);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JLabel createDetailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(108, 117, 125));
        return label;
    }
    
    private JLabel createValueLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        if (color != null) {
            label.setForeground(color);
        }
        return label;
    }
    
    private Color getTrustScoreColor(double score) {
        if (score >= 70) return SUCCESS_COLOR;
        if (score >= 50) return WARNING_COLOR.darker();
        return DANGER_COLOR;
    }
    
    private void viewClaimantEvidence(MultiEnterpriseDisputeResolution.Claimant claimant) {
        // Show dialog with claimant's evidence
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Evidence for " + claimant.claimantName, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Evidence Submitted by " + claimant.claimantName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JTextArea evidenceArea = new JTextArea();
        evidenceArea.setEditable(false);
        evidenceArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        evidenceArea.setLineWrap(true);
        evidenceArea.setWrapStyleWord(true);
        
        StringBuilder sb = new StringBuilder();
        sb.append("CLAIMANT DETAILS:\n");
        sb.append("━".repeat(40)).append("\n\n");
        sb.append("Name: ").append(claimant.claimantName).append("\n");
        sb.append("Email: ").append(claimant.claimantEmail != null ? claimant.claimantEmail : "N/A").append("\n");
        sb.append("Enterprise: ").append(claimant.enterpriseName != null ? claimant.enterpriseName : "N/A").append("\n");
        sb.append("Organization: ").append(claimant.organizationName != null ? claimant.organizationName : "N/A").append("\n");
        sb.append("Trust Score: ").append(String.format("%.0f", claimant.trustScore)).append("/100\n\n");
        
        sb.append("CLAIM DESCRIPTION:\n");
        sb.append("━".repeat(40)).append("\n");
        sb.append(claimant.claimDescription != null ? claimant.claimDescription : "No description provided").append("\n\n");
        
        sb.append("PROOF DESCRIPTION:\n");
        sb.append("━".repeat(40)).append("\n");
        sb.append(claimant.proofDescription != null ? claimant.proofDescription : "No proof description provided").append("\n\n");
        
        sb.append("EVIDENCE IDS:\n");
        sb.append("━".repeat(40)).append("\n");
        if (claimant.evidenceIds != null && !claimant.evidenceIds.isEmpty()) {
            for (String evId : claimant.evidenceIds) {
                sb.append("• ").append(evId).append("\n");
            }
        } else {
            sb.append("No evidence documents uploaded\n");
        }
        
        evidenceArea.setText(sb.toString());
        evidenceArea.setCaretPosition(0);
        
        panel.add(new JScrollPane(evidenceArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void updateResolutionPanel(MultiEnterpriseDisputeResolution dispute) {
        // Find resolution panel and update combo
        Component[] components = disputeDetailPanel.getComponents();
        for (Component c : components) {
            if (c instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) c;
                JPanel resPanel = (JPanel) tabs.getComponentAt(2); // Resolution tab
                
                @SuppressWarnings("unchecked")
                JComboBox<String> awardCombo = (JComboBox<String>) resPanel.getClientProperty("awardCombo");
                if (awardCombo != null) {
                    awardCombo.removeAllItems();
                    awardCombo.addItem("-- Select Winner --");
                    for (MultiEnterpriseDisputeResolution.Claimant cl : dispute.getClaimants()) {
                        awardCombo.addItem(cl.claimantId + " - " + cl.claimantName);
                    }
                }
                break;
            }
        }
    }
    
    private void submitPoliceFindings() {
        int row = disputesTable.getSelectedRow();
        if (row < 0 || row >= loadedDisputes.size()) {
            JOptionPane.showMessageDialog(this, "Please select a dispute first", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String reportNumber = policeReportNumberField.getText().trim();
        String findings = voteFindingsArea.getText().trim();
        
        if (reportNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a police report number", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (findings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your findings", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        MultiEnterpriseDisputeResolution dispute = loadedDisputes.get(row);
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return workRequestService.recordPoliceFindingsForDispute(
                    dispute.getRequestId(),
                    currentUser.getEmail(),
                    currentUser.getFullName(),
                    reportNumber,
                    findings
                );
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "✅ Police findings recorded successfully!\n\n" +
                            "Report #: " + reportNumber,
                            "Findings Recorded", JOptionPane.INFORMATION_MESSAGE);
                        loadDisputes();
                    } else {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "Failed to record findings. Please try again.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                        "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void castDisputeVote() {
        int row = disputesTable.getSelectedRow();
        if (row < 0 || row >= loadedDisputes.size()) {
            JOptionPane.showMessageDialog(this, "Please select a dispute first", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selectedClaimant = (String) voteClaimantCombo.getSelectedItem();
        if (selectedClaimant == null || selectedClaimant.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Please select a claimant to vote for", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String findings = voteFindingsArea.getText().trim();
        if (findings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide reasoning for your vote", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        MultiEnterpriseDisputeResolution dispute = loadedDisputes.get(row);
        
        // Find the claimant ID from the selection
        int selectedIndex = voteClaimantCombo.getSelectedIndex() - 1; // -1 for "Select" option
        if (selectedIndex < 0 || selectedIndex >= dispute.getClaimants().size()) {
            JOptionPane.showMessageDialog(this, "Invalid claimant selection", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String claimantId = dispute.getClaimants().get(selectedIndex).claimantId;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Cast your vote for: " + selectedClaimant + "?\n\n" +
            "This action cannot be undone.",
            "Confirm Vote",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return workRequestService.recordDisputeVote(
                        dispute.getRequestId(),
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        "POLICE_EVIDENCE_CUSTODIAN",
                        claimantId,
                        findings
                    );
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "✅ Vote recorded successfully!\n\n" +
                                "Your vote has been added to the dispute record.",
                                "Vote Recorded", JOptionPane.INFORMATION_MESSAGE);
                            loadDisputes();
                        } else {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "Failed to record vote. Please try again.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void requestAdditionalEvidence() {
        int row = disputesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a dispute first", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String request = JOptionPane.showInputDialog(this,
            "What additional evidence do you need?\n(This will be sent to all claimants)",
            "Request Evidence",
            JOptionPane.QUESTION_MESSAGE);
        
        if (request != null && !request.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Evidence request sent to all claimants:\n\n" + request,
                "Request Sent", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void resolveSelectedDispute(String selectedClaimant, String reason) {
        int row = disputesTable.getSelectedRow();
        if (row < 0 || row >= loadedDisputes.size()) {
            return;
        }
        
        MultiEnterpriseDisputeResolution dispute = loadedDisputes.get(row);
        
        // Extract claimant ID from selection (format: "ID - Name")
        String claimantId = selectedClaimant.split(" - ")[0].trim();
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ FINAL RESOLUTION\n\n" +
            "You are about to award the item to: " + selectedClaimant + "\n\n" +
            "Reason: " + reason + "\n\n" +
            "This action CANNOT be undone. All other claimants will be notified.\n\n" +
            "Proceed with resolution?",
            "Confirm Resolution",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return workRequestService.resolveDispute(
                        dispute.getRequestId(),
                        claimantId,
                        reason,
                        currentUser.getFullName() + " (Police Evidence Custodian)"
                    );
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "✅ Dispute resolved successfully!\n\n" +
                                "The item has been awarded and all parties have been notified.",
                                "Dispute Resolved", JOptionPane.INFORMATION_MESSAGE);
                            loadDisputes();
                            loadDashboardData();
                        } else {
                            JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                                "Failed to resolve dispute. Please try again.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(PoliceEvidenceCustodianPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void escalateToLegalSystem() {
        int row = disputesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a dispute first", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ LEGAL ESCALATION\n\n" +
            "This will escalate the dispute to the legal system.\n" +
            "All evidence and findings will be compiled for court proceedings.\n\n" +
            "This action should only be taken for complex disputes that\n" +
            "cannot be resolved through normal dispute resolution.\n\n" +
            "Proceed with legal escalation?",
            "Confirm Escalation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Dispute has been escalated to the legal system.\n\n" +
                "Case file has been generated and sent to:\n" +
                "- District Attorney's Office\n" +
                "- All involved enterprises\n" +
                "- All claimants\n\n" +
                "Case Reference: LEGAL-" + System.currentTimeMillis() % 100000,
                "Escalated", JOptionPane.INFORMATION_MESSAGE);
            
            loadDisputes();
        }
    }
    
    // ==================== TAB 4: EVIDENCE INTAKE ====================
    
    private JPanel createEvidenceIntakeTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Evidence Intake Form");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel helpLabel = new JLabel("Register new evidence items and establish chain of custody");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        headerPanel.add(helpLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: Item ID (auto-generated) and Source Type
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Evidence ID:"), gbc);
        
        gbc.gridx = 1;
        intakeItemIdField = new JTextField(15);
        intakeItemIdField.setText(generateEvidenceId());
        intakeItemIdField.setEditable(false);
        intakeItemIdField.setBackground(new Color(233, 236, 239));
        formPanel.add(intakeItemIdField, gbc);
        
        gbc.gridx = 2;
        formPanel.add(new JLabel("Source Type:"), gbc);
        
        gbc.gridx = 3;
        intakeSourceTypeCombo = new JComboBox<>(new String[]{
            "University Lost & Found",
            "MBTA Transit",
            "Logan Airport",
            "Direct Report",
            "Other Agency Transfer"
        });
        intakeSourceTypeCombo.setPreferredSize(new Dimension(180, 28));
        formPanel.add(intakeSourceTypeCombo, gbc);
        
        // Row 2: Source Enterprise and Case Number
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Source Enterprise:"), gbc);
        
        gbc.gridx = 1;
        intakeSourceEnterpriseField = new JTextField(15);
        formPanel.add(intakeSourceEnterpriseField, gbc);
        
        gbc.gridx = 2;
        formPanel.add(new JLabel("Case Number:"), gbc);
        
        gbc.gridx = 3;
        intakeCaseNumberField = new JTextField(15);
        intakeCaseNumberField.setToolTipText("Optional - Link to existing case");
        formPanel.add(intakeCaseNumberField, gbc);
        
        // Row 3: Serial Number
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Serial Number:"), gbc);
        
        gbc.gridx = 1;
        intakeSerialNumberField = new JTextField(15);
        formPanel.add(intakeSerialNumberField, gbc);
        
        gbc.gridx = 2;
        JButton checkSerialBtn = new JButton("Check NCIC");
        checkSerialBtn.setBackground(PRIMARY_COLOR);
        checkSerialBtn.setForeground(Color.WHITE);
        checkSerialBtn.addActionListener(e -> checkSerialNumber(intakeSerialNumberField.getText()));
        formPanel.add(checkSerialBtn, gbc);
        
        // Row 4: Description
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Item Description:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        intakeDescriptionArea = new JTextArea(4, 40);
        intakeDescriptionArea.setLineWrap(true);
        intakeDescriptionArea.setWrapStyleWord(true);
        intakeDescriptionArea.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
        formPanel.add(new JScrollPane(intakeDescriptionArea), gbc);
        
        // Chain of Custody section
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel cocPanel = createChainOfCustodyPanel();
        formPanel.add(cocPanel, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        
        JButton submitBtn = new JButton("Register Evidence");
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setBackground(SUCCESS_COLOR);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setPreferredSize(new Dimension(180, 40));
        submitBtn.addActionListener(e -> submitEvidenceIntake());
        buttonPanel.add(submitBtn);
        
        JButton printBtn = new JButton("Print Custody Form");
        printBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        printBtn.setPreferredSize(new Dimension(160, 40));
        printBtn.addActionListener(e -> printCustodyForm());
        buttonPanel.add(printBtn);
        
        JButton clearBtn = new JButton("Clear Form");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clearBtn.setPreferredSize(new Dimension(120, 40));
        clearBtn.addActionListener(e -> clearIntakeForm());
        buttonPanel.add(clearBtn);
        
        formPanel.add(buttonPanel, gbc);
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createChainOfCustodyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(POLICE_BLUE),
                "Chain of Custody",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                POLICE_BLUE
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 4, 15, 10));
        infoPanel.setOpaque(false);
        
        infoPanel.add(createCOCField("Received By:", currentUser.getFullName()));
        infoPanel.add(createCOCField("Badge #:", "BPD-" + (1000 + new Random().nextInt(9000))));
        infoPanel.add(createCOCField("Date/Time:", LocalDateTime.now().format(DT_FORMATTER)));
        infoPanel.add(createCOCField("Location:", "Evidence Room A"));
        
        infoPanel.add(createCOCField("Condition:", "Good"));
        infoPanel.add(createCOCField("Storage Bin:", "BIN-" + String.format("%04d", new Random().nextInt(10000))));
        infoPanel.add(createCOCField("Photo Doc:", "Pending"));
        infoPanel.add(createCOCField("Supervisor:", "Det. Johnson"));
        
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCOCField(String label, String value) {
        JPanel field = new JPanel(new BorderLayout(5, 0));
        field.setOpaque(false);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 11));
        labelComp.setForeground(new Color(108, 117, 125));
        field.add(labelComp, BorderLayout.WEST);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        field.add(valueComp, BorderLayout.CENTER);
        
        return field;
    }
    
    // ==================== TAB 3: SERIAL NUMBER LOOKUP ====================
    
    private JPanel createSerialLookupTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Search section
        JPanel searchPanel = new JPanel(new BorderLayout(15, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(POLICE_BLUE),
                "NCIC Serial Number Database Search",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                POLICE_BLUE
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        searchInputPanel.setOpaque(false);
        
        searchInputPanel.add(new JLabel("Serial Number:"));
        serialSearchField = new JTextField(25);
        serialSearchField.setFont(new Font("Consolas", Font.PLAIN, 14));
        serialSearchField.addActionListener(e -> performSerialLookup());
        searchInputPanel.add(serialSearchField);
        
        JButton searchBtn = new JButton("Search NCIC");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setBackground(POLICE_BLUE);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener(e -> performSerialLookup());
        searchInputPanel.add(searchBtn);
        
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            serialSearchField.setText("");
            serialResultsPanel.removeAll();
            serialResultsPanel.revalidate();
            serialResultsPanel.repaint();
        });
        searchInputPanel.add(clearBtn);
        
        searchPanel.add(searchInputPanel, BorderLayout.NORTH);
        
        // Warning label
        JLabel warningLabel = new JLabel("⚠ This search queries the National Crime Information Center (NCIC) database");
        warningLabel.setFont(EMOJI_FONT_ITALIC);
        warningLabel.setForeground(WARNING_COLOR.darker());
        warningLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        searchPanel.add(warningLabel, BorderLayout.SOUTH);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Results section
        serialResultsPanel = new JPanel();
        serialResultsPanel.setLayout(new BoxLayout(serialResultsPanel, BoxLayout.Y_AXIS));
        serialResultsPanel.setBackground(Color.WHITE);
        serialResultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Initial placeholder
        JLabel placeholder = new JLabel("Enter a serial number and click Search to query the database", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        placeholder.setForeground(new Color(108, 117, 125));
        placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
        serialResultsPanel.add(placeholder);
        
        JScrollPane scrollPane = new JScrollPane(serialResultsPanel);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Recent searches panel
        JPanel recentPanel = createTitledPanel("Recent Searches");
        recentPanel.setPreferredSize(new Dimension(0, 150));
        
        String[] columns = {"Serial #", "Search Time", "Result", "Item Type", "Status"};
        DefaultTableModel recentModel = new DefaultTableModel(columns, 0);
        recentModel.addRow(new Object[]{"C02G8KXNML7H", "Today 10:32 AM", "NO MATCH", "Laptop", "Cleared"});
        recentModel.addRow(new Object[]{"DNPVT2J1KX", "Today 09:15 AM", "NO MATCH", "Phone", "Cleared"});
        recentModel.addRow(new Object[]{"M28571937", "Yesterday 4:45 PM", "MATCH FOUND", "Watch", "Under Investigation"});
        
        JTable recentTable = new JTable(recentModel);
        recentTable.setRowHeight(30);
        recentTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("MATCH FOUND".equals(value)) {
                    setForeground(DANGER_COLOR);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    setForeground(SUCCESS_COLOR);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return this;
            }
        });
        
        recentPanel.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        panel.add(recentPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // ==================== TAB 4: RELEASE QUEUE ====================
    
    private JPanel createReleaseQueueTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Evidence Release Queue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        filterPanel.add(new JLabel("Status:"));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "All", "Pending Release", "Awaiting ID Verification", "Ready for Pickup", "Released"
        });
        statusFilter.addActionListener(e -> filterReleaseQueue((String) statusFilter.getSelectedItem()));
        filterPanel.add(statusFilter);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadReleaseQueue());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Release queue table
        String[] columns = {"Evidence ID", "Item", "Claimant", "ID Verified", "Enterprise", "Approved By", "Status", "Actions"};
        releaseQueueTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        
        releaseQueueTable = new JTable(releaseQueueTableModel);
        releaseQueueTable.setRowHeight(40);
        releaseQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        releaseQueueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Custom renderer for status
        releaseQueueTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                
                if (!isSelected) {
                    switch (status) {
                        case "Ready for Pickup" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        case "Awaiting ID" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "Pending Approval" -> { setBackground(new Color(207, 226, 255)); setForeground(PRIMARY_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        // Actions column
        releaseQueueTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer("Process"));
        releaseQueueTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox(), this::processRelease));
        
        // Double-click to view details
        releaseQueueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = releaseQueueTable.getSelectedRow();
                    if (row >= 0) {
                        showReleaseDetails(row);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(releaseQueueTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load initial data
        loadReleaseQueue();
        
        return panel;
    }
    
    // ==================== TAB 5: INVESTIGATIONS ====================
    
    private JPanel createInvestigationsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Fraud Investigations");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton newCaseBtn = new JButton("New Investigation");
        newCaseBtn.setBackground(POLICE_BLUE);
        newCaseBtn.setForeground(Color.WHITE);
        newCaseBtn.addActionListener(e -> showNewInvestigationDialog());
        buttonPanel.add(newCaseBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadInvestigations());
        buttonPanel.add(refreshBtn);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Split pane - cases list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        splitPane.setResizeWeight(0.6);
        
        // Left - Investigations table
        String[] columns = {"Case #", "Title", "Status", "Priority", "Created", "Lead"};
        investigationsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        investigationsTable = new JTable(investigationsTableModel);
        investigationsTable.setRowHeight(35);
        investigationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        investigationsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Status renderer
        investigationsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
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
                        case "Pending Evidence" -> { setBackground(new Color(255, 243, 205)); setForeground(WARNING_COLOR.darker()); }
                        case "Closed" -> { setBackground(new Color(209, 231, 221)); setForeground(SUCCESS_COLOR); }
                        default -> { setBackground(Color.WHITE); setForeground(Color.BLACK); }
                    }
                }
                return this;
            }
        });
        
        investigationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = investigationsTable.getSelectedRow();
                if (row >= 0) {
                    displayInvestigationDetails(row);
                }
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(investigationsTable);
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
        
        caseNotesArea = new JTextArea();
        caseNotesArea.setEditable(false);
        caseNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        caseNotesArea.setLineWrap(true);
        caseNotesArea.setWrapStyleWord(true);
        caseNotesArea.setText("Select a case to view details");
        caseNotesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        detailsPanel.add(new JScrollPane(caseNotesArea), BorderLayout.CENTER);
        
        // Action buttons for selected case
        JPanel caseActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        caseActionsPanel.setOpaque(false);
        
        JButton addNoteBtn = new JButton("Add Note");
        addNoteBtn.addActionListener(e -> addCaseNote());
        caseActionsPanel.add(addNoteBtn);
        
        JButton linkItemBtn = new JButton("Link Evidence");
        linkItemBtn.addActionListener(e -> linkEvidenceToCase());
        caseActionsPanel.add(linkItemBtn);
        
        JButton closeCaseBtn = new JButton("Close Case");
        closeCaseBtn.setBackground(SUCCESS_COLOR);
        closeCaseBtn.setForeground(Color.WHITE);
        closeCaseBtn.addActionListener(e -> closeCase());
        caseActionsPanel.add(closeCaseBtn);
        
        detailsPanel.add(caseActionsPanel, BorderLayout.SOUTH);
        
        splitPane.setRightComponent(detailsPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Load initial data
        loadInvestigations();
        
        return panel;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> data = new HashMap<>();
                
                // Count evidence items by status
                long inCustody = evidenceItems.stream()
                    .filter(e -> e.status == EvidenceStatus.IN_CUSTODY || 
                                e.status == EvidenceStatus.UNDER_INVESTIGATION)
                    .count();
                long pendingRelease = evidenceItems.stream()
                    .filter(e -> e.status == EvidenceStatus.PENDING_RELEASE)
                    .count();
                long stolen = evidenceItems.stream()
                    .filter(e -> e.status == EvidenceStatus.FLAGGED_STOLEN)
                    .count();
                
                data.put("inCustody", inCustody);
                data.put("pendingRelease", pendingRelease);
                data.put("activeInvestigations", activeInvestigations.stream()
                    .filter(i -> i.status != InvestigationStatus.CLOSED).count());
                data.put("stolenMatches", stolen);
                
                // Count pending work requests requiring police approval
                long pendingWorkRequests = workRequestService.getRequestsForRole(
                    "POLICE_EVIDENCE_CUSTODIAN", currentUser.getOrganizationId()
                ).stream()
                    .filter(r -> r.needsApprovalFromRole("POLICE_EVIDENCE_CUSTODIAN"))
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING || 
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                data.put("pendingWorkRequests", pendingWorkRequests);
                
                // Count disputes requiring police action
                List<MultiEnterpriseDisputeResolution> disputes = workRequestService.getDisputesRequiringPolice();
                data.put("pendingDisputes", (long) disputes.size());
                
                // Get verifications requiring police
                List<VerificationRequest> policeRequired = verificationService.getPoliceRequiredRequests();
                data.put("policeRequests", policeRequired);
                
                // Get stolen flags
                List<VerificationRequest> stolenFlags = verificationService.getStolenItemFlags();
                data.put("stolenFlags", stolenFlags);
                
                return data;
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    
                    evidenceInCustodyLabel.setText(String.valueOf(data.get("inCustody")));
                    pendingReleasesLabel.setText(String.valueOf(data.get("pendingRelease")));
                    activeInvestigationsLabel.setText(String.valueOf(data.get("activeInvestigations")));
                    stolenMatchesLabel.setText(String.valueOf(data.get("stolenMatches")));
                    
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
        
        // Stolen property alerts
        long stolenCount = (Long) data.get("stolenMatches");
        if (stolenCount > 0) {
            alertsPanel.add(createAlertItem("⚠ " + stolenCount + " item(s) flagged as STOLEN PROPERTY", DANGER_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Police-required verifications
        @SuppressWarnings("unchecked")
        List<VerificationRequest> policeRequests = (List<VerificationRequest>) data.get("policeRequests");
        if (policeRequests != null && !policeRequests.isEmpty()) {
            alertsPanel.add(createAlertItem(policeRequests.size() + " verification(s) require police coordination", WARNING_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Active investigations
        long activeInvest = (Long) data.get("activeInvestigations");
        if (activeInvest > 0) {
            alertsPanel.add(createAlertItem(activeInvest + " active fraud investigation(s)", INFO_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Pending releases
        long pendingReleases = (Long) data.get("pendingRelease");
        if (pendingReleases > 0) {
            alertsPanel.add(createAlertItem(pendingReleases + " item(s) pending release to owners", SUCCESS_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Pending work requests (cross-enterprise transfers)
        long pendingWorkRequests = (Long) data.get("pendingWorkRequests");
        if (pendingWorkRequests > 0) {
            alertsPanel.add(createAlertItem("📋 " + pendingWorkRequests + " cross-enterprise transfer(s) awaiting police verification", POLICE_BLUE));
            alertsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Pending disputes requiring police action
        long pendingDisputes = (Long) data.get("pendingDisputes");
        if (pendingDisputes > 0) {
            alertsPanel.add(createAlertItem("⚖️ " + pendingDisputes + " ownership dispute(s) requiring police resolution", PURPLE_COLOR));
            alertsPanel.add(Box.createVerticalStrut(10));
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
        msgLabel.setFont(EMOJI_FONT);
        msgLabel.setForeground(color.darker());
        panel.add(msgLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadReleaseQueue() {
        releaseQueueTableModel.setRowCount(0);
        
        // Add evidence items that are pending release
        for (EvidenceItem item : evidenceItems) {
            if (item.status == EvidenceStatus.PENDING_RELEASE || 
                item.status == EvidenceStatus.IN_CUSTODY) {
                
                String idVerified = item.status == EvidenceStatus.PENDING_RELEASE ? "Yes" : "Pending";
                String status = item.status == EvidenceStatus.PENDING_RELEASE ? "Ready for Pickup" : "Awaiting ID";
                
                releaseQueueTableModel.addRow(new Object[]{
                    item.evidenceId,
                    item.itemName,
                    "John Doe", // Placeholder claimant
                    idVerified,
                    item.sourceEnterprise,
                    "Officer Smith",
                    status,
                    "Process"
                });
            }
        }
    }
    
    private void filterReleaseQueue(String statusFilter) {
        // Reload and filter
        loadReleaseQueue();
        // Additional filtering would be implemented here
    }
    
    private void loadInvestigations() {
        investigationsTableModel.setRowCount(0);
        
        for (Investigation inv : activeInvestigations) {
            investigationsTableModel.addRow(new Object[]{
                inv.caseNumber,
                inv.title,
                inv.status.getDisplayName(),
                "High",
                inv.createdAt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                "Det. " + currentUser.getLastName()
            });
        }
    }
    
    private void displayInvestigationDetails(int row) {
        if (row < 0 || row >= activeInvestigations.size()) return;
        
        Investigation inv = activeInvestigations.get(row);
        
        StringBuilder details = new StringBuilder();
        details.append("CASE: ").append(inv.caseNumber).append("\n");
        details.append("━".repeat(40)).append("\n\n");
        details.append("Title: ").append(inv.title).append("\n");
        details.append("Status: ").append(inv.status.getDisplayName()).append("\n");
        details.append("Created: ").append(inv.createdAt.format(DT_FORMATTER)).append("\n\n");
        details.append("Description:\n").append(inv.description).append("\n\n");
        details.append("━".repeat(40)).append("\n");
        details.append("CASE NOTES:\n\n");
        details.append("[").append(LocalDateTime.now().minusDays(2).format(DT_FORMATTER)).append("]\n");
        details.append("Initial report received. Evidence collected from multiple enterprises.\n\n");
        details.append("[").append(LocalDateTime.now().minusDays(1).format(DT_FORMATTER)).append("]\n");
        details.append("Cross-referenced with NCIC database. Awaiting additional documentation.\n\n");
        details.append("[").append(LocalDateTime.now().format(DT_FORMATTER)).append("]\n");
        details.append("Case under active review.\n");
        
        caseNotesArea.setText(details.toString());
        caseNotesArea.setCaretPosition(0);
    }
    
    // ==================== ACTIONS ====================
    
    private void performSerialLookup() {
        String serial = serialSearchField.getText().trim();
        if (serial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a serial number", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        serialResultsPanel.removeAll();
        
        // Simulate NCIC lookup with loading
        JLabel loadingLabel = new JLabel("Searching NCIC database...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        serialResultsPanel.add(loadingLabel);
        serialResultsPanel.revalidate();
        serialResultsPanel.repaint();
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Thread.sleep(1500); // Simulate database lookup
                // 20% chance of being stolen for demo
                return Math.random() < 0.2;
            }
            
            @Override
            protected void done() {
                try {
                    boolean isStolen = get();
                    displaySerialResults(serial, isStolen);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void displaySerialResults(String serial, boolean isStolen) {
        serialResultsPanel.removeAll();
        
        if (isStolen) {
            // STOLEN - Show warning
            JPanel alertCard = new JPanel(new BorderLayout(15, 15));
            alertCard.setBackground(new Color(248, 215, 218));
            alertCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DANGER_COLOR, 3),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            alertCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
            
            JLabel warningIcon = new JLabel("⚠ STOLEN PROPERTY ALERT", SwingConstants.CENTER);
            warningIcon.setFont(EMOJI_FONT_BOLD_24);
            warningIcon.setForeground(DANGER_COLOR);
            alertCard.add(warningIcon, BorderLayout.NORTH);
            
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
            detailsPanel.setOpaque(false);
            detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
            
            detailsPanel.add(createResultRow("Serial Number:", serial));
            detailsPanel.add(createResultRow("Status:", "REPORTED STOLEN"));
            detailsPanel.add(createResultRow("NCIC Entry:", "NIC/" + System.currentTimeMillis() % 1000000));
            detailsPanel.add(createResultRow("Reported:", LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
            detailsPanel.add(createResultRow("Jurisdiction:", "Boston Police Department"));
            detailsPanel.add(createResultRow("Original Case:", "BPD-" + (100000 + new Random().nextInt(900000))));
            
            alertCard.add(detailsPanel, BorderLayout.CENTER);
            
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            actionPanel.setOpaque(false);
            
            JButton flagBtn = new JButton("Flag Evidence");
            flagBtn.setBackground(DANGER_COLOR);
            flagBtn.setForeground(Color.WHITE);
            flagBtn.addActionListener(e -> flagStolenEvidence(serial));
            actionPanel.add(flagBtn);
            
            JButton investigateBtn = new JButton("Open Investigation");
            investigateBtn.setBackground(WARNING_COLOR);
            investigateBtn.addActionListener(e -> openStolenInvestigation(serial));
            actionPanel.add(investigateBtn);
            
            JButton printBtn = new JButton("Print Report");
            printBtn.addActionListener(e -> printStolenReport(serial));
            actionPanel.add(printBtn);
            
            alertCard.add(actionPanel, BorderLayout.SOUTH);
            
            serialResultsPanel.add(alertCard);
            
        } else {
            // NOT STOLEN - Show clear result
            JPanel clearCard = new JPanel(new BorderLayout(15, 15));
            clearCard.setBackground(new Color(209, 231, 221));
            clearCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SUCCESS_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            clearCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
            
            JLabel clearIcon = new JLabel("✓ NO MATCH FOUND", SwingConstants.CENTER);
            clearIcon.setFont(EMOJI_FONT_BOLD_24);
            clearIcon.setForeground(SUCCESS_COLOR);
            clearCard.add(clearIcon, BorderLayout.NORTH);
            
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
            detailsPanel.setOpaque(false);
            
            detailsPanel.add(createResultRow("Serial Number:", serial));
            detailsPanel.add(createResultRow("NCIC Status:", "NOT IN STOLEN DATABASE"));
            detailsPanel.add(createResultRow("Search Time:", LocalDateTime.now().format(DT_FORMATTER)));
            detailsPanel.add(createResultRow("Searched By:", currentUser.getFullName()));
            
            clearCard.add(detailsPanel, BorderLayout.CENTER);
            
            JButton logBtn = new JButton("Log Search Result");
            logBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, 
                "Search result logged for serial: " + serial, "Logged", JOptionPane.INFORMATION_MESSAGE));
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            btnPanel.add(logBtn);
            clearCard.add(btnPanel, BorderLayout.SOUTH);
            
            serialResultsPanel.add(clearCard);
        }
        
        serialResultsPanel.revalidate();
        serialResultsPanel.repaint();
    }
    
    private JPanel createResultRow(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelComp.setPreferredSize(new Dimension(120, 20));
        row.add(labelComp);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(valueComp);
        
        return row;
    }
    
    private void checkSerialNumber(String serial) {
        if (serial == null || serial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a serial number first", 
                "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        serialSearchField.setText(serial);
        tabbedPane.setSelectedIndex(2); // Switch to Serial Lookup tab
        performSerialLookup();
    }
    
    private void submitEvidenceIntake() {
        String source = intakeSourceEnterpriseField.getText().trim();
        String description = intakeDescriptionArea.getText().trim();
        
        if (source.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in Source Enterprise and Item Description",
                "Required Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create new evidence item
        String evidenceId = intakeItemIdField.getText();
        String serial = intakeSerialNumberField.getText().trim();
        String caseNum = intakeCaseNumberField.getText().trim();
        
        EvidenceItem newItem = new EvidenceItem(
            evidenceId, 
            description.substring(0, Math.min(50, description.length())),
            source,
            caseNum.isEmpty() ? null : caseNum,
            serial.isEmpty() ? null : serial,
            description,
            EvidenceStatus.IN_CUSTODY,
            LocalDateTime.now()
        );
        
        evidenceItems.add(newItem);
        
        JOptionPane.showMessageDialog(this,
            "Evidence registered successfully!\n\n" +
            "Evidence ID: " + evidenceId + "\n" +
            "Chain of custody initiated.",
            "Evidence Registered",
            JOptionPane.INFORMATION_MESSAGE);
        
        clearIntakeForm();
        loadDashboardData();
    }
    
    private void clearIntakeForm() {
        intakeItemIdField.setText(generateEvidenceId());
        intakeSourceEnterpriseField.setText("");
        intakeCaseNumberField.setText("");
        intakeSerialNumberField.setText("");
        intakeDescriptionArea.setText("");
        intakeSourceTypeCombo.setSelectedIndex(0);
    }
    
    private void printCustodyForm() {
        JOptionPane.showMessageDialog(this,
            "Chain of Custody form sent to printer:\n\n" +
            "Evidence ID: " + intakeItemIdField.getText() + "\n" +
            "Custodian: " + currentUser.getFullName() + "\n" +
            "Date/Time: " + LocalDateTime.now().format(DT_FORMATTER),
            "Print Job Submitted",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void processRelease(int row) {
        if (row < 0) return;
        
        String evidenceId = (String) releaseQueueTableModel.getValueAt(row, 0);
        String item = (String) releaseQueueTableModel.getValueAt(row, 1);
        String claimant = (String) releaseQueueTableModel.getValueAt(row, 2);
        
        int choice = JOptionPane.showConfirmDialog(this,
            "Process release for:\n\n" +
            "Evidence ID: " + evidenceId + "\n" +
            "Item: " + item + "\n" +
            "Claimant: " + claimant + "\n\n" +
            "Has ID been verified?",
            "Confirm Release",
            JOptionPane.YES_NO_CANCEL_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Complete release
            JOptionPane.showMessageDialog(this,
                "Item released successfully!\n\n" +
                "Release ID: REL-" + System.currentTimeMillis() % 100000 + "\n" +
                "Released by: " + currentUser.getFullName(),
                "Release Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Update evidence item status
            evidenceItems.stream()
                .filter(e -> e.evidenceId.equals(evidenceId))
                .findFirst()
                .ifPresent(e -> e.status = EvidenceStatus.RELEASED);
            
            loadReleaseQueue();
            loadDashboardData();
        }
    }
    
    private void showReleaseDetails(int row) {
        String evidenceId = (String) releaseQueueTableModel.getValueAt(row, 0);
        
        EvidenceItem item = evidenceItems.stream()
            .filter(e -> e.evidenceId.equals(evidenceId))
            .findFirst()
            .orElse(null);
        
        if (item == null) return;
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Release Details", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Evidence Release Details");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        
        addDetailRow(detailsPanel, "Evidence ID:", item.evidenceId);
        addDetailRow(detailsPanel, "Item:", item.itemName);
        addDetailRow(detailsPanel, "Source:", item.sourceEnterprise);
        addDetailRow(detailsPanel, "Case #:", item.caseNumber != null ? item.caseNumber : "N/A");
        addDetailRow(detailsPanel, "Serial #:", item.serialNumber != null ? item.serialNumber : "N/A");
        addDetailRow(detailsPanel, "Status:", item.status.getDisplayName());
        addDetailRow(detailsPanel, "Intake Date:", item.intakeDate.format(DT_FORMATTER));
        
        panel.add(new JScrollPane(detailsPanel), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void addDetailRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelComp.setPreferredSize(new Dimension(100, 20));
        row.add(labelComp, BorderLayout.WEST);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(valueComp, BorderLayout.CENTER);
        
        panel.add(row);
    }
    
    private void showNewInvestigationDialog() {
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
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        panel.add(priorityCombo, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "New Investigation", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String desc = descArea.getText().trim();
            
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a case title", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String caseNum = "INV-2024-" + String.format("%03d", activeInvestigations.size() + 1);
            Investigation newInv = new Investigation(caseNum, title, desc, InvestigationStatus.ACTIVE, LocalDateTime.now());
            activeInvestigations.add(newInv);
            
            JOptionPane.showMessageDialog(this,
                "Investigation created!\n\nCase #: " + caseNum,
                "Investigation Created",
                JOptionPane.INFORMATION_MESSAGE);
            
            loadInvestigations();
            loadDashboardData();
        }
    }
    
    private void addCaseNote() {
        int row = investigationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String note = JOptionPane.showInputDialog(this, "Enter case note:");
        if (note != null && !note.isEmpty()) {
            // Add to display
            String current = caseNotesArea.getText();
            caseNotesArea.setText(current + "\n\n[" + LocalDateTime.now().format(DT_FORMATTER) + "]\n" + note);
            JOptionPane.showMessageDialog(this, "Note added to case", "Note Added", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void linkEvidenceToCase() {
        int row = investigationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String evidenceId = JOptionPane.showInputDialog(this, "Enter Evidence ID to link:");
        if (evidenceId != null && !evidenceId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Evidence " + evidenceId + " linked to case",
                "Evidence Linked",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void closeCase() {
        int row = investigationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a case first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to close this case?",
            "Confirm Close",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            activeInvestigations.get(row).status = InvestigationStatus.CLOSED;
            JOptionPane.showMessageDialog(this, "Case closed", "Case Closed", JOptionPane.INFORMATION_MESSAGE);
            loadInvestigations();
            loadDashboardData();
        }
    }
    
    private void showNCICLookupDialog() {
        tabbedPane.setSelectedIndex(2); // Switch to Serial Lookup tab
        serialSearchField.requestFocus();
    }
    
    private void showBOLODialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("BOLO Type:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Stolen Property", "Suspect", "Vehicle", "Missing Person"
        });
        panel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(4, 30);
        descArea.setLineWrap(true);
        panel.add(new JScrollPane(descArea), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Broadcast To:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<String> broadcastCombo = new JComboBox<>(new String[]{
            "All Enterprises", "Universities Only", "Transit Only", "Airport Only"
        });
        panel.add(broadcastCombo, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Issue BOLO (Be On the Lookout)", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                "BOLO Alert Issued!\n\n" +
                "Type: " + typeCombo.getSelectedItem() + "\n" +
                "Broadcast: " + broadcastCombo.getSelectedItem() + "\n" +
                "Alert ID: BOLO-" + System.currentTimeMillis() % 100000,
                "BOLO Issued",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void flagStolenEvidence(String serial) {
        JOptionPane.showMessageDialog(this,
            "Evidence flagged as STOLEN PROPERTY\n\n" +
            "Serial: " + serial + "\n" +
            "Status updated in system.\n" +
            "Enterprise notifications sent.",
            "Evidence Flagged",
            JOptionPane.WARNING_MESSAGE);
    }
    
    private void openStolenInvestigation(String serial) {
        String caseNum = "INV-2024-" + String.format("%03d", activeInvestigations.size() + 1);
        Investigation newInv = new Investigation(
            caseNum,
            "Stolen Property - S/N: " + serial,
            "Investigation into stolen property match from NCIC database",
            InvestigationStatus.ACTIVE,
            LocalDateTime.now()
        );
        activeInvestigations.add(newInv);
        
        JOptionPane.showMessageDialog(this,
            "Investigation opened!\n\nCase #: " + caseNum,
            "Investigation Created",
            JOptionPane.INFORMATION_MESSAGE);
        
        loadInvestigations();
        tabbedPane.setSelectedIndex(4);
    }
    
    private void printStolenReport(String serial) {
        JOptionPane.showMessageDialog(this,
            "Stolen Property Report printed.\n\n" +
            "Serial: " + serial + "\n" +
            "Report ID: SPR-" + System.currentTimeMillis() % 100000,
            "Report Printed",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void generateEvidenceReport() {
        JOptionPane.showMessageDialog(this,
            "Evidence Report Generated!\n\n" +
            "Items in Custody: " + evidenceItems.stream().filter(e -> e.status == EvidenceStatus.IN_CUSTODY).count() + "\n" +
            "Pending Releases: " + evidenceItems.stream().filter(e -> e.status == EvidenceStatus.PENDING_RELEASE).count() + "\n" +
            "Under Investigation: " + evidenceItems.stream().filter(e -> e.status == EvidenceStatus.UNDER_INVESTIGATION).count() + "\n" +
            "Flagged Stolen: " + evidenceItems.stream().filter(e -> e.status == EvidenceStatus.FLAGGED_STOLEN).count() + "\n\n" +
            "Report saved to: evidence_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf",
            "Report Generated",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void refreshAll() {
        loadDashboardData();
        loadReleaseQueue();
        loadInvestigations();
        JOptionPane.showMessageDialog(this, "All data refreshed!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String generateEvidenceId() {
        return "EV-" + String.format("%06d", System.currentTimeMillis() % 1000000);
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Represents an evidence item in police custody
     */
    private static class EvidenceItem {
        String evidenceId;
        String itemName;
        String sourceEnterprise;
        String caseNumber;
        String serialNumber;
        String description;
        EvidenceStatus status;
        LocalDateTime intakeDate;
        
        EvidenceItem(String evidenceId, String itemName, String sourceEnterprise,
                     String caseNumber, String serialNumber, String description,
                     EvidenceStatus status, LocalDateTime intakeDate) {
            this.evidenceId = evidenceId;
            this.itemName = itemName;
            this.sourceEnterprise = sourceEnterprise;
            this.caseNumber = caseNumber;
            this.serialNumber = serialNumber;
            this.description = description;
            this.status = status;
            this.intakeDate = intakeDate;
        }
    }
    
    /**
     * Evidence item status
     */
    private enum EvidenceStatus {
        IN_CUSTODY("In Custody"),
        PENDING_RELEASE("Pending Release"),
        UNDER_INVESTIGATION("Under Investigation"),
        FLAGGED_STOLEN("Flagged - Stolen"),
        RELEASED("Released"),
        TRANSFERRED("Transferred");
        
        private final String displayName;
        
        EvidenceStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Represents a fraud investigation case
     */
    private static class Investigation {
        String caseNumber;
        String title;
        String description;
        InvestigationStatus status;
        LocalDateTime createdAt;
        
        Investigation(String caseNumber, String title, String description,
                     InvestigationStatus status, LocalDateTime createdAt) {
            this.caseNumber = caseNumber;
            this.title = title;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Investigation status
     */
    private enum InvestigationStatus {
        ACTIVE("Active"),
        PENDING_EVIDENCE("Pending Evidence"),
        UNDER_REVIEW("Under Review"),
        CLOSED("Closed");
        
        private final String displayName;
        
        InvestigationStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
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
            setText(value != null ? value.toString() : "Process");
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
            label = value != null ? value.toString() : "Process";
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
