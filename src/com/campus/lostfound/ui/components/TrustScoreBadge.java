package com.campus.lostfound.ui.components;

import com.campus.lostfound.models.trustscore.TrustScore;
import com.campus.lostfound.models.trustscore.TrustScore.ScoreLevel;
import com.campus.lostfound.services.TrustScoreService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Reusable UI component for displaying a user's trust score as a badge.
 * 
 * Features:
 * - Color-coded score level (Excellent=green, Good=blue, Fair=yellow, Low=orange, Probation=red)
 * - Numeric score display
 * - Tooltip with score details
 * - Optional click handler for details popup
 * - Compact and full display modes
 * 
 * @author Developer 3 - Security & Verification Specialist
 */
public class TrustScoreBadge extends JPanel {
    
    // Data
    private String userId;
    private double score;
    private ScoreLevel level;
    private TrustScoreService trustScoreService;
    
    // UI Components
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JLabel iconLabel;
    
    // Display mode
    private boolean compactMode;
    
    // Colors for score levels
    private static final Color EXCELLENT_COLOR = new Color(40, 167, 69);    // Green
    private static final Color GOOD_COLOR = new Color(13, 110, 253);        // Blue
    private static final Color FAIR_COLOR = new Color(255, 193, 7);         // Yellow
    private static final Color LOW_COLOR = new Color(255, 152, 0);          // Orange
    private static final Color PROBATION_COLOR = new Color(220, 53, 69);    // Red
    private static final Color UNKNOWN_COLOR = new Color(108, 117, 125);    // Gray
    
    /**
     * Create a TrustScoreBadge with just a score value.
     * 
     * @param score The trust score (0-100)
     */
    public TrustScoreBadge(double score) {
        this(null, score, false);
    }
    
    /**
     * Create a TrustScoreBadge with a score value and compact mode option.
     * 
     * @param score The trust score (0-100)
     * @param compactMode If true, shows only the score number; if false, shows full badge
     */
    public TrustScoreBadge(double score, boolean compactMode) {
        this(null, score, compactMode);
    }
    
    /**
     * Create a TrustScoreBadge that loads score from database.
     * 
     * @param userId The user ID to load score for
     */
    public TrustScoreBadge(String userId) {
        this(userId, -1, false);
    }
    
    /**
     * Create a TrustScoreBadge with full configuration.
     * 
     * @param userId The user ID (optional - if provided, score is loaded from DB)
     * @param score The trust score (used if userId is null)
     * @param compactMode Display mode
     */
    public TrustScoreBadge(String userId, double score, boolean compactMode) {
        this.userId = userId;
        this.score = score;
        this.compactMode = compactMode;
        this.trustScoreService = new TrustScoreService();
        
        if (userId != null) {
            loadScore();
        } else {
            this.level = calculateLevel(score);
        }
        
        initComponents();
    }
    
    private void loadScore() {
        try {
            this.score = trustScoreService.getTrustScore(userId);
            this.level = calculateLevel(score);
        } catch (Exception e) {
            this.score = -1;
            this.level = null;
        }
    }
    
    private ScoreLevel calculateLevel(double score) {
        if (score < 0) return null;
        if (score >= 85) return ScoreLevel.EXCELLENT;
        if (score >= 70) return ScoreLevel.GOOD;
        if (score >= 50) return ScoreLevel.FAIR;
        if (score >= 30) return ScoreLevel.LOW;
        return ScoreLevel.PROBATION;
    }
    
    private void initComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        setOpaque(true);
        
        Color bgColor = getBackgroundColor();
        Color fgColor = getForegroundColor();
        
        setBackground(bgColor);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        
        if (compactMode) {
            // Compact mode: just the score
            scoreLabel = new JLabel(formatScore());
            scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            scoreLabel.setForeground(fgColor);
            add(scoreLabel);
        } else {
            // Full mode: icon + score + level
            iconLabel = new JLabel(getIcon());
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            add(iconLabel);
            
            scoreLabel = new JLabel(formatScore());
            scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            scoreLabel.setForeground(fgColor);
            add(scoreLabel);
            
            if (level != null) {
                levelLabel = new JLabel("(" + level.getDisplayName() + ")");
                levelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                levelLabel.setForeground(fgColor);
                add(levelLabel);
            }
        }
        
        // Tooltip with details
        setToolTipText(createTooltip());
        
