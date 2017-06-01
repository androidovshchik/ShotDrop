package com.shotdrop.models;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.PermissionsUtil;
import com.shotdrop.utils.Prefs;

public class Conditions {

    private Prefs prefs;

    private boolean hasAllPermissions;

    private boolean hasDropboxAccount;

    private boolean hasEnabledApplication;

    private boolean onlyWifiConnection;
    private boolean hasWifiConnection;

    public Conditions(Context context) {
        prefs = new Prefs(context);
    }

    @SuppressWarnings("all")
    public boolean checkAll(Context context) {
        hasAllPermissions = PermissionsUtil.hasAllPermissions(context);
        hasDropboxAccount = onlyWhen(Prefs.ENABLE_DROPBOX_ACCOUNT);
        hasEnabledApplication = onlyWhen(Prefs.ENABLE_APPLICATION);
        onlyWifiConnection = onlyWhen(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI);
        hasWifiConnection = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .isWifiEnabled();
    }

    private boolean onlyWhen(String key) {
        return prefs.getBoolean(key);
    }

    public void log() {
        String classname = getClass().getSimpleName();
        LogUtil.logDivider(classname, "#");
        LogUtil.logCentered(" ", classname, "Start service...");
        LogUtil.logDivider(classname, "#");
        return "Conditions{" +
                "hasAllPermissions=" + hasAllPermissions +
                ", hasDropboxAccount=" + hasDropboxAccount +
                ", hasEnabledApplication=" + hasEnabledApplication +
                ", onlyWifiConnection=" + onlyWifiConnection +
                ", hasWifiConnection=" + hasWifiConnection +
                '}';
    }
}