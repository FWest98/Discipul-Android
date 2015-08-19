package com.thomasdh.roosterpgplus.Adapters;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.R;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ActionBarSpinnerAdapter extends BaseAdapter {
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
        if(convertView == null || !convertView.getTag().toString().equals("DROPDOWN")) {
            convertView = activity.getLayoutInflater().inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);
            convertView.setTag("DROPDOWN");
        }

        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(data.get(position));

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (activity != null) {
            View view = View.inflate(activity, R.layout.action_bar_list_view, null);
            ((TextView) view.findViewById(R.id.action_bar_text_field)).setText(data.get(position));

            //int typeRes = type.getAnnotation(FragmentTitle.class).title();
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


