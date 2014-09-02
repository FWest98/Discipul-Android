package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.thomasdh.roosterpgplus.Database.DatabaseHelper;
import com.thomasdh.roosterpgplus.Database.DatabaseManager;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Rooster {
    public static void getRooster(List<NameValuePair> query, RoosterViewFragment.LoadType type, Context context, RoosterCallback callback, AsyncActionCallback errorCallback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            if (type == RoosterViewFragment.LoadType.ONLINE) {
                getRoosterFromInternet(query, false, context, callback, errorCallback);
            } else if (type == RoosterViewFragment.LoadType.NEWONLINE) {
                getRoosterFromInternet(query, true, context, callback, errorCallback);
            } else {
                getRoosterFromDatabase(query, context, callback, errorCallback);
            }
        } else if(type == RoosterViewFragment.LoadType.OFFLINE || type == RoosterViewFragment.LoadType.NEWONLINE) {
            getRoosterFromDatabase(query, context, callback, errorCallback);
        } else {
            ExceptionHandler.handleException(new Exception("Kon rooster niet laden, er is geen internetverbinding"), context, ExceptionHandler.HandleType.SIMPLE);
        }
    }

    private static void getRoosterFromInternet(List<NameValuePair> query, boolean isOffline, Context context, RoosterCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster?"+ URLEncodedUtils.format(query, "utf-8");

        WebDownloader.getRooster(url, result -> parseRooster((String) result, query, context, callback), exception -> {
            Log.e("RoosterDownloader", "Er ging iets mis met het ophalen van het rooster", (Exception) exception);
            if (isOffline) {
                ExceptionHandler.handleException(new Exception("Geen internetverbinding, oude versie van het rooster!"), context, ExceptionHandler.HandleType.SIMPLE);
                getRoosterFromDatabase(query, context, callback, errorCallback);
            } else {
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                try {
                    errorCallback.onAsyncActionComplete(null);
                } catch (Exception ignored) {}
            }
        });
    }

    private static void getRoosterFromDatabase(List<NameValuePair> query, Context context, RoosterCallback callback, AsyncActionCallback errorCallback) {
        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDaoWithCache(Lesuur.class);

            String searchQuery = URLEncodedUtils.format(query, "utf-8");
            List<Lesuur> lessen = dao.queryForEq("query", searchQuery);
            int week = !lessen.isEmpty() ? lessen.get(0).week : 0;

            callback.onCallback(lessen, RoosterInfo.getWeekUrenCount(context, week));
        } catch (SQLException e) {
            Log.e("SQL ERROR", e.getMessage(), e);
            ExceptionHandler.handleException(new Exception("Opslagfout, er is geen rooster geladen"), context, ExceptionHandler.HandleType.SIMPLE);
            try {
                errorCallback.onAsyncActionComplete(null);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Log.e("Callback error", e.getMessage(), e);
            try {
                errorCallback.onAsyncActionComplete(null);
            } catch (Exception ignored) {}
        }
    }

    private static void saveRoosterInDatabase(List<Lesuur> lessen, String query, Context context) {
        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDao(Lesuur.class);

            // Huidige lessen verwijderen
            DeleteBuilder<Lesuur, ?> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq("query", query);
            deleteBuilder.delete();

            // Nieuwe lessen opslaan
            for(Lesuur les : lessen) {
                dao.create(les);
            }

            // SetLoad triggeren

        } catch (SQLException e) {
            Log.e("SQL ERROR", e.getMessage(), e);
        }
    }

    private static void parseRooster(String JSON, List<NameValuePair> query, Context context, RoosterCallback callback) {
        try {
            String queryString = URLEncodedUtils.format(query, "utf-8");
            List<Lesuur> lessen = new ArrayList<>();
            int week = 0;

            JSONObject rooster = new JSONObject(JSON);

            JSONArray JSONlessen = rooster.getJSONArray("lessen");
            for(int i = 0; i < JSONlessen.length(); i++) {
                JSONObject JSONLes = JSONlessen.getJSONObject(i);
                Lesuur les = new Lesuur(JSONLes, context, queryString);
                lessen.add(les);
                week = les.week;
            }

            int urenCount = rooster.getInt("urenCount");

            RoosterInfo.setWeekUrenCount(context, week, urenCount);
            callback.onCallback(lessen, urenCount);
            saveRoosterInDatabase(lessen, queryString, context);
        } catch (JSONException e) {
            Log.e("JSON ERROR", e.getMessage(), e);
            ExceptionHandler.handleException(new Exception("Fout bij het verwerken van het rooster"), context, ExceptionHandler.HandleType.SIMPLE);
        } catch (Exception e) {
            Log.e("Callback error", e.getMessage(), e);
        }
    }

    public interface RoosterCallback {
        void onCallback(Object data, int urenCount);
    }
}
