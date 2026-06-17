/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui.components;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.utils.ImageHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author aksha
 */
public class ItemListRenderer extends JPanel implements ListCellRenderer<Item> {

    private static final Color SELECTED_BG = new Color(230, 240, 250);
    private static final Color HOVER_BG = new Color(245, 245, 245);
    private static final Color DEFAULT_BG = Color.WHITE;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final Map<String, ImageIcon> imageCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 12);
    private static final Font EMOJI_FONT_SMALL = UIConstants.getEmojiFont(Font.PLAIN, 11);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 30);

    private JLabel photoLabel;
    private JLabel titleLabel;
    private JLabel typeLabel;
    private JLabel categoryLabel;
    private JLabel locationLabel;
    private JLabel dateLabel;
    private JLabel statusLabel;
    private JLabel matchScoreLabel;
    private JPanel detailsPanel;
    private JPanel topPanel;

    public ItemListRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(true);

        // Photo on the left
        photoLabel = new JLabel();
        photoLabel.setPreferredSize(new Dimension(80, 80));
        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(photoLabel, BorderLayout.WEST);

        // Details panel in center
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);

        // Top row - Title and Type
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topPanel.setOpaque(false);

        titleLabel = new JLabel();
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(titleLabel);

        typeLabel = new JLabel();
        typeLabel.setFont(EMOJI_FONT_SMALL);
        topPanel.add(typeLabel);

        detailsPanel.add(topPanel);

        // Category and location
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        middlePanel.setOpaque(false);

        categoryLabel = new JLabel();
        categoryLabel.setFont(EMOJI_FONT);
        categoryLabel.setForeground(new Color(100, 100, 100));
        middlePanel.add(categoryLabel);

        middlePanel.add(new JLabel("  •  ")); // Separator

        locationLabel = new JLabel();
        locationLabel.setFont(EMOJI_FONT);
        locationLabel.setForeground(new Color(100, 100, 100));
        middlePanel.add(locationLabel);

        detailsPanel.add(middlePanel);

        // Bottom row - Date and Status
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bottomPanel.setOpaque(false);

        dateLabel = new JLabel();
        dateLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 11));
        dateLabel.setForeground(new Color(120, 120, 120));
        bottomPanel.add(dateLabel);

        bottomPanel.add(new JLabel("  •  ")); // Separator

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Arial", Font.BOLD, 11));
        bottomPanel.add(statusLabel);

        detailsPanel.add(bottomPanel);

        add(detailsPanel, BorderLayout.CENTER);

        // Match score on the right (if applicable)
        matchScoreLabel = new JLabel();
        matchScoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        matchScoreLabel.setPreferredSize(new Dimension(60, 30));
        matchScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(matchScoreLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Item> list,
            Item item,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        // Set background
        if (isSelected) {
            setBackground(SELECTED_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 150, 200), 2),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
        } else {
            setBackground(DEFAULT_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(9, 9, 9, 9)
            ));
        }

        // Load and scale photo
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            String imagePath = item.getImagePaths().get(0);

            // Use ImageHandler to load
            ImageIcon icon = imageCache.get(imagePath);
            //ImageIcon icon = ImageHandler.loadImage(imagePath, 80, 80);

            if (icon == null) {
                // 2. Not in cache → try to load (using your ImageHandler or whatever you use)
                icon = ImageHandler.loadImage(imagePath, 80, 80);

                if (icon != null && icon.getIconWidth() > 0) {
                    // 3. Put into cache (with size cap)
                    if (imageCache.size() < MAX_CACHE_SIZE) {
                        imageCache.put(imagePath, icon);
                    }
                    photoLabel.setIcon(icon);
                    photoLabel.setText("");
                } else {
                    // Failed to load → show placeholder
                    showPlaceholder(item);
                }
            } else {
                // 4. Use cached image
                photoLabel.setIcon(icon);
                photoLabel.setText("");
            }
        } else {
            // No image paths → placeholder
            showPlaceholder(item);
        }

        // Set title with truncation if needed
        String title = item.getTitle();
        if (title.length() > 30) {
            title = title.substring(0, 27) + "...";
        }
        titleLabel.setText(title);

        // Set type with colored badge (with null safety)
        if (item.getType() != null) {
            typeLabel.setText(item.getType().getIcon() + " " + item.getType().getLabel());
            if (item.getType() == Item.ItemType.LOST) {
                typeLabel.setForeground(new Color(200, 50, 50));
            } else {
                typeLabel.setForeground(new Color(50, 150, 50));
            }
        } else {
            typeLabel.setText("Unknown Type");
            typeLabel.setForeground(Color.GRAY);
        }

        // Set category (with null safety)
        if (item.getCategory() != null) {
            categoryLabel.setText(item.getCategory().getDisplayName());
        } else {
            categoryLabel.setText("Unknown Category");
        }

        // Set location (with null safety)
        if (item.getLocation() != null && item.getLocation().getBuilding() != null) {
            locationLabel.setText("📍 " + item.getLocation().getBuilding().getCode());
        } else {
            locationLabel.setText("📍 Unknown Location");
        }

        // Set date (with null safety)
        if (item.getReportedDate() != null) {
            dateLabel.setText("🕐 " + DATE_FORMAT.format(item.getReportedDate()));
        } else {
            dateLabel.setText("🕐 Unknown Date");
        }

        // Set status with color (with null safety)
        if (item.getStatus() != null) {
            statusLabel.setText(item.getStatus().getLabel());
            statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        } else {
            statusLabel.setText("Unknown");
            statusLabel.setForeground(Color.GRAY);
        }

        // Set match score if available
        if (item.getMatchScore() > 0) {
            int percentage = (int) (item.getMatchScore() * 100);
            matchScoreLabel.setText(percentage + "%");
            matchScoreLabel.setVisible(true);

            // Color code based on score
            if (percentage >= 80) {
                matchScoreLabel.setForeground(new Color(0, 150, 0));
            } else if (percentage >= 60) {
                matchScoreLabel.setForeground(new Color(200, 150, 0));
            } else {
                matchScoreLabel.setForeground(new Color(150, 150, 150));
            }
        } else {
            matchScoreLabel.setVisible(false);
        }

        return this;
    }

    private void showPlaceholder(Item item) {
        if (item.getCategory() != null) {
            photoLabel.setText(item.getCategory().getEmoji());
        } else {
            photoLabel.setText("📦"); // Default package emoji
        }
        photoLabel.setFont(EMOJI_FONT_LARGE);
        photoLabel.setIcon(null);
    }

    private ImageIcon loadAndScaleImage(String path, int width, int height) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            // Return default icon if image can't be loaded
            return new ImageIcon();
        }
    }
}