        // Click handler for details
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetailsPopup();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }
    
    private String formatScore() {
        if (score < 0) return "N/A";
        return String.format("%.0f", score);
    }
    
    private String getIcon() {
        if (level == null) return "❓";
        return switch (level) {
            case EXCELLENT -> "⭐";
            case GOOD -> "✓";
            case FAIR -> "●";
            case LOW -> "⚠";
            case PROBATION -> "⛔";
        };
    }
    
    private Color getBackgroundColor() {
        if (level == null) return UNKNOWN_COLOR;
        return switch (level) {
            case EXCELLENT -> new Color(209, 231, 221);  // Light green
            case GOOD -> new Color(207, 226, 255);       // Light blue
            case FAIR -> new Color(255, 243, 205);       // Light yellow
            case LOW -> new Color(255, 237, 213);        // Light orange
            case PROBATION -> new Color(248, 215, 218); // Light red
        };
    }
    
    private Color getForegroundColor() {
        if (level == null) return Color.WHITE;
        return switch (level) {
            case EXCELLENT -> EXCELLENT_COLOR.darker();
            case GOOD -> GOOD_COLOR;
            case FAIR -> FAIR_COLOR.darker();
            case LOW -> LOW_COLOR.darker();
            case PROBATION -> PROBATION_COLOR;
        };
    }
    
    private String createTooltip() {
        StringBuilder tip = new StringBuilder("<html>");
        tip.append("<b>Trust Score: </b>").append(formatScore()).append("/100<br>");
        if (level != null) {
            tip.append("<b>Level: </b>").append(level.getDisplayName()).append("<br>");
            tip.append("<br><i>").append(getLevelDescription()).append("</i>");
        }
        tip.append("<br><br><i>Double-click for details</i>");
        tip.append("</html>");
        return tip.toString();
    }
    
    private String getLevelDescription() {
        if (level == null) return "Score not available";
        return switch (level) {
            case EXCELLENT -> "Trusted user - full privileges";
            case GOOD -> "Good standing - standard privileges";
            case FAIR -> "Some restrictions may apply";
            case LOW -> "Limited privileges - additional verification required";
            case PROBATION -> "Account on probation - claims require approval";
        };
    }
    
    private void showDetailsPopup() {
        if (score < 0) {
            JOptionPane.showMessageDialog(this,
                "Trust score information not available.",
                "Trust Score",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        StringBuilder details = new StringBuilder();
        details.append("Trust Score Details\n");
        details.append("═══════════════════════════════\n\n");
        details.append("Current Score: ").append(formatScore()).append("/100\n");
        details.append("Level: ").append(level != null ? level.getDisplayName() : "Unknown").append("\n\n");
        details.append("Privileges:\n");
        
        if (level != null) {
            switch (level) {
                case EXCELLENT -> {
                    details.append("  ✓ All claim types allowed\n");
                    details.append("  ✓ High-value item claims\n");
                    details.append("  ✓ Expedited processing\n");
                    details.append("  ✓ Cross-enterprise claims\n");
                }
                case GOOD -> {
                    details.append("  ✓ Standard claim types\n");
                    details.append("  ✓ High-value items (with verification)\n");
                    details.append("  ○ Normal processing times\n");
                }
                case FAIR -> {
                    details.append("  ✓ Basic claim types\n");
                    details.append("  ○ High-value requires extra verification\n");
                    details.append("  ○ May require additional documentation\n");
                }
                case LOW -> {
                    details.append("  ⚠ Limited claim types\n");
                    details.append("  ⚠ All claims require verification\n");
                    details.append("  ✗ High-value claims restricted\n");
                }
                case PROBATION -> {
                    details.append("  ✗ Claims require admin approval\n");
                    details.append("  ✗ High-value claims not allowed\n");
                    details.append("  ✗ Cross-enterprise claims blocked\n");
                }
            }
        }
        
        JOptionPane.showMessageDialog(this,
            details.toString(),
            "Trust Score Details",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Update the displayed score.
     */
    public void setScore(double score) {
        this.score = score;
        this.level = calculateLevel(score);
        refreshDisplay();
    }
    
    /**
     * Reload score from database (if userId was provided).
     */
    public void refresh() {
        if (userId != null) {
            loadScore();
            refreshDisplay();
        }
    }
    
    /**
     * Get the current score.
     */
    public double getScore() {
        return score;
    }
    
    /**
     * Get the current score level.
     */
    public ScoreLevel getLevel() {
        return level;
    }
    
    /**
     * Check if user is in good standing (score >= 70).
     */
    public boolean isGoodStanding() {
        return score >= 70;
    }
    
    /**
     * Check if user requires additional verification (score < 50).
     */
    public boolean requiresVerification() {
        return score < 50;
    }
    
    /**
     * Check if user is on probation (score < 30).
     */
    public boolean isOnProbation() {
        return score < 30;
    }
    
    private void refreshDisplay() {
        removeAll();
        initComponents();
        revalidate();
        repaint();
    }
    
    // ==================== STATIC FACTORY METHODS ====================
    
    /**
     * Create a compact badge showing just the score.
     */
    public static TrustScoreBadge compact(double score) {
        return new TrustScoreBadge(score, true);
    }
    
    /**
     * Create a full badge with icon and level.
     */
    public static TrustScoreBadge full(double score) {
        return new TrustScoreBadge(score, false);
    }
    
    /**
     * Create a badge that loads score for a user.
     */
    public static TrustScoreBadge forUser(String userId) {
        return new TrustScoreBadge(userId);
    }
    
    /**
     * Get just the color for a given score (for use in tables, etc.).
     */
    public static Color getColorForScore(double score) {
        if (score < 0) return UNKNOWN_COLOR;
        if (score >= 85) return EXCELLENT_COLOR;
        if (score >= 70) return GOOD_COLOR;
        if (score >= 50) return FAIR_COLOR;
        if (score >= 30) return LOW_COLOR;
        return PROBATION_COLOR;
    }
    
    /**
     * Get the background color for a given score (lighter shade).
     */
    public static Color getBackgroundColorForScore(double score) {
        if (score < 0) return new Color(233, 236, 239);
        if (score >= 85) return new Color(209, 231, 221);
        if (score >= 70) return new Color(207, 226, 255);
        if (score >= 50) return new Color(255, 243, 205);
        if (score >= 30) return new Color(255, 237, 213);
        return new Color(248, 215, 218);
    }
}
