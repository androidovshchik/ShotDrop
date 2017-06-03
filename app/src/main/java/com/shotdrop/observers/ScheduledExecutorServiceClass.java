package com.shotdrop.observers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.shotdrop.utils.Prefs;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

// non multi-task observer
public class ScheduledExecutorServiceClass implements Runnable {

    private ScreenshotCallback callback;

    private File screenshotsFolder;

    private Prefs prefs;

    public ScheduledExecutorServiceClass(@NonNull String path, @NonNull Context context,
                                         @NonNull ScreenshotCallback callback) {
        screenshotsFolder = new File(path);
        prefs = new Prefs(context);
        this.callback = callback;
    }

    @Override
    public void run() {
        List<File> files = Arrays.asList(screenshotsFolder.listFiles());
        int count = files.size();
        Timber.d("ScheduledExecutorServiceClass: Screenshots count: " + count);
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
            // it's better to upload nothing in such case
            return;
        }
        if (files.get(count - 1).lastModified() > lastModifiedMemory) {
            callback.onScreenshotTaken(files.get(count - 1).getName(),
                    files.get(count - 1).lastModified());
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