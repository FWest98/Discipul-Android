package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.CustomUI.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Floris on 14-7-2014.
 */
@FragmentTitle(title = R.string.action_bar_dropdown_lokalenrooster)
public class LokalenRoosterFragment extends RoosterViewFragment {
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

        // Implementeer de rest. Ik faal met de API
        return null;
    }


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
        throw new UnsupportedOperationException("Nog niet af");
    }

    //endregion

    @Override
    public Type getType() {
        return Type.LOKALENROOSTER;
    }


}
