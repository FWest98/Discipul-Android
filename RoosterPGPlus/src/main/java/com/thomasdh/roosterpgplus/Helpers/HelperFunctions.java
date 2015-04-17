package com.thomasdh.roosterpgplus.Helpers;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.thomasdh.roosterpgplus.Settings.Constants;


public class HelperFunctions {
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static boolean showCaseView() {
        return false; // TODO Fix Showcaseview
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

            } else {
                Log.i("PlayServices", "No pushmessages");
            }
            return false;
        }
        return true;
    }

    public static boolean checkPlayServicesWithError(Activity activity) {
        if(checkPlayServices(activity)) {
            return true;
        } else {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
            GooglePlayServicesUtil.getErrorDialog(resultCode, activity, Constants.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            return false;
        }
    }
}
