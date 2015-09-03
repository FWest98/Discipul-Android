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
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.Apache.BasicNameValuePair;
import com.thomasdh.roosterpgplus.Helpers.Apache.NameValuePair;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;


@FragmentTitle(title = R.string.action_bar_dropdown_persoonlijk_rooster)
public class PersoonlijkRoosterFragment extends RoosterViewFragment implements RoosterBuilder.BuilderFunctions {
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
        Account.initialize(getContext());

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));

        swipeRefreshLayout = (SwipeRefreshLayout) getRootView().findViewById(R.id.rooster_swiperefresh);
        setupSwipeRefreshLayout();

        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        if(!Account.isSet()) {
            Account.getInstance(getContext()).login(getActivity(), result -> loadRooster(), result -> swipeRefreshLayout.setRefreshing(false));
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
    public void setLoad() { RoosterInfo.setLoad(LOADS_NAME+getWeek(), System.currentTimeMillis(), getContext()); }

    @Override
    public long getLoad() { return RoosterInfo.getLoad(LOADS_NAME+getWeek(), getContext()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad(LOADS_NAME+getWeek(), getContext());
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
                .setBuilderFunctions(this)
                .setShowVervangenUren(false);
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        TextView vakTextView = (TextView) lesView.findViewById(R.id.rooster_vak);
        TextView leraarTextView = (TextView) lesView.findViewById(R.id.rooster_leraar);
        TextView lokaalTextView = (TextView) lesView.findViewById(R.id.rooster_lokaal);
        TextView tijdenTextView = (TextView) lesView.findViewById(R.id.rooster_tijden);

        if(Account.getUserType() == Account.UserType.LEERLING) {
            // Leerlingrooster
            vakTextView.setText(lesuur.vak);
            leraarTextView.setText(StringUtils.join(lesuur.leraren, " & "));
            lokaalTextView.setText(lesuur.lokaal);
            tijdenTextView.setText(format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind));
        } else {
            // Leraarrooster
            vakTextView.setText(lesuur.vak);
            leraarTextView.setText(StringUtils.join(lesuur.klassen, " & "));
            lokaalTextView.setText(lesuur.lokaal);
            tijdenTextView.setText(format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind));
        }

        return lesView;
    }

    //endregion
}
