package com.thomasdh.roosterpgplus.Settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.thomasdh.roosterpgplus.R;

public class ThemedPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTheme(R.style.PreferenceTheme);
    }
}
