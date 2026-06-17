/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

/**
 *
 * @author aksha
 */
public class Building {

    private int buildingId;
    private String name;
    private String code; // Building code like "SL" for Snell Library
    private String address;
    private BuildingType type;
    private User coordinator; // Building coordinator
    
    // Enterprise Context
    private String enterpriseId;
    private String organizationId;

    public enum BuildingType {
        ACADEMIC("Academic"),
        LIBRARY("Library"),
        RESIDENTIAL("Residential"),
        DINING("Dining"),
        ATHLETIC("Athletic"),
        ADMINISTRATIVE("Administrative"),
        STUDENT_CENTER("Student Center"),
        // Transit Types (MBTA)
        TRANSIT_HUB("Transit Hub"),
        SUBWAY_STATION("Subway Station"),
        BUS_STATION("Bus Station"),
        COMMUTER_RAIL("Commuter Rail Station"),
        FERRY_TERMINAL("Ferry Terminal"),
        // Airport Types
        AIRPORT_TERMINAL("Airport Terminal"),
        AIRPORT_SECURITY("Airport Security"),
        BAGGAGE_CLAIM("Baggage Claim"),
        // Police Types
        POLICE_STATION("Police Station"),
        EVIDENCE_STORAGE("Evidence Storage");

        private String displayName;

        BuildingType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Building(String name, String code, BuildingType type) {
        this.name = name;
        this.code = code;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Building building = (Building) obj;
        return buildingId == building.buildingId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(buildingId);
    }

    // Getters and setters
    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BuildingType getType() {
        return type;
    }

    public void setType(BuildingType type) {
        this.type = type;
    }

    public User getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(User coordinator) {
        this.coordinator = coordinator;
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
}
