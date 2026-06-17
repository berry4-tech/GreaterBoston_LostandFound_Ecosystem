package com.campus.lostfound.services;

import com.campus.lostfound.dao.MongoItemDAO;
import com.campus.lostfound.dao.MongoEnterpriseDAO;
import com.campus.lostfound.dao.MongoOrganizationDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.Item.ItemType;
import com.campus.lostfound.models.Item.ItemStatus;
import com.campus.lostfound.models.Enterprise;
import com.campus.lostfound.models.Organization;
import com.campus.lostfound.models.EnterpriseMatchResult;
import com.campus.lostfound.models.EnterpriseMatchResult.MatchType;
import com.campus.lostfound.models.EnterpriseMatchResult.TransferComplexity;
import com.campus.lostfound.models.EnterpriseMatchResult.ScoreLevel;
import com.campus.lostfound.services.ItemMatcher.PotentialMatch;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Enterprise-aware item matching service.
 * 
 * Wraps the existing ItemMatcher to provide cross-enterprise matching
 * with enterprise context, bonus scoring, and transfer complexity calculation.
 * 
 * Key features:
 * - Match items across all enterprises in the ecosystem
 * - Apply bonuses for same-enterprise matches (higher priority)
 * - Calculate transfer complexity and estimated times
 * - Integrate trust scores into match rankings
 * - Generate comprehensive match reports
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class EnterpriseItemMatcher {
    
    private static final Logger LOGGER = Logger.getLogger(EnterpriseItemMatcher.class.getName());
    
    // ==================== SCORING CONFIGURATION ====================
    
    /** Bonus applied when items are in the same enterprise */
    public static final double SAME_ENTERPRISE_BONUS = 0.15;
    
    /** Bonus applied when items are in the same network (different enterprise) */
    public static final double SAME_NETWORK_BONUS = 0.08;
    
    /** Bonus applied when items are in the same organization */
    public static final double SAME_ORGANIZATION_BONUS = 0.20;
    
    /** Bonus for high trust score users (>= 85) */
    public static final double HIGH_TRUST_BONUS = 0.05;
    
    /** Minimum score threshold for cross-enterprise matches */
    public static final double MIN_CROSS_ENTERPRISE_SCORE = 0.40;
    
    /** Minimum score threshold for same-enterprise matches */
    public static final double MIN_SAME_ENTERPRISE_SCORE = 0.30;
    
    /** Default minimum score if not specified */
    public static final double DEFAULT_MIN_SCORE = 0.30;
    
    /** High trust score threshold */
    public static final double HIGH_TRUST_THRESHOLD = 85.0;
    
    // ==================== DEPENDENCIES ====================
    
    private final ItemMatcher baseMatcher;
    private final MongoItemDAO itemDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final EnterpriseItemService enterpriseItemService;
    private TrustScoreService trustScoreService;
    
    // ==================== CONSTRUCTORS ====================
    
    public EnterpriseItemMatcher() {
        this.baseMatcher = new ItemMatcher();
        this.itemDAO = new MongoItemDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.enterpriseItemService = new EnterpriseItemService();
        
        // Lazy load TrustScoreService
        try {
            this.trustScoreService = new TrustScoreService();
        } catch (Exception e) {
            LOGGER.warning("Could not initialize TrustScoreService: " + e.getMessage());
        }
    }
    
    /**
     * Constructor for testing with dependencies
     */
    public EnterpriseItemMatcher(ItemMatcher baseMatcher, MongoItemDAO itemDAO,
                                  MongoEnterpriseDAO enterpriseDAO,
                                  MongoOrganizationDAO organizationDAO,
                                  EnterpriseItemService enterpriseItemService,
                                  TrustScoreService trustScoreService) {
        this.baseMatcher = baseMatcher;
        this.itemDAO = itemDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
        this.enterpriseItemService = enterpriseItemService;
        this.trustScoreService = trustScoreService;
    }
    
    // ==================== CORE MATCHING ====================
    
    /**
     * Find matches for an item across ALL enterprises in the ecosystem.
     * 
     * @param sourceItem The item to find matches for (typically a lost item)
     * @return List of EnterpriseMatchResult sorted by score (highest first)
     */
    public List<EnterpriseMatchResult> matchAcrossEnterprises(Item sourceItem) {
        try {
            if (sourceItem == null) {
                LOGGER.warning("Cannot match null source item");
                return new ArrayList<>();
            }
            
            LOGGER.info("Matching item across enterprises: " + sourceItem.getTitle());
            
            // Get all items from all enterprises
            List<Item> allItems = itemDAO.findAll();
            
            // Use base matcher to find potential matches
            List<PotentialMatch> baseMatches = baseMatcher.findMatches(sourceItem, allItems);
            
            // Convert to EnterpriseMatchResult with enterprise context
            List<EnterpriseMatchResult> results = new ArrayList<>();
            
            for (PotentialMatch match : baseMatches) {
                EnterpriseMatchResult emr = createEnterpriseMatchResult(
                    sourceItem, match.getItem(), match.getScore());
                
                if (emr != null) {
                    results.add(emr);
                }
            }
            
            // Sort by effective score (includes enterprise bonuses)
            Collections.sort(results);
            
            LOGGER.info("Found " + results.size() + " potential matches across enterprises");
            
            return results;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error matching across enterprises", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find matches within a specific enterprise only.
     * 
     * @param sourceItem The item to find matches for
     * @param enterpriseId The enterprise to search within
     * @return List of matches within the enterprise
     */
    public List<EnterpriseMatchResult> matchWithinEnterprise(Item sourceItem, String enterpriseId) {
        try {
            if (sourceItem == null || enterpriseId == null) {
                return new ArrayList<>();
            }
            
            // Get items only from this enterprise
            List<Item> enterpriseItems = enterpriseItemService.getItemsByEnterpriseId(enterpriseId);
            
            // Use base matcher
            List<PotentialMatch> baseMatches = baseMatcher.findMatches(sourceItem, enterpriseItems);
            
            // Convert with enterprise context
            List<EnterpriseMatchResult> results = new ArrayList<>();
            
            for (PotentialMatch match : baseMatches) {
                EnterpriseMatchResult emr = createEnterpriseMatchResult(
                    sourceItem, match.getItem(), match.getScore());
                    
                if (emr != null) {
                    results.add(emr);
                }
            }
            
            Collections.sort(results);
            return results;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error matching within enterprise", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find matches within a specific network.
     * 
     * @param sourceItem The item to find matches for
     * @param networkId The network to search within
     * @return List of matches within the network
     */
    public List<EnterpriseMatchResult> matchWithinNetwork(Item sourceItem, String networkId) {
        try {
            if (sourceItem == null || networkId == null) {
                return new ArrayList<>();
            }
            
            // Get all enterprises in the network
            List<Enterprise> networkEnterprises = enterpriseDAO.findByNetworkId(networkId);
            
            // Collect all items from network enterprises
            List<Item> networkItems = new ArrayList<>();
            for (Enterprise enterprise : networkEnterprises) {
                networkItems.addAll(
                    enterpriseItemService.getItemsByEnterpriseId(enterprise.getEnterpriseId())
                );
            }
            
            // Use base matcher
            List<PotentialMatch> baseMatches = baseMatcher.findMatches(sourceItem, networkItems);
            
            // Convert with enterprise context
            List<EnterpriseMatchResult> results = new ArrayList<>();
            
            for (PotentialMatch match : baseMatches) {
                EnterpriseMatchResult emr = createEnterpriseMatchResult(
                    sourceItem, match.getItem(), match.getScore());
                    
                if (emr != null) {
                    results.add(emr);
                }
            }
            
            Collections.sort(results);
            return results;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error matching within network", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find matches in specific enterprises.
     * 
     * @param sourceItem The item to find matches for
     * @param enterpriseIds List of enterprise IDs to search
     * @return List of matches from specified enterprises
     */
    public List<EnterpriseMatchResult> matchSpecificEnterprises(Item sourceItem, 
                                                                 List<String> enterpriseIds) {
        try {
            if (sourceItem == null || enterpriseIds == null || enterpriseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Collect items from specified enterprises
            List<Item> items = new ArrayList<>();
            for (String entId : enterpriseIds) {
                items.addAll(enterpriseItemService.getItemsByEnterpriseId(entId));
            }
            
            // Use base matcher
            List<PotentialMatch> baseMatches = baseMatcher.findMatches(sourceItem, items);
            
            // Convert with enterprise context
            List<EnterpriseMatchResult> results = new ArrayList<>();
            
            for (PotentialMatch match : baseMatches) {
                EnterpriseMatchResult emr = createEnterpriseMatchResult(
                    sourceItem, match.getItem(), match.getScore());
                    
                if (emr != null) {
                    results.add(emr);
                }
            }
            
            Collections.sort(results);
            return results;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error matching specific enterprises", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== ENHANCED SCORING ====================
    
    /**
     * Calculate the enterprise-enhanced match score.
     * Applies bonuses based on enterprise relationship and trust scores.
     * 
     * @param baseScore The base match score from ItemMatcher
     * @param sourceItem The source item
     * @param matchedItem The matched item
     * @return Enhanced score with bonuses applied
     */
    public double calculateEnterpriseMatchScore(double baseScore, Item sourceItem, Item matchedItem) {
        double enhancedScore = baseScore;
        
        // Apply enterprise bonus
        MatchType matchType = determineMatchType(sourceItem, matchedItem);
        enhancedScore = applyEnterpriseBonus(enhancedScore, matchType);
        
        // Apply trust score bonus
        double avgTrustScore = getAverageTrustScore(sourceItem, matchedItem);
        enhancedScore = applyTrustScoreModifier(enhancedScore, avgTrustScore);
        
        // Cap at 1.0
        return Math.min(1.0, enhancedScore);
    }
    
    /**
     * Apply enterprise bonus based on match type.
     */
    public double applyEnterpriseBonus(double score, MatchType matchType) {
        switch (matchType) {
            case SAME_ORGANIZATION:
                return score + SAME_ORGANIZATION_BONUS;
            case SAME_ENTERPRISE:
                return score + SAME_ENTERPRISE_BONUS;
            case SAME_NETWORK:
                return score + SAME_NETWORK_BONUS;
            case CROSS_NETWORK:
            default:
                return score; // No bonus for cross-network
        }
    }
    
    /**
     * Apply trust score modifier.
     */
    public double applyTrustScoreModifier(double score, double userTrustScore) {
        if (userTrustScore >= HIGH_TRUST_THRESHOLD) {
            return score + HIGH_TRUST_BONUS;
        }
        return score;
    }
    
    /**
     * Calculate transfer complexity between two items' organizations.
     */
    public TransferComplexity calculateTransferComplexity(Item sourceItem, Item matchedItem) {
        if (sourceItem == null || matchedItem == null) {
            return TransferComplexity.HIGH;
        }
        
        String srcOrg = sourceItem.getOrganizationId();
        String matchOrg = matchedItem.getOrganizationId();
        String srcEnt = sourceItem.getEnterpriseId();
        String matchEnt = matchedItem.getEnterpriseId();
        
        // Same organization - no transfer needed
        if (srcOrg != null && srcOrg.equals(matchOrg)) {
            return TransferComplexity.NONE;
        }
        
        // Same enterprise - low complexity
        if (srcEnt != null && srcEnt.equals(matchEnt)) {
            return TransferComplexity.LOW;
        }
        
        // Check if same network
        String srcNetwork = getNetworkIdForEnterprise(srcEnt);
        String matchNetwork = getNetworkIdForEnterprise(matchEnt);
        
        if (srcNetwork != null && srcNetwork.equals(matchNetwork)) {
            return TransferComplexity.MEDIUM;
        }
        
        // Different networks - high complexity
        return TransferComplexity.HIGH;
    }
    
    // ==================== FILTERING & RANKING ====================
    
    /**
     * Filter results by minimum score threshold.
     */
    public List<EnterpriseMatchResult> filterByMinScore(List<EnterpriseMatchResult> results, 
                                                         double threshold) {
        return results.stream()
            .filter(r -> r.getMatchScore() >= threshold)
            .collect(Collectors.toList());
    }
    
    /**
     * Filter results to only include matches from a specific enterprise.
     */
    public List<EnterpriseMatchResult> filterByEnterprise(List<EnterpriseMatchResult> results,
                                                           String enterpriseId) {
        return results.stream()
            .filter(r -> enterpriseId.equals(r.getMatchedEnterpriseId()))
            .collect(Collectors.toList());
    }
    
    /**
     * Filter results by match type.
     */
    public List<EnterpriseMatchResult> filterByMatchType(List<EnterpriseMatchResult> results,
                                                          MatchType matchType) {
        return results.stream()
            .filter(r -> r.getMatchType() == matchType)
            .collect(Collectors.toList());
    }
    
    /**
     * Filter to only cross-enterprise matches.
     */
    public List<EnterpriseMatchResult> filterCrossEnterpriseOnly(List<EnterpriseMatchResult> results) {
        return results.stream()
            .filter(EnterpriseMatchResult::isCrossEnterprise)
            .collect(Collectors.toList());
    }
    
    /**
     * Sort results by score (highest first).
     */
    public List<EnterpriseMatchResult> sortByScore(List<EnterpriseMatchResult> results) {
        List<EnterpriseMatchResult> sorted = new ArrayList<>(results);
        sorted.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        return sorted;
    }
    
    /**
     * Sort results by transfer complexity (simplest first).
     */
    public List<EnterpriseMatchResult> sortByTransferComplexity(List<EnterpriseMatchResult> results) {
        List<EnterpriseMatchResult> sorted = new ArrayList<>(results);
        sorted.sort((a, b) -> Integer.compare(
            a.getTransferComplexity().getLevel(),
            b.getTransferComplexity().getLevel()
        ));
        return sorted;
    }
    
    /**
     * Get top N matches.
     */
    public List<EnterpriseMatchResult> getTopMatches(List<EnterpriseMatchResult> results, int limit) {
        return results.stream()
            .sorted()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get matches by score level.
     */
    public List<EnterpriseMatchResult> getMatchesByScoreLevel(List<EnterpriseMatchResult> results,
                                                               ScoreLevel level) {
        return results.stream()
            .filter(r -> r.getScoreLevel() == level)
            .collect(Collectors.toList());
    }
    
    // ==================== BATCH OPERATIONS ====================
    
    /**
     * Find all potential matches for all open items in an enterprise.
     * 
     * @param enterpriseId The enterprise to analyze
     * @return Map of source items to their potential matches
     */
    public Map<Item, List<EnterpriseMatchResult>> findAllPotentialMatches(String enterpriseId) {
        Map<Item, List<EnterpriseMatchResult>> allMatches = new LinkedHashMap<>();
        
        try {
            // Get open items from this enterprise
            List<Item> openItems = enterpriseItemService.getOpenItemsByEnterprise(enterpriseId);
            
            for (Item item : openItems) {
                List<EnterpriseMatchResult> matches = matchAcrossEnterprises(item);
                
                if (!matches.isEmpty()) {
                    allMatches.put(item, matches);
                }
            }
            
            LOGGER.info("Found matches for " + allMatches.size() + " items in enterprise " + enterpriseId);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding all potential matches", e);
        }
        
        return allMatches;
    }
    
    /**
     * Find items that have no matches anywhere in the ecosystem.
     */
    public List<Item> findUnmatchedItems(String enterpriseId) {
        List<Item> unmatched = new ArrayList<>();
        
        try {
            List<Item> openItems = enterpriseItemService.getOpenItemsByEnterprise(enterpriseId);
            
            for (Item item : openItems) {
                List<EnterpriseMatchResult> matches = matchAcrossEnterprises(item);
                
                if (matches.isEmpty()) {
                    unmatched.add(item);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding unmatched items", e);
        }
        
        return unmatched;
    }
    
    /**
     * Generate a comprehensive match report for an enterprise.
     */
    public MatchReport generateMatchReport(String enterpriseId) {
        MatchReport report = new MatchReport();
        report.enterpriseId = enterpriseId;
        report.enterpriseName = enterpriseItemService.getEnterpriseName(enterpriseId);
        report.generatedAt = new Date();
        
        try {
            List<Item> openItems = enterpriseItemService.getOpenItemsByEnterprise(enterpriseId);
            report.totalItemsAnalyzed = openItems.size();
            
            int totalMatches = 0;
            int crossEnterpriseMatches = 0;
            int sameEnterpriseMatches = 0;
            double totalScore = 0;
            List<EnterpriseMatchResult> topMatches = new ArrayList<>();
            
            for (Item item : openItems) {
                List<EnterpriseMatchResult> matches = matchAcrossEnterprises(item);
                
                if (!matches.isEmpty()) {
                    totalMatches += matches.size();
                    
                    for (EnterpriseMatchResult match : matches) {
                        totalScore += match.getMatchScore();
                        
                        if (match.isCrossEnterprise()) {
                            crossEnterpriseMatches++;
                        } else {
                            sameEnterpriseMatches++;
                        }
                    }
                    
                    // Add best match to top matches
                    topMatches.add(matches.get(0));
                }
            }
            
            report.matchesFound = totalMatches;
            report.crossEnterpriseMatches = crossEnterpriseMatches;
            report.sameEnterpriseMatches = sameEnterpriseMatches;
            report.averageMatchScore = totalMatches > 0 ? totalScore / totalMatches : 0;
            report.itemsWithMatches = topMatches.size();
            report.itemsWithoutMatches = openItems.size() - topMatches.size();
            
            // Sort top matches by score and take top 10
            topMatches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            report.topMatches = topMatches.stream().limit(10).collect(Collectors.toList());
            
            // Calculate match type distribution
            report.matchTypeDistribution = new HashMap<>();
            for (MatchType type : MatchType.values()) {
                long count = topMatches.stream()
                    .filter(m -> m.getMatchType() == type)
                    .count();
                report.matchTypeDistribution.put(type, count);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating match report", e);
        }
        
        return report;
    }
    
    /**
     * Find best matches across all enterprises for dashboard display.
     */
    public List<EnterpriseMatchResult> findBestMatchesAcrossSystem(int limit) {
        List<EnterpriseMatchResult> allBestMatches = new ArrayList<>();
        
        try {
            List<Item> allOpenItems = itemDAO.findAll().stream()
                .filter(i -> i.getStatus() == ItemStatus.OPEN || 
                            i.getStatus() == ItemStatus.PENDING_CLAIM)
                .filter(i -> i.getType() == ItemType.LOST) // Focus on lost items
                .limit(50) // Process up to 50 items
                .collect(Collectors.toList());
            
            for (Item item : allOpenItems) {
                List<EnterpriseMatchResult> matches = matchAcrossEnterprises(item);
                
                if (!matches.isEmpty()) {
                    // Add the best match for this item
                    allBestMatches.add(matches.get(0));
                }
            }
            
            // Sort by score and return top N
            allBestMatches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            return allBestMatches.stream().limit(limit).collect(Collectors.toList());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding best matches across system", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Create an EnterpriseMatchResult with full context.
     */
    private EnterpriseMatchResult createEnterpriseMatchResult(Item sourceItem, Item matchedItem, 
                                                               double baseScore) {
        try {
            EnterpriseMatchResult emr = new EnterpriseMatchResult();
            emr.setSourceItem(sourceItem);
            emr.setMatchedItem(matchedItem);
            
            // Determine match type
            MatchType matchType = determineMatchType(sourceItem, matchedItem);
            emr.setMatchType(matchType);
            
            // Calculate enhanced score
            double enhancedScore = calculateEnterpriseMatchScore(baseScore, sourceItem, matchedItem);
            emr.setMatchScore(enhancedScore);
            
            // Set enterprise context for source
            emr.setSourceEnterpriseId(sourceItem.getEnterpriseId());
            emr.setSourceEnterpriseName(enterpriseItemService.getEnterpriseName(sourceItem.getEnterpriseId()));
            emr.setSourceOrganizationId(sourceItem.getOrganizationId());
            emr.setSourceOrganizationName(enterpriseItemService.getOrganizationName(sourceItem.getOrganizationId()));
            
            // Set enterprise context for matched
            emr.setMatchedEnterpriseId(matchedItem.getEnterpriseId());
            emr.setMatchedEnterpriseName(enterpriseItemService.getEnterpriseName(matchedItem.getEnterpriseId()));
            emr.setMatchedOrganizationId(matchedItem.getOrganizationId());
            emr.setMatchedOrganizationName(enterpriseItemService.getOrganizationName(matchedItem.getOrganizationId()));
            
            // Set transfer complexity
            TransferComplexity complexity = calculateTransferComplexity(sourceItem, matchedItem);
            emr.setTransferComplexity(complexity);
            emr.setTransferRequired(complexity != TransferComplexity.NONE);
            emr.setEstimatedTransferTime(getEstimatedTransferTime(complexity));
            
            // Set user context
            if (sourceItem.getReportedBy() != null) {
                emr.setSourceUserId(sourceItem.getReportedBy().getEmail());
                emr.setSourceUserName(sourceItem.getReportedBy().getFullName());
                emr.setSourceUserTrustScore(getTrustScore(sourceItem.getReportedBy().getEmail()));
            }
            
            if (matchedItem.getReportedBy() != null) {
                emr.setMatchedUserId(matchedItem.getReportedBy().getEmail());
                emr.setMatchedUserName(matchedItem.getReportedBy().getFullName());
                emr.setMatchedUserTrustScore(getTrustScore(matchedItem.getReportedBy().getEmail()));
            }
            
            // Check if verification required
            boolean needsVerification = matchedItem.getEstimatedValue() >= 500 ||
                                        emr.getSourceUserTrustScore() < 50 ||
                                        emr.getMatchedUserTrustScore() < 50 ||
                                        complexity == TransferComplexity.HIGH;
            emr.setRequiresVerification(needsVerification);
            
            if (needsVerification) {
                emr.setVerificationReason(getVerificationReason(emr));
            }
            
            // Store detailed scores (approximations based on base score)
            emr.setCategoryScore(baseScore * 0.3);  // Category is ~30% of score
            emr.setKeywordScore(baseScore * 0.25);  // Keywords ~25%
            emr.setLocationScore(baseScore * 0.2);  // Location ~20%
            emr.setTimeScore(baseScore * 0.15);     // Time ~15%
            
            // Calculate bonuses applied
            double enterpriseBonus = enhancedScore - baseScore - 
                (emr.getSourceUserTrustScore() >= HIGH_TRUST_THRESHOLD ? HIGH_TRUST_BONUS : 0);
            emr.setEnterpriseBonus(Math.max(0, enterpriseBonus));
            
            double trustBonus = emr.getSourceUserTrustScore() >= HIGH_TRUST_THRESHOLD ? HIGH_TRUST_BONUS : 0;
            emr.setTrustBonus(trustBonus);
            
            return emr;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating EnterpriseMatchResult", e);
            return null;
        }
    }
    
    /**
     * Determine the match type based on enterprise/organization relationship.
     */
    private MatchType determineMatchType(Item sourceItem, Item matchedItem) {
        if (sourceItem == null || matchedItem == null) {
            return MatchType.CROSS_NETWORK;
        }
        
        String srcOrg = sourceItem.getOrganizationId();
        String matchOrg = matchedItem.getOrganizationId();
        String srcEnt = sourceItem.getEnterpriseId();
        String matchEnt = matchedItem.getEnterpriseId();
        
        // Same organization
        if (srcOrg != null && srcOrg.equals(matchOrg)) {
            return MatchType.SAME_ORGANIZATION;
        }
        
        // Same enterprise
        if (srcEnt != null && srcEnt.equals(matchEnt)) {
            return MatchType.SAME_ENTERPRISE;
        }
        
        // Check network
        String srcNetwork = getNetworkIdForEnterprise(srcEnt);
        String matchNetwork = getNetworkIdForEnterprise(matchEnt);
        
        if (srcNetwork != null && srcNetwork.equals(matchNetwork)) {
            return MatchType.SAME_NETWORK;
        }
        
        return MatchType.CROSS_NETWORK;
    }
    
    /**
     * Get network ID for an enterprise.
     */
    private String getNetworkIdForEnterprise(String enterpriseId) {
        if (enterpriseId == null) return null;
        
        try {
            Optional<Enterprise> entOpt = enterpriseDAO.findById(enterpriseId);
            return entOpt.map(Enterprise::getNetworkId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get trust score for a user.
     */
    private double getTrustScore(String userId) {
        if (trustScoreService == null || userId == null) {
            return 50.0; // Default
        }
        
        try {
            return trustScoreService.getTrustScore(userId);
        } catch (Exception e) {
            return 50.0;
        }
    }
    
    /**
     * Get average trust score between source and matched item reporters.
     */
    private double getAverageTrustScore(Item sourceItem, Item matchedItem) {
        double sourceTrust = 50.0;
        double matchedTrust = 50.0;
        
        if (sourceItem.getReportedBy() != null) {
            sourceTrust = getTrustScore(sourceItem.getReportedBy().getEmail());
        }
        
        if (matchedItem.getReportedBy() != null) {
            matchedTrust = getTrustScore(matchedItem.getReportedBy().getEmail());
        }
        
        return (sourceTrust + matchedTrust) / 2;
    }
    
    /**
     * Get estimated transfer time based on complexity.
     */
    private String getEstimatedTransferTime(TransferComplexity complexity) {
        switch (complexity) {
            case NONE: return "Immediate";
            case LOW: return "1-2 business days";
            case MEDIUM: return "2-3 business days";
            case HIGH: return "3-5 business days";
            default: return "Unknown";
        }
    }
    
    /**
     * Get reason for verification requirement.
     */
    private String getVerificationReason(EnterpriseMatchResult emr) {
        List<String> reasons = new ArrayList<>();
        
        if (emr.getMatchedItem() != null && emr.getMatchedItem().getEstimatedValue() >= 500) {
            reasons.add("High-value item ($" + String.format("%.0f", emr.getMatchedItem().getEstimatedValue()) + ")");
        }
        
        if (emr.getSourceUserTrustScore() < 50) {
            reasons.add("Low source user trust score");
        }
        
        if (emr.getMatchedUserTrustScore() < 50) {
            reasons.add("Low matched user trust score");
        }
        
        if (emr.getTransferComplexity() == TransferComplexity.HIGH) {
            reasons.add("High complexity transfer");
        }
        
        return reasons.isEmpty() ? null : String.join("; ", reasons);
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Comprehensive match report for an enterprise.
     */
    public static class MatchReport {
        public String enterpriseId;
        public String enterpriseName;
        public Date generatedAt;
        public int totalItemsAnalyzed;
        public int matchesFound;
        public int crossEnterpriseMatches;
        public int sameEnterpriseMatches;
        public int itemsWithMatches;
        public int itemsWithoutMatches;
        public double averageMatchScore;
        public List<EnterpriseMatchResult> topMatches;
        public Map<MatchType, Long> matchTypeDistribution;
        
        public String getSummary() {
            return String.format(
                "Match Report for %s\n" +
                "Generated: %s\n" +
                "Items Analyzed: %d\n" +
                "Total Matches Found: %d\n" +
                "  - Same Enterprise: %d\n" +
                "  - Cross Enterprise: %d\n" +
                "Items with Matches: %d\n" +
                "Items without Matches: %d\n" +
                "Average Match Score: %.0f%%",
                enterpriseName,
                generatedAt,
                totalItemsAnalyzed,
                matchesFound,
                sameEnterpriseMatches,
                crossEnterpriseMatches,
                itemsWithMatches,
                itemsWithoutMatches,
                averageMatchScore * 100
            );
        }
        
        public Object[] toSummaryRow() {
            return new Object[] {
                enterpriseName,
                totalItemsAnalyzed,
                matchesFound,
                sameEnterpriseMatches,
                crossEnterpriseMatches,
                String.format("%.0f%%", averageMatchScore * 100)
            };
        }
        
        public static String[] getSummaryColumns() {
            return new String[] {
                "Enterprise", "Items", "Matches", "Same Ent.", "Cross Ent.", "Avg Score"
            };
        }
    }
}
