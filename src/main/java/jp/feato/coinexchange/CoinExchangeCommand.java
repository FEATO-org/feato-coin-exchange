package jp.feato.coinexchange;

import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

final class CoinExchangeCommand implements CommandExecutor {
    private static final Component NO_COIN = Component.text("交換できる古銭を持っていません。", NamedTextColor.RED);
    private static final Component ECONOMY_ERROR =
            Component.text("古銭の交換に失敗しました。管理者にお問い合わせください。", NamedTextColor.RED);

    private final JavaPlugin plugin;
    private final Economy economy;
    private final AncientCoinMatcher matcher;
    private final double exchangeValue;

    CoinExchangeCommand(JavaPlugin plugin, Economy economy, AncientCoinMatcher matcher, double exchangeValue) {
        this.plugin = plugin;
        this.economy = economy;
        this.matcher = matcher;
        this.exchangeValue = exchangeValue;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text("このコマンドはコンソール専用です。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " <player>", NamedTextColor.RED));
            return true;
        }

        Player player = plugin.getServer().getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage(Component.text("Online player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        CoinSlot coinSlot = findCoin(player.getInventory());
        if (coinSlot == null) {
            player.sendMessage(NO_COIN);
            return true;
        }

        ItemStack original = coinSlot.item().clone();
        consumeOne(coinSlot);

        EconomyResponse response;
        try {
            response = economy.depositPlayer(player, exchangeValue);
        } catch (RuntimeException exception) {
            restore(coinSlot, original, player, "Vault deposit threw an exception", exception);
            return true;
        }

        if (response == null || !response.transactionSuccess()) {
            String detail = response == null ? "null EconomyResponse" : response.errorMessage;
            restore(coinSlot, original, player, "Vault deposit failed: " + detail, null);
            return true;
        }

        String amount = BalanceFormatter.format(exchangeValue);
        String balance = BalanceFormatter.format(economy.getBalance(player));
        player.sendMessage(Component.text("古銭1枚を" + amount + "Gに交換しました。", NamedTextColor.GREEN));
        player.sendMessage(Component.text("現在残高: " + balance + "G", NamedTextColor.YELLOW));
        return true;
    }

    private CoinSlot findCoin(PlayerInventory inventory) {
        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (matcher.matches(item)) {
                return new CoinSlot(inventory, slot, false, item);
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        return matcher.matches(offHand) ? new CoinSlot(inventory, -1, true, offHand) : null;
    }

    private void consumeOne(CoinSlot slot) {
        ItemStack reduced = slot.item().clone();
        if (reduced.getAmount() == 1) {
            slot.set(null);
        } else {
            reduced.setAmount(reduced.getAmount() - 1);
            slot.set(reduced);
        }
    }

    private void restore(CoinSlot slot, ItemStack original, Player player, String reason, RuntimeException exception) {
        try {
            slot.set(original);
        } catch (RuntimeException restoreException) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to restore an Ancient Coin for player " + player.getName(), restoreException);
            if (exception != null) {
                restoreException.addSuppressed(exception);
            }
            player.sendMessage(ECONOMY_ERROR);
            return;
        }

        if (exception == null) {
            plugin.getLogger().warning(reason + " for player " + player.getName()
                    + " (amount=" + exchangeValue + ")");
        } else {
            plugin.getLogger().log(Level.SEVERE,
                    reason + " for player " + player.getName() + " (amount=" + exchangeValue + ")", exception);
        }
        player.sendMessage(ECONOMY_ERROR);
    }

    private record CoinSlot(PlayerInventory inventory, int index, boolean offHand, ItemStack item) {
        void set(ItemStack replacement) {
            if (offHand) {
                inventory.setItemInOffHand(replacement);
            } else {
                inventory.setItem(index, replacement);
            }
        }
    }
}
