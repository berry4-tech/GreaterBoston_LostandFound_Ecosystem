/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.campus.lostfound.ui.panels;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

/**
 *
 * @author aksha
 */
public class MessagesPanel extends JPanel {

    private User currentUser;
    private MongoMessageDAO messageDAO;
    private MongoItemDAO itemDAO;
    private MongoUserDAO userDAO;

    private JList<Conversation> conversationList;
    private DefaultListModel<Conversation> conversationModel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel currentChatLabel;
    private Conversation currentConversation;

    public MessagesPanel(User currentUser) {
        this.currentUser = currentUser;
        this.messageDAO = new MongoMessageDAO();
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();

        initComponents();
        loadConversations();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Split pane - conversations list and chat area
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);

        // Left - Conversations list
        JPanel conversationsPanel = createConversationsPanel();
        splitPane.setLeftComponent(conversationsPanel);

        // Right - Chat area
        JPanel chatPanel = createChatPanel();
        splitPane.setRightComponent(chatPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("💬 Messages");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadConversations());
        panel.add(refreshButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createConversationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Conversations"));

        conversationModel = new DefaultListModel<>();
        conversationList = new JList<>(conversationModel);
        conversationList.setCellRenderer(new ConversationRenderer());
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Conversation selected = conversationList.getSelectedValue();
                if (selected != null) {
                    openConversation(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(conversationList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Chat header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(new Color(250, 250, 250));
        chatHeader.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        currentChatLabel = new JLabel("Select a conversation");
        currentChatLabel.setFont(new Font("Arial", Font.BOLD, 14));
        chatHeader.add(currentChatLabel, BorderLayout.CENTER);

        panel.add(chatHeader, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        panel.add(chatScroll, BorderLayout.CENTER);

        // Message input
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 13));
        messageField.setEnabled(false);
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        inputPanel.add(messageField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadConversations() {
        SwingWorker<List<Conversation>, Void> worker = new SwingWorker<List<Conversation>, Void>() {
            @Override
            protected List<Conversation> doInBackground() throws Exception {
                return messageDAO.getConversations(currentUser.getEmail());
            }

            @Override
            protected void done() {
                try {
                    List<Conversation> conversations = get();
                    conversationModel.clear();
                    for (Conversation conv : conversations) {
                        conversationModel.addElement(conv);
                    }

                    if (conversations.isEmpty()) {
                        chatArea.setText("\nNo messages yet.\n\n"
                                + "Messages will appear here when:\n"
                                + "• Someone contacts you about a found item\n"
                                + "• You message someone about their lost/found item\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void openConversation(Conversation conversation) {
        currentConversation = conversation;
        currentChatLabel.setText("Chat with " + conversation.getOtherUserName()
                + " - Re: " + conversation.getItemTitle());

        // Load messages
        SwingWorker<List<Message>, Void> worker = new SwingWorker<List<Message>, Void>() {
            @Override
            protected List<Message> doInBackground() throws Exception {
                return messageDAO.getMessages(
                        currentUser.getEmail(),
                        conversation.getOtherUserId(),
                        conversation.getItemId()
                );
            }

            @Override
            protected void done() {
                try {
                    List<Message> messages = get();
                    displayMessages(messages);
                    messageField.setEnabled(true);
                    sendButton.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void displayMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

        for (Message msg : messages) {
            String sender = currentUser.getEmail().equals(msg.getSenderId())
                    ? "You" : currentConversation.getOtherUserName();

            sb.append(sender).append(" (")
                    .append(dateFormat.format(msg.getSentDate()))
                    .append("):\n")
                    .append(msg.getMessageText())
                    .append("\n\n");
        }

        chatArea.setText(sb.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendMessage() {
        if (currentConversation == null || messageField.getText().trim().isEmpty()) {
            return;
        }

        String messageText = messageField.getText().trim();
        messageField.setText("");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Message message = new Message(
                        currentConversation.getItemId(),
                        currentUser.getEmail(),
                        currentConversation.getOtherUserId(),
                        messageText
                );
                return messageDAO.sendMessage(message) != null;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Refresh conversation
                        openConversation(currentConversation);
                    } else {
                        JOptionPane.showMessageDialog(MessagesPanel.this,
                                "Failed to send message",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    // Inner class for conversation display
    class ConversationRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            if (isSelected) {
                panel.setBackground(new Color(230, 240, 250));
            } else {
                panel.setBackground(Color.WHITE);
            }

            if (value instanceof Conversation) {
                Conversation conv = (Conversation) value;

                JLabel nameLabel = new JLabel(conv.getOtherUserName());
                nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
                panel.add(nameLabel, BorderLayout.NORTH);

                JLabel itemLabel = new JLabel("Re: " + conv.getItemTitle());
                itemLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                itemLabel.setForeground(Color.GRAY);
                panel.add(itemLabel, BorderLayout.CENTER);

                if (conv.getUnreadCount() > 0) {
                    JLabel unreadLabel = new JLabel(String.valueOf(conv.getUnreadCount()));
                    unreadLabel.setBackground(new Color(52, 152, 219));
                    unreadLabel.setForeground(Color.WHITE);
                    unreadLabel.setOpaque(true);
                    unreadLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                    panel.add(unreadLabel, BorderLayout.EAST);
                }
            }

            return panel;
        }
    }
}
