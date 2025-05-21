/**
 * WebConfig.java
 * This class is responsible for configuring CORS (Cross-Origin Resource Sharing) settings for the application.
 * It allows requests from the specified origin (http://localhost:3000) to access the API endpoints.
 *
 */

package com.teamtech.satellitevisualizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * This method configures CORS (Cross-Origin Resource Sharing) settings for the application.
     * It allows requests from the specified origin (http://localhost:3000) to access the API endpoints.
     *
     * @param registry The CorsRegistry object used to configure CORS settings.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000") // React app URL
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}