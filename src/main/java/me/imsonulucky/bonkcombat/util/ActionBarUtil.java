package me.imsonulucky.bonkcombat.util;

import me.imsonulucky.bonkcombat.BonkCombat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class ActionBarUtil {

    private static boolean started = false;
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public static void send(Player p, String msg) {
        if (p == null || !p.isOnline()) return;
        if (msg == null || msg.isEmpty()) return;

        Component component = legacySerializer.deserialize(ColorUtil.parseLegacy(msg));
        p.sendActionBar(component);
    }

    public static void startSmoothCombatBar(BonkCombat plugin) {
        if (started) return;
        started = true;

        long updateTicks = plugin.getConfig().getLong("actionbar-update-ticks", 2L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Set<UUID> combatPlayers = plugin.getCombatManager().getPlayersInCombat();

            for (UUID uuid : combatPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                long msLeft = plugin.getCombatManager().getCombatMillisLeft(player);
                double seconds = msLeft / 1000.0;

                if (seconds <= 0) continue;

                String format = plugin.getConfig().getString("actionbar-message", "&cCombat - <sec>s");
                String msg = format.replace("<sec>", String.format("%.1f", seconds));

                send(player, msg);
            }
        }, 0L, updateTicks);
    }
}
