package com.shotdrop.observers;

import android.os.FileObserver;
import android.support.annotation.NonNull;

import timber.log.Timber;

public final class FileObserverClass extends FileObserver {

    private ScreenshotCallback callback;

    private String lastFilename = null;

    public FileObserverClass(@NonNull String path, @NonNull ScreenshotCallback callback) {
        super(path, android.os.FileObserver.CLOSE_WRITE);
        this.callback = callback;
    }

    @Override
    public void onEvent(int event, String filename) {
        if (filename != null && (lastFilename == null || !filename.equals(lastFilename))) {
            Timber.d("FileObserverClass: Filename: %s", filename);
            lastFilename = filename;
            callback.onScreenshotTaken(lastFilename);
        }
    }

    public void start() {
        super.startWatching();
    }

    public void stop() {
        super.stopWatching();
    }
}