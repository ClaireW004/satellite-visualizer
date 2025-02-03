/*
WebConfig ensures that the backend allows requests to be made from our frontend application at http://localhost:3000
 */

package com.teamtech.satellitevisualizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000") // React app URL
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}