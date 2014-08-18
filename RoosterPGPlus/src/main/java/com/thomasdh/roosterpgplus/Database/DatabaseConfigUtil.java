package com.thomasdh.roosterpgplus.Database;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

/**
 * Created by Floris on 14-7-2014.
 */
public class DatabaseConfigUtil extends OrmLiteConfigUtil {
    public static void main(String[] args) throws Exception {
        writeConfigFile("ormlite_config.txt");
    }
}
