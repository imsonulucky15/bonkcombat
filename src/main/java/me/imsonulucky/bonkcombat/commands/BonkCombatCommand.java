package me.imsonulucky.bonkcombat.commands;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import me.imsonulucky.bonkcombat.util.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BonkCombatCommand implements CommandExecutor {

    private final BonkCombat plugin;

    public BonkCombatCommand(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bonkcombat.base")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        ConfigManager config = plugin.getConfigManager();
        CombatManager combatManager = plugin.getCombatManager();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            config.load();
            sender.sendMessage(config.getMessage("reload"));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tag")) {
            if (!sender.hasPermission("bonkcombat.testtag")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            combatManager.tag(target);
            sender.sendMessage("§aTagged §e" + target.getName() + "§a successfully.");
            return true;
        }

        sender.sendMessage("§cUsage: /bc reload OR /bc tag <player>");
        return true;
    }
}
