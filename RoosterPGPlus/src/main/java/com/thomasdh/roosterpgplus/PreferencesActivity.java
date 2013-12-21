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
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.thomasdh.roosterpgplus.roosterdata.RoosterInfoDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 6-12-13.
 */
public class PreferencesActivity extends PreferenceActivity {

    static PreferenceListener2 preferenceListener;

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
                    "Naam: " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("naam", "-") + ", " +
                            "Klas: " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("klas", "-")
            );

            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new LoginDialogClass(getApplicationContext(), null, null).showLoginDialog();
                    return true;
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

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_user);
            preferenceListener = new PreferenceListener();

            final ListPreferenceMultiSelect subklassen = (ListPreferenceMultiSelect) findPreference("subklassen");
            subklassen.setEntries(new String[]{"Subklassen"});
            subklassen.setEntryValues(new String[]{"Subklassen"});
            new AsyncTask<Void, Void, ArrayList<RoosterInfoDownloader.Subklas>>() {
                @Override
                protected ArrayList<RoosterInfoDownloader.Subklas> doInBackground(Void... params) {
                    try {
                        return RoosterInfoDownloader.getSubklassen(getActivity());
                    } catch (Exception e) {
                        //TODO analytics
                        Log.e("PreferenceActivity", "Fout", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(ArrayList<RoosterInfoDownloader.Subklas> subklasArray) {
                    if (subklassen != null && subklasArray != null) {
                        ArrayList<String> strings = new ArrayList<String>();
                        ArrayList<String> namen = new ArrayList<String>();
                        for (RoosterInfoDownloader.Subklas subklas : subklasArray) {
                            strings.add(subklas.subklas + ": " + subklas.vak + " van " + subklas.leraar);
                            namen.add(subklas.subklas);
                        }

                        subklassen.setEntries(strings.toArray(new String[strings.size()]));
                        subklassen.setEntryValues(namen.toArray(new String[namen.size()]));

                        subklassen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, final Object newValue) {
                                new AsyncTask<Void, Void, Void>(){
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        try {
                                            RoosterInfoDownloader.setSubklassen(getActivity(),
                                                    ((ArrayList<String>) newValue).toArray(new String[((ArrayList<String>) newValue).size()]));
                                        } catch (Exception e) {
                                            Log.e("PreferencesActivity", "SetSubklasfout", e);
                                        }
                                        return null;
                                    }
                                }.execute();
                                return true;
                            }
                        });
                    } else {
                        Log.d("PreferenceActivity", "Ze zijn null");
                    }
                }
            }.execute();

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("naam", "-") + ", " +
                            "Klas: " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("klas", "-")
            );

            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new LoginDialogClass(getActivity(), null, null).showLoginDialog();
                    return true;
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
                        "Naam: " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("naam", "-") + ", " +
                                "Klas: " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("klas", "-")
                );
            }
        }
    }

    public class PreferenceListener2 implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            findPreference("mijn_account").setSummary(
                    "Naam: " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("naam", "-") + ", " +
                            "Klas: " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("klas", "-")
            );
        }
    }

}
