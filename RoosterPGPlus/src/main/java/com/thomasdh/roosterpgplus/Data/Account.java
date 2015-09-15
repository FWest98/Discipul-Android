package com.thomasdh.roosterpgplus.Data;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.thomasdh.roosterpgplus.CustomUI.ProgressDialog;
import com.thomasdh.roosterpgplus.Helpers.Apache.BasicNameValuePair;
import com.thomasdh.roosterpgplus.Helpers.Apache.NameValuePair;
import com.thomasdh.roosterpgplus.Helpers.Apache.UrlEncodedFormEntity;
import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.ExceptionHandler;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.InternetConnection;
import com.thomasdh.roosterpgplus.Helpers.InternetConnectionManager;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.Notifications.NextUurNotifications;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@SuppressWarnings("UnusedDeclaration")
public class Account {
    @Getter private static int userID = 0;
    @Getter private static boolean isSet;
    @Getter private static boolean isHandlingNewVersion;
    @Getter private static String username;
    @Getter private static String name;
    @Getter private static String apiKey;
    @Getter private static boolean isAppAccount;
    @Getter private static UserType userType;

    @Getter private static boolean isVertegenwoordiger;
    @Getter private static String leerlingKlas;

    @Getter private static String leraarCode;

    private Context context;
    private static int currentVersion = 0;
    private static boolean isInitialized = false;

    private static Account instance;

    private static final String loginInternetListenerName = "loginDialog";
    private static final String registerInternetListenerName = "registerDialog";
    private static final String extendInternetListenerName = "extendDialog";
    private static final String changeUsernameInternetListenerName = "changeUsernameDialog";
    private static final String changePasswordInternetListenerName = "changePasswordDialog";

    //region Initialize

