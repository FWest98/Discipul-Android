package com.thomasdh.roosterpgplus.roosterdata;

import android.util.Log;

import com.thomasdh.roosterpgplus.Settings;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Created by Floris on 9-12-13.
 */
public class RoosterInfoDownloader {


    /* Lerarendownloader */
    static public ArrayList<Vak> getLeraren() throws IOException, JSONException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Settings.API_Base_URL + "rooster/info?leraren");
        HttpResponse response = httpClient.execute(get);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            String s = "";
            Scanner scanner = new Scanner(response.getEntity().getContent());
            while (scanner.hasNext()) {
                s += scanner.nextLine();
            }
            Log.d("RoosterInfoDownloader", "LerarenString: " + s);
            ArrayList<Vak> vakken = new ArrayList<Vak>();
            JSONObject root = new JSONObject(s);
            JSONObject vakkenObject = root.getJSONObject("SORTED");
            JSONObject allArray = root.getJSONObject("ALL");

            // Maak een array met alle vakken
            vakken.add(new Vak("Alles"));
            Iterator<String> namenKeys = allArray.keys();
            while (namenKeys.hasNext()) {
                String key = namenKeys.next();
                if (allArray.getString(key) != null)
                    vakken.get(0).leraren.add(new Leraar(key, allArray.getString(key)));
            }

            Iterator<String> keys = vakkenObject.keys();
            while (keys.hasNext()) {
                vakken.add(new Vak(keys.next()));
            }

            // Sorteer alle vakken
            Collections.sort(vakken, new Comparator<Vak>() {
                @Override
                public int compare(Vak lhs, Vak rhs) {
                    if (lhs.naam.equals("Alles")) {
                        return -1;
                    }
                    if (rhs.naam.equals("Alles")) {
                        return 1;
                    }
                    return lhs.naam.compareToIgnoreCase(rhs.naam);
                }
            });

            // Vul alle leraarcodes in en zoek de goede naam erbij
            for (Vak vak : vakken) {
                if (vakkenObject.has(vak.naam)) {
                    JSONArray namen = vakkenObject.getJSONArray(vak.naam);
                    for (int u = 0; u < namen.length(); u++) {
                        if (allArray.has(namen.getString(u))) {
                            // Als er een naam bekend is: gebruik die
                            vak.leraren.add(new Leraar(namen.getString(u), allArray.getString(namen.getString(u))));
                        } else {
                            vak.leraren.add(new Leraar(namen.getString(u), namen.getString(u)));
                        }
                    }
                }
            }

            // Sorteer de leraren in alle vakken
            for (Vak v : vakken) {
                Collections.sort(v.leraren, new Comparator<Leraar>() {
                    @Override
                    public int compare(Leraar lhs, Leraar rhs) {
                        return lhs.korteNaam.compareToIgnoreCase(rhs.korteNaam);
                    }
                });
            }

            return vakken;
        } else {
            throw new IOException("Onbekende status: " + status);
        }
    }

    /* Klassen downloader */
    static public ArrayList<String> getKlassen() {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet Get = new HttpGet(Settings.API_Base_URL + "rooster/info?klassen");

        try {
            HttpResponse response = httpClient.execute(Get);
            int status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                String s = "";
                Scanner sc = new Scanner(response.getEntity().getContent());
                while (sc.hasNext()) {
                    s += sc.nextLine();
                }
                if (!s.equals("")) {
                    Log.e("Klassen:", s);
                    // verder verwerken
                    ArrayList<String> klassen = new ArrayList<String>();
                    JSONArray jsonArray = new JSONArray(s);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        klassen.add(jsonArray.getString(i));
                    }

                    return klassen;
                }
            } else {
                throw new IOException("Onbekende status: " + status);
            }
        } catch (JSONException e) {
            Log.e("RoosterInfoDownloader", "JSONfout", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("RoosterInfoDownloader", "Fout bij laden van de klassen", e);
            e.printStackTrace();
        }
        return null;
    }

    /* Weken downloader */
    static public ArrayList<Week> getWeken() throws IOException, JSONException {
        return getWeken(false, 0);
    }

    static public ArrayList<Week> getWeken(Boolean AllWeeks) throws IOException, JSONException {
        return getWeken(AllWeeks, 0);
    }

    static public ArrayList<Week> getWeken(int numberOfWeeks) throws IOException, JSONException {
        return getWeken(false, numberOfWeeks);
    }

    private static ArrayList<Week> getWeken(boolean AllWeeks, int numberOfWeeks) throws IOException, JSONException {

        if (numberOfWeeks == 0)
            numberOfWeeks = 1000;

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Settings.API_Base_URL + "rooster/info?weken");

        HttpResponse response = httpClient.execute(get);
        int status = response.getStatusLine().getStatusCode();

        if (status == 200) {
            String s = "";
            Scanner scanner = new Scanner(response.getEntity().getContent());
            while (scanner.hasNext()) {
                s += scanner.nextLine();
            }
            // verder verwerken
            ArrayList<Week> weken = new ArrayList<Week>();
            JSONArray jsonArray = new JSONArray(s);

            for (int i = 0; i < Math.min(numberOfWeeks, jsonArray.length()); i++) {
                JSONObject week = jsonArray.getJSONObject(i);
                if (!AllWeeks && !week.getBoolean("vakantieweek")) {
                    weken.add(new Week(week.getInt("week"), week.getBoolean("vakantieweek")));
                }
            }
            return weken;
        } else {
            throw new IOException("Onbekende status: " + status);
        }
    }

    public static class Leraar {
        public final String korteNaam;
        public final String naam;

        public Leraar(String korteNaam, String naam) {
            this.naam = naam;
            this.korteNaam = korteNaam;
        }
    }

    public static class Vak {
        public final String naam;
        public final ArrayList<Leraar> leraren;

        public Vak(String naam) {
            this.naam = naam;
            leraren = new ArrayList<Leraar>();
        }
    }

    public static class Week implements Serializable {
        private static final long serialVersionUID = 102947213471347253L;
        public final int week;
        public final boolean vakantieweek;

        public Week(int week, boolean vakantieweek) {
            this.week = week;
            this.vakantieweek = vakantieweek;
        }
    }
}
