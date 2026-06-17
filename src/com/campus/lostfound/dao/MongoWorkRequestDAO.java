package com.campus.lostfound.dao;

import com.campus.lostfound.models.workrequest.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for WorkRequest and all its subclasses.
 * Handles polymorphic saving and loading of different request types.
 */
public class MongoWorkRequestDAO {
    private static final Logger LOGGER = Logger.getLogger(MongoWorkRequestDAO.class.getName());
    private final MongoCollection<Document> collection;
    
    public MongoWorkRequestDAO() {
        MongoDatabase database = MongoDBConnection.getInstance().getDatabase();
        this.collection = database.getCollection("work_requests");
        
        // Create indexes for common queries
        collection.createIndex(new Document("requesterId", 1));
        collection.createIndex(new Document("status", 1));
        collection.createIndex(new Document("requestType", 1));
        collection.createIndex(new Document("requesterOrganizationId", 1));
        collection.createIndex(new Document("targetOrganizationId", 1));
        collection.createIndex(new Document("currentApproverId", 1));
        
        LOGGER.info("MongoWorkRequestDAO initialized with indexes");
    }
    
    /**
     * Save a WorkRequest (any subclass) to the database.
     * Handles polymorphic saving by storing the request type.
     */
    public String save(WorkRequest request) {
        try {
            Document doc = workRequestToDocument(request);
            
            if (request.getRequestId() == null) {
                // New request - insert
                collection.insertOne(doc);
                String id = doc.getObjectId("_id").toString();
                request.setRequestId(id);
                LOGGER.info("Inserted new WorkRequest: " + id + " of type " + request.getRequestType());
                return id;
            } else {
                // Existing request - update
                ObjectId objectId = new ObjectId(request.getRequestId());
                collection.replaceOne(Filters.eq("_id", objectId), doc);
                LOGGER.info("Updated WorkRequest: " + request.getRequestId());
                return request.getRequestId();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving WorkRequest", e);
            return null;
        }
    }
    
    /**
     * Find a WorkRequest by ID.
     * Automatically instantiates the correct subclass based on stored type.
     */
    public WorkRequest findById(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            Document doc = collection.find(Filters.eq("_id", objectId)).first();
            
            if (doc == null) {
                LOGGER.warning("WorkRequest not found: " + id);
                return null;
            }
            
            return documentToWorkRequest(doc);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding WorkRequest by ID: " + id, e);
            return null;
        }
    }
    
