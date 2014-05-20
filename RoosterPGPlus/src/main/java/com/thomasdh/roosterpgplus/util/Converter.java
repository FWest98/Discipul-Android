package com.thomasdh.roosterpgplus.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by Thomas on 20-5-2014.
 */
public class Converter {

    public static float convertDPToPX(float pixel, Context c) {
        Resources r = c.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel, r.getDisplayMetrics());
    }
}
