package com.campus.lostfound.models.trustscore;

import java.time.LocalDateTime;

/**
 * Represents a single event that affects a user's trust score.
 * Each event records what happened, the point change, and context.
 * 
 * Events are immutable once created - they serve as an audit trail
 * for trust score changes.
 */
public class TrustScoreEvent {
    
    // ==================== ENUMS ====================
    
    /**
     * Types of events that can affect trust scores.
     * Each type has a standard point value associated with it.
     */
    public enum EventType {
        // Positive events
        REPORT_FOUND_ITEM(+2, "Reported a found item"),
        REPORT_LOST_ITEM(+1, "Reported a lost item"),
        SUCCESSFUL_CLAIM(+10, "Successfully claimed own item (verified)"),
        APPROVE_REQUEST(+5, "Approved a claim request"),
        ASSIST_RECOVERY(+3, "Assisted in item recovery"),
        GOOD_SAMARITAN(+15, "Returned high-value item without reward"),
        ACCOUNT_VERIFICATION(+5, "Completed identity verification"),
        REQUEST_COMPLETED(+3, "Work request completed successfully"),
        
        // Negative events
        FALSE_CLAIM(-25, "Submitted a false claim"),
        CLAIM_REJECTED(-5, "Claim was rejected"),
        SUSPICIOUS_ACTIVITY(-10, "Flagged for suspicious activity"),
        NO_SHOW_PICKUP(-8, "Failed to pick up claimed item"),
        FRAUDULENT_REPORT(-30, "Submitted fraudulent report"),
        
        // Administrative events
        FRAUD_FLAG(-50, "Flagged for fraud investigation"),
        FRAUD_CLEARED(+20, "Cleared from fraud investigation"),
        MANUAL_ADJUSTMENT(0, "Manual score adjustment by admin"),
        SCORE_DECAY(-1, "Periodic score decay for inactivity"),
        INITIAL_SCORE(0, "Initial trust score assignment");
        
        private final int defaultPoints;
        private final String description;
        
        EventType(int defaultPoints, String description) {
            this.defaultPoints = defaultPoints;
            this.description = description;
        }
        
        public int getDefaultPoints() {
            return defaultPoints;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Returns a display-friendly name for the event type.
         * Converts enum name like "FALSE_CLAIM" to "False Claim"
         */
        public String getDisplayName() {
            String[] words = name().toLowerCase().split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
            }
            return result.toString();
        }
        
        /**
         * Returns true if this is a positive event (increases trust)
         */
        public boolean isPositive() {
            return defaultPoints > 0;
        }
        
        /**
         * Returns true if this is a negative event (decreases trust)
         */
        public boolean isNegative() {
            return defaultPoints < 0;
        }
        
        /**
         * Returns true if this is a severe negative event
         */
        public boolean isSevere() {
            return defaultPoints <= -25;
        }
    }
    
    // ==================== FIELDS ====================
    
    // Identifiers
    private String eventId;         // MongoDB ObjectId as string (set after save)
    private String visibleId;       // Human-readable ID (e.g., "TSE-001234")
    
    // User context
    private String userId;          // The user whose score was affected
    private String userName;        // For display purposes
    
    // Event details
    private EventType eventType;
    private int pointsChange;       // Actual points (can override default)
    private double previousScore;   // Score before this event
    private double newScore;        // Score after this event
    private String description;     // Custom description or override
    
    // Related entities (optional)
    private String relatedItemId;       // If event relates to an item
    private String relatedRequestId;    // If event relates to a work request
    private String relatedClaimId;      // If event relates to a claim
    
    // Actor information
    private String recordedById;    // Who recorded this event (for admin events)
    private String recordedByName;
    
    // Timestamps
    private LocalDateTime timestamp;
    private LocalDateTime expiresAt;    // Optional: when this event stops affecting score
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Default constructor
     */
    public TrustScoreEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor with essential fields
     */
    public TrustScoreEvent(String userId, EventType eventType) {
        this();
        this.userId = userId;
        this.eventType = eventType;
        this.pointsChange = eventType.getDefaultPoints();
        this.description = eventType.getDescription();
    }
    
    /**
     * Constructor with custom points
     */
    public TrustScoreEvent(String userId, EventType eventType, int customPoints, String customDescription) {
        this(userId, eventType);
        this.pointsChange = customPoints;
        if (customDescription != null && !customDescription.isEmpty()) {
            this.description = customDescription;
        }
    }
    
    /**
     * Full constructor for loading from database
     */
    public TrustScoreEvent(String eventId, String userId, EventType eventType, 
                           int pointsChange, double previousScore, double newScore,
                           String description, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.pointsChange = pointsChange;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.description = description;
        this.timestamp = timestamp;
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Generates a human-readable visible ID
     */
    public void generateVisibleId(int sequenceNumber) {
        this.visibleId = String.format("TSE-%06d", sequenceNumber);
    }
    
    /**
     * Returns a formatted string showing the point change
     * e.g., "+10" or "-25"
     */
    public String getPointsChangeFormatted() {
        return (pointsChange >= 0 ? "+" : "") + pointsChange;
    }
    
    /**
     * Returns true if this event is still active (hasn't expired)
     */
    public boolean isActive() {
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }
    
    /**
     * Returns a summary suitable for display
     */
    public String getSummary() {
        return String.format("[%s] %s (%s points)", 
            eventType.name(), 
            description, 
            getPointsChangeFormatted());
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getVisibleId() {
        return visibleId;
    }
    
    public void setVisibleId(String visibleId) {
        this.visibleId = visibleId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
    
    public int getPointsChange() {
        return pointsChange;
    }
    
    public void setPointsChange(int pointsChange) {
        this.pointsChange = pointsChange;
    }
    
    public double getPreviousScore() {
        return previousScore;
    }
    
    public void setPreviousScore(double previousScore) {
        this.previousScore = previousScore;
    }
    
    public double getNewScore() {
        return newScore;
    }
    
    public void setNewScore(double newScore) {
        this.newScore = newScore;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRelatedItemId() {
        return relatedItemId;
    }
    
    public void setRelatedItemId(String relatedItemId) {
        this.relatedItemId = relatedItemId;
    }
    
    public String getRelatedRequestId() {
        return relatedRequestId;
    }
    
    public void setRelatedRequestId(String relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
    }
    
    public String getRelatedClaimId() {
        return relatedClaimId;
    }
    
    public void setRelatedClaimId(String relatedClaimId) {
        this.relatedClaimId = relatedClaimId;
    }
    
    public String getRecordedById() {
        return recordedById;
    }
    
    public void setRecordedById(String recordedById) {
        this.recordedById = recordedById;
    }
    
    public String getRecordedByName() {
        return recordedByName;
    }
    
    public void setRecordedByName(String recordedByName) {
        this.recordedByName = recordedByName;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    // ==================== OBJECT METHODS ====================
    
    @Override
    public String toString() {
        return "TrustScoreEvent{" +
                "eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType=" + eventType +
                ", pointsChange=" + pointsChange +
                ", previousScore=" + previousScore +
                ", newScore=" + newScore +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustScoreEvent that = (TrustScoreEvent) o;
        return eventId != null && eventId.equals(that.eventId);
    }
    
    @Override
    public int hashCode() {
        return eventId != null ? eventId.hashCode() : 0;
    }
}
