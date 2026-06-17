package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoTrustScoreDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.trustscore.TrustScoreEvent;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for Trust Score business logic.
 * 
 * Provides a clean API for:
 * - Managing user trust scores
 * - Recording trust events with automatic score updates
 * - Business rule checks (can claim high-value, needs verification, etc.)
 * - Statistics and reporting
 * 
 * This service coordinates between the TrustScore models and the DAO layer,
 * ensuring all business rules are applied consistently.
 */
public class TrustScoreService {
    
    private static final Logger LOGGER = Logger.getLogger(TrustScoreService.class.getName());
    
    private final MongoTrustScoreDAO trustScoreDAO;
    private final MongoUserDAO userDAO;
    
    // ==================== CONSTRUCTORS ====================
    
    public TrustScoreService() {
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.userDAO = new MongoUserDAO();
    }
    
    /**
     * Constructor for testing with mock DAOs
     */
    public TrustScoreService(MongoTrustScoreDAO trustScoreDAO, MongoUserDAO userDAO) {
        this.trustScoreDAO = trustScoreDAO;
        this.userDAO = userDAO;
    }
    
    // ==================== SCORE MANAGEMENT ====================
    
    /**
     * Get or create a TrustScore for a user.
     * If the user doesn't have a trust score record, creates one with default values.
     * 
     * @param userId The user's ID (can be email or ObjectId string)
     * @return The user's TrustScore (never null)
     */
    public TrustScore getOrCreateTrustScore(String userId) {
        try {
            // Try to find existing score
            TrustScore score = trustScoreDAO.findScoreByUserId(userId);
            
            if (score != null) {
                return score;
            }
            
            // Try by email if userId looks like an email
            if (userId != null && userId.contains("@")) {
                score = trustScoreDAO.findScoreByEmail(userId);
                if (score != null) {
                    return score;
                }
            }
            
            // Create new score for user
            score = new TrustScore(userId);
            
            // Try to get user details for context
            Optional<User> userOpt = userDAO.findByEmail(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                score.setUserName(user.getFullName());
                score.setUserEmail(user.getEmail());
                // Initialize from user's existing trust score if available
                if (user.getTrustScore() > 0) {
                    score.setCurrentScore(user.getTrustScore());
                }
            } else {
                // Try finding by ID
                try {
                    userOpt = userDAO.findById(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        score.setUserName(user.getFullName());
                        score.setUserEmail(user.getEmail());
                        if (user.getTrustScore() > 0) {
                            score.setCurrentScore(user.getTrustScore());
                        }
                    }
                } catch (Exception e) {
                    // userId might not be a valid ObjectId, that's okay
                }
            }
            
            // Save the new score
            String scoreId = trustScoreDAO.saveTrustScore(score);
            score.setScoreId(scoreId);
            
            // Record initial score event
            recordEventInternal(score, EventType.INITIAL_SCORE, 0, 
                "Initial trust score created", null, null, null);
            
            LOGGER.info("Created new TrustScore for user: " + userId + 
                       " with initial score: " + score.getCurrentScore());
            
            return score;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting/creating trust score for user: " + userId, e);
            // Return a default score object (not persisted) rather than null
            return new TrustScore(userId);
        }
    }
    
    /**
     * Get trust score value for a user (quick lookup)
     * 
     * @param userId The user's ID
     * @return The numeric score (0-100), or default 50 if not found
     */
    public double getTrustScore(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.getCurrentScore();
    }
    
    /**
     * Get display-friendly trust score label
     * 
     * @param userId The user's ID
     * @return String like "Good (78/100)"
     */
    public String getTrustScoreDisplay(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.getLevelDisplay();
    }
    
    /**
     * Get the score level for a user
     * 
     * @param userId The user's ID
     * @return The ScoreLevel enum value
     */
    public ScoreLevel getScoreLevel(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.getScoreLevel();
    }
    
    /**
     * Get the score level for a numeric score
     * 
     * @param score The numeric score
     * @return The ScoreLevel enum value
     */
    public ScoreLevel getScoreLevel(double score) {
        return ScoreLevel.fromScore(score);
    }
    
