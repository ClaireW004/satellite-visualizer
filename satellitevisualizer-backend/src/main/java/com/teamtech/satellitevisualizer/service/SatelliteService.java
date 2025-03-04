/*
SatelliteService handles the logic for interacting with the N2YO API.
It contains methods to fetch TLE data from the API and optionally save it to the database

@Service tag indicates that this class is a service component of the application, which contains the core logic.
@Autowired annotation is used for dependency injection for SatelliteRepository interface.
 */

package com.teamtech.satellitevisualizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import com.teamtech.satellitevisualizer.controller.SatelliteResponse;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.File;

@Service
public class SatelliteService {

    // Base URL for the N2YO API to fetch the TLE data
    private final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/tle/";
    @Value("${n2yo.api.key}")
    private String API_KEY;

    @Autowired
    private SatelliteRepository satelliteRepository;

    @Autowired
    public SatelliteService(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    /*
    Retrieves a SatelliteData object from MongoDB database based on the satellite's NORAD ID
    Returns the SatelliteData objects
     */
    public SatelliteData getSatelliteBySatid(int satid) {
        return satelliteRepository.findBySatid(satid);
    }

    /*
    Fetches TLE data for a satellite from N2YO API
    Returns a SatelliteResponse
    Throws exception if TLE fetch fails
     */
    public SatelliteResponse getSatelliteTLE(int noradId) throws Exception {
        String url = BASE_URL + noradId + "?apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response.getBody(), SatelliteResponse.class);
    }

    /*
    Saves satellite data in MongoDB
    Returns a SatelliteData object
     */
    public SatelliteData saveSatelliteData(SatelliteData satellite) {
        System.out.println("Saving satellite: " + satellite);
        return satelliteRepository.save(satellite);
    }

    /*
    Fetches TLE from the N2YO API, creates SatelliteData object based on norad ID, and saves that to MongoDB database
    Returns a SatelliteData object
    Throws exception if TLE fetch fails
     */
    public SatelliteData fetchAndSaveTLE(int noradId) throws Exception {
        System.out.println("Fetching and saving TLE for NORAD ID: " + noradId);
        SatelliteResponse satelliteResponse = getSatelliteTLE(noradId);

        SatelliteData satellite = new SatelliteData();
        satellite.setSatid(satelliteResponse.getInfo().getSatid());
        satellite.setSatname(satelliteResponse.getInfo().getSatname());
        satellite.setTle(satelliteResponse.getTle());

        return saveSatelliteData(satellite);
    }


    public void Orekit() {
        File orekitData = new File("src/main/resources/orekit-data");  // orekit data
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // load tle data
        String line1 = "1 25544U 98067A   23235.51284918  .00014007  00000-0  25659-3 0  9995";
        String line2 = "2 25544  51.6426 355.0105 0003727 342.0009 113.8232 15.49590945412235";

        // create tle object (define class)
        TLE tle = new TLE(line1, line2);

        // create tle propagator
        Propagator propagator = SGP4.selectExtrapolator(tle);

        // earth and frame
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // gets the earth frame
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, // creates an earth model
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        // station setup (latitude, longitude, altitude)
        double longitude = FastMath.toRadians(77.33);
        double latitude = FastMath.toRadians(28.58);
        double altitude = 0.0;
        GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        TopocentricFrame stationFrame = new TopocentricFrame(earth, station1, "station1");

        // event definition
        double maxCheck = 60.0; // check every 60 seconds
        double threshold = 0.001;
        double elevation = FastMath.toRadians(15.0);

        // create event detector
        ElevationDetector visibilityDetector = new ElevationDetector(maxCheck, threshold, stationFrame)
                .withConstantElevation(elevation)
                .withHandler((s, detector, increasing) -> {
                    System.out.println("visibility at " + stationFrame.getName() +
                            (increasing ? " begins at " : " ends at ") +
                            s.getDate());
                    return increasing ? Action.CONTINUE : Action.STOP;
                });

        // add event detector to propagator
        propagator.addEventDetector(visibilityDetector);

        // propagate satellite state (e.g., from the initial tle epoch)
        AbsoluteDate initialDate = tle.getDate();
        SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(555000.)); // propagate for a duration

        // print final state
        System.out.println("final state: " + finalState.getDate().durationFrom(initialDate));

        // print final state
        System.out.println("final state: " + finalState.getDate().durationFrom(initialDate));

        // calculate and print latitude, longitude, and altitude
        PVCoordinates pvCoordinates = finalState.getPVCoordinates();
        GeodeticPoint geodeticPoint = earth.transform(pvCoordinates.getPosition(), finalState.getFrame(), finalState.getDate());

        double latitudeRad = geodeticPoint.getLatitude();
        double longitudeRad = geodeticPoint.getLongitude();
        double altitudeMeters = geodeticPoint.getAltitude();
        double altitudeMiles = altitudeMeters * 0.000621371;

        double latitudeDeg = FastMath.toDegrees(latitudeRad);
        double longitudeDeg = FastMath.toDegrees(longitudeRad);

        System.out.println("latitude: " + latitudeDeg + " degrees");
        System.out.println("longitude: " + longitudeDeg + " degrees");
        System.out.println("altitude: " + altitudeMiles + " miles");
    }
}