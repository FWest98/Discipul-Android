package com.thomasdh.roosterpgplus;

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

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

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
        if(nextLes == null) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rooster_null);
            appWidgetManager.updateAppWidget(widgetID, views);
            return;
        }
        RemoteViews views;
        if(nextLes.verandering) {
            views = new RemoteViews(context.getPackageName(), R.layout.rooster_uur_gewijzigd_widget);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.rooster_uur_widget);
        }

        if(nextLes.verplaatsing) {
            views.setTextColor(R.id.rooster_lokaal, Color.parseColor("#FF0000"));
        } else if(nextLes.isNew) {
            views.setTextViewText(R.id.rooster_notes, "Nieuwe les");
        }

        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        views.setTextViewText(R.id.rooster_vak, nextLes.vak);
        views.setTextViewText(R.id.rooster_leraar, StringUtils.join(nextLes.leraren, " & "));
        views.setTextViewText(R.id.rooster_lokaal, nextLes.lokaal);
        views.setTextViewText(R.id.rooster_tijden, format.format(nextLes.lesStart) + " - " + format.format(nextLes.lesEind));

        Intent intent = new Intent(context, RoosterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.rooster_uur_linearlayout, pendingIntent);

        Intent refreshIntent = new Intent(context, WidgetProvider.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        PendingIntent refreshPendingIntent = PendingIntent.getActivity(context, 0, refreshIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        DateTime notificationDate = DateTime.now()
                .withWeekOfWeekyear(nextLes.week)
                .withDayOfWeek(nextLes.dag)
                .withTime(
                        nextLes.lesEind.getHours(),
                        nextLes.lesEind.getMinutes(),
                        nextLes.lesEind.getSeconds(), 0
                )
                .plusMinutes(6);
        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDate.getMillis(), pendingIntent);

        appWidgetManager.updateAppWidget(widgetID, views);
    }
}
