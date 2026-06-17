package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScoreEvent;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;
import com.campus.lostfound.services.TrustScoreService;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates the complete 4-enterprise ecosystem with test data
 */
public class MongoDataGenerator {

    private static final Logger LOGGER = Logger.getLogger(MongoDataGenerator.class.getName());

    private MongoNetworkDAO networkDAO;
    private MongoEnterpriseDAO enterpriseDAO;
    private MongoOrganizationDAO organizationDAO;
    private MongoUserDAO userDAO;
    private MongoBuildingDAO buildingDAO;
    private MongoItemDAO itemDAO;
    private MongoWorkRequestDAO workRequestDAO;
    private MongoTrustScoreDAO trustScoreDAO;
    private TrustScoreService trustScoreService;

    private Network network;
    private Map<String, Enterprise> enterprises;
    private Map<String, List<Organization>> organizations;
    private List<User> users;
    private List<Building> buildings;
    private Random random;

    public MongoDataGenerator() {
        this.networkDAO = new MongoNetworkDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.userDAO = new MongoUserDAO();
        this.buildingDAO = new MongoBuildingDAO();
        this.itemDAO = new MongoItemDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.trustScoreService = new TrustScoreService();
        
        this.enterprises = new HashMap<>();
        this.organizations = new HashMap<>();
        this.users = new ArrayList<>();
        this.buildings = new ArrayList<>();
        this.random = new Random();
    }

    public void generateFullEcosystem() {
        LOGGER.info("🚀 Starting Greater Boston Lost & Found Ecosystem Generation...");

        // Step 1: Create Network
        createNetwork();

        // Step 2: Create 4 Enterprises
        createEnterprises();

        // Step 3: Create Organizations within each enterprise
        createOrganizations();

        // Step 4: Create Users with enterprise/org assignments
        createUsers();

        // Step 5: Create Buildings with enterprise/org assignments
        createBuildings();

        // Step 6: Create sample items
        createItems();
        
        // Step 7: Create sample work requests
        createWorkRequests();
        
        // Step 8: Create trust scores and events
        createTrustScores();

        LOGGER.info("✅ Full ecosystem generation completed!");
        printSummary();
    }

    private void createNetwork() {
        LOGGER.info("📡 Creating Network...");
        
        Network network = new Network();
        network.setName("Greater Boston Lost & Found Network");
        network.setDescription("Cross-enterprise collaboration system for lost and found item recovery across universities, MBTA, Logan Airport, and law enforcement");
        
        String networkId = networkDAO.create(network);
        network.setNetworkId(networkId);
        this.network = network;
        
        LOGGER.info("✓ Network created: " + networkId);
    }

    private void createEnterprises() {
        LOGGER.info("🏢 Creating 4 Enterprises...");

        // Enterprise 1: Higher Education
        Enterprise higherEd = new Enterprise(null, network.getNetworkId(), 
            "Northeastern University", Enterprise.EnterpriseType.HIGHER_EDUCATION);
        higherEd.setDescription("Major research university in Boston");
        higherEd.setContactEmail("lostandfound@northeastern.edu");
        higherEd.setEnterpriseId(enterpriseDAO.create(higherEd));
        enterprises.put("higher_ed", higherEd);
        LOGGER.info("✓ Created: " + higherEd.getName());

        // Enterprise 2: MBTA
        Enterprise mbta = new Enterprise(null, network.getNetworkId(),
            "MBTA (Massachusetts Bay Transportation Authority)", Enterprise.EnterpriseType.PUBLIC_TRANSIT);
        mbta.setDescription("Boston's public transportation system");
        mbta.setContactEmail("lostandfound@mbta.com");
        mbta.setEnterpriseId(enterpriseDAO.create(mbta));
        enterprises.put("mbta", mbta);
        LOGGER.info("✓ Created: " + mbta.getName());

        // Enterprise 3: Logan Airport
        Enterprise airport = new Enterprise(null, network.getNetworkId(),
            "Logan International Airport", Enterprise.EnterpriseType.AIRPORT);
        airport.setDescription("Boston's primary airport");
        airport.setContactEmail("lostandfound@massport.com");
        airport.setEnterpriseId(enterpriseDAO.create(airport));
        enterprises.put("airport", airport);
        LOGGER.info("✓ Created: " + airport.getName());

        // Enterprise 4: Boston Police
        Enterprise police = new Enterprise(null, network.getNetworkId(),
            "Boston Police Department", Enterprise.EnterpriseType.LAW_ENFORCEMENT);
        police.setDescription("Law enforcement and evidence management");
        police.setContactEmail("evidence@bostonpolice.gov");
        police.setEnterpriseId(enterpriseDAO.create(police));
        enterprises.put("police", police);
        LOGGER.info("✓ Created: " + police.getName());
    }

