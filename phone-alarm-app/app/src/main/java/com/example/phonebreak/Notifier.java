package com.example.phonebreak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

/** 알림 채널 만들기 + 알림 띄우기 */
final class Notifier {
    static final String CH_STATUS = "status";
    static final String CH_ALARM = "break_alarm";
    static final int ID_STATUS = 1;
    static final int ID_ALARM = 2;
    static final long[] VIBRATE = {0, 600, 300, 600, 300, 900};

    private Notifier() {}

    static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel status = new NotificationChannel(
                CH_STATUS, "사용 시간 측정 중", NotificationManager.IMPORTANCE_LOW);
        status.setDescription("상단에 오늘 사용 시간을 계속 보여줍니다");
        status.setShowBadge(false);
        nm.createNotificationChannel(status);

        NotificationChannel alarm = new NotificationChannel(
                CH_ALARM, "휴식 알람", NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription("정해진 시간만큼 사용하면 울립니다");
        alarm.enableVibration(true);
        alarm.setVibrationPattern(VIBRATE);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        alarm.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        nm.createNotificationChannel(alarm);
    }

    private static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static Notification status(Context c, long todayMs, long sessionMs, int intervalMin) {
        long leftMs = Math.max(0, intervalMin * 60000L - sessionMs);
        return new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("오늘 " + Prefs.fmt(todayMs) + " 사용")
                .setContentText("다음 휴식 알람까지 " + Prefs.fmt(leftMs + 59999))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp(c))
                .build();
    }

    static void alarm(Context c, int usedMin, long todayMs) {
        ensureChannels(c);
        String used = usedMin >= 60 && usedMin % 60 == 0 ? (usedMin / 60) + "시간" : usedMin + "분";
        Notification n = new Notification.Builder(c, CH_ALARM)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("📵 핸드폰을 " + used + " 사용했어요!")
                .setContentText("잠깐 폰을 내려놓고 눈과 목을 쉬어주세요.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "핸드폰을 " + used + " 연속으로 사용했어요.\n"
                        + "폰을 내려놓고 먼 곳을 보거나 스트레칭을 해보세요.\n"
                        + "오늘 총 사용 시간: " + Prefs.fmt(todayMs)))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openApp(c))
                .build();
        c.getSystemService(NotificationManager.class).notify(ID_ALARM, n);
    }
}
