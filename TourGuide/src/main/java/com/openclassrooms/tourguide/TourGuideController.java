package com.openclassrooms.tourguide;

import java.util.ArrayList;
import java.util.List;

import com.openclassrooms.tourguide.dto.NearbyAttractionToJson;
import com.openclassrooms.tourguide.dto.AttractionDistanceFromUser;
import com.openclassrooms.tourguide.service.RewardsService;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	private TourGuideService tourGuideService;

    @Autowired
    private RewardsService rewardsService;

    @Autowired
    private GpsUtil gpsUtil;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) {
    	return tourGuideService.getUserLocation(getUser(userName));
    }


    /**
     * Retrieves the five closest tourist attractions to the given user.
     * This method fetches the user's last known location and finds the five nearest attractions,
     * returning a JSON list containing details such as:
     * - The name of the attraction
     * - The attraction's latitude and longitude
     * - The user's latitude and longitude
     * - The distance in miles between the user and the attraction
     * - The reward points for visiting each attraction
     *
     * @param userName The username of the user whose location is used to find nearby attractions.
     * @return A ResponseEntity containing a list of nearby attractions formatted as JSON.
     */
    @RequestMapping("/getNearbyAttractions")
    public ResponseEntity<List<NearbyAttractionToJson>> getNearbyAttractions(@RequestParam String userName) {
        // Retrieve the user's last known location
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        // Fetch the 5 closest attractions based on the user's location
        List<AttractionDistanceFromUser> nearbyAttractions = tourGuideService.getNearByAttractions(getUser(userName),visitedLocation,5);

        // Prepare the list of attractions to return as JSON
        List<NearbyAttractionToJson> nearbyAttractionsToReturnToJson = new ArrayList<>();
        for (AttractionDistanceFromUser attractionDistanceFromUser : nearbyAttractions) {
            Attraction attraction = attractionDistanceFromUser.getAttraction();

            // Convert each attraction into a JSON-friendly format with additional details
            nearbyAttractionsToReturnToJson.add(new NearbyAttractionToJson(attraction,
                    visitedLocation,
                    attractionDistanceFromUser.getDistance(),
                    rewardsService.getRewardPoints(attraction,getUser(userName))));
        }
        // Return the list of nearby attractions as a JSON response
        return ResponseEntity.ok(nearbyAttractionsToReturnToJson);
    }


    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}