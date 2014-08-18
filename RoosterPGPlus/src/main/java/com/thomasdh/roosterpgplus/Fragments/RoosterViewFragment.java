package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.CustomUI.Animations;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import roboguice.fragment.RoboFragment;

/**
 * Created by Floris on 7-7-2014.
 */
public abstract class RoosterViewFragment extends RoboFragment implements ViewPager.OnPageChangeListener, RoosterBuilder.lesViewBuilder {
    @Getter public ViewPager viewPager;
    @Getter @Setter private View rootView;
    public enum LoadType { OFFLINE, ONLINE, NEWONLINE; }

    @Getter(value = AccessLevel.PACKAGE) private int week;
    @Getter @Setter private int dag = 0;

    public interface onRoosterLoadedListener {
        public void onRoosterLoaded();
        public void onRoosterLoadStart();
    }
    @Setter private onRoosterLoadedListener roosterLoadedListener;

    //region Types
    public static Class<? extends RoosterViewFragment>[] types = new Class[]{
            PersoonlijkRoosterFragment.class,
            KlassenRoosterFragment.class,
            DocentenRoosterFragment.class,
            LeerlingRoosterFragment.class,
            LokalenRoosterFragment.class
    };

    //endregion
    //region Creating

    // Nieuwe instantie van het opgegeven type
    public static <T extends RoosterViewFragment> T newInstance(Class<T> type, int week, onRoosterLoadedListener listener) {
        T fragment = null;
        try {
            fragment = type.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        fragment.setRoosterLoadedListener(listener);
        fragment.setWeek(week, false);

        return fragment;
    }

    //endregion
    //region LifeCycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        setInternetConnectionState(HelperFunctions.hasInternetConnection(getActivity()));
    }

    //endregion
    //region Listeners

    // Pas een rooster laden als er een week bekend is (of herladen als je wil)
    public void setWeek(int week) {
        setWeek(week, true);
    }
    public void setWeek(int week, boolean loadRooster) {
        this.week = week;
        if(week == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) {
            setDag(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        } else {
            setDag(0);
        }
        if(loadRooster) {
            loadRooster();
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {}

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    public void onPageSelected(int i) { setDag(i); }

    //endregion
    //region Roosters

    protected abstract boolean canLoadRooster();
    public abstract List<NameValuePair> getURLQuery(List<NameValuePair> query);
    public abstract LoadType getLoadType();
    public abstract long getLoad();
    public abstract void setLoad();
    public boolean getShowVervangenUren() { return true; }

    // LesViewBuilder
    public abstract View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater);

    public void loadRooster() {
        loadRooster(false);
    }

    public void loadRooster(boolean reload) {
        if(!canLoadRooster()) return;
        roosterLoadedListener.onRoosterLoadStart();

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(getWeek())));
        query = getURLQuery(query);

        LoadType loadType = reload ? LoadType.ONLINE : getLoadType();

        Rooster.getRooster(query, loadType, getActivity(), (result, urenCount) -> {
            if(loadType == LoadType.ONLINE || (loadType == LoadType.NEWONLINE && HelperFunctions.hasInternetConnection(getActivity()))) {
                setLoad();
            }
            roosterLoadedListener.onRoosterLoaded();
            RoosterBuilder.build((List<Lesuur>) result, getDag(), getShowVervangenUren(), getLoad(), urenCount, getViewPager(), getActivity(), this, this);
        });
    }

    public void setInternetConnectionState(boolean hasInternetConnection) {
        TextView warning = (TextView) getRootView().findViewById(R.id.internet_connection_warning);
        if(hasInternetConnection) {
            Animations.collapse(warning);
            //warning.setVisibility(View.GONE);
        } else {
            Animations.expand(warning);
            //warning.setVisibility(View.VISIBLE);
        }
    }

    //endregion

    @Deprecated
    public enum Type {
        PERSOONLIJK_ROOSTER (0),
        KLASROOSTER (1),
        DOCENTENROOSTER (2),
        LOKALENROOSTER (3),
        LEERLINGROOSTER (4);

        Type(int id) {
            this.id = id;
        }
        private int id;
    }
}
