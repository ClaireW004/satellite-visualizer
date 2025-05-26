/**
 * SatelliteController handles the incoming HTTP requests, which will primarily be the REST API endpoints
 * to fetch and save TLE data to the MongoDB repository.
 *
 * @RestController indicates that this class is a RESTful web service controller.
 * @RequestMapping("/api/satellite") is the base URL path for accessing all endpoints in this controller.
 */

package com.teamtech.satellitevisualizer.controller;

import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.service.SatelliteService;
import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    /**
     * Fetches TLE data for a satellite based on its NORAD ID
     * @param noradId the NORAD ID of the satellite
     * @return SatelliteResponse object containing TLE data
     * @throws RuntimeException if TLE fetch fails
     */
    @GetMapping("/fetch-and-save/{noradId}")
    public SatelliteData fetchAndSaveTLE(@PathVariable int noradId) {
        try {
            return satelliteService.fetchAndSaveTLE(noradId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch and save satellite ", e);
        }
    }

    /**
     * Retrieves the TLE data for a satellite based on its NORAD ID from MongoDB repository
     * @return ResponseEntity<String> containing the TLE data or a 404 status if satellite is not found
     * @CrossOrigin tag allows cross-origin request to be made from our frontend side http://localhost:3000
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
            if (updatedSatellite == null) {
                System.err.printf("Failed to update geodetic coordinates for satellite %d\n", noradId);
                response.put("error", "Failed to update geodetic coordinates.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            response.put("currentLLA", updatedSatellite.getGeodeticCoordinates());

            SatelliteData xyzUpdatedSatellite = satellitePositionService.getXYZ(updatedSatellite.getSatid());
            response.put("currentXYZ", xyzUpdatedSatellite.getXyzCoordinates());

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Satellite not found!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    private static final BodyShape EARTH = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

    /**
     * Generates a CZML file for a satellite's orbit based on its NORAD ID
     * @param noradId of the satellite
     * @return ResponseEntity<String> containing the CZML data or a 404 status if satellite is not found
     */
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
            List<List<Double>> states = new ArrayList<>();

            String[] tleLines = tleData.split("\\r\\n");
            Propagator propagator = TLEPropagator.selectExtrapolator(new TLE(tleLines[0], tleLines[1]));

            // Propagate orbit for 90 minutes
            OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
            OffsetDateTime nowPlus90Min = now.plus(Duration.ofMinutes(90));
            AbsoluteDate nowAbsolute = new AbsoluteDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(),
                    now.getSecond(), TimeScalesFactory.getUTC());
            AbsoluteDate finalDate = new AbsoluteDate(nowPlus90Min.getYear(), nowPlus90Min.getMonthValue(), nowPlus90Min.getDayOfMonth(),
                    nowPlus90Min.getHour(), nowPlus90Min.getMinute(), nowPlus90Min.getSecond(), TimeScalesFactory.getUTC());
            AbsoluteDate currentTime = nowAbsolute;
            double offset = 0;
            while (currentTime.compareTo(finalDate) <= 0) {
                SpacecraftState state = propagator.propagate(currentTime);

                List<Double> cartesianLla = satellitePositionService.convertToCartesian(EARTH.transform(state.getPVCoordinates().getPosition(),
                        FramesFactory.getEME2000(), currentTime));
                cartesianLla.set(0, offset);
                states.add(cartesianLla);
                // Propagate every 60 seconds
                offset += 60;
                currentTime = currentTime.shiftedBy(60);
            }
            // Write propagated orbit to CZML file
            satellitePositionService.writeCZML(nowAbsolute, finalDate, states, noradId);
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

    /**
     * Checks if two satellites are visible to each other based on their NORAD IDs
     * @param noradId1 the NORAD ID of the first satellite
     * @param noradId2 the NORAD ID of the second satellite
     * @return ResponseEntity<String> containing visibility status or a 404 status if either satellite is not found
     */
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{noradId1}/{noradId2}/visible-check")
    public ResponseEntity<String> getVisibility(@PathVariable int noradId1, @PathVariable int noradId2) {
        try {
            SatelliteData satellite1 = satelliteService.getSatelliteBySatid(noradId1);
            SatelliteData satellite2 = satelliteService.getSatelliteBySatid(noradId2);

            if (satellite1 == null || satellite2 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Satellite(s) not found!");
            }

            // Check visibility
            boolean isVisible = satellitePositionService.isVisible(noradId1, noradId2);
            return ResponseEntity.ok("Visibility: " + isVisible);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking visibility: " + e.getMessage());
        }
    }
}
