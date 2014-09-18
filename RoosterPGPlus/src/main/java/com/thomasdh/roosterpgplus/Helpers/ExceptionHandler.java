package com.thomasdh.roosterpgplus.Helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;


public class ExceptionHandler {
    public static void handleException(Exception exception, Context context, HandleType type) {
        switch (type) {
            // Alles wordt gelogd, Analytics krijgt een crash door
            case FATAL:
                Toast.makeText(context, exception.getMessage() + ": " + Arrays.toString(exception.getStackTrace()), Toast.LENGTH_LONG).show();
                break;
            // Er wordt geen toast afgegeven, de gebruiker weet niet dat er iets misgaat
            // Bedoeld voor dingen die regelmatig msigaan, zoals het laden van een bestand waarvan je niet
            // zeker bent dat het bestaat
            case SILENT:

                break;
            // Alleen de description wordt getoast
            case SIMPLE:
                Log.e("FOUT", exception.getMessage(), exception);
                Toast.makeText(context, exception.getMessage(), Toast.LENGTH_LONG).show();
                break;
            // Zowel de description als de stacktrace wordt getoond
            case EXTENSIVE:
                Toast.makeText(context, exception.getMessage() + ": " + Arrays.toString(exception.getStackTrace()), Toast.LENGTH_LONG).show();
                break;
        }
    }

    public enum HandleType {
        FATAL, SILENT, SIMPLE, EXTENSIVE
    }

}
