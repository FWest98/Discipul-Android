package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.util.Converter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fj.F;
import fj.data.Array;

public class RoosterBuilder {
    public static void build(List<Lesuur> lessenList, int showDag, ViewPager viewPager, Context context) {
        if(viewPager == null) throw new IllegalArgumentException("Geen viewPager");
        if(viewPager.getAdapter() == null) viewPager.setAdapter(new AnimatedPagerAdapter());

        if(lessenList == null) lessenList = new ArrayList<>();
        Array<Lesuur> lessen = Array.iterableArray(lessenList);

        LinearLayout weekLinearLayout = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        boolean isWide = context.getResources().getBoolean(R.bool.isWideWeekview);
        boolean isHigh = context.getResources().getBoolean(R.bool.isHighWeekview);
        boolean weekView = isWide || isHigh;
        boolean weekViewNoScroll = isWide && isHigh;
        boolean weekViewHorizScroll = isHigh;
        boolean weekViewVerticScroll = isWide;

        int week = lessenList.get(0).week;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        Calendar calendar = Calendar.getInstance();

        if(calendar.get(Calendar.WEEK_OF_YEAR) > 33 && week <= 33) { // Najaar en wil voorjaar, volgend jaar
            calendar.add(Calendar.YEAR, 1);
        } else if(week > 33) { // Voorjaar en wil najaar, vorig jaar
            calendar.add(Calendar.YEAR, -1);
        }
        calendar.set(Calendar.WEEK_OF_YEAR, week);

        int urenCount = RoosterInfo.getWeekUrenCount(context, week);

        if(weekView) {
            weekLinearLayout = new LinearLayout(context);
            weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
            int padding = (int) Converter.convertDPToPX(10, context);
            weekLinearLayout.setPadding(padding, 0, padding, 0);
        }

        for (int i = 2; i < 7; i++) {
            final int dag = i;

            View dagView = inflater.inflate(R.layout.rooster_dag, null);
            LinearLayout dagLinearLayout = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);

            Array<Lesuur> lesdag = lessen.filter(new F<Lesuur, Boolean>() {
                @Override
                public Boolean f(Lesuur lesuur) {
                    return lesuur.dag == dag;
                }
            });

            /* Dagtitel */
            TextView dagNaamTextView = (TextView) dagView.findViewById(R.id.weekdagnaam);
            dagNaamTextView.setText(getDayOfWeek(i));

            calendar.set(Calendar.DAY_OF_WEEK, i);
            TextView dagDatumTextView = (TextView) dagView.findViewById(R.id.weekdagdatum);
            dagDatumTextView.setText(dateFormat.format(calendar.getTime()));

            for(int a = 1; a < urenCount + 1; a++) {
                final int uur = a;

                Array<Lesuur> lesuur = lesdag.filter(new F<Lesuur, Boolean>() {
                    @Override
                    public Boolean f(Lesuur lesuur) {
                        return lesuur.uur == uur;
                    }
                });

                if(lesuur == null || lesuur.length() == 0) { // Vrij
                    View les = inflater.inflate(R.layout.rooster_tussenuur, null);
                    les.setMinimumHeight((int) Converter.convertDPToPX(79, context));
                    if(uur == urenCount) {
                        les.setBackgroundResource(R.drawable.basic_rect);
                        les.setPadding(0,0,0,(int) Converter.convertDPToPX(1, context));
                    }
                    dagLinearLayout.addView(les);
                    continue;
                }


            }
        }
    }

    private static String getDayOfWeek(int dag) {
        switch (dag) {
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
}
