package com.thomasdh.roosterpgplus;

import java.util.Calendar;

/**
 * Created by Floris on 11-12-13.
 */
public class RoosterNextUurDecider {
    public int week;
    public int dag;
    public int uur;


    public RoosterNextUurDecider() {
        this.week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);


    }
}
