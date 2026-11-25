package com.example.midtermproject.model;

import static org.junit.Assert.assertEquals;

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
}
