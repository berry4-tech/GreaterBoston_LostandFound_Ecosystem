/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.campus.lostfound.models.Building;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.Sorts;
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
public class MongoBuildingDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoBuildingDAO.class.getName());
    private final MongoCollection<Document> buildingsCollection;

    public MongoBuildingDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.buildingsCollection = connection.getCollection("buildings");

        // Ensure default buildings exist
        ensureDefaultBuildings();
    }

    /**
     * Create a new building
     */
    public String create(Building building) {
        try {
            Document doc = buildingToDocument(building);
            buildingsCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            LOGGER.info("Building created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating building", e);
            return null;
        }
    }

    /**
     * Find building by MongoDB ObjectId
     */
    public Optional<Building> findById(String id) {
        try {
            Document doc = buildingsCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding building by ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Find building by code (e.g., "SL" for Snell Library)
     */
    public Optional<Building> findByCode(String code) {
        try {
            Document doc = buildingsCollection.find(Filters.eq("code", code)).first();
            if (doc != null) {
                return Optional.of(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding building by code: " + code, e);
        }
        return Optional.empty();
    }

    /**
     * Find building by name
     */
    public Optional<Building> findByName(String name) {
        try {
            Document doc = buildingsCollection.find(Filters.eq("name", name)).first();
            if (doc != null) {
                return Optional.of(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding building by name: " + name, e);
        }
        return Optional.empty();
    }

    /**
     * Get all buildings
     */
    public List<Building> findAll() {
        List<Building> buildings = new ArrayList<>();
        try {
            for (Document doc : buildingsCollection.find().sort(Sorts.ascending("name"))) {
                buildings.add(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all buildings", e);
        }
        return buildings;
    }

    /**
     * Get buildings by type
     */
    public List<Building> findByType(Building.BuildingType type) {
        List<Building> buildings = new ArrayList<>();
        try {
            Bson filter = Filters.eq("type", type.name());
            for (Document doc : buildingsCollection.find(filter).sort(Sorts.ascending("name"))) {
                buildings.add(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding buildings by type", e);
        }
        return buildings;
    }

    /**
     * Update building information
     */
    public boolean update(Building building) {
        try {
            Document doc = buildingToDocument(building);
            doc.remove("_id"); // Don't update the ID

            Bson filter = Filters.eq("code", building.getCode());
            buildingsCollection.updateOne(filter, new Document("$set", doc));

            LOGGER.info("Building updated: " + building.getCode());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating building", e);
            return false;
        }
    }

    /**
     * Set building coordinator
     */
    public boolean setCoordinator(String buildingCode, User coordinator) {
        try {
            Bson filter = Filters.eq("code", buildingCode);
            Bson update = Updates.combine(
                    Updates.set("coordinatorId", coordinator.getUserId()),
                    Updates.set("coordinatorName", coordinator.getFullName()),
                    Updates.set("coordinatorEmail", coordinator.getEmail())
            );

            buildingsCollection.updateOne(filter, update);
            LOGGER.info("Coordinator set for building: " + buildingCode);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting coordinator", e);
            return false;
        }
    }

    /**
     * Delete all buildings (used by data generator to clear before regenerating)
     */
    public void deleteAll() {
        try {
            long count = buildingsCollection.countDocuments();
            buildingsCollection.deleteMany(new Document());
            LOGGER.info("Deleted " + count + " buildings");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting all buildings", e);
        }
    }

    /**
     * Delete a building (not recommended - buildings are reference data)
     */
    public boolean delete(String id) {
        try {
            // Check if building is referenced by any items
            MongoItemDAO itemDAO = new MongoItemDAO();
            List<Item> allItems = itemDAO.findAll();

            for (Item item : allItems) {
                if (item.getLocation() != null
                        && item.getLocation().getBuilding() != null
                        && item.getLocation().getBuilding().getBuildingId() == id.hashCode()) {
                    LOGGER.warning("Cannot delete building - referenced by items");
                    return false;
                }
            }

            buildingsCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            LOGGER.info("Building deleted: " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting building", e);
            return false;
        }
    }

    /**
     * Count total buildings
     */
    public long count() {
        try {
            return buildingsCollection.countDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting buildings", e);
            return 0;
        }
    }

    /**
     * Count buildings by type
     */
    public long countByType(Building.BuildingType type) {
        try {
            return buildingsCollection.countDocuments(Filters.eq("type", type.name()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting buildings by type", e);
            return 0;
        }
    }

    /**
     * Search buildings by partial name or code
     */
    public List<Building> search(String query) {
        List<Building> buildings = new ArrayList<>();
        try {
            Bson filter = Filters.or(
                    Filters.regex("name", ".*" + query + ".*", "i"),
                    Filters.regex("code", ".*" + query + ".*", "i")
            );

            for (Document doc : buildingsCollection.find(filter)) {
                buildings.add(documentToBuilding(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching buildings", e);
        }
        return buildings;
    }

    /**
     * Get nearby buildings (within a certain distance) Requires buildings to
     * have latitude/longitude stored
     */
    public List<Building> findNearby(double latitude, double longitude, double maxDistanceKm) {
        List<Building> buildings = new ArrayList<>();
        try {
            // MongoDB geospatial query would go here if we had coordinates
            // For now, return all buildings
            buildings = findAll();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding nearby buildings", e);
        }
        return buildings;
    }

    /**
     * Ensure default buildings exist in database
     */
    private void ensureDefaultBuildings() {
        try {
            // Check if buildings collection is empty
            if (buildingsCollection.countDocuments() == 0) {
                insertDefaultBuildings();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking default buildings", e);
        }
    }

    /**
     * Insert default Northeastern University buildings
     */
    private void insertDefaultBuildings() {
        List<Document> defaultBuildings = Arrays.asList(
                new Document("name", "Snell Library")
                        .append("code", "SL")
                        .append("address", "360 Huntington Ave, Boston, MA 02115")
                        .append("type", "LIBRARY")
                        .append("latitude", 42.338478)
                        .append("longitude", -71.088024)
                        .append("floors", 4)
                        .append("hasLostFoundDesk", true),
                new Document("name", "Curry Student Center")
                        .append("code", "CSC")
                        .append("address", "346 Huntington Ave, Boston, MA 02115")
                        .append("type", "STUDENT_CENTER")
                        .append("latitude", 42.339015)
                        .append("longitude", -71.087214)
                        .append("floors", 4)
                        .append("hasLostFoundDesk", true),
                new Document("name", "Dodge Hall")
                        .append("code", "DG")
                        .append("address", "360 Huntington Ave, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.339814)
                        .append("longitude", -71.087843)
                        .append("floors", 5)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Egan Engineering/Science Research Center")
                        .append("code", "EG")
                        .append("address", "120 Forsyth St, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.337589)
                        .append("longitude", -71.089041)
                        .append("floors", 5)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Marino Recreation Center")
                        .append("code", "MRC")
                        .append("address", "369 Huntington Ave, Boston, MA 02115")
                        .append("type", "ATHLETIC")
                        .append("latitude", 42.340129)
                        .append("longitude", -71.090178)
                        .append("floors", 3)
                        .append("hasLostFoundDesk", true),
                new Document("name", "International Village")
                        .append("code", "IV")
                        .append("address", "1155 Tremont St, Boston, MA 02120")
                        .append("type", "RESIDENTIAL")
                        .append("latitude", 42.335419)
                        .append("longitude", -71.091598)
                        .append("floors", 22)
                        .append("hasLostFoundDesk", true),
                new Document("name", "Shillman Hall")
                        .append("code", "SH")
                        .append("address", "115 Forsyth St, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.337708)
                        .append("longitude", -71.090185)
                        .append("floors", 3)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Churchill Hall")
                        .append("code", "CH")
                        .append("address", "380 Huntington Ave, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.339903)
                        .append("longitude", -71.088719)
                        .append("floors", 4)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Hayden Hall")
                        .append("code", "HY")
                        .append("address", "370 Huntington Ave, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.339513)
                        .append("longitude", -71.087947)
                        .append("floors", 5)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Ryder Hall")
                        .append("code", "RY")
                        .append("address", "11 Leon St, Boston, MA 02115")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.336837)
                        .append("longitude", -71.091044)
                        .append("floors", 4)
                        .append("hasLostFoundDesk", false),
                new Document("name", "West Village H")
                        .append("code", "WVH")
                        .append("address", "440 Huntington Ave, Boston, MA 02115")
                        .append("type", "RESIDENTIAL")
                        .append("latitude", 42.338353)
                        .append("longitude", -71.092102)
                        .append("floors", 9)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Stetson East")
                        .append("code", "STE")
                        .append("address", "11 Speare Pl, Boston, MA 02115")
                        .append("type", "RESIDENTIAL")
                        .append("latitude", 42.341235)
                        .append("longitude", -71.090531)
                        .append("floors", 8)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Stetson West")
                        .append("code", "STW")
                        .append("address", "10 Speare Pl, Boston, MA 02115")
                        .append("type", "RESIDENTIAL")
                        .append("latitude", 42.341114)
                        .append("longitude", -71.090819)
                        .append("floors", 8)
                        .append("hasLostFoundDesk", false),
                new Document("name", "Centennial Common")
                        .append("code", "CC")
                        .append("address", "372 Huntington Ave, Boston, MA 02115")
                        .append("type", "DINING")
                        .append("latitude", 42.340231)
                        .append("longitude", -71.088532)
                        .append("floors", 1)
                        .append("hasLostFoundDesk", false),
                new Document("name", "ISEC (Interdisciplinary Science and Engineering Complex)")
                        .append("code", "ISEC")
                        .append("address", "805 Columbus Ave, Boston, MA 02120")
                        .append("type", "ACADEMIC")
                        .append("latitude", 42.337293)
                        .append("longitude", -71.086756)
                        .append("floors", 6)
                        .append("hasLostFoundDesk", false)
        );

        try {
            buildingsCollection.insertMany(defaultBuildings);
            LOGGER.info("Inserted " + defaultBuildings.size() + " default buildings");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error inserting default buildings", e);
        }
    }

    /**
     * Convert Building object to MongoDB Document
     */
    private Document buildingToDocument(Building building) {
        Document doc = new Document()
                .append("name", building.getName())
                .append("code", building.getCode())
                .append("type", building.getType().name())
                .append("address", building.getAddress())
                .append("enterpriseId", building.getEnterpriseId())
                .append("organizationId", building.getOrganizationId());

        // Add coordinator if exists
        if (building.getCoordinator() != null) {
            doc.append("coordinatorId", building.getCoordinator().getUserId())
                    .append("coordinatorName", building.getCoordinator().getFullName())
                    .append("coordinatorEmail", building.getCoordinator().getEmail());
        }

        return doc;
    }

    /**
     * Convert MongoDB Document to Building object
     */
    private Building documentToBuilding(Document doc) {
        Building building = new Building(
                doc.getString("name"),
                doc.getString("code"),
                Building.BuildingType.valueOf(doc.getString("type"))
        );

        // Use ObjectId hash as building ID for compatibility
        if (doc.getObjectId("_id") != null) {
            building.setBuildingId(doc.getObjectId("_id").toString().hashCode());
        }

        building.setAddress(doc.getString("address"));
        building.setEnterpriseId(doc.getString("enterpriseId"));
        building.setOrganizationId(doc.getString("organizationId"));

        // Note: Coordinator would need to be loaded separately if needed
        // This avoids circular dependencies
        return building;
    }

    /**
     * Get statistics about buildings
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalBuildings", buildingsCollection.countDocuments());

            // Count by type
            for (Building.BuildingType type : Building.BuildingType.values()) {
                long count = buildingsCollection.countDocuments(Filters.eq("type", type.name()));
                stats.put(type.name().toLowerCase() + "Count", count);
            }

            // Count buildings with lost & found desk
            long withDesk = buildingsCollection.countDocuments(Filters.eq("hasLostFoundDesk", true));
            stats.put("withLostFoundDesk", withDesk);

            // Count buildings with coordinators
            long withCoordinator = buildingsCollection.countDocuments(Filters.exists("coordinatorId"));
            stats.put("withCoordinator", withCoordinator);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting building statistics", e);
        }

        return stats;
    }

    /**
     * Test method to verify DAO functionality
     */
    public static void main(String[] args) {
        MongoBuildingDAO dao = new MongoBuildingDAO();

        System.out.println("Testing MongoBuildingDAO...");
        System.out.println("==========================\n");

        // Test count
        System.out.println("Total buildings: " + dao.count());

        // Test findAll
        List<Building> allBuildings = dao.findAll();
        System.out.println("\nAll Buildings:");
        for (Building b : allBuildings) {
            System.out.println("- " + b.getName() + " (" + b.getCode() + ") - " + b.getType());
        }

        // Test findByCode
        Optional<Building> snell = dao.findByCode("SL");
        if (snell.isPresent()) {
            System.out.println("\nFound Snell Library: " + snell.get().getName());
        }

        // Test findByType
        List<Building> residentialBuildings = dao.findByType(Building.BuildingType.RESIDENTIAL);
        System.out.println("\nResidential Buildings: " + residentialBuildings.size());

        // Test search
        List<Building> searchResults = dao.search("Village");
        System.out.println("\nSearch for 'Village': " + searchResults.size() + " results");

        // Test statistics
        Map<String, Object> stats = dao.getStatistics();
        System.out.println("\nBuilding Statistics:");
        stats.forEach((key, value) -> System.out.println("- " + key + ": " + value));

        System.out.println("\n✓ All tests completed!");
    }
}
