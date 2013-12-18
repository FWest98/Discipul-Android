package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Thomas on 18-12-13.
 */
public class SwitchView extends View{

    public View[] views;
    public int index;

    SwitchView(Context context, AttributeSet attributeSet, View[] views){
        super(context, attributeSet);
        this.views = views;
        this.index = 0;
    }
}
