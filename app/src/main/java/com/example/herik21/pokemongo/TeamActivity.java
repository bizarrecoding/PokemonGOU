package com.example.herik21.pokemongo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;
import java.util.Objects;

public class TeamActivity extends AppCompatActivity {

    public ListView lv;
    public CustomAdapter cAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Trainer tr= new Select().from(Trainer.class).querySingle();
        getSupportActionBar().setTitle(tr.name+"'s team");
        lv = (ListView)findViewById(R.id.listView);
        List<TeamPokemon> myTeam = new Select().from(TeamPokemon.class).orderBy(TeamPokemon_Table.main,false).queryList();
        cAdapter = new CustomAdapter(this,myTeam);
        Log.d("size",""+cAdapter.getCount());
        lv.setAdapter(cAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence request[] = new CharSequence[3];
                request[0]="Pokeballs";
                request[1]="Potions";
                request[2]="Superpotions";
                AlertDialog.Builder adb = new AlertDialog.Builder(TeamActivity.this);
                adb.setTitle("Request");
                adb.setItems(request, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Trainer tr= new Select().from(Trainer.class).querySingle();
                        switch (which){
                            case 0:
                                tr.pokeball+=5;
                                break;
                            case 1:
                                tr.potion+=5;
                                break;
                            case 2:
                                tr.superpotion+=5;
                                break;
                        }
                        tr.save();
                        Snackbar.make(TeamActivity.this.lv, "Request done", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }).create().show();
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(TeamActivity.this, PokemonActivity.class);
                if(cAdapter.team.get(position)!=null){
                    TeamPokemon tpk = cAdapter.team.get(position);
                    i.putExtra("pokemon",tpk);
                    i.putExtra("base",new Select().from(Pokemon.class).where(Pokemon_Table.id.is(tpk.basePokemon.id)).querySingle());
                    if(getIntent().getBooleanExtra("change",false)){
                        i.putExtra("change",true);
                    }
                    startActivityForResult(i, 1);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        cAdapter.setTeam(new Select().from(TeamPokemon.class).orderBy(TeamPokemon_Table.main,false).queryList());
        cAdapter.notifyDataSetChanged();
        Intent parent = getIntent();
        if(parent.getBooleanExtra("change",false)){
            Intent in = getIntent();
            in.putExtra("newlead",data.getStringExtra("lead"));
            setResult(Activity.RESULT_OK,in);
            finish();
        }
    }
}
