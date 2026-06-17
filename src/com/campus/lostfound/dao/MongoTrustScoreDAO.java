package com.campus.lostfound.dao;

import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.trustscore.TrustScoreEvent;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for TrustScore and TrustScoreEvent entities.
 * Handles all MongoDB operations for the trust score system.
 * 
 * Collections managed:
 * - trust_scores: User trust score records
 * - trust_score_events: Individual score-affecting events
 */
public class MongoTrustScoreDAO {
    
    private static final Logger LOGGER = Logger.getLogger(MongoTrustScoreDAO.class.getName());
    
    private final MongoCollection<Document> scoresCollection;
    private final MongoCollection<Document> eventsCollection;
    
    // Sequence counters for visible IDs
    private int scoreSequence = 1000;
    private int eventSequence = 1000;
    
    // ==================== CONSTRUCTOR ====================
    
    public MongoTrustScoreDAO() {
        MongoDatabase database = MongoDBConnection.getInstance().getDatabase();
        this.scoresCollection = database.getCollection("trust_scores");
        this.eventsCollection = database.getCollection("trust_score_events");
        
        initializeIndexes();
        initializeSequences();
        
        LOGGER.info("MongoTrustScoreDAO initialized");
    }
    
    /**
     * Create indexes for efficient queries
     */
    private void initializeIndexes() {
        try {
            // Trust Scores indexes
            scoresCollection.createIndex(new Document("userId", 1), 
                new IndexOptions().unique(true));
            scoresCollection.createIndex(new Document("userEmail", 1));
            scoresCollection.createIndex(new Document("currentScore", 1));
            scoresCollection.createIndex(new Document("scoreLevel", 1));
            scoresCollection.createIndex(new Document("isFlagged", 1));
            scoresCollection.createIndex(new Document("isUnderInvestigation", 1));
            
            // Trust Score Events indexes
            eventsCollection.createIndex(new Document("userId", 1));
            eventsCollection.createIndex(new Document("eventType", 1));
            eventsCollection.createIndex(new Document("timestamp", -1));
            eventsCollection.createIndex(new Document("relatedItemId", 1));
            eventsCollection.createIndex(new Document("relatedRequestId", 1));
            
            // Compound index for user event history
            eventsCollection.createIndex(new Document("userId", 1).append("timestamp", -1));
            
            LOGGER.info("Trust score indexes created successfully");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating indexes (may already exist)", e);
        }
    }
    
