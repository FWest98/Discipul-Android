package com.thomasdh.roosterpgplus.Adapters;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ActionBarSpinnerAdapter implements SpinnerAdapter {
    @Getter
    private Class<?> type;
    private final Activity activity;
    private final List<String> data;
    private ArrayList<DataSetObserver> observers = new ArrayList<>();

    public ActionBarSpinnerAdapter(Activity activity, List<String> data, Class<?> type) {
        this.activity = activity;
        this.data = data;
        setType(type);
    }

    public void setType (Class<?> type) {
        this.type = type;
        for(DataSetObserver observer : observers) {
            observer.onChanged();
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null && activity!= null) {
            activity.setTheme(R.style.AppTheme);
            LayoutInflater vi = activity.getLayoutInflater();
            convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        if (convertView != null) {
            ((TextView) convertView).setText(data.get(position));
        }
        return convertView;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (activity != null) {
            View view = View.inflate(activity, R.layout.action_bar_list_view, null);
            ((TextView) view.findViewById(R.id.action_bar_text_field)).setText(data.get(position));

            int typeRes = type.getAnnotation(FragmentTitle.class).title();
            ((TextView) view.findViewById(R.id.action_bar_dropdown_type)).setText(activity.getString(typeRes));
            return view;
        }
        return null;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }


}


