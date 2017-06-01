package com.shotdrop;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.dropbox.core.v2.users.FullAccount;

import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.DropboxPreferenceActivity;
import com.shotdrop.dropbox.GetCurrentAccountTask;
import com.shotdrop.utils.PermissionsUtil;
import com.shotdrop.utils.Prefs;

import timber.log.Timber;

public class ActivityMain extends DropboxPreferenceActivity implements
        DialogInterface.OnClickListener {

    public static final int REQUEST_PERMISSIONS = 100;
    public static final int REQUEST_SETTINGS = 101;

    private FragmentSettings settings;

    private Prefs prefs;

    private boolean needShowWindows = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(getApplicationContext());
        settings = new FragmentSettings();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
        if (!PermissionsUtil.hasAllPermissions(getApplicationContext())) {
            ActivityCompat.requestPermissions(this, PermissionsUtil.ALL_PERMISSIONS,
                    REQUEST_PERMISSIONS);
        } else {
            boolean isServiceRunning = ServiceMain.isRunning(getApplicationContext());
            if (prefs.getBoolean(Prefs.ENABLE_APPLICATION)) {
                if (!isServiceRunning) {
                    startService(ServiceMain.getStartIntent(getApplicationContext()));
                }
            } else {
                if (isServiceRunning) {
                    stopService(ServiceMain.getStartIntent(getApplicationContext()));
                }
            }
        }
    }

    @Override
    protected void loadData() {
        if (!prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT) && (!prefs.has(Prefs.USER_EMAIL) ||
                !prefs.has(Prefs.USER_DISPLAY_NAME))) {
            Toast.makeText(getApplicationContext(), getString(R.string.alert_login),
                    Toast.LENGTH_SHORT)
                    .show();
        }
        new GetCurrentAccountTask(DropboxClientFactory.getClient(getApplicationContext()),
                new GetCurrentAccountTask.Callback() {
                    @Override
                    public void onComplete(FullAccount result) {
                        Timber.d(result.getEmail());
                        Timber.d(result.getName().getDisplayName());
                        prefs.putBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT, true);
                        prefs.putString(Prefs.USER_EMAIL, result.getEmail());
                        prefs.putString(Prefs.USER_DISPLAY_NAME, result.getName()
                                .getDisplayName());
                        settings.applyAccountInfo(result.getEmail(), result.getName()
                                .getDisplayName());
                    }

                    @Override
                    public void onError(Exception e) {
                        Timber.e(e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .create();
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                        getString(android.R.string.cancel), this);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(android.R.string.ok), this);
                boolean needPermissions = false;
                for (String permission : PermissionsUtil.ALL_PERMISSIONS) {
                    if (!PermissionsUtil.hasPermission(getApplicationContext(), permission)) {
                        needPermissions = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                !shouldShowRequestPermissionRationale(permission)) {
                            needShowWindows = true;
                            alertDialog.setMessage(getString(R.string.prompt_settings));
                            alertDialog.show();
                            return;
                        }
                    }
                }
                if (needPermissions) {
                    alertDialog.setMessage(getString(R.string.prompt_permissions));
                    alertDialog.show();
                }
                break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                if (needShowWindows) {
                    openAppSettings();
                    finish();
                } else {
                    ActivityCompat.requestPermissions(this, PermissionsUtil.ALL_PERMISSIONS,
                            REQUEST_PERMISSIONS);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                finish();
                break;
        }
        dialog.dismiss();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivityForResult(intent, REQUEST_SETTINGS);
    }
}
