package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.util.Log;

import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Week;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import fj.data.Array;

/**
 * Ophalen van roosterinfo uit opslag of van internet
 */
public class RoosterInfo {
    private static final String WEKEN_FILENAME = "wekenarray";
    private static final String KLASSEN_FILENAME = "klassenarray";
    private static final String LERAREN_FILENAME = "lerarenarray";
    private static final String LOKALEN_FILENAME = "lokalenarray";
    private static final String LEERLINGEN_FILENAME = "leerlingenarray";
    private static final String LOADS_FILENAME = "loadshashtable";
    private static final String WEKEN_UREN_FILENAME = "wekenhashtable";

    private static final String WEKEN_LOADS = "wekenLoads";
    private static final String KLASSEN_LOADS = "klassenLoads";
    private static final String LERAREN_LOADS = "lerarenLoads";
    private static final String LOKALEN_LOADS = "lokalenLoads";
    private static final String LEERLINGEN_LOADS = "leerlingenLoads";

    private static final Long WEKEN_MAX_AGE = (long) 86400000; // 24 uur
    private static final Long KLASSEN_MAX_AGE = (long) 86400000; // 24 uur
    private static final Long LERAREN_MAX_AGE = (long) 86400000;
    private static final Long LOKALEN_MAX_AGE = (long) 86400000;
    private static final Long LEERLINGEN_MAX_AGE = (long) 86400000;

    //region Weken

    public static void getWeken(Context context, AsyncActionCallback callback) {
        ArrayList<Week> weken;
        Long lastLoad = getLoad(WEKEN_LOADS, context);
        if((weken = getFromStorage(WEKEN_FILENAME, context)) != null) {
            try {
                callback.onAsyncActionComplete(weken);
            } catch (Exception e) {
                Log.e("RoosterInfo", "Er ging iets mis in de wekenCallback", e);
            }
        }

        if(HelperFunctions.hasInternetConnection(context) && !(weken != null && System.currentTimeMillis() < lastLoad + WEKEN_MAX_AGE)) {
            WebDownloader.getWeken(result -> {
                if(weken == null) callback.onAsyncActionComplete(result);
                saveInStorage(WEKEN_FILENAME, context, result);
                setLoad(WEKEN_LOADS, System.currentTimeMillis(), context);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }, context);
        }
    }

    public static int getWeekUrenCount(Context context, int week) throws IllegalArgumentException {
        Hashtable<Integer, Integer> weken = getFromStorage(WEKEN_UREN_FILENAME, context);
        if(weken == null) {
            throw new IllegalArgumentException("Week niet opgeslagen");
        }
        return weken.get(week);
    }

    public static void setWeekUrenCount(Context context, int week, int urenCount) {
        Hashtable<Integer, Integer> weken = getFromStorage(WEKEN_UREN_FILENAME, context);
        if(weken == null) weken = new Hashtable<>();
        weken.put(week, urenCount);
        saveInStorage(WEKEN_UREN_FILENAME, context, weken);
    }

    public static int getCurrentWeek(Context context) {
        ArrayList<Week> weken = RoosterInfo.<ArrayList<Week>>getFromStorage(WEKEN_FILENAME, context);
        if(weken == null) return Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);

