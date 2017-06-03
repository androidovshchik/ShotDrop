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

import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.UploadFileTask;
import com.shotdrop.observers.ScheduledExecutorServiceClass;
import com.shotdrop.observers.ScreenshotCallback;
import com.shotdrop.utils.ClipboardUtil;
import com.shotdrop.utils.ConditionsUtil;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.Prefs;
import com.shotdrop.observers.FileObserverClass;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ServiceMain extends Service implements ScreenshotCallback {

    private static final String NOTIFICATION_ACTION_CANCEL = "com.shotdrop.broadcast.cancel";
    private static final String NOTIFICATION_ACTION_REPEAT = "com.shotdrop.broadcast.repeat";
    private static final String NOTIFICATION_ACTION_REMOVE = "com.shotdrop.broadcast.remove";
    private static final String KEY_NOTIFICATION_ID = "notificationId";
    private static final String KEY_FILENAME = "filename";
    private static final int NOTIFICATION_TYPE_PRIMARY_START = 1;
    private static final int NOTIFICATION_TYPE_PRIMARY_UPDATE = 2;
    private static final int NOTIFICATION_TYPE_CANCEL = 3;
    private static final int NOTIFICATION_TYPE_REPEAT = 4;
    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private ConditionsUtil conditions;

    private FileObserverClass fileObserver;
    private ScheduledFuture<?> scheduledFuture;

    private Prefs prefs;

    private ArrayList<UploadFileTask> tasks;

    private BroadcastReceiver cancelUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
            String filename = intent.getStringExtra(KEY_FILENAME);
            switch (intent.getAction()) {
                case NOTIFICATION_ACTION_CANCEL:
                    Timber.d("NOTIFICATION_ACTION_CANCEL");
                    onFinishUpload(notificationId);
                    showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                            "Отменен " + filename, null);
                    break;
                case NOTIFICATION_ACTION_REMOVE:
                    Timber.d("NOTIFICATION_ACTION_REMOVE");
                    onFinishUpload(notificationId);
                    break;
                case NOTIFICATION_ACTION_REPEAT:
                    Timber.d("NOTIFICATION_ACTION_REPEAT");
                    onFinishUpload(notificationId);
                    startUploadTask(filename);
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
        prefs = new Prefs(getApplicationContext());
        tasks = new ArrayList<>();
        if (prefs.isClassFileObserver()) {
            fileObserver = new FileObserverClass(prefs.getScreenshotsPath(), this);
        }
    }

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ServiceMain.class);
    }

    public static boolean isRunning(Context context) {
        return ComponentUtil.isServiceRunning(context, ServiceMain.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (conditions.checkRequired(getApplicationContext())) {
            String classname = getClass().getSimpleName();
            LogUtil.logDivider(classname, "#");
            LogUtil.logCentered(" ", classname, "Starting service...");
            LogUtil.logDivider(classname, "#");
            if (prefs.isClassFileObserver()) {
                fileObserver.start();
            }
            if (prefs.isClassScheduledExecutorService()) {
                ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
                scheduledFuture = timer.scheduleWithFixedDelay(
                        new ScheduledExecutorServiceClass(prefs.getScreenshotsPath(),
                                getApplicationContext(), this), 0, 1, TimeUnit.SECONDS);
            }
            showNotification(NOTIFICATION_TYPE_PRIMARY_START, getString(R.string.app_name),
                    "Служба запущена", null);
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onScreenshotTaken(String filename, Long lastModified) {
        if (!prefs.enabledMultiTasks()) {
            if (lastModified != null) {
                prefs.putString(Prefs.LAST_SCREENSHOT_MODIFIED, lastModified);
            }
        } else {
            prefs.remove(Prefs.LAST_SCREENSHOT_MODIFIED);
        }
        startUploadTask(filename);
    }

    /**
     * New task. New notification
     */
    private void startUploadTask(String filename) {
        if (!conditions.checkOptional()) {
            showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                    "Остановлен по опциональным условиям", null);
            return;
        }
        if (!prefs.enabledMultiTasks() && tasks.size() > 0) {
            showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                    "Пропущен новый скриншот", null);
        } else {
            showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                    "К загрузке " + filename, null);
            int notificationId = showNotification(NOTIFICATION_TYPE_CANCEL, filename,
                    "Идет загрузка...", null);
            tasks.add(getUploadTask(notificationId, filename));
            tasks.get(tasks.size() - 1).execute(prefs.getScreenshotsPath(), filename);
        }
    }

    private UploadFileTask getUploadTask(final int notificationId, final String filename) {
        return new UploadFileTask(notificationId, DropboxClientFactory
                .getClient(getApplicationContext()), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(SharedLinkMetadata result) {
                Timber.d("onUploadComplete: " + result.getUrl());
                onFinishUpload(notificationId);
                ClipboardUtil.copy(getApplicationContext(), result.getUrl());
                showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                        "Загружен и скопирован " + filename, null);
            }

            @Override
            public void onError(@Nullable Exception e) {
                String error = e == null ? "Возможно проблемы с подключением" :
                        e.getLocalizedMessage();
                Timber.e(error);
                onFinishUpload(notificationId);
                showNotification(NOTIFICATION_TYPE_REPEAT, filename,
                        "Не удалось загрузить ¯\\(ツ)/¯", null);
                showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                        error, null);
            }
        });
    }

    private void onFinishUpload(int notificationId) {
        notificationManager.cancel(notificationId);
        removeCertainTask(notificationId);
    }

    private void removeCertainTask(int notificationId) {
        if (!prefs.enabledMultiTasks()) {
            removeAllTasks();
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).notificationId == notificationId) {
                tasks.get(i).cancel(true);
                tasks.remove(i);
                break;
            }
        }
    }

    private void removeAllTasks() {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            tasks.get(i).cancel(true);
            tasks.remove(i);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllTasks();
        if (prefs.isClassFileObserver() && fileObserver != null) {
            fileObserver.stop();
        }
        if (prefs.isClassScheduledExecutorService() && scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        unregisterReceiver(cancelUploadReceiver);
        stopForeground(true);
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
                .setSmallIcon(type == NOTIFICATION_TYPE_PRIMARY_START ||
                        type == NOTIFICATION_TYPE_PRIMARY_UPDATE ? R.drawable.ic_dropbox_white :
                        R.drawable.ic_cloud_upload_white_24dp)
                .setContentTitle(title);
        Intent intentMain;
        Intent intentRemove = new Intent(NOTIFICATION_ACTION_REMOVE);
        int newNotificationId;
        if (text != null) {
            builder.setContentText(text);
        }
        switch (type) {
            case NOTIFICATION_TYPE_PRIMARY_START: case NOTIFICATION_TYPE_PRIMARY_UPDATE:
                newNotificationId = NOTIFICATION_PRIMARY_ID;
                intentMain = new Intent(getApplicationContext(), ActivityMain.class);
                intentMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                        0, intentMain, 0));
                if (type == NOTIFICATION_TYPE_PRIMARY_START) {
                    startForeground(NOTIFICATION_PRIMARY_ID, builder.build());
                    return newNotificationId;
                }
                break;
            case NOTIFICATION_TYPE_CANCEL:
                if (prevNotificationId != null) {
                    newNotificationId = prevNotificationId;
                } else {
                    lastNotificationId++;
                    newNotificationId = lastNotificationId;
                }
                intentMain = new Intent(NOTIFICATION_ACTION_CANCEL);
                intentMain.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intentMain.putExtra(KEY_FILENAME, title);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.setProgress(0, 0, true);
                builder.addAction(0, getString(android.R.string.cancel),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intentMain,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                intentRemove.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intentRemove.putExtra(KEY_FILENAME, title);
                builder.setDeleteIntent(PendingIntent.getBroadcast(getApplicationContext(), 0,
                        intentRemove, PendingIntent.FLAG_UPDATE_CURRENT));
                break;
            case NOTIFICATION_TYPE_REPEAT:
                if (prevNotificationId != null) {
                    newNotificationId = prevNotificationId;
                } else {
                    lastNotificationId++;
                    newNotificationId = lastNotificationId;
                }
                intentMain = new Intent(NOTIFICATION_ACTION_REPEAT);
                intentMain.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intentMain.putExtra(KEY_FILENAME, title);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.addAction(0, getString(R.string.notification_repeat),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intentMain,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                intentRemove.putExtra(KEY_NOTIFICATION_ID, newNotificationId);
                intentRemove.putExtra(KEY_FILENAME, title);
                builder.setDeleteIntent(PendingIntent.getBroadcast(getApplicationContext(), 0,
                        intentRemove, PendingIntent.FLAG_UPDATE_CURRENT));
                break;
            default:
                return 0;
        }
        notificationManager.notify(newNotificationId, builder.build());
        return newNotificationId;
    }
}