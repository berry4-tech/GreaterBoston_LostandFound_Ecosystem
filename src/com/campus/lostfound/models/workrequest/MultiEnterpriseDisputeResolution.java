package com.campus.lostfound.models.workrequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Work request for resolving disputes when multiple claimants from different enterprises
 * claim the same item. Requires verification from all involved enterprises.
 * 
 * Use Case (from PDF - Work Request Type #7):
 * - Multiple claimants → All 4 enterprises verify → Panel decision
 * - Cross-Enterprise: Yes (All enterprises)
 * 
 * Example Scenario:
 * - A laptop is found at MBTA
 * - NEU student claims it's theirs (lost on Green Line)
 * - BU student also claims it's theirs (lost near Kenmore)
 * - Both have similar descriptions, need multi-enterprise verification
 * - Panel from University, MBTA, and Police must review evidence and decide
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class MultiEnterpriseDisputeResolution extends WorkRequest {
    
    // Disputed item details
    private String itemId;
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private double estimatedValue;
    private String itemCurrentLocation;       // Where the item is currently held
    private String holdingEnterpriseId;       // Enterprise currently holding item
    private String holdingEnterpriseName;
    
    // Claimants (multiple people claiming the same item)
    private List<Claimant> claimants;
    
    // Enterprises involved in the dispute
    private List<String> involvedEnterpriseIds;
    private List<String> involvedEnterpriseNames;
    
    // Verification panel
    private List<PanelMember> verificationPanel;
    private int panelVotesRequired;           // How many panel members must vote
    private int panelVotesReceived;
    
    // Evidence collected
    private List<EvidenceItem> evidenceItems;
    
    // Dispute details
    private String disputeType;               // "OWNERSHIP", "PRIORITY", "AUTHENTICITY"
    private String disputeReason;             // Why there's a dispute
    private String disputeInitiatedBy;        // Who flagged the conflict
    private String disputeInitiatedByName;
    
    // Resolution
    private String resolutionStatus;          // "PENDING", "UNDER_REVIEW", "RESOLVED", "ESCALATED"
    private String resolutionDecision;        // Final decision
    private String winningClaimantId;         // Who gets the item
    private String winningClaimantName;
    private String resolutionReason;          // Why this decision was made
    private String resolutionNotes;           // Additional notes
    
    // Police involvement (for high-value or suspected fraud)
    private boolean policeInvolved;
    private String policeOfficerId;
    private String policeOfficerName;
    private String policeReportNumber;
    private String policeFindingsReport;
    
    // Timeline tracking
    private String disputeStartDate;
    private String evidenceDeadline;          // When all evidence must be submitted
    private String panelReviewDate;           // When panel meets
    private String resolutionDeadline;        // When decision must be made
    
    // Constructor
    public MultiEnterpriseDisputeResolution() {
        super();
        this.requestType = RequestType.MULTI_ENTERPRISE_DISPUTE;
        this.priority = RequestPriority.HIGH; // Disputes are high priority
        this.claimants = new ArrayList<>();
        this.involvedEnterpriseIds = new ArrayList<>();
        this.involvedEnterpriseNames = new ArrayList<>();
        this.verificationPanel = new ArrayList<>();
        this.evidenceItems = new ArrayList<>();
        this.panelVotesRequired = 3; // Default: 3 panel members must vote
        this.panelVotesReceived = 0;
        this.resolutionStatus = "PENDING";
    }
    
    // Convenience constructor
    public MultiEnterpriseDisputeResolution(String initiatorId, String initiatorName,
                                            String itemId, String itemName,
                                            String disputeReason) {
        this();
        this.disputeInitiatedBy = initiatorId;
        this.disputeInitiatedByName = initiatorName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.disputeReason = disputeReason;
        this.requesterId = initiatorId;
        this.requesterName = initiatorName;
        this.description = String.format("Multi-enterprise dispute for %s: %s", 
                                        itemName, disputeReason);
    }
    
    @Override
    public List<String> getApprovalChain() {
        // Disputes go directly to Police Evidence Custodian for resolution
        // The police panel has the full UI for reviewing all claimants,
        // evidence, voting, and making the final decision.
        // 
        // Note: All involved enterprises can VIEW the dispute, but
        // the Police are the authority for ownership disputes.
        return Arrays.asList(
            "POLICE_EVIDENCE_CUSTODIAN"     // Police handles dispute resolution
        );
    }
    
    @Override
    public String getRequestSummary() {
        return String.format("⚖️ DISPUTE: %s - %d claimants from %d enterprises", 
                            itemName, 
                            claimants.size(),
                            involvedEnterpriseNames.size());
    }
    
    @Override
    public boolean isValid() {
        // Must have item and at least 2 claimants for a dispute
        if (itemId == null || itemId.isEmpty()) return false;
        if (claimants.size() < 2) return false;
        if (disputeReason == null || disputeReason.isEmpty()) return false;
        
        return true;
    }
    
    // ==================== CLAIMANT MANAGEMENT ====================
    
    /**
     * Add a claimant to the dispute
     */
    public void addClaimant(Claimant claimant) {
        claimants.add(claimant);
        
        // Track enterprises involved
        if (claimant.enterpriseId != null && 
            !involvedEnterpriseIds.contains(claimant.enterpriseId)) {
            involvedEnterpriseIds.add(claimant.enterpriseId);
            involvedEnterpriseNames.add(claimant.enterpriseName);
        }
        
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Add a claimant with basic info
     */
    public void addClaimant(String claimantId, String claimantName, 
                           String enterpriseId, String enterpriseName,
                           String claimDescription) {
        Claimant c = new Claimant();
        c.claimantId = claimantId;
        c.claimantName = claimantName;
        c.enterpriseId = enterpriseId;
        c.enterpriseName = enterpriseName;
        c.claimDescription = claimDescription;
        c.claimSubmittedDate = java.time.LocalDateTime.now().toString();
        addClaimant(c);
    }
    
    /**
     * Get number of claimants
     */
    public int getClaimantCount() {
        return claimants.size();
    }
    
    // ==================== PANEL MANAGEMENT ====================
    
    /**
     * Add a panel member
     */
    public void addPanelMember(PanelMember member) {
        verificationPanel.add(member);
    }
    
    /**
     * Add panel member with basic info
     */
    public void addPanelMember(String memberId, String memberName, 
                               String role, String enterpriseName) {
        PanelMember pm = new PanelMember();
        pm.memberId = memberId;
        pm.memberName = memberName;
        pm.role = role;
        pm.enterpriseName = enterpriseName;
        pm.hasVoted = false;
        verificationPanel.add(pm);
    }
    
    /**
     * Record a panel member's vote
     */
    public void recordPanelVote(String memberId, String votedForClaimantId, String reason) {
        for (PanelMember pm : verificationPanel) {
            if (pm.memberId.equals(memberId)) {
                pm.hasVoted = true;
                pm.votedForClaimantId = votedForClaimantId;
                pm.voteReason = reason;
                pm.voteDate = java.time.LocalDateTime.now().toString();
                panelVotesReceived++;
                break;
            }
        }
        
        // Check if all required votes are in
        if (panelVotesReceived >= panelVotesRequired) {
            this.resolutionStatus = "UNDER_REVIEW";
            determineResolution();
        }
        
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Determine resolution based on panel votes
     */
    private void determineResolution() {
        // Count votes for each claimant
        java.util.Map<String, Integer> voteCounts = new java.util.HashMap<>();
        
        for (PanelMember pm : verificationPanel) {
            if (pm.hasVoted && pm.votedForClaimantId != null) {
                voteCounts.merge(pm.votedForClaimantId, 1, Integer::sum);
            }
        }
        
        // Find the claimant with most votes
        String winnerId = null;
        int maxVotes = 0;
        
        for (java.util.Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winnerId = entry.getKey();
            }
        }
        
        // If there's a clear winner (majority)
        if (winnerId != null && maxVotes > panelVotesRequired / 2) {
            this.winningClaimantId = winnerId;
            
            // Find winner name
            for (Claimant c : claimants) {
                if (c.claimantId.equals(winnerId)) {
                    this.winningClaimantName = c.claimantName;
                    break;
                }
            }
            
            this.resolutionStatus = "RESOLVED";
            this.resolutionDecision = "AWARDED";
            this.resolutionReason = String.format("Panel voted %d-%d in favor of %s", 
                                                 maxVotes, panelVotesReceived - maxVotes, 
                                                 winningClaimantName);
        } else {
            // Tie or no clear majority - escalate to police
            this.resolutionStatus = "ESCALATED";
            this.policeInvolved = true;
            this.resolutionDecision = "ESCALATED_TO_POLICE";
            this.resolutionReason = "No clear majority. Escalated to police for further investigation.";
        }
    }
    
    // ==================== EVIDENCE MANAGEMENT ====================
    
    /**
     * Add evidence to the dispute
     */
    public void addEvidence(EvidenceItem evidence) {
        evidenceItems.add(evidence);
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Add evidence with basic info
     */
    public void addEvidence(String submittedBy, String submittedByName,
                           String evidenceType, String description,
                           String documentPath) {
        EvidenceItem e = new EvidenceItem();
        e.evidenceId = "EV-" + System.currentTimeMillis();
        e.submittedById = submittedBy;
        e.submittedByName = submittedByName;
        e.evidenceType = evidenceType;
        e.description = description;
        e.documentPath = documentPath;
        e.submittedDate = java.time.LocalDateTime.now().toString();
        e.verified = false;
        addEvidence(e);
    }
    
    /**
     * Verify a piece of evidence
     */
    public void verifyEvidence(String evidenceId, String verifiedBy, boolean isValid) {
        for (EvidenceItem e : evidenceItems) {
            if (e.evidenceId.equals(evidenceId)) {
                e.verified = true;
                e.verifiedById = verifiedBy;
                e.verificationResult = isValid ? "VALID" : "INVALID";
                e.verificationDate = java.time.LocalDateTime.now().toString();
                break;
            }
        }
    }
    
    // ==================== RESOLUTION MANAGEMENT ====================
    
    /**
     * Manually set resolution (for admin override)
     */
    public void setManualResolution(String winnerId, String winnerName, 
                                    String reason, String decidedBy) {
        this.winningClaimantId = winnerId;
        this.winningClaimantName = winnerName;
        this.resolutionReason = reason;
        this.resolutionStatus = "RESOLVED";
        this.resolutionDecision = "ADMIN_DECISION";
        this.resolutionNotes = "Manual resolution by " + decidedBy + ": " + reason;
        this.complete();
    }
    
    /**
     * Escalate to police
     */
    public void escalateToPolice(String officerId, String officerName, String reason) {
        this.policeInvolved = true;
        this.policeOfficerId = officerId;
        this.policeOfficerName = officerName;
        this.resolutionStatus = "ESCALATED";
        addNote("Escalated to police officer " + officerName + ": " + reason);
    }
    
    /**
     * Record police findings
     */
    public void recordPoliceFindings(String reportNumber, String findings) {
        this.policeReportNumber = reportNumber;
        this.policeFindingsReport = findings;
        addNote("Police Report #" + reportNumber + ": " + findings);
    }
    
    /**
     * Add note to resolution
     */
    public void addNote(String note) {
        String timestamp = java.time.LocalDateTime.now().toString();
        if (resolutionNotes == null) {
            resolutionNotes = "[" + timestamp + "] " + note;
        } else {
            resolutionNotes += "\n[" + timestamp + "] " + note;
        }
        this.lastUpdatedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Get dispute status summary
     */
    public String getStatusSummary() {
        return String.format("Dispute Status: %s | Claimants: %d | Panel Votes: %d/%d | Resolution: %s",
                            resolutionStatus, claimants.size(), 
                            panelVotesReceived, panelVotesRequired,
                            resolutionDecision != null ? resolutionDecision : "Pending");
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Represents a claimant in the dispute
     */
    public static class Claimant {
        public String claimantId;
        public String claimantName;
        public String claimantEmail;
        public String claimantPhone;
        public String enterpriseId;
        public String enterpriseName;
        public String organizationId;
        public String organizationName;
        public String claimDescription;       // Why they believe it's theirs
        public String proofDescription;       // What proof they have
        public double trustScore;             // Their trust score
        public String claimSubmittedDate;
        public String claimStatus;            // "SUBMITTED", "VERIFIED", "REJECTED"
        public List<String> evidenceIds;      // IDs of evidence they submitted
        
        public Claimant() {
            this.evidenceIds = new ArrayList<>();
            this.claimStatus = "SUBMITTED";
        }
        
        @Override
        public String toString() {
            return String.format("Claimant{id=%s, name=%s, enterprise=%s, status=%s}",
                    claimantId, claimantName, enterpriseName, claimStatus);
        }
    }
    
    /**
     * Represents a panel member who votes on the dispute
     */
    public static class PanelMember {
        public String memberId;
        public String memberName;
        public String role;                   // e.g., "CAMPUS_COORDINATOR"
        public String enterpriseId;
        public String enterpriseName;
        public boolean hasVoted;
        public String votedForClaimantId;     // Who they voted for
        public String voteReason;             // Why they voted this way
        public String voteDate;
        
        @Override
        public String toString() {
            return String.format("PanelMember{name=%s, role=%s, voted=%s}",
                    memberName, role, hasVoted);
        }
    }
    
    /**
     * Represents evidence submitted for the dispute
     */
    public static class EvidenceItem {
        public String evidenceId;
        public String submittedById;
        public String submittedByName;
        public String forClaimantId;          // Which claimant this supports
        public String evidenceType;           // "RECEIPT", "PHOTO", "SERIAL_NUMBER", "WITNESS"
        public String description;
        public String documentPath;           // Path to uploaded file
        public String submittedDate;
        public boolean verified;
        public String verifiedById;
        public String verificationResult;     // "VALID", "INVALID", "INCONCLUSIVE"
        public String verificationDate;
        public String verificationNotes;
        
        @Override
        public String toString() {
            return String.format("Evidence{id=%s, type=%s, verified=%s, result=%s}",
                    evidenceId, evidenceType, verified, verificationResult);
        }
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public String getItemCategory() {
        return itemCategory;
    }
    
    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }
    
    public double getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    public String getItemCurrentLocation() {
        return itemCurrentLocation;
    }
    
    public void setItemCurrentLocation(String itemCurrentLocation) {
        this.itemCurrentLocation = itemCurrentLocation;
    }
    
    public String getHoldingEnterpriseId() {
        return holdingEnterpriseId;
    }
    
    public void setHoldingEnterpriseId(String holdingEnterpriseId) {
        this.holdingEnterpriseId = holdingEnterpriseId;
    }
    
    public String getHoldingEnterpriseName() {
        return holdingEnterpriseName;
    }
    
    public void setHoldingEnterpriseName(String holdingEnterpriseName) {
        this.holdingEnterpriseName = holdingEnterpriseName;
    }
    
    public List<Claimant> getClaimants() {
        return claimants;
    }
    
    public void setClaimants(List<Claimant> claimants) {
        this.claimants = claimants;
    }
    
    public List<String> getInvolvedEnterpriseIds() {
        return involvedEnterpriseIds;
    }
    
    public void setInvolvedEnterpriseIds(List<String> involvedEnterpriseIds) {
        this.involvedEnterpriseIds = involvedEnterpriseIds;
    }
    
    public List<String> getInvolvedEnterpriseNames() {
        return involvedEnterpriseNames;
    }
    
    public void setInvolvedEnterpriseNames(List<String> involvedEnterpriseNames) {
        this.involvedEnterpriseNames = involvedEnterpriseNames;
    }
    
    public List<PanelMember> getVerificationPanel() {
        return verificationPanel;
    }
    
    public void setVerificationPanel(List<PanelMember> verificationPanel) {
        this.verificationPanel = verificationPanel;
    }
    
    public int getPanelVotesRequired() {
        return panelVotesRequired;
    }
    
    public void setPanelVotesRequired(int panelVotesRequired) {
        this.panelVotesRequired = panelVotesRequired;
    }
    
    public int getPanelVotesReceived() {
        return panelVotesReceived;
    }
    
    public void setPanelVotesReceived(int panelVotesReceived) {
        this.panelVotesReceived = panelVotesReceived;
    }
    
    public List<EvidenceItem> getEvidenceItems() {
        return evidenceItems;
    }
    
    public void setEvidenceItems(List<EvidenceItem> evidenceItems) {
        this.evidenceItems = evidenceItems;
    }
    
    public String getDisputeType() {
        return disputeType;
    }
    
    public void setDisputeType(String disputeType) {
        this.disputeType = disputeType;
    }
    
    public String getDisputeReason() {
        return disputeReason;
    }
    
    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }
    
    public String getDisputeInitiatedBy() {
        return disputeInitiatedBy;
    }
    
    public void setDisputeInitiatedBy(String disputeInitiatedBy) {
        this.disputeInitiatedBy = disputeInitiatedBy;
    }
    
    public String getDisputeInitiatedByName() {
        return disputeInitiatedByName;
    }
    
    public void setDisputeInitiatedByName(String disputeInitiatedByName) {
        this.disputeInitiatedByName = disputeInitiatedByName;
    }
    
    public String getResolutionStatus() {
        return resolutionStatus;
    }
    
    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }
    
    public String getResolutionDecision() {
        return resolutionDecision;
    }
    
    public void setResolutionDecision(String resolutionDecision) {
        this.resolutionDecision = resolutionDecision;
    }
    
    public String getWinningClaimantId() {
        return winningClaimantId;
    }
    
    public void setWinningClaimantId(String winningClaimantId) {
        this.winningClaimantId = winningClaimantId;
    }
    
    public String getWinningClaimantName() {
        return winningClaimantName;
    }
    
    public void setWinningClaimantName(String winningClaimantName) {
        this.winningClaimantName = winningClaimantName;
    }
    
    public String getResolutionReason() {
        return resolutionReason;
    }
    
    public void setResolutionReason(String resolutionReason) {
        this.resolutionReason = resolutionReason;
    }
    
    public String getResolutionNotes() {
        return resolutionNotes;
    }
    
    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
    
    public boolean isPoliceInvolved() {
        return policeInvolved;
    }
    
    public void setPoliceInvolved(boolean policeInvolved) {
        this.policeInvolved = policeInvolved;
    }
    
    public String getPoliceOfficerId() {
        return policeOfficerId;
    }
    
    public void setPoliceOfficerId(String policeOfficerId) {
        this.policeOfficerId = policeOfficerId;
    }
    
    public String getPoliceOfficerName() {
        return policeOfficerName;
    }
    
    public void setPoliceOfficerName(String policeOfficerName) {
        this.policeOfficerName = policeOfficerName;
    }
    
    public String getPoliceReportNumber() {
        return policeReportNumber;
    }
    
    public void setPoliceReportNumber(String policeReportNumber) {
        this.policeReportNumber = policeReportNumber;
    }
    
    public String getPoliceFindingsReport() {
        return policeFindingsReport;
    }
    
    public void setPoliceFindingsReport(String policeFindingsReport) {
        this.policeFindingsReport = policeFindingsReport;
    }
    
    public String getDisputeStartDate() {
        return disputeStartDate;
    }
    
    public void setDisputeStartDate(String disputeStartDate) {
        this.disputeStartDate = disputeStartDate;
    }
    
    public String getEvidenceDeadline() {
        return evidenceDeadline;
    }
    
    public void setEvidenceDeadline(String evidenceDeadline) {
        this.evidenceDeadline = evidenceDeadline;
    }
    
    public String getPanelReviewDate() {
        return panelReviewDate;
    }
    
    public void setPanelReviewDate(String panelReviewDate) {
        this.panelReviewDate = panelReviewDate;
    }
    
    public String getResolutionDeadline() {
        return resolutionDeadline;
    }
    
    public void setResolutionDeadline(String resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
    }
    
    @Override
    public String toString() {
        return String.format("MultiEnterpriseDisputeResolution{id=%s, item=%s, claimants=%d, enterprises=%d, status=%s, resolution=%s}",
                requestId, itemName, claimants.size(), involvedEnterpriseNames.size(), 
                resolutionStatus, resolutionDecision);
    }
}
