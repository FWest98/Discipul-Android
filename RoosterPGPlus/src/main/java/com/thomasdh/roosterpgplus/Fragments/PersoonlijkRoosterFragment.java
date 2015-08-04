package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;


@FragmentTitle(title = R.string.action_bar_dropdown_persoonlijk_rooster)
public class PersoonlijkRoosterFragment extends RoosterViewFragment {
    private static final String LOADS_NAME = "personal";
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 2700000;

    @Override
    public String getAnalyticsTitle() {
        return Constants.ANALYTICS_FRAGMENT_PERSROOSTER;
    }

    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));

        swipeRefreshLayout = (SwipeRefreshLayout) getRootView().findViewById(R.id.rooster_swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> loadRooster(true));

        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        if(!Account.isSet()) {
            Account.getInstance(getActivity()).login(getActivity(), result -> loadRooster(), result -> roosterLoadStateListener.onRoosterLoadCancel());
        } else {
            loadRooster();
        }

        return getRootView();
    }

    //endregion
    //region Statemanagement

    @Override
    public boolean canLoadRooster() { return Account.isSet() && getViewPager() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("key", Account.getApiKey()));
        return query;
    }

    @Override
    public void setLoad() { RoosterInfo.setLoad(LOADS_NAME+getWeek(), System.currentTimeMillis(), getActivity()); }

    @Override
    public long getLoad() { return RoosterInfo.getLoad(LOADS_NAME+getWeek(), getActivity()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad(LOADS_NAME+getWeek(), getActivity());
        if(lastLoad == null || lastLoad == 0) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    //endregion
    //region Rooster

    @Override
    public RoosterBuilder buildRooster(int urenCount) {
        return super.buildRooster(urenCount)
                .setShowVervangenUren(false);
    }

    //endregion
}
