package com.teamtech.satellitevisualizer;

import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import org.orekit.propagation.analytical.tle.TLE;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SatellitevisualizerApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(SatellitevisualizerApplication.class, args);

		// Retrieve the SatellitePositionService bean from the application context
		SatellitePositionService satellitePositionService = context.getBean(SatellitePositionService.class);

		// Call getCurrentLLA with the satellite ID 25544
		String line1 = "1 25544U 98067A   25090.50208904  .00016177  00000-0  28736-3 0  9995";
		String line2 = "2 25544  51.6395 331.7733 0003654  68.9145 353.5231 15.50341422503118";
//		TLE tle = new TLE(line1, line2);
		int satId = 25544;
		satellitePositionService.getCurrentLLA(satId);
	}
}