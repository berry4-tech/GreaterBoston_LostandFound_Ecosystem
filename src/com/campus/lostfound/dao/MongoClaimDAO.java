/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.campus.lostfound.models.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO for managing Claims in MongoDB
 * @author aksha
 */
public class MongoClaimDAO {
    
    private static final Logger LOGGER = Logger.getLogger(MongoClaimDAO.class.getName());
    private final MongoCollection<Document> claimsCollection;
    
    public MongoClaimDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.claimsCollection = connection.getCollection("claims");
    }
    
    /**
     * Create a new claim
     */
    public String create(Claim claim) {
        try {
            Document doc = claimToDocument(claim);
            claimsCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            LOGGER.info("Claim created with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating claim", e);
            return null;
        }
    }
    
    /**
     * Find claim by ID
     */
    public Optional<Claim> findById(String id) {
        try {
            Document doc = claimsCollection.find(Filters.eq("_id", new ObjectId(id))).first();
            if (doc != null) {
                return Optional.of(documentToClaim(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding claim by ID: " + id, e);
        }
        return Optional.empty();
    }
    
    /**
     * Get all pending claims for an item
     */
    public List<Claim> findPendingClaimsByItem(int itemId) {
        List<Claim> claims = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("itemId", itemId),
                Filters.eq("status", "PENDING")
            );
            
            for (Document doc : claimsCollection.find(filter).sort(Sorts.descending("submittedDate"))) {
                claims.add(documentToClaim(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding pending claims for item: " + itemId, e);
        }
        return claims;
    }
    
    /**
     * Get all claims for an item (any status)
     */
    public List<Claim> findAllClaimsByItem(int itemId) {
        List<Claim> claims = new ArrayList<>();
        try {
            Bson filter = Filters.eq("itemId", itemId);
            
            for (Document doc : claimsCollection.find(filter).sort(Sorts.descending("submittedDate"))) {
                claims.add(documentToClaim(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding claims for item: " + itemId, e);
        }
        return claims;
    }
    
    /**
     * Get all claims by a user
     */
    public List<Claim> findClaimsByUser(int userId) {
        List<Claim> claims = new ArrayList<>();
        try {
            Bson filter = Filters.eq("claimant.userId", userId);
            
            for (Document doc : claimsCollection.find(filter).sort(Sorts.descending("submittedDate"))) {
                claims.add(documentToClaim(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding claims by user: " + userId, e);
        }
        return claims;
    }
    
    /**
     * Update claim status (approve/reject)
     */
    public boolean updateClaimStatus(String claimId, Claim.ClaimStatus newStatus, String rejectionReason) {
        try {
            Document update = new Document("$set", new Document()
                .append("status", newStatus.name())
                .append("reviewedDate", new Date())
                .append("rejectionReason", rejectionReason)
            );
            
            claimsCollection.updateOne(
                Filters.eq("_id", new ObjectId(claimId)),
                update
            );
            
            LOGGER.info("Claim " + claimId + " updated to " + newStatus);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating claim status", e);
            return false;
        }
    }
    
    /**
     * Count pending claims for an item
     */
    public long countPendingClaims(int itemId) {
        try {
            Bson filter = Filters.and(
                Filters.eq("itemId", itemId),
                Filters.eq("status", "PENDING")
            );
            return claimsCollection.countDocuments(filter);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting pending claims", e);
            return 0;
        }
    }
    
    /**
     * Convert Claim to MongoDB Document
     */
    private Document claimToDocument(Claim claim) {
        Document doc = new Document()
            .append("itemId", claim.getItemId())
            .append("uniqueFeatures", claim.getUniqueFeatures())
            .append("lostDate", claim.getLostDate())
            .append("proofOfOwnership", claim.getProofOfOwnership())
            .append("verificationAnswer", claim.getVerificationAnswer())
            .append("contactMethod", claim.getContactMethod())
            .append("status", claim.getStatus().name())
            .append("submittedDate", claim.getSubmittedDate())
            .append("reviewedDate", claim.getReviewedDate())
            .append("rejectionReason", claim.getRejectionReason());
        
        // Store time slots as array
        if (claim.getSelectedTimeSlots() != null) {
            doc.append("timeSlots", Arrays.asList(claim.getSelectedTimeSlots()));
        }
        
        // Embed claimant info
        User claimant = claim.getClaimant();
        if (claimant != null) {
            doc.append("claimant", new Document()
                .append("userId", claimant.getUserId())
                .append("email", claimant.getEmail())  // 🔥 This is the key - we need email!
                .append("firstName", claimant.getFirstName())
                .append("lastName", claimant.getLastName())
                .append("trustScore", claimant.getTrustScore())
            );
        }
        
        return doc;
    }
    
    /**
     * Convert MongoDB Document to Claim
     */
    private Claim documentToClaim(Document doc) {
        // Extract claimant
        Document claimantDoc = doc.get("claimant", Document.class);
        User claimant = new User(
            claimantDoc.getString("email"),
            claimantDoc.getString("firstName"),
            claimantDoc.getString("lastName"),
            User.UserRole.STUDENT // Default, could be stored if needed
        );
        claimant.setUserId(claimantDoc.getInteger("userId"));
        claimant.setTrustScore(claimantDoc.getDouble("trustScore"));
        
        // Create claim
        Claim claim = new Claim(doc.getInteger("itemId"), claimant);
        claim.setClaimId(doc.getObjectId("_id").toString());
        claim.setUniqueFeatures(doc.getString("uniqueFeatures"));
        claim.setLostDate(doc.getDate("lostDate"));
        claim.setProofOfOwnership(doc.getString("proofOfOwnership"));
        claim.setVerificationAnswer(doc.getString("verificationAnswer"));
        claim.setContactMethod(doc.getString("contactMethod"));
        claim.setStatus(Claim.ClaimStatus.valueOf(doc.getString("status")));
        claim.setSubmittedDate(doc.getDate("submittedDate"));
        claim.setReviewedDate(doc.getDate("reviewedDate"));
        claim.setRejectionReason(doc.getString("rejectionReason"));
        
        // Extract time slots
        List<String> timeSlots = doc.getList("timeSlots", String.class);
        if (timeSlots != null) {
            claim.setSelectedTimeSlots(timeSlots.toArray(new String[0]));
        }
        
        return claim;
    }
}
