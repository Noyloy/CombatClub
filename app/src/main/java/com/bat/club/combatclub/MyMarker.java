package com.bat.club.combatclub;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Noyloy on 3/30/2016.
 */
public class MyMarker {
    public int id;
    public Marker marker;

    public MyMarker(int id, Marker marker) {
        this.id = id;
        this.marker = marker;
    }
}
