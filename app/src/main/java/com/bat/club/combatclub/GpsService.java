package com.bat.club.combatclub;

/**
 * Created by Noyloy on 28-Jan-16.
 */

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class GpsService extends Service {
    private static final String TAG = "BOOMBOOMTESTGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;//Every second
    private static final float LOCATION_DISTANCE = 1.0f; //Every 1 meter

    String mUsername = "";
    String mPassword = "";
    String mUserID = "";

    CombatClubRestClient mClient;

    private final IBinder mBinder = new LocalBinder();

    private Geocoder mGcd;

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        String provider = "";

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
            this.provider = provider;
            Log.e(TAG, "LocationListenerConstructed: " + mLastLocation);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            String address = "";
            List<Address> add = getAddresses(1);
            for (int i = add.get(0).getMaxAddressLineIndex(); i >= 0; i--)
                address += add.get(0).getAddressLine(i) + " ";
            address = address.trim();

            // TODO: Send data to server
            /*RequestParams params = new RequestParams();
            params.add("userID", mUserID);
            params.add("username", mUsername);
            params.add("userPass", mPassword);
            params.add("lat", getLatitude() + "");
            params.add("lon", getLongitude() + "");
            params.add("address", address);
            params.add("provider", provider);
            mClient.post("/updateUserLocation", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.e(TAG, "data sent to server SUCCESSFULY");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "data UNSUCCESFULLY sent to server");
                }
            });*/
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        Bundle extras = null;
        if (intent != null) {
            extras = intent.getExtras();
        } else stopSelf();
        if (extras != null) {
            mUsername = extras.getString("Username");
            mPassword = extras.getString("Password");
            mUserID = extras.getString("UserID");
        }
        mClient = new CombatClubRestClient();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");

        mGcd = new Geocoder(GpsService.this, Locale.getDefault());
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
            mLocationListeners[1].mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
            mLocationListeners[0].mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public Location getCurrentLocation(){
        if (mLocationListeners[0]!= null && mLocationListeners[0].mLastLocation != null)
            return mLocationListeners[0].mLastLocation;
        else if (mLocationListeners[1]!= null && mLocationListeners[1].mLastLocation != null)
            return mLocationListeners[1].mLastLocation;
        else return null;
    }
    public double getLatitude(){
        Location tmp_loc = getCurrentLocation();
        if (tmp_loc!=null)
            return tmp_loc.getLatitude();
        else return 999;
    }
    public double getLongitude(){
        Location tmp_loc = getCurrentLocation();
        if (tmp_loc!=null)
            return tmp_loc.getLongitude();
        else return 999;
    }
    public List<Address> getAddresses(int address_count){
        try {
            return mGcd.getFromLocation(getLatitude(), getLongitude(), address_count);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class LocalBinder extends Binder{
        public GpsService getService(){
            return GpsService.this;
        }
    }
}