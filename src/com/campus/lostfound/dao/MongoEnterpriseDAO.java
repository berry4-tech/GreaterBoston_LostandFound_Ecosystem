package com.campus.lostfound.dao;

import com.campus.lostfound.models.Enterprise;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO for Enterprise operations
 */
public class MongoEnterpriseDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoEnterpriseDAO.class.getName());
    private final MongoCollection<Document> enterprisesCollection;

    public MongoEnterpriseDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.enterprisesCollection = connection.getCollection("enterprises");
    }

    public String create(Enterprise enterprise) {
        try {
            Document doc = new Document()
                    .append("networkId", enterprise.getNetworkId())
                    .append("name", enterprise.getName())
                    .append("type", enterprise.getType().name())
                    .append("description", enterprise.getDescription())
                    .append("contactEmail", enterprise.getContactEmail())
                    .append("contactPhone", enterprise.getContactPhone())
                    .append("joinedDate", enterprise.getJoinedDate())
                    .append("isActive", enterprise.isActive());

            enterprisesCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            enterprise.setEnterpriseId(id);
            LOGGER.info("Enterprise created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating enterprise", e);
            return null;
        }
    }

    public Optional<Enterprise> findById(String id) {
        try {
            Document doc = enterprisesCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToEnterprise(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding enterprise by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<Enterprise> findByName(String name) {
        try {
            Document doc = enterprisesCollection.find(Filters.eq("name", name)).first();
            if (doc != null) {
                return Optional.of(documentToEnterprise(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding enterprise by name: " + name, e);
        }
        return Optional.empty();
    }

    public List<Enterprise> findAll() {
        List<Enterprise> enterprises = new ArrayList<>();
        try {
            for (Document doc : enterprisesCollection.find()) {
                enterprises.add(documentToEnterprise(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all enterprises", e);
        }
        return enterprises;
    }

    public List<Enterprise> findByNetworkId(String networkId) {
        List<Enterprise> enterprises = new ArrayList<>();
        try {
            for (Document doc : enterprisesCollection.find(Filters.eq("networkId", networkId))) {
                enterprises.add(documentToEnterprise(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding enterprises by network ID: " + networkId, e);
        }
        return enterprises;
    }

    public List<Enterprise> findByType(Enterprise.EnterpriseType type) {
        List<Enterprise> enterprises = new ArrayList<>();
        try {
            for (Document doc : enterprisesCollection.find(Filters.eq("type", type.name()))) {
                enterprises.add(documentToEnterprise(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding enterprises by type: " + type, e);
        }
        return enterprises;
    }

    public boolean update(Enterprise enterprise) {
        try {
            enterprisesCollection.updateOne(
                    Filters.eq("_id", new ObjectId(enterprise.getEnterpriseId())),
                    Updates.combine(
                            Updates.set("name", enterprise.getName()),
                            Updates.set("type", enterprise.getType().name()),
                            Updates.set("description", enterprise.getDescription()),
                            Updates.set("contactEmail", enterprise.getContactEmail()),
                            Updates.set("contactPhone", enterprise.getContactPhone()),
                            Updates.set("isActive", enterprise.isActive())
                    )
            );
            LOGGER.info("Enterprise updated: " + enterprise.getEnterpriseId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating enterprise", e);
            return false;
        }
    }

    public boolean delete(String id) {
        try {
            enterprisesCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            LOGGER.info("Enterprise deleted: " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting enterprise", e);
            return false;
        }
    }

    private Enterprise documentToEnterprise(Document doc) {
        Enterprise enterprise = new Enterprise();
        enterprise.setEnterpriseId(doc.getObjectId("_id").toString());
        enterprise.setNetworkId(doc.getString("networkId"));
        enterprise.setName(doc.getString("name"));
        enterprise.setType(Enterprise.EnterpriseType.valueOf(doc.getString("type")));
        enterprise.setDescription(doc.getString("description"));
        enterprise.setContactEmail(doc.getString("contactEmail"));
        enterprise.setContactPhone(doc.getString("contactPhone"));
        enterprise.setJoinedDate(doc.getDate("joinedDate"));
        enterprise.setActive(doc.getBoolean("isActive", true));
        return enterprise;
    }
}
