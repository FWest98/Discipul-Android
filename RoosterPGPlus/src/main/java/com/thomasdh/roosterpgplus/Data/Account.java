package com.thomasdh.roosterpgplus.Data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.InternetConnectionManager;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotifications;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import lombok.Getter;

@SuppressWarnings("UnusedDeclaration")
public class Account {
    @Getter private static int userID = 0;
    @Getter private static boolean isSet;
    @Getter private static boolean isHandlingNewVersion;
    @Getter private static String name;
    @Getter private static String apiKey;
    @Getter private static boolean isAppAccount;
    @Getter private static UserType userType;

    @Getter private static boolean isVertegenwoordiger;
    @Getter private static String leerlingKlas;

    @Getter private static String leraarCode;

    private Context context;
    private static int currentVersion = 0;

    private static Account instance;

    private static final String loginInternetListenerName = "loginDialog";
    private static final String registerInternetListenerName = "registerDialog";
    private static final String extendInternetListenerName = "extendDialog";

    //region Initialize

    public static void initialize(Context context) {
        initialize(context, true);
    }

    public static void initialize(Context context, boolean showUI) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        currentVersion = 0;
        int oldVersion;

        try {
            currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Account", e.getMessage(), e);
        }

        if((oldVersion = pref.getInt("oldVersion", currentVersion)) != currentVersion && !isHandlingNewVersion) {
            // Er was een updatesel
            int[] breakingVersions = context.getResources().getIntArray(R.array.breaking_account_versions);
            for(int version : breakingVersions) {
                if(currentVersion >= version && oldVersion < version) { // breaking changes
                    switch(version) {
                        case 6: {
                            isSet = false;
                            isHandlingNewVersion = true;
                            userType = UserType.NO_ACCOUNT;
                            ExceptionHandler.handleException(new Exception("Wijzigingen in de app vereisen opnieuw inloggen"), context, ExceptionHandler.HandleType.SIMPLE);
                            return;
                        }
                        case 15: {
                            // Vragen voor nieuwe meldingen
                            if(!showUI || !HelperFunctions.checkPlayServices(context)) break;
                            isHandlingNewVersion = true;

                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setTitle("Wil je pushmeldingen inschakelen?")
                                    .setMessage("Vanaf nu kan je pushmeldingen ontvangen bij roosterwijzigingen op dezelfde dag. Wil je deze inschakelen?")
                                    .setPositiveButton("Inschakelen", (dialog, which) -> {
                                        Account.getInstance(context, true).registerGCM();

                                        pref.edit()
                                                .putInt("oldVersion", currentVersion)
                                                .putBoolean("pushNotificaties", true)
                                                .commit();
                                        isHandlingNewVersion = false;
                                    })
                                    .setNegativeButton("Negeren", (dialog, which) -> {
                                        Account.getInstance(context, true).registerGCM();

                                        pref.edit()
                                                .putInt("oldVersion", currentVersion)
                                                .putBoolean("pushNotificaties", false)
                                                .commit();
                                        isHandlingNewVersion = false;
                                    });

                            builder.show();
                        }
                        case 17: {
                            // Analytics, get Account ID
                            if((apiKey = pref.getString("key", null)) == null) break;

                            isHandlingNewVersion = true;

                            getAccountInfo(s -> {
                                JSONObject base = new JSONObject((String) s);

                                userID = base.getInt("id");
                                pref.edit().putInt("oldVersion", currentVersion).putInt("userid", userID).apply();

                                MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext())
                                    .set("&uid", String.valueOf(userID));

                                isHandlingNewVersion = false;
                            }, context);
                        }
                        case 19: {
                            // GCMID wel verzenden, maar niet lokaal inschakelen
                            isHandlingNewVersion = true;
                            if(HelperFunctions.hasInternetConnection(context)) {
                                Account.getInstance(context, true).registerGCM();
                                pref.edit().putInt("oldVersion", currentVersion).commit();
                                isHandlingNewVersion = false;
                            }
                        }
                    }
                }
            }
            if(!isHandlingNewVersion) /* No breaking change */ pref.edit().putInt("oldVersion", currentVersion).commit();
        }

        String key;
        if((key = pref.getString("key", null)) == null) {
            isSet = false;
            userType = UserType.NO_ACCOUNT;
        } else {
            isSet = true;
            userID = pref.getInt("userid", 0);
            name = pref.getString("naam", null);
            apiKey = key;
            isAppAccount = pref.getBoolean("appaccount", false);

            String klas;
            if((klas = pref.getString("klas", null)) != null) {
                // Leerling
                leerlingKlas = klas;
                userType = UserType.LEERLING;
                isVertegenwoordiger = pref.getBoolean("isVertegenwoordiger", false);
            } else {
                // Leraar
                userType = UserType.LERAAR;
                leraarCode = pref.getString("code", null);
            }
        }
    }

    private void processJSON(String JSON, boolean isAppAccount, Activity activity) throws JSONException {
        if(isSet()) logout(s -> {});
        JSONObject base = new JSONObject(JSON);
        SharedPreferences.Editor pref = PreferenceManager.getDefaultSharedPreferences(context).edit();

        apiKey = base.getString("key");
        pref.putString("key", apiKey);

        name = base.getString("naam");
        pref.putString("naam", name);

        Account.isAppAccount = isAppAccount;
        pref.putBoolean("appaccount", isAppAccount);

        isSet = true;

        pref.putInt("oldVersion", currentVersion);
        isHandlingNewVersion = false;

        if (base.has("code")) {
            // LERAAR
            userType = UserType.LERAAR;

            leraarCode = base.getString("code");
            pref.putString("code", leraarCode);
        } else {
            // LEERLING
            userType = UserType.LEERLING;

            leerlingKlas = base.getJSONObject("klas").getString("klasnaam");
            pref.putString("klas", leerlingKlas);

            isVertegenwoordiger = base.getBoolean("vertegenwoordiger");
            pref.putBoolean("isVertegenwoordiger", isVertegenwoordiger);
        }

        pref.apply();

        ExceptionHandler.handleException(new Exception("Welkom, " + getName()), context, ExceptionHandler.HandleType.SIMPLE);

        NextUurNotifications.disableNotifications(context);
        new NextUurNotifications(context);

        registerGCM();

        getAccountInfo(p -> {
            JSONObject json = new JSONObject((String) p);

            userID = json.getInt("id");
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("userid", userID).apply();

            MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext())
                    .set("&uid", String.valueOf(userID));
        }, context);
    }

    //endregion
    //region Constructors

    private Account(Context context) {
        initialize(context, true);
        this.context = context;
    }

    private Account(Context context, boolean showUI) {
        initialize(context, showUI);
        this.context = context;
    }

    public static Account getInstance(Context context) {
        return getInstance(context, true);
    }

    public static Account getInstance(Context context, boolean showUI) {
        if(instance == null || !context.equals(instance.context)) {
            instance = new Account(context, showUI);
        }
        return instance;
    }

    //endregion

    //region Login

    public void login(AsyncActionCallback callback) {
        login((Activity) null, callback);
    }

    public void login(AsyncActionCallback callback, AsyncActionCallback cancelCallback) {
        login(null, callback, cancelCallback);
    }

    public void login(Activity activity, AsyncActionCallback callback) {
        login(activity, callback, result -> {});
    }

    public void login(Activity activity, AsyncActionCallback callback, AsyncActionCallback cancelCallback) {
        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_LOGIN);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.logindialog, null);
        TabHost tabHost = (TabHost) dialogView.findViewById(R.id.DialogTabs);

        tabHost.setup();

        TabHost.TabSpec userPassTab = tabHost.newTabSpec("userPassTab");
        userPassTab.setContent(R.id.Tab_UserPass);
        userPassTab.setIndicator(context.getResources().getString(R.string.logindialog_tabs_userpass));
        tabHost.addTab(userPassTab);

        TabHost.TabSpec llnrTab = tabHost.newTabSpec("llnrTab");
        llnrTab.setContent(R.id.Tab_LLNR);
        llnrTab.setIndicator(context.getResources().getString(R.string.logindialog_tabs_llnr));
        tabHost.addTab(llnrTab);

        builder.setView(dialogView)
                .setPositiveButton(R.string.logindialog_loginbutton, (dialogInterface, id) -> {})
                .setNeutralButton(R.string.logindialog_registerbutton, (dialogInterface, id) -> {
                })
                .setNegativeButton(R.string.logindialog_cancelbutton, (dialogInterface, id) -> {
                });

        AlertDialog loginDialog = builder.create();
        loginDialog.show();
        loginDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> register(activity, result -> { loginDialog.dismiss(); callback.onAsyncActionComplete(result); }));
        loginDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            loginDialog.dismiss();
            try {
                cancelCallback.onAsyncActionComplete(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        loginDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = ((EditText) dialogView.findViewById(R.id.logindialogusername)).getText().toString();
            String password = ((EditText) dialogView.findViewById(R.id.logindialogpassword)).getText().toString();
            String llnrString = ((EditText) dialogView.findViewById(R.id.logindialogllnr)).getText().toString();

            try {
                int currentTab = tabHost.getCurrentTab();
                switch (currentTab) {
                    case 0:
                        if ("".equals(username)) throw new Exception("Gebruikersnaam is verplicht!");
                        if ("".equals(password)) throw new Exception("Wachtwoord is verplicht!");

                        login(activity, username, password, result -> {
                            loginDialog.dismiss();
                            callback.onAsyncActionComplete(result);
                        });

                        break;
                    case 1:
                        if ("".equals(llnrString)) throw new Exception("Leerlingnummer is verplicht!");

                        login(activity, llnrString, false, result -> {
                            loginDialog.dismiss();
                            callback.onAsyncActionComplete(result);
                        });

                        break;
                    default:
                        throw new Exception("Er ging iets mis. Probeer het opnieuw");
                }
            } catch(Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });

        /* Internetdingen */
        InternetConnectionManager.registerListener(loginInternetListenerName, hasInternetConnection -> {
            loginDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(hasInternetConnection);
            loginDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(hasInternetConnection);
        });
        loginDialog.setOnDismissListener(dialog -> InternetConnectionManager.unregisterListener(loginInternetListenerName));
    }

    private void login(Activity activity, String username, String password, AsyncActionCallback callback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_LOGIN)
                        .build());

        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                List<NameValuePair> postParamaters = new ArrayList<>();
                postParamaters.add(new BasicNameValuePair("username", username));
                postParamaters.add(new BasicNameValuePair("password", password));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParamaters);

                URL url = new URL(Constants.HTTP_BASE + "account/login");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) entity.getContentLength());

                entity.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Missende gegevens");
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Ongeldige logingegevens");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                        if("".equals(data)) throw new Exception("Onbekende fout. Probeer het opnieuw");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    processJSON(data, false, activity);
                    callback.onAsyncActionComplete(data);
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij het inloggen", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij het inloggen", e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    private void login(Activity activity, String llnr, boolean force, AsyncActionCallback callback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_LOGIN)
                        .build());

        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("llnr", llnr));
                UrlEncodedFormEntity data = new UrlEncodedFormEntity(postParameters);

                URL url = new URL(Constants.HTTP_BASE + "account/login" + (force ? "?force" : ""));
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Missende gegevens");
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        throw new IllegalArgumentException("Deze gebruiker bestaat al");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                        if("".equals(data)) throw new Exception("Onbekende fout. Probeer het opnieuw");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    processJSON(data, true, activity);
                    callback.onAsyncActionComplete(data);
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij het inloggen", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                try {
                    throw exception;
                } catch(IllegalArgumentException e) {
                    // Dubbele gebruiker
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.logindialog_warning_title);
                    builder.setMessage(R.string.logindialog_warning_text);
                    builder.setPositiveButton(R.string.logindialog_warning_submitButton, (dialog, which) -> login(activity, llnr, true, callback));
                    builder.setNegativeButton(R.string.logindialog_warning_cancelButton, (dialog, which) -> {});

                    builder.show();
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij het inloggen:" + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region Register

    public void register(AsyncActionCallback callback) {
        register(null, callback);
    }

    public void register(Activity activity, AsyncActionCallback callback) {
        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_REGISTER);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.registerdialog, null);

        builder.setTitle(context.getString(R.string.registerdialog_title));

        builder.setView(dialogView)
                .setPositiveButton(R.string.registerdialog_registerbutton, (dialog, which) -> {})
                .setNegativeButton(R.string.registerdialog_cancelbutton, (dialog, which) -> dialog.dismiss());

        AlertDialog registerDialog = builder.create();
        registerDialog.show();
        registerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = ((EditText) dialogView.findViewById(R.id.registerdialog_username)).getText().toString();
            String password = ((EditText) dialogView.findViewById(R.id.registerdialog_password)).getText().toString();
            String repass = ((EditText) dialogView.findViewById(R.id.registerdialog_passwordcheck)).getText().toString();
            String llnr = ((EditText) dialogView.findViewById(R.id.registerdialog_llnr)).getText().toString();
            String email = ((EditText) dialogView.findViewById(R.id.registerdialog_email)).getText().toString();

            try {
                if ("".equals(username)) throw new Exception("Gebruikersnaam is verplicht!");
                if ("".equals(password)) throw new Exception("Wachtwoord is verplicht!");
                if(!password.equals(repass)) throw new Exception("Wachtwoorden moeten gelijk zijn!");
                if ("".equals(llnr)) throw new Exception("Leerlingnummer is verplicht!");

                register(activity, username, password, llnr, email, false, result -> {
                    registerDialog.dismiss();
                    callback.onAsyncActionComplete(result);
                });
            } catch (Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });

        /* Internetdingen */
        InternetConnectionManager.registerListener(registerInternetListenerName, hasInternetConnection -> registerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(hasInternetConnection));
        registerDialog.setOnDismissListener(dialog -> InternetConnectionManager.unregisterListener(registerInternetListenerName));
    }

    private void register(Activity activity, String username, String password, String llnr, String email, boolean force, AsyncActionCallback callback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_REGISTER)
                        .build());

        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("username", username));
                postParameters.add(new BasicNameValuePair("password", password));
                postParameters.add(new BasicNameValuePair("llnr", llnr));
                postParameters.add(new BasicNameValuePair("email", email));
                UrlEncodedFormEntity data = new UrlEncodedFormEntity(postParameters);

                URL url = new URL(Constants.HTTP_BASE + "account/register" + (force ? "?force" : ""));
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        throw new IllegalArgumentException("Deze gebruiker bestaat al");
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Missende gegevens");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebReuest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_CONFLICT:
                        throw new Exception("Deze gebruikersnaam is al in gebruik!");
                    case HttpURLConnection.HTTP_OK:
                        if("".equals(data)) throw new Exception("Onbekende fout");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    processJSON(data, true, activity);
                    callback.onAsyncActionComplete(data);
                } catch (Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij registreren", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                try {
                    throw exception;
                } catch(IllegalArgumentException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.logindialog_warning_title);
                    builder.setMessage(R.string.logindialog_warning_text);

                    builder.setPositiveButton(R.string.logindialog_warning_submitButton, (dialog, which) -> register(activity, username, password, llnr, email, true, callback));
                    builder.setNegativeButton(R.string.logindialog_cancelbutton, (dialog, which) -> {});
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij registreren:" + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region Extend

    public void extend() {
        extend(result -> {});
    }

    public void extend(AsyncActionCallback callback) {
        if(!isAppAccount) {
            ExceptionHandler.handleException(new Exception("Je bent al geüpgraded!"), context, ExceptionHandler.HandleType.SIMPLE);
            return;
        }

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_EXTEND);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.extenddialog, null);

        builder.setTitle(R.string.extenddialog_title);
        builder.setView(dialogView)
                .setPositiveButton(R.string.extenddialog_extendbutton, (dialog, which) -> {})
                .setNegativeButton(R.string.registerdialog_cancelbutton, (dialog, which) -> dialog.dismiss());

        AlertDialog extendDialog = builder.create();
        extendDialog.show();
        extendDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = ((EditText) dialogView.findViewById(R.id.extenddialog_username)).getText().toString();
            String password = ((EditText) dialogView.findViewById(R.id.extenddialog_password)).getText().toString();
            String repass = ((EditText) dialogView.findViewById(R.id.extenddialog_passwordcheck)).getText().toString();
            String email = ((EditText) dialogView.findViewById(R.id.extenddialog_email)).getText().toString();

            try {
                if("".equals(username)) throw new Exception("Gebruikersnaam is verplicht!");
                if("".equals(password)) throw new Exception("Wachtwoord is verplicht!");
                if(!password.equals(repass)) throw new Exception("Wachtwoorden moeten gelijk zijn!");

                extend(username, password, email, result -> {
                    extendDialog.dismiss();
                    callback.onAsyncActionComplete(result);
                });
            } catch (Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });

        /* Internetdingen */
        InternetConnectionManager.registerListener(extendInternetListenerName, hasInternetConnection -> extendDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(hasInternetConnection));
        extendDialog.setOnDismissListener(dialog -> InternetConnectionManager.unregisterListener(extendInternetListenerName));
    }

    private void extend(String username, String password, String email, AsyncActionCallback callback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_EXTEND)
                        .build());

        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("username", username));
                postParameters.add(new BasicNameValuePair("password", password));
                postParameters.add(new BasicNameValuePair("email", email));
                postParameters.add(new BasicNameValuePair("key", apiKey));
                UrlEncodedFormEntity data = new UrlEncodedFormEntity(postParameters);

                URL url = new URL(Constants.HTTP_BASE + "account/extend");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Fout bij upgraden. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Missende gegevens. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_CONFLICT:
                        throw new Exception("Deze gebruikersnaam is al in gebruik!");
                    case HttpURLConnection.HTTP_BAD_METHOD:
                        throw new Exception("Dit account is al opgewaardeerd! Log opnieuw in");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("appaccount", false).commit();
                isAppAccount = false;
                try {
                    callback.onAsyncActionComplete(data);
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij verwerken. Herstart de app en het werkt", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij verwerken: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region AccountInfo

    private static void getAccountInfo(AsyncActionCallback callback, Context context) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                URL url = new URL(Constants.HTTP_BASE + "account/accountinfo?key=" + getApiKey());

                return (HttpsURLConnection) url.openConnection();
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Missende gegevens");
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Deze gerbuiker bestaat niet");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        if("".equals(data)) throw new Exception("Onbekende fout");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    callback.onAsyncActionComplete(data);
                } catch (Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij ophalen accountinformatie", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij ophalen accountinformatie: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region GCM

    public void registerGCM() {
        registerGCM(null);
    }

    public void registerGCM(Activity activity) {
        if(activity == null) {
            if(!HelperFunctions.checkPlayServices(context)) return;
        } else {
            if (!HelperFunctions.checkPlayServicesWithError(activity)) return;
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                String regKey = gcm.register(Constants.PLAY_SERVICES_SENDER_ID);

                URL url = new URL(Constants.HTTP_BASE + "account/gcm");

                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("key", getApiKey()));
                postParameters.add(new BasicNameValuePair("GCM", regKey));
                UrlEncodedFormEntity data = new UrlEncodedFormEntity(postParameters);

                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch (status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new Exception("Foute gegevens. Log opnieuw in.");
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        throw new Exception("Interne fout. Probeer het later nogmaals.");
                    case HttpURLConnection.HTTP_NO_CONTENT:
                    case HttpURLConnection.HTTP_OK:
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                // success!
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij aanmelden pushnotificaties: "+ e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region Logout

    public void logout(AsyncActionCallback callback) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                URL url = new URL(Constants.HTTP_BASE + "account/logout");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                UrlEncodedFormEntity data = new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("key", getApiKey())));

                connection.setFixedLengthStreamingMode((int) data.getContentLength());
                connection.setDoOutput(true);

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                return data;
            }

            @Override
            public void onProcessData(String data) {
                try {
                    callback.onAsyncActionComplete(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                try {
                    callback.onAsyncActionComplete(null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion

    //region Subklassen
    //region get

    public void getSubklassen(boolean all, AsyncActionCallback callback) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                URL url = new URL(Constants.HTTP_BASE + "account/clusterklassen?key=" + apiKey + (all ? "&all" : ""));
                return (HttpURLConnection) url.openConnection();
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Foute gegevens. Log opnieuw in");
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new Exception("Gebruiker niet gevonden. Log opnieuw in");
                    case HttpURLConnection.HTTP_BAD_METHOD:
                        throw new Exception("Hiervoor moet je leerling zijn");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                        if("".equals(data)) throw new Exception("Onbekende fout");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    ArrayList<Subklas> subklassen = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(data);

                    for(int i = 0;i < jsonArray.length(); i++) {
                        JSONObject subklas = jsonArray.getJSONObject(i);
                        subklassen.add(new Subklas(subklas));
                    }

                    callback.onAsyncActionComplete(subklassen);
                } catch (Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij verwerken", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij verwerken: "+e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //region set

    public void setSubklassen(boolean refresh, ArrayList<String> subklassen, AsyncActionCallback callback) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpURLConnection onCreateConnection() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("key", apiKey));
                if(subklassen != null) {
                    for (int i = 0; i < subklassen.size(); i++) {
                        postParameters.add(new BasicNameValuePair("klassen[" + i + "]", subklassen.get(i)));
                    }
                }
                UrlEncodedFormEntity data = new UrlEncodedFormEntity(postParameters);

                URL url = new URL(Constants.HTTP_BASE + "account/clusterklassen" + (refresh ? "?refresh" : ""));
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Foute gegevens. Log opnieuw in");
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new Exception("Gebruiker niet gevonden. Log opnieuw in");
                    case HttpURLConnection.HTTP_BAD_METHOD:
                        throw new Exception("Hiervoor moet je leerling zijn");
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Deze leerling bestaat niet meer");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case HttpURLConnection.HTTP_OK:
                        if("".equals(data)) throw new Exception("Onbekende fout");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    callback.onAsyncActionComplete(data);
                } catch (Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij verwerken", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij verwerken: "+e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
            }
        };

        new WebActions(context).execute(webRequestCallbacks);
    }

    //endregion
    //endregion


    //region WebActions

    private static class WebActions extends AsyncTask<Account.WebRequestCallbacks, Exception, String> {
        private Context context;
        private WebRequestCallbacks callbacks;

        private WebActions(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(WebRequestCallbacks... params) {
            callbacks = params[0];
            if (!HelperFunctions.hasInternetConnection(context)) {
                publishProgress(new Exception("Geen internetverbinding"));
                return null;
            }
            try {
                HttpURLConnection connection = callbacks.onCreateConnection();

                String content = "";
                try {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    while (scanner.hasNext()) {
                        content += scanner.nextLine();
                    }
                } catch(Exception e) {
                    // No content
                    // continue normally
                } finally {
                    connection.disconnect();
                }

                return callbacks.onValidateResponse(content, connection.getResponseCode());
            } catch (Exception e) {
                publishProgress(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            callbacks.onProcessData(o);
        }

        @Override
        protected void onProgressUpdate(Exception... values) {
            Exception exception = values[0];
            callbacks.onError(exception);
            cancel(true);
        }
    }

    //endregion
    //region Types

    public static class Subklas {
        public String naam;
        public int jaarlaag;
        public String vak;
        public String leraar;
        public String nummer;
        public boolean isIn;

        public Subklas(JSONObject object) throws JSONException {
            naam = object.getString("klasnaam");
            vak = object.getString("vak");
            nummer = object.getString("cijfer");
            leraar = object.getString("leraar");
            jaarlaag = object.getInt("jaarlaag");
            isIn = object.getBoolean("isIn");
        }
    }

    //endregion
    //region Interfaces

    public interface WebRequestCallbacks {
        public HttpURLConnection onCreateConnection() throws Exception;
        public String onValidateResponse(String data, int status) throws Exception;
        public void onProcessData(String data);
        public void onError(Exception e);
    }

    //endregion

    public enum UserType {
        LEERLING, LERAAR, NO_ACCOUNT
    }
}
