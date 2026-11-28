package com.example.phonebreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 폰을 재부팅하거나 앱을 업데이트해도 측정이 켜져 있었으면 자동으로 다시 시작 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Prefs.get(c).getBoolean(Prefs.ENABLED, false)) {
            try { UsageService.start(c); } catch (RuntimeException ignored) {}
        }
    }
}
