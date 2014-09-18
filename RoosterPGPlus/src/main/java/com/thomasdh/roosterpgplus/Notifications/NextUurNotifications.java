package com.thomasdh.roosterpgplus.Notifications;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.R;

import org.joda.time.DateTime;

public class NextUurNotifications {
    private static PendingIntent currentIntent;

    public NextUurNotifications(Context context) {
        this(context, 0, false);
    }
    @SuppressWarnings("deprecation")
    public NextUurNotifications(Context context, long delay, boolean force) {
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notificaties", true) && !force) return; // Notificaties uit
        Account.initialize(context);
        if(Account.getApiKey() == null) return; // Geen meldingen te tonen

        Intent intent = new Intent(context, NextUurNotificationActionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* First, get the next hour */
        Rooster.getNextLesuur(context, nextLes -> {
            if (nextLes == null) return;

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
            if(notificationTime >= System.currentTimeMillis()) {
                // Remove current notifications
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);

            currentIntent = pendingIntent;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setContentTitle("Nieuw")
                    .setContentText(new DateTime(notificationTime).toString())
                    .setSmallIcon(R.drawable.ic_notification)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(2, builder.build());
        });
    }

    public static void disableNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(currentIntent);
    }
}
