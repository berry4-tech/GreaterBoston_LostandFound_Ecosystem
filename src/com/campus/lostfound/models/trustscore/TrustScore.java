package com.campus.lostfound.models.trustscore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user's trust score in the Lost & Found ecosystem.
 * 
 * Trust scores range from 0-100 and are used to:
 * - Determine if a user can claim high-value items
 * - Flag users who may need additional verification
 * - Prioritize claims from trustworthy users
 * - Enable/disable certain features based on trust level
 * 
 * Score Levels:
 * - EXCELLENT (90-100): Premium access, fast-track claims
 * - GOOD (70-89): Standard access, can claim high-value items
 * - FAIR (50-69): Standard access, may need verification for high-value
 * - LOW (30-49): Restricted access, requires verification for most claims
 * - PROBATION (0-29): Very restricted, under review
 */
public class TrustScore {
    
    // ==================== ENUMS ====================
    
    /**
     * Trust score levels based on numeric score
     */
    public enum ScoreLevel {
        EXCELLENT(90, 100, "Excellent", "Highly trusted user with premium privileges"),
        GOOD(70, 89, "Good", "Trusted user with full access"),
        FAIR(50, 69, "Fair", "Standard user, may need occasional verification"),
        LOW(30, 49, "Low", "Restricted user, requires verification"),
        PROBATION(0, 29, "Probation", "Under review, very limited access");
        
        private final int minScore;
        private final int maxScore;
        private final String displayName;
        private final String description;
        
        ScoreLevel(int minScore, int maxScore, String displayName, String description) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.displayName = displayName;
            this.description = description;
        }
        
        public int getMinScore() {
            return minScore;
        }
        
        public int getMaxScore() {
            return maxScore;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Get the ScoreLevel for a given numeric score
         */
        public static ScoreLevel fromScore(double score) {
            if (score >= 90) return EXCELLENT;
            if (score >= 70) return GOOD;
            if (score >= 50) return FAIR;
            if (score >= 30) return LOW;
            return PROBATION;
        }
        
        /**
         * Returns a color code for UI display
         */
        public String getColorHex() {
            switch (this) {
                case EXCELLENT: return "#22C55E";  // Green
                case GOOD: return "#3B82F6";       // Blue
                case FAIR: return "#F59E0B";       // Amber/Yellow
                case LOW: return "#F97316";        // Orange
                case PROBATION: return "#EF4444";  // Red
                default: return "#6B7280";         // Gray
            }
        }
    }
    
    // ==================== CONSTANTS ====================
    
    public static final double DEFAULT_INITIAL_SCORE = 50.0;
    public static final double MIN_SCORE = 0.0;
    public static final double MAX_SCORE = 100.0;
    
    // Thresholds for business rules
    public static final double HIGH_VALUE_CLAIM_THRESHOLD = 70.0;
    public static final double SKIP_VERIFICATION_THRESHOLD = 85.0;
    public static final double REQUIRES_VERIFICATION_THRESHOLD = 50.0;
    public static final double FLAGGED_THRESHOLD = 30.0;
    
    // ==================== FIELDS ====================
    
    // Identifiers
    private String scoreId;         // MongoDB ObjectId as string
    private String visibleId;       // Human-readable ID (e.g., "TS-001234")
    
    // User reference
    private String userId;          // The user this score belongs to
    private String userName;        // For display purposes
    private String userEmail;       // For lookup
    
    // Core score data
    private double currentScore;    // Current score (0-100)
    private ScoreLevel scoreLevel;  // Derived from currentScore
    
    // Statistics
    private int totalEventsCount;       // Total number of events recorded
    private int positiveEventsCount;    // Count of positive events
    private int negativeEventsCount;    // Count of negative events
    private int totalPointsEarned;      // Sum of all positive points
    private int totalPointsLost;        // Sum of all negative points (as positive number)
    
    // Flags
    private boolean isFlagged;          // Manually flagged for review
    private boolean isUnderInvestigation;
    private String flagReason;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime lastEventAt;      // When last event was recorded
    private LocalDateTime flaggedAt;        // When user was flagged
    
