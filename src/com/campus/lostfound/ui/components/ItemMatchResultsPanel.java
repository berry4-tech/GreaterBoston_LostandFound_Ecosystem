package com.campus.lostfound.ui.components;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.dao.MongoItemDAO;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable panel component for displaying item match results with confidence scores.
 * Features:
 * - Visual confidence bars with color coding (green/yellow/orange/red)
 * - Item details with images
 * - "Create Claim Request" button
 * - Match reason display
 * - Sorting and filtering options
 * 
 * @author Developer 2 - UI Components
 */
public class ItemMatchResultsPanel extends JPanel {
    
    // Data
    private User currentUser;
    private Item sourceItem;  // The item we're finding matches for
    private List<PotentialMatch> matches;
    private ItemMatcher itemMatcher;
    private MongoItemDAO itemDAO;
    
    // UI Components
    private JPanel matchesListPanel;
    private JScrollPane scrollPane;
    private JLabel headerLabel;
    private JLabel countLabel;
    private JComboBox<String> sortCombo;
    private JSlider confidenceSlider;
    private JLabel confidenceLabel;
    private JProgressBar loadingBar;
    private JButton refreshButton;
    
    // Selected match
    private PotentialMatch selectedMatch;
    private JPanel selectedCard;
    
    // Callbacks
    private Consumer<PotentialMatch> onMatchSelected;
    private BiConsumer<Item, PotentialMatch> onClaimRequested;
    
    // Constants
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Color HIGH_MATCH_COLOR = new Color(40, 167, 69);      // Green >= 80%
    private static final Color GOOD_MATCH_COLOR = new Color(23, 162, 184);     // Teal >= 60%
    private static final Color MODERATE_MATCH_COLOR = new Color(255, 193, 7);  // Yellow >= 40%
    private static final Color LOW_MATCH_COLOR = new Color(255, 152, 0);       // Orange >= 30%
    private static final Color CARD_SELECTED_BG = new Color(232, 244, 253);
    private static final Color CARD_HOVER_BG = new Color(248, 249, 250);
    
    /**
     * Create a new ItemMatchResultsPanel.
     * 
     * @param currentUser The logged-in user
     */
    public ItemMatchResultsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemMatcher = new ItemMatcher();
        this.itemDAO = new MongoItemDAO();
        this.matches = new ArrayList<>();
        
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header with controls
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Matches list (scrollable)
        matchesListPanel = new JPanel();
        matchesListPanel.setLayout(new BoxLayout(matchesListPanel, BoxLayout.Y_AXIS));
        matchesListPanel.setBackground(Color.WHITE);
        
