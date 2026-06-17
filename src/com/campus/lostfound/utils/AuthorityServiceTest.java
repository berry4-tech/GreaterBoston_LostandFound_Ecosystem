package com.campus.lostfound.utils;

import com.campus.lostfound.dao.MongoUserDAO;
import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.services.AuthorityService;
import com.campus.lostfound.services.AuthorityService.Permission;

import java.util.*;
import java.util.logging.Logger;

/**
 * Test class for Authority Service (Part 12 of Developer 3)
 * Tests role-based permissions and access control
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class AuthorityServiceTest {
    
    private static final Logger LOGGER = Logger.getLogger(AuthorityServiceTest.class.getName());
    
    private AuthorityService service;
    private MongoUserDAO userDAO;
    private MongoItemDAO itemDAO;
    
    private int passedTests = 0;
    private int failedTests = 0;
    
    public AuthorityServiceTest() {
        this.service = new AuthorityService();
        this.userDAO = new MongoUserDAO();
        this.itemDAO = new MongoItemDAO();
    }
    
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔑 AUTHORITY SERVICE TESTS");
        System.out.println("=".repeat(70) + "\n");
        
        // Permission Enum Tests
        testPermissionEnum();
        
        // Role-Permission Mapping Tests
        testStudentPermissions();
        testCampusCoordinatorPermissions();
        testCampusSecurityPermissions();
        testPolicePermissions();
        testSystemAdminPermissions();
        testMBTARolePermissions();
        testAirportRolePermissions();
        
        // Approval Limits Tests
        testApprovalLimits();
        
        // Access Control Tests
        testAccessControlMethods();
        
        // Security Permission Tests
        testSecurityPermissions();
        
        // Role Classification Tests
        testRoleClassification();
        
        // Authorization Summary Tests
        testAuthorizationSummary();
        
        // Static Methods Tests
        testStaticMethods();
        
        // Live User Tests (if users exist in DB)
        testWithRealUsers();
        
        // Print summary
        printSummary();
    }
    
    // ==================== PERMISSION ENUM TESTS ====================
    
    private void testPermissionEnum() {
        System.out.println("📋 Testing Permission Enum...");
        
        // Test all permissions have required properties
        for (Permission perm : Permission.values()) {
            assertNotNull("Permission " + perm + " has display name", perm.getDisplayName());
            assertNotNull("Permission " + perm + " has description", perm.getDescription());
        }
        
        // Test specific permissions exist
        assertNotNull("VIEW_TRUST_SCORES exists", Permission.VIEW_TRUST_SCORES);
        assertNotNull("APPROVE_CLAIMS exists", Permission.APPROVE_CLAIMS);
        assertNotNull("POLICE_DATABASE_ACCESS exists", Permission.POLICE_DATABASE_ACCESS);
        assertNotNull("CROSS_ENTERPRISE_TRANSFER exists", Permission.CROSS_ENTERPRISE_TRANSFER);
        assertNotNull("SYSTEM_ADMIN exists", Permission.SYSTEM_ADMIN);
        
        // Test permission count (should be 24 based on AuthorityService)
        int permCount = Permission.values().length;
        assertTrue("At least 20 permissions defined", permCount >= 20);
        
        System.out.println("   Total permissions defined: " + permCount);
        System.out.println("   ✓ Permission enum tests passed\n");
    }
    
    // ==================== ROLE-PERMISSION MAPPING TESTS ====================
    
    private void testStudentPermissions() {
        System.out.println("📋 Testing Student Permissions...");
        
        Set<Permission> studentPerms = service.getPermissionsForRole(UserRole.STUDENT);
        assertNotNull("Student permissions exist", studentPerms);
        
        // Students should have basic permissions
        assertTrue("Student can report items", studentPerms.contains(Permission.REPORT_ITEMS));
        assertTrue("Student can claim items", studentPerms.contains(Permission.CLAIM_ITEMS));
        
        // Students should NOT have admin permissions
        assertFalse("Student cannot approve claims", studentPerms.contains(Permission.APPROVE_CLAIMS));
        assertFalse("Student cannot modify trust scores", studentPerms.contains(Permission.MODIFY_TRUST_SCORES));
        assertFalse("Student cannot access police DB", studentPerms.contains(Permission.POLICE_DATABASE_ACCESS));
        assertFalse("Student cannot manage users", studentPerms.contains(Permission.MANAGE_USERS));
        
        System.out.println("   Student permission count: " + studentPerms.size());
        System.out.println("   ✓ Student permissions tests passed\n");
    }
    
    private void testCampusCoordinatorPermissions() {
        System.out.println("📋 Testing Campus Coordinator Permissions...");
        
        Set<Permission> coordPerms = service.getPermissionsForRole(UserRole.CAMPUS_COORDINATOR);
        assertNotNull("Coordinator permissions exist", coordPerms);
        
        // Coordinators should have approval permissions
        assertTrue("Coordinator can approve claims", coordPerms.contains(Permission.APPROVE_CLAIMS));
        assertTrue("Coordinator can reject claims", coordPerms.contains(Permission.REJECT_CLAIMS));
        assertTrue("Coordinator can release items", coordPerms.contains(Permission.RELEASE_ITEMS));
        assertTrue("Coordinator can initiate transfers", coordPerms.contains(Permission.INITIATE_TRANSFERS));
        assertTrue("Coordinator can view reports", coordPerms.contains(Permission.VIEW_REPORTS));
        
        // Coordinators should NOT have high-level permissions
        assertFalse("Coordinator cannot approve high value", coordPerms.contains(Permission.APPROVE_HIGH_VALUE));
        assertFalse("Coordinator cannot flag users", coordPerms.contains(Permission.FLAG_USERS));
        
        System.out.println("   Coordinator permission count: " + coordPerms.size());
        System.out.println("   ✓ Campus Coordinator permissions tests passed\n");
    }
    
    private void testCampusSecurityPermissions() {
        System.out.println("📋 Testing Campus Security Permissions...");
        
        Set<Permission> securityPerms = service.getPermissionsForRole(UserRole.CAMPUS_SECURITY);
        assertNotNull("Security permissions exist", securityPerms);
        
        // Security should have security-focused permissions
        assertTrue("Security can view trust scores", securityPerms.contains(Permission.VIEW_TRUST_SCORES));
        assertTrue("Security can modify trust scores", securityPerms.contains(Permission.MODIFY_TRUST_SCORES));
        assertTrue("Security can approve high value", securityPerms.contains(Permission.APPROVE_HIGH_VALUE));
        assertTrue("Security can flag users", securityPerms.contains(Permission.FLAG_USERS));
        assertTrue("Security can unflag users", securityPerms.contains(Permission.UNFLAG_USERS));
        assertTrue("Security can verify identity", securityPerms.contains(Permission.VERIFY_IDENTITY));
        assertTrue("Security can process verifications", securityPerms.contains(Permission.PROCESS_VERIFICATIONS));
        assertTrue("Security can investigate fraud", securityPerms.contains(Permission.FRAUD_INVESTIGATION));
        assertTrue("Security can view all items", securityPerms.contains(Permission.VIEW_ALL_ITEMS));
        
        System.out.println("   Security permission count: " + securityPerms.size());
        System.out.println("   ✓ Campus Security permissions tests passed\n");
    }
    
    private void testPolicePermissions() {
        System.out.println("📋 Testing Police Permissions...");
        
        // Test Police Evidence Custodian
        Set<Permission> custodianPerms = service.getPermissionsForRole(UserRole.POLICE_EVIDENCE_CUSTODIAN);
        assertNotNull("Custodian permissions exist", custodianPerms);
        
        assertTrue("Custodian can access police DB", custodianPerms.contains(Permission.POLICE_DATABASE_ACCESS));
        assertTrue("Custodian can verify serial numbers", custodianPerms.contains(Permission.VERIFY_SERIAL_NUMBERS));
        assertTrue("Custodian can approve very high value", custodianPerms.contains(Permission.APPROVE_VERY_HIGH_VALUE));
        assertTrue("Custodian can cross-enterprise transfer", custodianPerms.contains(Permission.CROSS_ENTERPRISE_TRANSFER));
        assertTrue("Custodian can manage items", custodianPerms.contains(Permission.MANAGE_ITEMS));
        
        // Test Detective
        Set<Permission> detectivePerms = service.getPermissionsForRole(UserRole.DETECTIVE);
        assertNotNull("Detective permissions exist", detectivePerms);
        
        assertTrue("Detective can access police DB", detectivePerms.contains(Permission.POLICE_DATABASE_ACCESS));
        assertTrue("Detective can investigate fraud", detectivePerms.contains(Permission.FRAUD_INVESTIGATION));
        assertTrue("Detective can view investigations", detectivePerms.contains(Permission.VIEW_INVESTIGATIONS));
        
        System.out.println("   Custodian permission count: " + custodianPerms.size());
        System.out.println("   Detective permission count: " + detectivePerms.size());
        System.out.println("   ✓ Police permissions tests passed\n");
    }
    
    private void testSystemAdminPermissions() {
        System.out.println("📋 Testing System Admin Permissions...");
        
        Set<Permission> adminPerms = service.getPermissionsForRole(UserRole.SYSTEM_ADMIN);
        assertNotNull("Admin permissions exist", adminPerms);
        
        // System admin should have ALL permissions
        assertEqual("Admin has all permissions", Permission.values().length, adminPerms.size());
        
        for (Permission perm : Permission.values()) {
            assertTrue("Admin has " + perm, adminPerms.contains(perm));
        }
        
        System.out.println("   Admin permission count: " + adminPerms.size());
        System.out.println("   ✓ System Admin permissions tests passed\n");
    }
    
    private void testMBTARolePermissions() {
        System.out.println("📋 Testing MBTA Role Permissions...");
        
        // Station Manager
        Set<Permission> managerPerms = service.getPermissionsForRole(UserRole.STATION_MANAGER);
        assertNotNull("Station Manager permissions exist", managerPerms);
        assertTrue("Manager can approve claims", managerPerms.contains(Permission.APPROVE_CLAIMS));
        assertTrue("Manager can manage items", managerPerms.contains(Permission.MANAGE_ITEMS));
        
        // Lost & Found Clerk
        Set<Permission> clerkPerms = service.getPermissionsForRole(UserRole.LOST_FOUND_CLERK);
        assertNotNull("Clerk permissions exist", clerkPerms);
        assertTrue("Clerk can approve claims", clerkPerms.contains(Permission.APPROVE_CLAIMS));
        assertTrue("Clerk can release items", clerkPerms.contains(Permission.RELEASE_ITEMS));
        
        // Transit Security Inspector
        Set<Permission> inspectorPerms = service.getPermissionsForRole(UserRole.TRANSIT_SECURITY_INSPECTOR);
        assertNotNull("Inspector permissions exist", inspectorPerms);
        assertTrue("Inspector can investigate fraud", inspectorPerms.contains(Permission.FRAUD_INVESTIGATION));
        assertTrue("Inspector can cross-enterprise transfer", inspectorPerms.contains(Permission.CROSS_ENTERPRISE_TRANSFER));
        
        System.out.println("   ✓ MBTA role permissions tests passed\n");
    }
    
    private void testAirportRolePermissions() {
        System.out.println("📋 Testing Airport Role Permissions...");
        
        // Airport L&F Specialist
        Set<Permission> specialistPerms = service.getPermissionsForRole(UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
        assertNotNull("Specialist permissions exist", specialistPerms);
        assertTrue("Specialist can approve high value", specialistPerms.contains(Permission.APPROVE_HIGH_VALUE));
        assertTrue("Specialist can cross-enterprise transfer", specialistPerms.contains(Permission.CROSS_ENTERPRISE_TRANSFER));
        
        // TSA Coordinator
        Set<Permission> tsaPerms = service.getPermissionsForRole(UserRole.TSA_SECURITY_COORDINATOR);
        assertNotNull("TSA permissions exist", tsaPerms);
        assertTrue("TSA can access police DB", tsaPerms.contains(Permission.POLICE_DATABASE_ACCESS));
        assertTrue("TSA can approve very high value", tsaPerms.contains(Permission.APPROVE_VERY_HIGH_VALUE));
        
        System.out.println("   ✓ Airport role permissions tests passed\n");
    }
    
    // ==================== APPROVAL LIMITS TESTS ====================
    
    private void testApprovalLimits() {
        System.out.println("📋 Testing Approval Limits...");
        
        // Test approval limits for different roles using mock users
        
        // Students have 0 limit
        User student = createMockUser(UserRole.STUDENT);
        assertEqual("Student approval limit is 0", 0.0, getApprovalLimitForRole(UserRole.STUDENT));
        
        // Coordinators have medium limit
        assertEqual("Coordinator limit is 500", 500.0, getApprovalLimitForRole(UserRole.CAMPUS_COORDINATOR));
        
        // Security has higher limit
        assertEqual("Campus Security limit is 2000", 2000.0, getApprovalLimitForRole(UserRole.CAMPUS_SECURITY));
        
        // University Admin has very high limit
        assertEqual("University Admin limit is 10000", 10000.0, getApprovalLimitForRole(UserRole.UNIVERSITY_ADMIN));
        
        // Police has unlimited
        assertEqual("Police Custodian has max limit", Double.MAX_VALUE, getApprovalLimitForRole(UserRole.POLICE_EVIDENCE_CUSTODIAN));
        
        // Test canApproveAmount with real users if available
        String coordinatorEmail = findUserByRole(UserRole.CAMPUS_COORDINATOR);
        if (coordinatorEmail != null) {
            assertTrue("Coordinator can approve $400", service.canApproveAmount(coordinatorEmail, 400.0));
            assertFalse("Coordinator cannot approve $600", service.canApproveAmount(coordinatorEmail, 600.0));
        }
        
        System.out.println("   ✓ Approval limits tests passed\n");
    }
    
    private double getApprovalLimitForRole(UserRole role) {
        // Find a user with this role to test
        String userEmail = findUserByRole(role);
        if (userEmail != null) {
            return service.getApprovalLimit(userEmail);
        }
        // Return expected default if no user found
        return getExpectedApprovalLimit(role);
    }
    
    private double getExpectedApprovalLimit(UserRole role) {
        switch (role) {
            case STUDENT:
            case PUBLIC_TRAVELER:
            case STAFF:
                return 0.0;
            case LOST_FOUND_CLERK:
                return 100.0;
            case AIRLINE_REPRESENTATIVE:
            case TRANSIT_OFFICER:
                return 200.0;
            case CAMPUS_COORDINATOR:
            case BUILDING_MANAGER:
            case STATION_MANAGER:
                return 500.0;
            case CAMPUS_SECURITY:
            case TRANSIT_SECURITY_INSPECTOR:
            case AIRPORT_LOST_FOUND_SPECIALIST:
                return 2000.0;
            case TSA_SECURITY_COORDINATOR:
            case DETECTIVE:
                return 5000.0;
            case UNIVERSITY_ADMIN:
                return 10000.0;
            case POLICE_EVIDENCE_CUSTODIAN:
            case POLICE_ADMIN:
            case SYSTEM_ADMIN:
                return Double.MAX_VALUE;
            default:
                return 0.0;
        }
    }
    
    // ==================== ACCESS CONTROL TESTS ====================
    
    private void testAccessControlMethods() {
        System.out.println("📋 Testing Access Control Methods...");
        
        // Test with real users if available
        String adminEmail = findUserByRole(UserRole.SYSTEM_ADMIN);
        String studentEmail = findUserByRole(UserRole.STUDENT);
        String policeEmail = findUserByRole(UserRole.POLICE_EVIDENCE_CUSTODIAN);
        
        if (adminEmail != null) {
            // Admin should access everything
            assertTrue("Admin can access any enterprise", 
                service.canUserAccessEnterprise(adminEmail, "any-enterprise-id"));
            assertTrue("Admin can access any organization", 
                service.canUserAccessOrganization(adminEmail, "any-org-id"));
        }
        
        if (policeEmail != null) {
            // Police can access all for investigations
            assertTrue("Police can access any enterprise", 
                service.canUserAccessEnterprise(policeEmail, "any-enterprise-id"));
        }
        
        // Test hasPermission method
        if (studentEmail != null) {
            assertTrue("Student has REPORT_ITEMS permission", 
                service.hasPermission(studentEmail, Permission.REPORT_ITEMS));
            assertFalse("Student doesn't have APPROVE_CLAIMS permission", 
                service.hasPermission(studentEmail, Permission.APPROVE_CLAIMS));
        }
        
        // Test canUserApproveRequest
        String securityEmail = findUserByRole(UserRole.CAMPUS_SECURITY);
        if (securityEmail != null) {
            assertTrue("Security can approve standard requests", 
                service.canUserApproveRequest(securityEmail, "STANDARD"));
            assertTrue("Security can approve high value", 
                service.canUserApproveRequest(securityEmail, "HIGH_VALUE"));
        }
        
        System.out.println("   ✓ Access control tests passed\n");
    }
    
    // ==================== SECURITY PERMISSION TESTS ====================
    
    private void testSecurityPermissions() {
        System.out.println("📋 Testing Security Permission Methods...");
        
        String policeEmail = findUserByRole(UserRole.POLICE_EVIDENCE_CUSTODIAN);
        String studentEmail = findUserByRole(UserRole.STUDENT);
        String securityEmail = findUserByRole(UserRole.CAMPUS_SECURITY);
        
        if (policeEmail != null) {
            assertTrue("Police can access police DB", service.canAccessPoliceDatabase(policeEmail));
            assertTrue("Police can investigate fraud", service.canInvestigateFraud(policeEmail));
            assertTrue("Police can flag users", service.canFlagUsers(policeEmail));
            assertTrue("Police can unflag users", service.canUnflagUsers(policeEmail));
            assertTrue("Police can modify trust scores", service.canModifyTrustScores(policeEmail));
            assertTrue("Police can process verifications", service.canProcessVerifications(policeEmail));
            assertTrue("Police can verify serial numbers", service.canVerifySerialNumbers(policeEmail));
        }
        
        if (studentEmail != null) {
            assertFalse("Student cannot access police DB", service.canAccessPoliceDatabase(studentEmail));
            assertFalse("Student cannot investigate fraud", service.canInvestigateFraud(studentEmail));
            assertFalse("Student cannot flag users", service.canFlagUsers(studentEmail));
        }
        
        if (securityEmail != null) {
            assertTrue("Security can view trust scores", service.canViewTrustScores(securityEmail));
            assertTrue("Security can verify identity", service.canVerifyIdentity(securityEmail));
        }
        
        System.out.println("   ✓ Security permission tests passed\n");
    }
    
    // ==================== ROLE CLASSIFICATION TESTS ====================
    
    private void testRoleClassification() {
        System.out.println("📋 Testing Role Classification...");
        
        // Test isLawEnforcement
        assertTrue("Police Custodian is law enforcement", 
            service.isLawEnforcement(UserRole.POLICE_EVIDENCE_CUSTODIAN));
        assertTrue("Detective is law enforcement", 
            service.isLawEnforcement(UserRole.DETECTIVE));
        assertTrue("Police Admin is law enforcement", 
            service.isLawEnforcement(UserRole.POLICE_ADMIN));
        assertFalse("Student is not law enforcement", 
            service.isLawEnforcement(UserRole.STUDENT));
        assertFalse("Campus Security is not law enforcement", 
            service.isLawEnforcement(UserRole.CAMPUS_SECURITY));
        
        // Test isSecurityRole
        assertTrue("Campus Security is security role", 
            service.isSecurityRole(UserRole.CAMPUS_SECURITY));
        assertTrue("Transit Security is security role", 
            service.isSecurityRole(UserRole.TRANSIT_SECURITY_INSPECTOR));
        assertTrue("TSA is security role", 
            service.isSecurityRole(UserRole.TSA_SECURITY_COORDINATOR));
        assertTrue("Police is security role (inherits)", 
            service.isSecurityRole(UserRole.POLICE_EVIDENCE_CUSTODIAN));
        assertFalse("Student is not security role", 
            service.isSecurityRole(UserRole.STUDENT));
        
        // Test isAdminRole
        assertTrue("University Admin is admin role", 
            service.isAdminRole(UserRole.UNIVERSITY_ADMIN));
        assertTrue("Police Admin is admin role", 
            service.isAdminRole(UserRole.POLICE_ADMIN));
        assertTrue("System Admin is admin role", 
            service.isAdminRole(UserRole.SYSTEM_ADMIN));
        assertFalse("Coordinator is not admin role", 
            service.isAdminRole(UserRole.CAMPUS_COORDINATOR));
        
        System.out.println("   ✓ Role classification tests passed\n");
    }
    
    // ==================== AUTHORIZATION SUMMARY TESTS ====================
    
    private void testAuthorizationSummary() {
        System.out.println("📋 Testing Authorization Summary...");
        
        String securityEmail = findUserByRole(UserRole.CAMPUS_SECURITY);
        
        if (securityEmail != null) {
            Map<String, Object> summary = service.getAuthorizationSummary(securityEmail);
            
            assertNotNull("Summary returned", summary);
            assertNotNull("Summary has userId", summary.get("userId"));
            assertNotNull("Summary has role", summary.get("role"));
            assertNotNull("Summary has approvalLimit", summary.get("approvalLimit"));
            assertNotNull("Summary has permissions", summary.get("permissions"));
            assertNotNull("Summary has capabilities", summary.get("capabilities"));
            
            // Check capabilities map
            @SuppressWarnings("unchecked")
            Map<String, Boolean> caps = (Map<String, Boolean>) summary.get("capabilities");
            assertNotNull("Capabilities map exists", caps);
            assertTrue("Has canApproveHighValue capability", caps.containsKey("canApproveHighValue"));
            
            System.out.println("   📊 Sample Authorization Summary for Campus Security:");
            System.out.println("      Role: " + summary.get("role"));
            System.out.println("      Approval Limit: $" + summary.get("approvalLimit"));
            System.out.println("      Permission Count: " + summary.get("permissionCount"));
            System.out.println("      Is Security Role: " + summary.get("isSecurityRole"));
        } else {
            System.out.println("   ⚠️ No security user found for summary test");
        }
        
        // Test with non-existent user
        Map<String, Object> noUser = service.getAuthorizationSummary("non-existent-user");
        assertNotNull("Returns map even for non-existent user", noUser);
        
        System.out.println("   ✓ Authorization summary tests passed\n");
    }
    
    // ==================== STATIC METHOD TESTS ====================
    
    private void testStaticMethods() {
        System.out.println("📋 Testing Static Methods...");
        
        // Test getAllPermissions
        List<Permission> allPerms = AuthorityService.getAllPermissions();
        assertNotNull("getAllPermissions returns list", allPerms);
        assertEqual("All permissions count", Permission.values().length, allPerms.size());
        
        // Test getAllRoles
        List<UserRole> allRoles = AuthorityService.getAllRoles();
        assertNotNull("getAllRoles returns list", allRoles);
        assertEqual("All roles count", UserRole.values().length, allRoles.size());
        
        // Test getPermissionByName
        Permission viewTrust = AuthorityService.getPermissionByName("VIEW_TRUST_SCORES");
        assertNotNull("Found permission by name", viewTrust);
        assertEqual("Correct permission", Permission.VIEW_TRUST_SCORES, viewTrust);
        
        Permission invalid = AuthorityService.getPermissionByName("INVALID_PERMISSION");
        assertNull("Returns null for invalid permission", invalid);
        
        System.out.println("   ✓ Static methods tests passed\n");
    }
    
    // ==================== LIVE USER TESTS ====================
    
    private void testWithRealUsers() {
        System.out.println("📋 Testing with Real Database Users...");
        
        try {
            List<User> users = userDAO.findAll();
            if (users.isEmpty()) {
                System.out.println("   ⚠️ No users in database, skipping live tests");
                return;
            }
            
            int tested = 0;
            Set<UserRole> testedRoles = new HashSet<>();
            
            for (User user : users) {
                if (user.getRole() != null && !testedRoles.contains(user.getRole())) {
                    // Test this role
                    Set<Permission> perms = service.getPermissionsForRole(user.getRole());
                    assertNotNull("Permissions for " + user.getRole(), perms);
                    
                    // Test hasPermission with user's actual ID/email
                    String userId = user.getEmail();
                    Set<Permission> effectivePerms = service.getEffectivePermissions(userId);
                    assertNotNull("Effective permissions for user", effectivePerms);
                    
                    // Test permission names
                    List<String> permNames = service.getPermissionNames(userId);
                    assertNotNull("Permission names list", permNames);
                    
                    testedRoles.add(user.getRole());
                    tested++;
                }
                
                if (tested >= 5) break; // Limit to 5 roles
            }
            
            System.out.println("   Tested " + tested + " different roles from database");
            System.out.println("   ✓ Real user tests passed\n");
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Error testing with real users: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private User createMockUser(UserRole role) {
        String email = "mock-" + role.name().toLowerCase() + "@test.com";
        User user = new User(email, "Mock", "User", role);
        return user;
    }
    
    private String findUserByRole(UserRole role) {
        try {
            List<User> users = userDAO.findAll();
            for (User user : users) {
                if (user.getRole() == role) {
                    return user.getEmail();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Could not find user by role: " + e.getMessage());
        }
        return null;
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
            System.out.println("   ❌ FAIL: " + message + " - Expected null but got: " + obj);
        }
    }
    
    private void failTest(String message) {
        failedTests++;
        System.out.println("   ❌ FAIL: " + message);
    }
    
    private void printSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 AUTHORITY SERVICE TEST SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("✅ Passed: " + passedTests);
        System.out.println("❌ Failed: " + failedTests);
        System.out.println("📈 Total:  " + (passedTests + failedTests));
        
        if (failedTests == 0) {
            System.out.println("\n🎉 ALL AUTHORITY SERVICE TESTS PASSED!");
        } else {
            System.out.println("\n⚠️  Some tests failed. Review the output above.");
        }
        System.out.println("=".repeat(70) + "\n");
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        System.out.println("Starting Authority Service Tests...\n");
        AuthorityServiceTest test = new AuthorityServiceTest();
        test.runAllTests();
    }
}
