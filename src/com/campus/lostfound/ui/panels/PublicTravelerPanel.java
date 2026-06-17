package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.models.workrequest.MultiEnterpriseDisputeResolution.*;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.components.*;
import com.campus.lostfound.ui.dialogs.RequestDetailDialog;

import com.campus.lostfound.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main panel for Public Traveler users.
 * 
 * This panel is designed for non-student public users who:
 * - Lost items on MBTA transit or at Logan Airport
 * - Need to claim items with government ID verification
 * - May need emergency coordination for urgent situations
 * 
 * Features:
 * - Simple, user-friendly report form
 * - Track report status
 * - Claim with enhanced ID verification
 * - Emergency request for urgent situations (passport at airport)
 * 
 * @author Developer 2 - UI Panels
 */
public class PublicTravelerPanel extends JPanel {
    
    // Data
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private WorkRequestService workRequestService;
    private ItemMatcher itemMatcher;
    
    // UI Components
    private JTabbedPane tabbedPane;
    
    // Dashboard components
    private JLabel openReportsLabel;
    private JLabel pendingClaimsLabel;
    private JPanel recentActivityPanel;
    
    // Report Lost Item components
    private JTextField reportTitleField;
    private JTextArea reportDescriptionArea;
    private JComboBox<Item.ItemCategory> reportCategoryCombo;
    private JComboBox<String> reportLocationTypeCombo;
    private JComboBox<String> reportTransitLineCombo;
    private JTextField reportStationField;
    private JTextField reportDateField;
    private JTextField reportTimeField;
    private JTextField reportColorField;
    private JTextField reportBrandField;
    private JTextField reportContactEmailField;
    private JTextField reportContactPhoneField;
    
    // Track Reports components
    private JPanel myReportsListPanel;
    private JComboBox<String> reportsFilterCombo;
    private JTextField trackingNumberField;
    
    // Search & Claim components
    private JTextField searchQueryField;
    private JComboBox<String> searchLocationCombo;
    private JComboBox<Item.ItemCategory> searchCategoryCombo;
    private JPanel searchResultsPanel;
    private List<Item> currentSearchResults;
    
    // Emergency Request components
    private JComboBox<String> emergencyTypeCombo;
    private JTextArea emergencyDescriptionArea;
    private JTextField emergencyContactField;
    private JTextField emergencyFlightField;
    private JTextField emergencyGateField;
    
    // Disputes Tab components
    private JPanel disputesListPanel;
    private JComboBox<String> disputeFilterCombo;
    private List<MultiEnterpriseDisputeResolution> currentDisputes;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    private static final Color PRIMARY_COLOR = new Color(0, 102, 153);      // Transit blue
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color EMERGENCY_COLOR = new Color(156, 39, 176);   // Purple for emergency
    
