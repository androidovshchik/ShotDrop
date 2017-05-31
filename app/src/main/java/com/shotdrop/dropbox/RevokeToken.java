package com.shotdrop.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;

/**
 * Async task for revoking token
 */
public class RevokeToken extends AsyncTask<Void, Void, Void> {

    private final DbxClientV2 dbxClient;
    private final Callback callback;
    private Exception exception;

    public interface Callback {
        void onComplete();
        void onError(Exception e);
    }

    public RevokeToken(DbxClientV2 dbxClient, Callback callback) {
        this.dbxClient = dbxClient;
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (exception != null) {
            callback.onError(exception);
        } else {
            callback.onComplete();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            dbxClient.auth().tokenRevoke();
        } catch (DbxException e) {
            exception = e;
        }
        return null;
    }
}
