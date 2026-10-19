package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Random;

/**
 * Screensaver: the launcher's thin clock, greeting and date on black, over two soft ambient glows.
 *
 * Deliberately cheap for a 1 GB TV: no bitmaps, no video, no network. The clock moves to the next
 * of nine spots once a minute (so nothing burns in), the two glows drift apart and their colours
 * breathe through a palette that follows the time of day. Nothing animates continuously — each
 * change is a short fade tied to the minute tick, so the CPU idles in between.
 */
public class ClockDream extends DreamService {

    private static final long MOVE_MS = 60_000L;

    /** Deep, low-brightness glow colours, one set per time of day; drawn at partial alpha on black. */
    private static final int[] MORNING   = {0xFF1E88A8, 0xFF2BB673, 0xFF3F7CFF, 0xFF7A5CFF};
    private static final int[] AFTERNOON = {0xFF2B5CFF, 0xFF00A39A, 0xFF6A4CFF, 0xFF0E7C86};
    private static final int[] EVENING   = {0xFFE0661B, 0xFFD6336C, 0xFF8A3FFC, 0xFFB34A00};
    private static final int[] NIGHT     = {0xFF2B3A8F, 0xFF4A2C7A, 0xFF1F5C6B, 0xFF3A2B5C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private View content;
    private View ambient;
    private View ambient2;
    private GradientDrawable glow;
    private GradientDrawable glow2;
    private int[] palette = NIGHT;
    private int colorIndex;
    private int glowColor;
    private int slot;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);

        content = findViewById(R.id.dreamContent);
        ambient = findViewById(R.id.dreamAmbient);
        ambient2 = findViewById(R.id.dreamAmbient2);

        glow = radial();
        glow2 = radial();
        ambient.setBackground(glow);
        ambient2.setBackground(glow2);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            glow.setGradientRadius((r - l) * 0.55f);
            glow2.setGradientRadius((r - l) * 0.34f);
        });

        palette = paletteFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        colorIndex = random.nextInt(palette.length);
        setGlow(palette[colorIndex], false);
        setGlow(palette[(colorIndex + 2) % palette.length], true);
        showGreeting();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.postDelayed(tick, MOVE_MS);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private static GradientDrawable radial() {
        GradientDrawable d = new GradientDrawable();
        d.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        return d;
    }

    /** Four palettes, so the glow suits the hour instead of cycling the same five colours all day. */
    private static int[] paletteFor(int hour) {
        if (hour >= 5 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 17) return AFTERNOON;
        if (hour >= 17 && hour < 22) return EVENING;
        return NIGHT;
    }

    private void showGreeting() {
        TextView greeting = findViewById(R.id.dreamGreeting);
        if (greeting == null) return;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res;
        if (hour < 5) res = R.string.good_night;
        else if (hour < 12) res = R.string.good_morning;
        else if (hour < 17) res = R.string.good_afternoon;
        else if (hour < 22) res = R.string.good_evening;
        else res = R.string.good_night;

        String name = getString(R.string.user_name).trim();
        greeting.setText(name.isEmpty() ? getString(res) : getString(res) + ", " + name);
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        }).start();
    }

    /**
     * Steps through a 3x3 grid with a little jitter, so over nine minutes the clock visits every
     * part of the screen instead of clustering in the middle like pure random does.
     */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());

        int col = slot % 3;
        int row = (slot / 3) % 3;
        slot = (slot + 1) % 9;

        float fx = clamp(0.06f + col * 0.44f + (random.nextFloat() - 0.5f) * 0.08f);
        float fy = clamp(0.06f + row * 0.44f + (random.nextFloat() - 0.5f) * 0.08f);
        float x = maxX * fx;
        float y = maxY * fy;

        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
            // The second glow sits in the opposite corner, so the screen never looks lopsided.
            glow2.setGradientCenter(1f - fx, 1f - fy);
        }
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private void shiftGlow() {
        int from = glowColor;
        colorIndex = (colorIndex + 1) % palette.length;
        int to = palette[colorIndex];
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        anim.setDuration(8000);
        anim.addUpdateListener(a -> setGlow((int) a.getAnimatedValue(), false));
        anim.start();

        // Cross-fade the secondary glow to its next colour, then settle: a slow "breath" with no
        // continuously running animator.
        int to2 = palette[(colorIndex + 2) % palette.length];
        ambient2.animate().alpha(0.35f).setDuration(4000).withEndAction(() -> {
            setGlow(to2, true);
            ambient2.animate().alpha(1f).setDuration(4000).start();
        }).start();
    }

    private void setGlow(int color, boolean secondary) {
        int c = color & 0x00FFFFFF;
        int[] colors = secondary
                ? new int[]{0x26000000 | c, 0x0C000000 | c, 0x00000000 | c}
                : new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c};
        if (secondary) {
            glow2.setColors(colors);
        } else {
            glowColor = color;
            glow.setColors(colors);
        }
    }
}
