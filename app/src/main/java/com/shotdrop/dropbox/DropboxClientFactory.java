package com.shotdrop.dropbox;

import android.content.Context;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.shotdrop.utils.Prefs;

/**
 * Singleton instance of {@link DbxClientV2} and friends
 */
public class DropboxClientFactory {

    private static DbxClientV2 dbxClient;

    public static void init(String accessToken) {
        if (dbxClient == null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("examples-v2-demo")
                .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                .build();
            dbxClient = new DbxClientV2(requestConfig, accessToken);
        }
    }

    public static DbxClientV2 getClient(Context context) {
        if (dbxClient == null) {
            Prefs prefs = new Prefs(context);
            if (prefs.has(Prefs.ACCESS_TOKEN)) {
                init(prefs.getString(Prefs.ACCESS_TOKEN));
            } else {
                throw new IllegalStateException("Client not initialized.");
            }
        }
        return dbxClient;
    }

    public static void clearClient() {
        dbxClient = null;
    }
}
