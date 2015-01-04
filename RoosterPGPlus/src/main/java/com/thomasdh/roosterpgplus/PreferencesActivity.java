package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.CustomUI.ListPreferenceMultiSelect;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotificationActionReceiver;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotifications;
import com.thomasdh.roosterpgplus.Settings.Constants;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("deprecation")
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) return;

        String action = getIntent().getAction();
        if ("com.thomasdh.roosterpgplus.PreferencesActivity$InfoFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_info);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_INFO);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            try {
                findPreference("versie").setTitle("Versie: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$UserFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_user);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_USER);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            if(!Account.isSet()) {
                findPreference("subklassen").setEnabled(false);
                findPreference("clusterklassen_reload").setEnabled(false);
                findPreference("account_upgraden").setEnabled(false);
                Preference logIn = findPreference("log_in");
                Preference details = findPreference("mijn_account");

                logIn.setOnPreferenceClickListener(preference -> {
                    Account.getInstance(this).login(this, callback -> {
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    });
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

                if (subklasArray != null) {
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
                Account.getInstance(this).login(this, callback -> {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                });

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

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_OVERIG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            findPreference("analytics").setOnPreferenceChangeListener((pref, newValue) -> {
                GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut((Boolean) newValue);
                return true;
            });
        } else if ("com.thomasdh.roosterpgplus.PreferencesActivity$AchtergrondFragment".equals(action)) {
            addPreferencesFromResource(R.xml.preferences_achtergrond);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_NOTIFICATIES);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            Preference mainSetting = findPreference("notificaties");
            Preference notificationFirstShow = findPreference("notificationFirstShow");

            mainSetting.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean choice = (boolean) newValue;
                if (choice) {
                    notificationFirstShow.setEnabled(true);
                    new NextUurNotifications(this, 0, true); // Setup
                } else {
                    notificationFirstShow.setEnabled(false);
                    NextUurNotificationActionReceiver.disableNotifications(this);
                }
                return true;
            });

            notificationFirstShow.setOnPreferenceChangeListener((preference, newValue) -> {
                NextUurNotificationActionReceiver.disableNotifications(this);
                new NextUurNotifications(this, Long.parseLong((String) newValue), true); // Update alles
                return true;
            });

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preference_headers_old);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_MAIN);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
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
        /*EasyTracker tracker = EasyTracker.getInstance(getApplicationContext());
        tracker.set(Fields.SCREEN_NAME, null);
        tracker.send(MapBuilder
                .createAppView()
                .build()
        );*/
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
    public boolean hasHeaders() {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_MAIN);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class InfoFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_info);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_INFO);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

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

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_OVERIG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            findPreference("analytics").setOnPreferenceChangeListener((pref, newValue) -> {
                GoogleAnalytics.getInstance(getActivity().getApplicationContext()).setAppOptOut((Boolean) newValue);
                return true;
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AchtergrondFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_achtergrond);

            Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
            tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_NOTIFICATIES);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());

            Preference mainSetting = findPreference("notificaties");
            Preference notificationFirstShow = findPreference("notificationFirstShow");
            Preference pushNotification = findPreference("pushNotificaties");

            mainSetting.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean choice = (boolean) newValue;
                if (choice) {
                    notificationFirstShow.setEnabled(true);
                    new NextUurNotifications(getActivity(), 0, true); // Setup
                } else {
                    notificationFirstShow.setEnabled(false);
                    NextUurNotificationActionReceiver.disableNotifications(getActivity());
                }
                return true;
            });

            notificationFirstShow.setOnPreferenceChangeListener((preference, newValue) -> {
                NextUurNotificationActionReceiver.disableNotifications(getActivity());
                new NextUurNotifications(getActivity(), Long.parseLong((String) newValue), true); // Update alles
                return true;
            });

            if(!HelperFunctions.checkPlayServices(getActivity())) pushNotification.setEnabled(false);
            pushNotification.setOnPreferenceClickListener(preference -> {
                if(!HelperFunctions.checkPlayServices(getActivity())) {
                    HelperFunctions.checkPlayServicesWithError(getActivity());
                    return false;
                } else {
                    return true;
                }
            });
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
}
