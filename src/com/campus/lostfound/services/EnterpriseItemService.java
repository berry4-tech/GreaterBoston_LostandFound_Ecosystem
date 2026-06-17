package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.Item.ItemCategory;
import com.campus.lostfound.models.Item.ItemStatus;
import com.campus.lostfound.models.Item.ItemType;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.models.Organization;
import com.campus.lostfound.models.User;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Service for cross-enterprise item search and management.
 * 
 * Provides comprehensive functionality for:
 * - Searching items across all enterprises
 * - Filtering by enterprise, organization, category, status
 * - Enriching items with enterprise context
 * - Statistics and analytics for cross-enterprise operations
 * 
 * This service is the central point for all cross-enterprise item operations.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseItemService {
    
    private static final Logger LOGGER = Logger.getLogger(EnterpriseItemService.class.getName());
    
    // ==================== DEPENDENCIES ====================
    
    private final MongoItemDAO itemDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final MongoUserDAO userDAO;
    
    // Cache for enterprise/org names (refreshed periodically)
    private Map<String, String> enterpriseNameCache = new HashMap<>();
    private Map<String, String> organizationNameCache = new HashMap<>();
    private Map<String, String> orgToEnterpriseCache = new HashMap<>();
    private long cacheRefreshTime = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutes
    
    // ==================== CONSTRUCTORS ====================
    
    public EnterpriseItemService() {
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.userDAO = new MongoUserDAO();
        refreshCacheIfNeeded();
    }
    
    /**
     * Constructor for testing with mock DAOs
     */
    public EnterpriseItemService(MongoItemDAO itemDAO, MongoEnterpriseDAO enterpriseDAO,
                                  MongoOrganizationDAO organizationDAO, MongoUserDAO userDAO) {
        this.itemDAO = itemDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
        this.userDAO = userDAO;
        refreshCacheIfNeeded();
    }
    
    // ==================== CROSS-ENTERPRISE SEARCH ====================
    
    /**
     * Search for items across ALL enterprises
     * 
     * @param query Search query (can be null)
     * @param category Category filter (can be null)
     * @return List of items matching criteria from all enterprises
     */
    public List<Item> searchAllEnterprises(String query, ItemCategory category) {
        try {
            List<Item> allItems = itemDAO.findAll();
            
            return filterAndEnrichItems(allItems, query, category, null, null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching all enterprises", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Search for items in specific enterprises only
     * 
     * @param enterpriseIds List of enterprise IDs to search
     * @param query Search query (can be null)
     * @return List of items from specified enterprises
     */
    public List<Item> searchSpecificEnterprises(List<String> enterpriseIds, String query) {
        try {
            List<Item> allItems = itemDAO.findAll();
            
            // Filter to only specified enterprises
            List<Item> filtered = allItems.stream()
                .filter(item -> enterpriseIds.contains(item.getEnterpriseId()))
                .collect(Collectors.toList());
            
            return filterAndEnrichItems(filtered, query, null, null, null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching specific enterprises", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Search for items within a specific network
     * 
     * @param networkId Network ID
     * @param query Search query (can be null)
     * @return List of items from enterprises in the network
     */
    public List<Item> searchByNetwork(String networkId, String query) {
        try {
            // Get all enterprises in the network
            List<Enterprise> networkEnterprises = enterpriseDAO.findByNetworkId(networkId);
            List<String> enterpriseIds = networkEnterprises.stream()
                .map(Enterprise::getEnterpriseId)
                .collect(Collectors.toList());
            
            return searchSpecificEnterprises(enterpriseIds, query);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching by network", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Advanced search with multiple criteria
     * 
     * @param criteria Search criteria object
     * @return List of matching items
     */
    public List<Item> advancedSearch(SearchCriteria criteria) {
        try {
            List<Item> allItems = itemDAO.findAll();
            
            // Apply enterprise filter
            if (criteria.getEnterpriseIds() != null && !criteria.getEnterpriseIds().isEmpty()) {
                allItems = allItems.stream()
                    .filter(item -> criteria.getEnterpriseIds().contains(item.getEnterpriseId()))
                    .collect(Collectors.toList());
            }
            
            // Apply organization filter
            if (criteria.getOrganizationIds() != null && !criteria.getOrganizationIds().isEmpty()) {
                allItems = allItems.stream()
                    .filter(item -> criteria.getOrganizationIds().contains(item.getOrganizationId()))
                    .collect(Collectors.toList());
            }
            
            // Apply other filters
            return filterAndEnrichItems(
                allItems,
                criteria.getQuery(),
                criteria.getCategory(),
                criteria.getType(),
                criteria.getStatus()
            ).stream()
                // Date range filter
                .filter(item -> {
                    if (criteria.getDateFrom() != null && item.getReportedDate() != null) {
                        if (item.getReportedDate().before(criteria.getDateFrom())) return false;
                    }
                    if (criteria.getDateTo() != null && item.getReportedDate() != null) {
                        if (item.getReportedDate().after(criteria.getDateTo())) return false;
                    }
                    return true;
                })
                // Value range filter
                .filter(item -> {
                    if (criteria.getMinValue() != null && item.getEstimatedValue() < criteria.getMinValue()) {
                        return false;
                    }
                    if (criteria.getMaxValue() != null && item.getEstimatedValue() > criteria.getMaxValue()) {
                        return false;
                    }
                    return true;
                })
                // Include resolved filter
                .filter(item -> {
                    if (!criteria.isIncludeResolved()) {
                        return item.getStatus() != ItemStatus.CLAIMED && 
                               item.getStatus() != ItemStatus.EXPIRED;
                    }
                    return true;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in advanced search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find a specific item across enterprises
     * 
     * @param itemId Item ID (MongoDB ObjectId or visible ID)
     * @return Optional containing the item if found
     */
    public Optional<Item> findItemCrossEnterprise(String itemId) {
        try {
            Optional<Item> itemOpt = itemDAO.findById(itemId);
            
            if (itemOpt.isPresent()) {
                return Optional.of(enrichItemWithEnterpriseInfo(itemOpt.get()));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding item cross-enterprise: " + itemId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get item with full enterprise context
     */
    public ItemWithContext getItemWithEnterpriseContext(String itemId) {
        Optional<Item> itemOpt = findItemCrossEnterprise(itemId);
        
        if (itemOpt.isEmpty()) {
            return null;
        }
        
        Item item = itemOpt.get();
        ItemWithContext context = new ItemWithContext(item);
        
        // Add enterprise info
        if (item.getEnterpriseId() != null) {
            enterpriseDAO.findById(item.getEnterpriseId())
                .ifPresent(context::setEnterprise);
        }
        
        // Add organization info
        if (item.getOrganizationId() != null) {
            organizationDAO.findById(item.getOrganizationId())
                .ifPresent(context::setOrganization);
        }
        
        return context;
    }
    
    // ==================== ENTERPRISE-SCOPED QUERIES ====================
    
    /**
     * Get all items for a specific enterprise
     */
    public List<Item> getItemsByEnterpriseId(String enterpriseId) {
        try {
            return itemDAO.findAll().stream()
                .filter(item -> enterpriseId.equals(item.getEnterpriseId()))
                .map(this::enrichItemWithEnterpriseInfo)
                .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting items by enterprise", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all items for a specific organization
     */
    public List<Item> getItemsByOrganizationId(String orgId) {
        try {
            return itemDAO.findAll().stream()
                .filter(item -> orgId.equals(item.getOrganizationId()))
                .map(this::enrichItemWithEnterpriseInfo)
                .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting items by organization", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get lost items for a specific enterprise
     */
    public List<Item> getLostItemsByEnterprise(String enterpriseId) {
        return getItemsByEnterpriseId(enterpriseId).stream()
            .filter(item -> item.getType() == ItemType.LOST)
            .collect(Collectors.toList());
    }
    
    /**
     * Get found items for a specific enterprise
     */
    public List<Item> getFoundItemsByEnterprise(String enterpriseId) {
        return getItemsByEnterpriseId(enterpriseId).stream()
            .filter(item -> item.getType() == ItemType.FOUND)
            .collect(Collectors.toList());
    }
    
    /**
     * Get open (unresolved) items for an enterprise
     */
    public List<Item> getOpenItemsByEnterprise(String enterpriseId) {
        return getItemsByEnterpriseId(enterpriseId).stream()
            .filter(item -> item.getStatus() == ItemStatus.OPEN || 
                           item.getStatus() == ItemStatus.PENDING_CLAIM)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent items across all enterprises
     * 
     * @param days Number of days to look back
     * @param limit Maximum number of items to return
     * @return List of recent items
     */
    public List<Item> getRecentItemsAcrossEnterprises(int days, int limit) {
        try {
            Date cutoffDate = new Date(System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000));
            
            return itemDAO.findAll().stream()
                .filter(item -> item.getReportedDate() != null && 
                               item.getReportedDate().after(cutoffDate))
                .map(this::enrichItemWithEnterpriseInfo)
                .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recent items", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get high-value items across enterprises
     */
    public List<Item> getHighValueItemsAcrossEnterprises(double minValue) {
        try {
            return itemDAO.findAll().stream()
                .filter(item -> item.getEstimatedValue() >= minValue)
                .map(this::enrichItemWithEnterpriseInfo)
                .sorted((a, b) -> Double.compare(b.getEstimatedValue(), a.getEstimatedValue()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting high-value items", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Calculate cross-enterprise match rate
     * (Items matched across different enterprises / Total matched items)
     */
    public double getCrossEnterpriseMatchRate() {
        try {
            List<Item> allItems = itemDAO.findAll();
            
            long totalClaimed = allItems.stream()
                .filter(item -> item.getStatus() == ItemStatus.CLAIMED)
                .count();
            
            if (totalClaimed == 0) return 0.0;
            
            // For demo purposes, estimate cross-enterprise matches
            // In production, this would track actual cross-enterprise claims
            long estimatedCrossEnterprise = (long) (totalClaimed * 0.35); // 35% estimate
            
            return (double) estimatedCrossEnterprise / totalClaimed;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating cross-enterprise match rate", e);
            return 0.0;
        }
    }
    
    /**
     * Get item count grouped by enterprise
     */
    public Map<String, Long> getItemCountByEnterprise() {
        try {
            refreshCacheIfNeeded();
            
            Map<String, Long> counts = itemDAO.findAll().stream()
                .filter(item -> item.getEnterpriseId() != null)
                .collect(Collectors.groupingBy(
                    item -> getEnterpriseName(item.getEnterpriseId()),
                    Collectors.counting()
                ));
            
            return counts;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item count by enterprise", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get item count grouped by organization
     */
    public Map<String, Long> getItemCountByOrganization() {
        try {
            refreshCacheIfNeeded();
            
            Map<String, Long> counts = itemDAO.findAll().stream()
                .filter(item -> item.getOrganizationId() != null)
                .collect(Collectors.groupingBy(
                    item -> getOrganizationName(item.getOrganizationId()),
                    Collectors.counting()
                ));
            
            return counts;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item count by organization", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get category distribution for a specific enterprise
     */
    public Map<ItemCategory, Long> getCategoryDistributionByEnterprise(String enterpriseId) {
        try {
            return getItemsByEnterpriseId(enterpriseId).stream()
                .filter(item -> item.getCategory() != null)
                .collect(Collectors.groupingBy(
                    Item::getCategory,
                    Collectors.counting()
                ));
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting category distribution", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get recovery rate by enterprise
     * (Claimed items / Total items)
     */
    public Map<String, Double> getRecoveryRateByEnterprise() {
        try {
            refreshCacheIfNeeded();
            
            Map<String, Double> rates = new HashMap<>();
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            
            for (Enterprise enterprise : enterprises) {
                List<Item> items = getItemsByEnterpriseId(enterprise.getEnterpriseId());
                
                if (items.isEmpty()) {
                    rates.put(enterprise.getName(), 0.0);
                    continue;
                }
                
                long claimed = items.stream()
                    .filter(item -> item.getStatus() == ItemStatus.CLAIMED)
                    .count();
                
                double rate = (double) claimed / items.size();
                rates.put(enterprise.getName(), rate);
            }
            
            return rates;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recovery rate by enterprise", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get comprehensive enterprise statistics
     */
    public List<EnterpriseStats> getEnterpriseStatistics() {
        try {
            List<EnterpriseStats> stats = new ArrayList<>();
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            
            for (Enterprise enterprise : enterprises) {
                List<Item> items = getItemsByEnterpriseId(enterprise.getEnterpriseId());
                
                EnterpriseStats es = new EnterpriseStats();
                es.enterpriseId = enterprise.getEnterpriseId();
                es.enterpriseName = enterprise.getName();
                es.enterpriseType = enterprise.getType().name();
                es.totalItems = items.size();
                es.lostItems = (int) items.stream().filter(i -> i.getType() == ItemType.LOST).count();
                es.foundItems = (int) items.stream().filter(i -> i.getType() == ItemType.FOUND).count();
                es.claimedItems = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
                es.openItems = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.OPEN).count();
                es.recoveryRate = es.totalItems > 0 ? (double) es.claimedItems / es.totalItems : 0.0;
                es.totalValue = items.stream().mapToDouble(Item::getEstimatedValue).sum();
                
                stats.add(es);
            }
            
            // Sort by total items descending
            stats.sort((a, b) -> Integer.compare(b.totalItems, a.totalItems));
            
            return stats;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting enterprise statistics", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get network effect metrics showing improvement with collaboration
     */
    public NetworkEffectMetrics getNetworkEffectMetrics() {
        try {
            NetworkEffectMetrics metrics = new NetworkEffectMetrics();
            
            List<Item> allItems = itemDAO.findAll();
            long totalItems = allItems.size();
            
            if (totalItems == 0) {
                return metrics;
            }
            
            long claimedItems = allItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .count();
            
            // Calculate overall recovery rate
            metrics.overallRecoveryRate = (double) claimedItems / totalItems;
            
            // Simulate network effect metrics for demo
            // In production, these would be calculated from actual data
            metrics.singleEnterpriseRecoveryRate = 0.18;  // 18% baseline
            metrics.twoEnterpriseRecoveryRate = 0.35;     // 35% with 2
            metrics.threeEnterpriseRecoveryRate = 0.52;   // 52% with 3
            metrics.fourEnterpriseRecoveryRate = 0.68;    // 68% with all 4
            
            // Calculate improvement
            metrics.networkEffectImprovement = 
                (metrics.fourEnterpriseRecoveryRate - metrics.singleEnterpriseRecoveryRate) 
                / metrics.singleEnterpriseRecoveryRate;
            
            return metrics;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating network effect metrics", e);
            return new NetworkEffectMetrics();
        }
    }
    
    // ==================== ENTERPRISE CONTEXT HELPERS ====================
    
    /**
     * Enrich an item with enterprise name information
     */
    public Item enrichItemWithEnterpriseInfo(Item item) {
        refreshCacheIfNeeded();
        // Item doesn't have name fields, but we cache for display
        // The name will be retrieved via getEnterpriseName/getOrganizationName
        return item;
    }
    
    /**
     * Get enterprise name by ID (cached)
     */
    public String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null) return "Unknown";
        
        refreshCacheIfNeeded();
        return enterpriseNameCache.getOrDefault(enterpriseId, "Unknown");
    }
    
    /**
     * Get organization name by ID (cached)
     */
    public String getOrganizationName(String orgId) {
        if (orgId == null) return "Unknown";
        
        refreshCacheIfNeeded();
        return organizationNameCache.getOrDefault(orgId, "Unknown");
    }
    
    /**
     * Get enterprise ID for an organization (cached)
     */
    public String getEnterpriseIdForOrganization(String orgId) {
        if (orgId == null) return null;
        
        refreshCacheIfNeeded();
        return orgToEnterpriseCache.get(orgId);
    }
    
    /**
     * Get all enterprises
     */
    public List<Enterprise> getAllEnterprises() {
        return enterpriseDAO.findAll();
    }
    
    /**
     * Get all organizations
     */
    public List<Organization> getAllOrganizations() {
        return organizationDAO.findAll();
    }
    
    /**
     * Get organizations for an enterprise
     */
    public List<Organization> getOrganizationsForEnterprise(String enterpriseId) {
        return organizationDAO.findByEnterpriseId(enterpriseId);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Filter and enrich a list of items
     */
    private List<Item> filterAndEnrichItems(List<Item> items, String query, 
                                             ItemCategory category, ItemType type, ItemStatus status) {
        return items.stream()
            // Query filter (search in title, description, keywords)
            .filter(item -> {
                if (query == null || query.trim().isEmpty()) return true;
                
                String lowerQuery = query.toLowerCase();
                
                if (item.getTitle() != null && 
                    item.getTitle().toLowerCase().contains(lowerQuery)) return true;
                    
                if (item.getDescription() != null && 
                    item.getDescription().toLowerCase().contains(lowerQuery)) return true;
                    
                if (item.getKeywords() != null) {
                    for (String keyword : item.getKeywords()) {
                        if (keyword.toLowerCase().contains(lowerQuery)) return true;
                    }
                }
                
                if (item.getBrand() != null && 
                    item.getBrand().toLowerCase().contains(lowerQuery)) return true;
                
                return false;
            })
            // Category filter
            .filter(item -> category == null || item.getCategory() == category)
            // Type filter
            .filter(item -> type == null || item.getType() == type)
            // Status filter
            .filter(item -> status == null || item.getStatus() == status)
            // Enrich with enterprise info
            .map(this::enrichItemWithEnterpriseInfo)
            // Sort by date
            .sorted((a, b) -> {
                if (a.getReportedDate() == null) return 1;
                if (b.getReportedDate() == null) return -1;
                return b.getReportedDate().compareTo(a.getReportedDate());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Refresh the name caches if needed
     */
    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        
        if (now - cacheRefreshTime > CACHE_TTL) {
            try {
                // Refresh enterprise cache
                enterpriseNameCache.clear();
                for (Enterprise e : enterpriseDAO.findAll()) {
                    enterpriseNameCache.put(e.getEnterpriseId(), e.getName());
                }
                
                // Refresh organization cache
                organizationNameCache.clear();
                orgToEnterpriseCache.clear();
                for (Organization o : organizationDAO.findAll()) {
                    organizationNameCache.put(o.getOrganizationId(), o.getName());
                    orgToEnterpriseCache.put(o.getOrganizationId(), o.getEnterpriseId());
                }
                
                cacheRefreshTime = now;
                LOGGER.fine("Enterprise/Organization cache refreshed");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error refreshing cache", e);
            }
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Search criteria for advanced search
     */
    public static class SearchCriteria {
        private String query;
        private ItemCategory category;
        private ItemType type;
        private ItemStatus status;
        private List<String> enterpriseIds;
        private List<String> organizationIds;
        private Date dateFrom;
        private Date dateTo;
        private Double minValue;
        private Double maxValue;
        private boolean includeResolved = false;
        
        // Builder pattern
        public SearchCriteria withQuery(String query) {
            this.query = query;
            return this;
        }
        
        public SearchCriteria withCategory(ItemCategory category) {
            this.category = category;
            return this;
        }
        
        public SearchCriteria withType(ItemType type) {
            this.type = type;
            return this;
        }
        
        public SearchCriteria withStatus(ItemStatus status) {
            this.status = status;
            return this;
        }
        
        public SearchCriteria withEnterpriseIds(List<String> enterpriseIds) {
            this.enterpriseIds = enterpriseIds;
            return this;
        }
        
        public SearchCriteria withOrganizationIds(List<String> organizationIds) {
            this.organizationIds = organizationIds;
            return this;
        }
        
        public SearchCriteria withDateRange(Date from, Date to) {
            this.dateFrom = from;
            this.dateTo = to;
            return this;
        }
        
        public SearchCriteria withValueRange(Double min, Double max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }
        
        public SearchCriteria includeResolved(boolean include) {
            this.includeResolved = include;
            return this;
        }
        
        // Getters
        public String getQuery() { return query; }
        public ItemCategory getCategory() { return category; }
        public ItemType getType() { return type; }
        public ItemStatus getStatus() { return status; }
        public List<String> getEnterpriseIds() { return enterpriseIds; }
        public List<String> getOrganizationIds() { return organizationIds; }
        public Date getDateFrom() { return dateFrom; }
        public Date getDateTo() { return dateTo; }
        public Double getMinValue() { return minValue; }
        public Double getMaxValue() { return maxValue; }
        public boolean isIncludeResolved() { return includeResolved; }
    }
    
    /**
     * Item with full enterprise context
     */
    public static class ItemWithContext {
        private Item item;
        private Enterprise enterprise;
        private Organization organization;
        
        public ItemWithContext(Item item) {
            this.item = item;
        }
        
        public Item getItem() { return item; }
        public void setItem(Item item) { this.item = item; }
        
        public Enterprise getEnterprise() { return enterprise; }
        public void setEnterprise(Enterprise enterprise) { this.enterprise = enterprise; }
        
        public Organization getOrganization() { return organization; }
        public void setOrganization(Organization organization) { this.organization = organization; }
        
        public String getEnterpriseName() {
            return enterprise != null ? enterprise.getName() : "Unknown";
        }
        
        public String getOrganizationName() {
            return organization != null ? organization.getName() : "Unknown";
        }
    }
    
    /**
     * Enterprise statistics
     */
    public static class EnterpriseStats {
        public String enterpriseId;
        public String enterpriseName;
        public String enterpriseType;
        public int totalItems;
        public int lostItems;
        public int foundItems;
        public int claimedItems;
        public int openItems;
        public double recoveryRate;
        public double totalValue;
        
        public Object[] toTableRow() {
            return new Object[] {
                enterpriseName,
                enterpriseType,
                totalItems,
                lostItems,
                foundItems,
                claimedItems,
                String.format("%.1f%%", recoveryRate * 100),
                String.format("$%.2f", totalValue)
            };
        }
        
        public static String[] getTableColumns() {
            return new String[] {
                "Enterprise", "Type", "Total", "Lost", "Found", 
                "Claimed", "Recovery Rate", "Total Value"
            };
        }
    }
    
    /**
     * Network effect metrics
     */
    public static class NetworkEffectMetrics {
        public double overallRecoveryRate;
        public double singleEnterpriseRecoveryRate;
        public double twoEnterpriseRecoveryRate;
        public double threeEnterpriseRecoveryRate;
        public double fourEnterpriseRecoveryRate;
        public double networkEffectImprovement;
        
        public String getSummary() {
            return String.format(
                "Single Enterprise: %.0f%% → 4 Enterprises: %.0f%% (%.0f%% improvement)",
                singleEnterpriseRecoveryRate * 100,
                fourEnterpriseRecoveryRate * 100,
                networkEffectImprovement * 100
            );
        }
    }
}
