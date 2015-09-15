package com.thomasdh.roosterpgplus.Adapters;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.ValueAnimator;
import com.thomasdh.roosterpgplus.Helpers.Converter;

import java.util.ArrayList;

public class MultipleUrenClickListener implements View.OnClickListener {
    private int currentView;
    private final ArrayList<RelativeLayout> allUren;
    private final int shortAnimationTime;
    private boolean withAnimations;

    private int maxWidth = -1;
    private int minWidth;

    public MultipleUrenClickListener(ArrayList<RelativeLayout> allUren, Context context) {
        currentView = allUren.size();
        this.allUren = allUren;
        shortAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);

        minWidth = new Converter(context).DPtoPX(13);
        allUren.get(currentView - 1).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                maxWidth = allUren.get(currentView - 1).getWidth();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    allUren.get(currentView - 1).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    allUren.get(currentView - 1).getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        withAnimations = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("animaties", true);
    }

    @Override
    public void onClick(View v) {
        currentView--;
        if (currentView <= 0) {
            currentView = allUren.size();
            expandAll();
        } else {
            animateToNextView();
        }
    }

    private void animateToNextView() {
        RelativeLayout view = allUren.get(currentView);

        if(withAnimations) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(maxWidth, minWidth);
            valueAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                newLayoutParams.width = value;
                view.setLayoutParams(newLayoutParams);
            });
            valueAnimator.setDuration(shortAnimationTime / 2 * 3);
            valueAnimator.start();
        } else {
            RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            newLayoutParams.width = minWidth;
            view.setLayoutParams(newLayoutParams);
        }
    }

    private void expandAll() {
        for(int i = 1; i < allUren.size(); i++) {
            RelativeLayout view = allUren.get(i);

            if(withAnimations) {
                ValueAnimator valueAnimator = ValueAnimator.ofInt(minWidth, maxWidth);
                valueAnimator.addUpdateListener(animation -> {
                    int value = (int) animation.getAnimatedValue();
                    RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    newLayoutParams.width = value;
                    view.setLayoutParams(newLayoutParams);
                });
                valueAnimator.setDuration(shortAnimationTime / 2 * 3);
                valueAnimator.start();
            } else {
                RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                newLayoutParams.width = maxWidth;
                view.setLayoutParams(newLayoutParams);
            }
        }
    }
}
