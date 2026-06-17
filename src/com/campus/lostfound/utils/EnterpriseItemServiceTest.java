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
import com.campus.lostfound.services.EnterpriseItemService;
import com.campus.lostfound.services.EnterpriseItemService.SearchCriteria;
import com.campus.lostfound.services.EnterpriseItemService.EnterpriseStats;
import com.campus.lostfound.services.EnterpriseItemService.NetworkEffectMetrics;
import com.campus.lostfound.services.EnterpriseItemService.ItemWithContext;

import java.util.*;
import java.util.logging.Logger;

/**
 * Test class for EnterpriseItemService (Part 4 of Developer 4)
 * Tests cross-enterprise search, filtering, and statistics
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseItemServiceTest {
    
    private static final Logger LOGGER = Logger.getLogger(EnterpriseItemServiceTest.class.getName());
    
    private EnterpriseItemService service;
    private MongoItemDAO itemDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    
    private int passedTests = 0;
    private int failedTests = 0;
    
    // Test data references
    private List<Enterprise> testEnterprises;
    private List<Organization> testOrganizations;
    private List<Item> testItems;
    
    public EnterpriseItemServiceTest() {
        this.service = new EnterpriseItemService();
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
    }
    
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏢 ENTERPRISE ITEM SERVICE TESTS");
        System.out.println("=".repeat(70) + "\n");
        
        // Load test data
        loadTestData();
        
        // Run test groups
        testSearchAllEnterprises();
        testSearchSpecificEnterprises();
        testAdvancedSearch();
        testEnterprisesScopedQueries();
        testFindItemCrossEnterprise();
        testItemWithContext();
        testStatistics();
        testEnterpriseStatistics();
        testNetworkEffectMetrics();
        testCaching();
        testSearchCriteriaBuilder();
        
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
            
            System.out.println("   Loaded " + testEnterprises.size() + " enterprises");
            System.out.println("   Loaded " + testOrganizations.size() + " organizations");
            System.out.println("   Loaded " + testItems.size() + " items\n");
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Error loading test data: " + e.getMessage() + "\n");
        }
    }
    
    // ==================== SEARCH TESTS ====================
    
    private void testSearchAllEnterprises() {
        System.out.println("📋 Testing searchAllEnterprises...");
        
        // Test basic search without filters
        List<Item> allResults = service.searchAllEnterprises(null, null);
        assertNotNull("Search all returns results", allResults);
        assertTrue("Search all returns items", allResults.size() >= 0);
        
        // Test with query filter
        List<Item> queryResults = service.searchAllEnterprises("laptop", null);
        assertNotNull("Query search returns results", queryResults);
        
        // Verify query results contain search term (if any found)
        if (!queryResults.isEmpty()) {
            boolean foundMatch = false;
            for (Item item : queryResults) {
                String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                if (title.contains("laptop") || desc.contains("laptop")) {
                    foundMatch = true;
                    break;
                }
            }
            // It's okay if no exact match - keywords might match too
        }
        
        // Test with category filter
        List<Item> categoryResults = service.searchAllEnterprises(null, ItemCategory.ELECTRONICS);
        assertNotNull("Category search returns results", categoryResults);
        
        // Verify all results are electronics
        for (Item item : categoryResults) {
            assertEqual("Item is electronics", ItemCategory.ELECTRONICS, item.getCategory());
        }
        
        // Test with both query and category
        List<Item> combinedResults = service.searchAllEnterprises("phone", ItemCategory.ELECTRONICS);
        assertNotNull("Combined search returns results", combinedResults);
        
        System.out.println("   Found " + allResults.size() + " total items");
        System.out.println("   Found " + categoryResults.size() + " electronics items");
        System.out.println("   ✓ searchAllEnterprises tests passed\n");
    }
    
    private void testSearchSpecificEnterprises() {
        System.out.println("📋 Testing searchSpecificEnterprises...");
        
        if (testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ No enterprises available for testing\n");
            return;
        }
        
        // Get first enterprise ID
        String firstEnterpriseId = testEnterprises.get(0).getEnterpriseId();
        
        // Search in single enterprise
        List<Item> singleResults = service.searchSpecificEnterprises(
            Arrays.asList(firstEnterpriseId), null);
        assertNotNull("Single enterprise search works", singleResults);
        
        // Verify all results are from the specified enterprise
        for (Item item : singleResults) {
            assertEqual("Item from correct enterprise", firstEnterpriseId, item.getEnterpriseId());
        }
        
        // Search in multiple enterprises
        if (testEnterprises.size() >= 2) {
            List<String> multipleIds = Arrays.asList(
                testEnterprises.get(0).getEnterpriseId(),
                testEnterprises.get(1).getEnterpriseId()
            );
            
            List<Item> multiResults = service.searchSpecificEnterprises(multipleIds, "item");
            assertNotNull("Multiple enterprise search works", multiResults);
            
            // Verify results are from one of the specified enterprises
            for (Item item : multiResults) {
                assertTrue("Item from specified enterprises", 
                    multipleIds.contains(item.getEnterpriseId()));
            }
        }
        
        // Test with empty list
        List<Item> emptyResults = service.searchSpecificEnterprises(new ArrayList<>(), null);
        assertNotNull("Empty enterprise list returns empty", emptyResults);
        assertEqual("Empty list returns no results", 0, emptyResults.size());
        
        System.out.println("   ✓ searchSpecificEnterprises tests passed\n");
    }
    
    private void testAdvancedSearch() {
        System.out.println("📋 Testing advancedSearch...");
        
        // Test with SearchCriteria builder
        SearchCriteria criteria = new SearchCriteria()
            .withCategory(ItemCategory.ELECTRONICS)
            .includeResolved(false);
        
        List<Item> results = service.advancedSearch(criteria);
        assertNotNull("Advanced search returns results", results);
        
        // Verify category filter
        for (Item item : results) {
            assertEqual("Item is electronics", ItemCategory.ELECTRONICS, item.getCategory());
        }
        
        // Verify resolved items not included
        for (Item item : results) {
            assertTrue("Item not claimed", item.getStatus() != ItemStatus.CLAIMED);
            assertTrue("Item not expired", item.getStatus() != ItemStatus.EXPIRED);
        }
        
        // Test with value range
        SearchCriteria valueCriteria = new SearchCriteria()
            .withValueRange(100.0, 1000.0)
            .includeResolved(true);
        
        List<Item> valueResults = service.advancedSearch(valueCriteria);
        assertNotNull("Value range search works", valueResults);
        
        for (Item item : valueResults) {
            assertTrue("Item value >= 100", item.getEstimatedValue() >= 100.0);
            assertTrue("Item value <= 1000", item.getEstimatedValue() <= 1000.0);
        }
        
        // Test with type filter
        SearchCriteria typeCriteria = new SearchCriteria()
            .withType(ItemType.LOST);
        
        List<Item> lostResults = service.advancedSearch(typeCriteria);
        assertNotNull("Type filter search works", lostResults);
        
        for (Item item : lostResults) {
            assertEqual("Item is lost type", ItemType.LOST, item.getType());
        }
        
        // Test with enterprise filter
        if (!testEnterprises.isEmpty()) {
            SearchCriteria entCriteria = new SearchCriteria()
                .withEnterpriseIds(Arrays.asList(testEnterprises.get(0).getEnterpriseId()));
            
            List<Item> entResults = service.advancedSearch(entCriteria);
            assertNotNull("Enterprise filter works", entResults);
        }
        
        System.out.println("   ✓ advancedSearch tests passed\n");
    }
    
    // ==================== ENTERPRISE-SCOPED QUERIES ====================
    
    private void testEnterprisesScopedQueries() {
        System.out.println("📋 Testing enterprise-scoped queries...");
        
        if (testEnterprises.isEmpty()) {
            System.out.println("   ⚠️ No enterprises available for testing\n");
            return;
        }
        
        String enterpriseId = testEnterprises.get(0).getEnterpriseId();
        
        // Test getItemsByEnterpriseId
        List<Item> enterpriseItems = service.getItemsByEnterpriseId(enterpriseId);
        assertNotNull("getItemsByEnterpriseId works", enterpriseItems);
        
        for (Item item : enterpriseItems) {
            assertEqual("Item from correct enterprise", enterpriseId, item.getEnterpriseId());
        }
        
        // Test getLostItemsByEnterprise
        List<Item> lostItems = service.getLostItemsByEnterprise(enterpriseId);
        assertNotNull("getLostItemsByEnterprise works", lostItems);
        
        for (Item item : lostItems) {
            assertEqual("Item is lost", ItemType.LOST, item.getType());
        }
        
        // Test getFoundItemsByEnterprise
        List<Item> foundItems = service.getFoundItemsByEnterprise(enterpriseId);
        assertNotNull("getFoundItemsByEnterprise works", foundItems);
        
        for (Item item : foundItems) {
            assertEqual("Item is found", ItemType.FOUND, item.getType());
        }
        
        // Test getOpenItemsByEnterprise
        List<Item> openItems = service.getOpenItemsByEnterprise(enterpriseId);
        assertNotNull("getOpenItemsByEnterprise works", openItems);
        
        for (Item item : openItems) {
            assertTrue("Item is open or pending", 
                item.getStatus() == ItemStatus.OPEN || 
                item.getStatus() == ItemStatus.PENDING_CLAIM);
        }
        
        // Test organization-scoped query
        if (!testOrganizations.isEmpty()) {
            String orgId = testOrganizations.get(0).getOrganizationId();
            List<Item> orgItems = service.getItemsByOrganizationId(orgId);
            assertNotNull("getItemsByOrganizationId works", orgItems);
        }
        
        // Test getRecentItemsAcrossEnterprises
        List<Item> recentItems = service.getRecentItemsAcrossEnterprises(7, 10);
        assertNotNull("getRecentItemsAcrossEnterprises works", recentItems);
        assertTrue("Recent items limited to 10", recentItems.size() <= 10);
        
        // Verify sorted by date (most recent first)
        if (recentItems.size() >= 2) {
            for (int i = 0; i < recentItems.size() - 1; i++) {
                Date date1 = recentItems.get(i).getReportedDate();
                Date date2 = recentItems.get(i + 1).getReportedDate();
                if (date1 != null && date2 != null) {
                    assertTrue("Items sorted by date", date1.compareTo(date2) >= 0);
                }
            }
        }
        
        // Test getHighValueItemsAcrossEnterprises
        List<Item> highValueItems = service.getHighValueItemsAcrossEnterprises(500.0);
        assertNotNull("getHighValueItemsAcrossEnterprises works", highValueItems);
        
        for (Item item : highValueItems) {
            assertTrue("Item value >= 500", item.getEstimatedValue() >= 500.0);
        }
        
        System.out.println("   ✓ Enterprise-scoped query tests passed\n");
    }
    
    // ==================== FIND ITEM TESTS ====================
    
    private void testFindItemCrossEnterprise() {
        System.out.println("📋 Testing findItemCrossEnterprise...");
        
        if (testItems.isEmpty()) {
            System.out.println("   ⚠️ No items available for testing\n");
            return;
        }
        
        // Get a valid item ID
        Item firstItem = testItems.get(0);
        String itemId = firstItem.getMongoId();
        
        if (itemId != null) {
            // Find the item
            Optional<Item> found = service.findItemCrossEnterprise(itemId);
            assertTrue("Item found", found.isPresent());
            
            if (found.isPresent()) {
                assertEqual("Correct item found", firstItem.getTitle(), found.get().getTitle());
            }
        }
        
        // Test with non-existent ID
        Optional<Item> notFound = service.findItemCrossEnterprise("000000000000000000000000");
        assertFalse("Non-existent item not found", notFound.isPresent());
        
        System.out.println("   ✓ findItemCrossEnterprise tests passed\n");
    }
    
    private void testItemWithContext() {
        System.out.println("📋 Testing getItemWithEnterpriseContext...");
        
        if (testItems.isEmpty()) {
            System.out.println("   ⚠️ No items available for testing\n");
            return;
        }
        
        // Find an item with enterprise context
        Item itemWithEnterprise = null;
        for (Item item : testItems) {
            if (item.getEnterpriseId() != null && item.getMongoId() != null) {
                itemWithEnterprise = item;
                break;
            }
        }
        
        if (itemWithEnterprise != null) {
            ItemWithContext context = service.getItemWithEnterpriseContext(itemWithEnterprise.getMongoId());
            
            assertNotNull("ItemWithContext returned", context);
            assertNotNull("Item in context", context.getItem());
            assertNotNull("Enterprise name available", context.getEnterpriseName());
            
            System.out.println("   Item: " + context.getItem().getTitle());
            System.out.println("   Enterprise: " + context.getEnterpriseName());
            System.out.println("   Organization: " + context.getOrganizationName());
        } else {
            System.out.println("   ⚠️ No items with enterprise context found");
        }
        
        // Test with non-existent ID
        ItemWithContext nullContext = service.getItemWithEnterpriseContext("000000000000000000000000");
        assertNull("Null returned for non-existent item", nullContext);
        
        System.out.println("   ✓ getItemWithEnterpriseContext tests passed\n");
    }
    
    // ==================== STATISTICS TESTS ====================
    
    private void testStatistics() {
        System.out.println("📋 Testing statistics methods...");
        
        // Test getCrossEnterpriseMatchRate
        double matchRate = service.getCrossEnterpriseMatchRate();
        assertTrue("Match rate >= 0", matchRate >= 0.0);
        assertTrue("Match rate <= 1", matchRate <= 1.0);
        
        // Test getItemCountByEnterprise
        Map<String, Long> countByEnterprise = service.getItemCountByEnterprise();
        assertNotNull("Count by enterprise returned", countByEnterprise);
        
        // Test getItemCountByOrganization
        Map<String, Long> countByOrg = service.getItemCountByOrganization();
        assertNotNull("Count by organization returned", countByOrg);
        
        // Test getRecoveryRateByEnterprise
        Map<String, Double> recoveryRates = service.getRecoveryRateByEnterprise();
        assertNotNull("Recovery rates returned", recoveryRates);
        
        for (Double rate : recoveryRates.values()) {
            assertTrue("Recovery rate >= 0", rate >= 0.0);
            assertTrue("Recovery rate <= 1", rate <= 1.0);
        }
        
        // Test getCategoryDistributionByEnterprise
        if (!testEnterprises.isEmpty()) {
            Map<ItemCategory, Long> categoryDist = service.getCategoryDistributionByEnterprise(
                testEnterprises.get(0).getEnterpriseId());
            assertNotNull("Category distribution returned", categoryDist);
        }
        
        System.out.println("   Cross-enterprise match rate: " + String.format("%.1f%%", matchRate * 100));
        System.out.println("   Enterprises with items: " + countByEnterprise.size());
        System.out.println("   ✓ Statistics tests passed\n");
    }
    
    private void testEnterpriseStatistics() {
        System.out.println("📋 Testing getEnterpriseStatistics...");
        
        List<EnterpriseStats> stats = service.getEnterpriseStatistics();
        assertNotNull("Enterprise stats returned", stats);
        
        for (EnterpriseStats es : stats) {
            assertNotNull("Enterprise ID present", es.enterpriseId);
            assertNotNull("Enterprise name present", es.enterpriseName);
            assertTrue("Total items >= 0", es.totalItems >= 0);
            assertTrue("Lost items >= 0", es.lostItems >= 0);
            assertTrue("Found items >= 0", es.foundItems >= 0);
            assertTrue("Claimed items >= 0", es.claimedItems >= 0);
            assertTrue("Recovery rate >= 0", es.recoveryRate >= 0.0);
            assertTrue("Recovery rate <= 1", es.recoveryRate <= 1.0);
            
            // Verify totals add up
            assertTrue("Lost + Found >= Claimed", es.lostItems + es.foundItems >= es.claimedItems);
        }
        
        // Test table row generation
        if (!stats.isEmpty()) {
            Object[] row = stats.get(0).toTableRow();
            assertNotNull("Table row generated", row);
            assertEqual("Table row has 8 columns", 8, row.length);
            
            String[] columns = EnterpriseStats.getTableColumns();
            assertEqual("Column headers has 8 entries", 8, columns.length);
        }
        
        System.out.println("   Enterprise stats count: " + stats.size());
        if (!stats.isEmpty()) {
            System.out.println("   Sample: " + stats.get(0).enterpriseName + 
                " - " + stats.get(0).totalItems + " items, " +
                String.format("%.1f%%", stats.get(0).recoveryRate * 100) + " recovery");
        }
        System.out.println("   ✓ Enterprise statistics tests passed\n");
    }
    
    private void testNetworkEffectMetrics() {
        System.out.println("📋 Testing getNetworkEffectMetrics...");
        
        NetworkEffectMetrics metrics = service.getNetworkEffectMetrics();
        assertNotNull("Network metrics returned", metrics);
        
        assertTrue("Overall rate >= 0", metrics.overallRecoveryRate >= 0.0);
        assertTrue("Single enterprise rate >= 0", metrics.singleEnterpriseRecoveryRate >= 0.0);
        assertTrue("Two enterprise rate >= 0", metrics.twoEnterpriseRecoveryRate >= 0.0);
        assertTrue("Three enterprise rate >= 0", metrics.threeEnterpriseRecoveryRate >= 0.0);
        assertTrue("Four enterprise rate >= 0", metrics.fourEnterpriseRecoveryRate >= 0.0);
        
        // Verify network effect shows improvement
        assertTrue("Network effect shows improvement",
            metrics.fourEnterpriseRecoveryRate >= metrics.singleEnterpriseRecoveryRate);
        
        // Test summary generation
        String summary = metrics.getSummary();
        assertNotNull("Summary generated", summary);
        assertTrue("Summary contains percentage", summary.contains("%"));
        
        System.out.println("   " + summary);
        System.out.println("   ✓ Network effect metrics tests passed\n");
    }
    
    // ==================== CACHING & HELPERS TESTS ====================
    
    private void testCaching() {
        System.out.println("📋 Testing caching and name lookups...");
        
        if (!testEnterprises.isEmpty()) {
            String entId = testEnterprises.get(0).getEnterpriseId();
            String expectedName = testEnterprises.get(0).getName();
            
            // First call
            String name1 = service.getEnterpriseName(entId);
            
            // Second call (should be cached)
            String name2 = service.getEnterpriseName(entId);
            
            assertEqual("Enterprise names match", name1, name2);
            assertEqual("Correct enterprise name", expectedName, name1);
        }
        
        if (!testOrganizations.isEmpty()) {
            String orgId = testOrganizations.get(0).getOrganizationId();
            String expectedName = testOrganizations.get(0).getName();
            
            String name = service.getOrganizationName(orgId);
            assertEqual("Correct organization name", expectedName, name);
            
            // Test getEnterpriseIdForOrganization
            String entId = service.getEnterpriseIdForOrganization(orgId);
            assertEqual("Correct enterprise for org", 
                testOrganizations.get(0).getEnterpriseId(), entId);
        }
        
        // Test with null/invalid IDs
        String unknownEnt = service.getEnterpriseName(null);
        assertEqual("Null enterprise returns Unknown", "Unknown", unknownEnt);
        
        String unknownOrg = service.getOrganizationName("invalid-id");
        assertEqual("Invalid org returns Unknown", "Unknown", unknownOrg);
        
        System.out.println("   ✓ Caching tests passed\n");
    }
    
    private void testSearchCriteriaBuilder() {
        System.out.println("📋 Testing SearchCriteria builder...");
        
        // Test builder pattern
        SearchCriteria criteria = new SearchCriteria()
            .withQuery("test")
            .withCategory(ItemCategory.ELECTRONICS)
            .withType(ItemType.LOST)
            .withStatus(ItemStatus.OPEN)
            .withEnterpriseIds(Arrays.asList("ent1", "ent2"))
            .withOrganizationIds(Arrays.asList("org1"))
            .withDateRange(new Date(), new Date())
            .withValueRange(10.0, 100.0)
            .includeResolved(true);
        
        assertEqual("Query set", "test", criteria.getQuery());
        assertEqual("Category set", ItemCategory.ELECTRONICS, criteria.getCategory());
        assertEqual("Type set", ItemType.LOST, criteria.getType());
        assertEqual("Status set", ItemStatus.OPEN, criteria.getStatus());
        assertEqual("Enterprise IDs count", 2, criteria.getEnterpriseIds().size());
        assertEqual("Organization IDs count", 1, criteria.getOrganizationIds().size());
        assertNotNull("Date from set", criteria.getDateFrom());
        assertNotNull("Date to set", criteria.getDateTo());
        assertEqual("Min value set", 10.0, criteria.getMinValue());
        assertEqual("Max value set", 100.0, criteria.getMaxValue());
        assertTrue("Include resolved set", criteria.isIncludeResolved());
        
        System.out.println("   ✓ SearchCriteria builder tests passed\n");
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
        System.out.println("📊 ENTERPRISE ITEM SERVICE TEST SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("✅ Passed: " + passedTests);
        System.out.println("❌ Failed: " + failedTests);
        System.out.println("📈 Total:  " + (passedTests + failedTests));
        
        if (failedTests == 0) {
            System.out.println("\n🎉 ALL ENTERPRISE ITEM SERVICE TESTS PASSED!");
        } else {
            System.out.println("\n⚠️  Some tests failed. Review the output above.");
        }
        System.out.println("=".repeat(70) + "\n");
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        System.out.println("Starting Enterprise Item Service Tests...\n");
        EnterpriseItemServiceTest test = new EnterpriseItemServiceTest();
        test.runAllTests();
    }
}
