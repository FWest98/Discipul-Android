package com.thomasdh.roosterpgplus.Settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
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
public class UserFragment extends ThemedPreferenceFragment {
    private PreferenceScreen preferenceScreen;

    /* UserPrefs */
    private PreferenceCategory userCategory;
    private Preference myAccountPreference;
    private Preference logInPreference;
    private Preference extendPreference;
    private Preference changeUsernamePreference;
    private Preference changePasswordPreference;

    /* LLPrefs */
    private PreferenceCategory leerlingCategory;
    private Preference aboutLLPreference;
    private ListPreferenceMultiSelect setClusterklassenPreference;
    private Preference resetClusterklassenPreference;

    /* LeraarPrefs */
    private PreferenceCategory leraarCategory;
    private Preference aboutLerPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_user);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_USER);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        /* Get prefs */
        preferenceScreen = getPreferenceScreen();

        // User
        userCategory = (PreferenceCategory) findPreference("account_category");
        myAccountPreference = findPreference("about_user");
        logInPreference = findPreference("log_in");
        extendPreference = findPreference("account_upgraden");
        changeUsernamePreference = findPreference("change_username");
        changePasswordPreference = findPreference("change_password");

        // Leerling
        leerlingCategory = (PreferenceCategory) findPreference("leerling_category");
        aboutLLPreference = findPreference("about_leerling");
        setClusterklassenPreference = (ListPreferenceMultiSelect) findPreference("clusterklassen");
        resetClusterklassenPreference = findPreference("clusterklassen_reload");

        // Leraar
        leraarCategory = (PreferenceCategory) findPreference("leraar_category");
        aboutLerPreference = findPreference("about_leraar");

        if(!Account.isSet()) {
            /* Remove prefs */
            userCategory.removePreference(extendPreference);
            userCategory.removePreference(changePasswordPreference);
            userCategory.removePreference(changeUsernamePreference);

            preferenceScreen.removePreference(leerlingCategory);
            preferenceScreen.removePreference(leraarCategory);

            /* Login listener */
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

        // Leerling
        if(Account.getUserType() == Account.UserType.LEERLING) {
            // Set up stuff
            preferenceScreen.removePreference(leraarCategory);
            aboutLLPreference.setSummary("Naam: " + Account.getName() + ", " + "Klas: " + Account.getLeerlingKlas());

            // Clusterklassen set
            setClusterklassenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                ArrayList<String> newSubklassen = (ArrayList<String>) newValue;
                Account.getInstance(getActivity()).setClusterklassen(false, newSubklassen, result -> ExceptionHandler.handleException(new Exception("Clusterklassen bijgewerkt!"), getActivity(), ExceptionHandler.HandleType.SIMPLE));

                return true;
            });

            // Clusterklassen get
            Account.getInstance(getActivity()).getClusterklassen(true, result -> {
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
                Account.getInstance(getActivity()).setClusterklassen(true, null, result -> ExceptionHandler.handleException(new Exception("Clusterklassen opnieuw ingesteld!"), getActivity(), ExceptionHandler.HandleType.SIMPLE));

                return true;
            });
        } else {
            // Leraar
            preferenceScreen.removePreference(leerlingCategory);
            aboutLerPreference.setSummary("Naam: " + Account.getName() + ", " + Account.getLeraarCode());
        }

        /* Account section */
        myAccountPreference.setSummary("Gebruikersnaam: " + Account.getUsername());

        // Configure login button
        logInPreference.setOnPreferenceClickListener(preference -> {
            Account.getInstance(getActivity()).login(getActivity(), callback -> getActivity().recreate());

            return true;
        });

        if(!Account.isAppAccount()) {
            /* Userpass change */
            userCategory.removePreference(extendPreference);

            changeUsernamePreference.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).changeUsername(callback -> myAccountPreference.setSummary("Gebruikersnaam: " + Account.getUsername()));

                return true;
            });

            changePasswordPreference.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).changePassword();

                return true;
            });
        } else {
            /* Account upgraden */
            userCategory.removePreference(changePasswordPreference);
            userCategory.removePreference(changeUsernamePreference);

            extendPreference.setOnPreferenceClickListener(preference -> {
                Account.getInstance(getActivity()).extend(callback -> getActivity().recreate());

                return true;
            });
        }
    }
}
