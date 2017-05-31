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

import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.PermissionsUtil;
import com.shotdrop.utils.Prefs;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import timber.log.Timber;

public class ServiceMain extends Service {

    public static boolean IS_RUNNING;
    public final static int COMMAND_UNKNOWN = -1;
    public final static int COMMAND_STOP = 0;
    public final static int COMMAND_RUN = 1;

    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;

    private static final int PAUSE = 1250;
    private static Queue<String> logcat = new LinkedList<>();
    private Handler logDelay;
    private int logPause = 0;

    private PowerManager.WakeLock wakeLock;

    private boolean wifiIsEnabled;

    private Prefs prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getSimpleName());
        wakeLock.acquire();
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
            showNotification();
            initConditions();
            checkAllConditions();
        } else {
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void showNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_cloud_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(true);
        Intent intent = new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        addMessage("Служба запущена");
    }

    private void addMessage(String text) {
        logcat.add(text);
        refreshNotification();
    }

    private boolean validNotificationConditions() {
        return onlyWhen(Prefs.ENABLE_APPLICATION) && !logcat.isEmpty() && IS_RUNNING;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (builder != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        wakeLock.release();
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

    public void onNetworkState(boolean isWifiEnabled) {
        Timber.i("!!! onNetworkState !!!");
        wifiIsEnabled = isWifiEnabled;
        Timber.i("wifiIsEnabled is %b", wifiIsEnabled);
        addMessage(String.format(Locale.getDefault(),
                "Wifi %s", isWifiEnabled ? "включен" : "выключен"));
        checkAllConditions();
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
                onlyWhen(Prefs.ENABLE_APPLICATION),
                or (
                        and (
                                onlyWhen(Prefs.ENABLE_UPLOAD_ONLY_BY_WIFI),
                                wifiIsEnabled
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

    private void initConditions() {
        Timber.i("--- initConditions ---");
        wifiIsEnabled = ((WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
        Timber.i("wifiIsEnabled is %b", wifiIsEnabled);
    }

    private static boolean or(boolean... booleans) {
        for (boolean bool : booleans) {
            if (bool) {
                return true;
            }
        }
        return false;
    }

    private static boolean and(boolean... booleans) {
        for (boolean bool : booleans) {
            if (!bool) {
                return false;
            }
        }
        return true;
    }

    private boolean onlyWhen(String key) {
        return prefs.getBoolean(key);
    }
}