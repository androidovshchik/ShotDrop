package com.shotdrop.observers;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import timber.log.Timber;

public class NotificationListener extends NotificationListenerService {

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        switch (notification.getPackageName()) {
            default:
                Timber.d(notification.getPackageName());
                break;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {}
}
