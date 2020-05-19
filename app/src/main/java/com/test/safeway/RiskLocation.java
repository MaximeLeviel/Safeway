package com.test.safeway;

import com.here.sdk.core.GeoCoordinates;

public class RiskLocation {
    private int ID;
    private GeoCoordinates geoCoordinates;
    private int note;

    public RiskLocation(int ID, GeoCoordinates geoCoordinates, int note){
        this.ID = ID;
        this.geoCoordinates = geoCoordinates;
        this.note = note;
    }

    public int getID() {
        return ID;
    }

    public GeoCoordinates getGeoCoordinates() {
        return geoCoordinates;
    }

    public int getNote() {
        return note;
    }
}
