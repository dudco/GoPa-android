package com.example.dudco.gopa;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by dudco on 2017. 5. 14..
 */

public class PlaceData {
    private double lat;
    private double log;
    private String name;
    private String place;

    public PlaceData(double lat, double log, String name, String place) {
        this.lat = lat;
        this.log = log;
        this.name = name;
        this.place = place;
    }

    public double getLat() {
        return lat;
    }

    public double getLog() {
        return log;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }
}
