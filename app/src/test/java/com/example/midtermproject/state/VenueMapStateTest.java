package com.example.midtermproject.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.model.DisplayState;
import com.example.midtermproject.model.SelectionResult;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VenueMapStateTest {
    private Venue venue;
    private VenueMapState state;
    private int notifications;

    @Before
    public void setUp() {
        venue = MockVenueFactory.create();
        state = new VenueMapState(venue);
        notifications = 0;
        state.addListener(() -> notifications++);
    }

    private List<Spot> spotsWith(SpotStatus status, ZoneType zone) {
        List<Spot> result = new ArrayList<>();
        for (Spot spot : venue.getSpots()) {
            if (spot.baseStatus == status && (zone == null || spot.zone == zone)) {
                result.add(spot);
            }
        }
        return result;
    }

    private Spot anAvailableSpot() {
        return spotsWith(SpotStatus.AVAILABLE, null).get(0);
    }

    private List<DisplayState> allDisplayStates() {
        List<DisplayState> states = new ArrayList<>();
        for (Spot spot : venue.getSpots()) {
            states.add(state.getDisplayState(spot));
        }
        return states;
    }

    // ---- AC-3.1, AC-3.2: chọn và bỏ chọn ----

    @Test
    public void ac3_1_selectsAnAvailableSpotAndFocusesIt() {
        Spot spot = anAvailableSpot();
        assertEquals(SelectionResult.SELECTED, state.toggleSelection(spot.id));
        assertTrue(state.isSelected(spot.id));
        assertEquals(DisplayState.SELECTED, state.getDisplayState(spot));
        assertEquals(spot.id, state.getFocusedId());
        assertEquals(1, state.getSelectedCount());
        assertEquals(1, notifications);
    }

    @Test
    public void ac3_2_deselectRestoresTheSpotAndTouchesNoOtherSpot() {
        Spot spot = anAvailableSpot();
        List<DisplayState> before = allDisplayStates();
        state.toggleSelection(spot.id);
        assertEquals(SelectionResult.DESELECTED, state.toggleSelection(spot.id));
        assertEquals(DisplayState.AVAILABLE, state.getDisplayState(spot));
        assertEquals(before, allDisplayStates());
        assertEquals(0, state.getSelectedCount());
    }

    @Test
    public void ac3_1_selectingDoesNotChangeAnyOtherSpot() {
        Spot spot = anAvailableSpot();
        List<DisplayState> before = allDisplayStates();
        state.toggleSelection(spot.id);
        List<DisplayState> after = allDisplayStates();
        int changed = 0;
        for (int i = 0; i < before.size(); i++) {
            if (before.get(i) != after.get(i)) {
                changed++;
                assertEquals(spot, venue.getSpots().get(i));
            }
        }
        assertEquals(1, changed);
    }

    // ---- AC-3.3: giới hạn 8 chỗ ----

    @Test
    public void ac3_3_rejectsTheNinthSpotAndFocusesIt() {
        List<Spot> available = spotsWith(SpotStatus.AVAILABLE, null);
        for (int i = 0; i < VenueMapState.MAX_SELECTION; i++) {
            assertEquals(SelectionResult.SELECTED, state.toggleSelection(available.get(i).id));
        }
        Spot ninth = available.get(VenueMapState.MAX_SELECTION);
        assertEquals(SelectionResult.REJECTED_LIMIT, state.toggleSelection(ninth.id));
        assertEquals(8, state.getSelectedCount());
        assertFalse(state.isSelected(ninth.id));
        assertEquals(ninth.id, state.getFocusedId());

        // Bỏ một chỗ thì chọn được chỗ thứ chín.
        assertEquals(SelectionResult.DESELECTED, state.toggleSelection(available.get(0).id));
        assertEquals(SelectionResult.SELECTED, state.toggleSelection(ninth.id));
    }

    @Test
    public void ac3_3_aSelectedSpotCanStillBeDeselectedWhenFull() {
        List<Spot> available = spotsWith(SpotStatus.AVAILABLE, null);
        for (int i = 0; i < VenueMapState.MAX_SELECTION; i++) {
            state.toggleSelection(available.get(i).id);
        }
        assertEquals(SelectionResult.DESELECTED, state.toggleSelection(available.get(3).id));
        assertEquals(7, state.getSelectedCount());
    }

    // ---- AC-3.4, AC-3.5, AC-3.7: từ chối kèm lý do ----

    @Test
    public void ac3_4_rejectsABookedSpotButFocusesIt() {
        Spot booked = spotsWith(SpotStatus.BOOKED, null).get(0);
        Spot other = anAvailableSpot();
        state.toggleSelection(other.id);
        assertEquals(SelectionResult.REJECTED_BOOKED, state.toggleSelection(booked.id));
        assertEquals(booked.id, state.getFocusedId());
        assertEquals(1, state.getSelectedCount());
        assertTrue(state.isSelected(other.id));
        assertEquals(DisplayState.BOOKED, state.getDisplayState(booked));
    }

    @Test
    public void ac3_5_rejectsALockedSpotButFocusesIt() {
        Spot locked = spotsWith(SpotStatus.LOCKED, null).get(0);
        assertEquals(SelectionResult.REJECTED_LOCKED, state.toggleSelection(locked.id));
        assertEquals(locked.id, state.getFocusedId());
        assertEquals(0, state.getSelectedCount());
        assertEquals(DisplayState.LOCKED, state.getDisplayState(locked));
    }

    @Test
    public void ac3_7_rejectsASpotOutsideTheFilterButFocusesIt() {
        state.setFilter(SpotFilter.NONE.withZone(ZoneType.VIP));
        Spot standard = spotsWith(SpotStatus.AVAILABLE, ZoneType.STANDARD).get(0);
        assertEquals(SelectionResult.REJECTED_FILTERED, state.toggleSelection(standard.id));
        assertEquals(standard.id, state.getFocusedId());
        assertEquals(0, state.getSelectedCount());
        assertFalse(state.matchesFilter(standard));
    }

    // ---- AC-4.9: bộ lọc không làm mất lựa chọn ----

    @Test
    public void ac4_9_filterKeepsSelection() {
        Spot vip = spotsWith(SpotStatus.AVAILABLE, ZoneType.VIP).get(0);
        Spot standard = spotsWith(SpotStatus.AVAILABLE, ZoneType.STANDARD).get(0);
        state.toggleSelection(vip.id);
        state.toggleSelection(standard.id);
        long totalBefore = state.getSelectedTotal();

        state.setFilter(SpotFilter.NONE.withZone(ZoneType.VIP));

        assertEquals(2, state.getSelectedCount());
        assertTrue(state.isSelected(standard.id));
        assertFalse(state.matchesFilter(standard));
        assertEquals(DisplayState.SELECTED, state.getDisplayState(standard));
        assertEquals(totalBefore, state.getSelectedTotal());

        // Chạm vào chỗ đã chọn nhưng ngoài bộ lọc thì không bật tắt được.
        assertEquals(SelectionResult.REJECTED_FILTERED, state.toggleSelection(standard.id));
        assertTrue(state.isSelected(standard.id));
        // Muốn bỏ thì dùng nút xóa trong bảng chi tiết.
        state.removeSelection(standard.id);
        assertFalse(state.isSelected(standard.id));
    }

    @Test
    public void ac4_8_clearingTheFilterMatchesEverythingAgain() {
        state.setFilter(SpotFilter.NONE.withZone(ZoneType.VIP));
        assertEquals(72, state.getMatchCount());
        state.setFilter(SpotFilter.NONE);
        assertEquals(224, state.getMatchCount());
    }

    @Test
    public void ac4_7_aFilterWithNoMatchHasZeroMatches() {
        state.setFilter(SpotFilter.NONE.withQuery("zzz"));
        assertEquals(0, state.getMatchCount());
        assertFalse(state.matchesFilter(anAvailableSpot()));
    }

    // ---- Lệnh khác ----

    @Test
    public void removeSelectionRemovesAndNotifiesOnlyWhenSelected() {
        Spot spot = anAvailableSpot();
        state.toggleSelection(spot.id);
        notifications = 0;
        state.removeSelection(spot.id);
        assertEquals(1, notifications);
        assertFalse(state.isSelected(spot.id));
        state.removeSelection(spot.id);
        state.removeSelection("NOT-A-SPOT");
        assertEquals(1, notifications);
    }

    @Test
    public void setFocusedSetsAndClearsAndIgnoresNoChange() {
        Spot spot = anAvailableSpot();
        state.setFocused(spot.id);
        assertEquals(spot, state.getFocusedSpot());
        assertEquals(1, notifications);
        state.setFocused(spot.id);
        assertEquals(1, notifications);
        state.setFocused(null);
        assertNull(state.getFocusedSpot());
        assertEquals(2, notifications);
        state.setFocused(null);
        assertEquals(2, notifications);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toggleOfAnUnknownIdIsAnError() {
        state.toggleSelection("NOPE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void focusOfAnUnknownIdIsAnError() {
        state.setFocused("NOPE");
    }

    @Test
    public void selectionKeepsTheOrderItWasMadeIn() {
        List<Spot> available = spotsWith(SpotStatus.AVAILABLE, null);
        Spot a = available.get(10), b = available.get(2), c = available.get(30);
        state.toggleSelection(a.id);
        state.toggleSelection(b.id);
        state.toggleSelection(c.id);
        assertEquals(java.util.Arrays.asList(a, b, c), state.getSelectedSpots());
        state.removeSelection(b.id);
        assertEquals(java.util.Arrays.asList(a, c), state.getSelectedSpots());
        assertEquals(java.util.Arrays.asList(a.id, c.id), state.snapshotSelectedIds());
    }

    @Test
    public void selectedTotalIsTheSumOfThePrices() {
        List<Spot> available = spotsWith(SpotStatus.AVAILABLE, null);
        long expected = 0;
        for (int i = 0; i < 3; i++) {
            state.toggleSelection(available.get(i * 20).id);
            expected += available.get(i * 20).price;
        }
        assertEquals(expected, state.getSelectedTotal());
    }

    // ---- Người nghe ----

    @Test
    public void listenerNotifiedOncePerCommand() {
        Spot spot = anAvailableSpot();
        Spot booked = spotsWith(SpotStatus.BOOKED, null).get(0);
        state.toggleSelection(spot.id);                              // chọn
        assertEquals(1, notifications);
        state.toggleSelection(booked.id);                            // bị từ chối vẫn đổi chỗ đang xem
        assertEquals(2, notifications);
        state.toggleSelection(spot.id);                              // bỏ chọn
        assertEquals(3, notifications);
        state.setFilter(SpotFilter.NONE.withZone(ZoneType.VIP));     // đổi lọc
        assertEquals(4, notifications);
        state.setFilter(SpotFilter.NONE.withZone(ZoneType.VIP));     // không đổi gì
        assertEquals(4, notifications);
    }

    @Test
    public void aRemovedListenerIsNotCalledAndAListenerMayRemoveItselfWhileBeingCalled() {
        final int[] calls = {0, 0};
        VenueMapState.Listener once = new VenueMapState.Listener() {
            @Override
            public void onVenueMapStateChanged() {
                calls[0]++;
                state.removeListener(this);
            }
        };
        VenueMapState.Listener plain = () -> calls[1]++;
        state.addListener(once);
        state.addListener(plain);
        state.setFocused(anAvailableSpot().id);
        state.setFocused(null);
        assertEquals(1, calls[0]);
        assertEquals(2, calls[1]);
    }

    // ---- Bất biến sau chuỗi thao tác ngẫu nhiên ----

    @Test
    public void invariantsHoldAfterRandomOps() {
        Random random = new Random(42);
        List<Spot> spots = venue.getSpots();
        SpotFilter[] filters = {
                SpotFilter.NONE,
                SpotFilter.NONE.withZone(ZoneType.VIP),
                SpotFilter.NONE.withZone(ZoneType.STANDARD).withAvailableOnly(true),
                SpotFilter.NONE.withQuery("a0"),
                SpotFilter.NONE.withPriceRange(900_000L, 1_200_000L),
                SpotFilter.NONE.withQuery("zzz"),
        };
        for (int step = 0; step < 1000; step++) {
            switch (random.nextInt(4)) {
                case 0:
                    state.toggleSelection(spots.get(random.nextInt(spots.size())).id);
                    break;
                case 1:
                    state.removeSelection(spots.get(random.nextInt(spots.size())).id);
                    break;
                case 2:
                    state.setFilter(filters[random.nextInt(filters.length)]);
                    break;
                default:
                    state.setFocused(random.nextBoolean() ? null : spots.get(random.nextInt(spots.size())).id);
                    break;
            }
            assertInvariants(step);
        }
    }

    private void assertInvariants(int step) {
        assertTrue("step " + step, state.getSelectedCount() <= VenueMapState.MAX_SELECTION);
        for (Spot selected : state.getSelectedSpots()) {
            assertEquals("step " + step, SpotStatus.AVAILABLE, selected.baseStatus);
            assertFalse("step " + step, state.isMine(selected.id));
        }
        assertEquals("distinct ids at step " + step, state.getSelectedCount(),
                new java.util.HashSet<>(state.snapshotSelectedIds()).size());
        String focused = state.getFocusedId();
        assertTrue("step " + step, focused == null || venue.findSpot(focused) != null);
        int expectedMatches = 0;
        for (Spot spot : venue.getSpots()) {
            boolean matches = state.getFilter().matches(spot, state.isMine(spot.id));
            assertEquals("step " + step + " " + spot.id, matches, state.matchesFilter(spot));
            if (matches) {
                expectedMatches++;
            }
        }
        assertEquals("step " + step, expectedMatches, state.getMatchCount());
    }
}
