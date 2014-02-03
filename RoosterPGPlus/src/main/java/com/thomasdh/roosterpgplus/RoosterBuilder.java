package com.thomasdh.roosterpgplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.adapters.MyPagerAdapter;
import com.thomasdh.roosterpgplus.roosterdata.RoosterWeek;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by Thomas on 2-12-13.
 */
class RoosterBuilder {

    private final WeakReference<Context> context;
    private final WeakReference<ViewPager> viewPager;
    private final int week;
    private MainActivity.PlaceholderFragment.Type type;


    public RoosterBuilder(Context context, ViewPager viewPager, View rootView, int week, MainActivity.PlaceholderFragment.Type type) {
        this.context = new WeakReference<Context>(context);
        this.viewPager = new WeakReference<ViewPager>(viewPager);
        WeakReference<View> rootView1 = new WeakReference<View>(rootView);
        this.week = week;
        this.type = type;
    }

    private static String getTijden(int x) {
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

    private static String getDayOfWeek(int x) {
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

    public void buildLayout(RoosterWeek roosterWeek) {
        if (viewPager.get() != null) {
            if (viewPager.get().getAdapter() == null) {
                viewPager.get().setAdapter(new MyPagerAdapter());
            }
        }
        boolean weekView = PreferenceManager.getDefaultSharedPreferences(context.get()).getBoolean("weekview", context.get().getResources().getBoolean(R.bool.big_screen));

        if (roosterWeek != null) {

            // Verwijder alle bestaande items
            // ((MyPagerAdapter) viewPager.get().getAdapter()).deleteItems();

            LinearLayout weekLinearLayout = null;
            if (weekView) {
                weekLinearLayout = new LinearLayout(context.get());
                weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                int paddingLeftRight = (int) convertDPToPX(10, context.get());
                weekLinearLayout.setPadding(paddingLeftRight, 0, paddingLeftRight, 0);
            }

            LayoutInflater inflater = (LayoutInflater) context.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (int day = 2; day < 7; day++) {
                View dagView;
                final LinearLayout ll;
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

                //Ga langs alle uren
                for (int y = 0; y < 7; y++) {
                    if (roosterWeek.getUren(day, y) != null && roosterWeek.getUren(day, y).length != 0) {
                        Lesuur[] uurArray = roosterWeek.getUren(day, y);
                        ArrayList<Lesuur> tempUurArray = new ArrayList<Lesuur>(Arrays.asList(uurArray));
                        ArrayList<Lesuur> uitgevallenUren = new ArrayList<Lesuur>();
                        Iterator<Lesuur> iter = tempUurArray.iterator();
                        while (iter.hasNext()) {
                            Lesuur uur = iter.next();
                            if (uur.vervallen) {
                                uitgevallenUren.add(uur);
                                iter.remove();
                            }
                        }
                        if (uitgevallenUren.size() > 0) {
                            String uurNamen = uitgevallenUren.get(0).vak;
                            for (int x = 1; x < uitgevallenUren.size(); x++) {
                                uurNamen += " & " + uitgevallenUren.get(x).vak;
                            }
                            if(uitgevallenUren.size() > 1) {
                                uurNamen += "MULTIPLE";
                            }
                            tempUurArray.add(new Lesuur(uitgevallenUren.get(0).dag,
                                    uitgevallenUren.get(0).uur,
                                    uitgevallenUren.get(0).week,
                                    uitgevallenUren.get(0).klas,
                                    uitgevallenUren.get(0).leraar,
                                    uurNamen,
                                    uitgevallenUren.get(0).lokaal,
                                    true, false));
                        }
                        final ArrayList<View> allUren = new ArrayList<View>();
                        uurArray = tempUurArray.toArray(new Lesuur[tempUurArray.size()]);


                        final boolean multipleViews = (uurArray.length > 1);

                        final FrameLayout frameLayout = new FrameLayout(context.get());
                        for (int u = 0; u < uurArray.length; u++) {
                            final int k = u;
                            Lesuur lesuur = uurArray[u];
                            allUren.add(makeView(lesuur, inflater, y));
                            if (multipleViews) {
                                allUren.get(u).findViewById(R.id.layers).setVisibility(View.VISIBLE);
                            }

                            final int shortAnimationTime = context.get().getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);

                            allUren.get(u).setVisibility(View.GONE);
                            if (uurArray.length > 1) {
                                allUren.get(u).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                    public void onClick(View v) {
                                        final View oldView = allUren.get(k);
                                        final View newView = allUren.get((k + 1) % allUren.size());
                                        if (Build.VERSION.SDK_INT >= 12) {
                                            newView.setAlpha(1f);
                                            newView.setVisibility(View.VISIBLE);
                                            frameLayout.bringChildToFront(oldView);
                                            frameLayout.invalidate();
                                            newView.setClickable(false);
                                            oldView.setClickable(false);
                                            oldView.animate().alpha(0f).setDuration(shortAnimationTime).setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    oldView.setVisibility(View.GONE);
                                                    newView.setVisibility(View.VISIBLE);
                                                    newView.setAlpha(1f);
                                                    newView.setClickable(true);
                                                    oldView.setClickable(true);
                                                }
                                            });
                                        } else {
                                            oldView.setVisibility(View.GONE);
                                            newView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                            }
                            frameLayout.addView(allUren.get(u));
                            allUren.get(0).setVisibility(View.VISIBLE);
                            frameLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        }
                        ll.addView(frameLayout);
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
                    ((MyPagerAdapter) viewPager.get().getAdapter()).setView(dagView, day - 2, context.get());
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
                ((MyPagerAdapter) viewPager.get().getAdapter()).setView(weekScrollView, 0, context.get());
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
    }

    float convertDPToPX(float pixel, Context c) {
        Resources r = c.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel, r.getDisplayMetrics());
    }

    View makeView(Lesuur lesuur, LayoutInflater inflater, int y) {
        View uur;
        boolean paddingRight = true;
        if (lesuur.vervallen) {
            uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
            uur.setMinimumHeight((int) convertDPToPX(80, context.get()));
            if(lesuur.vak.endsWith("MULTIPLE")) {
                String temp = lesuur.vak.replace("MULTIPLE", "");
                ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(temp + " vallen uit");
            } else {
                ((TextView) uur.findViewById(R.id.vervallen_tekst)).setText(lesuur.vak + " valt uit");
            }
        } else {
            if (lesuur.verandering) {
                paddingRight = false;
                uur = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
            } else {
                paddingRight = false;
                uur = inflater.inflate(R.layout.rooster_uur, null);
            }
            ((TextView) uur.findViewById(R.id.rooster_vak)).setText(lesuur.vak);
            if (type != MainActivity.PlaceholderFragment.Type.DOCENTENROOSTER) {
                // Vul de leraar in
                if (lesuur.leraar2 == null || lesuur.leraar2.equals("")) {
                    ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.leraar);
                } else {
                    ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.leraar + " & " + lesuur.leraar2);
                }
            } else {
                //Geef bij een docentenrooster de klas in plaats van de leraar
                ((TextView) uur.findViewById(R.id.rooster_leraar)).setText(lesuur.klas);
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
        return uur;
    }
}
