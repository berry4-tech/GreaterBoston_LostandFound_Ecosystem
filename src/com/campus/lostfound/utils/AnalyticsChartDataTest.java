package com.campus.lostfound.utils;

import com.campus.lostfound.services.AnalyticsService;
import com.campus.lostfound.services.AnalyticsService.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Test class for AnalyticsService Part 6: Chart Data Methods
 * 
 * Tests all chart data generation methods including:
 * - Pie chart data (status, type, category, enterprise, etc.)
 * - Bar chart data with grouping dimensions
 * - Time series data for line charts
 * - Stacked bar chart data
 * - Enterprise comparison
 * - Period comparison
 * - Network benchmarking
 * 
 * @author Developer 4 - Cross-Enterprise Integration Lead
 */
public class AnalyticsChartDataTest {
    
    private static AnalyticsService analyticsService;
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     AnalyticsService Part 6: Chart Data Methods Test Suite       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
        
        try {
            analyticsService = new AnalyticsService();
            System.out.println("✓ AnalyticsService initialized successfully\n");
            
            // Run test groups
            testPieChartData();
            testBarChartData();
            testTimeSeriesData();
            testStackedBarData();
            testEnterpriseComparison();
            testPeriodComparison();
            testNetworkBenchmark();
            testHelperMethods();
            
            // Print summary
            printSummary();
            
        } catch (Exception e) {
            System.err.println("✗ Critical error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== PIE CHART DATA TESTS ====================
    
    private static void testPieChartData() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Pie Chart Data Methods");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Test Item Status Pie Data
        runTest("getItemStatusPieData", () -> {
            List<ChartDataPoint> data = analyticsService.getItemStatusPieData();
            assertNotNull(data, "Item status pie data should not be null");
            System.out.println("   Found " + data.size() + " status categories");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue() + 
                    " (Color: " + colorToHex(dp.color) + ")");
            }
            return true;
        });
        
