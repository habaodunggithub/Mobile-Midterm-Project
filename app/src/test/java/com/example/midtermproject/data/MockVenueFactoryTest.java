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
import java.util.Arrays;
import java.util.List;

public class MockVenueFactoryTest {

    /** Hai chỗ khác phân khu phải cách nhau ít nhất một đường kính ghế cộng lối đi tối thiểu. */
    private static final double MIN_CROSS_SECTION_DISTANCE = 2 * MockVenueFactory.SEAT_RADIUS + 14;

    /** Khoảng cách từ tâm vành cung tới (x, y) sau khi bỏ hệ số oval, tức bán kính "danh nghĩa". */
    private static double ringRadius(Venue venue, float x, float y) {
        double dx = (x - venue.arcCenterX) / MockVenueFactory.RING_SCALE_X;
        double dy = (y - venue.arcCenterY) / MockVenueFactory.RING_SCALE_Y;
        return Math.hypot(dx, dy);
    }

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
        assertEquals(MockVenueFactory.STANDING_GAP, (rowA.cy - rowA.size) - venue.stageBottom, 0.001f);
    }

    @Test
    public void standingLabelsSitInTheCorridorBetweenStageAndCells() {
        Venue venue = MockVenueFactory.create();
        Section a = sectionById(venue, "STA-A");
        assertEquals(MockVenueFactory.STAGE_BOTTOM + MockVenueFactory.STANDING_GAP / 2f, a.labelY, 0.001f);
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
        float[] radii = MockVenueFactory.VIP_ROW_RADII;
        for (Spot spot : spotsOf(venue, ZoneType.VIP)) {
            assertEquals(spot.id, SpotKind.SEAT, spot.kind);
            int row = spot.rowLabel.charAt(0) - 'A';
            assertEquals(spot.id, radii[row], ringRadius(venue, spot.cx, spot.cy), 0.01);
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
        assertTrue("aisle between VIP sections: " + aisle, aisle >= MIN_CROSS_SECTION_DISTANCE);
    }

    @Test
    public void vipLabelsSitBetweenTheVipAndStandardRings() {
        Venue venue = MockVenueFactory.create();
        Section middle = sectionById(venue, "VIP-G");
        assertEquals(MockVenueFactory.VIP_LABEL_RADIUS,
                ringRadius(venue, middle.labelX, middle.labelY), 0.01);
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

    // ---- Standard ----

    @Test
    public void standardHasOneHundredFourSeatsInFourSections() {
        Venue venue = MockVenueFactory.create();
        assertEquals(104, spotsOf(venue, ZoneType.STANDARD).size());
        for (String id : new String[]{"STD-1", "STD-2", "STD-3", "STD-4"}) {
            assertEquals("Standard " + id.charAt(4), sectionById(venue, id).name);
        }
    }

    @Test
    public void standardRowsHaveSixSixSevenSevenSeats() {
        Venue venue = MockVenueFactory.create();
        int[] expected = {6, 6, 7, 7};
        for (String section : new String[]{"STD-1", "STD-2", "STD-3", "STD-4"}) {
            for (int row = 0; row < 4; row++) {
                String rowLabel = String.valueOf((char) ('A' + row));
                int count = 0;
                for (Spot spot : venue.getSpots()) {
                    if (spot.sectionId.equals(section) && spot.rowLabel.equals(rowLabel)) {
                        count++;
                    }
                }
                assertEquals(section + " row " + rowLabel, expected[row], count);
            }
        }
    }

    @Test
    public void standardIdsFollowSectionRowNumberPattern() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.STANDARD)) {
            assertTrue(spot.id, spot.id.matches("STD-[1-4]-[A-D]0[1-7]"));
            assertEquals(spot.id, SpotKind.SEAT, spot.kind);
        }
    }

    @Test
    public void standardPriceDropsByOneHundredThousandPerRow() {
        for (Spot spot : spotsOf(MockVenueFactory.create(), ZoneType.STANDARD)) {
            long expected = 700_000L - (spot.rowLabel.charAt(0) - 'A') * 100_000L;
            assertEquals(spot.id, expected, spot.price);
        }
    }

    @Test
    public void standardSectionsAreOrderedLeftToRight() {
        Venue venue = MockVenueFactory.create();
        assertTrue(venue.findSpot("STD-1-A06").cx < venue.findSpot("STD-2-A01").cx);
        assertTrue(venue.findSpot("STD-2-A06").cx < venue.findSpot("STD-3-A01").cx);
        assertTrue(venue.findSpot("STD-3-A06").cx < venue.findSpot("STD-4-A01").cx);
    }

    // ---- Toàn bộ sơ đồ ----

    @Test
    public void totalSpotCountMeetsTheExamRange() {
        int total = MockVenueFactory.create().getSpots().size();
        assertEquals(224, total);
        assertTrue("exam grading needs 150-250 objects", total >= 150 && total <= 250);
    }

    @Test
    public void venueHasNineSectionsAndEverySpotBelongsToOne() {
        Venue venue = MockVenueFactory.create();
        assertEquals(9, venue.getSections().size());
        for (Spot spot : venue.getSpots()) {
            sectionById(venue, spot.sectionId);
        }
    }

    @Test
    public void exactlyEightSpotsAreLockedAtTheDesignedPlaces() {
        List<String> locked = new ArrayList<>();
        for (Spot spot : MockVenueFactory.create().getSpots()) {
            if (spot.baseStatus == SpotStatus.LOCKED) {
                locked.add(spot.id);
            }
        }
        // `locked` được gom theo thứ tự dựng sơ đồ, nên so sánh trực tiếp với danh sách mong đợi.
        assertEquals(Arrays.asList(
                "VIP-G-C03", "VIP-G-C04", "VIP-G-C05", "VIP-G-C06",
                "STD-1-D01", "STD-1-D02", "STD-4-D06", "STD-4-D07"), locked);
    }

    @Test
    public void bookedCountIsAboutOneThird() {
        int booked = 0;
        for (Spot spot : MockVenueFactory.create().getSpots()) {
            if (spot.baseStatus == SpotStatus.BOOKED) {
                booked++;
            }
        }
        assertTrue("booked = " + booked, booked >= 60 && booked <= 90);
    }

    @Test
    public void allThreeBaseStatusesAppear() {
        boolean available = false;
        boolean booked = false;
        boolean locked = false;
        for (Spot spot : MockVenueFactory.create().getSpots()) {
            available |= spot.baseStatus == SpotStatus.AVAILABLE;
            booked |= spot.baseStatus == SpotStatus.BOOKED;
            locked |= spot.baseStatus == SpotStatus.LOCKED;
        }
        assertTrue(available && booked && locked);
    }

    @Test
    public void spotsOfDifferentSectionsAreSeparatedByAnAisle() {
        List<Spot> spots = MockVenueFactory.create().getSpots();
        for (int i = 0; i < spots.size(); i++) {
            for (int j = i + 1; j < spots.size(); j++) {
                Spot a = spots.get(i);
                Spot b = spots.get(j);
                if (!a.sectionId.equals(b.sectionId)) {
                    double distance = Math.hypot(a.cx - b.cx, a.cy - b.cy);
                    assertTrue(a.id + " and " + b.id + ": " + distance, distance >= MIN_CROSS_SECTION_DISTANCE);
                }
            }
        }
    }

    // ---- Bố cục siết gọn (Chặng 1B) ----

    @Test
    public void worldHasThePlannedSize() {
        Venue venue = MockVenueFactory.create();
        assertEquals(780f, venue.worldWidth, 0f);
        assertEquals(520f, venue.worldHeight, 0f);
    }

    @Test
    public void contentFillsTheWorldWithSmallMarginsOnAllSides() {
        Venue venue = MockVenueFactory.create();
        float left = venue.stageLeft, right = venue.stageRight, top = venue.stageTop;
        float bottom = venue.stageBottom;
        for (Spot spot : venue.getSpots()) {
            left = Math.min(left, spot.cx - spot.size);
            right = Math.max(right, spot.cx + spot.size);
            top = Math.min(top, spot.cy - spot.size);
            bottom = Math.max(bottom, spot.cy + spot.size);
        }
        float[] margins = {left, venue.worldWidth - right, top, venue.worldHeight - bottom};
        for (float margin : margins) {
            assertTrue("margin " + margin, margin >= 15f && margin <= 30f);
        }
    }

    @Test
    public void spotsInTheSameSectionKeepThreeUnitsOfClearance() {
        List<Spot> spots = MockVenueFactory.create().getSpots();
        for (int i = 0; i < spots.size(); i++) {
            for (int j = i + 1; j < spots.size(); j++) {
                Spot a = spots.get(i);
                Spot b = spots.get(j);
                if (a.sectionId.equals(b.sectionId)) {
                    double gap = Math.hypot(a.cx - b.cx, a.cy - b.cy) - a.size - b.size;
                    assertTrue(a.id + " and " + b.id + ": gap " + gap, gap >= 3.0);
                }
            }
        }
    }

    @Test
    public void spotsInDifferentSectionsKeepFourteenUnitsOfAisle() {
        List<Spot> spots = MockVenueFactory.create().getSpots();
        for (int i = 0; i < spots.size(); i++) {
            for (int j = i + 1; j < spots.size(); j++) {
                Spot a = spots.get(i);
                Spot b = spots.get(j);
                if (!a.sectionId.equals(b.sectionId)) {
                    double gap = Math.hypot(a.cx - b.cx, a.cy - b.cy) - a.size - b.size;
                    assertTrue(a.id + " and " + b.id + ": gap " + gap, gap >= 14.0);
                }
            }
        }
    }

    @Test
    public void statusesDoNotDependOnTheGeometry() {
        // Số ghế đã đặt chỉ phụ thuộc seed và thứ tự dựng, nên đổi hình học không được làm nó lệch.
        int booked = 0;
        for (Spot spot : MockVenueFactory.create().getSpots()) {
            if (spot.baseStatus == SpotStatus.BOOKED) {
                booked++;
            }
        }
        assertEquals(83, booked);
    }

    @Test
    public void seatDiameterAtFitMeetsTheTargetsInBothOrientations() {
        // Công thức fit đầy đủ nằm ở MapCamera (Chặng 3); ở đây chỉ kiểm kích thước world cho phép đạt mục tiêu.
        // Dọc: sơ đồ xoay 90 độ nên rộng/cao hoán đổi; vùng sơ đồ 390 x 528dp, lề 16dp mỗi bên.
        Venue venue = MockVenueFactory.create();
        double portraitFit = Math.min((390 - 32) / (double) venue.worldHeight,
                (528 - 32) / (double) venue.worldWidth);
        double landscapeFit = Math.min((631 - 32) / (double) venue.worldWidth,
                (277 - 32) / (double) venue.worldHeight);
        double diameter = 2 * MockVenueFactory.SEAT_RADIUS;
        assertTrue("portrait " + diameter * portraitFit, diameter * portraitFit >= 12.0);
        assertTrue("landscape " + diameter * landscapeFit, diameter * landscapeFit >= 9.0);
    }
}