        Array<Integer> weekNummers = Array.iterableArray(weken).map(s -> s.week);
        Integer currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        if(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) currentWeek++;
        Integer definitiveWeek = currentWeek >= 52 ? 2 : currentWeek; // next year fix..... wanted?
        return weekNummers.filter(s -> s >= definitiveWeek).foldLeft((a, b) -> a < b ? a : b, 100);
    }

    //endregion
    //region Klassen

    public static void getKlassen(Context context, AsyncActionCallback callback) {
        Object klassen;
        Long lastLoad = getLoad(KLASSEN_LOADS, context);
        if((klassen = getFromStorage(KLASSEN_FILENAME, context)) != null) {
            try {
                callback.onAsyncActionComplete(klassen);
            } catch(Exception e) {
                Log.e("RoosterInfo", "Er ging iets mis in de klassenCallback", e);
            }
        }

        if(HelperFunctions.hasInternetConnection(context) && !(klassen != null && System.currentTimeMillis() < lastLoad + KLASSEN_MAX_AGE)) {
            WebDownloader.getKlassen(result -> {
                if(klassen == null) callback.onAsyncActionComplete(result);
                saveInStorage(KLASSEN_FILENAME, context, klassen);
                setLoad(KLASSEN_LOADS, System.currentTimeMillis(), context);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }, context);
        }
    }

    //endregion
    //region Leraren

    public static void getLeraren(Context context, AsyncActionCallback callback) {
        Object leraren;
        Long lastLoad = getLoad(LERAREN_LOADS, context);
        if((leraren = getFromStorage(LERAREN_FILENAME, context)) != null) {
            try {
                callback.onAsyncActionComplete(leraren);
            } catch (Exception e) {
                Log.e("RoosterInfo", "Er ging iets mis in de lerarenCallback", e);
            }
        }

        if(HelperFunctions.hasInternetConnection(context) && !(leraren != null && System.currentTimeMillis() < lastLoad + LERAREN_MAX_AGE)) {
            WebDownloader.getLeraren(result -> {
                if (leraren == null) callback.onAsyncActionComplete(result);
                saveInStorage(LERAREN_FILENAME, context, result);
                setLoad(LERAREN_LOADS, System.currentTimeMillis(), context);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis met het ophalen van de leraren", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }, context);
        }
    }

    //endregion
    //region Lokalen

    public static void getLokalen(Context context, AsyncActionCallback callback) {
        Object lokalen;
        Long lastLoad = getLoad(LOKALEN_LOADS, context);
        if((lokalen = getFromStorage(LOKALEN_FILENAME, context)) != null) {
            try {
                callback.onAsyncActionComplete(lokalen);
            } catch(Exception e) {
                Log.e("RoosterInfo", "Er ging iets mis in de lokalenCallback", e);
            }
        }

        if(HelperFunctions.hasInternetConnection(context) && !(lokalen != null && System.currentTimeMillis() < lastLoad + LOKALEN_MAX_AGE)) {
            WebDownloader.getLokalen(result -> {
                if(lokalen == null) callback.onAsyncActionComplete(result);
                saveInStorage(LOKALEN_FILENAME, context, result);
                setLoad(LOKALEN_LOADS, System.currentTimeMillis(), context);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets mis het het ophalen van de lokalen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }, context);
        }
    }

    //endregion
    //region Leerlingen

    public static void getLeerlingen(Context context, AsyncActionCallback callback) {
        Object leerlingen;
        Long lastLoad = getLoad(LEERLINGEN_LOADS, context);
        if((leerlingen = getFromStorage(LEERLINGEN_FILENAME, context)) != null) {
            try {
                callback.onAsyncActionComplete(leerlingen);
            } catch(Exception e) {
                Log.e("RoosterInfo", "Er ging iets mis in de leerlingenCallback", e);
            }
        }

        if(HelperFunctions.hasInternetConnection(context) && !(leerlingen != null && System.currentTimeMillis() < lastLoad + LEERLINGEN_MAX_AGE)) {
            WebDownloader.getLeerlingen(result -> {
                if(leerlingen == null) callback.onAsyncActionComplete(result);
                saveInStorage(LEERLINGEN_FILENAME, context, result);
                setLoad(LEERLINGEN_LOADS, System.currentTimeMillis(), context);
            }, exception -> {
                Log.e("WebDownloader", "Er ging iets met het het ophalen van de leerlingen", (Exception) exception);
                ExceptionHandler.handleException((Exception) exception, context, ExceptionHandler.HandleType.SIMPLE);
            }, context);
        }
    }

    //endregion
    //region LoadTimes

    public static Long getLoad(String itemName, Context context) {
        Hashtable<String, Long> loads = getLoads(context);
        if(loads == null) return (long) 0;
        Long result = loads.get(itemName);
        return result == null ? Long.valueOf(0) : result;
    }

    private static Hashtable<String, Long> getLoads(Context context) {
        return getFromStorage(LOADS_FILENAME, context);
    }

    public static void setLoad(String itemName, Long value, Context context) {
        Hashtable<String, Long> loads = getLoads(context);
        if(loads == null) loads = new Hashtable<>();
        loads.put(itemName, value);
        saveInStorage(LOADS_FILENAME, context, loads);
    }

    //endregion
    //region Helpers

    private static <T> T getFromStorage(String fileName, Context context) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(context.openFileInput(fileName));
            Object result = objectInputStream.readObject();
            return (T) result;
        } catch(Exception e) {
            Log.e("RoosterInfo", "Kon "+fileName+" niet openen", e);
            return null;
        }
    }

    private static <T> void saveInStorage(String fileName, Context context, T data) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch(Exception e) {
            Log.e("RoosterInfo", "Kon "+fileName+" niet opslaan", e);
        }
    }

    //endregion
}
