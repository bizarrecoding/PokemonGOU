package com.example.herik21.pokemongo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

    //DB info
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "PokemonDB";

    //DB columns
    public static final String TABLE ="myTable";
    public static final String KEY_ID = "id";
    public static final String FIELD1 = "field1";
    public static final String FIELD2 = "filed2";

    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.d("Test","DataHandler created");
    }

    public void onCreate(SQLiteDatabase db) {
        String createQuery = "CREATE TABLE "+TABLE+"("+KEY_ID+" integer primary key, "+
                FIELD1+" integer, "+FIELD2+" integer )";
        if(db.isOpen()){
            db.execSQL(createQuery);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE);
    }

    public SQLiteDatabase getWriteDatabase(){
        return super.getWritableDatabase();
    }
}
