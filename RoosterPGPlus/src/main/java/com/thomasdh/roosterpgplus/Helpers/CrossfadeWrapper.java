package com.thomasdh.roosterpgplus.Helpers;

import com.mikepenz.crossfader.Crossfader;
import com.mikepenz.materialdrawer.interfaces.ICrossfader;

public class CrossfadeWrapper implements ICrossfader {
    private Crossfader crossfader;

    public CrossfadeWrapper(Crossfader crossfader) {
        this.crossfader = crossfader;
    }

    @Override
    public void crossfade() {
        crossfader.crossFade();
    }

    @Override
    public boolean isCrossfaded() {
        return crossfader.isCrossFaded();
    }
}