    public static void initialize(Context context) {
        initialize(context, true);
    }
    public static void initialize(Context context, boolean showUI) { initialize(context, showUI, false); }
    public static void initialize(Context context, boolean showUI, boolean force) {
        if(isInitialized && !force) return;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        currentVersion = 0;
        int oldVersion;

        try {
            currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Account", e.getMessage(), e);
        }

        /*
        NIEUWE BREAKING RELEASES:
        - AndroidManifest versiecode ophogen
        - Hieronder nieuwe versie definiëren
        - In arrays.xml de versie definiëren als breaking(!!!!!)
         */

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
                                        isInitialized = true;
                                    })
                                    .setNegativeButton("Negeren", (dialog, which) -> {
                                        Account.getInstance(context, true).registerGCM();

                                        pref.edit()
                                                .putInt("oldVersion", currentVersion)
                                                .putBoolean("pushNotificaties", false)
                                                .commit();
                                        isHandlingNewVersion = false;
                                        isInitialized = true;
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
                                isInitialized = true;
                            }, context);
                        }
                        case 19: {
                            // GCMID wel verzenden, maar niet lokaal inschakelen
                            isHandlingNewVersion = true;
                            if(HelperFunctions.hasInternetConnection(context)) {
                                Account.getInstance(context, true).registerGCM();
                                pref.edit().putInt("oldVersion", currentVersion).commit();
                                isHandlingNewVersion = false;
                                isInitialized = true;
                            }
                        }
                        case 21: {
                            // Nieuw jaar, herlaad informatie over leerling
                            if((apiKey = pref.getString("key", null)) == null) break;

                            isHandlingNewVersion = true;
                            if(HelperFunctions.hasInternetConnection(context)) {
                                getAccountInfo(s -> {
                                    JSONObject base = new JSONObject((String) s);

                                    name = base.getString("naam");
                                    if(base.has("code")) {
                                        leraarCode = base.getString("code");
                                        pref.edit().putInt("oldVersion", currentVersion).putString("naam", name).putString("code", leraarCode).commit();
                                        isHandlingNewVersion = false;
                                        isInitialized = true;
                                    } else {
                                        leerlingKlas = base.getJSONObject("klas").getString("klasnaam");
                                        pref.edit().putInt("oldVersion", currentVersion).putString("naam", name).putString("klas", leerlingKlas).commit();
                                        isHandlingNewVersion = false;
                                        isInitialized = true;
                                    }

                                }, context);
                            }
                        }
                        case 25: {
                            // Username laden
                            if((apiKey = pref.getString("key", null)) == null) break;

                            isHandlingNewVersion = true;
                            if(HelperFunctions.hasInternetConnection(context)) {
                                getAccountInfo(s -> {
                                    JSONObject base = new JSONObject((String) s);

                                    username = base.getString("username");
                                    pref.edit().putString("username", username).putInt("oldVersion", currentVersion).commit();
                                    isHandlingNewVersion = false;
                                    isInitialized = true;
                                }, context);
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
            username = pref.getString("username", null);
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
        if(!isHandlingNewVersion) isInitialized = true;
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

            // remove klas
            leerlingKlas = null;
            isVertegenwoordiger = false;
            pref.remove("klas");
            pref.remove("isVertegenwoordiger");
        } else {
            // LEERLING
            userType = UserType.LEERLING;

            leerlingKlas = base.getJSONObject("klas").getString("klasnaam");
            pref.putString("klas", leerlingKlas);

            isVertegenwoordiger = base.getBoolean("vertegenwoordiger");
            pref.putBoolean("isVertegenwoordiger", isVertegenwoordiger);

            pref.remove("code");
            leraarCode = null;
        }

        pref.apply();

        ExceptionHandler.handleException(new Exception("Welkom, " + getName()), context, ExceptionHandler.HandleType.SIMPLE);

        NextUurNotifications.disableNotifications(context);
        new NextUurNotifications(context);

        registerGCM();

        getAccountInfo(p -> {
            JSONObject json = new JSONObject((String) p);

            userID = json.getInt("id");
            username = json.getString("username");
            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("userid", userID).putString("username", username).apply();

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

    //region Manage

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
        View dialogView = inflater.inflate(R.layout.dialog_login, null);

        /* Tabs */
        TabLayout tabLayout = (TabLayout) dialogView.findViewById(R.id.tabs);
        FrameLayout tabs = (FrameLayout) dialogView.findViewById(R.id.tab_wrapper);
        final TabLayout.Tab[] currentTab = { tabLayout.newTab().setText(R.string.logindialog_tabs_userpass).setTag(R.id.Tab_UserPass) };

        tabLayout.addTab(currentTab[0], true);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.logindialog_tabs_llnr).setTag(R.id.Tab_LLNR), false);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getTag() == null || !(tab.getTag() instanceof Integer)) return;

                for (int i = 0; i < tabs.getChildCount(); i++) {
                    View child = tabs.getChildAt(i);
                    child.setVisibility(View.GONE);
                }

                dialogView.findViewById((int) tab.getTag()).setVisibility(View.VISIBLE);
                currentTab[0] = tab;
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        /* Dialog buttons */
        builder.setView(dialogView)
                .setPositiveButton(R.string.logindialog_loginbutton, (dialogInterface, id) -> {})
                /*.setNeutralButton(R.string.logindialog_registerbutton, (dialogInterface, id) -> {})*/
                .setNegativeButton(R.string.logindialog_cancelbutton, (dialogInterface, id) -> {
                });

        AlertDialog loginDialog = builder.create();
        loginDialog.show();
        //loginDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> register(activity, result -> { loginDialog.dismiss(); callback.onAsyncActionComplete(result); }));
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
                AlertDialog progressDialog = ProgressDialog.create("Inloggen", "Even geduld...", context);
                switch ((int) currentTab[0].getTag()) {
                    case R.id.Tab_UserPass:
                        if ("".equals(username)) throw new Exception("Gebruikersnaam is verplicht!");
                        if ("".equals(password)) throw new Exception("Wachtwoord is verplicht!");

                        // Show progressdialog
                        progressDialog.show();

                        login(activity, username, password, result -> {
                            loginDialog.dismiss();
                            progressDialog.dismiss();
                            callback.onAsyncActionComplete(result);
                        }, error -> {
                            progressDialog.dismiss();
                        });

                        break;
                    case R.id.Tab_LLNR:
                        if ("".equals(llnrString)) throw new Exception("Leerlingnummer is verplicht!");

                        // Show progressdialog
                        progressDialog.show();

                        login(activity, llnrString, false, result -> {
                            loginDialog.dismiss();
                            progressDialog.dismiss();
                            callback.onAsyncActionComplete(result);
                        }, error -> {
                            progressDialog.dismiss();
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

    private void login(Activity activity, String username, String password, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_LOGIN)
                        .build());

        String url = Constants.HTTP_BASE + "account/login";

        InternetConnection.RequestCallbacks requestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParamaters = new ArrayList<>();
                postParamaters.add(new BasicNameValuePair("username", username));
                postParamaters.add(new BasicNameValuePair("password", password));

                return new UrlEncodedFormEntity(postParamaters);
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
                    try {
                        errorCallback.onAsyncActionComplete(e);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij het inloggen: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                try {
                    errorCallback.onAsyncActionComplete(e);
                } catch (Exception e1) {

                }
            }
        };

        InternetConnection.post(url, requestCallbacks, context);
    }

    private void login(Activity activity, String llnr, boolean force, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_LOGIN)
                        .build());

        String url = Constants.HTTP_BASE + "account/login" + (force ? "?force" : "");

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("llnr", llnr));

                return new UrlEncodedFormEntity(postParameters);
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
                    try {
                        errorCallback.onAsyncActionComplete(e);
                    } catch (Exception e1) {}
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
                    builder.setPositiveButton(R.string.logindialog_warning_submitButton, (dialog, which) -> login(activity, llnr, true, callback, errorCallback));
                    builder.setNegativeButton(R.string.logindialog_warning_cancelButton, (dialog, which) -> {});

                    builder.show();
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij het inloggen:" + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                    try {
                        errorCallback.onAsyncActionComplete(e);
                    } catch (Exception e1) {}
                }
            }
        };

        InternetConnection.post(url, webRequestCallbacks, context);
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

        String url = Constants.HTTP_BASE + "account/register" + (force ? "?force" : "");

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("username", username));
                postParameters.add(new BasicNameValuePair("password", password));
                postParameters.add(new BasicNameValuePair("llnr", llnr));
                postParameters.add(new BasicNameValuePair("email", email));

                return new UrlEncodedFormEntity(postParameters);
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

        InternetConnection.post(url, webRequestCallbacks, context);
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
        View dialogView = inflater.inflate(R.layout.dialog_extend, null);

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
            AlertDialog progressDialog = ProgressDialog.create("Registreren", "Even geduld...", context);

            try {
                if("".equals(username)) throw new Exception("Gebruikersnaam is verplicht!");
                if("".equals(password)) throw new Exception("Wachtwoord is verplicht!");
                if(!password.equals(repass)) throw new Exception("Wachtwoorden moeten gelijk zijn!");

                progressDialog.show();

                extend(username, password, email, result -> {
                    extendDialog.dismiss();
                    progressDialog.dismiss();
                    callback.onAsyncActionComplete(result);
                }, error -> { progressDialog.dismiss(); });
            } catch (Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });

        /* Internetdingen */
        InternetConnectionManager.registerListener(extendInternetListenerName, hasInternetConnection -> extendDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(hasInternetConnection));
        extendDialog.setOnDismissListener(dialog -> InternetConnectionManager.unregisterListener(extendInternetListenerName));
    }

    private void extend(String username, String password, String email, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_EXTEND)
                        .build());

        String url = Constants.HTTP_BASE + "account/extend";

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("username", username));
                postParameters.add(new BasicNameValuePair("password", password));
                postParameters.add(new BasicNameValuePair("email", email));
                postParameters.add(new BasicNameValuePair("key", apiKey));

                return new UrlEncodedFormEntity(postParameters);
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
                try {
                    errorCallback.onAsyncActionComplete(e);
                } catch (Exception e1) {}
            }
        };

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion
    //region AccountInfo

    private static void getAccountInfo(AsyncActionCallback callback, Context context) {
        String url = Constants.HTTP_BASE + "account/accountinfo?key=" + getApiKey();

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                return null;
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

        InternetConnection.get(url, webRequestCallbacks, context);
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
        String url = Constants.HTTP_BASE + "account/gcm";

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                String regKey = gcm.register(Constants.PLAY_SERVICES_SENDER_ID);

                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("key", getApiKey()));
                postParameters.add(new BasicNameValuePair("GCM", regKey));

                return new UrlEncodedFormEntity(postParameters);
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

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion
    //region Logout

    public void logout(AsyncActionCallback callback) {
        String url = Constants.HTTP_BASE + "account/logout";

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                return new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("key", getApiKey())));
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

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion

    //region Change Username

    public void changeUsername() { extend(s -> {}); }

    public void changeUsername(AsyncActionCallback callback) {
        if(isAppAccount()) {
            ExceptionHandler.handleException(new Exception("Je moet eerst een account aanmaken!"), context, ExceptionHandler.HandleType.SIMPLE);
            return;
        }

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_CHANGEUSERNAME);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_username, null);

        ((TextView) dialogView.findViewById(R.id.dialog_username_text)).setText(context.getString(R.string.dialog_changeUsername_desc) + getUsername());

        builder.setTitle(context.getString(R.string.dialog_changeUsername_title))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.dialog_changeUsername_button_change), (dialog, which) -> {})
                .setNegativeButton(context.getString(R.string.dialog_changeUsername_button_cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog changeUsernameDialog = builder.create();
        changeUsernameDialog.show();
        changeUsernameDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            TextInputLayout usernameEditor = (TextInputLayout) dialogView.findViewById(R.id.dialog_username_inputLayout);
            String newUsername = usernameEditor.getEditText().getText().toString();

            AlertDialog progressDialog = ProgressDialog.create("Gebruikersnaam wijzigen", "Even geduld...", context);
            try {
                if ("".equals(newUsername)) throw new Exception("Dit veld is verplicht!");
                if (newUsername.equals(getUsername())) throw new Exception("Nieuwe gebruikersnaam mag niet gelijk zijn aan de oude!");

                progressDialog.show();

                changeUsername(newUsername, result -> {
                    changeUsernameDialog.dismiss();
                    progressDialog.dismiss();
                    callback.onAsyncActionComplete(result);
                }, e -> progressDialog.dismiss());

            } catch (Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });

        /* Internetdingen */
        InternetConnectionManager.registerListener(changeUsernameInternetListenerName, hasInternetConnection -> changeUsernameDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(hasInternetConnection));
        changeUsernameDialog.setOnDismissListener(dialog -> InternetConnectionManager.unregisterListener(changeUsernameInternetListenerName));
    }

    private void changeUsername(String newUsername, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_CHANGEUSERNAME)
                        .build());

        String url = Constants.HTTP_BASE + "account/changeUsername";

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("newUsername", newUsername));
                postParameters.add(new BasicNameValuePair("key", getApiKey()));

                return new UrlEncodedFormEntity(postParameters);
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch (status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Fout bij wijzigen. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new Exception("Kon account niet (meer) vinden. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        return data;
                    default:
                        throw new Exception("Onbekende fout: " + status);
                }
            }

            @Override
            public void onProcessData(String data) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("username", newUsername).commit();
                username = newUsername;
                try {
                    callback.onAsyncActionComplete(data);
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij verwerken. Herstart de app en kijk of alles goed werkt", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij verwerken: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                try {
                    errorCallback.onAsyncActionComplete(e);
                } catch (Exception e1) {}
            }
        };

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion
    //region Change Password

    public void changePassword() { changePassword(s -> {}); }
    public void changePassword(AsyncActionCallback callback) {
        if(isAppAccount()) {
            ExceptionHandler.handleException(new Exception("Je moet eerst een account aanmaken!"), context, ExceptionHandler.HandleType.SIMPLE);
            return;
        }

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context.getApplicationContext());
        tracker.setScreenName(Constants.ANALYTICS_FRAGMENT_CHANGEPASSWORD);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);

        builder.setTitle("Wachtwoord wijzigen")
                .setView(dialogView)
                .setPositiveButton("Wijzig", (dialog, which) -> {})
                .setNegativeButton("Annuleer", (dialog, which) -> dialog.dismiss());

        AlertDialog changePasswordDialog = builder.create();
        changePasswordDialog.show();
        changePasswordDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPassword = ((AppCompatEditText) dialogView.findViewById(R.id.dialog_password_oldPassword)).getText().toString();
            String newPassword = ((AppCompatEditText) dialogView.findViewById(R.id.dialog_password_newPassword)).getText().toString();
            String confirmPass = ((AppCompatEditText) dialogView.findViewById(R.id.dialog_password_confirmPassword)).getText().toString();
            AlertDialog progressDialog = ProgressDialog.create("Wachtwoord wijzigen", "Even geduld...", context);

            try {
                if ("".equals(oldPassword)) throw new Exception("Oud wachtwoord is verplicht!");
                if ("".equals(newPassword)) throw new Exception("Nieuw wachtwoord is verplicht!");
                if (!newPassword.equals(confirmPass))
                    throw new Exception("Nieuwe wachtwoorden moeten gelijk zijn!");

                progressDialog.show();

                changePassword(oldPassword, newPassword, result -> {
                    changePasswordDialog.dismiss();
                    progressDialog.dismiss();
                    callback.onAsyncActionComplete(result);
                }, e -> progressDialog.dismiss());

            } catch (Exception e) {
                ExceptionHandler.handleException(e, context, ExceptionHandler.HandleType.SIMPLE);
            }
        });
    }

    public void changePassword(String oldPassword, String newPassword, AsyncActionCallback callback, AsyncActionCallback errorCallback) {
        MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, context)
                .send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.ANALYTICS_CATEGORIES_SETTINGS)
                        .setAction(Constants.ANALYTICS_ACTION_CHANGEPASS)
                        .build());

        String url = Constants.HTTP_BASE + "account/changePassword";

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParamaters = new ArrayList<>();
                postParamaters.add(new BasicNameValuePair("oldPassword", oldPassword));
                postParamaters.add(new BasicNameValuePair("newPassword", newPassword));
                postParamaters.add(new BasicNameValuePair("key", getApiKey()));

                return new UrlEncodedFormEntity(postParamaters);
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                switch(status) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new Exception("Fout bij wijzigen. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new Exception("Kon account niet (meer) vinden. Log opnieuw in en probeer het opnieuw");
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new Exception("Fout wachtwoord. Probeer het opnieuw");
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        return data;
                    default:
                        throw new Exception("Onbekende fout: " + status);
                }
            }

            @Override
            public void onProcessData(String data) {
                try {
                    callback.onAsyncActionComplete(data);
                } catch (Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij verwerken. Herstarty de app en kijk of alles goed werkt", e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }

            @Override
            public void onError(Exception e) {
                ExceptionHandler.handleException(new Exception("Fout bij wijzigen: " + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                try {
                    errorCallback.onAsyncActionComplete(e);
                } catch(Exception ex) {}
            }
        };

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion

    //region Clusterklassen
    //region get

    public void getClusterklassen(boolean all, AsyncActionCallback callback) {
        String url = Constants.HTTP_BASE + "account/clusterklassen?key=" + apiKey + (all ? "&all" : "");

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                return null;
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

        InternetConnection.get(url, webRequestCallbacks, context);
    }

    //endregion
    //region set

    public void setClusterklassen(boolean refresh, ArrayList<String> subklassen, AsyncActionCallback callback) {
        String url = Constants.HTTP_BASE + "account/clusterklassen" + (refresh ? "?refresh" : "");

        InternetConnection.RequestCallbacks webRequestCallbacks = new InternetConnection.RequestCallbacks() {
            @Override
            public UrlEncodedFormEntity onDataNeeded() throws Exception {
                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("key", apiKey));
                if(subklassen != null) {
                    for (int i = 0; i < subklassen.size(); i++) {
                        postParameters.add(new BasicNameValuePair("klassen[" + i + "]", subklassen.get(i)));
                    }
                }

                return new UrlEncodedFormEntity(postParameters);
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

        InternetConnection.post(url, webRequestCallbacks, context);
    }

    //endregion
    //endregion

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

    public enum UserType {
        LEERLING, LERAAR, NO_ACCOUNT
    }
}
