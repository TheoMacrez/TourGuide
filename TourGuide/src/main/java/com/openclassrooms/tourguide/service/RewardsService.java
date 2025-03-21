package com.openclassrooms.tourguide.service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 10000;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	// Executor service for handling asynchronous reward calculations
	private final static ExecutorService executorService = Executors.newFixedThreadPool(100);

	// Cache for storing distances to attractions
	private final HashMap<Attraction, Double> allDistances = new HashMap<>();

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/**
	 * Calculates rewards for a given user by waiting for the asynchronous process to complete.
	 * @param user The user whose rewards are to be calculated.
	 */
	public void calculateRewards(User user) {
		calculateRewardsAsync(user).join();
	}

	/**
	 * Asynchronously calculates rewards for a given user based on their visited locations.
	 * @param user The user for whom rewards are calculated.
	 * @return A CompletableFuture that completes when the calculation is done.
	 */
	public CompletableFuture<Void> calculateRewardsAsync(User user) {
		return CompletableFuture.runAsync(() -> {
			// Convert user visited locations to a thread-safe list
			// Use of CopyOnWriteArrayList to avoid Concurrence Exception
			List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
			List<Attraction> allAttractions = gpsUtil.getAttractions();

			for (VisitedLocation visitedLocation : userLocations) {
				for (Attraction attractionFromList : allAttractions) {
					// Check if the user has already received a reward for this attraction
					if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attractionFromList.attractionName))) {
						if (nearAttraction(visitedLocation, attractionFromList)) {
							user.addUserReward(new UserReward(visitedLocation, attractionFromList, getRewardPoints(attractionFromList, user)));
						}
					}
				}
			}
		}, executorService).exceptionally(ex -> {
			// Handle exceptions during reward calculation
			System.err.println("Error calculating rewards for user " + user.getUserId() + ": " + ex.getMessage());
			return null;
		});
	}

	/**
	 * Calculates rewards for all users asynchronously and waits for completion.
	 * @param users List of users for whom rewards should be calculated.
	 */
	public void calculateAllRewardsUsers(List<User> users) {
		List<CompletableFuture<Void>> futures = users.stream()
				.map(this::calculateRewardsAsync)
				.toList();
		futures.forEach(CompletableFuture::join);
	}

	/**
	 * Checks if a given location is within proximity of an attraction.
	 * @param attraction The attraction to check.
	 * @param location The location to compare.
	 * @return True if within the defined proximity range, false otherwise.
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		allDistances.put(attraction, getDistance(attraction, location));
		return getDistance(attraction, location) <= attractionProximityRange;
	}

	/**
	 * Checks if a visited location is near an attraction based on proximity buffer.
	 * @param visitedLocation The visited location.
	 * @param attraction The attraction to check against.
	 * @return True if within the proximity buffer, false otherwise.
	 */
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}

	/**
	 * Retrieves the reward points for a user visiting a specific attraction.
	 * @param attraction The attraction.
	 * @param user The user earning the reward.
	 * @return The number of reward points earned.
	 */
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	/**
	 * Calculates the distance between two locations using the Haversine formula.
	 * @param loc1 First location.
	 * @param loc2 Second location.
	 * @return The distance in miles.
	 */
	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

	/**
	 * Shuts down the executor service gracefully.
	 */
	@PreDestroy
	public void shutdownExecutor() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
