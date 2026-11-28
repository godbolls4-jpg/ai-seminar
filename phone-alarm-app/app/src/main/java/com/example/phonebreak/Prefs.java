package com.example.phonebreak;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    static final String DAY_PREFIX = "d_";               // 날짜별 사용 시간 기록 (d_2026-11-28)
    private static final int KEEP_DAYS = 90;

    /** 날짜가 바뀌었으면 어제 기록을 보관하고 오늘 기록을 0으로 */
    static void rollDay(SharedPreferences p) {
        String t = today();
        String prev = p.getString(DAY, "");
        if (!t.equals(prev)) {
            SharedPreferences.Editor e = p.edit();
            if (!prev.isEmpty()) e.putLong(DAY_PREFIX + prev, p.getLong(TODAY_MS, 0));
            e.putString(DAY, t).putLong(TODAY_MS, 0).putInt(ALARMS_TODAY, 0);
            // 90일 지난 기록은 정리
            String cutoff = dateKey(-KEEP_DAYS);
            for (String k : p.getAll().keySet()) {
                if (k.startsWith(DAY_PREFIX) && k.substring(DAY_PREFIX.length()).compareTo(cutoff) < 0) {
                    e.remove(k);
                }
            }
            e.apply();
        }
    }

    /** 오늘 기준 offset일 전/후 날짜 (예: -1 = 어제) */
    static String dateKey(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(c.getTime());
    }

    /** 해당 날짜의 요일 한 글자 (월, 화, ...) */
    static String weekday(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new String[]{"일", "월", "화", "수", "목", "금", "토"}[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /** 날짜별 사용 시간. 기록이 없으면 -1 */
    static long usageOn(SharedPreferences p, int offsetDays) {
        if (offsetDays == 0) return p.getLong(TODAY_MS, 0);
        return p.getLong(DAY_PREFIX + dateKey(offsetDays), -1);
    }

    static String fmt(long ms) {
        long totalMin = ms / 60000;
        long h = totalMin / 60, m = totalMin % 60;
        return h > 0 ? h + "시간 " + m + "분" : m + "분";
    }
}
