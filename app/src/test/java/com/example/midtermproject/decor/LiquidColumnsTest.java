package com.example.midtermproject.decor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LiquidColumnsTest {
    private static final float STEP = 10f;
    private static final float HEIGHT = 760f;

    /** Vị trí và bề rộng gốc của tám dải, chép từ LiquidColumns.poster(). */
    private static final float[] BASE_X = {18f, 70f, 112f, 160f, 222f, 262f, 314f, 350f};
    private static final float[] BASE_WIDTH = {34f, 22f, 14f, 40f, 18f, 26f, 30f, 16f};

    private final LiquidColumns columns = LiquidColumns.poster();
    private final int samples = LiquidColumns.samplesFor(HEIGHT, STEP);
    private final float[] left = new float[samples];
    private final float[] right = new float[samples];

    @Test
    public void q42_posterHasEightColumnsInThreeColours() {
        assertEquals(8, columns.count());
        for (int i = 0; i < columns.count(); i++) {
            int slot = columns.colorSlot(i);
            assertTrue(slot >= LiquidColumns.COLOR_ORANGE && slot <= LiquidColumns.COLOR_VIOLET);
        }
    }

    @Test
    public void q42_samplesCoverFullHeightWithMargin() {
        // Điểm đầu nằm trên mép trên, điểm cuối nằm dưới mép dưới.
        assertTrue((0 - 1) * STEP < 0f);
        assertTrue((samples - 2) * STEP >= HEIGHT);
    }

    @Test
    public void q42_widthStaysBetweenMinimumAndBase() {
        for (double t = 0; t < LiquidColumns.PERIOD; t += 0.7) {
            for (int i = 0; i < columns.count(); i++) {
                columns.edges(i, samples, STEP, t, left, right);
                float base = BASE_WIDTH[i];
                for (int k = 0; k < samples; k++) {
                    float w = right[k] - left[k];
                    assertTrue("min width", w >= base * LiquidColumns.MIN_WIDTH_RATIO - 1e-3f);
                    assertTrue("max width", w <= base + 1e-3f);
                }
            }
        }
    }

    @Test
    public void q42_centreSwaysWithinAmplitude() {
        for (int i = 0; i < columns.count(); i++) {
            columns.edges(i, samples, STEP, 1.3, left, right);
            float x0 = BASE_X[i];
            for (int k = 0; k < samples; k++) {
                float centre = (left[k] + right[k]) / 2f;
                assertTrue(Math.abs(centre - x0) <= LiquidColumns.SWAY + 1e-3f);
            }
        }
    }

    @Test
    public void q42_motionRepeatsAfterOnePeriod() {
        float[] left2 = new float[samples];
        float[] right2 = new float[samples];
        columns.edges(3, samples, STEP, 0.4, left, right);
        columns.edges(3, samples, STEP, 0.4 + LiquidColumns.PERIOD, left2, right2);
        for (int k = 0; k < samples; k++) {
            assertEquals(left[k], left2[k], 1e-2f);
            assertEquals(right[k], right2[k], 1e-2f);
        }
    }

    @Test
    public void q42_shapeChangesOverTime() {
        float[] later = new float[samples];
        float[] unused = new float[samples];
        columns.edges(0, samples, STEP, 0, left, right);
        columns.edges(0, samples, STEP, 1.0, later, unused);
        boolean moved = false;
        for (int k = 0; k < samples; k++) {
            moved |= Math.abs(left[k] - later[k]) > 1f;
        }
        assertTrue(moved);
    }
}
