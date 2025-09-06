package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.util.ColorUtil;
import me.imsonulucky.bonkcombat.util.CombatManager;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlockListener implements Listener {

    private final BonkCombat plugin;
    private final CombatManager manager;

    public CommandBlockListener(BonkCombat plugin, CombatManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!manager.isInCombat(e.getPlayer())) return;

        if (e.getPlayer().isOp() || e.getPlayer().hasPermission("bonkcombat.bypass")) return;

        String command = e.getMessage().split(" ")[0].toLowerCase().replace("/", "");

        if (plugin.getConfig().getBoolean("command-control.whitelist.enabled")) {
            List<String> whitelist = plugin.getConfig().getStringList("command-control.whitelist.commands");
            if (whitelist.contains(command)) return;
        }

        if (plugin.getConfig().getBoolean("command-control.blacklist.enabled")) {
            List<String> blacklist = plugin.getConfig().getStringList("command-control.blacklist.commands");
            if (blacklist.contains("*") || blacklist.contains(command)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("");
                e.getPlayer().sendMessage(ColorUtil.parse(ConfigManager.getMessage("blocked_command")));
            }
        }
    }
}
