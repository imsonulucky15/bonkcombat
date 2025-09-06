package me.imsonulucky.bonkcombat.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CombatManager {

    private final Map<UUID, Long> combatTimers = new HashMap<>();
    private final Map<UUID, Long> lastPearlUse = new HashMap<>();
    private final long combatDurationMillis;

    public CombatManager(long combatDurationSeconds) {
        this.combatDurationMillis = combatDurationSeconds * 1000L;

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                Bukkit.getPluginManager().getPlugin("BonkCombat"),
                this::cleanupExpiredCombat,
                20L, 20L
        );
    }

    public void tag(Player player) {
        combatTimers.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void untag(Player player) {
        endCombat(player);
    }

    public boolean isInCombat(Player player) {
        Long taggedAt = combatTimers.get(player.getUniqueId());
        return taggedAt != null && (System.currentTimeMillis() - taggedAt <= combatDurationMillis);
    }

    public long getCombatMillisLeft(Player player) {
        Long taggedAt = combatTimers.get(player.getUniqueId());
        if (taggedAt == null) return 0;

        long timeLeft = combatDurationMillis - (System.currentTimeMillis() - taggedAt);
        return Math.max(0, timeLeft);
    }

    public Set<UUID> getPlayersInCombat() {
        return Collections.unmodifiableSet(combatTimers.keySet());
    }

    private void cleanupExpiredCombat() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Long>> iterator = combatTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID uuid = entry.getKey();
            long taggedAt = entry.getValue();

            if (now - taggedAt > combatDurationMillis) {
                iterator.remove();
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    endCombat(player);
                }
            }
        }
    }

    private void endCombat(Player player) {
        combatTimers.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("BonkCombat"),
                () -> Bukkit.getPluginManager().callEvent(new CombatEnd(player))
        );
    }

    public void setLastPearlUse(Player player) {
        lastPearlUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getPearlCooldownMillisLeft(Player player, int cooldownSeconds) {
        long lastUsed = lastPearlUse.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMillis = cooldownSeconds * 1000L;
        return Math.max(0, cooldownMillis - (System.currentTimeMillis() - lastUsed));
    }
}
