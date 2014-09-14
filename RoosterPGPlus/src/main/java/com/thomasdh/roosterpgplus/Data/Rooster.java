package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.thomasdh.roosterpgplus.Database.DatabaseHelper;
import com.thomasdh.roosterpgplus.Database.DatabaseManager;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Lesuur;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fj.data.Array;

public class Rooster {
    //region Rooster

    public static void getRooster(List<NameValuePair> query, RoosterViewFragment.LoadType type, Context context, RoosterCallback callback, ExceptionCallback exceptionCallback) {
        if(HelperFunctions.hasInternetConnection(context)) {
            if (type == RoosterViewFragment.LoadType.ONLINE) {
                getRoosterFromInternet(query, false, context, callback, exceptionCallback);
            } else if (type == RoosterViewFragment.LoadType.NEWONLINE) {
                getRoosterFromInternet(query, true, context, callback, exceptionCallback);
            } else if(type == RoosterViewFragment.LoadType.REFRESH) {
                getRoosterFromInternet(query, false, context, callback, exceptionCallback);
            } else {
                getRoosterFromDatabase(query, context, callback, exceptionCallback);
            }
        } else if(type == RoosterViewFragment.LoadType.OFFLINE || type == RoosterViewFragment.LoadType.NEWONLINE) {
            getRoosterFromDatabase(query, context, callback, exceptionCallback);
        } else {
            ExceptionHandler.handleException(new Exception("Kon rooster niet laden, er is geen internetverbinding"), context, ExceptionHandler.HandleType.SIMPLE);
            exceptionCallback.onError(null);
        }
    }

    private static void getRoosterFromInternet(List<NameValuePair> query, boolean hasRoosterInDatabase, Context context, RoosterCallback callback, ExceptionCallback exceptionCallback) {
        String url = "rooster?"+ URLEncodedUtils.format(query, "utf-8");

        WebDownloader.getRooster(url, result -> parseRooster((String) result, query, context, callback), exception -> {
            Log.e("RoosterDownloader", "Er ging iets mis met het ophalen van het rooster", (Exception) exception);
            if (hasRoosterInDatabase) {
                ExceptionHandler.handleException(new Exception("Geen internetverbinding, oude versie van het rooster!"), context, ExceptionHandler.HandleType.SIMPLE);
                getRoosterFromDatabase(query, context, callback, exceptionCallback);
            } else {
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
                exceptionCallback.onError(null);
            }
        });
    }

    private static void getRoosterFromDatabase(List<NameValuePair> query, Context context, RoosterCallback callback, ExceptionCallback exceptionCallback) {
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
            exceptionCallback.onError(e);

        } catch (Exception e) {
            Log.e("Callback error", e.getMessage(), e);
            exceptionCallback.onError(e);
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

    //endregion
    //region Next uur

    public static String getNextLesuurText(Lesuur nextLesuur) {
        if(nextLesuur == null) {
            return "Geen les gevonden";
        } else {
            DateFormat format = new SimpleDateFormat("HH:mm");
            return String.format("De volgende les is %1$s, %2$s het %3$se uur en start om %4$s",
                    nextLesuur.vak,
                    getDayOfWeek(nextLesuur.dag + 1),
                    nextLesuur.uur,
                    format.format(nextLesuur.lesStart));
        }
    }

    public static Lesuur getNextLesuur(Context context) {
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int weekToGet = RoosterInfo.getCurrentWeek(context);
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        if(currentWeek != weekToGet) {
            // Het is weekend, dus maandag is de dag en een correctie voor de DB
            currentDay = Calendar.MONDAY;
        } else {
            // Het is geen weekend, we kijken of er vandaag nog een les komt (zo nee, op naar morgen!)
            Lesuur nextLes = getNextLesuurInDay(context, weekToGet, currentDay, new DateTime(now));
            if(nextLes == null) { // geen lessen meer
                currentDay++;
            } else {
                // We weten de volgende les zowaar... DONE
                return nextLes;
            }
        }

        Calendar timeToGet = now;
        timeToGet.set(Calendar.HOUR, 0);
        timeToGet.set(Calendar.MINUTE, 0);
        timeToGet.set(Calendar.SECOND, 0);

        return getNextLesuurInDay(context, weekToGet, currentDay, new DateTime(timeToGet));
    }

    private static Lesuur getNextLesuurInDay(Context context, int week, int day, DateTime time) {
        Account.initialize(context);
        Lesuur baseLesuur = new Lesuur();
        baseLesuur.uur = 10000; // Vast niet meer uren op een dag...
        DateTimeComparator comparator = DateTimeComparator.getTimeOnlyInstance();

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(week)));
        query.add(new BasicNameValuePair("key", Account.getApiKey()));

        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDaoWithCache(Lesuur.class);

            String searchQuery = URLEncodedUtils.format(query, "utf-8");
            Array<Lesuur> lessenThisWeek = Array.iterableArray(dao.queryForEq("query", searchQuery));
            Array<Lesuur> lessenThisDay = lessenThisWeek.filter(s -> s.dag == day - 1 && !s.vervallen); // DBcorrectie
            Array<Lesuur> futureLessen = lessenThisDay.filter(s -> comparator.compare(new DateTime(s.lesStart), time) >= 0);
            if(futureLessen.length() == 0) {
                return null;
            }
            Lesuur nextLes = futureLessen.foldLeft((newLes, oldLes) -> newLes.uur < oldLes.uur ? newLes : oldLes, baseLesuur);
            return nextLes;
        } catch (SQLException e) {
            return null;
        }
    }

    //endregion


    public interface RoosterCallback {
        void onCallback(Object data, int urenCount);
    }
    public interface ExceptionCallback {
        void onError(Exception e);
    }
    private static String getDayOfWeek(int dag) {
        switch (dag) {
            case Calendar.SUNDAY:
                return "zondag";
            case Calendar.MONDAY:
                return "maandag";
            case Calendar.TUESDAY:
                return "dinsdag";
            case Calendar.WEDNESDAY:
                return "woensdag";
            case Calendar.THURSDAY:
                return "donderdag";
            case Calendar.FRIDAY:
                return "vrijdag";
            case Calendar.SATURDAY:
                return "zaterdag";

        }
        return null;
    }
}
