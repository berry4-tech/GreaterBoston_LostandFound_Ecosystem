package com.campus.lostfound.models.workrequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Work request for claiming a lost item.
 * 
 * Approval chain is DYNAMIC based on where the item is held:
 * - Same enterprise (campus item): Campus Coordinator only
 * - MBTA item: Campus Coordinator → Station Manager
 * - Airport item: Campus Coordinator → Airport Specialist
 * - High-value items: Add Police Evidence Custodian at the end
 * 
 * This ensures the holding organization must approve before releasing the item.
 */
public class ItemClaimRequest extends WorkRequest {
    
    // Item-specific fields
    private String itemId;              // The found item being claimed
    private String lostItemId;          // The student's lost item (for matching)
    private String itemName;            // For display
    private String itemCategory;        // Electronics, Documents, etc.
    private double itemValue;           // Estimated value in USD
    private boolean isHighValue;        // True if value > $500
    
    // Claim details
    private String claimDetails;        // Why claimant believes it's theirs
    private String proofDescription;    // Description of proof they can provide
    private String identifyingFeatures; // Features only owner would know
    
    // Evidence photos (if any)
    private String claimPhotoUrl;       // Optional photo as proof
    
    // Location context
    private String foundLocationId;     // Building/location where found
    private String foundLocationName;   // For display
    
    // ==================== NEW FIELDS FOR DYNAMIC ROUTING ====================
    
    /**
     * The type of enterprise holding the item.
     * Used to determine which external approver is needed.
     * Values: "HIGHER_EDUCATION", "PUBLIC_TRANSIT", "AIRPORT", "LAW_ENFORCEMENT"
     */
    private String itemHoldingEnterpriseType;
    
    /**
     * Name of the enterprise holding the item (for display purposes)
     */
    private String itemHoldingEnterpriseName;
    
    // Constructor
    public ItemClaimRequest() {
        super();
        this.requestType = RequestType.ITEM_CLAIM;
        this.isHighValue = false;
    }
    
    // Convenience constructor
    public ItemClaimRequest(String requesterId, String requesterName, 
                           String itemId, String itemName, double itemValue) {
        this();
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemValue = itemValue;
        this.isHighValue = itemValue > 500.0;
        this.description = String.format("Claim request for %s (valued at $%.2f)", 
                                        itemName, itemValue);
    }
    
    /**
     * Returns the DYNAMIC approval chain based on:
     * 1. Where the item is currently held (enterprise type)
     * 2. Whether the item is high-value (requires police verification)
     * 
     * This ensures cross-enterprise claims are routed to the holding organization.
     */
    @Override
    public List<String> getApprovalChain() {
        List<String> chain = new ArrayList<>();
        
        // Step 1: Campus Coordinator always reviews first (verifies student identity)
        chain.add("CAMPUS_COORDINATOR");
        
        // Step 2: Add holding enterprise's approval if external
        String externalApproverRole = getHoldingEnterpriseApprovalRole();
        if (externalApproverRole != null) {
            chain.add(externalApproverRole);
        }
        
        // Step 3: High-value items need police verification at the end
        if (isHighValue) {
            // Avoid duplicate if police already in chain
            if (!"POLICE_EVIDENCE_CUSTODIAN".equals(externalApproverRole)) {
                chain.add("POLICE_EVIDENCE_CUSTODIAN");
            }
        }
        
        return chain;
    }
    
    /**
     * Determines the approval role required from the holding enterprise.
     * 
     * @return Role name for external approval, or null if item is at same enterprise
     */
    private String getHoldingEnterpriseApprovalRole() {
        // If no enterprise type set, assume same enterprise (backward compatibility)
        if (itemHoldingEnterpriseType == null || itemHoldingEnterpriseType.isEmpty()) {
            return null;
        }
        
        // Map enterprise type to the appropriate approver role
        switch (itemHoldingEnterpriseType.toUpperCase()) {
            case "PUBLIC_TRANSIT":
                return "STATION_MANAGER";
            case "AIRPORT":
                return "AIRPORT_LOST_FOUND_SPECIALIST";
            case "LAW_ENFORCEMENT":
                return "POLICE_EVIDENCE_CUSTODIAN";
            case "HIGHER_EDUCATION":
                // Same enterprise type as student - check if different org
                if (isExternalOrganization()) {
                    // Different campus within same enterprise type
                    // Still needs campus coordinator from that campus
                    // But we handle this at org level, not enterprise level
                    return null;
                }
                return null;
            default:
                return null;
        }
    }
    
    /**
     * Checks if the item is at a different organization than the requester.
     * Used to determine if cross-campus coordination is needed.
     */
    private boolean isExternalOrganization() {
        if (targetOrganizationId == null || requesterOrganizationId == null) {
            return false;
        }
        return !targetOrganizationId.equals(requesterOrganizationId);
    }
    
    /**
     * Checks if this claim requires approval from an external enterprise.
     */
    public boolean requiresExternalEnterpriseApproval() {
        return getHoldingEnterpriseApprovalRole() != null;
    }
    
    /**
     * Gets a description of the approval workflow for display purposes.
     */
    public String getApprovalWorkflowDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Campus Coordinator (student verification)");
        
