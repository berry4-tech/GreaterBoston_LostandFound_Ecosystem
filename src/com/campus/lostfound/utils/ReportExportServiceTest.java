package com.campus.lostfound.utils;

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.models.Item.*;
import com.campus.lostfound.models.workrequest.WorkRequest;
import com.campus.lostfound.services.*;
import com.campus.lostfound.services.AnalyticsService.*;
import com.campus.lostfound.services.ReportExportService.*;

import java.io.File;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Test class for ReportExportService.
 * 
 * Tests CSV export functionality, report generation, and export utilities.
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class ReportExportServiceTest {
    
    private static ReportExportService reportService;
    private static AnalyticsService analyticsService;
    private static MongoItemDAO itemDAO;
    private static MongoUserDAO userDAO;
    private static MongoWorkRequestDAO workRequestDAO;
    
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    private static final String TEST_EXPORT_DIR = "test_exports";
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         ReportExportService Test Suite - Developer 4            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            setup();
            
            // Run test categories
            System.out.println("═══════════════════════════════════════════════════════════════════");
            System.out.println("PHASE 1: Export Utilities Tests");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            testExportUtilities();
            
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("PHASE 2: CSV Export Tests");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            testCSVExports();
            
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("PHASE 3: Report Generation Tests");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            testReportGeneration();
            
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("PHASE 4: Report Export Tests");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            testReportExport();
            
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("PHASE 5: Custom Report Tests");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            testCustomReports();
            
        } catch (Exception e) {
            System.err.println("Test suite failed with exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
            printSummary();
        }
    }
    
    private static void setup() {
        System.out.println("Setting up test environment...");
        
        // Initialize services
        reportService = new ReportExportService();
        analyticsService = new AnalyticsService();
        itemDAO = new MongoItemDAO();
        userDAO = new MongoUserDAO();
        workRequestDAO = new MongoWorkRequestDAO();
        
        // Create test export directory
        File testDir = new File(TEST_EXPORT_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        
        System.out.println("✓ Services initialized");
        System.out.println("✓ Test export directory: " + testDir.getAbsolutePath());
        System.out.println();
    }
    
    // ==================== EXPORT UTILITIES TESTS ====================
    
    private static void testExportUtilities() {
        System.out.println("\n[TEST GROUP: Export Utilities]");
        
        // Test sanitize filename
        test("Sanitize filename - remove invalid chars", () -> {
            String result = reportService.sanitizeFileName("Report:Test<>File");
            return !result.contains(":") && !result.contains("<") && !result.contains(">");
        });
        
        test("Sanitize filename - handle null", () -> {
            String result = reportService.sanitizeFileName(null);
            return "unnamed".equals(result);
        });
        
        test("Sanitize filename - limit length", () -> {
            String longName = "A".repeat(200);
            String result = reportService.sanitizeFileName(longName);
            return result.length() <= 100;
        });
        
        // Test unique filename generation
        test("Generate unique filename", () -> {
            String filename1 = reportService.generateUniqueFilename("test", "csv");
            Thread.sleep(10); // Small delay to ensure different timestamp
            String filename2 = reportService.generateUniqueFilename("test", "csv");
            return filename1.startsWith("test_") && filename1.endsWith(".csv") && !filename1.equals(filename2);
        });
        
        // Test directory creation
        test("Create export directory", () -> {
            File dir = reportService.createExportDirectory(TEST_EXPORT_DIR);
            return dir.exists() && dir.isDirectory();
        });
        
        // Test format for export
        test("Format date for export", () -> {
            Date date = new Date();
            String result = reportService.formatForExport(date);
            return result != null && !result.isEmpty() && result.contains("-");
        });
        
        test("Format null for export", () -> {
            String result = reportService.formatForExport(null);
            return "".equals(result);
        });
        
        test("Format double for export", () -> {
            String result = reportService.formatForExport(123.456);
            return result.contains("123.46") || result.contains("123,46");
        });
        
        // Test default export path
        test("Get default export path", () -> {
            String path = reportService.getDefaultExportPath();
            return path != null && !path.isEmpty();
        });
        
        // Test export file path
        test("Get export file path", () -> {
            String path = reportService.getExportFilePath("test.csv");
            return path != null && path.endsWith("test.csv");
        });
    }
    
    // ==================== CSV EXPORT TESTS ====================
    
    private static void testCSVExports() {
        System.out.println("\n[TEST GROUP: CSV Exports]");
        
        // Test item export
        test("Export items to CSV", () -> {
            List<Item> items = itemDAO.findAll();
            if (items.isEmpty()) {
                System.out.println("    (No items in database - creating test data)");
                // Continue anyway, empty export should still work
            }
            
            String filePath = TEST_EXPORT_DIR + "/items_export.csv";
            boolean result = reportService.exportItemsToCSV(items, filePath);
            
            if (result) {
                File file = new File(filePath);
                System.out.println("    Items exported: " + items.size());
                System.out.println("    File size: " + file.length() + " bytes");
                return file.exists() && file.length() > 0;
            }
            return false;
        });
        
        // Test all items export
        test("Export all items to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/all_items_export.csv";
            boolean result = reportService.exportAllItemsToCSV(filePath);
            return result && new File(filePath).exists();
        });
        
        // Test user export
        test("Export users to CSV", () -> {
            List<User> users = userDAO.findAll();
            String filePath = TEST_EXPORT_DIR + "/users_export.csv";
            boolean result = reportService.exportUsersToCSV(users, filePath);
            
            if (result) {
                System.out.println("    Users exported: " + users.size());
                return new File(filePath).exists();
            }
            return false;
        });
        
        // Test all users export
        test("Export all users to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/all_users_export.csv";
            boolean result = reportService.exportAllUsersToCSV(filePath);
            return result && new File(filePath).exists();
        });
        
        // Test work requests export
        test("Export work requests to CSV", () -> {
            List<WorkRequest> requests = workRequestDAO.findAll();
            String filePath = TEST_EXPORT_DIR + "/work_requests_export.csv";
            boolean result = reportService.exportWorkRequestsToCSV(requests, filePath);
            
            if (result) {
                System.out.println("    Requests exported: " + requests.size());
                return new File(filePath).exists();
            }
            return false;
        });
        
        // Test all work requests export
        test("Export all work requests to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/all_work_requests_export.csv";
            boolean result = reportService.exportAllWorkRequestsToCSV(filePath);
            return result && new File(filePath).exists();
        });
        
        // Test analytics export
        test("Export analytics to CSV", () -> {
            ExecutiveSummary summary = analyticsService.getExecutiveSummary();
            String filePath = TEST_EXPORT_DIR + "/analytics_export.csv";
            boolean result = reportService.exportAnalyticsToCSV(summary, filePath);
            
            if (result) {
                System.out.println("    Analytics metrics exported");
                return new File(filePath).exists();
            }
            return false;
        });
        
        // Test enterprise stats export
        test("Export enterprise stats to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/enterprise_stats_export.csv";
            boolean result = reportService.exportEnterpriseStatsToCSV(filePath);
            return result && new File(filePath).exists();
        });
        
        // Test weekly trends export
        test("Export weekly trends to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/weekly_trends_export.csv";
            boolean result = reportService.exportWeeklyTrendsToCSV(8, filePath);
            return result && new File(filePath).exists();
        });
        
        // Test monthly trends export
        test("Export monthly trends to CSV", () -> {
            String filePath = TEST_EXPORT_DIR + "/monthly_trends_export.csv";
            boolean result = reportService.exportMonthlyTrendsToCSV(6, filePath);
            return result && new File(filePath).exists();
        });
        
        // Test custom query export
        test("Export custom query to CSV", () -> {
            String[] headers = {"Name", "Value", "Category"};
            List<Object[]> data = new ArrayList<>();
            data.add(new Object[] {"Test1", 100, "Cat A"});
            data.add(new Object[] {"Test2", 200, "Cat B"});
            data.add(new Object[] {"Test3", 300, "Cat C"});
            
            String filePath = TEST_EXPORT_DIR + "/custom_query_export.csv";
            boolean result = reportService.exportCustomQueryToCSV(headers, data, filePath);
            
            if (result) {
                // Verify file content
                try {
                    String content = Files.readString(Path.of(filePath));
                    return content.contains("Name") && content.contains("Test1") && content.contains("Cat B");
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        });
    }
    
    // ==================== REPORT GENERATION TESTS ====================
    
    private static void testReportGeneration() {
        System.out.println("\n[TEST GROUP: Report Generation]");
        
        // Test executive summary generation
        test("Generate executive summary report", () -> {
            ReportData report = reportService.generateExecutiveSummary();
            
            System.out.println("    Title: " + report.title);
            System.out.println("    Sections: " + report.sections.size());
            
            boolean hasOverview = report.sections.stream()
                .anyMatch(s -> s.title.contains("Overview"));
            boolean hasPerformance = report.sections.stream()
                .anyMatch(s -> s.title.contains("Performance"));
            
            return report.title != null && 
                   report.generatedAt != null && 
                   report.sections.size() > 0 &&
                   hasOverview && hasPerformance;
        });
        
        // Test weekly report generation
        test("Generate weekly report", () -> {
            ReportData report = reportService.generateWeeklyReport();
            
            System.out.println("    Title: " + report.title);
            System.out.println("    Period: " + report.periodStart + " to " + report.periodEnd);
            System.out.println("    Sections: " + report.sections.size());
            
            boolean hasWeeklySummary = report.sections.stream()
                .anyMatch(s -> s.title.contains("Week"));
            
            return report.title.contains("Weekly") && 
                   report.periodStart != null &&
                   report.periodEnd != null &&
                   hasWeeklySummary;
        });
        
        // Test monthly report generation
        test("Generate monthly report", () -> {
            ReportData report = reportService.generateMonthlyReport();
            
            System.out.println("    Title: " + report.title);
            System.out.println("    Sections: " + report.sections.size());
            
            boolean hasTrend = report.sections.stream()
                .anyMatch(s -> s.title.contains("Trend") || s.title.contains("Month"));
            boolean hasYoY = report.sections.stream()
                .anyMatch(s -> s.title.contains("Year"));
            
            return report.title.contains("Monthly") && 
                   hasTrend && hasYoY;
        });
        
        // Test enterprise report generation
        test("Generate enterprise report", () -> {
            // Get first enterprise ID
            List<Enterprise> enterprises = new MongoEnterpriseDAO().findAll();
            if (enterprises.isEmpty()) {
                System.out.println("    (No enterprises in database - skipping)");
                return true; // Pass if no data
            }
            
            String enterpriseId = enterprises.get(0).getEnterpriseId();
            ReportData report = reportService.generateEnterpriseReport(enterpriseId);
            
            System.out.println("    Title: " + report.title);
            System.out.println("    Sections: " + report.sections.size());
            
            return report.title.contains("Enterprise") && 
                   report.sections.size() > 0;
        });
        
        // Test enterprise report with invalid ID
        test("Generate enterprise report - invalid ID", () -> {
            ReportData report = reportService.generateEnterpriseReport("invalid-id-12345");
            
            // Should return error report
            return report.title.contains("Error") || 
                   report.sections.stream().anyMatch(s -> s.title.contains("Error"));
        });
        
        // Test report data methods
        test("Report data to formatted string", () -> {
            ReportData report = reportService.generateWeeklyReport();
            String formatted = report.toFormattedString();
            
            return formatted != null && 
                   formatted.contains(report.title) &&
                   formatted.length() > 100;
        });
        
        test("Report data to HTML", () -> {
            ReportData report = reportService.generateWeeklyReport();
            String html = report.toHtml();
            
            return html != null && 
                   html.contains("<html>") &&
                   html.contains(report.title) &&
                   html.contains("</html>");
        });
    }
    
    // ==================== REPORT EXPORT TESTS ====================
    
    private static void testReportExport() {
        System.out.println("\n[TEST GROUP: Report Export]");
        
        // Test export report to CSV
        test("Export report to CSV", () -> {
            ReportData report = reportService.generateWeeklyReport();
            String filePath = reportService.exportReportToCSV(report, 
                TEST_EXPORT_DIR + "/weekly_report.csv");
            
            if (filePath != null) {
                System.out.println("    Exported to: " + filePath);
                return new File(filePath).exists();
            }
            return false;
        });
        
        // Test export report to CSV with auto-generated filename
        test("Export report to CSV - auto filename", () -> {
            ReportData report = reportService.generateExecutiveSummary();
            String filePath = reportService.exportReportToCSV(report, null);
            
            if (filePath != null) {
                System.out.println("    Auto-generated path: " + filePath);
                return new File(filePath).exists();
            }
            return false;
        });
        
        // Test export report to text
        test("Export report to text", () -> {
            ReportData report = reportService.generateMonthlyReport();
            String filePath = reportService.exportReportToText(report, 
                TEST_EXPORT_DIR + "/monthly_report.txt");
            
            if (filePath != null) {
                System.out.println("    Exported to: " + filePath);
                
                // Verify content
                try {
                    String content = Files.readString(Path.of(filePath));
                    return content.contains(report.title) && 
                           content.contains("Greater Boston");
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        });
        
        // Test export report to text with auto filename
        test("Export report to text - auto filename", () -> {
            ReportData report = reportService.generateWeeklyReport();
            String filePath = reportService.exportReportToText(report, null);
            
            if (filePath != null) {
                System.out.println("    Auto-generated path: " + filePath);
                return new File(filePath).exists();
            }
            return false;
        });
    }
    
    // ==================== CUSTOM REPORT TESTS ====================
    
    private static void testCustomReports() {
        System.out.println("\n[TEST GROUP: Custom Reports]");
        
        // Test custom report with date range
        test("Generate custom report - date range", () -> {
            ReportCriteria criteria = ReportCriteria.builder()
                .withTitle("Last 30 Days Report")
                .withDateRange(LocalDate.now().minusDays(30), LocalDate.now())
                .includeCategories(true)
                .includeEnterprises(true)
                .includeValueAnalysis(true);
            
            ReportData report = reportService.generateCustomReport(criteria);
            
            System.out.println("    Title: " + report.title);
            System.out.println("    Sections: " + report.sections.size());
            
            return report.title.contains("30 Days") &&
                   report.sections.stream().anyMatch(s -> s.title.contains("Category")) &&
                   report.sections.stream().anyMatch(s -> s.title.contains("Value"));
        });
        
        // Test custom report with enterprise filter
        test("Generate custom report - enterprise filter", () -> {
            List<Enterprise> enterprises = new MongoEnterpriseDAO().findAll();
            if (enterprises.isEmpty()) {
                System.out.println("    (No enterprises - skipping)");
                return true;
            }
            
            List<String> entIds = Arrays.asList(enterprises.get(0).getEnterpriseId());
            
            ReportCriteria criteria = ReportCriteria.builder()
                .withTitle("Enterprise Specific Report")
                .withEnterprises(entIds)
                .includeEnterprises(true);
            
            ReportData report = reportService.generateCustomReport(criteria);
            
            return report.title.contains("Enterprise") && 
                   report.sections.size() > 0;
        });
        
        // Test custom report with category filter
        test("Generate custom report - category filter", () -> {
            List<ItemCategory> categories = Arrays.asList(
                ItemCategory.ELECTRONICS, 
                ItemCategory.CLOTHING
            );
            
            ReportCriteria criteria = ReportCriteria.builder()
                .withTitle("Electronics and Clothing Report")
                .withCategories(categories)
                .includeCategories(true);
            
            ReportData report = reportService.generateCustomReport(criteria);
            
            return report.title.contains("Electronics") && 
                   report.sections.size() > 0;
        });
        
        // Test custom report - all options disabled
        test("Generate custom report - minimal options", () -> {
            ReportCriteria criteria = new ReportCriteria();
            criteria.title = "Minimal Report";
            criteria.includeCategories = false;
            criteria.includeEnterprises = false;
            criteria.includeValueAnalysis = false;
            
            ReportData report = reportService.generateCustomReport(criteria);
            
            // Should still have summary section
            boolean hasSummary = report.sections.stream()
                .anyMatch(s -> s.title.contains("Summary") || s.title.contains("Statistics"));
            
            return report.title.equals("Minimal Report") && hasSummary;
        });
        
        // Test report criteria builder
        test("Report criteria builder pattern", () -> {
            ReportCriteria criteria = ReportCriteria.builder()
                .withTitle("Builder Test")
                .withDateRange(LocalDate.now().minusDays(7), LocalDate.now())
                .includeCategories(true)
                .includeEnterprises(true)
                .includeValueAnalysis(true)
                .includeUserStats(true)
                .includeWorkRequests(true);
            
            return "Builder Test".equals(criteria.title) &&
                   criteria.startDate != null &&
                   criteria.includeCategories &&
                   criteria.includeUserStats;
        });
        
        // Test export custom report
        test("Export custom report to CSV", () -> {
            ReportCriteria criteria = ReportCriteria.builder()
                .withTitle("Export Test Report")
                .withDateRange(LocalDate.now().minusDays(14), LocalDate.now());
            
            ReportData report = reportService.generateCustomReport(criteria);
            String filePath = reportService.exportReportToCSV(report, 
                TEST_EXPORT_DIR + "/custom_report.csv");
            
            return filePath != null && new File(filePath).exists();
        });
    }
    
    // ==================== TEST HELPERS ====================
    
    private static void test(String name, TestCase testCase) {
        testsRun++;
        System.out.print("  • " + name + "... ");
        
        try {
            boolean result = testCase.run();
            if (result) {
                testsPassed++;
                System.out.println("✓ PASSED");
            } else {
                testsFailed++;
                System.out.println("✗ FAILED");
            }
        } catch (Exception e) {
            testsFailed++;
            System.out.println("✗ FAILED (Exception: " + e.getMessage() + ")");
            e.printStackTrace();
        }
    }
    
    @FunctionalInterface
    interface TestCase {
        boolean run() throws Exception;
    }
    
    private static void cleanup() {
        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("CLEANUP");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        
        // List exported files
        File testDir = new File(TEST_EXPORT_DIR);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null && files.length > 0) {
                System.out.println("Exported files in " + TEST_EXPORT_DIR + ":");
                long totalSize = 0;
                for (File file : files) {
                    System.out.println("  • " + file.getName() + " (" + file.length() + " bytes)");
                    totalSize += file.length();
                }
                System.out.println("Total: " + files.length + " files, " + totalSize + " bytes");
            }
        }
        
        System.out.println("\nNote: Test export files preserved in: " + testDir.getAbsolutePath());
    }
    
    private static void printSummary() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      TEST RESULTS SUMMARY                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Total Tests:  %-5d                                             ║%n", testsRun);
        System.out.printf("║  Passed:       %-5d (%5.1f%%)                                    ║%n", 
            testsPassed, (double) testsPassed / testsRun * 100);
        System.out.printf("║  Failed:       %-5d (%5.1f%%)                                    ║%n", 
            testsFailed, (double) testsFailed / testsRun * 100);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        if (testsFailed == 0) {
            System.out.println("║                    ✓ ALL TESTS PASSED! ✓                        ║");
            System.out.println("║                                                                  ║");
            System.out.println("║  Part 7: ReportExportService - COMPLETE                         ║");
        } else {
            System.out.println("║               ✗ SOME TESTS FAILED - REVIEW NEEDED               ║");
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
