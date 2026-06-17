package com.campus.lostfound.models;

import java.util.Date;

/**
 * Organization - Sub-units within each Enterprise
 * Examples: NEU Campus Operations, MBTA Station Operations, Airport Security
 */
public class Organization {
    
    private String organizationId;
    private String enterpriseId;
    private String name;
    private OrganizationType type;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Date createdDate;
    private boolean isActive;
    
    public enum OrganizationType {
        // Higher Education Organizations
        CAMPUS_OPERATIONS("Campus Operations", "Campus-wide lost and found coordination"),
        STUDENT_SERVICES("Student Services", "Student support and services"),
        CAMPUS_SECURITY("Campus Security", "Campus safety and security operations"),
        
        // MBTA Organizations
        STATION_OPERATIONS("Station Operations", "Station-level lost and found management"),
        TRANSIT_POLICE("Transit Police", "Transit law enforcement and security"),
        CENTRAL_LOST_FOUND("Central Lost & Found", "Centralized MBTA lost and found depot"),
        
        // Airport Organizations
        AIRPORT_OPERATIONS("Airport Operations", "Airport-wide operations and coordination"),
        TSA_SECURITY("TSA Security", "Transportation Security Administration"),
        AIRLINE_SERVICES("Airline Services", "Individual airline lost and found"),
        
        // Law Enforcement Organizations
        POLICE_DEPARTMENT("Police Department", "Municipal police services"),
        EVIDENCE_MANAGEMENT("Evidence Management", "Evidence and property management"),
        DETECTIVE_BUREAU("Detective Bureau", "Investigative services");
        
        private final String displayName;
        private final String description;
        
        OrganizationType(String displayName, String description) {
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
    
    public Organization() {
        this.createdDate = new Date();
        this.isActive = true;
    }
    
    public Organization(String organizationId, String enterpriseId, String name, OrganizationType type) {
        this.organizationId = organizationId;
        this.enterpriseId = enterpriseId;
        this.name = name;
        this.type = type;
        this.createdDate = new Date();
        this.isActive = true;
    }
    
    // Getters and Setters
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getEnterpriseId() {
        return enterpriseId;
    }
    
    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public OrganizationType getType() {
        return type;
    }
    
    public void setType(OrganizationType type) {
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public Date getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
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
        Organization that = (Organization) obj;
        return organizationId != null && organizationId.equals(that.organizationId);
    }
    
    @Override
    public int hashCode() {
        return organizationId != null ? organizationId.hashCode() : 0;
    }
}