    private void createOrganizations() {
        LOGGER.info("🏛️ Creating Organizations...");

        // Higher Education Organizations
        Enterprise higherEd = enterprises.get("higher_ed");
        List<Organization> higherEdOrgs = new ArrayList<>();
        
        Organization neuCampusOps = new Organization(null, higherEd.getEnterpriseId(),
            "NEU Campus Operations", Organization.OrganizationType.CAMPUS_OPERATIONS);
        neuCampusOps.setDescription("Northeastern campus-wide lost and found");
        neuCampusOps.setOrganizationId(organizationDAO.create(neuCampusOps));
        higherEdOrgs.add(neuCampusOps);
        
        Organization neuSecurity = new Organization(null, higherEd.getEnterpriseId(),
            "NEU Campus Security", Organization.OrganizationType.CAMPUS_SECURITY);
        neuSecurity.setDescription("Campus security and high-value item management");
        neuSecurity.setOrganizationId(organizationDAO.create(neuSecurity));
        higherEdOrgs.add(neuSecurity);
        
        organizations.put("higher_ed", higherEdOrgs);
        LOGGER.info("✓ Created 2 organizations for Higher Education");

        // MBTA Organizations
        Enterprise mbta = enterprises.get("mbta");
        List<Organization> mbtaOrgs = new ArrayList<>();
        
        Organization stationOps = new Organization(null, mbta.getEnterpriseId(),
            "MBTA Station Operations", Organization.OrganizationType.STATION_OPERATIONS);
        stationOps.setDescription("Station-level lost and found");
        stationOps.setOrganizationId(organizationDAO.create(stationOps));
        mbtaOrgs.add(stationOps);
        
        Organization transitPolice = new Organization(null, mbta.getEnterpriseId(),
            "MBTA Transit Police", Organization.OrganizationType.TRANSIT_POLICE);
        transitPolice.setDescription("Transit security and high-value items");
        transitPolice.setOrganizationId(organizationDAO.create(transitPolice));
        mbtaOrgs.add(transitPolice);
        
        organizations.put("mbta", mbtaOrgs);
        LOGGER.info("✓ Created 2 organizations for MBTA");

        // Airport Organizations
        Enterprise airport = enterprises.get("airport");
        List<Organization> airportOrgs = new ArrayList<>();
        
        Organization airportOps = new Organization(null, airport.getEnterpriseId(),
            "Airport Operations", Organization.OrganizationType.AIRPORT_OPERATIONS);
        airportOps.setDescription("Airport-wide lost and found");
        airportOps.setOrganizationId(organizationDAO.create(airportOps));
        airportOrgs.add(airportOps);
        
        Organization tsa = new Organization(null, airport.getEnterpriseId(),
            "TSA Security", Organization.OrganizationType.TSA_SECURITY);
        tsa.setDescription("TSA security screening and confiscated items");
        tsa.setOrganizationId(organizationDAO.create(tsa));
        airportOrgs.add(tsa);
        
        organizations.put("airport", airportOrgs);
        LOGGER.info("✓ Created 2 organizations for Airport");

        // Police Organizations
        Enterprise police = enterprises.get("police");
        List<Organization> policeOrgs = new ArrayList<>();
        
        Organization policeDept = new Organization(null, police.getEnterpriseId(),
            "Boston Police Department", Organization.OrganizationType.POLICE_DEPARTMENT);
        policeDept.setDescription("General police services");
        policeDept.setOrganizationId(organizationDAO.create(policeDept));
        policeOrgs.add(policeDept);
        
        Organization evidenceMgmt = new Organization(null, police.getEnterpriseId(),
            "Evidence Management", Organization.OrganizationType.EVIDENCE_MANAGEMENT);
        evidenceMgmt.setDescription("Evidence and property room");
        evidenceMgmt.setOrganizationId(organizationDAO.create(evidenceMgmt));
        policeOrgs.add(evidenceMgmt);
        
        organizations.put("police", policeOrgs);
        LOGGER.info("✓ Created 2 organizations for Police");
    }

