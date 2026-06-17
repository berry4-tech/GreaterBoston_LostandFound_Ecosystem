/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui.dialogs;

import com.campus.lostfound.dao.MongoClaimDAO;
import com.campus.lostfound.dao.MongoMessageDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.Message;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.utils.ImageHandler;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 *
 * @author aksha
 */
public class ItemDetailDialog extends JDialog {

    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 72);
    private static final Font EMOJI_FONT_MEDIUM = UIConstants.getEmojiFont(Font.PLAIN, 18);

    private Item item;
    private User currentUser;
    private MongoClaimDAO claimDAO;
    private MongoUserDAO userDAO; // 🔥 Add this to reload reporter's current trust score
    private JLabel photoLabel;
    private JTextArea descriptionArea;
    private JProgressBar reporterTrustBar; // 🔥 Store reference to update it
    private JButton claimButton;
    private JButton messageButton;
    private JButton reportButton;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");

    public ItemDetailDialog(Window owner, Item item, User currentUser) {
        super(owner, "Item Details - " + item.getTitle(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.currentUser = currentUser;
        this.claimDAO = new MongoClaimDAO();
        this.userDAO = new MongoUserDAO(); // 🔥 Initialize

        initComponents();
        loadItemData();

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header panel with title and status
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side - Photo
        JPanel photoPanel = createPhotoPanel();
        contentPanel.add(photoPanel, BorderLayout.WEST);

        // Center - Details
        JPanel detailsPanel = createDetailsPanel();
        contentPanel.add(detailsPanel, BorderLayout.CENTER);

        add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        // Bottom action panel
        JPanel actionPanel = createActionPanel();
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Title and type
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titlePanel.add(titleLabel);

        JLabel typeLabel = new JLabel(item.getType().getIcon() + " " + item.getType().getLabel());
        typeLabel.setFont(EMOJI_FONT);
        if (item.getType() == Item.ItemType.LOST) {
            typeLabel.setForeground(new Color(200, 50, 50));
        } else {
            typeLabel.setForeground(new Color(50, 150, 50));
        }
        titlePanel.add(typeLabel);

        panel.add(titlePanel, BorderLayout.WEST);

        // Status badge
        JLabel statusLabel = new JLabel(item.getStatus().getLabel());
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.decode(item.getStatus().getColorCode()));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode(item.getStatus().getColorCode())),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        panel.add(statusLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createPhotoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(250, 250));

        photoLabel = new JLabel();
        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        photoLabel.setBackground(Color.WHITE);
        photoLabel.setOpaque(true);

        boolean imageLoaded = false;

        // Try to load image if present
        if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
            String imagePath = item.getImagePaths().get(0);
            System.out.println("Attempting to load image: " + imagePath);

            // NEW simple loading method (replaces ImageHandler)
            File imageFile = new File(System.getProperty("user.dir"), imagePath);

            if (imageFile.exists()) {
                try {
                    ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());

                    Image img = icon.getImage();
                    Image scaled = img.getScaledInstance(240, 240, Image.SCALE_DEFAULT);

                    photoLabel.setIcon(new ImageIcon(scaled));
                    photoLabel.setText("");
                    imageLoaded = true;

                    System.out.println("Image loaded successfully (simple loader).");

                } catch (Exception e) {
                    System.err.println("Error loading image: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Image file does NOT exist at: " + imageFile.getAbsolutePath());
            }
        }

        // If no image or loading failed, show placeholder
        if (!imageLoaded) {
            if (item.getImagePaths() != null && !item.getImagePaths().isEmpty()) {
                // Image path exists but couldn't load
                photoLabel.setText(
                        "<html><center>📷<br>Image loading error<br><small>File exists but couldn't display</small></center></html>"
                );
                photoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            } else {
                // No image uploaded → show category emoji
                photoLabel.setText(item.getCategory().getEmoji());
                photoLabel.setFont(EMOJI_FONT_LARGE);
            }
        }

        panel.add(photoLabel, BorderLayout.CENTER);

        // Debug button removed for production
        return panel;
    }


    /*// Add this temporary test method to check image paths:
    public static void debugImagePaths() {
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        File imgDir = new File("uploaded_images");
        System.out.println("Image Directory: " + imgDir.getAbsolutePath());
        System.out.println("Image Directory Exists: " + imgDir.exists());

        if (imgDir.exists()) {
            File[] files = imgDir.listFiles();
            System.out.println("Files in image directory: " + (files != null ? files.length : 0));
            if (files != null) {
                for (File f : files) {
                    System.out.println(" - " + f.getName());
                }
            }
        }
    }*/
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // Category
        panel.add(createDetailRow("Category:",
                item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName(), true));
        panel.add(Box.createVerticalStrut(10));

        // Location
        panel.add(createDetailRow("Location:",
                "📍 " + item.getLocation().getFullLocation(), true));
        panel.add(Box.createVerticalStrut(10));

        // Date reported
        panel.add(createDetailRow("Reported:",
                DATE_FORMAT.format(item.getReportedDate())));
        panel.add(Box.createVerticalStrut(10));

        // Reported by (partially hidden for privacy)
        String reporterName = item.getReportedBy().getFirstName() + " "
                + item.getReportedBy().getLastName().charAt(0) + ".";
        panel.add(createDetailRow("Reported by:", reporterName));

        // Trust score indicator - Store reference for updates
        JPanel trustPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        trustPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trustPanel.add(new JLabel("Trust Score:"));

        reporterTrustBar = new JProgressBar(0, 100);
        reporterTrustBar.setValue((int) item.getReportedBy().getTrustScore());
        reporterTrustBar.setPreferredSize(new Dimension(100, 15));
        reporterTrustBar.setStringPainted(true);
        trustPanel.add(reporterTrustBar);

        panel.add(trustPanel);
        panel.add(Box.createVerticalStrut(15));

        // Description
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Arial", Font.BOLD, 12));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(descLabel);
        panel.add(Box.createVerticalStrut(5));

        descriptionArea = new JTextArea(item.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(new Color(250, 250, 250));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(descriptionArea);

        // Additional details if available
        if (item.getPrimaryColor() != null || item.getBrand() != null) {
            panel.add(Box.createVerticalStrut(15));
            JLabel additionalLabel = new JLabel("Additional Details:");
            additionalLabel.setFont(new Font("Arial", Font.BOLD, 12));
            additionalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(additionalLabel);
            panel.add(Box.createVerticalStrut(5));

            if (item.getPrimaryColor() != null) {
                panel.add(createDetailRow("Color:", item.getPrimaryColor()));
            }
            if (item.getBrand() != null) {
                panel.add(createDetailRow("Brand:", item.getBrand()));
            }
        }

        // Match score if available
        if (item.getMatchScore() > 0) {
            panel.add(Box.createVerticalStrut(15));
            JPanel matchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            matchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            matchPanel.setBackground(new Color(230, 240, 250));
            matchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel matchLabel = new JLabel("Match Confidence: ");
            matchLabel.setFont(new Font("Arial", Font.BOLD, 14));
            matchPanel.add(matchLabel);

            int percentage = (int) (item.getMatchScore() * 100);
            JLabel scoreLabel = new JLabel(percentage + "%");
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));

            if (percentage >= 80) {
                scoreLabel.setForeground(new Color(0, 150, 0));
            } else if (percentage >= 60) {
                scoreLabel.setForeground(new Color(200, 150, 0));
            } else {
                scoreLabel.setForeground(new Color(150, 150, 150));
            }

            matchPanel.add(scoreLabel);
            panel.add(matchPanel);
        }

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createDetailRow(String label, String value) {
        return createDetailRow(label, value, false);
    }
    
    private JPanel createDetailRow(String label, String value, boolean useEmojiFont) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.BOLD, 12));
        row.add(labelComp);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(useEmojiFont ? EMOJI_FONT : new Font("Arial", Font.PLAIN, 12));
        row.add(valueComp);

        return row;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 15, 15, 15) // 🔥 Increased padding for better visibility
        ));
        panel.setPreferredSize(new Dimension(700, 80)); // 🔥 Set minimum height for button visibility

        // Determine which buttons to show based on item status and ownership
        boolean isOwner = item.getReportedBy().equals(currentUser);
        boolean canClaim = !isOwner
                && item.getType() == Item.ItemType.FOUND
                && item.getStatus() == Item.ItemStatus.OPEN;

        if (canClaim) {
            claimButton = new JButton("This is Mine!");
            claimButton.setPreferredSize(new Dimension(140, 40)); // 🔥 Set explicit size
            claimButton.setBackground(new Color(94, 242, 118));
            claimButton.setForeground(Color.BLACK);
            claimButton.setFont(new Font("Arial", Font.BOLD, 14));
            claimButton.setFocusPainted(false);
            claimButton.addActionListener(e -> openClaimWizard());
            panel.add(claimButton);
        }

        // Add Review Claims button for owners of FOUND items
        if (isOwner && item.getType() == Item.ItemType.FOUND) {
            SwingWorker<Long, Void> worker = new SwingWorker<Long, Void>() {
                @Override
                protected Long doInBackground() throws Exception {
                    return claimDAO.countPendingClaims(item.getItemId());
                }

                @Override
                protected void done() {
                    try {
                        long pendingCount = get();
                        if (pendingCount > 0) {
                            JButton reviewButton = new JButton("📋 Review Claims (" + pendingCount + ")");
                            reviewButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
                            reviewButton.setPreferredSize(new Dimension(180, 40)); // 🔥 Set explicit size
                            reviewButton.setBackground(new Color(255, 152, 0));
                            reviewButton.setForeground(Color.WHITE);
                            reviewButton.setFocusPainted(false);
                            reviewButton.addActionListener(e -> openClaimReview());

                            panel.add(reviewButton, 0);
                            panel.revalidate();
                            panel.repaint();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            worker.execute();
        }

        if (!isOwner) {
            messageButton = new JButton("Message Finder");
            messageButton.setPreferredSize(new Dimension(140, 40)); // 🔥 Set explicit size
            messageButton.addActionListener(e -> openMessageDialog());
            panel.add(messageButton);
        }

        if (isOwner && item.getStatus() == Item.ItemStatus.OPEN) {
            JButton editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(100, 40)); // 🔥 Set explicit size
            editButton.addActionListener(e -> editItem());
            panel.add(editButton);

            JButton markFoundButton = new JButton("Mark as Found");
            markFoundButton.setPreferredSize(new Dimension(140, 40)); // 🔥 Set explicit size
            markFoundButton.setBackground(new Color(33, 150, 243));
            markFoundButton.setForeground(Color.WHITE);
            markFoundButton.addActionListener(e -> markAsFound());
            panel.add(markFoundButton);
        }

        reportButton = new JButton("Report Issue");
        reportButton.setPreferredSize(new Dimension(120, 40)); // 🔥 Set explicit size
        reportButton.addActionListener(e -> reportIssue());
        panel.add(reportButton);

        JButton closeButton = new JButton("Close");
        closeButton.setPreferredSize(new Dimension(100, 40)); // 🔥 Set explicit size
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        return panel;
    }

    private void loadItemData() {
        // 🔥 Reload reporter's CURRENT trust score from database (not embedded stale data)
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                // Fetch reporter's latest data from users collection
                Optional<User> reporterOpt = userDAO.findByEmail(item.getReportedBy().getEmail());
                return reporterOpt.orElse(item.getReportedBy());
            }

            @Override
            protected void done() {
                try {
                    User currentReporter = get();
                    // Update the item's reporter reference with current data
                    item.setReportedBy(currentReporter);

                    // Update the trust score display in the details panel
                    updateTrustScoreDisplay(currentReporter.getTrustScore());

                } catch (Exception e) {
                    System.err.println("Error loading reporter's current trust score: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openClaimWizard() {
        // Check if user is suspended
        if (!currentUser.canSubmitClaims()) {
            JOptionPane.showMessageDialog(this,
                    "❌ ACCOUNT SUSPENDED\n\n"
                    + "Your account is suspended due to low trust score.\n"
                    + "Trust Score: " + String.format("%.0f", currentUser.getTrustScore()) + "/100\n"
                    + "False Claims: " + currentUser.getFalseClaims() + "\n\n"
                    + "You cannot submit claims until your trust score improves.\n\n"
                    + "To appeal, contact an administrator.",
                    "Cannot Submit Claim - Account Suspended",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if user can claim this specific item category
        if (!currentUser.canClaimItem(item.getCategory())) {
            String categoryName = item.getCategory().getDisplayName();
            String trustLevel = currentUser.getTrustLevel();

            JOptionPane.showMessageDialog(this,
                    "⚠️ CLAIMING RESTRICTED\n\n"
                    + "Your trust score is too low to claim this type of item.\n\n"
                    + "Item Category: " + categoryName + "\n"
                    + "Your Trust Score: " + String.format("%.0f", currentUser.getTrustScore()) + "/100\n"
                    + "Trust Level: " + trustLevel + "\n"
                    + "Required: 75+ for high-value items\n\n"
                    + "You can improve your trust score by:\n"
                    + "✓ Reporting found items (+2 points each)\n"
                    + "✓ Successfully returning claimed items (+10 points)\n"
                    + "✗ Avoid false claims (-25 points)\n\n"
                    + (currentUser.getTrustScore() < 50
                    ? "Current restrictions: Can only claim books, umbrellas, bottles, clothing"
                    : "Current restrictions: Cannot claim electronics, jewelry, IDs, keys"),
                    "Claiming Restricted",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show warning if requires admin approval
        if (currentUser.requiresAdminApproval()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "⚠️ ADMIN APPROVAL REQUIRED\n\n"
                    + "Due to your trust score, this claim will require admin approval.\n"
                    + "Trust Score: " + String.format("%.0f", currentUser.getTrustScore()) + "/100\n\n"
                    + "Do you want to proceed?",
                    "Admin Approval Required",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Proceed with claim wizard
        ClaimWizardDialog wizard = new ClaimWizardDialog(this, item, currentUser);
        wizard.setVisible(true);

        if (wizard.isClaimSubmitted()) {
            item.setStatus(Item.ItemStatus.PENDING_CLAIM);

            String message = "Your claim has been submitted!\n\n";

            if (currentUser.requiresAdminApproval()) {
                message += "⚠️ This claim requires admin approval due to your trust score.\n";
            }

            message += "The finder will review your claim and contact you.\n"
                    + "You'll be notified of their decision.";

            JOptionPane.showMessageDialog(this,
                    message,
                    "Claim Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    private void openMessageDialog() {
        JTextArea messageArea = new JTextArea(5, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(messageArea);

        int result = JOptionPane.showConfirmDialog(this,
                scrollPane,
                "Send Message to " + item.getReportedBy().getFirstName(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !messageArea.getText().trim().isEmpty()) {
            // Send actual message using DAO
            MongoMessageDAO messageDAO = new MongoMessageDAO();
            Message msg = new Message(
                    item.getMongoId(),
                    currentUser.getEmail(),
                    item.getReportedBy().getEmail(),
                    messageArea.getText().trim()
            );

            String messageId = messageDAO.sendMessage(msg);
            if (messageId != null) {
                JOptionPane.showMessageDialog(this,
                        "Message sent successfully!",
                        "Message Sent",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to send message",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editItem() {
        // Open edit dialog
        JOptionPane.showMessageDialog(this,
                "Edit Item dialog would open here",
                "Edit Item",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void markAsFound() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to mark this item as found?\n"
                + "This will close the listing.",
                "Mark as Found",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            item.setStatus(Item.ItemStatus.CLAIMED);
            item.setResolvedDate(new Date());
            JOptionPane.showMessageDialog(this,
                    "Great! The item has been marked as found.",
                    "Item Found",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    private void reportIssue() {
        String[] options = {"Incorrect Information", "Suspicious Activity", "Inappropriate Content", "Other"};
        String choice = (String) JOptionPane.showInputDialog(this,
                "What issue would you like to report?",
                "Report Issue",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice != null) {
            JOptionPane.showMessageDialog(this,
                    "Thank you for your report. An administrator will review it.",
                    "Issue Reported",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openClaimReview() {
        ClaimReviewDialog reviewDialog = new ClaimReviewDialog(this, item, currentUser);
        reviewDialog.setVisible(true);

        // After reviewing, you might want to refresh or close this dialog
    }

    /**
     * Update the reporter's trust score display with current value
     */
    private void updateTrustScoreDisplay(double newTrustScore) {
        if (reporterTrustBar != null) {
            reporterTrustBar.setValue((int) newTrustScore);
            reporterTrustBar.repaint();
            System.out.println("✓ ItemDetailDialog trust score updated: " + (int) newTrustScore + "%");
        }
    }
}
