/*
SatelliteResponse represents the response that we get from the N2YO API that has information about the satellite and its TLE.
 */

package com.teamtech.satellitevisualizer;

public class SatelliteResponse {

    private Info info;
    private String tle;

    /*
    Info is a static class that's nested in SatelliteResponse that defines 3 attributes:
    the satellite id (satid), satellite name (satname), and number of times we fetched a
    certain satellite (transactionCount).
     */
    public static class Info {
        private int satid;
        private String satname;
        private int transactionscount;

        public int getSatid() { return satid; }
        public void setSatid(int satid) { this.satid = satid; }

        public String getSatname() { return satname; }
        public void setSatname(String satname) { this.satname = satname; }

        public int getTransactionscount() { return transactionscount; }
        public void setTransactionscount(int transactionscount) { this.transactionscount = transactionscount; }
    }

    public Info getInfo() { return info; }
    public void setInfo(Info info) { this.info = info; }

    public String getTle() { return tle; }
    public void setTle(String tle) { this.tle = tle; }
}