    /**
     * Initialize sequence counters from existing data
     */
    private void initializeSequences() {
        try {
            // Find max score sequence
            Document maxScore = scoresCollection.find()
                .sort(Sorts.descending("_id"))
                .first();
            if (maxScore != null) {
                scoreSequence = (int) scoresCollection.countDocuments() + 1000;
            }
            
            // Find max event sequence
            Document maxEvent = eventsCollection.find()
                .sort(Sorts.descending("_id"))
                .first();
            if (maxEvent != null) {
                eventSequence = (int) eventsCollection.countDocuments() + 1000;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing sequences", e);
        }
    }
    
    // ==================== TRUST SCORE CRUD ====================
    
    /**
     * Save a new TrustScore or update existing one
     * @return The scoreId (ObjectId as string)
     */
    public String saveTrustScore(TrustScore score) {
        try {
            Document doc = trustScoreToDocument(score);
            
            if (score.getScoreId() == null) {
                // Generate visible ID
                score.generateVisibleId(scoreSequence++);
                doc.put("visibleId", score.getVisibleId());
                
                // Insert new
                scoresCollection.insertOne(doc);
                String id = doc.getObjectId("_id").toString();
                score.setScoreId(id);
                LOGGER.info("Created new TrustScore: " + id + " for user: " + score.getUserId());
                return id;
            } else {
                // Update existing
                ObjectId objectId = new ObjectId(score.getScoreId());
                doc.put("_id", objectId);
                scoresCollection.replaceOne(Filters.eq("_id", objectId), doc);
                LOGGER.info("Updated TrustScore: " + score.getScoreId());
                return score.getScoreId();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving TrustScore", e);
            return null;
        }
    }
    
    /**
     * Find TrustScore by its MongoDB ObjectId
     */
    public TrustScore findScoreById(String scoreId) {
        try {
            ObjectId objectId = new ObjectId(scoreId);
            Document doc = scoresCollection.find(Filters.eq("_id", objectId)).first();
            return doc != null ? documentToTrustScore(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding TrustScore by ID: " + scoreId, e);
            return null;
        }
    }
    
    /**
     * Find TrustScore by user ID (most common lookup)
     */
    public TrustScore findScoreByUserId(String userId) {
        try {
            Document doc = scoresCollection.find(Filters.eq("userId", userId)).first();
            return doc != null ? documentToTrustScore(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding TrustScore by userId: " + userId, e);
            return null;
        }
    }
    
    /**
     * Find TrustScore by user email
     */
    public TrustScore findScoreByEmail(String email) {
        try {
            Document doc = scoresCollection.find(Filters.eq("userEmail", email)).first();
            return doc != null ? documentToTrustScore(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding TrustScore by email: " + email, e);
            return null;
        }
    }
    
    /**
     * Update just the score value (quick update)
     */
    public boolean updateScore(String userId, double newScore) {
        try {
            ScoreLevel newLevel = ScoreLevel.fromScore(newScore);
            scoresCollection.updateOne(
                Filters.eq("userId", userId),
                Updates.combine(
                    Updates.set("currentScore", newScore),
                    Updates.set("scoreLevel", newLevel.name()),
                    Updates.set("lastUpdatedAt", toDate(LocalDateTime.now()))
                )
            );
            LOGGER.info("Updated score for user " + userId + " to " + newScore);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating score for user: " + userId, e);
            return false;
        }
    }
    
    /**
     * Delete a TrustScore (soft delete by flagging, or hard delete)
     */
    public boolean deleteTrustScore(String scoreId) {
        try {
            ObjectId objectId = new ObjectId(scoreId);
            scoresCollection.deleteOne(Filters.eq("_id", objectId));
            LOGGER.info("Deleted TrustScore: " + scoreId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting TrustScore: " + scoreId, e);
            return false;
        }
    }
    
    // ==================== TRUST SCORE QUERIES ====================
    
    /**
     * Get all TrustScores
     */
    public List<TrustScore> findAllScores() {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find().sort(Sorts.descending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all TrustScores", e);
        }
        return scores;
    }
    
    /**
     * Get users within a score range
     */
    public List<TrustScore> getScoresByRange(double minScore, double maxScore) {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(
                    Filters.and(
                        Filters.gte("currentScore", minScore),
                        Filters.lte("currentScore", maxScore)
                    )
                ).sort(Sorts.descending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding scores by range", e);
        }
        return scores;
    }
    
    /**
     * Get users below a threshold (for flagging/review)
     */
    public List<TrustScore> getScoresBelowThreshold(double threshold) {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(Filters.lt("currentScore", threshold))
                    .sort(Sorts.ascending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding scores below threshold", e);
        }
        return scores;
    }
    
    /**
     * Get users above a threshold
     */
    public List<TrustScore> getScoresAboveThreshold(double threshold) {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(Filters.gte("currentScore", threshold))
                    .sort(Sorts.descending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding scores above threshold", e);
        }
        return scores;
    }
    
    /**
     * Get low trust users (below 50)
     */
    public List<TrustScore> getLowTrustUsers() {
        return getScoresBelowThreshold(TrustScore.REQUIRES_VERIFICATION_THRESHOLD);
    }
    
    /**
     * Get excellent users (90+)
     */
    public List<TrustScore> getExcellentUsers() {
        return getScoresAboveThreshold(90.0);
    }
    
    /**
     * Get flagged users
     */
    public List<TrustScore> getFlaggedUsers() {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(Filters.eq("isFlagged", true))
                    .sort(Sorts.ascending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding flagged users", e);
        }
        return scores;
    }
    
    /**
     * Get users under investigation
     */
    public List<TrustScore> getUsersUnderInvestigation() {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(Filters.eq("isUnderInvestigation", true))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding users under investigation", e);
        }
        return scores;
    }
    
    /**
     * Get users by score level
     */
    public List<TrustScore> getScoresByLevel(ScoreLevel level) {
        List<TrustScore> scores = new ArrayList<>();
        try {
            for (Document doc : scoresCollection.find(Filters.eq("scoreLevel", level.name()))
                    .sort(Sorts.descending("currentScore"))) {
                scores.add(documentToTrustScore(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding scores by level", e);
        }
        return scores;
    }
    
    // ==================== TRUST SCORE EVENT CRUD ====================
    
    /**
     * Save a TrustScoreEvent
     * @return The eventId (ObjectId as string)
     */
    public String saveEvent(TrustScoreEvent event) {
        try {
            Document doc = eventToDocument(event);
            
            // Generate visible ID if new
            if (event.getVisibleId() == null) {
                event.generateVisibleId(eventSequence++);
                doc.put("visibleId", event.getVisibleId());
            }
            
            eventsCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            event.setEventId(id);
            
            LOGGER.info("Saved TrustScoreEvent: " + id + " type: " + event.getEventType() + 
                       " for user: " + event.getUserId());
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving TrustScoreEvent", e);
            return null;
        }
    }
    
    /**
     * Find event by ID
     */
    public TrustScoreEvent findEventById(String eventId) {
        try {
            ObjectId objectId = new ObjectId(eventId);
            Document doc = eventsCollection.find(Filters.eq("_id", objectId)).first();
            return doc != null ? documentToEvent(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding event by ID: " + eventId, e);
            return null;
        }
    }
    
    /**
     * Delete an event (admin only, for corrections)
     */
    public boolean deleteEvent(String eventId) {
        try {
            ObjectId objectId = new ObjectId(eventId);
            eventsCollection.deleteOne(Filters.eq("_id", objectId));
            LOGGER.info("Deleted TrustScoreEvent: " + eventId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting event: " + eventId, e);
            return false;
        }
    }
    
    // ==================== TRUST SCORE EVENT QUERIES ====================
    
    /**
     * Get all events for a user (most recent first)
     */
    public List<TrustScoreEvent> getEventsForUser(String userId) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(Filters.eq("userId", userId))
                    .sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting events for user: " + userId, e);
        }
        return events;
    }
    
    /**
     * Get recent events for a user (limited)
     */
    public List<TrustScoreEvent> getRecentEventsForUser(String userId, int limit) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(Filters.eq("userId", userId))
                    .sort(Sorts.descending("timestamp"))
                    .limit(limit)) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recent events for user: " + userId, e);
        }
        return events;
    }
    
    /**
     * Get events by type
     */
    public List<TrustScoreEvent> getEventsByType(EventType eventType) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(Filters.eq("eventType", eventType.name()))
                    .sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting events by type: " + eventType, e);
        }
        return events;
    }
    
    /**
     * Get events related to an item
     */
    public List<TrustScoreEvent> getEventsByItemId(String itemId) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(Filters.eq("relatedItemId", itemId))
                    .sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting events by itemId: " + itemId, e);
        }
        return events;
    }
    
    /**
     * Get events related to a work request
     */
    public List<TrustScoreEvent> getEventsByRequestId(String requestId) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(Filters.eq("relatedRequestId", requestId))
                    .sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting events by requestId: " + requestId, e);
        }
        return events;
    }
    
    /**
     * Get events within a time range
     */
    public List<TrustScoreEvent> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(
                    Filters.and(
                        Filters.gte("timestamp", toDate(start)),
                        Filters.lte("timestamp", toDate(end))
                    )
                ).sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting events by date range", e);
        }
        return events;
    }
    
    /**
     * Get negative events for a user (for investigation)
     */
    public List<TrustScoreEvent> getNegativeEventsForUser(String userId) {
        List<TrustScoreEvent> events = new ArrayList<>();
        try {
            for (Document doc : eventsCollection.find(
                    Filters.and(
                        Filters.eq("userId", userId),
                        Filters.lt("pointsChange", 0)
                    )
                ).sort(Sorts.descending("timestamp"))) {
                events.add(documentToEvent(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting negative events for user: " + userId, e);
        }
        return events;
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Get average trust score across all users
     */
    public double getAverageScore() {
        try {
            Document result = scoresCollection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", null)
                    .append("avgScore", new Document("$avg", "$currentScore")))
            )).first();
            
            if (result != null) {
                return result.getDouble("avgScore");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating average score", e);
        }
        return 50.0; // Default
    }
    
    /**
     * Get count of users at each score level
     */
    public Map<ScoreLevel, Long> getScoreDistribution() {
        Map<ScoreLevel, Long> distribution = new LinkedHashMap<>();
        
        // Initialize all levels to 0
        for (ScoreLevel level : ScoreLevel.values()) {
            distribution.put(level, 0L);
        }
        
        try {
            for (Document doc : scoresCollection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$scoreLevel")
                    .append("count", new Document("$sum", 1)))
            ))) {
                String levelName = doc.getString("_id");
                if (levelName != null) {
                    ScoreLevel level = ScoreLevel.valueOf(levelName);
                    distribution.put(level, doc.getInteger("count").longValue());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting score distribution", e);
        }
        
        return distribution;
    }
    
    /**
     * Get total count of scores
     */
    public long getTotalScoresCount() {
        try {
            return scoresCollection.countDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting scores", e);
            return 0;
        }
    }
    
    /**
     * Get total count of events
     */
    public long getTotalEventsCount() {
        try {
            return eventsCollection.countDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting events", e);
            return 0;
        }
    }
    
    /**
     * Get count of events by type
     */
    public Map<EventType, Long> getEventTypeDistribution() {
        Map<EventType, Long> distribution = new LinkedHashMap<>();
        
        try {
            for (Document doc : eventsCollection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$eventType")
                    .append("count", new Document("$sum", 1)))
            ))) {
                String typeName = doc.getString("_id");
                if (typeName != null) {
                    try {
                        EventType type = EventType.valueOf(typeName);
                        distribution.put(type, doc.getInteger("count").longValue());
                    } catch (IllegalArgumentException e) {
                        // Skip unknown types
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting event type distribution", e);
        }
        
        return distribution;
    }
    
    /**
     * Get flagged users count
     */
    public long getFlaggedUsersCount() {
        try {
            return scoresCollection.countDocuments(Filters.eq("isFlagged", true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting flagged users", e);
            return 0;
        }
    }
    
    // ==================== FLAG OPERATIONS ====================
    
    /**
     * Flag a user
     */
    public boolean flagUser(String userId, String reason) {
        try {
            scoresCollection.updateOne(
                Filters.eq("userId", userId),
                Updates.combine(
                    Updates.set("isFlagged", true),
                    Updates.set("flagReason", reason),
                    Updates.set("flaggedAt", toDate(LocalDateTime.now())),
                    Updates.set("lastUpdatedAt", toDate(LocalDateTime.now()))
                )
            );
            LOGGER.info("Flagged user: " + userId + " reason: " + reason);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error flagging user: " + userId, e);
            return false;
        }
    }
    
    /**
     * Clear a user's flag
     */
    public boolean clearUserFlag(String userId) {
        try {
            scoresCollection.updateOne(
                Filters.eq("userId", userId),
                Updates.combine(
                    Updates.set("isFlagged", false),
                    Updates.unset("flagReason"),
                    Updates.unset("flaggedAt"),
                    Updates.set("lastUpdatedAt", toDate(LocalDateTime.now()))
                )
            );
            LOGGER.info("Cleared flag for user: " + userId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error clearing flag for user: " + userId, e);
            return false;
        }
    }
    
    /**
     * Start investigation for a user
     */
    public boolean startInvestigation(String userId) {
        try {
            scoresCollection.updateOne(
                Filters.eq("userId", userId),
                Updates.combine(
                    Updates.set("isUnderInvestigation", true),
                    Updates.set("lastUpdatedAt", toDate(LocalDateTime.now()))
                )
            );
            LOGGER.info("Started investigation for user: " + userId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting investigation for user: " + userId, e);
            return false;
        }
    }
    
    /**
     * End investigation for a user
     */
    public boolean endInvestigation(String userId) {
        try {
            scoresCollection.updateOne(
                Filters.eq("userId", userId),
                Updates.combine(
                    Updates.set("isUnderInvestigation", false),
                    Updates.set("lastUpdatedAt", toDate(LocalDateTime.now()))
                )
            );
            LOGGER.info("Ended investigation for user: " + userId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error ending investigation for user: " + userId, e);
            return false;
        }
    }
    
    // ==================== DOCUMENT CONVERSION ====================
    
    /**
     * Convert TrustScore to MongoDB Document
     */
    private Document trustScoreToDocument(TrustScore score) {
        Document doc = new Document();
        
        if (score.getScoreId() != null) {
            doc.append("_id", new ObjectId(score.getScoreId()));
        }
        
        doc.append("visibleId", score.getVisibleId())
           .append("userId", score.getUserId())
           .append("userName", score.getUserName())
           .append("userEmail", score.getUserEmail())
           .append("currentScore", score.getCurrentScore())
           .append("scoreLevel", score.getScoreLevel().name())
           .append("totalEventsCount", score.getTotalEventsCount())
           .append("positiveEventsCount", score.getPositiveEventsCount())
           .append("negativeEventsCount", score.getNegativeEventsCount())
           .append("totalPointsEarned", score.getTotalPointsEarned())
           .append("totalPointsLost", score.getTotalPointsLost())
           .append("isFlagged", score.isFlagged())
           .append("isUnderInvestigation", score.isUnderInvestigation())
           .append("flagReason", score.getFlagReason())
           .append("createdAt", toDate(score.getCreatedAt()))
           .append("lastUpdatedAt", toDate(score.getLastUpdatedAt()))
           .append("lastEventAt", toDate(score.getLastEventAt()))
           .append("flaggedAt", toDate(score.getFlaggedAt()));
        
        return doc;
    }
    
    /**
     * Convert MongoDB Document to TrustScore
     */
    private TrustScore documentToTrustScore(Document doc) {
        TrustScore score = new TrustScore();
        
        score.setScoreId(doc.getObjectId("_id").toString());
        score.setVisibleId(doc.getString("visibleId"));
        score.setUserId(doc.getString("userId"));
        score.setUserName(doc.getString("userName"));
        score.setUserEmail(doc.getString("userEmail"));
        score.setCurrentScore(doc.getDouble("currentScore") != null ? 
            doc.getDouble("currentScore") : TrustScore.DEFAULT_INITIAL_SCORE);
        
        String levelStr = doc.getString("scoreLevel");
        if (levelStr != null) {
            try {
                score.setScoreLevel(ScoreLevel.valueOf(levelStr));
            } catch (IllegalArgumentException e) {
                score.setScoreLevel(ScoreLevel.fromScore(score.getCurrentScore()));
            }
        }
        
        score.setTotalEventsCount(doc.getInteger("totalEventsCount", 0));
        score.setPositiveEventsCount(doc.getInteger("positiveEventsCount", 0));
        score.setNegativeEventsCount(doc.getInteger("negativeEventsCount", 0));
        score.setTotalPointsEarned(doc.getInteger("totalPointsEarned", 0));
        score.setTotalPointsLost(doc.getInteger("totalPointsLost", 0));
        score.setFlagged(doc.getBoolean("isFlagged", false));
        score.setUnderInvestigation(doc.getBoolean("isUnderInvestigation", false));
        score.setFlagReason(doc.getString("flagReason"));
        score.setCreatedAt(toLocalDateTime(doc.getDate("createdAt")));
        score.setLastUpdatedAt(toLocalDateTime(doc.getDate("lastUpdatedAt")));
        score.setLastEventAt(toLocalDateTime(doc.getDate("lastEventAt")));
        score.setFlaggedAt(toLocalDateTime(doc.getDate("flaggedAt")));
        
        return score;
    }
    
    /**
     * Convert TrustScoreEvent to MongoDB Document
     */
    private Document eventToDocument(TrustScoreEvent event) {
        Document doc = new Document();
        
        doc.append("visibleId", event.getVisibleId())
           .append("userId", event.getUserId())
           .append("userName", event.getUserName())
           .append("eventType", event.getEventType().name())
           .append("pointsChange", event.getPointsChange())
           .append("previousScore", event.getPreviousScore())
           .append("newScore", event.getNewScore())
           .append("description", event.getDescription())
           .append("relatedItemId", event.getRelatedItemId())
           .append("relatedRequestId", event.getRelatedRequestId())
           .append("relatedClaimId", event.getRelatedClaimId())
           .append("recordedById", event.getRecordedById())
           .append("recordedByName", event.getRecordedByName())
           .append("timestamp", toDate(event.getTimestamp()))
           .append("expiresAt", toDate(event.getExpiresAt()));
        
        return doc;
    }
    
    /**
     * Convert MongoDB Document to TrustScoreEvent
     */
    private TrustScoreEvent documentToEvent(Document doc) {
        TrustScoreEvent event = new TrustScoreEvent();
        
        event.setEventId(doc.getObjectId("_id").toString());
        event.setVisibleId(doc.getString("visibleId"));
        event.setUserId(doc.getString("userId"));
        event.setUserName(doc.getString("userName"));
        
        String typeStr = doc.getString("eventType");
        if (typeStr != null) {
            try {
                event.setEventType(EventType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown event type: " + typeStr);
                event.setEventType(EventType.MANUAL_ADJUSTMENT);
            }
        }
        
        event.setPointsChange(doc.getInteger("pointsChange", 0));
        event.setPreviousScore(doc.getDouble("previousScore") != null ? 
            doc.getDouble("previousScore") : 0.0);
        event.setNewScore(doc.getDouble("newScore") != null ? 
            doc.getDouble("newScore") : 0.0);
        event.setDescription(doc.getString("description"));
        event.setRelatedItemId(doc.getString("relatedItemId"));
        event.setRelatedRequestId(doc.getString("relatedRequestId"));
        event.setRelatedClaimId(doc.getString("relatedClaimId"));
        event.setRecordedById(doc.getString("recordedById"));
        event.setRecordedByName(doc.getString("recordedByName"));
        event.setTimestamp(toLocalDateTime(doc.getDate("timestamp")));
        event.setExpiresAt(toLocalDateTime(doc.getDate("expiresAt")));
        
        return event;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Convert LocalDateTime to Date for MongoDB
     */
    private Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Convert Date from MongoDB to LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
