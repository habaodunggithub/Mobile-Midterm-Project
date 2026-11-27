package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DimFaderTest {
    private static final float EPS = 1e-4f;

    /** Một khung hình: bắt đầu, lấy mức của chỗ 0, kết thúc. */
    private static float frame(DimFader fader, float dt, boolean dimmed) {
        fader.beginFrame(dt);
        float level = fader.level(0, dimmed);
        fader.endFrame();
        return level;
    }

    @Test
    public void q49_firstFrameSnapsToTarget() {
        DimFader fader = new DimFader();
        fader.reset(1);
        assertEquals(1f, frame(fader, 16f, true), EPS);
        assertFalse(fader.isMoving());
    }

    @Test
    public void q49_dimmingTakesAboutTwoHundredMs() {
        DimFader fader = new DimFader();
        fader.reset(1);
        frame(fader, 16f, false);
        float level = 0f;
        int frames = 0;
        while (level < 1f) {
            level = frame(fader, 16f, true);
            frames++;
        }
        // 200ms / 16ms = 12,5 nên cần 13 khung.
        assertEquals(13, frames);
    }

    @Test
    public void q49_levelMovesBackWhenFilterCleared() {
        DimFader fader = new DimFader();
        fader.reset(1);
        frame(fader, 16f, true);
        float level = frame(fader, 20f, false);
        assertEquals(1f - 20f / DimFader.FADE_MS, level, EPS);
        assertTrue(fader.isMoving());
    }

    @Test
    public void q49_longPauseCountsAsOneShortFrame() {
        DimFader fader = new DimFader();
        fader.reset(1);
        frame(fader, 16f, false);
        float level = frame(fader, 5000f, true);
        assertEquals(DimFader.MAX_FRAME_MS / DimFader.FADE_MS, level, EPS);
    }

    @Test
    public void q49_disabledFaderSnaps() {
        DimFader fader = new DimFader();
        fader.reset(1);
        frame(fader, 16f, false);
        fader.setEnabled(false);
        assertEquals(1f, frame(fader, 16f, true), EPS);
        assertFalse(fader.isMoving());
    }

    @Test
    public void q49_settledSpotIsNotMoving() {
        DimFader fader = new DimFader();
        fader.reset(2);
        frame(fader, 16f, false);
        fader.beginFrame(16f);
        fader.level(0, false);
        fader.level(1, false);
        fader.endFrame();
        assertFalse(fader.isMoving());
    }
}
