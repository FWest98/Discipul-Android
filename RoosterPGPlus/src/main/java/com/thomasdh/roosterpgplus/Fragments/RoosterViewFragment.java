package com.thomasdh.roosterpgplus.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.CustomUI.Animations;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.RoosterWeek;
import com.thomasdh.roosterpgplus.MainActivity;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.RoosterBuilderOld;
import com.thomasdh.roosterpgplus.RoosterDownloaderOld;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.lang.ref.WeakReference;
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
    private static final String STATE_FRAGMENT = "fragmentType";

    @Deprecated public static String leraarLeerlingselected;

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
            LokalenRoosterFragment.class,
            LeerlingRoosterFragment.class
    };

    @Deprecated
    public abstract Type getType();

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

        Bundle args = new Bundle();
        args.putSerializable(STATE_FRAGMENT, fragment.getType());
        fragment.setArguments(args);

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
    public void laadRooster(Context context, View rootView, Type type) {
        if (getWeek() == -1){
            return;
        }
        int selectedWeek = MainActivity.getSelectedWeek();
        WeakReference<MenuItem> refreshItem = null;

        Log.d("MainActivity", "Rooster aan het laden van week " + selectedWeek);
        if (type == Type.PERSOONLIJK_ROOSTER) {
            //Probeer de string uit het geheugen te laden
            RoosterWeek roosterWeek = RoosterWeek.laadUitGeheugen(selectedWeek, getActivity());

            //Als het de goede week is, gebruik hem
            if (roosterWeek != null && roosterWeek.getWeek() == selectedWeek) {
                new RoosterBuilderOld(context, (ViewPager) rootView.findViewById(R.id.viewPager), selectedWeek, type).buildLayout(roosterWeek);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                new RoosterDownloaderOld(context, rootView, false, refreshItem.get(), selectedWeek).execute();
            } else {
                if (roosterWeek == null) {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is null");
                } else {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is van week " + roosterWeek.getWeek() + ", de gewilde week is " + MainActivity.getSelectedWeek());
                }
                new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek).execute();
            }
        } else if (type == Type.KLASROOSTER) {
            new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek, leraarLeerlingselected, type).execute();
        } else if (type == Type.DOCENTENROOSTER) {
            new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek, leraarLeerlingselected, type).execute();
        }
    }

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
