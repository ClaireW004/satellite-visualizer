/*
SatelliteRepository provides the database access for SatelliteData entities with Spring Data MongoDB.
 */

package com.teamtech.satellitevisualizer.repository;

import com.teamtech.satellitevisualizer.models.SatelliteData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SatelliteRepository extends MongoRepository<SatelliteData, String> {
    SatelliteData findBySatid(int satid);
}
