package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScoreEvent;
import com.campus.lostfound.models.trustscore.TrustScoreEvent.EventType;
import com.campus.lostfound.models.workrequest.*;
import com.campus.lostfound.services.TrustScoreService;
import com.github.javafaker.Faker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Enhanced Data Generator using JavaFaker for realistic test data.
 * Simplified for demo/testing - focused on cross-campus workflow.
 * 
 * ONLY creates users for roles that have UI panels:
 * - Higher Ed: STUDENT, CAMPUS_COORDINATOR, CAMPUS_SECURITY (2 students + 1 coordinator + 1 security per campus)
 * - MBTA: STATION_MANAGER, TRANSIT_SECURITY_INSPECTOR, PUBLIC_TRAVELER
 * - Airport: AIRPORT_LOST_FOUND_SPECIALIST, TSA_SECURITY_COORDINATOR, PUBLIC_TRAVELER
 * - Police: POLICE_EVIDENCE_CUSTODIAN
 * - System: SYSTEM_ADMIN
 * 
 * Generates:
 * - Higher Ed: 2 orgs (NEU, BU) with 8 users total
 * - MBTA: 4 users for cross-enterprise demos
 * - Airport: 3 users for cross-enterprise demos  
 * - Police: 1 user for evidence verification
 * - ~30 items for testing
 * - Work requests across all 7 types
 * 
 * Total: ~17 users (all with functional panels)
 * 
 * @author Enhanced Generator
 */
public class EnhancedDataGenerator {

    private static final Logger LOGGER = Logger.getLogger(EnhancedDataGenerator.class.getName());
    
    // Faker instance for generating realistic data
    private final Faker faker;
    private final Random random;
    
    // DAOs
    private final MongoNetworkDAO networkDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final MongoUserDAO userDAO;
    private final MongoBuildingDAO buildingDAO;
    private final MongoItemDAO itemDAO;
    private final MongoWorkRequestDAO workRequestDAO;
    private final MongoTrustScoreDAO trustScoreDAO;
    private final TrustScoreService trustScoreService;

    // Generated entities (stored for reference)
    private Network network;
    private Map<String, Enterprise> enterprises;
    private Map<String, List<Organization>> organizations;
    private Map<String, List<User>> usersByEnterprise;
    private List<User> allUsers;
    private Map<String, List<Building>> buildingsByEnterprise;
    private List<Building> allBuildings;
    private List<Item> allItems;
    private List<WorkRequest> allWorkRequests;

    // Item data for realistic generation
    private static final String[] ELECTRONICS = {
        "iPhone", "Samsung Galaxy", "MacBook Pro", "Dell Laptop", "iPad", "AirPods", 
        "Beats Headphones", "Sony Headphones", "Apple Watch", "Fitbit", "Kindle",
        "Nintendo Switch", "Bluetooth Speaker", "Portable Charger", "USB Drive",
        "Laptop Charger", "Phone Charger", "Wireless Mouse", "Calculator", "Camera"
    };
    
    private static final String[] BAGS = {
        "Backpack", "Laptop Bag", "Messenger Bag", "Tote Bag", "Gym Bag", "Purse",
        "Wallet", "Briefcase", "Duffel Bag", "Suitcase", "Carry-on", "Fanny Pack"
    };
    
    private static final String[] CLOTHING = {
        "Jacket", "Hoodie", "Sweater", "Coat", "Umbrella", "Scarf", "Gloves", "Hat",
        "Sunglasses", "Prescription Glasses", "Baseball Cap", "Beanie"
    };
    
    private static final String[] BOOKS = {
        "Textbook", "Notebook", "Planner", "Novel", "Study Guide", "Lab Manual",
        "Binder", "Folder with Documents"
    };
    
    private static final String[] IDS_CARDS = {
        "Student ID", "Driver's License", "Passport", "Credit Card", "Debit Card",
        "Library Card", "Transit Pass", "Employee Badge", "Health Insurance Card"
    };
    
    private static final String[] JEWELRY = {
        "Watch", "Ring", "Necklace", "Bracelet", "Earrings", "Pendant"
    };
    
    private static final String[] KEYS = {
        "Car Keys", "House Keys", "Apartment Keys", "Office Keys", "Bike Lock Key",
        "Key Ring with Multiple Keys", "Dorm Room Key"
    };
    
    private static final String[] COLORS = {
        "Black", "White", "Blue", "Red", "Green", "Gray", "Silver", "Gold", "Pink",
        "Purple", "Navy", "Brown", "Orange", "Yellow", "Teal"
    };
    
    private static final String[] BRANDS_ELECTRONICS = {
        "Apple", "Samsung", "Dell", "HP", "Lenovo", "Sony", "Bose", "JBL", "Logitech"
    };
    
    private static final String[] BRANDS_BAGS = {
        "North Face", "Herschel", "JanSport", "Nike", "Adidas", "Tumi", "Samsonite",
        "Patagonia", "Fjallraven", "Swiss Gear"
    };

    public EnhancedDataGenerator() {
        this.faker = new Faker();
        this.random = new Random();
        
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
        this.usersByEnterprise = new HashMap<>();
        this.allUsers = new ArrayList<>();
        this.buildingsByEnterprise = new HashMap<>();
        this.allBuildings = new ArrayList<>();
        this.allItems = new ArrayList<>();
        this.allWorkRequests = new ArrayList<>();
    }

    /**
     * Generate the complete enhanced ecosystem.
     * Call this to create all test data.
     */
    public void generateFullEcosystem() {
        LOGGER.info("🚀 Starting Enhanced Data Generation with JavaFaker...");
        long startTime = System.currentTimeMillis();

        // Step 1: Create Network
        createNetwork();

        // Step 2: Create 4 Enterprises
        createEnterprises();

        // Step 3: Create Organizations within each enterprise
        createOrganizations();

        // Step 4: Create 80+ Users
        createUsers();

        // Step 5: Create Buildings
        createBuildings();

        // Step 6: Create 200+ Items
        createItems();

        // Step 7: Create 30+ Work Requests (all 7 types)
        createWorkRequests();

        // Step 8: Create Trust Scores
        createTrustScores();

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("✅ Enhanced ecosystem generation completed in " + duration + "ms!");
        printSummary();
    }

    // ==================== NETWORK & ENTERPRISES ====================

    private void createNetwork() {
        LOGGER.info("📡 Creating Network...");
        
        Network network = new Network();
        network.setName("Greater Boston Lost & Found Network");
        network.setDescription("Cross-enterprise collaboration system for lost and found item recovery across universities, MBTA, Logan Airport, and law enforcement in the Greater Boston area.");
        
        String networkId = networkDAO.create(network);
        network.setNetworkId(networkId);
        this.network = network;
        
        LOGGER.info("   ✓ Network created: " + networkId);
    }

    private void createEnterprises() {
        LOGGER.info("🏢 Creating 4 Enterprises...");

        // Enterprise 1: Higher Education (umbrella for all universities)
        Enterprise higherEd = new Enterprise(null, network.getNetworkId(), 
            "Higher Education", Enterprise.EnterpriseType.HIGHER_EDUCATION);
        higherEd.setDescription("Greater Boston area universities and colleges - NEU, BU, MIT, Harvard, etc.");
        higherEd.setContactEmail("lostandfound@highered.boston.edu");
        higherEd.setEnterpriseId(enterpriseDAO.create(higherEd));
        enterprises.put("higher_ed", higherEd);

        // Enterprise 2: Public Transit (MBTA)
        Enterprise mbta = new Enterprise(null, network.getNetworkId(),
            "Public Transit (MBTA)", Enterprise.EnterpriseType.PUBLIC_TRANSIT);
        mbta.setDescription("Massachusetts Bay Transportation Authority - subway, bus, commuter rail");
        mbta.setContactEmail("lostandfound@mbta.com");
        mbta.setEnterpriseId(enterpriseDAO.create(mbta));
        enterprises.put("mbta", mbta);

        // Enterprise 3: Logan Airport
        Enterprise airport = new Enterprise(null, network.getNetworkId(),
            "Logan Airport", Enterprise.EnterpriseType.AIRPORT);
        airport.setDescription("Boston Logan International Airport operated by Massport");
        airport.setContactEmail("lostandfound@massport.com");
        airport.setEnterpriseId(enterpriseDAO.create(airport));
        enterprises.put("airport", airport);

        // Enterprise 4: Law Enforcement (Boston Police)
        Enterprise police = new Enterprise(null, network.getNetworkId(),
            "Law Enforcement", Enterprise.EnterpriseType.LAW_ENFORCEMENT);
        police.setDescription("Boston Police Department - evidence management and stolen property verification");
        police.setContactEmail("evidence@bostonpolice.gov");
        police.setEnterpriseId(enterpriseDAO.create(police));
        enterprises.put("police", police);

        LOGGER.info("   ✓ Created 4 enterprises");
    }

