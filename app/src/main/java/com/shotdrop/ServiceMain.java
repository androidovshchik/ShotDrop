package com.shotdrop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

import com.shotdrop.dropbox.DropboxClientFactory;
import com.shotdrop.dropbox.UploadFileRequest;
import com.shotdrop.observers.ScreenshotCallback;
import com.shotdrop.utils.ConditionsUtil;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.Prefs;
import com.shotdrop.observers.FileObserverClass;

import timber.log.Timber;

public class ServiceMain extends Service implements ScreenshotCallback, UploadFileRequest.Callback {

    public static final String NOTIFICATION_ACTION_SCREENSHOT = "com.shotdrop.broadcast.screenshot";
    private static final String NOTIFICATION_ACTION_REPEAT = "com.shotdrop.broadcast.repeat";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_NOTIFICATION_ID = "notificationId";
    private static final int NOTIFICATION_TYPE_PRIMARY_START = 1;
    private static final int NOTIFICATION_TYPE_PRIMARY_UPDATE = 2;
    private static final int NOTIFICATION_TYPE_UPLOAD = 3;
    private static final int NOTIFICATION_TYPE_REPEAT = 4;
    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private ClipboardManager clipboard;
    private Handler toastHandler;

    private ConditionsUtil conditions;

    private UploadFileRequest request;

    private FileObserverClass fileObserver;

    private Prefs prefs;

    private boolean hasWorkingRequest = false;

    private BroadcastReceiver cancelUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
            String filename = intent.getStringExtra(KEY_FILENAME);
            switch (intent.getAction()) {
                case NOTIFICATION_ACTION_SCREENSHOT:
                    Timber.d("NOTIFICATION_ACTION_SCREENSHOT");
                    onScreenshotTaken(filename);
                    break;
                case NOTIFICATION_ACTION_REPEAT:
                    Timber.d("NOTIFICATION_ACTION_REPEAT");
                    notificationManager.cancel(notificationId);
                    onScreenshotTaken(filename);
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
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        toastHandler = new Handler();
        notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATION_ACTION_REPEAT);
        intentFilter.addAction(NOTIFICATION_ACTION_SCREENSHOT);
        registerReceiver(cancelUploadReceiver, intentFilter);
        conditions = new ConditionsUtil(getApplicationContext());
        request = new UploadFileRequest(getApplicationContext(), DropboxClientFactory
                .getClient(getApplicationContext()), this);
        prefs = new Prefs(getApplicationContext());
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
            if (prefs.isClassNotificationListenerClass()) {

            }
            showNotification(NOTIFICATION_TYPE_PRIMARY_START, getString(R.string.app_name),
                    "Служба запущена", null);
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onScreenshotTaken(String filename) {
        if (!conditions.checkOptional()) {
            showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                    "Остановлен по опциональным условиям", null);
            return;
        }
        if (!prefs.enabledMultiTasks()) {
            if (hasWorkingRequest) {
                showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                        "Пропущен скриншот", null);
                return;
            }
            hasWorkingRequest = true;
        }
        showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                "К загрузке " + filename, null);
        int notificationId = showNotification(NOTIFICATION_TYPE_UPLOAD, filename,
                "Идет загрузка...", null);
        request.enqueue(notificationId, filename);
    }

    @Override
    public void onSuccess(int notificationId, String filename, String url) {
        notificationManager.cancel(notificationId);
        ClipData clip = ClipData.newPlainText("", url);
        clipboard.setPrimaryClip(clip);
        showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                "Загружен и скопирован " + filename, null);
        if (!prefs.enabledMultiTasks()) {
            hasWorkingRequest = false;
        }
        toastHandler.post(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "ShotDrop++ ссылка cкопирована", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
                toast.show();
            }
        });
    }

    @Override
    public void onError(int notificationId, String filename, String message) {
        notificationManager.cancel(notificationId);
        showNotification(NOTIFICATION_TYPE_REPEAT, filename,
                "Не удалось загрузить ¯\\(ツ)/¯", null);
        showNotification(NOTIFICATION_TYPE_PRIMARY_UPDATE, getString(R.string.app_name),
                message, null);
        if (!prefs.enabledMultiTasks()) {
            hasWorkingRequest = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (prefs.isClassFileObserver() && fileObserver != null) {
            fileObserver.stop();
        }
        if (prefs.isClassNotificationListenerClass()) {

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
            case NOTIFICATION_TYPE_UPLOAD:
                if (prevNotificationId != null) {
                    newNotificationId = prevNotificationId;
                } else {
                    lastNotificationId++;
                    newNotificationId = lastNotificationId;
                }
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.setOngoing(true);
                builder.setProgress(0, 0, true);
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
                break;
            default:
                return 0;
        }
        notificationManager.notify(newNotificationId, builder.build());
        return newNotificationId;
    }
}