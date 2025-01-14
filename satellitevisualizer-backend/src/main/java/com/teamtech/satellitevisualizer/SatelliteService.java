/*
SatelliteService handles the logic for interacting with the N2YO API.
It contains methods to fetch TLE data from the API and optionally save it to the database

@Service tag indicates that this class is a service component of the application, which contains the core logic.
@Autowired annotation is used for dependency injection for SatelliteRepository interface.
 */

package com.teamtech.satellitevisualizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class SatelliteService {

    // Base URL for the N2YO API to fetch the TLE data
    private final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/tle/";
    // Replace with your api key by signing up for an account at n2yo.com.
    private final String API_KEY = "YENSUG-T4SP3G-TVPZ5J-5EGD";

    @Autowired
    private SatelliteRepository satelliteRepository;

    @Autowired
    public SatelliteService(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    /*
    Retrieves a SatelliteData object from MongoDB database based on the satellite's NORAD ID
    Returns the SatelliteData objects
     */
    public SatelliteData getSatelliteBySatid(int satid) {
        return satelliteRepository.findBySatid(satid);
    }

    /*
    Fetches TLE data for a satellite from N2YO API
    Returns a SatelliteResponse
    Throws exception if TLE fetch fails
     */
    public SatelliteResponse getSatelliteTLE(int noradId) throws Exception {
        String url = BASE_URL + noradId + "?apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response.getBody(), SatelliteResponse.class);
    }

    /*
    Saves satellite data in MongoDB
    Returns a SatelliteData object
     */
    public SatelliteData saveSatelliteData(SatelliteData satellite) {
        System.out.println("Saving satellite: " + satellite);
        return satelliteRepository.save(satellite);
    }

    /*
    Fetches TLE from the N2YO API, creates SatelliteData object based on norad ID, and saves that to MongoDB database
    Returns a SatelliteData object
    Throws exception if TLE fetch fails
     */
    public SatelliteData fetchAndSaveTLE(int noradId) throws Exception {
        System.out.println("Fetching and saving TLE for NORAD ID: " + noradId);
        SatelliteResponse satelliteResponse = getSatelliteTLE(noradId);

        SatelliteData satellite = new SatelliteData();
        satellite.setSatid(satelliteResponse.getInfo().getSatid());
        satellite.setSatname(satelliteResponse.getInfo().getSatname());
        satellite.setTle(satelliteResponse.getTle());

        return saveSatelliteData(satellite);
    }
}