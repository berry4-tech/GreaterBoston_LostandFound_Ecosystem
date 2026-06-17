/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author aksha
 */
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import javax.swing.ImageIcon;

public class Item {

    private int itemId;
    private String mongoId; // Store actual MongoDB ObjectId for updates
    private String title;
    private String description;
    private ItemCategory category;
    private ItemStatus status;
    private ItemType type; // LOST or FOUND
    private Location location;
    private Date reportedDate;
    private Date resolvedDate;
    private User reportedBy;
    private User claimedBy;
    private List<String> imagePaths;
    private List<String> keywords; // For matching algorithm
    private String primaryColor;
    private String brand;
    private double matchScore; // Used when displaying potential matches
    
    // Enterprise Context
    private String enterpriseId;
    private String organizationId;
    
    // Security & Verification fields (Developer 3)
    private String serialNumber;
    private double estimatedValue;

    public enum ItemType {
        LOST("Lost", "❌"),
        FOUND("Found", "✅");

        private String label;
        private String icon;

        ItemType(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    public enum ItemStatus {
        OPEN("Open", "#4CAF50"),
        PENDING_CLAIM("Pending Claim", "#FF9800"),
        VERIFIED("Verified", "#2196F3"),
        CLAIMED("Claimed", "#9C27B0"),
        CANCELLED("Cancelled", "#9E9E9E"),
        EXPIRED("Expired", "#757575");

        private String label;
        private String colorCode;

        ItemStatus(String label, String colorCode) {
            this.label = label;
            this.colorCode = colorCode;
        }

        public String getLabel() {
            return label;
        }

        public String getColorCode() {
            return colorCode;
        }
    }

    public enum ItemCategory {
        ELECTRONICS("Electronics", "💻"),
        BOOKS("Books & Notebooks", "📚"),
        CLOTHING("Clothing & Accessories", "👕"),
        IDS_CARDS("IDs & Cards", "🆔"),
        KEYS("Keys", "🔑"),
        BAGS("Bags & Backpacks", "🎒"),
        JEWELRY("Jewelry & Watches", "💍"),
        SPORTS("Sports Equipment", "⚽"),
        UMBRELLAS("Umbrellas", "☂️"),
        BOTTLES("Water Bottles", "🍶"),
        OTHER("Other", "📦");

        private String displayName;
        private String emoji;

        ItemCategory(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    // Constructor for new item
    public Item(String title, String description, ItemCategory category,
            ItemType type, Location location, User reportedBy) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.type = type;
        this.location = location;
        this.reportedBy = reportedBy;
        this.reportedDate = new Date();
        this.status = ItemStatus.OPEN;
        this.imagePaths = new ArrayList<>();
        this.keywords = extractKeywords(title + " " + description);
    }

    // Extract keywords for matching algorithm
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            // Remove common words
            if (word.length() > 2 && !isStopWord(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    private boolean isStopWord(String word) {
        String[] stopWords = {"the", "and", "or", "but", "in", "on", "at", "to", "for"};
        for (String stop : stopWords) {
            if (word.equals(stop)) {
                return true;
            }
        }
        return false;
    }

    // Getters and setters
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(Date reportedDate) {
        this.reportedDate = reportedDate;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void addImagePath(String path) {
        this.imagePaths.add(path);
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public User getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(User claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Date getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(Date resolvedDate) {
        this.resolvedDate = resolvedDate;
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
    
    // Security & Verification getters/setters (Developer 3)
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public double getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    /**
     * Convenience method to get name (alias for title)
     * Used by VerificationService
     */
    public String getName() {
        return title;
    }
}
