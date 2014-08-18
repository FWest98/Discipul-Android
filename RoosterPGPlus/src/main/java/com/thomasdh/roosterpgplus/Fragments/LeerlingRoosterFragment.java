package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.CustomUI.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Klas;
import com.thomasdh.roosterpgplus.Models.Leerling;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import fj.data.Array;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Floris on 14-7-2014.
 */
@FragmentTitle(title = R.string.action_bar_dropdown_leerlingrooster)
public class LeerlingRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener {
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    @Getter @Setter private Klas klas;
    @Getter @Setter private Leerling leerling;
    @Getter @Setter private Array<Klas> klassen;
    @Getter @Setter private Array<Leerling> leerlingen;
    private String llToGet = null;

    private DefaultSpinner klasSpinner;
    private DefaultSpinner leerlingSpinner;
    private EditText leerlingNummerEditor;

    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_leerling, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        klasSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_leerling_klas);
        leerlingSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_leerling_naam);
        leerlingNummerEditor = (EditText) getRootView().findViewById(R.id.rooster_leerling_leerlingNummer);

        RoosterInfo.getLeerlingen(getActivity(), s -> onLeerlingenLoaded((ArrayList<Klas>) s));

        return getRootView();
    }

    public void onLeerlingenLoaded(ArrayList<Klas> result) {
        setKlassen(Array.iterableArray(result));
        if(getKlassen() == null) return;

        Array<Klas> klassen = getKlassen();

        String[] klasNamen = klassen.map(s -> s.klas).array(String[].class);

        ArrayAdapter<String> klasAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, klasNamen);
        klasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        klasSpinner.setAdapter(klasAdapter);
        klasSpinner.setOnItemSelectedListener(this);

        leerlingSpinner.setOnItemSelectedListener(this);

        leerlingen = Array.join(klassen.map(s -> Array.iterableArray(s.leerlingen)));
        leerlingNummerEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                Array<Leerling> opties = leerlingen.filter(e -> e.getLlnr().equals(text) || e.getNaam().equalsIgnoreCase(text));
                if(opties.length() > 0) {
                    setLeerling(opties.get(0));

                    llToGet = getLeerling().getNaam();
                    Klas leerlingKlas = klassen.filter(b -> Array.iterableArray(b.leerlingen).filter(a -> a.getLlnr().equals(getLeerling().getLlnr())).length() >= 1).get(0);
                    if(getKlas() == null || getKlas().klas != leerlingKlas.klas) {
                        klasSpinner.setSelection(klasAdapter.getPosition(leerlingKlas.klas));
                    } else {
                        onKlasSelected(klasAdapter.getPosition(leerlingKlas.klas));
                    }
                }
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.equals(klasSpinner)) {
            onKlasSelected(position);
        } else if(parent.equals(leerlingSpinner)) {
            onLeerlingSelected(position);
        }
    }

    public void onKlasSelected(int position) {
        setKlas(getKlassen().get(position));

        String[] leerlingNamen = Array.iterableArray(getKlas().leerlingen).map(s -> s.getNaam()).array(String[].class);
        ArrayAdapter<String> leerlingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, leerlingNamen);
        leerlingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leerlingSpinner.setAdapter(leerlingAdapter);

        if(llToGet != null) {
            leerlingSpinner.setSelection(leerlingAdapter.getPosition(llToGet));
            llToGet = null;
        }
    }

    public void onLeerlingSelected(int position) {
        setLeerling(getKlas().leerlingen.get(position));
        loadRooster();
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}


    //endregion
    //region Rooster

    @Override
    public boolean canLoadRooster() { return getLeerling() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("llnr", getLeerling().getLlnr()));
        return query;
    }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("leerling"+getLeerling().getLlnr()+getWeek(), getActivity());
        if(lastLoad == null) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public long getLoad() { return RoosterInfo.getLoad("leerling"+getLeerling().getLlnr()+getWeek(), getActivity()); }

    @Override
    public void setLoad() {
        RoosterInfo.setLoad("leerling"+getLeerling().getLlnr()+getWeek(), System.currentTimeMillis(), getActivity());
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        lesView.findViewById(R.id.optioneel_container).getBackground().setAlpha(0);
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

    //endregion
}
