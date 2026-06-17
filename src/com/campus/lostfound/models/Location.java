/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.lostfound.models;

/**
 *
 * @author aksha
 */
public class Location {

    private int locationId;
    private Building building;
    private String roomNumber;
    private String floor;
    private String specificLocation; // "Near water fountain", "By elevator", etc.
    private double latitude;
    private double longitude;

    public Location(Building building, String roomNumber, String specificLocation) {
        this.building = building;
        this.roomNumber = roomNumber;
        this.specificLocation = specificLocation;
    }

    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        sb.append(building.getName());
        if (roomNumber != null && !roomNumber.isEmpty()) {
            sb.append(", Room ").append(roomNumber);
        }
        if (floor != null && !floor.isEmpty()) {
            sb.append(", Floor ").append(floor);
        }
        if (specificLocation != null && !specificLocation.isEmpty()) {
            sb.append(" (").append(specificLocation).append(")");
        }
        return sb.toString();
    }

    // Calculate distance between two locations (simplified)
    public double distanceFrom(Location other) {
        if (this.building.equals(other.building)) {
            return 0.0; // Same building
        }
        // Simplified distance calculation
        double latDiff = Math.abs(this.latitude - other.latitude);
        double lonDiff = Math.abs(this.longitude - other.longitude);
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111.0; // km
    }

    // Getters and setters
    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getSpecificLocation() {
        return specificLocation;
    }

    public void setSpecificLocation(String specificLocation) {
        this.specificLocation = specificLocation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
