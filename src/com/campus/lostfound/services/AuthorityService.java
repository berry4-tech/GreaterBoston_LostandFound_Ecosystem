package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.Item;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for role-based access control and permission management.
 * 
 * Provides comprehensive authorization checks for:
 * - Enterprise and organization access
 * - Item viewing and claiming
 * - Request approval permissions
 * - Transfer permissions
 * - Security and investigation access
 * 
 * This service is the central authority for all permission checks in the system.
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class AuthorityService {
    
    private static final Logger LOGGER = Logger.getLogger(AuthorityService.class.getName());
    
    // ==================== PERMISSION ENUM ====================
    
    /**
     * All permissions available in the system
     */
    public enum Permission {
        // Trust Score Permissions
        VIEW_TRUST_SCORES("View Trust Scores", "Can view trust scores of users"),
        MODIFY_TRUST_SCORES("Modify Trust Scores", "Can manually adjust trust scores"),
        
        // Claim & Approval Permissions
        APPROVE_CLAIMS("Approve Claims", "Can approve standard item claims"),
        APPROVE_HIGH_VALUE("Approve High-Value", "Can approve high-value item claims ($500+)"),
        APPROVE_VERY_HIGH_VALUE("Approve Very High-Value", "Can approve very high-value claims ($2000+)"),
        REJECT_CLAIMS("Reject Claims", "Can reject item claims"),
        
        // Transfer Permissions
        INITIATE_TRANSFERS("Initiate Transfers", "Can start item transfers between organizations"),
        APPROVE_TRANSFERS("Approve Transfers", "Can approve incoming transfers"),
        RECEIVE_TRANSFERS("Receive Transfers", "Can receive transferred items"),
        CROSS_ENTERPRISE_TRANSFER("Cross-Enterprise Transfer", "Can transfer across enterprises"),
        
        // Security & Investigation
        POLICE_DATABASE_ACCESS("Police Database Access", "Can access police/stolen property databases"),
        FRAUD_INVESTIGATION("Fraud Investigation", "Can conduct fraud investigations"),
        FLAG_USERS("Flag Users", "Can flag users for suspicious activity"),
        UNFLAG_USERS("Unflag Users", "Can remove flags from users"),
        VIEW_INVESTIGATIONS("View Investigations", "Can view ongoing investigations"),
        
        // Verification Permissions
        VERIFY_IDENTITY("Verify Identity", "Can verify user identities"),
        VERIFY_OWNERSHIP("Verify Ownership", "Can verify item ownership"),
        VERIFY_SERIAL_NUMBERS("Verify Serial Numbers", "Can check serial numbers"),
        PROCESS_VERIFICATIONS("Process Verifications", "Can process verification requests"),
        
        // Administrative
        MANAGE_USERS("Manage Users", "Can create/edit/delete users"),
        MANAGE_ITEMS("Manage Items", "Can edit/delete any items"),
        VIEW_ALL_ITEMS("View All Items", "Can view items across all organizations"),
        VIEW_REPORTS("View Reports", "Can view system reports and analytics"),
        SYSTEM_ADMIN("System Administration", "Full system access"),
        
        // Item Management
        REPORT_ITEMS("Report Items", "Can report lost/found items"),
        CLAIM_ITEMS("Claim Items", "Can submit item claims"),
        RELEASE_ITEMS("Release Items", "Can release items to claimants");
        
        private final String displayName;
        private final String description;
        
        Permission(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // ==================== ROLE-PERMISSION MAPPINGS ====================
    
    /**
     * Static mapping of roles to their permissions
     */
    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(UserRole.class);
    
    static {
        // Initialize all role permissions
        initializeRolePermissions();
    }
    
    private static void initializeRolePermissions() {
        // ===== Higher Education Roles =====
        
        // STUDENT - Basic permissions
        ROLE_PERMISSIONS.put(UserRole.STUDENT, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS
        ));
        
        // STAFF - Basic + view
        ROLE_PERMISSIONS.put(UserRole.STAFF, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS,
            Permission.VIEW_TRUST_SCORES
        ));
        
        // CAMPUS_COORDINATOR - Approval permissions
        ROLE_PERMISSIONS.put(UserRole.CAMPUS_COORDINATOR, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.INITIATE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS,
            Permission.VIEW_REPORTS
        ));
        
        // BUILDING_MANAGER - Local management
        ROLE_PERMISSIONS.put(UserRole.BUILDING_MANAGER, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.MANAGE_ITEMS
        ));
        
        // CAMPUS_SECURITY - Security focused
        ROLE_PERMISSIONS.put(UserRole.CAMPUS_SECURITY, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VERIFY_IDENTITY,
            Permission.VERIFY_OWNERSHIP,
            Permission.PROCESS_VERIFICATIONS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.INITIATE_TRANSFERS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // UNIVERSITY_ADMIN - Full university access
        ROLE_PERMISSIONS.put(UserRole.UNIVERSITY_ADMIN, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.APPROVE_VERY_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.INITIATE_TRANSFERS,
            Permission.APPROVE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS,
            Permission.CROSS_ENTERPRISE_TRANSFER,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.MANAGE_USERS,
            Permission.MANAGE_ITEMS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // ===== MBTA Roles =====
        
        // STATION_MANAGER
        ROLE_PERMISSIONS.put(UserRole.STATION_MANAGER, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.INITIATE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS,
            Permission.MANAGE_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // LOST_FOUND_CLERK
        ROLE_PERMISSIONS.put(UserRole.LOST_FOUND_CLERK, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.RECEIVE_TRANSFERS
        ));
        
        // TRANSIT_SECURITY_INSPECTOR - Security focused
        ROLE_PERMISSIONS.put(UserRole.TRANSIT_SECURITY_INSPECTOR, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VERIFY_IDENTITY,
            Permission.PROCESS_VERIFICATIONS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.VIEW_ALL_ITEMS,
            Permission.INITIATE_TRANSFERS,
            Permission.CROSS_ENTERPRISE_TRANSFER,
            Permission.VIEW_REPORTS
        ));
        
        // TRANSIT_OFFICER
        ROLE_PERMISSIONS.put(UserRole.TRANSIT_OFFICER, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.VERIFY_IDENTITY,
            Permission.FLAG_USERS,
            Permission.VIEW_INVESTIGATIONS
        ));
        
        // ===== Airport Roles =====
        
        // AIRPORT_LOST_FOUND_SPECIALIST
        ROLE_PERMISSIONS.put(UserRole.AIRPORT_LOST_FOUND_SPECIALIST, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.VERIFY_IDENTITY,
            Permission.VERIFY_OWNERSHIP,
            Permission.INITIATE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS,
            Permission.CROSS_ENTERPRISE_TRANSFER,
            Permission.VIEW_REPORTS
        ));
        
        // TSA_SECURITY_COORDINATOR
        ROLE_PERMISSIONS.put(UserRole.TSA_SECURITY_COORDINATOR, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.APPROVE_VERY_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VERIFY_IDENTITY,
            Permission.VERIFY_SERIAL_NUMBERS,
            Permission.PROCESS_VERIFICATIONS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.POLICE_DATABASE_ACCESS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // AIRLINE_REPRESENTATIVE
        ROLE_PERMISSIONS.put(UserRole.AIRLINE_REPRESENTATIVE, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.INITIATE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS
        ));
        
        // ===== Law Enforcement Roles =====
        
        // POLICE_EVIDENCE_CUSTODIAN - Full evidence access
        ROLE_PERMISSIONS.put(UserRole.POLICE_EVIDENCE_CUSTODIAN, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.APPROVE_VERY_HIGH_VALUE,
            Permission.REJECT_CLAIMS,
            Permission.RELEASE_ITEMS,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VERIFY_IDENTITY,
            Permission.VERIFY_OWNERSHIP,
            Permission.VERIFY_SERIAL_NUMBERS,
            Permission.PROCESS_VERIFICATIONS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.POLICE_DATABASE_ACCESS,
            Permission.INITIATE_TRANSFERS,
            Permission.APPROVE_TRANSFERS,
            Permission.RECEIVE_TRANSFERS,
            Permission.CROSS_ENTERPRISE_TRANSFER,
            Permission.MANAGE_ITEMS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // DETECTIVE - Investigation focused
        ROLE_PERMISSIONS.put(UserRole.DETECTIVE, EnumSet.of(
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VERIFY_IDENTITY,
            Permission.VERIFY_OWNERSHIP,
            Permission.VERIFY_SERIAL_NUMBERS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.POLICE_DATABASE_ACCESS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // POLICE_ADMIN
        ROLE_PERMISSIONS.put(UserRole.POLICE_ADMIN, EnumSet.of(
            Permission.VIEW_TRUST_SCORES,
            Permission.MODIFY_TRUST_SCORES,
            Permission.APPROVE_CLAIMS,
            Permission.APPROVE_HIGH_VALUE,
            Permission.APPROVE_VERY_HIGH_VALUE,
            Permission.FLAG_USERS,
            Permission.UNFLAG_USERS,
            Permission.VIEW_INVESTIGATIONS,
            Permission.FRAUD_INVESTIGATION,
            Permission.POLICE_DATABASE_ACCESS,
            Permission.MANAGE_USERS,
            Permission.VIEW_ALL_ITEMS,
            Permission.VIEW_REPORTS
        ));
        
        // ===== Public/General Roles =====
        
        // PUBLIC_TRAVELER - Most restricted
        ROLE_PERMISSIONS.put(UserRole.PUBLIC_TRAVELER, EnumSet.of(
            Permission.REPORT_ITEMS,
            Permission.CLAIM_ITEMS
        ));
        
        // SYSTEM_ADMIN - Full access
        ROLE_PERMISSIONS.put(UserRole.SYSTEM_ADMIN, EnumSet.allOf(Permission.class));
    }
    
    // ==================== APPROVAL LIMITS ====================
    
    /**
     * Maximum dollar value a role can approve without escalation
     */
    private static final Map<UserRole, Double> APPROVAL_LIMITS = new EnumMap<>(UserRole.class);
    
    static {
        // Students/Public can't approve anything
        APPROVAL_LIMITS.put(UserRole.STUDENT, 0.0);
        APPROVAL_LIMITS.put(UserRole.PUBLIC_TRAVELER, 0.0);
        APPROVAL_LIMITS.put(UserRole.STAFF, 0.0);
        
        // Basic clerks - low value
        APPROVAL_LIMITS.put(UserRole.LOST_FOUND_CLERK, 100.0);
        APPROVAL_LIMITS.put(UserRole.AIRLINE_REPRESENTATIVE, 200.0);
        
        // Coordinators/Managers - medium value
        APPROVAL_LIMITS.put(UserRole.CAMPUS_COORDINATOR, 500.0);
        APPROVAL_LIMITS.put(UserRole.BUILDING_MANAGER, 500.0);
        APPROVAL_LIMITS.put(UserRole.STATION_MANAGER, 500.0);
        APPROVAL_LIMITS.put(UserRole.TRANSIT_OFFICER, 200.0);
        
        // Security roles - high value
        APPROVAL_LIMITS.put(UserRole.CAMPUS_SECURITY, 2000.0);
        APPROVAL_LIMITS.put(UserRole.TRANSIT_SECURITY_INSPECTOR, 2000.0);
        APPROVAL_LIMITS.put(UserRole.AIRPORT_LOST_FOUND_SPECIALIST, 2000.0);
        APPROVAL_LIMITS.put(UserRole.TSA_SECURITY_COORDINATOR, 5000.0);
        
        // Admin/Police - very high value
        APPROVAL_LIMITS.put(UserRole.UNIVERSITY_ADMIN, 10000.0);
        APPROVAL_LIMITS.put(UserRole.POLICE_EVIDENCE_CUSTODIAN, Double.MAX_VALUE);
        APPROVAL_LIMITS.put(UserRole.DETECTIVE, 5000.0);
        APPROVAL_LIMITS.put(UserRole.POLICE_ADMIN, Double.MAX_VALUE);
        APPROVAL_LIMITS.put(UserRole.SYSTEM_ADMIN, Double.MAX_VALUE);
    }
    
    // ==================== INSTANCE FIELDS ====================
    
    private final MongoUserDAO userDAO;
    private final MongoItemDAO itemDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    
    // ==================== CONSTRUCTORS ====================
    
    public AuthorityService() {
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
    }
    
    /**
     * Constructor for testing with mock DAOs
     */
    public AuthorityService(MongoUserDAO userDAO, MongoItemDAO itemDAO,
                           MongoEnterpriseDAO enterpriseDAO, 
                           MongoOrganizationDAO organizationDAO) {
        this.userDAO = userDAO;
        this.itemDAO = itemDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
    }
    
    // ==================== ACCESS CONTROL ====================
    
    /**
     * Check if user can access a specific enterprise
     */
    public boolean canUserAccessEnterprise(String userId, String enterpriseId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // System admins can access everything
            if (user.getRole() == UserRole.SYSTEM_ADMIN) {
                return true;
            }
            
            // Police/Detective can access all enterprises for investigations
            if (isLawEnforcement(user.getRole())) {
                return true;
            }
            
            // User must belong to the enterprise
            return enterpriseId != null && enterpriseId.equals(user.getEnterpriseId());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking enterprise access", e);
            return false;
        }
    }
    
    /**
     * Check if user can access a specific organization
     */
    public boolean canUserAccessOrganization(String userId, String orgId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // System admins can access everything
            if (user.getRole() == UserRole.SYSTEM_ADMIN) {
                return true;
            }
            
            // Police/Detective can access all organizations
            if (isLawEnforcement(user.getRole())) {
                return true;
            }
            
            // Users with VIEW_ALL_ITEMS can access any org in their enterprise
            if (hasPermission(user, Permission.VIEW_ALL_ITEMS)) {
                // Check if org is in user's enterprise
                return isOrgInEnterprise(orgId, user.getEnterpriseId());
            }
            
            // User must belong to the organization
            return orgId != null && orgId.equals(user.getOrganizationId());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking organization access", e);
            return false;
        }
    }
    
    /**
     * Check if user can view a specific item
     */
    public boolean canUserViewItem(String userId, String itemId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // System admin sees all
            if (user.getRole() == UserRole.SYSTEM_ADMIN) {
                return true;
            }
            
            // Police can see all items
            if (isLawEnforcement(user.getRole())) {
                return true;
            }
            
            // Get the item
            Optional<Item> itemOpt = itemDAO.findById(itemId);
            if (itemOpt.isEmpty()) {
                return false;
            }
            
            Item item = itemOpt.get();
            
            // User reported the item - can always view
            if (item.getReportedBy() != null && 
                userId.equals(item.getReportedBy().getEmail())) {
                return true;
            }
            
            // User claimed the item - can view
            if (item.getClaimedBy() != null && 
                userId.equals(item.getClaimedBy().getEmail())) {
                return true;
            }
            
            // Check if user can view all items
            if (hasPermission(user, Permission.VIEW_ALL_ITEMS)) {
                return true;
            }
            
            // Check organization access
            return canUserAccessOrganization(userId, item.getOrganizationId());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking item view access", e);
            return false;
        }
    }
    
    // ==================== APPROVAL PERMISSIONS ====================
    
    /**
     * Check if user can approve a specific request type
     */
    public boolean canUserApproveRequest(String userId, String requestType) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // Check basic approval permission
            if (!hasPermission(user, Permission.APPROVE_CLAIMS)) {
                return false;
            }
            
            // Additional checks based on request type
            if ("HIGH_VALUE".equals(requestType) || "HIGH_VALUE_ITEM_CLAIM".equals(requestType)) {
                return hasPermission(user, Permission.APPROVE_HIGH_VALUE);
            }
            
            if ("VERY_HIGH_VALUE".equals(requestType)) {
                return hasPermission(user, Permission.APPROVE_VERY_HIGH_VALUE);
            }
            
            if ("CROSS_ENTERPRISE".equals(requestType) || "CROSS_ENTERPRISE_TRANSFER".equals(requestType)) {
                return hasPermission(user, Permission.CROSS_ENTERPRISE_TRANSFER);
            }
            
            if ("TRANSFER".equals(requestType)) {
                return hasPermission(user, Permission.APPROVE_TRANSFERS);
            }
            
            return true; // Can approve standard requests
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking approval permission", e);
            return false;
        }
    }
    
    /**
     * Check if user can approve high-value items ($500+)
     */
    public boolean canUserApproveHighValue(String userId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            return hasPermission(user, Permission.APPROVE_HIGH_VALUE);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking high-value approval", e);
            return false;
        }
    }
    
    /**
     * Check if user can approve cross-enterprise requests
     */
    public boolean canUserApproveCrossEnterprise(String userId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            return hasPermission(user, Permission.CROSS_ENTERPRISE_TRANSFER);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking cross-enterprise approval", e);
            return false;
        }
    }
    
    /**
     * Get the maximum dollar value a user can approve
     */
    public double getApprovalLimit(String userId) {
        try {
            User user = getUser(userId);
            if (user == null) return 0.0;
            
            Double limit = APPROVAL_LIMITS.get(user.getRole());
            return limit != null ? limit : 0.0;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting approval limit", e);
            return 0.0;
        }
    }
    
    /**
     * Check if user can approve a specific dollar amount
     */
    public boolean canApproveAmount(String userId, double amount) {
        return getApprovalLimit(userId) >= amount;
    }
    
    // ==================== TRANSFER PERMISSIONS ====================
    
    /**
     * Check if user can transfer a specific item
     */
    public boolean canUserTransferItem(String userId, String itemId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // Must have transfer permission
            if (!hasPermission(user, Permission.INITIATE_TRANSFERS)) {
                return false;
            }
            
            // Get item to check ownership/organization
            Optional<Item> itemOpt = itemDAO.findById(itemId);
            if (itemOpt.isEmpty()) {
                return false;
            }
            
            Item item = itemOpt.get();
            
            // System admin can transfer anything
            if (user.getRole() == UserRole.SYSTEM_ADMIN) {
                return true;
            }
            
            // Police can transfer any item
            if (isLawEnforcement(user.getRole())) {
                return true;
            }
            
            // User must have access to item's organization
            return canUserAccessOrganization(userId, item.getOrganizationId());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking transfer permission", e);
            return false;
        }
    }
    
    /**
     * Check if user can initiate transfer between organizations
     */
    public boolean canUserInitiateTransfer(String userId, String fromOrgId, String toOrgId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // Must have transfer permission
            if (!hasPermission(user, Permission.INITIATE_TRANSFERS)) {
                return false;
            }
            
            // Must have access to source organization
            if (!canUserAccessOrganization(userId, fromOrgId)) {
                return false;
            }
            
            // Check if cross-enterprise
            String fromEnterprise = getEnterpriseForOrg(fromOrgId);
            String toEnterprise = getEnterpriseForOrg(toOrgId);
            
            if (fromEnterprise != null && !fromEnterprise.equals(toEnterprise)) {
                // Cross-enterprise transfer requires special permission
                return hasPermission(user, Permission.CROSS_ENTERPRISE_TRANSFER);
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking transfer initiation", e);
            return false;
        }
    }
    
    /**
     * Check if user can receive transfers from an organization
     */
    public boolean canUserReceiveTransfer(String userId, String fromOrgId) {
        try {
            User user = getUser(userId);
            if (user == null) return false;
            
            // Must have receive permission
            if (!hasPermission(user, Permission.RECEIVE_TRANSFERS)) {
                return false;
            }
            
            // Check if cross-enterprise
            String fromEnterprise = getEnterpriseForOrg(fromOrgId);
            String userEnterprise = user.getEnterpriseId();
            
            if (fromEnterprise != null && !fromEnterprise.equals(userEnterprise)) {
                // Cross-enterprise requires special permission
                return hasPermission(user, Permission.CROSS_ENTERPRISE_TRANSFER);
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking receive transfer", e);
            return false;
        }
    }
    
    // ==================== ROLE QUERIES ====================
    
    /**
     * Get all permissions for a specific role
     */
    public Set<Permission> getPermissionsForRole(UserRole role) {
        Set<Permission> permissions = ROLE_PERMISSIONS.get(role);
        return permissions != null ? EnumSet.copyOf(permissions) : EnumSet.noneOf(Permission.class);
    }
    
    /**
     * Check if a user has a specific permission
     */
    public boolean hasPermission(String userId, Permission permission) {
        try {
            User user = getUser(userId);
            return user != null && hasPermission(user, permission);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking permission", e);
            return false;
        }
    }
    
    /**
     * Check if a user object has a specific permission
     */
    public boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) return false;
        
        Set<Permission> permissions = ROLE_PERMISSIONS.get(user.getRole());
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Get all effective permissions for a user
     */
    public Set<Permission> getEffectivePermissions(String userId) {
        try {
            User user = getUser(userId);
            if (user == null) return EnumSet.noneOf(Permission.class);
            
            return getPermissionsForRole(user.getRole());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting effective permissions", e);
            return EnumSet.noneOf(Permission.class);
        }
    }
    
    /**
     * Get permissions as a list of strings (for UI display)
     */
    public List<String> getPermissionNames(String userId) {
        Set<Permission> permissions = getEffectivePermissions(userId);
        List<String> names = new ArrayList<>();
        for (Permission p : permissions) {
            names.add(p.getDisplayName());
        }
        Collections.sort(names);
        return names;
    }
    
    // ==================== SECURITY PERMISSIONS ====================
    
    /**
     * Check if user can access police databases
     */
    public boolean canAccessPoliceDatabase(String userId) {
        return hasPermission(userId, Permission.POLICE_DATABASE_ACCESS);
    }
    
    /**
     * Check if user can conduct fraud investigations
     */
    public boolean canInvestigateFraud(String userId) {
        return hasPermission(userId, Permission.FRAUD_INVESTIGATION);
    }
    
    /**
     * Check if user can flag other users
     */
    public boolean canFlagUsers(String userId) {
        return hasPermission(userId, Permission.FLAG_USERS);
    }
    
    /**
     * Check if user can unflag other users
     */
    public boolean canUnflagUsers(String userId) {
        return hasPermission(userId, Permission.UNFLAG_USERS);
    }
    
    /**
     * Check if user can view trust scores
     */
    public boolean canViewTrustScores(String userId) {
        return hasPermission(userId, Permission.VIEW_TRUST_SCORES);
    }
    
    /**
     * Check if user can modify trust scores
     */
    public boolean canModifyTrustScores(String userId) {
        return hasPermission(userId, Permission.MODIFY_TRUST_SCORES);
    }
    
    /**
     * Check if user can process verification requests
     */
    public boolean canProcessVerifications(String userId) {
        return hasPermission(userId, Permission.PROCESS_VERIFICATIONS);
    }
    
    /**
     * Check if user can verify identities
     */
    public boolean canVerifyIdentity(String userId) {
        return hasPermission(userId, Permission.VERIFY_IDENTITY);
    }
    
    /**
     * Check if user can verify serial numbers
     */
    public boolean canVerifySerialNumbers(String userId) {
        return hasPermission(userId, Permission.VERIFY_SERIAL_NUMBERS);
    }
    
    // ==================== ROLE CHECKS ====================
    
    /**
     * Check if role is a law enforcement role
     */
    public boolean isLawEnforcement(UserRole role) {
        return role == UserRole.POLICE_EVIDENCE_CUSTODIAN ||
               role == UserRole.DETECTIVE ||
               role == UserRole.POLICE_ADMIN;
    }
    
    /**
     * Check if role is a security role
     */
    public boolean isSecurityRole(UserRole role) {
        return role == UserRole.CAMPUS_SECURITY ||
               role == UserRole.TRANSIT_SECURITY_INSPECTOR ||
               role == UserRole.TSA_SECURITY_COORDINATOR ||
               isLawEnforcement(role);
    }
    
    /**
     * Check if role is an admin role
     */
    public boolean isAdminRole(UserRole role) {
        return role == UserRole.UNIVERSITY_ADMIN ||
               role == UserRole.POLICE_ADMIN ||
               role == UserRole.SYSTEM_ADMIN;
    }
    
    /**
     * Check if user is in a security role
     */
    public boolean isUserSecurityRole(String userId) {
        try {
            User user = getUser(userId);
            return user != null && isSecurityRole(user.getRole());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if user is law enforcement
     */
    public boolean isUserLawEnforcement(String userId) {
        try {
            User user = getUser(userId);
            return user != null && isLawEnforcement(user.getRole());
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== AUTHORIZATION SUMMARY ====================
    
    /**
     * Get a comprehensive authorization summary for a user
     */
    public Map<String, Object> getAuthorizationSummary(String userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        try {
            User user = getUser(userId);
            if (user == null) {
                summary.put("error", "User not found");
                return summary;
            }
            
            summary.put("userId", userId);
            summary.put("role", user.getRole().getDisplayName());
            summary.put("roleCode", user.getRole().name());
            summary.put("enterpriseId", user.getEnterpriseId());
            summary.put("organizationId", user.getOrganizationId());
            summary.put("approvalLimit", getApprovalLimit(userId));
            summary.put("isSecurityRole", isSecurityRole(user.getRole()));
            summary.put("isLawEnforcement", isLawEnforcement(user.getRole()));
            summary.put("isAdmin", isAdminRole(user.getRole()));
            summary.put("permissionCount", getEffectivePermissions(userId).size());
            summary.put("permissions", getPermissionNames(userId));
            
            // Key capabilities
            Map<String, Boolean> capabilities = new LinkedHashMap<>();
            capabilities.put("canApproveHighValue", canUserApproveHighValue(userId));
            capabilities.put("canApproveCrossEnterprise", canUserApproveCrossEnterprise(userId));
            capabilities.put("canAccessPoliceDatabase", canAccessPoliceDatabase(userId));
            capabilities.put("canInvestigateFraud", canInvestigateFraud(userId));
            capabilities.put("canModifyTrustScores", canModifyTrustScores(userId));
            capabilities.put("canProcessVerifications", canProcessVerifications(userId));
            summary.put("capabilities", capabilities);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting authorization summary", e);
            summary.put("error", e.getMessage());
        }
        
        return summary;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Get user by ID or email
     */
    private User getUser(String userId) {
        try {
            // Try by email first
            if (userId != null && userId.contains("@")) {
                Optional<User> userOpt = userDAO.findByEmail(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
            
            // Try by ID
            try {
                Optional<User> userOpt = userDAO.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            } catch (Exception e) {
                // Not a valid ObjectId
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting user: " + userId, e);
            return null;
        }
    }
    
    /**
     * Check if an organization belongs to an enterprise
     */
    private boolean isOrgInEnterprise(String orgId, String enterpriseId) {
        if (orgId == null || enterpriseId == null) return false;
        
        try {
            String orgEnterprise = getEnterpriseForOrg(orgId);
            return enterpriseId.equals(orgEnterprise);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get enterprise ID for an organization
     */
    private String getEnterpriseForOrg(String orgId) {
        if (orgId == null) return null;
        
        try {
            var orgOpt = organizationDAO.findById(orgId);
            if (orgOpt.isPresent()) {
                return orgOpt.get().getEnterpriseId();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting enterprise for org: " + orgId, e);
        }
        return null;
    }
    
    // ==================== STATIC UTILITY METHODS ====================
    
    /**
     * Get all available permissions
     */
    public static List<Permission> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }
    
    /**
     * Get all roles
     */
    public static List<UserRole> getAllRoles() {
        return Arrays.asList(UserRole.values());
    }
    
    /**
     * Get permission by name
     */
    public static Permission getPermissionByName(String name) {
        try {
            return Permission.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
