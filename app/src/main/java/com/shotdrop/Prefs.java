package com.shotdrop;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.shotdrop.utils.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class Prefs {

    /* Preferences */
    public static final String APP_RESTRICTIONS = "App Restrictions";

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String USER_ID = "userId";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_DISPLAY_NAME = "userDisplayName";

    public static final String ENABLE_DROPBOX_ACCOUNT = "enableDropboxAccount";
    public static final String ENABLE_APPLICATION = "enableApplication";
    public static final String ENABLE_UPLOAD_ONLY_BY_WIFI = "enableUploadOnlyByWifi";
    public static final String ENABLE_START_AFTER_REBOOT = "enableStartAfterReboot";

    /* Util strings */
    private static final String EMPTY = "";

    /* SharedPreferences parameters */

    private SharedPreferences preferences;

    public Prefs(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @SuppressWarnings("unused")
    public String getString(String name) {
        return preferences.getString(name, EMPTY).trim();
    }

    @SuppressWarnings("unused")
    public <T> String getString(String name, T def) {
        return preferences.getString(name, toString(def)).trim();
    }

    @SuppressWarnings("unused")
    public boolean getBoolean(String name) {
        return preferences.getBoolean(name, false);
    }

    @SuppressWarnings("unused")
    public boolean getBoolean(String name, boolean def) {
        return preferences.getBoolean(name, def);
    }

    @SuppressWarnings("unused")
    public <T> void putString(String name, T value) {
        preferences.edit().putString(name, toString(value)).apply();
    }

    @SuppressWarnings("unused")
    public void putBoolean(String name, boolean value) {
        preferences.edit().putBoolean(name, value).apply();
    }

    /* Controls functions */

    @SuppressWarnings("unused")
    public boolean has(String name) {
        return preferences.contains(name);
    }

    @SuppressWarnings("unused")
    public void clear() {
        preferences.edit().clear().apply();
    }

    @SuppressWarnings("unused")
    public void remove(String name) {
        if (has(name)) {
            preferences.edit().remove(name).apply();
        }
    }

    @SuppressWarnings("unused")
    public void logout() {
        remove(ACCESS_TOKEN);
        remove(USER_ID);
        remove(USER_EMAIL);
        remove(USER_DISPLAY_NAME);
    }

    /* Utils functions */

    @SuppressWarnings("unused")
    public <T> String toString(T value) {
        return String.class.isInstance(value)? ((String) value).trim() : String.valueOf(value);
    }

    @SuppressWarnings("unused")
    public void printAll() {
        Map<String, ?> prefAll = preferences.getAll();
        if (prefAll == null) {
            return;
        }
        List<Map.Entry<String, ?>> list = new ArrayList<>();
        list.addAll(prefAll.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, ?>>() {
            public int compare(final Map.Entry<String, ?> entry1, final Map.Entry<String, ?> entry2) {
                return entry1.getKey().compareTo(entry2.getKey());
            }
        });
        String classname = getClass().getSimpleName();
        LogUtils.logDivider(classname, "~");
        LogUtils.logCentered(" ", classname, "Printing all sharedPreferences");
        for(Map.Entry<String, ?> entry : list) {
            LogUtils.tag(classname).i(entry.getKey() + ": " + entry.getValue());
        }
        LogUtils.logDivider(classname, "~");
    }
}
