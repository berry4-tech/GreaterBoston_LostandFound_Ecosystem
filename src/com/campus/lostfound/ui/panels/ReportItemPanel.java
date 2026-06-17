/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.utils.ImageHandler;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 *
 * @author aksha
 */
public class ReportItemPanel extends JPanel {

    private User currentUser;
    private MongoItemDAO itemDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoUserDAO userDAO;

    // Form components
    private JRadioButton lostRadio;
    private JRadioButton foundRadio;
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<Item.ItemCategory> categoryCombo;
    private JComboBox<Building> buildingCombo;
    private JTextField roomField;
    private JTextField locationDetailsField;
    private JTextField colorField;
    private JTextField brandField;
    private JLabel imageLabel;
    private List<String> imagePaths;
    private JButton submitButton;
    private JButton clearButton;

    public ReportItemPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.userDAO = new MongoUserDAO();
        this.imagePaths = new ArrayList<>();

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Form panel with scroll
        JPanel formPanel = createFormPanel();
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Report Lost or Found Item");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.WEST);

        JLabel helpLabel = new JLabel("Fill out the form below to report an item");
        helpLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        helpLabel.setForeground(Color.GRAY);
        panel.add(helpLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Item Type Selection
        JPanel typePanel = new JPanel();
        typePanel.setOpaque(false);
        typePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        typePanel.setBorder(BorderFactory.createTitledBorder("Item Type *"));

        ButtonGroup typeGroup = new ButtonGroup();
        lostRadio = new JRadioButton("❌ I Lost Something");
        foundRadio = new JRadioButton("✅ I Found Something");
        lostRadio.setSelected(true);
        lostRadio.setOpaque(false);
        foundRadio.setOpaque(false);

        typeGroup.add(lostRadio);
        typeGroup.add(foundRadio);
        typePanel.add(lostRadio);
        typePanel.add(foundRadio);
        typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(typePanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Two column layout for the rest
        JPanel columnsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        columnsPanel.setOpaque(false);
        columnsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        columnsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));

