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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{noradId}/czml")
    public ResponseEntity<String> getCzml(@PathVariable int noradId) {
        try {
            // Retrieve TLE data using existing logic
            SatelliteData satellite = satelliteService.getSatelliteBySatid(noradId);
            if (satellite == null || satellite.getTle() == null || satellite.getTle().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("TLE not found for: " + noradId);
            }

            String tleData = satellite.getTle();
            List<SpacecraftState> states = new ArrayList<>();
            // gravitational parameter for Earth
			double MU = 3.986004418e14;

			EventDetector detector = satellitePositionService.setupDetector();
            AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
            Frame inertialFrame = FramesFactory.getEME2000();
            Orbit initialOrbit = satellitePositionService.createInitialOrbit(tleData, inertialFrame, initialDate, MU);
            KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
            propagator.addEventDetector(detector);

			// Propagate orbit for 90 minutes
			AbsoluteDate finalDate = initialDate.shiftedBy(90.0 * 60.0);
			SpacecraftState currentState = propagator.getInitialState();
			while (currentState.getDate().compareTo(finalDate) <= 0) {
				states.add(currentState);
                // Propagate every 60 seconds
				currentState = propagator.propagate(currentState.getDate().shiftedBy(60));
			}
			// Write propagated orbit to CZML file
			satellitePositionService.writeCZML(initialDate, finalDate, states);
            Path filePath = Paths.get("orbit.czml");
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CZML file not found for: " + noradId);
            }

            String czmlContent = Files.readString(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(czmlContent);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to load CZML: " + e.getMessage());
        }
    }
}
