package com.shotdrop;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.shotdrop.receivers.CancelUploadReceiver;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.PermissionsUtil;
import com.shotdrop.utils.Prefs;
import com.shotdrop.utils.ScreenshotObserver;

import java.util.Locale;

import timber.log.Timber;

public class ServiceMain extends Service implements ScreenshotObserver.Callback {

    public static boolean IS_RUNNING;
    public static boolean IS_WORKING;

    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private Prefs prefs;

    private ScreenshotObserver screenshotObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getSimpleName());
        wakeLock.acquire();
        notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = new Prefs(getApplicationContext());
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
        if (!PermissionsUtil.hasAllPermissions(getApplicationContext())) {
            Timber.w("Not having permissions");
            return START_NOT_STICKY;
        } else {
            Timber.i("Having permissions");
        }
        if (!onlyWhen(Prefs.ENABLE_APPLICATION)) {
            Timber.w("Application disabled");
            return START_NOT_STICKY;
        } else {
            Timber.i("Application enabled");
        }
        if (intent != null) {
            IS_RUNNING = true;
            String classname = getClass().getSimpleName();
            LogUtil.logDivider(classname, "#");
            LogUtil.logCentered(" ", classname, "Start service...");
            LogUtil.logDivider(classname, "#");
            showNotification(true, "Служба запущена");
            showNotification(false, "Filename1");
            initConditions();
            checkAllConditions();
        } else {
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void refreshNotification() {
        /*if (validNotificationConditions()) {
            if (builder == null) {
                showNotification();
            }
            logDelay.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (NotificationUtil.log.isEmpty()) {
                        return;
                    }
                    NotificationUtil.logPause -= NotificationUtil.PAUSE;
                    String nextText = NotificationUtil.log.poll();
                    if (nextText.equals(FOUND_IMAGE)) {
                        NotificationUtil.foundCount = 0;
                        builder.setContentText(String.format(Locale.getDefault(), FOUND_IMAGE_FORMAT,
                                NotificationUtil.foundCount, NotificationUtil
                                        .getEnding(NotificationUtil.foundCount, NotificationUtil
                                                .ENDING_IMAGES)));
                    } else {
                        builder.setContentText(nextText);
                    }
                    notificationManager.notify(NotificationUtil.ID, builder.build());
                }
            }, NotificationUtil.logPause);
            NotificationUtil.logPause += NotificationUtil.PAUSE;
        }*/
    }

    private void stopService() {
        addMessage("Прерывание службы");
        Timber.i("!!! Stopping service !!!");
        IS_RUNNING = false;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.i("!!! Service stopped !!!");
                stopForeground(true);
                stopSelf();
            }
        }, 5000);
    }

    private boolean checkAllConditions() {
        boolean hasAllConditions =  and (
                PermissionsUtil.hasAllPermissions(getApplicationContext()),
                onlyWhen(Prefs.ENABLE_DROPBOX_ACCOUNT),
                onlyWhen(Prefs.ENABLE_APPLICATION),
                or (
                        and (
                                onlyWhen(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI),
                                ((WifiManager) getApplicationContext()
                                        .getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()
                        ),
                        !onlyWhen(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI)
                )
        );
        Timber.i("*/*/*/* hasAllConditions is %b /*/*/*/", hasAllConditions);
        if (!hasAllConditions) {
            stop();
        } else {
            run();
        }
        return hasAllConditions;
    }

    private void rerun() {
        stop();
        run();
    }

    private void stop() {
        IS_RUNNING = false;
    }

    private void run() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancelAll();
        wakeLock.release();
    }

    private void showNotification(boolean primary, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(primary ? R.drawable.ic_cloud_white_24dp :
                        R.drawable.ic_cloud_upload_white_24dp)
                .setContentTitle(primary ? getString(R.string.app_name) : text)
                .setAutoCancel(false);
        int notificationId;
        Intent intent;
        if (primary) {
            notificationId = NOTIFICATION_PRIMARY_ID;
            intent = new Intent(getApplicationContext(), ActivityMain.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder.setContentText(text);
            builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                    0, intent, 0));
        } else {
            lastNotificationId++;
            notificationId = lastNotificationId;
            intent = new Intent(getApplicationContext(), CancelUploadReceiver.class);
            intent.putExtra(CancelUploadReceiver.KEY_NOTIFICATION_ID, notificationId);
            builder.addAction(0, getString(android.R.string.cancel),
                    PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
        }
        notificationManager.notify(notificationId, builder.build());
    }
}