        String externalRole = getHoldingEnterpriseApprovalRole();
        if (externalRole != null) {
            sb.append("\n2. ").append(formatRoleName(externalRole));
            sb.append(" (").append(itemHoldingEnterpriseName != null ? 
                itemHoldingEnterpriseName : "holding organization").append(")");
        }
        
        if (isHighValue && !"POLICE_EVIDENCE_CUSTODIAN".equals(externalRole)) {
            int step = externalRole != null ? 3 : 2;
            sb.append("\n").append(step).append(". Police Evidence Custodian (high-value verification)");
        }
        
        return sb.toString();
    }
    
    private String formatRoleName(String role) {
        if (role == null) return "";
        return role.replace("_", " ")
                  .toLowerCase()
                  .replace("mbta", "MBTA");
    }
    
    @Override
    public String getRequestSummary() {
        String highValueTag = isHighValue ? " [HIGH VALUE]" : "";
        String externalTag = requiresExternalEnterpriseApproval() ? 
            " [" + (itemHoldingEnterpriseName != null ? itemHoldingEnterpriseName : "External") + "]" : "";
        return String.format("Item Claim: %s%s%s - Claimed by %s", 
                            itemName, highValueTag, externalTag, requesterName);
    }
    
    @Override
    public boolean isValid() {
        // Must have essential fields
        if (itemId == null || itemId.isEmpty()) return false;
        if (requesterId == null || requesterId.isEmpty()) return false;
        if (claimDetails == null || claimDetails.isEmpty()) return false;
        if (identifyingFeatures == null || identifyingFeatures.isEmpty()) return false;
        
        // High-value items: proof is recommended but not strictly required
        // (Removed strict 20-character requirement)
        
        return true;
    }
    
    /**
     * Determine if this claim request should be escalated to police
     */
    public boolean requiresPoliceVerification() {
        return isHighValue;
    }
    
    /**
     * Get a confidence score based on how detailed the claim is
     */
    public int getClaimConfidenceScore() {
        int score = 0;
        
        // More details = higher confidence
        if (claimDetails != null && claimDetails.length() > 50) score += 25;
        if (identifyingFeatures != null && identifyingFeatures.length() > 30) score += 25;
        if (proofDescription != null && !proofDescription.isEmpty()) score += 25;
        if (claimPhotoUrl != null && !claimPhotoUrl.isEmpty()) score += 25;
        
        return score;
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    // NEW getters/setters for enterprise type tracking
    
    public String getItemHoldingEnterpriseType() {
        return itemHoldingEnterpriseType;
    }
    
    public void setItemHoldingEnterpriseType(String itemHoldingEnterpriseType) {
        this.itemHoldingEnterpriseType = itemHoldingEnterpriseType;
    }
    
    public String getItemHoldingEnterpriseName() {
        return itemHoldingEnterpriseName;
    }
    
    public void setItemHoldingEnterpriseName(String itemHoldingEnterpriseName) {
        this.itemHoldingEnterpriseName = itemHoldingEnterpriseName;
    }
    
    // Existing getters/setters
    
    public String getLostItemId() {
        return lostItemId;
    }
    
    public void setLostItemId(String lostItemId) {
        this.lostItemId = lostItemId;
    }
    
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
    
    public String getItemCategory() {
        return itemCategory;
    }
    
    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }
    
    public double getItemValue() {
        return itemValue;
    }
    
    public void setItemValue(double itemValue) {
        this.itemValue = itemValue;
        this.isHighValue = itemValue > 500.0;
    }
    
    public boolean isHighValue() {
        return isHighValue;
    }
    
    public void setHighValue(boolean highValue) {
        isHighValue = highValue;
    }
    
    public String getClaimDetails() {
        return claimDetails;
    }
    
    public void setClaimDetails(String claimDetails) {
        this.claimDetails = claimDetails;
    }
    
    public String getProofDescription() {
        return proofDescription;
    }
    
    public void setProofDescription(String proofDescription) {
        this.proofDescription = proofDescription;
    }
    
    public String getIdentifyingFeatures() {
        return identifyingFeatures;
    }
    
    public void setIdentifyingFeatures(String identifyingFeatures) {
        this.identifyingFeatures = identifyingFeatures;
    }
    
    public String getClaimPhotoUrl() {
        return claimPhotoUrl;
    }
    
    public void setClaimPhotoUrl(String claimPhotoUrl) {
        this.claimPhotoUrl = claimPhotoUrl;
    }
    
    public String getFoundLocationId() {
        return foundLocationId;
    }
    
    public void setFoundLocationId(String foundLocationId) {
        this.foundLocationId = foundLocationId;
    }
    
    public String getFoundLocationName() {
        return foundLocationName;
    }
    
    public void setFoundLocationName(String foundLocationName) {
        this.foundLocationName = foundLocationName;
    }
    
    @Override
    public String toString() {
        return String.format("ItemClaimRequest{id=%s, item=%s, value=$%.2f, highValue=%s, holdingEnterprise=%s, status=%s, step=%d/%d}",
                requestId, itemName, itemValue, isHighValue, itemHoldingEnterpriseType, status, approvalStep, getApprovalChain().size());
    }
}
