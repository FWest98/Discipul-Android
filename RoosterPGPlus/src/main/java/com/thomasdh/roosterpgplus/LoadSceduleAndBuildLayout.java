package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Created by Thomas on 27-11-13.
 */
public class LoadSceduleAndBuildLayout extends AsyncTask<String, Void, String> {

    public Context context;
    public ViewPager viewPager;
    public View rootLayout;

    public LoadSceduleAndBuildLayout(Context context, ViewPager viewPager, View rootLayout) {
        this.context = context;
        this.viewPager = viewPager;
        this.rootLayout = rootLayout;
    }

    public static String getTijden(int x) {
        switch (x) {
            case 0:
                return "8:30 - 9:30";
            case 1:
                return "9:30 - 10:30";
            case 2:
                return "10:50 - 11:50";
            case 3:
                return "11:50 - 12:50";
            case 4:
                return "13:20 - 14:20";
            case 5:
                return "14:20 - 15:20";
            case 6:
                return "15:30 - 16:30";
        }
        return "Foute tijd";
    }

    public static String getDayOfWeek(int x) {
        switch (x) {
            case Calendar.SUNDAY:
                return "zondag";
            case Calendar.MONDAY:
                return "maandag";
            case Calendar.TUESDAY:
                return "dinsdag";
            case Calendar.WEDNESDAY:
                return "woensdag";
            case Calendar.THURSDAY:
                return "donderdag";
            case Calendar.FRIDAY:
                return "vrijdag";
            case Calendar.SATURDAY:
                return "zaterdag";

        }
        return null;
    }

    @Override
    protected String doInBackground(String... params) {

        //Controleer of het apparaat een internetverbinding heeft
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            String JSON = laadViaInternet();
            slaOp(JSON);
            Log.d(getClass().getSimpleName(), "Loaded from internet");
            if (JSON == null) {
                Log.d(getClass().getSimpleName(), "The string is null");
            }
            return JSON;
        }

        String JSON = laadInternal();
        Log.d(getClass().getSimpleName(), "Loaded without internet");
        return JSON;
    }

    @Override
    protected void onPostExecute(String string) {
        boolean weekView = context.getResources().getBoolean(R.bool.big_screen);

        Log.d(getClass().getSimpleName(), "The string is: " + string);
        viewPager.setVisibility(View.VISIBLE);
        RelativeLayout progressBar = (RelativeLayout) rootLayout.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        if (string != null) {
            if (string.startsWith("error:")) {
                Toast.makeText(context, string.substring(6), Toast.LENGTH_LONG).show();
            } else {
                try {
                    LinearLayout weekLinearLayout = null;
                    if (weekView) {
                        weekLinearLayout = new LinearLayout(context);
                        weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                    }
                    JSONObject weekArray = new JSONObject(string);
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    boolean rightWeek = true;
                    roostermaker:
                    for (int day = 2; day < 7; day++) {

                        JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day));
                        View dagView;
                        LinearLayout ll;
                        if (!weekView) {
                            dagView = inflater.inflate(R.layout.rooster_dag, null);
                            ll = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);
                            ((TextView) ll.findViewById(R.id.weekdagnaam)).setText(getDayOfWeek(day));
                        } else {
                            dagView = new LinearLayout(context);
                            ll = (LinearLayout) dagView;
                            ll.setOrientation(LinearLayout.VERTICAL);
                        }

                        //Ga langs alle uren
                        for (int y = 0; y < 7; y++) {
                            if (dagArray.has(String.valueOf(y + 1))) {
                                JSONObject uurObject = dagArray.getJSONArray(String.valueOf(y + 1)).getJSONObject(0);
                                View uur = null;
                                if (uurObject.getInt("week") != Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + 1) {
                                    rightWeek = false;
                                    Log.d(getClass().getSimpleName(), "The wanted week is " + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + 1) + ", but the found week is " + uurObject.getInt("week"));
                                    break roostermaker;
                                }
                                if (uurObject.getString("vervallen").equals("1")) {
                                    uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
                                    uur.setMinimumHeight((int) convertDPToPX(80, context));
                                    ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(uurObject.getString("vak") + " valt uit");
                                } else {
                                    uur = inflater.inflate(R.layout.rooster_uur, null);
                                    ((TextView) uur.findViewById(R.id.rooster_vak)).setText(uurObject.getString("vak"));
                                    ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(uurObject.getString("leraar"));
                                    ((TextView) uur.findViewById(R.id.rooster_lokaal)).setText(uurObject.getString("lokaal"));
                                    ((TextView) uur.findViewById(R.id.rooster_tijden)).setText(getTijden(y));
                                }
                                uur.setMinimumHeight((int) convertDPToPX(80, context));
                                ll.addView(uur);
                            } else {
                                View vrij = inflater.inflate(R.layout.rooster_tussenuur, null);
                                vrij.setMinimumHeight((int) convertDPToPX(80, context));
                                ll.addView(vrij);
                            }
                        }
                        if (weekView) {
                            ll.setPadding((int) convertDPToPX(3, context), (int) convertDPToPX(3, context), (int) convertDPToPX(3, context), (int) convertDPToPX(3, context));
                            dagView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                            weekLinearLayout.addView(dagView);
                        } else {
                            ll.setPadding((int) convertDPToPX(10, context), (int) convertDPToPX(10, context), (int) convertDPToPX(10, context), (int) convertDPToPX(10, context));
                            ((MyPagerAdapter) viewPager.getAdapter()).addView(dagView);
                        }
                    }
                    if (!rightWeek) {
                        TextView tv = new TextView(context);
                        tv.setText("Helaas, de app kon geen rooster laden.");
                        viewPager.addView(tv);
                        viewPager.getAdapter().notifyDataSetChanged();
                    } else {
                        if (weekView) {
                            weekLinearLayout.invalidate();
                            ScrollView weekScrollView = new ScrollView(context);
                            weekScrollView.addView(weekLinearLayout);
                            ((MyPagerAdapter) viewPager.getAdapter()).addView(weekScrollView);
                        }
                        viewPager.getAdapter().notifyDataSetChanged();
                        viewPager.setCurrentItem(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            TextView tv = new TextView(context);
            tv.setText("Helaas, de app kon geen rooster laden.");
            viewPager.addView(tv);
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    float convertDPToPX(float pixel, Context c) {
        Resources r = c.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel, r.getDisplayMetrics());
        return px;
    }

    String laadInternal() {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("week", "error:Er is nog geen rooster in het geheugen opgeslagen");
    }

    void slaOp(String JSON) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("week", JSON).commit();
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
