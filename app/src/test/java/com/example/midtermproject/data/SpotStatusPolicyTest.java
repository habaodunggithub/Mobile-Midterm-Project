package com.example.midtermproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.model.SpotStatus;

import org.junit.Test;

public class SpotStatusPolicyTest {

    private static final long SEED = 2026L;

    @Test
    public void lockedClustersAreExactlyTheEightDesignedSeats() {
        SpotStatusPolicy policy = new SpotStatusPolicy(SEED);
        String[][] locked = {
                {"VIP-G", "C", "3"}, {"VIP-G", "C", "4"}, {"VIP-G", "C", "5"}, {"VIP-G", "C", "6"},
                {"STD-1", "D", "1"}, {"STD-1", "D", "2"},
                {"STD-4", "D", "6"}, {"STD-4", "D", "7"},
        };
        for (String[] seat : locked) {
            assertEquals(SpotStatus.LOCKED,
                    policy.statusFor(seat[0], seat[1], Integer.parseInt(seat[2])));
        }
    }

    @Test
    public void seatsNextToLockedClustersAreNotLocked() {
        SpotStatusPolicy policy = new SpotStatusPolicy(SEED);
        assertNotEquals(SpotStatus.LOCKED, policy.statusFor("VIP-G", "C", 2));
        assertNotEquals(SpotStatus.LOCKED, policy.statusFor("VIP-G", "C", 7));
        assertNotEquals(SpotStatus.LOCKED, policy.statusFor("STD-1", "D", 3));
        assertNotEquals(SpotStatus.LOCKED, policy.statusFor("STD-4", "D", 5));
    }

    @Test
    public void standingCellsAreNeverLocked() {
        SpotStatusPolicy policy = new SpotStatusPolicy(SEED);
        for (int number = 1; number <= 6; number++) {
            assertNotEquals(SpotStatus.LOCKED, policy.statusFor("STA-A", "D", number));
            assertNotEquals(SpotStatus.LOCKED, policy.statusFor("STA-B", "D", number));
        }
    }

    @Test
    public void sameSeedGivesSameSequence() {
        SpotStatusPolicy first = new SpotStatusPolicy(SEED);
        SpotStatusPolicy second = new SpotStatusPolicy(SEED);
        for (int number = 1; number <= 200; number++) {
            assertEquals(first.statusFor("STD-2", "B", number),
                    second.statusFor("STD-2", "B", number));
        }
    }

    @Test
    public void lockedSeatsDoNotConsumeRandomNumbers() {
        // Xen kẽ chỗ LOCKED vào giữa không được làm lệch trạng thái của các chỗ còn lại.
        SpotStatusPolicy plain = new SpotStatusPolicy(SEED);
        SpotStatusPolicy withLocked = new SpotStatusPolicy(SEED);
        for (int number = 1; number <= 50; number++) {
            withLocked.statusFor("VIP-G", "C", 4);
            assertEquals(plain.statusFor("STD-2", "B", number),
                    withLocked.statusFor("STD-2", "B", number));
        }
    }

    @Test
    public void bookedShareIsAboutOneThird() {
        SpotStatusPolicy policy = new SpotStatusPolicy(SEED);
        int draws = 10_000;
        int booked = 0;
        for (int i = 0; i < draws; i++) {
            if (policy.statusFor("STD-2", "B", 1) == SpotStatus.BOOKED) {
                booked++;
            }
        }
        double share = booked / (double) draws;
        assertTrue("booked share was " + share,
                share > SpotStatusPolicy.BOOKED_RATE - 0.03
                        && share < SpotStatusPolicy.BOOKED_RATE + 0.03);
    }
}
