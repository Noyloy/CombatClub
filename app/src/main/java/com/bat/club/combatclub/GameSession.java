package com.bat.club.combatclub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class GameSession extends Activity
implements OnMapReadyCallback, BluetoothDataListener, BestLocationListener, BearingListener {
    public static Typeface typeface;
    private SessionIDS m_cred;
    CameraPosition mCurrentCamera =
            new CameraPosition.Builder().target(new LatLng(32.215112, 34.993070))
                    .zoom(18.5f)
                    .bearing(0)
                    .tilt(10)
                    .build();

    // view for full screen immersion
    View mDecorView;

    // google map object
    GoogleMap googleMap;
    Marker mMarker;
    ArrayList<Marker> mTeamMarkers = new ArrayList<>();
    ArrayList<EnemyMarker> mEnemyMarkers = new ArrayList<>();
    // location helper class
    LocationHelper mLHelper;
    // bluetooth helper class
    BluetoothHelper mBTHelper;
    // compass helper class
    CompassHelper mComHelper;

    long reviveTime = 5000;
    boolean firstReviveClick = true;

    // GUI ELEMENTS
    TextView mAmmoTextView, mHpTextView;
    ImageView mAmmoImageView, mHpImageView;
    ToggleButton mCameraToggleButton;
    Button mReviveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/DS-DIGI.TTF");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_session);
        // load credentials
        loadCred();
        // hide android ui
        mDecorView = getWindow().getDecorView();
        hideSystemUI();
        // load GUI elements
        loadGUI();
        // load google map object
        ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        // connect listeners
        registerForSessionListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterSessionListeners();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        applyUserMapSettings();
    }

    @Override
    public void onNewData(String data) {
        final String[] dataParsed = data.split(",");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (dataParsed[0]) {
                    case "a":
                        handleAmmo(dataParsed[0]);
                        break;
                    case "h":
                        handleHp(dataParsed[0]);
                        break;
                    case "r":
                        Toast.makeText(GameSession.this, "Reloading", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void handleAmmo(String ammo_data){
        mAmmoTextView.setText(ammo_data);
        String[] ammoStatus = ammo_data.split("/");
        try{
            double ratio = Integer.parseInt(ammoStatus[0])/Integer.parseInt(ammoStatus[1]);
            if ( ratio <= 1 && ratio > 2/3 ) mAmmoImageView.setImageResource(R.drawable.ic_ammo_3_3);
            else if ( ratio <= 2/3 && ratio > 1/3 ) mAmmoImageView.setImageResource(R.drawable.ic_ammo_2_3);
            else if ( ratio <= 1/3 && ratio > 0) mAmmoImageView.setImageResource(R.drawable.ic_ammo_1_3);
            else if (ratio <= 0) mAmmoImageView.setImageResource(R.drawable.ic_ammo_0_3);
        } catch (Exception e) {
        }
    }

    private void handleHp(String hp_data) {
        mHpTextView.setText(hp_data + "%");
        try{
            int hp_val = Integer.parseInt(hp_data);
            if ( hp_val <= 100 && hp_val > 66 ) mHpImageView.setImageResource(R.drawable.ic_hp_3_3);
            else if ( hp_val <= 66 && hp_val > 33 ) mHpImageView.setImageResource(R.drawable.ic_hp_2_3);
            else if ( hp_val <= 33 && hp_val > 0) mHpImageView.setImageResource(R.drawable.ic_hp_1_3);
            else if ( hp_val <= 0 ) mHpImageView.setImageResource(R.drawable.ic_hp_0_3);
        }catch (Exception e){}
    }

    private  void applyUserMapSettings(){
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        mMarker = googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_me))
                .title("Noyloy")
                .position(new LatLng(mLHelper.currentLocation.getLatitude(),mLHelper.currentLocation.getLongitude())));

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {

                RequestParams params = new RequestParams();
                params.add("soldierID",m_cred.playerID+"");
                params.add("gameID",m_cred.gameID+"");
                params.add("teamID",m_cred.teamID+"");
                params.add("lat",latLng.latitude+"");
                params.add("lon",latLng.longitude+"");
                params.add("locName", "");

                CombatClubRestClient.post("/MarkEnemy", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            String jsonStr = new String(responseBody, "UTF-8");
                            jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                            JSONObject res = new JSONObject(jsonStr);
                            if (res.getInt("Code")<0) {
                                Toast.makeText(GameSession.this,res.getString("Message"),Toast.LENGTH_LONG).show();
                                return;
                            }
                            int enemyID = res.getInt("Value");
                            mEnemyMarkers.add(new EnemyMarker(enemyID, googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_enemy))
                                    .title("Enemy")
                                    .snippet("Tap to remove")
                                    .position(latLng))));
                        }
                        catch (Exception ex){}
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });

            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {
                final EnemyMarker em = getEnemy(marker);
                if (em!=null) {
                    RequestParams params = new RequestParams();
                    params.add("soldierID",m_cred.playerID+"");
                    params.add("gameID",m_cred.gameID+"");
                    params.add("teamID", m_cred.teamID + "");
                    params.add("enemyID", em.id + "");

                    CombatClubRestClient.post("/UnmarkEnemy", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            try {
                                String jsonStr = new String(responseBody, "UTF-8");
                                jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                                JSONObject res = new JSONObject(jsonStr);
                                if (res.getInt("Code") < 0){
                                    Toast.makeText(GameSession.this,res.getString("Message"),Toast.LENGTH_LONG).show();
                                    return;
                                }
                                marker.remove();
                                mEnemyMarkers.remove(em);
                            } catch (Exception ex) {
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                        }
                    });

                }
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });
    }

    private void connectToBTDevice(String device_name){
        String bt_res = mBTHelper.findBluetoothDevice(device_name);
        if (bt_res.equals(mBTHelper.NOT_ENABLED)) Toast.makeText(GameSession.this,"Try to Enable Your Bluetooth first",Toast.LENGTH_SHORT).show();
        else if (bt_res.equals(mBTHelper.NO_SUPPORT)) Toast.makeText(GameSession.this,"Your Phone Dosent support Bluetooth",Toast.LENGTH_SHORT).show();
        else if (bt_res.equals(mBTHelper.DEVICE_NOT_FOUND)) Toast.makeText(GameSession.this,"Device Not Found",Toast.LENGTH_SHORT).show();
        else mBTHelper.openBluetoothCommunication();
    }

    private void animateCameraToCurrentPosition(){
        googleMap.stopAnimation();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCurrentCamera), 50, null);
    }

    private void hideSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void loadGUI(){
        mAmmoTextView = (TextView) findViewById(R.id.m_ammo_tv);
        mAmmoImageView = (ImageView) findViewById(R.id.m_ammo_iv);
        mHpTextView = (TextView) findViewById(R.id.m_hp_tv);
        mHpImageView = (ImageView) findViewById(R.id.m_hp_iv);
        mCameraToggleButton = (ToggleButton) findViewById(R.id.camera_tb);
        mReviveBtn = (Button) findViewById(R.id.revive_btn);
    }

    private void loadCred(){
        try {
            Intent intent = getIntent();
            int playerID = intent.getIntExtra(SessionIDS.KEYS[0], -1);
                if (playerID==-1) throw new Exception();
            String playerName = intent.getStringExtra(SessionIDS.KEYS[1]);
            int gameID = intent.getIntExtra(SessionIDS.KEYS[2], -1);
            int teamID = intent.getIntExtra(SessionIDS.KEYS[3], -1);
            m_cred = new SessionIDS(playerID, playerName, gameID, teamID);
        }
        catch (Exception e){
            m_cred = new SessionIDS(1,"Noyloy", 0,0);
        }
    }

    private EnemyMarker getEnemy(Marker m){
        for(EnemyMarker em : mEnemyMarkers){
            if (em.marker.equals(m)) return em;
        }
        return null;
    }
    private void registerForSessionListeners(){
        mCameraToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) animateCameraToCurrentPosition();
            }
        });
        mReviveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstReviveClick){
                    firstReviveClick = false;
                    mReviveBtn.setBackgroundResource(R.drawable.ic_dif_on);
                    mReviveBtn.setEnabled(false);
                    Toast.makeText(GameSession.this,"Wait "+reviveTime/1000+" seconds to Revive.",Toast.LENGTH_LONG).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(reviveTime);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mReviveBtn.setBackgroundResource(R.drawable.ic_dif_green);
                                        mReviveBtn.setEnabled(true);
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                else {
                    //mBTHelper.sendData('*');
                    mReviveBtn.setBackgroundResource(R.drawable.ic_dif_off);
                    firstReviveClick=true;
                    Toast.makeText(GameSession.this,"You are now revived",Toast.LENGTH_LONG).show();

                }
            }
        });
        // register for refined location changes
        mLHelper = new LocationHelper(GameSession.this);
        mLHelper.registerOnNewLocationListener(this);

        // register for bluetooth data change
        mBTHelper = new BluetoothHelper();
        mBTHelper.registerOnNewBluetoothDataListener(this);
        // try to connect
        String bt_res = mBTHelper.findBluetoothDevice("HC-06");
        if (bt_res.equals(mBTHelper.NOT_ENABLED)) Toast.makeText(GameSession.this,"Try to Enable Your Bluetooth first",Toast.LENGTH_SHORT).show();
        else if (bt_res.equals(mBTHelper.NO_SUPPORT)) Toast.makeText(GameSession.this,"Your Phone Doesn't support Bluetooth",Toast.LENGTH_SHORT).show();
        else if (bt_res.equals(mBTHelper.DEVICE_NOT_FOUND)) Toast.makeText(GameSession.this,"Device Not Found",Toast.LENGTH_SHORT).show();
        else mBTHelper.openBluetoothCommunication();

        // register for bearing changes
        mComHelper = new CompassHelper(GameSession.this,mLHelper);
        mComHelper.registerOnBearingListener(this);
    }

    private void unregisterSessionListeners(){
        mLHelper.unRegisterOnNewLocationListener(this);
        mBTHelper.unregisterOnNewBluetoothDataListener(this);
        mComHelper.unRegisterOnBearingListener(this);
    }

    @Override
    public void onNewLocation(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MarkerAnimation.animateMarkerToGB(mMarker, new LatLng(location.getLatitude(), location.getLongitude()), new LatLngInterpolator.Spherical());
                mCurrentCamera =
                        new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude()))
                                .zoom(mCurrentCamera.zoom)
                                .bearing(mCurrentCamera.bearing)
                                .tilt(mCurrentCamera.tilt)
                                .build();
                if (mCameraToggleButton.isChecked()) {

                            animateCameraToCurrentPosition();
                        }
                }
        });



        // TODO: send my new location to server.
        // and stuff


    }

    @Override
    public void onNewBearing(float bearing) {
        if (Math.abs(bearing-mCurrentCamera.bearing)<5) return;
        mCurrentCamera =
                new CameraPosition.Builder().target(new LatLng(mLHelper.currentLocation.getLatitude(), mLHelper.currentLocation.getLongitude()))
                        .zoom(18.5f)
                        .bearing(bearing)
                        .tilt(50)
                        .build();
        if (mCameraToggleButton.isChecked()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateCameraToCurrentPosition();
                    }
                });

        }
    }
}
