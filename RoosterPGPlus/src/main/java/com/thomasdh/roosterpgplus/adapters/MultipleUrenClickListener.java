package com.thomasdh.roosterpgplus.Adapters;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.ValueAnimator;
import com.thomasdh.roosterpgplus.Helpers.Converter;

import java.util.ArrayList;

public class MultipleUrenClickListener implements View.OnClickListener {
    private int currentView;
    private final ArrayList<RelativeLayout> allUren;
    private final int shortAnimationTime;
    private final Context context;

    private int maxWidth = -1;
    private int minWidth;

    public MultipleUrenClickListener(ArrayList<RelativeLayout> allUren, Context context) {
        currentView = allUren.size();
        this.allUren = allUren;
        this.context = context;
        shortAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);

        minWidth = new Converter(context).DPtoPX(9);
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

        if(maxWidth == -1) {
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(spec, spec);
            maxWidth = new Converter(context).DPtoPX(view.getMeasuredWidth());
        }

        ValueAnimator valueAnimator = ValueAnimator.ofInt(maxWidth, minWidth);
        valueAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            newLayoutParams.width = value;
            view.setLayoutParams(newLayoutParams);
        });
        valueAnimator.setDuration(shortAnimationTime / 2 * 3);
        valueAnimator.start();
    }

    private void expandAll() {
        for(int i = 1; i < allUren.size(); i++) {
            RelativeLayout view = allUren.get(i);

            ValueAnimator valueAnimator = ValueAnimator.ofInt(minWidth, maxWidth);
            valueAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                newLayoutParams.width = value;
                view.setLayoutParams(newLayoutParams);
            });
            valueAnimator.setDuration(shortAnimationTime / 2 * 3);
            valueAnimator.start();
        }
    }
}
