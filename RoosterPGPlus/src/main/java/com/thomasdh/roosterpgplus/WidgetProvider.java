package com.thomasdh.roosterpgplus;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Models.Lesuur;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Alle widgets bijwerken
        for(int i = 0; i < appWidgetIds.length; i++) {
            int widgetID = appWidgetIds[i];

            Rooster.getNextLesuur(context, nextLes -> createWidgetView(context, appWidgetManager, widgetID, nextLes));
        }
    }

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

        appWidgetManager.updateAppWidget(widgetID, views);
    }
}
