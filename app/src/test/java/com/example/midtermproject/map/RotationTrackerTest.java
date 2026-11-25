package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RotationTrackerTest {
    private static final float SIX_DEGREES = (float) Math.toRadians(6);

    /** Hai ngón nằm đối xứng quanh (200, 300), đường nối có góc {@code degrees} (đo theo màn hình). */
    private static float[] fingersAt(double degrees) {
        double rad = Math.toRadians(degrees);
        float dx = (float) (100 * Math.cos(rad));
        float dy = (float) (100 * Math.sin(rad));
        return new float[]{200 - dx, 300 - dy, 200 + dx, 300 + dy};
    }

    private static void begin(RotationTracker tracker, double degrees) {
        float[] f = fingersAt(degrees);
        tracker.begin(f[0], f[1], f[2], f[3]);
    }

    private static float update(RotationTracker tracker, double degrees) {
        float[] f = fingersAt(degrees);
        return tracker.update(f[0], f[1], f[2], f[3]);
    }

    private static float degrees(float radians) {
        return (float) Math.toDegrees(radians);
    }

    @Test
    public void ac2_8_reportsSignedDelta() {
        RotationTracker tracker = new RotationTracker(0f);
        begin(tracker, 10);
        assertEquals(20f, degrees(update(tracker, 30)), 1e-3f);    // quay theo chiều kim đồng hồ: dương
        assertEquals(-35f, degrees(update(tracker, -5)), 1e-3f);   // quay ngược lại: âm
    }

    @Test
    public void ac2_8_deltaIsRelativeToTheLastUpdateNotToTheStart() {
        RotationTracker tracker = new RotationTracker(0f);
        begin(tracker, 0);
        assertEquals(10f, degrees(update(tracker, 10)), 1e-3f);
        assertEquals(10f, degrees(update(tracker, 20)), 1e-3f);
        assertEquals(0f, degrees(update(tracker, 20)), 1e-3f);
    }

    @Test
    public void ac2_8_wrapsAcrossPlusMinusPi() {
        RotationTracker tracker = new RotationTracker(0f);
        begin(tracker, 179);
        // Từ 179 độ sang -179 độ chỉ là 2 độ theo chiều dương, không phải -358 độ.
        assertEquals(2f, degrees(update(tracker, -179)), 1e-3f);
        assertEquals(-2f, degrees(update(tracker, 179)), 1e-3f);
    }

    @Test
    public void ac2_8_swappingTheTwoFingersDoesNotChangeTheReportedSign() {
        RotationTracker tracker = new RotationTracker(0f);
        float[] a = fingersAt(0);
        tracker.begin(a[0], a[1], a[2], a[3]);
        float[] b = fingersAt(15);
        assertEquals(15f, degrees(tracker.update(b[0], b[1], b[2], b[3])), 1e-3f);
    }

    @Test
    public void ac2_8_ignoresTwistsBelowThreshold() {
        RotationTracker tracker = new RotationTracker(SIX_DEGREES);
        begin(tracker, 0);
        assertEquals(0f, update(tracker, 2), 0f);
        assertEquals(0f, update(tracker, 5), 0f);
        assertEquals(0f, update(tracker, -4), 0f);
        assertFalse(tracker.isEngaged());
    }

    @Test
    public void ac2_8_continuesWithoutJumpAfterThreshold() {
        RotationTracker tracker = new RotationTracker(SIX_DEGREES);
        begin(tracker, 0);
        assertEquals(0f, update(tracker, 5), 0f);
        // Vượt ngưỡng 6 độ ở góc 8: chỉ phần vượt (2 độ) được tính, không nhảy 8 độ.
        assertEquals(2f, degrees(update(tracker, 8)), 1e-3f);
        assertTrue(tracker.isEngaged());
        // Từ đó trở đi xoay đủ theo ngón.
        assertEquals(3f, degrees(update(tracker, 11)), 1e-3f);
        assertEquals(-7f, degrees(update(tracker, 4)), 1e-3f);
    }

    @Test
    public void ac2_8_thresholdWorksInTheCounterClockwiseDirectionToo() {
        RotationTracker tracker = new RotationTracker(SIX_DEGREES);
        begin(tracker, 0);
        assertEquals(0f, update(tracker, -4), 0f);
        assertEquals(-3f, degrees(update(tracker, -9)), 1e-3f);
    }

    @Test
    public void ac2_8_thresholdAccumulatesAcrossSmallSteps() {
        RotationTracker tracker = new RotationTracker(SIX_DEGREES);
        begin(tracker, 0);
        for (int i = 1; i <= 5; i++) {
            assertEquals(0f, update(tracker, i), 0f);    // 5 độ tích lũy, chưa tới ngưỡng
        }
        assertEquals(1f, degrees(update(tracker, 7)), 1e-3f);
    }

    @Test
    public void beginResetsTheThresholdAndEngagement() {
        RotationTracker tracker = new RotationTracker(SIX_DEGREES);
        begin(tracker, 0);
        update(tracker, 20);
        assertTrue(tracker.isEngaged());

        begin(tracker, 50);
        assertFalse(tracker.isEngaged());
        assertEquals(0f, update(tracker, 53), 0f);
    }
}
