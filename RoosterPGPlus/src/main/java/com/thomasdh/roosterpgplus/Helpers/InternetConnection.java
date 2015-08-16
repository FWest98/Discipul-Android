package com.thomasdh.roosterpgplus.Helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.thomasdh.roosterpgplus.Settings.Constants;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class InternetConnection {
    public static boolean isToastTriggered = false;

    public static void post(String url, RequestCallbacks callbacks, Context context) {
        new WebTask(context).execute(new InternetCallbacks() {
            @Override
            public HttpsURLConnection onAsynchronous(HttpsURLConnection connection) throws Exception {
                connection = (HttpsURLConnection) (new URL(url)).openConnection();
                connection.addRequestProperty("APIVersion", Constants.API_VERSION);

                UrlEncodedFormEntity data = callbacks.onDataNeeded();

                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode((int) data.getContentLength());

                data.writeTo(connection.getOutputStream());

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                return callbacks.onValidateResponse(data, status);
            }

            @Override
            public void onProcessData(String data) {
                callbacks.onProcessData(data);
            }

            @Override
            public void onError(Exception e) {
                callbacks.onError(e);
            }
        });
    }

    public static void get(String url, RequestCallbacks callbacks, Context context) {
        new WebTask(context).execute(new InternetCallbacks() {
            @Override
            public HttpsURLConnection onAsynchronous(HttpsURLConnection connection) throws Exception {
                connection = (HttpsURLConnection) (new URL(url)).openConnection();
                connection.addRequestProperty("APIVersion", Constants.API_VERSION);

                return connection;
            }

            @Override
            public String onValidateResponse(String data, int status) throws Exception {
                return callbacks.onValidateResponse(data, status);
            }

            @Override
            public void onProcessData(String data) {
                callbacks.onProcessData(data);
            }

            @Override
            public void onError(Exception e) {
                callbacks.onError(e);
            }
        });
    }

    private static class WebTask extends AsyncTask<InternetCallbacks, Exception, String> {
        private Context context;
        private InternetCallbacks callbacks;
        private boolean hasNewAPIVersion;

        private WebTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(InternetCallbacks... params) {
            callbacks = params[0];

            if(!HelperFunctions.hasInternetConnection(context)) {
                publishProgress(new Exception("Geen internetverbinding"));
                return null;
            }

            try {
                HttpsURLConnection connection = callbacks.onAsynchronous(null);

                String content = "";
                try {
                    Scanner scanner = new Scanner(connection.getInputStream());

                    while(scanner.hasNext()) {
                        content += scanner.nextLine();
                    }
                } catch(Exception e) {
                    // no content
                } finally {
                    connection.disconnect();
                }

                hasNewAPIVersion = connection.getHeaderField("CurrentAPIVersion") != null;

                return callbacks.onValidateResponse(content, connection.getResponseCode());
            } catch(Exception e) {
                publishProgress(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            if(hasNewAPIVersion && !isToastTriggered) {
                /* Show warning */
                Toast.makeText(context, "Er is een nieuwe versie van de app beschikbaar!", Toast.LENGTH_LONG).show();
                isToastTriggered = true;
            }

            callbacks.onProcessData(o);
        }

        @Override
        protected void onProgressUpdate(Exception... values) {
            Exception exception = values[0];
            callbacks.onError(exception);
            cancel(true);
        }
    }

    private interface InternetCallbacks {
        HttpsURLConnection onAsynchronous(HttpsURLConnection connection) throws Exception;
        String onValidateResponse(String data, int status) throws Exception;
        void onProcessData(String data);
        void onError(Exception e);
    }

    public interface RequestCallbacks {
        UrlEncodedFormEntity onDataNeeded() throws Exception;
        String onValidateResponse(String data, int status) throws Exception;
        void onProcessData(String data);
        void onError(Exception e);
    }
}