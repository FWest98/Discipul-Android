package com.thomasdh.roosterpgplus.Notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

public class NextUurNotificationActionReceiver extends BroadcastReceiver {
    private static int notificationID = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        Lesuur nextLesuur = Rooster.getNextLesuur(context);
        String info = Rooster.getNextLesuurText(nextLesuur);
        String title = nextLesuur == null ? "Volgende les" : "Volgende les: "+nextLesuur.vak;

        /* Maak de notificatie :D */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setContentTitle(title)
            .setContentText(info)
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(info));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());

        // Set de volgende check
        new NextUurNotifications(context);
    }

    public static void disableNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        NextUurNotifications.disableNotifications(context);
    }
}
