/**
 * SatelliteService handles the logic for interacting with the N2YO API.
 * It contains methods to fetch TLE data from the API and optionally save it to the database
 *
 * @Service tag indicates that this class is a service component of the application, which contains the core logic.
 * @Autowired annotation is used for dependency injection for SatelliteRepository interface.
 */

package com.teamtech.satellitevisualizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import com.teamtech.satellitevisualizer.controller.SatelliteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.File;
@Service
public class SatelliteService {

    // Base URL for the N2YO API to fetch the TLE data
    private final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/tle/";
    @Value("${n2yo.api.key}")
    private String API_KEY;

    @Autowired
    private SatelliteRepository satelliteRepository;

    @Autowired
    public SatelliteService(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    /**
     * Retrieves a SatelliteData object from MongoDB database based on the satellite's NORAD ID
     * @param noradId the NORAD ID of the satellite
     * @return SatelliteData object
     */
    public SatelliteData getSatelliteBySatid(int noradId) {
        return satelliteRepository.findBySatid(noradId);
    }

    /**
     * Fetches TLE data for a satellite from N2YO API
     * @param noradId the NORAD ID of the satellite
     * @return SatelliteResponse
     * @throws @exception if TLE fetch fails
     */
    public SatelliteResponse getSatelliteTLE(int noradId) throws Exception {
        String url = BASE_URL + noradId + "?apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response.getBody(), SatelliteResponse.class);
    }

    /**
     * Saves satellite data in MongoDB
     * @param satellite the SatelliteData object to be saved
     * @return SatelliteData object
     */
    public SatelliteData saveSatelliteData(SatelliteData satellite) {
        System.out.println("Saving satellite: " + satellite);
        return satelliteRepository.save(satellite);
    }

    /**
     * Fetches TLE from the N2YO API, creates SatelliteData object based on norad ID, and saves that to MongoDB database
     * @param noradId the NORAD ID of the satellite
     * @return SatelliteData object
     * @throws @exception if TLE fetch fails
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