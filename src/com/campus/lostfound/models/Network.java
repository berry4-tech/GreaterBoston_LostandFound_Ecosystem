package com.campus.lostfound.models;

import java.util.Date;

/**
 * Network - Top level container for the entire Greater Boston Lost & Found Ecosystem
 * Represents the collaborative network connecting all enterprises
 */
public class Network {
    
    private String networkId;
    private String name;
    private String description;
    private Date createdDate;
    private boolean isActive;
    
    public Network() {
        this.createdDate = new Date();
        this.isActive = true;
    }
    
    public Network(String networkId, String name, String description) {
        this.networkId = networkId;
        this.name = name;
        this.description = description;
        this.createdDate = new Date();
        this.isActive = true;
    }
    
    // Getters and Setters
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
        return name;
    }
}
