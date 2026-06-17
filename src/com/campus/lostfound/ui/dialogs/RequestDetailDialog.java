package com.campus.lostfound.ui.dialogs;

import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.models.Organization;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.TrustScoreService;
import com.campus.lostfound.services.WorkRequestService;
import com.campus.lostfound.ui.UIConstants;
import com.campus.lostfound.ui.components.TrustScoreBadge;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modal dialog for viewing and acting on Work Request details.
 * Features:
 * - Full request details display
 * - Approval chain visualization
 * - Approve/Reject with comments
 * - SLA countdown display
 * - Type-specific details (claim info, transfer details, etc.)
 * 
 * @author Developer 2 - UI Components
 */
public class RequestDetailDialog extends JDialog {
    
    // Emoji-capable fonts
    private static final Font EMOJI_FONT = UIConstants.getEmojiFont(Font.PLAIN, 14);
    private static final Font EMOJI_FONT_BOLD = UIConstants.getEmojiFont(Font.BOLD, 13);
    private static final Font EMOJI_FONT_LARGE = UIConstants.getEmojiFont(Font.PLAIN, 36);
    
    // Data
    private WorkRequest request;
    private User currentUser;
    private WorkRequestService workRequestService;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private MongoUserDAO userDAO;  // Use UserDAO to get trust scores from users collection (consistent with WorkQueueTablePanel)
    private TrustScoreService trustScoreService;
    
    // UI Components
    private JPanel detailsPanel;
    private JTextArea commentsArea;
    private JButton approveButton;
    private JButton rejectButton;
    private JButton closeButton;
    private JLabel statusLabel;
    
    // Callbacks
    private Consumer<WorkRequest> onRequestUpdated;
    
    // Constants
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    private static final Color HEADER_BG = new Color(52, 73, 94);
    private static final Color SECTION_BG = new Color(248, 249, 250);
    
    /**
     * Create a new RequestDetailDialog.
     * 
     * @param owner Parent window
     * @param request The work request to display
     * @param currentUser The logged-in user
     */
    public RequestDetailDialog(Window owner, WorkRequest request, User currentUser) {
        super(owner, "Work Request Details", ModalityType.APPLICATION_MODAL);
        this.request = request;
        this.currentUser = currentUser;
        this.workRequestService = new WorkRequestService();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.userDAO = new MongoUserDAO();  // For trust score lookups from users collection
        this.trustScoreService = new TrustScoreService();
        
        initComponents();
        populateDetails();
        updateButtonStates();
        
        setSize(800, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Main content with scroll
        JScrollPane scrollPane = new JScrollPane(createContentPanel());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // Footer with buttons
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(HEADER_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left side - Type icon and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);
        
        // Type icon
        JLabel iconLabel = new JLabel(getTypeIcon(request.getRequestType()));
        iconLabel.setFont(EMOJI_FONT_LARGE);
        leftPanel.add(iconLabel);
        
        // Title and ID
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(getTypeDisplayName(request.getRequestType()));
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        JLabel idLabel = new JLabel("ID: " + (request.getRequestId() != null ? 
            request.getRequestId().substring(0, Math.min(8, request.getRequestId().length())) + "..." : "N/A"));
        idLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        idLabel.setForeground(new Color(189, 195, 199));
        titlePanel.add(idLabel);
        
        leftPanel.add(titlePanel);
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Status and Priority badges
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        // Priority badge
        JLabel priorityBadge = createPriorityBadge(request.getPriority());
        rightPanel.add(priorityBadge);
        
        // Status badge
        statusLabel = createStatusBadge(request.getStatus());
        rightPanel.add(statusLabel);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createContentPanel() {
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        return detailsPanel;
    }
    
    private void populateDetails() {
        detailsPanel.removeAll();
        
        // SLA Section (if pending)
        if (request.isPending()) {
            detailsPanel.add(createSlaSection());
            detailsPanel.add(Box.createVerticalStrut(20));
        }
        
        // Summary Section
        detailsPanel.add(createSummarySection());
        detailsPanel.add(Box.createVerticalStrut(20));
        
        // Requester Section
        detailsPanel.add(createRequesterSection());
        detailsPanel.add(Box.createVerticalStrut(20));
        
        // Approval Chain Section
        detailsPanel.add(createApprovalChainSection());
        detailsPanel.add(Box.createVerticalStrut(20));
        
        // Type-Specific Details Section
        JPanel typeSpecificSection = createTypeSpecificSection();
        if (typeSpecificSection != null) {
            detailsPanel.add(typeSpecificSection);
            detailsPanel.add(Box.createVerticalStrut(20));
        }
        
        // Notes Section
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            detailsPanel.add(createNotesSection());
            detailsPanel.add(Box.createVerticalStrut(20));
        }
        
        // Comments Section (for approval/rejection)
        if (canUserApprove()) {
            detailsPanel.add(createCommentsSection());
        }
        
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }
    
    private JPanel createSlaSection() {
        JPanel section = createSection("⏱️ SLA Status");
        
        long hoursRemaining = request.getHoursUntilSla();
        boolean isOverdue = request.isOverdue();
        
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        content.setOpaque(false);
        
        // Time remaining
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 24));
        
