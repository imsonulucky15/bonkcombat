package me.imsonulucky.bonkcombat;

import me.imsonulucky.bonkcombat.commands.BCCommand;
import me.imsonulucky.bonkcombat.commands.PotionStackCommand;
import me.imsonulucky.bonkcombat.integrations.VaultHook;
import me.imsonulucky.bonkcombat.listeners.*;
import me.imsonulucky.bonkcombat.util.ActionBarUtil;
import me.imsonulucky.bonkcombat.utils.CombatManager;
import me.imsonulucky.bonkcombat.utils.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import java.io.File;

public class BonkCombat extends JavaPlugin {

    private static BonkCombat instance;

    private CombatManager combatManager;
    private ConfigManager configManager;
    private FileConfiguration messages;

    private SafeZoneWarningListener safeZoneWarningListener;

    public static BonkCombat getInstance() {
        return instance;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public SafeZoneWarningListener getSafeZoneWarningListener() {
        return safeZoneWarningListener;
    }

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void onLoad() {
        if (!PacketEvents.getAPI().isInitialized()) {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveMessages();

        if (!VaultHook.setupEconomy()) {
            getLogger().warning("Could not hook into Vault economy. Money rewards will not work.");
        } else {
            getLogger().info("Vault economy hooked: " + VaultHook.getEconomy().getName());
        }

        configManager = new ConfigManager(this);
        combatManager = new CombatManager(getConfig().getInt("combat-timer", 15));

        Bukkit.getScheduler().runTask(this, () -> {
            PluginCommand bcCommand = getCommand("bc");
            if (bcCommand != null) {
                bcCommand.setExecutor(new BCCommand(combatManager));
            } else {
                getLogger().warning("Command 'bc' is not registered correctly in plugin.yml.");
            }

            PluginCommand potionstackCommand = getCommand("potionstack");
            if (potionstackCommand != null) {
                potionstackCommand.setExecutor(new PotionStackCommand(this));
            } else {
                getLogger().warning("Command 'potionstack' is not registered correctly in plugin.yml.");
            }
        });

        ActionBarUtil.startSmoothCombatBar(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new WindChargeListener(this), this);
        pm.registerEvents(new MaceDamageLimiterListener(this), this);
        pm.registerEvents(new KillUntagListener(combatManager), this);
        pm.registerEvents(new DamageTagListener(combatManager), this);
        pm.registerEvents(new CombatLogListener(this, combatManager), this);
        pm.registerEvents(new CommandBlockListener(this, combatManager), this);
        pm.registerEvents(new PearlListener(this, combatManager), this);
        pm.registerEvents(new ElytraListener(this, combatManager), this);
        pm.registerEvents(new PotionListener(this), this);
        pm.registerEvents(new RegionRewardListener(this), this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!PacketEvents.getAPI().isInitialized()) {
                PacketEvents.getAPI().init();
            }
            safeZoneWarningListener = new SafeZoneWarningListener(this, combatManager);
            pm.registerEvents(safeZoneWarningListener, this);
            getLogger().info("PacketEvents listeners registered (delayed)");
        }, 20L);

        getLogger().info("BonkCombat enabled!");
    }

    @Override
    public void onDisable() {
        if (PacketEvents.getAPI().isInitialized()) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("BonkCombat disabled.");
    }

    private void saveMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }
}
