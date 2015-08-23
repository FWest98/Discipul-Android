package com.thomasdh.roosterpgplus;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Settings.AchtergrondFragment;
import com.thomasdh.roosterpgplus.Settings.Constants;
import com.thomasdh.roosterpgplus.Settings.InfoFragment;
import com.thomasdh.roosterpgplus.Settings.OverigFragment;
import com.thomasdh.roosterpgplus.Settings.UserFragment;

import java.util.List;


public class PreferencesActivity extends PreferenceActivity {

    private Toolbar toolbar;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return UserFragment.class.getName().equals(fragmentName) ||
                InfoFragment.class.getName().equals(fragmentName) ||
                OverigFragment.class.getName().equals(fragmentName) ||
                AchtergrondFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Account.initialize(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_preferences_toolbar, root, false);
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(getTitle());
            root.addView(toolbar, 0);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);

            root.removeAllViews();

            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.activity_preferences_toolbar, root, false);

            int height;
            TypedValue typedValue = new TypedValue();
            if(getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
                height = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
            } else {
                height = toolbar.getHeight();
            }

            content.setPadding(0, height, 0, 0);
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(getTitle());

            root.addView(content);
            root.addView(toolbar);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View result = super.onCreateView(name, context, attrs);
        if(result != null) return result;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            switch(name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    @Override
    public boolean hasHeaders() {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_SETTINGS_MAIN);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}
