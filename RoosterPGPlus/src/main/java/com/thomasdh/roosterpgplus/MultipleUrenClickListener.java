package com.thomasdh.roosterpgplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import com.thomasdh.roosterpgplus.util.Converter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Thomas on 20-5-2014.
 */
public class MultipleUrenClickListener implements View.OnClickListener {

    boolean animating = false;
    int timesToGo = 0;
    int highestView;
    ArrayList<View> allUren;
    final int shortAnimationTime;
    WeakReference<Context> context;

    public MultipleUrenClickListener(ArrayList<View> allUren, WeakReference<Context> context) {
        this.highestView = 0;
        this.allUren = allUren;
        this.context = context;
        shortAnimationTime = context.get().getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override

    public void onClick(View v) {
        if (animating) {
            // Wait until the current animation is done
            timesToGo++;
        } else {
            final RelativeLayout parentLayout = (RelativeLayout) v;

            highestView--;
            if (highestView < 0)
                highestView += allUren.size();

            if (Build.VERSION.SDK_INT >= 12 && PreferenceManager.getDefaultSharedPreferences(context.get()).getBoolean("animaties", true)) {
                // Use fancy animations
                animateToNextView(parentLayout);
            } else {
                // Use the boring way to switch to the next view
                View newView = allUren.get(highestView < 1 ? allUren.size() - 1 : highestView - 1);
                if (highestView < 1) {
                    for (int c = 0; c < allUren.size(); c++) {
                        parentLayout.bringChildToFront(allUren.get(c));
                    }
                }
                parentLayout.bringChildToFront(newView);
                parentLayout.invalidate();
            }
        }
    }

    @TargetApi(12)
    void animateToNextView (final RelativeLayout parentLayout){
        animating = true;

        final View oldView = allUren.get(highestView);
        oldView.animate().alpha(0f).setDuration(shortAnimationTime / 3).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) oldView.getLayoutParams();
                lp.setMargins(0, 0, 0, 0);
                oldView.setLayoutParams(lp);
                oldView.setAlpha(100f);

                for (int ind = highestView + 1; ind < highestView + allUren.size(); ind++) {
                    final int uurIndex = ind % allUren.size();
                    parentLayout.bringChildToFront(allUren.get(uurIndex));

                    final int currentMargin = ((RelativeLayout.LayoutParams) allUren.get(uurIndex).getLayoutParams()).rightMargin;
                    final float newMargin = Converter.convertDPToPX(8, context.get());

                    Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) allUren.get(uurIndex).getLayoutParams();
                            params.rightMargin = (int) (interpolatedTime * newMargin) + currentMargin;
                            allUren.get(uurIndex).setLayoutParams(params);
                        }
                    };
                    a.setDuration(shortAnimationTime / 3 * 2);
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            // The user has clicked more!
                            animating = false;
                            timesToGo--;
                            if (timesToGo > 0){
                                animateToNextView(parentLayout);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    allUren.get(uurIndex).startAnimation(a);
                }
            }
        });
    }

}
