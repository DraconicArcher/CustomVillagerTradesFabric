package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import uk.co.dotcode.customvillagertrades.ModLogger;
import uk.co.dotcode.customvillagertrades.TradeUtil;
import uk.co.dotcode.customvillagertrades.fabric.TradeUtilImpl;

import java.util.List;
import java.util.Objects;

public class EnchantmentResolver {

    public record EnchResult(Enchantment enchantment, int level) {}



    public static EnchResult resolve(EnchantmentEntry entry, String itemKey, List<String> blacklist) {

        ItemStack stack = TradeUtil.getItemFromKey(itemKey).getDefaultInstance();

        List<String> keys = entry.enchantmentKey();

        if (keys == null || keys.isEmpty()) return null;

        List<Enchantment> pool;

        boolean isRandom = keys.size() == 1 && keys.get(0).equalsIgnoreCase("random");


        if (isRandom) {

            pool = TradeUtilImpl.getRegisteredEnchantments().stream()
                    .filter(e -> e != null && (stack.getItem() == Items.ENCHANTED_BOOK || e.canEnchant(stack)))
                    .filter(e -> !isBlacklisted(e, blacklist))
                    .toList();

        }

        else {

            pool = keys.stream()
                    .map(TradeUtilImpl::getEnchantmentFromKey)
                    .filter(Objects::nonNull)
                    .filter(e -> stack.getItem() == Items.ENCHANTED_BOOK || e.canEnchant(stack))
                    .filter(e -> !isBlacklisted(e, blacklist))
                    .toList();
        }

        if (pool.isEmpty()) {
            ModLogger.warn("No valid enchantments for item: " + itemKey);
            return null;
        }

        Enchantment ench = pool.get(TradeUtil.random.nextInt(pool.size()));

        int enchMax = ench.getMaxLevel();

        int min = entry.minEnchantmentLevel() != null
                ? entry.minEnchantmentLevel()
                : 1;

        int max = entry.maxEnchantmentLevel() != null
                ? entry.maxEnchantmentLevel()
                : enchMax;

        min = Math.max(1, Math.min(min, enchMax));
        max = Math.max(1, Math.min(max, enchMax));

        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }

        int level = TradeUtil.random.nextInt(min, max + 1);

        return new EnchResult(ench, level);
    }

    private static boolean isBlacklisted(Enchantment e, List<String> blacklist) {
        if (blacklist == null || blacklist.isEmpty()) return false;

        String key = TradeUtilImpl.getRegistryNameEnchantment(e).toString();

        return blacklist.stream().anyMatch(b -> b.equalsIgnoreCase(key));
    }
}