        scrollPane = new JScrollPane(matchesListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Footer with loading indicator
        add(createFooterPanel(), BorderLayout.SOUTH);
        
        // Show empty state initially
        showEmptyState("Select an item to find matches");
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        headerLabel = new JLabel("🔍 Potential Matches");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        leftPanel.add(headerLabel);
        
        countLabel = new JLabel("");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        countLabel.setForeground(new Color(108, 117, 125));
        leftPanel.add(countLabel);
        
        titleRow.add(leftPanel, BorderLayout.WEST);
        
        // Refresh button
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            if (sourceItem != null) {
                findMatchesFor(sourceItem);
            }
        });
        titleRow.add(refreshButton, BorderLayout.EAST);
        
        panel.add(titleRow, BorderLayout.NORTH);
        
        // Filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterRow.setOpaque(false);
        
        // Sort dropdown
        JLabel sortLabel = new JLabel("Sort by:");
        sortLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(sortLabel);
        
        sortCombo = new JComboBox<>(new String[]{
            "Confidence (High to Low)",
            "Confidence (Low to High)",
            "Date (Newest First)",
            "Date (Oldest First)"
        });
        sortCombo.setPreferredSize(new Dimension(180, 28));
        sortCombo.addActionListener(e -> sortMatches());
        filterRow.add(sortCombo);
        
        filterRow.add(Box.createHorizontalStrut(20));
        
        // Minimum confidence slider
        JLabel minLabel = new JLabel("Min Confidence:");
        minLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterRow.add(minLabel);
        
        confidenceSlider = new JSlider(0, 100, 30);
        confidenceSlider.setPreferredSize(new Dimension(150, 25));
        confidenceSlider.setOpaque(false);
        confidenceSlider.addChangeListener(e -> {
            confidenceLabel.setText(confidenceSlider.getValue() + "%");
            if (!confidenceSlider.getValueIsAdjusting()) {
                filterMatches();
            }
        });
        filterRow.add(confidenceSlider);
        
        confidenceLabel = new JLabel("30%");
        confidenceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        confidenceLabel.setForeground(new Color(13, 110, 253));
        filterRow.add(confidenceLabel);
        
        panel.add(filterRow, BorderLayout.SOUTH);
        
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
        
        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        
        legendPanel.add(createLegendItem("●", HIGH_MATCH_COLOR, "≥80% Very High"));
        legendPanel.add(createLegendItem("●", GOOD_MATCH_COLOR, "≥60% High"));
        legendPanel.add(createLegendItem("●", MODERATE_MATCH_COLOR, "≥40% Moderate"));
        legendPanel.add(createLegendItem("●", LOW_MATCH_COLOR, "≥30% Possible"));
        
        panel.add(legendPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JLabel createLegendItem(String symbol, Color color, String text) {
        JLabel label = new JLabel(symbol + " " + text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Find matches for the given item.
     * 
     * @param item The item to find matches for
     */
    public void findMatchesFor(Item item) {
        this.sourceItem = item;
        setLoading(true);
        
        headerLabel.setText("🔍 Matches for: " + truncate(item.getTitle(), 30));
        
        SwingWorker<List<PotentialMatch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PotentialMatch> doInBackground() {
                // Get all items from database
                List<Item> allItems = itemDAO.findAll();
                // Find matches
                return itemMatcher.findMatches(item, allItems);
            }
            
            @Override
            protected void done() {
                try {
                    matches = get();
                    filterMatches(); // Apply current filter
                } catch (Exception e) {
                    showError("Failed to find matches: " + e.getMessage());
                    matches = new ArrayList<>();
                    displayMatches(matches);
                } finally {
                    setLoading(false);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Display pre-computed matches (for use with external matching).
     * 
     * @param sourceItem The source item
     * @param matches The matches to display
     */
    public void displayMatches(Item sourceItem, List<PotentialMatch> matches) {
        this.sourceItem = sourceItem;
        this.matches = matches;
        headerLabel.setText("🔍 Matches for: " + truncate(sourceItem.getTitle(), 30));
        filterMatches();
    }
    
    /**
     * Set callback for when a match is selected.
     */
    public void setOnMatchSelected(Consumer<PotentialMatch> callback) {
        this.onMatchSelected = callback;
    }
    
    /**
     * Set callback for when "Create Claim" is clicked.
     * BiConsumer receives (sourceItem, selectedMatch)
     */
    public void setOnClaimRequested(BiConsumer<Item, PotentialMatch> callback) {
        this.onClaimRequested = callback;
    }
    
    /**
     * Get the currently selected match.
     */
    public PotentialMatch getSelectedMatch() {
        return selectedMatch;
    }
    
    /**
     * Get all displayed matches.
     */
    public List<PotentialMatch> getMatches() {
        return new ArrayList<>(matches);
    }
    
    /**
     * Get the source item.
     */
    public Item getSourceItem() {
        return sourceItem;
    }
    
    /**
     * Clear the display.
     */
    public void clear() {
        sourceItem = null;
        matches = new ArrayList<>();
        selectedMatch = null;
        selectedCard = null;
        headerLabel.setText("🔍 Potential Matches");
        countLabel.setText("");
        showEmptyState("Select an item to find matches");
    }
    
    // ==================== PRIVATE HELPERS ====================
    
    private void filterMatches() {
        double minConfidence = confidenceSlider.getValue() / 100.0;
        
        List<PotentialMatch> filtered = matches.stream()
            .filter(m -> m.getScore() >= minConfidence)
            .toList();
        
        sortMatches(filtered);
    }
    
    private void sortMatches() {
        filterMatches(); // Re-filter and sort
    }
    
    private void sortMatches(List<PotentialMatch> filteredMatches) {
        List<PotentialMatch> sorted = new ArrayList<>(filteredMatches);
        
        String sortOption = (String) sortCombo.getSelectedItem();
        if (sortOption != null) {
            switch (sortOption) {
                case "Confidence (High to Low)" -> 
                    sorted.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                case "Confidence (Low to High)" -> 
                    sorted.sort((a, b) -> Double.compare(a.getScore(), b.getScore()));
                case "Date (Newest First)" -> 
                    sorted.sort((a, b) -> b.getItem().getReportedDate().compareTo(a.getItem().getReportedDate()));
                case "Date (Oldest First)" -> 
                    sorted.sort((a, b) -> a.getItem().getReportedDate().compareTo(b.getItem().getReportedDate()));
            }
        }
        
        displayMatches(sorted);
    }
    
    private void displayMatches(List<PotentialMatch> matchesToDisplay) {
        matchesListPanel.removeAll();
        selectedMatch = null;
        selectedCard = null;
        
        if (matchesToDisplay.isEmpty()) {
            showEmptyState("No matches found above " + confidenceSlider.getValue() + "% confidence");
            countLabel.setText("0 matches");
        } else {
            countLabel.setText(matchesToDisplay.size() + " match" + (matchesToDisplay.size() != 1 ? "es" : ""));
            
            for (PotentialMatch match : matchesToDisplay) {
                JPanel card = createMatchCard(match);
                matchesListPanel.add(card);
                matchesListPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        matchesListPanel.revalidate();
        matchesListPanel.repaint();
        
        // Scroll to top
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }
    
    private JPanel createMatchCard(PotentialMatch match) {
        Item item = match.getItem();
        double score = match.getScore();
        int percentage = (int) (score * 100);
        
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Left - Image
        JLabel imageLabel = createImageLabel(item);
        card.add(imageLabel, BorderLayout.WEST);
        
        // Center - Details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        // Title with type badge
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel typeBadge = new JLabel(item.getType().getIcon() + " " + item.getType().getLabel());
        typeBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        typeBadge.setForeground(Color.WHITE);
        typeBadge.setOpaque(true);
        typeBadge.setBackground(item.getType() == Item.ItemType.FOUND ? 
            new Color(40, 167, 69) : new Color(220, 53, 69));
        typeBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        titleRow.add(typeBadge);
        
        JLabel titleLabel = new JLabel(truncate(item.getTitle(), 40));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleRow.add(titleLabel);
        
        detailsPanel.add(titleRow);
        detailsPanel.add(Box.createVerticalStrut(5));
        
        // Category and location
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel categoryLabel = new JLabel(item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName());
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(108, 117, 125));
        infoRow.add(categoryLabel);
        
        infoRow.add(new JLabel("  •  "));
        
        String locationText = item.getLocation() != null ? 
            "📍 " + item.getLocation().getBuilding().getName() : "📍 Unknown";
        JLabel locationLabel = new JLabel(locationText);
        locationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        locationLabel.setForeground(new Color(108, 117, 125));
        infoRow.add(locationLabel);
        
        detailsPanel.add(infoRow);
        detailsPanel.add(Box.createVerticalStrut(5));
        
        // Date
        JLabel dateLabel = new JLabel("Reported: " + DATE_FORMAT.format(item.getReportedDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dateLabel.setForeground(new Color(134, 142, 150));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(dateLabel);
        
        detailsPanel.add(Box.createVerticalStrut(8));
        
        // Match reason
        JLabel reasonLabel = new JLabel("💡 " + match.getMatchReason());
        reasonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        reasonLabel.setForeground(getConfidenceColor(score));
        reasonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(reasonLabel);
        
        card.add(detailsPanel, BorderLayout.CENTER);
        
        // Right - Confidence and action
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(130, 0));
        
        // Confidence display
        JLabel percentLabel = new JLabel(percentage + "%");
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        percentLabel.setForeground(getConfidenceColor(score));
        percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(percentLabel);
        
        JLabel confLabel = new JLabel("confidence");
        confLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        confLabel.setForeground(Color.GRAY);
        confLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(confLabel);
        
        // Confidence bar
        JProgressBar confBar = new JProgressBar(0, 100);
        confBar.setValue(percentage);
        confBar.setStringPainted(false);
        confBar.setPreferredSize(new Dimension(100, 8));
        confBar.setMaximumSize(new Dimension(100, 8));
        confBar.setForeground(getConfidenceColor(score));
        confBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(confBar);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Claim button
        JButton claimButton = new JButton("📋 Claim");
        claimButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        claimButton.setBackground(new Color(13, 110, 253));
        claimButton.setForeground(Color.WHITE);
        claimButton.setFocusPainted(false);
        claimButton.setBorderPainted(false);
        claimButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        claimButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        claimButton.setMaximumSize(new Dimension(100, 30));
        claimButton.addActionListener(e -> {
            if (onClaimRequested != null) {
                onClaimRequested.accept(sourceItem, match);
            }
        });
        rightPanel.add(claimButton);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        // Selection handling
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectCard(card, match);
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
    
    private void selectCard(JPanel card, PotentialMatch match) {
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
        selectedMatch = match;
        card.setBackground(CARD_SELECTED_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(13, 110, 253), 2),
            BorderFactory.createEmptyBorder(11, 11, 11, 11)
        ));
        
        // Callback
        if (onMatchSelected != null) {
            onMatchSelected.accept(match);
        }
    }
    
    private JLabel createImageLabel(Item item) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(100, 100));
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
                    Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
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
        label.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        return label;
    }
    
    private Color getConfidenceColor(double score) {
        if (score >= 0.8) return HIGH_MATCH_COLOR;
        if (score >= 0.6) return GOOD_MATCH_COLOR;
        if (score >= 0.4) return MODERATE_MATCH_COLOR;
        return LOW_MATCH_COLOR;
    }
    
    private void showEmptyState(String message) {
        matchesListPanel.removeAll();
        
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setOpaque(false);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        
        JLabel iconLabel = new JLabel("🔍");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(iconLabel);
        
        content.add(Box.createVerticalStrut(15));
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setForeground(new Color(108, 117, 125));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(msgLabel);
        
        emptyPanel.add(content);
        matchesListPanel.add(emptyPanel);
        
        matchesListPanel.revalidate();
        matchesListPanel.repaint();
    }
    
    private void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        refreshButton.setEnabled(!loading);
        sortCombo.setEnabled(!loading);
        confidenceSlider.setEnabled(!loading);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
