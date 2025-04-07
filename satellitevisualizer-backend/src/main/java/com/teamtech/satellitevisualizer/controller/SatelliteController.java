/*
SatelliteController handles the incoming HTTP requests, which will primarily be the REST API endpoints
to fetch and save TLE data to the MongoDB repository.

@RestController indicates that this class is a RESTful web service controller.
@RequestMapping("/api/satellite") is the base URL path for accessing all endpoints in this controller.
 */

package com.teamtech.satellitevisualizer.controller;

import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.service.SatelliteService;
import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/satellite")
public class SatelliteController {

    @Autowired
    private SatelliteService satelliteService;

    @Autowired
    private SatellitePositionService satellitePositionService;

    /*
    Fetches and saves TLE data based on its NORAD ID
    Returns a SatelliteData object
    Throws RuntimeException if fetch and save fails
     */
    @GetMapping("/fetch-and-save/{noradId}")
    public SatelliteData fetchAndSaveTLE(@PathVariable int noradId) {
        try {
            return satelliteService.fetchAndSaveTLE(noradId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch and save satellite ", e);
        }
    }

    /*
    Retrieves the TLE data for a satellite based on its NORAD ID from MongoDB repository
    Returns a ResponseEntity<String> containing the TLE data or a 404 status if satellite is not found
    @CrossOrigin tag allows cross-origin request to be made from our frontend side http://localhost:3000
     */
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{noradId}/tle")
    public ResponseEntity<Map<String, Object>> getTLE(@PathVariable int noradId) {
        System.out.println("Fetching TLE for NORAD ID: " + noradId);
        SatelliteData satellite = satelliteService.getSatelliteBySatid(noradId);
        System.out.println(satellite);
        if (satellite != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("tle", satellite.getTle());

            SatelliteData updatedSatellite = satellitePositionService.getCurrentLLA(noradId);
            response.put("currentLLA", updatedSatellite.getGeodeticCoordinates());

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Satellite not found!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}
