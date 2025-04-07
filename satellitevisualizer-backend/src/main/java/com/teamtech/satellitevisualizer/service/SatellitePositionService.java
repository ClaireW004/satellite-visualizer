package com.teamtech.satellitevisualizer.service;

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
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class SatellitePositionService {

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
//        return Optional.of(new TLE(line1, line2));
    }

    public Optional<TLE> fetchTLE(int satId) {
        System.out.println("satId: " + satId);
//        return satelliteRepository.findById(String.valueOf(satId))
//                .flatMap(sat -> parseTLE(sat.getTle()));
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
//        fetchTLE(satId).ifPresentOrElse(tle -> computeLLA(tle, satId),
//                () -> System.out.println("invalid tle"));
    }

//    public void getFutureLLA(int satId, ZonedDateTime futureTime) {
//        fetchTLE(satId).ifPresentOrElse(tle -> computeLLA(tle, futureTime),
//                () -> System.out.println("invalid tle"));
//    }

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
//            AbsoluteDate currentDate = new AbsoluteDate(
//                    dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
//                    dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
//                    TimeScalesFactory.getUTC()
//            );

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

            System.out.println("Position at " + currentDate + ":");
            System.out.printf("Latitude:  %.2f°\n", latitude);
            System.out.printf("Longitude: %.2f°\n", longitude);
            System.out.printf("Altitude:  %.2f km\n", altitudeKm);
            System.out.printf("x: %.2f\n", x);
            System.out.printf("y: %.2f\n", y);
            System.out.printf("z: %.2f\n", z);

            SatelliteData satelliteData = satelliteRepository.findBySatid(satId);
            if (satelliteData != null) {
                List<List<Double>> coordinates = Arrays.asList(Arrays.asList(latitude, longitude, altitudeKm));
                satelliteData.setGeodeticCoordinates(coordinates);
                satelliteRepository.save(satelliteData);
            }

            return satelliteData;

        } catch (OrekitException e) {
            e.printStackTrace();
            return null;
        }
    }
}