package com.example.midtermproject.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VenueTest {

    private static Spot spot(String row, int number) {
        return new Spot("VIP-G", ZoneType.VIP, SpotKind.SEAT, row, number,
                SpotStatus.AVAILABLE, 1_000_000L, number * 30f, 100f, 10f);
    }

    private static Venue venueOf(List<Spot> spots) {
        return new Venue(1080f, 630f, 390f, 30f, 690f, 90f,
                Collections.<Section>emptyList(), spots);
    }

    @Test
    public void findSpotReturnsSpotById() {
        Spot a = spot("A", 1);
        Venue venue = venueOf(Arrays.asList(a, spot("A", 2)));
        assertSame(a, venue.findSpot("VIP-G-A01"));
    }

    @Test
    public void findSpotReturnsNullForUnknownOrNullId() {
        Venue venue = venueOf(Arrays.asList(spot("A", 1)));
        assertNull(venue.findSpot("NOPE"));
        assertNull(venue.findSpot(null));
    }

    @Test
    public void arcCenterIsMiddleOfStageBottomEdge() {
        Venue venue = venueOf(Collections.<Spot>emptyList());
        assertEquals(540f, venue.arcCenterX, 0f);
        assertEquals(90f, venue.arcCenterY, 0f);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void spotListCannotBeModified() {
        venueOf(Arrays.asList(spot("A", 1))).getSpots().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sectionListCannotBeModified() {
        venueOf(Collections.<Spot>emptyList()).getSections().add(null);
    }

    @Test
    public void venueKeepsItsOwnCopyOfTheInputList() {
        List<Spot> input = new ArrayList<>(Arrays.asList(spot("A", 1)));
        Venue venue = venueOf(input);
        input.add(spot("A", 2));
        assertEquals(1, venue.getSpots().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateSpotIdsAreRejected() {
        venueOf(Arrays.asList(spot("A", 1), spot("A", 1)));
    }
}
