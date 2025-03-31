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

@Data
@Document(collection = "satellites")
@AllArgsConstructor
@NoArgsConstructor
public class SatelliteData {
    @Id
    private int satid;
    private String satname;
    private String tle;

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
}
