package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;

import java.util.List;

public class PGTVRoosterFragment extends RoosterViewFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        loadRooster();

        return getRootView();
    }

    @Override
    protected boolean canLoadRooster() {
        return false;
    }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        return query;
    }

    @Override
    public LoadType getLoadType() {
        return null;
    }

    @Override
    public long getLoad() {
        return 0;
    }

    @Override
    public void setLoad() {}

    @Override
    public void loadRooster(boolean reload) {
        // PGTV laden

    }
}
