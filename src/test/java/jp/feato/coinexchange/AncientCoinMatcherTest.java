package jp.feato.coinexchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class AncientCoinMatcherTest {
    @Test
    void acceptsExpectedIdentity() {
        assertTrue(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, "ancient_coin", 1));
    }

    @Test
    void rejectsOrdinaryGoldNuggetWithoutCustomData() {
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, null, null));
    }

    @Test
    void rejectsMissingCustomDataFields() {
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, null, 1));
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, "ancient_coin", null));
    }

    @Test
    void rejectsWrongId() {
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, "another_coin", 1));
    }

    @Test
    void rejectsWrongSchema() {
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.GOLD_NUGGET, "ancient_coin", 2));
    }

    @Test
    void rejectsWrongMaterial() {
        assertFalse(AncientCoinMatcher.hasExpectedIdentity(Material.IRON_NUGGET, "ancient_coin", 1));
    }
}
