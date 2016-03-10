package com.bat.club.combatclub;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Noyloy on 29-Jan-16.
 */
public class LocationHelper implements LocationListener {
    private static final int TIME_INTERVAL = 1000 * 3; // 3 sec
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int DISTANCE_INTERVAL = 1; // 1 meter

    ArrayList<BestLocationListener> mListeners = new ArrayList<BestLocationListener>();

    public LocationManager locationManager;
    public Location currentLocation;
    public Context context;

    private HandlerThread mLocationThread;

    public LocationHelper(Context context){
        this.context = context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location tmp = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (tmp!=null)
            if (isBetterLocation(tmp,currentLocation)) currentLocation = tmp;
        if (currentLocation != null)
            for (BestLocationListener listener : mListeners) {
                listener.onNewLocation(currentLocation);
            }

        mLocationThread = new HandlerThread("Location thread", Thread.NORM_PRIORITY);

    }

    public void registerOnNewLocationListener(BestLocationListener locationListener){
        mListeners.add(locationListener);
        // first one
        if (mListeners.size()==1){
            mLocationThread.start();

            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_INTERVAL, DISTANCE_INTERVAL, this,mLocationThread.getLooper());
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIME_INTERVAL, DISTANCE_INTERVAL, this,mLocationThread.getLooper());
        }
    }

    public void unRegisterOnNewLocationListener(BestLocationListener locationListener){
        mListeners.remove(locationListener);
        // last one
        if (mListeners.size() ==0){
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location,currentLocation)) {
            currentLocation = location;
            for (BestLocationListener listener : mListeners) {
                listener.onNewLocation(location);
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
