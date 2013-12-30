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
import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.thomasdh.roosterpgplus.roosterdata.RoosterInfoDownloader;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 6-12-13.
 */
public class PreferencesActivity extends PreferenceActivity {

    private static PreferenceListener2 preferenceListener;
    private Account user;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (UserFragment.class.getName().equals(fragmentName) ||
                InfoFragment.class.getName().equals(fragmentName)) {
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

        user = new Account(this);
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

        if (action != null && action.equals("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment")) {
            addPreferencesFromResource(R.xml.preferences_user);
            preferenceListener = new PreferenceListener2();


            // Initialize subklassen
            final ListPreferenceMultiSelect subklassen = (ListPreferenceMultiSelect) findPreference("subklassen");
            if (user.isAppAccount){
                subklassen.setEnabled(false);
            }else{
                subklassen.setEnabled(true);
            }
            subklassen.setEntries(new String[]{"Subklassen"});
            subklassen.setEntryValues(new String[]{"Subklassen"});
            new AsyncTask<Void, Void, ArrayList<RoosterInfoDownloader.Subklas>>() {
                @Override
                protected ArrayList<RoosterInfoDownloader.Subklas> doInBackground(Void... params) {
                    try {
                        return RoosterInfoDownloader.getSubklassen(getApplicationContext());
                    } catch (Exception e) {
                        ExceptionHandler.handleException(e, getApplicationContext(), "Fout bij het laden van subklassen", "PreferencesActivity", ExceptionHandler.HandleType.EXTENSIVE);
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
                                new AsyncTask<Void, Exception, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        try {
                                            RoosterInfoDownloader.setSubklassen(getApplicationContext(),
                                                    ((ArrayList<String>) newValue).toArray(new String[((ArrayList<String>) newValue).size()]));
                                        } catch (Exception e) {
                                            publishProgress(e);
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onProgressUpdate(Exception... e) {
                                        Toast.makeText(getApplicationContext(), e[0].getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("PreferencesActivity", "SetSubklasfout", e[0]);
                                        EasyTracker easyTracker = EasyTracker.getInstance(getApplicationContext());
                                        easyTracker.send(MapBuilder
                                                .createException(new StandardExceptionParser(getApplicationContext(), null)
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

            // Create user and fill in account information
            this.user = new Account(this);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );

            // Configure login button
            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    user.showLoginDialog();
                    return true;
                }
            });

            final Context context = this;

            /* account upgraden */
            if (!new Account(context).isAppAccount) {
                findPreference("account_upgraden").setEnabled(false);
            }else{
                findPreference("account_upgraden").setEnabled(true);
            }
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


            // Initialize subklassen
            final ListPreferenceMultiSelect subklassen = (ListPreferenceMultiSelect) findPreference("subklassen");
            user = new Account(getActivity());
            if (user.isAppAccount){
                subklassen.setEnabled(false);
            }else{
                subklassen.setEnabled(true);
            }
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
                                new AsyncTask<Void, Exception, Void>() {
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

            // Create user and fill in account information
            this.user = new Account(getActivity());
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );

            // Configure login button
            findPreference("log_in").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    user.showLoginDialog();
                    return true;
                }
            });

            final Context context = getActivity();

            /* account upgraden */
            if (!new Account(context).isAppAccount) {
                findPreference("account_upgraden").setEnabled(false);
            }else{
                findPreference("account_upgraden").setEnabled(true);
            }
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
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
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
                if (!new Account(getActivity()).isAppAccount) {
                    findPreference("account_upgraden").setEnabled(false);
                    findPreference("subklassen").setEnabled(true);
                } else {
                    findPreference("account_upgraden").setEnabled(true);
                    findPreference("subklassen").setEnabled(false);
                }
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
            if (!new Account(getApplicationContext()).isAppAccount) {
                findPreference("account_upgraden").setEnabled(false);
                findPreference("subklassen").setEnabled(true);
            } else {
                findPreference("account_upgraden").setEnabled(true);
                findPreference("subklassen").setEnabled(false);
            }
        }
    }

}
