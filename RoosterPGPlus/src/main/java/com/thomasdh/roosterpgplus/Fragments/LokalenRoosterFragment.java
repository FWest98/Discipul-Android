package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.CustomUI.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


@FragmentTitle(title = R.string.action_bar_dropdown_lokalenrooster)
public class LokalenRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener {
    private static final String CHOSEN_LOKAAL_KEY = "lastChosenLokaal";
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    @Getter @Setter private String lokaal;

    private DefaultSpinner lokaalSpinner;

    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_lokaal, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        lokaalSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_lokaal);

        RoosterInfo.getLokalen(getActivity(), this::onLokalenLoaded);

        return getRootView();
    }

    public void onLokalenLoaded(Object lok) {
        if(lok == null) return;
        ArrayList<String> lokalen = (ArrayList<String>) lok;

        ArrayAdapter<String> lokaalAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, lokalen);
        lokaalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lokaalSpinner.setAdapter(lokaalAdapter);

        lokaalSpinner.setOnItemSelectedListener(this);

        String lastChosenLokaal = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(CHOSEN_LOKAAL_KEY, null);
        if(lastChosenLokaal == null) return;
        setLokaal(lastChosenLokaal);
        lokaalSpinner.setSelection(lokaalAdapter.getPosition(lastChosenLokaal));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setLokaal(((TextView) view).getText().toString());
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(CHOSEN_LOKAAL_KEY, getLokaal()).commit();

        loadRooster();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    //endregion
    //region RoosterLoads

    @Override
    public boolean canLoadRooster() { return getLokaal() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("lokaal", getLokaal()));
        return query;
    }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("lokaal"+getLokaal()+getWeek(), getActivity());
        if(lastLoad == null) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public long getLoad() { return RoosterInfo.getLoad("lokaal"+getLokaal()+getWeek(), getActivity()); }

    @Override
    public void setLoad() {
        RoosterInfo.setLoad("lokaal"+getLokaal()+getWeek(), System.currentTimeMillis(), getActivity());
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        lesView.findViewById(R.id.optioneel_container).setBackgroundResource(0);

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
