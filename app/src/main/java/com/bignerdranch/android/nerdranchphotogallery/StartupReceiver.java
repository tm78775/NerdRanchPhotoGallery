package com.bignerdranch.android.nerdranchphotogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by TMiller on 8/26/2016.
 */
public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received ON_BOOT broadcast intent.");

        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);

        Toast.makeText(context, "onReceive called!", Toast.LENGTH_SHORT).show();
    }

}
