/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.services;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.Location;
import java.util.*;

/**
 *
 * @author aksha
 */
public class ItemMatcher {

    // Weights for matching - TITLE is most important!
    private static final double TITLE_WEIGHT = 0.35;       // Highest weight - title is key
    private static final double CATEGORY_WEIGHT = 0.20;
    private static final double DESCRIPTION_WEIGHT = 0.10;
    private static final double KEYWORD_WEIGHT = 0.10;
    private static final double LOCATION_WEIGHT = 0.10;
    private static final double TIME_WEIGHT = 0.10;
    private static final double COLOR_WEIGHT = 0.025;
    private static final double BRAND_WEIGHT = 0.025;

    // Find potential matches for a lost/found item
    public List<PotentialMatch> findMatches(Item targetItem, List<Item> candidateItems) {
        List<PotentialMatch> matches = new ArrayList<>();

        for (Item candidate : candidateItems) {
            // Skip if same type (don't match lost with lost)
            if (targetItem.getType() == candidate.getType()) {
                continue;
            }

            // Skip if already claimed
            if (candidate.getStatus() == Item.ItemStatus.CLAIMED) {
                continue;
            }

            double matchScore = calculateMatchScore(targetItem, candidate);

            // Only include if match score is above threshold
            if (matchScore >= 0.3) {
                PotentialMatch match = new PotentialMatch(candidate, matchScore);
                matches.add(match);
            }
        }

        // Sort by match score (highest first)
        Collections.sort(matches, (a, b) -> Double.compare(b.getScore(), a.getScore()));

        return matches;
    }

    // Calculate match score between two items
    private double calculateMatchScore(Item item1, Item item2) {
        double score = 0.0;

        // TITLE MATCHING - Most important factor!
        double titleScore = calculateTitleSimilarity(item1.getTitle(), item2.getTitle());
        score += titleScore * TITLE_WEIGHT;

        // Category matching (exact match gets full weight)
        if (item1.getCategory() == item2.getCategory()) {
            score += CATEGORY_WEIGHT;
        }

        // Description matching
        double descScore = calculateTextSimilarity(item1.getDescription(), item2.getDescription());
        score += descScore * DESCRIPTION_WEIGHT;

        // Keyword matching using Jaccard similarity
        double keywordSimilarity = calculateKeywordSimilarity(
                item1.getKeywords(),
                item2.getKeywords()
        );
        score += keywordSimilarity * KEYWORD_WEIGHT;

        // Location proximity
        double locationScore = calculateLocationScore(
                item1.getLocation(),
                item2.getLocation()
        );
        score += locationScore * LOCATION_WEIGHT;

        // Time proximity (items lost/found around same time more likely to match)
        double timeScore = calculateTimeScore(
                item1.getReportedDate(),
                item2.getReportedDate()
        );
        score += timeScore * TIME_WEIGHT;

        // Color matching (if specified)
        if (item1.getPrimaryColor() != null && item2.getPrimaryColor() != null) {
            if (item1.getPrimaryColor().equalsIgnoreCase(item2.getPrimaryColor())) {
                score += COLOR_WEIGHT;
            }
        }

        // Brand matching (if specified)
        if (item1.getBrand() != null && item2.getBrand() != null) {
            if (item1.getBrand().equalsIgnoreCase(item2.getBrand())) {
                score += BRAND_WEIGHT;
            }
        }

        return score;
    }

