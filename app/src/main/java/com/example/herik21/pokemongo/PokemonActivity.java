package com.example.herik21.pokemongo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;

public class PokemonActivity extends AppCompatActivity {

    public TextView pname,hp,atk,def,type;
    public ImageView pkimg;
    public Button leader;
    public TeamPokemon tpk;
    public Pokemon base;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);
        leader= (Button)findViewById(R.id.leader);
        pname = (TextView)findViewById(R.id.pkname);
        type = (TextView)findViewById(R.id.type);
        hp = (TextView)findViewById(R.id.hp);
        atk = (TextView)findViewById(R.id.atk);
        def = (TextView)findViewById(R.id.def);
        Intent parent = getIntent();
        tpk = (TeamPokemon) parent.getSerializableExtra("pokemon");
        base = (Pokemon) parent.getSerializableExtra("base");
        Log.d("name",base.name);
        hp.setText(tpk.currenthp+"/"+tpk.hp);
        atk.setText(tpk.atk+"/"+base.atk_max);
        def.setText(tpk.def+"/"+base.def_max);
        pname.setText(base.name);
        pkimg = (ImageView)findViewById(R.id.pkimg);
        if(tpk.main){
            leader.setEnabled(false);
            leader.setText("Leader");
        }
        if(tpk.currenthp<1){
            leader.setEnabled(false);
        }
        Glide.with(this).load(base.imgFront).into(pkimg);
        type.setText(base.type);
        int color=0;
        switch (base.type.toLowerCase()){
            case "fire":
                color = android.R.color.holo_red_light;
                break;
            case "water":
                color = android.R.color.holo_blue_dark;
                break;
            case "grass":
                color = android.R.color.holo_green_dark;
                break;
            case "electric":
                color = R.color.yellow;
                break;
            case "dragon":
                color = R.color.purple;
                break;
            case "psychic":
                color = R.color.pink;
                break;
            case "ghost":
                color = android.R.color.holo_purple;
                break;
            default:
                color = android.R.color.black;
                break;
        }
        type.setTextColor(getResources().getColor(color));
        if(tpk.currenthp==tpk.hp){
            findViewById(R.id.heal).setEnabled(false);
        }
    }

    public void refreshUI(){
        hp.setText(tpk.currenthp+"/"+tpk.hp);
        atk.setText(tpk.atk+"/"+base.atk_max);
        def.setText(tpk.def+"/"+base.def_max);
    }

    public void onClickHeal(View v){
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
                        tpk.currenthp+=20;
                        SQLite.update(Trainer.class).set(Trainer_Table.potion.eq(tr.potion-1)).execute();
                    }else{
                        tpk.currenthp+=50;
                        SQLite.update(Trainer.class).set(Trainer_Table.potion.eq(tr.superpotion-1)).execute();
                    }
                    if(tpk.currenthp>=tpk.hp){
                        tpk.currenthp=tpk.hp;
                        findViewById(R.id.heal).setEnabled(false);
                    }
                    //tr.save();
                    tpk.save();
                    refreshUI();
                }
            }).create().show();

        }else{
            v.setEnabled(false);
        }
    }


    public void onClickLeader(View v){
        TeamPokemon oldlead = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.main.is(true)).querySingle();
        if(oldlead!=null) {
            oldlead.main = false;
            oldlead.save();
        }
        TeamPokemon newlead = new Select().from(TeamPokemon.class).where(TeamPokemon_Table.id.is(tpk.id)).querySingle();
        newlead.main = true;
        newlead.save();
        leader.setEnabled(false);
        leader.setText("Leader");
        if(getIntent().getBooleanExtra("change",false)){
            Intent in = getIntent();
            in.putExtra("newlead","go");
            setResult(Activity.RESULT_OK,in);
            finish();
        }
    }
}
