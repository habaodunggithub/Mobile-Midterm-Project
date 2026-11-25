package com.example.midtermproject.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PriceFormatterTest {
    @Test
    public void smallAmountsHaveNoSeparator() {
        assertEquals("0", PriceFormatter.format(0L));
        assertEquals("7", PriceFormatter.format(7L));
        assertEquals("999", PriceFormatter.format(999L));
    }

    @Test
    public void thousandsAreSeparatedByDots() {
        assertEquals("1.000", PriceFormatter.format(1_000L));
        assertEquals("12.345", PriceFormatter.format(12_345L));
        assertEquals("900.000", PriceFormatter.format(900_000L));
    }

    @Test
    public void millionsAreSeparatedEveryThreeDigits() {
        assertEquals("2.500.000", PriceFormatter.format(2_500_000L));
        assertEquals("400.000", PriceFormatter.format(400_000L));
        assertEquals("20.000.000", PriceFormatter.format(20_000_000L));
        assertEquals("1.234.567.890", PriceFormatter.format(1_234_567_890L));
    }

    @Test
    public void theLargestSelectionTotalIsFormattedCorrectly() {
        // 8 chỗ VIP hàng A, 2.500.000 đồng mỗi chỗ.
        assertEquals("20.000.000", PriceFormatter.format(8 * 2_500_000L));
    }

    @Test
    public void negativeAmountsKeepTheSignBeforeTheDigits() {
        assertEquals("-1.500", PriceFormatter.format(-1_500L));
        assertEquals("-999", PriceFormatter.format(-999L));
    }

    @Test
    public void extremeValuesDoNotOverflowTheFormatting() {
        assertEquals("9.223.372.036.854.775.807", PriceFormatter.format(Long.MAX_VALUE));
        assertEquals("-9.223.372.036.854.775.808", PriceFormatter.format(Long.MIN_VALUE));
    }
}
