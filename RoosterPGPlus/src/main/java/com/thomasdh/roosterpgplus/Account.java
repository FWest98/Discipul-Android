package com.thomasdh.roosterpgplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

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

/**
 * Created by Thomas on 3-12-13.
 */
public class Account {

    public Boolean isSet;
    public String name;
    public String apikey;
    public String klas;
    public Boolean vertegenwoordiger;
    public Boolean isAppAccount;
    public UserTypes userType;
    public String code;
    private Context context;
    private AlertDialog LoginDialog;
    private AlertDialog RegisterDialog;
    private AlertDialog ExtendDialog;
    private MainActivity.PlaceholderFragment mainFragment;


    public Account(Context context) {
        this.context = context;
        this.mainFragment = null;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        if (preferences.getString("key", null) == null) {
            this.isSet = false;
        } else {
            this.isSet = true;
            this.name = preferences.getString("naam", null);
            this.apikey = preferences.getString("key", null);

            // Of er een leraarcode is
            if (preferences.getString("code", null) == null) {
                this.userType = UserTypes.LEERLING;
                this.klas = preferences.getString("klas", null);
                this.vertegenwoordiger = preferences.getBoolean("vertegenwoordiger", false);
            } else {
                this.userType = UserTypes.LERAAR;
                this.code = preferences.getString("code", null);
            }

            this.isAppAccount = preferences.getBoolean("appaccount", true);
        }
    }

    public Account(Context context, MainActivity.PlaceholderFragment mainFragment) {
        this(context);
        this.mainFragment = mainFragment;
    }


    //region JSON-verwerking

    final public Account JSON_InitializeAccount(String s) throws JSONException {
        JSONObject object = new JSONObject(s);
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putString("key", object.getString("key"));
        this.apikey = object.getString("key");
        System.out.println("The key: " + object.getString("key"));
        e.putString("naam", object.getString("naam"));
        this.name = object.getString("naam");
        if (object.has("klas")) {
            this.userType = UserTypes.LEERLING;
            e.putString("klas", object.getString("klas"));
            this.klas = object.getString("klas");
            e.putBoolean("vertegenwoordiger", object.getBoolean("vertegenwoordiger"));
            this.vertegenwoordiger = object.getBoolean("vertegenwoordiger");
        } else {
            this.userType = UserTypes.LERAAR;
            e.putString("code", object.getString("code"));
            this.code = object.getString("code");
        }
        if (object.has("app_user")) {
            e.putBoolean("appaccount", object.getBoolean("app_user"));
            this.isAppAccount = object.getBoolean("app_user");
        }else{
            e.putBoolean("appaccount", false);
            this.isAppAccount = false;
        }
        e.commit();

        return this;
    }

    //endregion

    //region Login

    /**
     * Inloggen met leerlingnummer
     */

    private void login(int leerlingnummer) {
        login(leerlingnummer, false, false);
    }

    private void login(int leerlingnummer, boolean laadRooster) {
        login(leerlingnummer, false, laadRooster);
    }