    /**
     * Manually update a user's score (admin function)
     * Records a MANUAL_ADJUSTMENT event
     * 
     * @param userId The user's ID
     * @param newScore The new score value
     * @param reason The reason for adjustment
     * @param adminId The admin making the change
     * @return true if successful
     */
    public boolean manuallyUpdateScore(String userId, double newScore, String reason, String adminId) {
        try {
            TrustScore score = getOrCreateTrustScore(userId);
            double oldScore = score.getCurrentScore();
            int pointsChange = (int) (newScore - oldScore);
            
            // Apply the change
            score.setCurrentScore(newScore);
            trustScoreDAO.saveTrustScore(score);
            
            // Record the event
            TrustScoreEvent event = new TrustScoreEvent(userId, EventType.MANUAL_ADJUSTMENT, 
                pointsChange, "Manual adjustment: " + reason);
            event.setPreviousScore(oldScore);
            event.setNewScore(newScore);
            event.setRecordedById(adminId);
            trustScoreDAO.saveEvent(event);
            
            // Sync to User model
            syncScoreToUser(userId, newScore);
            
            LOGGER.info("Manual score update for " + userId + ": " + oldScore + " -> " + newScore + 
                       " by admin " + adminId + " reason: " + reason);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error manually updating score for user: " + userId, e);
            return false;
        }
    }
    
    // ==================== EVENT RECORDING ====================
    
    /**
     * Record a trust score event and update the user's score.
     * This is the main method for recording events - it handles:
     * - Creating the event record
     * - Calculating and applying score change
     * - Updating statistics
     * - Syncing to User model
     * 
     * @param userId The user's ID
     * @param eventType The type of event
     * @param description Custom description (or null for default)
     * @return The recorded event, or null if failed
     */
    public TrustScoreEvent recordEvent(String userId, EventType eventType, String description) {
        return recordEvent(userId, eventType, description, null, null, null);
    }
    
    /**
     * Record a trust score event with related entity IDs
     * 
     * @param userId The user's ID
     * @param eventType The type of event
     * @param description Custom description
     * @param relatedItemId Related item ID (optional)
     * @param relatedRequestId Related work request ID (optional)
     * @param relatedClaimId Related claim ID (optional)
     * @return The recorded event, or null if failed
     */
    public TrustScoreEvent recordEvent(String userId, EventType eventType, String description,
                                       String relatedItemId, String relatedRequestId, String relatedClaimId) {
        try {
            TrustScore score = getOrCreateTrustScore(userId);
            int points = calculatePointsForEvent(eventType);
            
            return recordEventInternal(score, eventType, points, description, 
                relatedItemId, relatedRequestId, relatedClaimId);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording event for user: " + userId, e);
            return null;
        }
    }
    
