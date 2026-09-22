package jp.feato.coinexchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BalanceFormatterTest {
    @Test
    void removesUnnecessaryFraction() {
        assertEquals("10", BalanceFormatter.format(10.0));
        assertEquals("190", BalanceFormatter.format(190.0));
    }

    @Test
    void preservesMeaningfulFraction() {
        assertEquals("10.5", BalanceFormatter.format(10.5));
        assertEquals("0.01", BalanceFormatter.format(0.01));
    }
}
