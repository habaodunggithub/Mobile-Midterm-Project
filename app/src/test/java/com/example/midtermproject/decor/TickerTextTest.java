package com.example.midtermproject.decor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.midtermproject.data.MockVenueFactory;
import com.example.midtermproject.model.Section;
import com.example.midtermproject.model.Spot;
import com.example.midtermproject.model.SpotKind;
import com.example.midtermproject.model.SpotStatus;
import com.example.midtermproject.model.Venue;
import com.example.midtermproject.model.ZoneType;
import com.example.midtermproject.ui.PriceFormatter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

public class TickerTextTest {
    private static final String FORMAT = "%1$s từ %2$s đ";

    private static Spot seat(String section, int number, long price) {
        return new Spot(section, ZoneType.VIP, SpotKind.SEAT, "A", number,
                SpotStatus.AVAILABLE, price, number * 30f, 100f, 10f);
    }

    private static Venue smallVenue() {
        return new Venue(780f, 520f, 260f, 20f, 520f, 70f,
                Arrays.asList(new Section("VIP-G", "VIP Giữa", ZoneType.VIP, 0f, 0f),
                        new Section("EMPTY", "Khu trống", ZoneType.VIP, 0f, 0f)),
                Arrays.asList(seat("VIP-G", 1, 2_500_000L), seat("VIP-G", 2, 1_900_000L)));
    }

    @Test
    public void q42_startsWithStageAndUsesCheapestPrice() {
        String text = TickerText.build(smallVenue(), "Sân khấu", FORMAT, PriceFormatter::format);
        assertEquals("SÂN KHẤU • VIP GIỮA TỪ 1.900.000 Đ • ", text);
    }

    @Test
    public void q42_skipsSectionWithoutSpots() {
        String text = TickerText.build(smallVenue(), "Sân khấu", FORMAT, PriceFormatter::format);
        assertFalse(text.contains("KHU TRỐNG"));
    }

    @Test
    public void q42_endsWithSeparatorSoItRepeatsSeamlessly() {
        String text = TickerText.build(MockVenueFactory.create(), "SÂN KHẤU", FORMAT, PriceFormatter::format);
        assertTrue(text.endsWith(TickerText.SEPARATOR));
    }

    @Test
    public void q42_mockVenueListsEverySection() {
        Venue venue = MockVenueFactory.create();
        String text = TickerText.build(venue, "SÂN KHẤU", FORMAT, PriceFormatter::format);
        for (Section section : venue.getSections()) {
            assertTrue(section.name, text.contains(section.name.toUpperCase(Locale.forLanguageTag("vi-VN")) + " TỪ "));
        }
    }

    @Test
    public void q42_cheapestPriceIsMinusOneForEmptySection() {
        assertEquals(-1L, TickerText.cheapestPrice(smallVenue(), "EMPTY"));
        assertEquals(1_900_000L, TickerText.cheapestPrice(smallVenue(), "VIP-G"));
    }
}
