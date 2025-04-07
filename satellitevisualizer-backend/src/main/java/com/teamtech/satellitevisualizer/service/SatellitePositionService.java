package com.teamtech.satellitevisualizer.service;

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
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class SatellitePositionService {

    @Autowired
    private SatelliteRepository satelliteRepository;

    // separates a tle string into two lines
    public static Optional<TLE> parseTLE(String tleData) {
        // blank data
        if (tleData == null || tleData.isBlank()) return Optional.empty();

        // splits by newline
        String[] lines = tleData.split("\n");
        if (lines.length < 2) return Optional.empty(); // invalid tle

        String line1 = lines[0].trim();
        String line2 = lines[1].trim();

        return Optional.of(new TLE(line1, line2));
    }

    public Optional<TLE> fetchTLE(int satId) {
        return satelliteRepository.findById(String.valueOf(satId))
                .flatMap(sat -> parseTLE(sat.getTle()));
    }

    public void getCurrentLLA(int satId) {
        fetchTLE(satId).ifPresentOrElse(tle -> computeLLA(tle, ZonedDateTime.now(ZoneOffset.UTC)),
                () -> System.out.println("invalid tle"));
    }

    public void getFutureLLA(int satId, ZonedDateTime futureTime) {
        fetchTLE(satId).ifPresentOrElse(tle -> computeLLA(tle, futureTime),
                () -> System.out.println("invalid tle"));
    }

    private void computeLLA(TLE tle, ZonedDateTime dateTime) {
        try {
            File orekitData = new File("src/main/resources/orekit-data");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));

            Propagator propagator = SGP4.selectExtrapolator(tle);

            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame
            );

            AbsoluteDate currentDate = new AbsoluteDate(
                    dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                    dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                    TimeScalesFactory.getUTC()
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

            System.out.println("Position at " + dateTime + ":");
            System.out.printf("Latitude:  %.2f°\n", latitude);
            System.out.printf("Longitude: %.2f°\n", longitude);
            System.out.printf("Altitude:  %.2f km\n", altitudeKm);

        } catch (OrekitException e) {
            e.printStackTrace();
        }
    }
}