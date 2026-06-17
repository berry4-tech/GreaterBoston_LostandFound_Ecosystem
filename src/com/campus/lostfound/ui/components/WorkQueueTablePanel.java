package com.campus.lostfound.ui.components;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.workrequest.WorkRequest;
import com.campus.lostfound.models.workrequest.WorkRequest.RequestStatus;
import com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.UIConstants;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Reusable panel component for displaying Work Requests in a JTable.
 * Features:
 * - Color-coded priority badges (URGENT=red, HIGH=orange, NORMAL=blue, LOW=gray)
 * - Status filtering dropdown
 * - SLA countdown display with overdue warnings
 * - Row selection with callback support
 * - Async data loading with refresh capability
 * 
 * @author Developer 2 - UI Components
 */
public class WorkQueueTablePanel extends JPanel {
    
    // Data
    private User currentUser;
    private WorkRequestService workRequestService;
    private MongoUserDAO userDAO;  // Use UserDAO to get trust scores from users collection
    private List<WorkRequest> allRequests;
    private List<WorkRequest> filteredRequests;
    private java.util.Map<String, Double> trustScoreCache; // Cache trust scores by email
    
    // UI Components
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JComboBox<String> priorityFilter;
    private JLabel countLabel;
    private JButton refreshButton;
    private JProgressBar loadingBar;
    
    // Callbacks
    private Consumer<WorkRequest> onRequestSelected;
    private Consumer<WorkRequest> onRequestDoubleClicked;
    
