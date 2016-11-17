package com.example.herik21.pokemongo;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import java.io.Serializable;

@Table(database = AppDatabase.class)
public class Pokemon extends BaseModel implements Serializable{
    @Column
    @PrimaryKey //(autoincrement = false )
    public long id;
    @Column
    public String name;
    @Column
    public String type;
    @Column
    public String strength;
    @Column
    public String weakness;
    @Column
    public int hp_max;
    @Column
    public int atk_max;
    @Column
    public int def_max;
    @Column
    public long ev_id;
    @Column
    public String imgFront;
    @Column
    public String imgBack;


    public Pokemon(){}

    public Pokemon(long id, String name, String type, String strength, String weakness, int hp_max, int atk_max, int def_max, String imgFront,String imgBack,long ev_id){
        this.id=id;
        this.name=name;
        this.type=type;
        this.strength=strength;
        this.weakness=weakness;
        this.hp_max=hp_max;
        this.atk_max=atk_max;
        this.def_max=def_max;
        this.ev_id=ev_id;
        this.imgFront=imgFront;
        this.imgBack=imgBack;
    }
}
