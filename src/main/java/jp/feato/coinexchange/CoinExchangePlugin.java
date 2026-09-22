package jp.feato.coinexchange;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoinExchangePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            getLogger().severe("Vault Economy provider was not found; disabling FEATOCoinExchange.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        double exchangeValue = getConfig().getDouble("exchange-value", 10.0);
        if (!Double.isFinite(exchangeValue) || exchangeValue <= 0.0) {
            getLogger().severe("exchange-value must be a finite number greater than zero; disabling FEATOCoinExchange.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand command = getCommand("feato-coin-exchange");
        if (command == null) {
            getLogger().severe("Command feato-coin-exchange is missing from plugin.yml; disabling FEATOCoinExchange.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AncientCoinMatcher matcher = new AncientCoinMatcher(getServer().getItemFactory());
        command.setExecutor(new CoinExchangeCommand(this, registration.getProvider(), matcher, exchangeValue));
        getLogger().info("FEATOCoinExchange enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FEATOCoinExchange disabled.");
    }
}
