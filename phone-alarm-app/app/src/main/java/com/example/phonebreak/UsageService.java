package com.example.phonebreak;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

/**
 * 화면이 켜져 있는 시간을 재는 백그라운드 서비스.
 * - 화면 켜짐 시간이 설정값(기본 60분)에 도달하면 알람을 울리고 카운트를 다시 0부터 시작.
 * - 화면을 설정값(기본 10분) 이상 꺼두면 '쉬었다'고 보고 카운트를 0으로 초기화.
 */
public class UsageService extends Service {
    private static final long TICK_MS = 10_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean screenOn;
    private long lastTick; // elapsedRealtime

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            account();
            if (screenOn) handler.postDelayed(this, TICK_MS);
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) onScreenOff();
            else if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) onScreenOn();
        }
    };

    static void start(Context c) {
        c.startForegroundService(new Intent(c, UsageService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        Notifier.ensureChannels(this);
        SharedPreferences p = Prefs.get(this);
        Prefs.rollDay(p);
        startInForeground();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, f);

        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null && pm.isInteractive()) onScreenOn();
        else onScreenOff();
    }

    private void startInForeground() {
        SharedPreferences p = Prefs.get(this);
        android.app.Notification n = Notifier.status(this,
                p.getLong(Prefs.TODAY_MS, 0), p.getLong(Prefs.SESSION_MS, 0), Prefs.intervalMin(this));
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.ID_STATUS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(Notifier.ID_STATUS, n);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground();
        return START_STICKY;
    }

    private void onScreenOn() {
        if (screenOn) return;
        SharedPreferences p = Prefs.get(this);
        long offAt = p.getLong(Prefs.OFF_AT, 0);
        if (offAt > 0 && System.currentTimeMillis() - offAt >= Prefs.restMin(this) * 60_000L) {
            // 충분히 쉬었으니 연속 사용 시간 초기화
            p.edit().putLong(Prefs.SESSION_MS, 0).apply();
        }
        p.edit().putLong(Prefs.OFF_AT, 0).apply();
        screenOn = true;
        lastTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
        refreshStatus();
    }

    private void onScreenOff() {
        account();
        screenOn = false;
        handler.removeCallbacks(tick);
        Prefs.get(this).edit().putLong(Prefs.OFF_AT, System.currentTimeMillis()).apply();
    }

    /** 지난 측정 이후 흐른 시간을 사용 시간에 더하고, 기준을 넘으면 알람 */
    private void account() {
        if (!screenOn) return;
        long now = SystemClock.elapsedRealtime();
        long delta = Math.max(0, now - lastTick);
        lastTick = now;

        SharedPreferences p = Prefs.get(this);
        Prefs.rollDay(p);
        long today = p.getLong(Prefs.TODAY_MS, 0) + delta;
        long session = p.getLong(Prefs.SESSION_MS, 0) + delta;
        int alarms = p.getInt(Prefs.ALARMS_TODAY, 0);
        int interval = Prefs.intervalMin(this);

        if (session >= interval * 60_000L) {
            Notifier.alarm(this, interval, today);
            session = 0;
            alarms++;
        }
        p.edit().putLong(Prefs.TODAY_MS, today).putLong(Prefs.SESSION_MS, session)
                .putLong(Prefs.DAY_PREFIX + p.getString(Prefs.DAY, Prefs.today()), today)
                .putInt(Prefs.ALARMS_TODAY, alarms).apply();
        refreshStatus();
    }

    private void refreshStatus() {
        SharedPreferences p = Prefs.get(this);
        getSystemService(NotificationManager.class).notify(Notifier.ID_STATUS,
                Notifier.status(this, p.getLong(Prefs.TODAY_MS, 0),
                        p.getLong(Prefs.SESSION_MS, 0), Prefs.intervalMin(this)));
    }

    @Override public void onDestroy() {
        account();
        handler.removeCallbacks(tick);
        try { unregisterReceiver(screenReceiver); } catch (IllegalArgumentException ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
