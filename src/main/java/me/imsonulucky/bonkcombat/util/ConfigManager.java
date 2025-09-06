package me.imsonulucky.bonkcombat.util;

import me.imsonulucky.bonkcombat.BonkCombat;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private static ConfigManager instance;
    private final BonkCombat plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    private File configFile;
    private File messagesFile;

    public ConfigManager(BonkCombat plugin) {
        this.plugin = plugin;
        instance = this;
        load();
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static ConfigManager get() {
        return instance;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public static String getMessage(String path) {
        if (instance == null) return ChatColor.RED + "ConfigManager not initialized";

        String msg = instance.messages.getString(path, "");

        if (msg == null || msg.isEmpty()) {
            if (path.contains("_")) {
                msg = instance.messages.getString(path.replace("_", "-"), "");
            } else if (path.contains("-")) {
                msg = instance.messages.getString(path.replace("-", "_"), "");
            }
        }

        if (msg == null) msg = "";

        return colorize(msg.replace("{prefix}", instance.config.getString("prefix", "")));
    }

    public static String getMessage(String path, Map<String, String> placeholders) {
        if (instance == null) return ChatColor.RED + "ConfigManager not initialized";

        String raw = instance.messages.getString(path, "");

        if (raw == null || raw.isEmpty()) {
            if (path.contains("_")) {
                raw = instance.messages.getString(path.replace("_", "-"), "");
            } else if (path.contains("-")) {
                raw = instance.messages.getString(path.replace("-", "_"), "");
            }
        }

        if (raw == null) raw = "";

        raw = raw.replace("{prefix}", instance.config.getString("prefix", ""));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return colorize(raw);
    }

    public static String colorize(String msg) {
        if (msg == null) return "";

        Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(msg);

        while (matcher.find()) {
            String color = msg.substring(matcher.start(), matcher.end());
            msg = msg.replace(color, net.md_5.bungee.api.ChatColor.of(color).toString());
            matcher = hexPattern.matcher(msg);
        }

        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config.yml!");
            e.printStackTrace();
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save messages.yml!");
            e.printStackTrace();
        }
    }
}
