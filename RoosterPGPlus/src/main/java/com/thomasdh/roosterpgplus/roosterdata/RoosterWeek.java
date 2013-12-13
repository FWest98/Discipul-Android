package com.thomasdh.roosterpgplus.roosterdata;

import android.util.Log;

import com.thomasdh.roosterpgplus.Lesuur;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by Thomas on 13-12-13.
 */
public class RoosterWeek {

    public static final int MAANDAG = 2;
    public static final int DINSDAG = 3;
    public static final int WOENSDAG = 4;
    public static final int DONDERDAG = 5;
    public static final int VRIJDAG = 6;
    public static final int ZATERDAG = 7;
    public static final int ZONDAG = 1;
    private Lesuur[][][] uren;

    public RoosterWeek(String roosterJSON) {
        try {
            JSONObject weekArray = new JSONObject(roosterJSON);
            uren = new Lesuur[5][7][];
            for (int day = 0; day < 5; day++) {
                JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day + 2));
                for (int hour = 0; hour < 7; hour++) {
                    if (dagArray.has(String.valueOf(hour + 1))) {
                        JSONArray uurArray = dagArray.getJSONArray(String.valueOf(hour + 1));
                        uren[day][hour] = new Lesuur[uurArray.length()];
                        for (int p = 0; p < uurArray.length(); p++) {
                            uren[day][hour][p] = new Lesuur(uurArray.getJSONObject(p));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDayOfWeek(int x) {
        switch (x) {
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

    public Lesuur[] getUren(int dag, int uur) {
        try {
            return uren[dag - 2][uur];
        } catch (NullPointerException e) {
            Log.e(getClass().getSimpleName(), "De dag " + dag + " in combinatie met het uur " + uur + " bestaat niet.", e);
        }
        return null;
    }

    public Lesuur[][] getDag(int dag) {
        return uren[dag - 2];
    }


}
