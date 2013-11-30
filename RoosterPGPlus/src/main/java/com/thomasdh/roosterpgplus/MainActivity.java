package com.thomasdh.roosterpgplus;

import android.app.Dialog;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.List;
import java.util.Scanner;

public class MainActivity extends ActionBarActivity {

    public ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // Maak de navigation drawer
        String[] keuzes = {"Persoonlijk rooster", "Andere roosters"};

        ListView drawerList = (ListView) findViewById(R.id.drawer);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, keuzes));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_linearlayout, new PlaceholderFragment())
                            .commit();
                } else if (position == 1) {
                    //TODO Ander rooster
                    Toast.makeText(getApplicationContext(), "Deze functie is nog niet geïmplementeerd", Toast.LENGTH_SHORT).show();
                }
            }
        });
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);
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
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            ViewPager linearLayout = (ViewPager) rootView.findViewById(R.id.viewPager);
            linearLayout.setAdapter(new MyPagerAdapter());


            // If the user apikey is already obtained
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("key", null) == null) {
                //Laat de gebruiker inloggen
                showLoginDialog();
            } else {
                laadRooster(linearLayout, getActivity(), rootView);
            }

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

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.execute();
        }

        private void laadRooster(final ViewPager linearLayout, final Context context, final View v) {
            new LoadSceduleAndBuildLayout(context, linearLayout, v).execute();
        }

        private void login(String gebruikersnaam, String wachtwoord) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost("http://rooster.fwest98.nl/api/account/login.php");
                    String s = null;
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

                        if (status == 400) {
                            return "error:Missende parameters";
                        } else if (status == 401) {
                            return "error:Ongeldige logingegevens";
                        } else if (status == 500) {
                            return "error:Serverfout";
                        } else if (status == 200) {
                            return new Scanner(response.getEntity().getContent()).nextLine();
                        } else {
                            return "error:Onbekende status: " + status;
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
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                    super.onPostExecute(s);
                }
            }.execute(gebruikersnaam, wachtwoord);
        }

        private void showLoginDialog() {
            final Dialog dialog = new Dialog(getActivity());
            dialog.setTitle("Log in");
            dialog.setContentView(R.layout.logindialog);

            final EditText gebruikersnaamEditText = (EditText) dialog.findViewById(R.id.logindialogusername);
            final EditText wachtwoordEditText = (EditText) dialog.findViewById(R.id.logindialogpassword);

            Button button = (Button) dialog.findViewById(R.id.dialogButtonOK);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    login(gebruikersnaamEditText.getText().toString(), wachtwoordEditText.getText().toString());
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }
}
