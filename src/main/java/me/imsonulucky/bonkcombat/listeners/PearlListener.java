package me.imsonulucky.bonkcombat.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.util.CombatManager;
import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class PearlListener implements Listener {

    private final BonkCombat plugin;
    private final CombatManager combatManager;
    private final List<String> whitelistRegions;
    private final List<String> whitelistWorlds;

    public PearlListener(BonkCombat plugin, CombatManager manager) {
        this.plugin = plugin;
        this.combatManager = manager;
        this.whitelistRegions = plugin.getConfig().getStringList("pearl-settings.whitelist-regions");
        this.whitelistWorlds = plugin.getConfig().getStringList("pearl-settings.whitelist-worlds");
    }

    private boolean isInWhitelistedRegion(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        return set.getRegions().stream()
                .anyMatch(region -> whitelistRegions.contains(region.getId()));
    }

    private boolean isInWhitelistedWorld(Player player) {
        return whitelistWorlds.contains(player.getWorld().getName());
    }

    @EventHandler
    public void onPearlUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        if (item == null || item.getType() != Material.ENDER_PEARL) return;
        if (!combatManager.isInCombat(player)) return;
        if (!plugin.getConfig().getBoolean("pearl-settings.enabled", true)) return;

        if (isInWhitelistedRegion(player) || isInWhitelistedWorld(player)) {
            return;
        }

        if (plugin.getConfig().getBoolean("pearl-settings.block-throwing", false)) {
            e.setCancelled(true);
            player.sendMessage(ConfigManager.getMessage("pearl_blocked"));
            return;
        }

        int pearlCooldown = plugin.getConfig().getInt("pearl-settings.cooldown", 5);
        long millisLeft = combatManager.getPearlCooldownMillisLeft(player, pearlCooldown);

        if (millisLeft > 0) {
            e.setCancelled(true);
            String msg = ConfigManager.getMessage("pearl_cooldown",
                    Map.of("<sec>", String.format("%.1f", millisLeft / 1000.0)));
            player.sendMessage(msg);
            return;
        }

        combatManager.setLastPearlUse(player);
    }
}
