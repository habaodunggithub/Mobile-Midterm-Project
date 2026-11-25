package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.Venue;

import org.junit.Test;

import java.util.List;

public class HitTesterTest {
    private static final float VIEW_WIDTH = 390f;
    private static final float VIEW_HEIGHT = 528f;
    private static final float TOLERANCE_PX = 8f;
    private static final float[] ANGLES = {0f, -(float) (Math.PI / 2), (float) Math.toRadians(37)};

    private final Venue venue = MockVenueFactory.create();
    private final List<Spot> spots = venue.getSpots();

    /** Camera dọc, đặt ở {@code zoom} lần mức nhỏ nhất của góc đó, điểm sơ đồ (px, py) nằm giữa vùng nhìn. */
    private MapCamera cameraAt(float angle, float zoom, float px, float py) {
        MapCamera camera = new MapCamera(16f, 48f);
        camera.setWorldSize(venue.worldWidth, venue.worldHeight);
        camera.setViewport(VIEW_WIDTH, VIEW_HEIGHT, 0f);
        float scale = Math.min(camera.getMaxScale(), camera.fitScale(angle) * zoom);
        camera.set(scale, angle, 0f, 0f);
        float tx = VIEW_WIDTH / 2f - camera.worldToScreenX(px, py);
        float ty = VIEW_HEIGHT / 2f - camera.worldToScreenY(px, py);
        camera.set(scale, angle, tx, ty);
        return camera;
    }

    private MapCamera maxZoomCamera() {
        MapCamera camera = cameraAt(-(float) (Math.PI / 2), 1f, venue.worldWidth / 2f, venue.worldHeight / 2f);
        camera.zoomBy(1000f, VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f);
        return camera;
    }

    private Spot hit(MapCamera camera, float screenX, float screenY) {
        float wx = camera.screenToWorldX(screenX, screenY);
        float wy = camera.screenToWorldY(screenX, screenY);
        return HitTester.findSpotAt(spots, wx, wy, TOLERANCE_PX / camera.getScale());
    }

    // ---- Hình học cơ bản ----

    @Test
    public void ac3_9_hitsCenter() {
        MapCamera camera = cameraAt(0f, 1f, venue.worldWidth / 2f, venue.worldHeight / 2f);
        for (Spot spot : spots) {
            Spot found = HitTester.findSpotAt(spots, spot.cx, spot.cy, TOLERANCE_PX / camera.getScale());
            assertSame(spot.id, spot, found);
        }
    }

    @Test
    public void ac3_9_hitsWithinToleranceNearEdge() {
        MapCamera camera = maxZoomCamera();
        float tolerance = TOLERANCE_PX / camera.getScale();
        // Hàng D của Standard là hàng ngoài cùng nên hướng ra xa tâm vành cung không có chỗ nào khác.
        int tested = 0;
        for (Spot spot : spots) {
            if (spot.sectionId.startsWith("STD") && spot.rowLabel.equals("D")) {
                float dx = spot.cx - venue.arcCenterX, dy = spot.cy - venue.arcCenterY;
                float length = (float) Math.hypot(dx, dy);
                float reach = spot.size + tolerance;
                float insideX = spot.cx + dx / length * (reach - 0.1f), insideY = spot.cy + dy / length * (reach - 0.1f);
                float outsideX = spot.cx + dx / length * (reach + 0.1f), outsideY = spot.cy + dy / length * (reach + 0.1f);
                assertSame(spot.id + " just inside", spot, HitTester.findSpotAt(spots, insideX, insideY, tolerance));
                assertNull(spot.id + " just outside", HitTester.findSpotAt(spots, outsideX, outsideY, tolerance));
                tested++;
            }
        }
        assertTrue(tested > 20);
    }

    @Test
    public void ac3_9_missesAisle() {
        MapCamera camera = maxZoomCamera();
        float tolerance = TOLERANCE_PX / camera.getScale();
        // Giữa hàng C của VIP Giữa và hàng A của Standard 2/3 là lối đi rộng khoảng 40 đơn vị.
        Spot vipC = venue.findSpot("VIP-G-C01");
        Spot stdA = venue.findSpot("STD-2-A06");
        float midX = (vipC.cx + stdA.cx) / 2f, midY = (vipC.cy + stdA.cy) / 2f;
        assertNull(HitTester.findSpotAt(spots, midX, midY, tolerance));
    }

    @Test
    public void ac3_8_missesTheStageAndOutsideTheVenue() {
        MapCamera camera = maxZoomCamera();
        float tolerance = TOLERANCE_PX / camera.getScale();
        assertNull(HitTester.findSpotAt(spots, (venue.stageLeft + venue.stageRight) / 2f,
                (venue.stageTop + venue.stageBottom) / 2f, tolerance));
        assertNull(HitTester.findSpotAt(spots, -500f, -500f, tolerance));
        assertNull(HitTester.findSpotAt(spots, venue.worldWidth + 500f, venue.worldHeight + 500f, tolerance));
    }

