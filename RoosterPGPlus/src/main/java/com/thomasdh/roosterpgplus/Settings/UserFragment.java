package com.thomasdh.roosterpgplus.Settings;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

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
    private PreferenceScreen preferenceScreen;
    private Preference myAccountPreference;
    private Preference logInPreference;
    private Preference extendPreference;
    private ListPreferenceMultiSelect setClusterklassenPreference;
    private Preference resetClusterklassenPreference;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String summary;
        if(Account.getUserType() == Account.UserType.LEERLING) {
            summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
        } else {
            summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
        }
        myAccountPreference.setSummary(summary);

        extendPreference.setEnabled(Account.isAppAccount());
        setClusterklassenPreference.setEnabled(!Account.isAppAccount());
        resetClusterklassenPreference.setEnabled(!Account.isAppAccount());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_user);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_USER);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        preferenceScreen = getPreferenceScreen();
        myAccountPreference = findPreference("mijn_account");
        logInPreference = findPreference("log_in");
        extendPreference = findPreference("account_upgraden");
        setClusterklassenPreference = (ListPreferenceMultiSelect) findPreference("subklassen");
        resetClusterklassenPreference = findPreference("clusterklassen_reload");

        if(!Account.isSet()) {
            preferenceScreen.removePreference(extendPreference);
            preferenceScreen.removePreference(setClusterklassenPreference);
            preferenceScreen.removePreference(resetClusterklassenPreference);

            logInPreference.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).login(getActivity(), callback -> getActivity().recreate());
                return true;
            });
            logInPreference.setTitle("Log in");
            logInPreference.setSummary("Log in om je persoonlijke rooster te tonen");

            myAccountPreference.setTitle("Je bent nog niet ingelogd");
            myAccountPreference.setSummary(null);
            return;
        }

        // Initialize subklassen
        if(Account.getUserType() == Account.UserType.LEERLING) {
            setClusterklassenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
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

                    for (int i = 0; i < enabled.size(); i++) {
                        enabledNew[i] = enabled.get(i);
                    }

                    setClusterklassenPreference.setEntries(strings.toArray(new String[strings.size()]), enabledNew);
                    setClusterklassenPreference.setEntryValues(namen.toArray(new String[namen.size()]));
                }
            });

            setClusterklassenPreference.setEnabled(!Account.isAppAccount());
            resetClusterklassenPreference.setEnabled(!Account.isAppAccount());

            resetClusterklassenPreference.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).setSubklassen(true, null, result -> ExceptionHandler.handleException(new Exception("Clusterklassen opnieuw ingesteld!"), getActivity(), ExceptionHandler.HandleType.SIMPLE));

                return true;
            });
        } else {
            preferenceScreen.removePreference(setClusterklassenPreference);
            preferenceScreen.removePreference(resetClusterklassenPreference);
        }

        // Create user and fill in account information
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        String summary;
        if(Account.getUserType() == Account.UserType.LEERLING) {
            summary = "Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas();
        } else {
            summary = "Naam: " + Account.getName() + ", " + Account.getLeraarCode();
        }
        myAccountPreference.setSummary(summary);

        // Configure login button
        logInPreference.setOnPreferenceClickListener(preference -> {
            Account.getInstance(getActivity()).login(getActivity(), callback -> getActivity().recreate());

            return true;
        });

        /* account upgraden */
        extendPreference.setEnabled(Account.isAppAccount());
        extendPreference.setOnPreferenceClickListener(preference -> {
            Account.getInstance(getActivity()).extend();

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
