package me.imsonulucky.bonkcombat.utils;

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
        combatTimers.remove(player.getUniqueId());
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

    private void cleanupExpiredCombat() {
        long now = System.currentTimeMillis();
        combatTimers.entrySet().removeIf(entry -> now - entry.getValue() > combatDurationMillis);
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
