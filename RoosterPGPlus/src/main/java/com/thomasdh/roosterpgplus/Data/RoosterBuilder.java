package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Adapters.MultipleUrenClickListener;
import com.thomasdh.roosterpgplus.Helpers.Converter;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fj.data.Array;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class RoosterBuilder extends AsyncTask<Void, Void, Void> {
    @Setter private Context context;
    private ViewPager viewPager;
    @Setter private int showDag = Calendar.MONDAY;
    @Setter private boolean showVervangenUren = true;
    @Setter private long lastLoad = -1;
    @Setter private int urenCount = -1;
    @Setter private BuilderFunctions BuilderFunctions = new BuilderFunctions() {
        @Override
        public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
            lesView.findViewById(R.id.optioneel_container).getBackground().setAlpha(0); // Background doorzichtig, geen speciale uren
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");

            TextView vakTextView = (TextView) lesView.findViewById(R.id.rooster_vak);
            TextView leraarTextView = (TextView) lesView.findViewById(R.id.rooster_leraar);
            TextView lokaalTextView = (TextView) lesView.findViewById(R.id.rooster_lokaal);
            TextView tijdenTextView = (TextView) lesView.findViewById(R.id.rooster_tijden);

            vakTextView.setText(lesuur.vak);
            leraarTextView.setText(StringUtils.join(lesuur.leraren, " & "));
            lokaalTextView.setText(lesuur.lokaal);
            tijdenTextView.setText(format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind));

            return lesView;
        }
    };

    private Array<Lesuur> lessen;
    private boolean weekView;

    //region Builder-things

    public RoosterBuilder(@NonNull Context context) {
        setContext(context);
    }

    public RoosterBuilder in(@NonNull ViewPager viewPager) {
        this.viewPager = viewPager;
        return this;
    }

    public void build(List<Lesuur> lessen) {
        build(Array.iterableArray(lessen));
    }
    public void build(Array<Lesuur> lessen) {

    }
    @Override
    protected Void doInBackground(Void... unused) {
        if(viewPager == null) throw new IllegalStateException("Geen ViewPager");
        if(viewPager.getAdapter() == null) viewPager.setAdapter(new AnimatedPagerAdapter());

        if(lessen == null || lessen.length() == 0) {
            View noRoosterView = LayoutInflater.from(context).inflate(R.layout.rooster_null, null);
            ((AnimatedPagerAdapter) viewPager.getAdapter()).setView(noRoosterView, 0, context);
            viewPager.getAdapter().notifyDataSetChanged();
            return null;
        }

        return null;
    }

    //endregion
    //region AsyncThings

    @Accessors(chain = true)
    private class BuilderTask extends AsyncTask<Void, Void, View> {
        @Setter private View parentView;
        @Setter private int dag = Calendar.MONDAY;
        @Setter private Array<Lesuur> lessen;
        @Setter private Calendar dateViewCalendar;
        @Setter private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");

        @Override
        protected View doInBackground(Void... no) {
            LinearLayout linearLayout = (LinearLayout) parentView.findViewById(R.id.rooster_dag_linearlayout);

            /* Dagtitel */
            TextView dagNaamTextView = (TextView) parentView.findViewById(R.id.weekdagnaam);
            TextView dagDatumTextView = (TextView) parentView.findViewById(R.id.weekdagdatum);

            dagNaamTextView.setText(getDayOfWeek(dag));
            int week = dateViewCalendar.get(Calendar.WEEK_OF_YEAR);
            dateViewCalendar.set(Calendar.DAY_OF_WEEK, dag);
            dateViewCalendar.set(Calendar.WEEK_OF_YEAR, week);
            dagDatumTextView.setText(dateFormat.format(dateViewCalendar.getTime()));

            for(int uur = 1; uur <= urenCount; uur++) {

            }

            return null;
        }

        @Override
        protected void onPostExecute(View view) {

        }
    }

    //endregion

    public static void build(List<Lesuur> lessenList, int showDag, boolean showVervangenUren, long lastLoad, int urenCount, ViewPager viewPager, Context context, ViewPager.OnPageChangeListener listener, BuilderFunctions builder) {
        if (viewPager == null) throw new IllegalArgumentException("Geen viewPager");
        if (viewPager.getAdapter() == null) viewPager.setAdapter(new AnimatedPagerAdapter());
        AnimatedPagerAdapter pagerAdapter = (AnimatedPagerAdapter) viewPager.getAdapter();

        if (lessenList == null || lessenList.size() == 0) {
            lessenList = new ArrayList<>();
            lessenList.add(new Lesuur(0, 0, null, null, 1, null, null, null, null, false, false, false, false, null, false, 0, null));
        }
        Array<Lesuur> lessen = Array.iterableArray(lessenList);

        LinearLayout weekLinearLayout = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        context.getResources().getDrawable(R.drawable.diagonal_stripes_optioneeluur).setAlpha(100); // lichtere kleur

        boolean isWide = context.getResources().getBoolean(R.bool.isWideWeekview);
        boolean isHigh = context.getResources().getBoolean(R.bool.isHighWeekview);
        boolean weekView = isWide || isHigh;
        boolean weekViewNoScroll = isWide && isHigh;
        boolean weekViewHorizScroll = isHigh;
        boolean weekViewVerticScroll = isWide;

        int week = lessenList.get(0).week;
        Converter converter = new Converter(context);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.WEEK_OF_YEAR) > 30 && week <= 30) { // Najaar en wil voorjaar, volgend jaar
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR) + 1);
        } else if (now.get(Calendar.WEEK_OF_YEAR) <= 30 && week > 30) { // Voorjaar en wil najaar, vorig jaar
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR) - 1);
        } else {
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
        }
        calendar.set(Calendar.WEEK_OF_YEAR, week);

        if (weekView) {
            weekLinearLayout = new LinearLayout(context);
            weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
            int padding = (int) Converter.convertDPToPX(10, context);
            weekLinearLayout.setPadding(padding, 0, padding, 0);
        }

        for (int i = 2; i < 7; i++) {
            final int dag = i;

            View dagView = inflater.inflate(R.layout.rooster_dag, null);
            LinearLayout dagLinearLayout = (LinearLayout) dagView.findViewById(R.id.rooster_dag_linearlayout);

            Array<Lesuur> lesdag = lessen.filter(s -> s.dag == dag - 1);

            /* Dagtitel */
            TextView dagNaamTextView = (TextView) dagView.findViewById(R.id.weekdagnaam);
            dagNaamTextView.setText(getDayOfWeek(i));

            calendar.set(Calendar.DAY_OF_WEEK, i);
            calendar.set(Calendar.WEEK_OF_YEAR, week);
            TextView dagDatumTextView = (TextView) dagView.findViewById(R.id.weekdagdatum);
            dagDatumTextView.setText(dateFormat.format(calendar.getTime()));

            for (int a = 1; a <= urenCount; a++) {
                final int uur = a;
                boolean multipleViews;
                RelativeLayout urenContainer = new RelativeLayout(context);
                urenContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                ArrayList<RelativeLayout> lesViews = new ArrayList<>();

                Array<Lesuur> lesuur = lesdag.filter(s -> s.uur == uur);

                if (lesuur == null || lesuur.length() == 0) { // Vrij
                    View les = inflater.inflate(R.layout.rooster_tussenuur, null);
                    les.setMinimumHeight(converter.DPtoPX(79));
                    if (uur == urenCount) {
                        les.setBackgroundResource(R.drawable.basic_rect);
                        les.setPadding(0, 0, 0, converter.DPtoPX(1));
                    }
                    dagLinearLayout.addView(les);
                    continue;
                }

                Array<Lesuur> vervallenLessen = lesuur.filter(s -> s.vervallen);
                Array<Lesuur> normalLessen = lesuur.filter(s -> !s.vervallen);

                multipleViews = (vervallenLessen.isNotEmpty() && normalLessen.isNotEmpty()) || normalLessen.length() > 1;

                if (vervallenLessen.isNotEmpty()) { // Vervallen uren
                    if (normalLessen.isEmpty() || (normalLessen.isNotEmpty() && showVervangenUren)) { // Die weergeven
                        RelativeLayout lesView = (RelativeLayout) makeView(vervallenLessen, inflater, builder, urenCount);
                        lesViews.add(lesView);
                    }
                }

                if (normalLessen.isNotEmpty()) {
                    for (Lesuur les : normalLessen) {
                        RelativeLayout lesView = (RelativeLayout) makeView(les, inflater, builder, urenCount);
                        lesViews.add(lesView);
                    }
                }

                int reverseCounter = lesViews.size() + 1;
                int counter = -1;
                urenContainer.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                for (RelativeLayout lesView : lesViews) {
                    reverseCounter--;
                    counter++;
                    if (multipleViews) {
                        TextView uurCounter = (TextView) lesView.findViewById(R.id.layerCounter);
                        uurCounter.setVisibility(View.VISIBLE);
                        uurCounter.setText(new StringBuilder("(").append(reverseCounter).append("/").append(lesViews.size()).append(")"));

                        // Padding links/rechts zodat je uren kan zien :D
                        RelativeLayout.LayoutParams lesLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lesLayoutParams.setMargins(converter.DPtoPX(reverseCounter * 8 - 8), 0, converter.DPtoPX(counter * 8), 0);
                        lesView.setLayoutParams(lesLayoutParams);
                    }

                    urenContainer.addView(lesView);
                }
                if (multipleViews)
                    urenContainer.setOnClickListener(new MultipleUrenClickListener(lesViews, context));
                dagLinearLayout.addView(urenContainer);
            }
            if (weekView) {
                int pxPadding = converter.DPtoPX(3);
                dagLinearLayout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
                dagLinearLayout.setMinimumWidth(converter.DPtoPX(250));
                dagView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                weekLinearLayout.addView(dagView);
            } else {
                int pxPadding = converter.DPtoPX(10);
                dagLinearLayout.addView(getUpdateText(lastLoad, context));
                dagLinearLayout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
                pagerAdapter.setView(dagView, dag - 2, context);
            }
        }

        if (weekView) {
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(weekLinearLayout);

            TextView lastUpdateTextView = getUpdateText(lastLoad, context);
            lastUpdateTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int pxPadding = converter.DPtoPX(10);
            lastUpdateTextView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
            container.addView(lastUpdateTextView);

            if (weekViewNoScroll || weekViewVerticScroll) {
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(container);
                pagerAdapter.setView(scrollView, 0, context);
            } else if (weekViewHorizScroll) {
                HorizontalScrollView scrollView = new HorizontalScrollView(context);
                scrollView.addView(container);
                pagerAdapter.setView(scrollView, 0, context);
            }
        }

        pagerAdapter.notifyDataSetChanged();

        if (!weekView) {
            // Ga naar de juiste dag
            viewPager.setCurrentItem(showDag);
        }

        viewPager.setOnPageChangeListener(listener);


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

    private static View makeView(Lesuur lesuur, LayoutInflater inflater, BuilderFunctions builder, int urenCount) {
        return makeView(Array.array(lesuur), inflater, builder, urenCount);
    }

    private static View makeView(Array<Lesuur> lessen, LayoutInflater inflater, BuilderFunctions builder, int urenCount) {
        View lesView;
        Converter con = new Converter(inflater.getContext());

        if (lessen.get(0).vervallen) { // Vervallen lessen
            lessen = lessen.filter(lesuur -> lesuur.vervallen);

            lesView = inflater.inflate(R.layout.rooster_vervallen_uur, null);
            lesView.setMinimumHeight(con.DPtoPX(80));

            TextView vervallenTextView = (TextView) lesView.findViewById(R.id.vervallen_tekst);
            if (lessen.length() > 1) {
                vervallenTextView.setText(StringUtils.join(lessen.map(s -> s.vak).toList(), " & ") + " vallen uit");
            } else {
                vervallenTextView.setText(lessen.get(0).vak + " valt uit");
            }
        } else if (lessen.length() > 1) {
            throw new IllegalArgumentException("Meerdere lessen");
        } else {
            Lesuur lesuur = lessen.get(0);
            if (lesuur.verandering) {
                lesView = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
            } else {
                lesView = inflater.inflate(R.layout.rooster_uur, null);
            }
            lesView = builder.fillLesView(lesuur, lesView, inflater);
        }

        if (lessen.get(0).uur == urenCount) {
            lesView.findViewById(R.id.rooster_uur_linearlayout).setBackgroundResource(R.drawable.basic_rect);
        }

        lesView.setMinimumHeight(con.DPtoPX(80));
        return lesView;
    }

    private static TextView getUpdateText(long lastLoad, Context context) {
        TextView textView = new TextView(context);
        Date lastUpdate = new Date(lastLoad);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        String lastUpdateText = "Laatst geupdate: " + dateFormat.format(lastUpdate);
        if (!HelperFunctions.hasInternetConnection(context)) {
            lastUpdateText = "Geen internetverbinding. " + lastUpdateText;
        }

        textView.setText(lastUpdateText);
        return textView;
    }

    public interface BuilderFunctions {
        View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater);
    }
}
