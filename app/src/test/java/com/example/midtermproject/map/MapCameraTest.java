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

    // ---- AC-2.8, AC-2.9: xoay ----

    @Test
    public void ac2_8_rotateKeepsFocalPointFixed() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = zoomedCamera(angle, 4f);
            float fx = 390f / 2f - 30f, fy = 528f / 2f + 50f;
            float wx = camera.screenToWorldX(fx, fy), wy = camera.screenToWorldY(fx, fy);
            camera.rotateBy(0.35f, fx, fy);
            assertEquals("x at " + angle, fx, camera.worldToScreenX(wx, wy), 1e-2f);
            assertEquals("y at " + angle, fy, camera.worldToScreenY(wx, wy), 1e-2f);
        }
    }

    @Test
    public void ac2_8_rotateChangesTheAngleByExactlyTheDelta() {
        MapCamera camera = zoomedCamera(0.2f, 4f);
        camera.rotateBy(0.5f, 195f, 264f);
        assertEquals(0.7f, camera.getAngle(), 1e-5f);
        camera.rotateBy(-1.2f, 195f, 264f);
        assertEquals(-0.5f, camera.getAngle(), 1e-5f);
    }

    @Test
    public void ac2_8_angleStaysInTheHalfOpenRangeMinusPiToPi() {
        MapCamera camera = zoomedCamera(3f, 4f);
        camera.rotateBy(0.5f, 195f, 264f);          // 3,5 rad vượt quá pi
        assertEquals(3.5f - (float) (2 * Math.PI), camera.getAngle(), 1e-5f);
        assertTrue(camera.getAngle() > -Math.PI && camera.getAngle() <= Math.PI);
    }

    @Test
    public void ac2_8_normalizeAngleHandlesFullTurnsAndTheBoundary() {
        assertEquals(0f, MapCamera.normalizeAngle(0f), 1e-6f);
        assertEquals(0.5f, MapCamera.normalizeAngle(0.5f + (float) (2 * Math.PI)), 1e-5f);
        assertEquals(-0.5f, MapCamera.normalizeAngle(-0.5f - (float) (4 * Math.PI)), 1e-5f);
        assertEquals((float) Math.PI, MapCamera.normalizeAngle((float) -Math.PI), 1e-5f);
        assertEquals((float) Math.PI, MapCamera.normalizeAngle((float) Math.PI), 1e-5f);
    }

    @Test
    public void ac2_9_panFollowsTheFingerAfterRotating() {
        MapCamera camera = zoomedCamera(0f, 4f);
        camera.rotateBy((float) Math.toRadians(37), 195f, 264f);
        float beforeX = camera.worldToScreenX(300f, 200f);
        float beforeY = camera.worldToScreenY(300f, 200f);
        camera.panBy(11f, 7f);
        assertEquals(beforeX + 11f, camera.worldToScreenX(300f, 200f), 1e-3f);
        assertEquals(beforeY + 7f, camera.worldToScreenY(300f, 200f), 1e-3f);
    }

    @Test
    public void ac2_9_zoomKeepsTheFocalPointFixedAfterRotating() {
        MapCamera camera = zoomedCamera(0f, 4f);
        camera.rotateBy((float) Math.toRadians(37), 195f, 264f);
        float fx = 160f, fy = 300f;
        float wx = camera.screenToWorldX(fx, fy), wy = camera.screenToWorldY(fx, fy);
        camera.zoomBy(1.15f, fx, fy);
        assertEquals(fx, camera.worldToScreenX(wx, wy), 1e-2f);
        assertEquals(fy, camera.worldToScreenY(wx, wy), 1e-2f);
    }

    @Test
    public void ac2_2_boundsStillHoldWhenRotatedAwayFromTheDefault() {
        MapCamera camera = zoomedCamera(-HALF_PI, 4f);
        camera.rotateBy((float) Math.toRadians(37), 195f, 264f);
        camera.panBy(100000f, 100000f);
        float[] box = boundingBox(camera);
        assertEquals(PAN_MARGIN, box[0], 1e-2f);
        assertEquals(PAN_MARGIN, box[1], 1e-2f);
        camera.panBy(-100000f, -100000f);
        box = boundingBox(camera);
        assertEquals(390f - PAN_MARGIN, box[2], 1e-2f);
        assertEquals(528f - PAN_MARGIN, box[3], 1e-2f);
    }

    @Test
    public void rotatingBackToTheDefaultNeverLeavesTheScaleBelowTheNewMinimum() {
        // Ở góc 37 độ mức vừa khít nhỏ hơn ở góc mặc định; xoay về mặc định phải nâng tỉ lệ lên.
        MapCamera camera = portrait();
        float angle = (float) Math.toRadians(37);
        camera.set(camera.fitScale(angle), angle, 0f, 0f);
        camera.rotateBy(camera.defaultAngle() - angle, 195f, 264f);
        assertTrue(camera.getScale() >= camera.getMinScale() - 1e-5f);
        assertEquals(camera.defaultAngle(), camera.getAngle(), 1e-5f);
    }

    @Test
    public void rotateIsIgnoredUntilTheCameraIsReady() {
        MapCamera camera = new MapCamera(PADDING);
        camera.rotateBy(1f, 10f, 10f);
        assertEquals(0f, camera.getAngle(), 0f);
    }

    // ---- AC-2.10, AC-2.11: snap và góc mặc định ----

    private static final float EIGHT_DEGREES = (float) Math.toRadians(8);

    private static MapCamera atAngleDegrees(double degrees) {
        MapCamera camera = portrait();
        camera.set(camera.getScale(), (float) Math.toRadians(degrees), camera.getTx(), camera.getTy());
        return camera;
    }

    @Test
    public void ac2_10_snapWithinEightDegrees() {
        assertEquals(HALF_PI, atAngleDegrees(95).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
        assertEquals(HALF_PI, atAngleDegrees(83).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
        assertEquals(-HALF_PI, atAngleDegrees(-93).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
        assertEquals(0f, atAngleDegrees(-7.9).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
        assertEquals(0f, atAngleDegrees(8).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
    }

    @Test
    public void ac2_10_snapHandlesTheHalfTurn() {
        assertEquals((float) Math.PI, atAngleDegrees(175).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
        assertEquals((float) Math.PI, atAngleDegrees(-176).nearestRightAngle(EIGHT_DEGREES), 1e-5f);
    }

    @Test
    public void ac2_10_noSnapOutsideEightDegrees() {
        assertTrue(Float.isNaN(atAngleDegrees(37).nearestRightAngle(EIGHT_DEGREES)));
        assertTrue(Float.isNaN(atAngleDegrees(99).nearestRightAngle(EIGHT_DEGREES)));
        assertTrue(Float.isNaN(atAngleDegrees(-45).nearestRightAngle(EIGHT_DEGREES)));
        assertTrue(Float.isNaN(atAngleDegrees(8.5).nearestRightAngle(EIGHT_DEGREES)));
    }

    @Test
    public void ac2_11_isAtDefaultAngleTolerance() {
        // Dọc: góc mặc định là -90 độ.
        assertTrue(atAngleDegrees(-90).isAtDefaultAngle());
        assertTrue(atAngleDegrees(-90.4).isAtDefaultAngle());
        assertTrue(atAngleDegrees(-89.6).isAtDefaultAngle());
        assertFalse(atAngleDegrees(-90.6).isAtDefaultAngle());
        assertFalse(atAngleDegrees(-89.4).isAtDefaultAngle());
        assertFalse(atAngleDegrees(0).isAtDefaultAngle());
    }

    @Test
    public void ac2_11_isAtDefaultAngleInLandscapeUsesZero() {
        MapCamera camera = landscape();
        assertTrue(camera.isAtDefaultAngle());
        camera.set(camera.getScale(), 0.2f, camera.getTx(), camera.getTy());
        assertFalse(camera.isAtDefaultAngle());
    }

    @Test
    public void ac2_11_shortestAngleDeltaGoesTheShortWay() {
        assertEquals(0.5f, MapCamera.shortestAngleDelta(0.2f, 0.7f), 1e-5f);
        assertEquals(-0.5f, MapCamera.shortestAngleDelta(0.7f, 0.2f), 1e-5f);
        // Từ 170 độ sang -170 độ chỉ là 20 độ theo chiều dương, không phải -340 độ.
        float delta = MapCamera.shortestAngleDelta((float) Math.toRadians(170), (float) Math.toRadians(-170));
        assertEquals((float) Math.toRadians(20), delta, 1e-5f);
    }

    // ---- AC-2.13: snapshot ----

    @Test
    public void ac2_13_snapshotOfTheFitViewIsTheWorldCenterAtRatioOne() {
        float[] snapshot = portrait().snapshot();
        assertEquals(WORLD_WIDTH / 2f, snapshot[0], 1e-2f);
        assertEquals(WORLD_HEIGHT / 2f, snapshot[1], 1e-2f);
        assertEquals(1f, snapshot[2], 1e-4f);
    }

    @Test
    public void ac2_13_snapshotRestoresCenterAndScaleAcrossOrientation() {
        // Phóng đủ lớn để khung bao lớn hơn vùng nhìn ở hướng ngang, nếu không sơ đồ bị căn giữa
        // và tâm vùng nhìn không còn là điểm đã lưu (đúng theo AC-2.2).
        MapCamera portrait = zoomedCamera((float) Math.toRadians(37), 5f);
        portrait.panBy(30f, -20f);
        float[] snapshot = portrait.snapshot();

        MapCamera landscape = landscape();
        landscape.set(landscape.getScale() * 1.7f, 0.9f, 50f, 20f);   // trạng thái bất kỳ trước khi khôi phục
        assertTrue(landscape.restoreSnapshot(snapshot));

        assertEquals("angle is the default of the new orientation", 0f, landscape.getAngle(), 1e-6f);
        float regionCenterX = 631f / 2f, regionCenterY = 277f / 2f;
        assertEquals(snapshot[0], landscape.screenToWorldX(regionCenterX, regionCenterY), 0.5f);
        assertEquals(snapshot[1], landscape.screenToWorldY(regionCenterX, regionCenterY), 0.5f);
        assertEquals(snapshot[2], landscape.getScale() / landscape.fitScale(0f), 1e-3f);
    }

    @Test
    public void ac2_13_restoreFromLandscapeBackToPortraitUsesTheDefaultPortraitAngle() {
        MapCamera landscape = landscape();
        landscape.zoomBy(1.6f, 300f, 140f);
        float[] snapshot = landscape.snapshot();

        MapCamera portrait = portrait();
        assertTrue(portrait.restoreSnapshot(snapshot));
        assertEquals(-HALF_PI, portrait.getAngle(), 1e-6f);
        assertEquals(snapshot[2], portrait.getScale() / portrait.fitScale(-HALF_PI), 1e-3f);
    }

    @Test
    public void ac2_13_restoreClampsTheRatioToTheScaleLimits() {
        MapCamera camera = portrait();
        assertTrue(camera.restoreSnapshot(new float[]{390f, 260f, 0.4f}));
        assertEquals(camera.getMinScale(), camera.getScale(), 1e-5f);
        assertTrue(camera.restoreSnapshot(new float[]{390f, 260f, 1000f}));
        assertEquals(camera.getMaxScale(), camera.getScale(), 1e-5f);
    }

    @Test
    public void ac2_13_invalidSnapshotsAreRejectedAndLeaveTheCameraUntouched() {
        float[][] invalid = {
                null,
                {1f, 2f},
                {1f, 2f, 3f, 4f},
                {Float.NaN, 100f, 2f},
                {100f, Float.POSITIVE_INFINITY, 2f},
                {100f, 100f, Float.NaN},
                {100f, 100f, 0f},
                {100f, 100f, -1f},
                {-5f, 100f, 2f},
                {100f, 9999f, 2f},
        };
        for (float[] snapshot : invalid) {
            MapCamera camera = zoomedCamera(0.3f, 3f);
            float scale = camera.getScale(), angle = camera.getAngle(), tx = camera.getTx(), ty = camera.getTy();
            assertFalse("rejects " + java.util.Arrays.toString(snapshot), camera.restoreSnapshot(snapshot));
            assertEquals(scale, camera.getScale(), 0f);
            assertEquals(angle, camera.getAngle(), 0f);
            assertEquals(tx, camera.getTx(), 0f);
            assertEquals(ty, camera.getTy(), 0f);
        }
    }

    @Test
    public void ac2_13_restoreNeedsAReadyCamera() {
        MapCamera camera = new MapCamera(PADDING);
        assertFalse(camera.restoreSnapshot(new float[]{100f, 100f, 1f}));
    }

    // ---- AC-5.4: đưa một điểm về giữa vùng nhìn phía trên bảng chi tiết ----

    private static MapCamera withSheet(float angle) {
        MapCamera camera = camera(390f, 700f, 120f);       // bảng chi tiết thu gọn che 120px phía dưới
        camera.set(camera.fitScale(angle), angle, 0f, 0f);
        float dx = 390f / 2f - camera.worldToScreenX(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        float dy = (700f - 120f) / 2f - camera.worldToScreenY(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        camera.set(camera.getScale(), angle, dx, dy);
        return camera;
    }

    private static void apply(MapCamera camera, float[] target) {
        camera.set(target[0], target[1], target[2], target[3]);
    }

    @Test
    public void ac5_4_centerOnTargetPutsThePointInTheMiddleOfTheAreaAboveTheSheet() {
        for (float angle : TEST_ANGLES) {
            MapCamera camera = withSheet(angle);
            float wx = 395f, wy = 265f;                         // sát tâm sơ đồ nên không bị kẹp biên
            apply(camera, camera.centerOnTarget(wx, wy, camera.getMinScale() * 2.5f));
            assertEquals("x at " + angle, 390f / 2f, camera.worldToScreenX(wx, wy), 1e-2f);
            assertEquals("y at " + angle, (700f - 120f) / 2f, camera.worldToScreenY(wx, wy), 1e-2f);
        }
    }

    @Test
    public void ac5_4_theScaleIsAtLeastTheRequestedMinimum() {
        MapCamera camera = withSheet(-HALF_PI);
        float wanted = camera.getMinScale() * 2.5f;
        float[] target = camera.centerOnTarget(380f, 330f, wanted);
        assertEquals(wanted, target[0], 1e-4f);
    }

    @Test
    public void ac5_4_aSceneAlreadyZoomedInIsNotZoomedOut() {
        MapCamera camera = withSheet(-HALF_PI);
        camera.zoomBy(3.2f, 195f, 290f);
        float current = camera.getScale();
        float[] target = camera.centerOnTarget(380f, 330f, camera.getMinScale() * 2.5f);
        assertEquals(Math.max(current, camera.getMinScale() * 2.5f), target[0], 1e-4f);
        assertTrue(target[0] >= current - 1e-4f);
    }

    @Test
    public void ac5_4_theTargetScaleNeverExceedsTheMaximum() {
        MapCamera camera = withSheet(-HALF_PI);
        float[] target = camera.centerOnTarget(380f, 330f, camera.getMaxScale() * 10f);
        assertEquals(camera.getMaxScale(), target[0], 1e-4f);
    }

    @Test
    public void ac5_4_theTargetKeepsTheCurrentAngle() {
        MapCamera camera = withSheet((float) Math.toRadians(37));
        float[] target = camera.centerOnTarget(380f, 330f, camera.getMinScale() * 2.5f);
        assertEquals(camera.getAngle(), target[1], 0f);
    }

    @Test
    public void ac5_4_aPointNearTheEdgeIsClampedToTheBoundsInsteadOfBeingCentered() {
        MapCamera camera = withSheet(-HALF_PI);
        float wx = 20f, wy = 480f;                             // góc dưới trái sơ đồ
        float[] target = camera.centerOnTarget(wx, wy, camera.getMinScale() * 2.5f);
        apply(camera, target);
        float[] box = boundingBox(camera);
        // Khung bao vẫn nằm trong giới hạn kéo, nên điểm không thể nằm đúng giữa.
        assertTrue(box[0] <= PAN_MARGIN + 1e-2f && box[2] >= 390f - PAN_MARGIN - 1e-2f);
        assertTrue(box[1] <= PAN_MARGIN + 1e-2f && box[3] >= (700f - 120f) - PAN_MARGIN - 1e-2f);
        // Điểm vẫn nằm trong vùng nhìn (không bị đẩy ra khỏi màn hình).
        float sx = camera.worldToScreenX(wx, wy), sy = camera.worldToScreenY(wx, wy);
        assertTrue(sx > 0f && sx < 390f && sy > 0f && sy < 580f);
    }

    @Test
    public void ac5_4_centerOnTargetDoesNotChangeTheCamera() {
        MapCamera camera = withSheet(-HALF_PI);
        float scale = camera.getScale(), tx = camera.getTx(), ty = camera.getTy();
        camera.centerOnTarget(100f, 100f, 5f);
        assertEquals(scale, camera.getScale(), 0f);
        assertEquals(tx, camera.getTx(), 0f);
        assertEquals(ty, camera.getTy(), 0f);
    }

    @Test
    public void ac5_4_centerOnTargetOfAnUnreadyCameraReturnsTheCurrentState() {
        MapCamera camera = new MapCamera(PADDING);
        float[] target = camera.centerOnTarget(100f, 100f, 5f);
        assertEquals(1f, target[0], 0f);
        assertEquals(0f, target[2], 0f);
    }
}
