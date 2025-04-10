package com.teamtech.satellitevisualizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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


    public SatelliteData getCurrentLLA(int satId) {
        loadOrekitData();
        return fetchTLE(satId).map(tle -> computeLLA(tle, satId)).orElse(null);
    }


    private SatelliteData computeLLA(TLE tle, int satId) {
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

    private SatelliteData getXYZ(int satId) {
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