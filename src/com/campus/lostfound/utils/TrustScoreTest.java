package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoTrustScoreDAO;
import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.models.trustscore.TrustScoreEvent;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;
import com.campus.lostfound.services.TrustScoreService;

import java.util.*;
import java.util.logging.Logger;

/**
 * Test class for Trust Score system (Part 4 of Developer 3)
 * Tests models, DAO, and service functionality
 */
public class TrustScoreTest {
    
    private static final Logger LOGGER = Logger.getLogger(TrustScoreTest.class.getName());
    
    private TrustScoreService service;
    private MongoTrustScoreDAO dao;
    private int passedTests = 0;
    private int failedTests = 0;
    
    public TrustScoreTest() {
        this.service = new TrustScoreService();
        this.dao = new MongoTrustScoreDAO();
    }
    
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧪 TRUST SCORE SYSTEM TESTS");
        System.out.println("=".repeat(60) + "\n");
        
        // Part 1: Model Tests
        testTrustScoreModel();
        testTrustScoreEventModel();
        testScoreLevelEnum();
        testEventTypeEnum();
        
        // Part 2: DAO Tests
        testDAOSaveAndFind();
        testDAOEventOperations();
        testDAOQueryMethods();
        testDAOStatistics();
        
        // Part 3: Service Tests
        testServiceGetOrCreate();
        testServiceRecordEvent();
        testServiceBusinessRules();
        testServiceFlagOperations();
        testServiceConvenienceMethods();
        testServiceStatistics();
        