        // Left Column
        JPanel leftColumn = new JPanel();
        leftColumn.setOpaque(false);
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));

        // Title
        leftColumn.add(createLabel("Item Title *"));
        titleField = createTextField();
        titleField.setToolTipText("Brief description (e.g., 'Black iPhone 14 Pro')");
        leftColumn.add(titleField);
        leftColumn.add(Box.createVerticalStrut(15));

        // Description
        leftColumn.add(createLabel("Detailed Description *"));
        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        descriptionArea.setToolTipText("Include unique features, damage, stickers, etc.");
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(descScroll);
        leftColumn.add(Box.createVerticalStrut(15));

        // Category
        leftColumn.add(createLabel("Category *"));
        categoryCombo = new JComboBox<>(Item.ItemCategory.values());
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    Item.ItemCategory cat = (Item.ItemCategory) value;
                    setText(cat.getEmoji() + " " + cat.getDisplayName());
                }
                return this;
            }
        });
        categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        categoryCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(categoryCombo);
        leftColumn.add(Box.createVerticalStrut(15));

        // Color
        leftColumn.add(createLabel("Primary Color"));
        colorField = createTextField();
        colorField.setToolTipText("Main color of the item");
        leftColumn.add(colorField);
        leftColumn.add(Box.createVerticalStrut(15));

        // Brand
        leftColumn.add(createLabel("Brand"));
        brandField = createTextField();
        brandField.setToolTipText("Manufacturer or brand name");
        leftColumn.add(brandField);

        columnsPanel.add(leftColumn);

        // Right Column
        JPanel rightColumn = new JPanel();
        rightColumn.setOpaque(false);
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));

        // Building
        rightColumn.add(createLabel("Building *"));
        List<Building> buildings = buildingDAO.findAll();
        buildingCombo = new JComboBox<>(buildings.toArray(new Building[0]));
        buildingCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    Building building = (Building) value;
                    setText(building.getName() + " (" + building.getCode() + ")");
                }
                return this;
            }
        });
        buildingCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        buildingCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(buildingCombo);
        rightColumn.add(Box.createVerticalStrut(15));

        // Room Number
        rightColumn.add(createLabel("Room/Area"));
        roomField = createTextField();
        roomField.setToolTipText("Room number or general area");
        rightColumn.add(roomField);
        rightColumn.add(Box.createVerticalStrut(15));

        // Location Details
        rightColumn.add(createLabel("Location Details"));
        locationDetailsField = createTextField();
        locationDetailsField.setToolTipText("Specific location (e.g., 'Near water fountain')");
        rightColumn.add(locationDetailsField);
        rightColumn.add(Box.createVerticalStrut(15));

        // Image Upload
        rightColumn.add(createLabel("Upload Photo"));
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        imagePanel.setPreferredSize(new Dimension(300, 150));
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        imageLabel = new JLabel("📷 Click to upload image", SwingConstants.CENTER);
        imageLabel.setForeground(Color.GRAY);
        imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectImage();
            }
        });
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        rightColumn.add(imagePanel);

        columnsPanel.add(rightColumn);
        mainPanel.add(columnsPanel);

        return mainPanel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        clearButton = new JButton("Clear Form");
        clearButton.setPreferredSize(new Dimension(120, 40));
        clearButton.addActionListener(e -> clearForm());
        panel.add(clearButton);

        submitButton = new JButton("Submit Report");
        submitButton.setPreferredSize(new Dimension(150, 40));
        submitButton.setBackground(new Color(46, 204, 113));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> submitReport());
        panel.add(submitButton);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    // In ReportItemPanel.java, update the selectImage method:
    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            System.out.println("\n=== Image Selection ===");
            System.out.println("Selected file: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            System.out.println("File size: " + file.length() + " bytes");

            // Test if it's a valid image before saving
            try {
                BufferedImage testImage = ImageIO.read(file);
                if (testImage == null) {
                    JOptionPane.showMessageDialog(this,
                            "The selected file doesn't appear to be a valid image.\nPlease select a JPG or PNG file.",
                            "Invalid Image",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.out.println("Valid image: " + testImage.getWidth() + "x" + testImage.getHeight());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error reading image file: " + e.getMessage(),
                        "Image Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Use ImageHandler to save to shared folder
            String savedPath = ImageHandler.saveImage(file);

            if (savedPath != null) {
                imagePaths.clear();
                imagePaths.add(savedPath);

                // Update label
                imageLabel.setText("📷 " + file.getName() + " uploaded");
                imageLabel.setForeground(new Color(46, 204, 113));

                // Test loading immediately
                System.out.println("Testing saved image...");
                ImageHandler.testImageLoading(savedPath);

                // Try to show preview
                ImageIcon preview = ImageHandler.loadImage(savedPath, 150, 150);
                if (preview != null) {
                    imageLabel.setIcon(preview);
                    imageLabel.setText("");
                    System.out.println("Preview loaded successfully!");
                } else {
                    System.out.println("Preview failed to load");
                }
            } else {
                showError("Failed to save image");
            }
        }
    }

    private void submitReport() {
        // Validation
        if (titleField.getText().trim().isEmpty()) {
            showError("Please enter an item title");
            return;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            showError("Please enter a description");
            return;
        }

        if (buildingCombo.getSelectedItem() == null) {
            showError("Please select a building");
            return;
        }

        // Create location
        Building selectedBuilding = (Building) buildingCombo.getSelectedItem();
        Location location = new Location(
                selectedBuilding,
                roomField.getText().trim(),
                locationDetailsField.getText().trim()
        );

        // Create item
        Item item = new Item(
                titleField.getText().trim(),
                descriptionArea.getText().trim(),
                (Item.ItemCategory) categoryCombo.getSelectedItem(),
                lostRadio.isSelected() ? Item.ItemType.LOST : Item.ItemType.FOUND,
                location,
                currentUser
        );

        item.setPrimaryColor(colorField.getText().trim());
        item.setBrand(brandField.getText().trim());

        // Add image paths
        for (String path : imagePaths) {
            item.addImagePath(path);
        }

        // Save to database
        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return itemDAO.create(item);
            }

            @Override
            protected void done() {
                try {
                    String itemId = get();
                    if (itemId != null) {
                        JOptionPane.showMessageDialog(ReportItemPanel.this,
                                "Item reported successfully!\n\n"
                                + "Your " + (lostRadio.isSelected() ? "lost" : "found")
                                + " item has been added to the database.\n"
                                + "We'll notify you of any potential matches.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);

                        clearForm();

                        // Make sure image preview is really cleared
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(null);
                            imageLabel.setText("📷 Click to upload image");
                            imageLabel.setForeground(Color.GRAY);
                            imageLabel.repaint();  // Force UI refresh
                        });

                        // 🔥 Only reward FOUND items (+2 points), not LOST items
                        if (item.getType() == Item.ItemType.FOUND) {
                            userDAO.updateTrustScoreByEmail(currentUser.getEmail(), "REPORT");
                            System.out.println("Trust score increased by 2 points for reporting FOUND item");
                            
                            // Refresh UI to show new trust score
                            refreshDashboard();
                        } else {
                            System.out.println("No points for reporting LOST item (only FOUND items are rewarded)");
                        }
                    } else {
                        showError("Failed to save item. Please try again.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                } finally {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit Report");
                }
            }
        };

        worker.execute();
    }

// Update the clearForm() method in ReportItemPanel.java:
    private void clearForm() {
        titleField.setText("");
        descriptionArea.setText("");
        categoryCombo.setSelectedIndex(0);
        buildingCombo.setSelectedIndex(0);
        roomField.setText("");
        locationDetailsField.setText("");
        colorField.setText("");
        brandField.setText("");
        imagePaths.clear();

        // Clear the image preview - ADD THESE LINES
        imageLabel.setIcon(null);  // Clear the icon
        imageLabel.setText("📷 Click to upload image");  // Reset the text
        imageLabel.setForeground(Color.GRAY);  // Reset the color

        lostRadio.setSelected(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Refresh the dashboard to update trust score display
     */
    private void refreshDashboard() {
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof com.campus.lostfound.ui.MainDashboard) {
                ((com.campus.lostfound.ui.MainDashboard) window).refreshUserTrustScore();
            }
        });
    }
}
