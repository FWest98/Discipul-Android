package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.thomasdh.roosterpgplus.CustomUI.ListPreferenceMultiSelect;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 6-12-13.
 */
public class PreferencesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return UserFragment.class.getName().equals(fragmentName) ||
                InfoFragment.class.getName().equals(fragmentName) ||
                OverigFragment.class.getName().equals(fragmentName) ||
                AchtergrondFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String summary;
        if(Account.getUserType() == Account.UserType.LEERLING) {
            summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
        } else {
            summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
        }
        findPreference("mijn_account").setSummary(summary);

        findPreference("account_upgraden").setEnabled(Account.isAppAccount());
        findPreference("subklassen").setEnabled(!Account.isAppAccount());
        findPreference("clusterklassen_reload").setEnabled(!Account.isAppAccount());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Account.initialize(this);

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

            // Initialize subklassen
            ListPreferenceMultiSelect subklassenPref = (ListPreferenceMultiSelect) findPreference("subklassen");
            subklassenPref.setEnabled(!Account.isAppAccount());
            subklassenPref.setEntries(new String[]{"Subklassen"});
            subklassenPref.setEntryValues(new String[]{"Subklassen"});

            subklassenPref.setOnPreferenceChangeListener((preference, newValue) -> {
                ArrayList<String> newSubklassen = (ArrayList<String>) newValue;
                Account.getInstance(this).setSubklassen(false, newSubklassen, result -> {});

                return true;
            });

            Account.getInstance(this).getSubklassen(true, result -> {
                ArrayList<Account.Subklas> subklasArray = (ArrayList<Account.Subklas>) result;

                if (subklassenPref != null && subklasArray != null) {
                    ArrayList<String> strings = new ArrayList<>();
                    ArrayList<String> namen = new ArrayList<>();

                    for (Account.Subklas subklas : subklasArray) {
                        strings.add(subklas.naam + ": " + subklas.vak + " van " + subklas.leraar);
                        namen.add(subklas.naam);
                    }

                    subklassenPref.setEntries(strings.toArray(new String[strings.size()]));
                    subklassenPref.setEntryValues(namen.toArray(new String[namen.size()]));
                }
            });

            // Create user and fill in account information
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            String summary;
            if(Account.getUserType() == Account.UserType.LEERLING) {
                summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
            } else {
                summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
            }
            findPreference("mijn_account").setSummary(summary);

            // Configure login button
            findPreference("log_in").setOnPreferenceClickListener(preference -> {
                Account.getInstance(this).login();

                return true;
            });

            /* account upgraden */
            findPreference("account_upgraden").setEnabled(Account.isAppAccount());
            findPreference("account_upgraden").setOnPreferenceClickListener(preference -> {
                Account.getInstance(this).extend();

                return true;
            });

            findPreference("clusterklassen_reload").setEnabled(!Account.isAppAccount());
            findPreference("clusterklassen_reload").setOnPreferenceClickListener(preference -> {
                Account.getInstance(this).setSubklassen(true, null, result -> {
                });

                return true;
            });
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$OverigFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_overig);
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$AchtergrondFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_achtergrond);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preference_headers_old);
        }
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
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    protected void onStart() {
        /*EasyTracker tracker = EasyTracker.getInstance(getApplicationContext());
        tracker.set(Fields.SCREEN_NAME, "Instellingen");
        tracker.send(MapBuilder
                .createAppView()
                .build()
        );*/
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
    public static class UserFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            String summary;
            if(Account.getUserType() == Account.UserType.LEERLING) {
                summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
            } else {
                summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
            }
            findPreference("mijn_account").setSummary(summary);

            findPreference("account_upgraden").setEnabled(Account.isAppAccount());
            findPreference("subklassen").setEnabled(!Account.isAppAccount());
            findPreference("clusterklassen_reload").setEnabled(!Account.isAppAccount());
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_user);

            // Initialize subklassen
            ListPreferenceMultiSelect subklassenPref = (ListPreferenceMultiSelect) findPreference("subklassen");

            subklassenPref.setOnPreferenceChangeListener((preference, newValue) -> {
                ArrayList<String> newSubklassen = (ArrayList<String>) newValue;
                Account.getInstance(getActivity()).setSubklassen(false, newSubklassen, result -> {
                    ExceptionHandler.handleException(new Exception("Clusterklassen bijgewerkt!"), getActivity(), ExceptionHandler.HandleType.SIMPLE);
                });

                return true;
            });

            Account.getInstance(getActivity()).getSubklassen(true, result -> {
                ArrayList<Account.Subklas> subklasArray = (ArrayList<Account.Subklas>) result;

                if (subklassenPref != null && subklasArray != null) {
                    ArrayList<String> strings = new ArrayList<>();
                    ArrayList<String> namen = new ArrayList<>();
                    ArrayList<Boolean> enabled = new ArrayList<>();

                    for (Account.Subklas subklas : subklasArray) {
                        strings.add(subklas.naam + ": " + subklas.vak + " van " + subklas.leraar);
                        namen.add(subklas.naam);
                        enabled.add(subklas.isIn);
                    }

                    boolean[] enabledNew = new boolean[enabled.size()];

                    for(int i = 0; i < enabled.size(); i++) {
                        enabledNew[i] = enabled.get(i).booleanValue();
                    }

                    subklassenPref.setEntries(strings.toArray(new String[strings.size()]), enabledNew);
                    subklassenPref.setEntryValues(namen.toArray(new String[namen.size()]));

                    subklassenPref.setEnabled(!Account.isAppAccount());
                }
            });

            // Create user and fill in account information
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            String summary;
            if(Account.getUserType() == Account.UserType.LEERLING) {
                summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
            } else {
                summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
            }
            findPreference("mijn_account").setSummary(summary);

            // Configure login button
            findPreference("log_in").setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).login();

                return true;
            });

            /* account upgraden */
            findPreference("account_upgraden").setEnabled(Account.isAppAccount());
            findPreference("account_upgraden").setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).extend();

                return true;
            });

            findPreference("clusterklassen_reload").setEnabled(!Account.isAppAccount());
            findPreference("clusterklassen_reload").setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).setSubklassen(true, null, result -> {
                    ExceptionHandler.handleException(new Exception("Clusterklassen opnieuw ingesteld!"), getActivity(), ExceptionHandler.HandleType.SIMPLE);
                });

                return true;
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
