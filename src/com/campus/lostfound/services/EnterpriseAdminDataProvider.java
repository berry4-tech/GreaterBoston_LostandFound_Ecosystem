package com.campus.lostfound.services;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.workrequest.WorkRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise-scoped data provider for the Admin Panel.
 * Returns data ONLY from the specified enterprise and its organizations.
 * Used by Enterprise Administrators (e.g., University Admin, MBTA Admin, Airport Admin).
 * 
 * Key differences from NetworkAdminDataProvider:
 * - All data is filtered by enterprise ID
 * - Enterprise CRUD operations are limited to viewing only
 * - Organization CRUD is allowed only within the enterprise
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseAdminDataProvider implements AdminDataProvider {
    
    private final String enterpriseId;
    private final String enterpriseName;
    
    private final MongoItemDAO itemDAO;
    private final MongoUserDAO userDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final MongoWorkRequestDAO workRequestDAO;
    private final MongoBuildingDAO buildingDAO;
    
    // Cached enterprise and organization IDs for filtering
    private Enterprise currentEnterprise;
    private Set<String> enterpriseOrganizationIds;
    
    /**
     * Creates an enterprise-scoped data provider.
     * @param enterpriseId The enterprise ID to scope all data to
     */
    public EnterpriseAdminDataProvider(String enterpriseId) {
        this.enterpriseId = enterpriseId;
        
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.buildingDAO = new MongoBuildingDAO();
        
        // Initialize enterprise info
        this.currentEnterprise = loadEnterprise();
        this.enterpriseName = currentEnterprise != null ? currentEnterprise.getName() : "Unknown Enterprise";
        this.enterpriseOrganizationIds = loadOrganizationIds();
    }
    
    /**
     * Creates an enterprise-scoped data provider from a user's enterprise.
     * @param user The user whose enterprise to scope to
     */
    public EnterpriseAdminDataProvider(User user) {
        this(user.getEnterpriseId());
    }
    
    // Constructor for dependency injection (testing)
    public EnterpriseAdminDataProvider(String enterpriseId, MongoItemDAO itemDAO, 
            MongoUserDAO userDAO, MongoEnterpriseDAO enterpriseDAO, 
            MongoOrganizationDAO organizationDAO, MongoWorkRequestDAO workRequestDAO, 
            MongoBuildingDAO buildingDAO) {
        this.enterpriseId = enterpriseId;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
        this.workRequestDAO = workRequestDAO;
        this.buildingDAO = buildingDAO;
        
        this.currentEnterprise = loadEnterprise();
        this.enterpriseName = currentEnterprise != null ? currentEnterprise.getName() : "Unknown Enterprise";
        this.enterpriseOrganizationIds = loadOrganizationIds();
    }
    
    private Enterprise loadEnterprise() {
        return enterpriseDAO.findAll().stream()
                .filter(e -> e.getEnterpriseId().equals(enterpriseId))
                .findFirst()
                .orElse(null);
    }
    
    private Set<String> loadOrganizationIds() {
        return organizationDAO.findAll().stream()
                .filter(o -> enterpriseId.equals(o.getEnterpriseId()))
                .map(Organization::getOrganizationId)
                .collect(Collectors.toSet());
    }
    
    /**
     * Refreshes the cached organization IDs.
     * Call this after adding/removing organizations.
     */
    public void refreshOrganizationCache() {
        this.enterpriseOrganizationIds = loadOrganizationIds();
    }
    
    // ==================== SCOPE INFORMATION ====================
    
    @Override
    public String getScopeDescription() {
        return enterpriseName + " Enterprise";
    }
    
    @Override
    public String getEnterpriseId() {
        return enterpriseId;
    }
    
    @Override
    public boolean isNetworkScope() {
        return false;
    }
    
    /**
     * Returns the current enterprise object.
     */
    public Enterprise getCurrentEnterprise() {
        return currentEnterprise;
    }
    
    // ==================== ITEM DATA ====================
    
    @Override
    public List<Item> getItems() {
        return itemDAO.findAll().stream()
                .filter(this::isInEnterprise)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Item> getItemsByType(ItemType type) {
        return getItems().stream()
                .filter(i -> i.getType() == type)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Item> getItemsByStatus(ItemStatus status) {
        return getItems().stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Item> getRecentItems(int limit) {
        return getItems().stream()
                .sorted((a, b) -> b.getReportedDate().compareTo(a.getReportedDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<ItemCategory, Long> getItemCountByCategory() {
        return getItems().stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(Item::getCategory, Collectors.counting()));
    }
    
    /**
     * Checks if an item belongs to this enterprise.
     * An item belongs if:
     * 1. Its enterprise ID matches, OR
     * 2. Its organization ID is within this enterprise
     */
    private boolean isInEnterprise(Item item) {
        // Check direct enterprise match
        if (enterpriseId.equals(item.getEnterpriseId())) {
            return true;
        }
        
        // Check organization match
        if (item.getOrganizationId() != null && enterpriseOrganizationIds.contains(item.getOrganizationId())) {
            return true;
        }
        
        // Check if reported by a user in this enterprise
        if (item.getReportedBy() != null && enterpriseId.equals(item.getReportedBy().getEnterpriseId())) {
            return true;
        }
        
        return false;
    }
    
    // ==================== USER DATA ====================
    
    @Override
    public List<User> getUsers() {
        return userDAO.findAll().stream()
                .filter(this::isUserInEnterprise)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> getUsersByRole(UserRole role) {
        return getUsers().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> searchUsers(String searchText, String roleFilter, String enterpriseFilter) {
        String search = searchText != null ? searchText.toLowerCase().trim() : "";
        
        // For enterprise scope, we ignore the enterprise filter (already scoped)
        return getUsers().stream()
                .filter(u -> search.isEmpty() 
                        || u.getEmail().toLowerCase().contains(search)
                        || u.getFullName().toLowerCase().contains(search))
                .filter(u -> "All Roles".equals(roleFilter) || roleFilter == null
                        || (u.getRole() != null && u.getRole().name().equals(roleFilter)))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map.Entry<User, Integer>> getTopContributors(int limit) {
        List<Item> allItems = getItems();
        Map<User, Integer> userItemCounts = new HashMap<>();
        
        for (Item item : allItems) {
            User reporter = item.getReportedBy();
            if (reporter != null && isUserInEnterprise(reporter)) {
                userItemCounts.put(reporter, userItemCounts.getOrDefault(reporter, 0) + 1);
            }
        }
        
        return userItemCounts.entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public int getActiveUserCount() {
        Set<Integer> activeUserIds = new HashSet<>();
        for (Item item : getItems()) {
            if (item.getReportedBy() != null && isUserInEnterprise(item.getReportedBy())) {
                activeUserIds.add(item.getReportedBy().getUserId());
            }
        }
        return activeUserIds.size();
    }
    
    @Override
    public double getAverageTrustScore() {
        return getUsers().stream()
                .mapToDouble(User::getTrustScore)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Checks if a user belongs to this enterprise.
     */
    private boolean isUserInEnterprise(User user) {
        if (user == null) return false;
        
        // Check direct enterprise match
        if (enterpriseId.equals(user.getEnterpriseId())) {
            return true;
        }
        
        // Check organization match
        if (user.getOrganizationId() != null && enterpriseOrganizationIds.contains(user.getOrganizationId())) {
            return true;
        }
        
        return false;
    }
    
    // ==================== ENTERPRISE/ORGANIZATION DATA ====================
    
    @Override
    public List<Enterprise> getEnterprises() {
        // Enterprise admins can only see their own enterprise
        if (currentEnterprise != null) {
            return Collections.singletonList(currentEnterprise);
        }
        return Collections.emptyList();
    }
    
    @Override
    public List<Organization> getOrganizations() {
        return organizationDAO.findAll().stream()
                .filter(o -> enterpriseId.equals(o.getEnterpriseId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean canManageEnterprises() {
        return false; // Enterprise admins cannot create/delete enterprises
    }
    
    @Override
    public boolean canManageOrganizations() {
        return true; // Enterprise admins can manage organizations within their enterprise
    }
    
    // ==================== WORK REQUEST DATA ====================
    
    @Override
    public List<WorkRequest> getWorkRequests() {
        return workRequestDAO.findAll().stream()
                .filter(this::isWorkRequestInEnterprise)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkRequest> getPendingWorkRequests() {
        return getWorkRequests().stream()
                .filter(wr -> wr.getStatus() == WorkRequest.RequestStatus.PENDING)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a work request involves this enterprise.
     */
    private boolean isWorkRequestInEnterprise(WorkRequest request) {
        // Check requester enterprise
        if (enterpriseId.equals(request.getRequesterEnterpriseId())) {
            return true;
        }
        
        // Check target enterprise
        if (enterpriseId.equals(request.getTargetEnterpriseId())) {
            return true;
        }
        
        return false;
    }
    
    // ==================== BUILDING DATA ====================
    
    @Override
    public List<Building> getBuildings() {
        return buildingDAO.findAll().stream()
                .filter(this::isBuildingInEnterprise)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Integer> getBuildingItemCounts() {
        Map<String, Integer> buildingCounts = new HashMap<>();
        for (Item item : getItems()) {
            if (item.getLocation() != null && item.getLocation().getBuilding() != null) {
                Building building = item.getLocation().getBuilding();
                if (isBuildingInEnterprise(building)) {
                    String code = building.getCode();
                    buildingCounts.put(code, buildingCounts.getOrDefault(code, 0) + 1);
                }
            }
        }
        return buildingCounts;
    }
    
    /**
     * Checks if a building belongs to this enterprise.
     */
    private boolean isBuildingInEnterprise(Building building) {
        if (building == null) return false;
        
        // Check direct enterprise match
        if (enterpriseId.equals(building.getEnterpriseId())) {
            return true;
        }
        
        // Check organization match
        if (building.getOrganizationId() != null && enterpriseOrganizationIds.contains(building.getOrganizationId())) {
            return true;
        }
        
        return false;
    }
    
    // ==================== STATISTICS ====================
    
    @Override
    public long getTotalItemCount() {
        return getItems().size();
    }
    
    @Override
    public long getLostItemCount() {
        return getItems().stream().filter(i -> i.getType() == ItemType.LOST).count();
    }
    
    @Override
    public long getFoundItemCount() {
        return getItems().stream().filter(i -> i.getType() == ItemType.FOUND).count();
    }
    
    @Override
    public long getClaimedItemCount() {
        return getItems().stream().filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
    }
    
    @Override
    public long getTotalUserCount() {
        return getUsers().size();
    }
    
    @Override
    public long getTotalBuildingCount() {
        return getBuildings().size();
    }
    
    @Override
    public double getRecoveryRate() {
        List<Item> items = getItems();
        if (items.isEmpty()) return 0.0;
        
        long recovered = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .count();
        
        return (double) recovered / items.size();
    }
    
    // ==================== ENTERPRISE-SPECIFIC STATISTICS ====================
    
    /**
     * Returns item count by organization within this enterprise.
     */
    public Map<String, Long> getItemCountByOrganization() {
        Map<String, Long> counts = new HashMap<>();
        
        for (Organization org : getOrganizations()) {
            long count = getItems().stream()
                    .filter(i -> org.getOrganizationId().equals(i.getOrganizationId()))
                    .count();
            counts.put(org.getName(), count);
        }
        
        return counts;
    }
    
    /**
     * Returns user count by organization within this enterprise.
     */
    public Map<String, Long> getUserCountByOrganization() {
        Map<String, Long> counts = new HashMap<>();
        
        for (Organization org : getOrganizations()) {
            long count = getUsers().stream()
                    .filter(u -> org.getOrganizationId().equals(u.getOrganizationId()))
                    .count();
            counts.put(org.getName(), count);
        }
        
        return counts;
    }
    
    /**
     * Returns recovery rate by organization within this enterprise.
     */
    public Map<String, Double> getRecoveryRateByOrganization() {
        Map<String, Double> rates = new HashMap<>();
        
        for (Organization org : getOrganizations()) {
            List<Item> orgItems = getItems().stream()
                    .filter(i -> org.getOrganizationId().equals(i.getOrganizationId()))
                    .collect(Collectors.toList());
            
            if (orgItems.isEmpty()) {
                rates.put(org.getName(), 0.0);
            } else {
                long recovered = orgItems.stream()
                        .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                        .count();
                rates.put(org.getName(), (double) recovered / orgItems.size());
            }
        }
        
        return rates;
    }
    
    /**
     * Returns cross-enterprise transfer statistics.
     * Shows how many items were transferred to/from other enterprises.
     */
    public Map<String, Object> getCrossEnterpriseTransferStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long incomingTransfers = workRequestDAO.findAll().stream()
                .filter(wr -> enterpriseId.equals(wr.getTargetEnterpriseId()) 
                        && !enterpriseId.equals(wr.getRequesterEnterpriseId()))
                .count();
        
        long outgoingTransfers = workRequestDAO.findAll().stream()
                .filter(wr -> enterpriseId.equals(wr.getRequesterEnterpriseId()) 
                        && !enterpriseId.equals(wr.getTargetEnterpriseId()))
                .count();
        
        stats.put("incomingTransfers", incomingTransfers);
        stats.put("outgoingTransfers", outgoingTransfers);
        stats.put("totalCrossEnterpriseActivity", incomingTransfers + outgoingTransfers);
        
        return stats;
    }
}
