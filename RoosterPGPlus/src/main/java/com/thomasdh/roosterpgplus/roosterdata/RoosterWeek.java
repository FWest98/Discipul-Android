package com.thomasdh.roosterpgplus.roosterdata;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.thomasdh.roosterpgplus.Lesuur;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    public RoosterWeek(Cursor dagen) {
        uren = new Lesuur[5][7][];
        if (dagen.moveToFirst()) {
            do {
                Log.w("Les", LesuurData.cursorToLesuur(dagen).unique);
                if (uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1] == null) {
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1] = new Lesuur[1];
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1][0] = LesuurData.cursorToLesuur(dagen);
                } else {
                    ArrayList<Lesuur> nieuw = new ArrayList<Lesuur>();
                    for (Lesuur les : uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1]) {
                        nieuw.add(les);
                    }
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1] = new Lesuur[nieuw.size() + 1];
                    for (int o = 0; o < nieuw.size(); o++) {
                        uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1][o] = nieuw.get(o);
                    }
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1][nieuw.size()] = LesuurData.cursorToLesuur(dagen);
                }
            } while (dagen.moveToNext());
        }
    }

    public RoosterWeek(String roosterJSON, Context context) {

        try {
            JSONObject weekArray = new JSONObject(roosterJSON);
            uren = new Lesuur[5][7][];
            for (int day = 0; day < 5; day++) {
                if (weekArray.has(getDayOfWeek(day + 2))) {
                    JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day + 2));
                    for (int hour = 0; hour < 7; hour++) {
                        if (dagArray.has(String.valueOf(hour + 1))) {
                            JSONArray uurArray = dagArray.getJSONArray(String.valueOf(hour + 1));
                            uren[day][hour] = new Lesuur[uurArray.length()];
                            for (int p = 0; p < uurArray.length(); p++) {
                                uren[day][hour][p] = new Lesuur(uurArray.getJSONObject(p), context);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler.handleException(e, context, "Fout bij het verwerken van de roostergegevens", RoosterWeek.class.getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
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
        LesuurData ld = new LesuurData(context);
        ld.open();
        Cursor cursor = ld.db.query(SQLRooster.TABLE_ROOSTER, LesuurData.allLesuren,
                SQLRooster.COLUMN_WEEK + " = " + week, null, null, null, null);
        return new RoosterWeek(cursor);
    }

    public void slaOp(Context context) {
        LesuurData ld = new LesuurData(context);
        ld.open();
        List<Lesuur> savedLesuren = ld.getAllLesuren();
        if (uren != null) {
            for (Lesuur[][] lesuur1 : uren) {
                if (lesuur1 != null) {
                    for (Lesuur[] lesuur2 : lesuur1) {
                        if (lesuur2 != null) {
                            for (Lesuur lesuur3 : lesuur2) {
                                if (lesuur3 != null) {
                                    boolean copy = false;
                                    for (Lesuur saved : savedLesuren) {
                                        if (saved.unique.equals(lesuur3.unique)) {
                                            copy = true;
                                        }
                                    }
                                    if (!copy) {
                                        Log.d("Lesuur saved", "Het was geen kopie!?");
                                        ld.addLesuur(lesuur3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Lesuur lesuur : ld.getAllLesuren()) {
            Log.e("RoosterWeek", "!" + lesuur.vak);
        }
        ld.close();
    }

    public int getWeek() {
        if (uren != null) {
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
