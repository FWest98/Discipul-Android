package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.thomasdh.roosterpgplus.Helpers.Converter;

import java.util.ArrayList;

/**
 * Created by Thomas on 20-5-2014.
 */
public class MultipleUrenClickListener implements View.OnClickListener {

    private boolean animating = false;
    private int timesToGo = 0;
    private int highestView;
    private final ArrayList<RelativeLayout> allUren;
    private final int shortAnimationTime;
    private final Context context;

    public MultipleUrenClickListener(ArrayList<RelativeLayout> allUren, Context context) {
        highestView = 0;
        this.allUren = allUren;
        this.context = context;
        shortAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onClick(View v) {
        if (animating) {
            // Wait until the current animation is done
            timesToGo++;
        } else {
            RelativeLayout parentLayout = (RelativeLayout) v;

            highestView--;
            if (highestView < 0)
                highestView += allUren.size();

            animateToNextView(parentLayout);
        }
    }

    void animateToNextView(RelativeLayout parentLayout) {
        animating = true;
        View oldView = allUren.get(highestView);
        fadeAwayOldView(oldView, parentLayout);
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
        for (int ind = highestView + 1; ind < highestView + allUren.size(); ind++) {
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
                    animating = false;
                    timesToGo--;
                    if (timesToGo > 0) {
                        // The user has clicked more!
                        animateToNextView(parentLayout);
                    }
                }
            });
            a.start();
        }
    }

}
