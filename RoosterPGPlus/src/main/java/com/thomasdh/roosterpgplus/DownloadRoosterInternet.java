package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
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
public class DownloadRoosterInternet extends AsyncTask<String, Void, String> {

    public Context context;
    public View rootView;
    public boolean forceReload;
    public MenuItem menuItem;

    public DownloadRoosterInternet(Context context, View rootView, boolean forceReload, MenuItem menuItem) {
        this.context = context;
        this.rootView = rootView;
        this.forceReload = forceReload;
        this.menuItem = menuItem;
    }

    @Override
    protected String doInBackground(String... params) {

        //Controleer of het apparaat een internetverbinding heeft
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting() && (itIsTimeToReload() || forceReload)) {
            String JSON = laadViaInternet();
            slaOp(JSON, Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            Log.d(getClass().getSimpleName(), "Loaded from internet");
            if (JSON == null) {
                Log.d(getClass().getSimpleName(), "The string is null");
            }
            return JSON;
        }
        return null;
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
        if (string != null) {
            new LayoutBuilder(context, (ViewPager) rootView.findViewById(R.id.viewPager), rootView).buildLayout(string);
        }
        if (menuItem != null) {
            MenuItemCompat.setActionView(menuItem, null);
        }
    }

    void slaOp(String JSON, int weeknr) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("week" + (weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks)), JSON).commit();
    }

    String laadViaInternet() {
        if (menuItem != null) {
            // TODO: Fix dit. Dit zorgt voor een "Only the original thread that created a view hierarchy can touch its views
            //MenuItemCompat.setActionView(menuItem, R.layout.actionbar_refresh_progress);
        }

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
