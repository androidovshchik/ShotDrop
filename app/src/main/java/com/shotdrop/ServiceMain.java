package com.shotdrop;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.shotdrop.models.Conditions;
import com.shotdrop.receivers.CancelUploadReceiver;
import com.shotdrop.utils.ComponentUtil;
import com.shotdrop.utils.LogUtil;
import com.shotdrop.utils.ScreenshotObserver;

public class ServiceMain extends Service implements ScreenshotObserver.Callback {

    public static boolean IS_RUNNING = false;
    public static boolean IS_WORKING = false;

    private static final int NOTIFICATION_PRIMARY_ID = 1;
    private int lastNotificationId = NOTIFICATION_PRIMARY_ID;
    private NotificationManager notificationManager;

    private PowerManager.WakeLock wakeLock;

    private Conditions conditions;

    private ScreenshotObserver screenshotObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getString(R.string.app_name));
        wakeLock.acquire();
        notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
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
            killService();
            return START_NOT_STICKY;
        }
    }

    private void runService() {
        IS_WORKING = true;
        screenshotObserver.start();
        showNotification(true, "Служба запущена");
        showNotification(false, "Filename1");
    }

    @Override
    public void onScreenshotTaken(Uri uri) {

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
        notificationManager.cancelAll();
        wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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