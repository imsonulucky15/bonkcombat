package me.imsonulucky.bonkcombat.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Map<String, String> OLD = Map.ofEntries(
            Map.entry("black", "0"),
            Map.entry("dark_blue", "1"),
            Map.entry("dark_green", "2"),
            Map.entry("dark_aqua", "3"),
            Map.entry("dark_red", "4"),
            Map.entry("dark_purple", "5"),
            Map.entry("gold", "6"),
            Map.entry("gray", "7"),
            Map.entry("dark_gray", "8"),
            Map.entry("blue", "9"),
            Map.entry("green", "a"),
            Map.entry("aqua", "b"),
            Map.entry("red", "c"),
            Map.entry("light_purple", "d"),
            Map.entry("yellow", "e"),
            Map.entry("white", "f"),
            Map.entry("bold", "l"),
            Map.entry("italic", "o"),
            Map.entry("underlined", "n"),
            Map.entry("strikethrough", "m"),
            Map.entry("obfuscated", "k"),
            Map.entry("reset", "r")
    );

    private static final Pattern MINI_TAG = Pattern.compile("<(/?)(#[a-fA-F0-9]{6}|[a-zA-Z_]+)>");
    private static final Pattern HEX_TAG = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern MINI_FEATURES = Pattern.compile(
            "<(click|hover|insertion|gradient|rainbow|/|#)", Pattern.CASE_INSENSITIVE
    );

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Cache<String, Component> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(512)
            .build();

    private static final Cache<String, String> CACHE_LEGACY = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(256)
            .build();

    public static Component parse(final String in) {
        if (in == null || in.isBlank()) return Component.empty();

        try {
            return CACHE.get(in, () -> {
                if (MINI_FEATURES.matcher(in).find()) {
                    return MINI.deserialize(in);
                }
                return LEGACY.deserialize(in);
            });
        } catch (ExecutionException e) {
            return Component.empty();
        }
    }

    public static List<Component> parseList(final List<String> input) {
        return (input == null || input.isEmpty()) ? List.of() : input.stream()
                .map(ColorUtil::parse)
                .toList();
    }

    public static String toLegacyString(final Component component) {
        return LEGACY.serialize(component);
    }

    public static String toMiniMessageString(final Component component) {
        return MINI.serialize(component);
    }

    public static String parseLegacy(final String in) {
        if (in == null || in.isBlank()) return "";

        try {
            return CACHE_LEGACY.get(in, () -> {
                String step = org.bukkit.ChatColor.translateAlternateColorCodes('&', in);

                step = HEX_TAG.matcher(step).replaceAll(m ->
                        ChatColor.of("#" + m.group(1)).toString());

                final Matcher matcher = MINI_TAG.matcher(step);
                final StringBuilder out = new StringBuilder();
                final Deque<String> stack = new ArrayDeque<>();
                int last = 0;

                while (matcher.find()) {
                    out.append(step, last, matcher.start());

                    final boolean closing = matcher.group(1).equals("/");
                    final String tag = matcher.group(2).toLowerCase(Locale.ROOT);

                    if (closing && !stack.isEmpty() && (stack.contains(tag) || tag.startsWith("#"))) {
                        out.append(ChatColor.RESET);
                        stack.remove(tag);
                        for (final String t : stack) {
                            out.append(resolve(t));
                        }
                    } else if (!closing) {
                        stack.push(tag);
                        out.append(resolve(tag));
                    }

                    last = matcher.end();
                }

                return out.append(step.substring(last)).toString();
            });
        } catch (ExecutionException e) {
            return "";
        }
    }

    public static List<String> parseListLegacy(final List<String> input) {
        return (input == null || input.isEmpty()) ? List.of() : input
                .stream()
                .map(ColorUtil::parseLegacy)
                .toList();
    }

    private static String resolve(final String tag) {
        if (tag.startsWith("#") && tag.length() == 7) {
            try {
                return ChatColor.of(tag).toString();
            } catch (final IllegalArgumentException ignored) {
                return "";
            }
        }
        final String code = OLD.get(tag);
        return code != null ? "§" + code : "";
    }
}
