package com.thomasdh.roosterpgplus;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.fwest98.showcaseview.ShowcaseView;
import com.fwest98.showcaseview.targets.ViewTarget;
import com.thomasdh.roosterpgplus.Adapters.ActionBarSpinnerAdapter;
import com.thomasdh.roosterpgplus.Adapters.NavigationDrawerAdapter;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Data.SearchRecentsProvider;
import com.thomasdh.roosterpgplus.Fragments.EntityRoosterFragment;
import com.thomasdh.roosterpgplus.Fragments.PGTVRoosterFragment;
import com.thomasdh.roosterpgplus.Fragments.PersoonlijkRoosterFragment;
import com.thomasdh.roosterpgplus.Fragments.RoosterViewFragment;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.InternetConnectionManager;
import com.thomasdh.roosterpgplus.Models.Week;
import com.thomasdh.roosterpgplus.Settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import lombok.AccessLevel;
import lombok.Getter;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;

public class RoosterActivity extends RoboActionBarActivity implements ActionBar.OnNavigationListener, InternetConnectionManager.InternetConnectionChangeListener, RoosterViewFragment.onRoosterLoadStateChangedListener {
    private static final String ROOSTER_TYPE = "roosterType";

    @Getter
    private static int selectedWeek = -1;

    private static ActionBar actionBar;
    private static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem refreshItem;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem searchItem;

    @InjectView(R.id.drawerlayout)
    private DrawerLayout drawerLayout;
    @InjectView(R.id.drawer)
    private ExpandableListView drawerList;

