package com.thomasdh.roosterpgplus.Models;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode(callSuper = false)
@ToString
@DatabaseTable(tableName = "Lessen")
public class Lesuur extends BaseDaoEnabled implements Serializable {
    private static final long serialVersionUID = 7526472295622776147L;

    @DatabaseField(generatedId = true) public int id;
    @DatabaseField public String query;

    @DatabaseField public int dag;
    @DatabaseField public int uur;
    @DatabaseField public Date lesStart;
    @DatabaseField public Date lesEind;
    @DatabaseField public int week;

    @DatabaseField(dataType = DataType.SERIALIZABLE) public ArrayList<String> klassen;
    @DatabaseField(dataType = DataType.SERIALIZABLE) public ArrayList<String> leraren;
    @DatabaseField public String vak;
    @DatabaseField public String lokaal;

    @DatabaseField public boolean verandering;
    @DatabaseField public boolean vervallen;
    @DatabaseField public boolean isNew;
    @DatabaseField public boolean verplaatsing;

    @DatabaseField private String huiswerk;
    @DatabaseField private boolean master;
    @DatabaseField private int bijzonderheid;

    public Lesuur() {}

    public Lesuur(JSONObject JSON, Context context, String query) {
        try {
            dag = JSON.getInt("dag");
            uur = JSON.getInt("uur");

            SimpleDateFormat webFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                lesStart = webFormat.parse(JSON.getString("lesStart"));
                lesEind = webFormat.parse(JSON.getString("lesEind"));
            } catch (ParseException e) {
                Log.e("Datumparsefout", e.getMessage(), e);
                Calendar cal = Calendar.getInstance();
                cal.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
                lesStart = cal.getTime();
                lesEind = cal.getTime();
            }
            week = JSON.getInt("week");


            JSONArray klassenArray = JSON.getJSONArray("klassen");
            klassen = new ArrayList<>();
            for(int i = 0; i < klassenArray.length(); i++) {
                klassen.add(klassenArray.getString(i));
            }

            JSONArray lerarenArray = JSON.getJSONArray("leraren");
            leraren = new ArrayList<>();
            for(int i = 0; i < lerarenArray.length(); i++) {
                leraren.add(lerarenArray.getString(i));
            }
            vak = JSON.getString("vak");
            lokaal = JSON.getString("lokaal");


            verandering = JSON.getBoolean("verandering");
            vervallen = JSON.getBoolean("vervallen");
            verplaatsing = JSON.getBoolean("verplaatsing");
            isNew = JSON.getBoolean("new");


            huiswerk = JSON.getString("huiswerk");
            master = JSON.getBoolean("master");
            bijzonderheid = JSON.getInt("bijzonderheid");

            this.query =  query;
        } catch (JSONException e) {
            ExceptionHandler.handleException(e, context, "Er is een fout opgetreden bij het lezen van de roosterdata", Lesuur.class.getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
        }
    }

    public Lesuur(int dag, int uur, Date lesStart, Date lesEind, int week, ArrayList<String> klassen, ArrayList<String> leraren, String vak, String lokaal, boolean verandering, boolean vervallen, boolean isNew, boolean verplaatsing, String huiswerk, boolean master, int bijzonderheid, String query) {
        this.dag = dag;
        this.uur = uur;
        this.lesStart = lesStart;
        this.lesEind = lesEind;
        this.week = week;
        this.klassen = klassen;
        this.leraren = leraren;
        this.vak = vak;
        this.verandering = verandering;
        this.vervallen = vervallen;
        this.lokaal = lokaal;
        this.isNew = isNew;
        this.verplaatsing = verplaatsing;
        this.huiswerk = huiswerk;
        this.master = master;
        this.bijzonderheid = bijzonderheid;
        this.query = query;
    }
}