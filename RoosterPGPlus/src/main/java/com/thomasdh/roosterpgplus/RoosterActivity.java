package com.thomasdh.roosterpgplus;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;

import com.fwest98.showcaseview.ShowcaseView;
import com.fwest98.showcaseview.targets.ViewTarget;
import com.google.android.gms.analytics.HitBuilders;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.thomasdh.roosterpgplus.Adapters.ActionBarSpinnerAdapter;
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
import com.thomasdh.roosterpgplus.Notifications.NextUurNotifications;
import com.thomasdh.roosterpgplus.Settings.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import lombok.AccessLevel;
import lombok.Getter;
import roboguice.activity.RoboActionBarActivity;

public class RoosterActivity extends RoboActionBarActivity implements ActionBar.OnNavigationListener, InternetConnectionManager.InternetConnectionChangeListener, RoosterViewFragment.onRoosterLoadStateChangedListener {
    private static final String ROOSTER_TYPE = "roosterType";

    @Getter
    private static int selectedWeek = -1;

    private static ActionBar actionBar;
    private static Toolbar toolbar;
    private static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem refreshItem;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem searchItem;

    //@InjectView(R.id.drawerlayout)
    private DrawerLayout drawerLayout;
    //@InjectView(R.id.drawer)
    private ExpandableListView drawerList;

    private RoosterViewFragment mainFragment;
    private Class<? extends RoosterViewFragment> roosterType;
    private boolean isRooster = true;
    private Drawer drawer;
    private AccountHeader drawerHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Setup */
        setContentView(R.layout.activity_rooster);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        Account.getInstance(this);

        new NextUurNotifications(this);

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
                getSupportActionBar().setTitle("PGTV - " + ((PGTVRoosterFragment.PGTVType) savedInstanceState.getSerializable("PGTVTYPE")).toDesc());
            }
        }

        /* Navigation Drawer */
        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .addProfiles(
                        new ProfileDrawerItem().withName(Account.getName()).withEmail("Klas " + Account.getLeerlingKlas())
                )
                .withHeaderBackground(R.drawable.drawer_header_blurred)
                .withHeaderBackgroundScaleType(ImageView.ScaleType.CENTER_CROP)
                .withOnAccountHeaderListener((view, iProfile, b) -> false)
                .withSavedInstance(savedInstanceState)
                .withProfileImagesVisible(false)
                .withSelectionListEnabled(false)
                .build();

        ArrayList<String> groups = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_groups)));
        ArrayList<ArrayList<String>> itemTitles = new ArrayList<ArrayList<String>>() {{
            add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_items_roosters))));
            add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_items_pgtv))));
        }};
        ArrayList<ArrayList<String>> itemIcons = new ArrayList<ArrayList<String>>() {{
           add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_icons_rooster))));
            add(new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.drawer_icons_pgtv))));
        }};

        DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(drawerHeader)
                .withAnimateDrawerItems(true)
                .withCloseOnClick(true)
                .withSavedInstance(savedInstanceState)
                .withOnDrawerItemClickListener((adapterView, view, i, l, iDrawerItem) -> {
                    int firstDigit = iDrawerItem.getIdentifier() / 10; // first digit
                    int secondDigit = iDrawerItem.getIdentifier() % 10; // second digit

                    if (firstDigit == 0) {
                        // Roostergroup
                        Class<? extends RoosterViewFragment> newType = RoosterViewFragment.types[secondDigit];

                        mainFragment = RoosterViewFragment.newInstance(newType, getSelectedWeek(), this);
                        roosterType = newType;

                        if (actionBarSpinnerAdapter != null)
                            actionBarSpinnerAdapter.setType(newType);

                        isRooster = true;
                    } else if(firstDigit == 1) {
                        // PGTV group
                        Class<? extends RoosterViewFragment> newType = PGTVRoosterFragment.class;

                        PGTVRoosterFragment fragment = RoosterViewFragment.newInstance(PGTVRoosterFragment.class, getSelectedWeek(), this);

                        switch (secondDigit) {
                            case 0:
                                fragment.setType(PGTVRoosterFragment.PGTVType.ROOSTER);
                                break;
                            case 1:
                                fragment.setType(PGTVRoosterFragment.PGTVType.MEDEDELINGEN);
                                break;
                            default:
                                break;
                        }
                        actionBar.setTitle("PGTV - " + fragment.getType().toDesc());
                        isRooster = false;
                        mainFragment = fragment;
                        roosterType = newType;
                    } else if(firstDigit == 2) {
                        // Settings
                        drawer.closeDrawer();
                        Intent preferencesIntent = new Intent(this, PreferencesActivity.class);
                        startActivity(preferencesIntent);
                        return false;
                    }

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();
                    drawer.closeDrawer();

                    return true;
                });

        int i = 0;
        for(String groupName : groups) {
            drawerBuilder.addDrawerItems(new SectionDrawerItem().setDivider(i != 0).withName(groupName));
            int u = 0;
            for(String childName : itemTitles.get(i)) {
                int resource = HelperFunctions.getResId(itemIcons.get(i).get(u), R.drawable.class);
                drawerBuilder.addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(childName)
                                .withIdentifier(Integer.parseInt(i + "" + u))
                                .withIcon(resource)
                );
                u++;
            }
            i++;
        }

        drawer = drawerBuilder
                .addDrawerItems(
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("Instellingen").withIcon(R.drawable.ic_settings_grey).withIdentifier(20)
                )
                .withSelectedItem(1)
                .build();

        drawerLayout = drawer.getDrawerLayout();

        /*drawerList.setAdapter(new NavigationDrawerAdapter(this, groups, children));
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
        }*/

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if(getSelectedWeek() == -1) return;
                if(isRooster) {
                    if(actionBarSpinnerAdapter.getCount() >= 1) {
                        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                        getSupportActionBar().setDisplayShowTitleEnabled(false);
                    } else {
                        getSupportActionBar().setTitle(R.string.app_name);
                        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                        getSupportActionBar().setDisplayShowTitleEnabled(true);
                    }
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
        if(!Intent.ACTION_SEARCH.equals(intent.getAction())) return;

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
        savedInstanceState = drawer.saveInstanceState(savedInstanceState);
        savedInstanceState = drawerHeader.saveInstanceState(savedInstanceState);

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
    public void onBackPressed() {
        if(drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
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
        boolean drawerOpen = drawer.isDrawerOpen();
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
        drawer.openDrawer();
    }

    void setRefreshButtonState(boolean loading) {
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

        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getApplicationContext())
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_ROOSTER)
                        .setAction(Constants.ANALYTICS_ACTIVITY_ROOSTER_ACTION_CHANGE_WEEK)
                        .setLabel(mainFragment.getAnalyticsTitle())
                        .build());

        setSelectedWeek(week);
        return true;
    }

    private void addWekenToActionBar(Object weken) {
        ArrayList<Week> wekenArray = (ArrayList<Week>) weken;
        ArrayList<String> strings = new ArrayList<>();
        if (wekenArray == null || wekenArray.isEmpty()) {
            strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        } else {
            for (int c = 0; c < Constants.WEEKS_IN_SPINNER && c < wekenArray.size(); c++) {
                if(wekenArray.get(c).isVakantieweek()) continue;
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

    void setSelectedWeek(int week) {
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
