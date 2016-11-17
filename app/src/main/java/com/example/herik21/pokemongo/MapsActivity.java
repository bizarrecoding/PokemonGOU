package com.example.herik21.pokemongo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult>,OnMapReadyCallback {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500 ;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 123;
    private static final double STOPDISTANCE = 10;
    private GoogleApiClient mApiClient;
    private LocationRequest mLocationRequest;
    private GoogleMap gmap;
    private Marker trainer;
    private ProgressDialog pDialog;
    private ArrayList<WildMarker> wildMarkers = new ArrayList<>();
    private ArrayList<Marker> stops = new ArrayList<>();
    private TextView log;
    private LatLng mLocation;
    private boolean moveCam, near;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        moveCam = true;
        near = true;
        log = (TextView)findViewById(R.id.log);
        FlowManager.init(new FlowConfig.Builder(this).openDatabasesOnInit(true).build());
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        onGO();
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(MapsActivity.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connMgr.getActiveNetworkInfo();
        if(!ni.isConnected()){
            Toast.makeText(this,"Not Connected to Internet",Toast.LENGTH_LONG).show();
        }
    }

    public void append(String entry){
        if(entry.equals("wild pokemons near you")){
            if(near) {
                String logText = log.getText().toString();
                log.setText(logText + "\n" + entry);
            }
            near=false;
        }else{
            String logText = log.getText().toString();
            log.setText(logText+"\n"+entry);
            near=true;
        }
        ((ScrollView)findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_DOWN);
    }

    public void onGO(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest nLocationSettingsRequest = builder.build();
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mApiClient,
                        nLocationSettingsRequest);
        result.setResultCallback(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        append("Welcome trainer");
        gmap = googleMap;
        gmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                Log.d("MARKERS","touched id"+marker.getTag()+" title "+marker.getTitle());
                if(marker.getTitle()!=null){
                    if(marker.getTitle().equals("Trainer")) {
                        Intent i = new Intent(MapsActivity.this,TeamActivity.class);
                        startActivity(i);
                    }else if (marker.getTitle().equals("Pokestop")){
                        if(calcDist(marker.getPosition(),mLocation,STOPDISTANCE)){
                            LayoutInflater factory = LayoutInflater.from(MapsActivity.this);
                            View view = factory.inflate(R.layout.stoprewards,null);
                            Random r = new Random();
                            final int pkb = r.nextInt(5)+1;
                            final int pot = r.nextInt(5)+1;
                            final int spot = r.nextInt(2)+1;
                            ((TextView)view.findViewById(R.id.pokeballs)).setText("Pokeballs x"+pkb);
                            ((TextView)view.findViewById(R.id.potions)).setText("Potions x"+pot);
                            ((TextView)view.findViewById(R.id.superpotions)).setText("Spuerpotions x"+spot);
                            new AlertDialog.Builder(MapsActivity.this)
                                    .setTitle("PokeStop")
                                    .setView(view)
                                    .setIcon(R.drawable.pkcenter)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Trainer tr = new Select().from(Trainer.class).querySingle();
                                            tr.pokeball+=pkb;
                                            tr.potion+=pot;
                                            tr.superpotion+=spot;
                                            tr.save();
                                            Toast.makeText(MapsActivity.this, "Items acquired", Toast.LENGTH_SHORT).show();
                                            stops.remove(marker);
                                            marker.remove();
                                        }})
                                    .setNegativeButton(android.R.string.no, null)
                                    .create().show();
                            if(stops.isEmpty()){
                                setpkStops(mLocation.latitude,mLocation.longitude);
                            }
                            return true;
                        }else{
                            Toast.makeText(MapsActivity.this,"Pokestop too far",Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }else {
                        int mid = (int)marker.getTag();
                        Log.d("Marker","touched mk"+mid);
                        Intent i = new Intent(MapsActivity.this, BattleActivity.class);
                        TeamPokemon starter = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.main.is(true)).and(TeamPokemon_Table.currenthp.greaterThan(0)).querySingle();
                        if (starter == null) {
                            starter = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.currenthp.greaterThan(0)).querySingle();
                            if (starter != null) {
                                starter.main = true;
                                starter.save();
                            } else {
                                return false;
                            }
                        }
                        Pokemon wild = new Select().from(Pokemon.class).where(Pokemon_Table.name.is(marker.getTitle())).querySingle();
                        Log.d("starter", starter.basePokemon.name);
                        i.putExtra("pokemon", starter);
                        i.putExtra("base", starter.basePokemon);
                        i.putExtra("wild", wild);
                        if(mid!=-2) {
                            WildMarker wm = wildMarkers.get(mid);
                            if (wm.mk != null) {
                                wm.mk.setVisible(false);
                                wm.mk.remove();
                            }
                            wm.visible = false;
                            wm.mk = null;
                            wm.id = -2;
                        }else{
                            marker.setVisible(false);
                            marker.remove();
                        }
                        if(marker.getTitle()!= null){
                            marker.setVisible(false);
                            marker.remove();
                        }
                        Log.d("wmarker","marker mk"+mid+"released");
                        marker.remove();
                        i.putExtra("markerid",mid);
                        startActivityForResult(i, 1);
                        return true;
                    }
                }else{
                    marker.setVisible(false);
                    int mid = (int)marker.getTag();
                    if(mid!=-2) {
                        wildMarkers.get(mid).mk=null;
                    }
                }
                return false;
            }
        });
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            return;
        }else{
            gmap.setMyLocationEnabled(true);
            gmap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MapsActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                        return false;
                    }
                    Location loc = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                    LatLng myLocation = new LatLng(loc.getLatitude(),loc.getLongitude());
                    updateUI(myLocation,moveCam);
                    return false;
                }
            });
            try{
                LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient,mLocationRequest,this);
                Location loc = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                LatLng myLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                new HttpAsyncTask(loc.getLatitude(),loc.getLongitude()).execute();
                updateUI(myLocation,moveCam);
            }catch (Exception ex){
                Log.d("GMAPS","no location, api restricted");
            }
        }
    }
    public void setpkStops(double lat, double lng){
        for(int i=0;i<8;i++){
            Double ltd,longtd;
            ltd = lat+ (Math.random()*((0.015)))-0.0075;
            longtd = lng+ (Math.random()*((0.015)))-0.0075;
            try{
                Marker stop = gmap.addMarker(new MarkerOptions()
                        .position(new LatLng(ltd,longtd))
                        .title("Pokestop")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop))
                );
                stops.add(stop);
            }catch(Exception e){
                Log.d("Sht Happened",e.getLocalizedMessage());
            }
        }
    }

    public static String getStops(double lat, double lng){
        String response = "";
        try{
            URL url = new URL("http://190.144.171.172/function3.php?lat="+lat+"&lng="+lng);
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String result;
            while ((result = in.readLine())!=null){
                Log.d("HTTPGET","result = "+result);
                response = result;
            }
            in.close();
            return response;
        }catch (Exception ex){
            Log.d("HTTPGET",ex.getLocalizedMessage());
            return null;
        }
    }

    public void updateUI(LatLng location, boolean move) {
        gmap.setMaxZoomPreference(19.5f);
        gmap.setMinZoomPreference(16.5f);
        if(move){
            moveCam = false;
            gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));
        }
        if(trainer!=null){
            trainer.setPosition(location);
        }else{
            setpkStops(location.latitude,location.longitude);
            trainer = gmap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pkmn_loc))
                    .position(location)
                    .title("Trainer"));
        }
        mLocation = location;
        setWildPokemons();
    }

    public void setWildPokemons(){
        for(WildMarker wMarker : wildMarkers) {
            if(wMarker.mk==null && wMarker.id!=-2) {
                //wMarker.visible=true;
                wMarker.mk = gmap.addMarker(new MarkerOptions()
                        .position(new LatLng(wMarker.loc[0],wMarker.loc[1]))
                        .visible(false));
                wMarker.mk.setTag(wMarker.id);
                if(wMarker.mk.getTitle()!=null){
                    wMarker.mk.setVisible(true);
                    Log.d("MARKERS-S","new marker visible id="+wMarker.id);
                }
                Log.d("MARKERS-S","new marker id="+wMarker.id);
            }
            if (wMarker.visible && wMarker.mk!=null && wMarker.id!=-2) {
                wMarker.mk.setVisible(true);
                Pokemon pk = new Select().from(Pokemon.class).where(Pokemon_Table.id.is(wMarker.pkid)).querySingle();
                if(pk!=null) {
                    wMarker.mk.setPosition(new LatLng(wMarker.loc[0], wMarker.loc[1]));
                    wMarker.mk.setTitle(pk.name);
                    new MarkerIconAsyncTask(wMarker.mk).execute(pk.imgFront);
                }
                Log.d("MARKERS-S","marker visible id="+wMarker.id);
            }else if(wMarker.mk!=null && !wMarker.visible){
                wMarker.mk.setVisible(false);
                wMarker.visible=false;
            }else{
                wMarker.visible=false;
            }
        }
    }

    public void refreshMarkers(){
        for(WildMarker wMarker : wildMarkers) {
            if (false && wMarker.visible && wMarker.mk!=null && wMarker.mk.getTitle()!=null) {
                wMarker.mk.setVisible(true);
                wMarker.mk.remove();
                wMarker.id=-2;
                wMarker.mk=null;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mApiClient != null) {
            mApiClient.connect();
            Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mApiClient.connect();
    }
    @Override
    protected void onPause() {
        mApiClient.disconnect();
        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}
    @Override
    public void onConnectionSuspended(int i) {}
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
    @Override
    public void onLocationChanged(Location location) {
        LatLng loc = new LatLng(location.getLatitude(),location.getLongitude());
        updateUI(loc,moveCam);
        new DistanceAsyncTask(loc).execute();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        Log.d("GPS", "onResult" + status.getStatusCode());
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                if (mApiClient.isConnected()) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                        return;
                    }
                    gmap.setMyLocationEnabled(true);
                    gmap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                        @Override
                        public boolean onMyLocationButtonClick() {
                            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                                return false;
                            }
                            Location loc = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                            LatLng myLocation = new LatLng(loc.getLatitude(),loc.getLongitude());
                            updateUI(myLocation,moveCam);

                            int i =0;
                            for (WildMarker wm : wildMarkers){
                                if(wm.mk!=null && wm.mk.getTitle()!=null) {
                                    i++;
                                    wm.mk.setVisible(true);
                                }
                            }
                            Log.d("MARKERS-Click","current wild pkmons "+i);
                            return false;
                        }
                    });
                    LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, mLocationRequest, this);
                    Location loc = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                    new HttpAsyncTask(loc.getLatitude(),loc.getLongitude()).execute();
                    LatLng myLocation = new LatLng(loc.getLatitude(),loc.getLongitude());
                    updateUI(myLocation,moveCam);
                }
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                try {
                    status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(this, "LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                Pokemon newPk = (Pokemon) data.getSerializableExtra("captured");
                int i = data.getIntExtra("markerid",-2);
                if(i!=-2){
                    WildMarker wm= wildMarkers.get(i);
                    if(wm.mk!=null){
                        Log.d("MARKERS-onResults", "removing marker "+wm.mk.getTitle());
                        wm.mk.setVisible(false);
                        wm.mk.remove();
                    }
                    wm.mk=null;
                    wm.id=-2;
                    wm.visible=false;
                }
                if(newPk!=null){
                    long size = new Select().from(TeamPokemon.class).count();
                    Log.d("SIZE","before: "+size);
                    int newHP = data.getIntExtra("hp",20);
                    int newAtk = data.getIntExtra("atk",20);
                    int newDef = data.getIntExtra("def",20);
                    Pokemon wild = (Pokemon) data.getSerializableExtra("bwild");
                    TeamPokemon newTeam = new TeamPokemon(wild,newHP,newAtk,newDef,false);
                    newTeam.save();
                    Log.d("SIZE","after: "+size);
                    append(wild.name+" Captured!");
                }
                if(!data.getBooleanExtra("lose",true)){
                    TeamPokemon tpk = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.main.is(true)).querySingle();
                    Random r = new Random();
                    int hpbonus = 2+r.nextInt(4);
                    int atkbonus = 2+r.nextInt(4);
                    int defbonus = 2+r.nextInt(4);
                    if(tpk.basePokemon.hp_max > (tpk.hp + hpbonus)) {
                        tpk.hp += hpbonus;
                    }else{
                        tpk.hp += hpbonus;
                    }
                    tpk.currenthp += hpbonus;
                    if(tpk.currenthp>tpk.hp){
                        tpk.currenthp=tpk.hp;
                    }
                    if(tpk.basePokemon.atk_max > (tpk.atk + atkbonus)) {
                        tpk.atk += atkbonus;
                    }else{
                        tpk.atk = tpk.basePokemon.atk_max;
                    }
                    if(tpk.basePokemon.def_max > (tpk.def + defbonus)) {
                        tpk.def += defbonus;
                    }else{
                        tpk.def = tpk.basePokemon.def_max;
                    }
                    tpk.save();
                    checkEvo(tpk);
                }
                //updateVisibility();

            }else{
                //code
            }
            int wildcount =0;
            for (WildMarker wm : wildMarkers){
                if(wm.mk!=null && wm.mk.getTitle()!=null) {
                    wildcount++;
                    wm.mk.setVisible(true);
                }else{
                    if(wm.mk!=null) {
                        wm.mk.remove();
                        wm.visible=false;
                        wm.id=-2;
                    }
                }
            }
            Log.d("MARKERS-afterbatle","current wild pkmons "+wildcount);
            if(wildcount==0){
                requestNewLocation();
                updateVisibility();
                setWildPokemons();
            }
        }
    }

    public void checkEvo(TeamPokemon tpk){
        if (tpk.atk == tpk.basePokemon.atk_max && tpk.def == tpk.basePokemon.def_max && tpk.basePokemon.ev_id != -1){
            Pokemon evo = new Select().from(Pokemon.class).where(Pokemon_Table.id.is(tpk.basePokemon.ev_id)).querySingle();
            String prename = tpk.basePokemon.name;
            tpk.basePokemon=evo;
            tpk.save();
            LayoutInflater factory = LayoutInflater.from(this);
            View view = factory.inflate(R.layout.evolution,null);
            Glide.with(this).load(evo.imgFront).into((ImageView) view.findViewById(R.id.evopic));
            ((TextView)view.findViewById(R.id.message)).setText(prename+" evolved into "+evo.name);
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Congratulations");
            adb.setView(view);
            adb.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            adb.create().show();
        }
    }

    public boolean calcDist(LatLng loc1, LatLng loc2,final double BOUNDARY){
        double dx = Math.pow(loc1.latitude - loc2.latitude, 2);
        double dy = Math.pow(loc1.longitude - loc2.longitude, 2);
        double distance = Math.sqrt(dx+dy)*10000;
        return distance<=BOUNDARY;
    }
    @Override
    public void onBackPressed(){
        if (doubleBackToExitPressedOnce){
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce=true;
        Toast.makeText(this,"Please click BACK again to exit",Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        },2000);
    }

    private class DistanceAsyncTask extends AsyncTask<Void,Void,Boolean>{
        private static final double REFRESHDISTANCE = 7;
        private LatLng myLocation;

        public DistanceAsyncTask(LatLng loc){
            this.myLocation = loc;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            boolean near = false;
            double lat = myLocation.latitude;
            double lon = myLocation.longitude;
            int count=0;

            for (WildMarker wildpkmn : wildMarkers){
                double distance = Math.sqrt(Math.pow(lat - wildpkmn.loc[0], 2) + Math.pow(lon - wildpkmn.loc[1], 2));
                //Log.d("DISTANCE","distance "+10000*distance);
                if(distance*10000<=REFRESHDISTANCE){
                    count++;
                    //Log.d("DISTANCE","Marker with id "+wildpkmn.pkid+" visible, distance "+10000*distance+" visible "+count+" out of "+wildMarkers.size());
                    wildpkmn.visible = true;
                    near = true;
                }else if(distance*10000>REFRESHDISTANCE){
                    wildpkmn.visible = false;
                    near = false;
                }
            }
            Log.d("DISTANCE","visible "+count+" out of "+wildMarkers.size());
            return near;
        }

        @Override
        protected void onPostExecute(Boolean near) {
            super.onPostExecute(near);
            if(near){
                append("wild pokemons near you");
            }else{
                Log.d("REQUESTLOC","requesting more wild pkmons (near = "+near+")");
                requestNewLocation();
            }
        }
    }

    private void requestNewLocation() {
        LatLng loc = mLocation;
        Log.d("Request", "new wild pokemons");
        new HttpAsyncTask(loc.latitude, loc.longitude).execute();
    }

    public void updateVisibility(){
        Log.d("Request", "updates");
        new DistanceAsyncTask(mLocation).execute();
        refreshMarkers();
        updateUI(mLocation,false);

    }

    private class HttpAsyncTask extends AsyncTask<Void, Void, Void> {

        private final double lng;
        private final double lat;

        public HttpAsyncTask(double lat, double lng){
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                pDialog = new ProgressDialog(MapsActivity.this);
                pDialog.setMessage("Please Wait...");
                pDialog.setCancelable(false);
                pDialog.show();
            }catch (Exception ex){

            }
        }

        @Override
        protected Void doInBackground(Void... Voids) {
            String obj = MapsActivity.getStops(lat,lng);
            /*for(WildMarker wm : wildMarkers){
                if(wm.mk!=null) {
                    //wm.mk.setVisible(false);
                    wm.mk.remove();
                }
                wm.visible=false;
                wm.mk=null;
            }*/
            wildMarkers.removeAll(wildMarkers);
            if(obj != null){
                try {
                    JSONArray jsonArray = new JSONArray(obj);
                    Random r = new Random();
                    for (int i = 0 ; i<jsonArray.length();i++) {
                        JSONObject stop = jsonArray.getJSONObject(i);
                        String lt = stop.getString("lt");
                        String lng = stop.getString("lng");
                        double[] loc = {Double.parseDouble(lt), Double.parseDouble(lng)};
                        wildMarkers.add(new WildMarker(r.nextInt(15)+1,loc,false,wildMarkers.size()));
                    }
                    Log.d("MARKERS-HTTP","filled wildmarkers "+wildMarkers.size());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                Log.d("HTTPGET","null response");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(pDialog.isShowing()){
                pDialog.dismiss();
            }
            //setWildPokemons();
        }
    }

    private class MarkerIconAsyncTask extends AsyncTask<String,Void,Bitmap>{
        public Marker mk;
        public MarkerIconAsyncTask(Marker marker){
            this.mk = marker;
        }
        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new URL(imageURL).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Log.d("ICONS","bitmap setted on marker");
            mk.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }
    }
}