package com.campus.lostfound.ui.components;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.models.Organization;
import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Cross-enterprise search panel for finding items across all organizations.
 * Features: - Search across NEU, BU, MBTA, Airport, Police - Filter by
 * enterprise, category, item type, date range - Text search with keyword
 * matching - Visual enterprise badges showing where items are held - Card-based
 * results display
 *
 * @author Developer 2 - UI Components
 */
public class EnterpriseItemSearchPanel extends JPanel {

    // Data
    private User currentUser;
    private List<Item> searchResults;
    private List<Enterprise> enterprises;
    private Map<String, Enterprise> enterpriseMap;
    private Map<String, Organization> organizationMap;

    // DAOs
    private MongoItemDAO itemDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;

    // UI Components - Search Controls
    private JTextField searchField;
    private JButton searchButton;
    private JComboBox<EnterpriseOption> enterpriseCombo;
    private JComboBox<OrganizationOption> organizationCombo;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> typeCombo;
    private JComboBox<String> statusCombo;
    private JCheckBox crossEnterpriseCheckbox;

    // UI Components - Results
    private JPanel resultsPanel;
    private JScrollPane scrollPane;
    private JLabel resultsCountLabel;
    private JProgressBar loadingBar;

    // Selected item
    private Item selectedItem;
    private JPanel selectedCard;

    // Callbacks
    private Consumer<Item> onItemSelected;
    private Consumer<Item> onItemDoubleClicked;

    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Color CARD_SELECTED_BG = new Color(232, 244, 253);
    private static final Color CARD_HOVER_BG = new Color(248, 249, 250);

    // Enterprise colors
    private static final Map<Enterprise.EnterpriseType, Color> ENTERPRISE_COLORS = Map.of(
            Enterprise.EnterpriseType.HIGHER_EDUCATION, new Color(102, 16, 242), // Purple
            Enterprise.EnterpriseType.PUBLIC_TRANSIT, new Color(13, 110, 253), // Blue
            Enterprise.EnterpriseType.AIRPORT, new Color(111, 66, 193), // Indigo
            Enterprise.EnterpriseType.LAW_ENFORCEMENT, new Color(220, 53, 69) // Red
    );

    /**
     * Create a new EnterpriseItemSearchPanel.
     *
     * @param currentUser The logged-in user
     */
    public EnterpriseItemSearchPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.searchResults = new ArrayList<>();
        this.enterpriseMap = new HashMap<>();
        this.organizationMap = new HashMap<>();

