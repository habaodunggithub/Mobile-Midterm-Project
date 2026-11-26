package com.example.midtermproject.map;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Sóng mở màn và pháo giấy vẽ theo pixel màn hình để giữ kích thước khi zoom. */
final class MapLightShow {
    private static final int CONFETTI_COUNT = 36;
    private static final long INTRO_DURATION_MS = 920L;
    private static final long BOOKING_DURATION_MS = 980L;
    private static final long BOOKING_DELAY_MS = 160L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors;
    private final float density;
    private final float[] directionX = new float[CONFETTI_COUNT];
    private final float[] directionY = new float[CONFETTI_COUNT];
    private long introStart = -INTRO_DURATION_MS;
    private long bookingStart = -BOOKING_DURATION_MS;

    MapLightShow(float density, int orange, int violet, int pink, int green) {
        this.density = density;
        colors = new int[] { orange, violet, pink, green };
        for (int i = 0; i < CONFETTI_COUNT; i++) {
            final double angle = (i + (i % 4) * 0.23) * Math.PI * 2 / CONFETTI_COUNT;
            directionX[i] = (float) Math.cos(angle);
            directionY[i] = (float) Math.sin(angle);
        }
    }

    void startIntro(long now) {
        introStart = now;
    }

    void startBooking(long now) {
        // Hộp thoại cần một nhịp để đóng trước khi pháo giấy xuất hiện.
        bookingStart = now + BOOKING_DELAY_MS;
    }

    void clear() {
        introStart = -INTRO_DURATION_MS;
        bookingStart = -BOOKING_DURATION_MS;
    }

    boolean isRunning(long now) {
        return now < introStart + INTRO_DURATION_MS || now < bookingStart + BOOKING_DURATION_MS;
    }

    void draw(Canvas canvas, float stageX, float stageY, int width, int height, long now) {
        if (width <= 0 || height <= 0) {
            return;
        }
        final long introElapsed = now - introStart;
        if (introElapsed >= 0 && introElapsed < INTRO_DURATION_MS) {
            final float reach = (float) Math.hypot(width, height);
            drawWaves(canvas, stageX, stageY, reach, introElapsed);
        }
        final long bookingElapsed = now - bookingStart;
        if (bookingElapsed >= 0 && bookingElapsed < BOOKING_DURATION_MS) {
            drawConfetti(canvas, width, height, bookingElapsed / (float) BOOKING_DURATION_MS);
        }
    }

    private void drawWaves(Canvas canvas, float x, float y, float reach, long elapsed) {
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            final float progress = (elapsed - i * 90L) / (float) (INTRO_DURATION_MS - 180L);
            if (progress >= 0f && progress < 1f) {
                final float radius = reach * (1f - (1f - progress) * (1f - progress));
                paint.setColor(colors[i]);
                paint.setAlpha(Math.round(122f * (1f - progress)));
                paint.setStrokeWidth(density * (2f + 5f * (1f - progress)));
                canvas.drawCircle(x, y, radius, paint);
            }
        }
    }

    private void drawConfetti(Canvas canvas, int width, int height, float progress) {
        final float spread = 1f - (1f - progress) * (1f - progress);
        final float gravity = progress * progress * height * 0.18f;
        final int alpha = Math.round(235f * Math.min(1f, (1f - progress) * 2.5f));
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < CONFETTI_COUNT; i++) {
            final float speed = 0.54f + (i % 7) * 0.075f;
            final float x = width * (0.5f + directionX[i] * spread * speed * 0.47f);
            final float y = height * (0.43f + directionY[i] * spread * speed * 0.39f) + gravity;
            final float size = density * (2f + i % 3);
            paint.setColor(colors[i % colors.length]);
            paint.setAlpha(alpha);
            if (i % 3 == 0) {
                canvas.drawCircle(x, y, size * 0.72f, paint);
            } else {
                canvas.save();
                canvas.rotate(i * 37f + (i % 2 == 0 ? 1f : -1f) * 260f * progress, x, y);
                canvas.drawRect(x - size * 0.44f, y - size, x + size * 0.44f, y + size, paint);
                canvas.restore();
            }
        }
    }
}
