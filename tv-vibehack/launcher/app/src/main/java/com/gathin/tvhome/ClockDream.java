package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Random;

/**
 * Screensaver: the launcher's analog clock, greeting and date on black, over two soft ambient glows.
 *
 * Deliberately cheap for a 1 GB TV: no video, and no bitmaps unless the optional photo layer is
 * switched on. The clock content moves to the next of nine spots once a minute (so nothing burns
 * in), the two glows drift apart and their colours breathe through a palette that follows the time
 * of day. The only continuous work is the dial's second hand, which redraws a 360 dp circle once a
 * second while the screensaver is on.
 */
public class ClockDream extends DreamService {

    private static final long MOVE_MS = 60_000L;
    private static final long WEATHER_MS = 10 * 60_000L;
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
    private TextView weather;
    private View weatherRow;
    private ImageView photo;
    private View scrim;
    private Bitmap currentPhoto;
    private boolean photosEnabled;
    private String photoUrl;
    private long photoMs = 10 * 60_000L;
    private int photoWidth;
    private int photoHeight;
    private GradientDrawable glow;
    private GradientDrawable glow2;
    private int[] palette = NIGHT;
    private int colorIndex;
    private int glowColor;
    private int slot = 4;   // start dead centre; the sway then visits the other eight spots

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    private final Runnable weatherTick = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
            handler.postDelayed(this, WEATHER_MS);
        }
    };

    private final Runnable photoTick = new Runnable() {
        @Override
        public void run() {
            loadPhoto();
            handler.postDelayed(this, photoMs);
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
        weather = findViewById(R.id.dreamWeather);
        weatherRow = findViewById(R.id.dreamWeatherRow);
        if (Weather.cached() != null) showWeather(Weather.cached(), Weather.cachedCode());

        photo = findViewById(R.id.dreamPhoto);
        scrim = findViewById(R.id.dreamScrim);
        photosEnabled = "on".equalsIgnoreCase(getString(R.string.ambient_photos).trim());
        if (photosEnabled) {
            photoUrl = getString(R.string.ambient_photo_url);
            photoWidth = Math.min(1920, getResources().getDisplayMetrics().widthPixels);
            photoHeight = Math.min(1080, getResources().getDisplayMetrics().heightPixels);
            try {
                photoMs = Math.max(2, Math.min(120,
                        Integer.parseInt(getString(R.string.ambient_photo_minutes).trim()))) * 60_000L;
            } catch (NumberFormatException ignored) {
                photoMs = 10 * 60_000L;
            }
            scrim.setVisibility(View.VISIBLE);
        }

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
        refreshWeather();
        handler.postDelayed(weatherTick, WEATHER_MS);
        if (photosEnabled) {
            loadPhoto();
            handler.postDelayed(photoTick, photoMs);
        }
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        handler.removeCallbacks(weatherTick);
        handler.removeCallbacks(photoTick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        handler.removeCallbacks(weatherTick);
        handler.removeCallbacks(photoTick);
        // The dream is over: give the bitmap back now rather than waiting for GC.
        if (photo != null) photo.setImageDrawable(null);
        if (currentPhoto != null) {
            currentPhoto.recycle();
            currentPhoto = null;
        }
        photo = null;
        super.onDetachedFromWindow();
    }

    private void loadPhoto() {
        if (!photosEnabled || photo == null) return;
        final String url = photoUrl
                .replace("{w}", String.valueOf(photoWidth))
                .replace("{h}", String.valueOf(photoHeight))
                .replace("{n}", String.valueOf(System.currentTimeMillis()));
        new Thread(() -> {
            final Bitmap bitmap = download(url);
            handler.post(() -> showPhoto(bitmap));
        }, "dream-photo").start();
    }

    private static Bitmap download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "tvhome");
            BitmapFactory.Options options = new BitmapFactory.Options();
            // RGB_565 halves the footprint; this is a dimmed background, not a photo viewer.
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream in = connection.getInputStream()) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception e) {
            return null;   // no network, bad redirect: the previous frame (or black) stays
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void showPhoto(Bitmap bitmap) {
        if (bitmap == null) return;
        if (photo == null) {
            bitmap.recycle();
            return;
        }
        Bitmap old = currentPhoto;
        currentPhoto = bitmap;
        photo.setAlpha(0f);
        photo.setImageBitmap(bitmap);
        photo.animate().alpha(1f).setDuration(1400).start();
        if (old != null && old != bitmap) old.recycle();
    }

    private void refreshWeather() {
        Weather.refresh(this, (text, code) -> {
            if (text != null) showWeather(text, code);
        });
    }

    private void showWeather(String text, int code) {
        if (weather == null || weatherRow == null) return;
        weather.setText(text);
        weatherRow.setVisibility(View.VISIBLE);
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
     * Keeps the composition centred (the dial is the widest thing on screen) and sways it by a few
     * pixels on the same 3x3 cycle — enough to rest the panel without breaking the symmetry.
     */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());

        int col = slot % 3;
        int row = (slot / 3) % 3;
        slot = (slot + 1) % 9;

        float x = maxX / 2f + (col - 1) * Math.min(dp(8), maxX * 0.05f);
        float y = maxY / 2f + (row - 1) * Math.min(dp(6), maxY * 0.05f);

        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
            // A second pool of colour low on the right keeps the frame from looking flat.
            glow2.setGradientCenter(0.80f + (col - 1) * 0.02f, 0.74f + (row - 1) * 0.02f);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
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
