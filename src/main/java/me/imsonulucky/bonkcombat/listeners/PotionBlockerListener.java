package me.imsonulucky.bonkcombat.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class PotionBlockerListener implements Listener {

    private final ConfigManager configManager;
    private final Set<String> blockedEffects;
    private final Set<String> blockedRegions;
    private final boolean blockEverywhere;
    private final boolean useRegions;

    public PotionBlockerListener(ConfigManager configManager) {
        this.configManager = configManager;

        this.blockEverywhere = configManager.getConfig().getBoolean("blocked-potion-effects.enabled", false);
        this.useRegions = configManager.getConfig().getBoolean("blocked-potion-effects.use-regions", false);
        this.blockedEffects = new HashSet<>(configManager.getConfig().getStringList("blocked-potion-effects.effects"));
        this.blockedRegions = new HashSet<>(configManager.getConfig().getStringList("blocked-potion-effects.regions"));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Player player = event.getPlayer();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();

            if (!blockedEffects.contains(type.getName())) continue;

            if (blockEverywhere) {
                removeEffect(player, type);
            } else if (useRegions) {
                if (isInBlockedRegion(player)) {
                    removeEffect(player, type);
                }
            }
        }
    }

    private void removeEffect(Player player, PotionEffectType type) {
        player.removePotionEffect(type);
        String msg = configManager.getMessage("blocked-potion-effect")
                .replace("{effect}", type.getName());
        player.sendMessage(msg);
    }

    private boolean isInBlockedRegion(Player player) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) return false;

        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        for (ProtectedRegion region : regionSet) {
            if (blockedRegions.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }
}