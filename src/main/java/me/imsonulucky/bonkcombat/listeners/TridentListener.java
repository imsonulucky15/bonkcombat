package me.imsonulucky.bonkcombat.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.imsonulucky.bonkcombat.utils.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TridentListener implements Listener {

    private final boolean globalBlock;
    private final boolean useRegions;
    private final Set<String> blockedRegionsLower;

    public TridentListener() {
        var cfg = ConfigManager.get().getConfig();
        this.globalBlock = cfg.getBoolean("blocked-tridents.enabled", false);
        this.useRegions = cfg.getBoolean("blocked-tridents.use-regions", false);
        this.blockedRegionsLower = new HashSet<>();
        for (String id : cfg.getStringList("blocked-tridents.regions")) {
            if (id != null) blockedRegionsLower.add(id.toLowerCase(Locale.ROOT));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTridentInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TRIDENT) return;
        Player player = event.getPlayer();

        if (shouldBlockHere(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        var shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;

        if (shouldBlockHere(player)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldBlockHere(Player player) {
        if (globalBlock) return true;
        if (!useRegions || blockedRegionsLower.isEmpty()) return false;

        RegionManager rm = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (rm == null) return false;

        ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        for (ProtectedRegion r : set) {
            if (blockedRegionsLower.contains(r.getId().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
