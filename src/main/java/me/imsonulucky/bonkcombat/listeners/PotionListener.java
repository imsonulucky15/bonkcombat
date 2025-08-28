package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PotionListener implements Listener {

    private final BonkCombat plugin;

    public PotionListener(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfig().getBoolean("auto_delete_empty_potion_bottles", true)) return;

        Player player = event.getPlayer();
        ItemStack consumed = event.getItem();

        if (!consumed.getType().name().endsWith("POTION")) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null && item.getType() == Material.GLASS_BOTTLE) {
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                        } else {
                            player.getInventory().setItem(i, null);
                        }
                        break;
                    }
                }
            }
        }.runTaskLater(plugin, 2L);
    }
}