        // Test Item Type Pie Data
        runTest("getItemTypePieData", () -> {
            List<ChartDataPoint> data = analyticsService.getItemTypePieData();
            assertNotNull(data, "Item type pie data should not be null");
            System.out.println("   Found " + data.size() + " item types");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue());
            }
            return true;
        });
        
        // Test Category Pie Data
        runTest("getCategoryPieData", () -> {
            List<ChartDataPoint> data = analyticsService.getCategoryPieData();
            assertNotNull(data, "Category pie data should not be null");
            System.out.println("   Found " + data.size() + " categories");
            if (!data.isEmpty()) {
                System.out.println("   Top 3 categories:");
                for (int i = 0; i < Math.min(3, data.size()); i++) {
                    System.out.println("   • " + data.get(i).label + ": " + data.get(i).getFormattedValue());
                }
            }
            return true;
        });
        
        // Test Enterprise Pie Data
        runTest("getEnterprisePieData", () -> {
            List<ChartDataPoint> data = analyticsService.getEnterprisePieData();
            assertNotNull(data, "Enterprise pie data should not be null");
            System.out.println("   Found " + data.size() + " enterprises");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue() + " items");
            }
            return true;
        });
        
        // Test User Role Pie Data
        runTest("getUserRolePieData", () -> {
            List<ChartDataPoint> data = analyticsService.getUserRolePieData();
            assertNotNull(data, "User role pie data should not be null");
            System.out.println("   Found " + data.size() + " user roles");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue() + " users");
            }
            return true;
        });
        
        // Test Request Type Pie Data
        runTest("getRequestTypePieData", () -> {
            List<ChartDataPoint> data = analyticsService.getRequestTypePieData();
            assertNotNull(data, "Request type pie data should not be null");
            System.out.println("   Found " + data.size() + " request types");
            return true;
        });
        
        // Test Request Status Pie Data
        runTest("getRequestStatusPieData", () -> {
            List<ChartDataPoint> data = analyticsService.getRequestStatusPieData();
            assertNotNull(data, "Request status pie data should not be null");
            System.out.println("   Found " + data.size() + " request statuses");
            return true;
        });
        
        // Test Trust Score Pie Data
        runTest("getTrustScorePieData", () -> {
            List<ChartDataPoint> data = analyticsService.getTrustScorePieData();
            assertNotNull(data, "Trust score pie data should not be null");
            System.out.println("   Found " + data.size() + " trust score levels");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue() + " users");
            }
            return true;
        });
        
        // Test Generic Pie Chart Data Method
        runTest("getPieChartData(ChartMetric)", () -> {
            for (ChartMetric metric : new ChartMetric[]{
                ChartMetric.ITEM_STATUS, ChartMetric.ITEM_TYPE, 
                ChartMetric.ITEM_CATEGORY, ChartMetric.ENTERPRISE
            }) {
                List<ChartDataPoint> data = analyticsService.getPieChartData(metric);
                assertNotNull(data, "Pie data for " + metric + " should not be null");
                System.out.println("   " + metric + ": " + data.size() + " data points");
            }
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== BAR CHART DATA TESTS ====================
    
    private static void testBarChartData() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Bar Chart Data Methods");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Test Category Bar Data
        runTest("getCategoryBarData", () -> {
            List<ChartDataPoint> data = analyticsService.getCategoryBarData();
            assertNotNull(data, "Category bar data should not be null");
            System.out.println("   Found " + data.size() + " categories for bar chart");
            return true;
        });
        
        // Test Enterprise Comparison Data
        runTest("getEnterpriseComparisonData", () -> {
            List<ChartDataPoint> data = analyticsService.getEnterpriseComparisonData();
            assertNotNull(data, "Enterprise comparison data should not be null");
            System.out.println("   Enterprise item counts:");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + dp.getFormattedValue());
            }
            return true;
        });
        
        // Test Bar Chart Data with Grouping
        runTest("getBarChartData(ITEM_COUNT, CATEGORY)", () -> {
            List<ChartDataPoint> data = analyticsService.getBarChartData(
                ChartMetric.ITEM_COUNT, GroupByDimension.CATEGORY);
            assertNotNull(data, "Bar chart data should not be null");
            System.out.println("   Items by category: " + data.size() + " groups");
            return true;
        });
        
        runTest("getBarChartData(RECOVERY_RATE, ENTERPRISE)", () -> {
            List<ChartDataPoint> data = analyticsService.getBarChartData(
                ChartMetric.RECOVERY_RATE, GroupByDimension.ENTERPRISE);
            assertNotNull(data, "Recovery rate bar data should not be null");
            System.out.println("   Recovery rates by enterprise:");
            for (ChartDataPoint dp : data) {
                System.out.println("   • " + dp.label + ": " + String.format("%.1f%%", dp.value));
            }
            return true;
        });
        
        runTest("getBarChartData(USER_COUNT, ENTERPRISE)", () -> {
            List<ChartDataPoint> data = analyticsService.getBarChartData(
                ChartMetric.USER_COUNT, GroupByDimension.ENTERPRISE);
            assertNotNull(data, "User count bar data should not be null");
            System.out.println("   Users by enterprise: " + data.size() + " groups");
            return true;
        });
        
        runTest("getBarChartData(REQUEST_COUNT, TYPE)", () -> {
            List<ChartDataPoint> data = analyticsService.getBarChartData(
                ChartMetric.REQUEST_COUNT, GroupByDimension.TYPE);
            assertNotNull(data, "Request count bar data should not be null");
            System.out.println("   Requests by type: " + data.size() + " types");
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== TIME SERIES DATA TESTS ====================
    
    private static void testTimeSeriesData() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Time Series (Line Chart) Data Methods");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Test Recovery Trend Data
        runTest("getRecoveryTrendData(30)", () -> {
            List<TimeSeriesPoint> data = analyticsService.getRecoveryTrendData(30);
            assertNotNull(data, "Recovery trend data should not be null");
            assertEquals(30, data.size(), "Should have 30 days of data");
            System.out.println("   Recovery trend: " + data.size() + " days");
            
            long total = data.stream().mapToLong(d -> d.value).sum();
            System.out.println("   Total recoveries: " + total);
            
            // Show last 7 days
            System.out.println("   Last 7 days:");
            for (int i = Math.max(0, data.size() - 7); i < data.size(); i++) {
                TimeSeriesPoint tsp = data.get(i);
                System.out.println("   • " + tsp.getShortDateLabel() + ": " + tsp.value);
            }
            return true;
        });
        
        // Test Items Reported Trend
        runTest("getItemsReportedTrendData(14)", () -> {
            List<TimeSeriesPoint> data = analyticsService.getItemsReportedTrendData(14);
            assertNotNull(data, "Items reported trend should not be null");
            System.out.println("   Items reported over 14 days: " + data.size() + " points");
            return true;
        });
        
        // Test Request Volume Trend
        runTest("getRequestVolumeTrendData(7)", () -> {
            List<TimeSeriesPoint> data = analyticsService.getRequestVolumeTrendData(7);
            assertNotNull(data, "Request volume trend should not be null");
            assertEquals(7, data.size(), "Should have 7 days of data");
            System.out.println("   Request volume trend: " + data.size() + " days");
            return true;
        });
        
        // Test Active Users Trend
        runTest("getActiveUsersTrendData(14)", () -> {
            List<TimeSeriesPoint> data = analyticsService.getActiveUsersTrendData(14);
            assertNotNull(data, "Active users trend should not be null");
            System.out.println("   Active users trend: " + data.size() + " days");
            return true;
        });
        
        // Test Recovery Rate Trend
        runTest("getRecoveryRateTrendData(28)", () -> {
            List<TimeSeriesPoint> data = analyticsService.getRecoveryRateTrendData(28);
            assertNotNull(data, "Recovery rate trend should not be null");
            System.out.println("   Recovery rate trend: " + data.size() + " weeks");
            for (TimeSeriesPoint tsp : data) {
                System.out.println("   • " + tsp.date + ": " + tsp.value + "%");
            }
            return true;
        });
        
        // Test Generic Line Chart Method
        runTest("getLineChartData(ChartMetric, days)", () -> {
            for (ChartMetric metric : new ChartMetric[]{
                ChartMetric.ITEMS_REPORTED, ChartMetric.RECOVERIES, ChartMetric.ACTIVE_USERS
            }) {
                List<TimeSeriesPoint> data = analyticsService.getLineChartData(metric, 7);
                assertNotNull(data, "Line data for " + metric + " should not be null");
                System.out.println("   " + metric + ": " + data.size() + " points");
            }
            return true;
        });
        
        // Test alias methods
        runTest("getUserGrowthData / getRequestVolumeData aliases", () -> {
            List<TimeSeriesPoint> userGrowth = analyticsService.getUserGrowthData(7);
            List<TimeSeriesPoint> requestVolume = analyticsService.getRequestVolumeData(7);
            assertNotNull(userGrowth, "User growth data should not be null");
            assertNotNull(requestVolume, "Request volume data should not be null");
            System.out.println("   Alias methods working correctly");
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== STACKED BAR DATA TESTS ====================
    
    private static void testStackedBarData() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Stacked Bar Chart Data Methods");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Test Stacked by Enterprise
        runTest("getStackedBarData by ENTERPRISE", () -> {
            List<ChartMetric> metrics = Arrays.asList(
                ChartMetric.LOST_ITEMS, ChartMetric.FOUND_ITEMS, ChartMetric.CLAIMED_ITEMS
            );
            
            List<StackedDataPoint> data = analyticsService.getStackedBarData(
                metrics, GroupByDimension.ENTERPRISE);
            
            assertNotNull(data, "Stacked enterprise data should not be null");
            System.out.println("   Stacked data by enterprise: " + data.size() + " enterprises");
            
            for (StackedDataPoint sdp : data) {
                System.out.println("   • " + sdp.label + " (Total: " + String.format("%.0f", sdp.getTotal()) + ")");
                for (String series : sdp.getSeriesNames()) {
                    System.out.println("     - " + series + ": " + String.format("%.0f", sdp.getValue(series)));
                }
            }
            return true;
        });
        
        // Test Stacked by Week
        runTest("getStackedBarData by TIME_WEEK", () -> {
            List<ChartMetric> metrics = Arrays.asList(
                ChartMetric.LOST_ITEMS, ChartMetric.FOUND_ITEMS
            );
            
            List<StackedDataPoint> data = analyticsService.getStackedBarData(
                metrics, GroupByDimension.TIME_WEEK);
            
            assertNotNull(data, "Stacked weekly data should not be null");
            System.out.println("   Stacked data by week: " + data.size() + " weeks");
            
            if (!data.isEmpty()) {
                StackedDataPoint lastWeek = data.get(data.size() - 1);
                System.out.println("   Most recent week (" + lastWeek.label + "):");
                for (String series : lastWeek.getSeriesNames()) {
                    System.out.println("     - " + series + ": " + String.format("%.0f", lastWeek.getValue(series)));
                }
            }
            return true;
        });
        
        // Test StackedDataPoint methods
        runTest("StackedDataPoint utility methods", () -> {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("Series A", 100.0);
            values.put("Series B", 200.0);
            values.put("Series C", 150.0);
            
            StackedDataPoint sdp = new StackedDataPoint("Test Label", values);
            
            assertEquals(450.0, sdp.getTotal(), "Total should be 450");
            assertEquals(100.0, sdp.getValue("Series A"), "Series A should be 100");
            assertEquals(3, sdp.getSeriesNames().size(), "Should have 3 series");
            
            Object[] tableRow = sdp.toTableRow();
            assertEquals(4, tableRow.length, "Table row should have 4 columns");
            
            String[] columns = sdp.getTableColumns();
            assertEquals(4, columns.length, "Should have 4 column headers");
            
            System.out.println("   StackedDataPoint created successfully");
            System.out.println("   Total: " + sdp.getTotal());
            System.out.println("   Series: " + sdp.getSeriesNames());
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== ENTERPRISE COMPARISON TESTS ====================
    
    private static void testEnterpriseComparison() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Enterprise Comparison Analytics");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        runTest("compareEnterprises", () -> {
            // Get enterprise IDs from stats
            List<EnterpriseStats> stats = analyticsService.getEnterpriseStats();
            if (stats.size() < 2) {
                System.out.println("   ⚠ Need at least 2 enterprises for comparison test");
                return true;
            }
            
            List<String> enterpriseIds = new ArrayList<>();
            for (int i = 0; i < Math.min(3, stats.size()); i++) {
                enterpriseIds.add(stats.get(i).enterpriseId);
            }
            
            ComparisonResult result = analyticsService.compareEnterprises(enterpriseIds);
            assertNotNull(result, "Comparison result should not be null");
            
            System.out.println("   Comparing " + result.getEnterprises().size() + " enterprises");
            System.out.println();
            System.out.println(result.getSummary());
            
            // Test utility methods
            for (String enterprise : result.getEnterprises()) {
                Double items = result.getValue(enterprise, "Total Items");
                System.out.println("   " + enterprise + ": " + 
                    (items != null ? items.intValue() : 0) + " items");
            }
            
            return true;
        });
        
        runTest("ComparisonResult utility methods", () -> {
            ComparisonResult result = new ComparisonResult();
            
            // Add test data
            Map<String, Double> ent1 = new HashMap<>();
            ent1.put("Items", 100.0);
            ent1.put("Recovery", 80.0);
            result.enterpriseMetrics.put("Enterprise 1", ent1);
            
            Map<String, Double> ent2 = new HashMap<>();
            ent2.put("Items", 150.0);
            ent2.put("Recovery", 70.0);
            result.enterpriseMetrics.put("Enterprise 2", ent2);
            
            result.metricWinners.put("Items", "Enterprise 2");
            result.metricWinners.put("Recovery", "Enterprise 1");
            
            result.averages.put("Items", 125.0);
            result.averages.put("Recovery", 75.0);
            
            // Test methods
            assertEquals(Double.valueOf(100.0), result.getValue("Enterprise 1", "Items"), "Should get correct value");
            assertEquals("Enterprise 2", result.getWinner("Items"), "Should get correct winner");
            assertEquals(Double.valueOf(125.0), result.getAverage("Items"), "Should get correct average");
            assertTrue(result.isWinner("Enterprise 2", "Items"), "Enterprise 2 should be winner for Items");
            assertFalse(result.isWinner("Enterprise 1", "Items"), "Enterprise 1 should not be winner for Items");
            
            System.out.println("   ComparisonResult methods working correctly");
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== PERIOD COMPARISON TESTS ====================
    
    private static void testPeriodComparison() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Period Comparison Analytics");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        runTest("comparePeriods (last 2 weeks)", () -> {
            LocalDate today = LocalDate.now();
            LocalDate period1Start = today.minusDays(14);
            LocalDate period1End = today.minusDays(8);
            LocalDate period2Start = today.minusDays(7);
            LocalDate period2End = today;
            
            PeriodComparison comparison = analyticsService.comparePeriods(
                period1Start, period1End, period2Start, period2End);
            
            assertNotNull(comparison, "Period comparison should not be null");
            
            System.out.println(comparison.getSummary());
            System.out.println();
            System.out.println("   Items trend: " + comparison.getItemsTrend());
            System.out.println("   Recovery trend: " + comparison.getRecoveryTrend());
            
            return true;
        });
        
        runTest("comparePeriods (month over month)", () -> {
            LocalDate today = LocalDate.now();
            LocalDate thisMonthStart = today.withDayOfMonth(1);
            LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
            LocalDate lastMonthEnd = thisMonthStart.minusDays(1);
            
            PeriodComparison comparison = analyticsService.comparePeriods(
                lastMonthStart, lastMonthEnd, thisMonthStart, today);
            
            assertNotNull(comparison, "Period comparison should not be null");
            System.out.println("   Month-over-month comparison:");
            System.out.println("   Period 1: " + comparison.period1Label);
            System.out.println("   Period 2: " + comparison.period2Label);
            System.out.println("   Items change: " + String.format("%+.1f%%", comparison.itemsChange * 100));
            
            return true;
        });
        
        runTest("PeriodComparison utility methods", () -> {
            PeriodComparison pc = new PeriodComparison();
            pc.period1Label = "Week 1";
            pc.period2Label = "Week 2";
            pc.period1Items = 100;
            pc.period2Items = 120;
            pc.period1Recovered = 50;
            pc.period2Recovered = 70;
            pc.period1RecoveryRate = 0.5;
            pc.period2RecoveryRate = 0.583;
            pc.itemsChange = 0.2;
            pc.recoveryRateChange = 0.166;
            
            assertEquals("↑ Increasing", pc.getItemsTrend(), "Items should be increasing");
            assertEquals("↑ Improving", pc.getRecoveryTrend(), "Recovery should be improving");
            
            Object[][] tableData = pc.toTableData();
            assertEquals(4, tableData.length, "Should have 4 rows");
            
            System.out.println("   PeriodComparison methods working correctly");
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== NETWORK BENCHMARK TESTS ====================
    
    private static void testNetworkBenchmark() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Network Benchmark Analytics");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        runTest("benchmarkAgainstNetwork", () -> {
            List<EnterpriseStats> stats = analyticsService.getEnterpriseStats();
            if (stats.isEmpty()) {
                System.out.println("   ⚠ No enterprises available for benchmark test");
                return true;
            }
            
            String enterpriseId = stats.get(0).enterpriseId;
            BenchmarkResult result = analyticsService.benchmarkAgainstNetwork(enterpriseId);
            
            assertNotNull(result, "Benchmark result should not be null");
            assertNotNull(result.enterpriseName, "Enterprise name should not be null");
            
            System.out.println(result.getSummary());
            
            return true;
        });
        
        runTest("BenchmarkResult utility methods", () -> {
            BenchmarkResult br = new BenchmarkResult();
            br.enterpriseName = "Test Enterprise";
            br.enterpriseItems = 150;
            br.networkAvgItems = 100;
            br.enterpriseRecoveryRate = 0.7;
            br.networkAvgRecoveryRate = 0.5;
            br.enterpriseUsers = 50;
            br.networkAvgUsers = 40;
            br.itemsDifferential = 0.5;
            br.recoveryRateDifferential = 0.4;
            br.usersDifferential = 0.25;
            br.performanceLevel = "Excellent";
            
            assertEquals("⭐⭐⭐", br.getPerformanceIndicator(), "Should be 3 stars");
            assertEquals("↑↑", br.getDifferentialIndicator(0.5), "Should be strong up arrow");
            assertEquals("↑", br.getDifferentialIndicator(0.05), "Should be up arrow");
            assertEquals("→", br.getDifferentialIndicator(0.0), "Should be stable arrow");
            assertEquals("↓", br.getDifferentialIndicator(-0.15), "Should be down arrow");
            assertEquals("↓↓", br.getDifferentialIndicator(-0.25), "Should be strong down arrow");
            
            java.awt.Color color = br.getPerformanceColor();
            assertNotNull(color, "Performance color should not be null");
            
            List<ChartDataPoint> chartData = br.toChartData();
            assertEquals(3, chartData.size(), "Should have 3 chart data points");
            
            System.out.println("   BenchmarkResult methods working correctly");
            System.out.println("   Performance: " + br.performanceLevel + " " + br.getPerformanceIndicator());
            return true;
        });
        
        runTest("benchmarkAgainstNetwork - all enterprises", () -> {
            List<EnterpriseStats> stats = analyticsService.getEnterpriseStats();
            System.out.println("   Benchmarking all " + stats.size() + " enterprises:");
            
            for (EnterpriseStats stat : stats) {
                BenchmarkResult result = analyticsService.benchmarkAgainstNetwork(stat.enterpriseId);
                if (result.enterpriseName != null) {
                    System.out.println("   • " + result.enterpriseName + ": " + 
                        result.performanceLevel + " " + result.getPerformanceIndicator());
                }
            }
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== HELPER METHOD TESTS ====================
    
    private static void testHelperMethods() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("TEST GROUP: Chart Data Helper Methods");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Test ChartDataPoint
        runTest("ChartDataPoint class", () -> {
            ChartDataPoint dp = new ChartDataPoint("Test Label", 123.45, java.awt.Color.BLUE);
            
            assertEquals("Test Label", dp.label, "Label should match");
            assertEquals(123.45, dp.value, "Value should match");
            assertNotNull(dp.color, "Color should not be null");
            assertNotNull(dp.tooltip, "Tooltip should not be null");
            
            assertEquals(50.0, dp.getPercentage(246.9), "Percentage should be ~50%");
            assertEquals("123.45", dp.getFormattedValue(), "Should format to 2 decimal places");
            
            ChartDataPoint dpInt = new ChartDataPoint("Integer", 100.0, java.awt.Color.RED);
            assertEquals("100", dpInt.getFormattedValue(), "Integer should have no decimals");
            
            Object[] row = dp.toTableRow();
            assertEquals(2, row.length, "Table row should have 2 columns");
            
            System.out.println("   ChartDataPoint: " + dp);
            return true;
        });
        
        // Test TimeSeriesPoint
        runTest("TimeSeriesPoint class", () -> {
            LocalDate today = LocalDate.now();
            TimeSeriesPoint tsp = new TimeSeriesPoint(today, 42);
            
            assertEquals(today, tsp.date, "Date should match");
            assertEquals(42, tsp.value, "Value should match");
            
            assertNotNull(tsp.getShortDateLabel(), "Short date label should not be null");
            assertTrue(tsp.getTimestamp() > 0, "Timestamp should be positive");
            
            Object[] row = tsp.toTableRow();
            assertEquals(2, row.length, "Table row should have 2 columns");
            
            System.out.println("   TimeSeriesPoint: " + tsp);
            System.out.println("   Short label: " + tsp.getShortDateLabel());
            return true;
        });
        
        // Test ChartMetric enum
        runTest("ChartMetric enum coverage", () -> {
            ChartMetric[] metrics = ChartMetric.values();
            System.out.println("   Available metrics: " + metrics.length);
            for (ChartMetric m : metrics) {
                System.out.println("   • " + m.name());
            }
            return true;
        });
        
        // Test GroupByDimension enum
        runTest("GroupByDimension enum coverage", () -> {
            GroupByDimension[] dimensions = GroupByDimension.values();
            System.out.println("   Available dimensions: " + dimensions.length);
            for (GroupByDimension d : dimensions) {
                System.out.println("   • " + d.name());
            }
            return true;
        });
        
        System.out.println();
    }
    
    // ==================== TEST UTILITIES ====================
    
    private static void runTest(String testName, TestCase testCase) {
        testsRun++;
        try {
            System.out.println("▸ Test: " + testName);
            boolean result = testCase.run();
            if (result) {
                testsPassed++;
                System.out.println("  ✓ PASSED\n");
            } else {
                testsFailed++;
                System.out.println("  ✗ FAILED\n");
            }
        } catch (Exception e) {
            testsFailed++;
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
    
    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }
    
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }
    
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    
    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
    
    private static String colorToHex(java.awt.Color color) {
        if (color == null) return "null";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private static void printSummary() {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         TEST SUMMARY                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Tests Run:    %-49d ║%n", testsRun);
        System.out.printf("║  Tests Passed: %-49d ║%n", testsPassed);
        System.out.printf("║  Tests Failed: %-49d ║%n", testsFailed);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        double passRate = testsRun > 0 ? (double) testsPassed / testsRun * 100 : 0;
        String status = testsFailed == 0 ? "✓ ALL TESTS PASSED" : "✗ SOME TESTS FAILED";
        
        System.out.printf("║  Pass Rate:    %-47.1f%% ║%n", passRate);
        System.out.printf("║  Status:       %-49s ║%n", status);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        if (testsFailed == 0) {
            System.out.println("\n✅ Part 6: Chart Data Methods - COMPLETE");
            System.out.println("   All chart data generation methods verified");
            System.out.println("   Ready for Part 7: ReportExportService");
        }
    }
    
    @FunctionalInterface
    interface TestCase {
        boolean run() throws Exception;
    }
}
