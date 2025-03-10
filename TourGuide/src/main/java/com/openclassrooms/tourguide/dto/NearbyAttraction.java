package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

public class NearbyAttraction {
    private String attractionName;
    private double attractionLatitude;
    private double attractionLongitude;
    private double userLatitude;
    private double userLongitude;
    private double distanceInMiles;
    private int rewardPoints;

    public NearbyAttraction(Attraction attraction, VisitedLocation userLocation, double distance, int rewardPoints) {
        this.attractionName = attraction.attractionName;
        this.attractionLatitude = attraction.latitude;
        this.attractionLongitude = attraction.longitude;
        this.userLatitude = userLocation.location.latitude;
        this.userLongitude = userLocation.location.longitude;
        this.distanceInMiles = distance;
        this.rewardPoints = rewardPoints;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public double getAttractionLatitude() {
        return attractionLatitude;
    }

    public void setAttractionLatitude(double attractionLatitude) {
        this.attractionLatitude = attractionLatitude;
    }

    public double getAttractionLongitude() {
        return attractionLongitude;
    }

    public void setAttractionLongitude(double attractionLongitude) {
        this.attractionLongitude = attractionLongitude;
    }

    public double getUserLatitude() {
        return userLatitude;
    }

    public void setUserLatitude(double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public double getUserLongitude() {
        return userLongitude;
    }

    public void setUserLongitude(double userLongitude) {
        this.userLongitude = userLongitude;
    }

    public double getDistanceInMiles() {
        return distanceInMiles;
    }

    public void setDistanceInMiles(double distanceInMiles) {
        this.distanceInMiles = distanceInMiles;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

}
