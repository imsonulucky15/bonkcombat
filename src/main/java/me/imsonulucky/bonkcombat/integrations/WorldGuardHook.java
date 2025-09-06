package me.imsonulucky.bonkcombat.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldGuardHook {

    private static final Map<String, CachedWorld> WORLD_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheLoaded = false;

    private static WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        return (plugin instanceof WorldGuardPlugin) ? (WorldGuardPlugin) plugin : null;
    }

    public static void loadRegionCache() {
        WORLD_CACHE.clear();

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = getRegionManager(world);
            if (manager == null) continue;

            List<CachedRegion> regions = new ArrayList<>();
            for (ProtectedRegion region : manager.getRegions().values()) {
                boolean pvpDenied = region.getFlags().containsKey(Flags.PVP) &&
                        !Boolean.TRUE.equals(region.getFlags().get(Flags.PVP));
                boolean invincible = Boolean.TRUE.equals(region.getFlags().get(Flags.INVINCIBILITY));

                regions.add(new CachedRegion(
                        region.getId(),
                        region.getMinimumPoint(),
                        region.getMaximumPoint(),
                        pvpDenied,
                        invincible
                ));
            }
            WORLD_CACHE.put(world.getName(), new CachedWorld(world.getName(), regions));
        }

        cacheLoaded = true;
        Bukkit.getLogger().info("[WorldGuardHook] Region cache loaded: " + WORLD_CACHE.size() + " worlds");
    }

    public static boolean isInRegion(Player player, List<String> regionNames) {
        if (!cacheLoaded) return false;
        return isInRegionAt(player.getLocation(), regionNames);
    }

    public static boolean isInRegionAt(Location loc, List<String> regionNames) {
        if (!cacheLoaded) return false;

        CachedWorld world = WORLD_CACHE.get(loc.getWorld().getName());
        if (world == null) return false;

        for (CachedRegion region : world.getRegions()) {
            if (region.contains(loc) && regionNames.contains(region.getId())) return true;
        }
        return false;
    }

    public static Set<String> getRegionsAt(Location loc) {
        if (!cacheLoaded) return Collections.emptySet();

        CachedWorld world = WORLD_CACHE.get(loc.getWorld().getName());
        if (world == null) return Collections.emptySet();

        Set<String> ids = new HashSet<>();
        for (CachedRegion region : world.getRegions()) {
            if (region.contains(loc)) ids.add(region.getId());
        }
        return ids;
    }

    public static boolean canBeDamaged(Player player) {
        if (!cacheLoaded) return true;
        return canBeDamagedAt(player.getLocation());
    }

    public static boolean canBeDamagedAt(Player player, Location loc) {
        return canBeDamagedAt(loc);
    }

    public static boolean canBeDamagedAt(Location loc) {
        if (!cacheLoaded) return true;

        CachedWorld world = WORLD_CACHE.get(loc.getWorld().getName());
        if (world == null) return true;

        for (CachedRegion region : world.getRegions()) {
            if (region.contains(loc) && (region.isPvpDenied() || region.isInvincible())) {
                return false;
            }
        }
        return true;
    }

    private static RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));
    }

    private static class CachedWorld {
        private final String name;
        private final List<CachedRegion> regions;

        public CachedWorld(String name, List<CachedRegion> regions) {
            this.name = name;
            this.regions = regions;
        }

        public List<CachedRegion> getRegions() {
            return regions;
        }
    }

    private static class CachedRegion {
        private final String id;
        private final BlockVector3 min;
        private final BlockVector3 max;
        private final boolean pvpDenied;
        private final boolean invincible;

        public CachedRegion(String id, BlockVector3 min, BlockVector3 max, boolean pvpDenied, boolean invincible) {
            this.id = id.toLowerCase(Locale.ROOT);
            this.min = min;
            this.max = max;
            this.pvpDenied = pvpDenied;
            this.invincible = invincible;
        }

        public String getId() { return id; }
        public boolean isPvpDenied() { return pvpDenied; }
        public boolean isInvincible() { return invincible; }

        public boolean contains(Location loc) {
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            return x >= min.getX() && x <= max.getX()
                    && y >= min.getY() && y <= max.getY()
                    && z >= min.getZ() && z <= max.getZ();
        }
    }
}
