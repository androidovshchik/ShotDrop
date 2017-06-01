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
import com.shotdrop.models.Conditions;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.ScreenshotObserver;

import timber.log.Timber;

public class ServiceMain extends Service implements ScreenshotObserver.Callback {

    public static boolean IS_RUNNING = false;
    public static boolean IS_WORKING = false;

    private static final String NOTIFICATION_ACTION_CANCEL = "com.shotdrop.broadcast.cancel";
    private static final String NOTIFICATION_ACTION_REPEAT = "com.shotdrop.broadcast.repeat";
    private static final String KEY_NOTIFICATION_ID = "notificationId";
    private static final int NOTIFICATION_TYPE_PRIMARY = 1;
    private static final int NOTIFICATION_TYPE_CANCEL = 2;
    private static final int NOTIFICATION_TYPE_REPEAT = 3;
    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private Conditions conditions;

    private ScreenshotObserver screenshotObserver;

    private BroadcastReceiver cancelUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case NOTIFICATION_ACTION_CANCEL:
                    Timber.d("NOTIFICATION_ACTION_CANCEL");
                    break;
                case NOTIFICATION_ACTION_REPEAT:
                    Timber.d("NOTIFICATION_ACTION_REPEAT");
                    break;
                default:
                    break;
            }
            /*int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);*/
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
        conditions = new Conditions(getApplicationContext());
        screenshotObserver = new ScreenshotObserver(this);
    }

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ServiceMain.class);
    }

    public static boolean isRunning(Context context) {
        return ComponentUtil.isServiceRunning(context, ServiceMain.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            IS_RUNNING = true;
            String classname = getClass().getSimpleName();
            LogUtil.logDivider(classname, "#");
            LogUtil.logCentered(" ", classname, "Starting service...");
            LogUtil.logDivider(classname, "#");
            runService();
            return START_STICKY;
        } else {
            Timber.w("onStartCommand: intent == null");
            killService();
            return START_NOT_STICKY;
        }
    }

    private void runService() {
        IS_WORKING = true;
        screenshotObserver.start();
        showNotification(NOTIFICATION_TYPE_PRIMARY, getString(R.string.app_name),
                "Служба запущена");
    }

    @Override
    public void onScreenshotTaken(String filename) {
        final int notificationId = showNotification(NOTIFICATION_TYPE_CANCEL, filename, null);
        new UploadFileTask(DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                Timber.d("onUploadComplete");
                notificationManager.cancel(notificationId);
            }

            @Override
            public void onError(@Nullable Exception e) {
                Timber.e(e == null ? "UploadFileTask: onError" : e.getLocalizedMessage());
            }
        }).execute(filename);
    }

    private void stopService() {
        IS_WORKING = false;
        screenshotObserver.stop();
    }

    private void killService() {
        stopService();
        IS_RUNNING = false;
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cancelUploadReceiver);
        notificationManager.cancelAll();
        wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int showNotification(int type, @NonNull String title, @Nullable String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(type == NOTIFICATION_TYPE_PRIMARY ? R.drawable.ic_cloud_white_24dp :
                        R.drawable.ic_cloud_upload_white_24dp)
                .setContentTitle(title)
                .setOngoing(true);
        Intent intent;
        int notificationId;
        if (text != null) {
            builder.setContentText(text);
        }
        switch (type) {
            case NOTIFICATION_TYPE_PRIMARY:
                notificationId = NOTIFICATION_PRIMARY_ID;
                intent = new Intent(getApplicationContext(), ActivityMain.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                        0, intent, 0));
                break;
            case NOTIFICATION_TYPE_CANCEL:
                lastNotificationId++;
                notificationId = lastNotificationId;
                intent = new Intent(getApplicationContext(), cancelUploadReceiver.getClass());
                intent.setAction(NOTIFICATION_ACTION_CANCEL);
                intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.setProgress(0, 0, true);
                builder.addAction(0, getString(android.R.string.cancel),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
                break;
            case NOTIFICATION_TYPE_REPEAT:
                lastNotificationId++;
                notificationId = lastNotificationId;
                intent = new Intent(getApplicationContext(), cancelUploadReceiver.getClass());
                intent.setAction(NOTIFICATION_ACTION_REPEAT);
                intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.addAction(0, getString(R.string.notification_repeat),
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
                break;
            default:
                return 0;
        }
        notificationManager.notify(notificationId, builder.build());
        return notificationId;
    }
}