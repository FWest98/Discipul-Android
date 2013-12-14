package com.thomasdh.roosterpgplus.roosterdata;

import android.content.Context;
import android.util.Log;

import com.thomasdh.roosterpgplus.Lesuur;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by Thomas on 13-12-13.
 */
public class RoosterWeek implements Serializable {

    public static final int MAANDAG = 2;
    public static final int DINSDAG = 3;
    public static final int WOENSDAG = 4;
    public static final int DONDERDAG = 5;
    public static final int VRIJDAG = 6;
    public static final int ZATERDAG = 7;
    public static final int ZONDAG = 1;
    private static final long serialVersionUID = 1029472134713472957L;
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

    public static RoosterWeek laadUitGeheugen(int week, Context context) {
        if (week == -1) {
            week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        }
        RoosterWeek roosterWeek;
        try {
            FileInputStream fis = context.openFileInput("roosterWeek" + (week % 4));
            ObjectInputStream ois = new ObjectInputStream(fis);
            roosterWeek = (RoosterWeek) ois.readObject();
            ois.close();
            fis.close();
            return roosterWeek;
        } catch (Exception e) {
            Log.e("MainActivity", "Kon het rooster niet laden", e);
            e.printStackTrace();
        }
        return null;
    }

    public void slaOp(Context context) {
        if (getWeek() != -1) {
            try {
                FileOutputStream fos = context.openFileOutput("roosterWeek" + (getWeek() % 4), Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(this);
                oos.close();
                fos.close();
            } catch (Exception e) {
                Log.e("RoosterWeek", "Kon het bestand niet opslaan", e);
                e.printStackTrace();
            }
        }
    }

    public int getWeek() {
        for (Lesuur[][] lesuur1 : uren) {
            if (lesuur1 != null) {
                for (Lesuur[] lesuur2 : lesuur1) {
                    if (lesuur2 != null) {
                        for (Lesuur lesuur3 : lesuur2) {
                            if (lesuur3 != null) {
                                return lesuur3.week;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public Lesuur[] getUren(int dag, int uur) {
        try {
            return uren[dag - 2][uur];
        } catch (NullPointerException e) {
            Log.e("RoosterWeek", "De dag " + dag + " in combinatie met het uur " + uur + " bestaat niet.", e);
        }
        return null;
    }

    public Lesuur[][] getDag(int dag) {
        return uren[dag - 2];
    }


}
