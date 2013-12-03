package com.thomasdh.roosterpgplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

        // Maak de SpinnerAdapter voor weekkeuze

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
        private AlertDialog LoginDialog;

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
                showLoginDialog(true);
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


        // Inloggen met leerlingnummer

        private void laadRooster(final Context context, final View v) {

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

        private void login(int leerlingnummer) {
            login(leerlingnummer, false, false);
        }

        private void login(int leerlingnummer, boolean laadRooster) {
            login(leerlingnummer, false, laadRooster);
        }

        private void login(final int leerlingnummer, boolean force, final boolean laadRooster) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost(getString(R.string.API_base_url) + "account/login.php");
                    String s;

                    try {
                        // LLNR toevoegen
                        List<NameValuePair> getParameters = new ArrayList<NameValuePair>();

                        getParameters.add(new BasicNameValuePair("llnr", params[0]));
                        boolean push = params[1].equals("ja");
                        if (push) {
                            getParameters.add(new BasicNameValuePair("force", "true"));
                        }
                        UrlEncodedFormEntity form = new UrlEncodedFormEntity(getParameters);
                        httppost.setEntity(form);

                        // Uitvoeren
                        HttpResponse response = httpclient.execute(httppost);
                        int status = response.getStatusLine().getStatusCode();

                        switch (status) {
                            case 500:
                                return "error:Serverfout";
                            case 204:
                                return "duplicate";
                            case 200:
                                return new Scanner(response.getEntity().getContent()).nextLine();
                            default:
                                return "error:Onbekende fout";
                        }
                    } catch (ClientProtocolException e) {
                        s = "error:" + e.toString();
                    } catch (IOException e) {
                        s = "error:" + e.toString();
                    }
                    return s;
                }

                @Override
                protected void onPostExecute(String s) {
                    Log.e(this.getClass().getName(), "The string is: " + s);
                    if (s.startsWith("error:")) {
                        Toast.makeText(getActivity(), s.substring(6), Toast.LENGTH_LONG).show();
                    } else if (s.equals("duplicate")) {
                        // Maak een mooie notifybox en blablabla *sich*
                        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                        dialog.setTitle(getString(R.string.logindialog_warning_title));
                        dialog.setMessage(getString(R.string.logindialog_warning_text));
                        dialog.setPositiveButton(getString(R.string.logindialog_warning_submitButton), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                login(leerlingnummer, true, true);
                            }
                        })
                                .setNegativeButton(getString(R.string.logindialog_warning_cancelButton), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // do nothing
                                    }
                                });

                        dialog.show();
                    } else {
                        hideLoginDialog();
                        if (s.startsWith("nr1")) {
                            Toast.makeText(getActivity(), "Al bestaande app-account gekozen", Toast.LENGTH_LONG).show();
                            s = s.substring(4);
                        }
                        try {
                            JSONObject object = new JSONObject(s);
                            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            e.putString("key", object.getString("key"));
                            System.out.println("The key: " + object.getString("key"));
                            e.putString("naam", object.getString("klas"));
                            if (object.has("klas")) {
                                e.putString("klas", object.getString("klas"));
                                e.putBoolean("vertegenwoordiger", object.getBoolean("vertegenwoordiger"));
                            } else {
                                e.putString("code", object.getString("code"));
                            }
                            e.apply();
                            Toast.makeText(getActivity(), "Welkom, " + object.getString("naam") + "!", Toast.LENGTH_SHORT).show();
                            //Laad het rooster als de boolean true is
                            if (laadRooster) {
                                laadRooster(getActivity(), rootView);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        super.onPostExecute(s);
                    }
                }
            }.execute(Integer.toString(leerlingnummer), (force ? "ja" : "nee"));
        }

        /**
         * Inloggen met UserPass
         */
        private void login(String gebruikersnaam, String wachtwoord) {
            login(gebruikersnaam, wachtwoord, false);
        }

        private void login(String gebruikersnaam, String wachtwoord, final boolean laadRooster) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost(getString(R.string.API_base_url) + "account/login.php");
                    String s;
                    try {
                        // Add your data
                        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();

                        //TODO: Beveiliging!
                        postParameters.add(new BasicNameValuePair("username", params[0]));
                        postParameters.add(new BasicNameValuePair("password", params[1]));
                        postParameters.add(new BasicNameValuePair("encrypted", "false"));
                        UrlEncodedFormEntity form = new UrlEncodedFormEntity(postParameters);
                        httppost.setEntity(form);

                        // Execute HTTP Post Request
                        HttpResponse response = httpclient.execute(httppost);
                        int status = response.getStatusLine().getStatusCode();

                        switch (status) {
                            case 400:
                                return "error:Missende parameters";
                            case 401:
                                return "error:Ongeldige logingegevens";
                            case 500:
                                return "error:Serverfout";
                            case 200:
                                return new Scanner(response.getEntity().getContent()).nextLine();
                            default:
                                return "error:Onbekende fout";
                        }
                    } catch (ClientProtocolException e) {
                        s = "error:" + e.toString();
                    } catch (IOException e) {
                        s = "error:" + e.toString();
                    }
                    return s;
                }

                @Override
                protected void onPostExecute(String s) {
                    Log.e(this.getClass().getName(), "The string is: " + s);
                    if (s.startsWith("error:")) {
                        Toast.makeText(getActivity(), s.substring(6), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject(s);
                            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            e.putString("key", jsonObject.getString("key"));
                            System.out.println("The key: " + jsonObject.getString("key"));
                            e.putString("naam", jsonObject.getString("naam"));
                            if (jsonObject.has("klas")) {
                                e.putString("klas", jsonObject.getString("klas"));
                                e.putBoolean("vertegenwoordiger", jsonObject.getBoolean("vertegenwoordiger"));
                            } else {
                                e.putString("code", jsonObject.getString("code"));
                            }
                            e.apply();
                            Toast.makeText(getActivity(), "Welkom, " + jsonObject.getString("naam") + "!", Toast.LENGTH_SHORT).show();

                            //Laad het rooster
                            if (laadRooster) {
                                laadRooster(getActivity(), rootView);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                    super.onPostExecute(s);

                }
            }.execute(gebruikersnaam, wachtwoord);
        }

        private void hideLoginDialog() {
            LoginDialog.dismiss();
        }

        private void showLoginDialog() {
            showLoginDialog(false);
        }

        private void showLoginDialog(final boolean laadRooster) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.logindialog, null);

            TabHost tabHost = (TabHost) dialogView.findViewById(R.id.DialogTabs);
            tabHost.setup();

