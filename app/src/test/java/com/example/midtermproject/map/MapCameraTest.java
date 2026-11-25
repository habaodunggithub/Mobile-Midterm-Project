package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapCameraTest {
    private static final float WORLD_WIDTH = 780f;
    private static final float WORLD_HEIGHT = 520f;
    private static final float SEAT_DIAMETER = 20f;
    private static final float PADDING = 16f;
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private static MapCamera camera(float viewWidth, float viewHeight, float bottomInset) {
        MapCamera camera = new MapCamera(PADDING);
        camera.setWorldSize(WORLD_WIDTH, WORLD_HEIGHT);
        camera.setViewport(viewWidth, viewHeight, bottomInset);
        camera.resetToFit();
        return camera;
    }

    private static MapCamera portrait() {
        return camera(390f, 528f, 0f);
    }

    private static MapCamera landscape() {
        return camera(631f, 277f, 0f);
    }

    // ---- AC-1.5: kích thước ghế ở góc nhìn mặc định ----

    @Test
    public void ac1_5_seatDiameterAtFitPortraitAndLandscape() {
        assertTrue("portrait " + SEAT_DIAMETER * portrait().getScale(),
                SEAT_DIAMETER * portrait().getScale() >= 12f);
        assertTrue("landscape " + SEAT_DIAMETER * landscape().getScale(),
                SEAT_DIAMETER * landscape().getScale() >= 9f);
    }

    // ---- AC-2.12: góc mặc định theo hình dạng vùng nhìn ----

    @Test
    public void ac2_12_defaultAngleByViewportShape() {
        assertEquals(-HALF_PI, portrait().defaultAngle(), 1e-6f);
        assertEquals(0f, landscape().defaultAngle(), 1e-6f);
        assertEquals(0f, camera(500f, 500f, 0f).defaultAngle(), 1e-6f);
    }

    @Test
    public void ac2_12_resetToFitUsesTheDefaultAngle() {
        assertEquals(-HALF_PI, portrait().getAngle(), 1e-6f);
        assertEquals(0f, landscape().getAngle(), 1e-6f);
    }

    @Test
    public void ac1_1_stageIsOnTheLeftInPortraitAndOnTopInLandscape() {
        float stageX = WORLD_WIDTH / 2f, stageY = 45f;
        float centerX = WORLD_WIDTH / 2f, centerY = WORLD_HEIGHT / 2f;

        MapCamera portrait = portrait();
        assertTrue(portrait.worldToScreenX(stageX, stageY) < portrait.worldToScreenX(centerX, centerY));

        MapCamera landscape = landscape();
        assertTrue(landscape.worldToScreenY(stageX, stageY) < landscape.worldToScreenY(centerX, centerY));
    }

    // ---- Fit: thấy trọn sơ đồ, căn giữa vùng nhìn ----

    @Test
    public void fitKeepsEveryCornerInsideThePaddedViewport() {
        float[][] corners = {{0, 0}, {WORLD_WIDTH, 0}, {0, WORLD_HEIGHT}, {WORLD_WIDTH, WORLD_HEIGHT}};
        for (MapCamera camera : new MapCamera[]{portrait(), landscape()}) {
            for (float[] corner : corners) {
                float x = camera.worldToScreenX(corner[0], corner[1]);
                float y = camera.worldToScreenY(corner[0], corner[1]);
                assertTrue("x=" + x, x >= PADDING - 0.01f);
                assertTrue("y=" + y, y >= PADDING - 0.01f);
            }
        }
        MapCamera portrait = portrait();
        for (float[] corner : corners) {
            assertTrue(portrait.worldToScreenX(corner[0], corner[1]) <= 390f - PADDING + 0.01f);
            assertTrue(portrait.worldToScreenY(corner[0], corner[1]) <= 528f - PADDING + 0.01f);
        }
    }

    @Test
    public void fitCentersTheWorldInTheViewportAboveTheBottomInset() {
        MapCamera camera = camera(390f, 600f, 120f);
        float centerX = camera.worldToScreenX(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        float centerY = camera.worldToScreenY(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        assertEquals(390f / 2f, centerX, 0.01f);
        assertEquals((600f - 120f) / 2f, centerY, 0.01f);
    }

    @Test
    public void bottomInsetShrinksTheFitScale() {
        assertTrue(camera(390f, 600f, 150f).getScale() < camera(390f, 600f, 0f).getScale());
    }

    @Test
    public void fitScaleDependsOnTheAngleThroughTheBoundingBox() {
        MapCamera camera = portrait();
        // Xoay 90 độ thì rộng và cao hoán đổi, 180 độ thì giống 0 độ.
        assertEquals(camera.fitScale(0f), camera.fitScale((float) Math.PI), 1e-5f);
        assertEquals(camera.fitScale(HALF_PI), camera.fitScale(-HALF_PI), 1e-5f);
        // Xoay chéo thì khung bao lớn nhất nên fit nhỏ nhất.
        assertTrue(camera.fitScale((float) (Math.PI / 4)) < camera.fitScale(0f));
        assertTrue(camera.fitScale((float) (Math.PI / 4)) < camera.fitScale(HALF_PI));
    }

    // ---- Đổi tọa độ hai chiều ----

    @Test
    public void screenWorldRoundTripAtAnyAngle() {
        float[] angles = {0f, -HALF_PI, (float) Math.toRadians(37), (float) Math.PI};
        float[][] worldPoints = {{0, 0}, {390, 260}, {111.5f, 402.25f}, {780, 520}};
        for (float angle : angles) {
            MapCamera camera = portrait();
            camera.set(1.7f, angle, 123.4f, -56.7f);
            for (float[] p : worldPoints) {
                float sx = camera.worldToScreenX(p[0], p[1]);
                float sy = camera.worldToScreenY(p[0], p[1]);
                assertEquals("x at " + angle, p[0], camera.screenToWorldX(sx, sy), 1e-2f);
                assertEquals("y at " + angle, p[1], camera.screenToWorldY(sx, sy), 1e-2f);
            }
        }
    }

    // ---- Chưa sẵn sàng ----

    @Test
    public void notReadyUntilTheViewportHasASize() {
        MapCamera camera = new MapCamera(PADDING);
        camera.setWorldSize(WORLD_WIDTH, WORLD_HEIGHT);
        assertFalse(camera.isReady());
        camera.setViewport(390f, 528f, 0f);
        assertTrue(camera.isReady());
        camera.setViewport(390f, 100f, 100f);
        assertFalse(camera.isReady());
    }

    @Test
    public void fitTargetOfAnUnreadyCameraIsHarmless() {
        MapCamera camera = new MapCamera(PADDING);
        float[] target = camera.fitTarget();
        assertEquals(1f, target[0], 0f);
        assertEquals(0f, target[2], 0f);
        assertEquals(0f, target[3], 0f);
    }
}
