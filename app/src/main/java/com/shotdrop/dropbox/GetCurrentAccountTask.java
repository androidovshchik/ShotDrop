package com.shotdrop.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

/**
 * Async task for getting user account info
 */
public class GetCurrentAccountTask extends AsyncTask<Void, Void, FullAccount> {

    private final DbxClientV2 dbxClient;
    private final Callback callback;
    private Exception exception;

    public interface Callback {
        void onComplete(FullAccount result);
        void onError(Exception e);
    }

    public GetCurrentAccountTask(DbxClientV2 dbxClient, Callback callback) {
        this.dbxClient = dbxClient;
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(FullAccount account) {
        super.onPostExecute(account);
        if (exception != null) {
            callback.onError(exception);
        } else {
            callback.onComplete(account);
        }
    }

    @Override
    protected FullAccount doInBackground(Void... params) {
        try {
            return dbxClient.users().getCurrentAccount();
        } catch (DbxException e) {
            exception = e;
        }
        return null;
    }
}
