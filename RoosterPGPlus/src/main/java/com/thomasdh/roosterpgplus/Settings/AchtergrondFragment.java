package com.thomasdh.roosterpgplus.Settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotificationActionReceiver;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotifications;
import com.thomasdh.roosterpgplus.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AchtergrondFragment extends ThemedPreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_achtergrond);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getActivity().getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_NOTIFICATIES);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        CheckBoxPreference mainSetting = (CheckBoxPreference) findPreference("notificaties");
        ListPreference notificationFirstShow = (ListPreference) findPreference("notificationFirstShow");
        CheckBoxPreference pushNotification = (CheckBoxPreference) findPreference("pushNotificaties");

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
        pushNotification.setOnPreferenceChangeListener((preference, o) -> {
            if((boolean) o) {
                Account.getInstance(getActivity()).registerGCM(getActivity());
            }
            return true;
        });
    }
}
