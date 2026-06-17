package com.campus.lostfound.services;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.workrequest.WorkRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Network-wide data provider for the Admin Panel.
 * Returns data from ALL enterprises and organizations across the network.
 * Used by System/Network Administrators.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class NetworkAdminDataProvider implements AdminDataProvider {
    
    private final MongoItemDAO itemDAO;
    private final MongoUserDAO userDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final MongoWorkRequestDAO workRequestDAO;
    private final MongoBuildingDAO buildingDAO;
    
    public NetworkAdminDataProvider() {
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.buildingDAO = new MongoBuildingDAO();
    }
    
    // Constructor for dependency injection (testing)
    public NetworkAdminDataProvider(MongoItemDAO itemDAO, MongoUserDAO userDAO,
            MongoEnterpriseDAO enterpriseDAO, MongoOrganizationDAO organizationDAO,
            MongoWorkRequestDAO workRequestDAO, MongoBuildingDAO buildingDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
        this.workRequestDAO = workRequestDAO;
        this.buildingDAO = buildingDAO;
    }
    
    // ==================== SCOPE INFORMATION ====================
    
    @Override
    public String getScopeDescription() {
        return "Network-Wide (All Enterprises)";
    }
    
    @Override
    public String getEnterpriseId() {
        return null; // Network scope has no specific enterprise
    }
    
    @Override
    public boolean isNetworkScope() {
        return true;
    }
    
    // ==================== ITEM DATA ====================
    
    @Override
    public List<Item> getItems() {
        return itemDAO.findAll();
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
    
    // ==================== USER DATA ====================
    
    @Override
    public List<User> getUsers() {
        return userDAO.findAll();
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
        
        return getUsers().stream()
                .filter(u -> search.isEmpty() 
                        || u.getEmail().toLowerCase().contains(search)
                        || u.getFullName().toLowerCase().contains(search))
                .filter(u -> "All Roles".equals(roleFilter) || roleFilter == null
                        || (u.getRole() != null && u.getRole().name().equals(roleFilter)))
                .filter(u -> "All Enterprises".equals(enterpriseFilter) || enterpriseFilter == null
                        || getEnterpriseName(u.getEnterpriseId()).equals(enterpriseFilter))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map.Entry<User, Integer>> getTopContributors(int limit) {
        List<Item> allItems = getItems();
        Map<User, Integer> userItemCounts = new HashMap<>();
        
        for (Item item : allItems) {
            User reporter = item.getReportedBy();
            if (reporter != null) {
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
            if (item.getReportedBy() != null) {
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
    
    // ==================== ENTERPRISE/ORGANIZATION DATA ====================
    
    @Override
    public List<Enterprise> getEnterprises() {
        return enterpriseDAO.findAll();
    }
    
    @Override
    public List<Organization> getOrganizations() {
        return organizationDAO.findAll();
    }
    
    @Override
    public boolean canManageEnterprises() {
        return true; // Network admins can manage all enterprises
    }
    
    @Override
    public boolean canManageOrganizations() {
        return true; // Network admins can manage all organizations
    }
    
    // ==================== WORK REQUEST DATA ====================
    
    @Override
    public List<WorkRequest> getWorkRequests() {
        return workRequestDAO.findAll();
    }
    
    @Override
    public List<WorkRequest> getPendingWorkRequests() {
        return getWorkRequests().stream()
                .filter(wr -> wr.getStatus() == WorkRequest.RequestStatus.PENDING)
                .collect(Collectors.toList());
    }
    
    // ==================== BUILDING DATA ====================
    
    @Override
    public List<Building> getBuildings() {
        return buildingDAO.findAll();
    }
    
    @Override
    public Map<String, Integer> getBuildingItemCounts() {
        Map<String, Integer> buildingCounts = new HashMap<>();
        for (Item item : getItems()) {
            if (item.getLocation() != null && item.getLocation().getBuilding() != null) {
                String code = item.getLocation().getBuilding().getCode();
                buildingCounts.put(code, buildingCounts.getOrDefault(code, 0) + 1);
            }
        }
        return buildingCounts;
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
        return buildingDAO.count();
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
    
    // ==================== HELPER METHODS ====================
    
    private String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null) return "N/A";
        return getEnterprises().stream()
                .filter(e -> e.getEnterpriseId().equals(enterpriseId))
                .map(Enterprise::getName)
                .findFirst()
                .orElse("Unknown");
    }
}
