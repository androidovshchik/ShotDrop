package com.shotdrop;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.ListView;

public class FragmentSettings extends PreferenceFragment {

    private SwitchPreference enableDropboxAccount;
    private SwitchPreference enableApplication;
    private SwitchPreference enableUploadOnlyByWifi;
    private SwitchPreference enableStartAfterReboot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);

        enableDropboxAccount = (SwitchPreference) getPreferenceManager()
                .findPreference("enableDropboxAccount");
        enableApplication = (SwitchPreference) getPreferenceManager()
                .findPreference("enableApplication");
        enableUploadOnlyByWifi = (SwitchPreference) getPreferenceManager()
                .findPreference("enableUploadOnlyByWifi");
        enableStartAfterReboot = (SwitchPreference) getPreferenceManager()
                .findPreference("enableStartAfterReboot");
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