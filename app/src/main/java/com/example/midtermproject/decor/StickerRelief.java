package com.example.midtermproject.decor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.core.graphics.ColorUtils;

/** Viền phồng với ánh sáng từ góc trên trái và hai mép chuyển sắc ngược nhau. */
public final class StickerRelief {
    private final Path band = new Path();
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerRim = rimPaint();
    private final Paint innerRim = rimPaint();
    private final Matrix lighting = new Matrix();
    private final RectF bounds = new RectF();
    private int color;

    public StickerRelief(int color) {
        band.setFillType(Path.FillType.EVEN_ODD);
        setColor(color);
    }

    public void setColor(int color) {
        if (this.color == color && body.getShader() != null) {
            return;
        }
        this.color = color;
        body.setShader(gradient(tint(Color.WHITE, 0.29f), color, tint(0xFF240915, 0.12f)));
        outerRim.setShader(gradient(tint(Color.WHITE, 0.72f), tint(Color.WHITE, 0.28f), tint(0xFF240915, 0.58f)));
        innerRim.setShader(gradient(tint(0xFF240915, 0.39f), tint(0xFF240915, 0.16f), tint(Color.WHITE, 0.40f)));
        updateLighting();
    }

    public void update(Path outer, Path inner, RectF bounds, float width) {
        // reset() giữ EVEN_ODD. rewind() có thể đưa đường về WINDING và tô kín phần giữa.
        band.reset();
        if (outer.isEmpty() || inner.isEmpty()) {
            return;
        }
        band.addPath(outer);
        band.addPath(inner);
        this.bounds.set(bounds);
        outerRim.setStrokeWidth(width * 0.64f);
        innerRim.setStrokeWidth(width * 0.54f);
        updateLighting();
    }

    public void draw(Canvas canvas, Path outer, Path inner) {
        if (band.isEmpty()) {
            return;
        }
        canvas.drawPath(band, body);
        final int saved = canvas.save();
        canvas.clipPath(band);
        canvas.drawPath(outer, outerRim);
        canvas.drawPath(inner, innerRim);
        canvas.restoreToCount(saved);
    }

    public void setAlpha(int alpha) {
        body.setAlpha(alpha);
        outerRim.setAlpha(alpha);
        innerRim.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter filter) {
        body.setColorFilter(filter);
        outerRim.setColorFilter(filter);
        innerRim.setColorFilter(filter);
    }

    private void updateLighting() {
        lighting.setScale(Math.max(1f, bounds.width()), Math.max(1f, bounds.height()));
        lighting.postTranslate(bounds.left, bounds.top);
        body.getShader().setLocalMatrix(lighting);
        outerRim.getShader().setLocalMatrix(lighting);
        innerRim.getShader().setLocalMatrix(lighting);
    }

    private int tint(int target, float amount) {
        return ColorUtils.blendARGB(color, target, amount);
    }

    private static Shader gradient(int light, int middle, int shadow) {
        return new LinearGradient(0f, 0f, 1f, 1f, new int[] {light, middle, shadow}, null, Shader.TileMode.CLAMP);
    }

    private static Paint rimPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        return paint;
    }
}
