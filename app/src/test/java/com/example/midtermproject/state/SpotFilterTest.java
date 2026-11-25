package com.example.midtermproject.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SpotFilterTest {
    private final Venue venue = MockVenueFactory.create();

    private List<Spot> matching(SpotFilter filter) {
        List<Spot> result = new ArrayList<>();
        for (Spot spot : venue.getSpots()) {
            if (filter.matches(spot, false)) {
                result.add(spot);
            }
        }
        return result;
    }

    // ---- AC-4.2: chuẩn hóa và tìm theo mã ----

    @Test
    public void ac4_2_normalizeUppercasesAndDropsSpacesAndDashes() {
        assertEquals("VIPTA05", SpotFilter.normalize("vip t a05"));
        assertEquals("VIPTA05", SpotFilter.normalize("VIP-T-A05"));
        assertEquals("B05", SpotFilter.normalize("  b 0 5 "));
        assertEquals("", SpotFilter.normalize("   - - "));
        assertEquals("", SpotFilter.normalize(null));
    }

    @Test
    public void ac4_2_aFullCodeMatchesExactlyOneSpot() {
        for (String query : new String[]{"vip t a05", "VIP-T-A05", "ta05", "VIPTA05", "viptA05"}) {
            List<Spot> found = matching(SpotFilter.NONE.withQuery(query));
            assertEquals(query, 1, found.size());
            assertEquals("VIP-T-A05", found.get(0).id);
        }
    }

    @Test
    public void ac4_2_aRowAndNumberMatchesThatSpotInEverySection() {
        // Hàng A số 05 có ở cả 9 phân khu.
        List<Spot> found = matching(SpotFilter.NONE.withQuery("a05"));
        assertEquals(9, found.size());
        for (Spot spot : found) {
            assertEquals("A05", spot.label);
        }
    }

    @Test
    public void ac4_2_aZonePrefixMatchesTheWholeZone() {
        assertEquals(72, matching(SpotFilter.NONE.withQuery("vip")).size());
    }

    @Test
    public void ac4_7_aQueryWithNoMatchGivesAnEmptyResult() {
        assertTrue(matching(SpotFilter.NONE.withQuery("zzz")).isEmpty());
    }

    @Test
    public void ac4_1_zoneKeepsOnlyThatZone() {
        List<Spot> vip = matching(SpotFilter.NONE.withZone(ZoneType.VIP));
        assertEquals(72, vip.size());
        for (Spot spot : vip) {
            assertEquals(ZoneType.VIP, spot.zone);
        }
        assertEquals(224, matching(SpotFilter.NONE.withZone(null)).size());
    }

    // ---- AC-4.3, AC-4.4, AC-4.5 ----

    @Test
    public void ac4_3_availableOnlyKeepsOnlyOpenSpots() {
        List<Spot> open = matching(SpotFilter.NONE.withAvailableOnly(true));
        assertEquals(133, open.size());
        for (Spot spot : open) {
            assertEquals(SpotStatus.AVAILABLE, spot.baseStatus);
        }
    }

    @Test
    public void ac4_3_aTicketOfMineIsNotOpenEvenThoughItsBaseStatusIs() {
        Spot open = null;
        for (Spot spot : venue.getSpots()) {
            if (spot.baseStatus == SpotStatus.AVAILABLE) {
                open = spot;
                break;
            }
        }
        SpotFilter availableOnly = SpotFilter.NONE.withAvailableOnly(true);
        assertTrue(availableOnly.matches(open, false));
        assertFalse(availableOnly.matches(open, true));
        // Khi không lọc "chỉ chỗ trống" thì vé của bạn vẫn khớp.
        assertTrue(SpotFilter.NONE.matches(open, true));
    }

    @Test
    public void ac4_4_priceRangeIsInclusiveOnBothEnds() {
        // Giá VIP: hàng A 2.500.000, B 2.200.000, C 1.900.000, mỗi hàng 24 ghế.
        assertEquals(24, matching(SpotFilter.NONE.withPriceRange(1_900_000L, 1_900_000L)).size());
        assertEquals(48, matching(SpotFilter.NONE.withPriceRange(2_200_000L, 2_500_000L)).size());
        assertEquals(0, matching(SpotFilter.NONE.withPriceRange(1_900_001L, 2_199_999L)).size());
    }

    @Test
    public void ac4_4_aReversedRangeIsSwapped() {
        SpotFilter filter = SpotFilter.NONE.withPriceRange(2_500_000L, 2_200_000L);
        assertEquals(2_200_000L, filter.minPrice);
        assertEquals(2_500_000L, filter.maxPrice);
    }

    @Test
    public void ac4_5_combinedCriteriaAreAnded() {
        SpotFilter filter = SpotFilter.NONE
                .withZone(ZoneType.VIP).withQuery("g").withAvailableOnly(true)
                .withPriceRange(2_200_000L, 2_500_000L);
        List<Spot> found = matching(filter);
        assertFalse(found.isEmpty());
        for (Spot spot : found) {
            assertEquals(ZoneType.VIP, spot.zone);
            assertTrue(spot.searchKey.contains("G"));
            assertEquals(SpotStatus.AVAILABLE, spot.baseStatus);
            assertTrue(spot.price >= 2_200_000L && spot.price <= 2_500_000L);
        }
        // Bớt một tiêu chí thì số kết quả không thể giảm.
        assertTrue(matching(filter.withAvailableOnly(false)).size() >= found.size());
    }

    // ---- AC-4.6, AC-4.8: có đang lọc không ----

    @Test
    public void ac4_6_isActiveOnlyWhenSomethingDiffersFromTheDefault() {
        assertFalse(SpotFilter.NONE.isActive());
        assertTrue(SpotFilter.NONE.withZone(ZoneType.VIP).isActive());
        assertTrue(SpotFilter.NONE.withQuery("a").isActive());
        assertTrue(SpotFilter.NONE.withAvailableOnly(true).isActive());
        assertTrue(SpotFilter.NONE.withPriceRange(SpotFilter.PRICE_MIN + 1, SpotFilter.PRICE_MAX).isActive());
        assertTrue(SpotFilter.NONE.withPriceRange(SpotFilter.PRICE_MIN, SpotFilter.PRICE_MAX - 1).isActive());
    }

    @Test
    public void ac4_8_backToTheDefaultsIsNotActiveAgain() {
        SpotFilter filter = SpotFilter.NONE.withZone(ZoneType.VIP).withQuery("x")
                .withAvailableOnly(true).withPriceRange(1_000_000L, 2_000_000L);
        assertTrue(filter.isActive());
        SpotFilter cleared = filter.withZone(null).withQuery("").withAvailableOnly(false)
                .withPriceRange(SpotFilter.PRICE_MIN, SpotFilter.PRICE_MAX);
        assertFalse(cleared.isActive());
        assertEquals(SpotFilter.NONE, cleared);
    }

    @Test
    public void aQueryOfOnlySpacesAndDashesIsNotActive() {
        assertFalse(SpotFilter.NONE.withQuery(" - - ").isActive());
    }

    @Test
    public void ac4_8_noneMatchesEverySpot() {
        assertEquals(224, matching(SpotFilter.NONE).size());
    }

    // ---- Bất biến và so sánh ----

    @Test
    public void withMethodsReturnNewFiltersAndLeaveTheOriginalUntouched() {
        SpotFilter original = SpotFilter.NONE;
        SpotFilter changed = original.withZone(ZoneType.STANDARD);
        assertEquals(null, original.zone);
        assertEquals(ZoneType.STANDARD, changed.zone);
    }

    @Test
    public void filtersWithTheSameCriteriaAreEqualEvenIfTheRawQueryDiffers() {
        assertEquals(SpotFilter.NONE.withQuery("vip g"), SpotFilter.NONE.withQuery("VIP-G"));
        assertEquals(SpotFilter.NONE.withQuery("vip g").hashCode(), SpotFilter.NONE.withQuery("VIP-G").hashCode());
        assertFalse(SpotFilter.NONE.withQuery("a").equals(SpotFilter.NONE.withQuery("b")));
    }

    @Test
    public void theRawQueryIsKeptAsTypedForTheSearchBox() {
        assertEquals("vip t A05", SpotFilter.NONE.withQuery("vip t A05").rawQuery);
        assertEquals("", SpotFilter.NONE.withQuery(null).rawQuery);
    }
}
