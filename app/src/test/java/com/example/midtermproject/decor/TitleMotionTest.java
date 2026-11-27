package com.example.midtermproject.decor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TitleMotionTest {
    private static final float EPS = 1e-3f;
    private final float[] out = new float[3];

    @Test
    public void q49_letterStartsHiddenAboveItsPlace() {
        TitleMotion.letterAt(2, 0L, out);
        assertEquals(-TitleMotion.DROP_HEIGHT_DP, out[TitleMotion.DY], EPS);
        assertEquals(0f, out[TitleMotion.ALPHA], EPS);
    }

    @Test
    public void q49_lettersDropOneAfterAnother() {
        long t = TitleMotion.DROP_STAGGER_MS / 2;
        TitleMotion.letterAt(0, t, out);
        assertTrue("first letter already visible", out[TitleMotion.ALPHA] > 0f);
        TitleMotion.letterAt(1, t, out);
        assertEquals("second letter still waiting", 0f, out[TitleMotion.ALPHA], EPS);
    }

    @Test
    public void q49_dropOvershootsThenLandsAtRest() {
        float lowest = 0f;
        for (long t = 0; t <= TitleMotion.DROP_MS; t += 5) {
            TitleMotion.letterAt(0, t, out);
            lowest = Math.max(lowest, out[TitleMotion.DY]);
        }
        assertTrue("goes a little below its place", lowest > 0.5f);
        TitleMotion.letterAt(0, TitleMotion.DROP_MS, out);
        assertEquals(0f, out[TitleMotion.DY], EPS);
        assertEquals(0f, out[TitleMotion.ROTATION], EPS);
        assertEquals(1f, out[TitleMotion.ALPHA], EPS);
    }

    @Test
    public void q49_swayStartsSmoothlyAfterDrop() {
        for (int i = 0; i < 4; i++) {
            long end = i * TitleMotion.DROP_STAGGER_MS + TitleMotion.DROP_MS;
            TitleMotion.letterAt(i, end + 16, out);
            assertTrue("tiny change one frame after landing", Math.abs(out[TitleMotion.ROTATION]) < 0.05f);
        }
    }

    @Test
    public void q49_swayStaysWithinAmplitude() {
        // Bắt đầu sau khi chữ cuối đã rơi xong: trước đó các chữ sau còn đang ở pha rơi.
        for (long t = TitleMotion.dropEnd(4); t < 40_000; t += 37) {
            for (int i = 0; i < 4; i++) {
                TitleMotion.letterAt(i, t, out);
                assertTrue(Math.abs(out[TitleMotion.ROTATION]) <= TitleMotion.SWAY_DEG + EPS);
                assertTrue(Math.abs(out[TitleMotion.DY]) <= TitleMotion.SWAY_LIFT_DP + EPS);
            }
        }
    }

    @Test
    public void q49_dropEndCoversLastLetter() {
        assertEquals(3 * TitleMotion.DROP_STAGGER_MS + TitleMotion.DROP_MS, TitleMotion.dropEnd(4));
    }
}
