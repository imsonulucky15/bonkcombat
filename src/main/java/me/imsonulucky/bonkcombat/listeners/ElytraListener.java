package me.imsonulucky.bonkcombat.listeners;

import me.imsonulucky.bonkcombat.BonkCombat;
import me.imsonulucky.bonkcombat.integrations.WorldGuardHook;
import me.imsonulucky.bonkcombat.utils.CombatManager;
import me.imsonulucky.bonkcombat.utils.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ElytraListener implements Listener {

    private final BonkCombat plugin;
    private final CombatManager combatManager;

    public ElytraListener(BonkCombat plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onElytra(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ConfigurationSection elytraSection = plugin.getConfig().getConfigurationSection("disable_elytra");
        if (elytraSection == null) return;

        String mode = elytraSection.getString("enabled", "enabled").toLowerCase();
        boolean restrictToRegions = elytraSection.getBoolean("allowed_regions", false);
        List<String> allowedRegions = elytraSection.getStringList("regions");

        if (!mode.equals("disabled")) return;
        if (!combatManager.isInCombat(player) || !event.isGliding()) return;

        if (restrictToRegions && WorldGuardHook.isInRegion(player, allowedRegions)) return;

        event.setCancelled(true);

        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.ELYTRA) {
            player.getInventory().setChestplate(null);
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(chest);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), chest);
            }
        }

        player.sendMessage(ConfigManager.getMessage("elytra_disabled"));
    }
}
