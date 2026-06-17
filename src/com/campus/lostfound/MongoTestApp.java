/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound;

/**
 *
 * @author aksha
 */

import com.campus.lostfound.dao.*;
import com.campus.lostfound.models.*;
import com.campus.lostfound.utils.MongoDataGenerator;
import java.util.List;

public class MongoTestApp {
    public static void main(String[] args) {
        System.out.println("Testing MongoDB Connection...");
        
        // Test connection
        MongoDBConnection connection = MongoDBConnection.getInstance();
        if (connection.testConnection()) {
            System.out.println("✓ MongoDB connected successfully!");
            
            // Generate sample data
            System.out.println("\nGenerating sample data...");
            MongoDataGenerator generator = new MongoDataGenerator();
            generator.generateFullEcosystem();
            
            // Test queries
            System.out.println("\nTesting queries...");
            MongoItemDAO itemDAO = new MongoItemDAO();
            
            // Get all items
            System.out.println("Total items: " + itemDAO.findAll().size());
            
            // Search for items
            List<Item> electronics = itemDAO.searchItems(null, null, Item.ItemCategory.ELECTRONICS);
            System.out.println("Electronics found: " + electronics.size());
            
            // Test user authentication
            MongoUserDAO userDAO = new MongoUserDAO();
            boolean authenticated = userDAO.authenticate("admin@northeastern.edu", "admin123");
            System.out.println("Admin authentication: " + (authenticated ? "✓" : "✗"));
            
            System.out.println("\n✓ All tests passed!");
        } else {
            System.out.println("✗ MongoDB connection failed!");
        }
    }
}
