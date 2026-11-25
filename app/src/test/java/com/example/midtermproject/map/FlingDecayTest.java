package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlingDecayTest {
    private static final float TAU_MS = FlingDecay.TAU_SECONDS * 1000f;

    @Test
    public void ac2_14_distanceStartsAtZero() {
        assertEquals(0f, FlingDecay.distanceAt(3000f, 0L), 1e-6f);
    }

    @Test
    public void ac2_14_distanceGrowsAndConverges() {
        float v0 = 3000f;
        float previous = 0f;
        for (long t = 50; t <= 2000; t += 50) {
            float d = FlingDecay.distanceAt(v0, t);
            assertTrue("grows at " + t, d > previous);
            previous = d;
        }
        float limit = v0 * FlingDecay.TAU_SECONDS;
        assertTrue("never exceeds the limit", previous < limit);
        // Sau 5 hằng số thời gian đã đi được hơn 99% quãng đường giới hạn.
        assertTrue(FlingDecay.distanceAt(v0, (long) (5 * TAU_MS)) > 0.99f * limit);
    }

    @Test
    public void ac2_14_distanceFollowsTheSignOfTheVelocity() {
        assertEquals(-FlingDecay.distanceAt(2500f, 300L), FlingDecay.distanceAt(-2500f, 300L), 1e-3f);
        assertTrue(FlingDecay.distanceAt(-2500f, 300L) < 0f);
    }

    @Test
    public void ac2_14_distanceAtOneTauIsAbout63PercentOfTheLimit() {
        float limit = 4000f * FlingDecay.TAU_SECONDS;
        assertEquals(limit * (1f - (float) Math.exp(-1.0)), FlingDecay.distanceAt(4000f, (long) TAU_MS), 0.5f);
    }

    @Test
    public void ac2_14_durationEndsBelowMinSpeed() {
        for (float speed : new float[]{100f, 600f, 3000f, 8000f}) {
            long duration = FlingDecay.durationMillis(speed);
            float speedAtEnd = speed * (float) Math.exp(-duration / TAU_MS);
            assertEquals("speed at the end for " + speed, FlingDecay.MIN_SPEED_PX_PER_SEC, speedAtEnd, 0.5f);
        }
    }

    @Test
    public void ac2_14_noGlideWhenAlreadySlowerThanTheThreshold() {
        assertEquals(0L, FlingDecay.durationMillis(0f));
        assertEquals(0L, FlingDecay.durationMillis(FlingDecay.MIN_SPEED_PX_PER_SEC));
        assertEquals(0L, FlingDecay.durationMillis(10f));
    }

    @Test
    public void ac2_14_fasterFlingsGlideLongerButWithinAboutASecond() {
        long slow = FlingDecay.durationMillis(500f);
        long fast = FlingDecay.durationMillis(3000f);
        assertTrue(fast > slow);
        // Vuốt nhanh điển hình (khoảng 3000 px/s) trượt khoảng 1 giây, đúng AC-2.14.
        assertTrue("typical fling lasts " + fast, fast > 800L && fast < 1300L);
    }

    @Test
    public void ac2_14_remainingDistanceAtTheEndIsMinSpeedTimesTau() {
        float v0 = 3000f;
        long duration = FlingDecay.durationMillis(v0);
        float limit = v0 * FlingDecay.TAU_SECONDS;
        float remaining = limit - FlingDecay.distanceAt(v0, duration);
        assertEquals(FlingDecay.MIN_SPEED_PX_PER_SEC * FlingDecay.TAU_SECONDS, remaining, 0.5f);
    }
}
