package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.integrations.VaultHook;
import me.imsonulucky.bonkcombat.integrations.WorldGuardHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class RegionRewardListener implements Listener {

    private final BonkCombat plugin;

    public RegionRewardListener(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        if (!plugin.getConfig().getBoolean("reward.region-money-reward.enabled")) return;
        List<String> regions = plugin.getConfig().getStringList("reward.region-money-reward.regions");
        boolean debug = plugin.getConfig().getBoolean("reward.region-money-reward.debug", false);

        if (!WorldGuardHook.isInRegion(victim, regions)) {
            if (debug) plugin.getLogger().info("[CombatReward] " + victim.getName() + " is not in a reward region.");
            return;
        }

        Economy eco = VaultHook.getEconomy();
        if (eco == null) {
            if (debug) plugin.getLogger().warning("[CombatReward] Vault economy not found.");
            return;
        }

        double percentage = plugin.getConfig().getDouble("reward.region-money-reward.percentage", 20.0);
        double victimBalance = eco.getBalance(victim);
        double reward = victimBalance * (percentage / 100.0);

        if (debug) {
            plugin.getLogger().info("[CombatReward] Victim balance: $" + victimBalance);
            plugin.getLogger().info("[CombatReward] Reward calculated: $" + reward);
        }

        String prefix = plugin.getConfig().getString("prefix", "&c&lCOMBAT &8»");

        if (reward <= 0) {
            killer.sendMessage(plugin.color(
                    plugin.getMessages().getString("reward_zero")
                            .replace("{prefix}", prefix)
                            .replace("{victim}", victim.getName())
            ));
            return;
        }

        eco.withdrawPlayer(victim, reward);
        eco.depositPlayer(killer, reward);

        killer.sendMessage(plugin.color(
                plugin.getMessages().getString("reward_received")
                        .replace("{prefix}", prefix)
                        .replace("{amount}", String.format("%.2f", reward))
                        .replace("{victim}", victim.getName())
        ));

        victim.sendMessage(plugin.color(
                plugin.getMessages().getString("reward_lost")
                        .replace("{prefix}", prefix)
                        .replace("{amount}", String.format("%.2f", reward))
                        .replace("{killer}", killer.getName())
        ));
    }
}
