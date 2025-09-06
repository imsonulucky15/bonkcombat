package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.integrations.WorldGuardHook;
import me.imsonulucky.bonkcombat.util.CombatManager;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageTagListener implements Listener {

    private final CombatManager combatManager;
    private final BonkCombat plugin;
    private final boolean debug;

    private final Map<UUID, UUID> lastCombatTargets = new HashMap<>();

    public DamageTagListener(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.plugin = BonkCombat.getInstance();
        this.debug = plugin.getConfig().getBoolean("safezone-wall-warning.debug", false);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!WorldGuardHook.canBeDamagedAt(victim, victim.getLocation())) return;

        Player damager = resolveDamager(event.getDamager());
        if (damager == null || damager.equals(victim)) return;

        boolean damagerWasInCombat = combatManager.isInCombat(damager);

        combatManager.tag(damager);
        combatManager.tag(victim);

        UUID victimId = victim.getUniqueId();
        UUID damagerId = damager.getUniqueId();

        if (!damagerWasInCombat || !victimId.equals(lastCombatTargets.get(damagerId))) {
            damager.sendMessage(ConfigManager.getMessage("tagged", Map.of("%username%", victim.getName())));
            victim.sendMessage(ConfigManager.getMessage("tagged", Map.of("%username%", damager.getName())));
            lastCombatTargets.put(damagerId, victimId);
        }

        if (debug) {
            Bukkit.getLogger().info("[BonkCombat DEBUG] Tagged " + damager.getName() + " and " + victim.getName());
        }
    }

    private Player resolveDamager(Object source) {
        if (source instanceof Player p) {
            return p;
        }
        if (source instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Player p) {
                return p;
            }
        }
        return null;
    }
}