    /**
     * Record a trust score event with custom point value
     * 
     * @param userId The user's ID
     * @param eventType The type of event
     * @param customPoints Custom point value (overrides default)
     * @param description Custom description
     * @return The recorded event, or null if failed
     */
    public TrustScoreEvent recordEventWithCustomPoints(String userId, EventType eventType, 
                                                        int customPoints, String description) {
        try {
            TrustScore score = getOrCreateTrustScore(userId);
            return recordEventInternal(score, eventType, customPoints, description, null, null, null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording event with custom points for user: " + userId, e);
            return null;
        }
    }
    
    /**
     * Internal method to record an event and update score
     */
    private TrustScoreEvent recordEventInternal(TrustScore score, EventType eventType, int points,
                                                 String description, String relatedItemId, 
                                                 String relatedRequestId, String relatedClaimId) {
        try {
            double previousScore = score.getCurrentScore();
            
            // Apply score change
            double newScore = score.applyScoreChange(points);
            
            // Create the event
            TrustScoreEvent event = new TrustScoreEvent(score.getUserId(), eventType, points,
                description != null ? description : eventType.getDescription());
            event.setUserName(score.getUserName());
            event.setPreviousScore(previousScore);
            event.setNewScore(newScore);
            event.setRelatedItemId(relatedItemId);
            event.setRelatedRequestId(relatedRequestId);
            event.setRelatedClaimId(relatedClaimId);
            
            // Save the event
            String eventId = trustScoreDAO.saveEvent(event);
            event.setEventId(eventId);
            
            // Add to recent events cache
            score.addRecentEvent(event);
            
            // Save updated score
            trustScoreDAO.saveTrustScore(score);
            
            // Sync to User model
            syncScoreToUser(score.getUserId(), newScore);
            
            LOGGER.info("Recorded event: " + eventType + " for user: " + score.getUserId() + 
                       " points: " + (points >= 0 ? "+" : "") + points +
                       " score: " + previousScore + " -> " + newScore);
            
            return event;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording event internally", e);
            return null;
        }
    }
    
    /**
     * Calculate points for an event type
     * Can be overridden for special cases
     */
    public int calculatePointsForEvent(EventType eventType) {
        return eventType.getDefaultPoints();
    }
    
    /**
     * Sync trust score to the User model
     */
    private void syncScoreToUser(String userId, double newScore) {
        try {
            // Try by email first
            if (userId != null && userId.contains("@")) {
                userDAO.updateTrustScoreByEmail(userId, "SYNC");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not sync score to user model: " + userId, e);
        }
    }
    
    // ==================== BUSINESS RULE CHECKS ====================
    
    /**
     * Check if user can claim high-value items (> $500)
     * Requires score >= 70 and no flags
     * 
     * @param userId The user's ID
     * @return true if user can claim high-value items
     */
    public boolean canClaimHighValueItem(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.canClaimHighValueItem();
    }
    
    /**
     * Check if user can skip verification for claims
     * Requires score >= 85 and no flags
     * 
     * @param userId The user's ID
     * @return true if user can skip verification
     */
    public boolean canClaimWithoutVerification(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.canSkipVerification();
    }
    
    /**
     * Check if user requires additional verification for all claims
     * Required if score < 50 or user is flagged
     * 
     * @param userId The user's ID
     * @return true if user requires verification
     */
    public boolean requiresAdditionalVerification(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.requiresVerification();
    }
    
    /**
     * Check if user is flagged for review
     * 
     * @param userId The user's ID
     * @return true if user is flagged
     */
    public boolean isFlagged(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.isFlagged();
    }
    
    /**
     * Check if user is under investigation
     * 
     * @param userId The user's ID
     * @return true if user is under investigation
     */
    public boolean isUnderInvestigation(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.isUnderInvestigation();
    }
    
    /**
     * Check if user is in low trust / probation state
     * 
     * @param userId The user's ID
     * @return true if user has low trust score (< 30)
     */
    public boolean isLowTrust(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        return score.isLowTrust();
    }
    
    /**
     * Get the maximum item value a user can claim without extra verification
     * Based on trust score tier
     * 
     * @param userId The user's ID
     * @return Maximum claimable value in dollars
     */
    public double getMaxClaimValueWithoutVerification(String userId) {
        TrustScore score = getOrCreateTrustScore(userId);
        ScoreLevel level = score.getScoreLevel();
        
        switch (level) {
            case EXCELLENT:
                return 5000.0;  // Can claim items up to $5000 without extra verification
            case GOOD:
                return 1000.0;  // Can claim items up to $1000
            case FAIR:
                return 250.0;   // Can claim items up to $250
            case LOW:
                return 50.0;    // Can claim items up to $50
            case PROBATION:
            default:
                return 0.0;     // All claims need verification
        }
    }
    
    /**
     * Check if a specific claim needs verification based on user trust and item value
     * 
     * @param userId The user's ID
     * @param itemValue The value of the item being claimed
     * @return true if this claim needs additional verification
     */
    public boolean claimNeedsVerification(String userId, double itemValue) {
        if (requiresAdditionalVerification(userId)) {
            return true;  // User always needs verification
        }
        
        double maxWithoutVerification = getMaxClaimValueWithoutVerification(userId);
        return itemValue > maxWithoutVerification;
    }
    
    // ==================== FLAG & INVESTIGATION MANAGEMENT ====================
    
    /**
     * Flag a user for review
     * 
     * @param userId The user to flag
     * @param reason The reason for flagging
     * @param flaggedBy Who is flagging (admin ID)
     * @return true if successful
     */
    public boolean flagUser(String userId, String reason, String flaggedBy) {
        try {
            boolean success = trustScoreDAO.flagUser(userId, reason);
            
            if (success) {
                // Record event
                TrustScoreEvent event = new TrustScoreEvent(userId, EventType.FRAUD_FLAG);
                event.setDescription("User flagged: " + reason);
                event.setRecordedById(flaggedBy);
                TrustScore score = getOrCreateTrustScore(userId);
                event.setPreviousScore(score.getCurrentScore());
                event.setNewScore(score.applyScoreChange(EventType.FRAUD_FLAG.getDefaultPoints()));
                trustScoreDAO.saveEvent(event);
                trustScoreDAO.saveTrustScore(score);
                
                LOGGER.info("User flagged: " + userId + " reason: " + reason + " by: " + flaggedBy);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error flagging user: " + userId, e);
            return false;
        }
    }
    
    /**
     * Clear a user's flag
     * 
     * @param userId The user to clear
     * @param clearedBy Who is clearing (admin ID)
     * @return true if successful
     */
    public boolean clearUserFlag(String userId, String clearedBy) {
        try {
            boolean success = trustScoreDAO.clearUserFlag(userId);
            
            if (success) {
                // Record event
                TrustScoreEvent event = new TrustScoreEvent(userId, EventType.FRAUD_CLEARED);
                event.setDescription("User flag cleared");
                event.setRecordedById(clearedBy);
                TrustScore score = getOrCreateTrustScore(userId);
                event.setPreviousScore(score.getCurrentScore());
                event.setNewScore(score.applyScoreChange(EventType.FRAUD_CLEARED.getDefaultPoints()));
                trustScoreDAO.saveEvent(event);
                trustScoreDAO.saveTrustScore(score);
                
                LOGGER.info("User flag cleared: " + userId + " by: " + clearedBy);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error clearing user flag: " + userId, e);
            return false;
        }
    }
    
    /**
     * Start an investigation on a user
     * 
     * @param userId The user to investigate
     * @param startedBy Who is starting (admin ID)
     * @return true if successful
     */
    public boolean startInvestigation(String userId, String startedBy) {
        try {
            boolean success = trustScoreDAO.startInvestigation(userId);
            
            if (success) {
                LOGGER.info("Investigation started for user: " + userId + " by: " + startedBy);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting investigation for user: " + userId, e);
            return false;
        }
    }
    
    /**
     * End an investigation on a user
     * 
     * @param userId The user
     * @param endedBy Who is ending (admin ID)
     * @param wasCleared Whether user was cleared (true) or found guilty (false)
     * @return true if successful
     */
    public boolean endInvestigation(String userId, String endedBy, boolean wasCleared) {
        try {
            boolean success = trustScoreDAO.endInvestigation(userId);
            
            if (success) {
                if (wasCleared) {
                    // Clear flag and give bonus points
                    clearUserFlag(userId, endedBy);
                }
                
                LOGGER.info("Investigation ended for user: " + userId + " by: " + endedBy + 
                           " cleared: " + wasCleared);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error ending investigation for user: " + userId, e);
            return false;
        }
    }
    
    // ==================== QUERY METHODS ====================
    
    /**
     * Get all flagged users
     */
    public List<TrustScore> getFlaggedUsers() {
        return trustScoreDAO.getFlaggedUsers();
    }
    
    /**
     * Get all users under investigation
     */
    public List<TrustScore> getUsersUnderInvestigation() {
        return trustScoreDAO.getUsersUnderInvestigation();
    }
    
    /**
     * Get low trust users (score < 50)
     */
    public List<TrustScore> getLowTrustUsers() {
        return trustScoreDAO.getLowTrustUsers();
    }
    
    /**
     * Get excellent users (score >= 90)
     */
    public List<TrustScore> getExcellentUsers() {
        return trustScoreDAO.getExcellentUsers();
    }
    
    /**
     * Get users by score level
     */
    public List<TrustScore> getUsersByScoreLevel(ScoreLevel level) {
        return trustScoreDAO.getScoresByLevel(level);
    }
    
    /**
     * Get users in a score range
     */
    public List<TrustScore> getUsersByScoreRange(double minScore, double maxScore) {
        return trustScoreDAO.getScoresByRange(minScore, maxScore);
    }
    
    /**
     * Get all trust scores
     */
    public List<TrustScore> getAllTrustScores() {
        return trustScoreDAO.findAllScores();
    }
    
    /**
     * Get event history for a user
     */
    public List<TrustScoreEvent> getUserEventHistory(String userId) {
        return trustScoreDAO.getEventsForUser(userId);
    }
    
    /**
     * Get recent events for a user
     */
    public List<TrustScoreEvent> getUserRecentEvents(String userId, int limit) {
        return trustScoreDAO.getRecentEventsForUser(userId, limit);
    }
    
    /**
     * Get negative events for a user (for investigation)
     */
    public List<TrustScoreEvent> getUserNegativeEvents(String userId) {
        return trustScoreDAO.getNegativeEventsForUser(userId);
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Get statistics summary for dashboard
     */
    public TrustScoreStats getStatistics() {
        TrustScoreStats stats = new TrustScoreStats();
        
        stats.totalUsers = trustScoreDAO.getTotalScoresCount();
        stats.totalEvents = trustScoreDAO.getTotalEventsCount();
        stats.averageScore = trustScoreDAO.getAverageScore();
        stats.flaggedUsers = trustScoreDAO.getFlaggedUsersCount();
        stats.scoreDistribution = trustScoreDAO.getScoreDistribution();
        stats.eventTypeDistribution = trustScoreDAO.getEventTypeDistribution();
        
        return stats;
    }
    
    /**
     * Statistics container class
     */
    public static class TrustScoreStats {
        public long totalUsers;
        public long totalEvents;
        public double averageScore;
        public long flaggedUsers;
        public Map<ScoreLevel, Long> scoreDistribution;
        public Map<EventType, Long> eventTypeDistribution;
        
        @Override
        public String toString() {
            return "TrustScoreStats{" +
                    "totalUsers=" + totalUsers +
                    ", totalEvents=" + totalEvents +
                    ", averageScore=" + String.format("%.1f", averageScore) +
                    ", flaggedUsers=" + flaggedUsers +
                    '}';
        }
    }
    
    // ==================== CONVENIENCE METHODS FOR COMMON EVENTS ====================
    
    /**
     * Record that a user reported a found item
     */
    public TrustScoreEvent recordFoundItemReport(String userId, String itemId) {
        return recordEvent(userId, EventType.REPORT_FOUND_ITEM, 
            "Reported a found item", itemId, null, null);
    }
    
    /**
     * Record that a user reported a lost item
     */
    public TrustScoreEvent recordLostItemReport(String userId, String itemId) {
        return recordEvent(userId, EventType.REPORT_LOST_ITEM, 
            "Reported a lost item", itemId, null, null);
    }
    
    /**
     * Record a successful claim (item returned to owner)
     */
    public TrustScoreEvent recordSuccessfulClaim(String userId, String itemId, String claimId) {
        return recordEvent(userId, EventType.SUCCESSFUL_CLAIM, 
            "Successfully claimed and verified item", itemId, null, claimId);
    }
    
    /**
     * Record a rejected/false claim
     */
    public TrustScoreEvent recordFalseClaim(String userId, String itemId, String claimId) {
        return recordEvent(userId, EventType.FALSE_CLAIM, 
            "Claim was rejected as false", itemId, null, claimId);
    }
    
    /**
     * Record a claim rejection (less severe than false claim)
     */
    public TrustScoreEvent recordClaimRejected(String userId, String itemId, String claimId) {
        return recordEvent(userId, EventType.CLAIM_REJECTED, 
            "Claim was rejected", itemId, null, claimId);
    }
    
    /**
     * Record that a user approved a request
     */
    public TrustScoreEvent recordRequestApproval(String userId, String requestId) {
        return recordEvent(userId, EventType.APPROVE_REQUEST, 
            "Approved a claim request", null, requestId, null);
    }
    
    /**
     * Record that a user returned a high-value item (good samaritan)
     */
    public TrustScoreEvent recordGoodSamaritan(String userId, String itemId) {
        return recordEvent(userId, EventType.GOOD_SAMARITAN, 
            "Returned high-value item without claiming reward", itemId, null, null);
    }
    
    /**
     * Record a no-show for pickup
     */
    public TrustScoreEvent recordNoShowPickup(String userId, String itemId, String claimId) {
        return recordEvent(userId, EventType.NO_SHOW_PICKUP, 
            "Failed to pick up claimed item", itemId, null, claimId);
    }
    
    /**
     * Record suspicious activity
     */
    public TrustScoreEvent recordSuspiciousActivity(String userId, String description) {
        return recordEvent(userId, EventType.SUSPICIOUS_ACTIVITY, description, null, null, null);
    }
}
