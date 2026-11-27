package com.example.midtermproject.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.ZoneType;

import org.junit.Test;

import java.util.Arrays;

public class SpotEffectsTest {
    private static final float EPS = 1e-4f;

    /** Ghế cách gốc (0, 0) đúng {@code distance} đơn vị theo trục x. */
    private static Spot seatAt(int number, float distance) {
        return new Spot("VIP-G", ZoneType.VIP, SpotKind.SEAT, "A", number,
                SpotStatus.AVAILABLE, 1_000_000L, distance, 0f, 10f);
    }

    @Test
    public void q43_idleSpotIsNeutral() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        assertEquals(1f, effects.scaleOf(spot, 1000L), EPS);
        assertEquals(0f, effects.shakeOf(spot, 1000L), EPS);
        assertEquals(-1f, effects.ringOf(spot, 1000L), EPS);
        assertFalse(effects.isRunning(1000L));
    }

    @Test
    public void q43_introRevealsNearSpotsFirst() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot near = seatAt(1, 50f);
        Spot far = seatAt(2, 400f);
        effects.startIntro(0L, 400f);
        long t = (long) (50 * SpotEffects.INTRO_DELAY_PER_UNIT_MS) + 200L;
        assertTrue("near spot already growing", effects.scaleOf(near, t) > 0f);
        assertEquals("far spot not shown yet", 0f, effects.scaleOf(far, t), EPS);
        long end = (long) (400 * SpotEffects.INTRO_DELAY_PER_UNIT_MS) + SpotEffects.INTRO_DURATION_MS;
        assertTrue(effects.isRunning(end - 1));
        assertFalse(effects.isRunning(end));
        assertEquals(1f, effects.scaleOf(far, end), EPS);
    }

    @Test
    public void q43_introOvershootsThenSettles() {
        float peak = 0f;
        for (long t = 0; t <= SpotEffects.INTRO_DURATION_MS; t += 5) {
            peak = Math.max(peak, SpotEffects.introScale(t));
        }
        assertTrue("overshoots above 1", peak > 1.05f);
        assertEquals(0f, SpotEffects.introScale(0L), EPS);
        assertEquals(1f, SpotEffects.introScale(SpotEffects.INTRO_DURATION_MS), EPS);
    }

    @Test
    public void q43_popGrowsShrinksThenReturns() {
        long d = SpotEffects.POP_DURATION_MS;
        assertEquals(1.5f, SpotEffects.popScale((long) (d * 0.35f)), 0.01f);
        assertEquals(0.9f, SpotEffects.popScale((long) (d * 0.7f)), 0.01f);
        assertEquals(1f, SpotEffects.popScale(d), EPS);
        assertEquals(1f, SpotEffects.popScale(0L), EPS);
    }

    @Test
    public void q43_popAddsRingThatFinishes() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        effects.pop(spot, 1000L);
        assertEquals(0f, effects.ringOf(spot, 1000L), EPS);
        float mid = effects.ringOf(spot, 1000L + SpotEffects.RING_DURATION_MS / 2);
        assertTrue(mid > 0.5f && mid < 1f);   // chậm dần: nửa thời gian đã đi hơn nửa đường
        assertTrue(effects.isRunning(1000L + SpotEffects.RING_DURATION_MS - 1));
        assertFalse(effects.isRunning(1000L + SpotEffects.RING_DURATION_MS));
        assertEquals(-1f, effects.ringOf(spot, 1000L + SpotEffects.RING_DURATION_MS), EPS);
    }

    @Test
    public void q43_newEffectReplacesOldOnSameSpot() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        effects.pop(spot, 0L);
        effects.shake(spot, 100L);
        assertEquals("pop removed", -1f, effects.ringOf(spot, 150L), EPS);
        assertTrue(Math.abs(effects.shakeOf(spot, 130L)) > 0f);
    }

    @Test
    public void q43_flipWaveStartsWithSpotNearestStage() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot near = seatAt(1, 50f);
        Spot far = seatAt(2, 300f);
        effects.flipInWave(Arrays.asList(far, near), 0L);
        long t = SpotEffects.FLIP_STAGGER_MS / 2;
        assertTrue("near spot flipping", effects.scaleOf(near, t) < 1f);
        assertEquals("far spot waits its turn", 1f, effects.scaleOf(far, t), EPS);
        assertTrue(effects.scaleOf(far, SpotEffects.FLIP_STAGGER_MS + 60L) < 1f);
    }

    @Test
    public void q43_shakeFadesOut() {
        assertEquals(0f, SpotEffects.shakeOffset(0L), EPS);
        assertEquals(0f, SpotEffects.shakeOffset(SpotEffects.SHAKE_DURATION_MS), EPS);
        float early = 0f, late = 0f;
        for (long t = 0; t < SpotEffects.SHAKE_DURATION_MS / 3; t += 5) {
            early = Math.max(early, Math.abs(SpotEffects.shakeOffset(t)));
        }
        for (long t = 2 * SpotEffects.SHAKE_DURATION_MS / 3; t < SpotEffects.SHAKE_DURATION_MS; t += 5) {
            late = Math.max(late, Math.abs(SpotEffects.shakeOffset(t)));
        }
        assertTrue(late < early);
        assertTrue(early <= 1f);
    }

    @Test
    public void q43_disabledEffectsStayNeutral() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        effects.pop(spot, 0L);
        effects.setEnabled(false);
        assertEquals(1f, effects.scaleOf(spot, 100L), EPS);
        effects.startIntro(0L, 400f);
        effects.shake(spot, 0L);
        assertEquals(1f, effects.scaleOf(spot, 10L), EPS);
        assertEquals(0f, effects.shakeOf(spot, 100L), EPS);
        assertFalse(effects.isRunning(10L));
    }

    @Test
    public void q49_focusRingStartsWideAndSettles() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        effects.settleFocus(spot, 1000L);
        assertEquals(1f, effects.focusSettleOf(spot, 1000L), EPS);
        float half = effects.focusSettleOf(spot, 1000L + SpotEffects.FOCUS_SETTLE_MS / 2);
        assertEquals("(1 - 0,5)^2 = 0,25", 0.25f, half, 0.01f);
        assertEquals(0f, effects.focusSettleOf(spot, 1000L + SpotEffects.FOCUS_SETTLE_MS), EPS);
    }

    @Test
    public void q49_focusSettleOnlyForFocusedSpot() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot focused = seatAt(1, 100f);
        Spot other = seatAt(2, 200f);
        effects.settleFocus(focused, 0L);
        assertEquals(0f, effects.focusSettleOf(other, 10L), EPS);
    }

    @Test
    public void q49_focusSettleKeepsMapRedrawingUntilDone() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        assertFalse("nothing focused yet", effects.isRunning(5000L));
        effects.settleFocus(spot, 5000L);
        assertTrue(effects.isRunning(5000L + SpotEffects.FOCUS_SETTLE_MS - 1));
        assertFalse(effects.isRunning(5000L + SpotEffects.FOCUS_SETTLE_MS));
    }

    @Test
    public void q49_disabledEffectsSkipFocusSettle() {
        SpotEffects effects = new SpotEffects(0f, 0f);
        Spot spot = seatAt(1, 100f);
        effects.setEnabled(false);
        effects.settleFocus(spot, 0L);
        assertEquals(0f, effects.focusSettleOf(spot, 10L), EPS);
        assertFalse(effects.isRunning(10L));
    }
}