        // Print summary
        printSummary();
    }
    
    // ==================== MODEL TESTS ====================
    
    private void testTrustScoreModel() {
        System.out.println("📋 Testing TrustScore Model...");
        
        // Test default constructor
        TrustScore score = new TrustScore();
        assertEqual("Default score", 50.0, score.getCurrentScore());
        assertEqual("Default level", ScoreLevel.FAIR, score.getScoreLevel());
        assertFalse("Not flagged by default", score.isFlagged());
        
        // Test constructor with userId
        TrustScore score2 = new TrustScore("user123");
        assertEqual("UserId set", "user123", score2.getUserId());
        
        // Test constructor with initial score
        TrustScore score3 = new TrustScore("user456", 85.0);
        assertEqual("Custom score", 85.0, score3.getCurrentScore());
        assertEqual("Good level", ScoreLevel.GOOD, score3.getScoreLevel());
        
        // Test score bounds
        TrustScore score4 = new TrustScore("user789", 150.0);
        assertEqual("Score capped at 100", 100.0, score4.getCurrentScore());
        
        TrustScore score5 = new TrustScore("user000", -50.0);
        assertEqual("Score floored at 0", 0.0, score5.getCurrentScore());
        
        // Test applyScoreChange
        TrustScore score6 = new TrustScore("test", 50.0);
        score6.applyScoreChange(10);
        assertEqual("Score after +10", 60.0, score6.getCurrentScore());
        score6.applyScoreChange(-15);
        assertEqual("Score after -15", 45.0, score6.getCurrentScore());
        
        // Test business rule methods
        TrustScore highScore = new TrustScore("high", 90.0);
        assertTrue("High score can claim high value", highScore.canClaimHighValueItem());
        assertTrue("High score can skip verification", highScore.canSkipVerification());
        assertFalse("High score doesn't require verification", highScore.requiresVerification());
        
        TrustScore lowScore = new TrustScore("low", 25.0);
        assertFalse("Low score cannot claim high value", lowScore.canClaimHighValueItem());
        assertTrue("Low score requires verification", lowScore.requiresVerification());
        assertTrue("Low score is low trust", lowScore.isLowTrust());
        
        System.out.println("   ✓ TrustScore model tests passed\n");
    }
    
    private void testTrustScoreEventModel() {
        System.out.println("📋 Testing TrustScoreEvent Model...");
        
        // Test default constructor
        TrustScoreEvent event = new TrustScoreEvent();
        assertNotNull("Timestamp set", event.getTimestamp());
        
        // Test constructor with userId and eventType
        TrustScoreEvent event2 = new TrustScoreEvent("user123", EventType.SUCCESSFUL_CLAIM);
        assertEqual("UserId set", "user123", event2.getUserId());
        assertEqual("EventType set", EventType.SUCCESSFUL_CLAIM, event2.getEventType());
        assertEqual("Default points", 10, event2.getPointsChange());
        
        // Test constructor with custom points
        TrustScoreEvent event3 = new TrustScoreEvent("user123", EventType.MANUAL_ADJUSTMENT, 
            -20, "Custom penalty");
        assertEqual("Custom points", -20, event3.getPointsChange());
        assertEqual("Custom description", "Custom penalty", event3.getDescription());
        
        // Test formatted points
        TrustScoreEvent positive = new TrustScoreEvent("u", EventType.SUCCESSFUL_CLAIM);
        assertEqual("Positive formatted", "+10", positive.getPointsChangeFormatted());
        
        TrustScoreEvent negative = new TrustScoreEvent("u", EventType.FALSE_CLAIM);
        assertEqual("Negative formatted", "-25", negative.getPointsChangeFormatted());
        
        System.out.println("   ✓ TrustScoreEvent model tests passed\n");
    }
    
    private void testScoreLevelEnum() {
        System.out.println("📋 Testing ScoreLevel Enum...");
        
        assertEqual("95 -> EXCELLENT", ScoreLevel.EXCELLENT, ScoreLevel.fromScore(95));
        assertEqual("90 -> EXCELLENT", ScoreLevel.EXCELLENT, ScoreLevel.fromScore(90));
        assertEqual("89 -> GOOD", ScoreLevel.GOOD, ScoreLevel.fromScore(89));
        assertEqual("70 -> GOOD", ScoreLevel.GOOD, ScoreLevel.fromScore(70));
        assertEqual("69 -> FAIR", ScoreLevel.FAIR, ScoreLevel.fromScore(69));
        assertEqual("50 -> FAIR", ScoreLevel.FAIR, ScoreLevel.fromScore(50));
        assertEqual("49 -> LOW", ScoreLevel.LOW, ScoreLevel.fromScore(49));
        assertEqual("30 -> LOW", ScoreLevel.LOW, ScoreLevel.fromScore(30));
        assertEqual("29 -> PROBATION", ScoreLevel.PROBATION, ScoreLevel.fromScore(29));
        assertEqual("0 -> PROBATION", ScoreLevel.PROBATION, ScoreLevel.fromScore(0));
        
        // Test color codes exist
        for (ScoreLevel level : ScoreLevel.values()) {
            assertNotNull("Color for " + level, level.getColorHex());
        }
        
        System.out.println("   ✓ ScoreLevel enum tests passed\n");
    }
    
    private void testEventTypeEnum() {
        System.out.println("📋 Testing EventType Enum...");
        
        // Test positive events
        assertTrue("SUCCESSFUL_CLAIM is positive", EventType.SUCCESSFUL_CLAIM.isPositive());
        assertTrue("REPORT_FOUND_ITEM is positive", EventType.REPORT_FOUND_ITEM.isPositive());
        assertEqual("SUCCESSFUL_CLAIM points", 10, EventType.SUCCESSFUL_CLAIM.getDefaultPoints());
        
        // Test negative events
        assertTrue("FALSE_CLAIM is negative", EventType.FALSE_CLAIM.isNegative());
        assertTrue("FRAUD_FLAG is negative", EventType.FRAUD_FLAG.isNegative());
        assertTrue("FRAUD_FLAG is severe", EventType.FRAUD_FLAG.isSevere());
        assertEqual("FALSE_CLAIM points", -25, EventType.FALSE_CLAIM.getDefaultPoints());
        
        // Test descriptions
        for (EventType type : EventType.values()) {
            assertNotNull("Description for " + type, type.getDescription());
        }
        
        System.out.println("   ✓ EventType enum tests passed\n");
    }
    
    // ==================== DAO TESTS ====================
    
    private void testDAOSaveAndFind() {
        System.out.println("📋 Testing DAO Save/Find Operations...");
        
        String testUserId = "test-dao-user-" + System.currentTimeMillis();
        
        // Create and save
        TrustScore score = new TrustScore(testUserId, 75.0);
        score.setUserName("Test User");
        score.setUserEmail(testUserId + "@test.com");
        
        String scoreId = dao.saveTrustScore(score);
        assertNotNull("Score ID returned", scoreId);
        
        // Find by ID
        TrustScore found = dao.findScoreById(scoreId);
        assertNotNull("Found by ID", found);
        assertEqual("Score matches", 75.0, found.getCurrentScore());
        
        // Find by userId
        TrustScore foundByUser = dao.findScoreByUserId(testUserId);
        assertNotNull("Found by userId", foundByUser);
        
        // Update
        found.setCurrentScore(80.0);
        dao.saveTrustScore(found);
        
        TrustScore updated = dao.findScoreById(scoreId);
        assertEqual("Score updated", 80.0, updated.getCurrentScore());
        
        // Cleanup
        dao.deleteTrustScore(scoreId);
        
        System.out.println("   ✓ DAO save/find tests passed\n");
    }
    
    private void testDAOEventOperations() {
        System.out.println("📋 Testing DAO Event Operations...");
        
        String testUserId = "test-events-" + System.currentTimeMillis();
        
        // Save events
        TrustScoreEvent event1 = new TrustScoreEvent(testUserId, EventType.REPORT_FOUND_ITEM);
        event1.setPreviousScore(50.0);
        event1.setNewScore(52.0);
        String eventId1 = dao.saveEvent(event1);
        assertNotNull("Event 1 saved", eventId1);
        
        TrustScoreEvent event2 = new TrustScoreEvent(testUserId, EventType.SUCCESSFUL_CLAIM);
        event2.setPreviousScore(52.0);
        event2.setNewScore(62.0);
        dao.saveEvent(event2);
        
        TrustScoreEvent event3 = new TrustScoreEvent(testUserId, EventType.FALSE_CLAIM);
        event3.setPreviousScore(62.0);
        event3.setNewScore(37.0);
        dao.saveEvent(event3);
        
        // Query events
        List<TrustScoreEvent> userEvents = dao.getEventsForUser(testUserId);
        assertEqual("3 events for user", 3, userEvents.size());
        
        List<TrustScoreEvent> recent = dao.getRecentEventsForUser(testUserId, 2);
        assertEqual("2 recent events", 2, recent.size());
        
        List<TrustScoreEvent> negative = dao.getNegativeEventsForUser(testUserId);
        assertEqual("1 negative event", 1, negative.size());
        
        // Find by ID
        TrustScoreEvent found = dao.findEventById(eventId1);
        assertNotNull("Found event by ID", found);
        assertEqual("Event type matches", EventType.REPORT_FOUND_ITEM, found.getEventType());
        
        System.out.println("   ✓ DAO event operations tests passed\n");
    }
    
    private void testDAOQueryMethods() {
        System.out.println("📋 Testing DAO Query Methods...");
        
        // These tests depend on existing data, so just verify methods don't throw
        try {
            List<TrustScore> all = dao.findAllScores();
            assertNotNull("findAllScores works", all);
            
            List<TrustScore> range = dao.getScoresByRange(40, 60);
            assertNotNull("getScoresByRange works", range);
            
            List<TrustScore> low = dao.getLowTrustUsers();
            assertNotNull("getLowTrustUsers works", low);
            
            List<TrustScore> excellent = dao.getExcellentUsers();
            assertNotNull("getExcellentUsers works", excellent);
            
            List<TrustScore> flagged = dao.getFlaggedUsers();
            assertNotNull("getFlaggedUsers works", flagged);
            
            List<TrustScore> byLevel = dao.getScoresByLevel(ScoreLevel.GOOD);
            assertNotNull("getScoresByLevel works", byLevel);
            
            System.out.println("   ✓ DAO query methods tests passed\n");
        } catch (Exception e) {
            failTest("DAO query methods failed: " + e.getMessage());
        }
    }
    
    private void testDAOStatistics() {
        System.out.println("📋 Testing DAO Statistics...");
        
        try {
            double avg = dao.getAverageScore();
            assertTrue("Average score >= 0", avg >= 0);
            assertTrue("Average score <= 100", avg <= 100);
            
            Map<ScoreLevel, Long> dist = dao.getScoreDistribution();
            assertNotNull("Distribution not null", dist);
            
            long totalScores = dao.getTotalScoresCount();
            assertTrue("Total scores >= 0", totalScores >= 0);
            
            long totalEvents = dao.getTotalEventsCount();
            assertTrue("Total events >= 0", totalEvents >= 0);
            
            long flaggedCount = dao.getFlaggedUsersCount();
            assertTrue("Flagged count >= 0", flaggedCount >= 0);
            
            System.out.println("   ✓ DAO statistics tests passed\n");
        } catch (Exception e) {
            failTest("DAO statistics failed: " + e.getMessage());
        }
    }
    
    // ==================== SERVICE TESTS ====================
    
    private void testServiceGetOrCreate() {
        System.out.println("📋 Testing Service getOrCreate...");
        
        String testUserId = "service-test-" + System.currentTimeMillis();
        
        // Should create new score
        TrustScore score = service.getOrCreateTrustScore(testUserId);
        assertNotNull("Score created", score);
        assertEqual("Default score", 50.0, score.getCurrentScore());
        
        // Should return existing score
        TrustScore score2 = service.getOrCreateTrustScore(testUserId);
        assertNotNull("Score retrieved", score2);
        assertEqual("Same score ID", score.getScoreId(), score2.getScoreId());
        
        // Quick lookups
        double numericScore = service.getTrustScore(testUserId);
        assertEqual("Numeric score", 50.0, numericScore);
        
        String display = service.getTrustScoreDisplay(testUserId);
        assertNotNull("Display string", display);
        assertTrue("Display contains score", display.contains("50"));
        
        ScoreLevel level = service.getScoreLevel(testUserId);
        assertEqual("Level is FAIR", ScoreLevel.FAIR, level);
        
        System.out.println("   ✓ Service getOrCreate tests passed\n");
    }
    
    private void testServiceRecordEvent() {
        System.out.println("📋 Testing Service recordEvent...");
        
        String testUserId = "event-test-" + System.currentTimeMillis();
        
        // Create initial score
        service.getOrCreateTrustScore(testUserId);
        
        // Record positive event
        TrustScoreEvent event1 = service.recordEvent(testUserId, EventType.REPORT_FOUND_ITEM, "Test event");
        assertNotNull("Event recorded", event1);
        assertEqual("Points applied", 2, event1.getPointsChange());
        assertEqual("Previous score", 50.0, event1.getPreviousScore());
        assertEqual("New score", 52.0, event1.getNewScore());
        
        // Verify score updated
        double newScore = service.getTrustScore(testUserId);
        assertEqual("Score is 52", 52.0, newScore);
        
        // Record negative event
        TrustScoreEvent event2 = service.recordEvent(testUserId, EventType.CLAIM_REJECTED, "Test rejection");
        assertEqual("Negative points", -5, event2.getPointsChange());
        
        double afterNegative = service.getTrustScore(testUserId);
        assertEqual("Score after negative", 47.0, afterNegative);
        
        // Test with custom points
        TrustScoreEvent custom = service.recordEventWithCustomPoints(testUserId, 
            EventType.MANUAL_ADJUSTMENT, 10, "Admin bonus");
        assertEqual("Custom points applied", 10, custom.getPointsChange());
        
        System.out.println("   ✓ Service recordEvent tests passed\n");
    }
    
    private void testServiceBusinessRules() {
        System.out.println("📋 Testing Service Business Rules...");
        
        // Test with different score levels
        String lowUser = "low-user-" + System.currentTimeMillis();
        String highUser = "high-user-" + System.currentTimeMillis();
        
        // Create low trust user
        TrustScore lowScore = service.getOrCreateTrustScore(lowUser);
        service.recordEventWithCustomPoints(lowUser, EventType.MANUAL_ADJUSTMENT, -30, "Set to 20");
        
        assertFalse("Low user cannot claim high value", service.canClaimHighValueItem(lowUser));
        assertTrue("Low user requires verification", service.requiresAdditionalVerification(lowUser));
        assertTrue("Low user is low trust", service.isLowTrust(lowUser));
        
        // Create high trust user
        TrustScore highScore = service.getOrCreateTrustScore(highUser);
        service.recordEventWithCustomPoints(highUser, EventType.MANUAL_ADJUSTMENT, 40, "Set to 90");
        
        assertTrue("High user can claim high value", service.canClaimHighValueItem(highUser));
        assertTrue("High user can skip verification", service.canClaimWithoutVerification(highUser));
        assertFalse("High user doesn't require verification", service.requiresAdditionalVerification(highUser));
        
        // Test claim value limits
        double maxValue = service.getMaxClaimValueWithoutVerification(highUser);
        assertTrue("Max value > 0 for high user", maxValue > 0);
        
        assertTrue("$100 claim needs verification for low user", 
            service.claimNeedsVerification(lowUser, 100));
        
        System.out.println("   ✓ Service business rules tests passed\n");
    }
    
    private void testServiceFlagOperations() {
        System.out.println("📋 Testing Service Flag Operations...");
        
        String testUser = "flag-test-" + System.currentTimeMillis();
        service.getOrCreateTrustScore(testUser);
        
        // Flag user
        boolean flagged = service.flagUser(testUser, "Test flag reason", "admin123");
        assertTrue("User flagged successfully", flagged);
        assertTrue("User is flagged", service.isFlagged(testUser));
        
        // Clear flag
        boolean cleared = service.clearUserFlag(testUser, "admin123");
        assertTrue("Flag cleared successfully", cleared);
        assertFalse("User no longer flagged", service.isFlagged(testUser));
        
        // Investigation
        service.startInvestigation(testUser, "admin123");
        assertTrue("User under investigation", service.isUnderInvestigation(testUser));
        
        service.endInvestigation(testUser, "admin123", true);
        assertFalse("Investigation ended", service.isUnderInvestigation(testUser));
        
        System.out.println("   ✓ Service flag operations tests passed\n");
    }
    
    private void testServiceConvenienceMethods() {
        System.out.println("📋 Testing Service Convenience Methods...");
        
        String testUser = "convenience-test-" + System.currentTimeMillis();
        service.getOrCreateTrustScore(testUser);
        
        // Test all convenience methods
        TrustScoreEvent e1 = service.recordFoundItemReport(testUser, "item1");
        assertNotNull("recordFoundItemReport works", e1);
        assertEqual("Found item points", 2, e1.getPointsChange());
        
        TrustScoreEvent e2 = service.recordLostItemReport(testUser, "item2");
        assertNotNull("recordLostItemReport works", e2);
        assertEqual("Lost item points", 1, e2.getPointsChange());
        
        TrustScoreEvent e3 = service.recordSuccessfulClaim(testUser, "item3", "claim1");
        assertNotNull("recordSuccessfulClaim works", e3);
        assertEqual("Successful claim points", 10, e3.getPointsChange());
        
        TrustScoreEvent e4 = service.recordRequestApproval(testUser, "req1");
        assertNotNull("recordRequestApproval works", e4);
        assertEqual("Approval points", 5, e4.getPointsChange());
        
        // Negative events
        TrustScoreEvent e5 = service.recordClaimRejected(testUser, "item4", "claim2");
        assertNotNull("recordClaimRejected works", e5);
        assertEqual("Rejected claim points", -5, e5.getPointsChange());
        
        System.out.println("   ✓ Service convenience methods tests passed\n");
    }
    
    private void testServiceStatistics() {
        System.out.println("📋 Testing Service Statistics...");
        
        try {
            TrustScoreService.TrustScoreStats stats = service.getStatistics();
            assertNotNull("Stats not null", stats);
            assertTrue("Total users >= 0", stats.totalUsers >= 0);
            assertTrue("Total events >= 0", stats.totalEvents >= 0);
            assertTrue("Average score >= 0", stats.averageScore >= 0);
            assertNotNull("Score distribution", stats.scoreDistribution);
            
            System.out.println("   📊 Statistics Summary:");
            System.out.println("      Total Users: " + stats.totalUsers);
            System.out.println("      Total Events: " + stats.totalEvents);
            System.out.println("      Average Score: " + String.format("%.1f", stats.averageScore));
            System.out.println("      Flagged Users: " + stats.flaggedUsers);
            
            System.out.println("   ✓ Service statistics tests passed\n");
        } catch (Exception e) {
            failTest("Service statistics failed: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void assertEqual(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            passedTests++;
            return;
        }
        if (expected != null && expected.equals(actual)) {
            passedTests++;
        } else {
            failedTests++;
            System.out.println("   ❌ FAIL: " + message + " - Expected: " + expected + ", Got: " + actual);
        }
    }
    
    private void assertTrue(String message, boolean condition) {
        if (condition) {
            passedTests++;
        } else {
            failedTests++;
            System.out.println("   ❌ FAIL: " + message + " - Expected true");
        }
    }
    
    private void assertFalse(String message, boolean condition) {
        if (!condition) {
            passedTests++;
        } else {
            failedTests++;
            System.out.println("   ❌ FAIL: " + message + " - Expected false");
        }
    }
    
    private void assertNotNull(String message, Object obj) {
        if (obj != null) {
            passedTests++;
        } else {
            failedTests++;
            System.out.println("   ❌ FAIL: " + message + " - Was null");
        }
    }
    
    private void failTest(String message) {
        failedTests++;
        System.out.println("   ❌ FAIL: " + message);
    }
    
    private void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 TEST SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("✅ Passed: " + passedTests);
        System.out.println("❌ Failed: " + failedTests);
        System.out.println("📈 Total:  " + (passedTests + failedTests));
        
        if (failedTests == 0) {
            System.out.println("\n🎉 ALL TESTS PASSED!");
        } else {
            System.out.println("\n⚠️  Some tests failed. Review the output above.");
        }
        System.out.println("=".repeat(60) + "\n");
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        TrustScoreTest test = new TrustScoreTest();
        test.runAllTests();
    }
}
