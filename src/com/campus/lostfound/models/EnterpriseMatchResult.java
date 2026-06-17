package com.campus.lostfound.models;

import java.awt.Color;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a match result between items across enterprises.
 * 
 * This model extends the concept of a basic item match to include
 * enterprise context, transfer complexity, and trust score information.
 * It is used by EnterpriseItemMatcher to provide rich match results
 * that help users make informed decisions about cross-enterprise recoveries.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseMatchResult implements Comparable<EnterpriseMatchResult> {
    
    // ==================== ENUMS ====================
    
    /**
     * Type of match based on enterprise relationship
     */
    public enum MatchType {
        SAME_ORGANIZATION("Same Organization", "Items in the same organization", 1.0),
        SAME_ENTERPRISE("Same Enterprise", "Items in different orgs but same enterprise", 0.9),
        SAME_NETWORK("Same Network", "Items in different enterprises but same network", 0.7),
        CROSS_NETWORK("Cross Network", "Items across different networks", 0.5);
        
        private final String displayName;
        private final String description;
        private final double priorityMultiplier;
        
        MatchType(String displayName, String description, double priorityMultiplier) {
            this.displayName = displayName;
            this.description = description;
            this.priorityMultiplier = priorityMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getPriorityMultiplier() { return priorityMultiplier; }
    }
    
    /**
     * Transfer complexity between organizations
     */
    public enum TransferComplexity {
        NONE("No Transfer", "Same location, no transfer needed", "#4CAF50", 0),
        LOW("Low", "Same enterprise, simple internal transfer", "#8BC34A", 1),
        MEDIUM("Medium", "Cross-enterprise, standard coordination", "#FF9800", 2),
        HIGH("High", "Cross-network or requires special handling", "#F44336", 3);
        
        private final String label;
        private final String description;
        private final String colorHex;
        private final int level;
        
        TransferComplexity(String label, String description, String colorHex, int level) {
            this.label = label;
            this.description = description;
            this.colorHex = colorHex;
            this.level = level;
        }
        
        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public String getColorHex() { return colorHex; }
        public int getLevel() { return level; }
        
        public Color getColor() {
            return Color.decode(colorHex);
        }
    }
    
    /**
     * Score level for display purposes
     */
    public enum ScoreLevel {
        EXCELLENT("Excellent Match", 0.80, 1.00, "#4CAF50"),
        GOOD("Good Match", 0.60, 0.79, "#8BC34A"),
        FAIR("Fair Match", 0.40, 0.59, "#FF9800"),
        LOW("Possible Match", 0.00, 0.39, "#9E9E9E");
        
        private final String label;
        private final double minScore;
        private final double maxScore;
        private final String colorHex;
        
        ScoreLevel(String label, double minScore, double maxScore, String colorHex) {
            this.label = label;
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.colorHex = colorHex;
        }
        
        public String getLabel() { return label; }
        public double getMinScore() { return minScore; }
        public double getMaxScore() { return maxScore; }
        public String getColorHex() { return colorHex; }
        
        public Color getColor() {
            return Color.decode(colorHex);
        }
        
        public static ScoreLevel fromScore(double score) {
            if (score >= EXCELLENT.minScore) return EXCELLENT;
            if (score >= GOOD.minScore) return GOOD;
            if (score >= FAIR.minScore) return FAIR;
            return LOW;
        }
    }
    
    // ==================== CORE FIELDS ====================
    
    private String matchId;
    private Item sourceItem;        // The item being matched (usually the lost item)
    private Item matchedItem;       // The potential match (usually the found item)
    private double matchScore;      // Overall match score (0.0 - 1.0)
    private Date matchTimestamp;
    private MatchType matchType;
    
    // ==================== ENTERPRISE CONTEXT ====================
    
    // Source item enterprise context
    private String sourceEnterpriseId;
    private String sourceEnterpriseName;
    private String sourceOrganizationId;
    private String sourceOrganizationName;
    private String sourceNetworkId;
    private String sourceNetworkName;
    
    // Matched item enterprise context
    private String matchedEnterpriseId;
    private String matchedEnterpriseName;
    private String matchedOrganizationId;
    private String matchedOrganizationName;
    private String matchedNetworkId;
    private String matchedNetworkName;
    
    // ==================== DETAILED SCORING ====================
    
    private double categoryScore;    // Score from category matching
    private double keywordScore;     // Score from keyword similarity
    private double locationScore;    // Score from location proximity
    private double timeScore;        // Score from temporal proximity
    private double colorScore;       // Score from color matching
    private double brandScore;       // Score from brand matching
    private double enterpriseBonus;  // Bonus for same enterprise/network
    private double trustBonus;       // Bonus from high trust scores
    
    // ==================== USER CONTEXT ====================
    
    private String sourceUserId;
    private String sourceUserName;
    private double sourceUserTrustScore;
    
    private String matchedUserId;
    private String matchedUserName;
    private double matchedUserTrustScore;
    
    private boolean requiresVerification;
    private String verificationReason;
    
    // ==================== TRANSFER INFO ====================
    
    private boolean transferRequired;
    private TransferComplexity transferComplexity;
    private String estimatedTransferTime;
    private String transferNotes;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Default constructor
     */
    public EnterpriseMatchResult() {
        this.matchId = "EMR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.matchTimestamp = new Date();
        this.matchScore = 0.0;
        this.matchType = MatchType.CROSS_NETWORK;
        this.transferComplexity = TransferComplexity.MEDIUM;
        this.requiresVerification = false;
        this.transferRequired = true;
    }
    
    /**
     * Constructor with source and matched items
     */
    public EnterpriseMatchResult(Item sourceItem, Item matchedItem, double matchScore) {
        this();
        this.sourceItem = sourceItem;
        this.matchedItem = matchedItem;
        this.matchScore = Math.max(0.0, Math.min(1.0, matchScore)); // Clamp to 0-1
        
        // Auto-detect match type and transfer complexity
        detectMatchType();
        detectTransferComplexity();
    }
    
    /**
     * Full constructor
     */
    public EnterpriseMatchResult(Item sourceItem, Item matchedItem, double matchScore,
                                  MatchType matchType, TransferComplexity transferComplexity) {
        this(sourceItem, matchedItem, matchScore);
        this.matchType = matchType;
        this.transferComplexity = transferComplexity;
    }
    
    // ==================== AUTO-DETECTION ====================
    
    /**
     * Auto-detect match type based on enterprise/organization IDs
     */
    private void detectMatchType() {
        if (sourceItem == null || matchedItem == null) {
            this.matchType = MatchType.CROSS_NETWORK;
            return;
        }
        
        String srcOrg = sourceItem.getOrganizationId();
        String matchOrg = matchedItem.getOrganizationId();
        String srcEnt = sourceItem.getEnterpriseId();
        String matchEnt = matchedItem.getEnterpriseId();
        
        // Same organization
        if (srcOrg != null && srcOrg.equals(matchOrg)) {
            this.matchType = MatchType.SAME_ORGANIZATION;
            this.transferRequired = false;
            return;
        }
        
        // Same enterprise, different organization
        if (srcEnt != null && srcEnt.equals(matchEnt)) {
            this.matchType = MatchType.SAME_ENTERPRISE;
            this.transferRequired = true;
            return;
        }
        
        // Check if same network (would need network lookup)
        // For now, default to cross-network
        this.matchType = MatchType.CROSS_NETWORK;
        this.transferRequired = true;
    }
    
    /**
     * Auto-detect transfer complexity based on match type
     */
    private void detectTransferComplexity() {
        if (!transferRequired) {
            this.transferComplexity = TransferComplexity.NONE;
            this.estimatedTransferTime = "Immediate";
            return;
        }
        
        switch (matchType) {
            case SAME_ORGANIZATION:
                this.transferComplexity = TransferComplexity.NONE;
                this.estimatedTransferTime = "Immediate";
                break;
            case SAME_ENTERPRISE:
                this.transferComplexity = TransferComplexity.LOW;
                this.estimatedTransferTime = "1-2 business days";
                break;
            case SAME_NETWORK:
                this.transferComplexity = TransferComplexity.MEDIUM;
                this.estimatedTransferTime = "2-3 business days";
                break;
            case CROSS_NETWORK:
                this.transferComplexity = TransferComplexity.HIGH;
                this.estimatedTransferTime = "3-5 business days";
                break;
        }
        
        // Adjust for high-value items
        if (matchedItem != null && matchedItem.getEstimatedValue() >= 500) {
            if (this.transferComplexity != TransferComplexity.HIGH) {
                // Increase complexity for high-value items
                int newLevel = Math.min(this.transferComplexity.getLevel() + 1, 3);
                this.transferComplexity = TransferComplexity.values()[newLevel];
            }
            this.requiresVerification = true;
            this.verificationReason = "High-value item requires verification";
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if this is a cross-enterprise match
     */
    public boolean isCrossEnterprise() {
        return matchType == MatchType.SAME_NETWORK || matchType == MatchType.CROSS_NETWORK;
    }
    
    /**
     * Check if this is within the same enterprise
     */
    public boolean isSameEnterprise() {
        return matchType == MatchType.SAME_ORGANIZATION || matchType == MatchType.SAME_ENTERPRISE;
    }
    
    /**
     * Get score level for display
     */
    public ScoreLevel getScoreLevel() {
        return ScoreLevel.fromScore(matchScore);
    }
    
    /**
     * Get score label
     */
    public String getScoreLabel() {
        return getScoreLevel().getLabel();
    }
    
    /**
     * Get score color
     */
    public Color getScoreColor() {
        return getScoreLevel().getColor();
    }
    
    /**
     * Get score as percentage string
     */
    public String getScorePercentage() {
        return String.format("%.0f%%", matchScore * 100);
    }
    
    /**
     * Get formatted score breakdown
     */
    public String getScoreBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(String.format("%.0f%%", categoryScore * 100));
        sb.append(" | Keywords: ").append(String.format("%.0f%%", keywordScore * 100));
        sb.append(" | Location: ").append(String.format("%.0f%%", locationScore * 100));
        sb.append(" | Time: ").append(String.format("%.0f%%", timeScore * 100));
        
        if (enterpriseBonus > 0) {
            sb.append(" | Enterprise Bonus: +").append(String.format("%.0f%%", enterpriseBonus * 100));
        }
        if (trustBonus > 0) {
            sb.append(" | Trust Bonus: +").append(String.format("%.0f%%", trustBonus * 100));
        }
        
        return sb.toString();
    }
    
    /**
     * Get transfer info summary
     */
    public String getTransferSummary() {
        if (!transferRequired) {
            return "No transfer needed - same location";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Transfer: ").append(transferComplexity.getLabel());
        sb.append(" (").append(estimatedTransferTime).append(")");
        
        if (matchedEnterpriseName != null && sourceEnterpriseName != null) {
            sb.append("\nFrom: ").append(matchedEnterpriseName);
            sb.append(" → To: ").append(sourceEnterpriseName);
        }
        
        return sb.toString();
    }
    
    /**
     * Check if both users have good trust scores
     */
    public boolean hasTrustedUsers() {
        return sourceUserTrustScore >= 70 && matchedUserTrustScore >= 70;
    }
    
    /**
     * Get combined trust assessment
     */
    public String getTrustAssessment() {
        double avgTrust = (sourceUserTrustScore + matchedUserTrustScore) / 2;
        
        if (avgTrust >= 85) return "High Trust";
        if (avgTrust >= 70) return "Good Trust";
        if (avgTrust >= 50) return "Fair Trust";
        return "Low Trust - Verification Recommended";
    }
    
    /**
     * Calculate effective score (match score adjusted by type priority)
     */
    public double getEffectiveScore() {
        return matchScore * matchType.getPriorityMultiplier();
    }
    
    // ==================== COMPARABLE ====================
    
    @Override
    public int compareTo(EnterpriseMatchResult other) {
        // Primary sort: by match score (descending)
        int scoreCompare = Double.compare(other.matchScore, this.matchScore);
        if (scoreCompare != 0) return scoreCompare;
        
        // Secondary sort: by transfer complexity (ascending - prefer simpler)
        int complexityCompare = Integer.compare(
            this.transferComplexity.getLevel(), 
            other.transferComplexity.getLevel()
        );
        if (complexityCompare != 0) return complexityCompare;
        
        // Tertiary sort: by match type priority (descending)
        return Double.compare(
            other.matchType.getPriorityMultiplier(),
            this.matchType.getPriorityMultiplier()
        );
    }
    
    // ==================== DISPLAY HELPERS ====================
    
    /**
     * Get display row for table
     */
    public Object[] toTableRow() {
        return new Object[] {
            matchedItem != null ? matchedItem.getTitle() : "Unknown",
            matchedItem != null ? matchedItem.getCategory().getDisplayName() : "Unknown",
            getScorePercentage(),
            matchType.getDisplayName(),
            transferComplexity.getLabel(),
            matchedEnterpriseName != null ? matchedEnterpriseName : "Unknown",
            matchedOrganizationName != null ? matchedOrganizationName : "Unknown",
            estimatedTransferTime
        };
    }
    
    /**
     * Get column names for table
     */
    public static String[] getTableColumns() {
        return new String[] {
            "Item", "Category", "Match %", "Match Type", 
            "Transfer", "Enterprise", "Organization", "Est. Time"
        };
    }
    
    // ==================== TOSTRING ====================
    
    @Override
    public String toString() {
        return String.format("EnterpriseMatchResult[id=%s, score=%.2f, type=%s, " +
                            "source=%s, matched=%s, transfer=%s]",
            matchId, matchScore, matchType,
            sourceItem != null ? sourceItem.getTitle() : "null",
            matchedItem != null ? matchedItem.getTitle() : "null",
            transferComplexity.getLabel()
        );
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    
    public Item getSourceItem() { return sourceItem; }
    public void setSourceItem(Item sourceItem) { 
        this.sourceItem = sourceItem;
        detectMatchType();
        detectTransferComplexity();
    }
    
    public Item getMatchedItem() { return matchedItem; }
    public void setMatchedItem(Item matchedItem) { 
        this.matchedItem = matchedItem;
        detectMatchType();
        detectTransferComplexity();
    }
    
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { 
        this.matchScore = Math.max(0.0, Math.min(1.0, matchScore)); 
    }
    
    public Date getMatchTimestamp() { return matchTimestamp; }
    public void setMatchTimestamp(Date matchTimestamp) { this.matchTimestamp = matchTimestamp; }
    
    public MatchType getMatchType() { return matchType; }
    public void setMatchType(MatchType matchType) { this.matchType = matchType; }
    
    // Enterprise context - Source
    public String getSourceEnterpriseId() { return sourceEnterpriseId; }
    public void setSourceEnterpriseId(String sourceEnterpriseId) { this.sourceEnterpriseId = sourceEnterpriseId; }
    
    public String getSourceEnterpriseName() { return sourceEnterpriseName; }
    public void setSourceEnterpriseName(String sourceEnterpriseName) { this.sourceEnterpriseName = sourceEnterpriseName; }
    
    public String getSourceOrganizationId() { return sourceOrganizationId; }
    public void setSourceOrganizationId(String sourceOrganizationId) { this.sourceOrganizationId = sourceOrganizationId; }
    
    public String getSourceOrganizationName() { return sourceOrganizationName; }
    public void setSourceOrganizationName(String sourceOrganizationName) { this.sourceOrganizationName = sourceOrganizationName; }
    
    public String getSourceNetworkId() { return sourceNetworkId; }
    public void setSourceNetworkId(String sourceNetworkId) { this.sourceNetworkId = sourceNetworkId; }
    
    public String getSourceNetworkName() { return sourceNetworkName; }
    public void setSourceNetworkName(String sourceNetworkName) { this.sourceNetworkName = sourceNetworkName; }
    
    // Enterprise context - Matched
    public String getMatchedEnterpriseId() { return matchedEnterpriseId; }
    public void setMatchedEnterpriseId(String matchedEnterpriseId) { this.matchedEnterpriseId = matchedEnterpriseId; }
    
    public String getMatchedEnterpriseName() { return matchedEnterpriseName; }
    public void setMatchedEnterpriseName(String matchedEnterpriseName) { this.matchedEnterpriseName = matchedEnterpriseName; }
    
    public String getMatchedOrganizationId() { return matchedOrganizationId; }
    public void setMatchedOrganizationId(String matchedOrganizationId) { this.matchedOrganizationId = matchedOrganizationId; }
    
    public String getMatchedOrganizationName() { return matchedOrganizationName; }
    public void setMatchedOrganizationName(String matchedOrganizationName) { this.matchedOrganizationName = matchedOrganizationName; }
    
    public String getMatchedNetworkId() { return matchedNetworkId; }
    public void setMatchedNetworkId(String matchedNetworkId) { this.matchedNetworkId = matchedNetworkId; }
    
    public String getMatchedNetworkName() { return matchedNetworkName; }
    public void setMatchedNetworkName(String matchedNetworkName) { this.matchedNetworkName = matchedNetworkName; }
    
    // Detailed scoring
    public double getCategoryScore() { return categoryScore; }
    public void setCategoryScore(double categoryScore) { this.categoryScore = categoryScore; }
    
    public double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(double keywordScore) { this.keywordScore = keywordScore; }
    
    public double getLocationScore() { return locationScore; }
    public void setLocationScore(double locationScore) { this.locationScore = locationScore; }
    
    public double getTimeScore() { return timeScore; }
    public void setTimeScore(double timeScore) { this.timeScore = timeScore; }
    
    public double getColorScore() { return colorScore; }
    public void setColorScore(double colorScore) { this.colorScore = colorScore; }
    
    public double getBrandScore() { return brandScore; }
    public void setBrandScore(double brandScore) { this.brandScore = brandScore; }
    
    public double getEnterpriseBonus() { return enterpriseBonus; }
    public void setEnterpriseBonus(double enterpriseBonus) { this.enterpriseBonus = enterpriseBonus; }
    
    public double getTrustBonus() { return trustBonus; }
    public void setTrustBonus(double trustBonus) { this.trustBonus = trustBonus; }
    
    // User context
    public String getSourceUserId() { return sourceUserId; }
    public void setSourceUserId(String sourceUserId) { this.sourceUserId = sourceUserId; }
    
    public String getSourceUserName() { return sourceUserName; }
    public void setSourceUserName(String sourceUserName) { this.sourceUserName = sourceUserName; }
    
    public double getSourceUserTrustScore() { return sourceUserTrustScore; }
    public void setSourceUserTrustScore(double sourceUserTrustScore) { this.sourceUserTrustScore = sourceUserTrustScore; }
    
    public String getMatchedUserId() { return matchedUserId; }
    public void setMatchedUserId(String matchedUserId) { this.matchedUserId = matchedUserId; }
    
    public String getMatchedUserName() { return matchedUserName; }
    public void setMatchedUserName(String matchedUserName) { this.matchedUserName = matchedUserName; }
    
    public double getMatchedUserTrustScore() { return matchedUserTrustScore; }
    public void setMatchedUserTrustScore(double matchedUserTrustScore) { this.matchedUserTrustScore = matchedUserTrustScore; }
    
    public boolean isRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(boolean requiresVerification) { this.requiresVerification = requiresVerification; }
    
    public String getVerificationReason() { return verificationReason; }
    public void setVerificationReason(String verificationReason) { this.verificationReason = verificationReason; }
    
    // Transfer info
    public boolean isTransferRequired() { return transferRequired; }
    public void setTransferRequired(boolean transferRequired) { this.transferRequired = transferRequired; }
    
    public TransferComplexity getTransferComplexity() { return transferComplexity; }
    public void setTransferComplexity(TransferComplexity transferComplexity) { this.transferComplexity = transferComplexity; }
    
    public String getEstimatedTransferTime() { return estimatedTransferTime; }
    public void setEstimatedTransferTime(String estimatedTransferTime) { this.estimatedTransferTime = estimatedTransferTime; }
    
    public String getTransferNotes() { return transferNotes; }
    public void setTransferNotes(String transferNotes) { this.transferNotes = transferNotes; }
}
