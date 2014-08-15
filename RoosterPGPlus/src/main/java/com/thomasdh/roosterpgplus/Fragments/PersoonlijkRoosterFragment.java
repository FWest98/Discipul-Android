package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
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
    public boolean canLoadRooster() { return Account.isSet(); }

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

        if(!Account.isSet()) {
            Account.getInstance(getActivity()).login(result -> loadRooster());
        }

        return getRootView();
    }

    @Override
    public boolean getShowVervangenUren() {
        return false;
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
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
    }
}