    @Test
    public void ac3_9_nearestCenterWins() {
        // Ở mức fit, vùng nới của hai ghế kề nhau chồng lên nhau; điểm chạm gần ghế nào hơn thì chọn ghế đó.
        MapCamera camera = cameraAt(0f, 1f, venue.worldWidth / 2f, venue.worldHeight / 2f);
        float tolerance = TOLERANCE_PX / camera.getScale();
        Spot a = venue.findSpot("VIP-G-B02");
        Spot b = venue.findSpot("VIP-G-B03");
        float closerToB = 0.6f, closerToA = 0.4f;
        assertSame(b, HitTester.findSpotAt(spots, a.cx + (b.cx - a.cx) * closerToB, a.cy + (b.cy - a.cy) * closerToB, tolerance));
        assertSame(a, HitTester.findSpotAt(spots, a.cx + (b.cx - a.cx) * closerToA, a.cy + (b.cy - a.cy) * closerToA, tolerance));
    }

    @Test
    public void ac3_9_standingCellSquareHit() {
        MapCamera camera = maxZoomCamera();
        float tolerance = TOLERANCE_PX / camera.getScale();
        Spot cell = venue.findSpot("STA-A-B03");
        assertEquals(SpotKind.STANDING_CELL, cell.kind);
        // Góc của ô vuông cách tâm 14 đơn vị: một hình tròn cùng cỡ sẽ trượt, ô vuông thì trúng.
        assertSame(cell, HitTester.findSpotAt(spots, cell.cx + 9.9f, cell.cy + 9.9f, tolerance));
        assertSame(cell, HitTester.findSpotAt(spots, cell.cx - 9.9f, cell.cy + 9.9f, tolerance));
    }

    @Test
    public void emptyListFindsNothing() {
        assertNull(HitTester.findSpotAt(java.util.Collections.<Spot>emptyList(), 0f, 0f, 10f));
    }

    // ---- Đúng sau pan, zoom và xoay (điều kiện chấm điểm 25%) ----

    @Test
    public void ac3_9_correctAfterPanAndZoom() {
        float[] zooms = {1f, 2f, 1000f};                 // nhỏ nhất, 2 lần, lớn nhất
        float[][] pans = {{0f, 0f}, {120f, -80f}, {-300f, 200f}, {500f, 500f}};
        int configurations = 0;
        for (float angle : ANGLES) {
            for (float zoom : zooms) {
                for (float[] pan : pans) {
                    MapCamera camera = cameraAt(angle, zoom, venue.worldWidth / 2f, venue.worldHeight / 2f);
                    camera.panBy(pan[0], pan[1]);
                    int visible = checkEverySpotOnScreen(camera);
                    assertTrue("something visible at angle " + angle + " zoom " + zoom, visible > 0);
                    configurations++;
                }
            }
        }
        assertEquals(36, configurations);
    }

    @Test
    public void ac3_9_correctAfterZoomAroundAnOffCenterFocusAndRotation() {
        MapCamera camera = cameraAt(-(float) (Math.PI / 2), 1f, venue.worldWidth / 2f, venue.worldHeight / 2f);
        camera.zoomBy(2.5f, 90f, 400f);
        camera.rotateBy((float) Math.toRadians(23), 250f, 130f);
        camera.panBy(-60f, 45f);
        camera.zoomBy(1.4f, 300f, 200f);
        assertTrue(checkEverySpotOnScreen(camera) > 0);
    }

    /**
     * Với mọi chỗ đang nằm trong màn hình, chạm lệch khỏi tâm một đoạn nhỏ hơn nửa bán kính (tính
     * trên màn hình) phải trả về đúng chỗ đó. Trả về số chỗ đã kiểm.
     */
    private int checkEverySpotOnScreen(MapCamera camera) {
        int checked = 0;
        for (Spot spot : spots) {
            float sx = camera.worldToScreenX(spot.cx, spot.cy);
            float sy = camera.worldToScreenY(spot.cx, spot.cy);
            if (sx < 0f || sx > VIEW_WIDTH || sy < 0f || sy > VIEW_HEIGHT) {
                continue;
            }
            float offset = 0.45f * spot.size * camera.getScale();
            for (int k = 0; k < 6; k++) {
                double a = Math.toRadians(60 * k + 15);
                Spot found = hit(camera, sx + (float) (offset * Math.cos(a)), sy + (float) (offset * Math.sin(a)));
                assertNotNull(spot.id + " not found, offset " + k, found);
                assertSame(spot.id + " offset " + k, spot, found);
            }
            checked++;
        }
        return checked;
    }
}
