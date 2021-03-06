package com.bat.club.combatclub;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class GameSession extends Activity
implements OnMapReadyCallback, BluetoothDataListener, BestLocationListener, BearingListener {
    public static Typeface typeface;

    private SessionIDS m_cred;
    private int mHp = 100;

    Handler h;
    final int delay = 5000; //milliseconds

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
    ArrayList<MyMarker> mTeamMarkers = new ArrayList<>();
    ArrayList<MyMarker> mEnemyMarkers = new ArrayList<>();
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


        h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                refreshGameStatus();
                h.postDelayed(this, delay);
            }
        }, 0);
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
        leaveGame();
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
                        handleAmmo(dataParsed[1]);
                        break;
                    case "h":
                        handleHp(dataParsed[1]);
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
            double ratio = Double.parseDouble(ammoStatus[0])/Double.parseDouble(ammoStatus[1]);
            if ( ratio <= 1f && ratio > 2f/3f ) mAmmoImageView.setImageResource(R.drawable.ic_ammo_3_3);
            else if ( ratio <= 2f/3f && ratio > 1f/3f ) mAmmoImageView.setImageResource(R.drawable.ic_ammo_2_3);
            else if ( ratio <= 1f/3f && ratio > 0f) mAmmoImageView.setImageResource(R.drawable.ic_ammo_1_3);
            else if (ratio <= 0f) mAmmoImageView.setImageResource(R.drawable.ic_ammo_0_3);
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
            mHp = hp_val;
            // send my new hp to server.
            updatePlayerStatus();
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
                RequestParams params = makeBasicRequest();
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
                            mEnemyMarkers.add(new MyMarker(enemyID, googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_enemy))
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
                final MyMarker em = getEnemy(marker);
                if (em!=null) {
                    RequestParams params = makeBasicRequest();
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

    private RequestParams makeBasicRequest(){
        RequestParams params = new RequestParams();
        params.add("soldierID",m_cred.playerID+"");
        params.add("gameID",m_cred.gameID+"");
        params.add("teamID", m_cred.teamID + "");
        return params;
    }

    public MyMarker getEnemy(Marker m){
        for(MyMarker em : mEnemyMarkers){
            if (em.marker.equals(m)) return em;
        }
        return null;
    }

    private void updatePlayerStatus(){
        RequestParams params = makeBasicRequest();
        params.add("hp",mHp+"");
        params.add("lat",mLHelper.currentLocation.getLatitude()+"");
        params.add("lon",mLHelper.currentLocation.getLongitude()+"");
        params.add("locName","");
        CombatClubRestClient.post("/UpdateSoldier", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonStr = new String(responseBody, "UTF-8");
                    jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                    JSONObject res = new JSONObject(jsonStr);
                    if (res.getInt("Code") < 0) {
                        Toast.makeText(GameSession.this, res.getString("Message"), Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception ex) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void refreshGameStatus(){
        updateEnemyMarkers();
        updateTeamMarkers();
        updateGameStatus();
    }

    private void updateGameStatus() {
        RequestParams params = new RequestParams();
        params.add("gameID", m_cred.gameID + "");
        CombatClubRestClient.post("/GetGameStatus", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonStr = new String(responseBody, "UTF-8");
                    jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                    JSONArray resArr = new JSONArray(jsonStr);
                    // 0 Gorilla Team
                    JSONObject gorilObj = resArr.getJSONObject(0);
                    int gorilCount = gorilObj.getInt("count");
                    ((TextView) findViewById(R.id.m_gori_tv)).setText(gorilCount + "");

                    // 1 Army Team
                    JSONObject armyObj = resArr.getJSONObject(1);
                    int armyCount = armyObj.getInt("count");
                    ((TextView) findViewById(R.id.m_army_tv)).setText(armyCount + "");

                    if (armyCount == 0) {
                        unregisterSessionListeners();
                        EndDialog alert = new EndDialog();
                        alert.showDialog(GameSession.this, 0);
                    } else if (gorilCount == 0) {
                        unregisterSessionListeners();
                        EndDialog alert = new EndDialog();
                        alert.showDialog(GameSession.this, 1);
                    }

                } catch (Exception ex) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });

    }
    private void updateEnemyMarkers(){
        RequestParams params = makeBasicRequest();
        CombatClubRestClient.post("/GetTeamEnemyMarkStatus", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    clearEnemyMarkers();
                    String jsonStr = new String(responseBody, "UTF-8");
                    jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                    JSONArray resArr = new JSONArray(jsonStr);
                    for (int i = 0; i < resArr.length(); i++) {
                        JSONObject resObj = resArr.getJSONObject(i);
                        JSONObject resLocation = resObj.getJSONObject("Location");

                        LatLng eLatLng = new LatLng(resLocation.getDouble("Lat"), resLocation.getDouble("Long"));
                        int enemyID = resObj.getInt("ID");

                        mEnemyMarkers.add(new MyMarker(enemyID, googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_enemy))
                                .title("Enemy")
                                .snippet("Tap to remove")
                                .position(eLatLng))));
                    }
                } catch (Exception ex) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void updateTeamMarkers(){
        RequestParams params = makeBasicRequest();
        CombatClubRestClient.post("/GetTeamStatus", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonStr = new String(responseBody, "UTF-8");
                    jsonStr = CombatClubRestClient.interprateResponse(jsonStr);
                    JSONArray resArr = new JSONArray(jsonStr);
                    for (int i = 0; i < resArr.length(); i++) {
                        JSONObject resObj = resArr.getJSONObject(i);
                        JSONObject resLocation = resObj.getJSONObject("location");
                        JSONObject resHealth = resObj.getJSONObject("health");

                        LatLng tLatLng = new LatLng(resLocation.getDouble("Lat"), resLocation.getDouble("Long"));
                        String percentage = resHealth.getString("Percentage");
                        int teamID = resObj.getInt("id");
                        String name = resObj.getString("name");
                        addOrAnimateMarker(new MyMarker(teamID, googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker_teammate)))
                                .title(name)
                                .snippet(percentage + "%")
                                .position(tLatLng)
                                .visible(false))));
                    }
                } catch (Exception ex) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void addOrAnimateMarker(MyMarker marker){
        if (marker.id==m_cred.playerID){
            marker.marker.remove();
            return; // I know my location
        }

        int markerPos = getTeamMarkerPos(marker);
        if (markerPos!=-1){
            // update
            Marker m = mTeamMarkers.get(markerPos).marker;
            m.setTitle(marker.marker.getTitle());
            m.setSnippet(marker.marker.getSnippet());

            // animate
            MarkerAnimation.animateMarkerToGB(mTeamMarkers.get(markerPos).marker, marker.marker.getPosition(), new LatLngInterpolator.Spherical());
        }else{
            // add and show
            marker.marker.setVisible(true);
            mTeamMarkers.add(marker);
        }
    }

    private int getTeamMarkerPos(MyMarker marker){
        for(int i=0;i<mTeamMarkers.size();i++)
            if (mTeamMarkers.get(i).id == marker.id) return i;
        return -1;
    }

    private void clearEnemyMarkers(){
        for (MyMarker m : mEnemyMarkers) m.marker.remove();
        mEnemyMarkers.clear();
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
                    mBTHelper.sendData('*');
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
        else {
            String res = mBTHelper.openBluetoothCommunication();
            if (res.equals(mBTHelper.COMM_FAIL_RESULT)){
                Toast.makeText(GameSession.this,"Can't connect to Combat system data stream",Toast.LENGTH_LONG).show();
            }
            else if (res.equals(mBTHelper.SUCCESS_RESULT)){
                Toast.makeText(GameSession.this,"Connected to Combat system",Toast.LENGTH_SHORT).show();
            }
        }

        // register for bearing changes
        mComHelper = new CompassHelper(GameSession.this,mLHelper);
        mComHelper.registerOnBearingListener(this);

    }

    private void unregisterSessionListeners(){
        mLHelper.unRegisterOnNewLocationListener(this);
        mBTHelper.unregisterOnNewBluetoothDataListener(this);
        mComHelper.unRegisterOnBearingListener(this);
        h.removeCallbacksAndMessages(null);
    }

    private void leaveGame(){
        RequestParams params = new RequestParams();
        params.add("soldierID", m_cred.playerID + "");
        params.add("gameID", m_cred.gameID + "");
        params.add("teamID", m_cred.teamID + "");
        CombatClubRestClient.post("/LeaveTeam", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            }
        });

    }

    private void closeGame(){
        RequestParams params = new RequestParams();
        params.add("gameID", m_cred.gameID + "");
        CombatClubRestClient.post("/RemoveGame", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            }
        });
    }
    // costume dialog when opening a new game
    public class EndDialog {

        public void showDialog(final Activity activity, int winnerTeam) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.gameend_dialog);

            ImageView winnerImg = (ImageView)dialog.findViewById(R.id.winner_img);
            ImageView loserImg = (ImageView)dialog.findViewById(R.id.loser_img);
            Button enterButton = (Button) dialog.findViewById(R.id.enterBtn);

            // team 1 is army
            if (winnerTeam == 1){
                winnerImg.setImageResource(R.drawable.ic_army);
                loserImg.setImageResource(R.drawable.ic_gorilla);
            }

            enterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Intent intent = new Intent(getApplicationContext(), GameSelection.class);
                    startActivity(intent);
                    finish();
                }
            });

            dialog.show();

        }
    }

    @Override
    public void onNewLocation(final Location location) {
        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MarkerAnimation.animateMarkerToGB(mMarker, latLng, new LatLngInterpolator.Spherical());
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

        // send my new location to server.
        updatePlayerStatus();
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
