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
import java.util.List;
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

    @Test
    public void q48_itemsAreUpperCaseAndStartWithStage() {
        List<String> items = TickerText.items(smallVenue(), "Sân khấu", FORMAT, PriceFormatter::format);
        assertEquals(Arrays.asList("SÂN KHẤU", "VIP GIỮA TỪ 1.900.000 Đ"), items);
    }

    @Test
    public void q48_fitToColumnsHasExactLength() {
        List<String> items = TickerText.items(MockVenueFactory.create(), "SÂN KHẤU", FORMAT, PriceFormatter::format);
        for (int columns : new int[]{40, 120, 333, 500}) {
            assertEquals(columns, TickerText.fitToColumns(items, columns).length());
        }
    }

    @Test
    public void q48_fitToColumnsKeepsWholeItemsInOrder() {
        List<String> items = Arrays.asList("AAAA", "BB", "CCCCCC");
        // 3 mục đầu dài 4+2+6 = 12 ký tự, cộng 3 dấu ngăn (3 ký tự mỗi dấu) = 21; mục thứ tư (lặp lại AAAA) cần 7 nữa,
        // nên không lấy nó mà bù 4 dấu cách vào ba dấu ngăn: 2, 1, 1.
        String text = TickerText.fitToColumns(items, 25);
        assertEquals("AAAA •   BB •  CCCCCC •  ", text);
        assertFalse("không cắt dở mục thứ tư", text.contains("CCCCCC • A"));
    }

    @Test
    public void q48_paddingIsSpreadOverSeparators() {
        List<String> items = Arrays.asList("AA", "BB");
        // Hai mục chiếm 2·(2+3) = 10 ký tự; thêm 1 mục nữa thành 15 > 13, nên bù 3 dấu cách: 2 sau dấu đầu, 1 sau dấu sau.
        assertEquals("AA •   BB •  ", TickerText.fitToColumns(items, 13));
    }

    @Test
    public void q48_itemsRepeatWhenLapIsLong() {
        List<String> items = Arrays.asList("AA", "BB");
        assertEquals("AA • BB • AA • BB • ", TickerText.fitToColumns(items, 20));
    }

    @Test
    public void q48_shortLapCutsFirstItem() {
        assertEquals("SÂN", TickerText.fitToColumns(Arrays.asList("SÂN KHẤU"), 3));
        assertEquals("", TickerText.fitToColumns(Arrays.asList("A"), 0));
    }
}
