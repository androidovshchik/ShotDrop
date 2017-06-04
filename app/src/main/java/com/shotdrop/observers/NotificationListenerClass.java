package com.shotdrop.observers;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.shotdrop.utils.Prefs;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

public class NotificationListenerClass extends NotificationListenerService {

    private ScreenshotCallback callback;

    private File screenshotsFolder;

    private Prefs prefs;

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

    public void run() {
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
        long lastModifiedMemory = lastModifiedFromMemory();
        if (lastModifiedMemory == 0) {
            lastModifiedMemory = files.get(count - 1).lastModified();
            prefs.putString(Prefs.LAST_SCREENSHOT_MODIFIED, lastModifiedMemory);
        }
        if (files.get(count - 1).lastModified() > lastModifiedMemory) {
            prefs.putString(Prefs.LAST_SCREENSHOT_MODIFIED,
                    files.get(count - 1).lastModified());
            callback.onScreenshotTaken(files.get(count - 1).getName());
        }
    }

    private long lastModifiedFromMemory() {
        try {
            return Long.parseLong(prefs.getString(Prefs.LAST_SCREENSHOT_MODIFIED, "0"));
        } catch (NumberFormatException e) {
            Timber.e(e.getLocalizedMessage());
            return 0;
        }
    }
}
