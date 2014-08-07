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
import com.thomasdh.roosterpgplus.CustomViews.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.action_bar_dropdown_klassenrooster)
public class KlassenRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener {
    private static final String CHOSEN_KLAS_KEY = "lastChosenKlas";
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    private DefaultSpinner klasSpinner;

    @Getter @Setter private String klas;

    @Override
    public Type getType() {
        return Type.KLASROOSTER;
    }

    @Override
    public boolean canLoadRooster() { return getKlas() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("klas", getKlas()));
        return query;
    }

    @Override
    public void setLoad() { RoosterInfo.setLoad("klas"+getKlas(), System.currentTimeMillis(), getActivity()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("klas"+getKlas(), getActivity());
        if(lastLoad == null) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_klas, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager_klas);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        klasSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_klas);

        RoosterInfo.getKlassen(getActivity(), s -> onKlassenLoaded((ArrayList<String>) s));

        return getRootView();
    }

    public void onKlassenLoaded(ArrayList<String> klassen) {

        if(klassen == null) return;

        ArrayAdapter<String> klasAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, klassen);
        klasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        klasSpinner.setAdapter(klasAdapter);

        klasSpinner.setOnItemSelectedListener(this);

        String lastChosenKlas = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(CHOSEN_KLAS_KEY, null);
        if(lastChosenKlas == null) return;
        klasSpinner.setTag(lastChosenKlas);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setKlas(((TextView) view).getText().toString());
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(CHOSEN_KLAS_KEY, getKlas()).commit();

        loadRooster();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}