    private void createUsers() {
        LOGGER.info("👥 Creating Users across all enterprises...");

        // Higher Education Users
        createEnterpriseUsers("higher_ed", new String[][]{
            {"john.student", "John", "Student", "STUDENT"},
            {"sarah.coord", "Sarah", "Martinez", "CAMPUS_COORDINATOR"},
            {"mike.security", "Mike", "Johnson", "CAMPUS_SECURITY"}
        });

        // MBTA Users
        createEnterpriseUsers("mbta", new String[][]{
            {"tom.manager", "Tom", "Wilson", "STATION_MANAGER"},
            {"lisa.clerk", "Lisa", "Garcia", "LOST_FOUND_CLERK"},
            {"chris.officer", "Chris", "Brown", "TRANSIT_OFFICER"}
        });

        // Airport Users
        createEnterpriseUsers("airport", new String[][]{
            {"emily.specialist", "Emily", "Davis", "AIRPORT_LOST_FOUND_SPECIALIST"},
            {"david.tsa", "David", "Miller", "TSA_SECURITY_COORDINATOR"}
        });

        // Police Users
        createEnterpriseUsers("police", new String[][]{
            {"amy.custodian", "Amy", "Taylor", "POLICE_EVIDENCE_CUSTODIAN"},
            {"james.detective", "James", "Anderson", "DETECTIVE"}
        });

        // Create system admin
        User admin = new User("admin@system.com", "System", "Admin", User.UserRole.SYSTEM_ADMIN);
        admin.setEnterpriseId(enterprises.get("higher_ed").getEnterpriseId());
        admin.setOrganizationId(organizations.get("higher_ed").get(0).getOrganizationId());
        userDAO.create(admin, "admin123");
        users.add(admin);
        
        LOGGER.info("✓ Created " + users.size() + " users across all enterprises");
    }

    private void createEnterpriseUsers(String enterpriseKey, String[][] userData) {
        Enterprise enterprise = enterprises.get(enterpriseKey);
        List<Organization> orgs = organizations.get(enterpriseKey);
        
        for (String[] data : userData) {
            Organization org = orgs.get(random.nextInt(orgs.size()));
            
            String email = data[0] + "@" + enterpriseKey + ".com";
            User user = new User(email, data[1], data[2], User.UserRole.valueOf(data[3]));
            user.setEnterpriseId(enterprise.getEnterpriseId());
            user.setOrganizationId(org.getOrganizationId());
            user.setPhoneNumber("617-555-" + String.format("%04d", random.nextInt(10000)));
            user.setTrustScore(75 + random.nextDouble() * 25);
            
            userDAO.create(user, "password123");
            users.add(user);
        }
    }

    private void createBuildings() {
        LOGGER.info("🏢 Creating Buildings...");
        
        // NEU Buildings
        createEnterpriseBuilding("higher_ed", "Snell Library", "SL", Building.BuildingType.LIBRARY);
        createEnterpriseBuilding("higher_ed", "Curry Student Center", "CSC", Building.BuildingType.STUDENT_CENTER);
        
        // MBTA Stations
        createEnterpriseBuilding("mbta", "Park Street Station", "PKS", Building.BuildingType.ADMINISTRATIVE);
        createEnterpriseBuilding("mbta", "South Station", "SS", Building.BuildingType.ADMINISTRATIVE);
        
        // Airport Terminals
        createEnterpriseBuilding("airport", "Terminal A", "TA", Building.BuildingType.ADMINISTRATIVE);
        createEnterpriseBuilding("airport", "Terminal B", "TB", Building.BuildingType.ADMINISTRATIVE);
        
        // Police Stations
        createEnterpriseBuilding("police", "District 4 Station", "D4", Building.BuildingType.ADMINISTRATIVE);
        createEnterpriseBuilding("police", "Evidence Warehouse", "EW", Building.BuildingType.ADMINISTRATIVE);
        
        LOGGER.info("✓ Created " + buildings.size() + " buildings");
    }

