package uk.co.dotcode.customvillagertrades.trades;

import java.util.List;

public class EnchantmentEntry {

    /*
     * Explicit enchantment IDs, or:
     *
     *     ["random"]
     *
     * when the enchantment should be randomly selected.
     */
    public List<String> enchantmentKeys;

    /*
     * Optional enchantment tag.
     *
     * Example:
     *
     *     minecraft:on_traded_equipment
     *
     * or:
     *
     *     minecraft:tradeable
     *
     * When present with "random", the random enchantment is
     * selected from this tag instead of from every registered
     * enchantment.
     */
    public String enchantmentTag;

    public Integer minEnchantmentLevel;

    public Integer maxEnchantmentLevel;

    /*
     * Legacy single-level property.
     */
    public Integer enchantmentLevel;

    public EnchantmentEntry() {
    }

    public EnchantmentEntry(
            List<String> keys,
            int min,
            int max
    ) {
        this.enchantmentKeys = keys;
        this.minEnchantmentLevel = min;
        this.maxEnchantmentLevel = max;
    }

    public EnchantmentEntry(
            List<String> keys,
            int min,
            int max,
            String tag
    ) {
        this.enchantmentKeys = keys;
        this.minEnchantmentLevel = min;
        this.maxEnchantmentLevel = max;
        this.enchantmentTag = tag;
    }

    public List<String> getKeys() {
        return enchantmentKeys;
    }

    public List<String> enchantmentKey() {
        return enchantmentKeys;
    }

    public Integer minEnchantmentLevel() {
        return minEnchantmentLevel;
    }

    public Integer maxEnchantmentLevel() {
        return maxEnchantmentLevel;
    }

    public String getEnchantmentTag() {
        return enchantmentTag;
    }

    public int getMinLevel() {

        if (minEnchantmentLevel != null) {
            return minEnchantmentLevel;
        }

        if (enchantmentLevel != null) {
            return enchantmentLevel;
        }

        return 1;
    }

    public int getMaxLevel(
            int enchantmentMaxLevel
    ) {

        if (maxEnchantmentLevel != null) {
            return maxEnchantmentLevel;
        }

        if (enchantmentLevel != null) {
            return enchantmentLevel;
        }

        return enchantmentMaxLevel;
    }
}