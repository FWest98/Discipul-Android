package com.thomasdh.roosterpgplus.Data;

import android.content.Context;
import android.graphics.Color;
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
    @Setter private BuilderFunctions builderFunctions = (lesuur, lesView, inflater) -> {
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
    };

    private Array<Lesuur> lessen;
    private boolean weekView;
    private Converter converter;
    private AnimatedPagerAdapter adapter;
    private ArrayList<View> dagViews = new ArrayList<>();

    private LinearLayout weekLinearLayout = null;


    //region Builder-things

    public RoosterBuilder(@NonNull Context context) {
        setContext(context);
    }

    public RoosterBuilder in(@NonNull ViewPager viewPager) {
        this.viewPager = viewPager;
        return this;
    }

    public void build(List<Lesuur> lessen) {
        if(lessen == null) lessen = new ArrayList<>();
        build(Array.iterableArray(lessen));
    }
    public void build(Array<Lesuur> lessen) {
        this.lessen = lessen;
        execute();
    }

    @Override
    protected Void doInBackground(Void... unused) {
        if(viewPager == null) throw new IllegalStateException("Geen ViewPager");
        if(viewPager.getAdapter() == null) throw new IllegalArgumentException("Geen ViewPagerAdapter");
        adapter = (AnimatedPagerAdapter) viewPager.getAdapter();

        if(lessen == null || lessen.length() == 0) {
            return null;
        }

        if(urenCount == -1) {
            urenCount = lessen.foldLeft((a, b) -> b.uur > a ? b.uur : a, 0);
        }

        context.getResources().getDrawable(R.drawable.diagonal_stripes_optioneeluur).setAlpha(100);
        converter = new Converter(context);

        boolean isWide = context.getResources().getBoolean(R.bool.isWideWeekview);
        boolean isHigh = context.getResources().getBoolean(R.bool.isHighWeekview);
        weekView = isWide || isHigh;

        int week = lessen.get(0).week;
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

        if(weekView) {
            weekLinearLayout = new LinearLayout(context);
            weekLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f));
            int padding = converter.DPtoPX(10);
            weekLinearLayout.setPadding(padding, 0, padding, 0);
        }

        for(int i = 2; i < 7; i++) {
            final int dag = i;

            View dagView = LayoutInflater.from(context).inflate(R.layout.rooster_dag, null);

            dagView = new DagBuilder()
                    .setParentView(dagView)
                    .setDag(dag)
                    .setLessen(lessen.filter(s -> s.dag == dag - 1))
                    .setDateViewCalendar(calendar)
                    .build();

            if(weekView) {
                weekLinearLayout.addView(dagView);
            } else {
                dagViews.add(dagView);
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void s) {
        if(lessen == null || lessen.length() == 0) {
            View noRoosterView = LayoutInflater.from(context).inflate(R.layout.rooster_null, null);
            adapter.setView(noRoosterView, 0, context);
            viewPager.getAdapter().notifyDataSetChanged();

            return;
        }

        boolean isWide = context.getResources().getBoolean(R.bool.isWideWeekview);
        boolean isHigh = context.getResources().getBoolean(R.bool.isHighWeekview);
        boolean weekViewNoScroll = isWide && isHigh;
        boolean weekViewHorizScroll = isHigh;
        boolean weekViewVerticScroll = isWide;

        if (weekView) {
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(weekLinearLayout);

            TextView lastUpdateTextView = getUpdateText(lastLoad);
            lastUpdateTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int pxPadding = converter.DPtoPX(10);
            lastUpdateTextView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
            container.addView(lastUpdateTextView);

            if (weekViewNoScroll || weekViewVerticScroll) {
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(container);
                adapter.setView(scrollView, 0, context);
            } else if (weekViewHorizScroll) {
                HorizontalScrollView scrollView = new HorizontalScrollView(context);
                scrollView.addView(container);
                adapter.setView(scrollView, 0, context);
            }
        } else {
            for(int i = 0; i < dagViews.size(); i++) {
                adapter.setView(dagViews.get(i), i, context);
            }
        }

        adapter.notifyDataSetChanged();

        if (!weekView) {
            // Ga naar de juiste dag
            viewPager.setCurrentItem(showDag);
        }
    }

    //endregion
    //region AsyncThings

    @Accessors(chain = true)
    private class DagBuilder {
        @Setter private View parentView;
        @Setter private int dag = Calendar.MONDAY;
        @Setter private Array<Lesuur> lessen;
        @Setter private Calendar dateViewCalendar;
        @Setter private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");

        private LinearLayout linearLayout;

        public View build() {
            linearLayout = (LinearLayout) parentView.findViewById(R.id.rooster_dag_linearlayout);

            /* Dagtitel */
            TextView dagNaamTextView = (TextView) parentView.findViewById(R.id.weekdagnaam);
            TextView dagDatumTextView = (TextView) parentView.findViewById(R.id.weekdagdatum);

            dagNaamTextView.setText(getDayOfWeek(dag));
            int week = dateViewCalendar.get(Calendar.WEEK_OF_YEAR);
            dateViewCalendar.set(Calendar.DAY_OF_WEEK, dag);
            dateViewCalendar.set(Calendar.WEEK_OF_YEAR, week);
            dagDatumTextView.setText(dateFormat.format(dateViewCalendar.getTime()));

            for(int i = 1; i <= urenCount; i++) {
                final int uur = i;
                Array<Lesuur> lessenInUur = lessen.filter(s -> s.uur == uur);
                ArrayList<RelativeLayout> lesViews = new ArrayList<>();
                RelativeLayout urenContainer = null;

                if(lessenInUur == null || lessenInUur.length() == 0) { // Geen lessen -> vrij
                    View lesView = LayoutInflater.from(context).inflate(R.layout.rooster_tussenuur, null);
                    lesView.setMinimumHeight(converter.SPtoPX(89));
                    if(uur == urenCount) {
                        lesView.setBackgroundResource(R.drawable.basic_rect);
                        lesView.setPadding(0,0,0,converter.DPtoPX(1));
                    }
                    linearLayout.addView(lesView);

                    continue;
                }

                Array<Lesuur> vervallenLessen = lessenInUur.filter(s -> s.vervallen);
                Array<Lesuur> normalLessen = lessenInUur.filter(s -> !s.vervallen);

                boolean multipleViews = (vervallenLessen.isNotEmpty() && normalLessen.isNotEmpty()) || normalLessen.length() > 1;
                if(multipleViews) {
                    urenContainer = new RelativeLayout(context);
                    urenContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }

                if(vervallenLessen.isNotEmpty() && (normalLessen.isEmpty() || normalLessen.isNotEmpty() && showVervangenUren)) { // Vervallen uren verwerken
                    lesViews.add((RelativeLayout) makeView(vervallenLessen));
                }

                if(normalLessen.isNotEmpty()) {
                    for(Lesuur les : normalLessen) {
                        RelativeLayout lesLayout = (RelativeLayout) makeView(les);
                        if(lesLayout != null) lesViews.add(lesLayout);
                    }
                }

                int reverseCounter = lesViews.size() + 1;
                int counter = -1;
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

                    if(multipleViews) {
                        urenContainer.addView(lesView);
                    } else {
                        linearLayout.addView(lesView);
                    }
                }

                if(multipleViews) {
                    urenContainer.setOnClickListener(new MultipleUrenClickListener(lesViews, context));
                    linearLayout.addView(urenContainer);
                }
            }

            if(weekView) {
                int pxPadding = converter.DPtoPX(3);
                linearLayout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
                linearLayout.setMinimumWidth(converter.DPtoPX(250));
                parentView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            } else {
                int pxPadding = converter.DPtoPX(10);
                linearLayout.addView(getUpdateText(lastLoad));
                linearLayout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
            }

            return parentView;
        }
    }

    //endregion

    private String getDayOfWeek(int dag) {
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

    private View makeView(Lesuur lesuur) {
        return makeView(Array.array(lesuur));
    }

    private View makeView(Array<Lesuur> lessen) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View lesView;

        if (lessen.get(0).vervallen) { // Vervallen lessen
            lessen = lessen.filter(lesuur -> lesuur.vervallen);

            lesView = inflater.inflate(R.layout.rooster_vervallen_uur, null);
            lesView.setMinimumHeight(converter.SPtoPX(90));

            TextView vervallenTextView = (TextView) lesView.findViewById(R.id.vervallen_tekst);
            if (lessen.length() > 1) {
                vervallenTextView.setText(StringUtils.join(lessen.map(s -> s.vak).toList(), " & ") + " vallen uit");
            } else if(lessen.get(0).verplaatsing) {
                vervallenTextView.setText(lessen.get(0).vak + " is verplaatst naar " + lessen.get(0).lokaal);
            } else {
                vervallenTextView.setText(lessen.get(0).vak + " valt uit");
            }

        } else if (lessen.length() > 1) {
            throw new IllegalArgumentException("Meerdere lessen");
        } else {
            Lesuur lesuur = lessen.get(0);

            if (lesuur.verandering) {
                lesView = inflater.inflate(R.layout.rooster_uur_gewijzigd, null);
            } else if(lesuur.verplaatsing) {
                lesView = inflater.inflate(R.layout.rooster_uur, null);
                ((TextView) lesView.findViewById(R.id.rooster_lokaal)).setTextColor(Color.parseColor("#FF0000"));
            } else if(lesuur.isNew) {
                lesView = inflater.inflate(R.layout.rooster_uur, null);
                ((TextView) lesView.findViewById(R.id.rooster_notes)).setText("Nieuwe les");
            } else {
                lesView = inflater.inflate(R.layout.rooster_uur, null);
            }
            lesView = builderFunctions.fillLesView(lesuur, lesView, inflater);
        }

        if (lessen.get(0).uur == urenCount) {
            lesView.findViewById(R.id.rooster_uur_linearlayout).setBackgroundResource(R.drawable.basic_rect);
        }

        lesView.setMinimumHeight(converter.SPtoPX(90));
        return lesView;
    }

    private TextView getUpdateText(long lastLoad) {
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