    private void createOrganizations() {
        LOGGER.info("🏛️ Creating Organizations...");

        // Higher Education Organizations - 2 universities for cross-campus demo
        Enterprise higherEd = enterprises.get("higher_ed");
        List<Organization> higherEdOrgs = new ArrayList<>();
        
        // Northeastern University - single organization
        Organization neu = createOrganization(higherEd, "Northeastern University", 
            Organization.OrganizationType.CAMPUS_OPERATIONS, "NEU - Campus lost and found coordination");
        higherEdOrgs.add(neu);
        
        // Boston University - single organization
        Organization bu = createOrganization(higherEd, "Boston University", 
            Organization.OrganizationType.CAMPUS_OPERATIONS, "BU - Campus lost and found coordination");
        higherEdOrgs.add(bu);
        
        organizations.put("higher_ed", higherEdOrgs);

        // MBTA Organizations - Stations/Lines as Organizations
        Enterprise mbta = enterprises.get("mbta");
        List<Organization> mbtaOrgs = new ArrayList<>();
        
        Organization redLine = createOrganization(mbta, "Red Line Operations", 
            Organization.OrganizationType.STATION_OPERATIONS, "Red Line stations - Alewife to Ashmont/Braintree");
        mbtaOrgs.add(redLine);
        
        Organization orangeLine = createOrganization(mbta, "Orange Line Operations", 
            Organization.OrganizationType.STATION_OPERATIONS, "Orange Line stations - Oak Grove to Forest Hills");
        mbtaOrgs.add(orangeLine);
        
        Organization greenLine = createOrganization(mbta, "Green Line Operations", 
            Organization.OrganizationType.STATION_OPERATIONS, "Green Line stations - All branches B, C, D, E");
        mbtaOrgs.add(greenLine);
        
        Organization blueLine = createOrganization(mbta, "Blue Line Operations", 
            Organization.OrganizationType.STATION_OPERATIONS, "Blue Line stations - Wonderland to Bowdoin");
        mbtaOrgs.add(blueLine);
        
        Organization commuterRail = createOrganization(mbta, "Commuter Rail Operations", 
            Organization.OrganizationType.STATION_OPERATIONS, "Commuter Rail - All lines including South/North Station");
        mbtaOrgs.add(commuterRail);
        
        Organization transitPolice = createOrganization(mbta, "MBTA Transit Police", 
            Organization.OrganizationType.TRANSIT_POLICE, "Transit security and high-value items");
        mbtaOrgs.add(transitPolice);
        
        Organization centralLostFound = createOrganization(mbta, "MBTA Central Lost & Found", 
            Organization.OrganizationType.CENTRAL_LOST_FOUND, "Centralized lost and found depot at Downtown Crossing");
        mbtaOrgs.add(centralLostFound);
        
        organizations.put("mbta", mbtaOrgs);

        // Airport Organizations - Terminals as Organizations
        Enterprise airport = enterprises.get("airport");
        List<Organization> airportOrgs = new ArrayList<>();
        
        Organization terminalA = createOrganization(airport, "Terminal A Operations", 
            Organization.OrganizationType.AIRPORT_OPERATIONS, "Terminal A - Delta, United domestic flights");
        airportOrgs.add(terminalA);
        
        Organization terminalB = createOrganization(airport, "Terminal B Operations", 
            Organization.OrganizationType.AIRPORT_OPERATIONS, "Terminal B - American, United flights");
        airportOrgs.add(terminalB);
        
        Organization terminalC = createOrganization(airport, "Terminal C Operations", 
            Organization.OrganizationType.AIRPORT_OPERATIONS, "Terminal C - JetBlue hub");
        airportOrgs.add(terminalC);
        
        Organization terminalE = createOrganization(airport, "Terminal E Operations", 
            Organization.OrganizationType.AIRPORT_OPERATIONS, "Terminal E - International flights");
        airportOrgs.add(terminalE);
        
        Organization tsa = createOrganization(airport, "TSA Security", 
            Organization.OrganizationType.TSA_SECURITY, "TSA checkpoint items and security clearance");
        airportOrgs.add(tsa);
        
        Organization airlineServices = createOrganization(airport, "Airline Services Hub", 
            Organization.OrganizationType.AIRLINE_SERVICES, "Multi-airline lost and found coordination");
        airportOrgs.add(airlineServices);
        
        organizations.put("airport", airportOrgs);

        // Law Enforcement Organizations - Departments as Organizations
        Enterprise police = enterprises.get("police");
        List<Organization> policeOrgs = new ArrayList<>();
        
        Organization bpdCentral = createOrganization(police, "BPD Central Division", 
            Organization.OrganizationType.POLICE_DEPARTMENT, "Boston Police - Central Division property room");
        policeOrgs.add(bpdCentral);
        
        Organization evidenceMgmt = createOrganization(police, "Evidence Management Unit", 
            Organization.OrganizationType.EVIDENCE_MANAGEMENT, "Central evidence custody and stolen property verification");
        policeOrgs.add(evidenceMgmt);
        
        Organization transitUnit = createOrganization(police, "Transit Unit", 
            Organization.OrganizationType.POLICE_DEPARTMENT, "Police coordination with MBTA and Airport");
        policeOrgs.add(transitUnit);
        
        Organization detectiveBureau = createOrganization(police, "Detective Bureau", 
            Organization.OrganizationType.DETECTIVE_BUREAU, "Investigative services and case management");
        policeOrgs.add(detectiveBureau);
        
        organizations.put("police", policeOrgs);

        LOGGER.info("   ✓ Created " + (higherEdOrgs.size() + mbtaOrgs.size() + airportOrgs.size() + policeOrgs.size()) + " organizations across 4 enterprises");
    }

    private Organization createOrganization(Enterprise enterprise, String name, 
            Organization.OrganizationType type, String description) {
        Organization org = new Organization(null, enterprise.getEnterpriseId(), name, type);
        org.setDescription(description);
        org.setOrganizationId(organizationDAO.create(org));
        return org;
    }

    // ==================== USERS (80+) ====================

    private void createUsers() {
        LOGGER.info("👥 Creating Users (only roles with panels)...");

        // Higher Education Users (8 total: 2 students + 1 coordinator + 1 security per campus)
        createHigherEdUsers(8);

        // MBTA Users (4): 1 station manager + 1 transit security inspector + 2 travelers
        createMBTAUsers(4);

        // Airport Users (3): 1 specialist + 1 TSA coordinator + 1 traveler
        createAirportUsers(3);

        // Police Users (1): 1 evidence custodian
        createPoliceUsers(1);

        // System Admin (1)
        createSystemAdmin();

        LOGGER.info("   ✓ Created " + allUsers.size() + " users total (roles with panels only)");
    }

    private void createHigherEdUsers(int count) {
        Enterprise enterprise = enterprises.get("higher_ed");
        List<Organization> orgs = organizations.get("higher_ed"); // NEU and BU
        List<User> enterpriseUsers = new ArrayList<>();

        // For each university: 2 students + 1 coordinator + 1 security
        // Roles with panels: STUDENT, CAMPUS_COORDINATOR, CAMPUS_SECURITY
        for (Organization org : orgs) {
            // Create 2 students per university
            for (int i = 0; i < 2; i++) {
                User student = createUser(enterprise, org, User.UserRole.STUDENT);
                enterpriseUsers.add(student);
            }
            
            // Create 1 campus coordinator per university
            User coordinator = createUser(enterprise, org, User.UserRole.CAMPUS_COORDINATOR);
            enterpriseUsers.add(coordinator);
            
            // Create 1 campus security per university
            User security = createUser(enterprise, org, User.UserRole.CAMPUS_SECURITY);
            enterpriseUsers.add(security);
        }

        usersByEnterprise.put("higher_ed", enterpriseUsers);
        allUsers.addAll(enterpriseUsers);
        
        LOGGER.info("      Created " + enterpriseUsers.size() + " Higher Ed users (2 students + 1 coordinator + 1 security per campus)");
    }

