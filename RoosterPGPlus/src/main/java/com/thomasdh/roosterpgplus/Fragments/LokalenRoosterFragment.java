package com.thomasdh.roosterpgplus.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.CustomUI.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.Apache.BasicNameValuePair;
import com.thomasdh.roosterpgplus.Helpers.Apache.NameValuePair;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.Models.Lokaal;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fj.data.Array;
import lombok.Getter;
import lombok.Setter;


@FragmentTitle(title = R.string.action_bar_dropdown_lokalenrooster)
public class LokalenRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener, RoosterBuilder.BuilderFunctions {
    private static final String CHOSEN_LOKAAL_KEY = "lastChosenLokaal";
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    @Getter @Setter private Lokaal lokaal;
    @Getter @Setter private ArrayList<Lokaal> lokalen;

    private DefaultSpinner lokaalSpinner;

    @Override
    public String getAnalyticsTitle() {
        return Constants.ANALYTICS_FRAGMENT_LOKROOSTER;
    }

    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_lokaal, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());
        swipeRefreshLayout = (SwipeRefreshLayout) getRootView().findViewById(R.id.rooster_swiperefresh);
        setupSwipeRefreshLayout();

        lokaalSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_lokaal);

        RoosterInfo.getLokalen(getContext(), this::onLokalenLoaded);

        return getRootView();
    }

    void onLokalenLoaded(Object lok) {
        if(lok == null) return;
        lokalen = (ArrayList<Lokaal>) lok;

        String[] lokaalNamen = Array.iterableArray(lokalen).map(Lokaal::getNaam).array(String[].class);
        String[] lokaalCodes = Array.iterableArray(lokalen).map(Lokaal::getCode).array(String[].class);

        ArrayAdapter<String> lokaalAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_title, lokaalNamen);
        lokaalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lokaalSpinner.setAdapter(lokaalAdapter);

        lokaalSpinner.setOnItemSelectedListener(this);

        String lastChosenLokaal = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CHOSEN_LOKAAL_KEY, null);
        if(lastChosenLokaal == null || !Arrays.asList(lokaalCodes).contains(lastChosenLokaal)) return;

        int lokaalIndex = Arrays.asList(lokaalCodes).indexOf(lastChosenLokaal);
        setLokaal(lokalen.get(lokaalIndex));
        lokaalSpinner.setSelection(lokaalIndex);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setLokaal(getLokalen().get(position));
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(CHOSEN_LOKAAL_KEY, getLokaal().getCode()).commit();

        loadRooster();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    //endregion
    //region Statemanagement

    @Override
    public boolean canLoadRooster() { return getLokaal() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("lokaal", getLokaal().getCode()));
        return query;
    }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("lokaal"+getLokaal()+getWeek(), getContext());
        if(lastLoad == null || lastLoad == 0) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public long getLoad() { return RoosterInfo.getLoad("lokaal"+getLokaal()+getWeek(), getContext()); }

    @Override
    public void setLoad() {
        RoosterInfo.setLoad("lokaal"+getLokaal()+getWeek(), System.currentTimeMillis(), getContext());
    }

    //endregion
    //region Rooster

    @Override
    public RoosterBuilder buildRooster(int urenCount) {
        return super.buildRooster(urenCount)
                .setBuilderFunctions(this);
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            lesView.findViewById(R.id.optioneel_container).setBackground(null);
        } else {
            lesView.findViewById(R.id.optioneel_container).setBackgroundResource(0);
        }

        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        TextView vakTextView = (TextView) lesView.findViewById(R.id.rooster_vak);
        TextView leraarTextView = (TextView) lesView.findViewById(R.id.rooster_leraar);
        TextView lokaalTextView = (TextView) lesView.findViewById(R.id.rooster_lokaal);
        TextView tijdenTextView = (TextView) lesView.findViewById(R.id.rooster_tijden);

        vakTextView.setText(lesuur.vak);
        leraarTextView.setText(StringUtils.join(lesuur.leraren, " & "));
        lokaalTextView.setText(StringUtils.join(lesuur.klassen, " & "));
        String times = format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind);
        tijdenTextView.setText(times);

        return lesView;
    }

    //endregion
}
