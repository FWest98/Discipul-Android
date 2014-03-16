package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.google.analytics.tracking.android.Tracker;
import com.thomasdh.roosterpgplus.adapters.ActionBarSpinnerAdapter;
import com.thomasdh.roosterpgplus.adapters.MyPagerAdapter;
import com.thomasdh.roosterpgplus.adapters.NavigationDrawerAdapter;
import com.thomasdh.roosterpgplus.roosterdata.RoosterInfoDownloader;
import com.thomasdh.roosterpgplus.roosterdata.RoosterWeek;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends ActionBarActivity {

    private static WeakReference<MenuItem> refreshItem;
    private static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    private static ActionBar actionBar;
    private static int selectedWeek = -1;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private PlaceholderFragment mainFragment;
    private Account user;

    @Override
    protected void onStop() {
        Tracker easyTracker = EasyTracker.getInstance(this);
        easyTracker.set(Fields.SCREEN_NAME, null);
        easyTracker.send(MapBuilder
                        .createAppView()
                        .build()
        );
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleAnalytics.getInstance(this).setDryRun(true);

        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();

        mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.PERSOONLIJK_ROOSTER);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        }

        // Maak de navigation drawer

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);

        final ExpandableListView drawerList = (ExpandableListView) findViewById(R.id.drawer);

        ArrayList<String> groups = new ArrayList<String>() {{
            add(getString(R.string.drawer_group_rooster));
            add(getString(R.string.drawer_group_pgtv));
        }};

        ArrayList<ArrayList<String>> childs = new ArrayList<ArrayList<String>>() {{
            add(new ArrayList<String>() {{
                add(getString(R.string.drawer_item_persoonlijk_rooster));
                add(getString(R.string.drawer_item_klassenrooster));
                add(getString(R.string.drawer_item_docentenrooster));
            }});
            add(new ArrayList<String>() {{
                add(getString(R.string.drawer_item_roosterwijzigingen));
                add(getString(R.string.drawer_algemene_mededelingen));
            }});
        }};

        drawerList.setAdapter(new NavigationDrawerAdapter(this, groups, childs));
        drawerList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                if (groupPosition == 0) {
                    if (childPosition == 0) {
                        mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.PERSOONLIJK_ROOSTER);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, mainFragment, "Persoonlijk roosterFragment")
                                .commit();
                    } else if (childPosition == 1) {
                        mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.KLASROOSTER);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, mainFragment, "LeerlingroosterFragment")
                                .commit();
                    } else if (childPosition == 2) {
                        mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.DOCENTENROOSTER);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, mainFragment, "DocentenroosterFragment")
                                .commit();
                    }
                    drawerLayout.closeDrawer(drawerList);
                }
                return true;
            }
        });

        // Open alle groepen
        for (int pos = 0; pos < drawerList.getCount(); pos++){
            drawerList.expandGroup(pos);
        }

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Schakel List navigatie in

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ActionBar.OnNavigationListener onNavigationListener = new

                ActionBar.OnNavigationListener() {
                    @Override
                    public boolean onNavigationItemSelected(int i, long l) {
                        return false;
                    }
                };
        actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, new ArrayList<String>(), PlaceholderFragment.Type.PERSOONLIJK_ROOSTER);
        //Voeg beide toe
        getSupportActionBar().setListNavigationCallbacks(actionBarSpinnerAdapter, onNavigationListener);


        /** Aanmaken User */
        this.user = new Account(this, mainFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainFragment != null && mainFragment.getRootView() != null && refreshItem != null) {
            mainFragment.laadRooster(this, mainFragment.getRootView(), mainFragment.type);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        refreshItem = new WeakReference<MenuItem>(menu.findItem(R.id.menu_item_refresh));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent optiesIntent = new Intent(this, PreferencesActivity.class);
                startActivity(optiesIntent);
                return true;
            case R.id.menu_item_refresh:
                if (mainFragment.type == PlaceholderFragment.Type.PERSOONLIJK_ROOSTER) {
                    new RoosterDownloader(this, mainFragment.getRootView(), true, item, selectedWeek).execute();
                } else if (mainFragment.type == PlaceholderFragment.Type.KLASROOSTER) {
                    new RoosterDownloader(this, mainFragment.getRootView(), true, item, selectedWeek, mainFragment.leraarLerlingselected, PlaceholderFragment.Type.KLASROOSTER).execute();
                } else if (mainFragment.type == PlaceholderFragment.Type.DOCENTENROOSTER) {
                    new RoosterDownloader(this, mainFragment.getRootView(), true, item, selectedWeek, mainFragment.leraarLerlingselected, PlaceholderFragment.Type.DOCENTENROOSTER).execute();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        public static String leraarLerlingselected;
        public Type type;
        public ViewPager viewPager;
        public Account user;
        private View rootView;

        public PlaceholderFragment() {

        }

        public PlaceholderFragment(Type type) {
            this.type = type;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putSerializable("fragmentType", type);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            if (savedInstanceState != null) {
                type = (Type) savedInstanceState.getSerializable("fragmentType");
            }

            /** Aanmaken User */
            this.user = new Account(getActivity(), this);
            if (type == Type.PERSOONLIJK_ROOSTER) {
                setRootView(inflater.inflate(R.layout.fragment_main, container, false));
                viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager);
                viewPager.setAdapter(new MyPagerAdapter());

                if (!this.user.isSet) {
                    this.user.showLoginDialog(true);
                }

                Tracker easyTracker = EasyTracker.getInstance(getActivity());
                easyTracker.set(Fields.SCREEN_NAME, "Persoonlijk Rooster");
                easyTracker.send(MapBuilder
                                .createAppView()
                                .build()
                );

            } else if (type == Type.DOCENTENROOSTER) {
                setRootView(inflater.inflate(R.layout.fragment_main_docenten, container, false));
                viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager_docent);
                viewPager.setAdapter(new MyPagerAdapter());

                new AsyncTask<Void, Void, ArrayList<RoosterInfoDownloader.Vak>>() {
                    @Override
                    protected ArrayList<RoosterInfoDownloader.Vak> doInBackground(Void... params) {
                        ArrayList<RoosterInfoDownloader.Vak> leraren;
                        try {
                            leraren = RoosterInfoDownloader.getLeraren();
                        } catch (Exception e) {
                            Log.e("MainActivity", "Er ging iets mis bij het ophalen van de leraren", e);
                            EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
                            easyTracker.send(MapBuilder
                                    .createException(new StandardExceptionParser(getActivity(), null)
                                            .getDescription(Thread.currentThread().getName(), e), false)
                                    .build());
                            return null;
                        }
                        return leraren;
                    }

                    @Override
                    protected void onPostExecute(final ArrayList<RoosterInfoDownloader.Vak> vakken) {

                        final Spinner docentenNaamSpinner = (Spinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_naam);
                        final Spinner docentenVakSpinner = (Spinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_vak);

                        if (vakken != null) {
                            final ArrayList<String> vakNaam = new ArrayList<String>();
                            for (RoosterInfoDownloader.Vak vak : vakken) {
                                vakNaam.add(vak.naam);
                            }

                            ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(getActivity(), R.layout.spinner_title, vakNaam.toArray(new String[vakNaam.size()]));
                            adapter1.setDropDownViewResource(R.layout.spinner_dropdown);
                            docentenVakSpinner.setAdapter(adapter1);
                            docentenVakSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                    final RoosterInfoDownloader.Vak selectedVak = vakken.get(position);

                                    ArrayList<String> namenLeraren = new ArrayList<String>();
                                    for (RoosterInfoDownloader.Leraar leraar : selectedVak.leraren) {
                                        namenLeraren.add(leraar.naam);
                                    }
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_title, namenLeraren.toArray(new String[namenLeraren.size()]));
                                    adapter.setDropDownViewResource(R.layout.spinner_dropdown);
                                    docentenNaamSpinner.setAdapter(adapter);
                                    docentenNaamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                            leraarLerlingselected = selectedVak.leraren.get(position).korteNaam;
                                            new RoosterDownloader(getActivity(), getRootView(), true, refreshItem.get(), selectedWeek, selectedVak.leraren.get(position).korteNaam, Type.DOCENTENROOSTER).execute();
                                        }

                                        @Override
                                        public void onNothingSelected(AdapterView<?> parent) {

                                        }
                                    });
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                        }
                    }
                }.execute();

                Tracker easyTracker = EasyTracker.getInstance(getActivity());
                easyTracker.set(Fields.SCREEN_NAME, "Docentenrooster");
                easyTracker.send(MapBuilder
                                .createAppView()
                                .build()
                );

            } else if (type == Type.KLASROOSTER) {
                setRootView(inflater.inflate(R.layout.fragment_main_klas, container, false));
                viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager_leerling);
                viewPager.setAdapter(new MyPagerAdapter());

                final Spinner klasspinner = (Spinner) getRootView().findViewById(R.id.main_fragment_spinner_klas);
                new AsyncTask<Void, Exception, ArrayList<String>>() {


                    @Override
                    protected ArrayList<String> doInBackground(Void... params) {
                        try {
                            return RoosterInfoDownloader.getKlassen();
                        } catch (Exception e) {
                            publishProgress(e);
                        }
                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(Exception... values) {
                        ExceptionHandler.handleException(values[0], getActivity(), "Kon de klassen niet ophalen", getClass().getSimpleName(), ExceptionHandler.HandleType.EXTENSIVE);
                    }

                    @Override
                    protected void onPostExecute(final ArrayList<String> klassen) {
                        // doe iets met de klassen
                        if (klassen != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_title, klassen.toArray(new String[klassen.size()]));
                            adapter.setDropDownViewResource(R.layout.spinner_dropdown);
                            klasspinner.setAdapter(adapter);

                            final boolean[] init = {true};
                            klasspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    leraarLerlingselected = ((TextView) view).getText().toString();

                                    if (!init[0]) {
                                        //Sla de geselecteerde klas op
                                        Log.d("Spinner", "opgeslagen " + leraarLerlingselected);
                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                        preferences.edit().putString("laatstGeselecteerdeKlas", leraarLerlingselected).commit();
                                    }
                                    init[0] = false;

                                    new RoosterDownloader(getActivity(), getRootView(), true, refreshItem.get(), selectedWeek,
                                            ((TextView) view).getText().toString(), Type.KLASROOSTER).execute();
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {

                                }
                            });

                            // Ga naar de laatst gekozen klas toe
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            String laatstGeselecteerd = preferences.getString("laatstGeselecteerdeKlas", null);
                            if (laatstGeselecteerd != null) {
                                for (int klasIndex = 0; klasIndex < klassen.size(); klasIndex++) {
                                    if (klassen.get(klasIndex).equals(laatstGeselecteerd)) {
                                        klasspinner.setSelection(klasIndex);
                                        Log.d("Spinner", "selection set to " + laatstGeselecteerd);
                                    }
                                }
                            }
                        }
                    }
                }.execute();

                Tracker easyTracker = EasyTracker.getInstance(getActivity());
                easyTracker.set(Fields.SCREEN_NAME, "Klasrooster");
                easyTracker.send(MapBuilder
                                .createAppView()
                                .build()
                );
            }
            laadWeken(getActivity());
            return getRootView();

        }

        private void laadWeken(final Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                //Laad via internet
                new AsyncTask<Void, Void, ArrayList<RoosterInfoDownloader.Week>>() {

                    @Override
                    protected ArrayList<RoosterInfoDownloader.Week> doInBackground(Void... unused) {
                        try {
                            return RoosterInfoDownloader.getWeken();
                        } catch (Exception e) {
                            Log.e(getClass().getSimpleName(), "Fout bij het laden van de weken", e);
                            EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
                            easyTracker.send(MapBuilder
                                    .createException(new StandardExceptionParser(getActivity(), null)
                                            .getDescription(Thread.currentThread().getName(), e), false)
                                    .build());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<RoosterInfoDownloader.Week> wekenArray) {
                        try {
                            FileOutputStream arraySaver = context.openFileOutput("wekenarray", MODE_PRIVATE);
                            ObjectOutputStream arrayObjectOutputStream = new ObjectOutputStream(arraySaver);
                            arrayObjectOutputStream.writeObject(wekenArray);
                            arrayObjectOutputStream.close();
                            arraySaver.close();
                        } catch (Exception e) {
                            Log.e("MainActivity", "Kon de weken niet opslaan", e);
                            EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
                            easyTracker.send(MapBuilder
                                    .createException(new StandardExceptionParser(getActivity(), null)
                                            .getDescription(Thread.currentThread().getName(), e), false)
                                    .build());
                        }
                        addWekenToActionBar(wekenArray, context);
                    }
                }.execute();
            } else {
                try {
                    ObjectInputStream ois = new ObjectInputStream(context.openFileInput("wekenarray"));
                    ArrayList<RoosterInfoDownloader.Week> weken = (ArrayList<RoosterInfoDownloader.Week>) ois.readObject();
                    addWekenToActionBar(weken, context);
                } catch (Exception e) {
                    Log.e("MainActivity", "Kon de weken niet uit het geheugen laden", e);
                    EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
                    easyTracker.send(MapBuilder
                            .createException(new StandardExceptionParser(getActivity(), null)
                                    .getDescription(Thread.currentThread().getName(), e), false)
                            .build());
                }
            }
        }

        void addWekenToActionBar(ArrayList<RoosterInfoDownloader.Week> wekenArray, final Context context) {
            final ArrayList<String> strings = new ArrayList<String>();
            if (wekenArray == null) {
                strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            } else {

                //Get the index of the current week
                int indexCurrentWeek = 0;

                int correctionForWeekends = 0;
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 1 || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7) {
                    correctionForWeekends = 1;
                }

                loop:
                for (int u = 0; u < wekenArray.size(); u++)
                    if (wekenArray.get(u).week >= Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + correctionForWeekends) {
                        indexCurrentWeek = u;
                        break loop;
                    }
                final int NUMBER_OF_WEEKS_IN_SPINNER = 10;
                for (int c = 0; c < NUMBER_OF_WEEKS_IN_SPINNER; c++) {
                    strings.add("Week " + wekenArray.get((indexCurrentWeek + c) % wekenArray.size()).week);
                }
            }
            actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(getActivity(), strings, type);
            actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    if (PreferenceManager.getDefaultSharedPreferences(context).getString("key", null) != null) {
                        String itemString = (String) actionBarSpinnerAdapter.getItem(i);
                        int week = Integer.parseInt(itemString.substring(5));
                        selectedWeek = week;
                        laadRooster(context, getRootView(), type);
                    }
                    return true;
                }
            });
        }

        public void laadRooster(Context context, View rootView, Type type) {
            if (selectedWeek == -1){
                selectedWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            }

            Log.d("MainActivity", "Rooster aan het laden van week " + selectedWeek);
            if (type == Type.PERSOONLIJK_ROOSTER) {
                //Probeer de string uit het geheugen te laden
                RoosterWeek roosterWeek = RoosterWeek.laadUitGeheugen(selectedWeek, getActivity());

                //Als het de goede week is, gebruik hem
                if (roosterWeek != null && roosterWeek.getWeek() == selectedWeek) {
                    new RoosterBuilder(context, (ViewPager) rootView.findViewById(R.id.viewPager), selectedWeek, type).buildLayout(roosterWeek);
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                    new RoosterDownloader(context, rootView, false, refreshItem.get(), selectedWeek).execute();
                } else {
                    if (roosterWeek == null) {
                        Log.d("MainActivity", "Het uit het geheugen geladen rooster is null");
                    } else {
                        Log.d("MainActivity", "Het uit het geheugen geladen rooster is van week " + roosterWeek.getWeek() + ", de gewilde week is " + selectedWeek);
                    }
                    new RoosterDownloader(context, rootView, true, refreshItem.get(), selectedWeek).execute();
                }
            } else if (type == Type.KLASROOSTER) {
                new RoosterDownloader(context, rootView, true, refreshItem.get(), selectedWeek, leraarLerlingselected, type).execute();
            } else if (type == Type.DOCENTENROOSTER) {
                new RoosterDownloader(context, rootView, true, refreshItem.get(), selectedWeek, leraarLerlingselected, type).execute();
            }
        }

        public View getRootView() {
            return rootView;
        }

        public void setRootView(View rootView) {
            this.rootView = rootView;
        }

        public static enum Type {
            PERSOONLIJK_ROOSTER,
            KLASROOSTER,
            DOCENTENROOSTER,
        }
    }
}
