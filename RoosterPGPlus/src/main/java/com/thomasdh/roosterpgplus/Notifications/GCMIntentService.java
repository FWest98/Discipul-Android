package com.thomasdh.roosterpgplus.Notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.RoosterActivity;

public class GCMIntentService extends IntentService {

    public GCMIntentService() { super("GCMIntentService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if(!extras.isEmpty()) {
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE: {
                    // Handle notification
                    createNotification(extras.getString("message"), this);
                }
            }
        }
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    private static void createNotification(String message, Context context) {
        boolean pushNotificationsSetting = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pushNotificaties", true);
        if(!pushNotificationsSetting) {
            return;
        }

        String title = "Roosterwijziging";

        Intent contentIntent = new Intent(context, RoosterActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Notificatie maken */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(contentPendingIntent, true)
                .setContentIntent(contentPendingIntent)
                .setLights(Color.RED, 300, 200)
                .setVibrate(new long[] { 5000, 1000 })
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) (Math.random() * 54365323 + 100), builder.build());
    }
}
