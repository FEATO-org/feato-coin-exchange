package jp.feato.coinexchange;

import io.papermc.paper.datacomponent.DataComponentType;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;

final class AncientCoinMatcher {
    static final String COIN_ID = "ancient_coin";
    static final int COIN_SCHEMA = 1;
    private static final NamespacedKey CUSTOM_DATA_KEY = NamespacedKey.minecraft("custom_data");
    private static final String COIN_SIGNATURE =
            "minecraft:gold_nugget[minecraft:custom_data={feato_coin:{id:\"" + COIN_ID
                    + "\",schema:" + COIN_SCHEMA + "}}]";

    private final ItemStack signature;
    private final DataComponentType customDataType;

    AncientCoinMatcher(ItemFactory itemFactory) {
        signature = itemFactory.createItemStack(COIN_SIGNATURE);
        customDataType = signature.getDataTypes().stream()
                .filter(type -> type.getKey().equals(CUSTOM_DATA_KEY))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Paper did not expose minecraft:custom_data"));
    }

    boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.GOLD_NUGGET || !item.hasData(customDataType)) {
            return false;
        }

        Set<DataComponentType> ignoredComponents = new HashSet<>(signature.getDataTypes());
        ignoredComponents.addAll(item.getDataTypes());
        ignoredComponents.remove(customDataType);
        return signature.matchesWithoutData(item, ignoredComponents, true);
    }

    static boolean hasExpectedIdentity(Material material, String id, Integer schema) {
        return material == Material.GOLD_NUGGET && COIN_ID.equals(id) && Integer.valueOf(COIN_SCHEMA).equals(schema);
    }
}
