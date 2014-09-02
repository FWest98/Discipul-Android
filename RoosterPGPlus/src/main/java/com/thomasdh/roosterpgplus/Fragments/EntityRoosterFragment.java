package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.action_bar_dropdown_speciaal_rooster)
public class EntityRoosterFragment extends RoosterViewFragment {
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 2700000;

    @Getter @Setter private String entity;

    @Override
    protected boolean canLoadRooster() { return getEntity() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("entity", getEntity()));
        return query;
    }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("entity"+getEntity()+getWeek(), getActivity());
        if(lastLoad == null) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public long getLoad() {
        return RoosterInfo.getLoad("entity"+getEntity()+getWeek(), getActivity());
    }

    @Override
    public void setLoad() {
        RoosterInfo.setLoad("entity"+getEntity()+getWeek(), System.currentTimeMillis(), getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

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
        lokaalTextView.setText(lesuur.lokaal);
        String times = format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind);
        times = times + ", " + StringUtils.join(lesuur.klassen, " & ");
        tijdenTextView.setText(times);

        return lesView;
    }
}