    private RoosterViewFragment mainFragment;
    private Class<? extends RoosterViewFragment> roosterType;
    private boolean isRooster = true;

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
        setContentView(R.layout.activity_rooster);
        actionBar = getSupportActionBar();
        Account.getInstance(this);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if(intent.getAction() == Intent.ACTION_SEARCH) {
                roosterType = EntityRoosterFragment.class;
                EntityRoosterFragment searchFragment = (EntityRoosterFragment) RoosterViewFragment.newInstance(roosterType, getSelectedWeek(), this);
                mainFragment = searchFragment;
                searchFragment.setEntity(intent.getStringExtra(SearchManager.QUERY));
            } else {
                // Defaults
                roosterType = PersoonlijkRoosterFragment.class;
                mainFragment = RoosterViewFragment.newInstance(roosterType, getSelectedWeek(), this);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        } else {
            roosterType = (Class<? extends RoosterViewFragment>) savedInstanceState.getSerializable(ROOSTER_TYPE);
            mainFragment = (RoosterViewFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            mainFragment.setRoosterLoadStateListener(this);
            isRooster = roosterType != PGTVRoosterFragment.class;
            setSelectedWeek(savedInstanceState.getInt("WEEK"));
            if(!isRooster) {
                ((PGTVRoosterFragment) mainFragment).setType((PGTVRoosterFragment.PGTVType) savedInstanceState.getSerializable("PGTVTYPE"));
                getSupportActionBar().setTitle("PGTV - " +((PGTVRoosterFragment.PGTVType) savedInstanceState.getSerializable("PGTVTYPE")).toDesc());
            }
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
                mainFragment = RoosterViewFragment.newInstance(newType, getSelectedWeek(), this);
                roosterType = newType;

                if(actionBarSpinnerAdapter != null)
                    actionBarSpinnerAdapter.setType(newType);

                getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();
                isRooster = true;

                drawerLayout.closeDrawer(drawerList);
            } else {
                Class<? extends RoosterViewFragment> newType = PGTVRoosterFragment.class;

                PGTVRoosterFragment fragment = RoosterViewFragment.newInstance(PGTVRoosterFragment.class, getSelectedWeek(), this);

                switch (childPosition) {
                    case 0:
                        fragment.setType(PGTVRoosterFragment.PGTVType.ROOSTER);
                        break;
                    case 1:
                        fragment.setType(PGTVRoosterFragment.PGTVType.MEDEDELINGEN);
                        break;
                    default:
                        break;
                }
                mainFragment = fragment;
                roosterType = newType;

                getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();
                isRooster = false;

                drawerLayout.closeDrawer(drawerList);
                actionBar.setTitle("PGTV - "+fragment.getType().toDesc());
            }
            return true;
        });

        // Open alle groepen
        for (int pos = 0; pos < drawerList.getExpandableListAdapter().getGroupCount(); pos++) {
            drawerList.expandGroup(pos);
        }

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if(getSelectedWeek() == -1) return;
                if(isRooster) {
                    getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                } else {
                    getSupportActionBar().setTitle("PGTV - "+((PGTVRoosterFragment) mainFragment).getType().toDesc());
                }
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                getSupportActionBar().setTitle(R.string.app_name);
                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                supportInvalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        RoosterInfo.getWeken(this, this::addWekenToActionBar); // hier gebeurt de rest
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if(!intent.getAction().equals(Intent.ACTION_SEARCH)) return;

        if(((Object) mainFragment).getClass() != EntityRoosterFragment.class) {
            roosterType = EntityRoosterFragment.class;
            EntityRoosterFragment searchFragment = (EntityRoosterFragment) RoosterViewFragment.newInstance(roosterType, getSelectedWeek(), this);
            actionBarSpinnerAdapter.setType(roosterType);

            mainFragment = searchFragment;
            searchFragment.setEntity(intent.getStringExtra(SearchManager.QUERY));

            getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();

            new SearchRecentSuggestions(this, SearchRecentsProvider.AUTHORITY, SearchRecentsProvider.MODE).saveRecentQuery(searchFragment.getEntity(), null);
        } else {
            ((EntityRoosterFragment) mainFragment).setEntity(intent.getStringExtra(SearchManager.QUERY));
            mainFragment.loadRooster();
        }

        MenuItemCompat.collapseActionView(searchItem);
        ((SearchView) MenuItemCompat.getActionView(searchItem)).onActionViewCollapsed();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(ROOSTER_TYPE, roosterType);
        savedInstanceState.putInt("WEEK", getSelectedWeek());
        if(!isRooster) {
            savedInstanceState.putSerializable("PGTVTYPE", ((PGTVRoosterFragment) mainFragment).getType());
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (HelperFunctions.hasInternetConnection(this)) {
            mainFragment.loadRooster(true);
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

        // Search
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        searchView.setQueryRefinementEnabled(true);

        refreshItem = refresh;
        searchItem = menu.findItem(R.id.menu_item_search);
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
                mainFragment.loadRooster(true);
                return true;
            case R.id.menu_item_search:
                if(HelperFunctions.showCaseView()) {
                    new ShowcaseView.Builder(this)
                            .setTarget(new ViewTarget(MenuItemCompat.getActionView(searchItem)))
                            .setContentTitle(R.string.showcaseview_zoeken_title)
                            .setContentText(R.string.showcaseview_zoeken_content)
                            .hideOnTouchOutside()
                            .singleShot(2)
                            .setStyle(R.style.ShowCaseTheme)
                            .build();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRoosterLoadEnd() {
        setRefreshButtonState(false);
    }

    @Override
    public void onRoosterLoadStart() {
        setRefreshButtonState(true);
    }

    @Override
    public void onRoosterLoadCancel() {
        setRefreshButtonState(false);
        drawerLayout.openDrawer(drawerList);
    }

    public void setRefreshButtonState(boolean loading) {
        MenuItem refreshItem = getRefreshItem();
        if(refreshItem != null) {
            if(loading) {
                MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_refresh_progress);
            } else {
                MenuItemCompat.setActionView(refreshItem, null);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(int pos, long item) {
        String itemString = (String) actionBarSpinnerAdapter.getItem(pos);
        int week = Integer.parseInt(itemString.substring(5));

        setSelectedWeek(week);
        return true;
    }

    private void addWekenToActionBar(Object weken) {
        ArrayList<Week> wekenArray = (ArrayList<Week>) weken;
        ArrayList<String> strings = new ArrayList<>();
        if (wekenArray == null || wekenArray.isEmpty()) {
            strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        } else {
            for (int c = 0; c < Settings.WEEKS_IN_SPINNER && c < wekenArray.size(); c++) {
                strings.add("Week " + wekenArray.get(c).week);
            }
        }
        actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, strings, ((Object) mainFragment).getClass());
        actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, this);

        if(isRooster) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        }

        if(getSelectedWeek() != -1) {
            actionBar.setSelectedNavigationItem(strings.indexOf("Week "+getSelectedWeek()));
        }
    }

    public void setSelectedWeek(int week) {
        selectedWeek = week;
        mainFragment.setWeek(week);
    }

    @Override
    public void internetConnectionChanged(boolean hasInternetConnection) {
        mainFragment.setInternetConnectionState(hasInternetConnection);
    }

    public void onResume() {
        super.onResume();
        InternetConnectionManager.registerListener("main", this);
    }

    public void onPause() {
        super.onPause();
        InternetConnectionManager.unregisterListener("main");
    }
}
