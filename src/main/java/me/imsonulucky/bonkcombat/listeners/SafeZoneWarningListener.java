package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.integrations.WorldGuardHook;
import me.imsonulucky.bonkcombat.util.CombatEnd;
import me.imsonulucky.bonkcombat.utils.CombatManager;
import me.imsonulucky.bonkcombat.utils.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SafeZoneWarningListener implements Listener {

    private final BonkCombat plugin;
    private final CombatManager combatManager;

    private final Map<UUID, PlayerSafeZoneData> playerData = new HashMap<>();

    private boolean debug;
    private boolean enabled;
    private int teleportCooldown;
    private int teleportOffset;
    private int proximityRadius;
    private int markerHeight;
    private int refreshInterval;
    private Material markerMaterial;
    private BlockData markerBlockData;

    private static final int FADE_DELAY = 0;
    private static final int[][] CARDINAL_OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static final long CACHE_EXPIRY_MS = 1000L;
    private static final int CACHE_SWEEP_EVERY_CALLS = 64;

    private final Map<UUID, Long2DamageCache> worldCaches = new ConcurrentHashMap<>();
    private int cacheCallsSinceSweep = 0;

    private final Location tempLoc = new Location(null, 0, 0, 0);

    public SafeZoneWarningListener(BonkCombat plugin, CombatManager manager) {
        this.plugin = plugin;
        this.combatManager = manager;
        reloadConfigValues();
        startProximityCheck();
    }

    public void reloadConfigValues() {
        var config = plugin.getConfig();
        this.debug = config.getBoolean("safezone-wall-warning.debug", false);
        this.enabled = config.getBoolean("safezone-wall-warning.enabled", true);
        this.teleportCooldown = config.getInt("safezone-wall-warning.teleport-cooldown", 1500);
        this.teleportOffset = config.getInt("safezone-wall-warning.teleport-offset", 5);
        this.proximityRadius = config.getInt("safezone-wall-warning.proximity-radius", 10);
        this.markerHeight = config.getInt("safezone-wall-warning.marker-height", 10);
        this.refreshInterval = config.getInt("safezone-wall-warning.refresh-interval", 5);

        String matName = config.getString("safezone-wall-warning.marker-block", "RED_STAINED_GLASS");
        this.markerMaterial = Material.matchMaterial(matName);
        if (this.markerMaterial == null) this.markerMaterial = Material.RED_STAINED_GLASS;
        this.markerBlockData = markerMaterial.createBlockData();
    }

    private void startProximityCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;

                for (UUID uuid : combatManager.getPlayersInCombat()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    PlayerSafeZoneData data = playerData.computeIfAbsent(uuid, id -> new PlayerSafeZoneData());

                    Location loc = player.getLocation();
                    long now = System.currentTimeMillis();

                    if (!canBeDamagedCached(player, loc)) {
                        if (data.lastLegalLoc != null && now - data.lastTeleport >= teleportCooldown) {
                            Location safeLoc = getOffsetLocation(player, data.lastLegalLoc);
                            data.lastTeleport = now;
                            safeTeleport(player, safeLoc);
                            debug("Proximity check teleported " + player.getName() + " to " + safeLoc);
                        }
                    } else {
                        data.lastLegalLoc = loc.clone();
                    }

                    Set<BlockKey> newEdges = findRegionEdgeKeys(player, proximityRadius);
                    updateMarkerBlocks(player, data, newEdges);
                }
            }
        }.runTaskTimer(plugin, 0, refreshInterval);
    }

    private void updateMarkerBlocks(Player player, PlayerSafeZoneData data, Set<BlockKey> newEdges) {
        final World world = player.getWorld();
        final int baseY = player.getLocation().getBlockY();
        final int maxY = Math.max(0, Math.min(markerHeight, world.getMaxHeight() - baseY));

        final Location sendLoc = tempLoc;
        sendLoc.setWorld(world);

        for (BlockKey key : newEdges) {
            for (int y = 0; y < maxY; y++) {
                sendLoc.setX(key.x);
                sendLoc.setY(baseY + y);
                sendLoc.setZ(key.z);
                player.sendBlockChange(sendLoc, markerBlockData);
            }
        }

        for (BlockKey key : data.sentBlockKeys) {
            if (!newEdges.contains(key)) {
                for (int y = 0; y < maxY; y++) {
                    sendLoc.setX(key.x);
                    sendLoc.setY(baseY + y);
                    sendLoc.setZ(key.z);
                    Block realBlock = sendLoc.getBlock();
                    player.sendBlockChange(sendLoc, realBlock.getBlockData());
                }
            }
        }

        data.sentBlockKeys.clear();
        data.sentBlockKeys.addAll(newEdges);
    }

    private void fadeOutFakeBlocks(Player player, PlayerSafeZoneData data) {
        if (data.sentBlockKeys.isEmpty()) return;

        Runnable fade = () -> {
            final World world = player.getWorld();
            final Location loc = tempLoc;
            loc.setWorld(world);

            int baseY = player.getLocation().getBlockY();

            for (BlockKey key : data.sentBlockKeys) {
                for (int dy = 0; dy < markerHeight; dy++) {
                    loc.setX(key.x);
                    loc.setY(baseY + dy);
                    loc.setZ(key.z);
                    Block realBlock = loc.getBlock();
                    player.sendBlockChange(loc, realBlock.getBlockData());
                }
            }
        };

        if (FADE_DELAY > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, fade, FADE_DELAY);
        } else {
            fade.run();
        }

        data.sentBlockKeys.clear();
    }

    private void applyPunishment(Player player) {
        int duration = plugin.getConfig().getInt("safezone-wall-warning.punishment.effect-duration", 60);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));

        String title = ConfigManager.colorize(plugin.getConfig().getString(
                "safezone-wall-warning.punishment.title.main",
                "&cYou can't enter this region during combat"
        ));
        String subtitle = ConfigManager.colorize(plugin.getConfig().getString(
                "safezone-wall-warning.punishment.title.subtitle",
                ""
        ));
        player.sendTitle(title, subtitle, 10, 60, 10);

        player.sendMessage(ConfigManager.getMessage("safezone_block"));
        debug("Punishment applied to " + player.getName());
    }

    private Location getOffsetLocation(Player player, Location lastLegal) {
        Vector diff = lastLegal.toVector().subtract(player.getLocation().toVector());

        if (diff.lengthSquared() == 0) {
            return lastLegal.clone().add(0, 1, 0);
        }

        Vector awayFromSafezone = diff.normalize();
        Location offsetLoc = lastLegal.clone().add(awayFromSafezone.multiply(teleportOffset));
        offsetLoc.setY(lastLegal.getY());

        return offsetLoc;
    }

    private void safeTeleport(Player player, Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        if (Double.isFinite(loc.getX()) &&
                Double.isFinite(loc.getY()) &&
                Double.isFinite(loc.getZ())) {
            player.teleport(loc);
        } else {
            debug("Skipped teleport for " + player.getName() + " (invalid location: " + loc + ")");
        }
    }

    private Set<BlockKey> findRegionEdgeKeys(Player player, int radius) {
        Set<BlockKey> edges = new HashSet<>();
        Location base = player.getLocation();
        World world = base.getWorld();

        final int baseX = base.getBlockX();
        final int baseY = base.getBlockY();
        final int baseZ = base.getBlockZ();

        tempLoc.setWorld(world);

        for (int dx = -radius; dx <= radius; dx++) {
            int x = baseX + dx;
            for (int dz = -radius; dz <= radius; dz++) {
                int z = baseZ + dz;

                if (!canBeDamagedCached(player, world, x, baseY, z)) {
                    for (int[] dir : CARDINAL_OFFSETS) {
                        int nx = x + dir[0];
                        int nz = z + dir[1];
                        if (canBeDamagedCached(player, world, nx, baseY, nz)) {
                            edges.add(new BlockKey(x, z));
                            break;
                        }
                    }
                }
            }
        }
        return edges;
    }

    private boolean canBeDamagedCached(Player player, Location loc) {
        World w = loc.getWorld();
        return canBeDamagedCached(player, w, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean canBeDamagedCached(Player player, World world, int x, int y, int z) {
        UUID wid = world.getUID();
        Long2DamageCache cache = worldCaches.computeIfAbsent(wid, k -> new Long2DamageCache());

        long key = packBlock(x, y, z);
        long now = System.currentTimeMillis();

        Long2DamageCache.Entry e = cache.map.get(key);
        if (e != null && (now - e.time) < CACHE_EXPIRY_MS) {
            return e.value;
        }

        tempLoc.setWorld(world);
        tempLoc.setX(x);
        tempLoc.setY(y);
        tempLoc.setZ(z);
        boolean result = WorldGuardHook.canBeDamagedAt(player, tempLoc);

        cache.map.put(key, new Long2DamageCache.Entry(result, now));

        if (++cacheCallsSinceSweep >= CACHE_SWEEP_EVERY_CALLS) {
            cacheCallsSinceSweep = 0;
            sweepExpired(cache, now);
        }

        return result;
    }

    private void sweepExpired(Long2DamageCache cache, long now) {
        int checked = 0;
        Iterator<Map.Entry<Long, Long2DamageCache.Entry>> it = cache.map.entrySet().iterator();
        while (it.hasNext() && checked < 64) {
            Map.Entry<Long, Long2DamageCache.Entry> en = it.next();
            if ((now - en.getValue().time) >= CACHE_EXPIRY_MS) {
                it.remove();
            }
            checked++;
        }
    }

    private static long packBlock(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFFL);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!enabled) return;
        if (!combatManager.isInCombat(event.getPlayer())) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        if (!canBeDamagedCached(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ConfigManager.getMessage("safezone_block"));
            debug("Cancelled ender pearl teleport for " + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onCombatEnd(CombatEnd event) {
        Player player = event.getPlayer();
        PlayerSafeZoneData data = playerData.remove(player.getUniqueId());
        if (data != null) {
            fadeOutFakeBlocks(player, data);
        }
    }

    private void debug(String msg) {
        if (debug) plugin.getLogger().info("[BonkCombat DEBUG] " + msg);
    }

    private static class PlayerSafeZoneData {
        final Set<BlockKey> sentBlockKeys = new HashSet<>();
        Location lastLegalLoc;
        long lastTeleport = 0L;

        void reset() {
            sentBlockKeys.clear();
            lastLegalLoc = null;
            lastTeleport = 0L;
        }
    }

    private record BlockKey(int x, int z) {}

    private static class Long2DamageCache {
        static class Entry {
            final boolean value;
            final long time;
            Entry(boolean v, long t) { value = v; time = t; }
        }
        final Map<Long, Entry> map = new HashMap<>(1024);
    }
}
