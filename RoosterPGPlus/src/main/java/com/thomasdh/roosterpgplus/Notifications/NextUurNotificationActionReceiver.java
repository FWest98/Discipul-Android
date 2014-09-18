package com.thomasdh.roosterpgplus.Notifications;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.joda.time.DateTime;

public class NextUurNotificationActionReceiver extends BroadcastReceiver {
    private static int notificationID = 0;
    private static final String NOTIFICATION = "com.thomasdh.roosterpgplus.Notifications.NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == NOTIFICATION) {
            new NextUurNotifications(context);
        } else {
            Rooster.getNextLesuur(context, lesuur -> createNotification(context, lesuur));
        }
    }

    @SuppressWarnings("deprecation")
    private void createNotification(Context context, Lesuur nextLesuur) {
        if(nextLesuur == null) return;
        String info = Rooster.getNextLesuurText(nextLesuur);
        String title = "Volgende les: " + nextLesuur.vak;

        /* Maak de notificatie :D */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setContentTitle(title)
            .setContentText(info)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(info));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());

        // Set de volgende check
        DateTime notificationDate = DateTime.now()
                .withWeekOfWeekyear(nextLesuur.week)
                .withDayOfWeek(nextLesuur.dag)
                .withTime(
                        nextLesuur.lesStart.getHours(),
                        nextLesuur.lesStart.getMinutes(),
                        nextLesuur.lesStart.getSeconds(), 0
                )
                .plusMinutes(6);

        Intent intent = new Intent(context, NextUurNotificationActionReceiver.class);
        intent.setAction(NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC_WAKEUP, notificationDate.toDate().getTime(), pendingIntent);

        NotificationCompat.Builder extraBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("Extra")
                .setContentText(notificationDate.toString())
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(1, extraBuilder.build());
    }

    public static void disableNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        NextUurNotifications.disableNotifications(context);
    }
}
