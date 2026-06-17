package com.campus.lostfound.dao;

import com.campus.lostfound.models.Network;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO for Network operations
 */
public class MongoNetworkDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoNetworkDAO.class.getName());
    private final MongoCollection<Document> networksCollection;

    public MongoNetworkDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.networksCollection = connection.getCollection("networks");
    }

    public String create(Network network) {
        try {
            Document doc = new Document()
                    .append("name", network.getName())
                    .append("description", network.getDescription())
                    .append("createdDate", network.getCreatedDate())
                    .append("isActive", network.isActive());

            networksCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            network.setNetworkId(id);
            LOGGER.info("Network created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating network", e);
            return null;
        }
    }

    public Optional<Network> findById(String id) {
        try {
            Document doc = networksCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToNetwork(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding network by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<Network> findByName(String name) {
        try {
            Document doc = networksCollection.find(Filters.eq("name", name)).first();
            if (doc != null) {
                return Optional.of(documentToNetwork(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding network by name: " + name, e);
        }
        return Optional.empty();
    }

    public List<Network> findAll() {
        List<Network> networks = new ArrayList<>();
        try {
            for (Document doc : networksCollection.find()) {
                networks.add(documentToNetwork(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all networks", e);
        }
        return networks;
    }

    public boolean update(Network network) {
        try {
            networksCollection.updateOne(
                    Filters.eq("_id", new ObjectId(network.getNetworkId())),
                    Updates.combine(
                            Updates.set("name", network.getName()),
                            Updates.set("description", network.getDescription()),
                            Updates.set("isActive", network.isActive())
                    )
            );
            LOGGER.info("Network updated: " + network.getNetworkId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating network", e);
            return false;
        }
    }

    public boolean delete(String id) {
        try {
            networksCollection.deleteOne(Filters.eq("_id", new ObjectId(id)));
            LOGGER.info("Network deleted: " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting network", e);
            return false;
        }
    }

    private Network documentToNetwork(Document doc) {
        Network network = new Network();
        network.setNetworkId(doc.getObjectId("_id").toString());
        network.setName(doc.getString("name"));
        network.setDescription(doc.getString("description"));
        network.setCreatedDate(doc.getDate("createdDate"));
        network.setActive(doc.getBoolean("isActive", true));
        return network;
    }
}
