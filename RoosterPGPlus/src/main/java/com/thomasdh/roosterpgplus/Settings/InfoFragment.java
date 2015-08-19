package com.thomasdh.roosterpgplus.Settings;

import android.os.Bundle;

import com.mikepenz.aboutlibraries.Libs;
import com.thomasdh.roosterpgplus.CustomUI.LibsFragment;
import com.thomasdh.roosterpgplus.R;

public class InfoFragment extends LibsFragment {
    public InfoFragment() {

        Bundle bundle = new Bundle();

        bundle.putInt(Libs.BUNDLE_THEME, R.style.AppTheme);
        bundle.putBoolean(Libs.BUNDLE_VERSION, false);

        bundle.putStringArray(Libs.BUNDLE_FIELDS, Libs.toStringArray(R.string.class.getFields()));
        bundle.putStringArray(Libs.BUNDLE_LIBS, new String[]{
                "projectlombok", "ormlite", "functionaljava", "acra", "rxjava"
        });

        bundle.putBoolean(Libs.BUNDLE_VERSION, true);
        bundle.putBoolean(Libs.BUNDLE_LICENSE, true);

        setArguments(bundle);
    }
}
