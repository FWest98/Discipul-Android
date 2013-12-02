package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Created by Thomas on 27-11-13.
 */
public class LoadSceduleAndBuildLayout extends AsyncTask<String, Void, String> {

    public Context context;
    public ViewPager viewPager;
    public View rootView;
    public boolean forceReload;

    public LoadSceduleAndBuildLayout(Context context, ViewPager viewPager, View rootView, boolean forceReload) {
        this.context = context;
        this.viewPager = viewPager;
        this.rootView = rootView;
        this.forceReload = forceReload;
    }

    @Override
    protected String doInBackground(String... params) {

        //Controleer of het apparaat een internetverbinding heeft
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        String JSON = laadInternal(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));

        if (!JSON.contains("\"week: \" \"" + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + 1) + "\"")) {
            Log.d(getClass().getSimpleName(), "The wanted week is " + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + 1) + ", but the found week is something different.");
        }

        if (netInfo != null && netInfo.isConnectedOrConnecting() && (itIsTimeToReload() || forceReload)) {
            JSON = laadViaInternet();
            slaOp(JSON, Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            Log.d(getClass().getSimpleName(), "Loaded from internet");
            if (JSON == null) {
                Log.d(getClass().getSimpleName(), "The string is null");
            }
            return JSON;
        }


        Log.d(getClass().getSimpleName(), "Loaded without internet");
        return JSON;
    }

    boolean itIsTimeToReload() {
        if (PreferenceManager.getDefaultSharedPreferences(context).getLong("lastRefreshTime", 0) +
                context.getResources().getInteger(R.integer.min_refresh_wait_time) < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onPostExecute(String string) {
        new LayoutBuilder(context, viewPager, rootView).buildLayout(string);
    }

    String laadInternal(int weeknr) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("week" + weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks), "error:Er is nog geen rooster in het geheugen opgeslagen");
    }

    void slaOp(String JSON, int weeknr) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("week" + weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks), JSON).commit();
    }

    String laadViaInternet() {
        String apikey = PreferenceManager.getDefaultSharedPreferences(context).getString("key", null);
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://rooster.fwest98.nl/api/rooster/?key=" + apikey);

        // Execute HTTP Post Request
        try {
            HttpResponse response = httpclient.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();

            if (status == 400) {
                return "error:Missende parameters";
            } else if (status == 401) {
                return "error:Account bestaat niet";
            } else if (status == 500) {
                return "error:Serverfout";
            } else if (status == 200) {
                String s = "";
                Scanner sc = new Scanner(response.getEntity().getContent());
                while (sc.hasNext()) {
                    s += sc.nextLine();
                }

                // Sla de tijd op wanneer het rooster voor het laatst gedownload is.
                PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("lastRefreshTime", System.currentTimeMillis()).commit();

                return s;
            } else {
                return "error:Onbekende status: " + status;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