        if (isOverdue) {
            timeLabel.setText("⚠️ OVERDUE by " + Math.abs(hoursRemaining) + " hours");
            timeLabel.setForeground(new Color(220, 53, 69));
            section.setBackground(new Color(255, 235, 235));
        } else if (hoursRemaining <= 4) {
            timeLabel.setText("🔴 " + hoursRemaining + " hours remaining");
            timeLabel.setForeground(new Color(220, 53, 69));
            section.setBackground(new Color(255, 243, 224));
        } else if (hoursRemaining <= 24) {
            timeLabel.setText("🟠 " + hoursRemaining + " hours remaining");
            timeLabel.setForeground(new Color(255, 152, 0));
            section.setBackground(new Color(255, 249, 196));
        } else {
            long days = hoursRemaining / 24;
            long hours = hoursRemaining % 24;
            timeLabel.setText("🟢 " + days + " days, " + hours + " hours remaining");
            timeLabel.setForeground(new Color(40, 167, 69));
        }
        
        content.add(timeLabel);
        
        // SLA target
        JLabel targetLabel = new JLabel("Target: " + request.getSlaTargetHours() + " hours (" + 
            request.getPriority().name() + " priority)");
        targetLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        targetLabel.setForeground(Color.GRAY);
        content.add(targetLabel);
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createSummarySection() {
        JPanel section = createSection("📋 Request Summary");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        // Summary text
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel summaryLabel = new JLabel(request.getRequestSummary());
        summaryLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 14));
        content.add(summaryLabel, gbc);
        
        // Description
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            gbc.gridy = 1;
            JTextArea descArea = new JTextArea(request.getDescription());
            descArea.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
            descArea.setEditable(false);
            descArea.setOpaque(false);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setPreferredSize(new Dimension(600, 60));
            content.add(descArea, gbc);
        }
        
        // Created date
        gbc.gridy = 2; gbc.gridwidth = 1;
        content.add(createFieldLabel("Created:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(request.getCreatedAt().format(DATE_FORMAT)), gbc);
        
        // Last updated
        gbc.gridx = 0; gbc.gridy = 3;
        content.add(createFieldLabel("Last Updated:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(request.getLastUpdatedAt().format(DATE_FORMAT)), gbc);
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createRequesterSection() {
        JPanel section = createSection("👤 Requester Information");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        int row = 0;
        
        // Name
        gbc.gridx = 0; gbc.gridy = row;
        content.add(createFieldLabel("Name:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(request.getRequesterName()), gbc);
        row++;
        
        // Trust Score with badge
        gbc.gridx = 0; gbc.gridy = row;
        content.add(createFieldLabel("Trust Score:"), gbc);
        gbc.gridx = 1;
        try {
            // Get trust score from users collection (consistent with WorkQueueTablePanel)
            // Use requesterEmail, fallback to requesterId (which often contains email)
            String emailForLookup = request.getRequesterEmail();
            if (emailForLookup == null || emailForLookup.isEmpty()) {
                emailForLookup = request.getRequesterId();  // Fallback: requesterId often contains email
            }
            
            double score = 50.0;  // Default
            if (emailForLookup != null && !emailForLookup.isEmpty()) {
                // Look up trust score from users collection directly
                java.util.Optional<User> userOpt = userDAO.findByEmail(emailForLookup);
                if (userOpt.isPresent()) {
                    score = userOpt.get().getTrustScore();
                }
            }
            
            TrustScoreBadge trustBadge = TrustScoreBadge.full(score);
            content.add(trustBadge, gbc);
            
            // Add warning if low trust
            if (score < 50) {
                gbc.gridx = 2;
                JLabel warningLabel = new JLabel("⚠️ Low trust - verify carefully");
                warningLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 11));
                warningLabel.setForeground(new Color(220, 53, 69));
                content.add(warningLabel, gbc);
            }
        } catch (Exception e) {
            content.add(createFieldValue("Unable to load"), gbc);
        }
        row++;
        
        // Enterprise
        if (request.getRequesterEnterpriseId() != null) {
            gbc.gridx = 0; gbc.gridy = row;
            content.add(createFieldLabel("Enterprise:"), gbc);
            gbc.gridx = 1;
            String enterpriseName = getEnterpriseName(request.getRequesterEnterpriseId());
            content.add(createFieldValue(enterpriseName), gbc);
            row++;
        }
        
        // Organization
        if (request.getRequesterOrganizationId() != null) {
            gbc.gridx = 0; gbc.gridy = row;
            content.add(createFieldLabel("Organization:"), gbc);
            gbc.gridx = 1;
            String orgName = getOrganizationName(request.getRequesterOrganizationId());
            content.add(createFieldValue(orgName), gbc);
        }
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createApprovalChainSection() {
        JPanel section = createSection("✅ Approval Chain Progress");
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        List<String> chain = request.getApprovalChain();
        List<String> approverNames = request.getApproverNames();
        int currentStep = request.getApprovalStep();
        
        for (int i = 0; i < chain.size(); i++) {
            JPanel stepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            stepPanel.setOpaque(false);
            stepPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            
            // Step indicator
            JLabel stepIcon = new JLabel();
            stepIcon.setFont(EMOJI_FONT);
            
            if (i < currentStep) {
                // Completed
                stepIcon.setText("✅");
            } else if (i == currentStep && request.isPending()) {
                // Current
                stepIcon.setText("🔄");
            } else {
                // Pending
                stepIcon.setText("⏳");
            }
            stepPanel.add(stepIcon);
            
            // Step number
            JLabel stepNumLabel = new JLabel("Step " + (i + 1) + ":");
            stepNumLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
            stepNumLabel.setForeground(i <= currentStep ? Color.BLACK : Color.GRAY);
            stepPanel.add(stepNumLabel);
            
            // Role name
            JLabel roleLabel = new JLabel(formatRoleName(chain.get(i)));
            roleLabel.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
            roleLabel.setForeground(i <= currentStep ? Color.BLACK : Color.GRAY);
            stepPanel.add(roleLabel);
            
            // Approver name if completed
            if (i < currentStep && i < approverNames.size()) {
                JLabel approverLabel = new JLabel("(" + approverNames.get(i) + ")");
                approverLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 11));
                approverLabel.setForeground(new Color(40, 167, 69));
                stepPanel.add(approverLabel);
            }
            
            // "Waiting for you" indicator
            if (i == currentStep && canUserApprove()) {
                JLabel youLabel = new JLabel("← Waiting for your approval");
                youLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 11));
                youLabel.setForeground(new Color(13, 110, 253));
                stepPanel.add(youLabel);
            }
            
            content.add(stepPanel);
            
            // Add connector line between steps
            if (i < chain.size() - 1) {
                JLabel connector = new JLabel("     │");
                connector.setForeground(Color.LIGHT_GRAY);
                connector.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
                content.add(connector);
            }
        }
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createTypeSpecificSection() {
        if (request instanceof ItemClaimRequest) {
            return createItemClaimDetails((ItemClaimRequest) request);
        } else if (request instanceof CrossCampusTransferRequest) {
            return createCrossTransferDetails((CrossCampusTransferRequest) request);
        } else if (request instanceof TransitToUniversityTransferRequest) {
            return createTransitTransferDetails((TransitToUniversityTransferRequest) request);
        } else if (request instanceof AirportToUniversityTransferRequest) {
            return createAirportTransferDetails((AirportToUniversityTransferRequest) request);
        } else if (request instanceof PoliceEvidenceRequest) {
            return createPoliceEvidenceDetails((PoliceEvidenceRequest) request);
        }
        return null;
    }
    
    private JPanel createItemClaimDetails(ItemClaimRequest claimRequest) {
        JPanel section = createSection("📦 Item Claim Details");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(createFieldLabel("Item:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(claimRequest.getItemName()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(createFieldLabel("Value:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(String.format("$%.2f", claimRequest.getItemValue())), gbc);
        
        if (claimRequest.getClaimDetails() != null) {
            gbc.gridx = 0; gbc.gridy = 2;
            content.add(createFieldLabel("Claim Details:"), gbc);
            gbc.gridx = 1;
            content.add(createFieldValue(claimRequest.getClaimDetails()), gbc);
        }
        
        if (claimRequest.getIdentifyingFeatures() != null) {
            gbc.gridx = 0; gbc.gridy = 3;
            content.add(createFieldLabel("Identifying Features:"), gbc);
            gbc.gridx = 1;
            content.add(createFieldValue(claimRequest.getIdentifyingFeatures()), gbc);
        }
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createCrossTransferDetails(CrossCampusTransferRequest transferRequest) {
        JPanel section = createSection("🔄 Cross-Campus Transfer Details");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(createFieldLabel("Source Campus:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getSourceCampusName()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(createFieldLabel("Destination Campus:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getDestinationCampusName()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        content.add(createFieldLabel("Item:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getItemName()), gbc);
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createTransitTransferDetails(TransitToUniversityTransferRequest transferRequest) {
        JPanel section = createSection("🚇 Transit-to-University Transfer Details");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(createFieldLabel("Transit Station:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getStationName()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(createFieldLabel("Transit Line:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getTransitType() + " - " + transferRequest.getRouteNumber()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        content.add(createFieldLabel("Destination University:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getUniversityName()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        content.add(createFieldLabel("Item:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getItemName()), gbc);
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createAirportTransferDetails(AirportToUniversityTransferRequest transferRequest) {
        JPanel section = createSection("✈️ Airport-to-University Transfer Details");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(createFieldLabel("Airport:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue("Logan International Airport"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(createFieldLabel("Terminal:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getTerminalNumber()), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        content.add(createFieldLabel("Destination University:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(transferRequest.getUniversityName()), gbc);
        
        if (transferRequest.getFlightNumber() != null) {
            gbc.gridx = 0; gbc.gridy = 3;
            content.add(createFieldLabel("Flight Number:"), gbc);
            gbc.gridx = 1;
            content.add(createFieldValue(transferRequest.getFlightNumber()), gbc);
        }
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createPoliceEvidenceDetails(PoliceEvidenceRequest evidenceRequest) {
        JPanel section = createSection("🚔 Police Evidence Request Details");
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(createFieldLabel("Item:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(evidenceRequest.getItemName()), gbc);
        
        if (evidenceRequest.getSerialNumber() != null) {
            gbc.gridx = 0; gbc.gridy = 1;
            content.add(createFieldLabel("Serial Number:"), gbc);
            gbc.gridx = 1;
            content.add(createFieldValue(evidenceRequest.getSerialNumber()), gbc);
        }
        
        gbc.gridx = 0; gbc.gridy = 2;
        content.add(createFieldLabel("Verification Reason:"), gbc);
        gbc.gridx = 1;
        content.add(createFieldValue(evidenceRequest.getVerificationReason()), gbc);
        
        if (evidenceRequest.isStolenCheck()) {
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JLabel warningLabel = new JLabel("⚠️ SUSPECTED STOLEN - Requires verification against police database");
            warningLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
            warningLabel.setForeground(new Color(220, 53, 69));
            content.add(warningLabel, gbc);
        }
        
        section.add(content, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createNotesSection() {
        JPanel section = createSection("📝 Notes");
        
        JTextArea notesArea = new JTextArea(request.getNotes());
        notesArea.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        notesArea.setEditable(false);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(SECTION_BG);
        notesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        section.add(notesArea, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createCommentsSection() {
        JPanel section = createSection("💬 Your Comments (Optional)");
        
        commentsArea = new JTextArea(4, 50);
        commentsArea.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JScrollPane scrollPane = new JScrollPane(commentsArea);
        scrollPane.setBorder(null);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapper.add(scrollPane, BorderLayout.CENTER);
        
        JLabel hintLabel = new JLabel("Add any notes or justification for your decision");
        hintLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 11));
        hintLabel.setForeground(Color.GRAY);
        wrapper.add(hintLabel, BorderLayout.SOUTH);
        
        section.add(wrapper, BorderLayout.CENTER);
        return section;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));
        panel.setBackground(Color.WHITE);
        
        // Left side - Help text
        JLabel helpLabel = new JLabel();
        if (canUserApprove()) {
            helpLabel.setText("Review the details above and make your decision");
        } else if (!request.isPending()) {
            helpLabel.setText("This request has been " + request.getStatus().name().toLowerCase());
        } else {
            helpLabel.setText("Waiting for: " + formatRoleName(request.getNextRequiredRole()));
        }
        helpLabel.setFont(UIConstants.getEmojiFont(Font.ITALIC, 12));
        helpLabel.setForeground(Color.GRAY);
        panel.add(helpLabel, BorderLayout.WEST);
        
        // Right side - Buttons
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonGroup.setOpaque(false);
        
        // Approve button
        approveButton = new JButton("✓ Approve");
        approveButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        approveButton.setBackground(new Color(40, 167, 69));
        approveButton.setForeground(Color.WHITE);
        approveButton.setFocusPainted(false);
        approveButton.setBorderPainted(false);
        approveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        approveButton.setPreferredSize(new Dimension(120, 38));
        approveButton.addActionListener(e -> handleApprove());
        buttonGroup.add(approveButton);
        
        // Reject button
        rejectButton = new JButton("✗ Reject");
        rejectButton.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        rejectButton.setBackground(new Color(220, 53, 69));
        rejectButton.setForeground(Color.WHITE);
        rejectButton.setFocusPainted(false);
        rejectButton.setBorderPainted(false);
        rejectButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rejectButton.setPreferredSize(new Dimension(120, 38));
        rejectButton.addActionListener(e -> handleReject());
        buttonGroup.add(rejectButton);
        
        // Close button
        closeButton = new JButton("Close");
        closeButton.setFont(UIConstants.getEmojiFont(Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 38));
        closeButton.addActionListener(e -> dispose());
        buttonGroup.add(closeButton);
        
        panel.add(buttonGroup, BorderLayout.EAST);
        
        return panel;
    }
    
    // ==================== HELPER METHODS ====================
    
    private JPanel createSection(String title) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(SECTION_BG);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        // Title bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        titleBar.setBackground(new Color(233, 236, 239));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.getEmojiFont(Font.BOLD, 13));
        titleBar.add(titleLabel);
        
        section.add(titleBar, BorderLayout.NORTH);
        
        return section;
    }
    
    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.getEmojiFont(Font.BOLD, 12));
        label.setForeground(new Color(73, 80, 87));
        return label;
    }
    
    private JLabel createFieldValue(String text) {
        JLabel label = new JLabel(text != null ? text : "N/A");
        label.setFont(UIConstants.getEmojiFont(Font.PLAIN, 12));
        return label;
    }
    
    private JLabel createPriorityBadge(WorkRequest.RequestPriority priority) {
        JLabel badge = new JLabel();
        badge.setFont(UIConstants.getEmojiFont(Font.BOLD, 11));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        
        switch (priority) {
            case URGENT -> {
                badge.setText("🔴 URGENT");
                badge.setBackground(new Color(220, 53, 69));
                badge.setForeground(Color.WHITE);
            }
            case HIGH -> {
                badge.setText("🟠 HIGH");
                badge.setBackground(new Color(255, 152, 0));
                badge.setForeground(Color.WHITE);
            }
            case NORMAL -> {
                badge.setText("🔵 NORMAL");
                badge.setBackground(new Color(13, 110, 253));
                badge.setForeground(Color.WHITE);
            }
            case LOW -> {
                badge.setText("⚪ LOW");
                badge.setBackground(new Color(108, 117, 125));
                badge.setForeground(Color.WHITE);
            }
        }
        
        return badge;
    }
    
    private JLabel createStatusBadge(WorkRequest.RequestStatus status) {
        JLabel badge = new JLabel();
        badge.setFont(UIConstants.getEmojiFont(Font.BOLD, 11));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        
        switch (status) {
            case PENDING -> {
                badge.setText("⏳ PENDING");
                badge.setBackground(new Color(255, 193, 7));
                badge.setForeground(Color.BLACK);
            }
            case IN_PROGRESS -> {
                badge.setText("🔄 IN PROGRESS");
                badge.setBackground(new Color(13, 110, 253));
                badge.setForeground(Color.WHITE);
            }
            case APPROVED -> {
                badge.setText("✅ APPROVED");
                badge.setBackground(new Color(40, 167, 69));
                badge.setForeground(Color.WHITE);
            }
            case REJECTED -> {
                badge.setText("❌ REJECTED");
                badge.setBackground(new Color(220, 53, 69));
                badge.setForeground(Color.WHITE);
            }
            case COMPLETED -> {
                badge.setText("✔️ COMPLETED");
                badge.setBackground(new Color(0, 150, 136));
                badge.setForeground(Color.WHITE);
            }
            case CANCELLED -> {
                badge.setText("🚫 CANCELLED");
                badge.setBackground(new Color(108, 117, 125));
                badge.setForeground(Color.WHITE);
            }
        }
        
        return badge;
    }
    
    private String getTypeIcon(WorkRequest.RequestType type) {
        return switch (type) {
            case ITEM_CLAIM -> "📦";
            case CROSS_CAMPUS_TRANSFER -> "🔄";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "🚇";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "✈️";
            case POLICE_EVIDENCE_REQUEST -> "🚔";
            case MBTA_TO_AIRPORT_EMERGENCY -> "🚨";
            case MULTI_ENTERPRISE_DISPUTE -> "⚖️";
        };
    }
    
    private String getTypeDisplayName(WorkRequest.RequestType type) {
        return switch (type) {
            case ITEM_CLAIM -> "Item Claim Request";
            case CROSS_CAMPUS_TRANSFER -> "Cross-Campus Transfer";
            case TRANSIT_TO_UNIVERSITY_TRANSFER -> "Transit-to-University Transfer";
            case AIRPORT_TO_UNIVERSITY_TRANSFER -> "Airport-to-University Transfer";
            case POLICE_EVIDENCE_REQUEST -> "Police Evidence Request";
            case MBTA_TO_AIRPORT_EMERGENCY -> "MBTA-to-Airport Emergency";
            case MULTI_ENTERPRISE_DISPUTE -> "Multi-Enterprise Dispute Resolution";
        };
    }
    
    private String formatRoleName(String role) {
        if (role == null) return "N/A";
        return role.replace("_", " ")
                  .toLowerCase()
                  .replaceFirst(".", String.valueOf(Character.toUpperCase(role.charAt(0))));
    }
    
    /**
     * Look up enterprise name by ID
     */
    private String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null) return "N/A";
        try {
            return enterpriseDAO.findById(enterpriseId)
                .map(Enterprise::getName)
                .orElse("Unknown Enterprise");
        } catch (Exception e) {
            return "Unknown Enterprise";
        }
    }
    
    /**
     * Look up organization name by ID
     */
    private String getOrganizationName(String organizationId) {
        if (organizationId == null) return "N/A";
        try {
            return organizationDAO.findById(organizationId)
                .map(Organization::getName)
                .orElse("Unknown Organization");
        } catch (Exception e) {
            return "Unknown Organization";
        }
    }
    
    private boolean canUserApprove() {
        if (!request.isPending()) return false;
        
        String requiredRole = request.getNextRequiredRole();
        if (requiredRole == null) return false;
        
        // Check if current user has the required role
        boolean hasRequiredRole = currentUser.getRole().name().equals(requiredRole) ||
               (request.getCurrentApproverId() != null && 
                request.getCurrentApproverId().equals(currentUser.getEmail()));
        
        if (!hasRequiredRole) {
            return false;
        }
        
        // For CrossCampusTransferRequest, also check organization-level access
        if (request instanceof CrossCampusTransferRequest) {
            return canUserApproveCrossCampusTransfer((CrossCampusTransferRequest) request);
        }
        
        // For other request types, role match is sufficient
        return true;
    }
    
    /**
     * Check if current user can approve a CrossCampusTransferRequest based on organization.
     * Step 1 (source approval): User must be from the SOURCE organization
     * Step 2 (destination approval): User must be from the TARGET organization
     * 
     * @param ccRequest The cross-campus transfer request
     * @return true if user can approve this step
     */
    private boolean canUserApproveCrossCampusTransfer(CrossCampusTransferRequest ccRequest) {
        int currentStep = ccRequest.getApprovalStep();
        String userOrgId = currentUser.getOrganizationId();
        
        if (userOrgId == null) {
            return false; // Can't verify organization
        }
        
        if (currentStep == 0) {
            // Step 1: Source campus coordinator must approve
            // User must be from the requester's (source) organization
            String sourceOrgId = ccRequest.getRequesterOrganizationId();
            return sourceOrgId != null && userOrgId.equals(sourceOrgId);
            
        } else if (currentStep == 1) {
            // Step 2: Destination campus coordinator must approve
            // User must be from the target organization
            String targetOrgId = ccRequest.getTargetOrganizationId();
            return targetOrgId != null && userOrgId.equals(targetOrgId);
            
        } else if (currentStep == 2) {
            // Step 3: Student confirmation
            return currentUser.getRole().name().equals("STUDENT");
        }
        
        return false;
    }
    
    private void updateButtonStates() {
        boolean canApprove = canUserApprove();
        approveButton.setEnabled(canApprove);
        rejectButton.setEnabled(canApprove);
        
        if (!canApprove) {
            approveButton.setBackground(new Color(200, 200, 200));
            rejectButton.setBackground(new Color(200, 200, 200));
        }
    }
    
    // ==================== ACTION HANDLERS ====================
    
    private void handleApprove() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to APPROVE this request?\n\n" +
            "Request: " + request.getRequestSummary() + "\n" +
            "Requester: " + request.getRequesterName(),
            "Confirm Approval",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        setButtonsEnabled(false);
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                // Add comments to notes if provided
                if (commentsArea != null && !commentsArea.getText().trim().isEmpty()) {
                    String existingNotes = request.getNotes() != null ? request.getNotes() + "\n" : "";
                    request.setNotes(existingNotes + "APPROVED by " + currentUser.getFullName() + 
                        ": " + commentsArea.getText().trim());
                }
                
                return workRequestService.approveRequest(
                    request.getRequestId(),
                    currentUser.getEmail()  // Use email for unified lookup
                );
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Refresh request data
                        request = workRequestService.getRequestById(request.getRequestId());
                        
                        JOptionPane.showMessageDialog(RequestDetailDialog.this,
                            "Request approved successfully!\n\n" +
                            (request.getStatus() == WorkRequest.RequestStatus.APPROVED ?
                                "✅ All approvals complete - request is now APPROVED" :
                                "🔄 Routed to next approver: " + formatRoleName(request.getNextRequiredRole())),
                            "Approval Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Refresh UI
                        populateDetails();
                        updateButtonStates();
                        statusLabel.setText(createStatusBadge(request.getStatus()).getText());
                        
                        // Notify callback
                        if (onRequestUpdated != null) {
                            onRequestUpdated.accept(request);
                        }
                        
                    } else {
                        JOptionPane.showMessageDialog(RequestDetailDialog.this,
                            "Failed to approve request. Please try again.",
                            "Approval Failed",
                            JOptionPane.ERROR_MESSAGE);
                        setButtonsEnabled(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RequestDetailDialog.this,
                        "Error approving request: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    setButtonsEnabled(true);
                }
            }
        };
        worker.execute();
    }
    
    private void handleReject() {
        // Get rejection reason
        String[] reasons = {
            "Insufficient documentation",
            "Information mismatch",
            "Suspected fraud",
            "Policy violation",
            "Duplicate request",
            "Other (specify)"
        };
        
        String selectedReason = (String) JOptionPane.showInputDialog(this,
            "Select rejection reason:\n\n" +
            "Request: " + request.getRequestSummary() + "\n" +
            "Requester: " + request.getRequesterName(),
            "Reject Request",
            JOptionPane.WARNING_MESSAGE,
            null,
            reasons,
            reasons[0]);
        
        if (selectedReason == null) return;
        
        // Get custom reason if "Other" selected
        String finalReason = selectedReason;
        if (selectedReason.equals("Other (specify)")) {
            finalReason = JOptionPane.showInputDialog(this,
                "Please specify the rejection reason:",
                "Rejection Reason",
                JOptionPane.PLAIN_MESSAGE);
            if (finalReason == null || finalReason.trim().isEmpty()) return;
        }
        
        // Add comments if provided
        if (commentsArea != null && !commentsArea.getText().trim().isEmpty()) {
            finalReason += " - " + commentsArea.getText().trim();
        }
        
        final String reason = finalReason;
        
        // Confirm rejection
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ Are you sure you want to REJECT this request?\n\n" +
            "Reason: " + reason + "\n\n" +
            "This action cannot be undone.",
            "Confirm Rejection",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        setButtonsEnabled(false);
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return workRequestService.rejectRequest(
                    request.getRequestId(),
                    currentUser.getEmail(),  // Use email for unified lookup
                    reason
                );
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Refresh request data
                        request = workRequestService.getRequestById(request.getRequestId());
                        
                        JOptionPane.showMessageDialog(RequestDetailDialog.this,
                            "Request has been rejected.\n\n" +
                            "Reason: " + reason + "\n" +
                            "The requester will be notified.",
                            "Request Rejected",
                            JOptionPane.WARNING_MESSAGE);
                        
                        // Refresh UI
                        populateDetails();
                        updateButtonStates();
                        
                        // Notify callback
                        if (onRequestUpdated != null) {
                            onRequestUpdated.accept(request);
                        }
                        
                    } else {
                        JOptionPane.showMessageDialog(RequestDetailDialog.this,
                            "Failed to reject request. Please try again.",
                            "Rejection Failed",
                            JOptionPane.ERROR_MESSAGE);
                        setButtonsEnabled(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RequestDetailDialog.this,
                        "Error rejecting request: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    setButtonsEnabled(true);
                }
            }
        };
        worker.execute();
    }
    
    private void setButtonsEnabled(boolean enabled) {
        approveButton.setEnabled(enabled && canUserApprove());
        rejectButton.setEnabled(enabled && canUserApprove());
        closeButton.setEnabled(enabled);
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Set callback for when the request is updated (approved/rejected).
     */
    public void setOnRequestUpdated(Consumer<WorkRequest> callback) {
        this.onRequestUpdated = callback;
    }
    
    /**
     * Get the current request (may be updated after approval/rejection).
     */
    public WorkRequest getRequest() {
        return request;
    }
}
