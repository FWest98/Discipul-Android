package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.Converter;
import com.thomasdh.roosterpgplus.Models.Lesuur;

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
@Deprecated
public class RoosterBuilderOld {

    private final WeakReference<Context> context;
    private final WeakReference<ViewPager> viewPager;
    private final int week;
    private final RoosterViewFragment.Type type;
    private String klas;


    public RoosterBuilderOld(Context context, ViewPager viewPager, int week, RoosterViewFragment.Type type) {
        this.context = new WeakReference<Context>(context);
        this.viewPager = new WeakReference<ViewPager>(viewPager);
        this.week = week;
        this.type = type;
        klas = null;
    }

    public RoosterBuilderOld(Context context, ViewPager viewPager, int week, RoosterViewFragment.Type type, String klas) {
        this(context, viewPager, week, type);
        this.klas = klas;
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

    private static class RoosterWeek {
        Lesuur[] getUren(int d, int i) { return null; }
    }

    public void buildLayout(RoosterWeek roosterWeek) {
        if (viewPager.get() != null) {
            if (viewPager.get().getAdapter() == null) {
                viewPager.get().setAdapter(new AnimatedPagerAdapter());
            }
        }
        boolean wideEnoughForWeekview = context.get().getResources().getBoolean(R.bool.isWideWeekview);
        final boolean highEnoughForWeekview = context.get().getResources().getBoolean(R.bool.isHighWeekview);

        if (roosterWeek != null) {

            LinearLayout weekLinearLayout = null;
            if (wideEnoughForWeekview || highEnoughForWeekview) {
                weekLinearLayout = new LinearLayout(context.get());
                weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
                int paddingLeftRight = (int) Converter.convertDPToPX(10, context.get());
                weekLinearLayout.setPadding(paddingLeftRight, 0, paddingLeftRight, 0);
            }

            LayoutInflater inflater = (LayoutInflater) context.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (int day = 2; day < 7; day++) {
                View dagView;
                LinearLayout ll;
                dagView = inflater.inflate(R.layout.rooster_dag, null);
                ll = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);

                TextView dagTextView = (TextView) dagView.findViewById(R.id.weekdagnaam);
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
                        if (!uitgevallenUren.isEmpty()) {
                            String uurNamen = uitgevallenUren.get(0).vak;
                            int count = 1;
                            for (int x = 1; x < uitgevallenUren.size(); x++) {
                                if (!uurNamen.contains(uitgevallenUren.get(x).vak)) {
                                    count++;
                                    uurNamen += " & " + uitgevallenUren.get(x).vak;
                                }
                            }
                            if (count > 1) {
                                uurNamen += "MULTIPLE";
                            }
                            // In het persoonlijke rooster wordt uitval niet weergegeven als er tegelijkertijd een
                            // andere les is die niet uitvalt.
                            if (type != RoosterViewFragment.Type.PERSOONLIJK_ROOSTER || tempUurArray.isEmpty()) {
                                tempUurArray.add(new Lesuur(uitgevallenUren.get(0).dag,
                                        uitgevallenUren.get(0).uur,
                                        null, null,
                                        uitgevallenUren.get(0).week,
                                        uitgevallenUren.get(0).klassen,
                                        uitgevallenUren.get(0).leraren,
                                        null,
                                        uitgevallenUren.get(0).lokaal,
                                        false, true, false, false, null, false, 0, null));
                            }
                        }
                        final ArrayList<RelativeLayout> allUren = new ArrayList<>();
                        uurArray = tempUurArray.toArray(new Lesuur[tempUurArray.size()]);


                        boolean multipleViews = uurArray.length > 1;


                        final RelativeLayout parentLayout = new RelativeLayout(context.get());

                        int a = -1;

                        for (int u = uurArray.length - 1; u >= 0; u--) {
                            a++;
                            Lesuur lesuur = uurArray[u];
                            RelativeLayout uurview = (RelativeLayout) makeView(lesuur, inflater, y);

                            allUren.add(uurview);

                            if (multipleViews) {
                                TextView lagen = (TextView) uurview.findViewById(R.id.layerCounter);
                                lagen.setVisibility(View.VISIBLE);
                                lagen.setText("(" + (u + 1) + "/" + uurArray.length + ")");


                                // Dit geeft de padding door voor uren zodat je achterliggende uren kunt zien
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                params.setMargins(0, 0, (int) Converter.convertDPToPX(a * 8, context.get()), 0);
                                uurview.setLayoutParams(params);
                            }

                            parentLayout.addView(allUren.get(a));
                            parentLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        }

                        if (multipleViews)
                            parentLayout.setOnClickListener(new MultipleUrenClickListener(allUren, context.get()));

                        ll.addView(parentLayout);
                    } else {
                        View vrij = inflater.inflate(R.layout.rooster_tussenuur, null);
                        vrij.setMinimumHeight((int) Converter.convertDPToPX(79, context.get()));
                        if (y == 6) {
                            vrij.setBackgroundResource(R.drawable.basic_rect);
                            vrij.setPadding(0, 0, 0, (int) Converter.convertDPToPX(1, context.get()));
                        }
                        ll.addView(vrij);
                    }

                }

                if (wideEnoughForWeekview || highEnoughForWeekview) {
                    ll.setPadding((int) Converter.convertDPToPX(3, context.get()), (int) Converter.convertDPToPX(3, context.get()), (int) Converter.convertDPToPX(3, context.get()), (int) Converter.convertDPToPX(3, context.get()));
                    ll.setMinimumWidth((int) Converter.convertDPToPX(250, context.get()));
                    dagView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    weekLinearLayout.addView(dagView);
                } else {
                    ll.addView(getBottomTextView());
                    ll.setPadding((int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()));
                    ((AnimatedPagerAdapter) viewPager.get().getAdapter()).setView(dagView, day - 2, context.get());
                }
            }
            if (wideEnoughForWeekview || highEnoughForWeekview) {
                LinearLayout completeLinearLayout = new LinearLayout(context.get());
                completeLinearLayout.setOrientation(LinearLayout.VERTICAL);
                completeLinearLayout.addView(weekLinearLayout);

                // Maak het laatstgeupdate vak
                TextView dataTextView = getBottomTextView();
                dataTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                dataTextView.setPadding((int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()), (int) Converter.convertDPToPX(10, context.get()));
                completeLinearLayout.addView(dataTextView);

                if (wideEnoughForWeekview && highEnoughForWeekview) {
                    ((AnimatedPagerAdapter) viewPager.get().getAdapter()).setView(completeLinearLayout, 0, context.get());
                } else if (wideEnoughForWeekview) {
                    ScrollView weekScrollView = new ScrollView(context.get());
                    weekScrollView.addView(completeLinearLayout);
                    ((AnimatedPagerAdapter) viewPager.get().getAdapter()).setView(weekScrollView, 0, context.get());
                } else if (highEnoughForWeekview) {
                    HorizontalScrollView completeScrollView = new HorizontalScrollView(context.get());
                    completeScrollView.addView(completeLinearLayout);
                    ((AnimatedPagerAdapter) viewPager.get().getAdapter()).setView(completeScrollView, 0, context.get());
                }

            }
            viewPager.get().getAdapter().notifyDataSetChanged();
            if (!wideEnoughForWeekview && !highEnoughForWeekview)
                // Ga naar de gewilde dag
                if (PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("geselecteerdeweek", -1) == week) {
                    Log.d(getClass().getSimpleName(), "De geselecteerde week is niet veranderd, de dag blijft " + PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("dagvandeweeklaatst", 0));
                    viewPager.get().setCurrentItem(PreferenceManager.getDefaultSharedPreferences(context.get()).getInt("dagvandeweeklaatst", 0));
                } else if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) == week) {
                    Log.d(getClass().getSimpleName(), "De geselecteerde week is veranderd, en is deze week, de dag wordt " + (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2));
                    int goedeDag = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2;
                    viewPager.get().setCurrentItem(goedeDag);
                    PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("dagvandeweek", goedeDag).commit();
                } else {
                    Log.d(getClass().getSimpleName(), "De geselecteerde week is veranderd en is niet deze week, de dag wordt maandag.");
                    viewPager.get().setCurrentItem(0);
                    PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("dagvandeweek", 0).commit();
                }

            PreferenceManager.getDefaultSharedPreferences(context.get()).edit().putInt("geselecteerdeweek", week).commit();

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

    TextView getBottomTextView() {
        TextView dataTextView = new TextView(context.get());
        Date laatstGeupdate = new Date(PreferenceManager.getDefaultSharedPreferences(context.get()).getLong("lastRefreshTime", 0));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        String subText = "Laatst geupdate: " + simpleDateFormat.format(laatstGeupdate) + ".";

        ConnectivityManager cm = (ConnectivityManager) context.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnectedOrConnecting()) {
            subText = "Geen internetverbinding. " + subText;
        }

        dataTextView.setText(subText);
        return dataTextView;
    }


    View makeView(Lesuur lesuur, LayoutInflater inflater, int y) {
        View uur;
        if (lesuur.vervallen) {
            uur = inflater.inflate(R.layout.rooster_vervallen_uur, null);
            uur.setMinimumHeight((int) Converter.convertDPToPX(80, context.get()));
            TextView vervallenTextView = (TextView) uur.findViewById(R.id.vervallen_tekst);
            if (lesuur.vak.endsWith("MULTIPLE")) {
                String temp = lesuur.vak.replace("MULTIPLE", "");
                vervallenTextView.setText(temp + " vallen uit");
            } else {
                vervallenTextView.setText(lesuur.vak + " valt uit");
            }

            // CAPS LOCK DAY
            if (Build.VERSION.SDK_INT >= 14)
                if (Calendar.getInstance().get(Calendar.MONTH) == 9 && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 22)
                    vervallenTextView.setAllCaps(true);

        } else {
            if (lesuur.verandering) {
                uur = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
                if (klas != null && !lesuur.klassen.get(0).equals(klas)) {
                    uur.findViewById(R.id.optioneel_container).getBackground().setAlpha(100);
                } else {
                    uur.findViewById(R.id.optioneel_container).getBackground().setAlpha(0);
                }
            } else {
                //Als het geen algemeen uur is
                if (klas != null && !lesuur.klassen.get(0).equals(klas)) {
                    uur = inflater.inflate(R.layout.rooster_uur_optioneel, null);
                    uur.findViewById(R.id.optioneel_container).getBackground().setAlpha(100);
                } else {
                    uur = inflater.inflate(R.layout.rooster_uur, null);
                }
            }
            TextView vakTextView = (TextView) uur.findViewById(R.id.rooster_vak);
            vakTextView.setText(lesuur.vak);

            TextView leraarTextView = (TextView) uur.findViewById(R.id.rooster_leraar);
            if (type != RoosterViewFragment.Type.DOCENTENROOSTER) {
                // Vul de leraar in
                if (lesuur.leraren.size() == 1) {
                    leraarTextView.setText(lesuur.leraren.get(0));
                } else {
                    leraarTextView.setText(lesuur.leraren.toString());
                }
            } else {
                //Geef bij een docentenrooster de klas in plaats van de leraar
                leraarTextView.setText(lesuur.klassen.get(0));
            }
            TextView lokaalTextView = (TextView) uur.findViewById(R.id.rooster_lokaal);
            lokaalTextView.setText(lesuur.lokaal);

            TextView tijdenTextView = (TextView) uur.findViewById(R.id.rooster_tijden);
            tijdenTextView.setText(getTijden(y));

            // CAPS LOCK DAY
            if (Build.VERSION.SDK_INT >= 14)
                if (Calendar.getInstance().get(Calendar.MONTH) == 9 && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 22) {
                    lokaalTextView.setAllCaps(true);
                    tijdenTextView.setAllCaps(true);
                    leraarTextView.setAllCaps(true);
                    vakTextView.setAllCaps(true);
                }
        }
        if (y == 6) {
            uur.findViewById(R.id.rooster_uur_linearlayout).setBackgroundResource(R.drawable.basic_rect);
        }
        uur.setMinimumHeight((int) Converter.convertDPToPX(80, context.get()));
        return uur;
    }
}
