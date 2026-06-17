/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.dao;

import com.campus.lostfound.models.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * MongoDB Data Access Object for Message operations.
 * Uses String IDs (email for users, MongoDB ObjectId strings for items).
 *
 * @author aksha
 */
public class MongoMessageDAO {

    private static final Logger LOGGER = Logger.getLogger(MongoMessageDAO.class.getName());
    private final MongoCollection<Document> messagesCollection;
    private final MongoCollection<Document> usersCollection;
    private final MongoCollection<Document> itemsCollection;

    public MongoMessageDAO() {
        MongoDBConnection connection = MongoDBConnection.getInstance();
        this.messagesCollection = connection.getCollection("messages");
        this.usersCollection = connection.getCollection("users");
        this.itemsCollection = connection.getCollection("items");
    }

    public String sendMessage(Message message) {
        try {
            Document doc = new Document()
                    .append("itemId", message.getItemId())
                    .append("senderId", message.getSenderId())
                    .append("recipientId", message.getRecipientId())
                    .append("messageText", message.getMessageText())
                    .append("isRead", false)
                    .append("sentDate", message.getSentDate());

            messagesCollection.insertOne(doc);
            String id = doc.getObjectId("_id").toString();
            LOGGER.info("Message sent with ID: " + id);
            return id;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending message", e);
            return null;
        }
    }

    /**
     * Get messages between two users about a specific item.
     * 
     * @param userEmail1 First user's email
     * @param userEmail2 Second user's email
     * @param itemId Item's MongoDB ObjectId as String
     * @return List of messages
     */
    public List<Message> getMessages(String userEmail1, String userEmail2, String itemId) {
        List<Message> messages = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                    Filters.eq("itemId", itemId),
                    Filters.or(
                            Filters.and(
                                    Filters.eq("senderId", userEmail1),
                                    Filters.eq("recipientId", userEmail2)
                            ),
                            Filters.and(
                                    Filters.eq("senderId", userEmail2),
                                    Filters.eq("recipientId", userEmail1)
                            )
                    )
            );

            for (Document doc : messagesCollection.find(filter).sort(Sorts.ascending("sentDate"))) {
                messages.add(documentToMessage(doc));
            }

            // Mark as read for the current user
            messagesCollection.updateMany(
                    Filters.and(filter, Filters.eq("recipientId", userEmail1)),
                    new Document("$set", new Document("isRead", true))
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting messages", e);
        }
        return messages;
    }

    /**
     * Get all conversations for a user.
     * 
     * @param userEmail User's email address
     * @return List of conversations
     */
    public List<Conversation> getConversations(String userEmail) {
        Map<String, Conversation> conversationMap = new HashMap<>();

        try {
            // Find all messages where user is sender or recipient
            Bson filter = Filters.or(
                    Filters.eq("senderId", userEmail),
                    Filters.eq("recipientId", userEmail)
            );

            for (Document doc : messagesCollection.find(filter).sort(Sorts.descending("sentDate"))) {
                String senderEmail = doc.getString("senderId");
                String recipientEmail = doc.getString("recipientId");
                String otherUserEmail = senderEmail.equals(userEmail) ? recipientEmail : senderEmail;
                String itemId = doc.getString("itemId");

                String key = otherUserEmail + "_" + itemId;

                if (!conversationMap.containsKey(key)) {
                    // Create new conversation
                    Conversation conv = new Conversation(itemId, otherUserEmail);

                    // Get other user's name from database
                    String otherUserName = getUserName(otherUserEmail);
                    conv.setOtherUserName(otherUserName != null ? otherUserName : otherUserEmail);

                    // Get item title from database
                    String itemTitle = getItemTitle(itemId);
                    conv.setItemTitle(itemTitle != null ? itemTitle : "Item");

                    conv.setLastMessage(doc.getString("messageText"));
                    conv.setLastMessageDate(doc.getDate("sentDate"));

                    if (!doc.getBoolean("isRead", false)
                            && recipientEmail.equals(userEmail)) {
                        conv.incrementUnreadCount();
                    }

                    conversationMap.put(key, conv);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting conversations", e);
        }

        return new ArrayList<>(conversationMap.values());
    }

    /**
     * Get user's full name from their email.
     */
    private String getUserName(String email) {
        try {
            Document userDoc = usersCollection.find(Filters.eq("email", email)).first();
            if (userDoc != null) {
                String firstName = userDoc.getString("firstName");
                String lastName = userDoc.getString("lastName");
                if (firstName != null && lastName != null) {
                    return firstName + " " + lastName;
                } else if (firstName != null) {
                    return firstName;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting user name for: " + email, e);
        }
        return null;
    }

    /**
     * Get item title from its MongoDB ObjectId.
     */
    private String getItemTitle(String itemId) {
        try {
            if (itemId != null && ObjectId.isValid(itemId)) {
                Document itemDoc = itemsCollection.find(Filters.eq("_id", new ObjectId(itemId))).first();
                if (itemDoc != null) {
                    return itemDoc.getString("title");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting item title for: " + itemId, e);
        }
        return null;
    }

    private Message documentToMessage(Document doc) {
        Message msg = new Message(
                doc.getString("itemId"),
                doc.getString("senderId"),
                doc.getString("recipientId"),
                doc.getString("messageText")
        );
        msg.setSentDate(doc.getDate("sentDate"));
        msg.setRead(doc.getBoolean("isRead", false));
        if (doc.getObjectId("_id") != null) {
            msg.setMessageId(doc.getObjectId("_id").toString());
        }
        return msg;
    }
}
