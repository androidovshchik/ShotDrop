package com.shotdrop;

import android.os.Bundle;
import android.widget.Toast;

import com.dropbox.core.v2.users.FullAccount;

import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.DropboxPreferenceActivity;
import com.shotdrop.dropbox.GetCurrentAccountTask;
import com.shotdrop.utils.Prefs;

import timber.log.Timber;

public class ActivityMain extends DropboxPreferenceActivity {

    private FragmentSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new FragmentSettings();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settings)
                .commit();
    }

    @Override
    protected void loadData() {
        final Prefs prefs = new Prefs(getApplicationContext());
        if (!prefs.getBoolean(Prefs.ENABLE_DROPBOX_ACCOUNT) && (!prefs.has(Prefs.USER_EMAIL) ||
                !prefs.has(Prefs.USER_DISPLAY_NAME))) {
            Toast.makeText(getApplicationContext(), getString(R.string.alert_wait),
                    Toast.LENGTH_SHORT)
                    .show();
        }
        new GetCurrentAccountTask(DropboxClientFactory.getClient(),
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
                        Timber.e(e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }).execute();
    }
}
