package com.shotdrop.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

public final class PermissionsUtil {

    public static final String[] ALL_PERMISSIONS = new String[] {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
    };

    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : ALL_PERMISSIONS) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }
}
