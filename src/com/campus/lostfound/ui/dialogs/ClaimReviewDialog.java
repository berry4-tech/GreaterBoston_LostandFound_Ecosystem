/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.ui.dialogs;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.ui.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Dialog for reviewing and approving/rejecting claims
 * This is where FALSE_CLAIM trust score decreases happen!
 * @author aksha
 */
public class ClaimReviewDialog extends JDialog {
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_BOLD = UIConstants.getEmojiFont(Font.BOLD, 13);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.BOLD, 18);
    
    private Item item;
    private User currentUser; // The finder/reporter
    private List<Claim> pendingClaims;
    private MongoClaimDAO claimDAO;
    private MongoUserDAO userDAO;
    private MongoItemDAO itemDAO;
    
    private JList<Claim> claimsList;
    private DefaultListModel<Claim> claimsModel;
    private JTextArea claimDetailsArea;
    private JButton approveButton;
    private JButton rejectButton;
    private JButton closeButton;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
    
    public ClaimReviewDialog(Window owner, Item item, User currentUser) {
        super(owner, "Review Claims - " + item.getTitle(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.currentUser = currentUser;
        this.claimDAO = new MongoClaimDAO();
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        
        initComponents();
        loadClaims();
        
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Split pane - claims list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        
        // Left - Claims list
        JPanel claimsPanel = createClaimsListPanel();
        splitPane.setLeftComponent(claimsPanel);
        
        // Right - Claim details
        JPanel detailsPanel = createDetailsPanel();
        splitPane.setRightComponent(detailsPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Bottom buttons
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🔍 Review Claims");
        titleLabel.setFont(EMOJI_FONT_LARGE);
        panel.add(titleLabel, BorderLayout.WEST);
        
        JLabel itemLabel = new JLabel("Item: " + item.getTitle());
        itemLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        itemLabel.setForeground(Color.GRAY);
        panel.add(itemLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createClaimsListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Pending Claims"));
        
        claimsModel = new DefaultListModel<>();
        claimsList = new JList<>(claimsModel);
        claimsList.setCellRenderer(new ClaimCellRenderer());
        claimsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        claimsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedClaim();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(claimsList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Claim Details"));
        
        claimDetailsArea = new JTextArea();
        claimDetailsArea.setEditable(false);
        claimDetailsArea.setFont(EMOJI_FONT);
        claimDetailsArea.setLineWrap(true);
        claimDetailsArea.setWrapStyleWord(true);
        claimDetailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(claimDetailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        approveButton = new JButton("✓ Approve Claim");
        approveButton.setBackground(new Color(76, 175, 80));
        approveButton.setForeground(Color.WHITE);
        approveButton.setFont(EMOJI_FONT_BOLD);
        approveButton.setFocusPainted(false);
        approveButton.setEnabled(false);
        approveButton.addActionListener(e -> approveClaim());
        panel.add(approveButton);
        
        rejectButton = new JButton("✗ Reject Claim");
        rejectButton.setBackground(new Color(244, 67, 54));
        rejectButton.setForeground(Color.WHITE);
        rejectButton.setFont(EMOJI_FONT_BOLD);
        rejectButton.setFocusPainted(false);
        rejectButton.setEnabled(false);
        rejectButton.addActionListener(e -> rejectClaim());
        panel.add(rejectButton);
        
        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    private void loadClaims() {
        SwingWorker<List<Claim>, Void> worker = new SwingWorker<List<Claim>, Void>() {
            @Override
            protected List<Claim> doInBackground() throws Exception {
                return claimDAO.findPendingClaimsByItem(item.getItemId());
            }
            
            @Override
            protected void done() {
                try {
                    pendingClaims = get();
                    claimsModel.clear();
                    for (Claim claim : pendingClaims) {
                        claimsModel.addElement(claim);
                    }
                    
                    if (pendingClaims.isEmpty()) {
                        claimDetailsArea.setText("No pending claims for this item.");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                        "Error loading claims: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void displaySelectedClaim() {
        Claim selectedClaim = claimsList.getSelectedValue();
        if (selectedClaim == null) {
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
            return;
        }
        
        approveButton.setEnabled(true);
        rejectButton.setEnabled(true);
        
        // Build detailed view
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("CLAIMANT INFORMATION\n");
        details.append("═══════════════════════════════════════\n\n");
        
        User claimant = selectedClaim.getClaimant();
        details.append("Name: ").append(claimant.getFullName()).append("\n");
        details.append("Email: ").append(claimant.getEmail()).append("\n");
        details.append("Trust Score: ").append(String.format("%.0f", claimant.getTrustScore())).append("/100");
        
        // Enhanced trust warnings based on score
        double trust = claimant.getTrustScore();
        if (trust < 25) {
            details.append(" 🔴 SUSPENDED - EXTREME CAUTION!\n");
            details.append("⚠️ This user's account is suspended!\n");
            details.append("⚠️ Multiple false claims on record!\n");
            details.append("⚠️ Recommend REJECTING unless proof is excellent!\n");
        } else if (trust < 50) {
            details.append(" 🟠 POOR - HIGH RISK!\n");
            details.append("⚠️ This user has a history of false claims!\n");
            details.append("⚠️ Verify all details carefully!\n");
        } else if (trust < 75) {
            details.append(" 🟡 GOOD - Some risk\n");
            details.append("⚠️ User has some claim issues - review carefully\n");
        } else {
            details.append(" 🟢 EXCELLENT - Trusted user\n");
        }
        
        details.append("\nFalse Claims: ").append(claimant.getFalseClaims()).append("\n");
        details.append("Submitted: ").append(DATE_FORMAT.format(selectedClaim.getSubmittedDate())).append("\n\n");
        
        details.append("═══════════════════════════════════════\n");
        details.append("VERIFICATION DETAILS\n");
        details.append("═══════════════════════════════════════\n\n");
        
        details.append("🔍 Unique Features (Not in Photo):\n");
        details.append(selectedClaim.getUniqueFeatures()).append("\n\n");
        
        details.append("📅 Date Lost:\n");
        if (selectedClaim.getLostDate() != null) {
            details.append(DATE_FORMAT.format(selectedClaim.getLostDate())).append("\n\n");
        }
        
        details.append("❓ Verification Question Answer:\n");
        details.append(selectedClaim.getVerificationAnswer()).append("\n\n");
        
        if (selectedClaim.getProofOfOwnership() != null && !selectedClaim.getProofOfOwnership().isEmpty()) {
            details.append("📄 Proof of Ownership:\n");
            details.append(selectedClaim.getProofOfOwnership()).append("\n\n");
        }
        
        details.append("═══════════════════════════════════════\n");
        details.append("PICKUP DETAILS\n");
        details.append("═══════════════════════════════════════\n\n");
        
        details.append("Preferred Contact: ").append(selectedClaim.getContactMethod()).append("\n");
        details.append("Available Time Slots:\n");
        if (selectedClaim.getSelectedTimeSlots() != null) {
            for (String slot : selectedClaim.getSelectedTimeSlots()) {
                details.append("  • ").append(slot).append("\n");
            }
        }
        
        details.append("\n═══════════════════════════════════════\n");
        details.append("⚠️ IMPORTANT REMINDER\n");
        details.append("═══════════════════════════════════════\n\n");
        details.append("Rejecting a false claim will:\n");
        details.append("• Decrease claimant's trust score by 25 points\n");
        details.append("• Flag their account if trust score goes below 50\n");
        details.append("• Restrict their ability to claim high-value items\n\n");
        details.append("Only reject if you're confident this is NOT their item!");
        
        claimDetailsArea.setText(details.toString());
        claimDetailsArea.setCaretPosition(0);
    }
    
    private void approveClaim() {
        Claim selectedClaim = claimsList.getSelectedValue();
        if (selectedClaim == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to approve this claim?\n\n" +
            "The item will be marked as CLAIMED and the claimant will be notified.\n" +
            "Claimant: " + selectedClaim.getClaimant().getFullName(),
            "Confirm Approval",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    // Update claim status
                    boolean claimUpdated = claimDAO.updateClaimStatus(
                        selectedClaim.getClaimId(), 
                        Claim.ClaimStatus.APPROVED, 
                        null
                    );
                    
                    if (!claimUpdated) return false;
                    
                    // Update item status to CLAIMED
                    item.setStatus(Item.ItemStatus.CLAIMED);
                    item.setResolvedDate(new java.util.Date());
                    item.setClaimedBy(selectedClaim.getClaimant());
                    itemDAO.update(item);
                    
                    // Increase claimant's trust score for successful return
                    // 🔥 FIX: Use email instead of userId
                    userDAO.updateTrustScoreByEmail(
                        selectedClaim.getClaimant().getEmail(), 
                        "RETURN"
                    );
                    
                    // 🔥 NEW: Reward the FINDER for helping someone get their item back
                    userDAO.updateTrustScoreByEmail(
                        currentUser.getEmail(),
                        "HELPED_RETURN"
                    );
                    
                    return true;
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                                "Claim approved successfully!\n\n" +
                                "✓ Item marked as claimed\n" +
                                "✓ Claimant's trust score increased by 10 points\n" +
                                "✓ Your trust score increased by 5 points (for helping!)\n" +
                                "✓ Claimant will be notified",
                                "Claim Approved",
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            // 🔥 Refresh dashboard UI to show new trust score
                            refreshDashboard();
                            
                            loadClaims(); // Refresh list
                        } else {
                            JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                                "Failed to approve claim. Please try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                            approveButton.setEnabled(true);
                            rejectButton.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                            "Error approving claim: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        approveButton.setEnabled(true);
                        rejectButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void rejectClaim() {
        Claim selectedClaim = claimsList.getSelectedValue();
        if (selectedClaim == null) return;
        
        // Get rejection reason
        String[] options = {
            "Features don't match",
            "Can't answer verification questions",
            "Suspicious/fraudulent claim",
            "Date doesn't match",
            "No valid proof provided",
            "Other"
        };
        
        String reason = (String) JOptionPane.showInputDialog(this,
            "Why are you rejecting this claim?\n\n" +
            "⚠️ This will decrease the claimant's trust score by 25 points!\n\n" +
            "Claimant: " + selectedClaim.getClaimant().getFullName() + "\n" +
            "Current Trust Score: " + String.format("%.0f", selectedClaim.getClaimant().getTrustScore()),
            "Reject Claim - Select Reason",
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        
        if (reason == null) return; // User cancelled
        
        // Get additional details if "Other" selected
        if (reason.equals("Other")) {
            reason = JOptionPane.showInputDialog(this,
                "Please provide details for rejection:",
                "Rejection Details",
                JOptionPane.PLAIN_MESSAGE);
            if (reason == null || reason.trim().isEmpty()) return;
        }
        
        final String finalReason = reason;
        
        // Final confirmation
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ FINAL CONFIRMATION ⚠️\n\n" +
            "You are about to reject this claim for:\n" +
            "\"" + finalReason + "\"\n\n" +
            "This will:\n" +
            "• Decrease trust score from " + String.format("%.0f", selectedClaim.getClaimant().getTrustScore()) + 
            " to " + Math.max(0, selectedClaim.getClaimant().getTrustScore() - 25) + "\n" +
            "• Flag the user's account\n" +
            "• Notify the claimant of rejection\n\n" +
            "Are you absolutely sure?",
            "Final Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    // Update claim status
                    boolean claimUpdated = claimDAO.updateClaimStatus(
                        selectedClaim.getClaimId(), 
                        Claim.ClaimStatus.REJECTED, 
                        finalReason
                    );
                    
                    if (!claimUpdated) return false;
                    
                    // 🔥 THIS IS WHERE THE FALSE_CLAIM HAPPENS! 🔥
                    // Decrease trust score by 25 points
                    // 🔥 FIX: Use email instead of userId
                    userDAO.updateTrustScoreByEmail(
                        selectedClaim.getClaimant().getEmail(), 
                        "FALSE_CLAIM"
                    );
                    
                    return true;
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            double newScore = Math.max(0, selectedClaim.getClaimant().getTrustScore() - 25);
                            
                            JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                                "Claim rejected successfully!\n\n" +
                                "✗ Claim marked as rejected\n" +
                                "✗ Trust score decreased: " + 
                                String.format("%.0f → %.0f (-25 points)", 
                                    selectedClaim.getClaimant().getTrustScore(), newScore) + "\n" +
                                "✗ User has been notified\n\n" +
                                "Reason: " + finalReason,
                                "Claim Rejected",
                                JOptionPane.WARNING_MESSAGE);
                            
                            // 🔥 Refresh dashboard UI to show new trust score
                            refreshDashboard();
                            
                            loadClaims(); // Refresh list
                        } else {
                            JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                                "Failed to reject claim. Please try again.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                            approveButton.setEnabled(true);
                            rejectButton.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(ClaimReviewDialog.this,
                            "Error rejecting claim: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        approveButton.setEnabled(true);
                        rejectButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        }
    }
    
    /**
     * Refresh the dashboard to update trust score display
     */
    private void refreshDashboard() {
        SwingUtilities.invokeLater(() -> {
            Window owner = getOwner();
            while (owner != null && !(owner instanceof com.campus.lostfound.ui.MainDashboard)) {
                owner = owner.getOwner();
            }
            if (owner instanceof com.campus.lostfound.ui.MainDashboard) {
                ((com.campus.lostfound.ui.MainDashboard) owner).refreshUserTrustScore();
            }
        });
    }
    
    // Custom renderer for claims list
    class ClaimCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            if (isSelected) {
                panel.setBackground(new Color(230, 240, 250));
            } else {
                panel.setBackground(Color.WHITE);
            }
            
            if (value instanceof Claim) {
                Claim claim = (Claim) value;
                
                JLabel nameLabel = new JLabel(claim.getClaimant().getFullName());
                nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
                panel.add(nameLabel, BorderLayout.NORTH);
                
                JLabel trustLabel = new JLabel("Trust: " + 
                    String.format("%.0f", claim.getClaimant().getTrustScore()) + "/100");
                trustLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                
                // Color code based on trust score
                double trust = claim.getClaimant().getTrustScore();
                if (trust < 50) {
                    trustLabel.setForeground(Color.RED);
                } else if (trust < 75) {
                    trustLabel.setForeground(Color.ORANGE);
                } else {
                    trustLabel.setForeground(new Color(0, 150, 0));
                }
                
                panel.add(trustLabel, BorderLayout.CENTER);
                
                JLabel dateLabel = new JLabel(
                    new SimpleDateFormat("MMM dd").format(claim.getSubmittedDate()));
                dateLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                dateLabel.setForeground(Color.GRAY);
                panel.add(dateLabel, BorderLayout.SOUTH);
            }
            
            return panel;
        }
    }
}
