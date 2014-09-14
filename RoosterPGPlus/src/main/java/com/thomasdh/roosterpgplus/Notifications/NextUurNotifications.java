package com.thomasdh.roosterpgplus.Notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Models.Lesuur;

import org.joda.time.DateTime;

public class NextUurNotifications {
    private static PendingIntent currentIntent;

    public NextUurNotifications(Context context) {
        this(context, 0, false);
    }
    public NextUurNotifications(Context context, long delay, boolean force) {
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notificaties", true) && !force) return; // Notificaties uit
        Account.initialize(context);
        if(Account.getApiKey() == null) return; // Geen meldingen te tonen

        Intent intent = new Intent(context, NextUurNotificationActionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        /* First, get the next hour */
        Lesuur nextLes = Rooster.getNextLesuur(context);

        /* Dan de notificatietijd bepalen */
        long notificationDelay = delay == 0 ? Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString("notificationFirstShow", "3600000")) : delay;
        DateTime notificationDate = DateTime.now()
                .withWeekOfWeekyear(nextLes.week)
                .withDayOfWeek(nextLes.dag)
                .withTime(
                nextLes.lesStart.getHours(),
                nextLes.lesStart.getMinutes(),
                nextLes.lesStart.getSeconds(),
                0
        );
        long notificationTime = notificationDate.toDate().getTime() - notificationDelay;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);

        currentIntent = pendingIntent;
    }

    public static void disableNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(currentIntent);
    }
}
