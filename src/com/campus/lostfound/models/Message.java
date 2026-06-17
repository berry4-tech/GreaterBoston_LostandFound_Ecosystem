/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

import java.util.Date;

/**
 * Message model for the messaging system.
 * Uses String IDs for consistency with MongoDB (email for users, ObjectId for items).
 *
 * @author aksha
 */
public class Message {

    private String messageId;
    private String itemId;      // MongoDB ObjectId as String
    private String senderId;    // User email
    private String recipientId; // User email
    private String messageText;
    private boolean isRead;
    private Date sentDate;

    public Message(String itemId, String senderId, String recipientId, String messageText) {
        this.itemId = itemId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.messageText = messageText;
        this.sentDate = new Date();
        this.isRead = false;
    }

    // Getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }
}
