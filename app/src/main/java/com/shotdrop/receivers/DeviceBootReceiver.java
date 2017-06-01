package com.shotdrop.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceBootReceiver extends BroadcastReceiver {

    public static final String ACTION = Intent.ACTION_BOOT_COMPLETED;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()) {
            case ACTION:
                break;
            default:
                break;
        }
    }
}
