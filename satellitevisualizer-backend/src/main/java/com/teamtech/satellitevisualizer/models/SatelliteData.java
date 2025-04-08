/*
SatelliteData represents all the information about a satellite we want to store in our MongoDB database.

The satid or NORAD catalog number is how satellites are identified, which is an integer ranging
from 1 to 43235 and counting.

The satname is the name of the satellite.

The tle is the TLE (two-line element) of the satellite represented on a single line string.
 */

package com.teamtech.satellitevisualizer.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "satellites")
@AllArgsConstructor
@NoArgsConstructor
public class SatelliteData {
    @Id
    private int satid;
    private String satname;
    private String tle;
    private List<List<Double>> geodeticCoordinates;
    private List<List<Double>> xyzCoordinates;
    // Whenever we save a new satellitedata, use timestamp.now so the timestamp is set to the current time
    // private Instant timestamp;

    public int getSatid() {
        return satid;
    }

    public void setSatid(int satid) {
        this.satid = satid;
    }

    public String getSatname() {
        return satname;
    }

    public void setSatname(String satname) {
        this.satname = satname;
    }

    public String getTle() {
        return tle;
    }

    public void setTle(String tle) {
        this.tle = tle;
    }
    public List<List<Double>> getGeodeticCoordinates() {
        return geodeticCoordinates;
    }

    public void setGeodeticCoordinates(List<List<Double>> geodeticCoordinates) {
        this.geodeticCoordinates = geodeticCoordinates;
    }

    public void setXYZCoordinates(List<List<Double>> xyzCoordinates) {
        this.xyzCoordinates = xyzCoordinates;
    }

}
