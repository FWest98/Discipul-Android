package com.thomasdh.roosterpgplus.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.thomasdh.roosterpgplus.R;

import java.util.ArrayList;

public class NavigationDrawerAdapter extends BaseExpandableListAdapter {
    @Override
    public boolean areAllItemsEnabled(){
        return true;
    }

    private static final int GROUP_LAYOUT_ID = R.layout.drawer_list_item;
    private static final int CHILD_LAYOUT_ID = R.layout.drawer_list_child;


    private Context context;
    private ArrayList<String> groups;
    private ArrayList<ArrayList<String>> children;
    public NavigationDrawerAdapter(Context context, ArrayList<String> groups, ArrayList<ArrayList<String>> children) {
        this.context = context;
        this.groups = groups;
        this.children = children;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return children.get(groupPosition).get(childPosition);
    }


    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    // Return a child view. You can load your custom layout here.
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        String string = (String) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(CHILD_LAYOUT_ID, null);
        }
        TextView tv = (TextView) convertView;
        tv.setText(string);
        return convertView;
    }


    @Override
    public int getChildrenCount(int groupPosition) {
        return children.get(groupPosition).size();
    }


    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }


    @Override
    public int getGroupCount() {
        return groups.size();
    }


    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }


    // Return a group view. You can load your custom layout here.
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        String group = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(GROUP_LAYOUT_ID, null);
        }
        TextView tv = (TextView) convertView;
        tv.setText(group);
        return convertView;
    }


    @Override
    public boolean hasStableIds() {
        return true;
    }


    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }
}
