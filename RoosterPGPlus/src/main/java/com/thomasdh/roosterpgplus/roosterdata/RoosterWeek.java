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
import java.util.Collections;

/**
 * Created by Thomas on 13-12-13.
 */
public class RoosterWeek implements Serializable {

    private static final long serialVersionUID = 1029472134713472957L;
    private Lesuur[][][] uren;

    private RoosterWeek(Cursor dagen) {
        uren = new Lesuur[5][7][];
        if (dagen.moveToFirst()) {
            do {
                if (uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1] == null) {
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1] = new Lesuur[1];
                    uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1][0] = LesuurData.cursorToLesuur(dagen);
                } else {
                    ArrayList<Lesuur> nieuw = new ArrayList<Lesuur>();
                    Collections.addAll(nieuw, uren[LesuurData.cursorToLesuur(dagen).dag - 1][LesuurData.cursorToLesuur(dagen).uur - 1]);
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
            uren = new Lesuur[5][7][];
            if(roosterJSON == null) {
                return;
            }
            JSONObject weekArray = new JSONObject(roosterJSON);
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

    private static String getDayOfWeek(int x) {
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

    public void slaOp(Context context, int week) {
        LesuurData ld = new LesuurData(context);
        ld.open();
        ld.deleteWeek(week);
        if (uren != null) {
            for (Lesuur[][] lesuur1 : uren) {
                if (lesuur1 != null) {
                    for (Lesuur[] lesuur2 : lesuur1) {
                        if (lesuur2 != null) {
                            for (Lesuur lesuur3 : lesuur2) {
                                if (lesuur3 != null) {
                                    ld.addLesuur(lesuur3);
                                }
                            }
                        }
                    }
                }
            }
        }
        ld.deleteOldWeeks();
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
