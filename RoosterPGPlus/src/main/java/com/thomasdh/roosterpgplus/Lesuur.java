package com.thomasdh.roosterpgplus;

import android.content.Context;

import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Floris on 3-12-13.
 */
public class Lesuur implements Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    private String unique;
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
    private String huiswerk;
    private boolean so;
    private boolean pw;
    private boolean master;
    private int bijzonder;

    public Lesuur(JSONObject JSON, Context context) {
        try {
            unique = JSON.getString("unique");
            dag = JSON.getInt("dag");
            uur = JSON.getInt("uur");
            week = JSON.getInt("week");
            klas = JSON.getString("klas");
            leraar = JSON.getString("leraar");
            leraar2 = JSON.getString("leraar2");
            vak = JSON.getString("vak");
            lokaal = JSON.getString("lokaal");
            verandering = JSON.getInt("verandering") == 1;
            vervallen = JSON.getInt("vervallen") == 1;
            huiswerk = JSON.getString("huiswerk");
            //this.bijlage_id = JSON.getInt("bijlage-id");
            so = JSON.getInt("so") == 1;
            pw = JSON.getInt("pw") == 1;
            master = JSON.getInt("master") == 1;
            bijzonder = JSON.getInt("bijzonder");
            unique = klas + dag + uur + week;
        } catch (JSONException e) {
            ExceptionHandler.handleException(e, context, "Er is een fout opgetreden bij het lezen van de roosterdata", Lesuur.class.getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
        }
    }

    public Lesuur(int dag, int uur, int week, String klas, String leraar, String vak, String lokaal, boolean vervallen, boolean verandering) {
        unique = klas + dag + uur + week;
        this.dag = dag;
        this.uur = uur;
        this.week = week;
        this.klas = klas;
        this.leraar = leraar;
        leraar2 = null;
        this.vak = vak;
        this.lokaal = lokaal;
        this.verandering = verandering;
        this.vervallen = vervallen;
        huiswerk = null;
        int bijlage_id = 0;
        so = false;
        pw = false;
        master = false;
        bijzonder = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() == Lesuur.class) {
            Lesuur lesuur = (Lesuur) o;
            return unique.equals(lesuur.unique);
        }
        return false;
    }
}