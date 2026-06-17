/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.components.ItemListRenderer;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author aksha
 */
public class MyItemsPanel extends JPanel {

    private User currentUser;
    private MongoItemDAO itemDAO;
    private JTabbedPane tabbedPane;
    private JTable lostItemsTable;
    private JTable foundItemsTable;
    private DefaultTableModel lostTableModel;
    private DefaultTableModel foundTableModel;

    public MyItemsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.itemDAO = new MongoItemDAO();

        initComponents();
        loadUserItems();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane for lost/found items
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lost items tab
        JPanel lostPanel = createItemsPanel(true);
        tabbedPane.addTab("❌ My Lost Items", lostPanel);

        // Found items tab
        JPanel foundPanel = createItemsPanel(false);
        tabbedPane.addTab("✅ My Found Items", foundPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("My Reported Items");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> refreshItems());
        panel.add(refreshButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createItemsPanel(boolean isLostItems) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columns = {"Title", "Category", "Location", "Date", "Status", "Actions"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only actions column is editable
            }
        };

        // Create table
        JTable table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.setFont(new Font("Arial", Font.PLAIN, 12));

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200); // Title
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Category
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Location
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Date
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Status
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Actions

        // Custom renderer for status column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString();
                    if (status.equals("OPEN")) {
                        setForeground(new Color(46, 204, 113));
                    } else if (status.equals("PENDING_CLAIM")) {
                        setForeground(new Color(241, 196, 15));
                    } else if (status.equals("CLAIMED")) {
                        setForeground(new Color(155, 89, 182));
                    } else {
                        setForeground(Color.GRAY);
                    }
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                return this;
            }
        });

        // Actions column
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox(), table, isLostItems));

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Store references
        if (isLostItems) {
            lostItemsTable = table;
            lostTableModel = tableModel;
        } else {
            foundItemsTable = table;
            foundTableModel = tableModel;
        }

        return panel;
    }

    public void refreshItems() {
        loadUserItems();
    }

    private void loadUserItems() {
        SwingWorker<List<Item>, Void> worker = new SwingWorker<List<Item>, Void>() {
            @Override
            protected List<Item> doInBackground() throws Exception {
                return itemDAO.findByUser(currentUser.getEmail());
            }

            @Override
            protected void done() {
                try {
                    List<Item> items = get();
                    updateTables(items);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MyItemsPanel.this,
                            "Error loading items: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTables(List<Item> items) {
        // Clear tables
        lostTableModel.setRowCount(0);
        foundTableModel.setRowCount(0);

        // Add items to appropriate tables
        for (Item item : items) {
            Object[] row = {
                item.getTitle(),
                item.getCategory().getDisplayName(),
                item.getLocation().getBuilding().getCode(),
                new java.text.SimpleDateFormat("MM/dd/yy").format(item.getReportedDate()),
                item.getStatus().name(),
                "View/Edit"
            };

            if (item.getType() == Item.ItemType.LOST) {
                lostTableModel.addRow(row);
            } else {
                foundTableModel.addRow(row);
            }
        }

        // Update tab titles with counts
        tabbedPane.setTitleAt(0, "❌ My Lost Items (" + lostTableModel.getRowCount() + ")");
        tabbedPane.setTitleAt(1, "✅ My Found Items (" + foundTableModel.getRowCount() + ")");
    }

    // Button renderer for actions column
    class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Button editor for actions column
    class ButtonEditor extends DefaultCellEditor {

        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private boolean isLostItems;

        public ButtonEditor(JCheckBox checkBox, JTable table, boolean isLostItems) {
            super(checkBox);
            this.table = table;
            this.isLostItems = isLostItems;

            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Get selected item details
                int row = table.getSelectedRow();
                String title = table.getValueAt(row, 0).toString();
                String status = table.getValueAt(row, 4).toString();

                // Show options dialog
                String[] options = {"Mark as Found/Returned", "Delete Item", "Cancel"};
                int choice = JOptionPane.showOptionDialog(
                        MyItemsPanel.this,
                        "What would you like to do with: " + title,
                        "Item Actions",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[2]
                );

                if (choice == 0) {
                    // Mark as found/returned
                    markItemAsResolved(title);
                } else if (choice == 1) {
                    // Delete item
                    deleteItem(title);
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private void markItemAsResolved(String title) {
        // Find and update item
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                List<Item> items = itemDAO.findByUser(currentUser.getEmail());
                for (Item item : items) {
                    if (item.getTitle().equals(title)) {
                        item.setStatus(Item.ItemStatus.CLAIMED);
                        item.setResolvedDate(new Date());
                        return itemDAO.update(item);
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(MyItemsPanel.this,
                                "Item marked as resolved!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        refreshItems();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void deleteItem(String title) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + title + "\"?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    // Use the new delete method
                    return itemDAO.deleteByTitleAndUser(title, currentUser.getEmail());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(MyItemsPanel.this,
                                    "Item \"" + title + "\" has been deleted.",
                                    "Item Deleted",
                                    JOptionPane.INFORMATION_MESSAGE);
                            refreshItems(); // Reload both tables
                        } else {
                            JOptionPane.showMessageDialog(MyItemsPanel.this,
                                    "Could not delete item. Please try again.",
                                    "Delete Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(MyItemsPanel.this,
                                "Error: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}
