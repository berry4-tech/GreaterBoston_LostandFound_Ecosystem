package com.campus.lostfound.services;

import com.campus.lostfound.models.*;
import com.campus.lostfound.models.workrequest.WorkRequest;
import java.util.List;
import java.util.Map;

/**
 * Interface for providing admin panel data with different scopes.
 * Implementations can filter data for network-wide, enterprise-specific,
 * or organization-specific views.
 * 
 * This follows the Strategy Pattern to allow the same AdminPanel UI
 * to work with different data scopes without code duplication.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public interface AdminDataProvider {
    
    // ==================== SCOPE INFORMATION ====================
    
    /**
     * Returns a human-readable description of this data scope.
     * E.g., "Network-Wide" or "Higher Education Enterprise"
     */
    String getScopeDescription();
    
    /**
     * Returns the enterprise ID if scoped to an enterprise, null otherwise.
     */
    String getEnterpriseId();
    
    /**
     * Returns true if this provider shows network-wide data.
     */
    boolean isNetworkScope();
    
    // ==================== ITEM DATA ====================
    
    /**
     * Returns all items within this scope.
     */
    List<Item> getItems();
    
    /**
     * Returns items filtered by type within this scope.
     */
    List<Item> getItemsByType(Item.ItemType type);
    
    /**
     * Returns items filtered by status within this scope.
     */
    List<Item> getItemsByStatus(Item.ItemStatus status);
    
    /**
     * Returns the most recent items within this scope.
     */
    List<Item> getRecentItems(int limit);
    
    /**
     * Returns item count by category within this scope.
     */
    Map<Item.ItemCategory, Long> getItemCountByCategory();
    
    // ==================== USER DATA ====================
    
    /**
     * Returns all users within this scope.
     */
    List<User> getUsers();
    
    /**
     * Returns users filtered by role within this scope.
     */
    List<User> getUsersByRole(User.UserRole role);
    
    /**
     * Searches users by name or email within this scope.
     */
    List<User> searchUsers(String searchText, String roleFilter, String enterpriseFilter);
    
    /**
     * Returns top contributors (users with most items reported) within this scope.
     */
    List<Map.Entry<User, Integer>> getTopContributors(int limit);
    
    /**
     * Returns active users (who have reported items) within this scope.
     */
    int getActiveUserCount();
    
    /**
     * Returns average trust score of users within this scope.
     */
    double getAverageTrustScore();
    
    // ==================== ENTERPRISE/ORGANIZATION DATA ====================
    
    /**
     * Returns enterprises visible in this scope.
     * For network scope: all enterprises
     * For enterprise scope: only the current enterprise
     */
    List<Enterprise> getEnterprises();
    
    /**
     * Returns organizations within this scope.
     */
    List<Organization> getOrganizations();
    
    /**
     * Returns whether enterprise management CRUD is allowed in this scope.
     * Typically only network admins can create/delete enterprises.
     */
    boolean canManageEnterprises();
    
    /**
     * Returns whether organization management CRUD is allowed in this scope.
     */
    boolean canManageOrganizations();
    
    // ==================== WORK REQUEST DATA ====================
    
    /**
     * Returns work requests within this scope.
     */
    List<WorkRequest> getWorkRequests();
    
    /**
     * Returns pending work requests within this scope.
     */
    List<WorkRequest> getPendingWorkRequests();
    
    // ==================== BUILDING DATA ====================
    
    /**
     * Returns buildings within this scope.
     */
    List<Building> getBuildings();
    
    /**
     * Returns building statistics within this scope.
     * Map of building code to item count.
     */
    Map<String, Integer> getBuildingItemCounts();
    
    // ==================== STATISTICS ====================
    
    /**
     * Returns total item count within this scope.
     */
    long getTotalItemCount();
    
    /**
     * Returns lost item count within this scope.
     */
    long getLostItemCount();
    
    /**
     * Returns found item count within this scope.
     */
    long getFoundItemCount();
    
    /**
     * Returns claimed item count within this scope.
     */
    long getClaimedItemCount();
    
    /**
     * Returns total user count within this scope.
     */
    long getTotalUserCount();
    
    /**
     * Returns total building count within this scope.
     */
    long getTotalBuildingCount();
    
    /**
     * Calculates recovery rate within this scope.
     */
    double getRecoveryRate();
}
