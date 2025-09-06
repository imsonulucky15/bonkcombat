package me.imsonulucky.bonkcombat.commands;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.util.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BCCommand implements CommandExecutor {

    private final CombatManager combatManager;
    private final BonkCombat plugin;

    public BCCommand(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.plugin = BonkCombat.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = "§8[§cBonkCombat§8] ";

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bonkcombat.reload")) {
                sender.sendMessage(prefix + "§cYou don't have permission to do that.");
                return true;
            }

            plugin.reloadConfig();
            plugin.getConfigManager().load();
            plugin.reloadMessages();

            if (plugin.getSafeZoneWarningListener() != null) {
                plugin.getSafeZoneWarningListener().reloadConfigValues();
            }

            sender.sendMessage(prefix + "§aConfig, messages, and listeners reloaded.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tag")) {
            if (!sender.hasPermission("bonkcombat.tag")) {
                sender.sendMessage(prefix + "§cYou don't have permission to do that.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found.");
                return true;
            }

            combatManager.tag(target);
            sender.sendMessage(prefix + "§aTagged §e" + target.getName() + " §afor combat.");
            return true;
        }
        sender.sendMessage(prefix + "§cUsage:");
        sender.sendMessage("§c - /bc reload");
        sender.sendMessage("§c - /bc tag <player>");
        return true;
    }
}
