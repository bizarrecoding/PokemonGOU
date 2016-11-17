package com.example.herik21.pokemongo;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@Table(database = AppDatabase.class)
public class TeamPokemon extends BaseModel implements Serializable {
    @Column
    @PrimaryKey (autoincrement = true )
    public long id;
    @Column
    @ForeignKey(saveForeignKeyModel = false)
    public Pokemon basePokemon;
    @Column
    public int hp;
    @Column
    public int currenthp;
    @Column
    public int atk;
    @Column
    public int def;
    @Column
    public boolean main;


    public TeamPokemon(){}

    public TeamPokemon(Pokemon pk, int hp, int atk, int def, boolean main){
        this.basePokemon=pk;
        this.hp=hp;
        this.currenthp=hp;
        this.atk=atk;
        this.def=def;
        this.main=main;
    }
}
