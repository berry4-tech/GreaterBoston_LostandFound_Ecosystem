/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.dialogs.ClaimReviewDialog;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Panel showing all items with pending claims for current user
 * Provides centralized claim review access
 * @author aksha
 */
public class ReviewClaimsPanel extends JPanel {
    
    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoClaimDAO claimDAO;
    
    private JList<ItemWithClaims> itemsList;
    private DefaultListModel<ItemWithClaims> itemsModel;
    private JLabel countLabel;
    private JButton refreshButton;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
    
    public ReviewClaimsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.claimDAO = new MongoClaimDAO();
        
        initComponents();
        loadItemsWithClaims();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Items list
        JPanel listPanel = createListPanel();
        add(listPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Title and count
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📋 Pending Claims to Review");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        leftPanel.add(titleLabel);
        
        countLabel = new JLabel("Loading...");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        countLabel.setForeground(Color.GRAY);
        leftPanel.add(countLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Refresh button
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadItemsWithClaims());
        panel.add(refreshButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setBackground(new Color(245, 245, 245));
        
        // Help text
        JLabel helpLabel = new JLabel(
            "<html>Review claims submitted by users for items you found. " +
            "Approve legitimate claims or reject false ones.</html>"
        );
        helpLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        helpLabel.setForeground(Color.GRAY);
        helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(helpLabel, BorderLayout.NORTH);
        
        // Create list
        itemsModel = new DefaultListModel<>();
        itemsList = new JList<>(itemsModel);
        itemsList.setCellRenderer(new ItemWithClaimsRenderer());
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 🔥 ADD: Double-click or Enter to review claims
        itemsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click
                    int index = itemsList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        ItemWithClaims selected = itemsModel.getElementAt(index);
                        openReviewDialog(selected.item);
                    }
                }
            }
        });
        
        // 🔥 ADD: Press Enter to review
        itemsList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    ItemWithClaims selected = itemsList.getSelectedValue();
                    if (selected != null) {
                        openReviewDialog(selected.item);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(itemsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 🔥 ADD: Help text at bottom
        JLabel instructionLabel = new JLabel("Double-click an item to review claims");
        instructionLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        instructionLabel.setForeground(Color.GRAY);
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(instructionLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    public void loadItemsWithClaims() {
        refreshButton.setEnabled(false);
        countLabel.setText("Loading...");
        
        SwingWorker<List<ItemWithClaims>, Void> worker = new SwingWorker<List<ItemWithClaims>, Void>() {
            @Override
            protected List<ItemWithClaims> doInBackground() throws Exception {
                List<ItemWithClaims> itemsWithClaims = new ArrayList<>();
                
                // Get all items reported by current user
                List<Item> userItems = itemDAO.findByUser(currentUser.getEmail());
                
                // Filter for FOUND items only
                for (Item item : userItems) {
                    if (item.getType() == Item.ItemType.FOUND) {
                        // Check if this item has pending claims
                        List<Claim> pendingClaims = claimDAO.findPendingClaimsByItem(item.getItemId());
                        
                        if (!pendingClaims.isEmpty()) {
                            itemsWithClaims.add(new ItemWithClaims(item, pendingClaims));
                        }
                    }
                }
                
                // Sort by most claims first, then by most recent
                itemsWithClaims.sort((a, b) -> {
                    int claimDiff = b.claims.size() - a.claims.size();
                    if (claimDiff != 0) return claimDiff;
                    return b.item.getReportedDate().compareTo(a.item.getReportedDate());
                });
                
                return itemsWithClaims;
            }
            
            @Override
            protected void done() {
                try {
                    List<ItemWithClaims> items = get();
                    itemsModel.clear();
                    
                    for (ItemWithClaims itemWithClaims : items) {
                        itemsModel.addElement(itemWithClaims);
                    }
                    
                    // Update count
                    int totalClaims = items.stream()
                        .mapToInt(i -> i.claims.size())
                        .sum();
                    
                    if (items.isEmpty()) {
                        countLabel.setText("No pending claims");
                    } else {
                        countLabel.setText(totalClaims + " claim" + 
                            (totalClaims != 1 ? "s" : "") + " on " + 
                            items.size() + " item" + (items.size() != 1 ? "s" : ""));
                    }
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ReviewClaimsPanel.this,
                        "Error loading claims: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }
    
    private void openReviewDialog(Item item) {
        ClaimReviewDialog dialog = new ClaimReviewDialog(
            SwingUtilities.getWindowAncestor(this),
            item,
            currentUser
        );
        dialog.setVisible(true);
        
        // Refresh list after reviewing
        loadItemsWithClaims();
    }
    
    public int getTotalPendingClaims() {
        int total = 0;
        for (int i = 0; i < itemsModel.getSize(); i++) {
            total += itemsModel.getElementAt(i).claims.size();
        }
        return total;
    }
    
    // Inner class to hold item and its claims
    private static class ItemWithClaims {
        Item item;
        List<Claim> claims;
        
        ItemWithClaims(Item item, List<Claim> claims) {
            this.item = item;
            this.claims = claims;
        }
    }
    
    // Custom renderer for items with claims
    private class ItemWithClaimsRenderer extends JPanel implements ListCellRenderer<ItemWithClaims> {
        
        private JLabel photoLabel;
        private JLabel titleLabel;
        private JLabel categoryLabel;
        private JLabel locationLabel;
        private JLabel dateLabel;
        private JLabel claimCountLabel;
        private JButton reviewButton;
        private JPanel claimantsPanel;
        
        public ItemWithClaimsRenderer() {
            setLayout(new BorderLayout(15, 0));
            setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            setOpaque(true);
            
            // Photo on the left
            photoLabel = new JLabel();
            photoLabel.setPreferredSize(new Dimension(100, 100));
            photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            photoLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(photoLabel, BorderLayout.WEST);
            
            // Details in center
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setOpaque(false);
            
            // Title
            titleLabel = new JLabel();
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(titleLabel);
            centerPanel.add(Box.createVerticalStrut(5));
            
            // Category and location
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            infoPanel.setOpaque(false);
            infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            categoryLabel = new JLabel();
            categoryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            categoryLabel.setForeground(new Color(100, 100, 100));
            infoPanel.add(categoryLabel);
            
            infoPanel.add(new JLabel("  •  "));
            
            locationLabel = new JLabel();
            locationLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            locationLabel.setForeground(new Color(100, 100, 100));
            infoPanel.add(locationLabel);
            
            centerPanel.add(infoPanel);
            centerPanel.add(Box.createVerticalStrut(5));
            
            // Date
            dateLabel = new JLabel();
            dateLabel.setFont(new Font("Arial", Font.ITALIC, 11));
            dateLabel.setForeground(new Color(120, 120, 120));
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(dateLabel);
            
            centerPanel.add(Box.createVerticalStrut(10));
            
            // Claimants preview
            claimantsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            claimantsPanel.setOpaque(false);
            claimantsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(claimantsPanel);
            
            add(centerPanel, BorderLayout.CENTER);
            
            // Right side - claim count and button
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setOpaque(false);
            
            claimCountLabel = new JLabel();
            claimCountLabel.setFont(new Font("Arial", Font.BOLD, 24));
            claimCountLabel.setForeground(new Color(255, 152, 0));
            claimCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rightPanel.add(claimCountLabel);
            
            JLabel claimsTextLabel = new JLabel("Claims");
            claimsTextLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            claimsTextLabel.setForeground(Color.GRAY);
            claimsTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rightPanel.add(claimsTextLabel);
            
            rightPanel.add(Box.createVerticalStrut(10));
            
            reviewButton = new JButton("Review");
            reviewButton.setBackground(new Color(255, 152, 0));
            reviewButton.setForeground(Color.WHITE);
            reviewButton.setFont(new Font("Arial", Font.BOLD, 12));
            reviewButton.setFocusPainted(false);
            reviewButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            reviewButton.setMaximumSize(new Dimension(100, 35));
            rightPanel.add(reviewButton);
            
            add(rightPanel, BorderLayout.EAST);
        }
        
        @Override
        public Component getListCellRendererComponent(
                JList<? extends ItemWithClaims> list,
                ItemWithClaims itemWithClaims,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            
            Item item = itemWithClaims.item;
            List<Claim> claims = itemWithClaims.claims;
            
            // Set background
            if (isSelected) {
                setBackground(new Color(230, 240, 250));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 150, 200), 2),
                    BorderFactory.createEmptyBorder(13, 13, 13, 13)
                ));
            } else {
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)
                ));
            }
            
            // Load photo
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                String imagePath = item.getImagePaths().get(0);
                java.io.File imageFile = new java.io.File(System.getProperty("user.dir"), imagePath);
                
                if (imageFile.exists()) {
                    try {
                        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                        Image img = icon.getImage();
                        Image scaled = img.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                        photoLabel.setIcon(new ImageIcon(scaled));
                        photoLabel.setText("");
                    } catch (Exception e) {
                        photoLabel.setText(item.getCategory().getEmoji());
                        photoLabel.setFont(new Font("Arial", Font.PLAIN, 40));
                        photoLabel.setIcon(null);
                    }
                } else {
                    photoLabel.setText(item.getCategory().getEmoji());
                    photoLabel.setFont(new Font("Arial", Font.PLAIN, 40));
                    photoLabel.setIcon(null);
                }
            } else {
                photoLabel.setText(item.getCategory().getEmoji());
                photoLabel.setFont(new Font("Arial", Font.PLAIN, 40));
                photoLabel.setIcon(null);
            }
            
            // Set title
            String title = item.getTitle();
            if (title.length() > 35) {
                title = title.substring(0, 32) + "...";
            }
            titleLabel.setText(title);
            
            // Set category
            categoryLabel.setText(item.getCategory().getEmoji() + " " + 
                item.getCategory().getDisplayName());
            
            // Set location
            locationLabel.setText("📍 " + item.getLocation().getBuilding().getCode());
            
            // Set date
            dateLabel.setText("Posted: " + new SimpleDateFormat("MMM dd, yyyy").format(item.getReportedDate()));
            
            // Set claim count
            claimCountLabel.setText(String.valueOf(claims.size()));
            
            // Set color based on urgency
            if (claims.size() >= 3) {
                claimCountLabel.setForeground(new Color(244, 67, 54)); // Red - urgent
            } else if (claims.size() >= 2) {
                claimCountLabel.setForeground(new Color(255, 152, 0)); // Orange
            } else {
                claimCountLabel.setForeground(new Color(33, 150, 243)); // Blue
            }
            
            // Update claimants preview
            claimantsPanel.removeAll();
            
            JLabel claimantsLabel = new JLabel("Claimants: ");
            claimantsLabel.setFont(new Font("Arial", Font.BOLD, 11));
            claimantsLabel.setForeground(Color.GRAY);
            claimantsPanel.add(claimantsLabel);
            
            // Show up to 3 claimants
            int displayCount = Math.min(3, claims.size());
            for (int i = 0; i < displayCount; i++) {
                Claim claim = claims.get(i);
                JLabel claimantLabel = new JLabel(claim.getClaimant().getFirstName());
                claimantLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                
                // Color code by trust score
                double trust = claim.getClaimant().getTrustScore();
                if (trust < 50) {
                    claimantLabel.setForeground(Color.RED);
                } else if (trust < 75) {
                    claimantLabel.setForeground(Color.ORANGE);
                } else {
                    claimantLabel.setForeground(new Color(0, 150, 0));
                }
                
                claimantsPanel.add(claimantLabel);
                
                if (i < displayCount - 1) {
                    claimantsPanel.add(new JLabel(", "));
                }
            }
            
            if (claims.size() > 3) {
                JLabel moreLabel = new JLabel("+" + (claims.size() - 3) + " more");
                moreLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                moreLabel.setForeground(Color.GRAY);
                claimantsPanel.add(moreLabel);
            }
            
            // Review button action
            for (java.awt.event.ActionListener al : reviewButton.getActionListeners()) {
                reviewButton.removeActionListener(al);
            }
            reviewButton.addActionListener(e -> openReviewDialog(item));
            
            return this;
        }
    }
    
}
