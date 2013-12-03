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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public PlaceholderFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                new DownloadRoosterInternet(this, mainFragment.rootView, true, item).execute();
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
            return rootView;

        }

        private void laadWeken(final LinearLayout linearLayout, final Context context) {
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
                    if (string.startsWith("error:")) {
                        Toast.makeText(getActivity(), string.substring(6), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            JSONArray weekArray = new JSONArray(string);
                            ArrayList<String> weken = new ArrayList<String>();

                            for (int i = 0; i < weekArray.length(); i++) {
                                JSONObject week = weekArray.getJSONObject(i);
                                weken.add(week.getString("week"));
                            }

                            /* TODO: Implement Actionbar Spinner */


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.execute();
        }

        public void laadRooster(final Context context, final View v) {

            //Probeer de string uit het geheugen te laden
            String JSON = laadInternal(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), getActivity());

            //Als het de goede week is, gebruik hem
            if (JSON.contains("\"week\":\"" + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) + "\"")) {
                new LayoutBuilder(context, (ViewPager) v.findViewById(R.id.viewPager), v).buildLayout(JSON);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
            } else {
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is niet van de goede week, de week is nu " + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)));
                Log.d("MainActivity", "De uit het geheugen geladen string is: " + JSON);
            }

            //Download het rooster
            new DownloadRoosterInternet(context, v, false, refreshItem).execute();

        }

        String laadInternal(int weeknr, Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("week" + (weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks)), "error:Er is nog geen rooster in het geheugen opgeslagen");
        }
    }
}
