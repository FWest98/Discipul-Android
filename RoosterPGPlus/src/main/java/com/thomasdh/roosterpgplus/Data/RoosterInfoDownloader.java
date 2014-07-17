package com.thomasdh.roosterpgplus.Data;

import android.os.AsyncTask;
import android.util.Log;

import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Models.Leraar;
import com.thomasdh.roosterpgplus.Models.Vak;
import com.thomasdh.roosterpgplus.Models.Week;
import com.thomasdh.roosterpgplus.Settings;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Scanner;


public class RoosterInfoDownloader extends AsyncTask<Object, Void, Hashtable<String, Object>> {
    private static final String DATA_KEY = "data";
    private static final String INTERFACE_KEY = "interface";
    private static final String ERROR_KEY = "exception";

    @Override
    protected Hashtable<String, Object> doInBackground(Object... info) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(Settings.API_Base_URL + info[0]);
            HttpResponse response = httpClient.execute(get);

            Object data = ((AsyncCallback) info[1]).onBackground(response);

            Hashtable<String, Object> hashtable = new Hashtable<>();
            hashtable.put(DATA_KEY, data);
            hashtable.put(INTERFACE_KEY, info[2]);

            return hashtable;
        } catch (Exception e) {
            Hashtable<String, Object> hashtable = new Hashtable<>();
            hashtable.put(ERROR_KEY, e);
            hashtable.put(INTERFACE_KEY, info[3]);
            return hashtable;
        }
    }

    @Override
    protected void onPostExecute(Hashtable<String, Object> hashtable) {
        String getter = DATA_KEY;
        if(hashtable.containsKey(ERROR_KEY)) {
            getter = ERROR_KEY;
        }

        AsyncActionCallback callback = (AsyncActionCallback) hashtable.get(INTERFACE_KEY);
        try {
            callback.onAsyncActionComplete(hashtable.get(getter));
        } catch (Exception e) {
            Log.e("RoosterInfoDownloader", "Fout in de succesCallback", e);
        }
    }

    /* Lerarendownloader */
    public static void getLeraren(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?leraren&sort";

        AsyncCallback AsyncCallback = r -> {
            HttpResponse response = (HttpResponse) r;
            int status = response.getStatusLine().getStatusCode();
            switch(status) {
                case 500: throw new Exception("Serverfout, probeer het later nogmaals");
                case 401: throw new Exception("Fout in de aanvraag, probeer de app te updaten");
                case 200: break;
                default: throw new Exception("Onbekende fout, "+status);
            }

            String s = "";
            Scanner scanner = new Scanner(response.getEntity().getContent());
            while (scanner.hasNext()) {
                s += scanner.nextLine();
            }

            if(s.equals("")) throw new NullPointerException("Geen leraren gevonden!");

            ArrayList<Vak> vakken = new ArrayList<>();
            ArrayList<Leraar> leraren = new ArrayList<>();
            JSONArray root = new JSONArray(s);

            for(int i = 0; i < root.length(); i++) {
                JSONObject JSONvak = root.getJSONObject(i);
                Vak vak = new Vak(JSONvak.getString("naam"));

                JSONArray Jleraren = JSONvak.getJSONArray("leraren");
                for(int a = 0; a < Jleraren.length(); a++) {
                    JSONObject JSONleraar = Jleraren.getJSONObject(a);
                    Leraar leraar = new Leraar(JSONleraar.getString("code"), JSONleraar.getString("naam"));
                    vak.setLeraren(leraar);
                    if(!leraren.contains(leraar)) {
                        leraren.add(leraar);
                    }
                }

                vakken.add(vak);
            }
            Vak allVak = new Vak("Alles");
            allVak.setLeraren(leraren);
            vakken.add(allVak);

            // Sorteer alle vakken
            Collections.sort(vakken, (lhs, rhs) -> {
                if (lhs.getNaam().equals("Alles")) {
                    return -1;
                }
                if (rhs.getNaam().equals("Alles")) {
                    return 1;
                }
                return lhs.getNaam().compareToIgnoreCase(rhs.getNaam());
            });

            // Sorteer de leraren in alle vakken
            for (Vak v : vakken) {
                Collections.sort(v.getLeraren(), (lhs, rhs) -> lhs.getNaam().compareToIgnoreCase(rhs.getNaam()));
            }

            return vakken;
        };

        new RoosterInfoDownloader().execute(url, AsyncCallback, callback, errorCallback);
    }

    /* Klassen downloader */
    public static void getKlassen(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?klassen";

        AsyncCallback AsyncCallback = r -> {
            HttpResponse response = (HttpResponse) r;
            int status = response.getStatusLine().getStatusCode();

            switch(status) {
                case 200: break;
                case 500: throw new Exception("Serverfout. Probeer het later nogmaals");
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                default: throw new Exception("Onbekende fout, "+status);
            }

            String s = "";
            Scanner sc = new Scanner(response.getEntity().getContent());
            while (sc.hasNext()) {
                s += sc.nextLine();
            }

            if (s.equals("")) throw new NullPointerException("Geen klassen gevonden!");

            // verder verwerken
            ArrayList<String> klassen = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(s);

            for (int i = 0; i < jsonArray.length(); i++) {
                // TODO maak een klas-class
                JSONObject klas = jsonArray.getJSONObject(i);
                klassen.add(klas.getString("klasnaam"));
            }

            return klassen;
        };

        new RoosterInfoDownloader().execute(url, AsyncCallback, callback, errorCallback);
    }

    /* Weken downloader */
    public static void getWeken(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        getWeken(false, true, 10, callback, errorCallback);
    }

    public static void getWeken(boolean periode, boolean known, int limit, AsyncActionCallback parentCallback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?weken";
        if(periode) url += "&periode";
        if(known) url += "&known";

        AsyncCallback AsyncCallback = r -> {
            HttpResponse response = (HttpResponse) r;
            int status = response.getStatusLine().getStatusCode();

            switch(status) {
                case 200: break;
                case 500: throw new Exception("Serverfout, Probeer het later nogmaals");
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                default: throw new Exception("Onbekende fout, "+status);
            }

            String s = "";
            Scanner scanner = new Scanner(response.getEntity().getContent());
            while (scanner.hasNext()) {
                s += scanner.nextLine();
            }

            if(s.equals("")) throw new NullPointerException("Geen weken gevonden!");

            // Verder verwerken
            ArrayList<Week> weken = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(s);

            for (int i = 0; i < Math.min(limit, jsonArray.length()); i++) {
                JSONObject week = jsonArray.getJSONObject(i);
                if (!week.getBoolean("vakantieweek")) {
                    weken.add(new Week(week.getInt("week"), week.getBoolean("vakantieweek")));
                }
            }

            return weken;
        };

        new RoosterInfoDownloader().execute(url, AsyncCallback, parentCallback, errorCallback);
    }

    /* Rooster downloader */
    public static void getRooster(String url, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        AsyncCallback AsyncCallback = r -> {
            HttpResponse response = (HttpResponse) r;
            int status = response.getStatusLine().getStatusCode();

            String content = "";
            Scanner sc = new Scanner(response.getEntity().getContent());
            while(sc.hasNext()) { content += sc.nextLine(); }

            switch(status) {
                case 200: break;
                case 404: throw new Exception(content);
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                case 500: throw new Exception("Serverfout. Probeer het later nog eens");
                default: throw new Exception("Onbekende fout, "+status+", "+content);
            }

            return content; // TODO roosterformat
        };

        new RoosterInfoDownloader().execute(url, AsyncCallback, callback, errorCallback);
    }

    private interface AsyncCallback {
        public Object onBackground(Object result) throws Exception;
    }

}