    // Calculate title similarity using multiple methods
    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null || title1.isEmpty() || title2.isEmpty()) {
            return 0.0;
        }

        String t1 = title1.toLowerCase().trim();
        String t2 = title2.toLowerCase().trim();

        // Exact match
        if (t1.equals(t2)) {
            return 1.0;
        }

        // One contains the other
        if (t1.contains(t2) || t2.contains(t1)) {
            return 0.9;
        }

        // Word-based matching
        Set<String> words1 = new HashSet<>(Arrays.asList(t1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(t2.split("\\s+")));
        
        // Remove common filler words
        Set<String> fillerWords = new HashSet<>(Arrays.asList("a", "an", "the", "my", "lost", "found"));
        words1.removeAll(fillerWords);
        words2.removeAll(fillerWords);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        // Calculate word overlap (Jaccard-like)
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        double wordOverlap = (double) intersection.size() / union.size();
        
        // Also check for partial word matches (e.g., "iphone" matches "iphone17")
        double partialMatchBonus = 0.0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.length() >= 3 && w2.length() >= 3) {
                    if (w1.contains(w2) || w2.contains(w1)) {
                        partialMatchBonus += 0.1;
                    }
                }
            }
        }
        partialMatchBonus = Math.min(partialMatchBonus, 0.3); // Cap bonus

        return Math.min(wordOverlap + partialMatchBonus, 1.0);
    }

    // Calculate text similarity for descriptions
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        String t1 = text1.toLowerCase().trim();
        String t2 = text2.toLowerCase().trim();

        // Exact match
        if (t1.equals(t2)) {
            return 1.0;
        }

        // One contains the other
        if (t1.contains(t2) || t2.contains(t1)) {
            return 0.8;
        }

        // Word-based matching
        Set<String> words1 = new HashSet<>(Arrays.asList(t1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(t2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    // Calculate Jaccard similarity between keyword sets
    private double calculateKeywordSimilarity(List<String> keywords1, List<String> keywords2) {
        if (keywords1 == null || keywords2 == null || keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(keywords2);

        // Calculate intersection
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // Calculate union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        // Jaccard similarity = |intersection| / |union|
        return (double) intersection.size() / union.size();
    }

    // Calculate location score based on proximity
    private double calculateLocationScore(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return 0.0;
        }

        // Same building = perfect score
        if (loc1.getBuilding().equals(loc2.getBuilding())) {
            // Same room = 1.0, same building = 0.8
            if (loc1.getRoomNumber() != null
                    && loc1.getRoomNumber().equals(loc2.getRoomNumber())) {
                return 1.0;
            }
            return 0.8;
        }

        // Different buildings - use distance
        double distance = loc1.distanceFrom(loc2);

        // Convert distance to score (closer = higher score)
        // Within 100m = high score, >1km = low score
        if (distance < 0.1) {
            return 0.6;  // Within 100m
        }
        if (distance < 0.5) {
            return 0.3;  // Within 500m
        }
        if (distance < 1.0) {
            return 0.1;  // Within 1km
        }
        return 0.0; // Too far
    }

    // Calculate time score based on temporal proximity
    private double calculateTimeScore(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return 0.0;
        }

        long diffInMillis = Math.abs(date1.getTime() - date2.getTime());
        long diffInHours = diffInMillis / (1000 * 60 * 60);

        // Same day = high score, decreasing over time
        if (diffInHours < 24) {
            return 1.0;      // Same day
        }
        if (diffInHours < 72) {
            return 0.7;      // Within 3 days
        }
        if (diffInHours < 168) {
            return 0.4;     // Within a week
        }
        if (diffInHours < 336) {
            return 0.2;     // Within 2 weeks
        }
        return 0.0; // Too long ago
    }

    // Inner class to hold match results
    public static class PotentialMatch {

        private Item item;
        private double score;
        private String matchReason;

        public PotentialMatch(Item item, double score) {
            this.item = item;
            this.score = score;
            this.matchReason = generateMatchReason(score);
        }

        private String generateMatchReason(double score) {
            if (score >= 0.8) {
                return "Very High Match";
            }
            if (score >= 0.6) {
                return "High Match";
            }
            if (score >= 0.4) {
                return "Moderate Match";
            }
            return "Possible Match";
        }

        public Item getItem() {
            return item;
        }

        public double getScore() {
            return score;
        }

        public String getMatchReason() {
            return matchReason;
        }
    }
}