    private void createMBTAUsers(int count) {
        Enterprise enterprise = enterprises.get("mbta");
        List<Organization> orgs = organizations.get("mbta");
        List<User> enterpriseUsers = new ArrayList<>();

        // Get first station operations org for simplicity
        Organization stationOrg = orgs.stream()
            .filter(o -> o.getType() == Organization.OrganizationType.STATION_OPERATIONS)
            .findFirst().orElse(orgs.get(0));
        
        Organization transitPoliceOrg = orgs.stream()
            .filter(o -> o.getType() == Organization.OrganizationType.TRANSIT_POLICE)
            .findFirst().orElse(stationOrg);

        // Only create users for roles with panels:
        // STATION_MANAGER, TRANSIT_SECURITY_INSPECTOR, PUBLIC_TRAVELER
        enterpriseUsers.add(createUser(enterprise, stationOrg, User.UserRole.STATION_MANAGER));
        enterpriseUsers.add(createUser(enterprise, transitPoliceOrg, User.UserRole.TRANSIT_SECURITY_INSPECTOR));
        enterpriseUsers.add(createUser(enterprise, stationOrg, User.UserRole.PUBLIC_TRAVELER));
        enterpriseUsers.add(createUser(enterprise, stationOrg, User.UserRole.PUBLIC_TRAVELER)); // Second traveler for testing

        usersByEnterprise.put("mbta", enterpriseUsers);
        allUsers.addAll(enterpriseUsers);
        
        LOGGER.info("      Created " + enterpriseUsers.size() + " MBTA users (roles with panels only)");
    }

    private void createAirportUsers(int count) {
        Enterprise enterprise = enterprises.get("airport");
        List<Organization> orgs = organizations.get("airport");
        List<User> enterpriseUsers = new ArrayList<>();

        // Get relevant orgs
        Organization airportOpsOrg = orgs.stream()
            .filter(o -> o.getType() == Organization.OrganizationType.AIRPORT_OPERATIONS)
            .findFirst().orElse(orgs.get(0));
        
        Organization tsaOrg = orgs.stream()
            .filter(o -> o.getType() == Organization.OrganizationType.TSA_SECURITY)
            .findFirst().orElse(airportOpsOrg);

        // Only create users for roles with panels:
        // AIRPORT_LOST_FOUND_SPECIALIST, TSA_SECURITY_COORDINATOR, PUBLIC_TRAVELER
        enterpriseUsers.add(createUser(enterprise, airportOpsOrg, User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST));
        enterpriseUsers.add(createUser(enterprise, tsaOrg, User.UserRole.TSA_SECURITY_COORDINATOR));
        enterpriseUsers.add(createUser(enterprise, airportOpsOrg, User.UserRole.PUBLIC_TRAVELER));

        usersByEnterprise.put("airport", enterpriseUsers);
        allUsers.addAll(enterpriseUsers);
        
        LOGGER.info("      Created " + enterpriseUsers.size() + " Airport users (roles with panels only)");
    }

    private void createPoliceUsers(int count) {
        Enterprise enterprise = enterprises.get("police");
        List<Organization> orgs = organizations.get("police");
        List<User> enterpriseUsers = new ArrayList<>();

        // Get relevant orgs
        Organization evidenceOrg = orgs.stream()
            .filter(o -> o.getType() == Organization.OrganizationType.EVIDENCE_MANAGEMENT)
            .findFirst().orElse(orgs.get(0));

        // Only create users for roles with panels:
        // POLICE_EVIDENCE_CUSTODIAN
        enterpriseUsers.add(createUser(enterprise, evidenceOrg, User.UserRole.POLICE_EVIDENCE_CUSTODIAN));

        usersByEnterprise.put("police", enterpriseUsers);
        allUsers.addAll(enterpriseUsers);
        
        LOGGER.info("      Created " + enterpriseUsers.size() + " Police users (roles with panels only)");
    }

    private void createSystemAdmin() {
        Enterprise enterprise = enterprises.get("higher_ed");
        List<Organization> orgs = organizations.get("higher_ed");
        
        User admin = new User("admin@system.com", "System", "Administrator", User.UserRole.SYSTEM_ADMIN);
        admin.setEnterpriseId(enterprise.getEnterpriseId());
        admin.setOrganizationId(orgs.get(0).getOrganizationId());
        admin.setPhoneNumber("617-555-0001");
        admin.setTrustScore(100.0);
        userDAO.create(admin, "admin123");
        
        allUsers.add(admin);
    }

    private User createUser(Enterprise enterprise, Organization org, User.UserRole role) {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String email = generateEmail(firstName, lastName, enterprise);
        
        User user = new User(email, firstName, lastName, role);
        user.setEnterpriseId(enterprise.getEnterpriseId());
        user.setOrganizationId(org.getOrganizationId());
        user.setPhoneNumber(faker.phoneNumber().cellPhone());
        user.setTrustScore(50 + random.nextDouble() * 50); // 50-100 initial score
        
        userDAO.create(user, "password123");
        return user;
    }

    private String generateEmail(String firstName, String lastName, Enterprise enterprise) {
        String domain = switch (enterprise.getType()) {
            case HIGHER_EDUCATION -> "northeastern.edu";
            case PUBLIC_TRANSIT -> "mbta.com";
            case AIRPORT -> "massport.com";
            case LAW_ENFORCEMENT -> "bostonpolice.gov";
            default -> "example.com";
        };
        return (firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + domain)
            .replaceAll("[^a-z0-9@.]", "");
    }

    // ==================== BUILDINGS ====================

    private void createBuildings() {
        LOGGER.info("🏢 Creating Buildings...");
        
        // Clear existing buildings to avoid duplicates
        LOGGER.info("   Clearing existing buildings...");
        buildingDAO.deleteAll();

        // Higher Ed Buildings (10)
        createHigherEdBuildings();

        // MBTA Stations (8)
        createMBTABuildings();

        // Airport Terminals (5)
        createAirportBuildings();

        // Police Buildings (3)
        createPoliceBuildings();

        LOGGER.info("   ✓ Created " + allBuildings.size() + " buildings total");
    }

    private void createHigherEdBuildings() {
        Enterprise enterprise = enterprises.get("higher_ed");
        List<Organization> orgs = organizations.get("higher_ed"); // NEU and BU
        List<Building> buildings = new ArrayList<>();

        // NEU Buildings (assigned to first org - Northeastern University)
        Organization neuOrg = orgs.get(0);
        String[][] neuBuildings = {
            {"Snell Library", "SL", "LIBRARY"},
            {"Curry Student Center", "CSC", "STUDENT_CENTER"},
            {"Ell Hall", "EH", "ACADEMIC"}
        };
        
        for (String[] data : neuBuildings) {
            Building building = new Building(data[0], data[1], Building.BuildingType.valueOf(data[2]));
            building.setEnterpriseId(enterprise.getEnterpriseId());
            building.setOrganizationId(neuOrg.getOrganizationId());
            buildingDAO.create(building);
            buildings.add(building);
        }

        // BU Buildings (assigned to second org - Boston University)
        Organization buOrg = orgs.get(1);
        String[][] buBuildings = {
            {"Mugar Memorial Library", "MML", "LIBRARY"},
            {"George Sherman Union", "GSU", "STUDENT_CENTER"},
            {"College of Arts & Sciences", "CAS", "ACADEMIC"}
        };
        
        for (String[] data : buBuildings) {
            Building building = new Building(data[0], data[1], Building.BuildingType.valueOf(data[2]));
            building.setEnterpriseId(enterprise.getEnterpriseId());
            building.setOrganizationId(buOrg.getOrganizationId());
            buildingDAO.create(building);
            buildings.add(building);
        }

        buildingsByEnterprise.put("higher_ed", buildings);
        allBuildings.addAll(buildings);
    }

    private void createMBTABuildings() {
        Enterprise enterprise = enterprises.get("mbta");
        List<Organization> orgs = organizations.get("mbta");
        List<Building> buildings = new ArrayList<>();

        String[][] stationData = {
            {"Park Street Station", "PKS", "Green/Red Line hub"},
            {"South Station", "SS", "Commuter Rail terminal"},
            {"North Station", "NS", "Commuter Rail terminal"},
            {"Back Bay Station", "BB", "Orange Line/Commuter Rail"},
            {"Harvard Station", "HRV", "Red Line"},
            {"Northeastern Station", "NEU", "Green Line E Branch"},
            {"Ruggles Station", "RUG", "Orange Line/Commuter Rail"},
            {"Downtown Crossing", "DTC", "Red/Orange Line hub"}
        };

        for (String[] data : stationData) {
            Building building = new Building(data[0], data[1], Building.BuildingType.ADMINISTRATIVE);
            building.setEnterpriseId(enterprise.getEnterpriseId());
            building.setOrganizationId(orgs.get(0).getOrganizationId());
            buildingDAO.create(building);
            buildings.add(building);
        }

        buildingsByEnterprise.put("mbta", buildings);
        allBuildings.addAll(buildings);
    }

