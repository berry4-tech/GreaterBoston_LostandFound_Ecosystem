package com.campus.lostfound.services;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.workrequest.WorkRequest;
import com.campus.lostfound.models.workrequest.WorkRequest.*;
import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive analytics service for the Lost & Found ecosystem.
 * 
 * Provides analytics and reporting across:
 * - Item statistics (counts, recovery rates, trends)
 * - User statistics (activity, trust scores, contributions)
 * - Enterprise statistics (cross-enterprise metrics, network effects)
 * - Work request analytics (SLA compliance, approval times)
 * - Time-based trends (daily, weekly, monthly)
 * - Dashboard aggregations (executive summary, quick stats, alerts)
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class AnalyticsService {
    
    private static final Logger LOGGER = Logger.getLogger(AnalyticsService.class.getName());
    
    // ==================== DEPENDENCIES ====================
    
    private final MongoItemDAO itemDAO;
    private final MongoUserDAO userDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    private final MongoOrganizationDAO organizationDAO;
    private final MongoWorkRequestDAO workRequestDAO;
    private final MongoTrustScoreDAO trustScoreDAO;
    private final EnterpriseItemService enterpriseItemService;
    
    // Cache for expensive calculations
    private Map<String, Object> analyticsCache = new HashMap<>();
    private long cacheRefreshTime = 0;
    private static final long CACHE_TTL = 2 * 60 * 1000; // 2 minutes
    
    // ==================== CONSTRUCTORS ====================
    
    public AnalyticsService() {
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
        this.organizationDAO = new MongoOrganizationDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.trustScoreDAO = new MongoTrustScoreDAO();
        this.enterpriseItemService = new EnterpriseItemService();
    }
    
    /**
     * Constructor for testing with mock DAOs
     */
    public AnalyticsService(MongoItemDAO itemDAO, MongoUserDAO userDAO,
                           MongoEnterpriseDAO enterpriseDAO, MongoOrganizationDAO organizationDAO,
                           MongoWorkRequestDAO workRequestDAO, MongoTrustScoreDAO trustScoreDAO,
                           EnterpriseItemService enterpriseItemService) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.enterpriseDAO = enterpriseDAO;
        this.organizationDAO = organizationDAO;
        this.workRequestDAO = workRequestDAO;
        this.trustScoreDAO = trustScoreDAO;
        this.enterpriseItemService = enterpriseItemService;
    }
    
    // ==================== ITEM ANALYTICS ====================
    
    /**
     * Get total count of all items in the system
     */
    public long getTotalItemCount() {
        try {
            return itemDAO.findAll().size();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting total item count", e);
            return 0;
        }
    }
    
    /**
     * Get item counts grouped by status
     */
    public Map<ItemStatus, Long> getItemCountByStatus() {
        try {
            List<Item> items = itemDAO.findAll();
            return items.stream()
                .filter(i -> i.getStatus() != null)
                .collect(Collectors.groupingBy(Item::getStatus, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item count by status", e);
            return new EnumMap<>(ItemStatus.class);
        }
    }
    
    /**
     * Get item counts grouped by type (LOST/FOUND)
     */
    public Map<ItemType, Long> getItemCountByType() {
        try {
            List<Item> items = itemDAO.findAll();
            return items.stream()
                .filter(i -> i.getType() != null)
                .collect(Collectors.groupingBy(Item::getType, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item count by type", e);
            return new EnumMap<>(ItemType.class);
        }
    }
    
    /**
     * Get item counts grouped by category
     */
    public Map<ItemCategory, Long> getItemCountByCategory() {
        try {
            List<Item> items = itemDAO.findAll();
            return items.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(Item::getCategory, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item count by category", e);
            return new EnumMap<>(ItemCategory.class);
        }
    }
    
    /**
     * Get daily item counts for the past N days
     */
    public List<DailyCount> getItemsReportedOverTime(int days) {
        try {
            List<Item> items = itemDAO.findAll();
            Date cutoff = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
            
            // Group by date
            Map<LocalDate, Long> dailyCounts = items.stream()
                .filter(i -> i.getReportedDate() != null && i.getReportedDate().after(cutoff))
                .collect(Collectors.groupingBy(
                    i -> i.getReportedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    Collectors.counting()
                ));
            
            // Fill in missing days with 0
            List<DailyCount> result = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                long count = dailyCounts.getOrDefault(date, 0L);
                result.add(new DailyCount(date, count));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting items reported over time", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Calculate overall recovery rate (claimed / total reported)
     */
    public double getRecoveryRate() {
        try {
            List<Item> items = itemDAO.findAll();
            if (items.isEmpty()) return 0.0;
            
            long claimed = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .count();
            
            return (double) claimed / items.size();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating recovery rate", e);
            return 0.0;
        }
    }
    
    /**
     * Calculate average recovery time in hours (from reported to claimed)
     */
    public double getAverageRecoveryTime() {
        try {
            List<Item> items = itemDAO.findAll();
            
            List<Long> recoveryTimes = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .filter(i -> i.getReportedDate() != null && i.getResolvedDate() != null)
                .map(i -> {
                    long millis = i.getResolvedDate().getTime() - i.getReportedDate().getTime();
                    return millis / (1000 * 60 * 60); // Convert to hours
                })
                .collect(Collectors.toList());
            
            if (recoveryTimes.isEmpty()) return 0.0;
            
            return recoveryTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating average recovery time", e);
            return 0.0;
        }
    }
    
    /**
     * Get statistics for high-value items (>= $500)
     */
    public HighValueStats getHighValueItemStats() {
        try {
            List<Item> items = itemDAO.findAll();
            double highValueThreshold = 500.0;
            
            List<Item> highValueItems = items.stream()
                .filter(i -> i.getEstimatedValue() >= highValueThreshold)
                .collect(Collectors.toList());
            
            long total = highValueItems.size();
            long claimed = highValueItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .count();
            long pending = highValueItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.PENDING_CLAIM)
                .count();
            long open = highValueItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.OPEN)
                .count();
            
            double totalValue = highValueItems.stream()
                .mapToDouble(Item::getEstimatedValue)
                .sum();
            
            double recoveryRate = total > 0 ? (double) claimed / total : 0.0;
            
            return new HighValueStats(total, claimed, pending, open, totalValue, recoveryRate);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting high value item stats", e);
            return new HighValueStats(0, 0, 0, 0, 0.0, 0.0);
        }
    }
    
    // ==================== USER ANALYTICS ====================
    
    /**
     * Get total count of all users
     */
    public long getTotalUserCount() {
        try {
            return userDAO.findAll().size();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting total user count", e);
            return 0;
        }
    }
    
    /**
     * Get count of active users in the past N days
     * (users who have reported or claimed items)
     */
    public long getActiveUserCount(int days) {
        try {
            List<Item> items = itemDAO.findAll();
            Date cutoff = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
            
            Set<String> activeUserIds = new HashSet<>();
            
            for (Item item : items) {
                if (item.getReportedDate() != null && item.getReportedDate().after(cutoff)) {
                    if (item.getReportedBy() != null) {
                        activeUserIds.add(item.getReportedBy().getEmail());
                    }
                }
            }
            
            return activeUserIds.size();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting active user count", e);
            return 0;
        }
    }
    
    /**
     * Get user counts grouped by role
     */
    public Map<UserRole, Long> getUserCountByRole() {
        try {
            List<User> users = userDAO.findAll();
            return users.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting user count by role", e);
            return new EnumMap<>(UserRole.class);
        }
    }
    
    /**
     * Get user counts grouped by enterprise
     */
    public Map<String, Long> getUserCountByEnterprise() {
        try {
            List<User> users = userDAO.findAll();
            
            Map<String, Long> counts = users.stream()
                .filter(u -> u.getEnterpriseId() != null)
                .collect(Collectors.groupingBy(User::getEnterpriseId, Collectors.counting()));
            
            // Convert enterprise IDs to names
            Map<String, Long> namedCounts = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                String name = enterpriseItemService.getEnterpriseName(entry.getKey());
                namedCounts.put(name, entry.getValue());
            }
            
            return namedCounts;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting user count by enterprise", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Calculate average trust score across all users
     */
    public double getAverageTrustScore() {
        try {
            List<TrustScore> scores = trustScoreDAO.findAllScores();
            if (scores.isEmpty()) {
                // Fall back to user model trust scores
                List<User> users = userDAO.findAll();
                return users.stream()
                    .mapToDouble(User::getTrustScore)
                    .average()
                    .orElse(50.0);
            }
            
            return scores.stream()
                .mapToDouble(TrustScore::getCurrentScore)
                .average()
                .orElse(50.0);
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating average trust score", e);
            return 50.0;
        }
    }
    
    /**
     * Get trust score distribution by level
     */
    public Map<ScoreLevel, Long> getTrustScoreDistribution() {
        try {
            List<TrustScore> scores = trustScoreDAO.findAllScores();
            
            if (scores.isEmpty()) {
                // Fall back to user model
                List<User> users = userDAO.findAll();
                Map<ScoreLevel, Long> dist = new EnumMap<>(ScoreLevel.class);
                for (ScoreLevel level : ScoreLevel.values()) {
                    dist.put(level, 0L);
                }
                
                for (User user : users) {
                    ScoreLevel level = getScoreLevelFromValue(user.getTrustScore());
                    dist.merge(level, 1L, Long::sum);
                }
                return dist;
            }
            
            return scores.stream()
                .collect(Collectors.groupingBy(TrustScore::getScoreLevel, Collectors.counting()));
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting trust score distribution", e);
            return new EnumMap<>(ScoreLevel.class);
        }
    }
    
    /**
     * Get top N contributors (users with most successful returns)
     */
    public List<UserContribution> getTopContributors(int limit) {
        try {
            List<User> users = userDAO.findAll();
            
            return users.stream()
                .map(u -> new UserContribution(
                    u.getEmail(),
                    u.getFullName(),
                    u.getItemsReported(),
                    u.getItemsReturned(),
                    u.getTrustScore()
                ))
                .sorted((a, b) -> {
                    // Sort by items returned, then by trust score
                    int cmp = Integer.compare(b.itemsReturned, a.itemsReturned);
                    if (cmp == 0) {
                        cmp = Double.compare(b.trustScore, a.trustScore);
                    }
                    return cmp;
                })
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting top contributors", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== ENTERPRISE ANALYTICS ====================
    
    /**
     * Get comprehensive statistics for all enterprises
     */
    public List<EnterpriseStats> getEnterpriseStats() {
        try {
            List<Enterprise> enterprises = enterpriseDAO.findAll();
            List<Item> allItems = itemDAO.findAll();
            List<User> allUsers = userDAO.findAll();
            
            List<EnterpriseStats> stats = new ArrayList<>();
            
            for (Enterprise enterprise : enterprises) {
                String entId = enterprise.getEnterpriseId();
                
                // Count items
                List<Item> entItems = allItems.stream()
                    .filter(i -> entId.equals(i.getEnterpriseId()))
                    .collect(Collectors.toList());
                
                long totalItems = entItems.size();
                long lostItems = entItems.stream()
                    .filter(i -> i.getType() == ItemType.LOST).count();
                long foundItems = entItems.stream()
                    .filter(i -> i.getType() == ItemType.FOUND).count();
                long claimedItems = entItems.stream()
                    .filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
                long openItems = entItems.stream()
                    .filter(i -> i.getStatus() == ItemStatus.OPEN).count();
                
                double recoveryRate = totalItems > 0 ? (double) claimedItems / totalItems : 0.0;
                
                // Count users
                long userCount = allUsers.stream()
                    .filter(u -> entId.equals(u.getEnterpriseId()))
                    .count();
                
                stats.add(new EnterpriseStats(
                    entId, enterprise.getName(), totalItems, lostItems, foundItems,
                    claimedItems, openItems, recoveryRate, userCount
                ));
            }
            
            // Sort by total items descending
            stats.sort((a, b) -> Long.compare(b.totalItems, a.totalItems));
            
            return stats;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting enterprise stats", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get item counts by enterprise (ID -> count)
     */
    public Map<String, Long> getItemsByEnterprise() {
        try {
            List<Item> items = itemDAO.findAll();
            
            Map<String, Long> counts = items.stream()
                .filter(i -> i.getEnterpriseId() != null)
                .collect(Collectors.groupingBy(Item::getEnterpriseId, Collectors.counting()));
            
            // Convert to enterprise names
            Map<String, Long> namedCounts = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                String name = enterpriseItemService.getEnterpriseName(entry.getKey());
                namedCounts.put(name, entry.getValue());
            }
            
            return namedCounts;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting items by enterprise", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get recovery rate by enterprise
     */
    public Map<String, Double> getRecoveryRateByEnterprise() {
        try {
            return enterpriseItemService.getRecoveryRateByEnterprise();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recovery rate by enterprise", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Count cross-enterprise transfer requests
     */
    public long getCrossEnterpriseTransferCount() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            
            return requests.stream()
                .filter(r -> r.getRequestType() == RequestType.CROSS_CAMPUS_TRANSFER ||
                            r.getRequestType() == RequestType.TRANSIT_TO_UNIVERSITY_TRANSFER ||
                            r.getRequestType() == RequestType.AIRPORT_TO_UNIVERSITY_TRANSFER)
                .count();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting cross-enterprise transfers", e);
            return 0;
        }
    }
    
    /**
     * Get cross-enterprise match rate from EnterpriseItemService
     */
    public double getCrossEnterpriseMatchRate() {
        try {
            return enterpriseItemService.getCrossEnterpriseMatchRate();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting cross-enterprise match rate", e);
            return 0.0;
        }
    }
    
    /**
     * Get network effect metrics showing recovery improvement with collaboration
     */
    public NetworkEffectStats getNetworkEffectMetrics() {
        try {
            EnterpriseItemService.NetworkEffectMetrics metrics = 
                enterpriseItemService.getNetworkEffectMetrics();
            
            if (metrics == null) {
                return new NetworkEffectStats();
            }
            
            // Wrap in our stats class
            NetworkEffectStats stats = new NetworkEffectStats();
            stats.overallRecoveryRate = metrics.overallRecoveryRate;
            stats.singleEnterpriseRate = metrics.singleEnterpriseRecoveryRate;
            stats.twoEnterpriseRate = metrics.twoEnterpriseRecoveryRate;
            stats.threeEnterpriseRate = metrics.threeEnterpriseRecoveryRate;
            stats.fourPlusEnterpriseRate = metrics.fourEnterpriseRecoveryRate;
            
            return stats;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting network effect metrics", e);
            return new NetworkEffectStats();
        }
    }
    
    // ==================== WORK REQUEST ANALYTICS ====================
    
    /**
     * Get work request counts by type
     */
    public Map<RequestType, Long> getRequestCountByType() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            return requests.stream()
                .filter(r -> r.getRequestType() != null)
                .collect(Collectors.groupingBy(WorkRequest::getRequestType, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request count by type", e);
            return new EnumMap<>(RequestType.class);
        }
    }
    
    /**
     * Get work request counts by status
     */
    public Map<RequestStatus, Long> getRequestCountByStatus() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            return requests.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(WorkRequest::getStatus, Collectors.counting()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request count by status", e);
            return new EnumMap<>(RequestStatus.class);
        }
    }
    
    /**
     * Calculate average approval time in hours
     */
    public double getAverageApprovalTime() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            
            List<Long> approvalTimes = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED || 
                            r.getStatus() == RequestStatus.APPROVED)
                .filter(r -> r.getCreatedAt() != null && r.getCompletedAt() != null)
                .map(r -> ChronoUnit.HOURS.between(r.getCreatedAt(), r.getCompletedAt()))
                .collect(Collectors.toList());
            
            if (approvalTimes.isEmpty()) return 0.0;
            
            return approvalTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating average approval time", e);
            return 0.0;
        }
    }
    
    /**
     * Calculate SLA compliance rate (requests completed within SLA target)
     */
    public double getSLAComplianceRate() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            
            List<WorkRequest> completedRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED || 
                            r.getStatus() == RequestStatus.APPROVED)
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.toList());
            
            if (completedRequests.isEmpty()) return 1.0; // No completed requests = 100% compliance
            
            long compliant = completedRequests.stream()
                .filter(r -> {
                    LocalDateTime endTime = r.getCompletedAt() != null ? 
                        r.getCompletedAt() : LocalDateTime.now();
                    long hours = ChronoUnit.HOURS.between(r.getCreatedAt(), endTime);
                    return hours <= r.getSlaTargetHours();
                })
                .count();
            
            return (double) compliant / completedRequests.size();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating SLA compliance rate", e);
            return 0.0;
        }
    }
    
    /**
     * Get pending work requests grouped by enterprise
     */
    public Map<String, Long> getPendingRequestsByEnterprise() {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            
            Map<String, Long> counts = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING || 
                            r.getStatus() == RequestStatus.IN_PROGRESS)
                .filter(r -> r.getRequesterEnterpriseId() != null)
                .collect(Collectors.groupingBy(
                    WorkRequest::getRequesterEnterpriseId, Collectors.counting()));
            
            // Convert to enterprise names
            Map<String, Long> namedCounts = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                String name = enterpriseItemService.getEnterpriseName(entry.getKey());
                namedCounts.put(name, entry.getValue());
            }
            
            return namedCounts;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting pending requests by enterprise", e);
            return new HashMap<>();
        }
    }
    
    // ==================== TIME-BASED ANALYTICS ====================
    
    /**
     * Get daily statistics for a date range
     */
    public List<DailyStats> getDailyStats(Date startDate, Date endDate) {
        try {
            List<Item> items = itemDAO.findAll();
            List<WorkRequest> requests = workRequestDAO.findAll();
            
            LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            List<DailyStats> result = new ArrayList<>();
            
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                final LocalDate currentDate = date;
                
                // Items reported on this date
                long itemsReported = items.stream()
                    .filter(i -> i.getReportedDate() != null)
                    .filter(i -> {
                        LocalDate itemDate = i.getReportedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        return itemDate.equals(currentDate);
                    })
                    .count();
                
                // Items recovered on this date
                long recoveries = items.stream()
                    .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                    .filter(i -> i.getResolvedDate() != null)
                    .filter(i -> {
                        LocalDate resolvedDate = i.getResolvedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        return resolvedDate.equals(currentDate);
                    })
                    .count();
                
                // Requests created on this date
                long requestsCreated = requests.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(currentDate))
                    .count();
                
                result.add(new DailyStats(currentDate, itemsReported, requestsCreated, recoveries));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting daily stats", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get weekly trends for past N weeks
     */
    public List<WeeklyTrend> getWeeklyTrends(int weeks) {
        try {
            List<Item> items = itemDAO.findAll();
            List<WeeklyTrend> result = new ArrayList<>();
            
            LocalDate today = LocalDate.now();
            
            for (int i = weeks - 1; i >= 0; i--) {
                LocalDate weekEnd = today.minusWeeks(i);
                LocalDate weekStart = weekEnd.minusDays(6);
                
                final LocalDate finalWeekStart = weekStart;
                final LocalDate finalWeekEnd = weekEnd;
                
                // Items in this week
                List<Item> weekItems = items.stream()
                    .filter(item -> item.getReportedDate() != null)
                    .filter(item -> {
                        LocalDate itemDate = item.getReportedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        return !itemDate.isBefore(finalWeekStart) && !itemDate.isAfter(finalWeekEnd);
                    })
                    .collect(Collectors.toList());
                
                long itemsReported = weekItems.size();
                long lostItems = weekItems.stream()
                    .filter(item -> item.getType() == ItemType.LOST).count();
                long foundItems = weekItems.stream()
                    .filter(item -> item.getType() == ItemType.FOUND).count();
                long recovered = weekItems.stream()
                    .filter(item -> item.getStatus() == ItemStatus.CLAIMED).count();
                
                double recoveryRate = itemsReported > 0 ? (double) recovered / itemsReported : 0.0;
                
                result.add(new WeeklyTrend(weekStart, weekEnd, itemsReported, 
                    lostItems, foundItems, recovered, recoveryRate));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting weekly trends", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get monthly trends for past N months
     */
    public List<MonthlyTrend> getMonthlyTrends(int months) {
        try {
            List<Item> items = itemDAO.findAll();
            List<MonthlyTrend> result = new ArrayList<>();
            
            YearMonth current = YearMonth.now();
            
            for (int i = months - 1; i >= 0; i--) {
                YearMonth month = current.minusMonths(i);
                LocalDate monthStart = month.atDay(1);
                LocalDate monthEnd = month.atEndOfMonth();
                
                final LocalDate finalMonthStart = monthStart;
                final LocalDate finalMonthEnd = monthEnd;
                
                // Items in this month
                List<Item> monthItems = items.stream()
                    .filter(item -> item.getReportedDate() != null)
                    .filter(item -> {
                        LocalDate itemDate = item.getReportedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        return !itemDate.isBefore(finalMonthStart) && !itemDate.isAfter(finalMonthEnd);
                    })
                    .collect(Collectors.toList());
                
                long itemsReported = monthItems.size();
                long recovered = monthItems.stream()
                    .filter(item -> item.getStatus() == ItemStatus.CLAIMED).count();
                
                double recoveryRate = itemsReported > 0 ? (double) recovered / itemsReported : 0.0;
                
                // Calculate total value
                double totalValue = monthItems.stream()
                    .mapToDouble(Item::getEstimatedValue)
                    .sum();
                
                result.add(new MonthlyTrend(month, itemsReported, recovered, recoveryRate, totalValue));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting monthly trends", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get year-over-year comparison
     */
    public YearComparison getYearOverYearComparison() {
        try {
            List<Item> items = itemDAO.findAll();
            
            LocalDate today = LocalDate.now();
            LocalDate thisYearStart = today.withDayOfYear(1);
            LocalDate lastYearStart = thisYearStart.minusYears(1);
            LocalDate lastYearEnd = thisYearStart.minusDays(1);
            
            // This year's items (YTD)
            List<Item> thisYearItems = items.stream()
                .filter(i -> i.getReportedDate() != null)
                .filter(i -> {
                    LocalDate date = i.getReportedDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                    return !date.isBefore(thisYearStart);
                })
                .collect(Collectors.toList());
            
            // Same period last year
            int dayOfYear = today.getDayOfYear();
            LocalDate lastYearSamePeriodEnd = lastYearStart.plusDays(dayOfYear - 1);
            
            List<Item> lastYearItems = items.stream()
                .filter(i -> i.getReportedDate() != null)
                .filter(i -> {
                    LocalDate date = i.getReportedDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                    return !date.isBefore(lastYearStart) && !date.isAfter(lastYearSamePeriodEnd);
                })
                .collect(Collectors.toList());
            
            long thisYearCount = thisYearItems.size();
            long lastYearCount = lastYearItems.size();
            
            long thisYearRecovered = thisYearItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
            long lastYearRecovered = lastYearItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
            
            double thisYearRate = thisYearCount > 0 ? (double) thisYearRecovered / thisYearCount : 0.0;
            double lastYearRate = lastYearCount > 0 ? (double) lastYearRecovered / lastYearCount : 0.0;
            
            double itemsChange = lastYearCount > 0 ? 
                ((double) thisYearCount - lastYearCount) / lastYearCount : 0.0;
            double rateChange = lastYearRate > 0 ? 
                (thisYearRate - lastYearRate) / lastYearRate : 0.0;
            
            return new YearComparison(
                today.getYear(), today.getYear() - 1,
                thisYearCount, lastYearCount, itemsChange,
                thisYearRate, lastYearRate, rateChange
            );
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting year-over-year comparison", e);
            return new YearComparison(LocalDate.now().getYear(), LocalDate.now().getYear() - 1,
                0, 0, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    // ==================== DASHBOARD AGGREGATIONS ====================
    
    /**
     * Get executive summary with highlights, alerts, and recommendations
     */
    public ExecutiveSummary getExecutiveSummary() {
        try {
            ExecutiveSummary summary = new ExecutiveSummary();
            
            // Highlights
            summary.totalItems = getTotalItemCount();
            summary.totalUsers = getTotalUserCount();
            summary.overallRecoveryRate = getRecoveryRate();
            summary.avgRecoveryTimeHours = getAverageRecoveryTime();
            summary.slaComplianceRate = getSLAComplianceRate();
            summary.crossEnterpriseMatchRate = getCrossEnterpriseMatchRate();
            
            // Network effect improvement
            NetworkEffectStats networkStats = getNetworkEffectMetrics();
            summary.networkEffectImprovement = networkStats.getImprovementPercentage();
            
            // Top enterprise
            List<EnterpriseStats> entStats = getEnterpriseStats();
            if (!entStats.isEmpty()) {
                summary.topEnterprise = entStats.get(0).enterpriseName;
                summary.topEnterpriseItems = entStats.get(0).totalItems;
            }
            
            // Alerts and recommendations
            summary.alerts = getAlerts();
            summary.recommendations = generateRecommendations();
            
            return summary;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating executive summary", e);
            return new ExecutiveSummary();
        }
    }
    
    /**
     * Get quick stats for dashboard cards
     */
    public QuickStats getQuickStats() {
        try {
            Map<ItemStatus, Long> statusCounts = getItemCountByStatus();
            
            long totalItems = getTotalItemCount();
            long openItems = statusCounts.getOrDefault(ItemStatus.OPEN, 0L);
            long pendingClaims = statusCounts.getOrDefault(ItemStatus.PENDING_CLAIM, 0L);
            long claimedItems = statusCounts.getOrDefault(ItemStatus.CLAIMED, 0L);
            
            double recoveryRate = getRecoveryRate();
            long activeUsers = getActiveUserCount(30); // Last 30 days
            
            // Count pending work requests
            Map<RequestStatus, Long> requestCounts = getRequestCountByStatus();
            long pendingRequests = requestCounts.getOrDefault(RequestStatus.PENDING, 0L) +
                                  requestCounts.getOrDefault(RequestStatus.IN_PROGRESS, 0L);
            
            // High value items
            HighValueStats hvStats = getHighValueItemStats();
            
            return new QuickStats(
                totalItems, openItems, pendingClaims, claimedItems,
                recoveryRate, activeUsers, pendingRequests,
                hvStats.totalCount, hvStats.totalValue
            );
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting quick stats", e);
            return new QuickStats(0, 0, 0, 0, 0.0, 0, 0, 0, 0.0);
        }
    }
    
    /**
     * Get system alerts (issues requiring attention)
     */
    public List<SystemAlert> getAlerts() {
        List<SystemAlert> alerts = new ArrayList<>();
        
        try {
            // Check SLA compliance
            double slaCompliance = getSLAComplianceRate();
            if (slaCompliance < 0.8) {
                alerts.add(new SystemAlert(
                    AlertLevel.WARNING,
                    "SLA Compliance Below Target",
                    String.format("Current SLA compliance is %.0f%%, below 80%% target", slaCompliance * 100)
                ));
            }
            
            // Check for overdue requests
            List<WorkRequest> requests = workRequestDAO.findAll();
            long overdueCount = requests.stream()
                .filter(WorkRequest::isPending)
                .filter(WorkRequest::isOverdue)
                .count();
            
            if (overdueCount > 0) {
                alerts.add(new SystemAlert(
                    AlertLevel.CRITICAL,
                    "Overdue Work Requests",
                    String.format("%d work requests are past their SLA deadline", overdueCount)
                ));
            }
            
            // Check for high-value items pending too long
            HighValueStats hvStats = getHighValueItemStats();
            if (hvStats.openCount > 5) {
                alerts.add(new SystemAlert(
                    AlertLevel.INFO,
                    "High-Value Items Pending",
                    String.format("%d high-value items still open, requiring attention", hvStats.openCount)
                ));
            }
            
            // Check recovery rate trend
            List<WeeklyTrend> trends = getWeeklyTrends(4);
            if (trends.size() >= 2) {
                double recentRate = trends.get(trends.size() - 1).recoveryRate;
                double previousRate = trends.get(trends.size() - 2).recoveryRate;
                if (recentRate < previousRate * 0.8) { // More than 20% drop
                    alerts.add(new SystemAlert(
                        AlertLevel.WARNING,
                        "Recovery Rate Declining",
                        String.format("Recovery rate dropped from %.0f%% to %.0f%% this week",
                            previousRate * 100, recentRate * 100)
                    ));
                }
            }
            
            // Sort by severity
            alerts.sort((a, b) -> Integer.compare(b.level.severity, a.level.severity));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating alerts", e);
        }
        
        return alerts;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Generate recommendations based on current data
     */
    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        try {
            // Based on recovery rate
            double recoveryRate = getRecoveryRate();
            if (recoveryRate < 0.3) {
                recommendations.add("Consider improving item matching algorithms to increase recovery rate");
            }
            
            // Based on cross-enterprise activity
            double crossEntRate = getCrossEnterpriseMatchRate();
            if (crossEntRate < 0.1) {
                recommendations.add("Encourage cross-enterprise collaboration to leverage network effects");
            }
            
            // Based on user activity
            long activeUsers = getActiveUserCount(30);
            long totalUsers = getTotalUserCount();
            if (totalUsers > 0 && (double) activeUsers / totalUsers < 0.2) {
                recommendations.add("User engagement is low - consider outreach campaigns");
            }
            
            // Based on high-value items
            HighValueStats hvStats = getHighValueItemStats();
            if (hvStats.recoveryRate < 0.5) {
                recommendations.add("Prioritize high-value item verification to improve valuable item recovery");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error generating recommendations", e);
        }
        
        return recommendations;
    }
    
    /**
     * Convert numeric score to ScoreLevel
     */
    private ScoreLevel getScoreLevelFromValue(double score) {
        if (score >= 85) return ScoreLevel.EXCELLENT;
        if (score >= 70) return ScoreLevel.GOOD;
        if (score >= 50) return ScoreLevel.FAIR;
        if (score >= 30) return ScoreLevel.LOW;
        return ScoreLevel.PROBATION;
    }
    
    /**
     * Clear analytics cache
     */
    public void clearCache() {
        analyticsCache.clear();
        cacheRefreshTime = 0;
    }
    
    // ====================================================================================
    // PART 6: CHART DATA METHODS
    // ====================================================================================
    
    // ==================== GENERIC CHART DATA METHODS ====================
    
    /**
     * Get pie chart data for a specific metric
     * @param metric The metric to chart (ITEM_STATUS, ITEM_TYPE, ITEM_CATEGORY, USER_ROLE, ENTERPRISE)
     */
    public List<ChartDataPoint> getPieChartData(ChartMetric metric) {
        switch (metric) {
            case ITEM_STATUS:
                return getItemStatusPieData();
            case ITEM_TYPE:
                return getItemTypePieData();
            case ITEM_CATEGORY:
                return getCategoryPieData();
            case USER_ROLE:
                return getUserRolePieData();
            case ENTERPRISE:
                return getEnterprisePieData();
            case REQUEST_TYPE:
                return getRequestTypePieData();
            case REQUEST_STATUS:
                return getRequestStatusPieData();
            case TRUST_SCORE_DISTRIBUTION:
                return getTrustScorePieData();
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get bar chart data for a metric grouped by a dimension
     * @param metric The metric to chart
     * @param groupBy The dimension to group by
     */
    public List<ChartDataPoint> getBarChartData(ChartMetric metric, GroupByDimension groupBy) {
        try {
            switch (metric) {
                case ITEM_COUNT:
                    return getItemCountBarData(groupBy);
                case RECOVERY_RATE:
                    return getRecoveryRateBarData(groupBy);
                case USER_COUNT:
                    return getUserCountBarData(groupBy);
                case REQUEST_COUNT:
                    return getRequestCountBarData(groupBy);
                default:
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating bar chart data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get line chart (time series) data for a metric over N days
     * @param metric The metric to chart
     * @param days Number of days to include
     */
    public List<TimeSeriesPoint> getLineChartData(ChartMetric metric, int days) {
        switch (metric) {
            case ITEMS_REPORTED:
                return getItemsReportedTrendData(days);
            case RECOVERIES:
                return getRecoveryTrendData(days);
            case REQUESTS_CREATED:
                return getRequestVolumeTrendData(days);
            case ACTIVE_USERS:
                return getActiveUsersTrendData(days);
            case RECOVERY_RATE:
                return getRecoveryRateTrendData(days);
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get stacked bar chart data for multiple metrics
     * @param metrics List of metrics to stack
     * @param groupBy The dimension to group by
     */
    public List<StackedDataPoint> getStackedBarData(List<ChartMetric> metrics, GroupByDimension groupBy) {
        try {
            if (groupBy == GroupByDimension.ENTERPRISE) {
                return getStackedByEnterprise(metrics);
            } else if (groupBy == GroupByDimension.TIME_WEEK) {
                return getStackedByWeek(metrics);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating stacked bar data", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== SPECIFIC PIE CHART DATA ====================
    
    /**
     * Get item status distribution for pie chart
     */
    public List<ChartDataPoint> getItemStatusPieData() {
        try {
            Map<ItemStatus, Long> statusCounts = getItemCountByStatus();
            List<ChartDataPoint> data = new ArrayList<>();
            
            // Color mapping for statuses
            Map<ItemStatus, java.awt.Color> colors = new HashMap<>();
            colors.put(ItemStatus.OPEN, new java.awt.Color(52, 152, 219));        // Blue
            colors.put(ItemStatus.PENDING_CLAIM, new java.awt.Color(241, 196, 15)); // Yellow
            colors.put(ItemStatus.CLAIMED, new java.awt.Color(39, 174, 96));       // Green
            colors.put(ItemStatus.VERIFIED, new java.awt.Color(155, 89, 182));     // Purple
            colors.put(ItemStatus.EXPIRED, new java.awt.Color(149, 165, 166));     // Gray
            
            for (Map.Entry<ItemStatus, Long> entry : statusCounts.entrySet()) {
                ItemStatus status = entry.getKey();
                data.add(new ChartDataPoint(
                    formatEnumLabel(status.name()),
                    entry.getValue().doubleValue(),
                    colors.getOrDefault(status, new java.awt.Color(100, 100, 100))
                ));
            }
            
            // Sort by value descending
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item status pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get item type distribution for pie chart
     */
    public List<ChartDataPoint> getItemTypePieData() {
        try {
            Map<ItemType, Long> typeCounts = getItemCountByType();
            List<ChartDataPoint> data = new ArrayList<>();
            
            Map<ItemType, java.awt.Color> colors = new HashMap<>();
            colors.put(ItemType.LOST, new java.awt.Color(231, 76, 60));   // Red
            colors.put(ItemType.FOUND, new java.awt.Color(46, 204, 113)); // Green
            
            for (Map.Entry<ItemType, Long> entry : typeCounts.entrySet()) {
                ItemType type = entry.getKey();
                data.add(new ChartDataPoint(
                    formatEnumLabel(type.name()),
                    entry.getValue().doubleValue(),
                    colors.getOrDefault(type, new java.awt.Color(100, 100, 100))
                ));
            }
            
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting item type pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get category distribution for pie chart
     */
    public List<ChartDataPoint> getCategoryPieData() {
        try {
            Map<ItemCategory, Long> catCounts = getItemCountByCategory();
            List<ChartDataPoint> data = new ArrayList<>();
            
            // Generate distinct colors for each category
            java.awt.Color[] palette = generateColorPalette(catCounts.size());
            int colorIndex = 0;
            
            for (Map.Entry<ItemCategory, Long> entry : catCounts.entrySet()) {
                data.add(new ChartDataPoint(
                    formatEnumLabel(entry.getKey().name()),
                    entry.getValue().doubleValue(),
                    palette[colorIndex++ % palette.length]
                ));
            }
            
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting category pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get user role distribution for pie chart
     */
    public List<ChartDataPoint> getUserRolePieData() {
        try {
            Map<UserRole, Long> roleCounts = getUserCountByRole();
            List<ChartDataPoint> data = new ArrayList<>();
            
            java.awt.Color[] palette = generateColorPalette(roleCounts.size());
            int colorIndex = 0;
            
            for (Map.Entry<UserRole, Long> entry : roleCounts.entrySet()) {
                data.add(new ChartDataPoint(
                    formatEnumLabel(entry.getKey().name()),
                    entry.getValue().doubleValue(),
                    palette[colorIndex++ % palette.length]
                ));
            }
            
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting user role pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get enterprise distribution for pie chart
     */
    public List<ChartDataPoint> getEnterprisePieData() {
        try {
            Map<String, Long> entCounts = getItemsByEnterprise();
            List<ChartDataPoint> data = new ArrayList<>();
            
            // Enterprise colors
            Map<String, java.awt.Color> entColors = new HashMap<>();
            entColors.put("Northeastern University", new java.awt.Color(204, 0, 0));      // NEU Red
            entColors.put("Boston University", new java.awt.Color(204, 0, 0));            // BU Red  
            entColors.put("MBTA Transit", new java.awt.Color(0, 102, 204));               // MBTA Blue
            entColors.put("Logan Airport", new java.awt.Color(0, 51, 102));               // Navy
            entColors.put("Boston Police", new java.awt.Color(0, 51, 102));               // Navy
            
            java.awt.Color[] fallbackPalette = generateColorPalette(entCounts.size());
            int colorIndex = 0;
            
            for (Map.Entry<String, Long> entry : entCounts.entrySet()) {
                java.awt.Color color = entColors.getOrDefault(entry.getKey(), 
                    fallbackPalette[colorIndex++ % fallbackPalette.length]);
                data.add(new ChartDataPoint(entry.getKey(), entry.getValue().doubleValue(), color));
            }
            
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting enterprise pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get request type distribution for pie chart
     */
    public List<ChartDataPoint> getRequestTypePieData() {
        try {
            Map<RequestType, Long> typeCounts = getRequestCountByType();
            List<ChartDataPoint> data = new ArrayList<>();
            
            java.awt.Color[] palette = generateColorPalette(typeCounts.size());
            int colorIndex = 0;
            
            for (Map.Entry<RequestType, Long> entry : typeCounts.entrySet()) {
                data.add(new ChartDataPoint(
                    formatEnumLabel(entry.getKey().name()),
                    entry.getValue().doubleValue(),
                    palette[colorIndex++ % palette.length]
                ));
            }
            
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request type pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get request status distribution for pie chart
     */
    public List<ChartDataPoint> getRequestStatusPieData() {
        try {
            Map<RequestStatus, Long> statusCounts = getRequestCountByStatus();
            List<ChartDataPoint> data = new ArrayList<>();
            
            Map<RequestStatus, java.awt.Color> colors = new HashMap<>();
            colors.put(RequestStatus.PENDING, new java.awt.Color(241, 196, 15));     // Yellow
            colors.put(RequestStatus.IN_PROGRESS, new java.awt.Color(52, 152, 219)); // Blue
            colors.put(RequestStatus.APPROVED, new java.awt.Color(46, 204, 113));    // Green
            colors.put(RequestStatus.COMPLETED, new java.awt.Color(39, 174, 96));    // Dark Green
            colors.put(RequestStatus.REJECTED, new java.awt.Color(231, 76, 60));     // Red
            colors.put(RequestStatus.CANCELLED, new java.awt.Color(149, 165, 166)); // Gray
            
            for (Map.Entry<RequestStatus, Long> entry : statusCounts.entrySet()) {
                RequestStatus status = entry.getKey();
                data.add(new ChartDataPoint(
                    formatEnumLabel(status.name()),
                    entry.getValue().doubleValue(),
                    colors.getOrDefault(status, new java.awt.Color(100, 100, 100))
                ));
            }
            
            data.sort((a, b) -> Double.compare(b.value, a.value));
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request status pie data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get trust score distribution for pie chart
     */
    public List<ChartDataPoint> getTrustScorePieData() {
        try {
            Map<ScoreLevel, Long> distribution = getTrustScoreDistribution();
            List<ChartDataPoint> data = new ArrayList<>();
            
            Map<ScoreLevel, java.awt.Color> colors = new HashMap<>();
            colors.put(ScoreLevel.EXCELLENT, new java.awt.Color(39, 174, 96));    // Green
            colors.put(ScoreLevel.GOOD, new java.awt.Color(46, 204, 113));        // Light Green
            colors.put(ScoreLevel.FAIR, new java.awt.Color(241, 196, 15));        // Yellow
            colors.put(ScoreLevel.LOW, new java.awt.Color(230, 126, 34));         // Orange
            colors.put(ScoreLevel.PROBATION, new java.awt.Color(231, 76, 60));    // Red
            
            for (Map.Entry<ScoreLevel, Long> entry : distribution.entrySet()) {
                ScoreLevel level = entry.getKey();
                data.add(new ChartDataPoint(
                    formatEnumLabel(level.name()),
                    entry.getValue().doubleValue(),
                    colors.getOrDefault(level, new java.awt.Color(100, 100, 100))
                ));
            }
            
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting trust score pie data", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== SPECIFIC BAR CHART DATA ====================
    
    /**
     * Get category distribution for bar chart (same as pie but formatted for bar)
     */
    public List<ChartDataPoint> getCategoryBarData() {
        return getCategoryPieData();
    }
    
    /**
     * Get enterprise comparison data for bar chart
     */
    public List<ChartDataPoint> getEnterpriseComparisonData() {
        try {
            List<EnterpriseStats> stats = getEnterpriseStats();
            List<ChartDataPoint> data = new ArrayList<>();
            
            java.awt.Color[] palette = generateColorPalette(stats.size());
            int colorIndex = 0;
            
            for (EnterpriseStats stat : stats) {
                data.add(new ChartDataPoint(
                    stat.enterpriseName,
                    (double) stat.totalItems,
                    palette[colorIndex++ % palette.length]
                ));
            }
            
            return data;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting enterprise comparison data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get item count bar data by grouping dimension
     */
    private List<ChartDataPoint> getItemCountBarData(GroupByDimension groupBy) {
        switch (groupBy) {
            case CATEGORY:
                return getCategoryBarData();
            case ENTERPRISE:
                return getEnterpriseComparisonData();
            case STATUS:
                return getItemStatusPieData();
            case TYPE:
                return getItemTypePieData();
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get recovery rate bar data by grouping dimension
     */
    private List<ChartDataPoint> getRecoveryRateBarData(GroupByDimension groupBy) {
        try {
            if (groupBy == GroupByDimension.ENTERPRISE) {
                Map<String, Double> rates = getRecoveryRateByEnterprise();
                List<ChartDataPoint> data = new ArrayList<>();
                
                java.awt.Color[] palette = generateColorPalette(rates.size());
                int colorIndex = 0;
                
                for (Map.Entry<String, Double> entry : rates.entrySet()) {
                    data.add(new ChartDataPoint(
                        entry.getKey(),
                        entry.getValue() * 100, // Convert to percentage
                        palette[colorIndex++ % palette.length]
                    ));
                }
                
                data.sort((a, b) -> Double.compare(b.value, a.value));
                return data;
            }
            return new ArrayList<>();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recovery rate bar data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get user count bar data by grouping dimension
     */
    private List<ChartDataPoint> getUserCountBarData(GroupByDimension groupBy) {
        try {
            if (groupBy == GroupByDimension.ENTERPRISE) {
                Map<String, Long> counts = getUserCountByEnterprise();
                List<ChartDataPoint> data = new ArrayList<>();
                
                java.awt.Color[] palette = generateColorPalette(counts.size());
                int colorIndex = 0;
                
                for (Map.Entry<String, Long> entry : counts.entrySet()) {
                    data.add(new ChartDataPoint(
                        entry.getKey(),
                        entry.getValue().doubleValue(),
                        palette[colorIndex++ % palette.length]
                    ));
                }
                
                data.sort((a, b) -> Double.compare(b.value, a.value));
                return data;
                
            } else if (groupBy == GroupByDimension.ROLE) {
                return getUserRolePieData();
            }
            return new ArrayList<>();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting user count bar data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get request count bar data by grouping dimension
     */
    private List<ChartDataPoint> getRequestCountBarData(GroupByDimension groupBy) {
        try {
            if (groupBy == GroupByDimension.TYPE) {
                return getRequestTypePieData();
            } else if (groupBy == GroupByDimension.STATUS) {
                return getRequestStatusPieData();
            } else if (groupBy == GroupByDimension.ENTERPRISE) {
                Map<String, Long> pending = getPendingRequestsByEnterprise();
                List<ChartDataPoint> data = new ArrayList<>();
                
                java.awt.Color[] palette = generateColorPalette(pending.size());
                int colorIndex = 0;
                
                for (Map.Entry<String, Long> entry : pending.entrySet()) {
                    data.add(new ChartDataPoint(
                        entry.getKey(),
                        entry.getValue().doubleValue(),
                        palette[colorIndex++ % palette.length]
                    ));
                }
                
                return data;
            }
            return new ArrayList<>();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request count bar data", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== TIME SERIES DATA ====================
    
    /**
     * Get recovery trend data (items recovered per day)
     */
    public List<TimeSeriesPoint> getRecoveryTrendData(int days) {
        try {
            List<Item> items = itemDAO.findAll();
            Date cutoff = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
            
            // Group recovered items by date
            Map<LocalDate, Long> dailyCounts = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED)
                .filter(i -> i.getResolvedDate() != null && i.getResolvedDate().after(cutoff))
                .collect(Collectors.groupingBy(
                    i -> i.getResolvedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    Collectors.counting()
                ));
            
            // Fill in missing days
            List<TimeSeriesPoint> result = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                long count = dailyCounts.getOrDefault(date, 0L);
                result.add(new TimeSeriesPoint(date, count));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recovery trend data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get items reported trend data
     */
    public List<TimeSeriesPoint> getItemsReportedTrendData(int days) {
        try {
            List<DailyCount> dailyCounts = getItemsReportedOverTime(days);
            return dailyCounts.stream()
                .map(dc -> new TimeSeriesPoint(dc.date, dc.count))
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting items reported trend data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get request volume trend data
     */
    public List<TimeSeriesPoint> getRequestVolumeTrendData(int days) {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            LocalDate cutoff = LocalDate.now().minusDays(days);
            
            Map<LocalDate, Long> dailyCounts = requests.stream()
                .filter(r -> r.getCreatedAt() != null)
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(cutoff))
                .collect(Collectors.groupingBy(
                    r -> r.getCreatedAt().toLocalDate(),
                    Collectors.counting()
                ));
            
            List<TimeSeriesPoint> result = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                long count = dailyCounts.getOrDefault(date, 0L);
                result.add(new TimeSeriesPoint(date, count));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting request volume trend data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get active users trend data
     */
    public List<TimeSeriesPoint> getActiveUsersTrendData(int days) {
        try {
            List<Item> items = itemDAO.findAll();
            LocalDate today = LocalDate.now();
            
            List<TimeSeriesPoint> result = new ArrayList<>();
            
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                final LocalDate targetDate = date;
                
                // Count unique users who reported items on this date
                long activeUsers = items.stream()
                    .filter(item -> item.getReportedDate() != null)
                    .filter(item -> {
                        LocalDate itemDate = item.getReportedDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        return itemDate.equals(targetDate);
                    })
                    .filter(item -> item.getReportedBy() != null)
                    .map(item -> item.getReportedBy().getEmail())
                    .distinct()
                    .count();
                
                result.add(new TimeSeriesPoint(date, activeUsers));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting active users trend data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recovery rate trend data (percentage per week)
     */
    public List<TimeSeriesPoint> getRecoveryRateTrendData(int days) {
        try {
            List<WeeklyTrend> weeklyTrends = getWeeklyTrends(days / 7 + 1);
            
            return weeklyTrends.stream()
                .map(wt -> new TimeSeriesPoint(wt.weekEnd, (long) (wt.recoveryRate * 100)))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recovery rate trend data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Alias for compatibility - get user growth data
     */
    public List<TimeSeriesPoint> getUserGrowthData(int days) {
        return getActiveUsersTrendData(days);
    }
    
    /**
     * Alias for compatibility - get request volume data
     */
    public List<TimeSeriesPoint> getRequestVolumeData(int days) {
        return getRequestVolumeTrendData(days);
    }
    
    // ==================== STACKED BAR DATA ====================
    
    /**
     * Get stacked bar data by enterprise
     */
    private List<StackedDataPoint> getStackedByEnterprise(List<ChartMetric> metrics) {
        try {
            List<EnterpriseStats> stats = getEnterpriseStats();
            List<StackedDataPoint> result = new ArrayList<>();
            
            for (EnterpriseStats entStat : stats) {
                Map<String, Double> values = new LinkedHashMap<>();
                
                for (ChartMetric metric : metrics) {
                    switch (metric) {
                        case LOST_ITEMS:
                            values.put("Lost", (double) entStat.lostItems);
                            break;
                        case FOUND_ITEMS:
                            values.put("Found", (double) entStat.foundItems);
                            break;
                        case CLAIMED_ITEMS:
                            values.put("Claimed", (double) entStat.claimedItems);
                            break;
                        case OPEN_ITEMS:
                            values.put("Open", (double) entStat.openItems);
                            break;
                        case RECOVERY_RATE:
                            values.put("Recovery %", entStat.recoveryRate * 100);
                            break;
                        default:
                            break;
                    }
                }
                
                result.add(new StackedDataPoint(entStat.enterpriseName, values));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting stacked by enterprise data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get stacked bar data by week
     */
    private List<StackedDataPoint> getStackedByWeek(List<ChartMetric> metrics) {
        try {
            List<WeeklyTrend> trends = getWeeklyTrends(8); // Last 8 weeks
            List<StackedDataPoint> result = new ArrayList<>();
            
            for (WeeklyTrend trend : trends) {
                Map<String, Double> values = new LinkedHashMap<>();
                
                for (ChartMetric metric : metrics) {
                    switch (metric) {
                        case LOST_ITEMS:
                            values.put("Lost", (double) trend.lostItems);
                            break;
                        case FOUND_ITEMS:
                            values.put("Found", (double) trend.foundItems);
                            break;
                        case RECOVERIES:
                            values.put("Recovered", (double) trend.recovered);
                            break;
                        case ITEMS_REPORTED:
                            values.put("Total", (double) trend.itemsReported);
                            break;
                        default:
                            break;
                    }
                }
                
                result.add(new StackedDataPoint(trend.getWeekLabel(), values));
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting stacked by week data", e);
            return new ArrayList<>();
        }
    }
    
    // ==================== COMPARISON ANALYTICS ====================
    
    /**
     * Compare multiple enterprises
     * @param enterpriseIds List of enterprise IDs to compare
     */
    public ComparisonResult compareEnterprises(List<String> enterpriseIds) {
        try {
            List<EnterpriseStats> allStats = getEnterpriseStats();
            
            // Filter to requested enterprises
            List<EnterpriseStats> selectedStats = allStats.stream()
                .filter(s -> enterpriseIds.contains(s.enterpriseId))
                .collect(Collectors.toList());
            
            if (selectedStats.isEmpty()) {
                return new ComparisonResult();
            }
            
            ComparisonResult result = new ComparisonResult();
            
            // Calculate metrics for comparison
            for (EnterpriseStats stat : selectedStats) {
                result.enterpriseMetrics.put(stat.enterpriseName, new HashMap<>());
                result.enterpriseMetrics.get(stat.enterpriseName).put("Total Items", (double) stat.totalItems);
                result.enterpriseMetrics.get(stat.enterpriseName).put("Recovery Rate", stat.recoveryRate * 100);
                result.enterpriseMetrics.get(stat.enterpriseName).put("Users", (double) stat.userCount);
                result.enterpriseMetrics.get(stat.enterpriseName).put("Open Items", (double) stat.openItems);
            }
            
            // Determine winners for each metric
            result.metricWinners.put("Total Items", selectedStats.stream()
                .max(Comparator.comparingLong(s -> s.totalItems))
                .map(s -> s.enterpriseName).orElse("N/A"));
            
            result.metricWinners.put("Recovery Rate", selectedStats.stream()
                .max(Comparator.comparingDouble(s -> s.recoveryRate))
                .map(s -> s.enterpriseName).orElse("N/A"));
            
            result.metricWinners.put("Users", selectedStats.stream()
                .max(Comparator.comparingLong(s -> s.userCount))
                .map(s -> s.enterpriseName).orElse("N/A"));
            
            // Calculate averages for benchmarking
            result.averages.put("Total Items", selectedStats.stream()
                .mapToLong(s -> s.totalItems).average().orElse(0));
            result.averages.put("Recovery Rate", selectedStats.stream()
                .mapToDouble(s -> s.recoveryRate * 100).average().orElse(0));
            result.averages.put("Users", selectedStats.stream()
                .mapToLong(s -> s.userCount).average().orElse(0));
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error comparing enterprises", e);
            return new ComparisonResult();
        }
    }
    
    /**
     * Compare two time periods
     * @param period1Start Start of first period
     * @param period1End End of first period
     * @param period2Start Start of second period
     * @param period2End End of second period
     */
    public PeriodComparison comparePeriods(LocalDate period1Start, LocalDate period1End,
                                           LocalDate period2Start, LocalDate period2End) {
        try {
            List<Item> items = itemDAO.findAll();
            
            // Filter items for each period
            List<Item> period1Items = filterItemsByDateRange(items, period1Start, period1End);
            List<Item> period2Items = filterItemsByDateRange(items, period2Start, period2End);
            
            PeriodComparison comparison = new PeriodComparison();
            comparison.period1Label = period1Start + " to " + period1End;
            comparison.period2Label = period2Start + " to " + period2End;
            
            // Calculate metrics for each period
            comparison.period1Items = period1Items.size();
            comparison.period2Items = period2Items.size();
            
            comparison.period1Recovered = period1Items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
            comparison.period2Recovered = period2Items.stream()
                .filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
            
            comparison.period1RecoveryRate = comparison.period1Items > 0 ?
                (double) comparison.period1Recovered / comparison.period1Items : 0;
            comparison.period2RecoveryRate = comparison.period2Items > 0 ?
                (double) comparison.period2Recovered / comparison.period2Items : 0;
            
            // Calculate changes
            comparison.itemsChange = comparison.period1Items > 0 ?
                ((double) comparison.period2Items - comparison.period1Items) / comparison.period1Items : 0;
            comparison.recoveryRateChange = comparison.period1RecoveryRate > 0 ?
                (comparison.period2RecoveryRate - comparison.period1RecoveryRate) / comparison.period1RecoveryRate : 0;
            
            return comparison;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error comparing periods", e);
            return new PeriodComparison();
        }
    }
    
    /**
     * Benchmark an enterprise against network average
     * @param enterpriseId The enterprise to benchmark
     */
    public BenchmarkResult benchmarkAgainstNetwork(String enterpriseId) {
        try {
            List<EnterpriseStats> allStats = getEnterpriseStats();
            
            // Find the target enterprise
            EnterpriseStats targetStat = allStats.stream()
                .filter(s -> s.enterpriseId.equals(enterpriseId))
                .findFirst()
                .orElse(null);
            
            if (targetStat == null) {
                return new BenchmarkResult();
            }
            
            BenchmarkResult result = new BenchmarkResult();
            result.enterpriseName = targetStat.enterpriseName;
            
            // Calculate network averages (excluding target)
            List<EnterpriseStats> otherStats = allStats.stream()
                .filter(s -> !s.enterpriseId.equals(enterpriseId))
                .collect(Collectors.toList());
            
            if (otherStats.isEmpty()) {
                // Only one enterprise, can't benchmark
                result.networkAvgItems = targetStat.totalItems;
                result.networkAvgRecoveryRate = targetStat.recoveryRate;
                result.networkAvgUsers = targetStat.userCount;
            } else {
                result.networkAvgItems = (long) otherStats.stream()
                    .mapToLong(s -> s.totalItems).average().orElse(0);
                result.networkAvgRecoveryRate = otherStats.stream()
                    .mapToDouble(s -> s.recoveryRate).average().orElse(0);
                result.networkAvgUsers = (long) otherStats.stream()
                    .mapToLong(s -> s.userCount).average().orElse(0);
            }
            
            // Set enterprise values
            result.enterpriseItems = targetStat.totalItems;
            result.enterpriseRecoveryRate = targetStat.recoveryRate;
            result.enterpriseUsers = targetStat.userCount;
            
            // Calculate differentials
            result.itemsDifferential = result.networkAvgItems > 0 ?
                ((double) result.enterpriseItems - result.networkAvgItems) / result.networkAvgItems : 0;
            result.recoveryRateDifferential = result.networkAvgRecoveryRate > 0 ?
                (result.enterpriseRecoveryRate - result.networkAvgRecoveryRate) / result.networkAvgRecoveryRate : 0;
            result.usersDifferential = result.networkAvgUsers > 0 ?
                ((double) result.enterpriseUsers - result.networkAvgUsers) / result.networkAvgUsers : 0;
            
            // Determine performance level
            double avgDiff = (result.itemsDifferential + result.recoveryRateDifferential + result.usersDifferential) / 3;
            if (avgDiff > 0.2) {
                result.performanceLevel = "Excellent";
            } else if (avgDiff > 0) {
                result.performanceLevel = "Above Average";
            } else if (avgDiff > -0.2) {
                result.performanceLevel = "Average";
            } else {
                result.performanceLevel = "Below Average";
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error benchmarking against network", e);
            return new BenchmarkResult();
        }
    }
    
    // ==================== CHART HELPER METHODS ====================
    
    /**
     * Filter items by date range
     */
    private List<Item> filterItemsByDateRange(List<Item> items, LocalDate start, LocalDate end) {
        return items.stream()
            .filter(i -> i.getReportedDate() != null)
            .filter(i -> {
                LocalDate date = i.getReportedDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
                return !date.isBefore(start) && !date.isAfter(end);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Format enum name for display (SOME_VALUE -> Some Value)
     */
    private String formatEnumLabel(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        
        String[] words = enumName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Generate a color palette with N distinct colors
     */
    private java.awt.Color[] generateColorPalette(int size) {
        java.awt.Color[] baseColors = new java.awt.Color[] {
            new java.awt.Color(52, 152, 219),   // Blue
            new java.awt.Color(46, 204, 113),   // Green
            new java.awt.Color(231, 76, 60),    // Red
            new java.awt.Color(241, 196, 15),   // Yellow
            new java.awt.Color(155, 89, 182),   // Purple
            new java.awt.Color(230, 126, 34),   // Orange
            new java.awt.Color(26, 188, 156),   // Teal
            new java.awt.Color(52, 73, 94),     // Dark Blue
            new java.awt.Color(149, 165, 166),  // Gray
            new java.awt.Color(192, 57, 43),    // Dark Red
            new java.awt.Color(39, 174, 96),    // Dark Green
            new java.awt.Color(41, 128, 185),   // Navy Blue
            new java.awt.Color(142, 68, 173),   // Dark Purple
            new java.awt.Color(211, 84, 0),     // Burnt Orange
            new java.awt.Color(22, 160, 133)    // Dark Teal
        };
        
        if (size <= baseColors.length) {
            return Arrays.copyOf(baseColors, size);
        }
        
        // Generate additional colors if needed
        java.awt.Color[] extended = new java.awt.Color[size];
        System.arraycopy(baseColors, 0, extended, 0, baseColors.length);
        
        for (int i = baseColors.length; i < size; i++) {
            // Generate colors by shifting hue
            float hue = (float) i / size;
            extended[i] = java.awt.Color.getHSBColor(hue, 0.7f, 0.8f);
        }
        
        return extended;
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Daily count for time series
     */
    public static class DailyCount {
        public LocalDate date;
        public long count;
        
        public DailyCount(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }
        
        public Object[] toTableRow() {
            return new Object[] { date.toString(), count };
        }
    }
    
    /**
     * Daily statistics
     */
    public static class DailyStats {
        public LocalDate date;
        public long itemsReported;
        public long requestsCreated;
        public long recoveries;
        
        public DailyStats(LocalDate date, long itemsReported, long requestsCreated, long recoveries) {
            this.date = date;
            this.itemsReported = itemsReported;
            this.requestsCreated = requestsCreated;
            this.recoveries = recoveries;
        }
        
        public Object[] toTableRow() {
            return new Object[] { date.toString(), itemsReported, requestsCreated, recoveries };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "Date", "Items Reported", "Requests", "Recoveries" };
        }
    }
    
    /**
     * Weekly trend data
     */
    public static class WeeklyTrend {
        public LocalDate weekStart;
        public LocalDate weekEnd;
        public long itemsReported;
        public long lostItems;
        public long foundItems;
        public long recovered;
        public double recoveryRate;
        
        public WeeklyTrend(LocalDate weekStart, LocalDate weekEnd, long itemsReported,
                          long lostItems, long foundItems, long recovered, double recoveryRate) {
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.itemsReported = itemsReported;
            this.lostItems = lostItems;
            this.foundItems = foundItems;
            this.recovered = recovered;
            this.recoveryRate = recoveryRate;
        }
        
        public String getWeekLabel() {
            return weekStart.toString() + " - " + weekEnd.toString();
        }
        
        public Object[] toTableRow() {
            return new Object[] { 
                getWeekLabel(), itemsReported, lostItems, foundItems, 
                recovered, String.format("%.0f%%", recoveryRate * 100)
            };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "Week", "Total", "Lost", "Found", "Recovered", "Rate" };
        }
    }
    
    /**
     * Monthly trend data
     */
    public static class MonthlyTrend {
        public YearMonth month;
        public long itemsReported;
        public long recovered;
        public double recoveryRate;
        public double totalValue;
        
        public MonthlyTrend(YearMonth month, long itemsReported, long recovered,
                           double recoveryRate, double totalValue) {
            this.month = month;
            this.itemsReported = itemsReported;
            this.recovered = recovered;
            this.recoveryRate = recoveryRate;
            this.totalValue = totalValue;
        }
        
        public String getMonthLabel() {
            return month.toString();
        }
        
        public Object[] toTableRow() {
            return new Object[] {
                getMonthLabel(), itemsReported, recovered,
                String.format("%.0f%%", recoveryRate * 100),
                String.format("$%.2f", totalValue)
            };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "Month", "Items", "Recovered", "Rate", "Value" };
        }
    }
    
    /**
     * Year-over-year comparison
     */
    public static class YearComparison {
        public int currentYear;
        public int previousYear;
        public long currentYearItems;
        public long previousYearItems;
        public double itemsChangePercent;
        public double currentYearRecoveryRate;
        public double previousYearRecoveryRate;
        public double recoveryRateChangePercent;
        
        public YearComparison(int currentYear, int previousYear,
                              long currentYearItems, long previousYearItems, double itemsChangePercent,
                              double currentYearRecoveryRate, double previousYearRecoveryRate,
                              double recoveryRateChangePercent) {
            this.currentYear = currentYear;
            this.previousYear = previousYear;
            this.currentYearItems = currentYearItems;
            this.previousYearItems = previousYearItems;
            this.itemsChangePercent = itemsChangePercent;
            this.currentYearRecoveryRate = currentYearRecoveryRate;
            this.previousYearRecoveryRate = previousYearRecoveryRate;
            this.recoveryRateChangePercent = recoveryRateChangePercent;
        }
        
        public String getSummary() {
            String itemsTrend = itemsChangePercent >= 0 ? "↑" : "↓";
            String rateTrend = recoveryRateChangePercent >= 0 ? "↑" : "↓";
            
            return String.format(
                "Year-over-Year: Items %s%.0f%% (%d vs %d), Recovery Rate %s%.0f%% (%.0f%% vs %.0f%%)",
                itemsTrend, Math.abs(itemsChangePercent * 100),
                currentYearItems, previousYearItems,
                rateTrend, Math.abs(recoveryRateChangePercent * 100),
                currentYearRecoveryRate * 100, previousYearRecoveryRate * 100
            );
        }
    }
    
    /**
     * Enterprise statistics
     */
    public static class EnterpriseStats {
        public String enterpriseId;
        public String enterpriseName;
        public long totalItems;
        public long lostItems;
        public long foundItems;
        public long claimedItems;
        public long openItems;
        public double recoveryRate;
        public long userCount;
        
        public EnterpriseStats(String enterpriseId, String enterpriseName,
                               long totalItems, long lostItems, long foundItems,
                               long claimedItems, long openItems, double recoveryRate,
                               long userCount) {
            this.enterpriseId = enterpriseId;
            this.enterpriseName = enterpriseName;
            this.totalItems = totalItems;
            this.lostItems = lostItems;
            this.foundItems = foundItems;
            this.claimedItems = claimedItems;
            this.openItems = openItems;
            this.recoveryRate = recoveryRate;
            this.userCount = userCount;
        }
        
        public Object[] toTableRow() {
            return new Object[] {
                enterpriseName, totalItems, lostItems, foundItems, claimedItems,
                openItems, String.format("%.0f%%", recoveryRate * 100), userCount
            };
        }
        
        public static String[] getTableColumns() {
            return new String[] {
                "Enterprise", "Total", "Lost", "Found", "Claimed", "Open", "Recovery", "Users"
            };
        }
    }
    
    /**
     * User contribution data
     */
    public static class UserContribution {
        public String userId;
        public String userName;
        public int itemsReported;
        public int itemsReturned;
        public double trustScore;
        
        public UserContribution(String userId, String userName, 
                                int itemsReported, int itemsReturned, double trustScore) {
            this.userId = userId;
            this.userName = userName;
            this.itemsReported = itemsReported;
            this.itemsReturned = itemsReturned;
            this.trustScore = trustScore;
        }
        
        public Object[] toTableRow() {
            return new Object[] {
                userName, itemsReported, itemsReturned, String.format("%.0f", trustScore)
            };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "User", "Reported", "Returned", "Trust Score" };
        }
    }
    
    /**
     * High-value item statistics
     */
    public static class HighValueStats {
        public long totalCount;
        public long claimedCount;
        public long pendingCount;
        public long openCount;
        public double totalValue;
        public double recoveryRate;
        
        public HighValueStats(long totalCount, long claimedCount, long pendingCount,
                              long openCount, double totalValue, double recoveryRate) {
            this.totalCount = totalCount;
            this.claimedCount = claimedCount;
            this.pendingCount = pendingCount;
            this.openCount = openCount;
            this.totalValue = totalValue;
            this.recoveryRate = recoveryRate;
        }
        
        public String getSummary() {
            return String.format(
                "High-Value Items: %d total ($%.2f), %d claimed (%.0f%% recovery), %d pending, %d open",
                totalCount, totalValue, claimedCount, recoveryRate * 100, pendingCount, openCount
            );
        }
    }
    
    /**
     * Network effect statistics (wrapper for EnterpriseItemService.NetworkEffectMetrics)
     */
    public static class NetworkEffectStats {
        public double overallRecoveryRate;
        public double singleEnterpriseRate;
        public double twoEnterpriseRate;
        public double threeEnterpriseRate;
        public double fourPlusEnterpriseRate;
        
        public NetworkEffectStats() {
            // Default constructor
        }
        
        public NetworkEffectStats(double overall, double single, double two, 
                                  double three, double fourPlus) {
            this.overallRecoveryRate = overall;
            this.singleEnterpriseRate = single;
            this.twoEnterpriseRate = two;
            this.threeEnterpriseRate = three;
            this.fourPlusEnterpriseRate = fourPlus;
        }
        
        public double getImprovementPercentage() {
            if (singleEnterpriseRate <= 0) return 0.0;
            return ((fourPlusEnterpriseRate - singleEnterpriseRate) / singleEnterpriseRate) * 100;
        }
        
        public String getSummary() {
            return String.format(
                "Network Effect: Single enterprise %.0f%% → Multi-enterprise %.0f%% (%.0f%% improvement)",
                singleEnterpriseRate * 100, fourPlusEnterpriseRate * 100, getImprovementPercentage()
            );
        }
    }
    
    /**
     * Executive summary
     */
    public static class ExecutiveSummary {
        public long totalItems;
        public long totalUsers;
        public double overallRecoveryRate;
        public double avgRecoveryTimeHours;
        public double slaComplianceRate;
        public double crossEnterpriseMatchRate;
        public double networkEffectImprovement;
        public String topEnterprise;
        public long topEnterpriseItems;
        public List<SystemAlert> alerts = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
        
        public String getHighlightsSummary() {
            return String.format(
                "System Overview:\n" +
                "• %d items tracked across %d users\n" +
                "• %.0f%% overall recovery rate (avg %.1f hours)\n" +
                "• %.0f%% SLA compliance\n" +
                "• %.0f%% cross-enterprise match rate\n" +
                "• Network effect shows %.0f%% improvement\n" +
                "• Top enterprise: %s (%d items)",
                totalItems, totalUsers,
                overallRecoveryRate * 100, avgRecoveryTimeHours,
                slaComplianceRate * 100,
                crossEnterpriseMatchRate * 100,
                networkEffectImprovement,
                topEnterprise != null ? topEnterprise : "N/A",
                topEnterpriseItems
            );
        }
    }
    
    /**
     * Quick stats for dashboard
     */
    public static class QuickStats {
        public long totalItems;
        public long openItems;
        public long pendingClaims;
        public long claimedItems;
        public double recoveryRate;
        public long activeUsers;
        public long pendingRequests;
        public long highValueItemCount;
        public double highValueTotalValue;
        
        public QuickStats(long totalItems, long openItems, long pendingClaims,
                          long claimedItems, double recoveryRate, long activeUsers,
                          long pendingRequests, long highValueItemCount, double highValueTotalValue) {
            this.totalItems = totalItems;
            this.openItems = openItems;
            this.pendingClaims = pendingClaims;
            this.claimedItems = claimedItems;
            this.recoveryRate = recoveryRate;
            this.activeUsers = activeUsers;
            this.pendingRequests = pendingRequests;
            this.highValueItemCount = highValueItemCount;
            this.highValueTotalValue = highValueTotalValue;
        }
    }
    
    /**
     * System alert
     */
    public static class SystemAlert {
        public AlertLevel level;
        public String title;
        public String message;
        public LocalDateTime timestamp;
        
        public SystemAlert(AlertLevel level, String title, String message) {
            this.level = level;
            this.title = title;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getIcon() {
            return level.icon;
        }
        
        public java.awt.Color getColor() {
            return level.color;
        }
    }
    
    /**
     * Alert severity levels
     */
    public enum AlertLevel {
        CRITICAL(3, "🔴", new java.awt.Color(220, 53, 69)),
        WARNING(2, "🟠", new java.awt.Color(255, 193, 7)),
        INFO(1, "🔵", new java.awt.Color(23, 162, 184));
        
        public final int severity;
        public final String icon;
        public final java.awt.Color color;
        
        AlertLevel(int severity, String icon, java.awt.Color color) {
            this.severity = severity;
            this.icon = icon;
            this.color = color;
        }
    }
    
    // ====================================================================================
    // PART 6: CHART-SPECIFIC INNER CLASSES AND ENUMS
    // ====================================================================================
    
    /**
     * Metrics available for charting
     */
    public enum ChartMetric {
        // Item metrics
        ITEM_STATUS,
        ITEM_TYPE,
        ITEM_CATEGORY,
        ITEM_COUNT,
        ITEMS_REPORTED,
        LOST_ITEMS,
        FOUND_ITEMS,
        CLAIMED_ITEMS,
        OPEN_ITEMS,
        
        // Recovery metrics
        RECOVERY_RATE,
        RECOVERIES,
        
        // User metrics
        USER_ROLE,
        USER_COUNT,
        ACTIVE_USERS,
        TRUST_SCORE_DISTRIBUTION,
        
        // Request metrics
        REQUEST_TYPE,
        REQUEST_STATUS,
        REQUEST_COUNT,
        REQUESTS_CREATED,
        
        // Enterprise metrics
        ENTERPRISE
    }
    
    /**
     * Dimensions for grouping data
     */
    public enum GroupByDimension {
        CATEGORY,
        ENTERPRISE,
        STATUS,
        TYPE,
        ROLE,
        TIME_DAY,
        TIME_WEEK,
        TIME_MONTH
    }
    
    /**
     * Data point for pie and bar charts
     */
    public static class ChartDataPoint {
        public String label;
        public double value;
        public java.awt.Color color;
        public String tooltip;
        
        public ChartDataPoint(String label, double value, java.awt.Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
            this.tooltip = String.format("%s: %.0f", label, value);
        }
        
        public ChartDataPoint(String label, double value, java.awt.Color color, String tooltip) {
            this.label = label;
            this.value = value;
            this.color = color;
            this.tooltip = tooltip;
        }
        
        /**
         * Get percentage of total
         */
        public double getPercentage(double total) {
            return total > 0 ? (value / total) * 100 : 0;
        }
        
        /**
         * Format for display
         */
        public String getFormattedValue() {
            if (value == Math.floor(value)) {
                return String.format("%.0f", value);
            }
            return String.format("%.2f", value);
        }
        
        /**
         * Format as percentage
         */
        public String getFormattedPercentage(double total) {
            return String.format("%.1f%%", getPercentage(total));
        }
        
        /**
         * Convert to table row
         */
        public Object[] toTableRow() {
            return new Object[] { label, getFormattedValue() };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "Label", "Value" };
        }
        
        @Override
        public String toString() {
            return String.format("ChartDataPoint{label='%s', value=%.2f}", label, value);
        }
    }
    
    /**
     * Time series data point for line charts
     */
    public static class TimeSeriesPoint {
        public LocalDate date;
        public long value;
        public String label;
        
        public TimeSeriesPoint(LocalDate date, long value) {
            this.date = date;
            this.value = value;
            this.label = date.toString();
        }
        
        public TimeSeriesPoint(LocalDate date, long value, String label) {
            this.date = date;
            this.value = value;
            this.label = label;
        }
        
        /**
         * Get formatted date string (e.g., "Nov 27")
         */
        public String getShortDateLabel() {
            return date.getMonth().toString().substring(0, 3) + " " + date.getDayOfMonth();
        }
        
        /**
         * Get timestamp for chart plotting
         */
        public long getTimestamp() {
            return date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        }
        
        /**
         * Convert to table row
         */
        public Object[] toTableRow() {
            return new Object[] { date.toString(), value };
        }
        
        public static String[] getTableColumns() {
            return new String[] { "Date", "Value" };
        }
        
        @Override
        public String toString() {
            return String.format("TimeSeriesPoint{date=%s, value=%d}", date, value);
        }
    }
    
    /**
     * Stacked bar chart data point (multiple values per label)
     */
    public static class StackedDataPoint {
        public String label;
        public Map<String, Double> values;
        
        public StackedDataPoint(String label, Map<String, Double> values) {
            this.label = label;
            this.values = values;
        }
        
        /**
         * Get total of all stacked values
         */
        public double getTotal() {
            return values.values().stream().mapToDouble(Double::doubleValue).sum();
        }
        
        /**
         * Get value for a specific series
         */
        public double getValue(String series) {
            return values.getOrDefault(series, 0.0);
        }
        
        /**
         * Get all series names
         */
        public Set<String> getSeriesNames() {
            return values.keySet();
        }
        
        /**
         * Convert to table row (label + all values)
         */
        public Object[] toTableRow() {
            Object[] row = new Object[values.size() + 1];
            row[0] = label;
            int i = 1;
            for (Double val : values.values()) {
                row[i++] = String.format("%.0f", val);
            }
            return row;
        }
        
        /**
         * Get table columns (Label + series names)
         */
        public String[] getTableColumns() {
            String[] cols = new String[values.size() + 1];
            cols[0] = "Label";
            int i = 1;
            for (String key : values.keySet()) {
                cols[i++] = key;
            }
            return cols;
        }
        
        @Override
        public String toString() {
            return String.format("StackedDataPoint{label='%s', values=%s}", label, values);
        }
    }
    
    /**
     * Result of comparing multiple enterprises
     */
    public static class ComparisonResult {
        public Map<String, Map<String, Double>> enterpriseMetrics = new LinkedHashMap<>();
        public Map<String, String> metricWinners = new LinkedHashMap<>();
        public Map<String, Double> averages = new LinkedHashMap<>();
        
        /**
         * Get value for a specific enterprise and metric
         */
        public Double getValue(String enterprise, String metric) {
            Map<String, Double> metrics = enterpriseMetrics.get(enterprise);
            return metrics != null ? metrics.get(metric) : null;
        }
        
        /**
         * Get the winner for a specific metric
         */
        public String getWinner(String metric) {
            return metricWinners.get(metric);
        }
        
        /**
         * Get network average for a metric
         */
        public Double getAverage(String metric) {
            return averages.get(metric);
        }
        
        /**
         * Get all enterprise names being compared
         */
        public Set<String> getEnterprises() {
            return enterpriseMetrics.keySet();
        }
        
        /**
         * Get all metric names
         */
        public Set<String> getMetrics() {
            return metricWinners.keySet();
        }
        
        /**
         * Check if an enterprise is the winner for a metric
         */
        public boolean isWinner(String enterprise, String metric) {
            String winner = metricWinners.get(metric);
            return winner != null && winner.equals(enterprise);
        }
        
        /**
         * Get formatted summary
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Enterprise Comparison:\n");
            
            for (String metric : metricWinners.keySet()) {
                sb.append(String.format("• %s: Winner = %s (Avg: %.1f)\n",
                    metric, metricWinners.get(metric), averages.get(metric)));
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
    
    /**
     * Result of comparing two time periods
     */
    public static class PeriodComparison {
        public String period1Label;
        public String period2Label;
        public long period1Items;
        public long period2Items;
        public long period1Recovered;
        public long period2Recovered;
        public double period1RecoveryRate;
        public double period2RecoveryRate;
        public double itemsChange;
        public double recoveryRateChange;
        
        /**
         * Get items trend direction
         */
        public String getItemsTrend() {
            if (itemsChange > 0.05) return "↑ Increasing";
            if (itemsChange < -0.05) return "↓ Decreasing";
            return "→ Stable";
        }
        
        /**
         * Get recovery rate trend direction
         */
        public String getRecoveryTrend() {
            if (recoveryRateChange > 0.05) return "↑ Improving";
            if (recoveryRateChange < -0.05) return "↓ Declining";
            return "→ Stable";
        }
        
        /**
         * Get formatted summary
         */
        public String getSummary() {
            return String.format(
                "Period Comparison:\n" +
                "Period 1 (%s): %d items, %d recovered (%.0f%% rate)\n" +
                "Period 2 (%s): %d items, %d recovered (%.0f%% rate)\n" +
                "Changes: Items %s%.0f%%, Recovery Rate %s%.0f%%",
                period1Label, period1Items, period1Recovered, period1RecoveryRate * 100,
                period2Label, period2Items, period2Recovered, period2RecoveryRate * 100,
                itemsChange >= 0 ? "+" : "", itemsChange * 100,
                recoveryRateChange >= 0 ? "+" : "", recoveryRateChange * 100
            );
        }
        
        /**
         * Convert to table rows for display
         */
        public Object[][] toTableData() {
            return new Object[][] {
                { "Period", period1Label, period2Label, "Change" },
                { "Items", period1Items, period2Items, String.format("%+.0f%%", itemsChange * 100) },
                { "Recovered", period1Recovered, period2Recovered, "" },
                { "Recovery Rate", String.format("%.0f%%", period1RecoveryRate * 100),
                  String.format("%.0f%%", period2RecoveryRate * 100),
                  String.format("%+.0f%%", recoveryRateChange * 100) }
            };
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
    
    /**
     * Result of benchmarking an enterprise against network average
     */
    public static class BenchmarkResult {
        public String enterpriseName;
        public long enterpriseItems;
        public double enterpriseRecoveryRate;
        public long enterpriseUsers;
        public long networkAvgItems;
        public double networkAvgRecoveryRate;
        public long networkAvgUsers;
        public double itemsDifferential;
        public double recoveryRateDifferential;
        public double usersDifferential;
        public String performanceLevel;
        
        /**
         * Get overall performance indicator
         */
        public String getPerformanceIndicator() {
            switch (performanceLevel) {
                case "Excellent": return "⭐⭐⭐";
                case "Above Average": return "⭐⭐";
                case "Average": return "⭐";
                default: return "";
            }
        }
        
        /**
         * Get color for performance level
         */
        public java.awt.Color getPerformanceColor() {
            switch (performanceLevel) {
                case "Excellent": return new java.awt.Color(39, 174, 96);      // Green
                case "Above Average": return new java.awt.Color(46, 204, 113); // Light Green
                case "Average": return new java.awt.Color(241, 196, 15);       // Yellow
                default: return new java.awt.Color(231, 76, 60);               // Red
            }
        }
        
        /**
         * Get differential indicator (positive/negative)
         */
        public String getDifferentialIndicator(double differential) {
            if (differential > 0.1) return "↑↑";
            if (differential > 0) return "↑";
            if (differential > -0.1) return "→";
            if (differential > -0.2) return "↓";
            return "↓↓";
        }
        
        /**
         * Get formatted summary
         */
        public String getSummary() {
            return String.format(
                "Benchmark: %s vs Network Average\n" +
                "• Items: %d vs %.0f (%s%.0f%%) %s\n" +
                "• Recovery: %.0f%% vs %.0f%% (%s%.0f%%) %s\n" +
                "• Users: %d vs %.0f (%s%.0f%%) %s\n" +
                "Overall Performance: %s %s",
                enterpriseName,
                enterpriseItems, (double) networkAvgItems,
                itemsDifferential >= 0 ? "+" : "", itemsDifferential * 100,
                getDifferentialIndicator(itemsDifferential),
                enterpriseRecoveryRate * 100, networkAvgRecoveryRate * 100,
                recoveryRateDifferential >= 0 ? "+" : "", recoveryRateDifferential * 100,
                getDifferentialIndicator(recoveryRateDifferential),
                enterpriseUsers, (double) networkAvgUsers,
                usersDifferential >= 0 ? "+" : "", usersDifferential * 100,
                getDifferentialIndicator(usersDifferential),
                performanceLevel, getPerformanceIndicator()
            );
        }
        
        /**
         * Convert to chart-ready data for gauge display
         */
        public List<ChartDataPoint> toChartData() {
            List<ChartDataPoint> data = new ArrayList<>();
            
            data.add(new ChartDataPoint(
                "Items", 
                itemsDifferential * 100,
                itemsDifferential >= 0 ? new java.awt.Color(46, 204, 113) : new java.awt.Color(231, 76, 60)
            ));
            
            data.add(new ChartDataPoint(
                "Recovery Rate",
                recoveryRateDifferential * 100,
                recoveryRateDifferential >= 0 ? new java.awt.Color(46, 204, 113) : new java.awt.Color(231, 76, 60)
            ));
            
            data.add(new ChartDataPoint(
                "Users",
                usersDifferential * 100,
                usersDifferential >= 0 ? new java.awt.Color(46, 204, 113) : new java.awt.Color(231, 76, 60)
            ));
            
            return data;
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
}
