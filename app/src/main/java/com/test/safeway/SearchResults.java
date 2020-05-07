package com.test.safeway;

import com.here.sdk.core.GeoCoordinates;

public class SearchResults {
    private String name;
    private String address;
    private GeoCoordinates geoCoordinates;

    public SearchResults(String name, String address, GeoCoordinates geoCoordinates){
        this.name = name;
        this.address = address;
        this.geoCoordinates = geoCoordinates;
    }

    public String getname(){
        return name;
    }

    public String getaddress(){
        return address;
    }

    public GeoCoordinates getGeoCoordinates() {
        return geoCoordinates;
    }
}
