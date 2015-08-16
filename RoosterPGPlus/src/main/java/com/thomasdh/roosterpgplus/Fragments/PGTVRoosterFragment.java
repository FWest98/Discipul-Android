package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.WebDownloader;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.PGTVPage;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.PGTV_Title)
public class PGTVRoosterFragment extends RoosterViewFragment {

    public enum PGTVType {
        ROOSTER("rooster", "Rooster"), MEDEDELINGEN("nieuws", "Nieuws");

        private String urlQuery;
        private String desc;

        PGTVType(String urlQuery, String desc) {
            this.urlQuery = urlQuery;
            this.desc = desc;
        }
        @Override
        public String toString() {
            return urlQuery;
        }

        public String toDesc() {
            return desc;
        }
    }
    @Getter @Setter private PGTVType type;

    @Override
    public String getAnalyticsTitle() {
        switch(type) {
            case ROOSTER:
                return Constants.ANALYTICS_FRAGMENT_PGTVROOSTER;
            default:
                return Constants.ANALYTICS_FRAGMENT_PGTVALGEM;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            type = (PGTVType) savedInstanceState.getSerializable("TYPE");
        } else if(type == null) {
            type = PGTVType.ROOSTER;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));

        swipeRefreshLayout = (SwipeRefreshLayout) getRootView().findViewById(R.id.rooster_swiperefresh);
        setupSwipeRefreshLayout();

        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        loadRooster();

        return getRootView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("TYPE", type);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected boolean canLoadRooster() {
        return false;
    }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        return query;
    }

    @Override
    public LoadType getLoadType() {
        return null;
    }

    @Override
    public long getLoad() {
        return 0;
    }

    @Override
    public void setLoad() {}

    @Override
    public void loadRooster(boolean reload) {
        // PGTV laden
        if(type == null) return;
        if(swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        WebDownloader.getPGTVRooster(type.toString(), result -> {
            swipeRefreshLayout.setRefreshing(false);
            ArrayList<PGTVPage> data = (ArrayList<PGTVPage>) result;
            new PGTVBuilder().build(data, this);
        }, e -> {
            swipeRefreshLayout.setRefreshing(false);
            new PGTVBuilder().build(null, this);
        }, getActivity());
    }

    private class PGTVBuilder {
        public void build(ArrayList<PGTVPage> data, RoosterViewFragment fragment) {
            if(getViewPager() == null) return;
            if(getViewPager().getAdapter() == null) getViewPager().setAdapter(new AnimatedPagerAdapter());
            getViewPager().addOnPageChangeListener(fragment);

            if(data == null || data.isEmpty()) {
                View noContentView = LayoutInflater.from(getActivity()).inflate(R.layout.pgtv_null, null);
                ((AnimatedPagerAdapter) getViewPager().getAdapter()).setView(noContentView, 0, getActivity());
                getViewPager().getAdapter().notifyDataSetChanged();

                return;
            }

            int i = -1;
            for(PGTVPage page : data) {
                i++;
                View dagView = LayoutInflater.from(getActivity()).inflate(R.layout.pgtv_dag, null);

                /* Titel */
                TextView title = (TextView) dagView.findViewById(R.id.pgtv_dag_titel);
                title.setText(page.title);

                TextView desc = (TextView) dagView.findViewById(R.id.pgtv_dag_content);
                desc.setText(page.desc);

                ScrollView scrollView = (ScrollView) dagView.findViewById(R.id.scrollview);
                final int finalI = i;
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    int scrollY = scrollView.getScrollY();

                    fragment.OnScroll(scrollY, finalI);
                });

                ((AnimatedPagerAdapter) getViewPager().getAdapter()).setView(dagView, i, getActivity());
            }

            getViewPager().getAdapter().notifyDataSetChanged();
        }
    }
}
