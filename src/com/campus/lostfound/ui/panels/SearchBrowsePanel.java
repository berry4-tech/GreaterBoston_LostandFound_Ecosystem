// Updated SearchBrowsePanel.java to work with MongoDB
package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.EnterpriseItemService;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.components.ItemListRenderer;
import com.campus.lostfound.ui.dialogs.ItemDetailDialog;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SearchBrowsePanel extends JPanel {

    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 18);

    private JTextField searchField;
    private JComboBox<String> typeFilter;
    private JComboBox<Item.ItemCategory> categoryFilter;
    private JComboBox<Building> buildingFilter;
    private JComboBox<String> sortBy;
    private JButton searchButton;
    private JButton clearFiltersButton;
    private JToggleButton myItemsToggle;
    private JToggleButton matchingToggle;

    private JList<Item> itemList;
    private DefaultListModel<Item> listModel;
    private ItemListRenderer renderer;

    private JLabel resultsCountLabel;
    private JLabel matchingStatusLabel;
    private JProgressBar searchProgress;

    private List<Item> allItems;
    private List<Item> filteredItems;
    private User currentUser;
    private ItemMatcher matcher;

    // MongoDB DAOs
    private MongoItemDAO itemDAO;
    private MongoUserDAO userDAO;
    private MongoCollection<Document> buildingsCollection;
    
    // Enterprise services
    private EnterpriseItemService enterpriseItemService;
    private MongoEnterpriseDAO enterpriseDAO;
    private JComboBox<String> enterpriseFilter;

    public SearchBrowsePanel(User currentUser) {
        this.currentUser = currentUser;
        this.matcher = new ItemMatcher();
        this.allItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();

        // Initialize MongoDB DAOs
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.buildingsCollection = MongoDBConnection.getInstance().getCollection("buildings");
        
        // Initialize enterprise services
        this.enterpriseItemService = new EnterpriseItemService();
        this.enterpriseDAO = new MongoEnterpriseDAO();

        setLayout(new BorderLayout());
        initComponents();
        loadItemsFromMongoDB(); // Load from MongoDB instead of sample data
    }

    private void loadItemsFromMongoDB() {
        // Load items from MongoDB in background thread
        SwingWorker<List<Item>, Void> worker = new SwingWorker<List<Item>, Void>() {
            @Override
            protected List<Item> doInBackground() throws Exception {
                searchProgress.setVisible(true);
                return itemDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    allItems = get();
                    updateResultsList(allItems);
                    searchProgress.setVisible(false);

                    // Update status
                    resultsCountLabel.setText(allItems.size() + " items loaded from database");

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SearchBrowsePanel.this,
                            "Error loading items from database: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    searchProgress.setVisible(false);
                }
            }
        };

        worker.execute();
    }

    private void initComponents() {
        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setOneTouchExpandable(true);

        // Left panel - Filters
        JPanel filterPanel = createFilterPanel();
        splitPane.setLeftComponent(new JScrollPane(filterPanel));

        // Right panel - Results
        JPanel resultsPanel = createResultsPanel();
        splitPane.setRightComponent(resultsPanel);

        add(splitPane, BorderLayout.CENTER);

        // Top toolbar
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(new Color(240, 240, 240));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Title
        JLabel titleLabel = new JLabel("Lost & Found Search (MongoDB)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        toolbar.add(titleLabel);

        toolbar.add(Box.createHorizontalStrut(30));

        // Connection status indicator
        JLabel statusLabel = new JLabel("●");
        statusLabel.setForeground(MongoDBConnection.getInstance().testConnection()
                ? new Color(0, 200, 0) : Color.RED);
        statusLabel.setToolTipText(MongoDBConnection.getInstance().testConnection()
                ? "Connected to MongoDB" : "MongoDB Disconnected");
        toolbar.add(statusLabel);

        toolbar.add(Box.createHorizontalStrut(20));

        // Quick action buttons
        myItemsToggle = new JToggleButton("My Items");
        myItemsToggle.addActionListener(e -> filterMyItems());
        toolbar.add(myItemsToggle);

        matchingToggle = new JToggleButton("Find Matches");
        matchingToggle.setToolTipText("Find potential matches for your lost items");
        matchingToggle.addActionListener(e -> findMatches());
        toolbar.add(matchingToggle);

        JButton reportButton = new JButton("+ Report Item");
        reportButton.setBackground(new Color(76, 175, 80));
        reportButton.setForeground(Color.BLACK);
        reportButton.setFocusPainted(false);
        reportButton.addActionListener(e -> openReportDialog());
        toolbar.add(reportButton);

        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.addActionListener(e -> loadItemsFromMongoDB());
        toolbar.add(refreshButton);

        return toolbar;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search field
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField = new JTextField();
        searchField.setToolTipText("MongoDB full-text search");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performMongoSearch();
                }
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchButton = new JButton("🔍");
        searchButton.setFont(EMOJI_FONT);
        searchButton.addActionListener(e -> performMongoSearch());
        searchPanel.add(searchButton, BorderLayout.EAST);
        panel.add(searchPanel);
        panel.add(Box.createVerticalStrut(10));

        // Type filter (Lost/Found)
        JPanel typePanel = new JPanel(new BorderLayout());
        typePanel.setBorder(BorderFactory.createTitledBorder("Item Type"));
        typeFilter = new JComboBox<>(new String[]{"All", "Lost Items", "Found Items"});
        typeFilter.addActionListener(e -> applyFilters());
        typePanel.add(typeFilter);
        panel.add(typePanel);
        panel.add(Box.createVerticalStrut(10));

        // Category filter
        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Category"));
        Vector<Item.ItemCategory> categories = new Vector<>();
        categories.add(null); // "All" option
        categories.addAll(Arrays.asList(Item.ItemCategory.values()));
        categoryFilter = new JComboBox<>(categories);
        categoryFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(EMOJI_FONT);
                if (value == null) {
                    setText("All Categories");
                } else {
                    Item.ItemCategory cat = (Item.ItemCategory) value;
                    setText(cat.getEmoji() + " " + cat.getDisplayName());
                }
                return this;
            }
        });
        categoryFilter.addActionListener(e -> applyFilters());
        categoryPanel.add(categoryFilter);
        panel.add(categoryPanel);
        panel.add(Box.createVerticalStrut(10));

        // Building filter - Load from MongoDB
        JPanel buildingPanel = new JPanel(new BorderLayout());
        buildingPanel.setBorder(BorderFactory.createTitledBorder("Building"));
        Vector<Building> buildings = new Vector<>();
        buildings.add(null); // "All" option
        buildings.addAll(loadBuildingsFromMongoDB());
        buildingFilter = new JComboBox<>(buildings);
        buildingFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("All Buildings");
                } else {
                    setText(value.toString());
                }
                return this;
            }
        });
        buildingFilter.addActionListener(e -> applyFilters());
        buildingPanel.add(buildingFilter);
        panel.add(buildingPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // Enterprise filter (Cross-Enterprise Search)
        JPanel enterprisePanel = new JPanel(new BorderLayout());
        enterprisePanel.setBorder(BorderFactory.createTitledBorder("Enterprise (Cross-Org Search)"));
        Vector<String> enterprises = new Vector<>();
        enterprises.add("All Enterprises");
        try {
            enterpriseDAO.findAll().forEach(e -> enterprises.add(e.getName()));
        } catch (Exception e) {
            System.err.println("Error loading enterprises: " + e.getMessage());
        }
        enterpriseFilter = new JComboBox<>(enterprises);
        enterpriseFilter.addActionListener(e -> applyFilters());
        enterprisePanel.add(enterpriseFilter);
        panel.add(enterprisePanel);
        panel.add(Box.createVerticalStrut(10));

        // Sort options
        JPanel sortPanel = new JPanel(new BorderLayout());
        sortPanel.setBorder(BorderFactory.createTitledBorder("Sort By"));
        sortBy = new JComboBox<>(new String[]{
            "Most Recent", "Oldest First", "Best Match", "Category", "Building"
        });
        sortBy.addActionListener(e -> sortResults());
        sortPanel.add(sortBy);
        panel.add(sortPanel);
        panel.add(Box.createVerticalStrut(10));

        // Database stats
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder(BorderFactory.createTitledBorder("Database Stats"));
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));

        JLabel mongoLabel = new JLabel("MongoDB: Connected");
        mongoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsPanel.add(mongoLabel);

        JLabel itemCountLabel = new JLabel("Items: Loading...");
        itemCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsPanel.add(itemCountLabel);

        // Update stats in background
        SwingUtilities.invokeLater(() -> {
            int count = itemDAO.findAll().size();
            itemCountLabel.setText("Items in DB: " + count);
        });

        panel.add(statsPanel);
        panel.add(Box.createVerticalStrut(10));

        // Clear filters button
        clearFiltersButton = new JButton("Clear All Filters");
        clearFiltersButton.addActionListener(e -> clearFilters());
        panel.add(clearFiltersButton);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Results header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        headerPanel.setBackground(new Color(250, 250, 250));

        resultsCountLabel = new JLabel("Loading from MongoDB...");
        resultsCountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(resultsCountLabel, BorderLayout.WEST);

        matchingStatusLabel = new JLabel("");
        matchingStatusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        matchingStatusLabel.setForeground(new Color(100, 100, 200));
        headerPanel.add(matchingStatusLabel, BorderLayout.CENTER);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Item list
        listModel = new DefaultListModel<>();
        itemList = new JList<>(listModel);
        itemList.setCellRenderer(new ItemListRenderer());
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFixedCellHeight(100);
        itemList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Item selected = itemList.getSelectedValue();
                    if (selected != null) {
                        showItemDetails(selected);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(itemList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Progress bar for searching
        searchProgress = new JProgressBar();
        searchProgress.setIndeterminate(true);
        searchProgress.setVisible(false);
        panel.add(searchProgress, BorderLayout.SOUTH);

        return panel;
    }

    private void performMongoSearch() {
        searchProgress.setVisible(true);

        SwingWorker<List<Item>, Void> worker = new SwingWorker<List<Item>, Void>() {
            @Override
            protected List<Item> doInBackground() throws Exception {
                String query = searchField.getText();

                // Get selected type
                Item.ItemType type = null;
                String typeSelection = (String) typeFilter.getSelectedItem();
                if ("Lost Items".equals(typeSelection)) {
                    type = Item.ItemType.LOST;
                } else if ("Found Items".equals(typeSelection)) {
                    type = Item.ItemType.FOUND;
                }

                // Get selected category
                Item.ItemCategory category = (Item.ItemCategory) categoryFilter.getSelectedItem();
                
                // Get selected enterprise
                String enterpriseSelection = (String) enterpriseFilter.getSelectedItem();
                
                // Use enterprise service for cross-enterprise search
                List<Item> results;
                if ("All Enterprises".equals(enterpriseSelection) || enterpriseSelection == null) {
                    // Search all enterprises
                    results = enterpriseItemService.searchAllEnterprises(
                        query.isEmpty() ? null : query, 
                        category
                    );
                } else {
                    // Search specific enterprise by name
                    results = itemDAO.searchItems(query.isEmpty() ? null : query, type, category);
                    // Filter by enterprise name
                    final String entName = enterpriseSelection;
                    results = results.stream()
                        .filter(item -> {
                            String itemEntId = item.getEnterpriseId();
                            if (itemEntId == null) return false;
                            return enterpriseDAO.findAll().stream()
                                .anyMatch(e -> e.getEnterpriseId().equals(itemEntId) && e.getName().equals(entName));
                        })
                        .collect(Collectors.toList());
                }
                
                // Apply type filter if needed
                if (type != null) {
                    final Item.ItemType finalType = type;
                    results = results.stream()
                        .filter(item -> item.getType() == finalType)
                        .collect(Collectors.toList());
                }
                
                return results;
            }

            @Override
            protected void done() {
                try {
                    List<Item> results = get();
                    filteredItems = results;
                    updateResultsList(results);
                    searchProgress.setVisible(false);

                    if (results.isEmpty()) {
                        matchingStatusLabel.setText("No items found in MongoDB");
                    } else {
                        matchingStatusLabel.setText("Found " + results.size() + " items");
                    }
                } catch (Exception e) {
                    searchProgress.setVisible(false);
                    JOptionPane.showMessageDialog(SearchBrowsePanel.this,
                            "Search error: " + e.getMessage(),
                            "MongoDB Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private List<Building> loadBuildingsFromMongoDB() {
        List<Building> buildings = new ArrayList<>();

        try {
            for (Document doc : buildingsCollection.find()) {
                Building building = new Building(
                        doc.getString("name"),
                        doc.getString("code"),
                        Building.BuildingType.valueOf(doc.getString("type"))
                );
                buildings.add(building);
            }
        } catch (Exception e) {
            System.err.println("Error loading buildings from MongoDB: " + e.getMessage());
        }

        return buildings;
    }

    private void applyFilters() {
        performMongoSearch(); // Re-run search with current filters
    }

    private void updateResultsList(List<Item> items) {
        listModel.clear();
        for (Item item : items) {
            listModel.addElement(item);
        }

        resultsCountLabel.setText(items.size() + " item" + (items.size() != 1 ? "s" : "") + " found");
    }

    private void filterMyItems() {
        if (myItemsToggle.isSelected()) {
            matchingToggle.setSelected(false);

            searchProgress.setVisible(true);
            SwingWorker<List<Item>, Void> worker = new SwingWorker<List<Item>, Void>() {
                @Override
                protected List<Item> doInBackground() throws Exception {
                    return itemDAO.findByUser(currentUser.getEmail());
                }

                @Override
                protected void done() {
                    try {
                        List<Item> myItems = get();
                        updateResultsList(myItems);
                        resultsCountLabel.setText("Your " + myItems.size() + " item(s)");
                        searchProgress.setVisible(false);
                    } catch (Exception e) {
                        searchProgress.setVisible(false);
                    }
                }
            };
            worker.execute();
        } else {
            applyFilters();
        }
    }

    private void findMatches() {
        if (matchingToggle.isSelected()) {
            myItemsToggle.setSelected(false);

            searchProgress.setVisible(true);
            SwingWorker<List<Item>, Void> worker = new SwingWorker<List<Item>, Void>() {
                @Override
                protected List<Item> doInBackground() throws Exception {
                    // Find user's lost items
                    List<Item> userItems = itemDAO.findByUser(currentUser.getEmail());
                    List<Item> userLostItems = userItems.stream()
                            .filter(item -> item.getType() == Item.ItemType.LOST)
                            .filter(item -> item.getStatus() != Item.ItemStatus.CLAIMED)
                            .collect(Collectors.toList());

                    if (userLostItems.isEmpty()) {
                        return new ArrayList<>();
                    }

                    // Find all found items
                    List<Item> foundItems = itemDAO.searchItems(null, Item.ItemType.FOUND, null);

                    // Find matches
                    List<Item> matchedItems = new ArrayList<>();
                    for (Item lostItem : userLostItems) {
                        List<ItemMatcher.PotentialMatch> matches = matcher.findMatches(lostItem, foundItems);
                        for (ItemMatcher.PotentialMatch match : matches) {
                            Item matchedItem = match.getItem();
                            matchedItem.setMatchScore(match.getScore());
                            matchedItems.add(matchedItem);
                        }
                    }

                    // Sort by match score
                    matchedItems.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

                    return matchedItems;
                }

                @Override
                protected void done() {
                    try {
                        List<Item> matches = get();
                        if (matches.isEmpty()) {
                            JOptionPane.showMessageDialog(SearchBrowsePanel.this,
                                    "You don't have any open lost items to match.",
                                    "No Lost Items",
                                    JOptionPane.INFORMATION_MESSAGE);
                            matchingToggle.setSelected(false);
                        } else {
                            updateResultsList(matches);
                            matchingStatusLabel.setText("Showing potential matches for your lost items");
                        }
                        searchProgress.setVisible(false);
                    } catch (Exception e) {
                        searchProgress.setVisible(false);
                    }
                }
            };
            worker.execute();
        } else {
            applyFilters();
        }
    }

    private void clearFilters() {
        searchField.setText("");
        typeFilter.setSelectedIndex(0);
        categoryFilter.setSelectedIndex(0);
        buildingFilter.setSelectedIndex(0);
        enterpriseFilter.setSelectedIndex(0);
        sortBy.setSelectedIndex(0);
        myItemsToggle.setSelected(false);
        matchingToggle.setSelected(false);
        filteredItems.clear();
        loadItemsFromMongoDB();
    }

    private void sortResults() {
        String sortOption = (String) sortBy.getSelectedItem();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            items.add(listModel.getElementAt(i));
        }

        switch (sortOption) {
            case "Most Recent":
                items.sort((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()));
                break;
            case "Oldest First":
                items.sort((a, b) -> a.getReportedDate().compareTo(b.getReportedDate()));
                break;
            case "Best Match":
                items.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
                break;
            case "Category":
                items.sort((a, b) -> a.getCategory().compareTo(b.getCategory()));
                break;
            case "Building":
                items.sort((a, b)
                        -> a.getLocation().getBuilding().getName()
                                .compareTo(b.getLocation().getBuilding().getName()));
                break;
        }

        updateResultsList(items);
    }

    private void showItemDetails(Item item) {
        ItemDetailDialog dialog = new ItemDetailDialog(
                SwingUtilities.getWindowAncestor(this),
                item,
                currentUser
        );
        dialog.setVisible(true);
    }

    private void openReportDialog() {
        JOptionPane.showMessageDialog(this,
                "Report Item Dialog will save to MongoDB",
                "Report Lost/Found Item",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
