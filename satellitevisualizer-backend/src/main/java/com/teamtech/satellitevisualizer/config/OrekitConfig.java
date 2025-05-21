package com.teamtech.satellitevisualizer.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrekitConfig {
    @PostConstruct
    public void init() {
        System.setProperty("orekit.data.path", "src/main/resources/orekit-data");
    }

}