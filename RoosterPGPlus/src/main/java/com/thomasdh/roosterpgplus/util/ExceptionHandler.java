package com.thomasdh.roosterpgplus.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

/**
 * Created by Thomas on 28-12-13.
 */
public class ExceptionHandler {

    private static void handleException(Exception exception, Context context, String description, String TAG, boolean fatal, boolean toast, boolean toastStackTrace) {

        Log.e(TAG, description, exception);

        if (toast) {
            if (toastStackTrace) {
                Toast.makeText(context, description + ": " + exception.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, description, Toast.LENGTH_LONG).show();
            }
        }

        EasyTracker easyTracker = EasyTracker.getInstance(context);
        easyTracker.send(MapBuilder
                .createException(new StandardExceptionParser(context, null)
                        .getDescription(Thread.currentThread().getName(), exception), fatal)
                .build());
    }

    public static void handleException(Exception exception, Context context, String description, String tag, HandleType type) {
        switch (type) {
            // Alles wordt gelogd, Analytics krijgt een crash door
            case FATAL:
                handleException(exception, context, description, tag, true, true, true);
                break;
            // Er wordt geen toast afgegeven, de gebruiker weet niet dat er iets misgaat
            // Bedoeld voor dingen die regelmatig msigaan, zoals het laden van een bestand waarvan je niet
            // zeker bent dat het bestaat
            case SILENT:
                handleException(exception, context, description, tag, false, false, false);
                break;
            // Alleen de description wordt getoast
            case SIMPLE:
                handleException(exception, context, description, tag, false, true, false);
                break;
            // Zowel de description als de stacktrace wordt getoond
            case EXTENSIVE:
                handleException(exception, context, description, tag, false, true, true);
                break;
        }
    }

    public enum HandleType {
        FATAL, SILENT, SIMPLE, EXTENSIVE
    }

}
