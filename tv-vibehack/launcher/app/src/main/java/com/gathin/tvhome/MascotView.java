package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * A tiny pixel companion for the focused tile. The sprite is drawn as rectangles on a Canvas:
 * no bitmap, no animation loop, and no background work. The only animation is the short hop the
 * home screen runs when focus moves.
 */
public class MascotView extends View {

    private static final String[] SPRITE = {
            "..#####..",
            ".#######.",
            "#########",
            "##oo#oo##",
            "#########",
            "#c#####c#",
            "#########",
            "#########",
            "#########",
            "##.###.##"
    };

    private final Paint paint = new Paint();
    private final Paint shadow = new Paint();

    public MascotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(false);
        shadow.setAntiAlias(true);
        shadow.setColor(0x50000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cell = Math.min(w / 9f, h / 10f);
        float left = (w - cell * 9f) / 2f;
        float top = (h - cell * 10f) / 2f;

        // A soft shadow keeps the character grounded on the black background.
        canvas.drawOval(left + cell, top + cell * 9.1f,
                left + cell * 8f, top + cell * 9.9f, shadow);

        for (int row = 0; row < SPRITE.length; row++) {
            String line = SPRITE[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                if (c == '.') continue;
                paint.setColor(c == 'o' ? 0xFF17171A : c == 'c' ? 0xFFFF9F0A : 0xFFF5F3EE);
                float l = left + col * cell;
                float t = top + row * cell;
                // A hair of overlap stops fractional cell sizes from leaving seams.
                canvas.drawRect(l, t, l + cell + 0.35f, t + cell + 0.35f, paint);
            }
        }
    }
}