        loadEnterprises();
        initComponents();
    }

    private void loadEnterprises() {
        enterprises = enterpriseDAO.findAll();
        for (Enterprise e : enterprises) {
            enterpriseMap.put(e.getEnterpriseId(), e);
        }

        List<Organization> orgs = organizationDAO.findAll();
        for (Organization o : orgs) {
            organizationMap.put(o.getOrganizationId(), o);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Search controls
        add(createSearchPanel(), BorderLayout.NORTH);

        // Results
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        add(createFooterPanel(), BorderLayout.SOUTH);

        // Initial state
        showEmptyState("Enter search terms or use filters to find items across all enterprises");
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(0, 0, 15, 0)
        ));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLabel = new JLabel("🌐 Cross-Enterprise Item Search");
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 18));
        titleRow.add(titleLabel, BorderLayout.WEST);

        crossEnterpriseCheckbox = new JCheckBox("Search all enterprises", true);
        crossEnterpriseCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        crossEnterpriseCheckbox.setOpaque(false);
        crossEnterpriseCheckbox.addActionListener(e -> {
            boolean searchAll = crossEnterpriseCheckbox.isSelected();
            enterpriseCombo.setEnabled(!searchAll);
            organizationCombo.setEnabled(!searchAll);
            if (searchAll) {
                enterpriseCombo.setSelectedIndex(0);
                updateOrganizationCombo();
            }
        });
        titleRow.add(crossEnterpriseCheckbox, BorderLayout.EAST);

        panel.add(titleRow);
        panel.add(Box.createVerticalStrut(15));

        // Search bar row
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.addActionListener(e -> performSearch());

        // Placeholder text
        searchField.setText("Search by title, description, keywords...");
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search by title, description, keywords...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search by title, description, keywords...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });

        searchRow.add(searchField, BorderLayout.CENTER);

        searchButton = new JButton("🔍 Search");
        searchButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        searchButton.setBackground(new Color(13, 110, 253));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setContentAreaFilled(true);
        searchButton.setOpaque(true);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.setPreferredSize(new Dimension(120, 0));
        searchButton.addActionListener(e -> performSearch());
        searchRow.add(searchButton, BorderLayout.EAST);

        panel.add(searchRow);
        panel.add(Box.createVerticalStrut(15));

        // Filters row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterRow.setOpaque(false);
        filterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Enterprise filter
        JLabel entLabel = new JLabel("Enterprise:");
        entLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(entLabel);

        enterpriseCombo = new JComboBox<>();
        enterpriseCombo.addItem(new EnterpriseOption(null, "All Enterprises"));
        for (Enterprise e : enterprises) {
            enterpriseCombo.addItem(new EnterpriseOption(e, getEnterpriseIcon(e.getType()) + " " + e.getName()));
        }
        enterpriseCombo.setPreferredSize(new Dimension(180, 28));
        enterpriseCombo.setEnabled(false); // Disabled when "Search all" is checked
        enterpriseCombo.addActionListener(e -> updateOrganizationCombo());
        filterRow.add(enterpriseCombo);

        filterRow.add(Box.createHorizontalStrut(10));

        // Organization filter
        JLabel orgLabel = new JLabel("Organization:");
        orgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(orgLabel);

        organizationCombo = new JComboBox<>();
        organizationCombo.addItem(new OrganizationOption(null, "All Organizations"));
        // Initially populate with all organizations
        for (Organization org : organizationMap.values()) {
            organizationCombo.addItem(new OrganizationOption(org, org.getName()));
        }
        organizationCombo.setPreferredSize(new Dimension(180, 28));
        organizationCombo.setEnabled(false); // Disabled when "Search all" is checked
        filterRow.add(organizationCombo);

        filterRow.add(Box.createHorizontalStrut(10));

        // Category filter
        JLabel catLabel = new JLabel("Category:");
        catLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(catLabel);

        categoryCombo = new JComboBox<>();
        categoryCombo.addItem("All Categories");
        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            categoryCombo.addItem(cat.getEmoji() + " " + cat.getDisplayName());
        }
        categoryCombo.setPreferredSize(new Dimension(180, 28));
        filterRow.add(categoryCombo);

        filterRow.add(Box.createHorizontalStrut(10));

        // Type filter
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(typeLabel);

        typeCombo = new JComboBox<>(new String[]{"All Types", "❌ Lost Items", "✅ Found Items"});
        typeCombo.setPreferredSize(new Dimension(130, 28));
        filterRow.add(typeCombo);

        filterRow.add(Box.createHorizontalStrut(10));

        // Status filter
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(statusLabel);

        statusCombo = new JComboBox<>(new String[]{"All Statuses", "Open", "Pending Claim", "Verified"});
        statusCombo.setPreferredSize(new Dimension(130, 28));
        filterRow.add(statusCombo);

        panel.add(filterRow);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Loading bar
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setPreferredSize(new Dimension(0, 3));
        panel.add(loadingBar, BorderLayout.NORTH);

        // Results count and legend
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        resultsCountLabel = new JLabel("0 items found");
        resultsCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultsCountLabel.setForeground(new Color(108, 117, 125));
        infoPanel.add(resultsCountLabel, BorderLayout.WEST);

        // Enterprise legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legendPanel.setOpaque(false);

        legendPanel.add(createLegendItem("🎓", ENTERPRISE_COLORS.get(Enterprise.EnterpriseType.HIGHER_EDUCATION), "University"));
        legendPanel.add(createLegendItem("🚇", ENTERPRISE_COLORS.get(Enterprise.EnterpriseType.PUBLIC_TRANSIT), "MBTA"));
        legendPanel.add(createLegendItem("✈️", ENTERPRISE_COLORS.get(Enterprise.EnterpriseType.AIRPORT), "Airport"));
        legendPanel.add(createLegendItem("🚔", ENTERPRISE_COLORS.get(Enterprise.EnterpriseType.LAW_ENFORCEMENT), "Police"));

        infoPanel.add(legendPanel, BorderLayout.EAST);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createLegendItem(String icon, Color color, String text) {
        JLabel label = new JLabel(icon + " " + text);
        // Use the same emoji-aware font helper as your button
        label.setFont(UIConstants.getEmojiFont(Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }

    // ==================== PUBLIC API ====================
    /**
     * Perform a search with current filters.
     */
    public void performSearch() {
        setLoading(true);

        SwingWorker<List<Item>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Item> doInBackground() {
                // Get search text
                String searchText = searchField.getText();
                if (searchText.equals("Search by title, description, keywords...")) {
                    searchText = "";
                }

                // Get all items
                List<Item> allItems = itemDAO.findAll();

                // Apply filters
                return filterItems(allItems, searchText);
            }

            @Override
            protected void done() {
                try {
                    searchResults = get();
                    displayResults(searchResults);
                } catch (Exception e) {
                    showError("Search failed: " + e.getMessage());
                    searchResults = new ArrayList<>();
                    displayResults(searchResults);
                } finally {
                    setLoading(false);
                }
            }
        };
        worker.execute();
    }

    /**
     * Search with specific parameters.
     */
    public void search(String query, String enterpriseId, Item.ItemCategory category) {
        searchField.setText(query);
        searchField.setForeground(Color.BLACK);

        if (enterpriseId != null) {
            crossEnterpriseCheckbox.setSelected(false);
            enterpriseCombo.setEnabled(true);
            // Find and select enterprise
            for (int i = 0; i < enterpriseCombo.getItemCount(); i++) {
                EnterpriseOption opt = enterpriseCombo.getItemAt(i);
                if (opt.enterprise != null && opt.enterprise.getEnterpriseId().equals(enterpriseId)) {
                    enterpriseCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (category != null) {
            categoryCombo.setSelectedItem(category.getEmoji() + " " + category.getDisplayName());
        }

        performSearch();
    }

    /**
     * Set callback for when an item is selected.
     */
    public void setOnItemSelected(Consumer<Item> callback) {
        this.onItemSelected = callback;
    }

    /**
     * Set callback for when an item is double-clicked.
     */
    public void setOnItemDoubleClicked(Consumer<Item> callback) {
        this.onItemDoubleClicked = callback;
    }

    /**
     * Get the currently selected item.
     */
    public Item getSelectedItem() {
        return selectedItem;
    }

    /**
     * Get all search results.
     */
    public List<Item> getSearchResults() {
        return new ArrayList<>(searchResults);
    }

    /**
     * Clear search and results.
     */
    public void clear() {
        searchField.setText("Search by title, description, keywords...");
        searchField.setForeground(Color.GRAY);
        enterpriseCombo.setSelectedIndex(0);
        organizationCombo.setSelectedIndex(0);
        categoryCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        crossEnterpriseCheckbox.setSelected(true);
        enterpriseCombo.setEnabled(false);
        organizationCombo.setEnabled(false);
        searchResults = new ArrayList<>();
        selectedItem = null;
        selectedCard = null;
        showEmptyState("Enter search terms or use filters to find items across all enterprises");
    }

    // ==================== PRIVATE HELPERS ====================
    private List<Item> filterItems(List<Item> items, String searchText) {
        return items.stream()
                .filter(item -> matchesSearchText(item, searchText))
                .filter(this::matchesEnterpriseFilter)
                .filter(this::matchesOrganizationFilter)
                .filter(this::matchesCategoryFilter)
                .filter(this::matchesTypeFilter)
                .filter(this::matchesStatusFilter)
                .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchText(Item item, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        String lowerSearch = searchText.toLowerCase();

        // Check title
        if (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerSearch)) {
            return true;
        }

        // Check description
        if (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerSearch)) {
            return true;
        }

        // Check keywords
        if (item.getKeywords() != null) {
            for (String keyword : item.getKeywords()) {
                if (keyword.toLowerCase().contains(lowerSearch)) {
                    return true;
                }
            }
        }

        // Check brand
        if (item.getBrand() != null && item.getBrand().toLowerCase().contains(lowerSearch)) {
            return true;
        }

        // Check color
        if (item.getPrimaryColor() != null && item.getPrimaryColor().toLowerCase().contains(lowerSearch)) {
            return true;
        }

        return false;
    }

    private boolean matchesEnterpriseFilter(Item item) {
        if (crossEnterpriseCheckbox.isSelected()) {
            return true;
        }

        EnterpriseOption selected = (EnterpriseOption) enterpriseCombo.getSelectedItem();
        if (selected == null || selected.enterprise == null) {
            return true;
        }

        return selected.enterprise.getEnterpriseId().equals(item.getEnterpriseId());
    }

    private boolean matchesOrganizationFilter(Item item) {
        if (crossEnterpriseCheckbox.isSelected()) {
            return true;
        }

        OrganizationOption selected = (OrganizationOption) organizationCombo.getSelectedItem();
        if (selected == null || selected.organization == null) {
            return true;
        }

        return selected.organization.getOrganizationId().equals(item.getOrganizationId());
    }

    /**
     * Update organization combo based on selected enterprise.
     */
    private void updateOrganizationCombo() {
        organizationCombo.removeAllItems();
        organizationCombo.addItem(new OrganizationOption(null, "All Organizations"));

        EnterpriseOption selectedEnterprise = (EnterpriseOption) enterpriseCombo.getSelectedItem();

        if (selectedEnterprise == null || selectedEnterprise.enterprise == null) {
            // Show all organizations when no enterprise is selected
            for (Organization org : organizationMap.values()) {
                organizationCombo.addItem(new OrganizationOption(org, org.getName()));
            }
        } else {
            // Filter organizations by selected enterprise
            String enterpriseId = selectedEnterprise.enterprise.getEnterpriseId();
            for (Organization org : organizationMap.values()) {
                if (enterpriseId.equals(org.getEnterpriseId())) {
                    organizationCombo.addItem(new OrganizationOption(org, org.getName()));
                }
            }
        }
    }

    private boolean matchesCategoryFilter(Item item) {
        String selected = (String) categoryCombo.getSelectedItem();
        if (selected == null || selected.equals("All Categories")) {
            return true;
        }

        for (Item.ItemCategory cat : Item.ItemCategory.values()) {
            String catString = cat.getEmoji() + " " + cat.getDisplayName();
            if (catString.equals(selected)) {
                return item.getCategory() == cat;
            }
        }
        return true;
    }

    private boolean matchesTypeFilter(Item item) {
        String selected = (String) typeCombo.getSelectedItem();
        if (selected == null || selected.equals("All Types")) {
            return true;
        }

        if (selected.contains("Lost")) {
            return item.getType() == Item.ItemType.LOST;
        }
        if (selected.contains("Found")) {
            return item.getType() == Item.ItemType.FOUND;
        }
        return true;
    }

    private boolean matchesStatusFilter(Item item) {
        String selected = (String) statusCombo.getSelectedItem();
        if (selected == null || selected.equals("All Statuses")) {
            return true;
        }

        return switch (selected) {
            case "Open" ->
                item.getStatus() == Item.ItemStatus.OPEN;
            case "Pending Claim" ->
                item.getStatus() == Item.ItemStatus.PENDING_CLAIM;
            case "Verified" ->
                item.getStatus() == Item.ItemStatus.VERIFIED;
            default ->
                true;
        };
    }

    private void displayResults(List<Item> items) {
        resultsPanel.removeAll();
        selectedItem = null;
        selectedCard = null;

        if (items.isEmpty()) {
            showEmptyState("No items found matching your search criteria");
            resultsCountLabel.setText("0 items found");
        } else {
            resultsCountLabel.setText(items.size() + " item" + (items.size() != 1 ? "s" : "") + " found");

            // Group by enterprise for display
            Map<String, List<Item>> byEnterprise = items.stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getEnterpriseId() != null ? item.getEnterpriseId() : "unknown"
                    ));

            for (Item item : items) {
                JPanel card = createItemCard(item);
                resultsPanel.add(card);
                resultsPanel.add(Box.createVerticalStrut(10));
            }
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();

        // Scroll to top
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private JPanel createItemCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Left - Image
        JLabel imageLabel = createImageLabel(item);
        card.add(imageLabel, BorderLayout.WEST);

        // Center - Details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);

        // Enterprise badge + Type badge + Title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Enterprise badge
        Enterprise enterprise = enterpriseMap.get(item.getEnterpriseId());
        if (enterprise != null) {
            JLabel entBadge = createEnterpriseBadge(enterprise);
            titleRow.add(entBadge);
        }

        // Type badge
        JLabel typeBadge = new JLabel(item.getType().getIcon() + " " + item.getType().getLabel());
        typeBadge.setFont(UIConstants.getEmojiFont(Font.BOLD, 10));
        typeBadge.setForeground(Color.WHITE);
        typeBadge.setOpaque(true);
        typeBadge.setBackground(item.getType() == Item.ItemType.FOUND
                ? new Color(40, 167, 69) : new Color(220, 53, 69));
        typeBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        titleRow.add(typeBadge);

        // Title
        JLabel titleLabel = new JLabel(truncate(item.getTitle(), 35));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleRow.add(titleLabel);

        detailsPanel.add(titleRow);
        detailsPanel.add(Box.createVerticalStrut(5));

        // Category and location
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryLabel = new JLabel(item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName());
        categoryLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(108, 117, 125));
        infoRow.add(categoryLabel);

        infoRow.add(new JLabel("  •  "));

        String locationText = item.getLocation() != null
                ? "📍 " + item.getLocation().getBuilding().getName() : "📍 Unknown";
        JLabel locationLabel = new JLabel(locationText);
        locationLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        locationLabel.setForeground(new Color(108, 117, 125));
        infoRow.add(locationLabel);

        // Status
        infoRow.add(new JLabel("  •  "));
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        infoRow.add(statusLabel);

        detailsPanel.add(infoRow);
        detailsPanel.add(Box.createVerticalStrut(5));

        // Description preview
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            JLabel descLabel = new JLabel(truncate(item.getDescription(), 80));
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            descLabel.setForeground(new Color(73, 80, 87));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(descLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
        }

        // Date and organization
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLabel = new JLabel("Reported: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        metaRow.add(dateLabel);

        // Organization name
        Organization org = organizationMap.get(item.getOrganizationId());
        if (org != null) {
            metaRow.add(new JLabel("  •  "));
            JLabel orgLabel = new JLabel(org.getName());
            orgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            orgLabel.setForeground(new Color(134, 142, 150));
            metaRow.add(orgLabel);
        }

        detailsPanel.add(metaRow);

        card.add(detailsPanel, BorderLayout.CENTER);

        // Right - Actions
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(100, 0));

        JButton viewButton = new JButton("View");
        viewButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        viewButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewButton.setMaximumSize(new Dimension(90, 30));
        viewButton.addActionListener(e -> {
            if (onItemDoubleClicked != null) {
                onItemDoubleClicked.accept(item);
            }
        });
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(viewButton);
        rightPanel.add(Box.createVerticalGlue());

        card.add(rightPanel, BorderLayout.EAST);

        // Selection handling
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectCard(card, item);
                if (e.getClickCount() == 2 && onItemDoubleClicked != null) {
                    onItemDoubleClicked.accept(item);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (card != selectedCard) {
                    card.setBackground(CARD_HOVER_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (card != selectedCard) {
                    card.setBackground(Color.WHITE);
                }
            }
        });

        return card;
    }

    private void selectCard(JPanel card, Item item) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setBackground(Color.WHITE);
            selectedCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(222, 226, 230)),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
        }

        // Select new
        selectedCard = card;
        selectedItem = item;
        card.setBackground(CARD_SELECTED_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(13, 110, 253), 2),
                BorderFactory.createEmptyBorder(11, 11, 11, 11)
        ));

        // Callback
        if (onItemSelected != null) {
            onItemSelected.accept(item);
        }
    }

    private JLabel createImageLabel(Item item) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(90, 90));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        label.setOpaque(true);
        label.setBackground(new Color(248, 249, 250));

        // Try to load image
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            String imagePath = item.getImagePaths().get(0);
            File imageFile = new File(System.getProperty("user.dir"), imagePath);

            if (imageFile.exists()) {
                try {
                    ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                    Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(img));
                    label.setText("");
                    return label;
                } catch (Exception e) {
                    // Fall through to emoji
                }
            }
        }

        // Fallback to category emoji
        label.setText(item.getCategory().getEmoji());
        label.setFont(UIConstants.getEmojiFont(Font.PLAIN, 32));
        return label;
    }

    private JLabel createEnterpriseBadge(Enterprise enterprise) {
        String icon = getEnterpriseIcon(enterprise.getType());
        String shortName = getEnterpriseShortName(enterprise.getName());

        JLabel badge = new JLabel(icon + " " + shortName);
        badge.setFont(UIConstants.getEmojiFont(Font.BOLD, 9));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(ENTERPRISE_COLORS.getOrDefault(enterprise.getType(), Color.GRAY));
        badge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        return badge;
    }

    private String getEnterpriseIcon(Enterprise.EnterpriseType type) {
        return switch (type) {
            case HIGHER_EDUCATION ->
                "🎓";
            case PUBLIC_TRANSIT ->
                "🚇";
            case AIRPORT ->
                "✈️";
            case LAW_ENFORCEMENT ->
                "🚔";
        };
    }

    private String getEnterpriseShortName(String name) {
        if (name == null) {
            return "?";
        }
        if (name.contains("Northeastern")) {
            return "NEU";
        }
        if (name.contains("Boston University")) {
            return "BU";
        }
        if (name.contains("MBTA")) {
            return "MBTA";
        }
        if (name.contains("Logan") || name.contains("Airport")) {
            return "Logan";
        }
        if (name.contains("Police")) {
            return "BPD";
        }
        if (name.length() > 10) {
            return name.substring(0, 8) + "...";
        }
        return name;
    }

    private void showEmptyState(String message) {
        resultsPanel.removeAll();

        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setOpaque(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel iconLabel = new JLabel("🌐");
        iconLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(iconLabel);

        content.add(Box.createVerticalStrut(15));

        JLabel msgLabel = new JLabel("<html><center>" + message + "</center></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setForeground(new Color(108, 117, 125));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(msgLabel);

        emptyPanel.add(content);
        resultsPanel.add(emptyPanel);

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        searchButton.setEnabled(!loading);
        enterpriseCombo.setEnabled(!loading && !crossEnterpriseCheckbox.isSelected());
        organizationCombo.setEnabled(!loading && !crossEnterpriseCheckbox.isSelected());
        categoryCombo.setEnabled(!loading);
        typeCombo.setEnabled(!loading);
        statusCombo.setEnabled(!loading);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Search Error", JOptionPane.ERROR_MESSAGE);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // ==================== INNER CLASS ====================
    /**
     * Wrapper for enterprise combo box items.
     */
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

    /**
     * Wrapper for organization combo box items.
     */
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
