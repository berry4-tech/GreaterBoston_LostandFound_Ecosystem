package com.campus.lostfound.dao;

import com.campus.lostfound.models.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO for Organization operations
 */
public class MongoOrganizationDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoOrganizationDAO.class.getName());
    private final MongoCollection<Document> organizationsCollection;

    public MongoOrganizationDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.organizationsCollection = connection.getCollection("organizations");
    }

    public String create(Organization organization) {
        try {
            Document doc = new Document()
                    .append("enterpriseId", organization.getEnterpriseId())
                    .append("name", organization.getName())
                    .append("type", organization.getType().name())
                    .append("description", organization.getDescription())
                    .append("contactEmail", organization.getContactEmail())
                    .append("contactPhone", organization.getContactPhone())
                    .append("address", organization.getAddress())
                    .append("createdDate", organization.getCreatedDate())
                    .append("isActive", organization.isActive());

            organizationsCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            organization.setOrganizationId(id);
            LOGGER.info("Organization created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating organization", e);
            return null;
        }
    }

    public Optional<Organization> findById(String id) {
        try {
            Document doc = organizationsCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToOrganization(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding organization by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<Organization> findByName(String name) {
        try {
            Document doc = organizationsCollection.find(Filters.eq("name", name)).first();
            if (doc != null) {
                return Optional.of(documentToOrganization(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding organization by name: " + name, e);
        }
        return Optional.empty();
    }

    public List<Organization> findAll() {
        List<Organization> organizations = new ArrayList<>();
        try {
            for (Document doc : organizationsCollection.find()) {
                organizations.add(documentToOrganization(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all organizations", e);
        }
        return organizations;
    }

    public List<Organization> findByEnterpriseId(String enterpriseId) {
        List<Organization> organizations = new ArrayList<>();
        try {
            for (Document doc : organizationsCollection.find(Filters.eq("enterpriseId", enterpriseId))) {
                organizations.add(documentToOrganization(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding organizations by enterprise ID: " + enterpriseId, e);
        }
        return organizations;
    }

    public List<Organization> findByType(Organization.OrganizationType type) {
        List<Organization> organizations = new ArrayList<>();
        try {
            for (Document doc : organizationsCollection.find(Filters.eq("type", type.name()))) {
                organizations.add(documentToOrganization(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding organizations by type: " + type, e);
        }
        return organizations;
    }

    public boolean update(Organization organization) {
        try {
            organizationsCollection.updateOne(
                    Filters.eq("_id", new ObjectId(organization.getOrganizationId())),
                    Updates.combine(
                            Updates.set("name", organization.getName()),
                            Updates.set("type", organization.getType().name()),
                            Updates.set("description", organization.getDescription()),
                            Updates.set("contactEmail", organization.getContactEmail()),
                            Updates.set("contactPhone", organization.getContactPhone()),
                            Updates.set("address", organization.getAddress()),
                            Updates.set("isActive", organization.isActive())
                    )
            );
            LOGGER.info("Organization updated: " + organization.getOrganizationId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating organization", e);
            return false;
        }
    }

    public boolean delete(String id) {
        try {
            organizationsCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            LOGGER.info("Organization deleted: " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting organization", e);
            return false;
        }
    }

    private Organization documentToOrganization(Document doc) {
        Organization organization = new Organization();
        organization.setOrganizationId(doc.getObjectId("_id").toString());
        organization.setEnterpriseId(doc.getString("enterpriseId"));
        organization.setName(doc.getString("name"));
        organization.setType(Organization.OrganizationType.valueOf(doc.getString("type")));
        organization.setDescription(doc.getString("description"));
        organization.setContactEmail(doc.getString("contactEmail"));
        organization.setContactPhone(doc.getString("contactPhone"));
        organization.setAddress(doc.getString("address"));
        organization.setCreatedDate(doc.getDate("createdDate"));
        organization.setActive(doc.getBoolean("isActive", true));
        return organization;
    }
}
