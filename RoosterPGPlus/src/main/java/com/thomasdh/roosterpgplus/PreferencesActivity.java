package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.thomasdh.roosterpgplus.roosterdata.RoosterInfoDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 6-12-13.
 */
public class PreferencesActivity extends PreferenceActivity {

    private static PreferenceListener2 preferenceListener;
    private Account user2;
    final private Account user = user2;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (UserFragment.class.getName().equals(fragmentName) ||
                InfoFragment.class.getName().equals(fragmentName)){
            return true;
        }
        return false;
    }

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
            final Context context = this;
            findPreference("account_upgraden").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO code toevoegen voor android 2.x
                    try {
                        user.extend();
                    } catch(Exception e) {
                        Toast.makeText(context, context.getString(R.string.extenddialog_isExtended), Toast.LENGTH_SHORT).show();
                    }
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
                                new AsyncTask<Void, Exception, Void>(){
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        try {
                                            RoosterInfoDownloader.setSubklassen(getActivity(),
                                                    ((ArrayList<String>) newValue).toArray(new String[((ArrayList<String>) newValue).size()]));
                                        } catch (Exception e) {
                                            publishProgress(e);
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onProgressUpdate(Exception... e) {
                                        Toast.makeText(getActivity(), e[0].getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("PreferencesActivity", "SetSubklasfout", e[0]);
                                        EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
                                        easyTracker.send(MapBuilder
                                                .createException(new StandardExceptionParser(getActivity(), null)
                                                        //True betekent geen fatale exceptie
                                                        .getDescription(Thread.currentThread().getName(), e[0]), true)
                                                .build()
                                        );
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

            final Context context = getActivity();

            findPreference("account_upgraden").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        user.extend();
                    } catch (Exception e) {
                        Toast.makeText(context, context.getString(R.string.extenddialog_isExtended), Toast.LENGTH_SHORT).show();
                    }
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
