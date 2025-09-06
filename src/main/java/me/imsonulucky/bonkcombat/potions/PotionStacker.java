package me.imsonulucky.bonkcombat.potions;

import me.imsonulucky.bonkcombat.util.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.*;

public class PotionStacker {

    public static StackResult stackSplashPotions(ItemStack[] contents, int maxStack) {
        Map<PotionKey, List<Integer>> potionSlots = new HashMap<>();
        Map<PotionKey, Integer> potionCounts = new HashMap<>();
        int totalPotions = 0;
        boolean potionLimitReached = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.SPLASH_POTION || !(item.getItemMeta() instanceof PotionMeta meta)) {
                continue;
            }
            PotionKey key = new PotionKey(meta, item);
            potionSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            potionCounts.put(key, potionCounts.getOrDefault(key, 0) + item.getAmount());
            totalPotions += item.getAmount();
        }

        if (totalPotions > maxStack) {
            potionLimitReached = true;
        }

        ItemStack[] result = Arrays.copyOf(contents, contents.length);
        boolean maxReached = false;
        int totalStacked = 0;

        for (Map.Entry<PotionKey, List<Integer>> entry : potionSlots.entrySet()) {
            PotionKey key = entry.getKey();
            List<Integer> slots = entry.getValue();
            int total = potionCounts.get(key);

            Iterator<Integer> slotIt = slots.iterator();
            while (slotIt.hasNext() && total > 0 && totalStacked < maxStack) {
                int slot = slotIt.next();
                int stack = Math.min(maxStack - totalStacked, total);
                ItemStack stacked = key.toItemStack(stack);
                result[slot] = stacked;
                total -= stack;
                totalStacked += stack;

                if (totalStacked == maxStack) {
                    maxReached = true;
                    break;
                }
            }

            while (slotIt.hasNext()) {
                result[slotIt.next()] = null;
            }

            if (totalStacked >= maxStack) {
                break;
            }
        }

        return new StackResult(result, maxReached, potionLimitReached);
    }

    private static class PotionKey {
        private final PotionMeta meta;
        private final ItemStack base;

        PotionKey(PotionMeta meta, ItemStack base) {
            this.meta = (PotionMeta) meta.clone();
            this.base = new ItemStack(base.getType());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PotionKey other)) return false;
            return Objects.equals(meta, other.meta) && base.isSimilar(other.base);
        }

        @Override
        public int hashCode() {
            return Objects.hash(meta, base.getType());
        }

        public ItemStack toItemStack(int amount) {
            ItemStack item = new ItemStack(base.getType(), amount);
            item.setItemMeta(meta.clone());
            return item;
        }
    }

    public static class StackResult {
        public final ItemStack[] updated;
        public final boolean maxReached;
        public final boolean potionLimitReached;

        public StackResult(ItemStack[] updated, boolean maxReached, boolean potionLimitReached) {
            this.updated = updated;
            this.maxReached = maxReached;
            this.potionLimitReached = potionLimitReached;
        }
    }

    public static void handlePotionStackingResult(Player player, StackResult result, int maxStack) {
        if (result.potionLimitReached) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{max}", String.valueOf(maxStack));

            String message = ConfigManager.getMessage("potionstack-max-reached", placeholders);
            player.sendMessage(message);
        } else {
            String message = ConfigManager.getMessage("potionstack-success");
            player.sendMessage(message);
        }
    }
}
