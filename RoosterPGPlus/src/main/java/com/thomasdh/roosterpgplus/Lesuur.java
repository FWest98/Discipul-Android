package com.thomasdh.roosterpgplus;

import android.content.Context;

import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Floris on 3-12-13.
 */
public class Lesuur implements Serializable{

    private static final long serialVersionUID = 7526472295622776147L;

    public String unique;
    public int dag;
    public int uur;
    public int week;
    public String klas;
    public String leraar;
    public String leraar2;
    public String vak;
    public String lokaal;
    public boolean verandering;
    public boolean vervallen;
    public String huiswerk;
    public int bijlage_id;
    public boolean so;
    public boolean pw;
    public boolean master;
    public int bijzonder;

    public Lesuur(JSONObject JSON, Context context) {
        try {
            this.unique = JSON.getString("unique");
            this.dag = JSON.getInt("dag");
            this.uur = JSON.getInt("uur");
            this.week = JSON.getInt("week");
            this.klas = JSON.getString("klas");
            this.leraar = JSON.getString("leraar");
            this.leraar2 = JSON.getString("leraar2");
            this.vak = JSON.getString("vak");
            this.lokaal = JSON.getString("lokaal");
            this.verandering = JSON.getInt("verandering") == 1;
            this.vervallen = JSON.getInt("vervallen") == 1;
            this.huiswerk = JSON.getString("huiswerk");
            //this.bijlage_id = JSON.getInt("bijlage-id");
            this.so = JSON.getInt("so") == 1;
            this.pw = JSON.getInt("pw") == 1;
            this.master = JSON.getInt("master") == 1;
            this.bijzonder = JSON.getInt("bijzonder");
        } catch (JSONException e) {
            ExceptionHandler.handleException(e, context, "Er is een fout opgetreden bij het lezen van de roosterdata", getClass().getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
        }
    }
    public Lesuur(int dag, int uur, int week, String klas, String leraar, String vak, String lokaal, boolean vervallen) {
        this.unique = klas+dag+uur+week;
        this.dag = dag;
        this.uur = uur;
        this.week = week;
        this.klas = klas;
        this.leraar = leraar;
        this.leraar2 = null;
        this.vak = vak;
        this.lokaal = lokaal;
        this.verandering = false;
        this.vervallen = vervallen;
        this.huiswerk = null;
        this.bijlage_id = 0;
        this.so = false;
        this.pw = false;
        this.master = false;
        this.bijzonder = 0;
    }


}