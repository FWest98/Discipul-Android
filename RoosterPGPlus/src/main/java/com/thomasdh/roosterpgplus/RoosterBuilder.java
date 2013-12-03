package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by Thomas on 2-12-13.
 */
public class RoosterBuilder {

    public Context context;
    public ViewPager viewPager;
    public View rootView;

    public RoosterBuilder(Context context, ViewPager viewPager, View rootView) {
        this.context = context;
        this.viewPager = viewPager;
        this.rootView = rootView;
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

    public void buildLayout(String roosterJSON) {
        viewPager.setAdapter(new MyPagerAdapter());

        boolean weekView = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("weekview", context.getResources().getBoolean(R.bool.big_screen));

        Log.d(getClass().getSimpleName(), "The string is: " + roosterJSON);


        if (roosterJSON != null) {
            if (roosterJSON.startsWith("error:")) {
                Toast.makeText(context, roosterJSON.substring(6), Toast.LENGTH_LONG).show();
            } else {
                try {
                    LinearLayout weekLinearLayout = null;
                    if (weekView) {
                        weekLinearLayout = new LinearLayout(context);
                        weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                        int paddingLeftRight = (int) convertDPToPX(10, context);
                        weekLinearLayout.setPadding(paddingLeftRight, 0, paddingLeftRight, 0);
                    }
                    Log.e(this.getClass().getName(), "REPSONSE: " + roosterJSON);
                    JSONObject weekArray = new JSONObject(roosterJSON);
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    for (int day = 2; day < 7; day++) {

                        JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day));
                        View dagView;
                        LinearLayout ll;
                        dagView = inflater.inflate(R.layout.rooster_dag, null);
                        ll = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);

                        TextView dagTextView = ((TextView) dagView.findViewById(R.id.weekdagnaam));
                        dagTextView.setText(getDayOfWeek(day));

                        //Ga langs alle uren
                        for (int y = 0; y < 7; y++) {
                            if (dagArray.has(String.valueOf(y + 1))) {
                                JSONObject uurObject = dagArray.getJSONArray(String.valueOf(y + 1)).getJSONObject(0);
                                Lesuur lesuur = new Lesuur(uurObject);
                                View uur;
                                if (uurObject.getString("vervallen").equals("1")) {
                                    uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
                                    uur.setMinimumHeight((int) convertDPToPX(80, context));
                                    ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(lesuur.vak + " valt uit");
                                } else {
                                    if (uurObject.getString("verandering").equals("1")) {
                                        uur = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
                                    } else {
                                        uur = inflater.inflate(R.layout.rooster_uur, null);
                                    }
                                    ((TextView) uur.findViewById(R.id.rooster_vak)).setText(lesuur.vak);
                                    ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.leraar);
                                    ((TextView) uur.findViewById(R.id.rooster_lokaal)).setText(lesuur.lokaal);
                                    ((TextView) uur.findViewById(R.id.rooster_tijden)).setText(getTijden(y));
                                }
                                if (y == 6) {
                                    uur.setBackgroundResource(R.drawable.basic_rect);
                                }
                                uur.setMinimumHeight((int) convertDPToPX(81, context));
                                ll.addView(uur);
                            } else {
                                View vrij = inflater.inflate(R.layout.rooster_tussenuur, null);
                                vrij.setMinimumHeight((int) convertDPToPX(80, context));
                                if (y == 6) {
                                    vrij.setBackgroundResource(R.drawable.basic_rect);
                                }
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
                    if (weekView) {
                        weekLinearLayout.invalidate();
                        ScrollView weekScrollView = new ScrollView(context);
                        weekScrollView.addView(weekLinearLayout);
                        ((MyPagerAdapter) viewPager.getAdapter()).addView(weekScrollView);
                    }
                    viewPager.getAdapter().notifyDataSetChanged();
                    if (!weekView)
                        viewPager.setCurrentItem(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
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
}
