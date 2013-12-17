package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.thomasdh.roosterpgplus.roosterdata.RoosterWeek;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Created by Thomas on 27-11-13.
 */
public class RoosterDownloader extends AsyncTask<String, Void, String> {

    public WeakReference<Context> context;
    public WeakReference<View> rootView;
    public boolean forceReload;
    public MenuItem menuItem;
    MainActivity.PlaceholderFragment.Type type;
    private int week;
    private ViewPager viewPager;
    private String klas;
    private String docent;


    //Voor docenten
    public RoosterDownloader(Context context, View rooView, boolean forceReload, MenuItem menuItem, int week, String klas, MainActivity.PlaceholderFragment.Type type) {
        this(context, rooView, forceReload, menuItem, week);
        this.type = type;
        this.klas = klas;
    }

    public RoosterDownloader(Context context, View rootView, boolean forceReload, MenuItem menuItem, int week) {
        this.context = new WeakReference<Context>(context);
        this.rootView = new WeakReference<View>(rootView);
        this.forceReload = forceReload;
        this.menuItem = menuItem;
        this.week = week;
        this.type = MainActivity.PlaceholderFragment.Type.PERSOONLIJK_ROOSTER;

        if (this.menuItem != null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            MenuItemCompat.setActionView(this.menuItem, layoutInflater.inflate(R.layout.actionbar_refresh_progress, null));
        } else {
            Log.w(getClass().getSimpleName(), "The MenuItem is null.");
        }
    }

    public RoosterDownloader(Context context, boolean forceReload, int week) {
        this.type = MainActivity.PlaceholderFragment.Type.PERSOONLIJK_ROOSTER;
        this.context = new WeakReference<Context>(context);
        this.forceReload = forceReload;
        this.week = week;
    }

    @Override
    protected String doInBackground(String... params) {

        //Controleer of het apparaat een internetverbinding heeft
        ConnectivityManager cm = (ConnectivityManager) context.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            if ((itIsTimeToReload() || forceReload)) {
                Log.d(getClass().getSimpleName(), "De app gaat de string van het internet downloaden.");
                String JSON = laadViaInternet();
                Log.d(getClass().getSimpleName(), "Loaded from internet");
                if (JSON == null) {
                    Log.d(getClass().getSimpleName(), "The string is null");
                }
                return JSON;
            }
            Log.d(getClass().getSimpleName(), "Het rooster is uit het geheugen geladen");
            return null;
        }
        return "error:Geen internetverbinding";
    }

    boolean itIsTimeToReload() {
        return PreferenceManager.getDefaultSharedPreferences(context.get()).getLong("lastRefreshTime", 0) +
                context.get().getResources().getInteger(R.integer.min_refresh_wait_time) < System.currentTimeMillis();
    }

    @Override
    protected void onPostExecute(String string) {
        if (this.menuItem != null) {
            MenuItemCompat.setActionView(this.menuItem, null);
        } else {
            Log.w(getClass().getSimpleName(), "The MenuItem is null on PostExecute.");
        }
        if (string != null) {
            if (string.startsWith("error:")) {
                Toast.makeText(context.get(), string.substring(6), Toast.LENGTH_SHORT).show();
            } else {
                RoosterWeek roosterWeek = new RoosterWeek(string);
                if (type == MainActivity.PlaceholderFragment.Type.PERSOONLIJK_ROOSTER) {
                    roosterWeek.slaOp(context.get());
                }
                if (context != null && rootView.get() != null) {
                    if (type == MainActivity.PlaceholderFragment.Type.PERSOONLIJK_ROOSTER)
                        new RoosterBuilder(context.get(), (ViewPager) (rootView.get()).findViewById(R.id.viewPager), rootView.get(), week).buildLayout(new RoosterWeek(string));
                    if (type == MainActivity.PlaceholderFragment.Type.DOCENTENROOSTER)
                        new RoosterBuilder(context.get(), (ViewPager) (rootView.get()).findViewById(R.id.viewPager_docent), rootView.get(), week).buildLayout(new RoosterWeek(string));
                    if (type == MainActivity.PlaceholderFragment.Type.KLASROOSTER)
                        new RoosterBuilder(context.get(), (ViewPager) (rootView.get()).findViewById(R.id.viewPager_leerling), rootView.get(), week).buildLayout(new RoosterWeek(string));
                }
            }
        }
    }

    String laadViaInternet() {

        String apikey = PreferenceManager.getDefaultSharedPreferences(context.get()).getString("key", null);
        HttpClient httpclient = new DefaultHttpClient();
        if (week == -1) {
            week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        }
        HttpGet httpGet = null;
        if (type == MainActivity.PlaceholderFragment.Type.PERSOONLIJK_ROOSTER) {
            httpGet = new HttpGet(Settings.API_Base_URL + "rooster/?key=" + apikey + "&week=" + week);
        } else if (type == MainActivity.PlaceholderFragment.Type.DOCENTENROOSTER) {
            httpGet = new HttpGet(Settings.API_Base_URL + "rooster/?leraar=" + klas + "&week=" + week);
        } else if (type == MainActivity.PlaceholderFragment.Type.KLASROOSTER) {
            httpGet = new HttpGet(Settings.API_Base_URL + "rooster/?klas=" + klas + "&week=" + week);
        }

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
                PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putLong("lastRefreshTime", System.currentTimeMillis()).commit();

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