    private void createAirportBuildings() {
        Enterprise enterprise = enterprises.get("airport");
        List<Organization> orgs = organizations.get("airport");
        List<Building> buildings = new ArrayList<>();

        String[][] terminalData = {
            {"Terminal A", "TA", "Delta, United domestic"},
            {"Terminal B", "TB", "American, United"},
            {"Terminal C", "TC", "JetBlue hub"},
            {"Terminal E", "TE", "International terminal"},
            {"Central Parking Garage", "CPG", "Main parking facility"}
        };

        for (String[] data : terminalData) {
            Building building = new Building(data[0], data[1], Building.BuildingType.ADMINISTRATIVE);
            building.setEnterpriseId(enterprise.getEnterpriseId());
            building.setOrganizationId(orgs.get(random.nextInt(orgs.size())).getOrganizationId());
            buildingDAO.create(building);
            buildings.add(building);
        }

        buildingsByEnterprise.put("airport", buildings);
        allBuildings.addAll(buildings);
    }

    private void createPoliceBuildings() {
        Enterprise enterprise = enterprises.get("police");
        List<Organization> orgs = organizations.get("police");
        List<Building> buildings = new ArrayList<>();

        String[][] stationData = {
            {"District A-1 Station", "A1", "Downtown"},
            {"District D-4 Station", "D4", "Back Bay/South End"},
            {"Evidence Warehouse", "EW", "Central Evidence Storage"}
        };

        for (String[] data : stationData) {
            Building building = new Building(data[0], data[1], Building.BuildingType.ADMINISTRATIVE);
            building.setEnterpriseId(enterprise.getEnterpriseId());
            building.setOrganizationId(orgs.get(random.nextInt(orgs.size())).getOrganizationId());
            buildingDAO.create(building);
            buildings.add(building);
        }

        buildingsByEnterprise.put("police", buildings);
        allBuildings.addAll(buildings);
    }

    // ==================== ITEMS (200+) ====================

    private void createItems() {
        LOGGER.info("📦 Creating Items (simplified for testing)...");

        // Reduced distribution for testing (~30 items)
        createItemsOfCategory(Item.ItemCategory.ELECTRONICS, 8);
        createItemsOfCategory(Item.ItemCategory.BAGS, 5);
        createItemsOfCategory(Item.ItemCategory.CLOTHING, 4);
        createItemsOfCategory(Item.ItemCategory.BOOKS, 4);
        createItemsOfCategory(Item.ItemCategory.IDS_CARDS, 4);
        createItemsOfCategory(Item.ItemCategory.JEWELRY, 2);
        createItemsOfCategory(Item.ItemCategory.KEYS, 3);
        createItemsOfCategory(Item.ItemCategory.OTHER, 2);

        LOGGER.info("   ✓ Created " + allItems.size() + " items total");
    }

    private void createItemsOfCategory(Item.ItemCategory category, int count) {
        for (int i = 0; i < count; i++) {
            // Random building and user
            Building building = allBuildings.get(random.nextInt(allBuildings.size()));
            User reporter = allUsers.get(random.nextInt(allUsers.size()));
            
            // Generate item data
            String name = generateItemName(category);
            String description = generateItemDescription(category, name);
            Item.ItemType type = random.nextBoolean() ? Item.ItemType.LOST : Item.ItemType.FOUND;
            
            // Create location
            String room = String.valueOf(100 + random.nextInt(400));
            String area = generateArea(building);
            Location location = new Location(building, room, area);
            
            // Create item
            Item item = new Item(name, description, category, type, location, reporter);
            item.setEnterpriseId(building.getEnterpriseId());
            item.setOrganizationId(building.getOrganizationId());
            
            // Set random status
            Item.ItemStatus status = getRandomItemStatus();
            item.setStatus(status);
            
            // Set value for certain categories
            if (category == Item.ItemCategory.ELECTRONICS || category == Item.ItemCategory.JEWELRY) {
                item.setEstimatedValue(generateItemValue(category));
            }
            
            itemDAO.create(item);
            allItems.add(item);
        }
    }

    private String generateItemName(Item.ItemCategory category) {
        String color = COLORS[random.nextInt(COLORS.length)];
        String item = switch (category) {
            case ELECTRONICS -> ELECTRONICS[random.nextInt(ELECTRONICS.length)];
            case BAGS -> BAGS[random.nextInt(BAGS.length)];
            case CLOTHING -> CLOTHING[random.nextInt(CLOTHING.length)];
            case BOOKS -> BOOKS[random.nextInt(BOOKS.length)];
            case IDS_CARDS -> IDS_CARDS[random.nextInt(IDS_CARDS.length)];
            case JEWELRY -> JEWELRY[random.nextInt(JEWELRY.length)];
            case KEYS -> KEYS[random.nextInt(KEYS.length)];
            default -> "Miscellaneous Item";
        };
        return color + " " + item;
    }

    private String generateItemDescription(Item.ItemCategory category, String name) {
        StringBuilder desc = new StringBuilder(name);
        
        switch (category) {
            case ELECTRONICS -> {
                String brand = BRANDS_ELECTRONICS[random.nextInt(BRANDS_ELECTRONICS.length)];
                desc.append(". ").append(brand).append(" brand");
                if (random.nextBoolean()) {
                    desc.append(", ").append(random.nextBoolean() ? "good condition" : "minor scratches");
                }
                if (random.nextBoolean()) {
                    desc.append(". Has ").append(random.nextBoolean() ? "protective case" : "stickers on it");
                }
            }
            case BAGS -> {
                String brand = BRANDS_BAGS[random.nextInt(BRANDS_BAGS.length)];
                desc.append(". ").append(brand).append(" brand");
                if (random.nextBoolean()) {
                    desc.append(", contains ").append(random.nextBoolean() ? "books and papers" : "personal items");
                }
            }
            case CLOTHING -> {
                desc.append(". Size ").append(new String[]{"S", "M", "L", "XL"}[random.nextInt(4)]);
                if (random.nextBoolean()) {
                    desc.append(", ").append(faker.company().name()).append(" brand");
                }
            }
            case IDS_CARDS -> {
                desc.append(" belonging to ").append(faker.name().fullName());
                if (name.contains("Student")) {
                    desc.append(". ID# ").append(String.format("%09d", random.nextInt(1000000000)));
                }
            }
            case KEYS -> {
                int numKeys = 1 + random.nextInt(5);
                desc.append(". ").append(numKeys).append(" key(s) on ");
                desc.append(random.nextBoolean() ? "metal ring" : "lanyard");
            }
            default -> {
                desc.append(". ").append(faker.lorem().sentence());
            }
        }
        
        return desc.toString();
    }

    private String generateArea(Building building) {
        String[] areas = {"Near entrance", "By the elevators", "In hallway", "At reception", 
            "Near restrooms", "In lobby", "By stairs", "In common area", "At front desk"};
        return areas[random.nextInt(areas.length)];
    }

    private Item.ItemStatus getRandomItemStatus() {
        double rand = random.nextDouble();
        if (rand < 0.4) return Item.ItemStatus.OPEN;
        if (rand < 0.6) return Item.ItemStatus.PENDING_CLAIM;
        if (rand < 0.75) return Item.ItemStatus.CLAIMED;
        if (rand < 0.9) return Item.ItemStatus.VERIFIED;
        return Item.ItemStatus.EXPIRED;
    }

    private double generateItemValue(Item.ItemCategory category) {
        return switch (category) {
            case ELECTRONICS -> 50 + random.nextDouble() * 2000;  // $50 - $2050
            case JEWELRY -> 25 + random.nextDouble() * 500;       // $25 - $525
            case BAGS -> 20 + random.nextDouble() * 200;          // $20 - $220
            default -> 10 + random.nextDouble() * 100;            // $10 - $110
        };
    }

    // ==================== WORK REQUESTS (30+) ====================

