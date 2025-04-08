package com.teamtech.satellitevisualizer;

import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import com.teamtech.satellitevisualizer.service.SatelliteService;
import org.orekit.propagation.analytical.tle.TLE;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SatellitevisualizerApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext context = SpringApplication.run(SatellitevisualizerApplication.class, args);

		// Retrieve the SatellitePositionService bean from the application context
//		SatellitePositionService satellitePositionService = context.getBean(SatellitePositionService.class);
//		SatelliteService satelliteService = context.getBean(SatelliteService.class);
//		satelliteService.fetchAndSaveTLE(51850);
//		satelliteService.fetchAndSaveTLE(44945);

		// Call getCurrentLLA with the satellite ID 25544
		String line1 = "1 44945U 20001AH  25092.91668981  .00183380  00000-0  94935-3 0  9991";
		String line2 = "2 44945  53.0463   1.5987 0004901 132.7147  31.9413 15.80006585289523";
//		String line1 = "1 51850U 22021A   25093.54661021  .00000085  00000-0  00000+0 0  9992";
//		String line2 = "2 51850   0.0419  20.9525 0000029 187.1507  43.7843  1.00272382 11396";
//		TLE tle = new TLE(line1, line2);
//		int satId = 44945;
//		satellitePositionService.getCurrentLLA(satId);
	}
}