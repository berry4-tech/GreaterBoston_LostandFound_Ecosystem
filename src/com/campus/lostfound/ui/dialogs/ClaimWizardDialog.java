/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui.dialogs;

import com.campus.lostfound.dao.MongoClaimDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import static com.campus.lostfound.models.Item.ItemCategory.BAGS;
import static com.campus.lostfound.models.Item.ItemCategory.BOOKS;
import static com.campus.lostfound.models.Item.ItemCategory.ELECTRONICS;
import static com.campus.lostfound.models.Item.ItemCategory.KEYS;
import com.campus.lostfound.models.User;
import static java.awt.AWTEventMulticaster.add;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import java.util.Date;
import java.util.List;

/**
 *
 * @author aksha
 */
class ClaimWizardDialog extends JDialog {

    private Item item;
    private User claimant;
    private int currentStep = 0;
    private boolean claimSubmitted = false;
    private MongoClaimDAO claimDAO;

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JButton backButton;
    private JButton nextButton;
    private JButton submitButton;

    // Step 1 components
    private JTextArea uniqueFeaturesArea;
    private JSpinner lostDateSpinner;
    private JTextField proofField;

    // Step 2 components
    private JTextArea verificationAnswerArea;

    // Step 3 components
    private JList<String> timeSlotList;
    private JComboBox<String> contactMethodCombo;

