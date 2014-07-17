package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.util.Log;

import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

public class Rooster {
    public static void getRooster(String query, RoosterViewFragment.LoadType type, Context context, AsyncActionCallback callback) {
        if(type == RoosterViewFragment.LoadType.ONLINE) {
            getRoosterFromInternet(query, false, context, callback);
        } else if(type == RoosterViewFragment.LoadType.NEWONLINE) {
            getRoosterFromInternet(query, true, context, callback);
        } else {
            getRoosterFromDatabase(query, context, callback);
        }
    }

    private static void getRoosterFromInternet(String query, boolean isOffline, Context context, AsyncActionCallback callback) {
        String url = "rooster"+query;

        RoosterInfoDownloader.getRooster(url, result -> {
            callback.onAsyncActionComplete(result);
        }, exception -> {
            Log.e("RoosterDownloader", "Er ging iets mis met het ophalen van het rooster", (Exception) exception);
            if(isOffline) {
                ExceptionHandler.handleException(new Exception("Oude versie van het rooster: " + ((Exception) exception).getMessage()), context, ExceptionHandler.HandleType.SIMPLE);
                getRoosterFromDatabase(query, context, callback);
            } else {
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });
    }

    private static void getRoosterFromDatabase(String query, Context context, AsyncActionCallback callback) {

    }

    private static void saveRoosterInDatabase() {

    }
}