    private void createWorkRequests() {
        LOGGER.info("📝 Creating Work Requests (all 7 types)...");

        // Item Claims (3)
        createItemClaimRequests(3);

        // Cross-Campus Transfers (2) - KEY for demo!
        createCrossCampusTransferRequests(2);

        // Transit-to-University Transfers (2)
        createTransitTransferRequests(2);

        // Airport-to-University Transfers (2)
        createAirportTransferRequests(2);

        // Police Evidence Requests (1)
        createPoliceEvidenceRequests(1);

        // MBTA-to-Airport Emergency (1)
        createMBTAToAirportEmergencyRequests(1);

        // Multi-Enterprise Dispute (1)
        createMultiEnterpriseDisputeRequests(1);

        LOGGER.info("   ✓ Created " + allWorkRequests.size() + " work requests total");
    }

    private void createItemClaimRequests(int count) {
        List<User> students = getUsersByRole(User.UserRole.STUDENT);
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        
        if (students.isEmpty() || coordinators.isEmpty()) return;

        WorkRequest.RequestPriority[] priorities = {
            WorkRequest.RequestPriority.LOW,
            WorkRequest.RequestPriority.NORMAL,
            WorkRequest.RequestPriority.HIGH,
            WorkRequest.RequestPriority.URGENT
        };

        for (int i = 0; i < count; i++) {
            User student = students.get(random.nextInt(students.size()));
            User coordinator = coordinators.get(random.nextInt(coordinators.size()));
            Item item = getRandomItemByStatus(Item.ItemStatus.OPEN, Item.ItemStatus.PENDING_CLAIM);
            
            if (item == null) {
                item = allItems.get(random.nextInt(allItems.size()));
            }

            double value = 25 + random.nextDouble() * 500;
            ItemClaimRequest claim = new ItemClaimRequest(
                student.getEmail(), student.getFullName(),  // Use email as requesterId
                String.valueOf(item.getItemId()), item.getTitle(), value
            );
            
            claim.setClaimDetails(faker.lorem().paragraph());
            claim.setIdentifyingFeatures(generateIdentifyingFeatures(item));
            claim.setProofDescription("I can provide " + (random.nextBoolean() ? "receipt" : "photos"));
            claim.setPriority(priorities[random.nextInt(priorities.length)]);
            claim.setRequesterOrganizationId(student.getOrganizationId());
            claim.setTargetOrganizationId(coordinator.getOrganizationId());
            claim.setRequesterEnterpriseId(student.getEnterpriseId());
            claim.setTargetEnterpriseId(coordinator.getEnterpriseId());
            
            workRequestDAO.save(claim);
            allWorkRequests.add(claim);
        }
    }

    private void createCrossCampusTransferRequests(int count) {
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        List<User> students = getUsersByRole(User.UserRole.STUDENT);
        List<Organization> higherEdOrgs = organizations.get("higher_ed"); // NEU and BU
        
        if (coordinators.isEmpty() || students.isEmpty() || higherEdOrgs.size() < 2) return;
        
        // Get NEU and BU organizations
        Organization neuOrg = higherEdOrgs.get(0); // Northeastern University
        Organization buOrg = higherEdOrgs.get(1);  // Boston University

        for (int i = 0; i < count; i++) {
            // Alternate direction: NEU->BU or BU->NEU
            boolean neuToBu = (i % 2 == 0);
            Organization sourceOrg = neuToBu ? neuOrg : buOrg;
            Organization destOrg = neuToBu ? buOrg : neuOrg;
            
            // Find coordinators for source and destination
            User sourceCoord = coordinators.stream()
                .filter(c -> c.getOrganizationId().equals(sourceOrg.getOrganizationId()))
                .findFirst().orElse(coordinators.get(0));
            User destCoord = coordinators.stream()
                .filter(c -> c.getOrganizationId().equals(destOrg.getOrganizationId()))
                .findFirst().orElse(coordinators.get(1 % coordinators.size()));
            
            // Find a student from destination campus (they're claiming the item)
            User student = students.stream()
                .filter(s -> s.getOrganizationId().equals(destOrg.getOrganizationId()))
                .findFirst().orElse(students.get(0));
            
            Item item = allItems.get(random.nextInt(allItems.size()));

            CrossCampusTransferRequest transfer = new CrossCampusTransferRequest(
                sourceCoord.getEmail(), sourceCoord.getFullName(),
                String.valueOf(item.getItemId()), item.getTitle(),
                student.getEmail(), student.getFullName()
            );
            
            // Set source and destination campus names
            transfer.setSourceCampusName(sourceOrg.getName());
            transfer.setDestinationCampusName(destOrg.getName());
            transfer.setSourceCoordinatorId(sourceCoord.getEmail());
            transfer.setSourceCoordinatorName(sourceCoord.getFullName());
            transfer.setDestinationCoordinatorId(destCoord.getEmail());
            transfer.setDestinationCoordinatorName(destCoord.getFullName());
            transfer.setPickupLocation(sourceOrg.getName() + " Lost & Found Office");
            transfer.setTransferMethod(random.nextBoolean() ? "Courier" : "In-Person Pickup");
            transfer.setEstimatedPickupDate(generateFutureDate(3, 14));
            transfer.setPriority(WorkRequest.RequestPriority.NORMAL);
            transfer.setRequesterOrganizationId(sourceOrg.getOrganizationId());
            transfer.setTargetOrganizationId(destOrg.getOrganizationId());
            transfer.setRequesterEnterpriseId(sourceCoord.getEnterpriseId());
            transfer.setTargetEnterpriseId(destCoord.getEnterpriseId());
            
            workRequestDAO.save(transfer);
            allWorkRequests.add(transfer);
        }
        
        LOGGER.info("      Created " + count + " Cross-Campus Transfer requests (NEU <-> BU)");
    }

    private void createTransitTransferRequests(int count) {
        List<User> mbtaManagers = getUsersByRole(User.UserRole.STATION_MANAGER);
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        List<User> students = getUsersByRole(User.UserRole.STUDENT);
        List<Organization> higherEdOrgs = organizations.get("higher_ed");
        
        if (mbtaManagers.isEmpty() || coordinators.isEmpty() || students.isEmpty()) return;

        String[] transitLines = {"Red Line", "Orange Line", "Green Line E"};

        for (int i = 0; i < count; i++) {
            User manager = mbtaManagers.get(random.nextInt(mbtaManagers.size()));
            
            // Alternate between NEU and BU
            Organization targetOrg = higherEdOrgs.get(i % higherEdOrgs.size());
            User coordinator = coordinators.stream()
                .filter(c -> c.getOrganizationId().equals(targetOrg.getOrganizationId()))
                .findFirst().orElse(coordinators.get(0));
            User student = students.stream()
                .filter(s -> s.getOrganizationId().equals(targetOrg.getOrganizationId()))
                .findFirst().orElse(students.get(0));
            
            Item item = allItems.get(random.nextInt(allItems.size()));

            TransitToUniversityTransferRequest transfer = new TransitToUniversityTransferRequest(
                manager.getEmail(), manager.getFullName(),
                String.valueOf(item.getItemId()), item.getTitle(),
                student.getEmail(), student.getFullName()
            );
            
            transfer.setTransitType("Subway");
            transfer.setRouteNumber(transitLines[random.nextInt(transitLines.length)]);
            transfer.setStationName(getRandomMBTAStation());
            transfer.setUniversityName(targetOrg.getName());
            transfer.setCampusCoordinatorId(coordinator.getEmail());
            transfer.setCampusCoordinatorName(coordinator.getFullName());
            transfer.setCampusPickupLocation(targetOrg.getName() + " Lost & Found Office");
            transfer.setMbtaIncidentNumber("MBTA-" + (2025000 + random.nextInt(10000)));
            transfer.setFoundDate(generatePastDate(1, 7));
            transfer.setStudentEmail(student.getEmail());
            transfer.setPriority(WorkRequest.RequestPriority.HIGH);
            transfer.setRequesterOrganizationId(manager.getOrganizationId());
            transfer.setTargetOrganizationId(targetOrg.getOrganizationId());
            transfer.setRequesterEnterpriseId(manager.getEnterpriseId());
            transfer.setTargetEnterpriseId(coordinator.getEnterpriseId());
            
            workRequestDAO.save(transfer);
            allWorkRequests.add(transfer);
        }
    }

