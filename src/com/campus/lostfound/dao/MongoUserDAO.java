/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.campus.lostfound.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author aksha
 */
public class MongoUserDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoUserDAO.class.getName());
    private final MongoCollection<Document> usersCollection;

    public MongoUserDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.usersCollection = connection.getCollection("users");
    }

    public String create(User user, String password) {
        try {
            Document doc = new Document()
                    .append("email", user.getEmail())
                    .append("passwordHash", hashPassword(password))
                    .append("firstName", user.getFirstName())
                    .append("lastName", user.getLastName())
                    .append("phoneNumber", user.getPhoneNumber())
                    .append("role", user.getRole().name())
                    .append("trustScore", user.getTrustScore())
                    .append("itemsReported", 0)
                    .append("itemsReturned", 0)
                    .append("falseClaims", 0)
                    .append("isActive", true)
                    .append("joinDate", new Date())
                    .append("lastLogin", null)
                    .append("enterpriseId", user.getEnterpriseId())
                    .append("organizationId", user.getOrganizationId());

            usersCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            LOGGER.info("User created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating user", e);
            return null;
        }
    }

    public Optional<User> findById(String id) {
        try {
            Document doc = usersCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToUser(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        try {
            Document doc = usersCollection.find(Filters.eq("email", email)).first();
            if (doc != null) {
                return Optional.of(documentToUser(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    public boolean authenticate(String email, String password) {
        try {
            Document doc = usersCollection.find(
                    Filters.and(
                            Filters.eq("email", email),
                            Filters.eq("isActive", true)
                    )
            ).first();

            if (doc != null) {
                String storedHash = doc.getString("passwordHash");
                String inputHash = hashPassword(password);

                if (storedHash.equals(inputHash)) {
                    // Update last login
                    usersCollection.updateOne(
                            Filters.eq("email", email),
                            Updates.set("lastLogin", new Date())
                    );
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error authenticating user", e);
        }
        return false;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try {
            for (Document doc : usersCollection.find(Filters.eq("isActive", true))) {
                users.add(documentToUser(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all users", e);
        }
        return users;
    }

    public void updateTrustScore(String userId, String action) {
        try {
            Document user = usersCollection.find(Filters.eq("_id", new ObjectId(userId))).first();
            if (user != null) {
                double currentScore = user.getDouble("trustScore");
                double newScore = currentScore;

                switch (action) {
                    case "RETURN":
                        newScore = Math.min(100, currentScore + 10);
                        usersCollection.updateOne(
                                Filters.eq("_id", new ObjectId(userId)),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("itemsReturned", 1)
                                )
                        );
                        break;
                    case "FALSE_CLAIM":
                        newScore = Math.max(0, currentScore - 25);
                        usersCollection.updateOne(
                                Filters.eq("_id", new ObjectId(userId)),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("falseClaims", 1)
                                )
                        );
                        break;
                    case "REPORT":
                        newScore = Math.min(100, currentScore + 2);
                        usersCollection.updateOne(
                                Filters.eq("_id", new ObjectId(userId)),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("itemsReported", 1)
                                )
                        );
                        break;
                }

                LOGGER.info("Trust score updated for user " + userId + ": " + currentScore + " -> " + newScore);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating trust score", e);
        }
    }

    /**
     * Update trust score by email (more reliable than using hashCode userId)
     */
    public void updateTrustScoreByEmail(String email, String action) {
        try {
            Document user = usersCollection.find(Filters.eq("email", email)).first();
            if (user != null) {
                double currentScore = user.getDouble("trustScore");
                double newScore = currentScore;

                switch (action) {
                    case "RETURN":
                        newScore = Math.min(100, currentScore + 10);
                        usersCollection.updateOne(
                                Filters.eq("email", email),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("itemsReturned", 1)
                                )
                        );
                        break;
                    case "FALSE_CLAIM":
                        newScore = Math.max(0, currentScore - 25);
                        usersCollection.updateOne(
                                Filters.eq("email", email),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("falseClaims", 1)
                                )
                        );
                        break;
                    case "REPORT":
                        newScore = Math.min(100, currentScore + 2);
                        usersCollection.updateOne(
                                Filters.eq("email", email),
                                Updates.combine(
                                        Updates.set("trustScore", newScore),
                                        Updates.inc("itemsReported", 1)
                                )
                        );
                        break;
                    case "HELPED_RETURN":
                        // Reward for finders who approve claims and help others get items back
                        newScore = Math.min(100, currentScore + 5);
                        usersCollection.updateOne(
                                Filters.eq("email", email),
                                Updates.set("trustScore", newScore)
                        );
                        break;
                }

                LOGGER.info("Trust score updated for user " + email + ": " + currentScore + " -> " + newScore);
            } else {
                LOGGER.warning("User not found with email: " + email);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating trust score by email", e);
        }
    }

    private User documentToUser(Document doc) {
        User user = new User(
                doc.getString("email"),
                doc.getString("firstName"),
                doc.getString("lastName"),
                User.UserRole.valueOf(doc.getString("role"))
        );

        String objectId = doc.getObjectId("_id").toString();
        user.setMongoId(objectId);  // Store actual MongoDB ObjectId for DB lookups
        user.setUserId(objectId.hashCode()); // Use hash for int ID (legacy compatibility)
        user.setPhoneNumber(doc.getString("phoneNumber"));
        user.setTrustScore(doc.getDouble("trustScore"));
        user.setItemsReported(doc.getInteger("itemsReported", 0));
        user.setItemsReturned(doc.getInteger("itemsReturned", 0));
        user.setFalseClaims(doc.getInteger("falseClaims", 0));
        user.setJoinDate(doc.getDate("joinDate"));
        user.setEnterpriseId(doc.getString("enterpriseId"));
        user.setOrganizationId(doc.getString("organizationId"));
        user.setActive(doc.getBoolean("isActive", true));

        return user;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Error hashing password", e);
            return password;
        }
    }

    /**
     * Update user information (role, trust score, active status)
     */
    public boolean update(User user) {
        return updateUser(user);
    }

    /**
     * Update user information (role, trust score, active status)
     * @deprecated Use update(User) instead
     */
    @Deprecated
    public boolean updateUser(User user) {
        try {
            usersCollection.updateOne(
                Filters.eq("email", user.getEmail()),
                Updates.combine(
                    Updates.set("firstName", user.getFirstName()),
                    Updates.set("lastName", user.getLastName()),
                    Updates.set("role", user.getRole().name()),
                    Updates.set("trustScore", user.getTrustScore()),
                    Updates.set("phoneNumber", user.getPhoneNumber()),
                    Updates.set("enterpriseId", user.getEnterpriseId()),
                    Updates.set("organizationId", user.getOrganizationId()),
                    Updates.set("isActive", user.isActive())
                )
            );
            LOGGER.info("User updated: " + user.getEmail());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating user", e);
            return false;
        }
    }

    /**
     * Set user active/inactive status
     */
    public boolean setUserActive(String email, boolean active) {
        try {
            usersCollection.updateOne(
                Filters.eq("email", email),
                Updates.set("isActive", active)
            );
            LOGGER.info("User " + email + " active status set to: " + active);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting user active status", e);
            return false;
        }
    }

    /**
     * Check if user is active
     */
    public boolean isUserActive(String email) {
        try {
            Document doc = usersCollection.find(Filters.eq("email", email)).first();
            if (doc != null) {
                return doc.getBoolean("isActive", true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking user active status", e);
        }
        return false;
    }

    /**
     * Find all users including inactive ones
     */
    public List<User> findAllIncludingInactive() {
        List<User> users = new ArrayList<>();
        try {
            for (Document doc : usersCollection.find()) {
                User user = documentToUser(doc);
                users.add(user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all users including inactive", e);
        }
        return users;
    }
}
