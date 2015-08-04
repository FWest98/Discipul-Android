package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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


public class PreferencesActivity extends PreferenceActivity {

    private Toolbar toolbar;

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
        Account.initialize(this);

        /*ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_preferences, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        toolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(v -> finish());*/
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_preferences_toolbar, root, false);
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(getTitle());
            root.addView(toolbar, 0);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            }
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);

            root.removeAllViews();

            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_preferences_toolbar, root, false);

            int height;
            TypedValue typedValue = new TypedValue();
            if(getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
                height = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
            } else {
                height = toolbar.getHeight();
            }

            content.setPadding(0, height, 0, 0);
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(getTitle());

            root.addView(content);
            root.addView(toolbar);
        }

        toolbar.setNavigationOnClickListener((v) -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        final View result = super.onCreateView(name, context, attrs);
        if(result != null) return result;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            switch(name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
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

    public static class ThemedPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getActivity().setTheme(R.style.PreferenceTheme);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class InfoFragment extends ThemedPreferenceFragment {
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
    public static class OverigFragment extends ThemedPreferenceFragment {
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
    public static class AchtergrondFragment extends ThemedPreferenceFragment {
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
    public static class UserFragment extends ThemedPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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
