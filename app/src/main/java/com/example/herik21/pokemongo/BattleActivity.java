package com.example.herik21.pokemongo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

public class BattleActivity extends AppCompatActivity {

    public TeamPokemon pk;
    public Pokemon bpk;
    public Pokemon wild;
    public int twHP, wHP, newAtk, newDef;
    public TextView battleLog;
    public TextView pkname, pkhp;
    public TextView wildname, wildhp;
    public ProgressBar hpbar;
    public ProgressBar wildbar;
    public ImageView wildpk;
    public ImageView pkmon;
    public MediaPlayer mPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);
        pkname = (TextView) findViewById(R.id.pkname);
        wildname = (TextView) findViewById(R.id.wildname);
        hpbar = (ProgressBar) findViewById(R.id.hpbar);
        wildbar = (ProgressBar) findViewById(R.id.wildbar);
        pkhp = (TextView)findViewById(R.id.pkhp);
        wildhp = (TextView)findViewById(R.id.wildhp);
        Intent i = getIntent();

        pkmon = (ImageView) findViewById(R.id.mypkmn);
        pk = (TeamPokemon) i.getSerializableExtra("pokemon");
        bpk = (Pokemon) i.getSerializableExtra("base");//pk.basePokemon;
        pkname.setText(bpk.name);

        wildpk = (ImageView) findViewById(R.id.wild);
        wild = (Pokemon) i.getSerializableExtra("wild");
        wildname.setText(wild.name);
        Random r = new Random();
        wHP = wild.hp_max / 2 + r.nextInt(wild.hp_max / 2);
        twHP = wHP;
        newAtk = wild.hp_max / 2 + r.nextInt(wild.atk_max / 2);
        newDef = wild.hp_max / 2 + r.nextInt(wild.def_max / 2);

        battleLog = (TextView) findViewById(R.id.battleLog);
        new DownloadImageTask(0).execute(pk.basePokemon.imgBack);
        new DownloadImageTask(1).execute(wild.imgFront);
        append("wild " + wild.name + " appeared!");
        refreshLowUI();
        refreshHPbarsUI();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int bg;
            if(r.nextInt(3)==0){
                bg = R.drawable.pokemon_battle_bg;
            }else{
                bg = R.drawable.pokemon_battle_bg2;
            }
            Drawable background = getResources().getDrawable(bg);
            ((FrameLayout)findViewById(R.id.battlelayout)).setBackground(background);
        }
        playbgm();
    }
    public void playbgm(){

        try {
            mPlayer = new MediaPlayer();
            Uri uri=Uri.parse("android.resource://"+getPackageName()+"/raw/battlebgm");
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(getApplicationContext(), uri);
            mPlayer.prepare();
            mPlayer.start();
        }catch (IOException ioex){
            Log.d("IOException","BGM playback error");
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        if(mPlayer!=null) {
            mPlayer.pause();
            mPlayer.reset();
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public void refreshLowUI(){
        Trainer tr = new Select().from(Trainer.class).querySingle();
        Button pokeballs = (Button)findViewById(R.id.pokeball);
        Button potions = (Button)findViewById(R.id.potion);
        pokeballs.setText("Pokeball x"+tr.pokeball);
        potions.setText("potion x"+(tr.potion+tr.superpotion));
    }

    public void refreshHPbarsUI(){
        float chp = 100.0f*pk.currenthp/pk.hp;
        Log.d("newhp",""+chp);
        hpbar.setProgress((int)chp);
        if(chp < 50 && chp >= 25){
            hpbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_orange_light), PorterDuff.Mode.SRC_IN);
        }else if(chp < 25){
            hpbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
        }else{
            hpbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_IN);
        }
        pkhp.setText(pk.currenthp+"/"+pk.hp);

        float wchp = 100.0f*wHP/twHP;
        Log.d("new wild hp",""+wchp);
        wildbar.setProgress((int)wchp);
        wildhp.setText(wHP+"/"+twHP);
        if(wchp < 50 && wchp >= 25){
            wildbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_orange_light), PorterDuff.Mode.SRC_IN);
        }else if(wchp < 25 ){
            wildbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
        }else{
            wildbar.getProgressDrawable().setColorFilter(getResources().getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_IN);
        }
    }

    public void append(String entry){
        String log = battleLog.getText().toString();
        battleLog.setText(log+"\n"+entry);
        ((ScrollView)findViewById(R.id.battlelogscroll)).fullScroll(ScrollView.FOCUS_DOWN);
    }

    public int calcDMG(String weakness, String strength, String type,int atk, int def){
        float multiplier = 1f;
        if(weakness.equals(type)){
            multiplier = 2f;
        }
        if(strength.equals(type)){
            multiplier = 0.5f;
        }
        int dmg = (int)((atk-def)*multiplier);
        return dmg > 0 ? dmg : 1;
    }

    public void wildAction(){
        append(wild.name+" attacks!");
        int dmg = calcDMG(bpk.weakness,bpk.strength,wild.type,newAtk,pk.def);
        if(dmg>0){
            append(bpk.name+" took "+dmg+" damage!");
            pk.currenthp = pk.currenthp - dmg;
        }else{
            append(bpk.name+" took 1 damage!");
            pk.currenthp = pk.currenthp -1;
        }
        pk.save();
        refreshHPbarsUI();
        if(pk.currenthp <=0){
            pk.currenthp=0;
            append(wild.name+" Defeated "+bpk.name+"!");
            pkmon.setImageBitmap(null);
            requestChange();
        }else{
            append(bpk.name+" current hp: "+pk.currenthp);
        }
    }

    public void requestChange(){
        pk.currenthp=0;
        pk.main=false;
        pk.save();
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Choose");
        adb.setNegativeButton("Run", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prepareToExit(2,wild.name+" Defeated "+bpk.name+"!");
            }
        });
        adb.setPositiveButton("Keep going", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(BattleActivity.this,TeamActivity.class);
                i.putExtra("change",true);
                startActivityForResult(i,9);
            }
        });
        adb.create().show();

    }
    public void onAttack(View view){
        append(bpk.name+" attacks!");
        int dmg = calcDMG(wild.weakness,wild.strength,bpk.type,pk.atk,newDef);
        if(dmg>0){
            append(wild.name+" took "+dmg+" damage!");
            wHP = wHP-dmg;
        }else{
            append(wild.name+" took 1 damage!");
            wHP = wHP-1;
        }
        refreshHPbarsUI();
        if(wHP>0){
            append(wild.name+" current hp: "+wHP);
            wildAction();
        }else{
            wHP=0;
            append(bpk.name+" Defeated "+wild.name+"!");
            wildpk.setImageBitmap(null);
            prepareToExit(1,bpk.name+" Defeated "+wild.name+"!");
        }
    }
    public void onPokeball(View view){
        Trainer tr = new Select().from(Trainer.class).querySingle();
        int percent = (int)(100 - (float)(70*wHP/twHP));
        Random r = new Random();
        int p = r.nextInt(100);
        Log.d("CAPTURE","chance "+p+" in "+percent);
        tr.pokeball = tr.pokeball-1;
        tr.save();
        refreshLowUI();
        if(p<percent){
            append("wild "+wild.name+" captured!");
            wildpk.setImageBitmap(null);
            if(pk.currenthp+10<pk.hp){
                pk.currenthp+=10;
            }else{
                pk.currenthp=pk.hp;
            }
            pk.save();
            prepareToExit(3,"wild "+wild.name+" captured!");
        }else{
            append("pokeball failed");
            wildAction();
        }
    }

    public void onPotion(View view){
        Trainer tr = new Select().from(Trainer.class).querySingle();
        int potion = tr.potion;
        int superpotion = tr.superpotion;
        CharSequence potions[];
        if(potion>0 || superpotion>0){
            if(potion>0 && superpotion>0) {
                potions = new CharSequence[2];
                potions[0] = "Potion x" + potion;
                potions[1] = "Superpotion x" + superpotion;
            }else{
                potions = new CharSequence[1];
                if(potion==0){
                    potions[0] ="Superpotion x"+superpotion;
                }
                if(superpotion==0){
                    potions[0] ="Potion x"+potion;
                }
            }
            final AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Use which potion?");
            adb.setItems(potions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Trainer tr = new Select().from(Trainer.class).querySingle();
                    if(which==0){
                        append(bpk.name+" heals 20HP");
                        pk.currenthp+=20;
                        tr.potion--;
                    }else{
                        append(bpk.name+" heals 50HP");
                        pk.currenthp+=50;
                        tr.superpotion--;
                    }
                    if(pk.currenthp>=pk.hp){
                        pk.currenthp=pk.hp;
                        findViewById(R.id.potion).setEnabled(false);
                    }
                    tr.save();
                    pk.save();
                    refreshLowUI();
                    float chp = 100.0f*pk.currenthp/pk.hp;
                    Log.d("newhp",""+chp);
                    hpbar.setProgress((int)chp);
                    wildAction();
                }
            }).create().show();

        }else{
            view.setEnabled(false);
        }
    }
    public void onRun(View view){
        append("Got away safely");
        //TODO: update pokemons
        pk.save();
        prepareToExit(0,"Got away safely");
    }

    public void prepareToExit(final int i, String message){
        pk.save();
        if(mPlayer!=null){
            mPlayer.pause();
            mPlayer.reset();
            mPlayer.stop();
            mPlayer.release();
            mPlayer=null;
            Log.d("MPlayer","music stopped");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       boolean lose = true;
                       Intent in = getIntent();
                       if(i==1 || i==3){
                           lose=false;
                           if(i==3){
                               in.putExtra("captured",wild);
                               in.putExtra("bwild",getIntent().getSerializableExtra("wild"));
                               in.putExtra("hp",twHP);
                               in.putExtra("atk",newAtk);
                               in.putExtra("def",newDef);
                           }
                       }
                       in.putExtra("lose",lose);
                       setResult(Activity.RESULT_OK,in);
                       if(mPlayer!=null){
                           mPlayer.reset();
                           mPlayer.stop();
                           mPlayer.release();
                           mPlayer=null;
                           Log.d("MPlayer","music stopped");
                       }
                       in.putExtra("markerid",getIntent().getIntExtra("markerid",-1));
                       finish();
                   }
               });
        builder.create().show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9) {
            if (resultCode == RESULT_OK) {
                pk = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.main.is(true)).and(TeamPokemon_Table.currenthp.greaterThan(0)).querySingle();
                if(pk==null) {
                    TeamPokemon ntpk = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.currenthp.greaterThan(0)).querySingle();
                    if(ntpk!=null) {
                        ntpk.main = true;
                        ntpk.save();
                    }
                    prepareToExit(2, wild.name + " Defeated " + bpk.name + "!");
                }else{
                    bpk = pk.basePokemon;
                    pkname.setText(bpk.name);
                    hpbar.setProgress(100);
                    Glide.with(this).load(bpk.imgBack).into((ImageView) findViewById(R.id.mypkmn));
                }
            }else{
                prepareToExit(2,wild.name+" Defeated "+bpk.name+"!");
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView target;

        public DownloadImageTask(int i) {
            if(i==0) {
                target = (ImageView) findViewById(R.id.mypkmn);
            }else{
                target = (ImageView) findViewById(R.id.wild);
            }
        }

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

        protected void onPostExecute(Bitmap result) {
            target.setImageBitmap(result);
            Log.d("HTTPGET-IMG","set result on view");
        }
    }
}
