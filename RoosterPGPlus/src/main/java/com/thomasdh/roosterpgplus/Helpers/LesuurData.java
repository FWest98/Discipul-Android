package com.thomasdh.roosterpgplus.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.thomasdh.roosterpgplus.Models.Lesuur;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Thomas on 30-12-13.
 */
public class LesuurData {

    public static final String[] allLesuren = {SQLRooster.COLUMN_ID,
            SQLRooster.COLUMN_DAG,
            SQLRooster.COLUMN_UUR,
            SQLRooster.COLUMN_WEEK,
            SQLRooster.COLUMN_VERANDERING,
            SQLRooster.COLUMN_VERVALLEN,
            SQLRooster.COLUMN_VERPLAATSING,
            SQLRooster.COLUMN_BIJZONDERHEID,
            SQLRooster.COLUMN_MASTER,
            SQLRooster.COLUMN_KLAS,
            SQLRooster.COLUMN_VAK,
            SQLRooster.COLUMN_LOKAAL,
            SQLRooster.COLUMN_LERAAR};
    private final SQLRooster dbHelper;
    public SQLiteDatabase db;
    private final Context context;

    public LesuurData(Context context) {
        dbHelper = new SQLRooster(context);
        this.context = context;
    }

    public static Lesuur cursorToLesuur(Cursor cursor) {
        /*return new Lesuur(cursor.getInt(1),
                cursor.getInt(2),
                cursor.getInt(3),
                cursor.getString(9),
                cursor.getString(12),
                cursor.getString(10),
                cursor.getString(11),
                cursor.getInt(5) > 0,
                cursor.getInt(4) > 0);*/
        return null;
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addLesuur(Lesuur lesuur) {
        ContentValues values = new ContentValues();
        values.put(SQLRooster.COLUMN_DAG, lesuur.dag);
        values.put(SQLRooster.COLUMN_UUR, lesuur.uur);
        values.put(SQLRooster.COLUMN_WEEK, lesuur.week);
        values.put(SQLRooster.COLUMN_KLAS, lesuur.klassen.get(0));
        values.put(SQLRooster.COLUMN_LERAAR, lesuur.leraren.get(0));
        values.put(SQLRooster.COLUMN_VAK, lesuur.vak);
        values.put(SQLRooster.COLUMN_LOKAAL, lesuur.lokaal);
        values.put(SQLRooster.COLUMN_VERVALLEN, lesuur.vervallen);
        values.put(SQLRooster.COLUMN_VERANDERING, lesuur.verandering);
        db.insert(SQLRooster.TABLE_ROOSTER, null, values);
    }

    public void deleteOldWeeks() {
        int dezeWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int aantalOpgeslagenWeken = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("opgeslagenWeken", "10"));
        db.delete(SQLRooster.TABLE_ROOSTER, "(" + SQLRooster.COLUMN_WEEK + " < " + dezeWeek + " and " + SQLRooster.COLUMN_WEEK + " > " + (dezeWeek - 52 + aantalOpgeslagenWeken) + ") and " + SQLRooster.COLUMN_WEEK + " > " + (aantalOpgeslagenWeken + dezeWeek), null);
    }

    public void deleteWeek(int week) {
        db.delete(SQLRooster.TABLE_ROOSTER, SQLRooster.COLUMN_WEEK + " = " + week, null);
    }

    public void deleteLesuur(Cursor cursor) {
        int id = cursor.getInt(0);
        db.delete(SQLRooster.TABLE_ROOSTER, SQLRooster.COLUMN_ID + " = " + id, null);
    }

    public List<Lesuur> getAllLesuren() {
        List<Lesuur> lesuren = new ArrayList<Lesuur>();
        Cursor cursor = db.query(SQLRooster.TABLE_ROOSTER, allLesuren, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Lesuur lesuur = cursorToLesuur(cursor);
            lesuren.add(lesuur);
            cursor.moveToNext();
        }
        cursor.close();
        return lesuren;
    }

}
