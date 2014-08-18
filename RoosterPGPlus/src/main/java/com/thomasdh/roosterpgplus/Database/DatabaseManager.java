package com.thomasdh.roosterpgplus.Database;

import android.content.Context;

public class DatabaseManager {
    private static DatabaseHelper helper;

    public static DatabaseHelper getHelper(Context context) {
        if(helper == null) {
            helper = new DatabaseHelper(context);
        }
        return helper;
    }
}
