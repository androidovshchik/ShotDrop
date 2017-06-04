package com.shotdrop.observers;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.widget.Toast;

import com.shotdrop.ServiceMain;
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
    @SuppressWarnings("deprecation")
    public void onNotificationPosted(StatusBarNotification notification) {
        if (!ServiceMain.isRunning(getApplicationContext())) {
            return;
        }
        if (prefs == null) {
            prefs = new Prefs(getApplicationContext());
        }
        if (prefs.getBoolean(Prefs.HIDE_SYSTEM_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cancelNotification(notification.getKey());
            } else {
                cancelNotification(notification.getPackageName(), notification.getTag(),
                        notification.getId());
            }
        }
        logNotification(notification);
        if (prefs.getBoolean(Prefs.DEBUG_MODE)) {
            Toast toast = Toast.makeText(getApplicationContext(), notification.getPackageName(),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
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
        Intent intent = new Intent(ServiceMain.NOTIFICATION_ACTION_SCREENSHOT);
        intent.putExtra(ServiceMain.KEY_FILENAME, files.get(count - 1).getName());
        intent.putExtra(ServiceMain.KEY_NOTIFICATION_ID, 0);
        sendBroadcast(intent);
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
