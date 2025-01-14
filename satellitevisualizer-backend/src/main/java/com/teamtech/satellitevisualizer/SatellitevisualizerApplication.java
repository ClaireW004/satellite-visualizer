package com.teamtech.satellitevisualizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SatellitevisualizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SatellitevisualizerApplication.class, args);
	}

	@GetMapping("/")
	public String apiRoot() {
		return "Hi Team Tech";
	}
}
