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
import android.widget.Toast;

import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.RoosterWeek;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Scanner;

import static com.thomasdh.roosterpgplus.R.integer.min_refresh_wait_time;

/**
 * Created by Thomas on 27-11-13.
 */
@Deprecated
public class RoosterDownloaderOld extends AsyncTask<String, Exception, String> {

    private final WeakReference<Context> context;
    private final boolean forceReload;
    private final WeakReference<View> rootView;
    private final MenuItem menuItem;
    private RoosterViewFragment.Type type;
    private int week;
    private String klas;


    //Voor docenten
    public RoosterDownloaderOld(Context context, View rooView, boolean forceReload, MenuItem menuItem, int week, String klas, RoosterViewFragment.Type type) {
        this(context, rooView, forceReload, menuItem, week);
        this.type = type;
        this.klas = klas;
    }

    public RoosterDownloaderOld(Context context, View rootView, boolean forceReload, MenuItem menuItem, int week) {
        this.context = new WeakReference<Context>(context);
        this.rootView = new WeakReference<View>(rootView);
        this.forceReload = forceReload;
        this.menuItem = menuItem;
        this.week = week;
        type = RoosterViewFragment.Type.PERSOONLIJK_ROOSTER;

        if (this.menuItem != null) {
            MenuItemCompat.expandActionView(menuItem);
        } else {
            Log.w(getClass().getSimpleName(), "The MenuItem is null.");
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            //Controleer of het apparaat een internetverbinding heeft
            ConnectivityManager cm = (ConnectivityManager) context.get().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                if (itIsTimeToReload() || forceReload) {
                    Log.d(getClass().getSimpleName(), "De app gaat de string van het internet downloaden.");
                    String JSON = laadViaInternet();
                    Log.d(getClass().getSimpleName(), "Loaded from internet");
                    if (JSON == null) {
                        Log.d(getClass().getSimpleName(), "The string is null");
                    }
                    return JSON;
                }
                return null;
            }
            throw new IOException("Geen internetverbinding");
        } catch (Exception e) {
            publishProgress(e);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Exception... e) {
        if (context.get() != null) {
            MenuItemCompat.setActionView(menuItem, null);
            ExceptionHandler.handleException(e[0], context.get(), "Fout bij het downloaden van het rooster", "RoosterDownloader", ExceptionHandler.HandleType.SILENT);

            // Maak een leeg rooster bij een docenten- of klasrooster
            if (type == RoosterViewFragment.Type.DOCENTENROOSTER)
                new RoosterBuilderOld(context.get(), (ViewPager) rootView.get().findViewById(R.id.viewPager_docent), week, type).buildLayout(new RoosterWeek(null, context.get()));
            if (type == RoosterViewFragment.Type.KLASROOSTER)
                new RoosterBuilderOld(context.get(), (ViewPager) rootView.get().findViewById(R.id.viewPager_klas), week, type, klas).buildLayout(new RoosterWeek(null, context.get()));
        }
    }

    boolean itIsTimeToReload() {
        return PreferenceManager.getDefaultSharedPreferences(context.get()).getLong("lastRefreshTime", 0) +
                context.get().getResources().getInteger(min_refresh_wait_time) < System.currentTimeMillis();
    }

    @Override
    protected void onPostExecute(String string) {
        if (menuItem != null) {
            MenuItemCompat.setActionView(menuItem, null);
        } else {
            Log.w(getClass().getSimpleName(), "The MenuItem is null on PostExecute.");
        }
        if (string != null) {
            if (string.startsWith("error:")) {
                Toast.makeText(context.get(), string.substring(6), Toast.LENGTH_SHORT).show();
            //} else if(string.equals("[]")) {
//                RoosterWeek roosterWeek = new RoosterWeek(null, context.get());

            } else {
                if(string.equals("[]")) {
                    string = null;
                }
                RoosterWeek roosterWeek = new RoosterWeek(string, context.get());
                if (type == RoosterViewFragment.Type.PERSOONLIJK_ROOSTER) {
                    roosterWeek.slaOp(context.get(), week);
                }
                if (context != null && rootView.get() != null) {
                    if (type == RoosterViewFragment.Type.PERSOONLIJK_ROOSTER)
                        new RoosterBuilderOld(context.get(), (ViewPager) rootView.get().findViewById(R.id.viewPager), week, type).buildLayout(new RoosterWeek(string, context.get()));
                    if (type == RoosterViewFragment.Type.DOCENTENROOSTER)
                        new RoosterBuilderOld(context.get(), (ViewPager) rootView.get().findViewById(R.id.viewPager_docent), week, type).buildLayout(new RoosterWeek(string, context.get()));
                    if (type == RoosterViewFragment.Type.KLASROOSTER)
                        new RoosterBuilderOld(context.get(), (ViewPager) rootView.get().findViewById(R.id.viewPager_klas), week, type, klas).buildLayout(new RoosterWeek(string, context.get()));
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
        if (type == RoosterViewFragment.Type.PERSOONLIJK_ROOSTER) {
            httpGet = new HttpGet(Settings.API_Base_URL + "rooster/?key=" + apikey + "&week=" + week);
        } else if (type == RoosterViewFragment.Type.DOCENTENROOSTER) {
            httpGet = new HttpGet(Settings.API_Base_URL + "rooster/?leraar=" + klas + "&week=" + week);
        } else if (type == RoosterViewFragment.Type.KLASROOSTER) {
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
            if (context.get() != null) {
                ExceptionHandler.handleException(e, context.get(), "Kon het rooster niet downloaden", getClass().getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
            }
        }
        return null;
    }
}
