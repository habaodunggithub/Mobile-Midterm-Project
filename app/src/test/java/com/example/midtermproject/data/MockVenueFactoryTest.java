package com.example.midtermproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MockVenueFactoryTest {

    private static List<Spot> spotsOf(Venue venue, ZoneType zone) {
        List<Spot> result = new ArrayList<>();
        for (Spot spot : venue.getSpots()) {
            if (spot.zone == zone) {
                result.add(spot);
            }
        }
        return result;
    }

    private static Section sectionById(Venue venue, String id) {
        for (Section section : venue.getSections()) {
            if (section.id.equals(id)) {
                return section;
            }
        }
        throw new AssertionError("Missing section " + id);
    }

    // ---- Bất biến chung, đúng với mọi phân khu đã dựng ----

    @Test
    public void noTwoSpotsOverlap() {
        List<Spot> spots = MockVenueFactory.create().getSpots();
        for (int i = 0; i < spots.size(); i++) {
            for (int j = i + 1; j < spots.size(); j++) {
                Spot a = spots.get(i);
                Spot b = spots.get(j);
                double distance = Math.hypot(a.cx - b.cx, a.cy - b.cy);
                assertTrue(a.id + " overlaps " + b.id, distance >= a.size + b.size);
            }
        }
    }

    @Test
    public void everySpotIsInsideTheWorld() {
        Venue venue = MockVenueFactory.create();
        for (Spot spot : venue.getSpots()) {
            assertTrue(spot.id, spot.cx - spot.size >= 0f);
            assertTrue(spot.id, spot.cy - spot.size >= 0f);
            assertTrue(spot.id, spot.cx + spot.size <= venue.worldWidth);
            assertTrue(spot.id, spot.cy + spot.size <= venue.worldHeight);
        }
    }

    @Test
    public void everySpotIsBelowTheStage() {
        Venue venue = MockVenueFactory.create();
        for (Spot spot : venue.getSpots()) {
            assertTrue(spot.id, spot.cy - spot.size >= venue.stageBottom);
        }
    }

    @Test
    public void createIsDeterministic() {
        List<Spot> first = MockVenueFactory.create().getSpots();
        List<Spot> second = MockVenueFactory.create().getSpots();
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).id, second.get(i).id);
            assertEquals(first.get(i).baseStatus, second.get(i).baseStatus);
            assertEquals(first.get(i).cx, second.get(i).cx, 0f);
            assertEquals(first.get(i).cy, second.get(i).cy, 0f);
        }
    }

    // ---- Bãi Standing ----

    @Test
    public void standingHasFortyEightCellsInTwoSections() {
        Venue venue = MockVenueFactory.create();
        assertEquals(48, spotsOf(venue, ZoneType.STANDING).size());
        assertEquals("Standing A", sectionById(venue, "STA-A").name);
        assertEquals("Standing B", sectionById(venue, "STA-B").name);
    }

    @Test
    public void standingIdsFollowSectionRowNumberPattern() {
        List<Spot> standing = spotsOf(MockVenueFactory.create(), ZoneType.STANDING);
        assertEquals("STA-A-A01", standing.get(0).id);
        assertEquals("STA-B-D06", standing.get(standing.size() - 1).id);
        for (Spot spot : standing) {
            assertTrue(spot.id, spot.id.matches("STA-[AB]-[A-D]0[1-6]"));
        }
    }

    @Test
    public void standingCellsAreSquareAndNeverLocked() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.STANDING)) {
            assertEquals(spot.id, SpotKind.STANDING_CELL, spot.kind);
            assertNotEquals(spot.id, SpotStatus.LOCKED, spot.baseStatus);
        }
    }

    @Test
    public void standingPriceDropsByOneHundredThousandPerRow() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.STANDING)) {
            long expected = 1_200_000L - (spot.rowLabel.charAt(0) - 'A') * 100_000L;
            assertEquals(spot.id, expected, spot.price);
        }
    }

    @Test
    public void standingBlocksMirrorEachOtherAroundTheStageCenter() {
        Venue venue = MockVenueFactory.create();
        Spot firstOfA = venue.findSpot("STA-A-A01");
        Spot lastOfB = venue.findSpot("STA-B-A06");
        assertEquals(2 * venue.arcCenterX, firstOfA.cx + lastOfB.cx, 0.001f);
        assertTrue(venue.findSpot("STA-A-A06").cx < venue.arcCenterX);
        assertTrue(venue.findSpot("STA-B-A01").cx > venue.arcCenterX);
    }

    @Test
    public void standingRowAIsNearestToTheStageAndLeavesACorridor() {
        Venue venue = MockVenueFactory.create();
        Spot rowA = venue.findSpot("STA-A-A01");
        Spot rowD = venue.findSpot("STA-A-D01");
        assertTrue(rowA.cy < rowD.cy);
        assertEquals(40f, (rowA.cy - rowA.size) - venue.stageBottom, 0.001f);
    }

    @Test
    public void standingLabelsSitInTheCorridorBetweenStageAndCells() {
        Venue venue = MockVenueFactory.create();
        Section a = sectionById(venue, "STA-A");
        assertEquals(110f, a.labelY, 0.001f);
        assertTrue(a.labelX < venue.arcCenterX);
    }

    // ---- VIP ----

    @Test
    public void vipHasSeventyTwoSeatsInThreeSections() {
        Venue venue = MockVenueFactory.create();
        assertEquals(72, spotsOf(venue, ZoneType.VIP).size());
        assertEquals("VIP Trái", sectionById(venue, "VIP-T").name);
        assertEquals("VIP Giữa", sectionById(venue, "VIP-G").name);
        assertEquals("VIP Phải", sectionById(venue, "VIP-P").name);
    }

    @Test
    public void vipSeatsAreRoundAndLieOnTheirRowRadius() {
        Venue venue = MockVenueFactory.create();
        float[] radii = {260f, 286f, 312f};
        for (Spot spot : spotsOf(venue, ZoneType.VIP)) {
            assertEquals(spot.id, SpotKind.SEAT, spot.kind);
            int row = spot.rowLabel.charAt(0) - 'A';
            double distance = Math.hypot(spot.cx - venue.arcCenterX, spot.cy - venue.arcCenterY);
            assertEquals(spot.id, radii[row], distance, 0.01);
        }
    }

    @Test
    public void vipIdsFollowSectionRowNumberPattern() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.VIP)) {
            assertTrue(spot.id, spot.id.matches("VIP-[TGP]-[A-C]0[1-8]"));
        }
    }

    @Test
    public void vipPriceDropsByThreeHundredThousandPerRow() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.VIP)) {
            long expected = 2_500_000L - (spot.rowLabel.charAt(0) - 'A') * 300_000L;
            assertEquals(spot.id, expected, spot.price);
        }
    }

    @Test
    public void seatNumbersGrowFromLeftToRightOnScreen() {
        Venue venue = MockVenueFactory.create();
        for (String row : new String[]{"A", "B", "C"}) {
            for (String section : new String[]{"VIP-T", "VIP-G", "VIP-P"}) {
                for (int number = 1; number < 8; number++) {
                    Spot left = venue.findSpot(section + "-" + row + "0" + number);
                    Spot right = venue.findSpot(section + "-" + row + "0" + (number + 1));
                    assertTrue(left.id + " should be left of " + right.id, left.cx < right.cx);
                }
            }
        }
    }

    @Test
    public void vipSectionsAreOrderedLeftToRightAndSeparatedByAisles() {
        Venue venue = MockVenueFactory.create();
        assertTrue(venue.findSpot("VIP-T-A08").cx < venue.findSpot("VIP-G-A01").cx);
        assertTrue(venue.findSpot("VIP-G-A08").cx < venue.findSpot("VIP-P-A01").cx);
        double aisle = Math.hypot(
                venue.findSpot("VIP-T-A08").cx - venue.findSpot("VIP-G-A01").cx,
                venue.findSpot("VIP-T-A08").cy - venue.findSpot("VIP-G-A01").cy);
        assertTrue("aisle between VIP sections: " + aisle, aisle >= 40.0);
    }

    @Test
    public void vipLabelsSitBetweenTheVipAndStandardRings() {
        Venue venue = MockVenueFactory.create();
        Section middle = sectionById(venue, "VIP-G");
        double distance = Math.hypot(middle.labelX - venue.arcCenterX,
                middle.labelY - venue.arcCenterY);
        assertEquals(344.0, distance, 0.01);
        assertEquals(venue.arcCenterX, middle.labelX, 0.01f);
    }

    @Test
    public void vipLockedSeatsAreTheMixerClusterInTheMiddleSection() {
        Venue venue = MockVenueFactory.create();
        for (Spot spot : spotsOf(venue, ZoneType.VIP)) {
            boolean expectedLocked = spot.sectionId.equals("VIP-G")
                    && spot.rowLabel.equals("C") && spot.number >= 3 && spot.number <= 6;
            assertEquals(spot.id, expectedLocked, spot.baseStatus == SpotStatus.LOCKED);
        }
    }
}
