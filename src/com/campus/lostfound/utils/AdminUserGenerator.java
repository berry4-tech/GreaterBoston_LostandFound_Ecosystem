package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import java.util.List;

/**
 * Minimal generator - ONLY creates MBTA_ADMIN and AIRPORT_ADMIN users. Does NOT
 * delete or modify any existing data.
 */
public class AdminUserGenerator {

    public static void main(String[] args) {
        System.out.println("🔧 Adding Admin Users (no data wipe)...\n");

        MongoEnterpriseDAO enterpriseDAO = new MongoEnterpriseDAO();
        MongoOrganizationDAO organizationDAO = new MongoOrganizationDAO();
        MongoUserDAO userDAO = new MongoUserDAO();

        // Find MBTA Enterprise
        List<Enterprise> allEnterprises = enterpriseDAO.findAll();

        Enterprise mbtaEnterprise = allEnterprises.stream()
                .filter(e -> e.getType() == Enterprise.EnterpriseType.PUBLIC_TRANSIT)
                .findFirst()
                .orElse(null);

        Enterprise airportEnterprise = allEnterprises.stream()
                .filter(e -> e.getType() == Enterprise.EnterpriseType.AIRPORT)
                .findFirst()
                .orElse(null);

        if (mbtaEnterprise == null || airportEnterprise == null) {
            System.out.println("❌ Could not find MBTA or Airport enterprise!");
            return;
        }

        // Get first organization for each enterprise
        List<Organization> mbtaOrgs = organizationDAO.findByEnterpriseId(mbtaEnterprise.getEnterpriseId());
        List<Organization> airportOrgs = organizationDAO.findByEnterpriseId(airportEnterprise.getEnterpriseId());

        if (mbtaOrgs.isEmpty() || airportOrgs.isEmpty()) {
            System.out.println("❌ Could not find organizations!");
            return;
        }

        // Create MBTA Admin
        User mbtaAdmin = new User("admin@mbta.com", "MBTA", "Administrator", User.UserRole.MBTA_ADMIN);
        mbtaAdmin.setEnterpriseId(mbtaEnterprise.getEnterpriseId());
        mbtaAdmin.setOrganizationId(mbtaOrgs.get(0).getOrganizationId());
        mbtaAdmin.setPhoneNumber("617-555-0002");
        mbtaAdmin.setTrustScore(100.0);
        userDAO.create(mbtaAdmin, "test123");
        System.out.println("✅ Created MBTA_ADMIN: admin@mbta.com / test123123");

        // Create Airport Admin
        User airportAdmin = new User("admin@massport.com", "Airport", "Administrator", User.UserRole.AIRPORT_ADMIN);
        airportAdmin.setEnterpriseId(airportEnterprise.getEnterpriseId());
        airportAdmin.setOrganizationId(airportOrgs.get(0).getOrganizationId());
        airportAdmin.setPhoneNumber("617-555-0003");
        airportAdmin.setTrustScore(100.0);
        userDAO.create(airportAdmin, "test123");
        System.out.println("✅ Created AIRPORT_ADMIN: admin@massport.com / test123123");

        System.out.println("\n🎉 Done! Two admin users added without affecting existing data.");
    }
}
