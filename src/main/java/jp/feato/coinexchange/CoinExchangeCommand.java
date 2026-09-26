package jp.feato.coinexchange;

import java.util.ArrayList;
import java.util.List;
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
    private static final Component CONSOLE_ONLY =
            Component.text("このコマンドはコンソールからのみ実行できます。", NamedTextColor.RED);
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
            sender.sendMessage(CONSOLE_ONLY);
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

        List<CoinSlot> coinSlots = findCoins(player.getInventory());
        if (coinSlots.isEmpty()) {
            player.sendMessage(NO_COIN);
            return true;
        }

        int coinCount = coinSlots.stream().mapToInt(slot -> slot.original().getAmount()).sum();
        double depositAmount = exchangeValue * coinCount;
        consumeAll(coinSlots);

        EconomyResponse response;
        try {
            response = economy.depositPlayer(player, depositAmount);
        } catch (RuntimeException exception) {
            restore(coinSlots, player, depositAmount, "Vault deposit threw an exception", exception);
            return true;
        }

        if (response == null || !response.transactionSuccess()) {
            String detail = response == null ? "null EconomyResponse" : response.errorMessage;
            restore(coinSlots, player, depositAmount, "Vault deposit failed: " + detail, null);
            return true;
        }

        String amount = BalanceFormatter.format(depositAmount);
        String balance = BalanceFormatter.format(economy.getBalance(player));
        player.sendMessage(Component.text(
                "古銭" + coinCount + "枚を" + amount + "Gに交換しました。", NamedTextColor.GREEN));
        player.sendMessage(Component.text("現在残高: " + balance + "G", NamedTextColor.YELLOW));
        return true;
    }

    private List<CoinSlot> findCoins(PlayerInventory inventory) {
        List<CoinSlot> result = new ArrayList<>();
        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (matcher.matches(item)) {
                result.add(new CoinSlot(inventory, slot, false, item.clone()));
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (matcher.matches(offHand)) {
            result.add(new CoinSlot(inventory, -1, true, offHand.clone()));
        }
        return result;
    }

    private void consumeAll(List<CoinSlot> slots) {
        for (CoinSlot slot : slots) {
            slot.set(null);
        }
    }

    private void restore(
            List<CoinSlot> slots,
            Player player,
            double amount,
            String reason,
            RuntimeException exception) {
        try {
            for (CoinSlot slot : slots) {
                slot.set(slot.original().clone());
            }
        } catch (RuntimeException restoreException) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to restore Ancient Coins for player " + player.getName(),
                    restoreException);
            if (exception != null) {
                restoreException.addSuppressed(exception);
            }
            player.sendMessage(ECONOMY_ERROR);
            return;
        }

        if (exception == null) {
            plugin.getLogger().warning(reason + " for player " + player.getName() + " (amount=" + amount + ")");
        } else {
            plugin.getLogger().log(
                    Level.SEVERE,
                    reason + " for player " + player.getName() + " (amount=" + amount + ")",
                    exception);
        }
        player.sendMessage(ECONOMY_ERROR);
    }

    private record CoinSlot(PlayerInventory inventory, int index, boolean offHand, ItemStack original) {
        void set(ItemStack replacement) {
            if (offHand) {
                inventory.setItemInOffHand(replacement);
            } else {
                inventory.setItem(index, replacement);
            }
        }
    }
}
