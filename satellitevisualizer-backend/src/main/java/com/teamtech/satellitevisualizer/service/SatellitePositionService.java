/**
 * SatellitePositionService handles the logic for interacting with the N2YO API.
 *  It contains methods to compute a satellite's geodetic coordinates based on its TLE and write its propagated orbit
 *  to a CZML file, which can be used to visualize the satellite's orbit in CesiumJS.
 */

package com.teamtech.satellitevisualizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class SatellitePositionService {
    @Value("${n2yo.api.key}")
    private String apiKey;

    @Autowired
    private SatelliteRepository satelliteRepository;

    /**
     * Loads Orekit data from the specified directory.
     * This method is called to initialize the Orekit library with the necessary data files.
     */
    private void loadOrekitData() {
        File orekitData = new File("src/main/resources/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
    }

    /**
     * Separates a tle string into two lines and constructs a TLE object out of them.
     * @param tleData The TLE data of the satellite.
     * @return A TLE object containing the two separated lines of a TLE string.
     */
    public static Optional<TLE> parseTLE(String tleData) {
        // blank data
        if (tleData == null || tleData.isBlank()) return Optional.empty();
        System.out.println("TLE data" + tleData);
        // splits by newline
        String[] lines = tleData.split("\\r?\\n");
        if (lines.length < 2) {
            System.out.println("Invalid TLE data: Less than 2 lines");
            return Optional.empty(); // invalid tle
        }

        String line1 = lines[0].trim();
        String line2 = lines[1].trim();
        System.out.println("line1: " + line1);
        System.out.println("line2: " + line2);

        try {
            return Optional.of(new TLE(line1, line2));
        } catch (Exception e) {
            System.out.println("Error creating TLE: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static String getLine1(String tleData) {
        if (tleData == null || tleData.isBlank()) {
            throw new IllegalArgumentException("Invalid TLE data: Cannot be null or blank.");
        }
        String[] lines = tleData.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid TLE data: Less than 2 lines.");
        }
        return lines[0].trim();
    }

    public static String getLine2(String tleData) {
        if (tleData == null || tleData.isBlank()) {
            throw new IllegalArgumentException("Invalid TLE data: Cannot be null or blank.");
        }
        String[] lines = tleData.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid TLE data: Less than 2 lines.");
        }
        return lines[1].trim();
    }

    /**
     * Fetches TLE data for a satellite from the database based on its NORAD ID.
     * @param satId The satellite NORAD ID.
     * @return An Optional containing the TLE object if found, or an empty Optional if not found.
     */
    public Optional<TLE> fetchTLE(int satId) {
        System.out.println("satId: " + satId);
        SatelliteData satelliteData = satelliteRepository.findBySatid(satId);
        System.out.println("satelliteData: " + satelliteData);
        Optional<TLE> resTLE = Optional.ofNullable(satelliteData)
                .flatMap(sat -> {
                    System.out.println("Parsing TLE for satellite: " + sat);
                    return parseTLE(sat.getTle());
                });
        System.out.println("resTLE: " + resTLE);
        return resTLE;
    }

    public double parseEccentricity(String line) {
        // Extract the eccentricity from the TLE line
        String eccString = line.substring(26, 33).trim();
        // Convert the string to a double and divide by 1e7 to get the actual eccentricity
        return Double.parseDouble(eccString) / 1e7;
    }

    public double parseInclination(String line) {
        // Extract the inclination from the TLE line
        String incString = line.substring(8, 16).trim();
        return Double.parseDouble(incString);
    }

    public double parsePerigee(String line) {
        // Extract the perigee from the TLE line
        String perigeeString = line.substring(17, 25).trim();
        return Double.parseDouble(perigeeString);
    }

    public double parseRightAscension(String line) {
        // Extract the right ascension from the TLE line
        String raString = line.substring(34, 42).trim();
        return Double.parseDouble(raString);
    }

    public double parseMeanAnomaly(String line) {
        // Extract the mean anomaly from the TLE line
        String maString = line.substring(43, 51).trim();
        return Double.parseDouble(maString);
    }

    public double parseDrag(String line) {
        // Extract the drag from the TLE line
        String dragString = line.substring(52, 63).trim();
        return Double.parseDouble(dragString);
    }

    public SatelliteData getCurrentLLA(int satId) {
        loadOrekitData();
        Optional<TLE> optionalTLE = fetchTLE(satId);

        if (optionalTLE.isEmpty()) {
            System.err.println("TLE data not found for satellite ID: " + satId);
            return null;
        }

        return computeLLA(optionalTLE.get(), satId);    }

    /**
     * Computes the latitude, longitude, and altitude of a satellite based on its TLE data.
     * @param tle The TLE data of the satellite.
     * @param satId The satellite norad ID.
     * @return A SatelliteData object containing the geodetic coordinates.
     */
    public SatelliteData computeLLA(TLE tle, int satId) {
        try {
            Propagator propagator = SGP4.selectExtrapolator(tle);

            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame
            );

            OffsetDateTime offsetDateTime = Instant.now().atOffset(ZoneOffset.UTC);
            int year = offsetDateTime.getYear();
            int month = offsetDateTime.getMonthValue();
            int day = offsetDateTime.getDayOfMonth();
            int hour = offsetDateTime.getHour();
            int minute = offsetDateTime.getMinute();
            int second = offsetDateTime.getSecond();
            AbsoluteDate currentDate = new AbsoluteDate(
                    year, month, day, hour, minute, second, TimeScalesFactory.getUTC()
            );

            SpacecraftState state = propagator.propagate(currentDate);
            PVCoordinates pvCoordinates = state.getPVCoordinates(earthFrame);

            GeodeticPoint geodeticPoint = earth.transform(
                    pvCoordinates.getPosition(),
                    earthFrame,
                    currentDate
            );

            double latitude = FastMath.toDegrees(geodeticPoint.getLatitude());
            double longitude = FastMath.toDegrees(geodeticPoint.getLongitude());
            double altitudeKm = geodeticPoint.getAltitude() / 1000.0;

            SatelliteData satelliteData = satelliteRepository.findBySatid(satId);
            if (satelliteData != null) {
                List<List<Double>> coordinates = List.of(Arrays.asList(latitude, longitude, altitudeKm));
                satelliteData.setGeodeticCoordinates(coordinates);
                satelliteRepository.save(satelliteData);
            }

            return satelliteData;

        } catch (OrekitException e) {
            e.getMessage();
            return null;
        }
    }

    private static final BodyShape EARTH = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

    /**
     * Computes the cartesian coordinates of a satellite based on its geodetic coordinates.
     * @param satId The satellite norad ID.
     * @return A SatelliteData object containing the cartesian coordinates.
     */
    public SatelliteData getXYZ(int satId) {
        double latitude=0, longitude=0, altitudeKm=0;
        SatelliteData satelliteData = satelliteRepository.findBySatid(satId);

        if (satelliteData == null) return null;

        List<List<Double>> coords = satelliteData.getGeodeticCoordinates();
        System.out.println("Geodetic Coordinates: " + coords);
        Vector3D cartesianPoint = new Vector3D(0, 0, 0); // Initialize to zero vector

        if (coords != null && !coords.isEmpty() && coords.get(0).size() == 3) {
            // to be edited with the specific current / future coords
            latitude = coords.get(0).get(0);
            longitude = coords.get(0).get(1);
            altitudeKm = coords.get(0).get(2);

            double latitudeRad = Math.toRadians(latitude);
            double longitudeRad = Math.toRadians(longitude);
            double altitudeMeters = altitudeKm * 1000;

            System.out.println("Latitude: " + latitude + ", Longitude: " + longitude + ", Altitude (km): " + altitudeKm);
            GeodeticPoint geodeticPoint = new GeodeticPoint(latitudeRad, longitudeRad, altitudeMeters);

            // Transform the GeodeticPoint to a Cartesian point
            cartesianPoint = EARTH.transform(geodeticPoint);
        }

        double x = cartesianPoint.getX();
        double y = cartesianPoint.getY();
        double z = cartesianPoint.getZ();

        System.out.println("Cartesian Coordinates: (" + x + ", " + y + ", " + z + ")");

        List<List<Double>> coordinates = List.of(Arrays.asList(x, y, z));
        satelliteData.setXYZCoordinates(coordinates);
        satelliteRepository.save(satelliteData);

        return satelliteData;
    }

    /**
     * Converts geodetic coordinates to Cartesian coordinates.
     * @param geodeticPoint The geodetic point containing latitude, longitude, and altitude.
     * @return A list of Cartesian coordinates (x, y, z).
     */
    public List<Double> convertToCartesian(GeodeticPoint geodeticPoint) {
        Vector3D cartesianPoint = new Vector3D(0, 0, 0);
        if (geodeticPoint != null) {
            cartesianPoint = EARTH.transform(geodeticPoint);
        }
        double x = cartesianPoint.getX();
        double y = cartesianPoint.getY();
        double z = cartesianPoint.getZ();
        System.out.println("Converted Cartesian Coordinates: (" + x + ", " + y + ", " + z + ")");
        return new ArrayList<>(List.of(0D, x, y, z)); // 0D is a placeholder for time, can be replaced with actual time if needed
    }

    /**
     * Writes the propagated orbit of a satellite to a CZML file.
     *
     * @param initialDate The initial date of the propagation.
     * @param finalDate   The final date of the propagation.
     * @param states      A list of spacecraft states representing the satellite's state at different points in time.
     *                    The states must be ordered by date, from earliest to latest.
     * @param noradId     The NORAD ID of the satellite.
     *
     * The method generates "orbit.czml" in the root directory of the backend project. It stores the
     * satellite's position at each time step in Cartesian coordinate format (x, y, z). The positions are
     * interpolated using the LAGRANGE algorithm with a degree of 5.
     *
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    public void writeCZML(AbsoluteDate initialDate, AbsoluteDate finalDate, List<List<Double>> states, int noradId) {
        try (FileWriter writer = new FileWriter("orbit.czml")) {
            // Write the CZML header
            writer.write("[\n");
            writer.write("{\"id\":\"document\",\"version\":\"1.0\"},\n");

            // Write the satellite's path
            writer.write("{\"id\":\"Satellite " + noradId + "\",\"availability\":\"" + initialDate + "/" + finalDate + "\",\n");
            writer.write("\"position\":{\"interpolationAlgorithm\":\"LAGRANGE\",\"interpolationDegree\":5,\"epoch\":\"" + initialDate + "\",\"cartesian\":[");

            // Write the satellite's position at each time step
            for (int i = 0; i < states.size(); i++) {
                List<Double> offsetLla = states.get(i);
                writer.write("\n" + offsetLla.get(0) + "," + offsetLla.get(1) + "," + offsetLla.get(2) + "," + offsetLla.get(3));
                if (i < states.size() - 1) {
                    writer.write(",");
                }
            }

            // Write the CZML footer
            writer.write("\n]},\n");

            // Write the satellite's label
            writer.write("\"label\":{"
                    + "\"text\":\"Sat " + noradId + "\","
                    + "\"font\":\"14px Helvetica\","
                    + "\"fillColor\":{\"rgba\":[255,255,0,255]},"
                    + "\"outlineColor\":{\"rgba\":[0,0,0,255]},"
                    + "\"outlineWidth\":2,"
                    + "\"style\":\"FILL\","
                    + "\"horizontalOrigin\":\"LEFT\","
                    + "\"verticalOrigin\":\"BOTTOM\","
                    + "\"pixelOffset\":{\"cartesian2\":[10,-10]}"
                    + "},\n");

            writer.write("\"path\":{\"show\":[{\"boolean\":true}]},\n");
            writer.write("\"point\":{\"pixelSize\":10,\"color\":{\"rgba\":[255,255,0,255]}},\n");
            writer.write("\"description\":\"Satellite " + noradId + "\"}\n");
            writer.write("]\n");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    /**
     * Checks if two satellites are visible to each other based on their positions.
     * @param satId1 The first satellite's NORAD ID.
     * @param satId2 The second satellite's NORAD ID.
     * @return true if the satellites are visible to each other, false otherwise.
     */
    public boolean isVisible(int satId1, int satId2) {
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        // gets interested satellites in db
        SatelliteData sat1 = satelliteRepository.findBySatid(satId1);
        SatelliteData sat2 = satelliteRepository.findBySatid(satId2);

        // calculates xyz
        double x1 = sat1.getXyzCoordinates().get(0).get(0);
        double y1 = sat1.getXyzCoordinates().get(0).get(1);
        double z1 = sat1.getXyzCoordinates().get(0).get(2);
        double x2 = sat2.getXyzCoordinates().get(0).get(0);
        double y2 = sat2.getXyzCoordinates().get(0).get(1);
        double z2 = sat2.getXyzCoordinates().get(0).get(2);

        // calculates change
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        System.out.println("dx: " + dx + " dy: " + dy + " dz: " + dz);
        double distance = FastMath.sqrt(dx * dx + dy * dy + dz * dz);
        System.out.println("distance: " + distance);

        double x_dot = dx / distance;
        double y_dot = dy / distance;
        double z_dot = dz / distance;
        System.out.printf("x_dot: %.2f, y_dot: %.2f, z_dot: %.2f\n", x_dot, y_dot, z_dot);

        double earthRadius = 6378137.0;
        double step = 10000; // in meters

        boolean visible = true;
        for (double i = 0; i <= distance; i += step) {
            double x = x1 + x_dot * i;
            double y = y1 + y_dot * i;
            double z = z1 + z_dot * i;
            System.out.printf("x: %.2f, y: %.2f, z: %.2f\n", x, y, z);

            double r = FastMath.sqrt(x * x + y * y + z * z);
            System.out.printf("r: %.2f\n", r);

            if (r <= earthRadius) {
                System.out.println("smaller than earth radius: " + r);
                visible = false;
                System.out.println("satellites cannot see each other at time: " + hour + ":" + minute + ":" + second);
                break;
            }
        }

        if (visible) {
            System.out.println("satellites are visible to each other at time: " + hour + ":" + minute + ":" + second);
        }

        return visible;
    }

    /**
     * Gets new TLE data for all satellites from the N2YO API and updates the database every 24 hours.
     */
    @Scheduled(fixedRate = 86400000) // 24 hours in ms
    public void refreshTLEs() {
        // get all satellites from the db
        List<SatelliteData> allSatellites = satelliteRepository.findAll();
        for (SatelliteData satellite : allSatellites) {
            int satId = satellite.getSatid();

            try {
                // sends api request to get new tle
                String url = String.format("https://api.n2yo.com/rest/v1/satellite/tle/%d&apiKey=%s", satId, apiKey);
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode root = objectMapper.readTree(response.getBody());

                    // saves new tle to db
                    String newTle = root.get("tle").asText();
                    satellite.setTle(newTle);
                    satelliteRepository.save(satellite);

                    System.out.printf("updated for satellite %d\n", satId);
                    SatelliteData updatedSatellite = getCurrentLLA(satId);
                    if (updatedSatellite == null) {
                        System.err.printf("Failed to update geodetic coordinates for satellite %d\n", satId);
                        continue;
                    }
                    getXYZ(satId);
                }
            } catch (Exception e) {
                System.err.printf("failed for satellite %d: %s\n", satId, e.getMessage());
            }
        }
    }
}