package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/**
 * Minimal analog dial: a thin rim, 60 ticks and three hands, drawn with Canvas.
 *
 * No bitmaps, no font work, a handful of Paint objects. The view redraws once per second while it is
 * attached (the screensaver is on), and not at all otherwise. The small invalidate region is the
 * dial only, so the cost is far below one frame of video.
 */
public class AnalogClockView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Calendar calendar = Calendar.getInstance();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            invalidate();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 1000 - (now % 1000));   // land on the next whole second
        }
    };

    public AnalogClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f - dp(6);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x33FFFFFF);
        canvas.drawCircle(cx, cy, r, paint);

        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(i * 6 - 90);
            boolean major = i % 5 == 0;
            float inner = r - (major ? dp(13) : dp(7));
            paint.setStrokeWidth(major ? dp(1.6f) : dp(1));
            paint.setColor(major ? 0xCCFFFFFF : 0x3DFFFFFF);
            canvas.drawLine(cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner,
                    cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, paint);
        }

        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        hand(canvas, cx, cy, (hour + minute / 60f) * 30f, r * 0.48f, dp(4.5f), 0xFFFFFFFF);
        hand(canvas, cx, cy, (minute + second / 60f) * 6f, r * 0.70f, dp(3f), 0xFFFFFFFF);
        hand(canvas, cx, cy, second * 6f, r * 0.80f, dp(1.4f), 0xFFFF9F0A);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFF9F0A);
        canvas.drawCircle(cx, cy, dp(3.2f), paint);
        paint.setColor(0xFF0A0A0C);
        canvas.drawCircle(cx, cy, dp(1.4f), paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float degrees, float length,
                      float width, int color) {
        double a = Math.toRadians(degrees - 90);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        canvas.drawLine(cx, cy,
                cx + (float) Math.cos(a) * length, cy + (float) Math.sin(a) * length, paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
