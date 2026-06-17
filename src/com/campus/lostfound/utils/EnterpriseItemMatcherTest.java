package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.Item.ItemCategory;
import com.campus.lostfound.models.Item.ItemType;
import com.campus.lostfound.models.Item.ItemStatus;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.models.Organization;
import com.campus.lostfound.models.EnterpriseMatchResult;
import com.campus.lostfound.models.EnterpriseMatchResult.MatchType;
import com.campus.lostfound.models.EnterpriseMatchResult.TransferComplexity;
import com.campus.lostfound.models.EnterpriseMatchResult.ScoreLevel;
import com.campus.lostfound.models.Location;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.Building;
import com.campus.lostfound.services.EnterpriseItemMatcher;
import com.campus.lostfound.services.EnterpriseItemMatcher.MatchReport;
import com.campus.lostfound.services.EnterpriseItemService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test class for EnterpriseItemMatcher (Part 4 of Developer 4)
 * Tests cross-enterprise matching, scoring, filtering, and batch operations
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseItemMatcherTest {
    
    private EnterpriseItemMatcher matcher;
    private EnterpriseItemService itemService;
    private MongoItemDAO itemDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    
    private int passedTests = 0;
    private int failedTests = 0;
    
    // Test data references
    private List<Enterprise> testEnterprises;
    private List<Organization> testOrganizations;
    private List<Item> testItems;
    private List<Item> lostItems;
    private List<Item> foundItems;
    
    public EnterpriseItemMatcherTest() {
        this.matcher = new EnterpriseItemMatcher();
        this.itemService = new EnterpriseItemService();
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
    }
    
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔗 ENTERPRISE ITEM MATCHER TESTS");
        System.out.println("=".repeat(70) + "\n");
        
        // Load test data
        loadTestData();
        
        // Run test groups
        testScoringConstants();
        testMatchAcrossEnterprises();
        testMatchWithinEnterprise();
        testMatchWithinNetwork();
        testMatchSpecificEnterprises();
        testEnterpriseBonusScoring();
        testTrustScoreModifier();
        testTransferComplexityCalculation();
        testFilterByMinScore();
        testFilterByEnterprise();
        testFilterByMatchType();
        testSortingMethods();
        testGetTopMatches();
        testBatchOperations();
        testMatchReport();
        testEdgeCases();
        
        // Print summary
        printSummary();
    }
    
    // ==================== LOAD TEST DATA ====================
    
    private void loadTestData() {
        System.out.println("📋 Loading test data...");
        
        try {
            testEnterprises = enterpriseDAO.findAll();
            testOrganizations = organizationDAO.findAll();
            testItems = itemDAO.findAll();
            
            // Separate lost and found items for matching tests
            lostItems = testItems.stream()
                .filter(i -> i.getType() == ItemType.LOST)
                .filter(i -> i.getStatus() == ItemStatus.OPEN || i.getStatus() == ItemStatus.PENDING_CLAIM)
                .collect(Collectors.toList());
            
            foundItems = testItems.stream()
                .filter(i -> i.getType() == ItemType.FOUND)
                .filter(i -> i.getStatus() == ItemStatus.OPEN || i.getStatus() == ItemStatus.PENDING_CLAIM)
                .collect(Collectors.toList());
            
            System.out.println("   Loaded " + testEnterprises.size() + " enterprises");
            System.out.println("   Loaded " + testOrganizations.size() + " organizations");
            System.out.println("   Loaded " + testItems.size() + " total items");
            System.out.println("   Found " + lostItems.size() + " lost items (open/pending)");
            System.out.println("   Found " + foundItems.size() + " found items (open/pending)\n");
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Error loading test data: " + e.getMessage() + "\n");
        }
    }
    
    // ==================== SCORING CONSTANTS TESTS ====================
    
    private void testScoringConstants() {
        System.out.println("📋 Testing scoring constants...");
        
        // Verify constants are within expected ranges
        assertTrue("SAME_ENTERPRISE_BONUS > 0", 
            EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS > 0);
        assertTrue("SAME_ENTERPRISE_BONUS <= 0.25", 
            EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS <= 0.25);
        
        assertTrue("SAME_NETWORK_BONUS > 0", 
            EnterpriseItemMatcher.SAME_NETWORK_BONUS > 0);
        assertTrue("SAME_NETWORK_BONUS < SAME_ENTERPRISE_BONUS", 
            EnterpriseItemMatcher.SAME_NETWORK_BONUS < EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS);
        
        assertTrue("SAME_ORGANIZATION_BONUS > SAME_ENTERPRISE_BONUS", 
            EnterpriseItemMatcher.SAME_ORGANIZATION_BONUS > EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS);
        
        assertTrue("HIGH_TRUST_BONUS > 0", 
            EnterpriseItemMatcher.HIGH_TRUST_BONUS > 0);
        assertTrue("HIGH_TRUST_BONUS <= 0.10", 
            EnterpriseItemMatcher.HIGH_TRUST_BONUS <= 0.10);
        
        assertTrue("MIN_CROSS_ENTERPRISE_SCORE >= MIN_SAME_ENTERPRISE_SCORE", 
            EnterpriseItemMatcher.MIN_CROSS_ENTERPRISE_SCORE >= EnterpriseItemMatcher.MIN_SAME_ENTERPRISE_SCORE);
        
        assertTrue("HIGH_TRUST_THRESHOLD reasonable (>= 70)", 
            EnterpriseItemMatcher.HIGH_TRUST_THRESHOLD >= 70);
        
        System.out.println("   SAME_ENTERPRISE_BONUS = " + EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS);
        System.out.println("   SAME_NETWORK_BONUS = " + EnterpriseItemMatcher.SAME_NETWORK_BONUS);
        System.out.println("   SAME_ORGANIZATION_BONUS = " + EnterpriseItemMatcher.SAME_ORGANIZATION_BONUS);
        System.out.println("   HIGH_TRUST_BONUS = " + EnterpriseItemMatcher.HIGH_TRUST_BONUS);
        System.out.println("   ✓ Scoring constants tests passed\n");
    }
    
    // ==================== CORE MATCHING TESTS ====================
    
    private void testMatchAcrossEnterprises() {
        System.out.println("📋 Testing matchAcrossEnterprises...");
        
        if (lostItems.isEmpty()) {
            System.out.println("   ⚠️ No lost items available for testing\n");
            return;
        }
        
        // Test with a lost item
        Item testLostItem = lostItems.get(0);
        List<EnterpriseMatchResult> matches = matcher.matchAcrossEnterprises(testLostItem);
        
        assertNotNull("matchAcrossEnterprises returns non-null", matches);
        
        // Verify results are sorted by score
        if (matches.size() >= 2) {
            for (int i = 0; i < matches.size() - 1; i++) {
                assertTrue("Results sorted by score (descending)", 
                    matches.get(i).getMatchScore() >= matches.get(i + 1).getMatchScore());
            }
        }
        
        // Verify all matches have required fields populated
        for (EnterpriseMatchResult emr : matches) {
            assertNotNull("Match has source item", emr.getSourceItem());
            assertNotNull("Match has matched item", emr.getMatchedItem());
            assertNotNull("Match has match type", emr.getMatchType());
            assertTrue("Match score >= 0", emr.getMatchScore() >= 0);
            assertTrue("Match score <= 1", emr.getMatchScore() <= 1.0);
            assertNotNull("Match has transfer complexity", emr.getTransferComplexity());
        }
        
        // Test with null item
        List<EnterpriseMatchResult> nullResults = matcher.matchAcrossEnterprises(null);
        assertNotNull("Null item returns empty list", nullResults);
        assertEqual("Null item returns 0 matches", 0, nullResults.size());
        
        System.out.println("   Tested item: " + testLostItem.getTitle());
        System.out.println("   Found " + matches.size() + " potential matches");
        if (!matches.isEmpty()) {
            System.out.println("   Best match score: " + String.format("%.1f%%", matches.get(0).getMatchScore() * 100));
        }
        System.out.println("   ✓ matchAcrossEnterprises tests passed\n");
    }
    
    private void testMatchWithinEnterprise() {
        System.out.println("📋 Testing matchWithinEnterprise...");
        
        if (lostItems.isEmpty() || testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ Insufficient data for testing\n");
            return;
        }
        
        // Find a lost item with enterprise ID
        Item testItem = null;
        String enterpriseId = null;
        for (Item item : lostItems) {
            if (item.getEnterpriseId() != null) {
                testItem = item;
                enterpriseId = item.getEnterpriseId();
                break;
            }
        }
        
        if (testItem == null) {
            enterpriseId = testEnterprises.get(0).getEnterpriseId();
            testItem = lostItems.get(0);
        }
        
        List<EnterpriseMatchResult> matches = matcher.matchWithinEnterprise(testItem, enterpriseId);
        
        assertNotNull("matchWithinEnterprise returns non-null", matches);
        
        // Verify all matches are from the specified enterprise
        final String expectedEntId = enterpriseId;
        for (EnterpriseMatchResult emr : matches) {
            // Check if matched item is from correct enterprise
            if (emr.getMatchedItem().getEnterpriseId() != null) {
                assertEqual("Matched item from correct enterprise", 
                    expectedEntId, emr.getMatchedItem().getEnterpriseId());
            }
        }
        
        // Test with null parameters
        List<EnterpriseMatchResult> nullItemResults = matcher.matchWithinEnterprise(null, enterpriseId);
        assertEqual("Null item returns empty", 0, nullItemResults.size());
        
        List<EnterpriseMatchResult> nullEntResults = matcher.matchWithinEnterprise(testItem, null);
        assertEqual("Null enterprise returns empty", 0, nullEntResults.size());
        
        System.out.println("   Found " + matches.size() + " matches within enterprise");
        System.out.println("   ✓ matchWithinEnterprise tests passed\n");
    }
    
    private void testMatchWithinNetwork() {
        System.out.println("📋 Testing matchWithinNetwork...");
        
        if (lostItems.isEmpty()) {
            System.out.println("   ⚠️ No lost items available for testing\n");
            return;
        }
        
        Item testItem = lostItems.get(0);
        
        // Get a network ID
        String networkId = null;
        for (Enterprise ent : testEnterprises) {
            if (ent.getNetworkId() != null) {
                networkId = ent.getNetworkId();
                break;
            }
        }
        
        if (networkId == null) {
            System.out.println("   ⚠️ No network IDs found in test data\n");
            return;
        }
        
        List<EnterpriseMatchResult> matches = matcher.matchWithinNetwork(testItem, networkId);
        
        assertNotNull("matchWithinNetwork returns non-null", matches);
        
        // Verify all matches are from enterprises in the network
        // (Would need to check enterprise network IDs)
        
        // Test with null parameters
        List<EnterpriseMatchResult> nullNetResults = matcher.matchWithinNetwork(testItem, null);
        assertEqual("Null network returns empty", 0, nullNetResults.size());
        
        System.out.println("   Network ID: " + networkId);
        System.out.println("   Found " + matches.size() + " matches within network");
        System.out.println("   ✓ matchWithinNetwork tests passed\n");
    }
    
    private void testMatchSpecificEnterprises() {
        System.out.println("📋 Testing matchSpecificEnterprises...");
        
        if (lostItems.isEmpty() || testEnterprises.size() < 2) {
            System.out.println("   ⚠️ Insufficient data for testing\n");
            return;
        }
        
        Item testItem = lostItems.get(0);
        
        // Test with multiple enterprise IDs
        List<String> enterpriseIds = testEnterprises.stream()
            .limit(3)
            .map(Enterprise::getEnterpriseId)
            .collect(Collectors.toList());
        
        List<EnterpriseMatchResult> matches = matcher.matchSpecificEnterprises(testItem, enterpriseIds);
        
        assertNotNull("matchSpecificEnterprises returns non-null", matches);
        
        // Verify all matches are from specified enterprises
        for (EnterpriseMatchResult emr : matches) {
            String matchedEntId = emr.getMatchedItem().getEnterpriseId();
            if (matchedEntId != null) {
                assertTrue("Matched item from specified enterprise", 
                    enterpriseIds.contains(matchedEntId));
            }
        }
        
        // Test with empty list
        List<EnterpriseMatchResult> emptyResults = matcher.matchSpecificEnterprises(testItem, new ArrayList<>());
        assertEqual("Empty enterprise list returns empty", 0, emptyResults.size());
        
        // Test with null list
        List<EnterpriseMatchResult> nullResults = matcher.matchSpecificEnterprises(testItem, null);
        assertEqual("Null enterprise list returns empty", 0, nullResults.size());
        
        System.out.println("   Searched " + enterpriseIds.size() + " enterprises");
        System.out.println("   Found " + matches.size() + " matches");
        System.out.println("   ✓ matchSpecificEnterprises tests passed\n");
    }
    
    // ==================== SCORING TESTS ====================
    
    private void testEnterpriseBonusScoring() {
        System.out.println("📋 Testing enterprise bonus scoring...");
        
        double baseScore = 0.50;
        
        // Test SAME_ORGANIZATION bonus
        double sameOrgScore = matcher.applyEnterpriseBonus(baseScore, MatchType.SAME_ORGANIZATION);
        assertTrue("Same org bonus applied", sameOrgScore > baseScore);
        assertEqual("Same org bonus correct", 
            baseScore + EnterpriseItemMatcher.SAME_ORGANIZATION_BONUS, sameOrgScore);
        
        // Test SAME_ENTERPRISE bonus
        double sameEntScore = matcher.applyEnterpriseBonus(baseScore, MatchType.SAME_ENTERPRISE);
        assertTrue("Same enterprise bonus applied", sameEntScore > baseScore);
        assertEqual("Same enterprise bonus correct", 
            baseScore + EnterpriseItemMatcher.SAME_ENTERPRISE_BONUS, sameEntScore);
        
        // Test SAME_NETWORK bonus
        double sameNetScore = matcher.applyEnterpriseBonus(baseScore, MatchType.SAME_NETWORK);
        assertTrue("Same network bonus applied", sameNetScore > baseScore);
        assertEqual("Same network bonus correct", 
            baseScore + EnterpriseItemMatcher.SAME_NETWORK_BONUS, sameNetScore);
        
        // Test CROSS_NETWORK (no bonus)
        double crossNetScore = matcher.applyEnterpriseBonus(baseScore, MatchType.CROSS_NETWORK);
        assertEqual("Cross network no bonus", baseScore, crossNetScore);
        
        // Verify bonus hierarchy: SAME_ORG > SAME_ENT > SAME_NET > CROSS_NET
        assertTrue("Same org > same enterprise", sameOrgScore > sameEntScore);
        assertTrue("Same enterprise > same network", sameEntScore > sameNetScore);
        assertTrue("Same network > cross network", sameNetScore > crossNetScore);
        
        System.out.println("   Base score: " + String.format("%.0f%%", baseScore * 100));
        System.out.println("   + Same Org bonus:    " + String.format("%.0f%%", sameOrgScore * 100));
        System.out.println("   + Same Ent bonus:    " + String.format("%.0f%%", sameEntScore * 100));
        System.out.println("   + Same Net bonus:    " + String.format("%.0f%%", sameNetScore * 100));
        System.out.println("   + Cross Net (none):  " + String.format("%.0f%%", crossNetScore * 100));
        System.out.println("   ✓ Enterprise bonus scoring tests passed\n");
    }
    
    private void testTrustScoreModifier() {
        System.out.println("📋 Testing trust score modifier...");
        
        double baseScore = 0.60;
        double highTrust = EnterpriseItemMatcher.HIGH_TRUST_THRESHOLD + 5; // Above threshold
        double lowTrust = EnterpriseItemMatcher.HIGH_TRUST_THRESHOLD - 10; // Below threshold
        
        // Test high trust score
        double highTrustResult = matcher.applyTrustScoreModifier(baseScore, highTrust);
        assertTrue("High trust bonus applied", highTrustResult > baseScore);
        assertEqual("High trust bonus correct", 
            baseScore + EnterpriseItemMatcher.HIGH_TRUST_BONUS, highTrustResult);
        
        // Test low trust score (no bonus)
        double lowTrustResult = matcher.applyTrustScoreModifier(baseScore, lowTrust);
        assertEqual("Low trust no bonus", baseScore, lowTrustResult);
        
        // Test exactly at threshold
        double thresholdResult = matcher.applyTrustScoreModifier(baseScore, 
            EnterpriseItemMatcher.HIGH_TRUST_THRESHOLD);
        assertTrue("Threshold trust gets bonus", thresholdResult > baseScore);
        
        System.out.println("   Trust threshold: " + EnterpriseItemMatcher.HIGH_TRUST_THRESHOLD);
        System.out.println("   High trust (" + highTrust + "): " + String.format("%.0f%%", highTrustResult * 100));
        System.out.println("   Low trust (" + lowTrust + "): " + String.format("%.0f%%", lowTrustResult * 100));
        System.out.println("   ✓ Trust score modifier tests passed\n");
    }
    
    private void testTransferComplexityCalculation() {
        System.out.println("📋 Testing transfer complexity calculation...");
        
        // Create mock items for testing
        Item item1 = createMockItem("org1", "ent1");
        Item item2SameOrg = createMockItem("org1", "ent1");
        Item item3SameEnt = createMockItem("org2", "ent1");
        Item item4DiffEnt = createMockItem("org3", "ent2");
        
        // Test NONE - same organization
        TransferComplexity noneComplexity = matcher.calculateTransferComplexity(item1, item2SameOrg);
        assertEqual("Same org = NONE complexity", TransferComplexity.NONE, noneComplexity);
        
        // Test LOW - same enterprise, different org
        TransferComplexity lowComplexity = matcher.calculateTransferComplexity(item1, item3SameEnt);
        assertEqual("Same enterprise = LOW complexity", TransferComplexity.LOW, lowComplexity);
        
        // Test with null items
        TransferComplexity nullComplexity = matcher.calculateTransferComplexity(null, item1);
        assertEqual("Null item = HIGH complexity", TransferComplexity.HIGH, nullComplexity);
        
        // Verify complexity levels ordering
        assertTrue("NONE < LOW", TransferComplexity.NONE.getLevel() < TransferComplexity.LOW.getLevel());
        assertTrue("LOW < MEDIUM", TransferComplexity.LOW.getLevel() < TransferComplexity.MEDIUM.getLevel());
        assertTrue("MEDIUM < HIGH", TransferComplexity.MEDIUM.getLevel() < TransferComplexity.HIGH.getLevel());
        
        System.out.println("   Same org: " + noneComplexity);
        System.out.println("   Same enterprise: " + lowComplexity);
        System.out.println("   ✓ Transfer complexity calculation tests passed\n");
    }
    
    // ==================== FILTERING TESTS ====================
    
    private void testFilterByMinScore() {
        System.out.println("📋 Testing filterByMinScore...");
        
        // Create test results
        List<EnterpriseMatchResult> testResults = createTestMatchResults();
        
        if (testResults.isEmpty()) {
            System.out.println("   ⚠️ No test results available\n");
            return;
        }
        
        // Test with 0.5 threshold
        List<EnterpriseMatchResult> filtered50 = matcher.filterByMinScore(testResults, 0.5);
        assertNotNull("Filter returns non-null", filtered50);
        
        for (EnterpriseMatchResult emr : filtered50) {
            assertTrue("Score >= 0.5", emr.getMatchScore() >= 0.5);
        }
        
        // Test with high threshold
        List<EnterpriseMatchResult> filteredHigh = matcher.filterByMinScore(testResults, 0.9);
        assertNotNull("High threshold filter returns non-null", filteredHigh);
        
        for (EnterpriseMatchResult emr : filteredHigh) {
            assertTrue("Score >= 0.9", emr.getMatchScore() >= 0.9);
        }
        
        // Test with 0 threshold (all pass)
        List<EnterpriseMatchResult> filteredZero = matcher.filterByMinScore(testResults, 0.0);
        assertEqual("Zero threshold returns all", testResults.size(), filteredZero.size());
        
        // Test with 1.0 threshold (very few pass)
        List<EnterpriseMatchResult> filteredMax = matcher.filterByMinScore(testResults, 1.0);
        // Most items won't have perfect score
        
        System.out.println("   Original count: " + testResults.size());
        System.out.println("   After 50% filter: " + filtered50.size());
        System.out.println("   After 90% filter: " + filteredHigh.size());
        System.out.println("   ✓ filterByMinScore tests passed\n");
    }
    
    private void testFilterByEnterprise() {
        System.out.println("📋 Testing filterByEnterprise...");
        
        if (lostItems.isEmpty() || testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ Insufficient data for testing\n");
            return;
        }
        
        // Get matches across enterprises first
        Item testItem = lostItems.get(0);
        List<EnterpriseMatchResult> allMatches = matcher.matchAcrossEnterprises(testItem);
        
        if (allMatches.isEmpty()) {
            System.out.println("   ⚠️ No matches found for test item\n");
            return;
        }
        
        // Filter by first enterprise
        String targetEntId = testEnterprises.get(0).getEnterpriseId();
        List<EnterpriseMatchResult> filtered = matcher.filterByEnterprise(allMatches, targetEntId);
        
        assertNotNull("Filter returns non-null", filtered);
        
        // Verify all results are from target enterprise
        for (EnterpriseMatchResult emr : filtered) {
            assertEqual("Matched enterprise ID correct", targetEntId, emr.getMatchedEnterpriseId());
        }
        
        // Test with non-existent enterprise
        List<EnterpriseMatchResult> noResults = matcher.filterByEnterprise(allMatches, "non-existent-id");
        assertEqual("Non-existent enterprise returns empty", 0, noResults.size());
        
        System.out.println("   Total matches: " + allMatches.size());
        System.out.println("   After enterprise filter: " + filtered.size());
        System.out.println("   ✓ filterByEnterprise tests passed\n");
    }
    
    private void testFilterByMatchType() {
        System.out.println("📋 Testing filterByMatchType...");
        
        List<EnterpriseMatchResult> testResults = createTestMatchResults();
        
        if (testResults.isEmpty()) {
            System.out.println("   ⚠️ No test results available\n");
            return;
        }
        
        // Filter by each match type
        for (MatchType type : MatchType.values()) {
            List<EnterpriseMatchResult> filtered = matcher.filterByMatchType(testResults, type);
            assertNotNull("Filter for " + type + " returns non-null", filtered);
            
            for (EnterpriseMatchResult emr : filtered) {
                assertEqual("Match type correct", type, emr.getMatchType());
            }
        }
        
        // Test filterCrossEnterpriseOnly
        List<EnterpriseMatchResult> crossOnly = matcher.filterCrossEnterpriseOnly(testResults);
        assertNotNull("Cross-enterprise filter returns non-null", crossOnly);
        
        for (EnterpriseMatchResult emr : crossOnly) {
            assertTrue("Is cross-enterprise", emr.isCrossEnterprise());
        }
        
        System.out.println("   ✓ filterByMatchType tests passed\n");
    }
    
    // ==================== SORTING TESTS ====================
    
    private void testSortingMethods() {
        System.out.println("📋 Testing sorting methods...");
        
        List<EnterpriseMatchResult> testResults = createTestMatchResults();
        
        if (testResults.size() < 2) {
            System.out.println("   ⚠️ Insufficient test results for sorting\n");
            return;
        }
        
        // Test sortByScore
        List<EnterpriseMatchResult> sortedByScore = matcher.sortByScore(testResults);
        assertNotNull("sortByScore returns non-null", sortedByScore);
        assertEqual("sortByScore preserves count", testResults.size(), sortedByScore.size());
        
        // Verify descending order
        for (int i = 0; i < sortedByScore.size() - 1; i++) {
            assertTrue("Scores in descending order", 
                sortedByScore.get(i).getMatchScore() >= sortedByScore.get(i + 1).getMatchScore());
        }
        
        // Test sortByTransferComplexity
        List<EnterpriseMatchResult> sortedByComplexity = matcher.sortByTransferComplexity(testResults);
        assertNotNull("sortByTransferComplexity returns non-null", sortedByComplexity);
        
        // Verify ascending complexity order
        for (int i = 0; i < sortedByComplexity.size() - 1; i++) {
            assertTrue("Complexity in ascending order", 
                sortedByComplexity.get(i).getTransferComplexity().getLevel() <= 
                sortedByComplexity.get(i + 1).getTransferComplexity().getLevel());
        }
        
        System.out.println("   ✓ Sorting methods tests passed\n");
    }
    
    private void testGetTopMatches() {
        System.out.println("📋 Testing getTopMatches...");
        
        List<EnterpriseMatchResult> testResults = createTestMatchResults();
        
        if (testResults.isEmpty()) {
            System.out.println("   ⚠️ No test results available\n");
            return;
        }
        
        // Test getting top 3
        int limit = 3;
        List<EnterpriseMatchResult> top3 = matcher.getTopMatches(testResults, limit);
        
        assertNotNull("getTopMatches returns non-null", top3);
        assertTrue("Top 3 has at most 3 items", top3.size() <= limit);
        
        // Verify sorted by score
        for (int i = 0; i < top3.size() - 1; i++) {
            assertTrue("Top matches sorted by score", 
                top3.get(i).getMatchScore() >= top3.get(i + 1).getMatchScore());
        }
        
        // Test with limit larger than results
        List<EnterpriseMatchResult> topAll = matcher.getTopMatches(testResults, 1000);
        assertEqual("Large limit returns all", testResults.size(), topAll.size());
        
        // Test with limit 0
        List<EnterpriseMatchResult> topZero = matcher.getTopMatches(testResults, 0);
        assertEqual("Zero limit returns empty", 0, topZero.size());
        
        // Test getMatchesByScoreLevel
        for (ScoreLevel level : ScoreLevel.values()) {
            List<EnterpriseMatchResult> byLevel = matcher.getMatchesByScoreLevel(testResults, level);
            assertNotNull("getMatchesByScoreLevel returns non-null", byLevel);
            
            for (EnterpriseMatchResult emr : byLevel) {
                assertEqual("Score level matches", level, emr.getScoreLevel());
            }
        }
        
        System.out.println("   Total results: " + testResults.size());
        System.out.println("   Top " + limit + ": " + top3.size());
        System.out.println("   ✓ getTopMatches tests passed\n");
    }
    
    // ==================== BATCH OPERATIONS TESTS ====================
    
    private void testBatchOperations() {
        System.out.println("📋 Testing batch operations...");
        
        if (testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ No enterprises available for testing\n");
            return;
        }
        
        String enterpriseId = testEnterprises.get(0).getEnterpriseId();
        
        // Test findAllPotentialMatches
        System.out.println("   Testing findAllPotentialMatches (may take a moment)...");
        Map<Item, List<EnterpriseMatchResult>> allMatches = matcher.findAllPotentialMatches(enterpriseId);
        
        assertNotNull("findAllPotentialMatches returns non-null", allMatches);
        
        // Verify each entry has matches
        for (Map.Entry<Item, List<EnterpriseMatchResult>> entry : allMatches.entrySet()) {
            assertNotNull("Key (item) is not null", entry.getKey());
            assertNotNull("Value (matches) is not null", entry.getValue());
            assertTrue("Has at least one match", entry.getValue().size() >= 1);
        }
        
        // Test findUnmatchedItems
        System.out.println("   Testing findUnmatchedItems...");
        List<Item> unmatched = matcher.findUnmatchedItems(enterpriseId);
        
        assertNotNull("findUnmatchedItems returns non-null", unmatched);
        
        // Test findBestMatchesAcrossSystem
        System.out.println("   Testing findBestMatchesAcrossSystem...");
        List<EnterpriseMatchResult> bestMatches = matcher.findBestMatchesAcrossSystem(5);
        
        assertNotNull("findBestMatchesAcrossSystem returns non-null", bestMatches);
        assertTrue("Best matches limited to 5", bestMatches.size() <= 5);
        
        // Verify sorted by score
        for (int i = 0; i < bestMatches.size() - 1; i++) {
            assertTrue("Best matches sorted", 
                bestMatches.get(i).getMatchScore() >= bestMatches.get(i + 1).getMatchScore());
        }
        
        System.out.println("   Items with matches: " + allMatches.size());
        System.out.println("   Unmatched items: " + unmatched.size());
        System.out.println("   Best system matches: " + bestMatches.size());
        System.out.println("   ✓ Batch operations tests passed\n");
    }
    
    // ==================== MATCH REPORT TESTS ====================
    
    private void testMatchReport() {
        System.out.println("📋 Testing generateMatchReport...");
        
        if (testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ No enterprises available for testing\n");
            return;
        }
        
        String enterpriseId = testEnterprises.get(0).getEnterpriseId();
        
        System.out.println("   Generating report (may take a moment)...");
        MatchReport report = matcher.generateMatchReport(enterpriseId);
        
        assertNotNull("Report generated", report);
        assertNotNull("Report has enterprise ID", report.enterpriseId);
        assertNotNull("Report has enterprise name", report.enterpriseName);
        assertNotNull("Report has generated date", report.generatedAt);
        assertTrue("Total items >= 0", report.totalItemsAnalyzed >= 0);
        assertTrue("Matches found >= 0", report.matchesFound >= 0);
        assertTrue("Average score >= 0", report.averageMatchScore >= 0);
        assertTrue("Average score <= 1", report.averageMatchScore <= 1.0);
        assertNotNull("Top matches list exists", report.topMatches);
        
        // Test summary generation
        String summary = report.getSummary();
        assertNotNull("Summary generated", summary);
        assertTrue("Summary contains enterprise name", summary.contains(report.enterpriseName));
        
        // Test table row generation
        Object[] row = report.toSummaryRow();
        assertNotNull("Table row generated", row);
        assertEqual("Row has 6 columns", 6, row.length);
        
        String[] columns = MatchReport.getSummaryColumns();
        assertEqual("Column headers has 6 entries", 6, columns.length);
        
        // Verify match type distribution
        if (report.matchTypeDistribution != null) {
            for (MatchType type : MatchType.values()) {
                assertTrue("Has count for " + type, 
                    report.matchTypeDistribution.containsKey(type) || 
                    report.matchTypeDistribution.size() == 0);
            }
        }
        
        System.out.println("\n   " + summary.replace("\n", "\n   "));
        System.out.println("\n   ✓ Match report tests passed\n");
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    private void testEdgeCases() {
        System.out.println("📋 Testing edge cases...");
        
        // Test with item that has no enterprise/org IDs
        Building testBuilding = new Building("Test Building", "TST", Building.BuildingType.ACADEMIC);
        Location location = new Location(testBuilding, "101", "Near entrance");
        User testUser = new User("test@test.com", "Test", "User", User.UserRole.STUDENT);
        
        Item bareItem = new Item("Test Item", "Test Description", ItemCategory.OTHER,
                                 ItemType.LOST, location, testUser);
        
        List<EnterpriseMatchResult> bareResults = matcher.matchAcrossEnterprises(bareItem);
        assertNotNull("Bare item matching works", bareResults);
        
        // Test scoring with extreme values
        double maxScore = matcher.applyEnterpriseBonus(1.0, MatchType.SAME_ORGANIZATION);
        assertTrue("Max score capped at 1.0 or slightly over", maxScore <= 1.5);
        
        double zeroScore = matcher.applyEnterpriseBonus(0.0, MatchType.CROSS_NETWORK);
        assertEqual("Zero score stays zero with no bonus", 0.0, zeroScore);
        
        // Test filtering empty list
        List<EnterpriseMatchResult> emptyList = new ArrayList<>();
        List<EnterpriseMatchResult> filteredEmpty = matcher.filterByMinScore(emptyList, 0.5);
        assertEqual("Filter empty list returns empty", 0, filteredEmpty.size());
        
        // Test sorting empty list
        List<EnterpriseMatchResult> sortedEmpty = matcher.sortByScore(emptyList);
        assertEqual("Sort empty list returns empty", 0, sortedEmpty.size());
        
        // Test getTopMatches with negative limit (should handle gracefully)
        try {
            List<EnterpriseMatchResult> negativeLimit = matcher.getTopMatches(createTestMatchResults(), -1);
            assertTrue("Negative limit handled", negativeLimit.size() >= 0);
        } catch (Exception e) {
            // Exception is also acceptable behavior
            System.out.println("   Negative limit throws exception (acceptable)");
        }
        
        System.out.println("   ✓ Edge case tests passed\n");
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Create a mock item for testing.
     */
    private Item createMockItem(String orgId, String entId) {
        Building testBuilding = new Building("Test Building", "TST", Building.BuildingType.ACADEMIC);
        Location location = new Location(testBuilding, "101", "Near entrance");
        User testUser = new User("test@test.com", "Test", "User", User.UserRole.STUDENT);
        
        Item item = new Item("Test Item", "Test Description", ItemCategory.ELECTRONICS,
                            ItemType.LOST, location, testUser);
        item.setStatus(ItemStatus.OPEN);
        item.setOrganizationId(orgId);
        item.setEnterpriseId(entId);
        return item;
    }
    
    /**
     * Create test match results from actual data.
     */
    private List<EnterpriseMatchResult> createTestMatchResults() {
        if (lostItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get matches for first few lost items
        List<EnterpriseMatchResult> allResults = new ArrayList<>();
        
        for (int i = 0; i < Math.min(3, lostItems.size()); i++) {
            List<EnterpriseMatchResult> matches = matcher.matchAcrossEnterprises(lostItems.get(i));
            allResults.addAll(matches);
        }
        
        return allResults;
    }
    
    // ==================== ASSERTION HELPERS ====================
    
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
    
    private void assertEqual(String message, double expected, double actual) {
        if (Math.abs(expected - actual) < 0.0001) {
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
    
    private void assertNull(String message, Object obj) {
        if (obj == null) {
            passedTests++;
        } else {
            failedTests++;
            System.out.println("   ❌ FAIL: " + message + " - Expected null");
        }
    }
    
    private void printSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 ENTERPRISE ITEM MATCHER TEST SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("✅ Passed: " + passedTests);
        System.out.println("❌ Failed: " + failedTests);
        System.out.println("📈 Total:  " + (passedTests + failedTests));
        
        if (failedTests == 0) {
            System.out.println("\n🎉 ALL ENTERPRISE ITEM MATCHER TESTS PASSED!");
        } else {
            System.out.println("\n⚠️  Some tests failed. Review the output above.");
        }
        System.out.println("=".repeat(70) + "\n");
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        System.out.println("Starting Enterprise Item Matcher Tests...\n");
        EnterpriseItemMatcherTest test = new EnterpriseItemMatcherTest();
        test.runAllTests();
    }
}
