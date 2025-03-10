package com.teamtech.satellitevisualizer.service;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SatellitePositionService {
    public static void convertTLE_manual(ArrayList elements) {
        double inclination = Double.parseDouble((String) elements.get(1));
        double rightAscension = Double.parseDouble((String) elements.get(2));
        double eccentricity = Double.parseDouble("0." + (String) elements.get(3));
        double perigee = Double.parseDouble((String) elements.get(4));
        double meanAnomaly = Double.parseDouble((String) elements.get(5));
        double semiMajorAxis = 149598023; // semi-major axis of Earth's orbit in km, need to be able to get this value for different satellites

        // TODO 1. Convert Mean Anomaly to True Anomaly
        double epsilon = 1e-6; // Convergence threshold
        double E = meanAnomaly; // Initial guess for E
        double delta;

        do {
            delta = E - eccentricity * Math.sin(E) - meanAnomaly;
            E = E - delta / (1 - eccentricity * Math.cos(E));
        } while (Math.abs(delta) > epsilon);

        // Convert E to True Anomaly (ν)
        double trueAnomaly = 2 * Math.atan2(
                Math.sqrt(1 + eccentricity) * Math.sin(E / 2),
                Math.sqrt(1 - eccentricity) * Math.cos(E / 2)
        );

        System.out.println("Calculated True Anomaly: " + trueAnomaly);

        // TODO 2. Compute the satellite's position in the orbital plane using kepler's third law and the perifocal system.
        double r = (semiMajorAxis * (1 - eccentricity * eccentricity)) /
                (1 + eccentricity * Math.cos(trueAnomaly));

        // Compute perifocal coordinates
        double rx = r * Math.cos(trueAnomaly);
        double ry = r * Math.sin(trueAnomaly);
        double rz = 0;

        // Put these coordinates in a vector
        double[] rVec = {rx, ry, rz};
        System.out.println("Calculated PQW Position Coordinates: " + Arrays.toString(rVec));

        // TODO 3. Rotate to ECI coordinates
        double [][] R = {
                {Math.cos(rightAscension) * Math.cos(perigee) - Math.sin(rightAscension) * Math.sin(perigee) * Math.cos(inclination), -Math.cos(rightAscension) * Math.sin(perigee) - Math.sin(rightAscension) * Math.cos(perigee) * Math.cos(inclination), Math.sin(rightAscension) * Math.sin(inclination)},
                {Math.sin(rightAscension) * Math.cos(perigee) + Math.cos(rightAscension) * Math.sin(perigee) * Math.cos(inclination), -Math.sin(rightAscension) * Math.sin(perigee) + Math.cos(rightAscension) * Math.cos(perigee) * Math.cos(inclination), -Math.cos(rightAscension) * Math.sin(inclination)},
                {Math.sin(perigee) * Math.sin(inclination), Math.cos(perigee) * Math.sin(inclination), Math.cos(inclination)}
        };

        double[] eciCoordinates = new double[3];
        eciCoordinates[0] = R[0][0] * rVec[0] + R[0][1] * rVec[1] + R[0][2] * rVec[2];
        eciCoordinates[1] = R[1][0] * rVec[0] + R[1][1] * rVec[1] + R[1][2] * rVec[2];
        eciCoordinates[2] = R[2][0] * rVec[0] + R[2][1] * rVec[1] + R[2][2] * rVec[2];

        // TODO 4. Convert ECI to ECEF
        // TODO 5. Convert ECEF to Geodetic LLA
    }

    public static List<double[]> convertTLE_auto(TLE tle) {
        List<double[]> llaCoordinatesList = new ArrayList<>();
        // Convert Mean Anomaly to True Anomaly
        // Does it matter if we use Propagator or TLEPropagator?
        TLEPropagator propagator = SGP4.selectExtrapolator(tle);

        // Define Earth model and reference frame
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame
        );

        // Define time step and initial propagation time
        AbsoluteDate initialDate = tle.getDate();
        double timeStep = 60.0; // Propagate every 60 seconds

        // Propagate for multiple points in time
        for (double t = 0; t < 6000; t += timeStep) { // Example: 6000 seconds (~1.6 hours)
            AbsoluteDate date = initialDate.shiftedBy(t);
            SpacecraftState state = propagator.propagate(date);

            // Get satellite position in ECEF
            PVCoordinates pvCoordinates = state.getPVCoordinates();
            GeodeticPoint geodeticPoint = earth.transform(pvCoordinates.getPosition(), earthFrame, date);

            // Convert to degrees
            double latitude = FastMath.toDegrees(geodeticPoint.getLatitude());
            double longitude = FastMath.toDegrees(geodeticPoint.getLongitude());
            double altitude = geodeticPoint.getAltitude();

            double[] lla = {latitude, longitude, altitude};
            llaCoordinatesList.add(lla);

            // Print LLA coordinates
//            System.out.printf("Time: %.0f sec | Lat: %.6f | Lon: %.6f | Alt: %.2f km\n",
//                    t, latitude, longitude, altitude / 1000);
        }
        return llaCoordinatesList;
    }

    public static ArrayList parseTLE(String tleLine) {
        // Normalize spaces (replace multiple spaces with a single space)
        String normalizedTLE = tleLine.replaceAll("\\s+", " ");

        // Split the string by space
        String[] elements = normalizedTLE.split(" ");

        // Access individual elements
        String satelliteNumber = elements[0];
        String inclination = elements[2];
        String rightAscension = elements[3];
        String eccentricity = elements[4];
        String perigee = elements[5];
        String meanAnomaly = elements[6];
        String meanMotion = elements[7];

        // Add the elements to an ArrayList
        ArrayList<String> elementsList = new ArrayList<>(Arrays.asList(
                satelliteNumber, inclination, rightAscension, eccentricity, perigee, meanAnomaly, meanMotion
        ));

        // Print out parsed elements
        System.out.println("Satellite Number: " + satelliteNumber);
        System.out.println("Inclination: " + inclination);
        System.out.println("Right Ascension: " + rightAscension);
        System.out.println("Eccentricity: " + eccentricity);
        System.out.println("Perigee: " + perigee);
        System.out.println("Mean Anomaly: " + meanAnomaly);
        System.out.println("Mean Motion: " + meanMotion);

        return elementsList;
    }


    public static void main(String[] args) {
        /*** how to push and pull from git
         pull to your branch: git pull <remote> <branch>

         ex: git pull origin main pulls updates from the main branch of the origin remote

         so, to pull changes from my master backend branch use:
         git pull origin backend

         add this, then push it to your branch!
         then, push it to my master branch too :)
         ***/

//        File orekitData = new File("src/main/resources/orekit-data");
//        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
//        manager.addProvider(new DirectoryCrawler(orekitData));

//        System.out.println("hi from Cara");
        // push a line with your name!

//        String line1 = "1 25544U 98067A   23235.51284918  .00014007  00000-0  25659-3 0  9995";
//        String line2 = "2 25544  51.6426 355.0105 0003727 342.0009 113.8232 15.49590945412235";
//
//        parseTLE(line2);

    }

}
