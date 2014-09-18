package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fwest98.showcaseview.ShowcaseView;
import com.fwest98.showcaseview.targets.ActionViewTarget;
import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
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

public abstract class RoosterViewFragment extends RoboFragment implements ViewPager.OnPageChangeListener {
    @Getter
    ViewPager viewPager;
    @Getter @Setter private View rootView;
    public enum LoadType { OFFLINE, ONLINE, NEWONLINE, REFRESH }

    @Getter(value = AccessLevel.PACKAGE) private int week;
    @Getter @Setter private int dag = 0;

    public interface onRoosterLoadStateChangedListener {
        public void onRoosterLoadEnd();
        public void onRoosterLoadCancel();
        public void onRoosterLoadStart();
    }
    @Setter onRoosterLoadStateChangedListener roosterLoadStateListener;

    private boolean hadInternetConnection = true;

    //region Types
    public static Class<? extends RoosterViewFragment>[] types = new Class[]{
            PersoonlijkRoosterFragment.class,
            KlassenRoosterFragment.class,
            DocentenRoosterFragment.class,
            LokalenRoosterFragment.class,
            LeerlingRoosterFragment.class,
            EntityRoosterFragment.class,
            PGTVRoosterFragment.class
    };

    //endregion
    //region Creating

    // Nieuwe instantie van het opgegeven type
    public static <T extends RoosterViewFragment> T newInstance(Class<T> type, int week, onRoosterLoadStateChangedListener listener) {
        T fragment;
        try {
            fragment = type.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        fragment.setRoosterLoadStateListener(listener);
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
            setDag(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
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
    protected abstract List<NameValuePair> getURLQuery(List<NameValuePair> query);
    protected abstract LoadType getLoadType();
    protected abstract long getLoad();
    protected abstract void setLoad();

    public void loadRooster() {
        loadRooster(false);
    }

    public void loadRooster(boolean reload) {
        if(!canLoadRooster()) return;
        if(getWeek() == -1) return;
        roosterLoadStateListener.onRoosterLoadStart();

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(getWeek())));
        query = getURLQuery(query);

        LoadType loadType = reload ? LoadType.REFRESH : getLoadType();

        Rooster.getRooster(query, loadType, getActivity(), (result, urenCount) -> {
            if(loadType == LoadType.ONLINE || loadType == LoadType.REFRESH || loadType == LoadType.NEWONLINE && HelperFunctions.hasInternetConnection(getActivity())) {
                setLoad();
            }
            roosterLoadStateListener.onRoosterLoadEnd();
            buildRooster(urenCount).build((List<Lesuur>) result);
        }, exception -> {
            roosterLoadStateListener.onRoosterLoadEnd();
            if(loadType != LoadType.REFRESH) {
                buildRooster(0).build((List<Lesuur>) null);
            }
        });

        if(HelperFunctions.showCaseView()) {
            new ShowcaseView.Builder(getActivity())
                    .setTarget(new ActionViewTarget(getActivity(), ActionViewTarget.Type.SPINNER))
                    .setContentTitle(R.string.showcaseview_weekkeuze_title)
                    .setContentText(R.string.showcaseview_weekkeuze_content)
                    .doNotBlockTouches()
                    .singleShot(1)
                    .setStyle(R.style.ShowCaseTheme)
                    .build();
        }
    }

    RoosterBuilder buildRooster(int urenCount) {
        if(getViewPager().getAdapter() == null) getViewPager().setAdapter(new AnimatedPagerAdapter());
        getViewPager().getAdapter().notifyDataSetChanged();
        getViewPager().setOnPageChangeListener(this);
        return new RoosterBuilder(getActivity())
                .in(getViewPager())
                .setShowDag(getDag())
                .setShowVervangenUren(true)
                .setLastLoad(getLoad())
                .setUrenCount(urenCount);
    }

    public void setInternetConnectionState(boolean hasInternetConnection) {
        TextView warning = (TextView) getRootView().findViewById(R.id.internet_connection_warning);
        if(!hadInternetConnection && hasInternetConnection) {
            loadRooster(true);
        }
        hadInternetConnection = hasInternetConnection;
        if(hasInternetConnection) {
            Animations.collapse(warning);
        } else {
            Animations.expand(warning);
        }
    }

    //endregion
}
