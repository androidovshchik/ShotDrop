package com.shotdrop;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.RevokeTokenTask;
import com.shotdrop.utils.Prefs;

import timber.log.Timber;

public class FragmentSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, DialogInterface.OnClickListener {

    private SwitchPreference enableDropboxAccount;
    private SwitchPreference enableApplication;
    private SwitchPreference enableUploadOnlyByWifi;
    private SwitchPreference enableStartAfterReboot;
    private SwitchPreference enableMultiTasks;
    private SwitchPreference debugMode;

    private PreferenceCategory userInfo;

    private Prefs prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);

        prefs = new Prefs(getActivity().getApplicationContext());

        enableDropboxAccount = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_DROPBOX_ACCOUNT);
        enableDropboxAccount.setChecked(prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT));
        enableDropboxAccount.setOnPreferenceChangeListener(this);
        enableApplication = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_APPLICATION);
        enableApplication.setOnPreferenceChangeListener(this);
        enableUploadOnlyByWifi = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI);
        enableUploadOnlyByWifi.setOnPreferenceChangeListener(this);
        enableStartAfterReboot = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_START_AFTER_REBOOT);
        enableStartAfterReboot.setOnPreferenceChangeListener(this);
        enableMultiTasks = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_MULTI_TASKS);
        enableMultiTasks.setOnPreferenceChangeListener(this);
        debugMode = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.DEBUG_MODE);
        debugMode.setOnPreferenceChangeListener(this);

        if (!prefs.has(Prefs.SCREENSHOTS_PATH)) {
            prefs.putString(Prefs.SCREENSHOTS_PATH, prefs.getScreenshotsPath());
        }
        EditTextPreference screenshotsPath = (EditTextPreference) getPreferenceManager()
                .findPreference(Prefs.SCREENSHOTS_PATH);
        screenshotsPath.setDefaultValue(prefs.getScreenshotsPath());
        screenshotsPath.setText(prefs.getScreenshotsPath());
        screenshotsPath.setOnPreferenceChangeListener(this);

        Preference observerClass = getPreferenceManager().findPreference(Prefs.OBSERVER_CLASS);
        observerClass.setOnPreferenceChangeListener(this);

        userInfo = (PreferenceCategory) getPreferenceManager().findPreference("userInfo");
        if (prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT) && prefs.has(Prefs.USER_EMAIL) &&
                prefs.has(Prefs.USER_DISPLAY_NAME)) {
            applyAccountInfo(prefs.getString(Prefs.USER_EMAIL),
                    prefs.getString(Prefs.USER_DISPLAY_NAME));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .create();
        boolean isServiceRunning = ServiceMain.isRunning(getActivity()
                .getApplicationContext());
        switch (preference.getKey()) {
            case Prefs.ENABLE_DROPBOX_ACCOUNT:
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                        getString(android.R.string.cancel), this);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                        getString(android.R.string.ok), this);
                if (!prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT)) {
                    Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
                } else {
                    alertDialog.setMessage(getString(R.string.prompt_logout));
                    alertDialog.show();
                }
                return false;
            case Prefs.ENABLE_APPLICATION: case Prefs.ENABLE_START_AFTER_REBOOT:
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                        getString(android.R.string.ok), this);
                if (!prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT)) {
                    alertDialog.setMessage(getString(R.string.prompt_require_account));
                    alertDialog.show();
                    return false;
                } else if (preference.getKey().equals(Prefs.ENABLE_APPLICATION)) {
                    if ((boolean) newValue) {
                        if (!isServiceRunning) {
                            getActivity().startService(ServiceMain.getStartIntent(getActivity()
                                    .getApplicationContext()));
                        }
                    } else {
                        if (isServiceRunning) {
                            getActivity().stopService(ServiceMain.getStartIntent(getActivity()
                                    .getApplicationContext()));
                        }
                    }
                }
                return true;
            case Prefs.ENABLE_MULTI_TASKS: case Prefs.SCREENSHOTS_PATH: case Prefs.OBSERVER_CLASS:
                if (isServiceRunning) {
                    getActivity().stopService(ServiceMain.getStartIntent(getActivity()
                            .getApplicationContext()));
                }
                if (prefs.getBoolean(Prefs.ENABLE_APPLICATION)) {
                    prefs.putBoolean(Prefs.ENABLE_APPLICATION, false);
                    applyMainSwitches();
                    Toast.makeText(getActivity().getApplicationContext(),
                            getString(R.string.alert_stop), Toast.LENGTH_SHORT)
                            .show();
                }
                if (preference.getKey().equals(Prefs.ENABLE_MULTI_TASKS) && (boolean) newValue &&
                        prefs.isClassScheduledExecutorService() ||
                        preference.getKey().equals(Prefs.OBSERVER_CLASS) &&
                                prefs.enabledMultiTasks() && newValue.equals("2")) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            getString(R.string.alert_not_compatible), Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
                return true;
        }
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                Toast.makeText(getActivity().getApplicationContext(),
                        getString(R.string.alert_logout), Toast.LENGTH_SHORT)
                        .show();
                new RevokeTokenTask(DropboxClientFactory.getClient(getActivity()
                        .getApplicationContext()),
                        new RevokeTokenTask.Callback() {
                            @Override
                            public void onComplete() {
                                logout();
                            }

                            @Override
                            public void onError(Exception e) {
                                Timber.e(e.getMessage());
                                logout();
                            }
                        }).execute();
                break;
        }
        dialog.dismiss();
    }

    private void logout() {
        DropboxClientFactory.clearClient();
        AuthActivity.result = null;
        prefs.logout();
        applyAccountInfo(null, null);
    }

    public void applyMainSwitches() {
        enableDropboxAccount.setChecked(prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT));
        enableApplication.setChecked(prefs.getBoolean(Prefs.ENABLE_APPLICATION));
        enableStartAfterReboot.setChecked(prefs.getBoolean(Prefs.ENABLE_START_AFTER_REBOOT));
        enableUploadOnlyByWifi.setChecked(prefs.getBoolean(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI));
        enableMultiTasks.setChecked(prefs.getBoolean(Prefs.ENABLE_MULTI_TASKS));
        debugMode.setChecked(prefs.getBoolean(Prefs.DEBUG_MODE));
    }

    public void applyAccountInfo(@Nullable String email, @Nullable String displayName) {
        applyMainSwitches();
        if (email == null || displayName == null) {
            userInfo.setTitle("");
            userInfo.setSummary("");
        } else {
            userInfo.setTitle(getString(R.string.settings_user_title));
            userInfo.setSummary(getString(R.string.settings_user_info, displayName, email));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getView() != null) {
            ListView list = (ListView) getView().findViewById(android.R.id.list);
            if (list != null) {
                list.setPadding(0, 0, 0, 0);
            }
        }
    }
}