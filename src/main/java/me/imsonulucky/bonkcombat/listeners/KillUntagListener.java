package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.util.CombatManager;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillUntagListener implements Listener {

    private final CombatManager combatManager;

    public KillUntagListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String untagMessage = ConfigManager.getMessage("untagged");

        if (combatManager.isInCombat(victim)) {
            combatManager.untag(victim);
            victim.sendMessage(untagMessage);
        }

        if (killer != null && combatManager.isInCombat(killer)) {
            combatManager.untag(killer);
            killer.sendMessage(untagMessage);
        }
    }
}
