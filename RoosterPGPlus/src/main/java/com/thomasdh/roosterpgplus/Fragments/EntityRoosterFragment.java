package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.Apache.BasicNameValuePair;
import com.thomasdh.roosterpgplus.Helpers.Apache.NameValuePair;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.action_bar_dropdown_speciaal_rooster)
public class EntityRoosterFragment extends RoosterViewFragment {
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 2700000;

    @Getter @Setter
    private String entity;

    @Override
    public String getAnalyticsTitle() {
        return Constants.ANALYTICS_FRAGMENT_SEARCHROOSTER;
    }

    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_entity, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());
        swipeRefreshLayout = (SwipeRefreshLayout) getRootView().findViewById(R.id.rooster_swiperefresh);
        setupSwipeRefreshLayout();

        if(savedInstanceState != null) {
            setEntity(savedInstanceState.getString("ENTITY"));
        }

        loadRooster();

        return getRootView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("ENTITY", getEntity());

        super.onSaveInstanceState(outState);
    }

    //endregion
    //region Statemanagement

    @Override
    protected boolean canLoadRooster() { return getEntity() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("entity", getEntity().replace("ik", Account.getApiKey())));
        return query;
    }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("entity"+getEntity()+getWeek(), getContext());
        if(lastLoad == null || lastLoad == 0) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public long getLoad() {
        return RoosterInfo.getLoad("entity"+getEntity()+getWeek(), getContext());
    }

    @Override
    public void setLoad() {
        RoosterInfo.setLoad("entity"+getEntity()+getWeek(), System.currentTimeMillis(), getContext());
    }

    //endregion
    //region Rooster

    @Override
    public void loadRooster(boolean reload) {
        if(getRootView() != null) ((TextView) getRootView().findViewById(R.id.rooster_entity_desc)).setText("Rooster voor: " + getEntity());
        super.loadRooster(reload);
    }


    //endregion
}
