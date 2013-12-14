package com.thomasdh.roosterpgplus.RoosterInfo;

import android.util.Log;

import com.thomasdh.roosterpgplus.Settings;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Floris on 9-12-13.
 */
public class RoosterInfoDownloader {


    /* Leraren downloader */
    static public ArrayList<Leraar> getLeraren() {
        ArrayList<Leraar> leraren = new ArrayList<Leraar>();
        try {
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
                try {
                    JSONArray lerarenJSON = new JSONArray(s);
                    for (int y = 0; y < lerarenJSON.length(); y++) {
                        JSONObject leraar = lerarenJSON.getJSONObject(y);
                        leraren.add(new Leraar(leraar.getString("key"), leraar.getString("value")));
                    }
                    return leraren;
                } catch (JSONException e) {
                    Log.e("RoosterInfoDownloader", "Parsefout", e);
                    e.printStackTrace();
                }
            } else {
                throw new IOException("Onbekende status: " + status);
            }
        } catch (ClientProtocolException e) {
            Log.e("RoosterInfoDownloader", "Fout met het protocol", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("RoosterInfoDownloader", "Input-Output exception bij het laden van het rooster", e);
            e.printStackTrace();
        }
        return null;
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
    static public ArrayList<Week> getWeken() {
        return getWeken(false, 0);
    }

    static public ArrayList<Week> getWeken(final Boolean AllWeeks) {
        return getWeken(AllWeeks, 0);
    }

    static public ArrayList<Week> getWeken(final int numberOfWeeks) {
        return getWeken(false, numberOfWeeks);
    }

    static public ArrayList<Week> getWeken(final boolean AllWeeks, final int numberOfWeeks) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Settings.API_Base_URL + "rooster/info?weken");

        try {
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

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject week = jsonArray.getJSONObject(i);
                    weken.add(new Week(week.getInt("week"), week.getBoolean("vakantieweek")));
                }

                if (!AllWeeks && numberOfWeeks <= 0) {
                    for (Week week : weken) {
                        if (!week.vakantieweek) {
                            weken.remove(week);
                        }
                    }
                } else if (!AllWeeks && numberOfWeeks > 0) {
                    ArrayList<Week> weken2 = new ArrayList<Week>();
                    for (int i = 0; i < numberOfWeeks; i++) {
                        Week week = weken.get(i);
                        weken2.add(week);
                        if (!week.vakantieweek) {
                            weken2.remove(i);
                        }
                    }
                    weken = weken2;
                } else if (AllWeeks && numberOfWeeks > 0) {
                    ArrayList<Week> weken2 = new ArrayList<Week>();
                    for (int i = 0; i < numberOfWeeks; i++) {
                        Week week = weken.get(i);
                        weken2.add(week);
                    }
                    weken = weken2;
                }
                return weken;
            } else {
                throw new IOException("Onbekende status: " + status);
            }
        } catch (IOException e) {
            Log.e("RoosterInfoDownloader", "Fout bij het laden van de klassen", e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("RoosterInfoDownloader", "Fout bij het laden van de klassen", e);
            e.printStackTrace();
        }
        return null;
    }

    public static class Leraar {
        public String korteNaam;
        public String naam;

        public Leraar(String korteNaam, String naam) {
            this.naam = naam;
            this.korteNaam = korteNaam;
        }
    }

    public static class Week {
        public int week;
        public boolean vakantieweek;

        public Week(int week, boolean vakantieweek) {
            this.week = week;
            this.vakantieweek = vakantieweek;
        }
    }
}
