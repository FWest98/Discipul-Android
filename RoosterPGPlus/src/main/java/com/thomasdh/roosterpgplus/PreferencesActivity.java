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
import android.view.MenuItem;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
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
        return UserFragment.class.getName().equals(fragmentName) ||
                InfoFragment.class.getName().equals(fragmentName) ||
                OverigFragment.class.getName().equals(fragmentName) ||
                AchtergrondFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Voor android versies vóór 3.0
        String action = getIntent().getAction();
        if ("com.thomasdh.roosterpgplus.PreferencesActivity$InfoFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_info);
            try {
                findPreference("versie").setTitle("Versie: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_user);
            preferenceListener = new PreferenceListener2();


            // Initialize subklassen
            final ListPreferenceMultiSelect subklassen = (ListPreferenceMultiSelect) findPreference("subklassen");
            user = new Account(this);
            if (user.isAppAccount) {
                subklassen.setEnabled(false);
            } else {
                subklassen.setEnabled(true);
            }
            subklassen.setEntries(new String[]{"Subklassen"});
            subklassen.setEntryValues(new String[]{"Subklassen"});
            new AsyncTask<Void, Exception, ArrayList<Account.Subklas>>() {
                @Override
                protected ArrayList<Account.Subklas> doInBackground(Void... params) {
                    try {
                        return user.getSubklassen();
                    } catch (Exception e) {
                        publishProgress(e);
                        return null;
                    }
                }

                @Override
                protected void onProgressUpdate(Exception... values) {
                    ExceptionHandler.handleException(values[0], getApplicationContext(), "Fout bij het ophalen van de subklassen", "PreferencesActivity", ExceptionHandler.HandleType.EXTENSIVE);
                }

                @Override
                protected void onPostExecute(ArrayList<Account.Subklas> subklasArray) {
                    if (subklassen != null && subklasArray != null) {
                        ArrayList<String> strings = new ArrayList<String>();
                        ArrayList<String> namen = new ArrayList<>();
                        for (Account.Subklas subklas : subklasArray) {
                            strings.add(subklas.subklas + ": " + subklas.vak + " van " + subklas.leraar);
                            namen.add(subklas.subklas);
                        }

                        subklassen.setEntries(strings.toArray(new String[strings.size()]));
                        subklassen.setEntryValues(namen.toArray(new String[namen.size()]));

                        subklassen.setOnPreferenceChangeListener((preference, newValue) -> {
                            new AsyncTask<Void, Exception, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    try {
                                        user.setSubklassen(((ArrayList<String>) newValue).toArray(new String[((ArrayList<String>) newValue).size()]));
                                    } catch (Exception e) {
                                        publishProgress(e);
                                    }
                                    return null;
                                }

                                @Override
                                protected void onProgressUpdate(Exception... e) {
                                    ExceptionHandler.handleException(e[0], getApplicationContext(), "SetSubklasfout", "PreferencesActivity", ExceptionHandler.HandleType.SIMPLE);
                                }
                            }.execute();
                            return true;
                        });
                    } else {
                        Log.d("PreferenceActivity", "Ze zijn null");
                    }
                }
            }.execute();

            // Create user and fill in account information
            user = new Account(this);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
            findPreference("mijn_account").setSummary(
                    "Naam: " + user.name + ", " +
                            "Klas: " + user.klas
            );

            // Configure login button
            findPreference("log_in").setOnPreferenceClickListener(preference -> {
                user.showLoginDialog();
                return true;
            });

            final Context context = this;

            /* account upgraden */
            if (!new Account(context).isAppAccount) {
                findPreference("account_upgraden").setEnabled(false);
            } else {
                findPreference("account_upgraden").setEnabled(true);
            }
            findPreference("account_upgraden").setOnPreferenceClickListener(preference -> {
                try {
                    user.extend();
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.extenddialog_isExtended), Toast.LENGTH_SHORT).show();
                }
                return false;
            });
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$OverigFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_overig);
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$AchtergrondFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_achtergrond);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preference_headers_old);
        }

        user = new Account(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        String action = getIntent().getAction();
        if ("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment".equals(action)) {
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);
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
    protected void onPause() {
        super.onPause();
        String action = getIntent().getAction();
        if ("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment".equals(action)) {
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
    public static class OverigFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_overig);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AchtergrondFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_achtergrond);
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
            if (user.isAppAccount) {
                subklassen.setEnabled(false);
            } else {
                subklassen.setEnabled(true);
            }
            subklassen.setEntries(new String[]{"Subklassen"});
            subklassen.setEntryValues(new String[]{"Subklassen"});
            new AsyncTask<Void, Exception, ArrayList<Account.Subklas>>() {
                @Override
                protected ArrayList<Account.Subklas> doInBackground(Void... params) {
                    try {
                        return user.getSubklassen();
                    } catch (Exception e) {
                        publishProgress(e);
                        return null;
                    }
                }

                @Override
                protected void onProgressUpdate(Exception... values) {
                    ExceptionHandler.handleException(values[0], getActivity(), "Fout bij het ophalen van de subklassen", "PreferencesActivity", ExceptionHandler.HandleType.EXTENSIVE);
                }

                @Override
                protected void onPostExecute(ArrayList<Account.Subklas> subklasArray) {
                    if (subklassen != null && subklasArray != null) {
                        ArrayList<String> strings = new ArrayList<String>();
                        ArrayList<String> namen = new ArrayList<String>();
                        for (Account.Subklas subklas : subklasArray) {
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
                                            user.setSubklassen(((ArrayList<String>) newValue).toArray(new String[((ArrayList<String>) newValue).size()]));
                                        } catch (Exception e) {
                                            publishProgress(e);
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onProgressUpdate(Exception... e) {
                                        ExceptionHandler.handleException(e[0], getActivity(), "SetSubklasfout", "PreferencesActivity", ExceptionHandler.HandleType.SIMPLE);
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
            user = new Account(getActivity());
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
            } else {
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
                user = new Account(getActivity());
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
            user = new Account(getApplicationContext());
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
