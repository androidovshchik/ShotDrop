package com.shotdrop.observers;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.Prefs;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

public class NotificationListenerClass extends NotificationListenerService {

    public static final String COM_ANDROID_SYSTEMUI = "com.android.systemui";

    private File screenshotsFolder;

    private Prefs prefs;

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        //if (!ServiceMain.isRunning(getApplicationContext())) {
        //    return;
        //}
        logNotification(notification);
        switch (notification.getPackageName()) {
            case COM_ANDROID_SYSTEMUI:
                if (notification.getNotification().contentIntent != null) {
                    onScreenshotReady();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {}

    public void onScreenshotReady() {
        if (prefs == null) {
            prefs = new Prefs(getApplicationContext());
        }
        if (screenshotsFolder == null) {
            screenshotsFolder = new File(prefs.getScreenshotsPath());
        }
        List<File> files = Arrays.asList(screenshotsFolder.listFiles());
        int count = files.size();
        if (count <= 0) {
            return;
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        //callback.onScreenshotTaken(files.get(count - 1).getName());
        //files.get(count - 1).lastModified()
    }

    private void logNotification(StatusBarNotification notification) {
        String classname = getClass().getSimpleName();
        LogUtil.logDivider(classname, ":");
        LogUtil.logCentered(" ", classname, "New notification");
        LogUtil.logCentered(":", classname, "packageName: " + notification.getPackageName());
        LogUtil.logCentered(":", classname, "id: " + notification.getId());
        if (notification.getNotification().actions != null) {
            LogUtil.logCentered(" ", classname, "Notification actions");
            for (Notification.Action action : notification.getNotification().actions) {
                LogUtil.logCentered(":", classname, "action.title: " + action.title);
            }
        }
        if (notification.getNotification().extras != null) {
            LogUtil.logCentered(" ", classname, "Notification extras");
            for (String key : notification.getNotification().extras.keySet()) {
                Timber.d(key + ": " + notification.getNotification().extras.get(key));
            }
        }
        LogUtil.logDivider(classname, ":");
    }
}
