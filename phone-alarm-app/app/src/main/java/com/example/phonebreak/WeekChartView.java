package com.example.phonebreak;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 최근 7일 사용 시간 막대그래프 */
public class WeekChartView extends View {
    private long[] values = new long[7];      // -1 = 기록 없음
    private String[] labels = new String[7];
    private long averageMs = 0;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WeekChartView(Context c) {
        super(c);
        float d = getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTextSize(11 * d);
        line.setColor(Color.parseColor("#9CA3AF"));
        line.setStrokeWidth(1.5f * d);
        line.setPathEffect(new android.graphics.DashPathEffect(new float[]{6 * d, 4 * d}, 0));
    }

    void setData(long[] values, String[] labels, long averageMs) {
        this.values = values;
        this.labels = labels;
        this.averageMs = averageMs;
        invalidate();
    }

    private static String shortFmt(long ms) {
        long min = ms / 60000;
        if (min < 60) return min + "분";
        long h = min / 60, m = min % 60;
        return m == 0 ? h + "h" : h + "h" + m;
    }

    @Override protected void onDraw(Canvas cv) {
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight();
        float top = 18 * d, bottom = h - 20 * d;
        long max = Math.max(averageMs, 60 * 60_000L);
        for (long v : values) max = Math.max(max, v);
        float slot = w / values.length;
        float bw = Math.min(28 * d, slot * 0.6f);

        for (int i = 0; i < values.length; i++) {
            float cx = slot * i + slot / 2;
            boolean today = i == values.length - 1;
            long v = values[i];
            if (v >= 0) {
                float bh = Math.max(2 * d, (bottom - top) * v / max);
                bar.setColor(today ? Color.parseColor("#E5484D") : Color.parseColor("#F4A3A5"));
                cv.drawRoundRect(new RectF(cx - bw / 2, bottom - bh, cx + bw / 2, bottom), 6 * d, 6 * d, bar);
                txt.setColor(Color.parseColor("#374151"));
                cv.drawText(shortFmt(v), cx, bottom - bh - 4 * d, txt);
            } else {
                txt.setColor(Color.parseColor("#D1D5DB"));
                cv.drawText("-", cx, bottom - 4 * d, txt);
            }
            txt.setColor(today ? Color.parseColor("#E5484D") : Color.GRAY);
            cv.drawText(today ? "오늘" : labels[i], cx, h - 4 * d, txt);
        }
        if (averageMs > 0) {
            float y = bottom - (bottom - top) * averageMs / max;
            cv.drawLine(0, y, w, y, line);
        }
    }
}
