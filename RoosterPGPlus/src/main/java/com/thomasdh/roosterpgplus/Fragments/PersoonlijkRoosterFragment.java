package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;


@FragmentTitle(title = R.string.action_bar_dropdown_persoonlijk_rooster)
public class PersoonlijkRoosterFragment extends RoosterViewFragment {
    private static final String LOADS_NAME = "personal";
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 2700000;

    @Override
    public Type getType() {
        return Type.PERSOONLIJK_ROOSTER;
    }

    @Override
    public boolean canLoadRooster() { return user != null && user.isSet; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("key", user.getApikey()));
        return query;
    }

    @Override
    public void setLoad() { RoosterInfo.setLoad(LOADS_NAME, System.currentTimeMillis(), getActivity()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad(LOADS_NAME, getActivity());
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

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        if(!user.isSet) {
            user.showLoginDialog(true);
        }

        return getRootView();
    }
}
