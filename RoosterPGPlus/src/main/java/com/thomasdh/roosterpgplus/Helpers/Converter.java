package com.thomasdh.roosterpgplus.Helpers;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;


public class Converter {
    private Resources resources;

    public Converter(Context context) {
        resources = context.getResources();
    }

    public float DPtoPX(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }
    public int DPtoPX(int dp) {
        return (int) DPtoPX((float) dp);
    }

    public float SPtoPX(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
    }
    public int SPtoPX(int sp) { return (int) SPtoPX((float) sp); }
}
