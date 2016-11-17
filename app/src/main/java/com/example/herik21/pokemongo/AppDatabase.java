package com.example.herik21.pokemongo;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by Herik21 on 10/09/2016.
 */
@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {
    public static final String NAME = "PokemonDB";
    public static  final int VERSION = 1;
}