    /**
     * Find all requests created by a specific user
     */
    public List<WorkRequest> findByRequesterId(String requesterId) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.eq("requesterId", requesterId))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by requester: " + requesterId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all requests by status
     */
    public List<WorkRequest> findByStatus(WorkRequest.RequestStatus status) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.eq("status", status.name()))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by status: " + status, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all requests by type
     */
    public List<WorkRequest> findByType(WorkRequest.RequestType type) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.eq("requestType", type.name()))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by type: " + type, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all requests for a specific organization (as requester)
     */
    public List<WorkRequest> findByRequesterOrganization(String organizationId) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.eq("requesterOrganizationId", organizationId))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by organization: " + organizationId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all requests targeting a specific organization
     */
    public List<WorkRequest> findByTargetOrganization(String organizationId) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.eq("targetOrganizationId", organizationId))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding requests by target organization: " + organizationId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all pending requests that need approval from a specific user
     */
    public List<WorkRequest> findPendingForApprover(String approverId) {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find(Filters.and(
                        Filters.eq("currentApproverId", approverId),
                        Filters.in("status", "PENDING", "IN_PROGRESS")
                     ))
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding pending requests for approver: " + approverId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all requests (for admin)
     */
    public List<WorkRequest> findAll() {
        try {
            List<WorkRequest> requests = new ArrayList<>();
            collection.find()
                     .sort(Sorts.descending("createdAt"))
                     .forEach(doc -> requests.add(documentToWorkRequest(doc)));
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all requests", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Delete a WorkRequest by ID
     */
    public boolean delete(String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return collection.deleteOne(Filters.eq("_id", objectId)).getDeletedCount() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting WorkRequest: " + id, e);
            return false;
        }
    }
    
    /**
     * Count total requests
     */
    public long count() {
        try {
            return collection.countDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting requests", e);
            return 0;
        }
    }
    
    /**
     * Count requests by status
     */
    public long countByStatus(WorkRequest.RequestStatus status) {
        try {
            return collection.countDocuments(Filters.eq("status", status.name()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting requests by status", e);
            return 0;
        }
    }
    
    // ==================== CONVERSION METHODS ====================
    
    /**
     * Convert a WorkRequest to a MongoDB Document.
     * Handles all subclasses polymorphically.
     */
    private Document workRequestToDocument(WorkRequest request) {
        Document doc = new Document();
        
        // Set _id if exists
        if (request.getRequestId() != null) {
            doc.put("_id", new ObjectId(request.getRequestId()));
        }
        
        // Common fields
        doc.put("requestType", request.getRequestType().name());
        doc.put("status", request.getStatus().name());
        doc.put("priority", request.getPriority().name());
        doc.put("requesterId", request.getRequesterId());
        doc.put("requesterEmail", request.getRequesterEmail());
        doc.put("requesterName", request.getRequesterName());
        doc.put("requesterEnterpriseId", request.getRequesterEnterpriseId());
        doc.put("requesterOrganizationId", request.getRequesterOrganizationId());
        doc.put("targetEnterpriseId", request.getTargetEnterpriseId());
        doc.put("targetOrganizationId", request.getTargetOrganizationId());
        doc.put("approverIds", request.getApproverIds());
        doc.put("approverNames", request.getApproverNames());
        doc.put("currentApproverId", request.getCurrentApproverId());
        doc.put("approvalStep", request.getApprovalStep());
        doc.put("description", request.getDescription());
        doc.put("notes", request.getNotes());
        doc.put("createdAt", localDateTimeToDate(request.getCreatedAt()));
        doc.put("lastUpdatedAt", localDateTimeToDate(request.getLastUpdatedAt()));
        doc.put("completedAt", localDateTimeToDate(request.getCompletedAt()));
        
        // Type-specific fields
        switch (request.getRequestType()) {
            case ITEM_CLAIM:
                addItemClaimFields(doc, (ItemClaimRequest) request);
                break;
            case CROSS_CAMPUS_TRANSFER:
                addCrossCampusTransferFields(doc, (CrossCampusTransferRequest) request);
                break;
            case TRANSIT_TO_UNIVERSITY_TRANSFER:
                addTransitTransferFields(doc, (TransitToUniversityTransferRequest) request);
                break;
            case AIRPORT_TO_UNIVERSITY_TRANSFER:
                addAirportTransferFields(doc, (AirportToUniversityTransferRequest) request);
                break;
            case POLICE_EVIDENCE_REQUEST:
                addPoliceEvidenceFields(doc, (PoliceEvidenceRequest) request);
                break;
            case MBTA_TO_AIRPORT_EMERGENCY:
                addMBTAToAirportEmergencyFields(doc, (MBTAToAirportEmergencyRequest) request);
                break;
            case MULTI_ENTERPRISE_DISPUTE:
                addMultiEnterpriseDisputeFields(doc, (MultiEnterpriseDisputeResolution) request);
                break;
        }
        
        return doc;
    }
    
    /**
     * Convert a MongoDB Document to a WorkRequest.
     * Instantiates the correct subclass based on requestType.
     */
    private WorkRequest documentToWorkRequest(Document doc) {
        String typeStr = doc.getString("requestType");
        WorkRequest.RequestType type = WorkRequest.RequestType.valueOf(typeStr);
        
        WorkRequest request;
        
        // Instantiate the correct subclass
        switch (type) {
            case ITEM_CLAIM:
                request = new ItemClaimRequest();
                loadItemClaimFields(doc, (ItemClaimRequest) request);
                break;
            case CROSS_CAMPUS_TRANSFER:
                request = new CrossCampusTransferRequest();
                loadCrossCampusTransferFields(doc, (CrossCampusTransferRequest) request);
                break;
            case TRANSIT_TO_UNIVERSITY_TRANSFER:
                request = new TransitToUniversityTransferRequest();
                loadTransitTransferFields(doc, (TransitToUniversityTransferRequest) request);
                break;
            case AIRPORT_TO_UNIVERSITY_TRANSFER:
                request = new AirportToUniversityTransferRequest();
                loadAirportTransferFields(doc, (AirportToUniversityTransferRequest) request);
                break;
            case POLICE_EVIDENCE_REQUEST:
                request = new PoliceEvidenceRequest();
                loadPoliceEvidenceFields(doc, (PoliceEvidenceRequest) request);
                break;
            case MBTA_TO_AIRPORT_EMERGENCY:
                request = new MBTAToAirportEmergencyRequest();
                loadMBTAToAirportEmergencyFields(doc, (MBTAToAirportEmergencyRequest) request);
                break;
            case MULTI_ENTERPRISE_DISPUTE:
                request = new MultiEnterpriseDisputeResolution();
                loadMultiEnterpriseDisputeFields(doc, (MultiEnterpriseDisputeResolution) request);
                break;
            default:
                throw new IllegalArgumentException("Unknown request type: " + type);
        }
        
        // Load common fields
        request.setRequestId(doc.getObjectId("_id").toString());
        request.setRequestType(type);
        request.setStatus(WorkRequest.RequestStatus.valueOf(doc.getString("status")));
        
        // Handle priority (may be null for old requests)
        String priorityStr = doc.getString("priority");
        if (priorityStr != null) {
            request.setPriority(WorkRequest.RequestPriority.valueOf(priorityStr));
        } else {
            // Default priority for old requests without priority field
            request.setPriority(WorkRequest.RequestPriority.NORMAL);
        }
        request.setRequesterId(doc.getString("requesterId"));
        request.setRequesterEmail(doc.getString("requesterEmail"));
        request.setRequesterName(doc.getString("requesterName"));
        request.setRequesterEnterpriseId(doc.getString("requesterEnterpriseId"));
        request.setRequesterOrganizationId(doc.getString("requesterOrganizationId"));
        request.setTargetEnterpriseId(doc.getString("targetEnterpriseId"));
        request.setTargetOrganizationId(doc.getString("targetOrganizationId"));
        request.setApproverIds(doc.getList("approverIds", String.class, new ArrayList<>()));
        request.setApproverNames(doc.getList("approverNames", String.class, new ArrayList<>()));
        request.setCurrentApproverId(doc.getString("currentApproverId"));
        request.setApprovalStep(doc.getInteger("approvalStep", 0));
        request.setDescription(doc.getString("description"));
        request.setNotes(doc.getString("notes"));
        request.setCreatedAt(dateToLocalDateTime(doc.getDate("createdAt")));
        request.setLastUpdatedAt(dateToLocalDateTime(doc.getDate("lastUpdatedAt")));
        request.setCompletedAt(dateToLocalDateTime(doc.getDate("completedAt")));
        
        return request;
    }
    
    // ==================== TYPE-SPECIFIC FIELD METHODS ====================
    
    private void addItemClaimFields(Document doc, ItemClaimRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("lostItemId", request.getLostItemId());  // For auto-closing matched lost item
        doc.put("itemName", request.getItemName());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("itemValue", request.getItemValue());
        doc.put("isHighValue", request.isHighValue());
        doc.put("claimDetails", request.getClaimDetails());
        doc.put("proofDescription", request.getProofDescription());
        doc.put("identifyingFeatures", request.getIdentifyingFeatures());
        doc.put("claimPhotoUrl", request.getClaimPhotoUrl());
        doc.put("foundLocationId", request.getFoundLocationId());
        doc.put("foundLocationName", request.getFoundLocationName());
        
        // ========== NEW FIELDS FOR DYNAMIC ROUTING ==========
        doc.put("itemHoldingEnterpriseType", request.getItemHoldingEnterpriseType());
        doc.put("itemHoldingEnterpriseName", request.getItemHoldingEnterpriseName());
    }
    
    private void loadItemClaimFields(Document doc, ItemClaimRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setLostItemId(doc.getString("lostItemId"));  // For auto-closing matched lost item
        request.setItemName(doc.getString("itemName"));
        request.setItemCategory(doc.getString("itemCategory"));
        
        // Handle null itemValue safely
        Double itemValue = doc.getDouble("itemValue");
        request.setItemValue(itemValue != null ? itemValue : 0.0);
        
        request.setHighValue(doc.getBoolean("isHighValue", false));
        request.setClaimDetails(doc.getString("claimDetails"));
        request.setProofDescription(doc.getString("proofDescription"));
        request.setIdentifyingFeatures(doc.getString("identifyingFeatures"));
        request.setClaimPhotoUrl(doc.getString("claimPhotoUrl"));
        request.setFoundLocationId(doc.getString("foundLocationId"));
        request.setFoundLocationName(doc.getString("foundLocationName"));
        
        // ========== NEW FIELDS FOR DYNAMIC ROUTING ==========
        // These will be null for existing claims (backward compatible)
        request.setItemHoldingEnterpriseType(doc.getString("itemHoldingEnterpriseType"));
        request.setItemHoldingEnterpriseName(doc.getString("itemHoldingEnterpriseName"));
    }
    
    private void addCrossCampusTransferFields(Document doc, CrossCampusTransferRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("lostItemId", request.getLostItemId());  // For auto-closing matched lost item
        doc.put("itemName", request.getItemName());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("sourceCampusName", request.getSourceCampusName());
        doc.put("sourceCoordinatorId", request.getSourceCoordinatorId());
        doc.put("sourceCoordinatorName", request.getSourceCoordinatorName());
        doc.put("sourceLocationId", request.getSourceLocationId());
        doc.put("sourceLocationName", request.getSourceLocationName());
        doc.put("destinationCampusName", request.getDestinationCampusName());
        doc.put("destinationCoordinatorId", request.getDestinationCoordinatorId());
        doc.put("destinationCoordinatorName", request.getDestinationCoordinatorName());
        doc.put("studentId", request.getStudentId());
        doc.put("studentName", request.getStudentName());
        doc.put("studentEmail", request.getStudentEmail());
        doc.put("studentPhone", request.getStudentPhone());
        doc.put("pickupLocation", request.getPickupLocation());
        doc.put("estimatedPickupDate", request.getEstimatedPickupDate());
        doc.put("transferMethod", request.getTransferMethod());
        doc.put("trackingNotes", request.getTrackingNotes());
    }
    
    private void loadCrossCampusTransferFields(Document doc, CrossCampusTransferRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setLostItemId(doc.getString("lostItemId"));  // For auto-closing matched lost item
        request.setItemName(doc.getString("itemName"));
        request.setItemCategory(doc.getString("itemCategory"));
        request.setSourceCampusName(doc.getString("sourceCampusName"));
        request.setSourceCoordinatorId(doc.getString("sourceCoordinatorId"));
        request.setSourceCoordinatorName(doc.getString("sourceCoordinatorName"));
        request.setSourceLocationId(doc.getString("sourceLocationId"));
        request.setSourceLocationName(doc.getString("sourceLocationName"));
        request.setDestinationCampusName(doc.getString("destinationCampusName"));
        request.setDestinationCoordinatorId(doc.getString("destinationCoordinatorId"));
        request.setDestinationCoordinatorName(doc.getString("destinationCoordinatorName"));
        request.setStudentId(doc.getString("studentId"));
        request.setStudentName(doc.getString("studentName"));
        request.setStudentEmail(doc.getString("studentEmail"));
        request.setStudentPhone(doc.getString("studentPhone"));
        request.setPickupLocation(doc.getString("pickupLocation"));
        request.setEstimatedPickupDate(doc.getString("estimatedPickupDate"));
        request.setTransferMethod(doc.getString("transferMethod"));
        request.setTrackingNotes(doc.getString("trackingNotes"));
    }
    
    private void addTransitTransferFields(Document doc, TransitToUniversityTransferRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("lostItemId", request.getLostItemId());  // For auto-closing matched lost item
        doc.put("itemName", request.getItemName());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("itemDescription", request.getItemDescription());
        doc.put("transitType", request.getTransitType());
        doc.put("routeNumber", request.getRouteNumber());
        doc.put("stationName", request.getStationName());
        doc.put("mbtaStationManagerId", request.getMbtaStationManagerId());
        doc.put("mbtaStationManagerName", request.getMbtaStationManagerName());
        doc.put("mbtaLocationId", request.getMbtaLocationId());
        doc.put("universityName", request.getUniversityName());
        doc.put("campusCoordinatorId", request.getCampusCoordinatorId());
        doc.put("campusCoordinatorName", request.getCampusCoordinatorName());
        doc.put("campusPickupLocation", request.getCampusPickupLocation());
        doc.put("studentId", request.getStudentId());
        doc.put("studentName", request.getStudentName());
        doc.put("studentEmail", request.getStudentEmail());
        doc.put("studentIdNumber", request.getStudentIdNumber());
        doc.put("transferDate", request.getTransferDate());
        doc.put("transferNotes", request.getTransferNotes());
        doc.put("requiresIdVerification", request.isRequiresIdVerification());
        doc.put("mbtaIncidentNumber", request.getMbtaIncidentNumber());
        doc.put("foundDate", request.getFoundDate());
    }
    
    private void loadTransitTransferFields(Document doc, TransitToUniversityTransferRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setLostItemId(doc.getString("lostItemId"));  // For auto-closing matched lost item
        request.setItemName(doc.getString("itemName"));
        request.setItemCategory(doc.getString("itemCategory"));
        request.setItemDescription(doc.getString("itemDescription"));
        request.setTransitType(doc.getString("transitType"));
        request.setRouteNumber(doc.getString("routeNumber"));
        request.setStationName(doc.getString("stationName"));
        request.setMbtaStationManagerId(doc.getString("mbtaStationManagerId"));
        request.setMbtaStationManagerName(doc.getString("mbtaStationManagerName"));
        request.setMbtaLocationId(doc.getString("mbtaLocationId"));
        request.setUniversityName(doc.getString("universityName"));
        request.setCampusCoordinatorId(doc.getString("campusCoordinatorId"));
        request.setCampusCoordinatorName(doc.getString("campusCoordinatorName"));
        request.setCampusPickupLocation(doc.getString("campusPickupLocation"));
        request.setStudentId(doc.getString("studentId"));
        request.setStudentName(doc.getString("studentName"));
        request.setStudentEmail(doc.getString("studentEmail"));
        request.setStudentIdNumber(doc.getString("studentIdNumber"));
        request.setTransferDate(doc.getString("transferDate"));
        request.setTransferNotes(doc.getString("transferNotes"));
        request.setRequiresIdVerification(doc.getBoolean("requiresIdVerification", true));
        request.setMbtaIncidentNumber(doc.getString("mbtaIncidentNumber"));
        request.setFoundDate(doc.getString("foundDate"));
    }
    
    private void addAirportTransferFields(Document doc, AirportToUniversityTransferRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("lostItemId", request.getLostItemId());  // For auto-closing matched lost item
        doc.put("itemName", request.getItemName());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("itemDescription", request.getItemDescription());
        doc.put("estimatedValue", request.getEstimatedValue());
        doc.put("terminalNumber", request.getTerminalNumber());
        doc.put("airportArea", request.getAirportArea());
        doc.put("airportSpecialistId", request.getAirportSpecialistId());
        doc.put("airportSpecialistName", request.getAirportSpecialistName());
        doc.put("foundLocation", request.getFoundLocation());
        doc.put("universityName", request.getUniversityName());
        doc.put("campusCoordinatorId", request.getCampusCoordinatorId());
        doc.put("campusCoordinatorName", request.getCampusCoordinatorName());
        doc.put("campusPickupLocation", request.getCampusPickupLocation());
        doc.put("studentId", request.getStudentId());
        doc.put("studentName", request.getStudentName());
        doc.put("studentEmail", request.getStudentEmail());
        doc.put("studentIdNumber", request.getStudentIdNumber());
        doc.put("flightNumber", request.getFlightNumber());
        doc.put("policeOfficerId", request.getPoliceOfficerId());
        doc.put("policeOfficerName", request.getPoliceOfficerName());
        doc.put("requiresPoliceVerification", request.isRequiresPoliceVerification());
        doc.put("securityClearanceLevel", request.getSecurityClearanceLevel());
        doc.put("airportIncidentNumber", request.getAirportIncidentNumber());
        doc.put("tsamanagerInvolved", request.getTsamanagerInvolved());
        doc.put("foundDateTime", request.getFoundDateTime());
        doc.put("wasInSecureArea", request.isWasInSecureArea());
        doc.put("transferDate", request.getTransferDate());
        doc.put("transferNotes", request.getTransferNotes());
        doc.put("securityNotes", request.getSecurityNotes());
    }
    
    private void loadAirportTransferFields(Document doc, AirportToUniversityTransferRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setLostItemId(doc.getString("lostItemId"));  // For auto-closing matched lost item
        request.setItemName(doc.getString("itemName"));
        request.setItemCategory(doc.getString("itemCategory"));
        request.setItemDescription(doc.getString("itemDescription"));
        request.setEstimatedValue(doc.getDouble("estimatedValue"));
        request.setTerminalNumber(doc.getString("terminalNumber"));
        request.setAirportArea(doc.getString("airportArea"));
        request.setAirportSpecialistId(doc.getString("airportSpecialistId"));
        request.setAirportSpecialistName(doc.getString("airportSpecialistName"));
        request.setFoundLocation(doc.getString("foundLocation"));
        request.setUniversityName(doc.getString("universityName"));
        request.setCampusCoordinatorId(doc.getString("campusCoordinatorId"));
        request.setCampusCoordinatorName(doc.getString("campusCoordinatorName"));
        request.setCampusPickupLocation(doc.getString("campusPickupLocation"));
        request.setStudentId(doc.getString("studentId"));
        request.setStudentName(doc.getString("studentName"));
        request.setStudentEmail(doc.getString("studentEmail"));
        request.setStudentIdNumber(doc.getString("studentIdNumber"));
        request.setFlightNumber(doc.getString("flightNumber"));
        request.setPoliceOfficerId(doc.getString("policeOfficerId"));
        request.setPoliceOfficerName(doc.getString("policeOfficerName"));
        request.setRequiresPoliceVerification(doc.getBoolean("requiresPoliceVerification", true));
        request.setSecurityClearanceLevel(doc.getString("securityClearanceLevel"));
        request.setAirportIncidentNumber(doc.getString("airportIncidentNumber"));
        request.setTsamanagerInvolved(doc.getString("tsamanagerInvolved"));
        request.setFoundDateTime(doc.getString("foundDateTime"));
        request.setWasInSecureArea(doc.getBoolean("wasInSecureArea", false));
        request.setTransferDate(doc.getString("transferDate"));
        request.setTransferNotes(doc.getString("transferNotes"));
        request.setSecurityNotes(doc.getString("securityNotes"));
    }
    
    private void addPoliceEvidenceFields(Document doc, PoliceEvidenceRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("itemName", request.getItemName());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("itemDescription", request.getItemDescription());
        doc.put("estimatedValue", request.getEstimatedValue());
        doc.put("serialNumber", request.getSerialNumber());
        doc.put("modelNumber", request.getModelNumber());
        doc.put("brandName", request.getBrandName());
        doc.put("imeiNumber", request.getImeiNumber());
        doc.put("otherIdentifiers", request.getOtherIdentifiers());
        doc.put("sourceEnterpriseName", request.getSourceEnterpriseName());
        doc.put("sourceOrganizationName", request.getSourceOrganizationName());
        doc.put("coordinatorId", request.getCoordinatorId());
        doc.put("coordinatorName", request.getCoordinatorName());
        doc.put("foundLocationId", request.getFoundLocationId());
        doc.put("foundLocationName", request.getFoundLocationName());
        doc.put("policeOfficerId", request.getPoliceOfficerId());
        doc.put("policeOfficerName", request.getPoliceOfficerName());
        doc.put("policeDepartment", request.getPoliceDepartment());
        doc.put("caseNumber", request.getCaseNumber());
        doc.put("verificationReason", request.getVerificationReason());
        doc.put("isStolenCheck", request.isStolenCheck());
        doc.put("isHighValueVerification", request.isHighValueVerification());
        doc.put("requiresSerialCheck", request.isRequiresSerialCheck());
        doc.put("verificationStatus", request.getVerificationStatus());
        doc.put("verificationNotes", request.getVerificationNotes());
        doc.put("matchesStoredReport", request.isMatchesStoredReport());
        doc.put("stolenReportId", request.getStolenReportId());
        doc.put("submittedDateTime", request.getSubmittedDateTime());
        doc.put("urgencyLevel", request.getUrgencyLevel());
        doc.put("evidencePhotoUrl", request.getEvidencePhotoUrl());
    }
    
    private void loadPoliceEvidenceFields(Document doc, PoliceEvidenceRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setItemName(doc.getString("itemName"));
        request.setItemCategory(doc.getString("itemCategory"));
        request.setItemDescription(doc.getString("itemDescription"));
        request.setEstimatedValue(doc.getDouble("estimatedValue"));
        request.setSerialNumber(doc.getString("serialNumber"));
        request.setModelNumber(doc.getString("modelNumber"));
        request.setBrandName(doc.getString("brandName"));
        request.setImeiNumber(doc.getString("imeiNumber"));
        request.setOtherIdentifiers(doc.getString("otherIdentifiers"));
        request.setSourceEnterpriseName(doc.getString("sourceEnterpriseName"));
        request.setSourceOrganizationName(doc.getString("sourceOrganizationName"));
        request.setCoordinatorId(doc.getString("coordinatorId"));
        request.setCoordinatorName(doc.getString("coordinatorName"));
        request.setFoundLocationId(doc.getString("foundLocationId"));
        request.setFoundLocationName(doc.getString("foundLocationName"));
        request.setPoliceOfficerId(doc.getString("policeOfficerId"));
        request.setPoliceOfficerName(doc.getString("policeOfficerName"));
        request.setPoliceDepartment(doc.getString("policeDepartment"));
        request.setCaseNumber(doc.getString("caseNumber"));
        request.setVerificationReason(doc.getString("verificationReason"));
        request.setStolenCheck(doc.getBoolean("isStolenCheck", false));
        request.setHighValueVerification(doc.getBoolean("isHighValueVerification", false));
        request.setRequiresSerialCheck(doc.getBoolean("requiresSerialCheck", true));
        request.setVerificationStatus(doc.getString("verificationStatus"));
        request.setVerificationNotes(doc.getString("verificationNotes"));
        request.setMatchesStoredReport(doc.getBoolean("matchesStoredReport", false));
        request.setStolenReportId(doc.getString("stolenReportId"));
        request.setSubmittedDateTime(doc.getString("submittedDateTime"));
        request.setUrgencyLevel(doc.getString("urgencyLevel"));
        request.setEvidencePhotoUrl(doc.getString("evidencePhotoUrl"));
    }
    
    // ==================== MBTA TO AIRPORT EMERGENCY FIELDS ====================
    
    private void addMBTAToAirportEmergencyFields(Document doc, MBTAToAirportEmergencyRequest request) {
        doc.put("itemId", request.getItemId());
        doc.put("itemName", request.getItemName());
        doc.put("itemDescription", request.getItemDescription());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("mbtaStationId", request.getMbtaStationId());
        doc.put("mbtaStationName", request.getMbtaStationName());
        doc.put("mbtaStationManagerId", request.getMbtaStationManagerId());
        doc.put("mbtaStationManagerName", request.getMbtaStationManagerName());
        doc.put("transitLine", request.getTransitLine());
        doc.put("foundLocation", request.getFoundLocation());
        doc.put("foundDateTime", request.getFoundDateTime());
        doc.put("mbtaIncidentNumber", request.getMbtaIncidentNumber());
        doc.put("airportTerminal", request.getAirportTerminal());
        doc.put("airportGate", request.getAirportGate());
        doc.put("airportSpecialistId", request.getAirportSpecialistId());
        doc.put("airportSpecialistName", request.getAirportSpecialistName());
        doc.put("airportContactPhone", request.getAirportContactPhone());
        doc.put("travelerId", request.getTravelerId());
        doc.put("travelerName", request.getTravelerName());
        doc.put("travelerPhone", request.getTravelerPhone());
        doc.put("travelerEmail", request.getTravelerEmail());
        doc.put("flightNumber", request.getFlightNumber());
        doc.put("flightDepartureTime", request.getFlightDepartureTime());
        doc.put("airline", request.getAirline());
        doc.put("destinationCity", request.getDestinationCity());
        doc.put("emergencyContactNumber", request.getEmergencyContactNumber());
        doc.put("courierMethod", request.getCourierMethod());
        doc.put("estimatedDeliveryTime", request.getEstimatedDeliveryTime());
        doc.put("policeEscortRequested", request.isPoliceEscortRequested());
        doc.put("gateHoldRequested", request.isGateHoldRequested());
        doc.put("gateHoldStatus", request.getGateHoldStatus());
        doc.put("documentType", request.getDocumentType());
        doc.put("documentNumber", request.getDocumentNumber());
        doc.put("documentIssuingCountry", request.getDocumentIssuingCountry());
        doc.put("documentPhotoMatch", request.isDocumentPhotoMatch());
        doc.put("pickupConfirmationCode", request.getPickupConfirmationCode());
        doc.put("deliveryConfirmationCode", request.getDeliveryConfirmationCode());
        doc.put("currentLocationStatus", request.getCurrentLocationStatus());
        doc.put("deliveryNotes", request.getDeliveryNotes());
    }
    
    private void loadMBTAToAirportEmergencyFields(Document doc, MBTAToAirportEmergencyRequest request) {
        request.setItemId(doc.getString("itemId"));
        request.setItemName(doc.getString("itemName"));
        request.setItemDescription(doc.getString("itemDescription"));
        request.setItemCategory(doc.getString("itemCategory"));
        request.setMbtaStationId(doc.getString("mbtaStationId"));
        request.setMbtaStationName(doc.getString("mbtaStationName"));
        request.setMbtaStationManagerId(doc.getString("mbtaStationManagerId"));
        request.setMbtaStationManagerName(doc.getString("mbtaStationManagerName"));
        request.setTransitLine(doc.getString("transitLine"));
        request.setFoundLocation(doc.getString("foundLocation"));
        request.setFoundDateTime(doc.getString("foundDateTime"));
        request.setMbtaIncidentNumber(doc.getString("mbtaIncidentNumber"));
        request.setAirportTerminal(doc.getString("airportTerminal"));
        request.setAirportGate(doc.getString("airportGate"));
        request.setAirportSpecialistId(doc.getString("airportSpecialistId"));
        request.setAirportSpecialistName(doc.getString("airportSpecialistName"));
        request.setAirportContactPhone(doc.getString("airportContactPhone"));
        request.setTravelerId(doc.getString("travelerId"));
        request.setTravelerName(doc.getString("travelerName"));
        request.setTravelerPhone(doc.getString("travelerPhone"));
        request.setTravelerEmail(doc.getString("travelerEmail"));
        request.setFlightNumber(doc.getString("flightNumber"));
        request.setFlightDepartureTime(doc.getString("flightDepartureTime"));
        request.setAirline(doc.getString("airline"));
        request.setDestinationCity(doc.getString("destinationCity"));
        request.setEmergencyContactNumber(doc.getString("emergencyContactNumber"));
        request.setCourierMethod(doc.getString("courierMethod"));
        request.setEstimatedDeliveryTime(doc.getString("estimatedDeliveryTime"));
        request.setPoliceEscortRequested(doc.getBoolean("policeEscortRequested", false));
        request.setGateHoldRequested(doc.getBoolean("gateHoldRequested", false));
        request.setGateHoldStatus(doc.getString("gateHoldStatus"));
        request.setDocumentType(doc.getString("documentType"));
        request.setDocumentNumber(doc.getString("documentNumber"));
        request.setDocumentIssuingCountry(doc.getString("documentIssuingCountry"));
        request.setDocumentPhotoMatch(doc.getBoolean("documentPhotoMatch", false));
        request.setPickupConfirmationCode(doc.getString("pickupConfirmationCode"));
        request.setDeliveryConfirmationCode(doc.getString("deliveryConfirmationCode"));
        request.setCurrentLocationStatus(doc.getString("currentLocationStatus"));
        request.setDeliveryNotes(doc.getString("deliveryNotes"));
    }
    
    // ==================== MULTI ENTERPRISE DISPUTE FIELDS ====================
    
    private void addMultiEnterpriseDisputeFields(Document doc, MultiEnterpriseDisputeResolution request) {
        doc.put("itemId", request.getItemId());
        doc.put("itemName", request.getItemName());
        doc.put("itemDescription", request.getItemDescription());
        doc.put("itemCategory", request.getItemCategory());
        doc.put("estimatedValue", request.getEstimatedValue());
        doc.put("itemCurrentLocation", request.getItemCurrentLocation());
        doc.put("holdingEnterpriseId", request.getHoldingEnterpriseId());
        doc.put("holdingEnterpriseName", request.getHoldingEnterpriseName());
        doc.put("involvedEnterpriseIds", request.getInvolvedEnterpriseIds());
        doc.put("involvedEnterpriseNames", request.getInvolvedEnterpriseNames());
        doc.put("panelVotesRequired", request.getPanelVotesRequired());
        doc.put("panelVotesReceived", request.getPanelVotesReceived());
        doc.put("disputeType", request.getDisputeType());
        doc.put("disputeReason", request.getDisputeReason());
        doc.put("disputeInitiatedBy", request.getDisputeInitiatedBy());
        doc.put("disputeInitiatedByName", request.getDisputeInitiatedByName());
        doc.put("resolutionStatus", request.getResolutionStatus());
        doc.put("resolutionDecision", request.getResolutionDecision());
        doc.put("winningClaimantId", request.getWinningClaimantId());
        doc.put("winningClaimantName", request.getWinningClaimantName());
        doc.put("resolutionReason", request.getResolutionReason());
        doc.put("resolutionNotes", request.getResolutionNotes());
        doc.put("policeInvolved", request.isPoliceInvolved());
        doc.put("policeOfficerId", request.getPoliceOfficerId());
        doc.put("policeOfficerName", request.getPoliceOfficerName());
        doc.put("policeReportNumber", request.getPoliceReportNumber());
        doc.put("policeFindingsReport", request.getPoliceFindingsReport());
        doc.put("disputeStartDate", request.getDisputeStartDate());
        doc.put("evidenceDeadline", request.getEvidenceDeadline());
        doc.put("panelReviewDate", request.getPanelReviewDate());
        doc.put("resolutionDeadline", request.getResolutionDeadline());
        
        // Convert claimants to Documents
        List<Document> claimantDocs = new ArrayList<>();
        for (MultiEnterpriseDisputeResolution.Claimant c : request.getClaimants()) {
            Document cd = new Document();
            cd.put("claimantId", c.claimantId);
            cd.put("claimantName", c.claimantName);
            cd.put("claimantEmail", c.claimantEmail);
            cd.put("claimantPhone", c.claimantPhone);
            cd.put("enterpriseId", c.enterpriseId);
            cd.put("enterpriseName", c.enterpriseName);
            cd.put("organizationId", c.organizationId);
            cd.put("organizationName", c.organizationName);
            cd.put("claimDescription", c.claimDescription);
            cd.put("proofDescription", c.proofDescription);
            cd.put("trustScore", c.trustScore);
            cd.put("claimSubmittedDate", c.claimSubmittedDate);
            cd.put("claimStatus", c.claimStatus);
            cd.put("evidenceIds", c.evidenceIds);
            claimantDocs.add(cd);
        }
        doc.put("claimants", claimantDocs);
        
        // Convert panel members to Documents
        List<Document> panelDocs = new ArrayList<>();
        for (MultiEnterpriseDisputeResolution.PanelMember pm : request.getVerificationPanel()) {
            Document pd = new Document();
            pd.put("memberId", pm.memberId);
            pd.put("memberName", pm.memberName);
            pd.put("role", pm.role);
            pd.put("enterpriseId", pm.enterpriseId);
            pd.put("enterpriseName", pm.enterpriseName);
            pd.put("hasVoted", pm.hasVoted);
            pd.put("votedForClaimantId", pm.votedForClaimantId);
            pd.put("voteReason", pm.voteReason);
            pd.put("voteDate", pm.voteDate);
            panelDocs.add(pd);
        }
        doc.put("verificationPanel", panelDocs);
        
        // Convert evidence items to Documents
        List<Document> evidenceDocs = new ArrayList<>();
        for (MultiEnterpriseDisputeResolution.EvidenceItem e : request.getEvidenceItems()) {
            Document ed = new Document();
            ed.put("evidenceId", e.evidenceId);
            ed.put("submittedById", e.submittedById);
            ed.put("submittedByName", e.submittedByName);
            ed.put("forClaimantId", e.forClaimantId);
            ed.put("evidenceType", e.evidenceType);
            ed.put("description", e.description);
            ed.put("documentPath", e.documentPath);
            ed.put("submittedDate", e.submittedDate);
            ed.put("verified", e.verified);
            ed.put("verifiedById", e.verifiedById);
            ed.put("verificationResult", e.verificationResult);
            ed.put("verificationDate", e.verificationDate);
            ed.put("verificationNotes", e.verificationNotes);
            evidenceDocs.add(ed);
        }
        doc.put("evidenceItems", evidenceDocs);
    }
    
    private void loadMultiEnterpriseDisputeFields(Document doc, MultiEnterpriseDisputeResolution request) {
        request.setItemId(doc.getString("itemId"));
        request.setItemName(doc.getString("itemName"));
        request.setItemDescription(doc.getString("itemDescription"));
        request.setItemCategory(doc.getString("itemCategory"));
        Double estValue = doc.getDouble("estimatedValue");
        request.setEstimatedValue(estValue != null ? estValue : 0.0);
        request.setItemCurrentLocation(doc.getString("itemCurrentLocation"));
        request.setHoldingEnterpriseId(doc.getString("holdingEnterpriseId"));
        request.setHoldingEnterpriseName(doc.getString("holdingEnterpriseName"));
        request.setInvolvedEnterpriseIds(doc.getList("involvedEnterpriseIds", String.class, new ArrayList<>()));
        request.setInvolvedEnterpriseNames(doc.getList("involvedEnterpriseNames", String.class, new ArrayList<>()));
        request.setPanelVotesRequired(doc.getInteger("panelVotesRequired", 3));
        request.setPanelVotesReceived(doc.getInteger("panelVotesReceived", 0));
        request.setDisputeType(doc.getString("disputeType"));
        request.setDisputeReason(doc.getString("disputeReason"));
        request.setDisputeInitiatedBy(doc.getString("disputeInitiatedBy"));
        request.setDisputeInitiatedByName(doc.getString("disputeInitiatedByName"));
        request.setResolutionStatus(doc.getString("resolutionStatus"));
        request.setResolutionDecision(doc.getString("resolutionDecision"));
        request.setWinningClaimantId(doc.getString("winningClaimantId"));
        request.setWinningClaimantName(doc.getString("winningClaimantName"));
        request.setResolutionReason(doc.getString("resolutionReason"));
        request.setResolutionNotes(doc.getString("resolutionNotes"));
        request.setPoliceInvolved(doc.getBoolean("policeInvolved", false));
        request.setPoliceOfficerId(doc.getString("policeOfficerId"));
        request.setPoliceOfficerName(doc.getString("policeOfficerName"));
        request.setPoliceReportNumber(doc.getString("policeReportNumber"));
        request.setPoliceFindingsReport(doc.getString("policeFindingsReport"));
        request.setDisputeStartDate(doc.getString("disputeStartDate"));
        request.setEvidenceDeadline(doc.getString("evidenceDeadline"));
        request.setPanelReviewDate(doc.getString("panelReviewDate"));
        request.setResolutionDeadline(doc.getString("resolutionDeadline"));
        
        // Load claimants
        List<Document> claimantDocs = doc.getList("claimants", Document.class, new ArrayList<>());
        List<MultiEnterpriseDisputeResolution.Claimant> claimants = new ArrayList<>();
        for (Document cd : claimantDocs) {
            MultiEnterpriseDisputeResolution.Claimant c = new MultiEnterpriseDisputeResolution.Claimant();
            c.claimantId = cd.getString("claimantId");
            c.claimantName = cd.getString("claimantName");
            c.claimantEmail = cd.getString("claimantEmail");
            c.claimantPhone = cd.getString("claimantPhone");
            c.enterpriseId = cd.getString("enterpriseId");
            c.enterpriseName = cd.getString("enterpriseName");
            c.organizationId = cd.getString("organizationId");
            c.organizationName = cd.getString("organizationName");
            c.claimDescription = cd.getString("claimDescription");
            c.proofDescription = cd.getString("proofDescription");
            Double ts = cd.getDouble("trustScore");
            c.trustScore = ts != null ? ts : 0.0;
            c.claimSubmittedDate = cd.getString("claimSubmittedDate");
            c.claimStatus = cd.getString("claimStatus");
            c.evidenceIds = cd.getList("evidenceIds", String.class, new ArrayList<>());
            claimants.add(c);
        }
        request.setClaimants(claimants);
        
        // Load panel members
        List<Document> panelDocs = doc.getList("verificationPanel", Document.class, new ArrayList<>());
        List<MultiEnterpriseDisputeResolution.PanelMember> panel = new ArrayList<>();
        for (Document pd : panelDocs) {
            MultiEnterpriseDisputeResolution.PanelMember pm = new MultiEnterpriseDisputeResolution.PanelMember();
            pm.memberId = pd.getString("memberId");
            pm.memberName = pd.getString("memberName");
            pm.role = pd.getString("role");
            pm.enterpriseId = pd.getString("enterpriseId");
            pm.enterpriseName = pd.getString("enterpriseName");
            pm.hasVoted = pd.getBoolean("hasVoted", false);
            pm.votedForClaimantId = pd.getString("votedForClaimantId");
            pm.voteReason = pd.getString("voteReason");
            pm.voteDate = pd.getString("voteDate");
            panel.add(pm);
        }
        request.setVerificationPanel(panel);
        
        // Load evidence items
        List<Document> evidenceDocs = doc.getList("evidenceItems", Document.class, new ArrayList<>());
        List<MultiEnterpriseDisputeResolution.EvidenceItem> evidence = new ArrayList<>();
        for (Document ed : evidenceDocs) {
            MultiEnterpriseDisputeResolution.EvidenceItem e = new MultiEnterpriseDisputeResolution.EvidenceItem();
            e.evidenceId = ed.getString("evidenceId");
            e.submittedById = ed.getString("submittedById");
            e.submittedByName = ed.getString("submittedByName");
            e.forClaimantId = ed.getString("forClaimantId");
            e.evidenceType = ed.getString("evidenceType");
            e.description = ed.getString("description");
            e.documentPath = ed.getString("documentPath");
            e.submittedDate = ed.getString("submittedDate");
            e.verified = ed.getBoolean("verified", false);
            e.verifiedById = ed.getString("verifiedById");
            e.verificationResult = ed.getString("verificationResult");
            e.verificationDate = ed.getString("verificationDate");
            e.verificationNotes = ed.getString("verificationNotes");
            evidence.add(e);
        }
        request.setEvidenceItems(evidence);
    }
    
    // ==================== UTILITY METHODS ====================
    
    private Date localDateTimeToDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
