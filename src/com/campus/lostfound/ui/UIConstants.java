package com.campus.lostfound.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Enumeration;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * UI Constants and helper methods for consistent styling.
 * Handles emoji rendering issues in Java Swing.
 */
public class UIConstants {
    
    // Emoji-capable font name (platform-specific)
    private static final String EMOJI_FONT;
    private static final boolean EMOJI_SUPPORTED;
    
    static {
        // Check for available emoji fonts
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        
        String foundFont = null;
        for (String fontName : fontNames) {
            if (fontName.equalsIgnoreCase("Segoe UI Emoji") ||
                fontName.equalsIgnoreCase("Apple Color Emoji") ||
                fontName.equalsIgnoreCase("Noto Color Emoji")) {
                foundFont = fontName;
                break;
            }
        }
        
        EMOJI_FONT = foundFont != null ? foundFont : "Segoe UI";
        EMOJI_SUPPORTED = foundFont != null;
    }
    
    /**
     * Get a font that can render emojis.
     */
    public static Font getEmojiFont(int style, int size) {
        return new Font(EMOJI_FONT, style, size);
    }
    
    /**
     * Check if emoji rendering is supported.
     */
    public static boolean isEmojiSupported() {
        return EMOJI_SUPPORTED;
    }
    
    /**
     * Create a label with emoji support.
     */
    public static JLabel createEmojiLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(getEmojiFont(Font.PLAIN, size));
        return label;
    }
    
    /**
     * Apply emoji-capable fonts globally to all Swing components.
     * Call this ONCE at application startup, before creating any UI.
     */
    public static void applyGlobalEmojiFont() {
        if (!EMOJI_SUPPORTED) {
            System.out.println("Warning: No emoji font found. Emojis may not render correctly.");
            return;
        }
        
        // Set default fonts for common Swing components
        FontUIResource emojiFont12 = new FontUIResource(EMOJI_FONT, Font.PLAIN, 12);
        FontUIResource emojiFont13 = new FontUIResource(EMOJI_FONT, Font.PLAIN, 13);
        FontUIResource emojiFont14 = new FontUIResource(EMOJI_FONT, Font.PLAIN, 14);
        FontUIResource emojiFontBold12 = new FontUIResource(EMOJI_FONT, Font.BOLD, 12);
        
        // Apply to UI defaults
        String[] componentKeys = {
            "Label.font",
            "Button.font",
            "TabbedPane.font",
            "ComboBox.font",
            "List.font",
            "Menu.font",
            "MenuItem.font",
            "Table.font",
            "TableHeader.font",
            "Tree.font",
            "TitledBorder.font",
            "ToolTip.font",
            "OptionPane.font",
            "OptionPane.messageFont",
            "OptionPane.buttonFont"
        };
        
        for (String key : componentKeys) {
            UIManager.put(key, emojiFont14);
        }
        
        // Text components typically use 13pt
        UIManager.put("TextField.font", emojiFont13);
        UIManager.put("TextArea.font", emojiFont13);
        UIManager.put("TextPane.font", emojiFont13);
        UIManager.put("EditorPane.font", emojiFont13);
        
        System.out.println("Global emoji font applied: " + EMOJI_FONT);
    }
    
    // ==================== TEXT ALTERNATIVES FOR EMOJIS ====================
    // Use these if emojis don't render properly
    
    public static class Icons {
        // Navigation & Actions
        public static final String HOME = isEmojiSupported() ? "🏠" : "[Home]";
        public static final String REPORT = isEmojiSupported() ? "📝" : "[Report]";
        public static final String TRACK = isEmojiSupported() ? "📋" : "[Track]";
        public static final String SEARCH = isEmojiSupported() ? "🔍" : "[Search]";
        public static final String EMERGENCY = isEmojiSupported() ? "🚨" : "[!]";
        public static final String REFRESH = isEmojiSupported() ? "🔄" : "[Refresh]";
        public static final String SUBMIT = isEmojiSupported() ? "📤" : "[Submit]";
        
        // Items & Categories
        public static final String TRANSIT = isEmojiSupported() ? "🚇" : "[Transit]";
        public static final String USER = isEmojiSupported() ? "👤" : "[User]";
        public static final String STATS = isEmojiSupported() ? "📊" : "[Stats]";
        public static final String PACKAGE = isEmojiSupported() ? "📦" : "[Package]";
        public static final String CHECK = isEmojiSupported() ? "✅" : "[OK]";
        public static final String ID = isEmojiSupported() ? "🆔" : "[ID]";
        public static final String CLAIM = isEmojiSupported() ? "📋" : "[Claim]";
        
        // Status indicators
        public static final String WARNING = isEmojiSupported() ? "⚠️" : "[!]";
        public static final String ERROR = isEmojiSupported() ? "❌" : "[X]";
        public static final String SUCCESS = isEmojiSupported() ? "✓" : "[OK]";
        public static final String INFO = isEmojiSupported() ? "ℹ️" : "[i]";
        
        // Categories (matching ItemCategory emojis)
        public static final String ELECTRONICS = isEmojiSupported() ? "📱" : "[Electronics]";
        public static final String BAGS = isEmojiSupported() ? "👜" : "[Bags]";
        public static final String CLOTHING = isEmojiSupported() ? "👕" : "[Clothing]";
        public static final String DOCUMENTS = isEmojiSupported() ? "📄" : "[Documents]";
        public static final String KEYS = isEmojiSupported() ? "🔑" : "[Keys]";
        public static final String WALLET = isEmojiSupported() ? "👛" : "[Wallet]";
        public static final String JEWELRY = isEmojiSupported() ? "💍" : "[Jewelry]";
        public static final String SPORTS = isEmojiSupported() ? "⚽" : "[Sports]";
        public static final String BOOKS = isEmojiSupported() ? "📚" : "[Books]";
        public static final String MEDICAL = isEmojiSupported() ? "💊" : "[Medical]";
        public static final String OTHER = isEmojiSupported() ? "📦" : "[Other]";
    }
}
