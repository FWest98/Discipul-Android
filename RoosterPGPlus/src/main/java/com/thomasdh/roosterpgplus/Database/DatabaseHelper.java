package com.thomasdh.roosterpgplus.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import java.sql.SQLException;
import java.util.Hashtable;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "roosterpgplus.db";
    private static final int DATABASE_VERSION = 1;

    private Hashtable<String, Dao<?, Integer>> daoHashtable = new Hashtable<>();

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Lesuur.class);
        } catch (SQLException e) {
            Log.e("ERROR", "Fout bij maken DB", e);
            throw new RuntimeException(e);
        }

        // Data proberen

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, Lesuur.class, true);

            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e("ERROR", "Fout bij verwijderen DB", e);
            throw new RuntimeException(e);
        }
    }

    public <T> Dao<T, Integer> getDaoWithCache(Class<T> type) throws SQLException {
        String className = type.getName();
        if(daoHashtable.containsKey(className)) {
            return (Dao<T, Integer>) daoHashtable.get(className);
        }
        Dao<T, Integer> newDao = this.<Dao<T, Integer>, T>getDao(type);
        daoHashtable.put(className, newDao);

        return newDao;
    }

    @Override
    public void close() {
        super.close();
        daoHashtable = null;
    }
}
