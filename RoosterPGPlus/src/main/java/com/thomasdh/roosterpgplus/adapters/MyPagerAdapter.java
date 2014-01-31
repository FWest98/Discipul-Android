package com.thomasdh.roosterpgplus.adapters;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by Thomas on 27-11-13.
 */
public class MyPagerAdapter extends PagerAdapter {
    private final ArrayList<View> views = new ArrayList<View>();

    @Override
    public int getCount() {
        return views.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }

    @Override
    public int getItemPosition(Object object) {
        int index = views.indexOf(object);
        if (index == -1)
            return POSITION_NONE;
        else
            return index;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = views.get(position);
        container.addView(v);
        return v;
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void deleteItems() {
        views.clear();
    }

    public void addView(View v) {
        views.add(v);
    }

    public void setView(View v, int position) {
        if (views.size() <= position) {
            views.add(position, v);
        } else {
            views.set(position, v);
        }
    }

}
