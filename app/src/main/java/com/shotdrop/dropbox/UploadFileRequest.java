package com.shotdrop.dropbox;

import android.content.Context;

import com.dropbox.core.v2.DbxClientV2;
import com.shotdrop.utils.Prefs;
import com.shotdrop.utils.RequestBinaryUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class UploadFileRequest implements okhttp3.Callback {

    private static final String UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload";

    private final OkHttpClient client;

    private final Callback callback;

    private final Prefs prefs;

    private final DbxClientV2 dbxClient;

    public interface Callback {

        void onSuccess(int notificationId, String filename, String url);

        void onError(int notificationId, String filename, String message);
    }

    public UploadFileRequest(Context context, DbxClientV2 dbxClient, Callback callback) {
        this.callback = callback;
        this.dbxClient = dbxClient;
        this.prefs = new Prefs(context);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void enqueue(int notificationId, String filename) {
        JSONObject tag = new JSONObject();
        JSONObject path = new JSONObject();
        try {
            tag.put("notificationId", notificationId);
            tag.put("filename", File.separator + filename);
            path.put("path", File.separator + filename);
        } catch (JSONException e) {
            Timber.e(e.getLocalizedMessage());
            callback.onError(notificationId, filename, e.getLocalizedMessage());
            return;
        }
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(prefs.getScreenshotsPath() +
                    filename));
        } catch (FileNotFoundException e) {
            Timber.e(e.getLocalizedMessage());
            callback.onError(notificationId, filename, e.getLocalizedMessage());
            return;
        }
        RequestBody requestBody = RequestBinaryUtil.create(MediaType
                .parse("application/octet-stream"), inputStream);
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer " + prefs.getString(Prefs.ACCESS_TOKEN))
                .addHeader("Dropbox-API-Arg", path.toString())
                .addHeader("Content-Type", "application/octet-stream")
                .post(requestBody)
                .tag(tag.toString())
                .build();
        client.newCall(request)
                .enqueue(this);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Timber.e(e.toString());
        callback.onError(getNotificationId(call), getFilename(call), e.getLocalizedMessage());
    }

    @Override
    @SuppressWarnings("all")
    public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) {
            Timber.e("ERROR: Response is unsuccessful");
            callback.onError(getNotificationId(call), getFilename(call), "Запрос не выполнен");
            return;
        }
        String json = response.body().string();
        Timber.d("OUTPUT: %s", json);
        if (json == null || json.isEmpty() || json.equals("null")) {
            callback.onError(getNotificationId(call), getFilename(call),
                    "Невалидный ответ на запрос");
            return;
        }
        try {
            JSONObject object = new JSONObject(json);
            .put("path", File.separator + filename);
        } catch (JSONException e) {
            Timber.e(e.getLocalizedMessage());
            callback.onError(notificationId, e.getLocalizedMessage());
            return;
        }
        callback.onSuccess(getNotificationId(call), getFilename(call),  dbxClient.sharing()
                .createSharedLinkWithSettings(File.separator + values[1]));
    }

    private int getNotificationId(Call call) {
        try {
            JSONObject object = new JSONObject((String) call.request().tag());
            return object.getInt("notificationId");
        } catch (JSONException e) {
            Timber.e(e.getLocalizedMessage());
            return 0;
        }
    }

    private String getFilename(Call call) {
        try {
            JSONObject object = new JSONObject((String) call.request().tag());
            return object.getString("filename");
        } catch (JSONException e) {
            Timber.e(e.getLocalizedMessage());
            return "неизвестный файл";
        }
    }
}
