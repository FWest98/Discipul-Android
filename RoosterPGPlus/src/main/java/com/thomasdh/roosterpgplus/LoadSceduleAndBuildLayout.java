package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Created by Thomas on 27-11-13.
 */
public class LoadSceduleAndBuildLayout extends AsyncTask<String, Void, String> {

    public Context context;
    public ViewPager viewPager;

    public LoadSceduleAndBuildLayout(Context context, ViewPager viewPager) {
        this.context = context;
        this.viewPager = viewPager;
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

    @Override
    protected void onPostExecute(String string) {
        System.out.println("!!!Lessen: " + string);
        if (string.startsWith("error:")) {
            Toast.makeText(context, string.substring(6), Toast.LENGTH_LONG).show();
        } else {
            try {
                JSONObject weekArray = new JSONObject(string);

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                for (int day = 2; day < 7; day++) {

                    JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day));
                    View dagView = inflater.inflate(R.layout.rooster_dag, null);
                    LinearLayout ll = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);


                    TextView t = new TextView(context);
                    t.setText(getDayOfWeek(day));
                    ll.addView(t);

                    for (int y = 0; y < 7; y++) {
                        if (dagArray.has(String.valueOf(y + 1))) {
                            JSONObject uurObject = dagArray.getJSONArray(String.valueOf(y + 1)).getJSONObject(0);
                            View uur = null;
                            if (uurObject.getString("vervallen").equals("1")) {
                                uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
                                ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(uurObject.getString("vak") + " valt uit");
                            } else {
                                uur = inflater.inflate(R.layout.rooster_uur, null);
                                ((TextView) uur.findViewById(R.id.rooster_vak)).setText(uurObject.getString("vak"));
                                ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(uurObject.getString("leraar"));
                                ((TextView) uur.findViewById(R.id.rooster_lokaal)).setText(uurObject.getString("lokaal"));
                                ((TextView) uur.findViewById(R.id.rooster_tijden)).setText(getTijden(y));
                            }
                            ll.addView(uur);
                        } else {
                            View vrij = inflater.inflate(R.layout.rooster_tussenuur, null);
                            ll.addView(vrij);
                        }
                    }
                    ((MyPagerAdapter) viewPager.getAdapter()).addView(dagView);
                }
                viewPager.getAdapter().notifyDataSetChanged();
                viewPager.setCurrentItem(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
