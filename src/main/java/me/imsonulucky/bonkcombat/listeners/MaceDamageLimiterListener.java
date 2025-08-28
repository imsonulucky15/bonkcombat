package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MaceDamageLimiterListener implements Listener {

    private final BonkCombat plugin;

    public MaceDamageLimiterListener(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("mace-damage-limiter.enabled")) return;

        if (!(event.getDamager() instanceof Player)) return;
        if (event.getEntityType() != EntityType.PLAYER) return;

        Player damager = (Player) event.getDamager();
        Material mainHand = damager.getInventory().getItemInMainHand().getType();

        if (mainHand != Material.MACE) return;

        double maxDamage = plugin.getConfig().getDouble("mace-damage-limiter.max-damage", 25.0);
        if (event.getDamage() > maxDamage) {
            event.setDamage(maxDamage);
        }
    }
}
