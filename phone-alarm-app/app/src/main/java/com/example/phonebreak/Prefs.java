package com.example.phonebreak;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 앱 설정과 사용 시간 기록을 저장하는 곳 */
final class Prefs {
    static final String ENABLED = "enabled";
    static final String INTERVAL_MIN = "interval_min";   // 몇 분 사용하면 알람
    static final String REST_MIN = "rest_min";           // 몇 분 화면을 끄면 휴식으로 인정(카운트 초기화)
    static final String DAY = "day";
    static final String TODAY_MS = "today_ms";           // 오늘 총 사용 시간
    static final String SESSION_MS = "session_ms";       // 마지막 알람/휴식 이후 사용 시간
    static final String ALARMS_TODAY = "alarms_today";
    static final String OFF_AT = "off_at";               // 화면이 꺼진 시각(벽시계)

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("phone_break", Context.MODE_PRIVATE);
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }

    static int intervalMin(Context c) { return get(c).getInt(INTERVAL_MIN, 60); }
    static int restMin(Context c) { return get(c).getInt(REST_MIN, 10); }

    /** 날짜가 바뀌었으면 오늘 기록을 0으로 */
    static void rollDay(SharedPreferences p) {
        String t = today();
        if (!t.equals(p.getString(DAY, ""))) {
            p.edit().putString(DAY, t).putLong(TODAY_MS, 0).putInt(ALARMS_TODAY, 0).apply();
        }
    }

    static String fmt(long ms) {
        long totalMin = ms / 60000;
        long h = totalMin / 60, m = totalMin % 60;
        return h > 0 ? h + "시간 " + m + "분" : m + "분";
    }
}
