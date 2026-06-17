package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.services.ItemMatcher;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;
import com.campus.lostfound.ui.components.ItemMatchResultsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Test class for ItemMatchResultsPanel component.
 * Demonstrates usage and verifies functionality.
 * 
 * @author Developer 2 - UI Components
 */
public class ItemMatchResultsPanelTest {
    
    public static void main(String[] args) {
        System.out.println("=" .repeat(60));
        System.out.println("ItemMatchResultsPanel Component Test");
        System.out.println("=" .repeat(60));
        
        // Run tests
        testItemMatching();
        testUIDisplay();
    }
    
    /**
     * Test 1: Item Matching Logic
     */
    private static void testItemMatching() {
        System.out.println("\n[TEST 1] Item Matching Logic");
        System.out.println("-".repeat(40));
        
        try {
            MongoItemDAO itemDAO = new MongoItemDAO();
            ItemMatcher matcher = new ItemMatcher();
            
            List<Item> allItems = itemDAO.findAll();
            System.out.println("✓ Total items in database: " + allItems.size());
            
            if (allItems.isEmpty()) {
                System.out.println("⚠ No items found. Run MongoDataGenerator first.");
                return;
            }
            
            // Count by type
            long lostCount = allItems.stream()
                .filter(i -> i.getType() == Item.ItemType.LOST)
                .count();
            long foundCount = allItems.stream()
                .filter(i -> i.getType() == Item.ItemType.FOUND)
                .count();
            
            System.out.println("  - Lost items: " + lostCount);
            System.out.println("  - Found items: " + foundCount);
            
            // Find a LOST item to test matching
            Optional<Item> lostItem = allItems.stream()
                .filter(i -> i.getType() == Item.ItemType.LOST)
                .findFirst();
            
            if (lostItem.isPresent()) {
                Item testItem = lostItem.get();
                System.out.println("\n✓ Testing matches for: " + testItem.getTitle());
                System.out.println("  Category: " + testItem.getCategory());
                
                List<PotentialMatch> matches = matcher.findMatches(testItem, allItems);
                System.out.println("  Found " + matches.size() + " potential matches");
                
                // Show top 3 matches
                int showCount = Math.min(3, matches.size());
                for (int i = 0; i < showCount; i++) {
                    PotentialMatch match = matches.get(i);
                    int percent = (int) (match.getScore() * 100);
                    System.out.println("    " + (i + 1) + ". " + match.getItem().getTitle() + 
                        " (" + percent + "% - " + match.getMatchReason() + ")");
                }
            } else {
                System.out.println("⚠ No LOST items found to test matching");
            }
            
            System.out.println("\n✓ TEST 1 PASSED");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 1 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 2: UI Display (Interactive)
     */
    private static void testUIDisplay() {
        System.out.println("\n[TEST 2] UI Display (Interactive)");
        System.out.println("-".repeat(40));
        
        try {
            MongoUserDAO userDAO = new MongoUserDAO();
            MongoItemDAO itemDAO = new MongoItemDAO();
            
            List<User> users = userDAO.findAll();
            List<Item> items = itemDAO.findAll();
            
            if (users.isEmpty()) {
                System.out.println("❌ No users found.");
                return;
            }
            
            if (items.isEmpty()) {
                System.out.println("❌ No items found.");
                return;
            }
            
            User testUser = users.get(0);
            System.out.println("✓ Using test user: " + testUser.getFullName());
            
            // Find a LOST item to use as source
            Optional<Item> lostItem = items.stream()
                .filter(i -> i.getType() == Item.ItemType.LOST)
                .findFirst();
            
            if (lostItem.isEmpty()) {
                System.out.println("⚠ No LOST items found. Using first item.");
            }
            
            Item sourceItem = lostItem.orElse(items.get(0));
            System.out.println("✓ Source item: " + sourceItem.getTitle());
            
            // Launch UI
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Use default
                }
                
                // Create frame
                JFrame frame = new JFrame("ItemMatchResultsPanel Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(900, 700);
                
                // Create panel
                ItemMatchResultsPanel matchPanel = new ItemMatchResultsPanel(testUser);
                
                // Set callbacks
                matchPanel.setOnMatchSelected(match -> {
                    System.out.println("Selected: " + match.getItem().getTitle() + 
                        " (" + (int)(match.getScore() * 100) + "%)");
                });
                
                matchPanel.setOnClaimRequested((source, match) -> {
                    JOptionPane.showMessageDialog(frame,
                        "Creating claim request:\n\n" +
                        "Source Item: " + source.getTitle() + "\n" +
                        "Matched Item: " + match.getItem().getTitle() + "\n" +
                        "Confidence: " + (int)(match.getScore() * 100) + "%\n\n" +
                        "This would open the claim wizard...",
                        "Create Claim Request",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                // Top panel with item selector
                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
                topPanel.setBackground(new Color(52, 73, 94));
                
                JLabel selectLabel = new JLabel("Select source item:");
                selectLabel.setForeground(Color.WHITE);
                selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                topPanel.add(selectLabel);
                
                // Item dropdown
                JComboBox<Item> itemCombo = new JComboBox<>();
                itemCombo.setPreferredSize(new Dimension(400, 30));
                itemCombo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, 
                            int index, boolean isSelected, boolean cellHasFocus) {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof Item item) {
                            setText(item.getType().getIcon() + " " + item.getTitle() + 
                                " (" + item.getCategory().getDisplayName() + ")");
                        }
                        return this;
                    }
                });
                
                // Add items to combo
                for (Item item : items) {
                    itemCombo.addItem(item);
                }
                
                itemCombo.addActionListener(e -> {
                    Item selected = (Item) itemCombo.getSelectedItem();
                    if (selected != null) {
                        matchPanel.findMatchesFor(selected);
                    }
                });
                
                topPanel.add(itemCombo);
                
                // Find matches button
                JButton findButton = new JButton("🔍 Find Matches");
                findButton.setBackground(new Color(40, 167, 69));
                findButton.setForeground(Color.WHITE);
                findButton.setFocusPainted(false);
                findButton.addActionListener(e -> {
                    Item selected = (Item) itemCombo.getSelectedItem();
                    if (selected != null) {
                        matchPanel.findMatchesFor(selected);
                    }
                });
                topPanel.add(findButton);
                
                // Layout
                frame.setLayout(new BorderLayout());
                frame.add(topPanel, BorderLayout.NORTH);
                frame.add(matchPanel, BorderLayout.CENTER);
                
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                
                // Auto-find matches for first item
                if (itemCombo.getItemCount() > 0) {
                    itemCombo.setSelectedIndex(0);
                    matchPanel.findMatchesFor((Item) itemCombo.getSelectedItem());
                }
                
                System.out.println("✓ UI window opened.");
                System.out.println("  → Select different items from dropdown to test matching");
                System.out.println("  → Click on match cards to select");
                System.out.println("  → Use 'Claim' button to test claim callback");
                System.out.println("  → Adjust confidence slider to filter results");
            });
            
            System.out.println("✓ TEST 2 LAUNCHED (check UI window)");
            
        } catch (Exception e) {
            System.out.println("❌ TEST 2 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
