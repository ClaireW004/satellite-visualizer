/**
 * SatelliteRepository.java
 * This interface defines the repository for accessing satellite data in the MongoDB database.
 * It extends the MongoRepository interface provided by Spring Data MongoDB.
 * The repository is annotated with @Repository to indicate that it is a Spring Data repository.
 *
 */

package com.teamtech.satellitevisualizer.repository;

import com.teamtech.satellitevisualizer.models.SatelliteData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SatelliteRepository extends MongoRepository<SatelliteData, String> {
    SatelliteData findBySatid(int satid);
}
