package com.thomasdh.roosterpgplus.Settings;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.CustomUI.ListPreferenceMultiSelect;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.R;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class UserFragment extends ThemedPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_USER);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        if(!Account.isSet()) {
            findPreference("subklassen").setEnabled(false);
            findPreference("clusterklassen_reload").setEnabled(false);
            findPreference("account_upgraden").setEnabled(false);
            Preference logIn = findPreference("log_in");
            Preference details = findPreference("mijn_account");

            logIn.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).login(getActivity(), callback -> getActivity().recreate());
                return true;
            });
            logIn.setTitle("Log in");
            logIn.setSummary("Je bent nog niet ingelogd");

            details.setTitle("Je bent nog niet ingelogd");
            details.setSummary(null);
            return;
        }

        // Initialize subklassen
        ListPreferenceMultiSelect subklassenPref = (ListPreferenceMultiSelect) findPreference("subklassen");

        subklassenPref.setOnPreferenceChangeListener((preference, newValue) -> {
            ArrayList<String> newSubklassen = (ArrayList<String>) newValue;
            Account.getInstance(getActivity()).setSubklassen(false, newSubklassen, result -> ExceptionHandler.handleException(new Exception("Clusterklassen bijgewerkt!"), getActivity(), ExceptionHandler.HandleType.SIMPLE));

            return true;
        });

        Account.getInstance(getActivity()).getSubklassen(true, result -> {
            ArrayList<Account.Subklas> subklasArray = (ArrayList<Account.Subklas>) result;

            if (subklasArray != null) {
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
                    enabledNew[i] = enabled.get(i);
                }

                subklassenPref.setEntries(strings.toArray(new String[strings.size()]), enabledNew);
                subklassenPref.setEntryValues(namen.toArray(new String[namen.size()]));

                subklassenPref.setEnabled(!Account.isAppAccount());
            }
        });

        subklassenPref.setEnabled(!Account.isAppAccount());

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
            Account.getInstance(getActivity()).login(getActivity(), callback -> getActivity().recreate());

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
            Account.getInstance(getActivity()).setSubklassen(true, null, result -> ExceptionHandler.handleException(new Exception("Clusterklassen opnieuw ingesteld!"), getActivity(), ExceptionHandler.HandleType.SIMPLE));

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
