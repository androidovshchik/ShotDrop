package com.shotdrop;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceMain extends Service {
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private static final AccessType ACCESS_TYPE;
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String APP_KEY = "7yv7zegteoa832s";
    private static final String APP_SECRET = "muuhnwpj628elam";
    public static FileObserver observer;
    private final int STATUS_BAR_NOTIFICATION;
    private final String Screenshots_DIR;
    public final String TAG;
    private Context context;
    SharedPreferences data;
    DropboxAPI<AndroidAuthSession> mApi;
    private Context mContext;
    private String mErrorMsg;
    private long mFileLen;
    private boolean mLoggedIn;
    private UploadRequest mRequest;
    private boolean mRun;
    private NotificationManager nm;
    private Notification noti;
    int time;

    /* renamed from: com.humayoun.shotdropfree.BackgroundService.1 */
    class C01361 extends TimerTask {
        C01361() {
        }

        public void run() {
            Calendar c = Calendar.getInstance();
            long now = c.getTimeInMillis();
            c.set(11, 0);
            c.set(12, 0);
            c.set(13, 0);
            c.set(14, 0);
            long secondsPassed = (now - c.getTimeInMillis()) / 1000;
            Log.d("timer", "time is" + secondsPassed);
            if (secondsPassed > 86200) {
                BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                Editor editor3 = BackgroundService.this.data.edit();
                editor3.putInt("total", 5);
                editor3.commit();
            }
        }
    }

    /* renamed from: com.humayoun.shotdropfree.BackgroundService.2 */
    class C01372 extends FileObserver {
        private final /* synthetic */ ClipboardManager val$clipboard;
        private final /* synthetic */ String val$pathToWatch;

        C01372(String $anonymous0, String str, ClipboardManager clipboardManager) {
            this.val$pathToWatch = str;
            this.val$clipboard = clipboardManager;
            super($anonymous0);
        }

        @SuppressLint({"ShowToast"})
        public void onEvent(int event, String filename) {
            if (event == 8) {
                Log.d("DEBUG", "File created [" + this.val$pathToWatch + filename + "]");
                BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                if (BackgroundService.this.data.getInt("total", 100) > 0) {
                    File file = new File(this.val$pathToWatch, filename);
                    try {
                        Log.d("ready to upload", "File  [" + this.val$pathToWatch + filename + "]");
                        FileInputStream fis = new FileInputStream(file);
                        String path = "/Screenshots (ShotDrop)/" + file.getName();
                        BackgroundService.this.mRequest = BackgroundService.this.mApi.putFileOverwriteRequest(path, fis, file.length(), null);
                        PendingIntent pi;
                        Notification access$4;
                        RemoteViews contentView;
                        CharSequence charSequence;
                        if (BackgroundService.this.isinternetavailable()) {
                            BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                            long when;
                            Entry ent;
                            String shareAddress;
                            long when2;
                            int total;
                            Editor editor3;
                            if (BackgroundService.this.data.getBoolean("wificheckbox", true)) {
                                Log.d("wifi", "wif pref true");
                                if (BackgroundService.this.iswifiavailable()) {
                                    Log.d("wifi", "wifi is available");
                                    if (BackgroundService.this.mRequest != null) {
                                        Log.d(" uploading.......", "File  [" + this.val$pathToWatch + filename + "]");
                                        when = System.currentTimeMillis();
                                        BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, "Uploading screenshot to Dropbox..", when);
                                        pi = PendingIntent.getService(BackgroundService.this.context, 0, new Intent(BackgroundService.this.context, BackgroundService.class), 0);
                                        access$4 = BackgroundService.this.noti;
                                        access$4.flags |= 16;
                                        contentView = new RemoteViews(BackgroundService.this.getPackageName(), C0142R.layout.noti);
                                        contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                                        contentView.setTextViewText(C0142R.id.status_text, "Uploading screenshot to Dropbox..");
                                        contentView.setProgressBar(C0142R.id.status_progress, 100, 0, true);
                                        BackgroundService.this.noti.contentView = contentView;
                                        BackgroundService.this.noti.contentIntent = pi;
                                        BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                                        BackgroundService.this.mRequest.upload();
                                        Log.d(" upload done", "File  [" + this.val$pathToWatch + filename + "]");
                                        ent = BackgroundService.this.mApi.metadata(path, 1000, null, true, null);
                                        if (!ent.isDir) {
                                            shareAddress = BackgroundService.this.mApi.share(ent.path).url;
                                            Log.d("url", "dropbox share link " + shareAddress);
                                            this.val$clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot shareAddress", shareAddress));
                                            Log.d(" Clipboard copied", "File ");
                                            when2 = System.currentTimeMillis();
                                            BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, "Dropbox screenshot link copied to clipboard", when);
                                            access$4 = BackgroundService.this.noti;
                                            access$4.flags |= 16;
                                            contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                                            contentView.setTextViewText(C0142R.id.status_text, "Screenshot successfully uploaded to Dropbox");
                                            contentView.setProgressBar(C0142R.id.status_progress, 100, 100, false);
                                            BackgroundService.this.noti.contentView = contentView;
                                            BackgroundService.this.noti.contentIntent = pi;
                                            BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                                            BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                                            total = BackgroundService.this.data.getInt("total", 100) - 1;
                                            BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                                            editor3 = BackgroundService.this.data.edit();
                                            editor3.putInt("total", total);
                                            editor3.commit();
                                            return;
                                        }
                                        return;
                                    }
                                    return;
                                }
                                Log.d("Wifi", "wifi not availble");
                                charSequence = "WiFi not connected";
                                BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, tickerText, System.currentTimeMillis());
                                pi = PendingIntent.getService(BackgroundService.this.context, 0, new Intent(BackgroundService.this.context, BackgroundService.class), 0);
                                access$4 = BackgroundService.this.noti;
                                access$4.flags |= 16;
                                contentView = new RemoteViews(BackgroundService.this.getPackageName(), C0142R.layout.noti);
                                contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                                contentView.setTextViewText(C0142R.id.status_text, "WiFi not connected.\nPlease check your WiFi connection");
                                contentView.setProgressBar(C0142R.id.status_progress, 100, 0, false);
                                BackgroundService.this.noti.contentView = contentView;
                                BackgroundService.this.noti.contentIntent = pi;
                                BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                                BackgroundService.this.nm.cancel(1);
                                return;
                            } else if (BackgroundService.this.mRequest != null) {
                                Log.d(" uploading.......", "File  [" + this.val$pathToWatch + filename + "]");
                                when = System.currentTimeMillis();
                                BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, "Uploading screenshot to Dropbox..", when);
                                pi = PendingIntent.getService(BackgroundService.this.context, 0, new Intent(BackgroundService.this.context, BackgroundService.class), 0);
                                access$4 = BackgroundService.this.noti;
                                access$4.flags |= 16;
                                contentView = new RemoteViews(BackgroundService.this.getPackageName(), C0142R.layout.noti);
                                contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                                contentView.setTextViewText(C0142R.id.status_text, "Uploading screenshot to Dropbox..");
                                contentView.setProgressBar(C0142R.id.status_progress, 100, 0, true);
                                BackgroundService.this.noti.contentView = contentView;
                                BackgroundService.this.noti.contentIntent = pi;
                                BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                                BackgroundService.this.mRequest.upload();
                                Log.d(" upload done", "File  [" + this.val$pathToWatch + filename + "]");
                                ent = BackgroundService.this.mApi.metadata(path, 1000, null, true, null);
                                if (!ent.isDir) {
                                    shareAddress = BackgroundService.this.mApi.share(ent.path).url;
                                    Log.d("url", "dropbox share link " + shareAddress);
                                    this.val$clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot shareAddress", shareAddress));
                                    Log.d(" Clipboard copied", "File ");
                                    when2 = System.currentTimeMillis();
                                    BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, "Dropbox screenshot link copied to clipboard", when);
                                    access$4 = BackgroundService.this.noti;
                                    access$4.flags |= 16;
                                    contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                                    contentView.setTextViewText(C0142R.id.status_text, "Screenshot successfully uploaded to Dropbox");
                                    contentView.setProgressBar(C0142R.id.status_progress, 100, 100, false);
                                    BackgroundService.this.noti.contentView = contentView;
                                    BackgroundService.this.noti.contentIntent = pi;
                                    BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                                    BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                                    total = BackgroundService.this.data.getInt("total", 100) - 1;
                                    BackgroundService.this.data = BackgroundService.this.getSharedPreferences("settings", 0);
                                    editor3 = BackgroundService.this.data.edit();
                                    editor3.putInt("total", total);
                                    editor3.commit();
                                    return;
                                }
                                return;
                            } else {
                                return;
                            }
                        }
                        charSequence = "No Internet connection";
                        BackgroundService.this.noti = new Notification(C0142R.drawable.ic_launcher, tickerText, System.currentTimeMillis());
                        pi = PendingIntent.getService(BackgroundService.this.context, 0, new Intent(BackgroundService.this.context, BackgroundService.class), 0);
                        access$4 = BackgroundService.this.noti;
                        access$4.flags |= 16;
                        contentView = new RemoteViews(BackgroundService.this.getPackageName(), C0142R.layout.noti);
                        contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
                        contentView.setTextViewText(C0142R.id.status_text, "Error.\n No Internet conection");
                        contentView.setProgressBar(C0142R.id.status_progress, 100, 0, false);
                        BackgroundService.this.noti.contentView = contentView;
                        BackgroundService.this.noti.contentIntent = pi;
                        BackgroundService.this.nm.notify(1, BackgroundService.this.noti);
                        BackgroundService.this.nm.cancel(1);
                        return;
                    } catch (DropboxUnlinkedException e) {
                        BackgroundService.this.mErrorMsg = "This app wasn't authenticated properly.";
                        return;
                    } catch (DropboxFileSizeException e2) {
                        BackgroundService.this.mErrorMsg = "This file is too big to upload";
                        return;
                    } catch (DropboxPartialFileException e3) {
                        BackgroundService.this.mErrorMsg = "Upload canceled";
                        return;
                    } catch (DropboxServerException e4) {
                        if (!(e4.error == DropboxServerException._401_UNAUTHORIZED || e4.error == DropboxServerException._403_FORBIDDEN || e4.error == DropboxServerException._404_NOT_FOUND)) {
                            int i = e4.error;
                        }
                        BackgroundService.this.mErrorMsg = e4.body.userError;
                        if (BackgroundService.this.mErrorMsg == null) {
                            BackgroundService.this.mErrorMsg = e4.body.error;
                            return;
                        }
                        return;
                    } catch (DropboxIOException e5) {
                        BackgroundService.this.mErrorMsg = "Network error.  Try again.";
                        Log.d("error", "Network error.  Try again");
                        BackgroundService.this.showToast("Network error.  Try again");
                        return;
                    } catch (DropboxParseException e6) {
                        BackgroundService.this.mErrorMsg = "Dropbox error.  Try again.";
                        return;
                    } catch (DropboxException e7) {
                        BackgroundService.this.mErrorMsg = "Unknown error.  Try again.";
                        return;
                    } catch (FileNotFoundException e8) {
                        return;
                    }
                }
                BackgroundService.this.runlimittimer();
                NotificationManager notificationManager = (NotificationManager) BackgroundService.this.context.getSystemService("notification");
                Notification notification = new Notification(C0142R.drawable.ic_launcher, "You've reached your screenshot upload limit for the day.Buy ShotDrop Pro to remove it. :)", System.currentTimeMillis());
                notification.flags |= 16;
                notification.defaults |= 4;
                PendingIntent pendingIntent = PendingIntent.getActivity(BackgroundService.this.context, 0, new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.humayoun.shotdrop")), 0);
                notification.setLatestEventInfo(BackgroundService.this.context, "Buy ShotDrop Pro", "Screenshot upload limit for the Day reached.\nBuy ShotDrop Pro to remove it. :)", pendingIntent);
                notificationManager.notify(0, notification);
            }
        }
    }

    public BackgroundService() {
        this.TAG = "DEBUG";
        this.Screenshots_DIR = "/Screenshots (ShotDrop)/";
        this.STATUS_BAR_NOTIFICATION = 1;
        this.time = 0;
    }

    static {
        ACCESS_TYPE = AccessType.DROPBOX;
    }

    @SuppressLint({"NewApi"})
    public void onCreate() {
        super.onCreate();
        this.nm = (NotificationManager) getSystemService("notification");
        this.noti = new Notification(C0142R.drawable.ic_launcher, "Uploading screenshot to Dropbox..", System.currentTimeMillis());
        this.context = getApplicationContext();
        PendingIntent pi = PendingIntent.getService(this.context, 0, new Intent(this.context, BackgroundService.class), 0);
        Notification notification = this.noti;
        notification.flags |= 16;
        RemoteViews contentView = new RemoteViews(getPackageName(), C0142R.layout.noti);
        contentView.setImageViewResource(C0142R.id.status_icon, C0142R.drawable.ic_launcher);
        contentView.setTextViewText(C0142R.id.status_text, "Uploading screenshot to Dropbox..");
        contentView.setProgressBar(C0142R.id.status_progress, 100, 0, false);
        this.noti.contentView = contentView;
        this.noti.contentIntent = pi;
    }

    public void onDestroy() {
        this.data = getSharedPreferences("settings", 0);
        Editor editor3 = this.data.edit();
        editor3.putBoolean("enablecheckbox", false);
        editor3.commit();
        showToast("ShotDrop disabled ");
        super.onDestroy();
    }

    public void onStart(Intent intent, int startid) {
    }

    boolean iswifiavailable() {
        if (((ConnectivityManager) getSystemService("connectivity")).getNetworkInfo(1).isConnected()) {
            return true;
        }
        return false;
    }

    boolean isinternetavailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        NetworkInfo wifi1 = cm.getNetworkInfo(1);
        boolean mobileinternet = cm.getNetworkInfo(0).isConnected();
        if (wifi1.isConnected() || mobileinternet) {
            return true;
        }
        return false;
    }

    void runlimittimer() {
        new Timer().scheduleAtFixedRate(new C01361(), 0, 100000);
    }

    @SuppressLint({"NewApi"})
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DEBUG", "onStartCommand... now background service will run");
        this.nm = (NotificationManager) getSystemService("notification");
        this.mApi = new DropboxAPI(buildSession());
        checkAppKeySetup();
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        Log.d("DEBUG", "onStart");
        String pathToWatch = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots").toString();
        observer = new C01372(pathToWatch, pathToWatch, clipboard);
        observer.startWatching();
        return super.onStartCommand(intent, flags, startId);
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void checkAppKeySetup() {
        if (APP_KEY.startsWith("CHANGE") || APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            return;
        }
        Intent testIntent = new Intent("android.intent.action.VIEW");
        String scheme = "db-7yv7zegteoa832s";
        testIntent.setData(Uri.parse(new StringBuilder(String.valueOf(scheme)).append("://").append(1).append("/test").toString()));
        if (getPackageManager().queryIntentActivities(testIntent, 0).size() == 0) {
            showToast("URL scheme in your app's manifest is not set up correctly. You should have a com.dropbox.client2.android.AuthActivity with the scheme: " + scheme);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, 1).show();
    }
}
