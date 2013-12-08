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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Thomas on 2-12-13.
 */
public class RoosterBuilder {

    public WeakReference<Context> context;
    public WeakReference<ViewPager> viewPager;
    public WeakReference<View> rootView;
    private int week;


    public RoosterBuilder(Context context, ViewPager viewPager, View rootView, int week) {
        this.context = new WeakReference<Context>(context);
        this.viewPager = new WeakReference<ViewPager>(viewPager);
        this.rootView = new WeakReference<View>(rootView);
        this.week = week;
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
        viewPager.get().setAdapter(new MyPagerAdapter());

        boolean weekView = PreferenceManager.getDefaultSharedPreferences(context.get()).getBoolean("weekview", context.get().getResources().getBoolean(R.bool.big_screen));

        Log.d(getClass().getSimpleName(), "The string is: " + roosterJSON);


        if (roosterJSON != null) {
            if (roosterJSON.startsWith("error:")) {
                Toast.makeText(context.get(), roosterJSON.substring(6), Toast.LENGTH_LONG).show();
            } else {
                try {
                    LinearLayout weekLinearLayout = null;
                    if (weekView) {
                        weekLinearLayout = new LinearLayout(context.get());
                        weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                        int paddingLeftRight = (int) convertDPToPX(10, context.get());
                        weekLinearLayout.setPadding(paddingLeftRight, 0, paddingLeftRight, 0);
                    }
                    Log.e(this.getClass().getName(), "RESPONSE: " + roosterJSON);
                    JSONObject weekArray = new JSONObject(roosterJSON);
                    LayoutInflater inflater = (LayoutInflater) context.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    for (int day = 2; day < 7; day++) {

                        JSONObject dagArray = weekArray.getJSONObject(getDayOfWeek(day));
                        View dagView;
                        LinearLayout ll;
                        dagView = inflater.inflate(R.layout.rooster_dag, null);
                        ll = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);

                        TextView dagTextView = ((TextView) dagView.findViewById(R.id.weekdagnaam));
                        dagTextView.setText(getDayOfWeek(day));

                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
                        Calendar now = Calendar.getInstance();
                        Calendar date = Calendar.getInstance();
                        date.setTime(new Date());

                        //Als de huidige week hoger dan 30 is (Schooljaar x/x+1)
                        if (now.get(Calendar.WEEK_OF_YEAR) > 30) {
                            // Als de gevraagde week lager dan dertig is
                            if (week < 30) {
                                // ga naar het volgende jaar
                                date.set(Calendar.YEAR, now.get(Calendar.YEAR) + 1);
                            } else {
                                date.set(Calendar.YEAR, now.get(Calendar.YEAR));
                            }
                            //Als de huidige week lager dan 30 is (Schooljaar x-1/x)
                        } else {
                            if (week < 30) {
                                date.set(Calendar.YEAR, now.get(Calendar.YEAR));
                            } else {
                                // Als de gevraagde week hoger dan 30 is
                                date.set(Calendar.YEAR, now.get(Calendar.YEAR) - 1);
                            }
                        }

                        date.set(Calendar.DAY_OF_WEEK, day);
                        date.set(Calendar.WEEK_OF_YEAR, week);
                        ((TextView) dagView.findViewById(R.id.weekdagdatum)).setText(dateFormat.format(date.getTime()));

                        Log.e("OPHALEN", dateFormat.format(date.getTime()));

                        //Ga langs alle uren
                        for (int y = 0; y < 7; y++) {
                            if (dagArray.has(String.valueOf(y + 1))) {
                                JSONArray uurArray = dagArray.getJSONArray(String.valueOf(y + 1));
                                JSONObject uurObject = uurArray.getJSONObject(0);
                                Lesuur lesuur = new Lesuur(uurObject);
                                JoinableList vervallenVakken = new JoinableList();

                                // Bepalen welk uur van de (eventueel meerdere) hij moet nemen
                                if (lesuur.vervallen && uurArray.length() > 1) {
                                    int uurCounter = 1;
                                    while (lesuur.vervallen && uurCounter < uurArray.length()) { // vervallen les, en er moet nog één beschikbaar zijn
                                        vervallenVakken.add(lesuur.vak);
                                        uurObject = uurArray.getJSONObject(uurCounter);
                                        lesuur = new Lesuur(uurObject);
                                        uurCounter++;
                                    }
                                }


                                View uur;
                                boolean paddingRight = true;
                                if (uurObject.getString("vervallen").equals("1")) {
                                    uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
                                    uur.setMinimumHeight((int) convertDPToPX(80, context.get()));
                                    if (vervallenVakken.size() > 0) {
                                        vervallenVakken.add(lesuur.vak);
                                        String vakken = vervallenVakken.join(" & ");
                                        ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(vakken + " vallen uit");
                                    } else {
                                        ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(lesuur.vak + " valt uit");
                                    }
                                } else {
                                    if (uurObject.getString("verandering").equals("1")) {
                                        paddingRight = false;
                                        uur = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
                                    } else {
                                        paddingRight = false;
                                        uur = inflater.inflate(R.layout.rooster_uur, null);
                                    }
                                    ((TextView) uur.findViewById(R.id.rooster_vak)).setText(lesuur.vak);
                                    if (lesuur.leraar2 == null || lesuur.leraar2.equals("")) {
                                        ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.leraar);
                                    } else {
                                        ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.leraar + " & " + lesuur.leraar2);
                                    }
                                    ((TextView) uur.findViewById(R.id.rooster_lokaal)).setText(lesuur.lokaal);
                                    ((TextView) uur.findViewById(R.id.rooster_tijden)).setText(getTijden(y));
                                }
                                if (y == 6) {
                                    uur.setBackgroundResource(R.drawable.basic_rect);
                                    if (!paddingRight) {
                                        uur.setPadding((int) convertDPToPX(7, context.get()), (int) convertDPToPX(3, context.get()), (int) convertDPToPX(10, context.get()), (int) convertDPToPX(0, context.get()));
                                    }
                                }
                                uur.setMinimumHeight((int) convertDPToPX(81, context.get()));
                                ll.addView(uur);
                            } else {
                                View vrij = inflater.inflate(R.layout.rooster_tussenuur, null);
                                vrij.setMinimumHeight((int) convertDPToPX(80, context.get()));
                                if (y == 6) {
                                    vrij.setBackgroundResource(R.drawable.basic_rect);
                                }
                                ll.addView(vrij);
                            }
                        }

                        if (weekView) {
                            ll.setPadding((int) convertDPToPX(3, context.get()), (int) convertDPToPX(3, context.get()), (int) convertDPToPX(3, context.get()), (int) convertDPToPX(3, context.get()));
                            dagView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                            weekLinearLayout.addView(dagView);
                        } else {

                            TextView dataTextView = new TextView(context.get());
                            Date laatstGeupdate = new Date(PreferenceManager.getDefaultSharedPreferences(context.get()).getLong("lastRefreshTime", 0));
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                            dataTextView.setText("Laatst geupdate: " + simpleDateFormat.format(laatstGeupdate));
                            ll.addView(dataTextView);

                            PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("geselecteerdeweek", week).commit();

                            ll.setPadding((int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()));
                            ((MyPagerAdapter) viewPager.get().getAdapter()).addView(dagView);
                        }
                    }
                    if (weekView) {
                        LinearLayout completeLinearLayout = new LinearLayout(context.get());
                        completeLinearLayout.setOrientation(LinearLayout.VERTICAL);
                        completeLinearLayout.addView(weekLinearLayout);

                        // Maak het laatstgeupdate vak
                        TextView dataTextView = new TextView(context.get());
                        Date laatstGeupdate = new Date(PreferenceManager.getDefaultSharedPreferences(context.get()).getLong("lastRefreshTime", 0));
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        dataTextView.setText("Laatst geupdate: " + simpleDateFormat.format(laatstGeupdate));
                        dataTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        dataTextView.setPadding((int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()), (int) convertDPToPX(10, context.get()));

                        completeLinearLayout.addView(dataTextView);

                        ScrollView weekScrollView = new ScrollView(context.get());
                        weekScrollView.addView(completeLinearLayout);
                        ((MyPagerAdapter) viewPager.get().getAdapter()).addView(weekScrollView);
                    }
                    viewPager.get().getAdapter().notifyDataSetChanged();
                    if (!weekView)
                        // Ga naar de gewilde dag
                        if (PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("geselecteerdeweek", -1) == week) {
                            Log.d(getClass().getSimpleName(), "De geselecteerde week is niet veranderd, de dag blijft " + PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("dagvandeweeklaatst", 0));
                            viewPager.get().setCurrentItem(PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("dagvandeweeklaatst", 0));
                        } else if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) == week) {
                            Log.d(getClass().getSimpleName(), "De geselecteerde week is veranderd, en is deze week, de dag wordt " + (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2));
                            viewPager.get().setCurrentItem(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
                            PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("dagvandeweek", Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2).commit();
                        } else {
                            Log.d(getClass().getSimpleName(), "De geselecteerde week is veranderd en is niet deze week, de dag wordt maandag.");
                            viewPager.get().setCurrentItem(0);
                            PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("dagvandeweek", 0).commit();
                        }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                viewPager.get().setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int i, float v, int i2) {
                    }

                    @Override
                    public void onPageSelected(int i) {
                        //De laatst geselecteerde week wordt opgeslagen
                        PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("dagvandeweeklaatst", i).commit();
                    }

                    @Override
                    public void onPageScrollStateChanged(int i) {

                    }
                });
            }
        } else {
            TextView tv = new TextView(context.get());
            tv.setText("Helaas, de app kon geen rooster laden.");
            viewPager.get().addView(tv);
            viewPager.get().getAdapter().notifyDataSetChanged();
        }
    }

    float convertDPToPX(float pixel, Context c) {
        Resources r = c.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel, r.getDisplayMetrics());
        return px;
    }
}
