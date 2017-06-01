package com.shotdrop.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shotdrop.ServiceMain;
import com.shotdrop.utils.Prefs;

public class DeviceBootReceiver extends BroadcastReceiver {

    public static final String ACTION = Intent.ACTION_BOOT_COMPLETED;

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = new Prefs(context);
        switch(intent.getAction()) {
            case ACTION:
                boolean isServiceRunning = ServiceMain.isRunning(context);
                if (prefs.getBoolean(Prefs.ENABLE_APPLICATION)) {
                    if (!isServiceRunning) {
                        context.startService(ServiceMain.getStartIntent(context));
                    }
                } else {
                    if (isServiceRunning) {
                        context.stopService(ServiceMain.getStartIntent(context));
                    }
                }
                break;
            default:
                break;
        }
    }
}
