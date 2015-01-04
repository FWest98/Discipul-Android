package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

public class MainApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        /* GA setup */
        if(pref.getBoolean("analytics", false)) {
            GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(true);
        }
        if(BuildConfig.GRADLE_DEBUG) {
            GoogleAnalytics.getInstance(getApplicationContext()).setDryRun(true);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        trackers = null;
    }

    private static HashMap<TrackerName, Tracker> trackers = new HashMap<>();

    public static synchronized Tracker getTracker(TrackerName trackerId, Context context) {
        if(!trackers.containsKey(trackerId)) {
            int userID = PreferenceManager.getDefaultSharedPreferences(context).getInt("userid", 0);

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            Tracker tracker = analytics.newTracker(R.xml.app_tracker);
            tracker.enableAdvertisingIdCollection(true);
            if(userID != 0) tracker.set("&uid", String.valueOf(userID));
            trackers.put(trackerId, tracker);
        }

        Tracker returnTracker = trackers.get(trackerId);
        returnTracker.setScreenName(null);
        return returnTracker;
    }

    public enum TrackerName {
        APP_TRACKER
    }
}