    private void createEnterpriseBuilding(String enterpriseKey, String name, String code, Building.BuildingType type) {
        Enterprise enterprise = enterprises.get(enterpriseKey);
        List<Organization> orgs = organizations.get(enterpriseKey);
        Organization org = orgs.get(0);
        
        Building building = new Building(name, code, type);
        building.setEnterpriseId(enterprise.getEnterpriseId());
        building.setOrganizationId(org.getOrganizationId());
        buildingDAO.create(building);
        buildings.add(building);
    }

    private void createItems() {
        LOGGER.info("📦 Creating sample items...");
        
        String[][] itemData = {
            {"Black iPhone", "iPhone 14 with cracked screen", "ELECTRONICS", "LOST"},
            {"Blue Backpack", "North Face backpack with laptop", "BAGS", "FOUND"},
            {"Student ID", "NEU student ID card", "IDS_CARDS", "FOUND"},
            {"Silver Watch", "Casio digital watch", "JEWELRY", "LOST"},
            {"Math Textbook", "Calculus 8th Edition", "BOOKS", "FOUND"}
        };
        
        for (String[] data : itemData) {
            if (users.isEmpty() || buildings.isEmpty()) break;
            
            User reporter = users.get(random.nextInt(users.size()));
            Building building = buildings.get(random.nextInt(buildings.size()));
            
            Location location = new Location(building, "101", "Near entrance");
            Item item = new Item(data[0], data[1], 
                Item.ItemCategory.valueOf(data[2]),
                Item.ItemType.valueOf(data[3]),
                location, reporter);
            
            item.setEnterpriseId(building.getEnterpriseId());
            item.setOrganizationId(building.getOrganizationId());
            itemDAO.create(item);
        }
        
        LOGGER.info("✓ Created sample items");
    }
    
    private void createWorkRequests() {
        LOGGER.info("📝 Creating sample work requests...");
        
        int requestCount = 0;
        
        // Get some users for requests
        User student = findUserByRole(User.UserRole.STUDENT);
        User coordinator = findUserByRole(User.UserRole.CAMPUS_COORDINATOR);
        User mbtaManager = findUserByRole(User.UserRole.STATION_MANAGER);
        User airportSpec = findUserByRole(User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
        User policeCustodian = findUserByRole(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
        
        // Create 3 Item Claim Requests (different priorities)
        if (student != null && coordinator != null) {
            // Regular claim
            com.campus.lostfound.models.workrequest.ItemClaimRequest claim1 = 
                new com.campus.lostfound.models.workrequest.ItemClaimRequest(
                    String.valueOf(student.getUserId()), student.getFullName(),
                    "gen-item-1", "Blue Water Bottle", 25.00
                );
            claim1.setClaimDetails("Lost my water bottle in Snell Library yesterday afternoon");
            claim1.setIdentifyingFeatures("Hydro Flask brand, has my name written on bottom");
            claim1.setProofDescription("I can describe the exact location where I left it");
            claim1.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.NORMAL);
            claim1.setRequesterEmail(student.getEmail());
            claim1.setRequesterOrganizationId(student.getOrganizationId());
            claim1.setTargetOrganizationId(coordinator.getOrganizationId());
            workRequestDAO.save(claim1);
            requestCount++;
            
            // High-value claim (URGENT priority)
            com.campus.lostfound.models.workrequest.ItemClaimRequest claim2 = 
                new com.campus.lostfound.models.workrequest.ItemClaimRequest(
                    String.valueOf(student.getUserId()), student.getFullName(),
                    "gen-item-2", "MacBook Pro", 2499.00
                );
            claim2.setClaimDetails("This is my laptop that I use for all my coursework and projects");
            claim2.setIdentifyingFeatures("Serial number ABC123XYZ789, small dent on bottom right corner, NEU sticker on lid");
            claim2.setProofDescription("I have the original purchase receipt from Apple Store, AppleCare+ documentation, and photos of the serial number");
            claim2.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.URGENT);
            claim2.setRequesterEmail(student.getEmail());
            claim2.setRequesterOrganizationId(student.getOrganizationId());
            claim2.setTargetOrganizationId(coordinator.getOrganizationId());
            workRequestDAO.save(claim2);
            requestCount++;
            
            // Medium-value claim (HIGH priority)
            com.campus.lostfound.models.workrequest.ItemClaimRequest claim3 = 
                new com.campus.lostfound.models.workrequest.ItemClaimRequest(
                    String.valueOf(student.getUserId()), student.getFullName(),
                    "gen-item-3", "Beats Headphones", 299.00
                );
            claim3.setClaimDetails("My noise-cancelling headphones that I left in the library");
            claim3.setIdentifyingFeatures("Black Beats Studio Pro, small scratch on right ear cup");
            claim3.setProofDescription("I have the original box with matching serial number");
            claim3.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.HIGH);
            claim3.setRequesterEmail(student.getEmail());
            claim3.setRequesterOrganizationId(student.getOrganizationId());
            claim3.setTargetOrganizationId(coordinator.getOrganizationId());
            workRequestDAO.save(claim3);
            requestCount++;
        }
        
