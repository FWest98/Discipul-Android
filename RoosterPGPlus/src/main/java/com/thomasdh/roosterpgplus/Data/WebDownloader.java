package com.thomasdh.roosterpgplus.Data;

import android.os.AsyncTask;
import android.util.Log;

import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Models.Klas;
import com.thomasdh.roosterpgplus.Models.Leerling;
import com.thomasdh.roosterpgplus.Models.Leraar;
import com.thomasdh.roosterpgplus.Models.PGTVPage;
import com.thomasdh.roosterpgplus.Models.Vak;
import com.thomasdh.roosterpgplus.Models.Week;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class WebDownloader extends AsyncTask<Object, Void, Hashtable<String, Object>> {
    private static final String DATA_KEY = "data";
    private static final String ERROR_KEY = "exception";

    private AsyncCallback asyncAction;
    private AsyncActionCallback successCallback;
    private AsyncActionCallback errorCallback;

    private WebDownloader(AsyncCallback asyncAction, AsyncActionCallback successCallback, AsyncActionCallback errorCallback) {
        this.asyncAction = asyncAction;
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    protected Hashtable<String, Object> doInBackground(Object... info) {
        try {
            URL url = new URL(Constants.HTTP_BASE + info[0]);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            String content = "";
            try {
                Scanner scanner = new Scanner(connection.getInputStream());
                while(scanner.hasNext()) content += scanner.nextLine();
            } catch (Exception e) {
                // error whatever
            } finally {
                connection.disconnect();
            }

            Object data = asyncAction.onBackground(connection.getResponseCode(), content);

            Hashtable<String, Object> hashtable = new Hashtable<>();
            hashtable.put(DATA_KEY, data);

            return hashtable;
        } catch (Exception e) {
            Hashtable<String, Object> hashtable = new Hashtable<>();
            hashtable.put(ERROR_KEY, e);
            return hashtable;
        }
    }

    @Override
    protected void onPostExecute(Hashtable<String, Object> hashtable) {
        AsyncActionCallback callback;
        String getter = DATA_KEY;
        callback = successCallback;
        if(hashtable.containsKey(ERROR_KEY)) {
            callback = errorCallback;
            getter = ERROR_KEY;
        }

        try {
            callback.onAsyncActionComplete(hashtable.get(getter));
        } catch (Exception e) {
            Log.e("WebDownloader", "Fout in de succesCallback", e);
        }
    }

    /* Lerarendownloader */
    public static void getLeraren(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?leraren&sort";

        AsyncCallback AsyncCallback = (status, s) -> {
            switch(status) {
                case 500: throw new Exception("Serverfout, probeer het later nogmaals");
                case 401: throw new Exception("Fout in de aanvraag, probeer de app te updaten");
                case 200: break;
                default: throw new Exception("Onbekende fout, "+status);
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

        new WebDownloader(AsyncCallback, callback, errorCallback).execute(url);
    }

    /* Klassen downloader */
    public static void getKlassen(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?klassen";

        AsyncCallback AsyncCallback = (status, s) -> {
            switch(status) {
                case 200: break;
                case 500: throw new Exception("Serverfout. Probeer het later nogmaals");
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                default: throw new Exception("Onbekende fout, "+status);
            }

            if (s.equals("")) throw new NullPointerException("Geen klassen gevonden!");

            // verder verwerken
            ArrayList<String> klassen = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(s);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject klas = jsonArray.getJSONObject(i);
                klassen.add(klas.getString("klasnaam"));
            }

            return klassen;
        };

        new WebDownloader(AsyncCallback, callback, errorCallback).execute(url);
    }

    /* Lokalen downloader */
    public static void getLokalen(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?lokalen";

        AsyncCallback AsyncCallback = (status, s) -> {
            switch(status) {
                case 200: break;
                case 500: throw new Exception("Serverfout. Probeer het later nogmaals");
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                default: throw new Exception("Onbekende fout, "+status);
            }

            if("".equals(s)) throw new NullPointerException("Geen lokalen gevonden!");

            // verder verwerken
            ArrayList<String> lokalen = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(s);

            for(int i = 0; i < jsonArray.length(); i++) {
                lokalen.add(jsonArray.getString(i));
            }

            return lokalen;
        };

        new WebDownloader(AsyncCallback, callback, errorCallback).execute(url);
    }

    /* Weken downloader */
    public static void getWeken(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        getWeken(false, true, 10, callback, errorCallback);
    }

    public static void getWeken(boolean periode, boolean known, int limit, AsyncActionCallback parentCallback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?weken";
        if(periode) url += "&periode";
        if(known) url += "&known";

        AsyncCallback AsyncCallback = (status, s) -> {
            switch(status) {
                case 200: break;
                case 500: throw new Exception("Serverfout, Probeer het later nogmaals");
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                default: throw new Exception("Onbekende fout, "+status);
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

        new WebDownloader(AsyncCallback, parentCallback, errorCallback).execute(url);
    }

    /* Leerlingen downloader */
    public static void getLeerlingen(AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        String url = "rooster/info?leerlingen&sort";

        AsyncCallback asyncCallback = (status, s) -> {
            switch(status) {
                case 500: throw new Exception("Serverfout, probeer het later nogmaals");
                case 401: throw new Exception("Fout in de aanvraag, probeer de app te updaten");
                case 200: break;
                default: throw new Exception("Onbekende fout, "+status);
            }

            if("".equals(s)) throw new Exception("Geen leerlingen gevonden");

            ArrayList<Klas> klassen = new ArrayList<>();

            JSONArray root = new JSONArray(s);

            for(int i = 0; i < root.length(); i++) {
                JSONObject JSONklas = root.getJSONObject(i);
                Klas klas = new Klas(JSONklas.getString("klas"));

                JSONArray JSONleerlingen = JSONklas.getJSONArray("leerlingen");
                for(int a = 0; a < JSONleerlingen.length(); a++) {
                    JSONObject JSONleerling = JSONleerlingen.getJSONObject(a);
                    Leerling leerling = new Leerling(JSONleerling.getString("llnr"), JSONleerling.getString("naam"));

                    klas.setLeerlingen(leerling);
                }

                klassen.add(klas);
            }

            // Sorteersels
            Collections.sort(klassen, (lhs, rhs) -> lhs.klas.compareToIgnoreCase(rhs.klas));

            return klassen;
        };

        new WebDownloader(asyncCallback, callback, errorCallback).execute(url);
    }

    /* Rooster downloader */
    public static void getRooster(String url, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        AsyncCallback AsyncCallback = (status, content) -> {
            switch(status) {
                case 200: break;
                case 404: throw new Exception(content);
                case 401: throw new Exception("Onverwachte aanvraag. Update de app");
                case 500: throw new Exception("Serverfout. Probeer het later nog eens");
                default: throw new Exception("Onbekende fout, "+status+", "+content);
            }

            return content;
        };

        new WebDownloader(AsyncCallback, callback, errorCallback).execute(url);
    }

    /* PGTV downloader */
    public static void getPGTVRooster(String query, AsyncActionCallback successCallback, AsyncActionCallback errorCallback) {
        String url = "pgtv/"+query;

        AsyncCallback callback = (status, content) -> {
            switch (status) {
                case 200: break;
                case 503: throw new Exception("PGTV niet bereikbaar. Probeer het later nog eens");
                default: throw new Exception("Onbekende fout, "+status+", "+content);
            }

            if("".equals(content)) throw new Exception("PGTV is leeg");
            ArrayList<PGTVPage> pgtv = new ArrayList<>();
            JSONArray data = new JSONArray(content);

            for(int i = 0; i < data.length(); i++) {
                JSONObject JSONdag = data.getJSONObject(i);
                pgtv.add(new PGTVPage(
                        JSONdag.getString("title"),
                        JSONdag.getString("desc")
                ));
            }

            return pgtv;
        };

        new WebDownloader(callback, successCallback, errorCallback).execute(url);
    }



    private interface AsyncCallback {
        public Object onBackground(int statusCode, String result) throws Exception;
    }

}