    private void createAirportTransferRequests(int count) {
        List<User> specialists = getUsersByRole(User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        List<User> students = getUsersByRole(User.UserRole.STUDENT);
        List<Organization> higherEdOrgs = organizations.get("higher_ed");
        
        if (specialists.isEmpty() || coordinators.isEmpty() || students.isEmpty()) return;

        String[] terminals = {"Terminal A", "Terminal B", "Terminal C", "Terminal E"};
        String[] airlines = {"Delta", "JetBlue", "United"};

        for (int i = 0; i < count; i++) {
            User specialist = specialists.get(random.nextInt(specialists.size()));
            
            // Alternate between NEU and BU
            Organization targetOrg = higherEdOrgs.get(i % higherEdOrgs.size());
            User coordinator = coordinators.stream()
                .filter(c -> c.getOrganizationId().equals(targetOrg.getOrganizationId()))
                .findFirst().orElse(coordinators.get(0));
            User student = students.stream()
                .filter(s -> s.getOrganizationId().equals(targetOrg.getOrganizationId()))
                .findFirst().orElse(students.get(0));
            
            Item item = allItems.get(random.nextInt(allItems.size()));

            AirportToUniversityTransferRequest transfer = new AirportToUniversityTransferRequest(
                specialist.getEmail(), specialist.getFullName(),
                String.valueOf(item.getItemId()), item.getTitle(),
                student.getEmail(), student.getFullName()
            );
            
            transfer.setTerminalNumber(terminals[random.nextInt(terminals.length)]);
            transfer.setAirportArea(random.nextBoolean() ? "Security Checkpoint" : "Gate Area");
            transfer.setFoundLocation("Gate " + (char)('A' + random.nextInt(5)) + (10 + random.nextInt(30)));
            transfer.setUniversityName(targetOrg.getName());
            transfer.setCampusCoordinatorId(coordinator.getEmail());
            transfer.setCampusCoordinatorName(coordinator.getFullName());
            transfer.setCampusPickupLocation(targetOrg.getName() + " Lost & Found Office");
            transfer.setAirportIncidentNumber("LOGAN-" + (2025000 + random.nextInt(10000)));
            transfer.setFlightNumber(airlines[random.nextInt(airlines.length)].substring(0, 2).toUpperCase() + 
                (100 + random.nextInt(900)));
            transfer.setFoundDateTime(generatePastDate(1, 5) + " " + String.format("%02d:%02d", 
                6 + random.nextInt(16), random.nextInt(60)));
            transfer.setWasInSecureArea(random.nextBoolean());
            transfer.setEstimatedValue(50 + random.nextDouble() * 500);
            transfer.setPriority(WorkRequest.RequestPriority.HIGH);
            transfer.setRequesterOrganizationId(specialist.getOrganizationId());
            transfer.setTargetOrganizationId(targetOrg.getOrganizationId());
            transfer.setRequesterEnterpriseId(specialist.getEnterpriseId());
            transfer.setTargetEnterpriseId(coordinator.getEnterpriseId());
            
            workRequestDAO.save(transfer);
            allWorkRequests.add(transfer);
        }
    }

    private void createPoliceEvidenceRequests(int count) {
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        List<User> custodians = getUsersByRole(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
        
        if (coordinators.isEmpty() || custodians.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            User coordinator = coordinators.get(random.nextInt(coordinators.size()));
            User custodian = custodians.get(random.nextInt(custodians.size()));
            Item item = allItems.get(random.nextInt(allItems.size()));

            String reason = random.nextBoolean() ? 
                "High-value item requires serial verification" : 
                "Suspected stolen property - serial number check required";

            PoliceEvidenceRequest evidence = new PoliceEvidenceRequest(
                String.valueOf(coordinator.getUserId()), coordinator.getFullName(),
                String.valueOf(item.getItemId()), item.getTitle(), reason
            );
            
            evidence.setSerialNumber("SN" + faker.number().digits(12));
            evidence.setBrandName(BRANDS_ELECTRONICS[random.nextInt(BRANDS_ELECTRONICS.length)]);
            evidence.setItemCategory(item.getCategory().name());
            evidence.setEstimatedValue(200 + random.nextDouble() * 2000);
            evidence.setSourceEnterpriseName("Higher Education");
            evidence.setStolenCheck(true);
            evidence.setHighValueVerification(random.nextBoolean());
            evidence.setUrgencyLevel(random.nextBoolean() ? "High" : "Urgent");
            evidence.setPriority(WorkRequest.RequestPriority.URGENT);
            evidence.setRequesterOrganizationId(coordinator.getOrganizationId());
            evidence.setTargetOrganizationId(custodian.getOrganizationId());
            evidence.setRequesterEnterpriseId(coordinator.getEnterpriseId());
            evidence.setTargetEnterpriseId(custodian.getEnterpriseId());
            
            workRequestDAO.save(evidence);
            allWorkRequests.add(evidence);
        }
    }

    private void createMBTAToAirportEmergencyRequests(int count) {
        List<User> mbtaManagers = getUsersByRole(User.UserRole.STATION_MANAGER);
        List<User> specialists = getUsersByRole(User.UserRole.AIRPORT_LOST_FOUND_SPECIALIST);
        List<User> travelers = getUsersByRole(User.UserRole.PUBLIC_TRAVELER);
        
        if (mbtaManagers.isEmpty() || specialists.isEmpty()) return;
        
        // Create a traveler if none exist
        if (travelers.isEmpty()) {
            travelers = getUsersByRole(User.UserRole.STUDENT); // Fallback
        }

        String[] airlines = {"Delta", "American", "JetBlue", "United", "Southwest", "Spirit"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Miami", "Denver", "Seattle", "San Francisco"};
        String[] documentTypes = {"Passport", "Driver's License", "Student ID", "Travel Documents"};

        for (int i = 0; i < count; i++) {
            User manager = mbtaManagers.get(random.nextInt(mbtaManagers.size()));
            User specialist = specialists.get(random.nextInt(specialists.size()));
            User traveler = travelers.get(random.nextInt(travelers.size()));

            MBTAToAirportEmergencyRequest emergency = new MBTAToAirportEmergencyRequest();
            
            // Base request fields
            emergency.setRequesterId(String.valueOf(manager.getUserId()));
            emergency.setRequesterName(manager.getFullName());
            emergency.setRequesterEnterpriseId(manager.getEnterpriseId());
            emergency.setRequesterOrganizationId(manager.getOrganizationId());
            emergency.setTargetEnterpriseId(specialist.getEnterpriseId());
            emergency.setTargetOrganizationId(specialist.getOrganizationId());
            emergency.setPriority(WorkRequest.RequestPriority.URGENT);
            emergency.setDescription("URGENT: Traveler left critical documents on MBTA, flight departing soon");
            
            // Item details
            emergency.setItemId("emergency-item-" + faker.number().digits(6));
            emergency.setItemName(documentTypes[random.nextInt(documentTypes.length)] + " and Travel Documents");
            emergency.setItemDescription("Critical travel documents needed for international flight");
            emergency.setItemCategory("IDS_CARDS");
            
            // MBTA details
            emergency.setMbtaStationId("mbta-" + faker.number().digits(4));
            emergency.setMbtaStationName(getRandomMBTAStation());
            emergency.setMbtaStationManagerId(String.valueOf(manager.getUserId()));
            emergency.setMbtaStationManagerName(manager.getFullName());
            emergency.setTransitLine(new String[]{"Blue Line", "Silver Line"}[random.nextInt(2)]);
            emergency.setFoundLocation("Platform " + (1 + random.nextInt(2)));
            emergency.setFoundDateTime(LocalDateTime.now().minusHours(random.nextInt(3))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            emergency.setMbtaIncidentNumber("MBTA-EMG-" + faker.number().digits(6));
            
            // Airport details
            emergency.setAirportTerminal("Terminal " + (char)('A' + random.nextInt(4)));
            emergency.setAirportGate("Gate " + (char)('A' + random.nextInt(5)) + (10 + random.nextInt(30)));
            emergency.setAirportSpecialistId(String.valueOf(specialist.getUserId()));
            emergency.setAirportSpecialistName(specialist.getFullName());
            emergency.setAirportContactPhone(faker.phoneNumber().cellPhone());
            
            // Traveler details
            emergency.setTravelerId(String.valueOf(traveler.getUserId()));
            emergency.setTravelerName(traveler.getFullName());
            emergency.setTravelerPhone(faker.phoneNumber().cellPhone());
            emergency.setTravelerEmail(traveler.getEmail());
            
            // Flight details
            String airline = airlines[random.nextInt(airlines.length)];
            emergency.setFlightNumber(airline.substring(0, 2).toUpperCase() + (100 + random.nextInt(900)));
            emergency.setAirline(airline);
            emergency.setFlightDepartureTime(LocalDateTime.now().plusHours(1 + random.nextInt(3))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            emergency.setDestinationCity(cities[random.nextInt(cities.length)]);
            
            // Emergency coordination
            emergency.setEmergencyContactNumber("617-555-" + String.format("%04d", random.nextInt(10000)));
            emergency.setCourierMethod(random.nextBoolean() ? "Police Escort" : "MBTA Express Courier");
            emergency.setEstimatedDeliveryTime("45-60 minutes");
            emergency.setPoliceEscortRequested(random.nextBoolean());
            emergency.setGateHoldRequested(true);
            emergency.setGateHoldStatus("Requested");
            
            // Document verification
            emergency.setDocumentType(documentTypes[random.nextInt(documentTypes.length)]);
            emergency.setDocumentNumber("DOC" + faker.number().digits(9));
            emergency.setDocumentIssuingCountry("United States");
            emergency.setDocumentPhotoMatch(false); // To be verified
            
            // Status tracking
            emergency.setCurrentLocationStatus("At MBTA Station - Awaiting Pickup");
            emergency.setPickupConfirmationCode("PKU-" + faker.number().digits(6));
            
            workRequestDAO.save(emergency);
            allWorkRequests.add(emergency);
        }
        
        LOGGER.info("      Created " + count + " MBTA-to-Airport Emergency requests");
    }

    private void createMultiEnterpriseDisputeRequests(int count) {
        List<User> coordinators = getUsersByRole(User.UserRole.CAMPUS_COORDINATOR);
        List<User> custodians = getUsersByRole(User.UserRole.POLICE_EVIDENCE_CUSTODIAN);
        
        if (coordinators.isEmpty()) return;

        String[] disputeTypes = {"Ownership Dispute", "Claim Conflict", "Evidence Discrepancy"};
        String[] disputeReasons = {
            "Multiple parties claiming ownership of high-value item",
            "Conflicting evidence presented by different claimants",
            "Item found at enterprise boundary - jurisdiction unclear"
        };

        for (int i = 0; i < count; i++) {
            User initiator = coordinators.get(random.nextInt(coordinators.size()));
            Item item = allItems.get(random.nextInt(allItems.size()));

            MultiEnterpriseDisputeResolution dispute = new MultiEnterpriseDisputeResolution();
            
            // Base request fields
            dispute.setRequesterId(String.valueOf(initiator.getUserId()));
            dispute.setRequesterName(initiator.getFullName());
            dispute.setRequesterEnterpriseId(initiator.getEnterpriseId());
            dispute.setRequesterOrganizationId(initiator.getOrganizationId());
            dispute.setPriority(WorkRequest.RequestPriority.HIGH);
            dispute.setDescription("Multi-enterprise dispute requiring panel review");
            
            // Item details
            dispute.setItemId(String.valueOf(item.getItemId()));
            dispute.setItemName(item.getTitle());
            dispute.setItemDescription(item.getDescription());
            dispute.setItemCategory(item.getCategory().name());
            dispute.setEstimatedValue(500 + random.nextDouble() * 2000);
            // Use NEU or BU to hold the item
            List<Organization> higherEdOrgs = organizations.get("higher_ed");
            Organization holdingOrg = higherEdOrgs.get(i % higherEdOrgs.size());
            dispute.setItemCurrentLocation("Held at " + holdingOrg.getName());
            
            // Enterprise involvement
            dispute.setHoldingEnterpriseId(enterprises.get("higher_ed").getEnterpriseId());
            dispute.setHoldingEnterpriseName(enterprises.get("higher_ed").getName());
            
            List<String> involvedIds = new ArrayList<>();
            List<String> involvedNames = new ArrayList<>();
            involvedIds.add(enterprises.get("higher_ed").getEnterpriseId());
            involvedNames.add(enterprises.get("higher_ed").getName());
            involvedIds.add(enterprises.get("mbta").getEnterpriseId());
            involvedNames.add(enterprises.get("mbta").getName());
            if (random.nextBoolean()) {
                involvedIds.add(enterprises.get("airport").getEnterpriseId());
                involvedNames.add(enterprises.get("airport").getName());
            }
            dispute.setInvolvedEnterpriseIds(involvedIds);
            dispute.setInvolvedEnterpriseNames(involvedNames);
            
            // Dispute details
            dispute.setDisputeType(disputeTypes[random.nextInt(disputeTypes.length)]);
            dispute.setDisputeReason(disputeReasons[random.nextInt(disputeReasons.length)]);
            dispute.setDisputeInitiatedBy(String.valueOf(initiator.getUserId()));
            dispute.setDisputeInitiatedByName(initiator.getFullName());
            dispute.setResolutionStatus("Pending Panel Review");
            
            // Panel voting
            dispute.setPanelVotesRequired(3);
            dispute.setPanelVotesReceived(0);
            
            // Create claimants (2-3)
            List<MultiEnterpriseDisputeResolution.Claimant> claimants = new ArrayList<>();
            int numClaimants = 2 + random.nextInt(2);
            for (int j = 0; j < numClaimants; j++) {
                User claimantUser = allUsers.get(random.nextInt(allUsers.size()));
                MultiEnterpriseDisputeResolution.Claimant claimant = new MultiEnterpriseDisputeResolution.Claimant();
                claimant.claimantId = String.valueOf(claimantUser.getUserId());
                claimant.claimantName = claimantUser.getFullName();
                claimant.claimantEmail = claimantUser.getEmail();
                claimant.claimantPhone = faker.phoneNumber().cellPhone();
                claimant.enterpriseId = claimantUser.getEnterpriseId();
                claimant.enterpriseName = getEnterpriseName(claimantUser.getEnterpriseId());
                claimant.organizationId = claimantUser.getOrganizationId();
                claimant.claimDescription = faker.lorem().paragraph();
                claimant.proofDescription = "Submitted " + (random.nextBoolean() ? "receipt and photos" : "purchase history");
                claimant.trustScore = 50 + random.nextDouble() * 50;
                claimant.claimSubmittedDate = generatePastDate(1, 10);
                claimant.claimStatus = "Under Review";
                claimant.evidenceIds = new ArrayList<>();
                claimants.add(claimant);
            }
            dispute.setClaimants(claimants);
            
            // Create verification panel (3 members from different enterprises)
            List<MultiEnterpriseDisputeResolution.PanelMember> panel = new ArrayList<>();
            String[] enterpriseKeys = {"higher_ed", "mbta", "airport"};
            for (String key : enterpriseKeys) {
                List<User> enterpriseUsers = usersByEnterprise.get(key);
                if (enterpriseUsers != null && !enterpriseUsers.isEmpty()) {
                    User panelUser = enterpriseUsers.get(random.nextInt(enterpriseUsers.size()));
                    MultiEnterpriseDisputeResolution.PanelMember member = new MultiEnterpriseDisputeResolution.PanelMember();
                    member.memberId = String.valueOf(panelUser.getUserId());
                    member.memberName = panelUser.getFullName();
                    member.role = panelUser.getRole().name();
                    member.enterpriseId = panelUser.getEnterpriseId();
                    member.enterpriseName = enterprises.get(key).getName();
                    member.hasVoted = false;
                    panel.add(member);
                }
            }
            dispute.setVerificationPanel(panel);
            
            // Evidence items (empty initially)
            dispute.setEvidenceItems(new ArrayList<>());
            
            // Police involvement (optional)
            if (random.nextBoolean() && !custodians.isEmpty()) {
                User officer = custodians.get(random.nextInt(custodians.size()));
                dispute.setPoliceInvolved(true);
                dispute.setPoliceOfficerId(String.valueOf(officer.getUserId()));
                dispute.setPoliceOfficerName(officer.getFullName());
                dispute.setPoliceReportNumber("BPD-" + faker.number().digits(8));
            }
            
            // Timeline
            dispute.setDisputeStartDate(generatePastDate(1, 7));
            dispute.setEvidenceDeadline(generateFutureDate(3, 7));
            dispute.setPanelReviewDate(generateFutureDate(7, 14));
            dispute.setResolutionDeadline(generateFutureDate(14, 21));
            
            workRequestDAO.save(dispute);
            allWorkRequests.add(dispute);
        }
        
        LOGGER.info("      Created " + count + " Multi-Enterprise Dispute requests");
    }

    // ==================== TRUST SCORES ====================

    private void createTrustScores() {
        LOGGER.info("🏆 Creating Trust Scores...");
        
        int scoresCreated = 0;
        
        for (User user : allUsers) {
            try {
                double targetScore = determineTargetScore(user);
                
                TrustScore score = new TrustScore(user.getEmail(), targetScore);
                score.setUserName(user.getFullName());
                score.setUserEmail(user.getEmail());
                
                // Flag low-trust users
                if (targetScore < 30) {
                    score.setFlagged(true);
                    score.setFlagReason("Low trust score - requires review");
                }
                
                trustScoreDAO.saveTrustScore(score);
                scoresCreated++;
                
                // Generate some event history
                generateEventHistory(user.getEmail(), targetScore);
                
            } catch (Exception e) {
                // Continue with next user
            }
        }
        
        LOGGER.info("   ✓ Created " + scoresCreated + " trust scores");
    }

    private double determineTargetScore(User user) {
        // Only roles with panels are created, so we only need to handle those
        return switch (user.getRole()) {
            case SYSTEM_ADMIN, CAMPUS_COORDINATOR, STATION_MANAGER, 
                 AIRPORT_LOST_FOUND_SPECIALIST, POLICE_EVIDENCE_CUSTODIAN -> 85 + random.nextDouble() * 15;
            case CAMPUS_SECURITY, TSA_SECURITY_COORDINATOR, TRANSIT_SECURITY_INSPECTOR -> 75 + random.nextDouble() * 15;
            case STUDENT, PUBLIC_TRAVELER -> {
                // Students and travelers get varied scores
                double rand = random.nextDouble();
                if (rand < 0.1) yield 10 + random.nextDouble() * 20;
                if (rand < 0.25) yield 30 + random.nextDouble() * 20;
                if (rand < 0.55) yield 50 + random.nextDouble() * 20;
                if (rand < 0.85) yield 70 + random.nextDouble() * 20;
                yield 90 + random.nextDouble() * 10;
            }
            default -> 50 + random.nextDouble() * 30; // Fallback for any unexpected roles
        };
    }

    private void generateEventHistory(String userId, double targetScore) {
        int numEvents = 2 + random.nextInt(5);
        
        EventType[] positiveEvents = {EventType.REPORT_FOUND_ITEM, EventType.SUCCESSFUL_CLAIM, EventType.APPROVE_REQUEST};
        EventType[] negativeEvents = {EventType.CLAIM_REJECTED, EventType.NO_SHOW_PICKUP};
        
        for (int i = 0; i < numEvents; i++) {
            EventType eventType;
            if (targetScore >= 50) {
                eventType = random.nextDouble() < 0.8 ? 
                    positiveEvents[random.nextInt(positiveEvents.length)] :
                    negativeEvents[random.nextInt(negativeEvents.length)];
            } else {
                eventType = random.nextDouble() < 0.6 ?
                    negativeEvents[random.nextInt(negativeEvents.length)] :
                    positiveEvents[random.nextInt(positiveEvents.length)];
            }
            
            try {
                trustScoreService.recordEvent(userId, eventType, faker.lorem().sentence());
            } catch (Exception e) {
                // Continue
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private List<User> getUsersByRole(User.UserRole role) {
        return allUsers.stream()
            .filter(u -> u.getRole() == role)
            .toList();
    }

    private Item getRandomItemByStatus(Item.ItemStatus... statuses) {
        List<Item.ItemStatus> statusList = Arrays.asList(statuses);
        return allItems.stream()
            .filter(item -> statusList.contains(item.getStatus()))
            .skip(random.nextInt(Math.max(1, (int) allItems.stream()
                .filter(item -> statusList.contains(item.getStatus())).count())))
            .findFirst()
            .orElse(null);
    }

    private String getRandomMBTAStation() {
        String[] stations = {"Park Street", "South Station", "North Station", "Back Bay", 
            "Harvard", "Northeastern", "Ruggles", "Downtown Crossing", "Haymarket", 
            "Government Center", "Airport", "Maverick"};
        return stations[random.nextInt(stations.length)] + " Station";
    }

    private String generateIdentifyingFeatures(Item item) {
        List<String> features = new ArrayList<>();
        features.add("Distinctive " + COLORS[random.nextInt(COLORS.length)].toLowerCase() + " color");
        if (random.nextBoolean()) features.add("has visible scratches");
        if (random.nextBoolean()) features.add("contains personal items");
        if (random.nextBoolean()) features.add("has name written inside");
        return String.join(", ", features);
    }

    private String generateFutureDate(int minDays, int maxDays) {
        int days = minDays + random.nextInt(maxDays - minDays + 1);
        return LocalDateTime.now().plusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String generatePastDate(int minDays, int maxDays) {
        int days = minDays + random.nextInt(maxDays - minDays + 1);
        return LocalDateTime.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String getEnterpriseName(String enterpriseId) {
        for (Enterprise e : enterprises.values()) {
            if (e.getEnterpriseId().equals(enterpriseId)) {
                return e.getName();
            }
        }
        return "Unknown Enterprise";
    }

    // ==================== SUMMARY ====================

    private void printSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 SIMPLIFIED DATA GENERATION SUMMARY (For Testing)");
        System.out.println("=".repeat(70));
        System.out.println("Network:        " + network.getName());
        System.out.println("Enterprises:    " + enterprises.size());
        System.out.println("Organizations:  " + organizations.values().stream().mapToInt(List::size).sum());
        System.out.println("Users:          " + allUsers.size());
        System.out.println("Buildings:      " + allBuildings.size());
        System.out.println("Items:          " + allItems.size());
        System.out.println("Work Requests:  " + allWorkRequests.size());
        System.out.println("-".repeat(70));
        System.out.println("Higher Ed Structure (for Cross-Campus Demo):");
        List<Organization> higherEdOrgs = organizations.get("higher_ed");
        for (Organization org : higherEdOrgs) {
            long studentCount = allUsers.stream()
                .filter(u -> u.getOrganizationId().equals(org.getOrganizationId()) && u.getRole() == User.UserRole.STUDENT)
                .count();
            long coordCount = allUsers.stream()
                .filter(u -> u.getOrganizationId().equals(org.getOrganizationId()) && u.getRole() == User.UserRole.CAMPUS_COORDINATOR)
                .count();
            long securityCount = allUsers.stream()
                .filter(u -> u.getOrganizationId().equals(org.getOrganizationId()) && u.getRole() == User.UserRole.CAMPUS_SECURITY)
                .count();
            System.out.println("  • " + org.getName() + ": " + studentCount + " students, " + coordCount + " coordinator, " + securityCount + " security");
        }
        System.out.println("-".repeat(70));
        System.out.println("Users by Role (only roles with panels):");
        System.out.println("  Higher Ed: STUDENT, CAMPUS_COORDINATOR, CAMPUS_SECURITY");
        System.out.println("  MBTA: STATION_MANAGER, TRANSIT_SECURITY_INSPECTOR, PUBLIC_TRAVELER");
        System.out.println("  Airport: AIRPORT_LOST_FOUND_SPECIALIST, TSA_SECURITY_COORDINATOR, PUBLIC_TRAVELER");
        System.out.println("  Police: POLICE_EVIDENCE_CUSTODIAN");
        System.out.println("  System: SYSTEM_ADMIN");
        System.out.println("-".repeat(70));
        System.out.println("Work Request Breakdown:");
        System.out.println("  • Item Claims:              " + countRequestsByType(WorkRequest.RequestType.ITEM_CLAIM));
        System.out.println("  • Cross-Campus Transfers:   " + countRequestsByType(WorkRequest.RequestType.CROSS_CAMPUS_TRANSFER));
        System.out.println("  • Transit Transfers:        " + countRequestsByType(WorkRequest.RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER));
        System.out.println("  • Airport Transfers:        " + countRequestsByType(WorkRequest.RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER));
        System.out.println("  • Police Evidence:          " + countRequestsByType(WorkRequest.RequestType.POLICE_EVIDENCE_REQUEST));
        System.out.println("  • MBTA-Airport Emergency:   " + countRequestsByType(WorkRequest.RequestType.MBTA_TO_AIRPORT_EMERGENCY));
        System.out.println("  • Multi-Enterprise Dispute: " + countRequestsByType(WorkRequest.RequestType.MULTI_ENTERPRISE_DISPUTE));
        System.out.println("=".repeat(70) + "\n");
    }

    private long countRequestsByType(WorkRequest.RequestType type) {
        return allWorkRequests.stream().filter(r -> r.getRequestType() == type).count();
    }

    // ==================== MAIN ====================

    public static void main(String[] args) {
        System.out.println("🚀 Starting Enhanced Data Generator...\n");
        
        EnhancedDataGenerator generator = new EnhancedDataGenerator();
        generator.generateFullEcosystem();
        
        System.out.println("✅ Done! You can now run the application with realistic test data.");
    }
}
