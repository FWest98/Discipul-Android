package com.thomasdh.roosterpgplus.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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

    @TargetApi(12)
    public void setView(View newView, int position, Context context) {
        if (views.size() <= position) {
            FrameLayout parent = new FrameLayout(context);
            parent.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            views.add(parent);
        }

        final FrameLayout parent = (FrameLayout) views.get(position);
        boolean animationsBeschikbaar = Build.VERSION.SDK_INT >= 12;

        if (animationsBeschikbaar) {
            int shortAnimationDuration = context.getResources().getInteger(
                    android.R.integer.config_shortAnimTime);
            parent.addView(newView);
            newView.setAlpha(0f);
            newView.setVisibility(View.VISIBLE);
            newView.bringToFront();
            newView.animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (parent.getChildCount() > 1) {
                                parent.removeViewAt(0);
                            }
                        }
                    });
        } else {
            parent.removeAllViews();
            parent.addView(newView);
        }
    }

}
