/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

import java.util.Date;

/**
 *
 * @author aksha
 */
public class User {

    private int userId;
    private String mongoId;  // Actual MongoDB ObjectId string for DB lookups
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private double trustScore;
    private int itemsReported;
    private int itemsReturned;
    private int falseClaims;
    private Date joinDate;
    private Building primaryBuilding;
    
    // Enterprise Context
    private String enterpriseId;
    private String organizationId;
    
    // Account status
    private boolean active = true;

    public enum UserRole {
        // Higher Education Roles
        STUDENT("Student"),
        STAFF("Staff"),
        CAMPUS_COORDINATOR("Campus Coordinator"),
        BUILDING_MANAGER("Building Manager"),
        CAMPUS_SECURITY("Campus Security"),
        UNIVERSITY_ADMIN("University Administrator"),
        
        // MBTA Roles
        STATION_MANAGER("Station Manager"),
        LOST_FOUND_CLERK("Lost & Found Clerk"),
        TRANSIT_SECURITY_INSPECTOR("Transit Security Inspector"),
        TRANSIT_OFFICER("Transit Officer"),
        MBTA_ADMIN("MBTA Administrator"),
        
        // Airport Roles
        AIRPORT_LOST_FOUND_SPECIALIST("Airport Lost & Found Specialist"),
        TSA_SECURITY_COORDINATOR("TSA Security Coordinator"),
        AIRLINE_REPRESENTATIVE("Airline Representative"),
        AIRPORT_ADMIN("Airport Administrator"),
        
        // Law Enforcement Roles
        POLICE_EVIDENCE_CUSTODIAN("Police Evidence Custodian"),
        DETECTIVE("Detective"),
        POLICE_ADMIN("Police Administrator"),
        
        // Public/General Roles
        PUBLIC_TRAVELER("Public Traveler"),
        SYSTEM_ADMIN("System Administrator");

        private String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * No-arg constructor for flexibility (used by tests and frameworks)
     */
    public User() {
        this.trustScore = 100.0;
        this.itemsReported = 0;
        this.itemsReturned = 0;
        this.falseClaims = 0;
        this.joinDate = new Date();
        this.active = true;
    }

    public User(String email, String firstName, String lastName, UserRole role) {
        this();
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Calculate trust score based on user behavior
    public void recalculateTrustScore() {
        double baseScore = 100.0;

        // Positive actions
        baseScore += itemsReturned * 10; // +10 for each successful return
        baseScore += itemsReported * 2;  // +2 for reporting items

        // Negative actions
        baseScore -= falseClaims * 25;   // -25 for each false claim

        // Normalize between 0 and 100
        this.trustScore = Math.max(0, Math.min(100, baseScore));
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean canClaimHighValueItems() {
        return trustScore >= 75.0;
    }

    /**
     * Get trust level category
     */
    public String getTrustLevel() {
        if (trustScore >= 75) return "EXCELLENT";
        if (trustScore >= 50) return "GOOD";
        if (trustScore >= 25) return "POOR";
        return "SUSPENDED";
    }

    /**
     * Get trust level color for UI
     */
    public java.awt.Color getTrustLevelColor() {
        if (trustScore >= 75) return new java.awt.Color(76, 175, 80);  // Green
        if (trustScore >= 50) return new java.awt.Color(255, 193, 7);   // Yellow
        if (trustScore >= 25) return new java.awt.Color(255, 152, 0);   // Orange
        return new java.awt.Color(244, 67, 54);                          // Red
    }

    /**
     * Check if user can claim a specific item category based on trust score
     */
    public boolean canClaimItem(Item.ItemCategory category) {
        // Suspended - cannot claim anything
        if (trustScore < 25) {
            return false;
        }
        
        // Poor trust (25-49) - only low-value items
        if (trustScore < 50) {
            return category == Item.ItemCategory.BOOKS ||
                   category == Item.ItemCategory.UMBRELLAS ||
                   category == Item.ItemCategory.BOTTLES ||
                   category == Item.ItemCategory.CLOTHING;
        }
        
        // Good trust (50-74) - cannot claim high-value items
        if (trustScore < 75) {
            return category != Item.ItemCategory.ELECTRONICS &&
                   category != Item.ItemCategory.JEWELRY &&
                   category != Item.ItemCategory.IDS_CARDS &&
                   category != Item.ItemCategory.KEYS;
        }
        
        // Excellent (75+) - can claim anything
        return true;
    }

    /**
     * Check if user can submit any claims at all
     */
    public boolean canSubmitClaims() {
        return trustScore >= 25; // Below 25 = account suspended
    }

    /**
     * Check if claims require admin approval
     */
    public boolean requiresAdminApproval() {
        return trustScore < 50;
    }

    /**
     * Get restriction message for user
     */
    public String getRestrictionMessage() {
        if (trustScore < 25) {
            return "Account suspended. You cannot submit claims. Contact admin to appeal.";
        } else if (trustScore < 50) {
            return "Restricted access. Can only claim low-value items. Claims require admin approval.";
        } else if (trustScore < 75) {
            return "Limited access. Cannot claim high-value items (electronics, jewelry, IDs, keys).";
        }
        return "Full access. No restrictions.";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User user = (User) obj;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }

    // Getters and setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public int getItemsReported() {
        return itemsReported;
    }

    public void setItemsReported(int itemsReported) {
        this.itemsReported = itemsReported;
    }

    public int getItemsReturned() {
        return itemsReturned;
    }

    public void setItemsReturned(int itemsReturned) {
        this.itemsReturned = itemsReturned;
    }

    public int getFalseClaims() {
        return falseClaims;
    }

    public void setFalseClaims(int falseClaims) {
        this.falseClaims = falseClaims;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public Building getPrimaryBuilding() {
        return primaryBuilding;
    }

    public void setPrimaryBuilding(Building primaryBuilding) {
        this.primaryBuilding = primaryBuilding;
    }
    
    public String getEnterpriseId() {
        return enterpriseId;
    }
    
    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}
