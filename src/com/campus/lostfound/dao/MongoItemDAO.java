/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.campus.lostfound.models.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author aksha
 */
public class MongoItemDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoItemDAO.class.getName());
    private final MongoCollection<Document> itemsCollection;
    private final MongoCollection<Document> usersCollection;
    private final MongoCollection<Document> buildingsCollection;

    public MongoItemDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.itemsCollection = connection.getCollection("items");
        this.usersCollection = connection.getCollection("users");
        this.buildingsCollection = connection.getCollection("buildings");
    }

    public String create(Item item) {
        try {
            Document doc = itemToDocument(item);
            itemsCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            LOGGER.info("Item created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating item", e);
            return null;
        }
    }

    public Optional<Item> findById(String id) {
        try {
            Document doc = itemsCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToItem(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding item by ID: " + id, e);
        }
        return Optional.empty();
    }

    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        try {
            FindIterable<Document> documents = itemsCollection.find()
                    .sort(Sorts.descending("reportedDate"));

            for (Document doc : documents) {
                items.add(documentToItem(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all items", e);
        }
        return items;
    }

    public List<Item> findByStatus(Item.ItemStatus status) {
        List<Item> items = new ArrayList<>();
        try {
            Bson filter = status != null
                    ? Filters.eq("status", status.name())
                    : Filters.in("status", "OPEN", "PENDING_CLAIM", "VERIFIED");

            FindIterable<Document> documents = itemsCollection.find(filter)
                    .sort(Sorts.descending("reportedDate"));

            for (Document doc : documents) {
                items.add(documentToItem(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding items by status", e);
        }
        return items;
    }

    public List<Item> findByUser(String email) {
        List<Item> items = new ArrayList<>();
        try {
            FindIterable<Document> documents = itemsCollection.find(
                    Filters.eq("reportedBy.email", email))
                    .sort(Sorts.descending("reportedDate"));

            for (Document doc : documents) {
                items.add(documentToItem(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding items by user", e);
        }
        return items;
    }

    /**
     * Find items reported by a specific user (by userId)
     */
    public List<Item> findByReporter(String reporterMongoId) {
        List<Item> items = new ArrayList<>();
        try {
            FindIterable<Document> documents = itemsCollection.find(
                    Filters.eq("reportedBy.userId", reporterMongoId))
                    .sort(Sorts.descending("reportedDate"));

            for (Document doc : documents) {
                try {
                    items.add(documentToItem(doc));
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error parsing item document", ex);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding items by reporter", e);
        }
        return items;
    }

    public List<Item> searchItems(String query, Item.ItemType type, Item.ItemCategory category) {
        List<Item> items = new ArrayList<>();
        try {
            List<Bson> filters = new ArrayList<>();

            // Status filter
            filters.add(Filters.in("status", "OPEN", "PENDING_CLAIM", "VERIFIED"));

            // Text search
            if (query != null && !query.trim().isEmpty()) {
                filters.add(Filters.text(query));
            }

            // Type filter
            if (type != null) {
                filters.add(Filters.eq("type", type.name()));
            }

            // Category filter
            if (category != null) {
                filters.add(Filters.eq("category", category.name()));
            }

            Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);

            FindIterable<Document> documents = itemsCollection.find(filter)
                    .sort(Sorts.descending("reportedDate"));

            for (Document doc : documents) {
                items.add(documentToItem(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching items", e);
        }
        return items;
    }

    public boolean update(Item item) {
        try {
            Document doc = itemToDocument(item);
            doc.remove("_id"); // Don't update the ID

            // 🔥 FIX: Use mongoId (actual ObjectId string) instead of itemId (hashCode)
            if (item.getMongoId() == null || item.getMongoId().isEmpty()) {
                LOGGER.severe("Cannot update item - mongoId is null or empty");
                return false;
            }
            
            itemsCollection.updateOne(
                    Filters.eq("_id", new ObjectId(item.getMongoId())),
                    new Document("$set", doc)
            );

            LOGGER.info("Item updated: " + item.getMongoId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating item", e);
            return false;
        }
    }

    public boolean delete(String id) {
        try {
            itemsCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            LOGGER.info("Item deleted: " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting item", e);
            return false;
        }
    }

    public boolean deleteByTitleAndUser(String title, String email) {
        try {
            // Find and delete the item
            Bson filter = Filters.and(
                    Filters.eq("title", title),
                    Filters.eq("reportedBy.email", email)
            );

            itemsCollection.deleteOne(filter);
            LOGGER.info("Item deleted: " + title);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting item", e);
            return false;
        }
    }

    // Helper methods for conversion
    private Document itemToDocument(Item item) {
        Document doc = new Document()
                .append("title", item.getTitle())
                .append("description", item.getDescription())
                .append("category", item.getCategory().name())
                .append("type", item.getType().name())
                .append("status", item.getStatus().name())
                .append("primaryColor", item.getPrimaryColor())
                .append("brand", item.getBrand())
                .append("reportedDate", item.getReportedDate())
                .append("resolvedDate", item.getResolvedDate())
                .append("viewCount", 0)
                .append("imagePaths", item.getImagePaths())
                .append("keywords", item.getKeywords())
                .append("enterpriseId", item.getEnterpriseId())
                .append("organizationId", item.getOrganizationId());

        // Embed location
        Location loc = item.getLocation();
        if (loc != null) {
            doc.append("location", new Document()
                    .append("building", new Document()
                            .append("id", loc.getBuilding().getBuildingId())
                            .append("name", loc.getBuilding().getName())
                            .append("code", loc.getBuilding().getCode())
                            .append("type", loc.getBuilding().getType().name()))
                    .append("roomNumber", loc.getRoomNumber())
                    .append("floor", loc.getFloor())
                    .append("specificLocation", loc.getSpecificLocation())
                    .append("latitude", loc.getLatitude())
                    .append("longitude", loc.getLongitude()));
        }

        // Embed reporter info
        User reporter = item.getReportedBy();
        if (reporter != null) {
            doc.append("reportedBy", new Document()
                    .append("userId", reporter.getUserId() + "")
                    .append("email", reporter.getEmail())
                    .append("firstName", reporter.getFirstName())
                    .append("lastName", reporter.getLastName())
                    .append("trustScore", reporter.getTrustScore()));
        }

        return doc;
    }

    private Item documentToItem(Document doc) {
        // Extract location (handle null gracefully)
        Location location = null;
        Document locDoc = doc.get("location", Document.class);
        
        if (locDoc != null) {
            Document buildingDoc = locDoc.get("building", Document.class);
            
            if (buildingDoc != null) {
                Building building = new Building(
                        buildingDoc.getString("name"),
                        buildingDoc.getString("code"),
                        Building.BuildingType.valueOf(buildingDoc.getString("type"))
                );
                building.setBuildingId(buildingDoc.getInteger("id", 0));

                location = new Location(
                        building,
                        locDoc.getString("roomNumber"),
                        locDoc.getString("specificLocation")
                );
                location.setFloor(locDoc.getString("floor"));
                location.setLatitude(locDoc.getDouble("latitude"));
                location.setLongitude(locDoc.getDouble("longitude"));
            }
        }

        // Extract reporter
        Document userDoc = doc.get("reportedBy", Document.class);
        User reporter = new User(
                userDoc.getString("email"),
                userDoc.getString("firstName"),
                userDoc.getString("lastName"),
                User.UserRole.STUDENT
        );
        reporter.setUserId(Integer.parseInt(userDoc.getString("userId")));
        reporter.setTrustScore(userDoc.getDouble("trustScore"));

        // Create item
        Item item = new Item(
                doc.getString("title"),
                doc.getString("description"),
                Item.ItemCategory.valueOf(doc.getString("category")),
                Item.ItemType.valueOf(doc.getString("type")),
                location,
                reporter
        );

        item.setItemId(doc.getObjectId("_id").toString().hashCode()); // Use hash for int ID
        item.setMongoId(doc.getObjectId("_id").toString()); // 🔥 Store actual MongoDB ObjectId for updates
        item.setStatus(Item.ItemStatus.valueOf(doc.getString("status")));
        item.setPrimaryColor(doc.getString("primaryColor"));
        item.setBrand(doc.getString("brand"));
        item.setReportedDate(doc.getDate("reportedDate"));
        item.setResolvedDate(doc.getDate("resolvedDate"));

        // Set image paths
        List<String> imagePaths = doc.getList("imagePaths", String.class);
        if (imagePaths != null) {
            imagePaths.forEach(item::addImagePath);
        }
        
        // Set keywords - CRITICAL: must restore keywords from database!
        List<String> keywords = doc.getList("keywords", String.class);
        if (keywords != null) {
            item.setKeywords(new ArrayList<>(keywords));
        }
        
        // Set enterprise context
        item.setEnterpriseId(doc.getString("enterpriseId"));
        item.setOrganizationId(doc.getString("organizationId"));

        return item;
    }
}