    // Recent history (last N events for quick display)
    private List<TrustScoreEvent> recentEvents;
    private static final int MAX_RECENT_EVENTS = 10;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Default constructor
     */
    public TrustScore() {
        this.currentScore = DEFAULT_INITIAL_SCORE;
        this.scoreLevel = ScoreLevel.fromScore(currentScore);
        this.totalEventsCount = 0;
        this.positiveEventsCount = 0;
        this.negativeEventsCount = 0;
        this.totalPointsEarned = 0;
        this.totalPointsLost = 0;
        this.isFlagged = false;
        this.isUnderInvestigation = false;
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        this.recentEvents = new ArrayList<>();
    }
    
    /**
     * Constructor with user ID
     */
    public TrustScore(String userId) {
        this();
        this.userId = userId;
    }
    
    /**
     * Constructor with user ID and initial score
     */
    public TrustScore(String userId, double initialScore) {
        this(userId);
        setCurrentScore(initialScore);
    }
    
    /**
     * Full constructor for loading from database
     */
    public TrustScore(String scoreId, String userId, double currentScore,
                      int totalEventsCount, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.scoreId = scoreId;
        this.userId = userId;
        this.currentScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore));
        this.scoreLevel = ScoreLevel.fromScore(this.currentScore);
        this.totalEventsCount = totalEventsCount;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.recentEvents = new ArrayList<>();
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Generates a human-readable visible ID
     */
    public void generateVisibleId(int sequenceNumber) {
        this.visibleId = String.format("TS-%06d", sequenceNumber);
    }
    
    /**
     * Apply a score change and update statistics
     * @param pointsChange The points to add (can be negative)
     * @return The new score after applying the change
     */
    public double applyScoreChange(int pointsChange) {
        double oldScore = this.currentScore;
        this.currentScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, this.currentScore + pointsChange));
        this.scoreLevel = ScoreLevel.fromScore(this.currentScore);
        this.lastUpdatedAt = LocalDateTime.now();
        this.lastEventAt = LocalDateTime.now();
        this.totalEventsCount++;
        
        if (pointsChange > 0) {
            this.positiveEventsCount++;
            this.totalPointsEarned += pointsChange;
        } else if (pointsChange < 0) {
            this.negativeEventsCount++;
            this.totalPointsLost += Math.abs(pointsChange);
        }
        
        // Auto-flag if score drops below threshold
        if (this.currentScore < FLAGGED_THRESHOLD && oldScore >= FLAGGED_THRESHOLD) {
            this.isFlagged = true;
            this.flaggedAt = LocalDateTime.now();
            this.flagReason = "Score dropped below threshold";
        }
        
        return this.currentScore;
    }
    
    /**
     * Add an event to recent history
     */
    public void addRecentEvent(TrustScoreEvent event) {
        if (recentEvents == null) {
            recentEvents = new ArrayList<>();
        }
        recentEvents.add(0, event);  // Add to front
        
        // Keep only the most recent events
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(recentEvents.size() - 1);
        }
    }
    
    /**
     * Check if user can claim high-value items without additional verification
     */
    public boolean canClaimHighValueItem() {
        return !isFlagged && !isUnderInvestigation && currentScore >= HIGH_VALUE_CLAIM_THRESHOLD;
    }
    
    /**
     * Check if user can skip verification for claims
     */
    public boolean canSkipVerification() {
        return !isFlagged && !isUnderInvestigation && currentScore >= SKIP_VERIFICATION_THRESHOLD;
    }
    
    /**
     * Check if user requires additional verification for all claims
     */
    public boolean requiresVerification() {
        return isFlagged || isUnderInvestigation || currentScore < REQUIRES_VERIFICATION_THRESHOLD;
    }
    
    /**
     * Check if user is in probation/low trust state
     */
    public boolean isLowTrust() {
        return currentScore < FLAGGED_THRESHOLD;
    }
    
    /**
     * Get a formatted score string for display
     */
    public String getScoreDisplay() {
        return String.format("%.0f/100", currentScore);
    }
    
    /**
     * Get a formatted level string for display
     */
    public String getLevelDisplay() {
        return scoreLevel.getDisplayName() + " (" + getScoreDisplay() + ")";
    }
    
    /**
     * Manually flag this user for review
     */
    public void flag(String reason) {
        this.isFlagged = true;
        this.flaggedAt = LocalDateTime.now();
        this.flagReason = reason;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Clear the flag
     */
    public void clearFlag() {
        this.isFlagged = false;
        this.flagReason = null;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Start investigation
     */
    public void startInvestigation() {
        this.isUnderInvestigation = true;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * End investigation
     */
    public void endInvestigation() {
        this.isUnderInvestigation = false;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Reset score to default (for admin use)
     */
    public void resetToDefault() {
        this.currentScore = DEFAULT_INITIAL_SCORE;
        this.scoreLevel = ScoreLevel.fromScore(currentScore);
        this.isFlagged = false;
        this.isUnderInvestigation = false;
        this.flagReason = null;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Get the trust trend (positive, negative, or neutral)
     */
    public String getTrend() {
        if (positiveEventsCount == 0 && negativeEventsCount == 0) {
            return "NEUTRAL";
        }
        double ratio = (double) positiveEventsCount / (positiveEventsCount + negativeEventsCount);
        if (ratio > 0.6) return "POSITIVE";
        if (ratio < 0.4) return "NEGATIVE";
        return "NEUTRAL";
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public String getScoreId() {
        return scoreId;
    }
    
    public void setScoreId(String scoreId) {
        this.scoreId = scoreId;
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
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public double getCurrentScore() {
        return currentScore;
    }
    
    public void setCurrentScore(double currentScore) {
        this.currentScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore));
        this.scoreLevel = ScoreLevel.fromScore(this.currentScore);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public ScoreLevel getScoreLevel() {
        return scoreLevel;
    }
    
    public void setScoreLevel(ScoreLevel scoreLevel) {
        this.scoreLevel = scoreLevel;
    }
    
    public int getTotalEventsCount() {
        return totalEventsCount;
    }
    
    public void setTotalEventsCount(int totalEventsCount) {
        this.totalEventsCount = totalEventsCount;
    }
    
    public int getPositiveEventsCount() {
        return positiveEventsCount;
    }
    
    public void setPositiveEventsCount(int positiveEventsCount) {
        this.positiveEventsCount = positiveEventsCount;
    }
    
    public int getNegativeEventsCount() {
        return negativeEventsCount;
    }
    
    public void setNegativeEventsCount(int negativeEventsCount) {
        this.negativeEventsCount = negativeEventsCount;
    }
    
    public int getTotalPointsEarned() {
        return totalPointsEarned;
    }
    
    public void setTotalPointsEarned(int totalPointsEarned) {
        this.totalPointsEarned = totalPointsEarned;
    }
    
    public int getTotalPointsLost() {
        return totalPointsLost;
    }
    
    public void setTotalPointsLost(int totalPointsLost) {
        this.totalPointsLost = totalPointsLost;
    }
    
    public boolean isFlagged() {
        return isFlagged;
    }
    
    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }
    
    public boolean isUnderInvestigation() {
        return isUnderInvestigation;
    }
    
    public void setUnderInvestigation(boolean underInvestigation) {
        isUnderInvestigation = underInvestigation;
    }
    
    public String getFlagReason() {
        return flagReason;
    }
    
    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public LocalDateTime getLastEventAt() {
        return lastEventAt;
    }
    
    public void setLastEventAt(LocalDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
    }
    
    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }
    
    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }
    
    public List<TrustScoreEvent> getRecentEvents() {
        return recentEvents;
    }
    
    public void setRecentEvents(List<TrustScoreEvent> recentEvents) {
        this.recentEvents = recentEvents;
    }
    
    // ==================== OBJECT METHODS ====================
    
    @Override
    public String toString() {
        return "TrustScore{" +
                "scoreId='" + scoreId + '\'' +
                ", userId='" + userId + '\'' +
                ", currentScore=" + currentScore +
                ", scoreLevel=" + scoreLevel +
                ", isFlagged=" + isFlagged +
                ", totalEventsCount=" + totalEventsCount +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustScore that = (TrustScore) o;
        if (scoreId != null) {
            return scoreId.equals(that.scoreId);
        }
        return userId != null && userId.equals(that.userId);
    }
    
    @Override
    public int hashCode() {
        return scoreId != null ? scoreId.hashCode() : (userId != null ? userId.hashCode() : 0);
    }
}
