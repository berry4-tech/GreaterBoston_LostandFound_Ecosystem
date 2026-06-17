package com.campus.lostfound.dao;

import com.campus.lostfound.models.verification.VerificationRequest;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationType;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationStatus;
import com.campus.lostfound.models.verification.VerificationRequest.VerificationPriority;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for VerificationRequest entities.
 * Handles all MongoDB operations for the verification system.
 * 
 * Supports:
 * - Identity verification requests
 * - High-value item claim verifications
 * - Cross-enterprise transfer verifications
 * - Serial number and stolen property checks
 * - Police background checks
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class MongoVerificationDAO {
    
    private static final Logger LOGGER = Logger.getLogger(MongoVerificationDAO.class.getName());
    
    private final MongoCollection<Document> collection;
    
    // Sequence counter for visible IDs
    private int sequenceCounter = 1000;
    
    // ==================== CONSTRUCTOR ====================
    
    public MongoVerificationDAO() {
        MongoDatabase database = MongoDBConnection.getInstance().getDatabase();
        this.collection = database.getCollection("verification_requests");
        
        initializeIndexes();
        initializeSequence();
        
        LOGGER.info("MongoVerificationDAO initialized");
    }
    
    /**
     * Create indexes for efficient queries
     */
    private void initializeIndexes() {
        try {
            // Primary lookups
            collection.createIndex(new Document("visibleId", 1), 
                new IndexOptions().unique(true).sparse(true));
            collection.createIndex(new Document("subjectUserId", 1));
            collection.createIndex(new Document("subjectItemId", 1));
            collection.createIndex(new Document("requesterId", 1));
            collection.createIndex(new Document("verifierId", 1));
            
            // Status and type filters
            collection.createIndex(new Document("status", 1));
            collection.createIndex(new Document("verificationType", 1));
            collection.createIndex(new Document("priority", 1));
            
            // Time-based queries
            collection.createIndex(new Document("createdAt", -1));
            collection.createIndex(new Document("expiresAt", 1));
            collection.createIndex(new Document("updatedAt", -1));
            
            // Compound indexes for common queries
            collection.createIndex(new Document("status", 1).append("verifierId", 1));
            collection.createIndex(new Document("status", 1).append("priority", -1));
            collection.createIndex(new Document("subjectUserId", 1).append("verificationType", 1));
            collection.createIndex(new Document("verificationType", 1).append("status", 1));
            
            // Related entity lookups
            collection.createIndex(new Document("relatedWorkRequestId", 1));
            collection.createIndex(new Document("relatedClaimId", 1));
            
            LOGGER.info("Verification indexes created successfully");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating indexes (may already exist)", e);
        }
    }
    
    /**
     * Initialize sequence counter from existing data
     */
    private void initializeSequence() {
        try {
            long count = collection.countDocuments();
            if (count > 0) {
                sequenceCounter = (int) count + 1000;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing sequence", e);
        }
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Save a new VerificationRequest
     * @return The requestId (ObjectId as string)
     */
    public String save(VerificationRequest request) {
        try {
            Document doc = toDocument(request);
            
            // Generate visible ID if not set
            if (request.getVisibleId() == null) {
                request.generateVisibleId(sequenceCounter++);
                doc.put("visibleId", request.getVisibleId());
            }
            
            collection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            request.setRequestId(id);
            
            LOGGER.info("Created VerificationRequest: " + request.getVisibleId() + 
                       " type: " + request.getVerificationType());
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving VerificationRequest", e);
            return null;
        }
    }
    
    /**
     * Update an existing VerificationRequest
     */
    public boolean update(VerificationRequest request) {
        try {
            if (request.getRequestId() == null) {
                LOGGER.warning("Cannot update request without ID");
                return false;
            }
            
            ObjectId objectId = new ObjectId(request.getRequestId());
            Document doc = toDocument(request);
            doc.put("_id", objectId);
            
            collection.replaceOne(Filters.eq("_id", objectId), doc);
            LOGGER.info("Updated VerificationRequest: " + request.getVisibleId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating VerificationRequest", e);
            return false;
        }
    }
    
    /**
     * Find by MongoDB ObjectId
     */
    public VerificationRequest findById(String requestId) {
        try {
            ObjectId objectId = new ObjectId(requestId);
            Document doc = collection.find(Filters.eq("_id", objectId)).first();
            return doc != null ? toVerificationRequest(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding request by ID: " + requestId, e);
            return null;
        }
    }
    
    /**
     * Find by visible ID (e.g., "VR-001234")
     */
    public VerificationRequest findByVisibleId(String visibleId) {
        try {
            Document doc = collection.find(Filters.eq("visibleId", visibleId)).first();
            return doc != null ? toVerificationRequest(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding request by visibleId: " + visibleId, e);
            return null;
        }
    }
    
    /**
     * Delete a VerificationRequest
     */
    public boolean delete(String requestId) {
        try {
            ObjectId objectId = new ObjectId(requestId);
            collection.deleteOne(Filters.eq("_id", objectId));
            LOGGER.info("Deleted VerificationRequest: " + requestId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting request: " + requestId, e);
            return false;
        }
    }
    
    // ==================== QUERY METHODS ====================
    
    /**
     * Find all verification requests for a user (as subject)
     */
    public List<VerificationRequest> findByUserId(String userId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("subjectUserId", userId))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by userId: " + userId, e);
        }
        return requests;
    }
    
    /**
     * Find all verification requests for an item
     */
    public List<VerificationRequest> findByItemId(String itemId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("subjectItemId", itemId))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by itemId: " + itemId, e);
        }
        return requests;
    }
    
    /**
     * Find by status
     */
    public List<VerificationRequest> findByStatus(VerificationStatus status) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("status", status.name()))
                    .sort(Sorts.orderBy(Sorts.descending("priority"), Sorts.descending("createdAt")))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by status: " + status, e);
        }
        return requests;
    }
    
    /**
     * Find by type
     */
    public List<VerificationRequest> findByType(VerificationType type) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("verificationType", type.name()))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by type: " + type, e);
        }
        return requests;
    }
    
    /**
     * Find pending requests assigned to a verifier
     */
    public List<VerificationRequest> findPendingForVerifier(String verifierId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("verifierId", verifierId),
                Filters.in("status", Arrays.asList(
                    VerificationStatus.IN_PROGRESS.name(),
                    VerificationStatus.AWAITING_DOCUMENTS.name(),
                    VerificationStatus.AWAITING_RESPONSE.name()
                ))
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.orderBy(Sorts.descending("priority"), Sorts.ascending("createdAt")))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding pending for verifier: " + verifierId, e);
        }
        return requests;
    }
    
    /**
     * Find all unassigned pending requests
     */
    public List<VerificationRequest> findUnassignedPending() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("status", VerificationStatus.PENDING.name()),
                Filters.or(
                    Filters.eq("verifierId", null),
                    Filters.eq("verifierId", "")
                )
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.orderBy(Sorts.descending("priority"), Sorts.ascending("createdAt")))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding unassigned pending requests", e);
        }
        return requests;
    }
    
    /**
     * Find expired requests (not yet marked as expired)
     */
    public List<VerificationRequest> findExpired() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.nin("status", Arrays.asList(
                    VerificationStatus.VERIFIED.name(),
                    VerificationStatus.FAILED.name(),
                    VerificationStatus.EXPIRED.name(),
                    VerificationStatus.CANCELLED.name()
                )),
                Filters.lt("expiresAt", toDate(LocalDateTime.now()))
            );
            
            for (Document doc : collection.find(filter)) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding expired requests", e);
        }
        return requests;
    }
    
    /**
     * Find overdue requests (past SLA but not expired)
     */
    public List<VerificationRequest> findOverdue() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            // Get non-terminal requests
            for (Document doc : collection.find(
                    Filters.nin("status", Arrays.asList(
                        VerificationStatus.VERIFIED.name(),
                        VerificationStatus.FAILED.name(),
                        VerificationStatus.EXPIRED.name(),
                        VerificationStatus.CANCELLED.name()
                    )))) {
                VerificationRequest req = toVerificationRequest(doc);
                if (req.isOverdue()) {
                    requests.add(req);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding overdue requests", e);
        }
        return requests;
    }
    
    /**
     * Get verification history for a user
     */
    public List<VerificationRequest> getVerificationHistory(String userId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            // Get requests where user is either subject or requester
            Bson filter = Filters.or(
                Filters.eq("subjectUserId", userId),
                Filters.eq("requesterId", userId)
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting verification history: " + userId, e);
        }
        return requests;
    }
    
    /**
     * Find requests by related work request ID
     */
    public List<VerificationRequest> findByWorkRequestId(String workRequestId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("relatedWorkRequestId", workRequestId))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding by work request: " + workRequestId, e);
        }
        return requests;
    }
    
    /**
     * Find requests by related claim ID
     */
    public List<VerificationRequest> findByClaimId(String claimId) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("relatedClaimId", claimId))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding by claim: " + claimId, e);
        }
        return requests;
    }
    
    /**
     * Find requests requiring police involvement
     */
    public List<VerificationRequest> findRequiringPolice() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.in("verificationType", Arrays.asList(
                    VerificationType.STOLEN_PROPERTY_CHECK.name(),
                    VerificationType.POLICE_BACKGROUND_CHECK.name(),
                    VerificationType.SERIAL_NUMBER_CHECK.name()
                )),
                Filters.nin("status", Arrays.asList(
                    VerificationStatus.VERIFIED.name(),
                    VerificationStatus.FAILED.name(),
                    VerificationStatus.EXPIRED.name(),
                    VerificationStatus.CANCELLED.name()
                ))
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.orderBy(Sorts.descending("priority"), Sorts.ascending("createdAt")))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding police-required requests", e);
        }
        return requests;
    }
    
    /**
     * Find items flagged as potentially stolen
     */
    public List<VerificationRequest> findStolenFlags() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("isReportedStolen", true))
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding stolen flags", e);
        }
        return requests;
    }
    
    /**
     * Find high-value item verifications
     */
    public List<VerificationRequest> findHighValuePending(double minValue) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("verificationType", VerificationType.HIGH_VALUE_ITEM_CLAIM.name()),
                Filters.gte("subjectItemValue", minValue),
                Filters.eq("status", VerificationStatus.PENDING.name())
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.descending("subjectItemValue"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding high-value pending", e);
        }
        return requests;
    }
    
    /**
     * Check if user has pending verification of a specific type
     */
    public boolean hasPendingVerification(String userId, VerificationType type) {
        try {
            Bson filter = Filters.and(
                Filters.eq("subjectUserId", userId),
                Filters.eq("verificationType", type.name()),
                Filters.nin("status", Arrays.asList(
                    VerificationStatus.VERIFIED.name(),
                    VerificationStatus.FAILED.name(),
                    VerificationStatus.EXPIRED.name(),
                    VerificationStatus.CANCELLED.name()
                ))
            );
            
            return collection.countDocuments(filter) > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking pending verification", e);
            return false;
        }
    }
    
    /**
     * Check if user has been verified for a specific type
     */
    public boolean isUserVerified(String userId, VerificationType type) {
        try {
            Bson filter = Filters.and(
                Filters.eq("subjectUserId", userId),
                Filters.eq("verificationType", type.name()),
                Filters.eq("status", VerificationStatus.VERIFIED.name())
            );
            
            return collection.countDocuments(filter) > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking user verification", e);
            return false;
        }
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Get count by status
     */
    public Map<VerificationStatus, Long> getCountByStatus() {
        Map<VerificationStatus, Long> counts = new LinkedHashMap<>();
        
        for (VerificationStatus status : VerificationStatus.values()) {
            counts.put(status, 0L);
        }
        
        try {
            for (Document doc : collection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$status")
                    .append("count", new Document("$sum", 1)))
            ))) {
                String statusName = doc.getString("_id");
                if (statusName != null) {
                    try {
                        VerificationStatus status = VerificationStatus.valueOf(statusName);
                        counts.put(status, doc.getInteger("count").longValue());
                    } catch (IllegalArgumentException e) {
                        // Skip unknown status
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting count by status", e);
        }
        
        return counts;
    }
    
    /**
     * Get count by type
     */
    public Map<VerificationType, Long> getCountByType() {
        Map<VerificationType, Long> counts = new LinkedHashMap<>();
        
        for (VerificationType type : VerificationType.values()) {
            counts.put(type, 0L);
        }
        
        try {
            for (Document doc : collection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$verificationType")
                    .append("count", new Document("$sum", 1)))
            ))) {
                String typeName = doc.getString("_id");
                if (typeName != null) {
                    try {
                        VerificationType type = VerificationType.valueOf(typeName);
                        counts.put(type, doc.getInteger("count").longValue());
                    } catch (IllegalArgumentException e) {
                        // Skip unknown type
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting count by type", e);
        }
        
        return counts;
    }
    
    /**
     * Get total count
     */
    public long getTotalCount() {
        try {
            return collection.countDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting requests", e);
            return 0;
        }
    }
    
    /**
     * Get pending count
     */
    public long getPendingCount() {
        try {
            return collection.countDocuments(Filters.eq("status", VerificationStatus.PENDING.name()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting pending", e);
            return 0;
        }
    }
    
    /**
     * Get today's completions
     */
    public long getTodayCompletions() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            
            Bson filter = Filters.and(
                Filters.eq("status", VerificationStatus.VERIFIED.name()),
                Filters.gte("completedAt", toDate(startOfDay))
            );
            
            return collection.countDocuments(filter);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting today's completions", e);
            return 0;
        }
    }
    
    /**
     * Get average processing time (in hours) for completed verifications
     */
    public double getAverageProcessingTimeHours() {
        try {
            Document result = collection.aggregate(Arrays.asList(
                new Document("$match", new Document("status", VerificationStatus.VERIFIED.name())),
                new Document("$project", new Document("duration", 
                    new Document("$subtract", Arrays.asList("$completedAt", "$createdAt")))),
                new Document("$group", new Document("_id", null)
                    .append("avgDuration", new Document("$avg", "$duration")))
            )).first();
            
            if (result != null && result.get("avgDuration") != null) {
                // Convert milliseconds to hours
                return result.getDouble("avgDuration") / (1000.0 * 60 * 60);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating avg processing time", e);
        }
        return 0.0;
    }
    
    // ==================== BULK OPERATIONS ====================
    
    /**
     * Mark multiple requests as expired
     */
    public int markExpiredRequests() {
        int count = 0;
        try {
            List<VerificationRequest> expired = findExpired();
            for (VerificationRequest req : expired) {
                req.expire();
                update(req);
                count++;
            }
            LOGGER.info("Marked " + count + " requests as expired");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking expired requests", e);
        }
        return count;
    }
    
    /**
     * Get all requests (for admin)
     */
    public List<VerificationRequest> findAll() {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            for (Document doc : collection.find()
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all requests", e);
        }
        return requests;
    }
    
    /**
     * Find requests within date range
     */
    public List<VerificationRequest> findByDateRange(LocalDateTime start, LocalDateTime end) {
        List<VerificationRequest> requests = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.gte("createdAt", toDate(start)),
                Filters.lte("createdAt", toDate(end))
            );
            
            for (Document doc : collection.find(filter)
                    .sort(Sorts.descending("createdAt"))) {
                requests.add(toVerificationRequest(doc));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding by date range", e);
        }
        return requests;
    }
    
    // ==================== DOCUMENT CONVERSION ====================
    
    /**
     * Convert VerificationRequest to MongoDB Document
     */
    private Document toDocument(VerificationRequest req) {
        Document doc = new Document();
        
        if (req.getRequestId() != null) {
            doc.append("_id", new ObjectId(req.getRequestId()));
        }
        
        doc.append("visibleId", req.getVisibleId())
           .append("verificationType", req.getVerificationType() != null ? 
               req.getVerificationType().name() : null)
           .append("status", req.getStatus() != null ? req.getStatus().name() : null)
           .append("priority", req.getPriority() != null ? req.getPriority().name() : null)
           
           // Subject info
           .append("subjectUserId", req.getSubjectUserId())
           .append("subjectUserName", req.getSubjectUserName())
           .append("subjectUserEmail", req.getSubjectUserEmail())
           .append("subjectItemId", req.getSubjectItemId())
           .append("subjectItemName", req.getSubjectItemName())
           .append("subjectItemValue", req.getSubjectItemValue())
           
           // Requester info
           .append("requesterId", req.getRequesterId())
           .append("requesterName", req.getRequesterName())
           .append("requesterEmail", req.getRequesterEmail())
           .append("requesterOrganizationId", req.getRequesterOrganizationId())
           .append("requesterEnterpriseName", req.getRequesterEnterpriseName())
           
           // Verifier info
           .append("verifierId", req.getVerifierId())
           .append("verifierName", req.getVerifierName())
           .append("verifierRole", req.getVerifierRole())
           .append("verifierOrganizationId", req.getVerifierOrganizationId())
           
           // Related entities
           .append("relatedWorkRequestId", req.getRelatedWorkRequestId())
           .append("relatedClaimId", req.getRelatedClaimId())
           
           // Verification details
           .append("verificationNotes", req.getVerificationNotes())
           .append("requestReason", req.getRequestReason())
           .append("failureReason", req.getFailureReason())
           
           // External check results
           .append("policeCheckResult", req.getPoliceCheckResult())
           .append("policeCheckCaseNumber", req.getPoliceCheckCaseNumber())
           .append("serialNumberCheckResult", req.getSerialNumberCheckResult())
           .append("stolenPropertyCheckResult", req.getStolenPropertyCheckResult())
           .append("isReportedStolen", req.isReportedStolen())
           
           // Document verification
           .append("documentType", req.getDocumentType())
           .append("documentVerificationResult", req.getDocumentVerificationResult())
           .append("documentsVerified", req.isDocumentsVerified())
           
           // Multi-party approval
           .append("requiredApprovals", req.getRequiredApprovals())
           .append("currentApprovals", req.getCurrentApprovals())
           .append("approverIds", req.getApproverIds())
           .append("approverNames", req.getApproverNames())
           
           // Timestamps
           .append("createdAt", toDate(req.getCreatedAt()))
           .append("updatedAt", toDate(req.getUpdatedAt()))
           .append("assignedAt", toDate(req.getAssignedAt()))
           .append("completedAt", toDate(req.getCompletedAt()))
           .append("expiresAt", toDate(req.getExpiresAt()));
        
        return doc;
    }
    
    /**
     * Convert MongoDB Document to VerificationRequest
     */
    private VerificationRequest toVerificationRequest(Document doc) {
        VerificationRequest req = new VerificationRequest();
        
        req.setRequestId(doc.getObjectId("_id").toString());
        req.setVisibleId(doc.getString("visibleId"));
        
        // Enums
        String typeStr = doc.getString("verificationType");
        if (typeStr != null) {
            try {
                req.setVerificationType(VerificationType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown verification type: " + typeStr);
            }
        }
        
        String statusStr = doc.getString("status");
        if (statusStr != null) {
            try {
                req.setStatus(VerificationStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                req.setStatus(VerificationStatus.PENDING);
            }
        }
        
        String priorityStr = doc.getString("priority");
        if (priorityStr != null) {
            try {
                req.setPriority(VerificationPriority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                req.setPriority(VerificationPriority.NORMAL);
            }
        }
        
        // Subject info
        req.setSubjectUserId(doc.getString("subjectUserId"));
        req.setSubjectUserName(doc.getString("subjectUserName"));
        req.setSubjectUserEmail(doc.getString("subjectUserEmail"));
        req.setSubjectItemId(doc.getString("subjectItemId"));
        req.setSubjectItemName(doc.getString("subjectItemName"));
        req.setSubjectItemValue(doc.getDouble("subjectItemValue") != null ? 
            doc.getDouble("subjectItemValue") : 0.0);
        
        // Requester info
        req.setRequesterId(doc.getString("requesterId"));
        req.setRequesterName(doc.getString("requesterName"));
        req.setRequesterEmail(doc.getString("requesterEmail"));
        req.setRequesterOrganizationId(doc.getString("requesterOrganizationId"));
        req.setRequesterEnterpriseName(doc.getString("requesterEnterpriseName"));
        
        // Verifier info
        req.setVerifierId(doc.getString("verifierId"));
        req.setVerifierName(doc.getString("verifierName"));
        req.setVerifierRole(doc.getString("verifierRole"));
        req.setVerifierOrganizationId(doc.getString("verifierOrganizationId"));
        
        // Related entities
        req.setRelatedWorkRequestId(doc.getString("relatedWorkRequestId"));
        req.setRelatedClaimId(doc.getString("relatedClaimId"));
        
        // Verification details
        req.setVerificationNotes(doc.getString("verificationNotes"));
        req.setRequestReason(doc.getString("requestReason"));
        req.setFailureReason(doc.getString("failureReason"));
        
        // External check results
        req.setPoliceCheckResult(doc.getString("policeCheckResult"));
        req.setPoliceCheckCaseNumber(doc.getString("policeCheckCaseNumber"));
        req.setSerialNumberCheckResult(doc.getString("serialNumberCheckResult"));
        req.setStolenPropertyCheckResult(doc.getString("stolenPropertyCheckResult"));
        req.setReportedStolen(doc.getBoolean("isReportedStolen", false));
        
        // Document verification
        req.setDocumentType(doc.getString("documentType"));
        req.setDocumentVerificationResult(doc.getString("documentVerificationResult"));
        req.setDocumentsVerified(doc.getBoolean("documentsVerified", false));
        
        // Multi-party approval
        req.setRequiredApprovals(doc.getInteger("requiredApprovals", 1));
        req.setCurrentApprovals(doc.getInteger("currentApprovals", 0));
        req.setApproverIds(doc.getString("approverIds"));
        req.setApproverNames(doc.getString("approverNames"));
        
        // Timestamps
        req.setCreatedAt(toLocalDateTime(doc.getDate("createdAt")));
        req.setUpdatedAt(toLocalDateTime(doc.getDate("updatedAt")));
        req.setAssignedAt(toLocalDateTime(doc.getDate("assignedAt")));
        req.setCompletedAt(toLocalDateTime(doc.getDate("completedAt")));
        req.setExpiresAt(toLocalDateTime(doc.getDate("expiresAt")));
        
        return req;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Convert LocalDateTime to Date for MongoDB
     */
    private Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Convert Date from MongoDB to LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
