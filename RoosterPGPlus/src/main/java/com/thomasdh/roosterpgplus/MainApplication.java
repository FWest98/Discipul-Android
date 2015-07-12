package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.util.HashMap;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://logging.discipul.nl:5984/acra-discipul/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "discipul_reporter",
        formUriBasicAuthPassword = "2phh!3GRakVd"
)
public class MainApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        /* GA setup */
        if(pref.getBoolean("analytics", false)) {
            GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(true);
        }
        if(BuildConfig.GRADLE_DEBUG) {
            GoogleAnalytics.getInstance(getApplicationContext()).setDryRun(true);
        }

        Log.e("GRADLE DEBUG", String.valueOf(BuildConfig.GRADLE_DEBUG));
        Log.e("DEBUG", String.valueOf(BuildConfig.DEBUG));
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
