package com.bat.club.combatclub;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;

/**
 * Created by Noyloy on 30-Jan-16.
 */
public class CompassHelper implements SensorEventListener,BestLocationListener {
    SensorManager mSensorManager;
    Sensor mMagneticSensor, mAccelSensor;
    float[] mGravity;
    float[] mGeomagnetic;
    float mAzimuth = 0f;
    float mDeclination;
    long lastSampleTime =0;

    Context context;
    ArrayList<BearingListener> mListeners = new ArrayList<>();
    LocationHelper locationHelper;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    public CompassHelper(Context context,LocationHelper locationHelper){
        this.context = context;
        this.locationHelper = locationHelper;

        locationHelper.registerOnNewLocationListener(this);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorThread = new HandlerThread("Sensor thread", Thread.NORM_PRIORITY);
    }

    public void registerOnBearingListener(BearingListener listener){
        mListeners.add(listener);
        // first one
        if (mListeners.size()==1){
            mSensorThread.start();

            mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick

            mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
            mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_UI, mSensorHandler);
        }
    }
    public void unRegisterOnBearingListener(BearingListener listener){
        mListeners.remove(listener);
        // last one
        if (mListeners.size()==0){
            locationHelper.unRegisterOnNewLocationListener(this);
            mSensorManager.unregisterListener(this,mAccelSensor);
            mSensorManager.unregisterListener(this,mMagneticSensor);

            mSensorThread.quitSafely();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //if (System.currentTimeMillis() - lastSampleTime < 210) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                    mGeomagnetic);
            if (success) {
                lastSampleTime = System.currentTimeMillis();
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                mAzimuth = (float)Math.toDegrees(orientation[0])+mDeclination + 90;
                for (BearingListener listener : mListeners){
                    listener.onNewBearing(mAzimuth);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onNewLocation(Location location) {
        GeomagneticField field = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis()
        );
        // getDeclination returns degrees
        mDeclination = field.getDeclination();
    }
}
