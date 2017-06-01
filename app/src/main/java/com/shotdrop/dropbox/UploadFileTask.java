package com.shotdrop.dropbox;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxUploader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Async task to upload a file to a directory
 */
public class UploadFileTask extends AsyncTask<String, Void, FileMetadata> {

    public static final String PATH = Environment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots/";

    public final int notificationId;

    private final DbxClientV2 dbxClient;
    private DbxUploader uploader;

    private final Callback callback;

    private Exception exception;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(@Nullable Exception e);
    }

    public UploadFileTask(int notificationId, DbxClientV2 dbxClient, Callback callback) {
        this.notificationId = notificationId;
        this.dbxClient = dbxClient;
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (exception != null) {
            callback.onError(exception);
        } else if (result == null) {
            callback.onError(null);
        } else {
            callback.onUploadComplete(result);
        }
    }

    @Override
    protected void onCancelled() {
        uploader.abort();
        uploader.close();
    }

    @Override
    protected FileMetadata doInBackground(String... filenames) {
        try (InputStream inputStream = new FileInputStream(new File(PATH + filenames[0]))) {
            uploader = dbxClient.files()
                    .uploadBuilder(File.separator + filenames[0])
                    .withMode(WriteMode.OVERWRITE)
                    .start();
            return (FileMetadata) uploader.uploadAndFinish(inputStream);
        } catch (DbxException | IOException e) {
            exception = e;
        }
        return null;
    }
}
