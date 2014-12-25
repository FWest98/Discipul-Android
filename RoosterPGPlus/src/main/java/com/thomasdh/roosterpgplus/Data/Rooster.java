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
                getRoosterFromInternet(query, false, context, callback, exceptionCallback, false);
            } else if (type == RoosterViewFragment.LoadType.NEWONLINE) {
                getRoosterFromInternet(query, true, context, callback, exceptionCallback, false);
            } else if(type == RoosterViewFragment.LoadType.REFRESH) {
                getRoosterFromInternet(query, false, context, callback, exceptionCallback, false);
            } else {
                getRoosterFromDatabase(query, context, callback, exceptionCallback, false);
            }
        } else if(type == RoosterViewFragment.LoadType.OFFLINE || type == RoosterViewFragment.LoadType.NEWONLINE) {
            getRoosterFromDatabase(query, context, callback, exceptionCallback, false);
        } else {
            ExceptionHandler.handleException(new Exception("Kon rooster niet laden, er is geen internetverbinding"), context, ExceptionHandler.HandleType.SIMPLE);
            exceptionCallback.onError(null);
        }
    }

    private static void getRoosterFromInternet(List<NameValuePair> query, boolean hasRoosterInDatabase, Context context, RoosterCallback callback, ExceptionCallback exceptionCallback, boolean silent) {
        String url = "rooster?"+ URLEncodedUtils.format(query, "utf-8");

        WebDownloader.getRooster(url, result -> parseRooster((String) result, query, context, callback, silent), exception -> {
            Log.w("RoosterDownloader", "Er ging iets mis met het ophalen van het rooster", (Exception) exception);
            if (hasRoosterInDatabase) {
                if(!silent) ExceptionHandler.handleException(new Exception("Kon het rooster niet ophalen, oude rooster geladen. (" + ((Exception) exception).getMessage() + ")", (Exception) exception), context, ExceptionHandler.HandleType.SIMPLE);
                getRoosterFromDatabase(query, context, callback, exceptionCallback, silent);
            } else {
                if(!silent) ExceptionHandler.handleException(new Exception("Kon het rooster niet ophalen, geen rooster geladen. (" + ((Exception) exception).getMessage() + ")", (Exception) exception), context, ExceptionHandler.HandleType.SIMPLE);
                exceptionCallback.onError(null);
            }
        });
    }

    private static void getRoosterFromDatabase(List<NameValuePair> query, Context context, RoosterCallback callback, ExceptionCallback exceptionCallback, boolean silent) {
        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDaoWithCache(Lesuur.class);

            String searchQuery = URLEncodedUtils.format(query, "utf-8");
            List<Lesuur> lessen = dao.queryForEq("query", searchQuery);
            int week = !lessen.isEmpty() ? lessen.get(0).week : 0;

            callback.onCallback(lessen, RoosterInfo.getWeekUrenCount(context, week));
        } catch (SQLException e) {
            Log.e("SQL ERROR", e.getMessage(), e);
            if(!silent) ExceptionHandler.handleException(new Exception("Opslagfout, er is geen rooster geladen"), context, ExceptionHandler.HandleType.SIMPLE);
            exceptionCallback.onError(e);

        } catch (Exception e) {
            Log.e("Callback error", e.getMessage(), e);
            exceptionCallback.onError(e);
        }
    }

    private static void saveRoosterInDatabase(List<Lesuur> lessen, String query, Context context) {
        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDaoWithCache(Lesuur.class);

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

    private static void parseRooster(String JSON, List<NameValuePair> query, Context context, RoosterCallback callback, boolean silent) {
        try {
            String queryString = URLEncodedUtils.format(query, "utf-8");
            List<Lesuur> lessen = new ArrayList<>();
            int week = 0;

            JSONObject rooster = new JSONObject(JSON);

            JSONArray JSONlessen = rooster.getJSONArray("lessen");

            if(JSONlessen.length() == 0) {
                // Er moet iets fout zijn.... dus uit geheugen halen....
                getRoosterFromDatabase(query, context, (data, urenCount) -> {
                    if(urenCount == 0 && !silent) ExceptionHandler.handleException(new Exception("Rooster wordt opnieuw opgehaald... Probeer het later opnieuw."), context, ExceptionHandler.HandleType.SIMPLE);
                    else ExceptionHandler.handleException(new Exception("Rooster wordt opnieuw opgehaald. Oud rooster wordt getoond"), context, ExceptionHandler.HandleType.SIMPLE);
                    callback.onCallback(data, urenCount);
                }, e -> {
                    if(!silent) ExceptionHandler.handleException(new Exception("Rooster wordt opnieuw opgehaald... Probeer het later opnieuw"), context, ExceptionHandler.HandleType.SIMPLE);
                }, silent);
                return;
            }

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
            if(!silent) ExceptionHandler.handleException(new Exception("Fout bij het verwerken van het rooster", e), context, ExceptionHandler.HandleType.SIMPLE);
        } catch (Exception e) {
            Log.e("Callback error", e.getMessage(), e);
            if(!silent) ExceptionHandler.handleException(new Exception("Fout bij het verwerken van het rooster: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
        }
    }

    //endregion
    //region Next uur

    public static String getNextLesuurText(Lesuur nextLesuur) {
        if(nextLesuur == null) {
            return "Geen les gevonden";
        } else {
            DateFormat format = new SimpleDateFormat("HH:mm");
            return String.format("De volgende les is %1$s, %2$s het %3$se uur in %4$s en start om %5$s",
                    nextLesuur.vak,
                    getDayOfWeek(nextLesuur.dag + 1),
                    nextLesuur.uur,
                    nextLesuur.lokaal,
                    format.format(nextLesuur.lesStart));
        }
    }

    public static void getNextLesuur(Context context, NextUurCallback callback) {
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int weekToGet = RoosterInfo.getCurrentWeek(context);
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        int newDay;
        if(currentWeek != weekToGet) {
            // Het is een veranderde week (vakantie / weekend), dus maandag is de dag en een correctie voor de DB
            newDay = Calendar.MONDAY;

            DateTime timeToGet = DateTime.now()
                    .withTime(0, 0, 0, 0);

            getNextLesuurInDay(context, weekToGet, newDay, timeToGet, callback);
        } else {
            // Het is geen weekend, we kijken of er vandaag nog een les komt (zo nee, op naar morgen!)
            getNextLesuurInDay(context, weekToGet, currentDay, new DateTime(now), lesuur -> {

                if (lesuur == null) { // geen lessen meer
                    int updatedDay = currentDay + 1;
                    int updatedWeek = currentWeek;
                    if (updatedDay == Calendar.SATURDAY) {
                        updatedDay = Calendar.MONDAY;
                        updatedWeek++;
                    }

                    DateTime timeToGet = DateTime.now()
                            .withTime(0, 0, 0, 0);

                    getNextLesuurInDay(context, updatedWeek, updatedDay, timeToGet, callback);
                } else {
                    // We weten de volgende les zowaar... DONE
                    callback.onCallback(lesuur);
                }
            });
        }
    }

    private static void getNextLesuurInDay(Context context, int week, int day, DateTime time, NextUurCallback callback) {
        Account.initialize(context, false);

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(week)));
        query.add(new BasicNameValuePair("key", Account.getApiKey()));

        if (HelperFunctions.hasInternetConnection(context)) {
            // Herladen van het rooster FTW
            getRoosterFromInternet(query, true, context, (data, urenCount) -> callback.onCallback(getNextLesuurInDayCallback(day, time, Array.iterableArray((ArrayList<Lesuur>) data))), e -> callback.onCallback(getNextLesuurInDayCallback(context, day, time, query)), true);
        } else {
            callback.onCallback(getNextLesuurInDayCallback(context, day, time, query));
        }
    }
    private static Lesuur getNextLesuurInDayCallback(Context context, int day, DateTime time, List<NameValuePair> query) {
        DatabaseHelper helper = DatabaseManager.getHelper(context);
        try {
            Dao<Lesuur, ?> dao = helper.getDaoWithCache(Lesuur.class);

            String searchQuery = URLEncodedUtils.format(query, "utf-8");
            Array<Lesuur> lessenThisWeek = Array.iterableArray(dao.queryForEq("query", searchQuery));
            return getNextLesuurInDayCallback(day, time, lessenThisWeek);
        } catch (SQLException e) {
            return null;
        }
    }

    private static Lesuur getNextLesuurInDayCallback(int day, DateTime time, Array<Lesuur> lessenThisWeek) {
        Lesuur baseLesuur = new Lesuur();
        baseLesuur.uur = 10000; // Vast niet meer uren op een dag...
        DateTimeComparator comparator = DateTimeComparator.getTimeOnlyInstance();

        Array<Lesuur> lessenThisDay = lessenThisWeek.filter(s -> s.dag == day - 1 && !s.vervallen); // DBcorrectie
        Array<Lesuur> futureLessen = lessenThisDay.filter(s -> comparator.compare(new DateTime(s.lesStart).plusMinutes(5), time) >= 0);
        if(futureLessen.isEmpty()) {
            return null;
        }
        return futureLessen.foldLeft((newLes, oldLes) -> newLes.uur < oldLes.uur ? newLes : oldLes, baseLesuur);
    }

    //endregion


    public interface RoosterCallback {
        void onCallback(Object data, int urenCount);
    }
    public interface ExceptionCallback {
        void onError(Exception e);
    }
    public interface NextUurCallback {
        void onCallback(Lesuur lesuur);
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
