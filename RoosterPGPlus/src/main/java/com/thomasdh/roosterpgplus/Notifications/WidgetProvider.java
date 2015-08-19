package com.thomasdh.roosterpgplus.Notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.RoosterActivity;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Alle widgets bijwerken
        for (int widgetID : appWidgetIds) {
            Rooster.getNextLesuur(context, nextLes -> createWidgetView(context, appWidgetManager, widgetID, nextLes));
        }
    }

    @SuppressWarnings("deprecation")
    private void createWidgetView(Context context, AppWidgetManager appWidgetManager, int widgetID, Lesuur nextLes) {
        RemoteViews views;
        DateTime notificationDate;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(nextLes == null) {
            views = new RemoteViews(context.getPackageName(), R.layout.rooster_null);

            /* Try again over een een dag */
            notificationDate = DateTime.now().plusDays(1);
        } else {
            if (nextLes.verandering) {
                views = new RemoteViews(context.getPackageName(), R.layout.rooster_uur_gewijzigd_widget);
            } else {
                views = new RemoteViews(context.getPackageName(), R.layout.rooster_uur_widget);
            }

            if (nextLes.verplaatsing || nextLes.verandering) {
                views.setTextColor(R.id.rooster_lokaal, Color.parseColor("#FF0000"));
            } else {
                views.setTextColor(R.id.rooster_lokaal, Color.parseColor("#000000"));
            }
            if (nextLes.isNew) {
                views.setTextViewText(R.id.rooster_notes, "Nieuwe les");
            } else {
                views.setTextViewText(R.id.rooster_notes, "");
            }

            SimpleDateFormat format = new SimpleDateFormat("HH:mm");

            views.setTextViewText(R.id.rooster_vak, nextLes.vak);
            views.setTextViewText(R.id.rooster_leraar, StringUtils.join(nextLes.leraren, " & "));
            views.setTextViewText(R.id.rooster_lokaal, nextLes.lokaal);
            views.setTextViewText(R.id.rooster_tijden, format.format(nextLes.lesStart) + " - " + format.format(nextLes.lesEind));

            int year = nextLes.week < Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) ? /* nieuw jaar */ DateTime.now().getYear() + 1 : DateTime.now().getYear();
            notificationDate = DateTime.now()
                    .withYear(year)
                    .withWeekOfWeekyear(nextLes.week)
                    .withDayOfWeek(nextLes.dag)
                    .withTime(
                            nextLes.lesEind.getHours(),
                            nextLes.lesEind.getMinutes(),
                            nextLes.lesEind.getSeconds(), 0
                    )
                    .plusMinutes(6);
        }

        Intent intent = new Intent(context, RoosterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.rooster_uur_linearlayout, pendingIntent);

        Intent refreshIntent = new Intent(context, WidgetProvider.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetID});

        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent);

        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDate.getMillis(), pendingIntent);

        appWidgetManager.updateAppWidget(widgetID, views);
    }
}
