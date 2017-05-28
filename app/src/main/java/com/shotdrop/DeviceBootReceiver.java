package com.shotdrop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceBootReceiver extends BroadcastReceiver {

    public static final String ACTION1 = Intent.ACTION_BOOT_COMPLETED;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()) {
            case ACTION1:
                break;
            default:
                break;
        }
    }
}
