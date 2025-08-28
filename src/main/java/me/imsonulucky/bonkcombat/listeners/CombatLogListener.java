package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.utils.CombatManager;
import me.imsonulucky.bonkcombat.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class CombatLogListener implements Listener {

    private final BonkCombat plugin;
    private final CombatManager combatManager;

    public CombatLogListener(BonkCombat plugin, CombatManager manager) {
        this.plugin = plugin;
        this.combatManager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (!combatManager.isInCombat(player)) return;

        String punishment = plugin.getConfig().getString("combat-log-punishment", "kill");

        switch (punishment.toLowerCase()) {
            case "kill":
                player.setHealth(0.0);
                break;
            case "drop_inventory":
                player.getInventory().forEach(item -> {
                    if (item != null)
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                });
                player.getInventory().clear();
                break;
            default:
                break;
        }

        String msg = ConfigManager.getMessage("combat_log", Map.of("%username%", player.getName()));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
    }
}
