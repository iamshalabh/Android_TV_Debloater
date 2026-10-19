package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/**
 * Minimal analog dial: a thin rim, hour marks and three hands, drawn with Canvas.
 *
 * No bitmaps; the quarter numerals reuse the bundled Schibsted typeface, so the only memory is one
 * extra Paint and a typeface reference. The view redraws once per second while it is attached (the
 * screensaver is on), and not at all otherwise. The small invalidate region is the dial only, so the
 * cost is far below one frame of video.
 */
public class AnalogClockView extends View {

    /** Quarter numerals only: a full 1-12 ring reads as noise on a 720p panel at ten feet. */
    private static final int[] NUMERALS = {12, 3, 6, 9};
    private static final String[] NUMERAL_TEXT = {"12", "3", "6", "9"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numeral = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics numeralMetrics = new Paint.FontMetrics();
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
        numeral.setTextAlign(Paint.Align.CENTER);
        numeral.setColor(0xCCFFFFFF);
        try {
            Typeface face = getResources().getFont(R.font.schibsted);
            if (face != null) numeral.setTypeface(face);
            numeral.setFontVariationSettings("'wght' 300");
        } catch (Exception ignored) {
            // A missing font file is not worth crashing the screensaver over.
        }
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

        // A barely-there wash so the face reads as glass over the glow.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x08FFFFFF);
        canvas.drawCircle(cx, cy, r, paint);

        // Eight small ticks; the quarters are drawn as numerals instead.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        for (int i = 0; i < 12; i++) {
            if (i % 3 == 0) continue;
            double a = Math.toRadians(i * 30 - 90);
            float inner = r - dp(8);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0x4DFFFFFF);
            canvas.drawLine(cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner,
                    cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, paint);
        }

        paint.setStrokeWidth(dp(1));
        paint.setColor(0x33FFFFFF);
        canvas.drawCircle(cx, cy, r, paint);

        // 12 / 3 / 6 / 9, inside the rim and outside the hands. One Paint, no bitmap.
        numeral.setTextSize(r * 0.115f);
        numeral.setColor(0xB3FFFFFF);
        numeral.getFontMetrics(numeralMetrics);
        float numeralRadius = r * 0.77f;
        for (int i = 0; i < NUMERALS.length; i++) {
            double a = Math.toRadians(NUMERALS[i] * 30 - 90);
            float x = cx + (float) Math.cos(a) * numeralRadius;
            float y = cy + (float) Math.sin(a) * numeralRadius;
            float baseline = y - (numeralMetrics.ascent + numeralMetrics.descent) / 2f;
            canvas.drawText(NUMERAL_TEXT[i], x, baseline, numeral);
        }

        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        hand(canvas, cx, cy, (hour + minute / 60f) * 30f, r * 0.46f, dp(4.5f), 0xFFFFFFFF);
        hand(canvas, cx, cy, (minute + second / 60f) * 6f, r * 0.68f, dp(3f), 0xFFFFFFFF);
        secondHand(canvas, cx, cy, second * 6f, r);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFF9F0A);
        canvas.drawCircle(cx, cy, dp(4.5f), paint);
        paint.setColor(0xFF0A0A0C);
        canvas.drawCircle(cx, cy, dp(1.8f), paint);
    }

    private void secondHand(Canvas canvas, float cx, float cy, float degrees, float r) {
        double a = Math.toRadians(degrees - 90);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(1.4f));
        paint.setColor(0xFFFF9F0A);
        canvas.drawLine(cx - (float) Math.cos(a) * r * 0.12f,
                cy - (float) Math.sin(a) * r * 0.12f,
                cx + (float) Math.cos(a) * r * 0.80f,
                cy + (float) Math.sin(a) * r * 0.80f, paint);
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
