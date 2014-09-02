package com.thomasdh.roosterpgplus.Helpers;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;


public class Converter {
    private Resources resources;

    public static float convertDPToPX(float pixel, Context c) {
        Resources r = c.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel, r.getDisplayMetrics());
    }

    public Converter(Context context) {
        resources = context.getResources();
    }
    public float DPtoPX(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }
    public int DPtoPX(int dp) {
        return (int) DPtoPX((float) dp);
    }
}
