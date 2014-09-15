package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.WebDownloader;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.PGTVPage;
import com.thomasdh.roosterpgplus.R;

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

        private PGTVType(String urlQuery, String desc) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.rooster_viewPager);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        loadRooster();

        return getRootView();
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
        roosterLoadStateListener.onRoosterLoadStart();

        WebDownloader.getPGTVRooster(type.toString(), result -> {
            roosterLoadStateListener.onRoosterLoadEnd();
            ArrayList<PGTVPage> data = (ArrayList<PGTVPage>) result;
            new PGTVBuilder().build(data);
        }, e -> {
            roosterLoadStateListener.onRoosterLoadEnd();
            new PGTVBuilder().build(null);
        });
    }

    private class PGTVBuilder {
        public void build(ArrayList<PGTVPage> data) {
            if(getViewPager() == null) return;
            if(getViewPager().getAdapter() == null) getViewPager().setAdapter(new AnimatedPagerAdapter());
            getViewPager().setOnPageChangeListener(null);

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
                ((AnimatedPagerAdapter) getViewPager().getAdapter()).setView(dagView, i, getActivity());
            }

            getViewPager().getAdapter().notifyDataSetChanged();
        }
    }
}