    // Constants
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 13);
    private static final Font EMOJI_FONT_BOLD = UIConstants.getEmojiFont(Font.BOLD, 11);
    private static final Font EMOJI_FONT_BOLD_12 = UIConstants.getEmojiFont(Font.BOLD, 12);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.BOLD, 18);
    
    // Column indices
    private static final int COL_PRIORITY = 0;
    private static final int COL_TYPE = 1;
    private static final int COL_SUMMARY = 2;
    private static final int COL_REQUESTER = 3;
    private static final int COL_TRUST = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_SLA = 6;
    private static final int COL_CREATED = 7;
    
    /**
     * Create a new WorkQueueTablePanel.
     * 
     * @param currentUser The logged-in user
     */
    public WorkQueueTablePanel(User currentUser) {
        this.currentUser = currentUser;
        this.workRequestService = new WorkRequestService();
        this.userDAO = new MongoUserDAO();  // Use UserDAO to get trust scores from users collection
        this.allRequests = new ArrayList<>();
        this.filteredRequests = new ArrayList<>();
        this.trustScoreCache = new java.util.HashMap<>();
        
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header with filters
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Table
        add(createTablePanel(), BorderLayout.CENTER);
        
        // Footer with loading indicator
        add(createFooterPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setOpaque(false);
        
        // Left side - Title and count
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📋 Work Queue");
        titleLabel.setFont(EMOJI_FONT_LARGE);
        titlePanel.add(titleLabel);
        
        countLabel = new JLabel("Loading...");
        countLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        countLabel.setForeground(new Color(108, 117, 125));
        titlePanel.add(countLabel);
        
        panel.add(titlePanel, BorderLayout.WEST);
        
        // Right side - Filters and refresh
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        // Status filter
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        filterPanel.add(statusLabel);
        
        statusFilter = new JComboBox<>(new String[]{
            "All", "Pending", "In Progress", "Approved", "Rejected", "Completed", "Cancelled"
        });
        statusFilter.setPreferredSize(new Dimension(120, 28));
        statusFilter.addActionListener(e -> applyFilters());
        filterPanel.add(statusFilter);
        
        // Priority filter
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        filterPanel.add(priorityLabel);
        
        priorityFilter = new JComboBox<>(new String[]{
            "All", "🔴 Urgent", "🟠 High", "🔵 Normal", "⚪ Low"
        });
        priorityFilter.setPreferredSize(new Dimension(120, 28));
        priorityFilter.addActionListener(e -> applyFilters());
        filterPanel.add(priorityFilter);
        
        // Refresh button
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadRequestsForRole(currentUser.getRole().name()));
        filterPanel.add(refreshButton);
        
        panel.add(filterPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Create table model with columns
        String[] columns = {"Priority", "Type", "Summary", "Requester", "Trust", "Status", "SLA", "Created"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        requestTable = new JTable(tableModel);
        requestTable.setRowHeight(45);
        requestTable.setFont(EMOJI_FONT);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setShowGrid(false);
        requestTable.setIntercellSpacing(new Dimension(0, 0));
        
        // Style the table header with a custom renderer to ensure visibility
        JTableHeader header = requestTable.getTableHeader();
        header.setFont(EMOJI_FONT_BOLD_12);
        header.setPreferredSize(new Dimension(0, 35));
        header.setReorderingAllowed(false);
        
        // Custom header renderer for consistent styling across all L&Fs
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                label.setBackground(new Color(52, 73, 94));
                label.setForeground(Color.WHITE);
                label.setFont(EMOJI_FONT_BOLD_12);
                label.setHorizontalAlignment(CENTER);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(44, 62, 80)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
                label.setOpaque(true);
                return label;
            }
        });
        
        // Set column widths
        TableColumnModel columnModel = requestTable.getColumnModel();
        columnModel.getColumn(COL_PRIORITY).setPreferredWidth(80);
        columnModel.getColumn(COL_PRIORITY).setMaxWidth(100);
        columnModel.getColumn(COL_TYPE).setPreferredWidth(150);
        columnModel.getColumn(COL_SUMMARY).setPreferredWidth(280);
        columnModel.getColumn(COL_REQUESTER).setPreferredWidth(120);
        columnModel.getColumn(COL_TRUST).setPreferredWidth(70);
        columnModel.getColumn(COL_TRUST).setMaxWidth(80);
        columnModel.getColumn(COL_STATUS).setPreferredWidth(100);
        columnModel.getColumn(COL_SLA).setPreferredWidth(100);
        columnModel.getColumn(COL_CREATED).setPreferredWidth(140);
        
        // Custom cell renderers
        columnModel.getColumn(COL_PRIORITY).setCellRenderer(new PriorityCellRenderer());
        columnModel.getColumn(COL_TRUST).setCellRenderer(new TrustScoreCellRenderer());
        columnModel.getColumn(COL_STATUS).setCellRenderer(new StatusCellRenderer());
        columnModel.getColumn(COL_SLA).setCellRenderer(new SlaCellRenderer());
        
        // Alternating row colors
        requestTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        
        // Selection listener
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = requestTable.getSelectedRow();
                if (row >= 0 && row < filteredRequests.size()) {
                    WorkRequest selected = filteredRequests.get(row);
                    if (onRequestSelected != null) {
                        onRequestSelected.accept(selected);
                    }
                }
            }
        });
        
        // Double-click listener
        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = requestTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < filteredRequests.size()) {
                        WorkRequest selected = filteredRequests.get(row);
                        if (onRequestDoubleClicked != null) {
                            onRequestDoubleClicked.accept(selected);
                        }
                    }
                }
            }
        });
        
        // Keyboard support - Enter key
        requestTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    int row = requestTable.getSelectedRow();
                    if (row >= 0 && row < filteredRequests.size()) {
                        WorkRequest selected = filteredRequests.get(row);
                        if (onRequestDoubleClicked != null) {
                            onRequestDoubleClicked.accept(selected);
                        }
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(requestTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Loading bar (hidden by default)
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setPreferredSize(new Dimension(0, 3));
        panel.add(loadingBar, BorderLayout.NORTH);
        
        // Help text
        JLabel helpLabel = new JLabel("Double-click a request to view details • Press Enter to open selected");
        helpLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 11));
        helpLabel.setForeground(new Color(108, 117, 125));
        helpLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(helpLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Load work requests for the current user.
     * Loads asynchronously and updates the table when complete.
     */
    public void loadRequests() {
        loadRequests(null);
    }
    
    /**
     * Load work requests with a specific data source.
     * 
     * @param requests List of requests to display (if null, loads from service)
     */
    public void loadRequests(List<WorkRequest> requests) {
        setLoading(true);
        
        if (requests != null) {
            // Use provided requests
            allRequests = new ArrayList<>(requests);
            applyFilters();
            setLoading(false);
        } else {
            // Load from service asynchronously
            SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<WorkRequest> doInBackground() {
                    return workRequestService.getRequestsForUser(
                        currentUser.getEmail(),
                        currentUser.getRole().name()
                    );
                }
                
                @Override
                protected void done() {
                    try {
                        allRequests = get();
                        applyFilters();
                    } catch (Exception e) {
                        showError("Failed to load work requests: " + e.getMessage());
                        allRequests = new ArrayList<>();
                        applyFilters();
                    } finally {
                        setLoading(false);
                    }
                }
            };
            worker.execute();
        }
    }
    
    /**
     * Load requests for a specific role (for coordinators/managers).
     * 
     * @param role The role to filter by
     */
    public void loadRequestsForRole(String role) {
        setLoading(true);
        
        SwingWorker<List<WorkRequest>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WorkRequest> doInBackground() {
                // Pass the current user's organization ID for proper filtering
                return workRequestService.getRequestsForRole(role, currentUser.getOrganizationId());
            }
            
            @Override
            protected void done() {
                try {
                    allRequests = get();
                    applyFilters();
                } catch (Exception e) {
                    showError("Failed to load work requests: " + e.getMessage());
                    allRequests = new ArrayList<>();
                    applyFilters();
                } finally {
                    setLoading(false);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Set callback for when a request is selected (single click).
     */
    public void setOnRequestSelected(Consumer<WorkRequest> callback) {
        this.onRequestSelected = callback;
    }
    
    /**
     * Set callback for when a request is double-clicked or Enter pressed.
     */
    public void setOnRequestDoubleClicked(Consumer<WorkRequest> callback) {
        this.onRequestDoubleClicked = callback;
    }
    
    /**
     * Get the currently selected work request.
     * 
     * @return The selected WorkRequest, or null if none selected
     */
    public WorkRequest getSelectedRequest() {
        int row = requestTable.getSelectedRow();
        if (row >= 0 && row < filteredRequests.size()) {
            return filteredRequests.get(row);
        }
        return null;
    }
    
    /**
     * Get all currently displayed requests.
     */
    public List<WorkRequest> getDisplayedRequests() {
        return new ArrayList<>(filteredRequests);
    }
    
    /**
     * Get the count of pending requests.
     */
    public int getPendingCount() {
        return (int) allRequests.stream()
            .filter(r -> r.getStatus() == RequestStatus.PENDING || 
                        r.getStatus() == RequestStatus.IN_PROGRESS)
            .count();
    }
    
    /**
     * Get the count of overdue requests.
     */
    public int getOverdueCount() {
        return (int) allRequests.stream()
            .filter(WorkRequest::isOverdue)
            .count();
    }
    
    /**
     * Clear all filters and show all requests.
     */
    public void clearFilters() {
        statusFilter.setSelectedIndex(0);
        priorityFilter.setSelectedIndex(0);
        applyFilters();
    }
    
    // ==================== PRIVATE HELPERS ====================
    
    private void applyFilters() {
        String statusSelection = (String) statusFilter.getSelectedItem();
        String prioritySelection = (String) priorityFilter.getSelectedItem();
        
        filteredRequests = allRequests.stream()
            .filter(r -> matchesStatusFilter(r, statusSelection))
            .filter(r -> matchesPriorityFilter(r, prioritySelection))
            .sorted((a, b) -> {
                // Sort by priority (URGENT first), then by creation date (newest first)
                int priorityCompare = a.getPriority().ordinal() - b.getPriority().ordinal();
                if (priorityCompare != 0) return priorityCompare;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
            .collect(Collectors.toList());
        
        updateTable();
        updateCountLabel();
    }
    
    private boolean matchesStatusFilter(WorkRequest request, String filter) {
        if ("All".equals(filter)) return true;
        
        return switch (filter) {
            case "Pending" -> request.getStatus() == RequestStatus.PENDING;
            case "In Progress" -> request.getStatus() == RequestStatus.IN_PROGRESS;
            case "Approved" -> request.getStatus() == RequestStatus.APPROVED;
            case "Rejected" -> request.getStatus() == RequestStatus.REJECTED;
            case "Completed" -> request.getStatus() == RequestStatus.COMPLETED;
            case "Cancelled" -> request.getStatus() == RequestStatus.CANCELLED;
            default -> true;
        };
    }
    
    private boolean matchesPriorityFilter(WorkRequest request, String filter) {
        if ("All".equals(filter)) return true;
        
        return switch (filter) {
            case "🔴 Urgent" -> request.getPriority() == RequestPriority.URGENT;
            case "🟠 High" -> request.getPriority() == RequestPriority.HIGH;
            case "🔵 Normal" -> request.getPriority() == RequestPriority.NORMAL;
            case "⚪ Low" -> request.getPriority() == RequestPriority.LOW;
            default -> true;
        };
    }
    
    private void updateTable() {
        tableModel.setRowCount(0);
        
        System.out.println("\n=== updateTable DEBUG ===");
        System.out.println("Number of filtered requests: " + filteredRequests.size());
        
        for (WorkRequest request : filteredRequests) {
            // Debug: print requester info
            System.out.println("Request: " + request.getRequestSummary());
            System.out.println("  getRequesterEmail(): '" + request.getRequesterEmail() + "'");
            System.out.println("  getRequesterId(): '" + request.getRequesterId() + "'");
            
            // Get trust score - use requesterEmail, fallback to requesterId (which often contains email)
            String emailForLookup = request.getRequesterEmail();
            if (emailForLookup == null || emailForLookup.isEmpty()) {
                emailForLookup = request.getRequesterId();  // requesterId often contains email
                System.out.println("  Using requesterId as fallback: " + emailForLookup);
            }
            double trustScore = getTrustScoreForRequester(emailForLookup);
            System.out.println("  Trust score retrieved: " + trustScore);
            
            Object[] row = {
                request.getPriority(),
                formatRequestType(request.getRequestType()),
                request.getRequestSummary(),
                request.getRequesterName(),
                trustScore,  // Trust score column
                request.getStatus(),
                request.getHoursUntilSla(),
                request.getCreatedAt().format(DATE_FORMAT)
            };
            tableModel.addRow(row);
        }
    }
    
    /**
     * Get trust score for a requester, using cache to avoid repeated lookups.
     * Gets trust score from the users collection (consistent with header display).
     */
    private double getTrustScoreForRequester(String requesterEmail) {
        System.out.println("\n=== getTrustScoreForRequester DEBUG ===");
        System.out.println("Input requesterEmail: '" + requesterEmail + "'");
        
        if (requesterEmail == null || requesterEmail.isEmpty()) {
            System.out.println("Email is null/empty, returning default 50.0");
            return 50.0;
        }
        
        return trustScoreCache.computeIfAbsent(requesterEmail, email -> {
            try {
                System.out.println("Cache miss - looking up user by email: " + email);
                // Get trust score from users collection (NOT trust_scores collection)
                java.util.Optional<User> userOpt = userDAO.findByEmail(email);
                if (userOpt.isPresent()) {
                    double score = userOpt.get().getTrustScore();
                    System.out.println("Found user! Trust score from users collection: " + score);
                    return score;
                }
                System.out.println("User NOT found for email: " + email);
                return 65.0; // Default if user not found
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
                return 50.0; // Default on error
            }
        });
    }
    
    private void updateCountLabel() {
        int total = filteredRequests.size();
        int overdue = (int) filteredRequests.stream().filter(WorkRequest::isOverdue).count();
        
        if (overdue > 0) {
            countLabel.setText(String.format("%d request%s (%d overdue)", 
                total, total != 1 ? "s" : "", overdue));
            countLabel.setForeground(new Color(220, 53, 69));
        } else {
            countLabel.setText(String.format("%d request%s", total, total != 1 ? "s" : ""));
            countLabel.setForeground(new Color(108, 117, 125));
        }
    }
    
    private String formatRequestType(WorkRequest.RequestType type) {
        return switch (type) {
            case ITEM_CLAIM -> "📦 Item Claim";
            case CROSS_CAMPUS_TRANSFER -> "🔄 Campus Transfer";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "🚇 Transit Transfer";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "✈️ Airport Transfer";
            case POLICE_EVIDENCE_REQUEST -> "🚔 Police Evidence";
            case MBTA_TO_AIRPORT_EMERGENCY -> "🚨 MBTA-Airport Emergency";
            case MULTI_ENTERPRISE_DISPUTE -> "⚖️ Multi-Enterprise Dispute";
        };
    }
    
    private void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        refreshButton.setEnabled(!loading);
        statusFilter.setEnabled(!loading);
        priorityFilter.setEnabled(!loading);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== CUSTOM CELL RENDERERS ====================
    
    /**
     * Renderer for priority column with colored badges.
     */
    private class PriorityCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setHorizontalAlignment(CENTER);
            label.setFont(EMOJI_FONT_BOLD);
            
            if (value instanceof RequestPriority priority) {
                switch (priority) {
                    case URGENT -> {
                        label.setText("🔴 URGENT");
                        label.setBackground(isSelected ? new Color(200, 50, 50) : new Color(255, 235, 235));
                        label.setForeground(new Color(155, 28, 28));
                    }
                    case HIGH -> {
                        label.setText("🟠 HIGH");
                        label.setBackground(isSelected ? new Color(200, 120, 50) : new Color(255, 243, 224));
                        label.setForeground(new Color(180, 95, 6));
                    }
                    case NORMAL -> {
                        label.setText("🔵 NORMAL");
                        label.setBackground(isSelected ? new Color(50, 120, 200) : new Color(232, 244, 253));
                        label.setForeground(new Color(13, 110, 253));
                    }
                    case LOW -> {
                        label.setText("⚪ LOW");
                        label.setBackground(isSelected ? new Color(150, 150, 150) : new Color(248, 249, 250));
                        label.setForeground(new Color(108, 117, 125));
                    }
                }
            }
            
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return label;
        }
    }
    
    /**
     * Renderer for status column with color coding.
     */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setHorizontalAlignment(CENTER);
            label.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
            
            if (value instanceof RequestStatus status) {
                switch (status) {
                    case PENDING -> {
                        label.setText("⏳ Pending");
                        if (!isSelected) label.setForeground(new Color(255, 152, 0));
                    }
                    case IN_PROGRESS -> {
                        label.setText("🔄 In Progress");
                        if (!isSelected) label.setForeground(new Color(33, 150, 243));
                    }
                    case APPROVED -> {
                        label.setText("✅ Approved");
                        if (!isSelected) label.setForeground(new Color(76, 175, 80));
                    }
                    case REJECTED -> {
                        label.setText("❌ Rejected");
                        if (!isSelected) label.setForeground(new Color(244, 67, 54));
                    }
                    case COMPLETED -> {
                        label.setText("✔️ Completed");
                        if (!isSelected) label.setForeground(new Color(0, 150, 136));
                    }
                    case CANCELLED -> {
                        label.setText("🚫 Cancelled");
                        if (!isSelected) label.setForeground(new Color(158, 158, 158));
                    }
                }
            }
            
            return label;
        }
    }
    
    /**
     * Renderer for SLA column with countdown and overdue warning.
     */
    private class SlaCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setHorizontalAlignment(CENTER);
            label.setFont(EMOJI_FONT_BOLD);
            
            if (value instanceof Long hours) {
                if (hours < 0) {
                    // Overdue
                    label.setText("⚠️ OVERDUE");
                    label.setForeground(Color.WHITE);
                    label.setBackground(new Color(220, 53, 69));
                    label.setOpaque(true);
                } else if (hours <= 4) {
                    // Critical - less than 4 hours
                    label.setText(hours + "h left");
                    label.setForeground(isSelected ? Color.WHITE : new Color(220, 53, 69));
                    label.setOpaque(false);
                } else if (hours <= 24) {
                    // Warning - less than 24 hours
                    label.setText(hours + "h left");
                    label.setForeground(isSelected ? Color.WHITE : new Color(255, 152, 0));
                    label.setOpaque(false);
                } else {
                    // OK
                    long days = hours / 24;
                    if (days > 0) {
                        label.setText(days + "d " + (hours % 24) + "h");
                    } else {
                        label.setText(hours + "h left");
                    }
                    label.setForeground(isSelected ? Color.WHITE : new Color(40, 167, 69));
                    label.setOpaque(false);
                }
            }
            
            return label;
        }
    }
    
    /**
     * Renderer for trust score column with color-coded score.
     */
    private class TrustScoreCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setHorizontalAlignment(CENTER);
            label.setFont(EMOJI_FONT_BOLD);
            label.setOpaque(true);
            
            if (value instanceof Double score) {
                label.setText(String.format("%.0f", score));
                
                if (!isSelected) {
                    // Color based on score level
                    if (score >= 85) {
                        label.setBackground(new Color(209, 231, 221)); // Light green
                        label.setForeground(new Color(25, 135, 84));   // Dark green
                    } else if (score >= 70) {
                        label.setBackground(new Color(207, 226, 255)); // Light blue
                        label.setForeground(new Color(13, 110, 253));  // Blue
                    } else if (score >= 50) {
                        label.setBackground(new Color(255, 243, 205)); // Light yellow
                        label.setForeground(new Color(133, 100, 4));   // Dark yellow
                    } else if (score >= 30) {
                        label.setBackground(new Color(255, 237, 213)); // Light orange
                        label.setForeground(new Color(180, 95, 6));    // Dark orange
                    } else {
                        label.setBackground(new Color(248, 215, 218)); // Light red
                        label.setForeground(new Color(155, 28, 28));   // Dark red
                    }
                } else {
                    // When selected, keep the selection colors but add indicator
                    if (score < 50) {
                        label.setText(String.format("⚠️ %.0f", score));
                    }
                }
            } else {
                label.setText("N/A");
                label.setBackground(isSelected ? label.getBackground() : new Color(248, 249, 250));
            }
            
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return label;
        }
    }
    
    /**
     * Renderer for alternating row colors.
     */
    private class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            if (isSelected) {
                c.setBackground(new Color(51, 122, 183));
                c.setForeground(Color.WHITE);
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 249, 249));
                c.setForeground(Color.BLACK);
            }
            
            // Add padding
            if (c instanceof JLabel label) {
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            
            return c;
        }
    }
}
