package com.example.phonebreak;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 메인 화면: 오늘 사용 시간, 다음 알람까지 남은 시간, 설정 */
public class MainActivity extends Activity {
    private static final int[] INTERVALS = {30, 60, 90, 120};
    private static final int[] RESTS = {5, 10, 15};
    private static final int ACCENT = Color.parseColor("#E5484D");
    private static final int DARK = Color.parseColor("#111827");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView todayText, leftText, alarmsText, weekAvgText, weekCompareText;
    private WeekChartView weekChart;
    private ProgressBar progress;
    private Button toggleBtn;
    private final Button[] intervalBtns = new Button[INTERVALS.length];
    private final Button[] restBtns = new Button[RESTS.length];

    private final Runnable refresher = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 2000); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Notifier.ensureChannels(this);
        setContentView(buildUi());
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override protected void onResume() { super.onResume(); handler.post(refresher); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(refresher); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        l.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        l.setLayoutParams(lp);
        return l;
    }

    private Button plainButton(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        return b;
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(28));
        sv.addView(root);

        root.addView(text("📵 폰 휴식 알람", 26, true, DARK));
        root.addView(text("핸드폰을 오래 쓰면 쉬라고 알려드려요", 14, false, Color.GRAY));

        // 오늘 사용 현황
        LinearLayout stat = card();
        stat.addView(text("오늘 사용 시간", 14, false, Color.GRAY));
        todayText = text("-", 34, true, DARK);
        stat.addView(todayText);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(14));
        plp.topMargin = dp(10);
        stat.addView(progress, plp);
        leftText = text("-", 15, false, Color.parseColor("#374151"));
        stat.addView(leftText);
        alarmsText = text("-", 13, false, Color.GRAY);
        stat.addView(alarmsText);
        root.addView(stat);

        // 주간 통계
        LinearLayout week = card();
        week.addView(text("📊 최근 7일 통계", 15, true, DARK));
        weekAvgText = text("-", 22, true, DARK);
        week.addView(weekAvgText);
        weekCompareText = text("-", 13, false, Color.GRAY);
        week.addView(weekCompareText);
        weekChart = new WeekChartView(this);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, dp(170));
        wlp.topMargin = dp(10);
        week.addView(weekChart, wlp);
        week.addView(text("점선 = 하루 평균", 11, false, Color.GRAY));
        root.addView(week);

        // 켜기/끄기
        toggleBtn = plainButton("");
        toggleBtn.setTextSize(18);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, dp(60));
        tlp.topMargin = dp(14);
        root.addView(toggleBtn, tlp);
        toggleBtn.setOnClickListener(v -> toggle());

        // 알람 간격
        LinearLayout c1 = card();
        c1.addView(text("⏰ 몇 분 사용하면 알람을 울릴까요?", 15, true, DARK));
        c1.addView(choiceRow(INTERVALS, intervalBtns, Prefs.INTERVAL_MIN));
        root.addView(c1);

        // 휴식 인정 기준
        LinearLayout c2 = card();
        c2.addView(text("😌 화면을 몇 분 꺼두면 '쉬었다'고 볼까요?", 15, true, DARK));
        c2.addView(text("이만큼 화면을 끄면 연속 사용 시간이 0부터 다시 시작돼요", 12, false, Color.GRAY));
        c2.addView(choiceRow(RESTS, restBtns, Prefs.REST_MIN));
        root.addView(c2);

        // 기타
        LinearLayout c3 = card();
        Button test = plainButton("🔔 알람 미리 울려보기");
        test.setOnClickListener(v -> Notifier.alarm(this, Prefs.intervalMin(this),
                Prefs.get(this).getLong(Prefs.TODAY_MS, 0)));
        c3.addView(test);
        Button battery = plainButton("🔋 배터리 최적화에서 빼기 (꺼짐 방지)");
        battery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                Toast.makeText(this, "목록에서 '폰 휴식 알람'을 '제한 없음/최적화 안 함'으로 바꿔주세요",
                        Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        c3.addView(battery);
        c3.addView(text("※ 측정을 켠 이후 화면이 켜져 있던 시간만 계산해요.", 12, false, Color.GRAY));
        root.addView(c3);
        return sv;
    }

    private LinearLayout choiceRow(int[] values, Button[] btns, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(8), 0, 0);
        for (int i = 0; i < values.length; i++) {
            final int val = values[i];
            Button b = plainButton(val + "분");
            b.setOnClickListener(v -> {
                Prefs.get(this).edit().putInt(key, val).apply();
                refresh();
            });
            btns[i] = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(b, lp);
        }
        return row;
    }

    private void paintChoices(int[] values, Button[] btns, int selected) {
        for (int i = 0; i < values.length; i++) {
            boolean on = values[i] == selected;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(on ? ACCENT : Color.parseColor("#E5E7EB"));
            btns[i].setBackground(bg);
            btns[i].setTextColor(on ? Color.WHITE : Color.parseColor("#374151"));
        }
    }

    private void toggle() {
        SharedPreferences p = Prefs.get(this);
        boolean enabled = !p.getBoolean(Prefs.ENABLED, false);
        p.edit().putBoolean(Prefs.ENABLED, enabled).apply();
        if (enabled) {
            UsageService.start(this);
            Toast.makeText(this, "측정을 시작했어요", Toast.LENGTH_SHORT).show();
        } else {
            stopService(new Intent(this, UsageService.class));
            Toast.makeText(this, "측정을 멈췄어요", Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    /** 최근 7일(오늘 포함) 평균과 그 전 7일 평균 비교 */
    private void refreshWeek(SharedPreferences p) {
        long[] vals = new long[7];
        String[] labels = new String[7];
        long sum = 0;
        int days = 0;
        for (int i = 0; i < 7; i++) {
            int offset = i - 6;               // -6(6일 전) ~ 0(오늘)
            vals[i] = Prefs.usageOn(p, offset);
            labels[i] = Prefs.weekday(offset);
            if (vals[i] >= 0) { sum += vals[i]; days++; }
        }
        long avg = days > 0 ? sum / days : 0;
        weekChart.setData(vals, labels, avg);
        weekAvgText.setText("하루 평균 " + Prefs.fmt(avg));

        long prevSum = 0;
        int prevDays = 0;
        for (int offset = -13; offset <= -7; offset++) {
            long v = Prefs.usageOn(p, offset);
            if (v >= 0) { prevSum += v; prevDays++; }
        }
        String info = "기록된 " + days + "일 기준 · 7일 합계 " + Prefs.fmt(sum);
        if (prevDays > 0) {
            long prevAvg = prevSum / prevDays;
            long diff = avg - prevAvg;
            if (Math.abs(diff) < 60_000) info += "\n지난주와 비슷해요";
            else if (diff < 0) info += "\n지난주보다 하루 " + Prefs.fmt(-diff) + " 줄었어요 👍";
            else info += "\n지난주보다 하루 " + Prefs.fmt(diff) + " 늘었어요 😥";
        } else {
            info += "\n지난주 기록이 쌓이면 비교해 드려요";
        }
        weekCompareText.setText(info);
    }

    private void refresh() {
        SharedPreferences p = Prefs.get(this);
        Prefs.rollDay(p);
        boolean enabled = p.getBoolean(Prefs.ENABLED, false);
        int interval = Prefs.intervalMin(this);
        long today = p.getLong(Prefs.TODAY_MS, 0);
        long session = p.getLong(Prefs.SESSION_MS, 0);
        long left = Math.max(0, interval * 60_000L - session);

        todayText.setText(Prefs.fmt(today));
        progress.setProgress((int) Math.min(1000, session * 1000 / (interval * 60_000L)));
        leftText.setText(enabled
                ? "연속 " + Prefs.fmt(session) + " 사용 · 다음 알람까지 " + Prefs.fmt(left + 59_999)
                : "측정이 꺼져 있어요. 아래 버튼을 눌러 시작하세요.");
        alarmsText.setText("오늘 울린 알람: " + p.getInt(Prefs.ALARMS_TODAY, 0) + "번");

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(enabled ? Color.parseColor("#6B7280") : ACCENT);
        toggleBtn.setBackground(bg);
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setText(enabled ? "⏸ 측정 멈추기" : "▶ 측정 시작하기");

        refreshWeek(p);
        paintChoices(INTERVALS, intervalBtns, interval);
        paintChoices(RESTS, restBtns, Prefs.restMin(this));
    }
}
