package com.teamtech.satellitevisualizer;

import com.teamtech.satellitevisualizer.controller.SatelliteResponse;
import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import com.teamtech.satellitevisualizer.service.SatelliteService;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class SatellitevisualizerApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext context = SpringApplication.run(SatellitevisualizerApplication.class, args);

		// Retrieve the SatellitePositionService bean from the application context
		SatellitePositionService satellitePositionService = context.getBean(SatellitePositionService.class);
		SatelliteService satelliteService = context.getBean(SatelliteService.class);
//		satelliteService.fetchAndSaveTLE(51850);
//		satelliteService.fetchAndSaveTLE(44945);
//		SatelliteResponse satelliteResponse = satelliteService.getSatelliteTLE(44945);
//		String tleData = satelliteResponse.getTle();
//		List<SpacecraftState> states = new ArrayList<>();


		// Call getCurrentLLA with the satellite ID 25544
		String line1 = "1 44945U 20001AH  25092.91668981  .00183380  00000-0  94935-3 0  9991";
		String line2 = "2 44945  53.0463   1.5987 0004901 132.7147  31.9413 15.80006585289523";
		String tleData = "1 44945U 20001AH  25104.58335648  .00211921  00000-0  87018-3 0  9996\n" +
				"2 44945  53.0431 302.8405 0003981 211.2664 213.7415 15.84888341291374";
//		String line1 = "1 51850U 22021A   25093.54661021  .00000085  00000-0  00000+0 0  9992";
//		String line2 = "2 51850   0.0419  20.9525 0000029 187.1507  43.7843  1.00272382 11396";
//		TLE tle = new TLE(line1, line2);
//		int satId = 44945;
//		satellitePositionService.getCurrentLLA(satId);
//		try {
//			// Set the gravitational parameter for Earth (in m^3/s^2)
//			double MU = 3.986004418e14;
//			// Set up an event detector. This will trigger an event when the satellite's elevation above the horizon at a specific location exceeds a certain threshold.
//			EventDetector detector = satellitePositionService.setupDetector();
//
//			// Set the initial date. This is the date at which the orbit is defined.
//			AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
//
//			// Set the inertial frame. This is the frame in which the orbit is defined.
//			Frame inertialFrame = FramesFactory.getEME2000();
//
//			// Create the initial orbit. This defines the initial state of the satellite.
//			Orbit initialOrbit = satellitePositionService.createInitialOrbit(tleData, inertialFrame, initialDate, MU);
//
//			// Set the propagator. This will be used to propagate the orbit over time.
//			KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
//
//			// Add the event detector to the propagator.
//			propagator.addEventDetector(detector);
//
//			// Propagate the orbit for 90 minutes (typical period for LEO satellites).
//			AbsoluteDate finalDate = initialDate.shiftedBy(90.0 * 60.0);
//			SpacecraftState currentState = propagator.getInitialState();
//			while (currentState.getDate().compareTo(finalDate) <= 0) {
//				states.add(currentState);
//				currentState = propagator.propagate(currentState.getDate().shiftedBy(60)); // Propagate every 60 seconds
//			}
//			// Write the propagated orbit to a CZML file. This can be used to visualize the orbit.
//			satellitePositionService.writeCZML(initialDate, finalDate, states);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
}