package com.thomasdh.roosterpgplus.roosterdata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Thomas on 30-12-13.
 */
public class SQLRooster extends SQLiteOpenHelper{

    public static final String TABLE_ROOSTER = "rooster";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DAG = "dag";
    public static final String COLUMN_UUR = "uur";
    public static final String COLUMN_WEEK = "week";
    public static final String COLUMN_VERANDERING = "verandering";
   public static final String COLUMN_VERVALLEN = "vervallen";
    public static final String COLUMN_VERPLAATSING = "verplaatsing";
    public static final String COLUMN_BIJZONDERHEID = "bijzonderheid";
    public static final String COLUMN_MASTER = "master";
    public static final String COLUMN_KLAS = "klas";
    public static final String COLUMN_VAK = "vak";
    public static final String COLUMN_LOKAAL = "lokaal";
    public static final String COLUMN_LERAAR = "leraar";

    public static final String DATABASE_NAME = "rooster.db";
    public static final int DATABASE_VERSION = 1;


    public static final String DATABASE_CREATE = "create table " + TABLE_ROOSTER +
            "(" + COLUMN_ID + " integer primary key autoincrement, "+
            COLUMN_DAG + " int(1), " +
            COLUMN_UUR + " int(1), " +
            COLUMN_WEEK + " int(2), " +
            COLUMN_VERANDERING + " int(1), " +
            COLUMN_VERVALLEN + " int(1), " +
            COLUMN_VERPLAATSING + " int(1), " +
            COLUMN_BIJZONDERHEID + " int(1), " +
            COLUMN_MASTER + " int(1), " +
            COLUMN_KLAS + " varchar(10), " +
            COLUMN_VAK + " varchar(45), " +
            COLUMN_LOKAAL + " varchar(45)," +
            COLUMN_LERAAR + " varchar(45)" +
            ");";


    public SQLRooster(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOSTER);
        onCreate(db);
    }
}