    // Emoji-capable font for icons
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 48);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 36);
    private static final Font EMOJI_FONT_SMALL = UIConstants.getEmojiFont(Font.PLAIN, 32);
    
    // Transit location options
    private static final String[] LOCATION_TYPES = {
        "MBTA Subway", "MBTA Bus", "MBTA Commuter Rail", 
        "Logan Airport - Terminal A", "Logan Airport - Terminal B", 
        "Logan Airport - Terminal C", "Logan Airport - Terminal E",
        "Station Platform/Area", "Other"
    };
    
    private static final Map<String, String[]> TRANSIT_LINES = new HashMap<>();
    
    static {
        TRANSIT_LINES.put("MBTA Subway", new String[]{"Red Line", "Orange Line", "Blue Line", "Green Line B", "Green Line C", "Green Line D", "Green Line E"});
        TRANSIT_LINES.put("MBTA Bus", new String[]{"Bus Route (specify in description)"});
        TRANSIT_LINES.put("MBTA Commuter Rail", new String[]{"Framingham/Worcester", "Fitchburg", "Haverhill", "Lowell", "Newburyport/Rockport", "Providence/Stoughton"});
        TRANSIT_LINES.put("Logan Airport - Terminal A", new String[]{"Gate Area", "Security Checkpoint", "Baggage Claim", "Restroom Area"});
        TRANSIT_LINES.put("Logan Airport - Terminal B", new String[]{"Gate Area", "Security Checkpoint", "Baggage Claim", "Restroom Area"});
        TRANSIT_LINES.put("Logan Airport - Terminal C", new String[]{"Gate Area", "Security Checkpoint", "Baggage Claim", "Restroom Area"});
        TRANSIT_LINES.put("Logan Airport - Terminal E", new String[]{"Gate Area", "Security Checkpoint", "Baggage Claim", "Restroom Area"});
        TRANSIT_LINES.put("Station Platform/Area", new String[]{"Platform", "Entrance/Exit", "Ticket Area", "Waiting Area"});
        TRANSIT_LINES.put("Other", new String[]{"Please specify in description"});
    }
    
    /**
     * Create a new PublicTravelerPanel.
     * 
     * @param currentUser The logged-in public traveler
     */
    public PublicTravelerPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestService = new WorkRequestService();
        this.itemMatcher = new ItemMatcher();
        this.currentSearchResults = new ArrayList<>();
        this.currentDisputes = new ArrayList<>();
        
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
        tabbedPane.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));  // Use emoji-capable font
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        // Create tabs - simpler for public users
        tabbedPane.addTab("🏠 Home", createDashboardTab());
        tabbedPane.addTab("📝 Report Lost Item", createReportLostTab());
        tabbedPane.addTab("📋 Track My Reports", createTrackReportsTab());
        tabbedPane.addTab("🔍 Search & Claim", createSearchClaimTab());
        tabbedPane.addTab("⚖️ Disputes", createDisputesTab());
        tabbedPane.addTab("🚨 Emergency Request", createEmergencyTab());
        
        // Tab change listener
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            switch (index) {
                case 0 -> loadDashboardData();
                case 2 -> refreshMyReports();
                case 4 -> refreshDisputes();
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
        
        JLabel welcomeLabel = new JLabel("🚇 Greater Boston Lost & Found");
        welcomeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(welcomeLabel);
        
        JLabel subtitleLabel = new JLabel("MBTA • Logan Airport • Public Transit Recovery System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 220, 240));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitleLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right - User info
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JLabel userLabel = new JLabel("👤 " + currentUser.getFullName());
        userLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 14));
        userLabel.setForeground(Color.WHITE);
        rightPanel.add(userLabel);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    // ==================== TAB 1: DASHBOARD ====================
    
    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Welcome message
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setOpaque(false);
        welcomePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel helloLabel = new JLabel("Welcome, " + currentUser.getFirstName() + "!");
        helloLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        helloLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomePanel.add(helloLabel);
        
        JLabel helpLabel = new JLabel("How can we help you today?");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        helpLabel.setForeground(new Color(108, 117, 125));
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomePanel.add(helpLabel);
        welcomePanel.add(Box.createVerticalStrut(25));
        
        panel.add(welcomePanel, BorderLayout.NORTH);
        
        // Quick action cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        cardsPanel.setOpaque(false);
        
        cardsPanel.add(createActionCard("📝", "Report Lost Item", 
            "Lost something on transit? Report it here.", 
            PRIMARY_COLOR, () -> tabbedPane.setSelectedIndex(1)));
        
        cardsPanel.add(createActionCard("🔍", "Search Found Items", 
            "Browse items that have been found.", 
            SUCCESS_COLOR, () -> tabbedPane.setSelectedIndex(3)));
        
        cardsPanel.add(createActionCard("📋", "Track My Reports", 
            "Check the status of your lost item reports.", 
            WARNING_COLOR, () -> tabbedPane.setSelectedIndex(2)));
        
        cardsPanel.add(createActionCard("🚨", "Emergency Request", 
            "Urgent? Passport at airport? Get help fast.", 
            EMERGENCY_COLOR, () -> tabbedPane.setSelectedIndex(4)));
        
        panel.add(cardsPanel, BorderLayout.CENTER);
        
        // Bottom stats
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 10));
        statsPanel.setOpaque(false);
        TitledBorder statsBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Your Activity"
        );
        statsBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        statsPanel.setBorder(statsBorder);
        
        openReportsLabel = new JLabel("0 Open Reports");
        openReportsLabel.setFont(EMOJI_FONT);
        statsPanel.add(openReportsLabel);
        
        pendingClaimsLabel = new JLabel("0 Pending Claims");
        pendingClaimsLabel.setFont(EMOJI_FONT);
        statsPanel.add(pendingClaimsLabel);
        
        panel.add(statsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createActionCard(String icon, String title, String description, Color color, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(EMOJI_FONT_LARGE);
        card.add(iconLabel, BorderLayout.WEST);
        
        // Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(color);
        textPanel.add(titleLabel);
        
        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(108, 117, 125));
        textPanel.add(descLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        // Click handler
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(248, 249, 250));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
            }
        });
        
        return card;
    }
    
    // ==================== TAB 2: REPORT LOST ITEM ====================
    
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
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Fill out this form to report an item you lost on public transit or at the airport");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(subtitleLabel);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Section: What did you lose?
        formContainer.add(createSectionLabel("What did you lose?"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Item Name/Description *"));
        reportTitleField = new JTextField();
        reportTitleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        formContainer.add(reportTitleField);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Category *"));
        reportCategoryCombo = new JComboBox<>(Item.ItemCategory.values());
        reportCategoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        reportCategoryCombo.setRenderer(new CategoryComboRenderer());
        formContainer.add(reportCategoryCombo);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Detailed Description *"));
        reportDescriptionArea = new JTextArea(3, 30);
        reportDescriptionArea.setLineWrap(true);
        reportDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(reportDescriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(10));
        
        // Color and Brand row
        JPanel detailsRow = new JPanel(new GridLayout(1, 2, 15, 0));
        detailsRow.setOpaque(false);
        detailsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        detailsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel colorPanel = createFieldPanel("Color");
        reportColorField = new JTextField();
        colorPanel.add(reportColorField);
        detailsRow.add(colorPanel);
        
        JPanel brandPanel = createFieldPanel("Brand");
        reportBrandField = new JTextField();
        brandPanel.add(reportBrandField);
        detailsRow.add(brandPanel);
        
        formContainer.add(detailsRow);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Section: Where did you lose it?
        formContainer.add(createSectionLabel("Where did you lose it?"));
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Location Type *"));
        reportLocationTypeCombo = new JComboBox<>(LOCATION_TYPES);
        reportLocationTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        reportLocationTypeCombo.addActionListener(e -> updateTransitLineCombo());
        formContainer.add(reportLocationTypeCombo);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Line/Area *"));
        reportTransitLineCombo = new JComboBox<>();
        reportTransitLineCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        updateTransitLineCombo();
        formContainer.add(reportTransitLineCombo);
        formContainer.add(Box.createVerticalStrut(10));
        
        formContainer.add(createFormLabel("Station/Stop Name"));
        reportStationField = new JTextField();
        reportStationField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        formContainer.add(reportStationField);
        formContainer.add(Box.createVerticalStrut(10));
        
        // Date and Time row
        JPanel dateTimeRow = new JPanel(new GridLayout(1, 2, 15, 0));
        dateTimeRow.setOpaque(false);
        dateTimeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        dateTimeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel datePanel = createFieldPanel("Approximate Date *");
        reportDateField = new JTextField();
        reportDateField.setText(DATE_FORMAT.format(new Date()));
        datePanel.add(reportDateField);
        dateTimeRow.add(datePanel);
        
        JPanel timePanel = createFieldPanel("Approximate Time");
        reportTimeField = new JTextField();
        reportTimeField.setText(TIME_FORMAT.format(new Date()));
        timePanel.add(reportTimeField);
        dateTimeRow.add(timePanel);
        
        formContainer.add(dateTimeRow);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Section: Contact Information
        formContainer.add(createSectionLabel("How can we reach you?"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel contactRow = new JPanel(new GridLayout(1, 2, 15, 0));
        contactRow.setOpaque(false);
        contactRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        contactRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel emailPanel = createFieldPanel("Email *");
        reportContactEmailField = new JTextField();
        reportContactEmailField.setText(currentUser.getEmail());
        emailPanel.add(reportContactEmailField);
        contactRow.add(emailPanel);
        
        JPanel phonePanel = createFieldPanel("Phone Number *");
        reportContactPhoneField = new JTextField();
        reportContactPhoneField.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        phonePanel.add(reportContactPhoneField);
        contactRow.add(phonePanel);
        
        formContainer.add(contactRow);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton submitButton = new JButton("Submit Report");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitButton.setBackground(PRIMARY_COLOR);
        submitButton.setForeground(Color.BLACK);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setPreferredSize(new Dimension(180, 45));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> submitLostItemReport());
        buttonPanel.add(submitButton);
        
        JButton clearButton = new JButton("Clear Form");
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
    
    private void updateTransitLineCombo() {
        String locationType = (String) reportLocationTypeCombo.getSelectedItem();
        reportTransitLineCombo.removeAllItems();
        if (locationType != null && TRANSIT_LINES.containsKey(locationType)) {
            for (String line : TRANSIT_LINES.get(locationType)) {
                reportTransitLineCombo.addItem(line);
            }
        }
    }
    
    // ==================== TAB 3: TRACK MY REPORTS ====================
    
    private JPanel createTrackReportsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header with filter and tracking
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Track My Reports");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        // Tracking number lookup
        filterPanel.add(new JLabel("Tracking #:"));
        trackingNumberField = new JTextField(12);
        filterPanel.add(trackingNumberField);
        
        JButton lookupBtn = new JButton("Lookup");
        lookupBtn.addActionListener(e -> lookupByTrackingNumber());
        filterPanel.add(lookupBtn);
        
        filterPanel.add(Box.createHorizontalStrut(20));
        
        // Filter
        filterPanel.add(new JLabel("Filter:"));
        reportsFilterCombo = new JComboBox<>(new String[]{
            "All Reports", "Open", "In Progress", "Resolved"
        });
        reportsFilterCombo.setPreferredSize(new Dimension(120, 28));
        reportsFilterCombo.addActionListener(e -> refreshMyReports());
        filterPanel.add(reportsFilterCombo);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMyReports());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Reports list
        myReportsListPanel = new JPanel();
        myReportsListPanel.setLayout(new BoxLayout(myReportsListPanel, BoxLayout.Y_AXIS));
        myReportsListPanel.setBackground(Color.WHITE);
        myReportsListPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(myReportsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 4: SEARCH & CLAIM ====================
    
    private JPanel createSearchClaimTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Search controls
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setOpaque(false);
        TitledBorder searchBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            "Search Found Items"
        );
        searchBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        searchPanel.setBorder(searchBorder);
        
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchRow.setOpaque(false);
        
        searchRow.add(new JLabel("Keywords:"));
        searchQueryField = new JTextField(20);
        searchRow.add(searchQueryField);
        
        searchRow.add(new JLabel("Location:"));
        searchLocationCombo = new JComboBox<>();
        searchLocationCombo.addItem("All Locations");
        for (String loc : LOCATION_TYPES) {
            searchLocationCombo.addItem(loc);
        }
        searchLocationCombo.setPreferredSize(new Dimension(180, 28));
        searchRow.add(searchLocationCombo);
        
        searchRow.add(new JLabel("Category:"));
        searchCategoryCombo = new JComboBox<>();
        searchCategoryCombo.addItem(null); // All categories
        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            searchCategoryCombo.addItem(cat);
        }
        searchCategoryCombo.setRenderer(new CategoryComboRenderer());
        searchCategoryCombo.setPreferredSize(new Dimension(180, 28));
        searchRow.add(searchCategoryCombo);
        
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(PRIMARY_COLOR);
        searchButton.setForeground(Color.BLACK);
        searchButton.addActionListener(e -> performSearch());
        searchRow.add(searchButton);
        
        searchPanel.add(searchRow);
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Search results
        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(Color.WHITE);
        searchResultsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        // Initial message
        JLabel initialLabel = new JLabel("Enter search criteria and click Search to find items");
        initialLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        initialLabel.setForeground(new Color(108, 117, 125));
        initialLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchResultsPanel.add(initialLabel);
        
        JScrollPane scrollPane = new JScrollPane(searchResultsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== TAB 5: EMERGENCY REQUEST ====================
    
    private JPanel createEmergencyTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form container
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
        
        // Warning banner
        JPanel warningBanner = new JPanel(new BorderLayout(15, 0));
        warningBanner.setBackground(new Color(EMERGENCY_COLOR.getRed(), EMERGENCY_COLOR.getGreen(), EMERGENCY_COLOR.getBlue(), 40));
        warningBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(EMERGENCY_COLOR, 2),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        warningBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        warningBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel warningIcon = new JLabel("🚨");
        warningIcon.setFont(EMOJI_FONT_SMALL);
        warningBanner.add(warningIcon, BorderLayout.WEST);
        
        JLabel warningText = new JLabel("<html><b>Emergency Request</b><br>" +
            "Use this form only for urgent situations like a passport needed for an imminent flight.</html>");
        warningText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        warningBanner.add(warningText, BorderLayout.CENTER);
        
        formContainer.add(warningBanner);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Title
        JLabel titleLabel = new JLabel("Emergency Coordination Request");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(EMERGENCY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(titleLabel);
        formContainer.add(Box.createVerticalStrut(20));
        
        // Emergency type
        formContainer.add(createFormLabel("Emergency Type *"));
        emergencyTypeCombo = new JComboBox<>(new String[]{
            "Passport/Travel Document at Airport Gate",
            "Passport/ID Lost on MBTA - Need for Flight",
            "Medical Device Left Behind",
            "Time-Sensitive Document",
            "Other Urgent Item"
        });
        emergencyTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        formContainer.add(emergencyTypeCombo);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Description
        formContainer.add(createFormLabel("Describe Your Emergency *"));
        emergencyDescriptionArea = new JTextArea(4, 30);
        emergencyDescriptionArea.setLineWrap(true);
        emergencyDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(emergencyDescriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(descScroll);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Flight details
        formContainer.add(createSectionLabel("Flight Information (if applicable)"));
        formContainer.add(Box.createVerticalStrut(10));
        
        JPanel flightRow = new JPanel(new GridLayout(1, 2, 15, 0));
        flightRow.setOpaque(false);
        flightRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        flightRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel flightPanel = createFieldPanel("Flight Number");
        emergencyFlightField = new JTextField();
        flightPanel.add(emergencyFlightField);
        flightRow.add(flightPanel);
        
        JPanel gatePanel = createFieldPanel("Gate Number");
        emergencyGateField = new JTextField();
        gatePanel.add(emergencyGateField);
        flightRow.add(gatePanel);
        
        formContainer.add(flightRow);
        formContainer.add(Box.createVerticalStrut(15));
        
        // Contact
        formContainer.add(createFormLabel("Best Phone Number to Reach You *"));
        emergencyContactField = new JTextField();
        emergencyContactField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        emergencyContactField.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        formContainer.add(emergencyContactField);
        formContainer.add(Box.createVerticalStrut(25));
        
        // Submit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton submitButton = new JButton("Submit Emergency Request");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitButton.setBackground(EMERGENCY_COLOR);
        submitButton.setForeground(Color.BLACK);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setPreferredSize(new Dimension(250, 50));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> submitEmergencyRequest());
        buttonPanel.add(submitButton);
        
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
    
    // ==================== TAB 5: DISPUTES ====================
    
    private JPanel createDisputesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);
        
        JLabel titleLabel = new JLabel("⚖️ My Disputes");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftHeader.add(titleLabel);
        
        JLabel helpLabel = new JLabel("When multiple people claim the same item, a dispute is created for fair resolution");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftHeader.add(helpLabel);
        
        headerPanel.add(leftHeader, BorderLayout.WEST);
        
        // Filter and refresh
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        filterPanel.add(new JLabel("Filter:"));
        disputeFilterCombo = new JComboBox<>(new String[]{
            "All Disputes", "Pending", "Under Review", "Resolved", "Escalated"
        });
        disputeFilterCombo.setPreferredSize(new Dimension(140, 28));
        disputeFilterCombo.addActionListener(e -> filterAndDisplayDisputes());
        filterPanel.add(disputeFilterCombo);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(EMOJI_FONT);
        refreshBtn.addActionListener(e -> refreshDisputes());
        filterPanel.add(refreshBtn);
        
        headerPanel.add(filterPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Disputes list
        disputesListPanel = new JPanel();
        disputesListPanel.setLayout(new BoxLayout(disputesListPanel, BoxLayout.Y_AXIS));
        disputesListPanel.setBackground(Color.WHITE);
        disputesListPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(disputesListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshDisputes() {
        SwingWorker<List<MultiEnterpriseDisputeResolution>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MultiEnterpriseDisputeResolution> doInBackground() {
                // Get all disputes from the work request service
                List<WorkRequest> allRequests = workRequestService.getRequestsForUser(
                    currentUser.getEmail(), currentUser.getRole().name());
                
                // Also find disputes where user is a claimant
                List<WorkRequest> allDisputes = new MongoWorkRequestDAO()
                    .findByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE);
                
                List<MultiEnterpriseDisputeResolution> userDisputes = new ArrayList<>();
                
                for (WorkRequest wr : allDisputes) {
                    if (wr instanceof MultiEnterpriseDisputeResolution dispute) {
                        // Check if user is a claimant in this dispute
                        for (Claimant c : dispute.getClaimants()) {
                            if (c.claimantId != null && c.claimantId.equalsIgnoreCase(currentUser.getEmail()) ||
                                c.claimantEmail != null && c.claimantEmail.equalsIgnoreCase(currentUser.getEmail())) {
                                userDisputes.add(dispute);
                                break;
                            }
                        }
                    }
                }
                
                return userDisputes;
            }
            
            @Override
            protected void done() {
                try {
                    currentDisputes = get();
                    filterAndDisplayDisputes();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to load disputes: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void filterAndDisplayDisputes() {
        String filter = (String) disputeFilterCombo.getSelectedItem();
        List<MultiEnterpriseDisputeResolution> filtered = currentDisputes;
        
        if (filter != null && !filter.equals("All Disputes")) {
            filtered = currentDisputes.stream()
                .filter(d -> {
                    String status = d.getResolutionStatus();
                    return switch (filter) {
                        case "Pending" -> "PENDING".equals(status);
                        case "Under Review" -> "UNDER_REVIEW".equals(status);
                        case "Resolved" -> "RESOLVED".equals(status);
                        case "Escalated" -> "ESCALATED".equals(status);
                        default -> true;
                    };
                })
                .collect(Collectors.toList());
        }
        
        displayDisputes(filtered);
    }
    
    private void displayDisputes(List<MultiEnterpriseDisputeResolution> disputes) {
        disputesListPanel.removeAll();
        
        if (disputes.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
            emptyPanel.setOpaque(false);
            emptyPanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            
            JLabel emptyIcon = new JLabel("⚖️");
            emptyIcon.setFont(UIConstants.getEmojiFont(Font.PLAIN, 48));
            emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(emptyIcon);
            
            emptyPanel.add(Box.createVerticalStrut(15));
            
            JLabel emptyLabel = new JLabel("No disputes found");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(emptyLabel);
            
            JLabel helpLabel = new JLabel("Disputes occur when multiple people claim the same item");
            helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            helpLabel.setForeground(new Color(160, 160, 160));
            helpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(helpLabel);
            
            disputesListPanel.add(emptyPanel);
        } else {
            for (MultiEnterpriseDisputeResolution dispute : disputes) {
                disputesListPanel.add(createDisputeCard(dispute));
                disputesListPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        disputesListPanel.revalidate();
        disputesListPanel.repaint();
    }
    
    private JPanel createDisputeCard(MultiEnterpriseDisputeResolution dispute) {
        // Determine card border color based on status
        Color borderColor = switch (dispute.getResolutionStatus()) {
            case "PENDING" -> WARNING_COLOR;
            case "UNDER_REVIEW" -> PRIMARY_COLOR;
            case "RESOLVED" -> SUCCESS_COLOR;
            case "ESCALATED" -> DANGER_COLOR;
            default -> new Color(222, 226, 230);
        };
        
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        
        // Left - Icon and status
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(90, 0));
        
        JLabel iconLabel = new JLabel("⚖️");
        iconLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 40));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(iconLabel);
        
        leftPanel.add(Box.createVerticalStrut(8));
        
        // Status badge
        JLabel statusLabel = new JLabel(getStatusDisplayText(dispute.getResolutionStatus()));
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(borderColor);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(statusLabel);
        
        card.add(leftPanel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel itemLabel = new JLabel("Item: " + (dispute.getItemName() != null ? dispute.getItemName() : "Unknown"));
        itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(itemLabel);
        
        centerPanel.add(Box.createVerticalStrut(5));
        
        // Claimant count
        int claimantCount = dispute.getClaimants().size();
        JLabel claimantLabel = new JLabel("👥 " + claimantCount + " claimant" + (claimantCount != 1 ? "s" : "") + 
            " from " + dispute.getInvolvedEnterpriseNames().size() + " enterprise" + 
            (dispute.getInvolvedEnterpriseNames().size() != 1 ? "s" : ""));
        claimantLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        claimantLabel.setForeground(new Color(108, 117, 125));
        claimantLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(claimantLabel);
        
        centerPanel.add(Box.createVerticalStrut(5));
        
        // Panel voting progress
        int votesReceived = dispute.getPanelVotesReceived();
        int votesRequired = dispute.getPanelVotesRequired();
        JLabel votingLabel = new JLabel("📊 Panel Votes: " + votesReceived + "/" + votesRequired);
        votingLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        votingLabel.setForeground(votesReceived >= votesRequired ? SUCCESS_COLOR : PRIMARY_COLOR);
        votingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(votingLabel);
        
        // Progress bar for votes
        JProgressBar voteProgress = new JProgressBar(0, votesRequired);
        voteProgress.setValue(votesReceived);
        voteProgress.setStringPainted(true);
        voteProgress.setString(votesReceived + "/" + votesRequired + " votes");
        voteProgress.setMaximumSize(new Dimension(200, 20));
        voteProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(voteProgress);
        
        // Show resolution if resolved
        if ("RESOLVED".equals(dispute.getResolutionStatus()) && dispute.getWinningClaimantName() != null) {
            centerPanel.add(Box.createVerticalStrut(8));
            boolean isWinner = isCurrentUserWinner(dispute);
            JLabel resolutionLabel = new JLabel(isWinner ? "✅ Resolved in YOUR favor!" : 
                "Resolution: Awarded to " + dispute.getWinningClaimantName());
            resolutionLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
            resolutionLabel.setForeground(isWinner ? SUCCESS_COLOR : new Color(108, 117, 125));
            resolutionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(resolutionLabel);
        }
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Actions
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(140, 0));
        
        JButton viewBtn = new JButton("View Details");
        viewBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        viewBtn.setBackground(PRIMARY_COLOR);
        viewBtn.setForeground(Color.BLACK);
        viewBtn.setFocusPainted(false);
        viewBtn.setBorderPainted(false);
        viewBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewBtn.addActionListener(e -> showDisputeDetailsDialog(dispute));
        rightPanel.add(viewBtn);
        
        // Add evidence button (only if dispute is not resolved)
        if (!"RESOLVED".equals(dispute.getResolutionStatus())) {
            rightPanel.add(Box.createVerticalStrut(10));
            
            JButton addEvidenceBtn = new JButton("📎 Add Evidence");
            addEvidenceBtn.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
            addEvidenceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            addEvidenceBtn.addActionListener(e -> showAddEvidenceDialog(dispute));
            rightPanel.add(addEvidenceBtn);
        }
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private String getStatusDisplayText(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "PENDING" -> "Pending";
            case "UNDER_REVIEW" -> "Under Review";
            case "RESOLVED" -> "Resolved";
            case "ESCALATED" -> "Escalated";
            default -> status;
        };
    }
    
    private boolean isCurrentUserWinner(MultiEnterpriseDisputeResolution dispute) {
        String winnerId = dispute.getWinningClaimantId();
        if (winnerId == null) return false;
        return winnerId.equalsIgnoreCase(currentUser.getEmail());
    }
    
    private void showDisputeDetailsDialog(MultiEnterpriseDisputeResolution dispute) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Dispute Details - " + dispute.getItemName(),
            true
        );
        dialog.setSize(700, 650);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        JLabel titleLabel = new JLabel("⚖️ Dispute Resolution Case");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);
        
        JLabel itemTitleLabel = new JLabel(dispute.getItemName() != null ? dispute.getItemName() : "Unknown Item");
        itemTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemTitleLabel.setForeground(new Color(200, 220, 240));
        itemTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(itemTitleLabel);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content with tabs
        JTabbedPane detailsTabs = new JTabbedPane();
        detailsTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Tab 1: Overview
        detailsTabs.addTab("Overview", createDisputeOverviewPanel(dispute));
        
        // Tab 2: Claimants
        detailsTabs.addTab("Claimants (" + dispute.getClaimants().size() + ")", createClaimantsPanel(dispute));
        
        // Tab 3: Evidence
        detailsTabs.addTab("Evidence (" + dispute.getEvidenceItems().size() + ")", createEvidencePanel(dispute));
        
        // Tab 4: Panel Votes
        detailsTabs.addTab("Panel Votes", createPanelVotesPanel(dispute));
        
        mainPanel.add(detailsTabs, BorderLayout.CENTER);
        
        // Footer buttons
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footerPanel.setBackground(new Color(248, 249, 250));
        
        if (!"RESOLVED".equals(dispute.getResolutionStatus())) {
            JButton addEvidenceBtn = new JButton("📎 Submit Evidence");
            addEvidenceBtn.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
            addEvidenceBtn.setBackground(SUCCESS_COLOR);
            addEvidenceBtn.setForeground(Color.BLACK);
            addEvidenceBtn.addActionListener(e -> {
                dialog.dispose();
                showAddEvidenceDialog(dispute);
            });
            footerPanel.add(addEvidenceBtn);
        }
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        footerPanel.add(closeBtn);
        
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private JPanel createDisputeOverviewPanel(MultiEnterpriseDisputeResolution dispute) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Status section
        JPanel statusSection = createDetailSection("Status");
        addDetailRow(statusSection, "Resolution Status", getStatusDisplayText(dispute.getResolutionStatus()));
        addDetailRow(statusSection, "Panel Votes", dispute.getPanelVotesReceived() + "/" + dispute.getPanelVotesRequired());
        if (dispute.isPoliceInvolved()) {
            addDetailRow(statusSection, "Police Involved", "Yes - " + (dispute.getPoliceOfficerName() != null ? dispute.getPoliceOfficerName() : "Pending assignment"));
        }
        panel.add(statusSection);
        panel.add(Box.createVerticalStrut(15));
        
        // Item section
        JPanel itemSection = createDetailSection("Disputed Item");
        addDetailRow(itemSection, "Item Name", dispute.getItemName());
        addDetailRow(itemSection, "Description", dispute.getItemDescription());
        addDetailRow(itemSection, "Category", dispute.getItemCategory());
        addDetailRow(itemSection, "Estimated Value", String.format("$%.2f", dispute.getEstimatedValue()));
        addDetailRow(itemSection, "Current Location", dispute.getItemCurrentLocation());
        addDetailRow(itemSection, "Held By", dispute.getHoldingEnterpriseName());
        panel.add(itemSection);
        panel.add(Box.createVerticalStrut(15));
        
        // Dispute details section
        JPanel disputeSection = createDetailSection("Dispute Information");
        addDetailRow(disputeSection, "Dispute Type", dispute.getDisputeType());
        addDetailRow(disputeSection, "Reason", dispute.getDisputeReason());
        addDetailRow(disputeSection, "Initiated By", dispute.getDisputeInitiatedByName());
        addDetailRow(disputeSection, "Enterprises Involved", String.join(", ", dispute.getInvolvedEnterpriseNames()));
        panel.add(disputeSection);
        
        // Resolution section (if resolved)
        if ("RESOLVED".equals(dispute.getResolutionStatus())) {
            panel.add(Box.createVerticalStrut(15));
            JPanel resolutionSection = createDetailSection("Resolution");
            addDetailRow(resolutionSection, "Decision", dispute.getResolutionDecision());
            addDetailRow(resolutionSection, "Awarded To", dispute.getWinningClaimantName());
            addDetailRow(resolutionSection, "Reason", dispute.getResolutionReason());
            if (dispute.getResolutionNotes() != null) {
                addDetailRow(resolutionSection, "Notes", dispute.getResolutionNotes());
            }
            panel.add(resolutionSection);
        }
        
        panel.add(Box.createVerticalGlue());
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    private JPanel createClaimantsPanel(MultiEnterpriseDisputeResolution dispute) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        for (Claimant claimant : dispute.getClaimants()) {
            boolean isCurrentUser = claimant.claimantId != null && 
                claimant.claimantId.equalsIgnoreCase(currentUser.getEmail());
            boolean isWinner = claimant.claimantId != null && 
                claimant.claimantId.equals(dispute.getWinningClaimantId());
            
            JPanel claimantCard = new JPanel(new BorderLayout(10, 0));
            claimantCard.setBackground(isCurrentUser ? new Color(232, 245, 233) : Color.WHITE);
            claimantCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isWinner ? SUCCESS_COLOR : new Color(222, 226, 230), isWinner ? 2 : 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
            claimantCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            
            // Left - icon
            JLabel iconLabel = new JLabel(isCurrentUser ? "👤" : "👥");
            iconLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 30));
            iconLabel.setPreferredSize(new Dimension(50, 0));
            claimantCard.add(iconLabel, BorderLayout.WEST);
            
            // Center - details
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
            detailsPanel.setOpaque(false);
            
            JLabel nameLabel = new JLabel(claimant.claimantName + (isCurrentUser ? " (You)" : "") + 
                (isWinner ? " ✅ WINNER" : ""));
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            if (isWinner) nameLabel.setForeground(SUCCESS_COLOR);
            detailsPanel.add(nameLabel);
            
            JLabel enterpriseLabel = new JLabel(claimant.enterpriseName + 
                (claimant.organizationName != null ? " - " + claimant.organizationName : ""));
            enterpriseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            enterpriseLabel.setForeground(new Color(108, 117, 125));
            detailsPanel.add(enterpriseLabel);
            
            if (claimant.claimDescription != null && !claimant.claimDescription.isEmpty()) {
                JLabel claimLabel = new JLabel("<html><b>Claim:</b> " + 
                    truncateText(claimant.claimDescription, 100) + "</html>");
                claimLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                detailsPanel.add(claimLabel);
            }
            
            JLabel trustLabel = new JLabel(String.format("Trust Score: %.0f%%", claimant.trustScore));
            trustLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            trustLabel.setForeground(claimant.trustScore >= 70 ? SUCCESS_COLOR : 
                (claimant.trustScore >= 40 ? WARNING_COLOR : DANGER_COLOR));
            detailsPanel.add(trustLabel);
            
            claimantCard.add(detailsPanel, BorderLayout.CENTER);
            
            // Right - status
            JLabel statusLabel = new JLabel(claimant.claimStatus != null ? claimant.claimStatus : "SUBMITTED");
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
            statusLabel.setPreferredSize(new Dimension(80, 0));
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            claimantCard.add(statusLabel, BorderLayout.EAST);
            
            panel.add(claimantCard);
            panel.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    private JPanel createEvidencePanel(MultiEnterpriseDisputeResolution dispute) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        if (dispute.getEvidenceItems().isEmpty()) {
            JLabel emptyLabel = new JLabel("No evidence submitted yet");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createVerticalStrut(50));
            panel.add(emptyLabel);
        } else {
            for (EvidenceItem evidence : dispute.getEvidenceItems()) {
                boolean isMyEvidence = evidence.submittedById != null && 
                    evidence.submittedById.equalsIgnoreCase(currentUser.getEmail());
                
                JPanel evidenceCard = new JPanel(new BorderLayout(10, 0));
                evidenceCard.setBackground(isMyEvidence ? new Color(232, 245, 253) : Color.WHITE);
                evidenceCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(222, 226, 230)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                evidenceCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                
                // Icon based on evidence type
                String icon = switch (evidence.evidenceType != null ? evidence.evidenceType : "") {
                    case "RECEIPT" -> "🧾";
                    case "PHOTO" -> "📷";
                    case "SERIAL_NUMBER" -> "🔢";
                    case "WITNESS" -> "👁️";
                    default -> "📎";
                };
                JLabel iconLabel = new JLabel(icon);
                iconLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 24));
                iconLabel.setPreferredSize(new Dimension(40, 0));
                evidenceCard.add(iconLabel, BorderLayout.WEST);
                
                JPanel detailsPanel = new JPanel();
                detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
                detailsPanel.setOpaque(false);
                
                JLabel typeLabel = new JLabel(evidence.evidenceType + (isMyEvidence ? " (Your evidence)" : ""));
                typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                detailsPanel.add(typeLabel);
                
                JLabel descLabel = new JLabel(truncateText(evidence.description, 80));
                descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                descLabel.setForeground(new Color(108, 117, 125));
                detailsPanel.add(descLabel);
                
                JLabel submitterLabel = new JLabel("Submitted by: " + evidence.submittedByName);
                submitterLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                submitterLabel.setForeground(new Color(134, 142, 150));
                detailsPanel.add(submitterLabel);
                
                evidenceCard.add(detailsPanel, BorderLayout.CENTER);
                
                // Verification status
                JPanel statusPanel = new JPanel();
                statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
                statusPanel.setOpaque(false);
                statusPanel.setPreferredSize(new Dimension(80, 0));
                
                String verifyIcon = evidence.verified ? 
                    ("VALID".equals(evidence.verificationResult) ? "✅" : "❌") : "⏳";
                JLabel verifyLabel = new JLabel(verifyIcon);
                verifyLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 18));
                verifyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                statusPanel.add(verifyLabel);
                
                JLabel verifyTextLabel = new JLabel(evidence.verified ? evidence.verificationResult : "Pending");
                verifyTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                verifyTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                statusPanel.add(verifyTextLabel);
                
                evidenceCard.add(statusPanel, BorderLayout.EAST);
                
                panel.add(evidenceCard);
                panel.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    private JPanel createPanelVotesPanel(MultiEnterpriseDisputeResolution dispute) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Summary header
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryPanel.setOpaque(false);
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel summaryLabel = new JLabel(String.format("📊 Voting Progress: %d of %d votes received",
            dispute.getPanelVotesReceived(), dispute.getPanelVotesRequired()));
        summaryLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        summaryPanel.add(summaryLabel);
        panel.add(summaryPanel);
        panel.add(Box.createVerticalStrut(15));
        
        if (dispute.getVerificationPanel().isEmpty()) {
            JLabel emptyLabel = new JLabel("No panel members assigned yet");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(emptyLabel);
        } else {
            for (PanelMember member : dispute.getVerificationPanel()) {
                JPanel memberCard = new JPanel(new BorderLayout(10, 0));
                memberCard.setBackground(member.hasVoted ? new Color(232, 245, 233) : Color.WHITE);
                memberCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(member.hasVoted ? SUCCESS_COLOR : new Color(222, 226, 230)),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
                ));
                memberCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                
                // Vote icon
                JLabel voteIcon = new JLabel(member.hasVoted ? "✅" : "⏳");
                voteIcon.setFont(UIConstants.getEmojiFont(Font.PLAIN, 24));
                voteIcon.setPreferredSize(new Dimension(40, 0));
                memberCard.add(voteIcon, BorderLayout.WEST);
                
                JPanel detailsPanel = new JPanel();
                detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
                detailsPanel.setOpaque(false);
                
                JLabel nameLabel = new JLabel(member.memberName);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                detailsPanel.add(nameLabel);
                
                JLabel roleLabel = new JLabel(member.role + " - " + member.enterpriseName);
                roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                roleLabel.setForeground(new Color(108, 117, 125));
                detailsPanel.add(roleLabel);
                
                if (member.hasVoted && member.voteReason != null) {
                    JLabel reasonLabel = new JLabel("<html><i>\"" + truncateText(member.voteReason, 60) + "\"</i></html>");
                    reasonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    reasonLabel.setForeground(new Color(134, 142, 150));
                    detailsPanel.add(reasonLabel);
                }
                
                memberCard.add(detailsPanel, BorderLayout.CENTER);
                
                // Vote status label
                JLabel statusLabel = new JLabel(member.hasVoted ? "VOTED" : "PENDING");
                statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
                statusLabel.setForeground(member.hasVoted ? SUCCESS_COLOR : WARNING_COLOR);
                statusLabel.setPreferredSize(new Dimension(60, 0));
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
                memberCard.add(statusLabel, BorderLayout.EAST);
                
                panel.add(memberCard);
                panel.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    private JPanel createDetailSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)));
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(8));
        
        return section;
    }
    
    private void addDetailRow(JPanel section, String label, String value) {
        if (value == null || value.isEmpty()) return;
        
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel labelComp = new JLabel(label + ":");
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelComp.setForeground(new Color(73, 80, 87));
        labelComp.setPreferredSize(new Dimension(140, 20));
        row.add(labelComp);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(valueComp);
        
        section.add(row);
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private void showAddEvidenceDialog(MultiEnterpriseDisputeResolution dispute) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Submit Evidence for Dispute",
            true
        );
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("📎 Submit Supporting Evidence");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        JLabel itemLabel = new JLabel("For dispute: " + dispute.getItemName());
        itemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemLabel.setForeground(new Color(108, 117, 125));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(itemLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Evidence type
        contentPanel.add(createFormLabel("Evidence Type *"));
        JComboBox<String> evidenceTypeCombo = new JComboBox<>(new String[]{
            "RECEIPT", "PHOTO", "SERIAL_NUMBER", "WITNESS", "OTHER"
        });
        evidenceTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        evidenceTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(evidenceTypeCombo);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Description
        contentPanel.add(createFormLabel("Description *"));
        JTextArea descriptionArea = new JTextArea(4, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(descScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Document path (optional)
        contentPanel.add(createFormLabel("Document/Photo Reference (optional)"));
        JTextField documentPathField = new JTextField();
        documentPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        documentPathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(documentPathField);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Help text
        JLabel helpLabel = new JLabel("<html><i>Provide any evidence that supports your claim.<br>" +
            "Examples: purchase receipt details, serial number, witness contact info, etc.</i></html>");
        helpLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        helpLabel.setForeground(new Color(108, 117, 125));
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(helpLabel);
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        JButton submitBtn = new JButton("Submit Evidence");
        submitBtn.setBackground(SUCCESS_COLOR);
        submitBtn.setForeground(Color.BLACK);
        submitBtn.addActionListener(e -> {
            if (descriptionArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please provide a description.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            submitEvidence(dispute, (String) evidenceTypeCombo.getSelectedItem(),
                descriptionArea.getText().trim(), documentPathField.getText().trim());
            dialog.dispose();
        });
        buttonPanel.add(submitBtn);
        
        contentPanel.add(buttonPanel);
        
        dialog.add(new JScrollPane(contentPanel));
        dialog.setVisible(true);
    }
    
    private void submitEvidence(MultiEnterpriseDisputeResolution dispute, String evidenceType,
                                String description, String documentPath) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // Add evidence to the dispute
                    dispute.addEvidence(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        evidenceType,
                        description,
                        documentPath.isEmpty() ? null : documentPath
                    );
                    
                    // Find the claimant entry for this user and add evidence ID
                    for (Claimant c : dispute.getClaimants()) {
                        if (c.claimantId != null && c.claimantId.equalsIgnoreCase(currentUser.getEmail())) {
                            if (c.evidenceIds == null) c.evidenceIds = new ArrayList<>();
                            // Get the last added evidence ID
                            List<EvidenceItem> evidenceItems = dispute.getEvidenceItems();
                            if (!evidenceItems.isEmpty()) {
                                c.evidenceIds.add(evidenceItems.get(evidenceItems.size() - 1).evidenceId);
                            }
                            break;
                        }
                    }
                    
                    // Save updated dispute
                    MongoWorkRequestDAO dao = new MongoWorkRequestDAO();
                    String result = dao.save(dispute);
                    return result != null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(PublicTravelerPanel.this,
                            "Evidence submitted successfully!\n\n" +
                            "Your evidence will be reviewed by the verification panel.",
                            "Evidence Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        refreshDisputes();
                    } else {
                        showError("Failed to submit evidence. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        };
        worker.execute();
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
            BorderFactory.createEmptyBorder(10, 0, 8, 0)
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
    
    // ==================== DATA LOADING ====================
    
    private void loadDashboardData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                List<Item> userItems = itemDAO.findByUser(currentUser.getEmail());
                int openReports = (int) userItems.stream()
                    .filter(i -> i.getType() == Item.ItemType.LOST)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .count();
                
                List<WorkRequest> claims = workRequestService.getRequestsForUser(
                    currentUser.getEmail(), currentUser.getRole().name());
                int pendingClaims = (int) claims.stream()
                    .filter(r -> r.getStatus() == WorkRequest.RequestStatus.PENDING ||
                                r.getStatus() == WorkRequest.RequestStatus.IN_PROGRESS)
                    .count();
                
                return new int[]{openReports, pendingClaims};
            }
            
            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    openReportsLabel.setText(stats[0] + " Open Reports");
                    pendingClaimsLabel.setText(stats[1] + " Pending Claims");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void refreshMyReports() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.findByUser(currentUser.getEmail());
            }
            
            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    displayMyReports(filterReports(items));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private List<Item> filterReports(List<Item> items) {
        String filter = (String) reportsFilterCombo.getSelectedItem();
        if (filter == null || filter.equals("All Reports")) return items;
        
        return items.stream().filter(item -> switch (filter) {
            case "Open" -> item.getStatus() == Item.ItemStatus.OPEN;
            case "In Progress" -> item.getStatus() == Item.ItemStatus.PENDING_CLAIM;
            case "Resolved" -> item.getStatus() == Item.ItemStatus.CLAIMED;
            default -> true;
        }).collect(Collectors.toList());
    }
    
    private void displayMyReports(List<Item> items) {
        myReportsListPanel.removeAll();
        
        if (items.isEmpty()) {
            JLabel emptyLabel = new JLabel("No reports found. Use 'Report Lost Item' to submit a new report.");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            myReportsListPanel.add(Box.createVerticalGlue());
            myReportsListPanel.add(emptyLabel);
            myReportsListPanel.add(Box.createVerticalGlue());
        } else {
            for (Item item : items) {
                myReportsListPanel.add(createReportCard(item));
                myReportsListPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        myReportsListPanel.revalidate();
        myReportsListPanel.repaint();
    }
    
    private JPanel createReportCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Left - Icon and type
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(80, 0));
        
        JLabel catLabel = new JLabel(item.getCategory().getEmoji());
        catLabel.setFont(EMOJI_FONT_MEDIUM);
        catLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(catLabel);
        
        JLabel typeLabel = new JLabel(item.getType().getLabel());
        typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        typeLabel.setForeground(item.getType() == Item.ItemType.LOST ? DANGER_COLOR : SUCCESS_COLOR);
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(typeLabel);
        
        card.add(leftPanel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        centerPanel.add(titleLabel);
        
        JLabel categoryLabel = new JLabel(item.getCategory().getDisplayName());
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(categoryLabel);
        
        JLabel dateLabel = new JLabel("Reported: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        // Tracking number
        if (item.getMongoId() != null) {
            String trackingNum = "TRK-" + item.getMongoId().substring(Math.max(0, item.getMongoId().length() - 8)).toUpperCase();
            JLabel trackingLabel = new JLabel("Tracking: " + trackingNum);
            trackingLabel.setFont(new Font("Segoe UI Mono", Font.PLAIN, 11));
            trackingLabel.setForeground(PRIMARY_COLOR);
            centerPanel.add(trackingLabel);
        }
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Status
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(100, 0));
        
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(statusLabel);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void lookupByTrackingNumber() {
        String trackingNum = trackingNumberField.getText().trim();
        if (trackingNum.isEmpty()) {
            showError("Please enter a tracking number.");
            return;
        }
        
        // Extract the ID part
        String itemId = trackingNum.replace("TRK-", "").toLowerCase();
        
        // Search for item
        Optional<Item> found = itemDAO.findById(itemId);
        if (found.isPresent()) {
            Item item = found.get();
            myReportsListPanel.removeAll();
            myReportsListPanel.add(createReportCard(item));
            myReportsListPanel.revalidate();
            myReportsListPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this,
                "No report found with tracking number: " + trackingNum,
                "Not Found",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // ==================== ACTIONS ====================
    
    private void submitLostItemReport() {
        // Validate
        if (reportTitleField.getText().trim().isEmpty()) {
            showError("Please enter an item name/description.");
            reportTitleField.requestFocus();
            return;
        }
        if (reportDescriptionArea.getText().trim().isEmpty()) {
            showError("Please provide a detailed description.");
            reportDescriptionArea.requestFocus();
            return;
        }
        if (reportContactEmailField.getText().trim().isEmpty()) {
            showError("Please provide your email address.");
            reportContactEmailField.requestFocus();
            return;
        }
        if (reportContactPhoneField.getText().trim().isEmpty()) {
            showError("Please provide your phone number.");
            reportContactPhoneField.requestFocus();
            return;
        }
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    Item item = new Item(
                        reportTitleField.getText().trim(),
                        reportDescriptionArea.getText().trim(),
                        (Item.ItemCategory) reportCategoryCombo.getSelectedItem(),
                        Item.ItemType.LOST,
                        null,
                        currentUser
                    );
                    
                    item.setStatus(Item.ItemStatus.OPEN);
                    
                    if (!reportColorField.getText().trim().isEmpty()) {
                        item.setPrimaryColor(reportColorField.getText().trim());
                    }
                    if (!reportBrandField.getText().trim().isEmpty()) {
                        item.setBrand(reportBrandField.getText().trim());
                    }
                    
                    // Add location context to keywords
                    List<String> keywords = item.getKeywords();
                    if (keywords == null) keywords = new ArrayList<>();
                    keywords.add((String) reportLocationTypeCombo.getSelectedItem());
                    keywords.add((String) reportTransitLineCombo.getSelectedItem());
                    if (!reportStationField.getText().trim().isEmpty()) {
                        keywords.add(reportStationField.getText().trim());
                    }
                    item.setKeywords(keywords);
                    
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
                    if (itemId != null) {
                        String trackingNum = "TRK-" + itemId.substring(Math.max(0, itemId.length() - 8)).toUpperCase();
                        JOptionPane.showMessageDialog(PublicTravelerPanel.this,
                            "Your report has been submitted!\n\n" +
                            "Tracking Number: " + trackingNum + "\n\n" +
                            "Save this number to track your report status.\n" +
                            "We will contact you if we find a match.",
                            "Report Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        clearReportForm();
                        loadDashboardData();
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
        reportTitleField.setText("");
        reportDescriptionArea.setText("");
        reportCategoryCombo.setSelectedIndex(0);
        reportLocationTypeCombo.setSelectedIndex(0);
        reportStationField.setText("");
        reportDateField.setText(DATE_FORMAT.format(new Date()));
        reportTimeField.setText(TIME_FORMAT.format(new Date()));
        reportColorField.setText("");
        reportBrandField.setText("");
    }
    
    private void performSearch() {
        String query = searchQueryField.getText().trim();
        
        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                List<Item> allFoundItems = itemDAO.findAll().stream()
                    .filter(i -> i.getType() == Item.ItemType.FOUND)
                    .filter(i -> i.getStatus() == Item.ItemStatus.OPEN)
                    .collect(Collectors.toList());
                
                // Filter by category
                Item.ItemCategory selectedCat = (Item.ItemCategory) searchCategoryCombo.getSelectedItem();
                if (selectedCat != null) {
                    allFoundItems = allFoundItems.stream()
                        .filter(i -> i.getCategory() == selectedCat)
                        .collect(Collectors.toList());
                }
                
                // Filter by query
                if (!query.isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    allFoundItems = allFoundItems.stream()
                        .filter(i -> i.getTitle().toLowerCase().contains(lowerQuery) ||
                                    i.getDescription().toLowerCase().contains(lowerQuery) ||
                                    (i.getKeywords() != null && i.getKeywords().stream()
                                        .anyMatch(k -> k.toLowerCase().contains(lowerQuery))))
                        .collect(Collectors.toList());
                }
                
                return allFoundItems;
            }
            
            @Override
            protected void done() {
                try {
                    currentSearchResults = get();
                    displaySearchResults(currentSearchResults);
                } catch (Exception e) {
                    showError("Search failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void displaySearchResults(List<Item> items) {
        searchResultsPanel.removeAll();
        
        if (items.isEmpty()) {
            JLabel emptyLabel = new JLabel("No found items match your search criteria.");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(108, 117, 125));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            searchResultsPanel.add(emptyLabel);
        } else {
            JLabel countLabel = new JLabel(items.size() + " item(s) found:");
            countLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            searchResultsPanel.add(countLabel);
            searchResultsPanel.add(Box.createVerticalStrut(15));
            
            for (Item item : items) {
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
            BorderFactory.createLineBorder(SUCCESS_COLOR, 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Left - Icon
        JLabel catLabel = new JLabel(item.getCategory().getEmoji());
        catLabel.setFont(EMOJI_FONT_SMALL);
        catLabel.setPreferredSize(new Dimension(50, 0));
        card.add(catLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("FOUND: " + item.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(SUCCESS_COLOR);
        centerPanel.add(titleLabel);
        
        JLabel categoryLabel = new JLabel(item.getCategory().getDisplayName());
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(108, 117, 125));
        centerPanel.add(categoryLabel);
        
        JLabel dateLabel = new JLabel("Found: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        centerPanel.add(dateLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        // Right - Claim button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(140, 0));
        
        JButton claimBtn = new JButton("Claim This");
        claimBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        claimBtn.setBackground(PRIMARY_COLOR);
        claimBtn.setForeground(Color.BLACK);
        claimBtn.setFocusPainted(false);
        claimBtn.setBorderPainted(false);
        claimBtn.addActionListener(e -> showClaimWithIDDialog(item));
        rightPanel.add(claimBtn);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void showClaimWithIDDialog(Item item) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Claim Item with ID Verification",
            true
        );
        dialog.setSize(550, 650);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("Claim Found Item");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        JLabel itemLabel = new JLabel("Item: " + item.getTitle());
        itemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemLabel.setForeground(new Color(108, 117, 125));
        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(itemLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // ID Verification section
        JPanel idSection = new JPanel();
        idSection.setLayout(new BoxLayout(idSection, BoxLayout.Y_AXIS));
        idSection.setOpaque(false);
        TitledBorder idBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WARNING_COLOR),
            "Government ID Required"
        );
        idBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        idSection.setBorder(idBorder);
        idSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        idSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        JLabel idInfoLabel = new JLabel("<html>For security, you must present a valid government-issued ID<br>" +
            "(Driver's License, Passport, State ID) when picking up this item.</html>");
        idInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        idInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        idSection.add(idInfoLabel);
        idSection.add(Box.createVerticalStrut(10));
        
        JLabel idTypeLabel = new JLabel("ID Type You Will Present:");
        idTypeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        idTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        idSection.add(idTypeLabel);
        
        JComboBox<String> idTypeCombo = new JComboBox<>(new String[]{
            "Driver's License", "Passport", "State ID", "Military ID"
        });
        idTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        idTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        idSection.add(idTypeCombo);
        
        contentPanel.add(idSection);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Proof of ownership
        JLabel proofLabel = new JLabel("Why do you believe this is your item? *");
        proofLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        proofLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(proofLabel);
        
        JTextArea claimDetailsArea = new JTextArea(3, 30);
        claimDetailsArea.setLineWrap(true);
        claimDetailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(claimDetailsArea);
        detailsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        detailsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(detailsScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Unique identifiers
        JLabel featuresLabel = new JLabel("Unique identifying features only the owner would know *");
        featuresLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        featuresLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresLabel);
        
        JLabel featuresHintLabel = new JLabel("(e.g., scratches, stickers, contents, lock code)");
        featuresHintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        featuresHintLabel.setForeground(new Color(108, 117, 125));
        featuresHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresHintLabel);
        
        JTextArea featuresArea = new JTextArea(3, 30);
        featuresArea.setLineWrap(true);
        featuresArea.setWrapStyleWord(true);
        JScrollPane featuresScroll = new JScrollPane(featuresArea);
        featuresScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        featuresScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresScroll);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Device unlock (for electronics)
        if (item.getCategory() == Item.ItemCategory.ELECTRONICS) {
            JLabel unlockLabel = new JLabel("Device Unlock Code (if applicable)");
            unlockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            unlockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(unlockLabel);
            
            JTextField unlockField = new JTextField();
            unlockField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            unlockField.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(unlockField);
            contentPanel.add(Box.createVerticalStrut(15));
        }
        
        // Preferred pickup location
        JLabel pickupLabel = new JLabel("Preferred Pickup Location *");
        pickupLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pickupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(pickupLabel);
        
        JComboBox<String> pickupCombo = new JComboBox<>(new String[]{
            "MBTA Downtown Crossing Lost & Found",
            "MBTA Park Street Lost & Found",
            "MBTA South Station Lost & Found",
            "Logan Airport Terminal B Lost & Found",
            "Logan Airport Terminal E Lost & Found"
        });
        pickupCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        pickupCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(pickupCombo);
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
            
            submitClaimRequest(item, claimDetailsArea.getText().trim(),
                featuresArea.getText().trim(), (String) idTypeCombo.getSelectedItem(),
                (String) pickupCombo.getSelectedItem());
            dialog.dispose();
        });
        buttonPanel.add(submitBtn);
        
        contentPanel.add(buttonPanel);
        
        dialog.add(new JScrollPane(contentPanel));
        dialog.setVisible(true);
    }
    
    private void submitClaimRequest(Item item, String claimDetails, String features,
                                     String idType, String pickupLocation) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    ItemClaimRequest claim = new ItemClaimRequest(
                        currentUser.getEmail(),
                        currentUser.getFullName(),
                        item.getMongoId(),
                        item.getTitle(),
                        0.0
                    );
                    
                    claim.setClaimDetails(claimDetails);
                    claim.setIdentifyingFeatures(features);
                    claim.setProofDescription("ID Type: " + idType + " | Pickup: " + pickupLocation);
                    claim.setItemCategory(item.getCategory().name());
                    claim.setTargetOrganizationId(item.getOrganizationId());
                    claim.setTargetEnterpriseId(item.getEnterpriseId());
                    
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
                        JOptionPane.showMessageDialog(PublicTravelerPanel.this,
                            "Your claim has been submitted!\n\n" +
                            "You will be contacted at " + currentUser.getEmail() + "\n" +
                            "once your claim is reviewed.\n\n" +
                            "Remember to bring your " + "government ID" + " when picking up the item.",
                            "Claim Submitted",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadDashboardData();
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
    
    private void submitEmergencyRequest() {
        // Validate
        if (emergencyDescriptionArea.getText().trim().isEmpty()) {
            showError("Please describe your emergency.");
            emergencyDescriptionArea.requestFocus();
            return;
        }
        if (emergencyContactField.getText().trim().isEmpty()) {
            showError("Please provide a phone number.");
            emergencyContactField.requestFocus();
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Submit emergency coordination request?\n\n" +
            "This will alert MBTA and Airport staff for immediate action.",
            "Confirm Emergency Request",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        // In a real system, this would create a high-priority work request
        String refNumber = "EMG-" + System.currentTimeMillis() % 100000;
        
        JOptionPane.showMessageDialog(this,
            "🚨 Emergency Request Submitted!\n\n" +
            "Reference Number: " + refNumber + "\n\n" +
            "Our team has been alerted and will contact you at:\n" +
            emergencyContactField.getText() + "\n\n" +
            "For immediate assistance, call:\n" +
            "• MBTA Lost & Found: 617-222-3200\n" +
            "• Logan Airport: 617-561-1714",
            "Emergency Request Submitted",
            JOptionPane.INFORMATION_MESSAGE);
        
        // Clear form
        emergencyDescriptionArea.setText("");
        emergencyFlightField.setText("");
        emergencyGateField.setText("");
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class CategoryComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(EMOJI_FONT);  // Use emoji-capable font
            
            if (value == null) {
                setText("All Categories");
            } else if (value instanceof Item.ItemCategory category) {
                setText(category.getEmoji() + " " + category.getDisplayName());
            }
            
            return this;
        }
    }
}
