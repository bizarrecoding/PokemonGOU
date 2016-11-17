package com.example.herik21.pokemongo;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

/**
 * Created by Herik21 on 16/11/2016.
 */
@Table(database = AppDatabase.class)
public class Trainer extends BaseModel{
    @Column
    @PrimaryKey(autoincrement = true )
    public long id;
    @Column
    public String name;
    @Column
    public int pokeball;
    @Column
    public int potion;
    @Column
    public int superpotion;

    public Trainer(){}

    public Trainer(String name, int b, int p, int sp){
        this.name = name;
        this.pokeball=b;
        this.potion=p;
        this.superpotion=sp;
    }

    public void throwPokeball() {
        this.pokeball--;
    }

    public void useSuperpotion() {
        this.superpotion--;
    }

    public void usePotion() {
        this.potion--;
    }
}
