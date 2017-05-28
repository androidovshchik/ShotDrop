package com.shotdrop;

import android.os.Bundle;

import com.dropbox.core.v2.users.FullAccount;

import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.DropboxPreferenceActivity;
import com.shotdrop.dropbox.GetCurrentAccountTask;

import timber.log.Timber;

public class ActivityMain extends DropboxPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentSettings settings = new FragmentSettings();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
    }

    @Override
    protected void loadData() {
        new GetCurrentAccountTask(DropboxClientFactory.getClient(),
                new GetCurrentAccountTask.Callback() {
                    @Override
                    public void onComplete(FullAccount result) {
                        Timber.d(result.getEmail());
                        Timber.d(result.getName().getDisplayName());
                    }

                    @Override
                    public void onError(Exception e) {
                        Timber.e(e.getMessage());
                    }
                }).execute();
    }
}
