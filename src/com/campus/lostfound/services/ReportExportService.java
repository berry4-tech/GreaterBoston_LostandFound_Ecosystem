package com.campus.lostfound.services;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.User.UserRole;
import com.campus.lostfound.models.workrequest.WorkRequest;
import com.campus.lostfound.models.workrequest.WorkRequest.*;
import com.campus.lostfound.services.AnalyticsService.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// PDF Export imports
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Service for exporting reports and data to various formats.
 * 
 * Provides functionality for:
 * - CSV export of items, users, work requests, analytics
 * - Report generation (executive summary, enterprise, weekly, monthly)
 * - Export utilities for file management
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class ReportExportService {
    
    private static final Logger LOGGER = Logger.getLogger(ReportExportService.class.getName());
    
    // ==================== DEPENDENCIES ====================
    
    private final AnalyticsService analyticsService;
    private final EnterpriseItemService enterpriseItemService;
    private final MongoItemDAO itemDAO;
    private final MongoUserDAO userDAO;
    private final MongoWorkRequestDAO workRequestDAO;
    private final MongoEnterpriseDAO enterpriseDAO;
    
    // ==================== CONFIGURATION ====================
    
    private static final String DEFAULT_EXPORT_DIR = "exports";
    private static final String CSV_DELIMITER = ",";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";
    
    // ==================== CONSTRUCTORS ====================
    
    public ReportExportService() {
        this.analyticsService = new AnalyticsService();
        this.enterpriseItemService = new EnterpriseItemService();
        this.itemDAO = new MongoItemDAO();
        this.userDAO = new MongoUserDAO();
        this.workRequestDAO = new MongoWorkRequestDAO();
        this.enterpriseDAO = new MongoEnterpriseDAO();
    }
    
    /**
     * Constructor for testing with mock dependencies
     */
    public ReportExportService(AnalyticsService analyticsService, 
                               EnterpriseItemService enterpriseItemService,
                               MongoItemDAO itemDAO, MongoUserDAO userDAO,
                               MongoWorkRequestDAO workRequestDAO,
                               MongoEnterpriseDAO enterpriseDAO) {
        this.analyticsService = analyticsService;
        this.enterpriseItemService = enterpriseItemService;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.workRequestDAO = workRequestDAO;
        this.enterpriseDAO = enterpriseDAO;
    }
    
    // ====================================================================================
    // CSV EXPORT METHODS
    // ====================================================================================
    
    /**
     * Export items to CSV file
     * @param items List of items to export
     * @param filePath Target file path
     * @return true if successful, false otherwise
     */
    public boolean exportItemsToCSV(List<Item> items, String filePath) {
        try {
            // Ensure directory exists
            createParentDirectories(filePath);
            
            // Define headers
            String[] headers = {
                "Item ID", "Title", "Description", "Category", "Type", "Status",
                "Reported Date", "Resolved Date", "Reported By", "Reported By Email",
                "Claimed By", "Location", "Enterprise", "Organization",
                "Brand", "Color", "Estimated Value", "Serial Number", "Keywords"
            };
            
            List<String[]> rows = new ArrayList<>();
            rows.add(headers);
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            
            for (Item item : items) {
                String[] row = new String[] {
                    safeString(item.getMongoId()),
                    safeString(item.getTitle()),
                    escapeCSV(item.getDescription()),
                    item.getCategory() != null ? item.getCategory().name() : "",
                    item.getType() != null ? item.getType().name() : "",
                    item.getStatus() != null ? item.getStatus().name() : "",
                    item.getReportedDate() != null ? sdf.format(item.getReportedDate()) : "",
                    item.getResolvedDate() != null ? sdf.format(item.getResolvedDate()) : "",
                    item.getReportedBy() != null ? item.getReportedBy().getFullName() : "",
                    item.getReportedBy() != null ? item.getReportedBy().getEmail() : "",
                    item.getClaimedBy() != null ? item.getClaimedBy().getFullName() : "",
                    item.getLocation() != null ? item.getLocation().getFullLocation() : "",
                    getEnterpriseName(item.getEnterpriseId()),
                    getOrganizationName(item.getOrganizationId()),
                    safeString(item.getBrand()),
                    safeString(item.getPrimaryColor()),
                    String.format("%.2f", item.getEstimatedValue()),
                    safeString(item.getSerialNumber()),
                    item.getKeywords() != null ? String.join("; ", item.getKeywords()) : ""
                };
                rows.add(row);
            }
            
            return writeCSV(filePath, rows);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting items to CSV: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Export all items in the system to CSV
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportAllItemsToCSV(String filePath) {
        try {
            List<Item> items = itemDAO.findAll();
            return exportItemsToCSV(items, filePath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting all items", e);
            return false;
        }
    }
    
    /**
     * Export users to CSV file
     * @param users List of users to export
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportUsersToCSV(List<User> users, String filePath) {
        try {
            createParentDirectories(filePath);
            
            String[] headers = {
                "User ID", "Email", "Full Name", "Role", "Phone",
                "Enterprise", "Organization", "Trust Score",
                "Items Reported", "Items Returned", "Items Claimed",
                "Successful Returns", "Created Date", "Active"
            };
            
            List<String[]> rows = new ArrayList<>();
            rows.add(headers);
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            
            for (User user : users) {
                String[] row = new String[] {
                    safeString(user.getEmail()), // Use email as ID
                    safeString(user.getEmail()),
                    safeString(user.getFullName()),
                    user.getRole() != null ? user.getRole().name() : "",
                    safeString(user.getPhoneNumber()),
                    getEnterpriseName(user.getEnterpriseId()),
                    getOrganizationName(user.getOrganizationId()),
                    String.format("%.1f", user.getTrustScore()),
                    String.valueOf(user.getItemsReported()),
                    String.valueOf(user.getItemsReturned()),
                    "0", // Items claimed - not tracked in model
                    String.valueOf(user.getItemsReturned()), // Use items returned as successful returns
                    user.getJoinDate() != null ? sdf.format(user.getJoinDate()) : "",
                    String.valueOf(user.getTrustScore() >= 25) // Active if not suspended
                };
                rows.add(row);
            }
            
            return writeCSV(filePath, rows);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting users to CSV: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Export all users to CSV
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportAllUsersToCSV(String filePath) {
        try {
            List<User> users = userDAO.findAll();
            return exportUsersToCSV(users, filePath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting all users", e);
            return false;
        }
    }
    
    /**
     * Export work requests to CSV file
     * @param requests List of work requests to export
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportWorkRequestsToCSV(List<WorkRequest> requests, String filePath) {
        try {
            createParentDirectories(filePath);
            
            String[] headers = {
                "Request ID", "Type", "Status", "Priority",
                "Requester ID", "Requester Name", "Requester Enterprise",
                "Target Enterprise", "Description", "Notes",
                "Approval Step", "Approvers", "Current Approver",
                "Created At", "Last Updated", "Completed At",
                "SLA Target Hours", "Is Overdue", "Summary"
            };
            
            List<String[]> rows = new ArrayList<>();
            rows.add(headers);
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_FORMAT);
            
            for (WorkRequest request : requests) {
                String[] row = new String[] {
                    safeString(request.getRequestId()),
                    request.getRequestType() != null ? request.getRequestType().name() : "",
                    request.getStatus() != null ? request.getStatus().name() : "",
                    request.getPriority() != null ? request.getPriority().name() : "",
                    safeString(request.getRequesterId()),
                    safeString(request.getRequesterName()),
                    getEnterpriseName(request.getRequesterEnterpriseId()),
                    getEnterpriseName(request.getTargetEnterpriseId()),
                    escapeCSV(request.getDescription()),
                    escapeCSV(request.getNotes()),
                    String.valueOf(request.getApprovalStep()),
                    request.getApproverNames() != null ? String.join("; ", request.getApproverNames()) : "",
                    safeString(request.getCurrentApproverId()),
                    request.getCreatedAt() != null ? request.getCreatedAt().format(dtf) : "",
                    request.getLastUpdatedAt() != null ? request.getLastUpdatedAt().format(dtf) : "",
                    request.getCompletedAt() != null ? request.getCompletedAt().format(dtf) : "",
                    String.valueOf(request.getSlaTargetHours()),
                    String.valueOf(request.isOverdue()),
                    escapeCSV(request.getRequestSummary())
                };
                rows.add(row);
            }
            
            return writeCSV(filePath, rows);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting work requests to CSV: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Export all work requests to CSV
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportAllWorkRequestsToCSV(String filePath) {
        try {
            List<WorkRequest> requests = workRequestDAO.findAll();
            return exportWorkRequestsToCSV(requests, filePath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting all work requests", e);
            return false;
        }
    }
    
    /**
     * Export analytics data to CSV
     * @param analytics ExecutiveSummary containing analytics
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportAnalyticsToCSV(ExecutiveSummary analytics, String filePath) {
        try {
            createParentDirectories(filePath);
            
            List<String[]> rows = new ArrayList<>();
            
            // Summary section
            rows.add(new String[] { "=== EXECUTIVE SUMMARY ===" });
            rows.add(new String[] { "Generated At", LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT)) });
            rows.add(new String[] { "" });
            
            // Key metrics
            rows.add(new String[] { "Metric", "Value" });
            rows.add(new String[] { "Total Items", String.valueOf(analytics.totalItems) });
            rows.add(new String[] { "Total Users", String.valueOf(analytics.totalUsers) });
            rows.add(new String[] { "Overall Recovery Rate", String.format("%.1f%%", analytics.overallRecoveryRate * 100) });
            rows.add(new String[] { "Average Recovery Time (Hours)", String.format("%.1f", analytics.avgRecoveryTimeHours) });
            rows.add(new String[] { "SLA Compliance Rate", String.format("%.1f%%", analytics.slaComplianceRate * 100) });
            rows.add(new String[] { "Cross-Enterprise Match Rate", String.format("%.1f%%", analytics.crossEnterpriseMatchRate * 100) });
            rows.add(new String[] { "Network Effect Improvement", String.format("%.1f%%", analytics.networkEffectImprovement) });
            rows.add(new String[] { "Top Enterprise", analytics.topEnterprise != null ? analytics.topEnterprise : "N/A" });
            rows.add(new String[] { "Top Enterprise Items", String.valueOf(analytics.topEnterpriseItems) });
            rows.add(new String[] { "" });
            
            // Alerts section
            rows.add(new String[] { "=== ALERTS ===" });
            rows.add(new String[] { "Level", "Title", "Message" });
            if (analytics.alerts != null) {
                for (SystemAlert alert : analytics.alerts) {
                    rows.add(new String[] { alert.level.name(), alert.title, alert.message });
                }
            }
            rows.add(new String[] { "" });
            
            // Recommendations section
            rows.add(new String[] { "=== RECOMMENDATIONS ===" });
            if (analytics.recommendations != null) {
                for (String rec : analytics.recommendations) {
                    rows.add(new String[] { rec });
                }
            }
            
            return writeCSV(filePath, rows);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting analytics to CSV: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Export custom query results to CSV
     * @param headers Column headers
     * @param data Row data
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportCustomQueryToCSV(String[] headers, List<Object[]> data, String filePath) {
        try {
            createParentDirectories(filePath);
            
            List<String[]> rows = new ArrayList<>();
            rows.add(headers);
            
            for (Object[] row : data) {
                String[] strRow = new String[row.length];
                for (int i = 0; i < row.length; i++) {
                    strRow[i] = row[i] != null ? escapeCSV(row[i].toString()) : "";
                }
                rows.add(strRow);
            }
            
            return writeCSV(filePath, rows);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting custom query to CSV", e);
            return false;
        }
    }
    
    /**
     * Export enterprise statistics to CSV
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportEnterpriseStatsToCSV(String filePath) {
        try {
            List<EnterpriseStats> stats = analyticsService.getEnterpriseStats();
            
            String[] headers = EnterpriseStats.getTableColumns();
            List<Object[]> data = stats.stream()
                .map(EnterpriseStats::toTableRow)
                .collect(Collectors.toList());
            
            return exportCustomQueryToCSV(headers, data, filePath);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting enterprise stats", e);
            return false;
        }
    }
    
    /**
     * Export weekly trends to CSV
     * @param weeks Number of weeks
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportWeeklyTrendsToCSV(int weeks, String filePath) {
        try {
            List<WeeklyTrend> trends = analyticsService.getWeeklyTrends(weeks);
            
            String[] headers = WeeklyTrend.getTableColumns();
            List<Object[]> data = trends.stream()
                .map(WeeklyTrend::toTableRow)
                .collect(Collectors.toList());
            
            return exportCustomQueryToCSV(headers, data, filePath);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting weekly trends", e);
            return false;
        }
    }
    
    /**
     * Export monthly trends to CSV
     * @param months Number of months
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean exportMonthlyTrendsToCSV(int months, String filePath) {
        try {
            List<MonthlyTrend> trends = analyticsService.getMonthlyTrends(months);
            
            String[] headers = MonthlyTrend.getTableColumns();
            List<Object[]> data = trends.stream()
                .map(MonthlyTrend::toTableRow)
                .collect(Collectors.toList());
            
            return exportCustomQueryToCSV(headers, data, filePath);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting monthly trends", e);
            return false;
        }
    }
    
    // ====================================================================================
    // REPORT GENERATION METHODS
    // ====================================================================================
    
    /**
     * Generate executive summary report
     * @return ReportData containing the report
     */
    public ReportData generateExecutiveSummary() {
        try {
            ReportData report = new ReportData();
            report.title = "Executive Summary Report";
            report.generatedAt = LocalDateTime.now();
            report.reportType = ReportType.EXECUTIVE_SUMMARY;
            
            // Get analytics data
            ExecutiveSummary summary = analyticsService.getExecutiveSummary();
            QuickStats quickStats = analyticsService.getQuickStats();
            
            // Overview section
            ReportSection overview = new ReportSection("Overview");
            overview.addMetric("Total Items in System", String.valueOf(summary.totalItems));
            overview.addMetric("Total Registered Users", String.valueOf(summary.totalUsers));
            overview.addMetric("Overall Recovery Rate", String.format("%.1f%%", summary.overallRecoveryRate * 100));
            overview.addMetric("Average Recovery Time", String.format("%.1f hours", summary.avgRecoveryTimeHours));
            report.sections.add(overview);
            
            // Performance section
            ReportSection performance = new ReportSection("Performance Metrics");
            performance.addMetric("SLA Compliance Rate", String.format("%.1f%%", summary.slaComplianceRate * 100));
            performance.addMetric("Cross-Enterprise Match Rate", String.format("%.1f%%", summary.crossEnterpriseMatchRate * 100));
            performance.addMetric("Network Effect Improvement", String.format("%.1f%%", summary.networkEffectImprovement));
            performance.addMetric("Active Users (30 days)", String.valueOf(quickStats.activeUsers));
            report.sections.add(performance);
            
            // Current Status section
            ReportSection status = new ReportSection("Current Status");
            status.addMetric("Open Items", String.valueOf(quickStats.openItems));
            status.addMetric("Pending Claims", String.valueOf(quickStats.pendingClaims));
            status.addMetric("Claimed Items", String.valueOf(quickStats.claimedItems));
            status.addMetric("Pending Work Requests", String.valueOf(quickStats.pendingRequests));
            report.sections.add(status);
            
            // High-Value Items section
            ReportSection highValue = new ReportSection("High-Value Items");
            highValue.addMetric("High-Value Item Count", String.valueOf(quickStats.highValueItemCount));
            highValue.addMetric("Total High-Value Amount", String.format("$%.2f", quickStats.highValueTotalValue));
            report.sections.add(highValue);
            
            // Top Enterprise section
            if (summary.topEnterprise != null) {
                ReportSection topEnt = new ReportSection("Top Enterprise");
                topEnt.addMetric("Enterprise Name", summary.topEnterprise);
                topEnt.addMetric("Total Items", String.valueOf(summary.topEnterpriseItems));
                report.sections.add(topEnt);
            }
            
            // Alerts section
            if (summary.alerts != null && !summary.alerts.isEmpty()) {
                ReportSection alerts = new ReportSection("System Alerts");
                for (SystemAlert alert : summary.alerts) {
                    alerts.addMetric("[" + alert.level.name() + "] " + alert.title, alert.message);
                }
                report.sections.add(alerts);
            }
            
            // Recommendations section
            if (summary.recommendations != null && !summary.recommendations.isEmpty()) {
                ReportSection recs = new ReportSection("Recommendations");
                int i = 1;
                for (String rec : summary.recommendations) {
                    recs.addMetric("Recommendation " + i++, rec);
                }
                report.sections.add(recs);
            }
            
            // Store raw data
            report.rawData.put("executiveSummary", summary);
            report.rawData.put("quickStats", quickStats);
            
            return report;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating executive summary", e);
            return createErrorReport("Executive Summary", e.getMessage());
        }
    }
    
    /**
     * Generate enterprise-specific report
     * @param enterpriseId The enterprise ID
     * @return ReportData containing the report
     */
    public ReportData generateEnterpriseReport(String enterpriseId) {
        try {
            ReportData report = new ReportData();
            report.generatedAt = LocalDateTime.now();
            report.reportType = ReportType.ENTERPRISE;
            
            // Get enterprise details
            Optional<Enterprise> enterpriseOpt = enterpriseDAO.findById(enterpriseId);
            if (enterpriseOpt.isEmpty()) {
                return createErrorReport("Enterprise Report", "Enterprise not found: " + enterpriseId);
            }
            Enterprise enterprise = enterpriseOpt.get();
            
            report.title = "Enterprise Report: " + enterprise.getName();
            
            // Get enterprise-specific stats
            List<EnterpriseStats> allStats = analyticsService.getEnterpriseStats();
            EnterpriseStats entStats = allStats.stream()
                .filter(s -> s.enterpriseId.equals(enterpriseId))
                .findFirst()
                .orElse(null);
            
            // Overview section
            ReportSection overview = new ReportSection("Enterprise Overview");
            overview.addMetric("Enterprise Name", enterprise.getName());
            overview.addMetric("Enterprise Type", enterprise.getType() != null ? enterprise.getType().name() : "N/A");
            overview.addMetric("Active", String.valueOf(enterprise.isActive()));
            report.sections.add(overview);
            
            // Item Statistics section
            if (entStats != null) {
                ReportSection items = new ReportSection("Item Statistics");
                items.addMetric("Total Items", String.valueOf(entStats.totalItems));
                items.addMetric("Lost Items", String.valueOf(entStats.lostItems));
                items.addMetric("Found Items", String.valueOf(entStats.foundItems));
                items.addMetric("Claimed Items", String.valueOf(entStats.claimedItems));
                items.addMetric("Open Items", String.valueOf(entStats.openItems));
                items.addMetric("Recovery Rate", String.format("%.1f%%", entStats.recoveryRate * 100));
                report.sections.add(items);
                
                ReportSection users = new ReportSection("User Statistics");
                users.addMetric("Total Users", String.valueOf(entStats.userCount));
                report.sections.add(users);
            }
            
            // Benchmark against network
            BenchmarkResult benchmark = analyticsService.benchmarkAgainstNetwork(enterpriseId);
            if (benchmark != null && benchmark.enterpriseName != null) {
                ReportSection benchSection = new ReportSection("Network Benchmark");
                benchSection.addMetric("Performance Level", benchmark.performanceLevel);
                benchSection.addMetric("Items vs Network Avg", String.format("%+.1f%%", benchmark.itemsDifferential * 100));
                benchSection.addMetric("Recovery Rate vs Network", String.format("%+.1f%%", benchmark.recoveryRateDifferential * 100));
                benchSection.addMetric("Users vs Network Avg", String.format("%+.1f%%", benchmark.usersDifferential * 100));
                report.sections.add(benchSection);
            }
            
            // Pending work requests
            Map<String, Long> pendingByEnt = analyticsService.getPendingRequestsByEnterprise();
            String entName = enterprise.getName();
            long pendingCount = pendingByEnt.getOrDefault(entName, 0L);
            
            ReportSection workReqs = new ReportSection("Work Requests");
            workReqs.addMetric("Pending Requests", String.valueOf(pendingCount));
            report.sections.add(workReqs);
            
            // Store raw data
            report.rawData.put("enterprise", enterprise);
            report.rawData.put("stats", entStats);
            report.rawData.put("benchmark", benchmark);
            
            return report;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating enterprise report", e);
            return createErrorReport("Enterprise Report", e.getMessage());
        }
    }
    
    /**
     * Generate weekly activity report
     * @return ReportData containing the report
     */
    public ReportData generateWeeklyReport() {
        try {
            ReportData report = new ReportData();
            report.title = "Weekly Activity Report";
            report.generatedAt = LocalDateTime.now();
            report.reportType = ReportType.WEEKLY;
            
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(6);
            
            report.periodStart = weekStart;
            report.periodEnd = today;
            
            // Get weekly trends
            List<WeeklyTrend> trends = analyticsService.getWeeklyTrends(4);
            WeeklyTrend thisWeek = trends.isEmpty() ? null : trends.get(trends.size() - 1);
            WeeklyTrend lastWeek = trends.size() > 1 ? trends.get(trends.size() - 2) : null;
            
            // This Week Summary
            ReportSection summary = new ReportSection("This Week Summary (" + weekStart + " to " + today + ")");
            if (thisWeek != null) {
                summary.addMetric("Items Reported", String.valueOf(thisWeek.itemsReported));
                summary.addMetric("Lost Items", String.valueOf(thisWeek.lostItems));
                summary.addMetric("Found Items", String.valueOf(thisWeek.foundItems));
                summary.addMetric("Items Recovered", String.valueOf(thisWeek.recovered));
                summary.addMetric("Recovery Rate", String.format("%.1f%%", thisWeek.recoveryRate * 100));
            }
            report.sections.add(summary);
            
            // Week-over-Week comparison
            if (thisWeek != null && lastWeek != null) {
                ReportSection comparison = new ReportSection("Week-over-Week Comparison");
                
                double itemsChange = lastWeek.itemsReported > 0 ?
                    ((double) thisWeek.itemsReported - lastWeek.itemsReported) / lastWeek.itemsReported * 100 : 0;
                double recoveryChange = lastWeek.recoveryRate > 0 ?
                    (thisWeek.recoveryRate - lastWeek.recoveryRate) / lastWeek.recoveryRate * 100 : 0;
                
                comparison.addMetric("Items Reported Change", String.format("%+.1f%%", itemsChange));
                comparison.addMetric("Recovery Rate Change", String.format("%+.1f%%", recoveryChange));
                comparison.addMetric("Last Week Items", String.valueOf(lastWeek.itemsReported));
                comparison.addMetric("Last Week Recovery Rate", String.format("%.1f%%", lastWeek.recoveryRate * 100));
                report.sections.add(comparison);
            }
            
            // Daily breakdown
            Date startDate = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<DailyStats> dailyStats = analyticsService.getDailyStats(startDate, endDate);
            
            ReportSection daily = new ReportSection("Daily Breakdown");
            for (DailyStats ds : dailyStats) {
                String dayLabel = ds.date.getDayOfWeek().toString().substring(0, 3) + " " + ds.date.getDayOfMonth();
                daily.addMetric(dayLabel, String.format("Items: %d, Requests: %d, Recoveries: %d",
                    ds.itemsReported, ds.requestsCreated, ds.recoveries));
            }
            report.sections.add(daily);
            
            // Enterprise Activity
            List<EnterpriseStats> entStats = analyticsService.getEnterpriseStats();
            ReportSection entActivity = new ReportSection("Enterprise Activity");
            for (EnterpriseStats es : entStats) {
                entActivity.addMetric(es.enterpriseName, 
                    String.format("Items: %d, Recovery: %.0f%%", es.totalItems, es.recoveryRate * 100));
            }
            report.sections.add(entActivity);
            
            // Store raw data
            report.rawData.put("weeklyTrends", trends);
            report.rawData.put("dailyStats", dailyStats);
            
            return report;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating weekly report", e);
            return createErrorReport("Weekly Report", e.getMessage());
        }
    }
    
    /**
     * Generate monthly activity report
     * @return ReportData containing the report
     */
    public ReportData generateMonthlyReport() {
        try {
            ReportData report = new ReportData();
            report.title = "Monthly Activity Report";
            report.generatedAt = LocalDateTime.now();
            report.reportType = ReportType.MONTHLY;
            
            YearMonth thisMonth = YearMonth.now();
            report.periodStart = thisMonth.atDay(1);
            report.periodEnd = thisMonth.atEndOfMonth();
            
            // Get monthly trends
            List<MonthlyTrend> trends = analyticsService.getMonthlyTrends(6);
            MonthlyTrend currentMonth = trends.isEmpty() ? null : trends.get(trends.size() - 1);
            MonthlyTrend lastMonth = trends.size() > 1 ? trends.get(trends.size() - 2) : null;
            
            // This Month Summary
            ReportSection summary = new ReportSection("This Month Summary (" + thisMonth + ")");
            if (currentMonth != null) {
                summary.addMetric("Items Reported", String.valueOf(currentMonth.itemsReported));
                summary.addMetric("Items Recovered", String.valueOf(currentMonth.recovered));
                summary.addMetric("Recovery Rate", String.format("%.1f%%", currentMonth.recoveryRate * 100));
                summary.addMetric("Total Value Processed", String.format("$%.2f", currentMonth.totalValue));
            }
            report.sections.add(summary);
            
            // Month-over-Month comparison
            if (currentMonth != null && lastMonth != null) {
                ReportSection comparison = new ReportSection("Month-over-Month Comparison");
                
                double itemsChange = lastMonth.itemsReported > 0 ?
                    ((double) currentMonth.itemsReported - lastMonth.itemsReported) / lastMonth.itemsReported * 100 : 0;
                double recoveryChange = lastMonth.recoveryRate > 0 ?
                    (currentMonth.recoveryRate - lastMonth.recoveryRate) / lastMonth.recoveryRate * 100 : 0;
                double valueChange = lastMonth.totalValue > 0 ?
                    (currentMonth.totalValue - lastMonth.totalValue) / lastMonth.totalValue * 100 : 0;
                
                comparison.addMetric("Items Reported Change", String.format("%+.1f%%", itemsChange));
                comparison.addMetric("Recovery Rate Change", String.format("%+.1f%%", recoveryChange));
                comparison.addMetric("Value Processed Change", String.format("%+.1f%%", valueChange));
                report.sections.add(comparison);
            }
            
            // 6-Month Trend
            ReportSection trend = new ReportSection("6-Month Trend");
            for (MonthlyTrend mt : trends) {
                trend.addMetric(mt.getMonthLabel(), String.format("Items: %d, Recovery: %.0f%%, Value: $%.0f",
                    mt.itemsReported, mt.recoveryRate * 100, mt.totalValue));
            }
            report.sections.add(trend);
            
            // Year-over-Year comparison
            YearComparison yoy = analyticsService.getYearOverYearComparison();
            ReportSection yearSection = new ReportSection("Year-over-Year (YTD)");
            yearSection.addMetric("This Year Items", String.valueOf(yoy.currentYearItems));
            yearSection.addMetric("Last Year Items (Same Period)", String.valueOf(yoy.previousYearItems));
            yearSection.addMetric("Items Change", String.format("%+.1f%%", yoy.itemsChangePercent * 100));
            yearSection.addMetric("This Year Recovery Rate", String.format("%.1f%%", yoy.currentYearRecoveryRate * 100));
            yearSection.addMetric("Last Year Recovery Rate", String.format("%.1f%%", yoy.previousYearRecoveryRate * 100));
            yearSection.addMetric("Recovery Rate Change", String.format("%+.1f%%", yoy.recoveryRateChangePercent * 100));
            report.sections.add(yearSection);
            
            // Network Effects
            NetworkEffectStats networkStats = analyticsService.getNetworkEffectMetrics();
            ReportSection network = new ReportSection("Network Effect Analysis");
            network.addMetric("Single Enterprise Recovery", String.format("%.1f%%", networkStats.singleEnterpriseRate * 100));
            network.addMetric("Two Enterprise Recovery", String.format("%.1f%%", networkStats.twoEnterpriseRate * 100));
            network.addMetric("Three Enterprise Recovery", String.format("%.1f%%", networkStats.threeEnterpriseRate * 100));
            network.addMetric("Four+ Enterprise Recovery", String.format("%.1f%%", networkStats.fourPlusEnterpriseRate * 100));
            network.addMetric("Network Improvement", String.format("%.1f%%", networkStats.getImprovementPercentage()));
            report.sections.add(network);
            
            // Store raw data
            report.rawData.put("monthlyTrends", trends);
            report.rawData.put("yearComparison", yoy);
            report.rawData.put("networkStats", networkStats);
            
            return report;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating monthly report", e);
            return createErrorReport("Monthly Report", e.getMessage());
        }
    }
    
    /**
     * Generate custom report based on criteria
     * @param criteria Report criteria
     * @return ReportData containing the report
     */
    public ReportData generateCustomReport(ReportCriteria criteria) {
        try {
            ReportData report = new ReportData();
            report.title = criteria.title != null ? criteria.title : "Custom Report";
            report.generatedAt = LocalDateTime.now();
            report.reportType = ReportType.CUSTOM;
            report.periodStart = criteria.startDate;
            report.periodEnd = criteria.endDate;
            
            // Filter items by date range
            List<Item> allItems = itemDAO.findAll();
            List<Item> filteredItems = allItems.stream()
                .filter(i -> i.getReportedDate() != null)
                .filter(i -> {
                    if (criteria.startDate == null && criteria.endDate == null) return true;
                    LocalDate itemDate = i.getReportedDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                    if (criteria.startDate != null && itemDate.isBefore(criteria.startDate)) return false;
                    if (criteria.endDate != null && itemDate.isAfter(criteria.endDate)) return false;
                    return true;
                })
                .filter(i -> {
                    if (criteria.enterpriseIds == null || criteria.enterpriseIds.isEmpty()) return true;
                    return criteria.enterpriseIds.contains(i.getEnterpriseId());
                })
                .filter(i -> {
                    if (criteria.categories == null || criteria.categories.isEmpty()) return true;
                    return criteria.categories.contains(i.getCategory());
                })
                .collect(Collectors.toList());
            
            // Summary section
            ReportSection summary = new ReportSection("Report Summary");
            summary.addMetric("Report Period", (criteria.startDate != null ? criteria.startDate.toString() : "All") 
                + " to " + (criteria.endDate != null ? criteria.endDate.toString() : "Present"));
            summary.addMetric("Total Items Matching", String.valueOf(filteredItems.size()));
            report.sections.add(summary);
            
            // Item Statistics
            long lost = filteredItems.stream().filter(i -> i.getType() == ItemType.LOST).count();
            long found = filteredItems.stream().filter(i -> i.getType() == ItemType.FOUND).count();
            long claimed = filteredItems.stream().filter(i -> i.getStatus() == ItemStatus.CLAIMED).count();
            long open = filteredItems.stream().filter(i -> i.getStatus() == ItemStatus.OPEN).count();
            double recoveryRate = filteredItems.size() > 0 ? (double) claimed / filteredItems.size() : 0;
            
            ReportSection items = new ReportSection("Item Statistics");
            items.addMetric("Lost Items", String.valueOf(lost));
            items.addMetric("Found Items", String.valueOf(found));
            items.addMetric("Claimed Items", String.valueOf(claimed));
            items.addMetric("Open Items", String.valueOf(open));
            items.addMetric("Recovery Rate", String.format("%.1f%%", recoveryRate * 100));
            report.sections.add(items);
            
            // Category breakdown
            if (criteria.includeCategories) {
                Map<ItemCategory, Long> catCounts = filteredItems.stream()
                    .filter(i -> i.getCategory() != null)
                    .collect(Collectors.groupingBy(Item::getCategory, Collectors.counting()));
                
                ReportSection categories = new ReportSection("Category Breakdown");
                for (Map.Entry<ItemCategory, Long> entry : catCounts.entrySet()) {
                    categories.addMetric(entry.getKey().name(), String.valueOf(entry.getValue()));
                }
                report.sections.add(categories);
            }
            
            // Enterprise breakdown
            if (criteria.includeEnterprises) {
                Map<String, Long> entCounts = filteredItems.stream()
                    .filter(i -> i.getEnterpriseId() != null)
                    .collect(Collectors.groupingBy(Item::getEnterpriseId, Collectors.counting()));
                
                ReportSection enterprises = new ReportSection("Enterprise Breakdown");
                for (Map.Entry<String, Long> entry : entCounts.entrySet()) {
                    String entName = getEnterpriseName(entry.getKey());
                    enterprises.addMetric(entName, String.valueOf(entry.getValue()));
                }
                report.sections.add(enterprises);
            }
            
            // Value analysis
            if (criteria.includeValueAnalysis) {
                double totalValue = filteredItems.stream().mapToDouble(Item::getEstimatedValue).sum();
                double avgValue = filteredItems.stream().mapToDouble(Item::getEstimatedValue).average().orElse(0);
                double maxValue = filteredItems.stream().mapToDouble(Item::getEstimatedValue).max().orElse(0);
                long highValueCount = filteredItems.stream().filter(i -> i.getEstimatedValue() >= 500).count();
                
                ReportSection value = new ReportSection("Value Analysis");
                value.addMetric("Total Estimated Value", String.format("$%.2f", totalValue));
                value.addMetric("Average Item Value", String.format("$%.2f", avgValue));
                value.addMetric("Highest Value Item", String.format("$%.2f", maxValue));
                value.addMetric("High-Value Items (>$500)", String.valueOf(highValueCount));
                report.sections.add(value);
            }
            
            // Store raw data
            report.rawData.put("filteredItems", filteredItems);
            report.rawData.put("criteria", criteria);
            
            return report;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating custom report", e);
            return createErrorReport("Custom Report", e.getMessage());
        }
    }
    
    // ====================================================================================
    // EXPORT UTILITIES
    // ====================================================================================
    
    /**
     * Format object for CSV export
     * @param obj Object to format
     * @return Formatted string
     */
    public String formatForExport(Object obj) {
        if (obj == null) return "";
        if (obj instanceof Date) {
            return new SimpleDateFormat(DATE_FORMAT).format((Date) obj);
        }
        if (obj instanceof LocalDateTime) {
            return ((LocalDateTime) obj).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        }
        if (obj instanceof LocalDate) {
            return obj.toString();
        }
        if (obj instanceof Double) {
            return String.format("%.2f", obj);
        }
        return escapeCSV(obj.toString());
    }
    
    /**
     * Sanitize filename for safe file system use
     * @param name Original name
     * @return Sanitized filename
     */
    public String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }
        // Replace invalid characters
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Remove leading/trailing spaces and dots
        sanitized = sanitized.trim();
        while (sanitized.startsWith(".")) {
            sanitized = sanitized.substring(1);
        }
        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized.isEmpty() ? "unnamed" : sanitized;
    }
    
    /**
     * Create export directory if it doesn't exist
     * @return File representing the export directory
     */
    public File createExportDirectory() {
        return createExportDirectory(DEFAULT_EXPORT_DIR);
    }
    
    /**
     * Create export directory at specified path
     * @param dirPath Directory path
     * @return File representing the directory
     */
    public File createExportDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * Get default export path for a report type
     * @return Default export path
     */
    public String getDefaultExportPath() {
        return DEFAULT_EXPORT_DIR;
    }
    
    /**
     * Generate a unique filename with timestamp
     * @param prefix Filename prefix
     * @param extension File extension (e.g., "csv")
     * @return Unique filename
     */
    public String generateUniqueFilename(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(FILE_DATE_FORMAT));
        return sanitizeFileName(prefix) + "_" + timestamp + "." + extension;
    }
    
    /**
     * Get full path for an export file
     * @param filename Filename
     * @return Full path including export directory
     */
    public String getExportFilePath(String filename) {
        return DEFAULT_EXPORT_DIR + File.separator + filename;
    }
    
    /**
     * Export report to CSV file
     * @param report Report data to export
     * @param filePath Target file path (optional, will generate if null)
     * @return File path where report was saved
     */
    public String exportReportToCSV(ReportData report, String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                String filename = generateUniqueFilename(sanitizeFileName(report.title), "csv");
                filePath = getExportFilePath(filename);
            }
            
            createParentDirectories(filePath);
            
            List<String[]> rows = new ArrayList<>();
            
            // Report header
            rows.add(new String[] { "=== " + report.title + " ===" });
            rows.add(new String[] { "Generated At", report.generatedAt.format(DateTimeFormatter.ofPattern(DATE_FORMAT)) });
            if (report.periodStart != null && report.periodEnd != null) {
                rows.add(new String[] { "Period", report.periodStart + " to " + report.periodEnd });
            }
            rows.add(new String[] { "" });
            
            // Sections
            for (ReportSection section : report.sections) {
                rows.add(new String[] { "--- " + section.title + " ---" });
                for (Map.Entry<String, String> metric : section.metrics.entrySet()) {
                    rows.add(new String[] { metric.getKey(), metric.getValue() });
                }
                rows.add(new String[] { "" });
            }
            
            if (writeCSV(filePath, rows)) {
                return filePath;
            }
            return null;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting report to CSV", e);
            return null;
        }
    }
    
    /**
     * Export report to text file
     * @param report Report data to export
     * @param filePath Target file path (optional)
     * @return File path where report was saved
     */
    public String exportReportToText(ReportData report, String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                String filename = generateUniqueFilename(sanitizeFileName(report.title), "txt");
                filePath = getExportFilePath(filename);
            }
            
            createParentDirectories(filePath);
            
            StringBuilder sb = new StringBuilder();
            
            // Report header
            sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
            sb.append("║ ").append(centerString(report.title, 66)).append(" ║\n");
            sb.append("╚══════════════════════════════════════════════════════════════════╝\n\n");
            
            sb.append("Generated: ").append(report.generatedAt.format(DateTimeFormatter.ofPattern(DATE_FORMAT))).append("\n");
            if (report.periodStart != null && report.periodEnd != null) {
                sb.append("Period: ").append(report.periodStart).append(" to ").append(report.periodEnd).append("\n");
            }
            sb.append("\n");
            
            // Sections
            for (ReportSection section : report.sections) {
                sb.append("┌──────────────────────────────────────────────────────────────────┐\n");
                sb.append("│ ").append(padRight(section.title, 66)).append(" │\n");
                sb.append("└──────────────────────────────────────────────────────────────────┘\n");
                
                for (Map.Entry<String, String> metric : section.metrics.entrySet()) {
                    sb.append("  • ").append(padRight(metric.getKey() + ":", 35))
                      .append(metric.getValue()).append("\n");
                }
                sb.append("\n");
            }
            
            // Footer
            sb.append("═══════════════════════════════════════════════════════════════════════\n");
            sb.append("Greater Boston Lost & Found Recovery Ecosystem\n");
            
            Files.writeString(Path.of(filePath), sb.toString(), StandardCharsets.UTF_8);
            return filePath;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting report to text", e);
            return null;
        }
    }
    
    /**
     * Export report to PDF file
     * @param report Report data to export
     * @param filePath Target file path (optional, will generate if null)
     * @return File path where report was saved, or null if failed
     */
    public String exportReportToPDF(ReportData report, String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                String filename = generateUniqueFilename(sanitizeFileName(report.title), "pdf");
                filePath = getExportFilePath(filename);
            }
            
            createParentDirectories(filePath);
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth();
                float contentWidth = pageWidth - 2 * margin;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(report.title);
                contentStream.endText();
                yPosition -= 25;
                
                // Subtitle line
                contentStream.setStrokingColor(0, 102, 204); // Blue line
                contentStream.setLineWidth(2);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                yPosition -= 20;
                
                // Generated timestamp
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.setNonStrokingColor(100, 100, 100); // Gray
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Generated: " + report.generatedAt.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
                contentStream.endText();
                yPosition -= 15;
                
                // Period if available
                if (report.periodStart != null && report.periodEnd != null) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("Period: " + report.periodStart + " to " + report.periodEnd);
                    contentStream.endText();
                    yPosition -= 15;
                }
                
                yPosition -= 20; // Extra spacing before sections
                
                // Reset color to black
                contentStream.setNonStrokingColor(0, 0, 0);
                
                // Sections
                for (ReportSection section : report.sections) {
                    // Check if we need a new page
                    if (yPosition < 100) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - margin;
                    }
                    
                    // Section header with background
                    contentStream.setNonStrokingColor(240, 240, 240); // Light gray background
                    contentStream.addRect(margin, yPosition - 5, contentWidth, 20);
                    contentStream.fill();
                    
                    contentStream.setNonStrokingColor(0, 0, 0); // Black text
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.newLineAtOffset(margin + 5, yPosition);
                    contentStream.showText(truncateText(section.title, 70));
                    contentStream.endText();
                    yPosition -= 25;
                    
                    // Section metrics
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    for (Map.Entry<String, String> metric : section.metrics.entrySet()) {
                        if (yPosition < 50) {
                            contentStream.close();
                            page = new PDPage(PDRectangle.LETTER);
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            yPosition = page.getMediaBox().getHeight() - margin;
                        }
                        
                        // Metric name (bold)
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                        contentStream.newLineAtOffset(margin + 10, yPosition);
                        String metricName = truncateText(metric.getKey(), 40);
                        contentStream.showText(metricName + ":");
                        contentStream.endText();
                        
                        // Metric value
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, 10);
                        contentStream.newLineAtOffset(margin + 200, yPosition);
                        String metricValue = truncateText(metric.getValue(), 50);
                        contentStream.showText(metricValue);
                        contentStream.endText();
                        
                        yPosition -= 15;
                    }
                    
                    yPosition -= 10; // Extra spacing between sections
                }
                
                // Footer
                yPosition = 30;
                contentStream.setStrokingColor(200, 200, 200);
                contentStream.setLineWidth(1);
                contentStream.moveTo(margin, yPosition + 10);
                contentStream.lineTo(pageWidth - margin, yPosition + 10);
                contentStream.stroke();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 8);
                contentStream.setNonStrokingColor(128, 128, 128);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Greater Boston Lost & Found Recovery Ecosystem");
                contentStream.endText();
                
                contentStream.close();
                document.save(filePath);
            }
            
            LOGGER.info("PDF exported successfully: " + filePath);
            return filePath;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting report to PDF: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Export items list to PDF
     * @param items List of items to export
     * @param title Report title
     * @param filePath Target file path (optional)
     * @return File path where PDF was saved
     */
    public String exportItemsToPDF(List<Item> items, String title, String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                String filename = generateUniqueFilename("Items_Report", "pdf");
                filePath = getExportFilePath(filename);
            }
            
            createParentDirectories(filePath);
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth();
                
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(title != null ? title : "Items Report");
                contentStream.endText();
                yPosition -= 20;
                
                // Summary
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Items: " + items.size() + " | Generated: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
                contentStream.endText();
                yPosition -= 25;
                
                // Table header
                drawTableHeader(contentStream, margin, yPosition, pageWidth);
                yPosition -= 20;
                
                // Table rows
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                int rowCount = 0;
                
                for (Item item : items) {
                    if (yPosition < 60) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - margin;
                        
                        // Redraw header on new page
                        drawTableHeader(contentStream, margin, yPosition, pageWidth);
                        yPosition -= 20;
                    }
                    
                    // Alternating row colors
                    if (rowCount % 2 == 0) {
                        contentStream.setNonStrokingColor(248, 248, 248);
                        contentStream.addRect(margin, yPosition - 3, pageWidth - 2 * margin, 15);
                        contentStream.fill();
                    }
                    contentStream.setNonStrokingColor(0, 0, 0);
                    
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 8);
                    contentStream.newLineAtOffset(margin + 5, yPosition);
                    contentStream.showText(truncateText(item.getTitle(), 25));
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 130, yPosition);
                    contentStream.showText(item.getCategory() != null ? truncateText(item.getCategory().name(), 12) : "");
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 210, yPosition);
                    contentStream.showText(item.getType() != null ? item.getType().name() : "");
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 270, yPosition);
                    contentStream.showText(item.getStatus() != null ? truncateText(item.getStatus().name(), 12) : "");
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 350, yPosition);
                    contentStream.showText(item.getReportedDate() != null ? sdf.format(item.getReportedDate()) : "");
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 430, yPosition);
                    contentStream.showText(String.format("$%.0f", item.getEstimatedValue()));
                    contentStream.endText();
                    
                    yPosition -= 15;
                    rowCount++;
                }
                
                contentStream.close();
                document.save(filePath);
            }
            
            LOGGER.info("Items PDF exported successfully: " + filePath);
            return filePath;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting items to PDF", e);
            return null;
        }
    }
    
    /**
     * Export work requests to PDF
     * @param requests List of work requests
     * @param title Report title
     * @param filePath Target file path (optional)
     * @return File path where PDF was saved
     */
    public String exportWorkRequestsToPDF(List<WorkRequest> requests, String title, String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                String filename = generateUniqueFilename("WorkRequests_Report", "pdf");
                filePath = getExportFilePath(filename);
            }
            
            createParentDirectories(filePath);
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth();
                
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(title != null ? title : "Work Requests Report");
                contentStream.endText();
                yPosition -= 20;
                
                // Summary stats
                long pending = requests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
                long inProgress = requests.stream().filter(r -> r.getStatus() == RequestStatus.IN_PROGRESS).count();
                long completed = requests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(String.format("Total: %d | Pending: %d | In Progress: %d | Completed: %d",
                    requests.size(), pending, inProgress, completed));
                contentStream.endText();
                yPosition -= 30;
                
                // Request details
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
                
                for (WorkRequest request : requests) {
                    if (yPosition < 80) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - margin;
                    }
                    
                    // Request box
                    contentStream.setNonStrokingColor(245, 245, 245);
                    contentStream.addRect(margin, yPosition - 55, pageWidth - 2 * margin, 65);
                    contentStream.fill();
                    
                    // Border based on priority
                    switch (request.getPriority()) {
                        case URGENT -> contentStream.setStrokingColor(220, 53, 69); // Red
                        case HIGH -> contentStream.setStrokingColor(255, 152, 0);   // Orange
                        case NORMAL -> contentStream.setStrokingColor(13, 110, 253); // Blue
                        default -> contentStream.setStrokingColor(108, 117, 125);   // Gray
                    }
                    contentStream.setLineWidth(2);
                    contentStream.addRect(margin, yPosition - 55, pageWidth - 2 * margin, 65);
                    contentStream.stroke();
                    
                    contentStream.setNonStrokingColor(0, 0, 0);
                    
                    // Type and ID
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    contentStream.newLineAtOffset(margin + 5, yPosition);
                    String typeLabel = request.getRequestType() != null ? request.getRequestType().name() : "UNKNOWN";
                    contentStream.showText(typeLabel.replace("_", " "));
                    contentStream.endText();
                    
                    // Priority badge
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 8);
                    contentStream.newLineAtOffset(margin + 200, yPosition);
                    contentStream.showText("[" + request.getPriority().name() + "]");
                    contentStream.endText();
                    
                    // Status
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 280, yPosition);
                    contentStream.showText("Status: " + request.getStatus().name());
                    contentStream.endText();
                    
                    yPosition -= 15;
                    
                    // Summary
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 9);
                    contentStream.newLineAtOffset(margin + 5, yPosition);
                    contentStream.showText(truncateText(request.getRequestSummary(), 80));
                    contentStream.endText();
                    yPosition -= 12;
                    
                    // Requester and dates
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 8);
                    contentStream.setNonStrokingColor(100, 100, 100);
                    contentStream.newLineAtOffset(margin + 5, yPosition);
                    contentStream.showText("Requester: " + (request.getRequesterName() != null ? request.getRequesterName() : "N/A"));
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 200, yPosition);
                    contentStream.showText("Created: " + (request.getCreatedAt() != null ? request.getCreatedAt().format(dtf) : "N/A"));
                    contentStream.endText();
                    
                    // SLA info
                    contentStream.beginText();
                    if (request.isOverdue()) {
                        contentStream.setNonStrokingColor(220, 53, 69); // Red for overdue
                    }
                    contentStream.newLineAtOffset(margin + 350, yPosition);
                    contentStream.showText(request.isOverdue() ? "OVERDUE" : "SLA: " + request.getHoursUntilSla() + "h");
                    contentStream.endText();
                    
                    contentStream.setNonStrokingColor(0, 0, 0);
                    yPosition -= 35;
                }
                
                contentStream.close();
                document.save(filePath);
            }
            
            LOGGER.info("Work Requests PDF exported successfully: " + filePath);
            return filePath;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting work requests to PDF", e);
            return null;
        }
    }
    
    /**
     * Export all items to PDF
     * @param filePath Target file path
     * @return File path where PDF was saved
     */
    public String exportAllItemsToPDF(String filePath) {
        List<Item> items = itemDAO.findAll();
        return exportItemsToPDF(items, "All Items Report", filePath);
    }
    
    /**
     * Export all work requests to PDF
     * @param filePath Target file path
     * @return File path where PDF was saved
     */
    public String exportAllWorkRequestsToPDF(String filePath) {
        List<WorkRequest> requests = workRequestDAO.findAll();
        return exportWorkRequestsToPDF(requests, "All Work Requests Report", filePath);
    }
    
    /**
     * Export executive summary to PDF
     * @param filePath Target file path (optional)
     * @return File path where PDF was saved
     */
    public String exportExecutiveSummaryToPDF(String filePath) {
        ReportData report = generateExecutiveSummary();
        return exportReportToPDF(report, filePath);
    }
    
    /**
     * Export weekly report to PDF
     * @param filePath Target file path (optional)
     * @return File path where PDF was saved
     */
    public String exportWeeklyReportToPDF(String filePath) {
        ReportData report = generateWeeklyReport();
        return exportReportToPDF(report, filePath);
    }
    
    /**
     * Export monthly report to PDF
     * @param filePath Target file path (optional)
     * @return File path where PDF was saved
     */
    public String exportMonthlyReportToPDF(String filePath) {
        ReportData report = generateMonthlyReport();
        return exportReportToPDF(report, filePath);
    }
    
    /**
     * Helper method to draw table header for items PDF
     */
    private void drawTableHeader(PDPageContentStream contentStream, float margin, float yPosition, float pageWidth) throws IOException {
        // Header background
        contentStream.setNonStrokingColor(52, 73, 94); // Dark blue
        contentStream.addRect(margin, yPosition - 5, pageWidth - 2 * margin, 18);
        contentStream.fill();
        
        // Header text
        contentStream.setNonStrokingColor(255, 255, 255); // White
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 9);
        contentStream.newLineAtOffset(margin + 5, yPosition);
        contentStream.showText("Title");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 130, yPosition);
        contentStream.showText("Category");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 210, yPosition);
        contentStream.showText("Type");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 270, yPosition);
        contentStream.showText("Status");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 350, yPosition);
        contentStream.showText("Date");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(margin + 430, yPosition);
        contentStream.showText("Value");
        contentStream.endText();
        
        contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
    }
    
    /**
     * Helper method to truncate text for PDF
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        // Remove characters that cause issues in PDFBox
        text = text.replaceAll("[^\\x00-\\x7F]", ""); // Remove non-ASCII
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    // ====================================================================================
    // SCHEDULED REPORTS (PLACEHOLDER)
    // ====================================================================================
    
    /**
     * Schedule weekly report delivery (placeholder for future implementation)
     * @param email Email address for delivery
     */
    public void scheduleWeeklyReport(String email) {
        LOGGER.info("Weekly report scheduled for: " + email + " (placeholder - actual scheduling not implemented)");
        // TODO: Implement actual scheduling mechanism
    }
    
    /**
     * Schedule monthly report delivery (placeholder for future implementation)
     * @param email Email address for delivery
     */
    public void scheduleMonthlyReport(String email) {
        LOGGER.info("Monthly report scheduled for: " + email + " (placeholder - actual scheduling not implemented)");
        // TODO: Implement actual scheduling mechanism
    }
    
    // ====================================================================================
    // HELPER METHODS
    // ====================================================================================
    
    /**
     * Write data to CSV file
     */
    private boolean writeCSV(String filePath, List<String[]> rows) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(filePath), StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                writer.write(String.join(CSV_DELIMITER, row));
                writer.newLine();
            }
            LOGGER.info("CSV exported successfully: " + filePath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing CSV: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Create parent directories for a file path
     */
    private void createParentDirectories(String filePath) throws IOException {
        Path path = Path.of(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
    
    /**
     * Escape string for CSV (handle commas, quotes, newlines)
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        // If contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Safe string conversion (null-safe)
     */
    private String safeString(String value) {
        return value != null ? escapeCSV(value) : "";
    }
    
    /**
     * Get enterprise name from ID
     */
    private String getEnterpriseName(String enterpriseId) {
        if (enterpriseId == null || enterpriseId.isEmpty()) return "";
        try {
            return enterpriseItemService.getEnterpriseName(enterpriseId);
        } catch (Exception e) {
            return enterpriseId;
        }
    }
    
    /**
     * Get organization name from ID
     */
    private String getOrganizationName(String organizationId) {
        if (organizationId == null || organizationId.isEmpty()) return "";
        try {
            return enterpriseItemService.getOrganizationName(organizationId);
        } catch (Exception e) {
            return organizationId;
        }
    }
    
    /**
     * Center a string within a specified width
     */
    private String centerString(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        int padding = (width - s.length()) / 2;
        return " ".repeat(padding) + s + " ".repeat(width - s.length() - padding);
    }
    
    /**
     * Pad string to the right
     */
    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
    
    /**
     * Create an error report
     */
    private ReportData createErrorReport(String reportType, String errorMessage) {
        ReportData report = new ReportData();
        report.title = reportType + " - Error";
        report.generatedAt = LocalDateTime.now();
        report.reportType = ReportType.CUSTOM;
        
        ReportSection error = new ReportSection("Error");
        error.addMetric("Error Message", errorMessage);
        error.addMetric("Timestamp", LocalDateTime.now().toString());
        report.sections.add(error);
        
        return report;
    }
    
    // ====================================================================================
    // INNER CLASSES
    // ====================================================================================
    
    /**
     * Report data container
     */
    public static class ReportData {
        public String title;
        public LocalDateTime generatedAt;
        public ReportType reportType;
        public LocalDate periodStart;
        public LocalDate periodEnd;
        public List<ReportSection> sections = new ArrayList<>();
        public List<ChartData> charts = new ArrayList<>();
        public Map<String, Object> rawData = new HashMap<>();
        
        /**
         * Get report as formatted string
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(title).append(" ===\n");
            sb.append("Generated: ").append(generatedAt.format(DateTimeFormatter.ofPattern(DATE_FORMAT))).append("\n");
            if (periodStart != null && periodEnd != null) {
                sb.append("Period: ").append(periodStart).append(" to ").append(periodEnd).append("\n");
            }
            sb.append("\n");
            
            for (ReportSection section : sections) {
                sb.append("--- ").append(section.title).append(" ---\n");
                for (Map.Entry<String, String> metric : section.metrics.entrySet()) {
                    sb.append("  ").append(metric.getKey()).append(": ").append(metric.getValue()).append("\n");
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
        
        /**
         * Get report as HTML
         */
        public String toHtml() {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>").append(title).append("</title>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            html.append("h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
            html.append("h2 { color: #555; margin-top: 20px; }");
            html.append(".section { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 5px; }");
            html.append(".metric { margin: 5px 0; }");
            html.append(".metric-name { font-weight: bold; color: #333; }");
            html.append(".metric-value { color: #007bff; }");
            html.append(".footer { margin-top: 30px; font-size: 12px; color: #999; }");
            html.append("</style></head><body>");
            
            html.append("<h1>").append(title).append("</h1>");
            html.append("<p>Generated: ").append(generatedAt.format(DateTimeFormatter.ofPattern(DATE_FORMAT))).append("</p>");
            if (periodStart != null && periodEnd != null) {
                html.append("<p>Period: ").append(periodStart).append(" to ").append(periodEnd).append("</p>");
            }
            
            for (ReportSection section : sections) {
                html.append("<div class='section'>");
                html.append("<h2>").append(section.title).append("</h2>");
                for (Map.Entry<String, String> metric : section.metrics.entrySet()) {
                    html.append("<div class='metric'>");
                    html.append("<span class='metric-name'>").append(metric.getKey()).append(":</span> ");
                    html.append("<span class='metric-value'>").append(metric.getValue()).append("</span>");
                    html.append("</div>");
                }
                html.append("</div>");
            }
            
            html.append("<div class='footer'>Greater Boston Lost & Found Recovery Ecosystem</div>");
            html.append("</body></html>");
            
            return html.toString();
        }
        
        @Override
        public String toString() {
            return toFormattedString();
        }
    }
    
    /**
     * Report section with metrics
     */
    public static class ReportSection {
        public String title;
        public Map<String, String> metrics = new LinkedHashMap<>();
        public String description;
        
        public ReportSection(String title) {
            this.title = title;
        }
        
        public void addMetric(String name, String value) {
            metrics.put(name, value);
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getMetric(String name) {
            return metrics.get(name);
        }
        
        @Override
        public String toString() {
            return "ReportSection{title='" + title + "', metrics=" + metrics.size() + "}";
        }
    }
    
    /**
     * Chart data for reports
     */
    public static class ChartData {
        public String chartType; // "pie", "bar", "line"
        public String title;
        public List<ChartDataPoint> dataPoints = new ArrayList<>();
        
        public ChartData(String chartType, String title) {
            this.chartType = chartType;
            this.title = title;
        }
        
        public void addDataPoint(ChartDataPoint point) {
            dataPoints.add(point);
        }
    }
    
    /**
     * Report type enumeration
     */
    public enum ReportType {
        EXECUTIVE_SUMMARY,
        ENTERPRISE,
        WEEKLY,
        MONTHLY,
        CUSTOM
    }
    
    /**
     * Criteria for custom reports
     */
    public static class ReportCriteria {
        public String title;
        public LocalDate startDate;
        public LocalDate endDate;
        public List<String> enterpriseIds;
        public List<ItemCategory> categories;
        public boolean includeCategories = true;
        public boolean includeEnterprises = true;
        public boolean includeValueAnalysis = true;
        public boolean includeUserStats = false;
        public boolean includeWorkRequests = false;
        
        public static ReportCriteria builder() {
            return new ReportCriteria();
        }
        
        public ReportCriteria withTitle(String title) {
            this.title = title;
            return this;
        }
        
        public ReportCriteria withDateRange(LocalDate start, LocalDate end) {
            this.startDate = start;
            this.endDate = end;
            return this;
        }
        
        public ReportCriteria withEnterprises(List<String> enterpriseIds) {
            this.enterpriseIds = enterpriseIds;
            return this;
        }
        
        public ReportCriteria withCategories(List<ItemCategory> categories) {
            this.categories = categories;
            return this;
        }
        
        public ReportCriteria includeCategories(boolean include) {
            this.includeCategories = include;
            return this;
        }
        
        public ReportCriteria includeEnterprises(boolean include) {
            this.includeEnterprises = include;
            return this;
        }
        
        public ReportCriteria includeValueAnalysis(boolean include) {
            this.includeValueAnalysis = include;
            return this;
        }
        
        public ReportCriteria includeUserStats(boolean include) {
            this.includeUserStats = include;
            return this;
        }
        
        public ReportCriteria includeWorkRequests(boolean include) {
            this.includeWorkRequests = include;
            return this;
        }
    }
}
