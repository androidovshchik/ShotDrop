package com.shotdrop.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

public class ConditionsUtil {

    private Prefs prefs;

    private WifiManager wifiManager;

    private boolean hasAllPermissions;

    private boolean hasDropboxAccount;

    private boolean hasEnabledApplication;

    private boolean onlyWifiConnection;
    private boolean hasWifiConnection;

    @SuppressWarnings("all")
    public ConditionsUtil(Context context) {
        prefs = new Prefs(context);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean checkRequired(Context context) {
        hasAllPermissions = PermissionsUtil.hasAllPermissions(context);
        hasDropboxAccount = onlyWhen(Prefs.ENABLE_DROPBOX_ACCOUNT);
        hasEnabledApplication = onlyWhen(Prefs.ENABLE_APPLICATION);
        return hasAllPermissions && hasDropboxAccount && hasEnabledApplication;
    }

    public boolean checkOptional() {
        onlyWifiConnection = onlyWhen(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI);
        hasWifiConnection = wifiManager.isWifiEnabled();
        return hasWifiConnection || !onlyWifiConnection;
    }

    private boolean onlyWhen(String key) {
        return prefs.getBoolean(key);
    }

    public void log() {
        String classname = getClass().getSimpleName();
        LogUtil.logDivider(classname, "*");
        LogUtil.logCentered("*", classname, "hasAllPermissions: " + hasAllPermissions);
        LogUtil.logCentered("*", classname, "hasDropboxAccount: " + hasDropboxAccount);
        LogUtil.logCentered("*", classname, "hasEnabledApplication: " + hasEnabledApplication);
        LogUtil.logCentered("*", classname, "onlyWifiConnection: " + onlyWifiConnection);
        LogUtil.logCentered("*", classname, "hasWifiConnection: " + hasWifiConnection);
        LogUtil.logDivider(classname, "*");
    }
}