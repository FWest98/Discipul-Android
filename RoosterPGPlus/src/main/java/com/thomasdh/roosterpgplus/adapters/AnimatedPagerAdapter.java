package com.thomasdh.roosterpgplus.Adapters;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class AnimatedPagerAdapter extends PagerAdapter {
    private final ArrayList<View> views = new ArrayList<>();

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

    @TargetApi(12)
    public void setView(View newView, int position, Context context) {
        if (views.size() <= position) {
            FrameLayout parent = new FrameLayout(context);
            parent.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            views.add(parent);
        }

        final FrameLayout parent = (FrameLayout) views.get(position);
        parent.removeAllViews();
        parent.addView(newView);
    }
}
