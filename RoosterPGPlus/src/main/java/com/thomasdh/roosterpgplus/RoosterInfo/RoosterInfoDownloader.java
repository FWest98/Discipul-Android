package com.thomasdh.roosterpgplus.RoosterInfo;

import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings;

import org.apache.http.HttpResponse;
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
    /** Klassen downloader */
    static public ArrayList<String> getKlassen() throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet Get = new HttpGet(Settings.API_Base_URL+"rooster/info?klassen");

        try {
            HttpResponse response = httpClient.execute(Get);
            int status = response.getStatusLine().getStatusCode();

            if(status == 200) {
                String s = "";
                Scanner sc = new Scanner(response.getEntity().getContent());
                while(sc.hasNext()) {
                    s += sc.nextLine();
                }
                if(!s.equals("")) {
                    Log.e("Klassen:", s);
                    // verder verwerken
                    ArrayList<String> klassen = new ArrayList<String>();
                    JSONArray jsonArray = new JSONArray(s);

                    for(int i = 0; i < jsonArray.length(); i++) {
                        klassen.add(jsonArray.getString(i));
                    }

                    return klassen;
                } else {
                    return null;
                }
            } else {
                throw new IOException("Onbekende status: " + status);
            }
        } catch (IOException e) {
            throw e;
        } catch (JSONException e) {
            Log.e("Errors:", e.getStackTrace().toString());
            e.printStackTrace();
            throw new IOException("Parsefout " + e.getCause());
        }
    }

    /** Weken downloader */
    static public ArrayList<Week> getWeken() throws IOException {
        try {
            return getWeken(false, 0);
        } catch (IOException e) {
            throw e;
        }
    }
    static public ArrayList<Week> getWeken(final Boolean AllWeeks) throws IOException {
        try {
            return getWeken(AllWeeks, 0);
        } catch (IOException e) {
            throw e;
        }
    }
    static public ArrayList<Week> getWeken(final int numberOfWeeks) throws IOException {
        try {
            return getWeken(false, numberOfWeeks);
        } catch (IOException e) {
            throw e;
        }
    }
    static public ArrayList<Week> getWeken(final boolean AllWeeks, final int numberOfWeeks) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Settings.API_Base_URL+"rooster/info?weken");

        try {
            HttpResponse response = httpClient.execute(get);
            int status = response.getStatusLine().getStatusCode();

            if(status == 200) {
                String s = "";
                Scanner scanner = new Scanner(response.getEntity().getContent());
                while(scanner.hasNext()) {
                    s += scanner.nextLine();
                }
                // verder verwerken
                ArrayList<Week> weken = new ArrayList<Week>();
                JSONArray jsonArray = new JSONArray(s);

                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject week = jsonArray.getJSONObject(i);
                    weken.add(new Week(week.getInt("week"), week.getBoolean("vakantieweek")));
                }

                if(!AllWeeks && numberOfWeeks <= 0) {
                    for(Week week : weken) {
                        if(!week.vakantieweek) {
                            weken.remove(week);
                        }
                    }
                } else if(!AllWeeks && numberOfWeeks > 0) {
                    ArrayList<Week> weken2 = new ArrayList<Week>();
                    for(int i = 0; i < numberOfWeeks; i++) {
                        Week week = weken.get(i);
                        weken2.add(week);
                        if(!week.vakantieweek) {
                            weken2.remove(i);
                        }
                    }
                    weken = weken2;
                } else if(AllWeeks && numberOfWeeks > 0) {
                    ArrayList<Week> weken2 = new ArrayList<Week>();
                    for(int i = 0; i < numberOfWeeks; i++) {
                        Week week = weken.get(i);
                        weken2.add(week);
                    }
                    weken = weken2;
                }
                return weken;
            } else {
                throw new IOException("Onbekende status: " + status);
            }
        } catch(IOException e) {
            throw e;
        } catch(JSONException e) {
            throw new IOException("Parsefout");
        }
    }
}
