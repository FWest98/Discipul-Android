package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
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
import com.thomasdh.roosterpgplus.Models.Leraar;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.Models.Vak;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.action_bar_dropdown_docentenrooster)
public class DocentenRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener {
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    @Getter @Setter private Vak vak;
    @Getter @Setter private String leraar;
    @Getter @Setter private ArrayList<Vak> vakken;
    private String leraarToGet = null;

    private DefaultSpinner leraarSpinner;
    private DefaultSpinner vakSpinner;

    @Override
    public boolean canLoadRooster() { return getLeraar() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("docent", getLeraar()));
        return query;
    }

    @Override
    public long getLoad() { return RoosterInfo.getLoad("docent"+getLeraar()+getWeek(), getActivity()); }

    @Override
    public void setLoad() { RoosterInfo.setLoad("docent" + getLeraar() + getWeek(), System.currentTimeMillis(), getActivity()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("docent"+getLeraar()+getWeek(), getActivity());
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

        setRootView(inflater.inflate(R.layout.fragment_main_docenten, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        leraarSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_naam);
        vakSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_vak);

        if(savedInstanceState == null) {
            RoosterInfo.getLeraren(getActivity(), s -> onLerarenLoaded((ArrayList<Vak>) s));
        } else {
            setVak((Vak) savedInstanceState.getSerializable("VAK"));
            setLeraar(savedInstanceState.getString("LERAAR"));
            RoosterInfo.getLeraren(getActivity(), s -> onLerarenLoaded((ArrayList<Vak>) s, getLeraar(), getVak()));
        }

        return getRootView();
    }

    public void onLerarenLoaded(ArrayList<Vak> result) {
        onLerarenLoaded(result, null, null);
    }

    public void onLerarenLoaded(ArrayList<Vak> result, String Sleraar, Vak Svak) {
        if(result == null) return;
        setVakken(result);

        ArrayList<String> vakNamen = new ArrayList<>();
        for(Vak vak : vakken) { vakNamen.add(vak.getNaam()); }

        ArrayAdapter<String> vakAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, vakNamen.toArray(new String[vakNamen.size()]));
        vakAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vakSpinner.setAdapter(vakAdapter);
        vakSpinner.setOnItemSelectedListener(this);
        leraarSpinner.setOnItemSelectedListener(this);

        vakSpinner.setSelection(vakAdapter.getPosition(Svak.getNaam()));
        leraarToGet = Sleraar;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.equals(vakSpinner)) {
            onVakSelected(position);
        } else if(parent.equals(leraarSpinner)) {
            onLeraarSelected(position);
        }
    }

    public void onVakSelected(int position) {
        setVak(vakken.get(position));

        ArrayList<String> leraarNamen = new ArrayList<>();
        for(Leraar leraar : getVak().leraren) { leraarNamen.add(leraar.naam); }

        ArrayAdapter<String> leraarAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, leraarNamen.toArray(new String[leraarNamen.size()]));
        leraarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leraarSpinner.setAdapter(leraarAdapter);

        if(leraarToGet != null) {
            leraarSpinner.setSelection(leraarAdapter.getPosition(leraarToGet));
            leraarToGet = null;
        }
    }

    public void onLeraarSelected(int position) {
        setLeraar(getVak().getLeraren().get(position).getCode());
        loadRooster();
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        lesView.findViewById(R.id.optioneel_container).getBackground().setAlpha(0); // Background doorzichtig, geen speciale uren
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        TextView vakTextView = (TextView) lesView.findViewById(R.id.rooster_vak);
        TextView leraarTextView = (TextView) lesView.findViewById(R.id.rooster_leraar);
        TextView lokaalTextView = (TextView) lesView.findViewById(R.id.rooster_lokaal);
        TextView tijdenTextView = (TextView) lesView.findViewById(R.id.rooster_tijden);

        vakTextView.setText(lesuur.vak);
        leraarTextView.setText(StringUtils.join(lesuur.klassen, " & "));
        lokaalTextView.setText(lesuur.lokaal);
        tijdenTextView.setText(format.format(lesuur.lesStart) + " - " + format.format(lesuur.lesEind));

        return lesView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("LERAAR", leraar);
        outState.putSerializable("VAK", vak);

        super.onSaveInstanceState(outState);
    }
}
