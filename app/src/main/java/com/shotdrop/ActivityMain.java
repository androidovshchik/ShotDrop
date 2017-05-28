package com.shotdrop;

import android.os.Bundle;

import com.shotdrop.dropbox.DropboxPreferenceActivity;

public class ActivityMain extends DropboxPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentSettings settings = new FragmentSettings();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
    }
}
