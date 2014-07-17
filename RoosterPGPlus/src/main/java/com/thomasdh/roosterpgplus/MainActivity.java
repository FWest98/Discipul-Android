package com.thomasdh.roosterpgplus;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.thomasdh.roosterpgplus.Adapters.ActionBarSpinnerAdapter;
import com.thomasdh.roosterpgplus.Adapters.NavigationDrawerAdapter;
import com.thomasdh.roosterpgplus.Database.DatabaseHelper;
import com.thomasdh.roosterpgplus.Fragments.PersoonlijkRoosterFragment;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Models.Week;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import lombok.Getter;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;

public class MainActivity extends RoboActionBarActivity implements ActionBar.OnNavigationListener {
    private static final String ROOSTER_TYPE = "roosterType";

    @Getter private static int selectedWeek = -1;
    private Account user;

    private static ActionBar actionBar;
    private static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    @Getter @Deprecated private static WeakReference<MenuItem> refreshItem;

    @InjectView(R.id.drawerlayout) private DrawerLayout drawerLayout;
    @InjectView(R.id.drawer) private ExpandableListView drawerList;

    @Getter private static DatabaseHelper databaseHelper = null;

    private RoosterViewFragment mainFragment;
    private Class<? extends RoosterViewFragment> roosterType;

    @Override
    protected void onStop() {
        /*Tracker easyTracker = EasyTracker.getInstance(this);
        easyTracker.set(Fields.SCREEN_NAME, null);
        easyTracker.send(MapBuilder
                        .createAppView()
                        .build()
        );*/
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Setup */
        //databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        //GoogleAnalytics.getInstance(this).setDryRun(true);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();
        user = new Account(this);

        if (savedInstanceState == null) {
            // Defaults
            roosterType = PersoonlijkRoosterFragment.class;
            mainFragment = RoosterViewFragment.newInstance(roosterType, user, getSelectedWeek());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        } else {
            roosterType = (Class<? extends RoosterViewFragment>) savedInstanceState.getSerializable(ROOSTER_TYPE);
            mainFragment = (RoosterViewFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        }

        /* Navigation Drawer */
        ArrayList<String> groups = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_groups)));
        ArrayList<ArrayList<String>> children = new ArrayList<ArrayList<String>>() {{
            add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_items_roosters))));
            add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_items_pgtv))));
        }};

        drawerList.setAdapter(new NavigationDrawerAdapter(this, groups, children));
        drawerList.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            if (groupPosition == 0) {
                Class<? extends RoosterViewFragment> newType = RoosterViewFragment.types[childPosition];

                // Nieuwe dingen
                mainFragment = RoosterViewFragment.newInstance(newType, user, selectedWeek);
                roosterType = newType;
                actionBarSpinnerAdapter.setType(newType);

                getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();

                drawerLayout.closeDrawer(drawerList);
            } else {
                // TODO: iets voor PGTV
            }
            return true;
        });

        // Open alle groepen
        for (int pos = 0; pos < drawerList.getExpandableListAdapter().getGroupCount(); pos++){
            drawerList.expandGroup(pos);
        }

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                supportInvalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        /* Dropdown Navigation */
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        RoosterInfo.getWeken(this, result -> addWekenToActionBar((ArrayList<Week>) result)); // hier gebeurt de rest
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(ROOSTER_TYPE, roosterType);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(HelperFunctions.hasInternetConnection(this)) {
            mainFragment.loadRooster(); // TODO from internet!!!
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.menu_item_refresh).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu maken
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem refresh = menu.findItem(R.id.menu_item_refresh);
        MenuItemCompat.setActionView(refresh, R.layout.actionbar_refresh_progress);

        // TODO blijft dit werken?
        refreshItem = new WeakReference<>(refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) return true;

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferencesIntent = new Intent(this, PreferencesActivity.class);
                startActivity(preferencesIntent);
                return true;
            case R.id.menu_item_refresh:
                mainFragment.loadRooster();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int pos, long item) {
        String itemString = (String) actionBarSpinnerAdapter.getItem(pos);
        int week = Integer.parseInt(itemString.substring(5));

        setSelectedWeek(week);
        return true;
    }

    private void addWekenToActionBar(ArrayList<Week> wekenArray) {
        ArrayList<String> strings = new ArrayList<>();
        if (wekenArray == null || wekenArray.isEmpty()) {
            strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        } else {

            //Get the index of the current week
            int indexCurrentWeek = 0;

            int correctionForWeekends = 0;
            if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 1 || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7) {
                correctionForWeekends = 1;
            }

            for (int u = 0; u < wekenArray.size(); u++) {
                if (wekenArray.get(u).week >= Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + correctionForWeekends) {
                    indexCurrentWeek = u;
                    break;
                }
            }

            setSelectedWeek(wekenArray.get(indexCurrentWeek).week);

            for (int c = 0; c < Settings.WEEKS_IN_SPINNER && c < wekenArray.size(); c++) {
                strings.add("Week " + wekenArray.get(indexCurrentWeek + c).week);
            }
        }
        actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, strings, PersoonlijkRoosterFragment.class);
        actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, this);
    }

    public void setSelectedWeek(int week) {
        selectedWeek = week;
        mainFragment.setWeek(week);
    }
}
