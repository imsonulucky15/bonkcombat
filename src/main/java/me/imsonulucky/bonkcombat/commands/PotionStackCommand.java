package me.imsonulucky.bonkcombat.commands;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.potions.PotionStacker;
import me.imsonulucky.bonkcombat.util.ColorUtil;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class PotionStackCommand implements CommandExecutor {

    private final BonkCombat plugin;

    public PotionStackCommand(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("&cOnly players can use this command."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("potionstack.enabled", true)) {
            player.sendMessage(ColorUtil.parse("&cPotion stacking is disabled in the config."));
            return true;
        }

        if (!player.hasPermission("bonkcombat.potionstack")) {
            player.sendMessage(ColorUtil.parse("&cYou do not have permission to use this command."));
            return true;
        }

        FileConfiguration config = ConfigManager.get().getConfig();
        int maxStack = config.getInt("potionstack.max-stacked-potion-items", 16);

        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = inv.getItem(i);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PotionStacker.StackResult result = PotionStacker.stackSplashPotions(contents, maxStack);

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 36; i++) {
                        inv.setItem(i, result.updated[i]);
                    }

                    player.sendMessage(ConfigManager.getMessage("potionstack-success"));

                    if (result.maxReached) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("{max}", String.valueOf(maxStack));
                        player.sendMessage(ConfigManager.getMessage("potionstack-max-reached", placeholders));
                    }
                }
            }.runTask(plugin);
        });

        return true;
    }
}
