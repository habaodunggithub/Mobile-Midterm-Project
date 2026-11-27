package com.example.midtermproject.ui;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.midtermproject.decor.StickerRelief;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapeAppearancePathProvider;

import java.util.function.Supplier;

/** Vẽ lớp nổi trên đúng kích thước của nền Material, kể cả khi nền nằm trong inset của vùng chạm. */
final class RaisedStickerStroke extends Drawable {
    private final ShapeAppearanceModel outerShape;
    private final ShapeAppearancePathProvider paths = new ShapeAppearancePathProvider();
    private final Path outer = new Path();
    private final Path inner = new Path();
    private final Rect previousBounds = new Rect();
    private final RectF contour = new RectF();
    private final float width;
    private final ColorStateList colors;
    private final Supplier<Drawable> background;
    private final StickerRelief relief;

    RaisedStickerStroke(ShapeAppearanceModel shape, float width, ColorStateList colors,
                        Supplier<Drawable> background) {
        this.outerShape = shape;
        this.width = width;
        this.colors = colors;
        this.background = background;
        this.relief = new StickerRelief(colors.getDefaultColor());
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Drawable surface = background.get();
        if (surface == null || surface.getBounds().isEmpty()) {
            return;
        }
        final Rect bounds = surface.getBounds();
        if (!previousBounds.equals(bounds)) {
            previousBounds.set(bounds);
            contour.set(bounds);
            paths.calculatePath(outerShape, 1f, contour, outer);
            contour.inset(width, width);
            inner.rewind();
            if (!contour.isEmpty()) {
                paths.calculatePath(insetCorners(outerShape, contour, width), 1f, contour, inner);
            }
            contour.set(bounds);
            relief.update(outer, inner, contour, width);
        }
        relief.draw(canvas, outer, inner);
    }

    /**
     * Hình của mép trong: mỗi góc bo nhỏ hơn góc ngoài đúng bằng bề dày viền (không âm), để viền dày đều cả ở chỗ bo.
     * Chỉ chạy khi kích thước nền đổi. Dùng các hàm công khai của Material thay cho AdjustedCornerSize (API nội bộ).
     */
    private static ShapeAppearanceModel insetCorners(ShapeAppearanceModel shape, RectF bounds, float width) {
        return shape.toBuilder()
                .setTopLeftCornerSize(Math.max(0f, shape.getTopLeftCornerSize().getCornerSize(bounds) - width))
                .setTopRightCornerSize(Math.max(0f, shape.getTopRightCornerSize().getCornerSize(bounds) - width))
                .setBottomRightCornerSize(Math.max(0f, shape.getBottomRightCornerSize().getCornerSize(bounds) - width))
                .setBottomLeftCornerSize(Math.max(0f, shape.getBottomLeftCornerSize().getCornerSize(bounds) - width))
                .build();
    }

    @Override
    public boolean isStateful() {
        return colors.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        relief.setColor(colors.getColorForState(state, colors.getDefaultColor()));
        invalidateSelf();
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        relief.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filter) {
        relief.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
