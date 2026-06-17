/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

import java.util.Date;

/**
 * Conversation model for displaying message threads.
 * Uses String IDs for consistency with MongoDB (email for users, ObjectId for items).
 *
 * @author aksha
 */
public class Conversation {

    private String itemId;      // MongoDB ObjectId as String
    private String otherUserId; // User email
    private String otherUserName;
    private String itemTitle;
    private String lastMessage;
    private Date lastMessageDate;
    private int unreadCount;

    public Conversation(String itemId, String otherUserId) {
        this.itemId = itemId;
        this.otherUserId = otherUserId;
        this.unreadCount = 0;
    }

    // Getters and setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(Date lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
    }
}
