package com.teamtech.satellitevisualizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
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

    private void loadOrekitData() {
        File orekitData = new File("src/main/resources/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
    }

    // separates a tle string into two lines
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
        return fetchTLE(satId).map(tle -> computeLLA(tle, satId)).orElse(null);
    }

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

    public SatelliteData getXYZ(int satId) {
        double latitude=0, longitude=0, altitudeKm=0;
        SatelliteData satelliteData = satelliteRepository.findBySatid(satId);

        if (satelliteData == null) return null;

        List<List<Double>> coords = satelliteData.getGeodeticCoordinates();

        if (coords != null && !coords.isEmpty() && coords.get(0).size() == 3) {
            // to be edited with the specific current / future coords
            latitude = coords.get(0).get(0);
            longitude = coords.get(0).get(1);
            altitudeKm = coords.get(0).get(2);
        }

        // (L, L, A) -> (x, y, z)
        var cosLat = FastMath.cos(latitude * FastMath.PI / 180.0);
        var sinLat = FastMath.sin(latitude * FastMath.PI / 180.0);
        var cosLon = FastMath.cos(longitude * FastMath.PI / 180.0);
        var sinLon = FastMath.sin(longitude * FastMath.PI / 180.0);
        var rad = 6378137.0;
        var f = 1.0 / 298.257224;
        var C = 1.0 / FastMath.sqrt(cosLat * cosLat + (1 - f) * (1 - f) * sinLat * sinLat);
        var S = (1.0 - f) * (1.0 - f) * C;
        var h = 0.0;
        double x = (rad * C + h) * cosLat * cosLon;
        double y = (rad * C + h) * cosLat * sinLon;
        double z = (rad * S + h) * sinLat;

        System.out.printf("x: %.2f\n", x);
        System.out.printf("y: %.2f\n", y);
        System.out.printf("z: %.2f\n", z);

        List<List<Double>> coordinates = List.of(Arrays.asList(x, y, z));
        satelliteData.setXYZCoordinates(coordinates);
        satelliteRepository.save(satelliteData);

        return satelliteData;
    }

    /**
     * Sets up an event detector for the satellite propagation.
     * The detector triggers an event when the satellite's elevation above the horizon at a specific location exceeds a certain threshold.
     * @return the event detector
     */
    public EventDetector setupDetector() {
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        GeodeticPoint charlottesville = new GeodeticPoint(Math.toRadians(38.0293), Math.toRadians(-78.4767), 0.0);
        TopocentricFrame charlottesvilleFrame = new TopocentricFrame(earth, charlottesville, "Charlottesville");
        double maxcheck = 60.0;
        double threshold = 0.001;
        double elevation = Math.toRadians(5.0);
        EventDetector detector = new ElevationDetector(maxcheck, threshold, charlottesvilleFrame).
                withConstantElevation(elevation).
                withHandler(new EventHandler() {
                    @Override
                    public Action eventOccurred(SpacecraftState spacecraftState, EventDetector eventDetector, boolean b) {
                        System.out.println("BANG! ->" + spacecraftState.getDate().toString() + " : " + spacecraftState.getOrbit().getPVCoordinates().toString());
                        return Action.CONTINUE;
                    }
                });
        return detector;
    }

    /**
     * Creates the initial orbit for the satellite propagation.
     * a - the semi-major axis
     * e - the eccentricity
     * i - the inclination
     * omega - the perigee argument
     * raan - the right ascension of the ascending node
     * lM - the mean anomaly
     * inertialFrame - the frame in which the orbit is defined
     * initialDate - the date at which the orbit is defined
     * mu - the gravitational parameter
     * @return the initial orbit
     */
    public Orbit createInitialOrbit(String tleData, Frame inertialFrame, AbsoluteDate initialDate, double mu) {
        String line1 = getLine1(tleData);
        String line2 = getLine2(tleData);

        double a = 6378137.0;
        double e = parseEccentricity(line2);
        double i = parseInclination(line2);
        double omega = parsePerigee(line2);
        double raan = parseRightAscension(line2);
        double lM = parseMeanAnomaly(line2);

        return new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, initialDate, mu);
    }

    /**
     * Writes the propagated orbit of a satellite to a CZML file.
     *
     * @param initialDate The initial date of the propagation.
     * @param finalDate   The final date of the propagation.
     * @param states      A list of spacecraft states representing the satellite's state at different points in time.
     *                    The states must be ordered by date, from earliest to latest.
     *
     * The method generates a CZML file named "orbit.czml" in the project's root directory. The file contains the
     * satellite's position at each time step, represented as a Cartesian coordinate (x, y, z). The positions are
     * interpolated using the LAGRANGE algorithm with a degree of 5.
     *
     * The CZML file can be used to visualize the satellite's orbit in a 3D viewer that supports CZML, such as CesiumJS.
     *
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    public void writeCZML(AbsoluteDate initialDate, AbsoluteDate finalDate, List<SpacecraftState> states) {
        try (FileWriter writer = new FileWriter("orbit.czml")) {
            // Write the CZML header
            writer.write("[\n");
            writer.write("{\"id\":\"document\",\"version\":\"1.0\"},\n");

            // Write the satellite's path
            writer.write("{\"id\":\"satellite\",\"availability\":\"" + initialDate + "/" + finalDate + "\",\n");
            writer.write("\"position\":{\"interpolationAlgorithm\":\"LAGRANGE\",\"interpolationDegree\":5,\"epoch\":\"" + initialDate + "\",\"cartesian\":[");

            // Write the satellite's position at each time step
            for (int i = 0; i < states.size(); i++) {
                SpacecraftState state = states.get(i);
                double[] position = state.getPVCoordinates().getPosition().toArray();
                writer.write("\n" + state.getDate().durationFrom(initialDate) + "," + position[0] + "," + position[1] + "," + position[2]);
                if (i < states.size() - 1) {
                    writer.write(",");
                }
            }

            // Write the CZML footer
            writer.write("\n]},\n");
            writer.write("\"path\":{\"show\":[{\"boolean\":true}]},\n");
            writer.write("\"point\":{\"pixelSize\":5,\"color\":{\"rgba\":[255,255,0,255]}}}\n");
            writer.write("]\n");
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

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
        double distance = FastMath.sqrt(dx * dx + dy * dy + dz * dz);

        double x_dot = dx / distance;
        double y_dot = dy / distance;
        double z_dot = dz / distance;

        double earthRadius = 6378137.0;
        double step = 10000; // in meters

        boolean visible = true;
        for (double i = 0; i <= distance; i += step) {
            double x = x1 + x_dot * i;
            double y = y1 + y_dot * i;
            double z = z1 + z_dot * i;

            double r = FastMath.sqrt(x * x + y * y + z * z);

            if (r <= earthRadius) {
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
                    getCurrentLLA(satId);
                    getXYZ(satId);
                }
            } catch (Exception e) {
                System.err.printf("failed for satellite %d: %s\n", satId, e.getMessage());
            }
        }
    }
}