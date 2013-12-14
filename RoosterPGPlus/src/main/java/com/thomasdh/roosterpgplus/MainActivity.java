package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
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
import android.widget.ListView;
import android.widget.Toast;

import com.thomasdh.roosterpgplus.roosterdata.RoosterWeek;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

public class MainActivity extends ActionBarActivity {

    public static WeakReference<MenuItem> refreshItem;
    public static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    public static ActionBar actionBar;
    public static int selectedWeek = -1;
    private static ArrayList<String> weken;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public PlaceholderFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();

        mainFragment = new PlaceholderFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        }

        // Maak de navigation drawer
        String[] keuzes = {"Persoonlijk rooster", "Andere roosters"};

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);

        final ListView drawerList = (ListView) findViewById(R.id.drawer);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, keuzes));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mainFragment = new PlaceholderFragment();
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_linearlayout, mainFragment, "Main_Fragment")
                            .commit();
                    //TODO reload rooster (dus gewoon opnieuw de view aanmaken?)
                } else if (position == 1) {
                    //TODO Ander rooster
                    Toast.makeText(getApplicationContext(), "Deze functie is nog niet geïmplementeerd", Toast.LENGTH_SHORT).show();
                }
                drawerLayout.closeDrawer(drawerList);
            }
        });

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
        actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, new ArrayList<String>());
        //Voeg beide toe
        getSupportActionBar().setListNavigationCallbacks(actionBarSpinnerAdapter, onNavigationListener);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
        // new Notify(this);
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
                new RoosterDownloader(this, mainFragment.rootView, true, item, selectedWeek).execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        public View rootView;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
            viewPager.setAdapter(new MyPagerAdapter());


            // If the user apikey is already obtained
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("key", null) == null) {
                //Laat de gebruiker inloggen -> wel rooster laden daarna
                new LoginDialogClass(getActivity(), rootView, this).showLoginDialog(true);
            }
            this.rootView = rootView;

            laadWeken(getActivity());

            return rootView;

        }

        private void laadWeken(final Context context) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet Get = new HttpGet("http://rooster.fwest98.nl/api/rooster/info?weken");

                    try {
                        HttpResponse response = httpclient.execute(Get);
                        int status = response.getStatusLine().getStatusCode();

                        if (status == 200) {
                            String s = "";
                            Scanner sc = new Scanner(response.getEntity().getContent());
                            while (sc.hasNext()) {
                                s += sc.nextLine();
                            }
                            return s;
                        } else {
                            return "error:Onbekende status: " + status;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String string) {
                    System.out.println("!!!!WEKENE:" + string);
                    if (string != null && string.startsWith("error:")) {
                        Toast.makeText(getActivity(), string.substring(6), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            weken = new ArrayList<String>();
                            final ArrayList<String> strings = new ArrayList<String>();

                            if (string == null) {
                                string = PreferenceManager.getDefaultSharedPreferences(context).getString("weken", null);
                            }
                            if (string != null) {

                                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("weken", string).commit();

                                JSONArray weekArray = new JSONArray(string);

                                for (int i = 0; i < weekArray.length(); i++) {
                                    JSONObject week = weekArray.getJSONObject(i);
                                    if (!week.getBoolean("vakantieweek")) {
                                        weken.add(week.getString("week"));
                                    }
                                }
                                //Get the index of the current week
                                int indexCurrentWeek = -1;
                                for (int u = 0; u < weken.size(); u++) {

                                    int correctionForWeekends = 0;
                                    if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 1 || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7) {
                                        correctionForWeekends = 1;
                                    }

                                    if (Integer.parseInt(weken.get(u)) == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + correctionForWeekends) {
                                        indexCurrentWeek = u;
                                        break;
                                    }
                                }
                                final int NUMBER_OF_WEEKS_IN_SPINNER = 10;
                                for (int c = 0; c < NUMBER_OF_WEEKS_IN_SPINNER; c++) {
                                    strings.add("Week " + weken.get((indexCurrentWeek + c) % weken.size()));
                                }
                            } else {
                                strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                            }

                            actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(getActivity(), strings);
                            actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, new ActionBar.OnNavigationListener() {
                                final ActionBar.OnNavigationListener onNavigationListener = this;

                                @Override
                                public boolean onNavigationItemSelected(int i, long l) {
                                    if (PreferenceManager.getDefaultSharedPreferences(context).getString("key", null) != null) {
                                        String itemString = (String) actionBarSpinnerAdapter.getItem(i);
                                        int week = Integer.parseInt(itemString.substring(5));
                                        selectedWeek = week;
                                        laadRooster(context, rootView);
                                    }
                                    return true;
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.execute();
        }

        public void laadRooster(final Context context, final View v) {

            //Probeer de string uit het geheugen te laden
            RoosterWeek roosterWeek = laadInternal(selectedWeek, getActivity());

            //Als het de goede week is, gebruik hem
            if (roosterWeek != null && roosterWeek.getWeek() == selectedWeek) {
                new RoosterBuilder(context, (ViewPager) v.findViewById(R.id.viewPager), v, selectedWeek).buildLayout(roosterWeek);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                new RoosterDownloader(context, v, false, refreshItem.get(), selectedWeek).execute();
            } else {
                if (roosterWeek == null) {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is null");
                } else {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is van week " + roosterWeek.getWeek() + ", de gewilde week is " + selectedWeek);
                }
                new RoosterDownloader(context, v, true, refreshItem.get(), selectedWeek).execute();
            }
        }

        RoosterWeek laadInternal(int week, Context context) {
            if (week == -1) {
                week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            }
            RoosterWeek roosterWeek;
            try {
                FileInputStream fis = context.openFileInput("roosterWeek" + (week % 4));
                ObjectInputStream ois = new ObjectInputStream(fis);
                roosterWeek = (RoosterWeek) ois.readObject();
                ois.close();
                fis.close();
                return roosterWeek;
            } catch (Exception e) {
                Log.e("MainActivity", "Kon het rooster niet laden", e);
                e.printStackTrace();
            }
            return null;
        }
    }
}
