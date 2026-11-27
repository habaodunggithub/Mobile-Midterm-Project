package com.example.midtermproject.decor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class StickerReliefTest {
    @Test
    public void centerRemainsTransparentWhenBorderChangesSize() {
        final StickerRelief relief = new StickerRelief(Color.RED);
        final Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Path outer = new Path();
        final Path inner = new Path();

        for (int size : new int[] {64, 88}) {
            bitmap.eraseColor(Color.TRANSPARENT);
            outer.reset();
            inner.reset();
            final RectF bounds = new RectF(0, 0, size, size);
            outer.addRect(bounds, Path.Direction.CW);
            inner.addRect(8, 8, size - 8, size - 8, Path.Direction.CW);
            relief.update(outer, inner, bounds, 8f);
            relief.draw(canvas, outer, inner);

            assertNotEquals(Color.TRANSPARENT, bitmap.getPixel(4, size / 2));
            assertEquals(Color.TRANSPARENT, bitmap.getPixel(size / 2, size / 2));
            assertEquals(Color.TRANSPARENT, bitmap.getPixel(94, 94));
        }
    }
}
