package me.imsonulucky.bonkcombat.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorldGuardHook {

    private static WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(plugin instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) plugin;
    }

    public static boolean isInRegion(Player player, List<String> regionNames) {
        Location loc = player.getLocation();
        RegionManager manager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return false;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        for (ProtectedRegion region : set) {
            if (regionNames.contains(region.getId())) return true;
        }
        return false;
    }

    public static Set<String> getRegionIds(Location loc) {
        Set<String> ids = new HashSet<>();
        RegionManager manager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return ids;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        for (ProtectedRegion region : set) {
            ids.add(region.getId());
        }
        return ids;
    }

    public static boolean isInAnyRegion(Player player) {
        return isLocationInAnyRegion(player.getLocation());
    }

    public static boolean isLocationInAnyRegion(Location loc) {
        RegionManager manager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return false;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        return !set.getRegions().isEmpty();
    }

    public static boolean canBeDamaged(Player player) {
        return canBeDamagedAt(player, player.getLocation());
    }

    public static boolean canBeDamagedAt(Player player, Location loc) {
        RegionManager manager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return true;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        LocalPlayer localPlayer = getWorldGuard().wrapPlayer(player);

        boolean pvpExplicitlyDenied = set.size() > 0 && !set.testState(localPlayer, Flags.PVP);
        boolean invincible = set.testState(localPlayer, Flags.INVINCIBILITY);

        return !pvpExplicitlyDenied && !invincible;
    }
}
