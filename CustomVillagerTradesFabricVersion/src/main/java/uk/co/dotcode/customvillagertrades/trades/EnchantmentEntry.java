package uk.co.dotcode.customvillagertrades.trades;

import java.util.List;

public class EnchantmentEntry {

    public List<String> enchantmentKeys;
    public Integer minEnchantmentLevel;
    public Integer maxEnchantmentLevel;




    public List<String> getKeys() {
        return enchantmentKeys;
    }



    public Integer enchantmentLevel; // legacy



    public List<String> enchantmentKey() {
        return enchantmentKeys;
    }

    public Integer minEnchantmentLevel() {
        return minEnchantmentLevel;
    }

    public Integer maxEnchantmentLevel() {
        return maxEnchantmentLevel;
    }

    public EnchantmentEntry() {}

    public EnchantmentEntry(List<String> keys, int min, int max) {
        this.enchantmentKeys = keys;
        this.minEnchantmentLevel = min;
        this.maxEnchantmentLevel = max;
    }



    public int getMinLevel() {
        if (minEnchantmentLevel != null) return minEnchantmentLevel;
        if (enchantmentLevel != null) return enchantmentLevel;
        return 1;
    }

    public int getMaxLevel(int enchMax) {
        if (maxEnchantmentLevel != null) return maxEnchantmentLevel;
        if (enchantmentLevel != null) return enchantmentLevel;
        return enchMax;
    }
}