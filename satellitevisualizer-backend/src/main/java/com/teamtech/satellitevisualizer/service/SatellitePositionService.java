package com.teamtech.satellitevisualizer.service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import java.time.ZonedDateTime;

import java.io.File;
import java.time.ZonedDateTime;

import java.io.File;

public class SatellitePositionService {
    // TODO: accept TLE strings as input
    // function sig here

    // TODO: parse the TLE string input into its 2 lines
    // function sig here

    // TODO: propagate the satellite position using Orekit
    // function sig here

    // TODO: debug! convert the position to lat, long, alt coords
    // right now, the coordinates are pretty of.

    public static void main(String[] args) {
            try {
                File orekitData = new File("src/main/resources/orekit-data");
                DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
                manager.addProvider(new DirectoryCrawler(orekitData));

                // loads tle - example i've been using is
                // https://www.n2yo.com/satellite/?s=62483 - tle
                // https://www.n2yo.com/?s=62483&live=1 - lla
                String line1 = "1 62483U 25001A   25063.90154695 -.00000282  00000-0  00000-0 0  9999";
                String line2 = "2 62483   7.4125 312.2227 5749843 190.4814 148.1700  1.00557765   579";
                // TODO: replace with tle parser once completed

                TLE tle = new TLE(line1, line2);
                Propagator propagator = SGP4.selectExtrapolator(tle);

                // earth model and reference frame
                Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
                OneAxisEllipsoid earth = new OneAxisEllipsoid(
                        Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                        Constants.WGS84_EARTH_FLATTENING,
                        earthFrame
                );

                // utc time
                AbsoluteDate currentDate = new AbsoluteDate(
                        ZonedDateTime.now().getYear(),
                        ZonedDateTime.now().getMonthValue(),
                        ZonedDateTime.now().getDayOfMonth(),
                        ZonedDateTime.now().getHour(),
                        ZonedDateTime.now().getMinute(),
                        ZonedDateTime.now().getSecond(),
                        TimeScalesFactory.getUTC()
                );

                SpacecraftState state = propagator.propagate(currentDate);
                PVCoordinates pvCoordinates = state.getPVCoordinates(earthFrame);

                // position to geodetic coordinates (lat, long, alt)
                GeodeticPoint geodeticPoint = earth.transform(
                        pvCoordinates.getPosition(),
                        earthFrame,
                        currentDate
                );

                double latitude = FastMath.toDegrees(geodeticPoint.getLatitude());
                double longitude = FastMath.toDegrees(geodeticPoint.getLongitude());
                double altitudeKm = geodeticPoint.getAltitude() / 1000.0;

                // print results
                // orekit:
                System.out.println("orekit:");
                System.out.printf("lat:   %.2f°\n", latitude);
                System.out.printf("long:  %.2f°\n", longitude);
                System.out.printf("alt:   %.2f km\n", altitudeKm);

            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }
}
