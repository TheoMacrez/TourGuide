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
    

 	//   Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains a number of NearbyAttraction with the :
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions")
    public ResponseEntity<List<NearbyAttractionToJson>> getNearbyAttractions(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        List<AttractionDistanceFromUser> nearbyAttractions = tourGuideService.getNearByAttractions(getUser(userName),visitedLocation,5);

        List<NearbyAttractionToJson> nearbyAttractionsToReturnToJson = new ArrayList<>();
        for (AttractionDistanceFromUser attractionDistanceFromUser : nearbyAttractions) {
            Attraction attraction = attractionDistanceFromUser.getAttraction();
            nearbyAttractionsToReturnToJson.add(new NearbyAttractionToJson(attraction,
                    visitedLocation,
                    attractionDistanceFromUser.getDistance(),
                    rewardsService.getRewardPoints(attraction,getUser(userName))));
        }
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