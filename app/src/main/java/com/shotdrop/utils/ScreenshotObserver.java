package com.shotdrop.utils;

import android.os.FileObserver;
import android.support.annotation.NonNull;

import timber.log.Timber;

public final class ScreenshotObserver extends FileObserver {

    private Callback callback;

    private String lastFilename = null;

    public interface Callback {
        void onScreenshotTaken(String filename);
    }

    public ScreenshotObserver(@NonNull String path, @NonNull Callback callback) {
        super(path, FileObserver.CLOSE_WRITE);
        this.callback = callback;
    }

    @Override
    public void onEvent(int event, String filename) {
        if (filename != null && event == FileObserver.CLOSE_WRITE && (lastFilename == null ||
                !filename.equals(lastFilename))) {
            Timber.d("Event: %d; Path: %s", event, filename);
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