    public ClaimWizardDialog(Window owner, Item item, User claimant) {
        super(owner, "Claim Item - " + item.getTitle(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.claimant = claimant;
        this.claimDAO = new MongoClaimDAO();

        initComponents();

        setSize(600, 450);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header
        JLabel headerLabel = new JLabel("Claim Your Item");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(headerLabel, BorderLayout.NORTH);

        // Card panel for wizard steps
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create step panels
        cardPanel.add(createStep1Panel(), "step1");
        cardPanel.add(createStep2Panel(), "step2");
        cardPanel.add(createStep3Panel(), "step3");

        add(cardPanel, BorderLayout.CENTER);

        // Navigation panel
        JPanel navPanel = createNavigationPanel();
        add(navPanel, BorderLayout.SOUTH);

        updateNavigationButtons();
    }

    private JPanel createStep1Panel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Step 1: Verify Ownership");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Unique features
        JLabel featuresLabel = new JLabel("Describe unique features not visible in the photo:");
        featuresLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(featuresLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        uniqueFeaturesArea = new JTextArea(4, 40);
        uniqueFeaturesArea.setLineWrap(true);
        uniqueFeaturesArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(uniqueFeaturesArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(scrollPane);
        contentPanel.add(Box.createVerticalStrut(15));

        // Lost date
        JLabel dateLabel = new JLabel("When did you lose this item?");
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(dateLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        lostDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(lostDateSpinner, "MMMM dd, yyyy");
        lostDateSpinner.setEditor(dateEditor);
        lostDateSpinner.setMaximumSize(new Dimension(200, 30));
        lostDateSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lostDateSpinner);
        contentPanel.add(Box.createVerticalStrut(15));

        // Proof of ownership
        JLabel proofLabel = new JLabel("Upload proof of ownership (optional):");
        proofLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(proofLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        JPanel proofPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        proofPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        proofField = new JTextField(20);
        proofField.setEditable(false);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                proofField.setText(chooser.getSelectedFile().getName());
            }
        });
        proofPanel.add(proofField);
        proofPanel.add(browseButton);
        contentPanel.add(proofPanel);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createStep2Panel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Step 2: Verification Questions");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Dynamic question based on item category
        String question = generateVerificationQuestion();
        JLabel questionLabel = new JLabel("<html>" + question + "</html>");
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(questionLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        verificationAnswerArea = new JTextArea(4, 40);
        verificationAnswerArea.setLineWrap(true);
        verificationAnswerArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(verificationAnswerArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(scrollPane);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createStep3Panel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Step 3: Schedule Pickup");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Available time slots
        JLabel timeSlotsLabel = new JLabel("Select available time slots:");
        timeSlotsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(timeSlotsLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        String[] timeSlots = {
            "Today 2:00 PM - 3:00 PM",
            "Today 4:00 PM - 5:00 PM",
            "Tomorrow 10:00 AM - 11:00 AM",
            "Tomorrow 2:00 PM - 3:00 PM",
            "Tomorrow 4:00 PM - 5:00 PM"
        };
        timeSlotList = new JList<>(timeSlots);
        timeSlotList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(timeSlotList);
        scrollPane.setPreferredSize(new Dimension(300, 100));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(scrollPane);
        contentPanel.add(Box.createVerticalStrut(15));

        // Contact preference
        JLabel contactLabel = new JLabel("Preferred contact method:");
        contactLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(contactLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        contactMethodCombo = new JComboBox<>(new String[]{
            "In-app messaging",
            "Email",
            "Phone call",
            "Text message"
        });
        contactMethodCombo.setMaximumSize(new Dimension(200, 30));
        contactMethodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(contactMethodCombo);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("< Back");
        backButton.addActionListener(e -> previousStep());
        leftPanel.add(backButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nextButton = new JButton("Next >");
        nextButton.addActionListener(e -> nextStep());
        submitButton = new JButton("Submit Claim");
        submitButton.setBackground(new Color(76, 175, 80));
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> submitClaim());

        rightPanel.add(nextButton);
        rightPanel.add(submitButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        rightPanel.add(cancelButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private String generateVerificationQuestion() {
        switch (item.getCategory()) {
            case ELECTRONICS:
                return "What apps, stickers, or case modifications does your device have?";
            case BOOKS:
                return "What pages have highlights or notes? What's written on the inside cover?";
            case KEYS:
                return "Describe all items on your keychain in detail.";
            case BAGS:
                return "What items were inside the bag? List everything you remember.";
            default:
                return "Provide any additional details that prove this is your item.";
        }
    }

    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            cardLayout.show(cardPanel, "step" + (currentStep + 1));
            updateNavigationButtons();
        }
    }

    private void nextStep() {
        if (validateCurrentStep()) {
            if (currentStep < 2) {
                currentStep++;
                cardLayout.show(cardPanel, "step" + (currentStep + 1));
                updateNavigationButtons();
            }
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 0:
                if (uniqueFeaturesArea.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please describe unique features of your item.",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                break;
            case 1:
                if (verificationAnswerArea.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please answer the verification question.",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                break;
            case 2:
                if (timeSlotList.getSelectedValuesList().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please select at least one time slot.",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                break;
        }
        return true;
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(currentStep > 0);
        nextButton.setVisible(currentStep < 2);
        submitButton.setVisible(currentStep == 2);
    }

    private void submitClaim() {
        if (validateCurrentStep()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to submit this claim?\n"
                    + "False claims may affect your trust score.",
                    "Confirm Claim Submission",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                // Create claim object
                Claim claim = new Claim(item.getItemId(), claimant);
                
                // Set all the claim data from the wizard
                claim.setUniqueFeatures(uniqueFeaturesArea.getText().trim());
                claim.setLostDate((Date) lostDateSpinner.getValue());
                claim.setProofOfOwnership(proofField.getText().trim());
                claim.setVerificationAnswer(verificationAnswerArea.getText().trim());
                
                // Get selected time slots
                List<String> selectedSlots = timeSlotList.getSelectedValuesList();
                claim.setSelectedTimeSlots(selectedSlots.toArray(new String[0]));
                
                claim.setContactMethod((String) contactMethodCombo.getSelectedItem());
                
                // Save to database
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return claimDAO.create(claim);
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            String claimId = get();
                            if (claimId != null) {
                                claimSubmitted = true;
                                JOptionPane.showMessageDialog(ClaimWizardDialog.this,
                                    "Claim submitted successfully!\n\n" +
                                    "The item finder will review your claim and contact you.\n" +
                                    "You'll be notified of their decision.",
                                    "Claim Submitted",
                                    JOptionPane.INFORMATION_MESSAGE);
                                dispose();
                            } else {
                                JOptionPane.showMessageDialog(ClaimWizardDialog.this,
                                    "Failed to submit claim. Please try again.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(ClaimWizardDialog.this,
                                "Error submitting claim: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        }
    }

    public boolean isClaimSubmitted() {
        return claimSubmitted;
    }
}
