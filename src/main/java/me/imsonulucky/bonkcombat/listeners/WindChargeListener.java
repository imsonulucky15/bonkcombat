package me.imsonulucky.bonkcombat.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.util.ColorUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class WindChargeListener implements Listener {

    private final BonkCombat plugin;

    public WindChargeListener(BonkCombat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseWindCharge(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        boolean isMain = player.getInventory().getItemInMainHand().getType() == Material.WIND_CHARGE;
        boolean isOff = player.getInventory().getItemInOffHand().getType() == Material.WIND_CHARGE;

        if (!isMain && !isOff) return;

        if (!plugin.getConfig().getBoolean("windcharge-block.enabled")) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) return;

        BlockVector3 location = BukkitAdapter.asBlockVector(player.getLocation());
        ApplicableRegionSet regions = regionManager.getApplicableRegions(location);

        List<String> blockedRegions = plugin.getConfig().getStringList("windcharge-block.blocked-regions");

        boolean isBlocked = regions.getRegions().stream()
                .anyMatch(r -> blockedRegions.contains(r.getId()));

        if (isBlocked) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().getString("windcharge-blocked")));
        }
    }
}
