package com.teamtech.satellitevisualizer;

import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.propagation.analytical.tle.TLE;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.List;

@SpringBootApplication
public class SatellitevisualizerApplication {

	public static void main(String[] args) {
		File orekitData = new File("src/main/resources/orekit-data");
		DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
		manager.addProvider(new DirectoryCrawler(orekitData));

		SpringApplication.run(SatellitevisualizerApplication.class, args);

		String line1 = "1 25544U 98067A   23235.51284918  .00014007  00000-0  25659-3 0  9995";
		String line2 = "2 25544  51.6426 355.0105 0003727 342.0009 113.8232 15.49590945412235";
		TLE tle = new TLE(line1, line2);

		// Automated conversion using Orekit and SGP4 propagator
//		List<double[]> llaList = SatellitePositionService.convertTLE_auto(tle);
//		for (double[] lla : llaList) {
//			System.out.printf("%.6f, %.6f, %.6f%n", lla[0], lla[1], lla[2]);
//		}

		// Manual conversion using mathematical calculations
		SatellitePositionService.convertTLE_manual(SatellitePositionService.parseTLE(line2));
	}
}