    private void login(final int leerlingnummer, boolean force, final boolean laadRooster) {
        new AsyncTask<String, Exception, String>() {
            @Override
            protected String doInBackground(String... params) {

                //Controleer of het apparaat een internetverbinding heeft
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo == null || !netInfo.isConnectedOrConnecting()) {
                    return "error:De app kon geen verbinding maken met het internet";
                }

                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(Settings.API_Base_URL + "account/login.php");

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
                            throw new IOException("Serverfout");
                        case 204:
                            return "duplicate";
                        case 200:
                            String string = "";
                            Scanner s = new Scanner(response.getEntity().getContent());
                            while (s.hasNext()) {
                                string += s.nextLine();
                            }
                            return string;
                        default:
                            throw new Exception("Onbekende fout");
                    }
                } catch (Exception e) {
                    publishProgress(e);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Exception... values) {
                ExceptionHandler.handleException(values[0], context, "Fout bij het inloggen", "Account", ExceptionHandler.HandleType.EXTENSIVE);
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    Log.e(this.getClass().getName(), "The string is: " + s);
                    if (s.equals("duplicate")) {
                        // Maak een mooie notifybox en blablabla *sich*
                        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                        dialog.setTitle(context.getResources().getString(R.string.logindialog_warning_title));
                        dialog.setMessage(context.getResources().getString(R.string.logindialog_warning_text));
                        dialog.setPositiveButton(context.getResources().getString(R.string.logindialog_warning_submitButton), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                login(leerlingnummer, laadRooster, true);
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
                            Toast.makeText(context, "Al bestaand app-account gekozen", Toast.LENGTH_LONG).show();
                            s = s.substring(4);
                        }
                        try {
                            Account account = JSON_InitializeAccount(s);
                            Toast.makeText(context, "Welkom, " + account.name + "!", Toast.LENGTH_SHORT).show();

                            LoginDialog.dismiss();

                            //Laad het rooster
                            if (laadRooster && mainFragment != null) {
                                mainFragment.laadRooster(context, mainFragment.getRootView(), mainFragment.type);
                            }

                        } catch (JSONException e) {
                            ExceptionHandler.handleException(e, context, "Fout bij het inloggen", "Account", ExceptionHandler.HandleType.EXTENSIVE);
                        }
                    }
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
        new AsyncTask<String, Exception, String>() {
            @Override
            protected String doInBackground(String... params) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(Settings.API_Base_URL + "account/login.php");
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
                            throw new IOException("Missende parameters");
                        case 401:
                            throw new IOException("Ongeldige logingegevens");
                        case 500:
                            throw new IOException("Serverfout");
                        case 200:
                            String string = "";
                            Scanner s = new Scanner(response.getEntity().getContent());
                            while (s.hasNext()) {
                                string += s.nextLine();
                            }
                            return string;
                        default:
                            throw new Exception("Onbekende fout");
                    }
                } catch (Exception e) {
                    publishProgress(e);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Exception... e) {
                ExceptionHandler.handleException(e[0], context, "Fout bij het inloggen", "Account", ExceptionHandler.HandleType.EXTENSIVE);
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    Log.e(this.getClass().getName(), "The string is: " + s);
                    try {
                        Account account = JSON_InitializeAccount(s);
                        Toast.makeText(context, "Welkom, " + account.name + "!", Toast.LENGTH_SHORT).show();

                        LoginDialog.dismiss();

                        //Laad het rooster
                        if (laadRooster && mainFragment != null) {
                            mainFragment.laadRooster(context, mainFragment.getRootView(), mainFragment.type);
                        }

                    } catch (JSONException e) {
                        ExceptionHandler.handleException(e, context, "Fout bij het inloggen", "Account", ExceptionHandler.HandleType.EXTENSIVE);
                    }

                }
            }
        }.execute(gebruikersnaam, wachtwoord);
    }
    //endregion
    //region Registreren

    /**
     * Registreren
     */
    private void hideRegisterDialog() {
        RegisterDialog.dismiss();
    }

    private void register() {
        register(false);
    }

    private void register(final boolean laadRooster) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogView = inflater.inflate(R.layout.registerdialog, null);

        builder.setTitle("Registreer");

        builder.setView(dialogView)
                .setPositiveButton(R.string.registerdialog_registerbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(R.string.registerdialog_cancelbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        RegisterDialog = builder.create();
        RegisterDialog.show();
        RegisterDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Alle velden en verwerken */
                final EditText username = (EditText) dialogView.findViewById(R.id.registerdialog_username);
                final EditText password = (EditText) dialogView.findViewById(R.id.registerdialog_password);
                final EditText repass = (EditText) dialogView.findViewById(R.id.registerdialog_passwordcheck);
                final EditText llnr = (EditText) dialogView.findViewById(R.id.registerdialog_llnr);
                final EditText email = (EditText) dialogView.findViewById(R.id.registerdialog_email);
                username.requestFocus();
                if (username.getText().toString().equals("")) {
                    Toast.makeText(context, "Gebruikersnaam is verplicht!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.getText().toString().equals("")) {
                    Toast.makeText(context, "Wachtwoord is verplicht!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.getText().toString().equals(repass.getText().toString())) {
                    Toast.makeText(context, "Wachtwoorden niet gelijk!" + password.getText().toString() + "  " + repass.getText().toString(), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (llnr.getText().toString().equals("")) {
                    Toast.makeText(context, "Leerlingnummer is verplicht!", Toast.LENGTH_SHORT).show();
                    return;
                }
                register(username.getText().toString(), password.getText().toString(), Integer.parseInt(llnr.getText().toString()), email.getText().toString(), laadRooster, false);
                // dismissen in functie
            }
        });
    }

    private void register(final String username, final String password, final int llnr, final String email) {
        register(username, password, llnr, email, false, false);
    }

    private void register(final String username, final String password, final int llnr, final String email, final Boolean laadRooster, final boolean force) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(Settings.API_Base_URL + "account/register.php");
                String s;
                try {
                    List<NameValuePair> postParameters = new ArrayList<NameValuePair>();

                    // TODO: Beveiliging!

                    postParameters.add(new BasicNameValuePair("username", params[0]));
                    postParameters.add(new BasicNameValuePair("password", params[1]));
                    postParameters.add(new BasicNameValuePair("llnr", Integer.toString(llnr)));
                    postParameters.add(new BasicNameValuePair("email", params[2]));
                    if (force) {
                        postParameters.add(new BasicNameValuePair("force", "true"));
                    }
                    UrlEncodedFormEntity form = new UrlEncodedFormEntity(postParameters);
                    httpPost.setEntity(form);

                    HttpResponse response = httpClient.execute(httpPost);
                    int status = response.getStatusLine().getStatusCode();

                    switch (status) {
                        case 204:
                            return "duplicate";
                        case 500:
                            return "error:Serverfout";
                        case 409:
                            return "conflict";
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
                } else if (s.equals("conflict")) {
                    Toast.makeText(context, "Deze gebruikersnaam is al in gebruik", Toast.LENGTH_LONG).show();
                } else if (s.equals("duplicate")) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle(context.getResources().getString(R.string.logindialog_warning_title));
                    dialog.setMessage(context.getResources().getString(R.string.logindialog_warning_text));
                    dialog.setPositiveButton(context.getResources().getString(R.string.logindialog_warning_submitButton), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            register(username, password, llnr, email, laadRooster, true);
                        }
                    })
                            .setNegativeButton(context.getResources().getString(R.string.logindialog_warning_cancelButton), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // nothing
                                }
                            });
                    dialog.show();
                } else {
                    hideRegisterDialog();
                    hideLoginDialog();
                    try {
                        Account account = JSON_InitializeAccount(s);
                        Toast.makeText(context, "Welkom, " + account.name + "!", Toast.LENGTH_SHORT).show();
                        //Laad het rooster als de boolean true is
                        if (laadRooster && mainFragment != null) {
                            mainFragment.laadRooster(context, mainFragment.getRootView(), mainFragment.type);
                        }
                    } catch (JSONException e) {
                        ExceptionHandler.handleException(e, context, "Fout bij het inloggen", "Account", ExceptionHandler.HandleType.EXTENSIVE);
                    }
                    super.onPostExecute(s);
                }
            }
        }.execute(username, password, email);
    }
    //endregion
    //region Logindialogs

    /**
     * Dialogstuff
     */
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

        final TabHost tabHost = (TabHost) dialogView.findViewById(R.id.DialogTabs);
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
                register(laadRooster);
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
                int tab = tabHost.getCurrentTab();
                if(tab == 0) { // UserPass
                    if(username.getText().toString().equals("")) {
                        Toast.makeText(context, "Gebruikersnaam is verplicht!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(password.getText().toString().equals("")) {
                        Toast.makeText(context, "Wachtwoord is verplicht!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    login(username.getText().toString(), password.getText().toString(), laadRooster);
                } else if(tab == 1) {
                    if(llnr.getText().toString().equals("")) {
                        Toast.makeText(context, "Leerlingnummer is verplicht!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    login(Integer.parseInt(llnr.getText().toString()), laadRooster);
                } else {
                    Toast.makeText(context, "Er ging iets mis in de app; herstart de app en probeer het opnieuw", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    //endregion

    //region User_Extend

    /**
     * App_account uitbreiden
     */
    private void hideExtendDialog() {
        ExtendDialog.dismiss();
    }

    public void extend() throws Exception {
        if (this.isAppAccount) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View dialogView = inflater.inflate(R.layout.extenddialog, null);

            builder.setTitle("Upgrade account");
            builder.setView(dialogView)
                    .setPositiveButton(R.string.extenddialog_extendbutton, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setNegativeButton(R.string.registerdialog_cancelbutton, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            ExtendDialog = builder.create();
            ExtendDialog.show();
            ExtendDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText username = (EditText) dialogView.findViewById(R.id.extenddialog_username);
                    final EditText password = (EditText) dialogView.findViewById(R.id.extenddialog_password);
                    final EditText repass = (EditText) dialogView.findViewById(R.id.extenddialog_passwordcheck);
                    final EditText email = (EditText) dialogView.findViewById(R.id.extenddialog_email);
                    username.requestFocus();

                    if (username.getText().toString().equals("")) {
                        Toast.makeText(context, "Gebruikersnaam is verplicht!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (password.getText().toString().equals("")) {
                        Toast.makeText(context, "Wachtwoord is verplicht!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!password.getText().toString().equals(repass.getText().toString())) {
                        Toast.makeText(context, "Wachtwoorden niet gelijk!" + password.getText().toString() + "  " + repass.getText().toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        extend(username.getText().toString(), password.getText().toString(), email.getText().toString());
                    } catch (Exception e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            throw new Exception("Is al geen appaccount meer");
        }
    }

    public void extend(final String username, final String password, final String email) throws Exception {
        if (this.isAppAccount) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(Settings.API_Base_URL + "account/register.php?extend");
                    String s;
                    try {
                        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
                        postParameters.add(new BasicNameValuePair("key", params[0]));
                        postParameters.add(new BasicNameValuePair("username", params[1]));
                        postParameters.add(new BasicNameValuePair("password", params[2]));
                        if (!email.equals("")) {
                            postParameters.add(new BasicNameValuePair("email", email));
                        }

                        UrlEncodedFormEntity form = new UrlEncodedFormEntity(postParameters);
                        httpPost.setEntity(form);

                        HttpResponse response = httpClient.execute(httpPost);
                        int status = response.getStatusLine().getStatusCode();

                        switch (status) {
                            case 409:
                                return "conflict";
                            case 400:
                                return "error:Er mist een veld";
                            case 500:
                                return "error:Serverfout";
                            case 200:
                                return "nothing";
                            default:
                                return "error:Onbekende fout";
                        }
                    } catch (Exception e) {
                        s = "error:" + e.getMessage();
                        e.printStackTrace();
                    }
                    return s;
                }

                @Override
                protected void onPostExecute(String s) {
                    Log.e(this.getClass().getName(), "The string is:" + s);
                    if (s.startsWith("error:")) {
                        Toast.makeText(context, s.substring(6), Toast.LENGTH_LONG).show();
                    } else if (s.equals("conflict")) {
                        Toast.makeText(context, "Deze gebruikersnaam is al in gebruik", Toast.LENGTH_LONG).show();
                    } else {
                        hideExtendDialog();
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("appaccount", false).commit();
                        super.onPostExecute(s);
                    }
                }
            }.execute(this.apikey, username, password);
        } else {
            throw new Exception("Je bent al geüpgrade");
        }
    }

    //endregion
    //region Subklassen
    public ArrayList<Subklas> getSubklassen(Context context) throws IOException, JSONException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Settings.API_Base_URL + "account/manage/subklassen?key=" + apikey);
        HttpResponse response = httpClient.execute(get);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            String s = "";
            Scanner scanner = new Scanner(response.getEntity().getContent());
            while (scanner.hasNext()) {
                s += scanner.nextLine();
            }
            Log.d("RoosterInfoDownloader", "Subklassen: " + s);
            ArrayList<Subklas> subklassen = new ArrayList<Subklas>();
            JSONArray jsonArray = new JSONArray(s);
            for (int o = 0; o < jsonArray.length(); o++) {
                JSONObject subklas = jsonArray.getJSONObject(o);
                subklassen.add(new Subklas(subklas));
            }
            Log.d("RoosterInfoDownloader", "The size is " + jsonArray.length());
            return subklassen;

        } else if (status == 400) {
            throw new IOException("Er mist een parameter");
        } else if (status == 401) {
            throw new IOException("Account bestaat niet");
        } else if (status == 500) {
            throw new IOException("Serverfout");
        } else {
            throw new IOException("Onbekende status: " + status);
        }
    }

    public boolean setSubklassen(String[] subklassen) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Settings.API_Base_URL + "account/manage/subklassen");

        // Add your data
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();

        String klassen = "";
        postParameters.add(new BasicNameValuePair("key", apikey));
        for (int index = 0; index < subklassen.length; index++) {
            Log.d("RoosterInfoDownloader", subklassen[index]);
            postParameters.add(new BasicNameValuePair("subklassen[" + index + "]", subklassen[index]));
        }
        postParameters.add(new BasicNameValuePair("setAll", "true"));
        UrlEncodedFormEntity form = new UrlEncodedFormEntity(postParameters);
        httppost.setEntity(form);

        // Execute HTTP Post Request
        HttpResponse response = httpclient.execute(httppost);
        int status = response.getStatusLine().getStatusCode();

        switch (status) {
            case 400:
                String err = "";
                Scanner scanner = new Scanner(response.getEntity().getContent());
                while (scanner.hasNext()) {
                    err += scanner.nextLine();
                }
                throw new IOException("Er mist een parameter: " + err);
            case 401:
                throw new IOException("Verkeerde logingegevens");
            case 500:
                throw new IOException("Serverfout");
            case 200:
                return true;
            default:
                throw new IOException("Onbekende fout: " + status);
        }

    }

    public static class Subklas {
        public String subklas;
        public int jaarlaag;
        public String vak;
        public String leraar;
        public int nummer;

        public Subklas(String subklas, int jaarlaag, String vak, String leraar, int nummer) {
            this.subklas = subklas;
            this.jaarlaag = jaarlaag;
            this.vak = vak;
            this.leraar = leraar;
            this.nummer = nummer;
        }

        public Subklas(JSONObject object) throws JSONException {
            this(object.getString("subklas"),
                    object.getInt("jaarlaag"),
                    object.getString("vak"),
                    object.getString("leraar"),
                    object.getInt("nummer"));
        }
    }

    //endregion

    public enum UserTypes {
        LERAAR,
        LEERLING
    }
}
