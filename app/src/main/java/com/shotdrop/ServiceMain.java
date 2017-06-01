package com.shotdrop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.dropbox.core.v2.files.FileMetadata;
import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.UploadFileTask;
import com.shotdrop.utils.ClipboardUtil;
import com.shotdrop.utils.ConditionsUtil;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.ScreenshotObserver;

import java.util.ArrayList;

import timber.log.Timber;

public class ServiceMain extends Service implements ScreenshotObserver.Callback {

    private static final String NOTIFICATION_ACTION_CANCEL = "com.shotdrop.broadcast.cancel";
    private static final String NOTIFICATION_ACTION_REPEAT = "com.shotdrop.broadcast.repeat";
    private static final String KEY_NOTIFICATION_ID = "notificationId";
    private static final String KEY_FILENAME = "filename";
    private static final int NOTIFICATION_TYPE_PRIMARY = 1;
    private static final int NOTIFICATION_TYPE_CANCEL = 2;
    private static final int NOTIFICATION_TYPE_REPEAT = 3;
    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private ConditionsUtil conditions;

    private ScreenshotObserver screenshotObserver;

    private ArrayList<UploadFileTask> tasks;

    private BroadcastReceiver cancelUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
            String filename = intent.getStringExtra(KEY_FILENAME);
            switch (intent.getAction()) {
                case NOTIFICATION_ACTION_CANCEL:
                    Timber.d("NOTIFICATION_ACTION_CANCEL");
                    notificationManager.cancel(notificationId);
                    removeTask(notificationId);
                    break;
                case NOTIFICATION_ACTION_REPEAT:
                    Timber.d("NOTIFICATION_ACTION_REPEAT");
                    notificationManager.cancel(notificationId);
                    newUploadTask(filename);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getString(R.string.app_name));
        wakeLock.acquire();
        notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATION_ACTION_CANCEL);
        intentFilter.addAction(NOTIFICATION_ACTION_REPEAT);
        registerReceiver(cancelUploadReceiver, intentFilter);
        conditions = new ConditionsUtil(getApplicationContext());
        screenshotObserver = new ScreenshotObserver(this);
        tasks = new ArrayList<>();
    }

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ServiceMain.class);
    }

    public static boolean isRunning(Context context) {
        return ComponentUtil.isServiceRunning(context, ServiceMain.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        conditions.log();
        if (intent != null && conditions.checkRequired(getApplicationContext())) {
            String classname = getClass().getSimpleName();
            LogUtil.logDivider(classname, "#");
            LogUtil.logCentered(" ", classname, "Starting service...");
            LogUtil.logDivider(classname, "#");
            screenshotObserver.start();
            showNotification(NOTIFICATION_TYPE_PRIMARY, getString(R.string.app_name),
                    "Служба запущена", null);
            return START_STICKY;
        } else {
            if (intent == null) {
                Timber.w("onStartCommand: intent == null");
            }
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onScreenshotTaken(String filename) {
        conditions.log();
        if (!conditions.checkOptional()) {
            showNotification(NOTIFICATION_TYPE_PRIMARY, getString(R.string.app_name),
                    "Остановлен по опциональным условиям", null);
            return;
        }
        showNotification(NOTIFICATION_TYPE_PRIMARY, getString(R.string.app_name),
                "К загрузке " + filename, null);
        newUploadTask(filename);
    }

    /**
     * New task. New notification
     */
    private void newUploadTask(final String filename) {
        final int notificationId = showNotification(NOTIFICATION_TYPE_CANCEL, filename,
                "Идет загрузка...", null);
        tasks.add(new UploadFileTask(notificationId, DropboxClientFactory.getClient(),
                new UploadFileTask.Callback() {
                    @Override
                    public void onUploadComplete(FileMetadata result) {
                        Timber.d("onUploadComplete: " + result.toString());
                        removeTask(notificationId);
                        ClipboardUtil.copy(getApplicationContext(), result.getPathDisplay());
                    }

                    @Override
                    public void onError(@Nullable Exception e) {
                        Timber.e(e == null ? "UploadFileTask: onError" : e.getLocalizedMessage());
                        notificationManager.cancel(notificationId);
                        removeTask(notificationId);
                        showNotification(NOTIFICATION_TYPE_REPEAT, filename,
                                "Не удалось загрузить ¯\\(ツ)/¯", null);
                    }
                }));
        tasks.get(tasks.size() - 1).execute(filename);
    }

    private void removeTask(int notificationId) {
        for (int i =0; i < tasks.size(); i++) {
            if (tasks.get(i).notificationId == notificationId) {
                tasks.get(i).cancel(true);
                tasks.remove(i);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        screenshotObserver.stop();
        unregisterReceiver(cancelUploadReceiver);
        notificationManager.cancelAll();
        wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @param title  For non-primary notifications is a filename
     */
    private int showNotification(int type, @NonNull String title, @Nullable String text,
                                 @Nullable Integer prevNotificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(type == NOTIFICATION_TYPE_PRIMARY ? R.drawable.ic_dropbox_white :
                        R.drawable.ic_cloud_upload_white_24dp)
                .setContentTitle(title)
                .setOngoing(true);
        Intent intent;
        int newNotificationId;
        if (text != null) {
            builder.setContentText(text);
        }
        switch (type) {
            case NOTIFICATION_TYPE_PRIMARY:
                newNotificationId = NOTIFICATION_PRIMARY_ID;
                intent = new Intent(getApplicationContext(), ActivityMain.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                        0, intent, 0));
                break;
            case NOTIFICATION_TYPE_CANCEL:
                if (prevNotificationId != null) {
                    newNotificationId = prevNotificationId;
                } else {
                    lastNotificationId++;
                    newNotificationId = lastNotificationId;
                }
                intent = new Intent(NOTIFICATION_ACTION_CANCEL);
                intent.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intent.putExtra(KEY_FILENAME, title);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.setProgress(0, 0, true);
                builder.addAction(0, getString(android.R.string.cancel),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
                break;
            case NOTIFICATION_TYPE_REPEAT:
                if (prevNotificationId != null) {
                    newNotificationId = prevNotificationId;
                } else {
                    lastNotificationId++;
                    newNotificationId = lastNotificationId;
                }
                intent = new Intent(NOTIFICATION_ACTION_REPEAT);
                intent.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intent.putExtra(KEY_FILENAME, title);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.addAction(0, getString(R.string.notification_repeat),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
                break;
            default:
                return 0;
        }
        notificationManager.notify(newNotificationId, builder.build());
        return newNotificationId;
    }
}