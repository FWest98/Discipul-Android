package com.thomasdh.roosterpgplus;

import android.content.Context;
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
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

public class MainActivity extends ActionBarActivity {

    public static MenuItem refreshItem;
    public static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    public static ActionBar actionBar;
    private static int selectedWeek = -1;
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
        actionBarSpinnerAdapter.addItem("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_settings:
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
            } else {
                laadRooster(getActivity(), rootView);
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
                            ArrayList<String> strings = new ArrayList<String>();
<<<<<<< HEAD
                            if (string == null) {
                                string = PreferenceManager.getDefaultSharedPreferences(context).getString("weken", null);
                            }
                            if (string != null) {
=======
                            if(string == null) {
                                string = PreferenceManager.getDefaultSharedPreferences(context).getString("weken", null);
                            }
                            if(string != null) {
>>>>>>> hotfix-1.0.1

                                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("weken", string).commit();

                                JSONArray weekArray = new JSONArray(string);
<<<<<<< HEAD
                                ArrayList<String> weken = new ArrayList<String>();

                                for (int i = 0; i < weekArray.length(); i++) {
                                    JSONObject week = weekArray.getJSONObject(i);
                                    weken.add(week.getString("week"));
                                }
                                //Get the index of the current week
                                int indexCurrentWeek = -1;
                                for (int u = 0; u < weken.size(); u++) {
                                    if (Integer.parseInt(weken.get(u)) == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) {
                                        indexCurrentWeek = u;
                                        break;
                                    }
                                }
                                for (int c = 0; c < 3; c++) {
                                    strings.add("Week " + weken.get(indexCurrentWeek + c % weken.size()));
                                }
=======
                                ArrayList<Integer> weken = new ArrayList<Integer>();
                                ArrayList<Integer> vakantieweken = new ArrayList<Integer>();

                                for (int i = 0; i < weekArray.length(); i++) {
                                    JSONObject week = weekArray.getJSONObject(i);
                                    weken.add(week.getInt("week"));
                                    if(week.getBoolean("vakantieweek")) {
                                        vakantieweken.add(week.getInt("week"));
                                    }
                                }

                                //Get the index of the current week
                                int getweek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
                                for (int c = 0; c < 3; c++) {
                                    if(getweek > 52) {
                                        getweek = 1;

                                    }
                                    if(vakantieweken.contains(getweek)) {
                                        Log.e("Volgende", Integer.toString(getweek));
                                        getweek++;
                                        continue;
                                    }
                                    strings.add("Week " + getweek);
                                    getweek++;
                                }

                                Log.e("Ophalen", vakantieweken.toString());
>>>>>>> hotfix-1.0.1
                            } else {
                                strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                            }

                            //TODO Een andere week kan hiermee worden toegevoegd
                            // strings.add("Andere week");

                            actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(getActivity(), strings);
                            actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, new ActionBar.OnNavigationListener() {
                                @Override
                                public boolean onNavigationItemSelected(int i, long l) {
                                    if (i != 3) {
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
            String JSON = laadInternal(selectedWeek, getActivity());

            //Als het de goede week is, gebruik hem
            if (JSON.contains("\"week\":\"" + (selectedWeek) + "\"")) {
                new RoosterBuilder(context, (ViewPager) v.findViewById(R.id.viewPager), v).buildLayout(JSON);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                new RoosterDownloader(context, v, false, refreshItem, selectedWeek).execute();
            } else {
                if (JSON.startsWith("error:")) {
                    Log.w("MainActivity", JSON.substring(6));
                } else {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is niet van de goede week, de gewilde week is " + selectedWeek);
                    Log.d("MainActivity", "De uit het geheugen geladen string is: " + JSON);
                }
                new RoosterDownloader(context, v, true, refreshItem, selectedWeek).execute();
            }
        }

        String laadInternal(int weeknr, Context context) {
            if (weeknr == -1) {
                weeknr = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            }
            return PreferenceManager.getDefaultSharedPreferences(context).getString("week" + (weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks)), "error:Er is nog geen rooster in het geheugen opgeslagen voor week " + weeknr);
        }
    }
}
