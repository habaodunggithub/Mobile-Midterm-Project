package com.example.midtermproject.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpotTest {

    private static Spot spot(String sectionId, String row, int number) {
        return new Spot(sectionId, ZoneType.VIP, SpotKind.SEAT, row, number,
                SpotStatus.AVAILABLE, 1_000_000L, 10f, 20f, 10f);
    }

    @Test
    public void labelPadsNumberToTwoDigits() {
        assertEquals("C05", spot("VIP-G", "C", 5).label);
        assertEquals("A12", spot("VIP-G", "A", 12).label);
    }

    @Test
    public void idIsSectionIdPlusLabel() {
        assertEquals("VIP-G-C05", spot("VIP-G", "C", 5).id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNumberBelowOne() {
        spot("VIP-G", "C", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveSize() {
        new Spot("VIP-G", ZoneType.VIP, SpotKind.SEAT, "C", 1,
                SpotStatus.AVAILABLE, 0L, 0f, 0f, 0f);
    }

    // ---- Hit test hình học (AC-3.9) ----

    private static Spot seatAt(float cx, float cy) {
        return new Spot("VIP-G", ZoneType.VIP, SpotKind.SEAT, "A", 1,
                SpotStatus.AVAILABLE, 1L, cx, cy, 10f);
    }

    private static Spot cellAt(float cx, float cy) {
        return new Spot("STA-A", ZoneType.STANDING, SpotKind.STANDING_CELL, "A", 1,
                SpotStatus.AVAILABLE, 1L, cx, cy, 10f);
    }

    @Test
    public void ac3_9_seatHitsAtItsCenterAndInsideTheRadius() {
        Spot seat = seatAt(100f, 200f);
        assertTrue(seat.hitTest(100f, 200f, 0f));
        assertTrue(seat.hitTest(109f, 200f, 0f));
        assertTrue(seat.hitTest(100f + 7f, 200f + 7f, 0f));    // cách tâm 9,9 < 10
    }

    @Test
    public void ac3_9_seatMissesOutsideTheRadiusButToleranceWidensIt() {
        Spot seat = seatAt(100f, 200f);
        assertFalse(seat.hitTest(111f, 200f, 0f));
        assertTrue(seat.hitTest(111f, 200f, 2f));
        assertTrue(seat.hitTest(100f, 200f - 12f, 2f));         // đúng mép của dung sai
        assertFalse(seat.hitTest(100f, 200f - 12.1f, 2f));
    }

    @Test
    public void ac3_9_seatIsRoundSoTheBoundingBoxCornerMisses() {
        Spot seat = seatAt(100f, 200f);
        // Góc của hình vuông ngoại tiếp cách tâm 14,1 > 10: không được nhận.
        assertFalse(seat.hitTest(110f, 210f, 0f));
    }

    @Test
    public void ac3_9_standingCellIsSquareSoItsCornerHits() {
        Spot cell = cellAt(100f, 200f);
        assertTrue(cell.hitTest(110f, 210f, 0f));               // góc ô vuông
        assertTrue(cell.hitTest(90f, 190f, 0f));
        assertFalse(cell.hitTest(110.1f, 200f, 0f));
        assertTrue(cell.hitTest(112f, 200f, 2f));
        assertFalse(cell.hitTest(112.1f, 200f, 2f));
    }

    @Test
    public void ac3_9_distanceSqIsTheSquaredDistanceToTheCenter() {
        assertEquals(25f, seatAt(0f, 0f).distanceSqTo(3f, 4f), 1e-5f);
        assertEquals(0f, cellAt(7f, 7f).distanceSqTo(7f, 7f), 0f);
    }

    // ---- Khóa tìm kiếm ----

    @Test
    public void searchKeyIsTheUppercaseIdWithoutDashes() {
        assertEquals("VIPGC05", spot("VIP-G", "C", 5).searchKey);
        assertEquals("STA-A".replace("-", "") + "A01",
                new Spot("STA-A", ZoneType.STANDING, SpotKind.STANDING_CELL, "A", 1,
                        SpotStatus.AVAILABLE, 1L, 0f, 0f, 10f).searchKey);
    }
}
