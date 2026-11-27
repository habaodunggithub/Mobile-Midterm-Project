package com.example.midtermproject.decor;

import android.graphics.Path;
import android.graphics.RectF;

/** Dáng viền lượn sóng của sticker mẫu G. */
public final class StickerOutline {
    private static final int POINT_COUNT = 240;
    private static final double[] X = new double[POINT_COUNT];
    private static final double[] Y = new double[POINT_COUNT];
    private static final double[] OUTER_WAVE = new double[POINT_COUNT];
    private static final double[] INNER_WAVE = new double[POINT_COUNT];

    static {
        // Tính sẵn điểm và pha sóng để khi kéo thẻ chỉ cần đổi kích thước đường viền.
        for (int i = 0; i < POINT_COUNT; i++) {
            final double t = 2 * Math.PI * i / POINT_COUNT;
            X[i] = superellipseCoordinate(Math.cos(t));
            Y[i] = superellipseCoordinate(Math.sin(t));
            OUTER_WAVE[i] = wave(t, 1);
            INNER_WAVE[i] = wave(t, 2);
        }
    }

    private StickerOutline() {}

    public static void addOuter(Path path, RectF bounds, float density) {
        addOuter(path, bounds, density, 1f);
    }

    public static void addInner(Path path, RectF bounds, float density) {
        addInner(path, bounds, density, 1f);
    }

    public static void addOuter(Path path, RectF bounds, float density, float waveScale) {
        addContour(path, bounds, 2.5f * density * waveScale, OUTER_WAVE);
    }

    public static void addInner(Path path, RectF bounds, float density, float waveScale) {
        addContour(path, bounds, 1.5f * density * waveScale, INNER_WAVE);
    }

    private static double superellipseCoordinate(double value) {
        return Math.signum(value) * Math.pow(Math.abs(value), 2.0 / 10.0);
    }

    private static double wave(double angle, int phase) {
        return Math.sin(7 * angle + phase) + 0.6 * Math.sin(13 * angle - 2 * phase);
    }

    private static void addContour(Path path, RectF bounds, float amplitude, double[] wave) {
        final float cx = bounds.centerX();
        final float cy = bounds.centerY();
        final float halfWidth = bounds.width() / 2f;
        final float halfHeight = bounds.height() / 2f;
        for (int i = 0; i < POINT_COUNT; i++) {
            final double wobble = amplitude * wave[i];
            final float x = (float) (cx + (halfWidth + wobble) * X[i]);
            final float y = (float) (cy + (halfHeight + wobble) * Y[i]);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
}
