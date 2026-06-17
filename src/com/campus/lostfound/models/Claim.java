/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

import java.util.Date;

/**
 * Model for item claims - tracks when someone claims a found item
 * @author aksha
 */
public class Claim {
    
    private String claimId;
    private int itemId;
    private User claimant;
    private String uniqueFeatures;
    private Date lostDate;
    private String proofOfOwnership;
    private String verificationAnswer;
    private String[] selectedTimeSlots;
    private String contactMethod;
    private ClaimStatus status;
    private Date submittedDate;
    private Date reviewedDate;
    private String rejectionReason;
    
    public enum ClaimStatus {
        PENDING("Pending Review", "#FF9800"),
        APPROVED("Approved", "#4CAF50"),
        REJECTED("Rejected", "#F44336");
        
        private String label;
        private String colorCode;
        
        ClaimStatus(String label, String colorCode) {
            this.label = label;
            this.colorCode = colorCode;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    public Claim(int itemId, User claimant) {
        this.itemId = itemId;
        this.claimant = claimant;
        this.status = ClaimStatus.PENDING;
        this.submittedDate = new Date();
    }
    
    // Getters and setters
    public String getClaimId() {
        return claimId;
    }
    
    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }
    
    public int getItemId() {
        return itemId;
    }
    
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    
    public User getClaimant() {
        return claimant;
    }
    
    public void setClaimant(User claimant) {
        this.claimant = claimant;
    }
    
    public String getUniqueFeatures() {
        return uniqueFeatures;
    }
    
    public void setUniqueFeatures(String uniqueFeatures) {
        this.uniqueFeatures = uniqueFeatures;
    }
    
    public Date getLostDate() {
        return lostDate;
    }
    
    public void setLostDate(Date lostDate) {
        this.lostDate = lostDate;
    }
    
    public String getProofOfOwnership() {
        return proofOfOwnership;
    }
    
    public void setProofOfOwnership(String proofOfOwnership) {
        this.proofOfOwnership = proofOfOwnership;
    }
    
    public String getVerificationAnswer() {
        return verificationAnswer;
    }
    
    public void setVerificationAnswer(String verificationAnswer) {
        this.verificationAnswer = verificationAnswer;
    }
    
    public String[] getSelectedTimeSlots() {
        return selectedTimeSlots;
    }
    
    public void setSelectedTimeSlots(String[] selectedTimeSlots) {
        this.selectedTimeSlots = selectedTimeSlots;
    }
    
    public String getContactMethod() {
        return contactMethod;
    }
    
    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }
    
    public ClaimStatus getStatus() {
        return status;
    }
    
    public void setStatus(ClaimStatus status) {
        this.status = status;
    }
    
    public Date getSubmittedDate() {
        return submittedDate;
    }
    
    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }
    
    public Date getReviewedDate() {
        return reviewedDate;
    }
    
    public void setReviewedDate(Date reviewedDate) {
        this.reviewedDate = reviewedDate;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