        // Create Cross-Campus Transfer Request
        if (coordinator != null && student != null) {
            com.campus.lostfound.models.workrequest.CrossCampusTransferRequest transfer = 
                new com.campus.lostfound.models.workrequest.CrossCampusTransferRequest(
                    String.valueOf(coordinator.getUserId()), coordinator.getFullName(),
                    "gen-item-4", "Student Backpack",
                    String.valueOf(student.getUserId()), student.getFullName()
                );
            transfer.setSourceCampusName("Northeastern University");
            transfer.setDestinationCampusName("Boston University");
            transfer.setSourceCoordinatorId(String.valueOf(coordinator.getUserId()));
            transfer.setSourceCoordinatorName(coordinator.getFullName());
            transfer.setDestinationCoordinatorId("coord-bu-001");
            transfer.setDestinationCoordinatorName("BU Coordinator");
            transfer.setPickupLocation("BU Student Center - Lost & Found Office");
            transfer.setTransferMethod("Courier");
            transfer.setEstimatedPickupDate("2025-11-26");
            transfer.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.NORMAL);
            transfer.setRequesterEmail(coordinator.getEmail());
            transfer.setRequesterOrganizationId(coordinator.getOrganizationId());
            transfer.setTargetOrganizationId("bu-campus-ops");
            workRequestDAO.save(transfer);
            requestCount++;
        }
        
        // Create Transit to University Transfer
        if (mbtaManager != null && coordinator != null && student != null) {
            com.campus.lostfound.models.workrequest.TransitToUniversityTransferRequest transit = 
                new com.campus.lostfound.models.workrequest.TransitToUniversityTransferRequest(
                    String.valueOf(mbtaManager.getUserId()), mbtaManager.getFullName(),
                    "gen-item-5", "Black Leather Wallet",
                    String.valueOf(student.getUserId()), student.getFullName()
                );
            transit.setTransitType("Subway");
            transit.setRouteNumber("Green Line");
            transit.setStationName("Northeastern Station");
            transit.setUniversityName("Northeastern University");
            transit.setCampusCoordinatorId(String.valueOf(coordinator.getUserId()));
            transit.setCampusCoordinatorName(coordinator.getFullName());
            transit.setCampusPickupLocation("Campus Security Office - Forsyth St");
            transit.setMbtaIncidentNumber("MBTA-2025-" + (1000 + random.nextInt(9000)));
            transit.setFoundDate("2025-11-23");
            transit.setStudentEmail(student.getEmail());
            transit.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.HIGH);
            transit.setRequesterEmail(mbtaManager.getEmail());
            transit.setRequesterOrganizationId(mbtaManager.getOrganizationId());
            transit.setTargetOrganizationId(coordinator.getOrganizationId());
            workRequestDAO.save(transit);
            requestCount++;
        }
        
        // Create Airport to University Transfer  
        if (airportSpec != null && coordinator != null && student != null) {
            com.campus.lostfound.models.workrequest.AirportToUniversityTransferRequest airport = 
                new com.campus.lostfound.models.workrequest.AirportToUniversityTransferRequest(
                    String.valueOf(airportSpec.getUserId()), airportSpec.getFullName(),
                    "gen-item-6", "Laptop Bag with Documents",
                    String.valueOf(student.getUserId()), student.getFullName()
                );
            airport.setTerminalNumber("Terminal B");
            airport.setAirportArea("Security Checkpoint");
            airport.setFoundLocation("Gate B22 - Security Lane 3");
            airport.setUniversityName("Northeastern University");
            airport.setCampusCoordinatorId(String.valueOf(coordinator.getUserId()));
            airport.setCampusCoordinatorName(coordinator.getFullName());
            airport.setCampusPickupLocation("International Student Services Office");
            airport.setAirportIncidentNumber("LOGAN-2025-" + (5000 + random.nextInt(5000)));
            airport.setFlightNumber("DL" + (100 + random.nextInt(900)));
            airport.setFoundDateTime("2025-11-24 08:30");
            airport.setWasInSecureArea(true);
            airport.setSecurityNotes("Item found in secure area after screening. TSA cleared. No prohibited items.");
            airport.setEstimatedValue(150.00);
            airport.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.HIGH);
            airport.setRequesterEmail(airportSpec.getEmail());
            airport.setRequesterOrganizationId(airportSpec.getOrganizationId());
            airport.setTargetOrganizationId(coordinator.getOrganizationId());
            workRequestDAO.save(airport);
            requestCount++;
        }
        
        // Create Police Evidence Requests (2 scenarios)
        if (coordinator != null && policeCustodian != null) {
            // Scenario 1: High-value verification
            com.campus.lostfound.models.workrequest.PoliceEvidenceRequest evidence1 = 
                new com.campus.lostfound.models.workrequest.PoliceEvidenceRequest(
                    String.valueOf(coordinator.getUserId()), coordinator.getFullName(),
                    "gen-item-7", "iPhone 15 Pro",
                    "High-value item requires serial number verification against stolen database"
                );
            evidence1.setSerialNumber("APPL" + random.nextInt(100000000));
            evidence1.setImeiNumber("35" + (100000000000000L + random.nextInt(899999999)));
            evidence1.setBrandName("Apple");
            evidence1.setModelNumber("iPhone 15 Pro 256GB");
            evidence1.setItemCategory("Electronics");
            evidence1.setEstimatedValue(1199.00);
            evidence1.setSourceEnterpriseName("Northeastern University");
            evidence1.setHighValueVerification(true);
            evidence1.setStolenCheck(true);
            evidence1.setUrgencyLevel("High");
            evidence1.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.URGENT);
            evidence1.setRequesterEmail(coordinator.getEmail());
            evidence1.setRequesterOrganizationId(coordinator.getOrganizationId());
            evidence1.setTargetOrganizationId(policeCustodian.getOrganizationId());
            workRequestDAO.save(evidence1);
            requestCount++;
            
            // Scenario 2: Suspected stolen item
            com.campus.lostfound.models.workrequest.PoliceEvidenceRequest evidence2 = 
                new com.campus.lostfound.models.workrequest.PoliceEvidenceRequest(
                    String.valueOf(coordinator.getUserId()), coordinator.getFullName(),
                    "gen-item-8", "Suspicious Laptop",
                    "Item has serial number filed off - potential stolen property"
                );
            evidence2.setBrandName("Dell");
            evidence2.setModelNumber("XPS 13");
            evidence2.setItemCategory("Electronics");
            evidence2.setEstimatedValue(999.00);
            evidence2.setSourceEnterpriseName("Northeastern University");
            evidence2.setStolenCheck(true);
            evidence2.setUrgencyLevel("Urgent");
            evidence2.setVerificationReason("Serial number removed - suspected stolen");
            evidence2.setPriority(com.campus.lostfound.models.workrequest.WorkRequest.RequestPriority.URGENT);
            evidence2.setRequesterEmail(coordinator.getEmail());
            evidence2.setRequesterOrganizationId(coordinator.getOrganizationId());
            evidence2.setTargetOrganizationId(policeCustodian.getOrganizationId());
            workRequestDAO.save(evidence2);
            requestCount++;
        }
        
        LOGGER.info("✓ Created " + requestCount + " work requests across all 5 types");
        LOGGER.info("   - Item Claims: 3 (NORMAL, HIGH, URGENT priorities)");
        LOGGER.info("   - Cross-Campus Transfer: 1");
        LOGGER.info("   - Transit Transfer: 1");
        LOGGER.info("   - Airport Transfer: 1");
        LOGGER.info("   - Police Evidence: 2 (both URGENT)");
    }
    
    private User findUserByRole(User.UserRole role) {
        return users.stream()
                   .filter(u -> u.getRole() == role)
                   .findFirst()
                   .orElse(null);
    }

    private void createTrustScores() {
        LOGGER.info("🏆 Creating Trust Scores and Events...");
        
        int scoresCreated = 0;
        int eventsCreated = 0;
        
        // Score distribution targets:
        // - 5+ users with EXCELLENT (90-100)
        // - 15+ users with GOOD (70-89)
        // - 15+ users with FAIR (50-69)
        // - 10+ users with LOW (30-49)
        // - 5+ users with PROBATION (0-29)
        
        // Create trust scores for all existing users
        for (User user : users) {
            try {
                String userId = user.getEmail(); // Use email as primary identifier
                
                // Determine target score based on role and random distribution
                double targetScore = determineTargetScore(user);
                
                // Create trust score
                TrustScore score = new TrustScore(userId, targetScore);
                score.setUserName(user.getFullName());
                score.setUserEmail(user.getEmail());
                trustScoreDAO.saveTrustScore(score);
                scoresCreated++;
                
                // Generate event history for this user
                int eventCount = generateEventHistory(userId, targetScore);
                eventsCreated += eventCount;
                
            } catch (Exception e) {
                LOGGER.warning("Error creating trust score for user: " + user.getEmail() + " - " + e.getMessage());
            }
        }
        
        // Create additional test users with specific trust levels
        createSpecialTrustUsers();
        
        LOGGER.info("✓ Created " + scoresCreated + " trust scores");
        LOGGER.info("✓ Created " + eventsCreated + " trust score events");
        
        // Print distribution
        printTrustScoreDistribution();
    }
    
    private double determineTargetScore(User user) {
        // Coordinators and managers get higher scores
        switch (user.getRole()) {
            case CAMPUS_COORDINATOR:
            case STATION_MANAGER:
            case AIRPORT_LOST_FOUND_SPECIALIST:
            case POLICE_EVIDENCE_CUSTODIAN:
            case SYSTEM_ADMIN:
                return 85 + random.nextDouble() * 15; // 85-100
                
            case CAMPUS_SECURITY:
            case TRANSIT_OFFICER:
            case TSA_SECURITY_COORDINATOR:
            case DETECTIVE:
                return 75 + random.nextDouble() * 15; // 75-90
                
            case STAFF:
            case LOST_FOUND_CLERK:
            case BUILDING_MANAGER:
                return 65 + random.nextDouble() * 20; // 65-85
                
            case STUDENT:
            case PUBLIC_TRAVELER:
            default:
                // Random distribution for students/public
                double rand = random.nextDouble();
                if (rand < 0.1) return 10 + random.nextDouble() * 20;      // 10% probation (10-30)
                if (rand < 0.25) return 30 + random.nextDouble() * 20;     // 15% low (30-50)
                if (rand < 0.55) return 50 + random.nextDouble() * 20;     // 30% fair (50-70)
                if (rand < 0.85) return 70 + random.nextDouble() * 20;     // 30% good (70-90)
                return 90 + random.nextDouble() * 10;                       // 15% excellent (90-100)
        }
    }
    
    private int generateEventHistory(String userId, double targetScore) {
        int eventCount = 0;
        
        // Generate 2-8 events per user
        int numEvents = 2 + random.nextInt(7);
        
        // Start from initial score of 50
        double currentScore = 50.0;
        
        // Positive event types with weights
        EventType[] positiveEvents = {
            EventType.REPORT_FOUND_ITEM,
            EventType.REPORT_LOST_ITEM,
            EventType.SUCCESSFUL_CLAIM,
            EventType.APPROVE_REQUEST,
            EventType.ASSIST_RECOVERY
        };
        
        // Negative event types
        EventType[] negativeEvents = {
            EventType.CLAIM_REJECTED,
            EventType.NO_SHOW_PICKUP,
            EventType.SUSPICIOUS_ACTIVITY
        };
        
        // Determine if user should trend positive or negative based on target
        boolean trendPositive = targetScore >= 50;
        
        for (int i = 0; i < numEvents; i++) {
            EventType eventType;
            
            if (trendPositive) {
                // 80% positive events for users trending up
                if (random.nextDouble() < 0.8) {
                    eventType = positiveEvents[random.nextInt(positiveEvents.length)];
                } else {
                    eventType = negativeEvents[random.nextInt(negativeEvents.length)];
                }
            } else {
                // 60% negative events for users trending down
                if (random.nextDouble() < 0.6) {
                    eventType = negativeEvents[random.nextInt(negativeEvents.length)];
                } else {
                    eventType = positiveEvents[random.nextInt(positiveEvents.length)];
                }
            }
            
            // Create and save event
            try {
                TrustScoreEvent event = new TrustScoreEvent(userId, eventType);
                event.setPreviousScore(currentScore);
                
                // Apply points
                int points = eventType.getDefaultPoints();
                currentScore = Math.max(0, Math.min(100, currentScore + points));
                event.setNewScore(currentScore);
                
                // Add some context
                if (eventType == EventType.REPORT_FOUND_ITEM || eventType == EventType.REPORT_LOST_ITEM) {
                    event.setRelatedItemId("gen-item-" + random.nextInt(1000));
                }
                
                trustScoreDAO.saveEvent(event);
                eventCount++;
            } catch (Exception e) {
                // Continue with next event
            }
        }
        
        return eventCount;
    }
    
    private void createSpecialTrustUsers() {
        // Create specific users for testing edge cases
        
        // 5 PROBATION users (score < 30)
        for (int i = 0; i < 5; i++) {
            String email = "probation.user" + i + "@test.com";
            TrustScore score = new TrustScore(email, 5 + random.nextDouble() * 20);
            score.setUserName("Probation User " + i);
            score.setUserEmail(email);
            score.setFlagged(true);
            score.setFlagReason("Multiple rejected claims");
            trustScoreDAO.saveTrustScore(score);
            
            // Add some negative events
            trustScoreService.recordEvent(email, EventType.FALSE_CLAIM, "Attempted to claim item that wasn't theirs");
            trustScoreService.recordEvent(email, EventType.SUSPICIOUS_ACTIVITY, "Multiple claims from different locations");
        }
        
        // 5 EXCELLENT users (score > 90)
        for (int i = 0; i < 5; i++) {
            String email = "excellent.user" + i + "@test.com";
            TrustScore score = new TrustScore(email, 92 + random.nextDouble() * 8);
            score.setUserName("Excellent User " + i);
            score.setUserEmail(email);
            trustScoreDAO.saveTrustScore(score);
            
            // Add positive history
            trustScoreService.recordEvent(email, EventType.GOOD_SAMARITAN, "Returned $500 cash without reward");
            trustScoreService.recordEvent(email, EventType.SUCCESSFUL_CLAIM, "Successfully claimed and verified");
            trustScoreService.recordEvent(email, EventType.REPORT_FOUND_ITEM, "Reported found laptop");
        }
        
        // 3 users under investigation
        for (int i = 0; i < 3; i++) {
            String email = "investigation.user" + i + "@test.com";
            TrustScore score = new TrustScore(email, 35 + random.nextDouble() * 15);
            score.setUserName("Investigation User " + i);
            score.setUserEmail(email);
            score.setUnderInvestigation(true);
            trustScoreDAO.saveTrustScore(score);
        }
        
        LOGGER.info("✓ Created 13 special trust test users");
    }
    
    private void printTrustScoreDistribution() {
        try {
            Map<TrustScore.ScoreLevel, Long> dist = trustScoreDAO.getScoreDistribution();
            System.out.println("\n   Trust Score Distribution:");
            for (TrustScore.ScoreLevel level : TrustScore.ScoreLevel.values()) {
                Long count = dist.getOrDefault(level, 0L);
                System.out.println("   - " + level.getDisplayName() + ": " + count + " users");
            }
        } catch (Exception e) {
            LOGGER.warning("Could not print distribution: " + e.getMessage());
        }
    }

    private void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 ECOSYSTEM GENERATION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Network: " + network.getName());
        System.out.println("Enterprises: " + enterprises.size());
        System.out.println("Organizations: " + organizations.values().stream().mapToInt(List::size).sum());
        System.out.println("Users: " + users.size());
        System.out.println("Buildings: " + buildings.size());
        System.out.println("Work Requests: " + workRequestDAO.count() + " (across all 5 types)");
        System.out.println("Trust Scores: " + trustScoreDAO.getTotalScoresCount());
        System.out.println("Trust Events: " + trustScoreDAO.getTotalEventsCount());
        System.out.println("=".repeat(60) + "\n");
    }

    public static void main(String[] args) {
        MongoDataGenerator generator = new MongoDataGenerator();
        generator.generateFullEcosystem();
    }
}
