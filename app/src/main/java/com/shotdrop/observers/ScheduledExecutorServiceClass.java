package com.shotdrop.observers;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

public class ScheduledExecutorServiceClass implements Runnable {

    private ScreenshotCallback callback;

    private String lastFilename = null;
    private Long lastModifiedTime = null;

    private File screenshotsFolder;

    public ScheduledExecutorServiceClass(@NonNull String path,
                                         @NonNull ScreenshotCallback callback) {
        screenshotsFolder = new File(path);
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
        if (lastFilename == null || lastModifiedTime == null) {
            lastFilename = files.get(count - 1).getName();
            lastModifiedTime = files.get(count - 1).lastModified();
            callback.onScreenshotTaken(lastFilename);
        } else {
            for (int i = 0; i < count; i++) {
                if (files.get(i).lastModified() > lastModifiedTime) {
                    lastFilename = files.get(i).getName();
                    lastModifiedTime = files.get(i).lastModified();
                    callback.onScreenshotTaken(lastFilename);
                }
            }
        }
    }
}