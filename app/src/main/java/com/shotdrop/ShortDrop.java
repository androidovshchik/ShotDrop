package com.shotdrop;

import android.app.Application;

import com.shotdrop.utils.Prefs;

import timber.log.Timber;

public class ShortDrop extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Prefs prefs = new Prefs(getApplicationContext());
        prefs.remove(Prefs.APP_RESTRICTIONS);
        if (BuildConfig.DEBUG) {
            prefs.printAll();
        }
    }
}
