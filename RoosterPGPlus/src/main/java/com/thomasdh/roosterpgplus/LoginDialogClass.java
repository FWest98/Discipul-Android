package com.thomasdh.roosterpgplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Thomas on 3-12-13.
 */
public class LoginDialogClass {

    private Context context;
    private AlertDialog LoginDialog;
    private View rootView;
    private MainActivity.PlaceholderFragment mainFragment;

    public LoginDialogClass(Context context, View rootView, MainActivity.PlaceholderFragment mainFragment) {
        this.context = context;
        this.rootView = rootView;
        this.mainFragment = mainFragment;
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
                HttpPost httppost = new HttpPost(context.getResources().getString(R.string.API_base_url) + "account/login.php");
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
                    Toast.makeText(context, s.substring(6), Toast.LENGTH_LONG).show();
                } else if (s.equals("duplicate")) {
                    // Maak een mooie notifybox en blablabla *sich*
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle(context.getResources().getString(R.string.logindialog_warning_title));
                    dialog.setMessage(context.getResources().getString(R.string.logindialog_warning_text));
                    dialog.setPositiveButton(context.getResources().getString(R.string.logindialog_warning_submitButton), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            login(leerlingnummer, true, true);
                        }
                    })
                            .setNegativeButton(context.getResources().getString(R.string.logindialog_warning_cancelButton), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // do nothing
                                }
                            });

                    dialog.show();
                } else {
                    hideLoginDialog();
                    if (s.startsWith("nr1")) {
                        Toast.makeText(context, "Al bestaande app-account gekozen", Toast.LENGTH_LONG).show();
                        s = s.substring(4);
                    }
                    try {
                        JSONObject object = new JSONObject(s);
                        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        e.putString("key", object.getString("key"));
                        System.out.println("The key: " + object.getString("key"));
                        e.putString("naam", object.getString("klas"));
                        if (object.has("klas")) {
                            e.putString("klas", object.getString("klas"));
                            e.putBoolean("vertegenwoordiger", object.getBoolean("vertegenwoordiger"));
                        } else {
                            e.putString("code", object.getString("code"));
                        }
                        e.commit();
                        Toast.makeText(context, "Welkom, " + object.getString("naam") + "!", Toast.LENGTH_SHORT).show();
                        //Laad het rooster als de boolean true is
                        if (laadRooster) {
                            mainFragment.laadRooster(context, rootView);
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
                HttpPost httppost = new HttpPost(context.getResources().getString(R.string.API_base_url) + "account/login.php");
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
                    Toast.makeText(context, s.substring(6), Toast.LENGTH_LONG).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        e.putString("key", jsonObject.getString("key"));
                        System.out.println("The key: " + jsonObject.getString("key"));
                        e.putString("naam", jsonObject.getString("naam"));
                        if (jsonObject.has("klas")) {
                            e.putString("klas", jsonObject.getString("klas"));
                            e.putBoolean("vertegenwoordiger", jsonObject.getBoolean("vertegenwoordiger"));
                        } else {
                            e.putString("code", jsonObject.getString("code"));
                        }
                        e.commit();
                        Toast.makeText(context, "Welkom, " + jsonObject.getString("naam") + "!", Toast.LENGTH_SHORT).show();

                        //Laad het rooster
                        if (laadRooster) {
                            mainFragment.laadRooster(context, rootView);
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

    public void showLoginDialog() {
        showLoginDialog(false);
    }

    public void showLoginDialog(final boolean laadRooster) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogView = inflater.inflate(R.layout.logindialog, null);

        TabHost tabHost = (TabHost) dialogView.findViewById(R.id.DialogTabs);
        tabHost.setup();

// create tabs
        TabHost.TabSpec spec1 = tabHost.newTabSpec("tab1");
        spec1.setContent(R.id.Tab_UserPass);
        spec1.setIndicator(context.getResources().getString(R.string.logindialog_tabs_userpass));
        tabHost.addTab(spec1);

        TabHost.TabSpec spec2 = tabHost.newTabSpec("tab2");
        spec2.setContent(R.id.Tab_LLNR);
        spec2.setIndicator(context.getResources().getString(R.string.logindialog_tabs_llnr));
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
}
