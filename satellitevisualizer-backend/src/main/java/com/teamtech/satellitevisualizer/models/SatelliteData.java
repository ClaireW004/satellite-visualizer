/**
 * SatelliteData.java
 * This class represents the data model for a satellite in the MongoDB database.
 * It contains fields for the satellite ID, name, TLE (two-line element), geodetic coordinates,
 * and XYZ coordinates.
 * The satid or NORAD catalog number is how satellites are identified, which is an integer ranging
 * from 1 to 43235 and counting.
 * The tle is the TLE (two-line element) of the satellite represented on a single line string.
 *
 * The @Document annotation indicates that this class is a MongoDB document and specifies the collection name.
 * The @Data annotation generates getters, setters, equals, hashCode, and toString methods automatically.
 * The @AllArgsConstructor and @NoArgsConstructor annotations generate constructors with all arguments and no arguments respectively.
 *
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

    public List<List<Double>> getXyzCoordinates() {
        return xyzCoordinates;
    }

    public void setXYZCoordinates(List<List<Double>> xyzCoordinates) {
        this.xyzCoordinates = xyzCoordinates;
    }
}
