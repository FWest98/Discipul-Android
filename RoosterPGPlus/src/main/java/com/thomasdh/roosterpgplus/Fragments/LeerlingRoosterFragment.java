package com.thomasdh.roosterpgplus.Fragments;

import android.view.LayoutInflater;
import android.view.View;

import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;

import java.util.List;

/**
 * Created by Floris on 14-7-2014.
 */
@FragmentTitle(title = R.string.action_bar_dropdown_leerlingrooster)
public class LeerlingRoosterFragment extends RoosterViewFragment {
    @Override
    public Type getType() {
        return Type.LEERLINGROOSTER;
    }

    @Override
    public boolean canLoadRooster() { return false; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        throw new UnsupportedOperationException("Nog niet af"); // TODO implement
    }

    @Override
    public LoadType getLoadType() {
        throw new UnsupportedOperationException("Nog niet af");
        // TODO implement
    }

    @Override
    public long getLoad() { throw new UnsupportedOperationException("Nog niet af"); }

    @Override
    public void setLoad() {
        throw new UnsupportedOperationException("Nog niet af");
    }

    @Override
    public View fillLesView(Lesuur lesuur, View lesView, LayoutInflater inflater) {
        throw new UnsupportedOperationException("Nog niet af");
    }
}
