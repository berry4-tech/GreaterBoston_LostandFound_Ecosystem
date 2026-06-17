/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.Document;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.io.InputStream;

/**
 *
 * @author aksha
 */
public class MongoDBConnection {

    private static final Logger LOGGER = Logger.getLogger(MongoDBConnection.class.getName());
    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    // Default configuration
    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE_NAME = "campus_lostfound";

    private String connectionString;
    private String databaseName;

    private MongoDBConnection() {
        loadConfiguration();
        connect();
        initializeCollections();
    }

    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mongodb.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                connectionString = prop.getProperty("mongodb.connection.string", DEFAULT_CONNECTION_STRING);
                databaseName = prop.getProperty("mongodb.database", DEFAULT_DATABASE_NAME);
            } else {
                connectionString = DEFAULT_CONNECTION_STRING;
                databaseName = DEFAULT_DATABASE_NAME;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load mongodb.properties, using defaults", e);
            connectionString = DEFAULT_CONNECTION_STRING;
            databaseName = DEFAULT_DATABASE_NAME;
        }
    }

    private void connect() {
        try {
            // Create codec registry for POJOs
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            // Create MongoClient settings
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(pojoCodecRegistry)
                    .serverApi(ServerApi.builder()
                            .version(ServerApiVersion.V1)
                            .build())
                    .build();

            // Create MongoClient
            mongoClient = MongoClients.create(settings);

            // Get database with POJO codec registry
            database = mongoClient.getDatabase(databaseName)
                    .withCodecRegistry(pojoCodecRegistry);

            // Test connection
            database.runCommand(new Document("ping", 1));
            LOGGER.info("Successfully connected to MongoDB!");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to MongoDB", e);
            throw new RuntimeException("Failed to connect to MongoDB. Please ensure MongoDB is running on " + connectionString, e);
        }
    }

    private void initializeCollections() {
        try {
            // Create collections if they don't exist
            String[] collections = {"users", "items", "buildings", "claims", "messages", "notifications"};

            for (String collectionName : collections) {
                if (!database.listCollectionNames().into(new java.util.ArrayList<>()).contains(collectionName)) {
                    database.createCollection(collectionName);
                    LOGGER.info("Created collection: " + collectionName);
                }
            }

            // Create indexes
            createIndexes();

            // Insert default buildings if collection is empty
            insertDefaultBuildings();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing collections", e);
        }
    }

    private void createIndexes() {
        // Users indexes
        MongoCollection<Document> users = database.getCollection("users");
        users.createIndex(new Document("email", 1));
        users.createIndex(new Document("trustScore", -1));

        // Items indexes
        MongoCollection<Document> items = database.getCollection("items");
        items.createIndex(new Document("status", 1));
        items.createIndex(new Document("category", 1));
        items.createIndex(new Document("type", 1));
        items.createIndex(new Document("reportedDate", -1));
        items.createIndex(new Document("title", "text").append("description", "text")); // Text search index

        // Buildings indexes
        MongoCollection<Document> buildings = database.getCollection("buildings");
        buildings.createIndex(new Document("code", 1));

        LOGGER.info("Indexes created successfully");
    }

    private void insertDefaultBuildings() {
        MongoCollection<Document> buildings = database.getCollection("buildings");

        // Check if buildings already exist
        if (buildings.countDocuments() > 0) {
            return;
        }

        // Insert default buildings
        java.util.List<Document> defaultBuildings = java.util.Arrays.asList(
                new Document("name", "Snell Library")
                        .append("code", "SL")
                        .append("address", "360 Huntington Ave, Boston, MA 02115")
                        .append("type", "LIBRARY"),
                new Document("name", "Curry Student Center")
                        .append("code", "CSC")
                        .append("address", "346 Huntington Ave, Boston, MA 02115")
                        .append("type", "STUDENT_CENTER"),
                new Document("name", "Dodge Hall")
                        .append("code", "DG")
                        .append("address", "360 Huntington Ave, Boston, MA 02115")
                        .append("type", "ACADEMIC"),
                new Document("name", "Marino Recreation Center")
                        .append("code", "MRC")
                        .append("address", "369 Huntington Ave, Boston, MA 02115")
                        .append("type", "ATHLETIC"),
                new Document("name", "International Village")
                        .append("code", "IV")
                        .append("address", "1155 Tremont St, Boston, MA 02120")
                        .append("type", "RESIDENTIAL"),
                new Document("name", "Shillman Hall")
                        .append("code", "SH")
                        .append("address", "115 Forsyth St, Boston, MA 02115")
                        .append("type", "ACADEMIC"),
                new Document("name", "ISEC")
                        .append("code", "ISEC")
                        .append("address", "805 Columbus Ave, Boston, MA 02120")
                        .append("type", "ACADEMIC"),
                new Document("name", "Ryder Hall")
                        .append("code", "RY")
                        .append("address", "11 Leon St, Boston, MA 02115")
                        .append("type", "ACADEMIC"),
                new Document("name", "West Village H")
                        .append("code", "WVH")
                        .append("address", "440 Huntington Ave, Boston, MA 02115")
                        .append("type", "RESIDENTIAL")
        );

        buildings.insertMany(defaultBuildings);
        LOGGER.info("Inserted " + defaultBuildings.size() + " default buildings");
    }

    public static synchronized MongoDBConnection getInstance() {
        if (instance == null) {
            instance = new MongoDBConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> clazz) {
        return database.getCollection(collectionName, clazz);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            LOGGER.info("MongoDB connection closed");
        }
    }

    public boolean testConnection() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
