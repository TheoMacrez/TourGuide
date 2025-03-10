package com.openclassrooms.tourguide.dto;

import com.openclassrooms.tourguide.user.User;
import gpsUtil.location.Attraction;

public class AttractionDistanceFromUser {
    private Attraction attraction;

    private User user;

    private double distance;

    public AttractionDistanceFromUser(Attraction attraction, User user, double distance) {
        this.attraction = attraction;
        this.user = user;
        this.distance = distance;
    }

    public Attraction getAttraction() {
        return attraction;
    }

    public void setAttraction(Attraction attraction) {
        this.attraction = attraction;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

}
