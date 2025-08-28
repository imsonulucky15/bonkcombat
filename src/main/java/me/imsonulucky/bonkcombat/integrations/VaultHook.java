package me.imsonulucky.bonkcombat.integrations;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private static Economy econ = null;

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().warning("[VaultHook] Vault not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger().warning("[VaultHook] No economy provider registered with Vault.");
            return false;
        }

        econ = rsp.getProvider();
        Bukkit.getLogger().info("[VaultHook] Vault economy hooked: " + econ.getName());
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
}