// create tabs
            TabHost.TabSpec spec1 = tabHost.newTabSpec("tab1");
            spec1.setContent(R.id.Tab_UserPass);
            spec1.setIndicator(getString(R.string.logindialog_tabs_userpass));
            tabHost.addTab(spec1);

            TabHost.TabSpec spec2 = tabHost.newTabSpec("tab2");
            spec2.setContent(R.id.Tab_LLNR);
            spec2.setIndicator(getString(R.string.logindialog_tabs_llnr));
            tabHost.addTab(spec2);


            builder.setView(dialogView)
                    .setPositiveButton(R.string.logindialog_loginbutton, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {
                        }
                    })
                    .setNeutralButton("Registreer", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                        }
                    })
                    .setNegativeButton(R.string.logindialog_cancelbutton, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                        }
                    });

            LoginDialog = builder.create();
            LoginDialog.show();
            LoginDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(inflater.getContext(), "Deze functie is nog niet geïmplementeerd", Toast.LENGTH_SHORT).show();
                }
            });
            LoginDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Implementeren week-/klaskeuze
                    LoginDialog.dismiss();
                }
            });
            LoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText username = (EditText) dialogView.findViewById(R.id.logindialogusername);
                    final EditText password = (EditText) dialogView.findViewById(R.id.logindialogpassword);
                    final EditText llnr = (EditText) dialogView.findViewById(R.id.logindialogllnr);
                    username.requestFocus();
                    try {
                        login(Integer.parseInt(llnr.getText().toString()), laadRooster);
                        // dismissen IN de login functie
                    } catch (Exception e) {
                        login(username.getText().toString(), password.getText().toString(), laadRooster);
                        LoginDialog.dismiss();
                    }

                }
            });
        }

        String laadInternal(int weeknr, Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("week" + (weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks)), "error:Er is nog geen rooster in het geheugen opgeslagen");
        }
    }
}
