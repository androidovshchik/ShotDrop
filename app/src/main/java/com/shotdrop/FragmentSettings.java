package com.shotdrop;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.ListView;

import com.dropbox.core.android.Auth;

public class FragmentSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);

        SwitchPreference enableDropboxAccount = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_DROPBOX_ACCOUNT);
        enableDropboxAccount.setOnPreferenceChangeListener(this);
        SwitchPreference enableApplication = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_APPLICATION);
        enableApplication.setOnPreferenceChangeListener(this);
        SwitchPreference enableUploadOnlyByWifi = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI);
        enableUploadOnlyByWifi.setOnPreferenceChangeListener(this);
        SwitchPreference enableStartAfterReboot = (SwitchPreference) getPreferenceManager()
                .findPreference(Prefs.ENABLE_START_AFTER_REBOOT);
        enableStartAfterReboot.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case Prefs.ENABLE_DROPBOX_ACCOUNT:
                Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
                return false;
            case Prefs.ENABLE_APPLICATION:
                break;
            case Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI:
                break;
            case Prefs.ENABLE_START_AFTER_REBOOT:
                break;
        }
        return true;
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