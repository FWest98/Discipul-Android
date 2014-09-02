package com.thomasdh.roosterpgplus.Adapters;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.thomasdh.roosterpgplus.Helpers.Converter;

import java.util.ArrayList;

public class MultipleUrenClickListener implements View.OnClickListener {
    private int timesToGo = 0;
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

    void animateToNextView() {
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

    void expandAll() {
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

    void fadeAwayOldView(View oldView, RelativeLayout parentLayout) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(oldView, "alpha", 0f);
        objectAnimator.setDuration(shortAnimationTime / 3);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) oldView.getLayoutParams();
                lp.setMargins(0, 0, 0, 0);
                oldView.setLayoutParams(lp);

                // There doesn't seem to be a better way to reset the alpha
                ObjectAnimator.ofFloat(oldView, "alpha", 1f)
                        .setDuration(0)
                        .start();
                shiftViews(parentLayout);
            }
        });
        objectAnimator.start();
    }

    void shiftViews(RelativeLayout parentLayout) {
        for (int ind = currentView + 1; ind < currentView + allUren.size(); ind++) {
            int uurIndex = ind % allUren.size();
            parentLayout.bringChildToFront(allUren.get(uurIndex));

            int currentMargin = ((RelativeLayout.LayoutParams) allUren.get(uurIndex).getLayoutParams()).rightMargin;
            float newMargin = Converter.convertDPToPX(8, context);

            ValueAnimator a = ValueAnimator.ofFloat(1f);
            a.addUpdateListener(animation -> {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) allUren.get(uurIndex).getLayoutParams();
                params.rightMargin = (int) (animation.getAnimatedFraction() * newMargin) + currentMargin;
                allUren.get(uurIndex).setLayoutParams(params);
            });
            a.setDuration(shortAnimationTime / 3 * 2);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (timesToGo > 0) {
                        // The user has clicked more!
                        timesToGo--;
                        animateToNextView();
                    }
                }
            });
            a.start();
        }
    }

}
