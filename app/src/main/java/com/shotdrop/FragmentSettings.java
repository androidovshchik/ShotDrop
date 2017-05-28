package com.shotdrop;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;

import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.utils.Prefs;

public class FragmentSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, DialogInterface.OnClickListener {

    private SwitchPreference enableDropboxAccount;

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

        SwitchPreference enableApplication = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_APPLICATION);
        SwitchPreference enableUploadOnlyByWifi = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI);
        SwitchPreference enableStartAfterReboot = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_START_AFTER_REBOOT);

        userInfo = (PreferenceCategory) getPreferenceManager().findPreference("userInfo");
        if (prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT) && prefs.has(Prefs.USER_EMAIL) &&
                prefs.has(Prefs.USER_DISPLAY_NAME)) {
            applyAccountInfo(prefs.getString(Prefs.USER_EMAIL),
                    prefs.getString(Prefs.USER_DISPLAY_NAME));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case Prefs.ENABLE_DROPBOX_ACCOUNT:
                if (!prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT)) {
                    Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                            .create();
                    alertDialog.setMessage(getString(R.string.prompt_logout));
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                            getString(android.R.string.cancel), this);
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                            getString(android.R.string.ok), this);
                    alertDialog.show();
                }
                return false;
        }
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                // TODO: insert method DbxUserAuthRequests.tokenRevoke(); More info here https://github.com/dropbox/dropbox-sdk-java/issues/92
                DropboxClientFactory.clearClient();
                AuthActivity.result = null;
                prefs.logout();
                applyAccountInfo(null, null);
                break;
        }
        dialog.dismiss();
    }

    public void applyAccountInfo(@Nullable String email, @Nullable String displayName) {
        enableDropboxAccount.setChecked(prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT));
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