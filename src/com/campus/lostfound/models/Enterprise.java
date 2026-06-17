package com.campus.lostfound.models;

import java.util.Date;

/**
 * Enterprise - Represents major participating organizations in the network
 * Examples: Universities, MBTA, Logan Airport, Boston Police
 */
public class Enterprise {
    
    private String enterpriseId;
    private String networkId;
    private String name;
    private EnterpriseType type;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private Date joinedDate;
    private boolean isActive;
    
    public enum EnterpriseType {
        HIGHER_EDUCATION("Higher Education", "University and College Campuses"),
        PUBLIC_TRANSIT("Public Transit", "MBTA Transportation Network"),
        AIRPORT("Airport", "Logan International Airport"),
        LAW_ENFORCEMENT("Law Enforcement", "Police and Security Services");
        
        private final String displayName;
        private final String description;
        
        EnterpriseType(String displayName, String description) {
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
    
    public Enterprise() {
        this.joinedDate = new Date();
        this.isActive = true;
    }
    
    public Enterprise(String enterpriseId, String networkId, String name, EnterpriseType type) {
        this.enterpriseId = enterpriseId;
        this.networkId = networkId;
        this.name = name;
        this.type = type;
        this.joinedDate = new Date();
        this.isActive = true;
    }
    
    // Getters and Setters
    public String getEnterpriseId() {
        return enterpriseId;
    }
    
    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }
    
    public String getNetworkId() {
        return networkId;
    }
    
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public EnterpriseType getType() {
        return type;
    }
    
    public void setType(EnterpriseType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public Date getJoinedDate() {
        return joinedDate;
    }
    
    public void setJoinedDate(Date joinedDate) {
        this.joinedDate = joinedDate;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return name + " (" + type.getDisplayName() + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Enterprise that = (Enterprise) obj;
        return enterpriseId != null && enterpriseId.equals(that.enterpriseId);
    }
    
    @Override
    public int hashCode() {
        return enterpriseId != null ? enterpriseId.hashCode() : 0;
    }
}
