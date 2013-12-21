package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

import java.util.List;

/**
 * Created by Thomas on 6-12-13.
 */
public class PreferencesActivity extends PreferenceActivity {

    private static PreferenceListener2 preferenceListener;
    private Account user2;
    final private Account user = user2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceListener = new PreferenceListener2();
        // Voor android versies voor 3.0
        String action = getIntent().getAction();
        if (action != null && action.equals("com.thomasdh.roosterpgplus.PreferencesActivity$InfoFragment")) {
            addPreferencesFromResource(R.xml.preferences_info);
        } else if (action != null && action.equals("com.thomasdh.roosterpgplus.PreferenceActivity$UserFragment")) {
            addPreferencesFromResource(R.xml.preferences_user);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preference_headers_old);
        }

        this.user2 = new Account(this);
    }

    @Override
    protected void onStop() {
        EasyTracker tracker = EasyTracker.getInstance(getApplicationContext());
        tracker.set(Fields.SCREEN_NAME, null);
        tracker.send(MapBuilder
                .createAppView()
                .build()
        );
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String action = getIntent().getAction();
        if (action != null && action.equals("com.thomasdh.roosterpgplus.PreferencesActivity$InfoFragment")) {
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );

            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    user.showLoginDialog();
                    return true;
                }
            });
        }

        if (action!=null && action.equals("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment")){
            findPreference("account_upgraden").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO code toevoegen voor android 2.x
                    return false;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        String action = getIntent().getAction();
        if (action != null && action.equals("com.thomasdh.roosterpgplus.PreferencesActivity$InfoFragment")) {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
    }

    @Override
    protected void onStart() {
        EasyTracker tracker = EasyTracker.getInstance(getApplicationContext());
        tracker.set(Fields.SCREEN_NAME, "Instellingen");
        tracker.send(MapBuilder
                .createAppView()
                .build()
        );
        super.onStart();
    }

    @Override
    public boolean hasHeaders() {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class InfoFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_info);
            try {
                findPreference("versie").setTitle("Versie: " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class UserFragment extends PreferenceFragment {

        public PreferenceListener preferenceListener;
        private Account user;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_user);
            preferenceListener = new PreferenceListener();
            this.user = new Account(getActivity());
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );

            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    user.showLoginDialog();
                    return true;
                }
            });

            final ListPreferenceMultiSelect subklassen = (ListPreferenceMultiSelect) findPreference("subklassen");
            new AsyncTask<Void, Void, String[]>() {
                @Override
                protected String[] doInBackground(Void... params) {
                    return null;
                }

                @Override
                protected void onPostExecute(String[] s) {
                    if (subklassen != null && s != null) {
                        subklassen.setEntries(s);
                        subklassen.setEntryValues(s);
                    }
                }
            }.execute();

            findPreference("account_upgraden").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO code toevoegen
                    return false;
                }
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }

        public class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                findPreference("mijn_account").setSummary(
                        "Naam: " + user.name + ", " +
                                "Klas: " + user.klas
                );
            }
        }
    }

    private class PreferenceListener2 implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );
        }
    }

}
