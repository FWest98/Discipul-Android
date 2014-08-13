package com.thomasdh.roosterpgplus.Data;

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

import com.thomasdh.roosterpgplus.Helpers.AsyncActionCallback;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.InternetConnectionManager;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings;
import com.thomasdh.roosterpgplus.util.ExceptionHandler;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import lombok.Getter;

public class Account {
    @Getter private static boolean isSet;
    @Getter private static String name;
    @Getter private static String apiKey;
    @Getter private static boolean isAppAccount;
    @Getter private static UserType userType;

    @Getter private static boolean isVertegenwoordiger;
    @Getter private static String leerlingKlas;

    @Getter private static String leraarCode;

    private Context context;
    private WebRequestCallbacks callbacks;

    private static Account instance;

    private static final String loginInternetListenerName = "loginDialog";
    private static final String registerInternetListenerName = "registerDialog";

    //region Initialize

    public static void initialize(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String key;
        if((key = pref.getString("key", null)) == null) {
            isSet = false;
            userType = UserType.NO_ACCOUNT;
        } else {
            isSet = true;
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

    private void processJSON(String JSON, boolean isAppAccount) throws JSONException {
        JSONObject base = new JSONObject(JSON);
        SharedPreferences.Editor pref = PreferenceManager.getDefaultSharedPreferences(context).edit();

        apiKey = base.getString("key");
        pref.putString("key", apiKey);

        name = base.getString("naam");
        pref.putString("naam", name);

        this.isAppAccount = isAppAccount;
        pref.putBoolean("appaccount", isAppAccount);

        if(base.has("code")) {
            // LERAAR
            userType = UserType.LERAAR;

            leraarCode = base.getString("code");
            pref.putString("code", leraarCode);
        } else {
            // LEERLING
            userType = UserType.LEERLING;

            leerlingKlas = base.getString("klas");
            pref.putString("klas", leerlingKlas);

            isVertegenwoordiger = base.getBoolean("vertegenwoordiger");
            pref.putBoolean("isVertegenwoordiger", isVertegenwoordiger);
        }

        pref.commit();

        ExceptionHandler.handleException(new Exception("Welkom, "+getName()), context, ExceptionHandler.HandleType.SIMPLE);
    }

    //endregion
    //region Constructors

    private Account(Context context) {
        initialize(context);
        this.context = context;
    }

    public static Account getInstance(Context context) {
        if(instance == null) {
            instance = new Account(context);
        }
        return instance;
    }

    //endregion
    //region Login

    public void login() {
        login(result -> {});
    }

    public void login(AsyncActionCallback callback) {
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
        loginDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> register(result -> {loginDialog.dismiss(); callback.onAsyncActionComplete(result); }));
        loginDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> loginDialog.dismiss());
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

                        login(username, password, result -> {
                            loginDialog.dismiss();
                            callback.onAsyncActionComplete(result);
                        });

                        break;
                    case 1:
                        if ("".equals(llnrString)) throw new Exception("Leerlingnummer is verplicht!");

                        login(llnrString, false, result -> {
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

    private void login(String username, String password, AsyncActionCallback callback) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpResponse onRequestCreate(HttpClient client) throws Exception {
                HttpPost httpPost = new HttpPost(Settings.API_Base_URL + "account/login");

                List<NameValuePair> postParamaters = new ArrayList<>();
                postParamaters.add(new BasicNameValuePair("username", username));
                postParamaters.add(new BasicNameValuePair("password", password));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParamaters);
                httpPost.setEntity(entity);

                return client.execute(httpPost);
            }

            @Override
            public Object onRequestComplete(String data, int status) throws Exception {
                switch(status) {
                    case 400:
                        throw new Exception("Missende gegevens");
                    case 401:
                        throw new Exception("Ongeldige logingegevens");
                    case 500:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case 200:
                        if("".equals(data)) throw new Exception("Onbekende fout. Probeer het opnieuw");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onDataHandle(Object data) {
                try {
                    String JSON = (String) data;
                    processJSON(JSON, true);
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

        new WebActions().execute(webRequestCallbacks);
    }

    private void login(String llnr, boolean force, AsyncActionCallback callback) {
        WebRequestCallbacks webRequestCallbacks = new WebRequestCallbacks() {
            @Override
            public HttpResponse onRequestCreate(HttpClient client) throws Exception {
                String url = Settings.API_Base_URL + "account/login";
                if(force) {
                    url += "?force";
                }
                HttpPost httpPost = new HttpPost(url);

                List<NameValuePair> postParameters = new ArrayList<>();
                postParameters.add(new BasicNameValuePair("llnr", llnr));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
                httpPost.setEntity(entity);

                return client.execute(httpPost);
            }

            @Override
            public Object onRequestComplete(String data, int status) throws Exception {
                switch(status) {
                    case 400:
                        throw new Exception("Missende gegevens");
                    case 204:
                        throw new IllegalArgumentException("Deze gebruiker bestaat al"); // TODO doe er wat mee
                    case 500:
                        Log.e("AccountWebRequest", data);
                        throw new Exception("Serverfout");
                    case 200:
                        if("".equals(data)) throw new Exception("Onbekende fout. Probeer het opnieuw");
                        return data;
                    default:
                        throw new Exception("Onbekende fout");
                }
            }

            @Override
            public void onDataHandle(Object data) {
                try {
                    String JSON = (String) data;
                    processJSON(JSON, false);
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
                    builder.setTitle(context.getResources().getString(R.string.logindialog_warning_title));
                    builder.setMessage(context.getResources().getString(R.string.logindialog_warning_text));
                    builder.setPositiveButton(context.getResources().getString(R.string.logindialog_warning_submitButton), (dialog, which) -> {
                        login(llnr, true, callback);
                    });
                    builder.setNegativeButton(context.getResources().getString(R.string.logindialog_warning_cancelButton), (dialog, which) -> {});

                    builder.show();
                } catch(Exception e) {
                    ExceptionHandler.handleException(new Exception("Fout bij het inloggen:" + e.getMessage(), e), context, ExceptionHandler.HandleType.SIMPLE);
                }
            }
        };

        new WebActions().execute(webRequestCallbacks);
    }

    //endregion
    //region Register

    public void register() {
        register(result -> {});
    }

    public void register(AsyncActionCallback callback) {

    }

    //endregion
    //region WebActions

    private class WebActions extends AsyncTask<Account.WebRequestCallbacks, Exception, Object> {
        @Override
        protected Object doInBackground(WebRequestCallbacks... params) {
            if (!HelperFunctions.hasInternetConnection(context)) {
                publishProgress(new Exception("Geen internetverbinding"));
                return null;
            }
            callbacks = params[0];
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse response = callbacks.onRequestCreate(httpClient);

                String content = "";
                Scanner scanner = new Scanner(response.getEntity().getContent());
                while (scanner.hasNext()) {
                    content += scanner.nextLine();
                }

                Object data = callbacks.onRequestComplete(content, response.getStatusLine().getStatusCode());
                return data;
            } catch (Exception e) {
                publishProgress(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            callbacks.onDataHandle(o);
        }

        @Override
        protected void onProgressUpdate(Exception... values) {
            Exception exception = values[0];
            callbacks.onError(exception);
            cancel(true);
        }
    }

    //endregion
    //region Interfaces

    public interface WebRequestCallbacks {
        public HttpResponse onRequestCreate(HttpClient client) throws Exception;
        public Object onRequestComplete(String data, int status) throws Exception;
        public void onDataHandle(Object data);
        public void onError(Exception e);
    }

    //endregion

    public enum UserType {
        LEERLING, LERAAR, NO_ACCOUNT
    }
}
