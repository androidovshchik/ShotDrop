package com.shotdrop.utils;

import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.NonNull;

import java.io.File;

import timber.log.Timber;

public final class ScreenshotObserver extends FileObserver {

    private static final String PATH = Environment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots/";

    private Callback callback;

    private String lastTakenPath = null;

    public interface Callback {
        void onScreenshotTaken(Uri uri);
    }

    public ScreenshotObserver(@NonNull Callback callback) {
        super(PATH, FileObserver.CLOSE_WRITE);
        this.callback = callback;
    }

    @Override
    public void onEvent(int event, String path) {
        Timber.i("Event: %d; Path: %s", event, path);
        if (path != null && event == FileObserver.CLOSE_WRITE && (lastTakenPath == null ||
                !path.equalsIgnoreCase(lastTakenPath))) {
            lastTakenPath = path;
            callback.onScreenshotTaken(Uri.fromFile(new File(PATH + path)));
        }
    }

    public void start() {
        super.startWatching();
    }

    public void stop() {
        super.stopWatching();
    }
}