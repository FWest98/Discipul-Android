package com.thomasdh.roosterpgplus.Settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class OverigFragment extends ThemedPreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_overig);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_OVERIG);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        findPreference("analytics").setOnPreferenceChangeListener((pref, newValue) -> {
            GoogleAnalytics.getInstance(getActivity().getApplicationContext()).setAppOptOut((Boolean) newValue);
            return true;
        });
    }
}
