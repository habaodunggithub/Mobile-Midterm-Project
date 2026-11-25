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

    // ---- Hàm tiện ích cho các test pan, zoom, xoay ----

    private static final float[] TEST_ANGLES = {0f, -HALF_PI, (float) Math.toRadians(37)};
    private static final float PAN_MARGIN = 3f * PADDING;

    /** Camera dọc, đã phóng {@code zoom} lần so với fit tại góc đó, tâm sơ đồ nằm giữa vùng nhìn. */
    private static MapCamera zoomedCamera(float angle, float zoom) {
        MapCamera camera = portrait();
        camera.set(camera.fitScale(angle) * zoom, angle, 0f, 0f);
        float dx = 390f / 2f - camera.worldToScreenX(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        float dy = 528f / 2f - camera.worldToScreenY(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        camera.set(camera.getScale(), angle, dx, dy);
        return camera;
    }

    /** Khung bao {minX, minY, maxX, maxY} của sơ đồ trên màn hình. */
    private static float[] boundingBox(MapCamera camera) {
        float[][] corners = {{0, 0}, {WORLD_WIDTH, 0}, {0, WORLD_HEIGHT}, {WORLD_WIDTH, WORLD_HEIGHT}};
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] corner : corners) {
            float x = camera.worldToScreenX(corner[0], corner[1]);
            float y = camera.worldToScreenY(corner[0], corner[1]);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new float[]{minX, minY, maxX, maxY};
    }

    // ---- AC-2.1, AC-2.2: pan ----

    @Test
    public void ac2_1_panKeepsPointUnderFinger() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 4f);
            float beforeX = camera.worldToScreenX(300f, 200f);
            float beforeY = camera.worldToScreenY(300f, 200f);
            camera.panBy(13f, -9f);
            assertEquals("x at " + angle, beforeX + 13f, camera.worldToScreenX(300f, 200f), 1e-3f);
            assertEquals("y at " + angle, beforeY - 9f, camera.worldToScreenY(300f, 200f), 1e-3f);
        }
    }

    @Test
    public void ac2_2_panClampsAtBounds() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 4f);
            camera.panBy(100000f, 100000f);
            float[] box = boundingBox(camera);
            assertEquals("minX at " + angle, PAN_MARGIN, box[0], 1e-2f);
            assertEquals("minY at " + angle, PAN_MARGIN, box[1], 1e-2f);

            camera.panBy(-100000f, -100000f);
            box = boundingBox(camera);
            assertEquals("maxX at " + angle, 390f - PAN_MARGIN, box[2], 1e-2f);
            assertEquals("maxY at " + angle, 528f - PAN_MARGIN, box[3], 1e-2f);
        }
    }

    @Test
    public void ac2_2_panLeavesAMarginOfTheVenueOnScreenAtAnyAngle() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 4f);
            for (float[] direction : new float[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}}) {
                camera.panBy(direction[0] * 100000f, direction[1] * 100000f);
                float[] box = boundingBox(camera);
                assertTrue("still visible at " + angle,
                        box[2] > 0f && box[0] < 390f && box[3] > 0f && box[1] < 528f);
            }
        }
    }

    @Test
    public void ac2_2_centersWhenBoundingBoxSmaller() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 1f);
            camera.panBy(120f, -80f);
            float[] box = boundingBox(camera);
            float boxWidth = box[2] - box[0];
            float boxHeight = box[3] - box[1];
            if (boxWidth <= 390f) {
                assertEquals("x center at " + angle, 390f / 2f, (box[0] + box[2]) / 2f, 1e-2f);
            }
            if (boxHeight <= 528f) {
                assertEquals("y center at " + angle, 528f / 2f, (box[1] + box[3]) / 2f, 1e-2f);
            }
        }
    }

    @Test
    public void panIsIgnoredUntilTheCameraIsReady() {
        MapCamera camera = new MapCamera(PADDING);
        camera.panBy(50f, 50f);
        assertEquals(0f, camera.getTx(), 0f);
        assertEquals(0f, camera.getTy(), 0f);
    }

    @Test
    public void bottomInsetMovesTheLowerBoundOfThePan() {
        MapCamera camera = camera(390f, 600f, 120f);
        camera.set(camera.getScale() * 3f, camera.getAngle(), 0f, 0f);
        camera.panBy(0f, -100000f);
        float[] box = boundingBox(camera);
        assertEquals(600f - 120f - PAN_MARGIN, box[3], 1e-2f);
    }

    // ---- AC-2.3, AC-2.4: zoom ----

    @Test
    public void ac2_3_zoomKeepsFocalPointFixed() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 4f);
            float fx = 390f / 2f + 25f, fy = 528f / 2f - 40f;
            float wx = camera.screenToWorldX(fx, fy), wy = camera.screenToWorldY(fx, fy);
            camera.zoomBy(1.2f, fx, fy);
            assertEquals("x at " + angle, fx, camera.worldToScreenX(wx, wy), 1e-2f);
            assertEquals("y at " + angle, fy, camera.worldToScreenY(wx, wy), 1e-2f);

            camera.zoomBy(0.8f, fx, fy);
            assertEquals("x back at " + angle, fx, camera.worldToScreenX(wx, wy), 1e-2f);
            assertEquals("y back at " + angle, fy, camera.worldToScreenY(wx, wy), 1e-2f);
        }
    }

    @Test
    public void ac2_3_zoomChangesTheScaleByTheGivenFactor() {
        MapCamera camera = zoomedCamera(-HALF_PI, 2f);
        float before = camera.getScale();
        camera.zoomBy(1.5f, 195f, 264f);
        assertEquals(before * 1.5f, camera.getScale(), 1e-4f);
    }

    @Test
    public void ac2_4_zoomClampsToMinMax() {
        for (float angle : new float[]{0f, -HALF_PI}) {
            MapCamera camera = zoomedCamera(angle, 2f);
            camera.zoomBy(100000f, 195f, 264f);
            assertEquals("max at " + angle, camera.getMaxScale(), camera.getScale(), 1e-4f);

            camera.zoomBy(0.00001f, 195f, 264f);
            assertEquals("min at " + angle, camera.getMinScale(), camera.getScale(), 1e-4f);
            assertEquals("min is fit at " + angle, camera.fitScale(angle), camera.getScale(), 1e-4f);
        }
    }

    @Test
    public void ac2_4_maxScaleIsSixTimesTheFitAtTheDefaultAngleWhateverTheCurrentAngle() {
        MapCamera rotated = zoomedCamera(0f, 1f);
        MapCamera upright = zoomedCamera(-HALF_PI, 1f);
        assertEquals(6f * upright.fitScale(-HALF_PI), rotated.getMaxScale(), 1e-4f);
        assertEquals(rotated.getMaxScale(), upright.getMaxScale(), 1e-4f);
    }

    @Test
    public void maxScaleMakesASeatAtLeastAFingerWide() {
        // Một ngón tay cần khoảng 48dp; ở mức zoom tối đa ghế phải to hơn thế.
        assertTrue(SEAT_DIAMETER * portrait().getMaxScale() >= 48f);
        assertTrue(SEAT_DIAMETER * landscape().getMaxScale() >= 48f);
    }

    @Test
    public void zoomingOutToTheMinimumCentersTheVenue() {
        MapCamera camera = zoomedCamera(-HALF_PI, 4f);
        camera.panBy(100000f, 100000f);
        camera.zoomBy(0.00001f, 20f, 20f);
        float[] box = boundingBox(camera);
        assertEquals(390f / 2f, (box[0] + box[2]) / 2f, 1e-2f);
        assertEquals(528f / 2f, (box[1] + box[3]) / 2f, 1e-2f);
    }

    @Test
    public void zoomIsIgnoredUntilTheCameraIsReady() {
        MapCamera camera = new MapCamera(PADDING);
        camera.zoomBy(2f, 10f, 10f);
        assertEquals(1f, camera.getScale(), 0f);
    }
}
