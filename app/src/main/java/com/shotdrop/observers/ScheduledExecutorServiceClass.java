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
        Timber.d("ScheduledExecutorServiceClass: Screenshots count: " + files.size());
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        for (File file : files) {
            Timber.d("LastModified: " + file.lastModified());
        }
        /*if (lastFilename == null || lastModifiedTime == null) {
        } else {
        }*/
    }
}