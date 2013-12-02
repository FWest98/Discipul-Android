package com.thomasdh.roosterpgplus;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by Thomas on 30-11-13.
 */
public class Notify {

    public Notify(Context context) {

        Intent intent = new Intent(context, ActionReceiver.class);
        intent.setAction("com.thomasdh.roosterpgplus");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmmanager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // alarmmanager.setRepeating(AlarmManager.RTC, 0, 1800000, pendingIntent);
        alarmmanager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 5000, pendingIntent);
    }

    public class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(getClass().getSimpleName(), "The receiver is being called");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setContentTitle("Hallo");
            builder.setContentText("Dit is een notificatie");
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.build();

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(9980, builder.build());
        }
    }

}
