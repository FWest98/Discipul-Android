package com.thomasdh.roosterpgplus;

import android.animation.ValueAnimator;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.quinny898.library.persistentsearch.SearchBox;
import com.thomasdh.roosterpgplus.Adapters.ActionBarSpinnerAdapter;
import com.thomasdh.roosterpgplus.Data.Account;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
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

public class RoosterActivity extends AppCompatActivity implements InternetConnectionManager.InternetConnectionChangeListener {
    private static final String ROOSTER_TYPE = "roosterType";

    @Getter
    private static int selectedWeek = -1;

    private static ActionBar actionBar;
    private static Toolbar toolbar;
    private static ActionBarSpinnerAdapter toolbarSpinnerAdapter;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem refreshItem;
    @Getter(value = AccessLevel.PRIVATE) private static MenuItem searchItem;

    private int currentSelection = 0;
    private RoosterViewFragment mainFragment;
    private Class<? extends RoosterViewFragment> roosterType;
    private boolean isRooster = true;
    private boolean isShowingBackArrow = false;
    private Drawer drawer;
    private AccountHeader drawerHeader;
    private SearchBox searchBox;
    private Snackbar internetConnectionSnackbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Setup */
        setContentView(R.layout.activity_rooster);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        Account.getInstance(this);

        new NextUurNotifications(this);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if(intent.getAction() == Intent.ACTION_SEARCH) {
                roosterType = EntityRoosterFragment.class;
                EntityRoosterFragment searchFragment = (EntityRoosterFragment) RoosterViewFragment.newInstance(roosterType, getSelectedWeek());
                mainFragment = searchFragment;
                searchFragment.setEntity(intent.getStringExtra(SearchManager.QUERY));
            } else {
                // Defaults
                roosterType = PersoonlijkRoosterFragment.class;
                mainFragment = RoosterViewFragment.newInstance(roosterType, getSelectedWeek());
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        } else {
            roosterType = (Class<? extends RoosterViewFragment>) savedInstanceState.getSerializable(ROOSTER_TYPE);
            mainFragment = (RoosterViewFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            isRooster = roosterType != PGTVRoosterFragment.class;
            setSelectedWeek(savedInstanceState.getInt("WEEK"));
            if(!isRooster) {
                ((PGTVRoosterFragment) mainFragment).setType((PGTVRoosterFragment.PGTVType) savedInstanceState.getSerializable("PGTVTYPE"));
            }
        }

        /* Navigation Drawer */
        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .addProfiles(
                        Account.isSet() ?
                        new ProfileDrawerItem().withName(Account.getName()).withEmail(
                                Account.getUserType() == Account.UserType.LEERLING ?
                                    "Klas " + Account.getLeerlingKlas() :
                                    "Docentcode: " + Account.getLeraarCode()
                        ) : new ProfileDrawerItem().withName("Nog niet ingelogd")
                )
                .withCompactStyle(!Account.isSet())
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
                .withActionBarDrawerToggleAnimated(true)
                .withSavedInstance(savedInstanceState)
                .withOnDrawerItemClickListener((adapterView, view, i, l, iDrawerItem) -> {
                    int firstDigit = iDrawerItem.getIdentifier() / 10; // first digit
                    int secondDigit = iDrawerItem.getIdentifier() % 10; // second digit

                    if (firstDigit == 0) {
                        // Roostergroup
                        Class<? extends RoosterViewFragment> newType = RoosterViewFragment.types[secondDigit];

                        mainFragment = RoosterViewFragment.newInstance(newType, getSelectedWeek());
                        roosterType = newType;

                        if (toolbarSpinnerAdapter != null)
                            toolbarSpinnerAdapter.setType(newType);

                        isRooster = true;
                    } else if (firstDigit == 1) {
                        // PGTV group
                        Class<? extends RoosterViewFragment> newType = PGTVRoosterFragment.class;

                        PGTVRoosterFragment fragment = RoosterViewFragment.newInstance(PGTVRoosterFragment.class, getSelectedWeek());

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
                        isRooster = false;
                        mainFragment = fragment;
                        roosterType = newType;
                    } else if (firstDigit == 2) {
                        // Settings
                        drawer.closeDrawer();
                        drawer.setSelectionByIdentifier(currentSelection);
                        Intent preferencesIntent = new Intent(this, PreferencesActivity.class);
                        startActivity(preferencesIntent);
                        return false;
                    }

                    getSupportFragmentManager().popBackStackImmediate(); // voor zoeken
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).commit();
                    drawer.closeDrawer();
                    currentSelection = iDrawerItem.getIdentifier();
                    toggleHamburgerArrow(false);

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

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View view) {
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View view) {
                if (getSelectedWeek() == -1) return;
                if (isRooster) {
                    toolbar.findViewById(R.id.toolbar_title).setVisibility(View.GONE);
                    toolbar.findViewById(R.id.toolbar_spinner).setVisibility(View.VISIBLE);
                } else {
                    toolbar.findViewById(R.id.toolbar_spinner).setVisibility(View.GONE);
                    toolbar.findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);
                    ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText("PGTV - " + ((PGTVRoosterFragment) mainFragment).getType().toDesc());
                }
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View view, float v) {
                if (isShowingBackArrow) {
                    super.onDrawerSlide(view, 1);
                } else {
                    super.onDrawerSlide(view, v);
                }
            }
        };

        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        View.OnClickListener toolbarOnClickListener = view -> {
            if (roosterType == EntityRoosterFragment.class && isShowingBackArrow) {
                onBackPressed();
            } else {
                if(drawer.isDrawerOpen()) {
                    drawer.closeDrawer();
                } else {
                    drawer.openDrawer();
                }
            }
        };

        toolbar.setNavigationOnClickListener(toolbarOnClickListener);
        actionBarDrawerToggle.setToolbarNavigationClickListener(toolbarOnClickListener);

        /* Search */
        searchBox = (SearchBox) findViewById(R.id.searchbox);
        searchBox.enableVoiceRecognition(this);

        RoosterInfo.getWeken(this, this::addWekenToActionBar); // hier gebeurt de rest
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
        } else if(searchBox != null && searchBox.isShown()) {
            searchBox.toggleSearch();
        } else if(roosterType == EntityRoosterFragment.class) {
            // actionbar goed maken
            toggleHamburgerArrow(false);

            // fragments goed maken
            getSupportFragmentManager().popBackStackImmediate();
            mainFragment = (RoosterViewFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            roosterType = mainFragment.getClass();
            toolbarSpinnerAdapter.setType(roosterType);
            drawer.setSelectionByIdentifier(currentSelection);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu maken
        getMenuInflater().inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.menu_item_search);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_item_search: {
                openSearch();
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    public boolean onWeekSelected(int pos) {
        String itemString = (String) toolbarSpinnerAdapter.getItem(pos);
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

        View spinnerContainer = LayoutInflater.from(this).inflate(R.layout.toolbar_spinner, toolbar, false);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        toolbar.addView(spinnerContainer, layoutParams);
        toolbar.findViewById(R.id.toolbar_title).setVisibility(View.GONE);

        toolbarSpinnerAdapter = new ActionBarSpinnerAdapter(this, strings, ((Object) mainFragment).getClass());

        Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
        spinner.setAdapter(toolbarSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onWeekSelected(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(getSelectedWeek() != -1) {
            spinner.setSelection(strings.indexOf("Week " + getSelectedWeek()));
        }
    }

    void setSelectedWeek(int week) {
        selectedWeek = week;
        mainFragment.setWeek(week);
    }

    public void openSearch() {
        searchBox.revealFromMenuItem(R.id.menu_item_search, this);
        searchBox.setLogoText(getString(R.string.search_hint));
        searchBox.setSearchListener(new SearchBox.SearchListener() {
            @Override
            public void onSearchOpened() {
                // Show tint
                View overlay = findViewById(R.id.search_overlay);
                overlay.setVisibility(View.VISIBLE);
                overlay.setOnClickListener(view -> searchBox.toggleSearch());
            }

            @Override
            public void onSearchCleared() {

            }

            @Override
            public void onSearchClosed() {
                closeSearch();
            }

            @Override
            public void onSearchTermChanged() {

            }

            @Override
            public void onSearch(String s) {
                closeSearch();
                /* Fragment dingen */
                EntityRoosterFragment searchFragment;
                if (roosterType != EntityRoosterFragment.class) {
                    // Set to entityrooster
                    roosterType = EntityRoosterFragment.class;
                    searchFragment = (EntityRoosterFragment) RoosterViewFragment.newInstance(roosterType, getSelectedWeek());
                    toolbarSpinnerAdapter.setType(roosterType);

                    mainFragment = searchFragment;
                    searchFragment.setEntity(s);

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, mainFragment).addToBackStack("search").commit();

                    // actionbar & drawer
                    toggleHamburgerArrow(true);
                    drawer.getListView().setSelection(-1);
                    drawer.getListView().setItemChecked(drawer.getCurrentSelection() + 1, false); // +1 for the header offset
                } else {
                    searchFragment = (EntityRoosterFragment) mainFragment;
                    searchFragment.setEntity(s);
                    searchFragment.loadRooster();
                }

                // Save search query
            }
        });
    }

    public void closeSearch() {
        searchBox.hideCircularly(this);
        View overlay = findViewById(R.id.search_overlay);
        overlay.setVisibility(View.GONE);
    }

    public void toggleHamburgerArrow(boolean showBackArrow) {
        float start = showBackArrow ? 0: 1;
        float end = showBackArrow ? 1 : 0;

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.addUpdateListener(valueAnimator -> {
            float slideOffset = (float) valueAnimator.getAnimatedValue();
            drawer.getActionBarDrawerToggle().onDrawerSlide(drawerLayout, slideOffset);
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(500);
        animator.start();

        isShowingBackArrow = showBackArrow;
    }

    @Override
    public void internetConnectionChanged(boolean hasInternetConnection) {
        if(!hasInternetConnection) {
            internetConnectionSnackbar = Snackbar.make(findViewById(R.id.container), "Geen internetverbinding meer. Zodra er weer verbinding is zal de app het nieuwste rooster ophalen", Snackbar.LENGTH_LONG);
            internetConnectionSnackbar.show();
        } else {
            if(internetConnectionSnackbar != null)
                internetConnectionSnackbar.dismiss();
        }
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
