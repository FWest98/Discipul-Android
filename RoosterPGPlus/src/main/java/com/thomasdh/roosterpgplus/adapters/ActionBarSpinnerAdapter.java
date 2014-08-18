package com.thomasdh.roosterpgplus.Adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ActionBarSpinnerAdapter implements SpinnerAdapter {
    @Getter
    private Class<? extends RoosterViewFragment> type;
    private final WeakReference<Context> context;
    private final List<String> data;
    private ArrayList<DataSetObserver> observers = new ArrayList<>();

    public ActionBarSpinnerAdapter(Context context, List<String> data, Class<? extends RoosterViewFragment> type) {
        this.context = new WeakReference<>(context);
        this.data = data;
        setType(type);
    }

    public void setType (Class<? extends RoosterViewFragment> type) {
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
        if (convertView == null && context.get() != null) {
            LayoutInflater vi = (LayoutInflater) context.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) convertView).setText(data.get(position));
        return convertView;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (context.get() != null) {
            View view = View.inflate(context.get(), R.layout.action_bar_list_view, null);
            ((TextView) view.findViewById(R.id.action_bar_text_field)).setText(data.get(position));

            int typeRes = type.getAnnotation(FragmentTitle.class).title();
            ((TextView) view.findViewById(R.id.action_bar_dropdown_type)).setText(context.get().getString(typeRes));
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


