package com.example.herik21.pokemongo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDoneException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.property.IProperty;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog pDialog;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FlowManager.init(new FlowConfig.Builder(this).openDatabasesOnInit(true).build());
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connMgr.getActiveNetworkInfo();
        if (!ni.isConnected()) {
            Toast.makeText(this, "Not Connected to Internet", Toast.LENGTH_LONG).show();
        }
        try {
            long count = new Select().from(Pokemon.class).count();
            if (count < 1) {
                Log.d("count", ""+count);
                Delete.table(Pokemon.class);
                new HttpAsyncTask().execute();
                //   findViewById(R.id.contbtn).setEnabled(false);
            } else {
                Log.d("HTTPGET", "DB already filled");
            }
        }catch (SQLiteDoneException ex){
            findViewById(R.id.contbtn).setEnabled(false);
            Log.d("HTTPFAILED",ex.getMessage());
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            return;
        }
    }
    public void onNewGame(View view) {
        Intent i = new Intent(this,ChooseActivity.class);
        startActivity(i);
    }
    public void onContinue(View view) {
        try{
            if(0L < new Select().from(TeamPokemon.class).count()) {
                Intent i = new Intent(this, MapsActivity.class);
                startActivity(i);
            }else{
                //no save file
                view.setEnabled(false);
            }
        }catch (Exception ex){
            view.setEnabled(false);
        }
    }
    public void onBattle(View view) {
        Intent i = new Intent(this,TeamActivity.class);
        startActivity(i);
    }

    public static String getPokemons(){
        String response = "";
        try{
            URL url = new URL("http://realizzazione-sito.tk/dev/service/pokemonGo.json");
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String result = "";
            while ((result = in.readLine())!=null){
                response = response + result;
            }
            in.close();
            return response;
        }catch (Exception ex){
            Log.d("HTTPGET",ex.getLocalizedMessage());
            return null;
        }
    }
    private class HttpAsyncTask extends AsyncTask<Void, Void, Void> {
        public HttpAsyncTask(){}
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }
        @Override
        protected Void doInBackground(Void... Voids) {
            String obj = MainActivity.getPokemons();
            Log.d("HTTPGET(129)",obj);
            if(obj != null){
                try {
                    JSONArray jsonArray = new JSONArray(obj);
                    for (int i = 0 ; i<jsonArray.length();i++){
                        JSONObject pokemon = jsonArray.getJSONObject(i);
                        Pokemon pk = new Pokemon(
                            pokemon.getLong("id"),
                            pokemon.getString("name"),
                            pokemon.getString("type"),
                            pokemon.getString("strength"),
                            pokemon.getString("weakness"),
                            pokemon.getInt("hp_max"),
                            pokemon.getInt("ataque_max"),
                            pokemon.getInt("defensa_max"),
                            pokemon.getString("ImgFront"),
                            pokemon.getString("ImgBack"),
                            pokemon.getInt("ev_id")
                        );
                        pk.def_max-=5;
                        pk.save();
                    }
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
        }
    }
}
