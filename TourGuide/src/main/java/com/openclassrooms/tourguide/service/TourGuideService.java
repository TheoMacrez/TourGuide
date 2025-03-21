package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.AttractionDistanceFromUser;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	// Thread pool for handling multiple user tracking requests concurrently
	private final static ExecutorService executorService = Executors.newFixedThreadPool(100);

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * Retrieves the last known location of the user.
	 * If the user has no recorded location, it tracks a new location.
	 *
	 * @param user The user whose location is retrieved.
	 * @return The last visited location of the user.
	 */
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user);
		return visitedLocation;
	}

	/**
	 * Retrieves the user's last known location asynchronously.
	 * If no location exists, a new tracking process starts.
	 *
	 * @param user The user whose location is needed.
	 * @return A CompletableFuture containing the visited location.
	 */
	public CompletableFuture<VisitedLocation> getUserLocationAsync(User user) {
		if (user.getVisitedLocations().size() > 0) {
			// Retourne le dernier emplacement visité dans un CompletableFuture
			return CompletableFuture.completedFuture(user.getLastVisitedLocation());
		} else {
			// Appelle la méthode asynchrone pour suivre la localisation de l'utilisateur
			return trackUserLocationAsync(user);
		}
	}


	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * Retrieves a list of trip deals for a given user based on their preferences.
	 *
	 * @param user The user for whom trip deals are requested.
	 * @return A list of travel providers with pricing.
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Tracks the user's location and updates their visited locations.
	 *
	 * @param user The user to track.
	 * @return The user's newly tracked location.
	 */
	public VisitedLocation trackUserLocation(User user) {
		// Use the asynchronous tracking method
		CompletableFuture<VisitedLocation> futureVisitedLocation = trackUserLocationAsync(user);
		// Wait for the asynchronous operation to complete and return the result
		return futureVisitedLocation.join();
	}


	/**
	 * Tracks the user's location asynchronously.
	 *
	 * @param user The user whose location will be tracked.
	 * @return A CompletableFuture containing the visited location.
	 */
	public CompletableFuture<VisitedLocation> trackUserLocationAsync(User user) {

		return CompletableFuture.supplyAsync(() -> {
			// Retrieve the user's current location from GPS service
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			// Add the new location to the user's visited locations
			user.addToVisitedLocations(visitedLocation);
			// Calculate rewards based on nearby attractions
			rewardsService.calculateRewards(user);
			return visitedLocation;
		},executorService
		);
	}

	/**
	 * Tracks the location of all users asynchronously.
	 *
	 * @param allUsers List of all users to be tracked.
	 */
	public void trackAllUsersLocation(List<User> allUsers)
	{
		List<CompletableFuture<VisitedLocation>> completableFutureUserList = allUsers.stream().
				map(this::trackUserLocationAsync).
				toList();
		// Wait for all tracking tasks to complete
		completableFutureUserList.forEach(CompletableFuture::join);

	}


	/**
	 * Retrieves a list of nearby attractions for a given user based on their location.
	 *
	 * @param user The user whose nearby attractions are being searched.
	 * @param visitedLocation The user's last known location.
	 * @param numberOfNearbyAttraction The number of nearby attractions to return.
	 * @return A sorted list of attractions by distance.
	 */
	public List<AttractionDistanceFromUser> getNearByAttractions(User user, VisitedLocation visitedLocation, int numberOfNearbyAttraction) {

		List<AttractionDistanceFromUser> nearbyAttractions = new ArrayList<>();
		for (Attraction attraction : gpsUtil.getAttractions()) {
			if (rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
				double distance = rewardsService.getDistance(attraction, visitedLocation.location);
				AttractionDistanceFromUser attractionDistanceFromUser = new AttractionDistanceFromUser(attraction,user,distance);
				nearbyAttractions.add(attractionDistanceFromUser);
			}
		}

		// Sort attractions by distance
		// limit by the numberOfNearbyAttraction
        return nearbyAttractions.stream()
                .sorted(Comparator.comparingDouble(AttractionDistanceFromUser::getDistance))
                .limit(numberOfNearbyAttraction)
                .collect(Collectors.toList());


	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**
	 * Shuts down the executor service gracefully.
	 */
	@PreDestroy
	public void shutdownExecutor() {
		logger.info("Shutting down ExecutorService...");
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				logger.warn("ExecutorService did not terminate, forcing shutdown.");
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			logger.error("Shutdown interrupted, forcing shutdown now.");
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
