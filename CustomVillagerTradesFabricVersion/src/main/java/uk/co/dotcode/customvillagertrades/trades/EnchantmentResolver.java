package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import uk.co.dotcode.customvillagertrades.ModLogger;
import uk.co.dotcode.customvillagertrades.TradeUtil;
import uk.co.dotcode.customvillagertrades.fabric.TradeUtilImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EnchantmentResolver {

    public record EnchResult(
            Holder<Enchantment> enchantment,
            int level
    ) {
    }

    public static EnchResult resolve(
            EnchantmentEntry entry,
            String itemKey,
            List<String> blacklist,
            RegistryAccess access
    ) {

        if (entry == null) {
            return null;
        }

        List<String> keys =
                entry.enchantmentKey();

        if (keys == null
                || keys.isEmpty()) {

            return null;
        }

        Registry<Enchantment> registry =
                access.registryOrThrow(
                        Registries.ENCHANTMENT
                );

        ItemStack stack =
                TradeUtil
                        .getItemFromKey(itemKey)
                        .getDefaultInstance();

        boolean isRandom =
                keys.size() == 1
                        && "random".equalsIgnoreCase(
                        keys.get(0)
                );

        List<Enchantment> pool =
                new ArrayList<>();

        /*
         * ---------------------------------------------------------
         * Random enchantment
         * ---------------------------------------------------------
         */
        if (isRandom) {

            /*
             * If the configuration specifies an enchantment tag,
             * use that tag as the random pool.
             *
             * This is how exported vanilla trades preserve their
             * original enchantment pool.
             */
            if (entry.enchantmentTag != null
                    && !entry.enchantmentTag.isBlank()) {

                ResourceLocation tagId =
                        ResourceLocation.tryParse(
                                entry.enchantmentTag
                        );

                if (tagId == null) {

                    ModLogger.warn(
                            "Invalid enchantment tag: "
                                    + entry.enchantmentTag
                    );

                } else {

                    TagKey<Enchantment> tag =
                            TagKey.create(
                                    Registries.ENCHANTMENT,
                                    tagId
                            );

                    var holders =
                            registry.getTag(tag);

                    if (holders.isPresent()) {

                        for (Holder<Enchantment> holder :
                                holders.get()) {

                            Enchantment enchantment =
                                    holder.value();

                            /*
                             * Books can store any enchantment.
                             *
                             * Equipment must be compatible with
                             * the enchantment.
                             */
                            if (stack.getItem()
                                    != Items.ENCHANTED_BOOK
                                    && !enchantment.canEnchant(
                                    stack
                            )) {
                                continue;
                            }

                            if (isBlacklisted(
                                    enchantment,
                                    blacklist
                            )) {
                                continue;
                            }

                            pool.add(
                                    enchantment
                            );
                        }
                    }
                }

            } else {

                /*
                 * No tag specified.
                 *
                 * Preserve the behavior of your existing
                 * configuration by selecting from all registered
                 * enchantments that are valid for the item.
                 */
                pool =
                        TradeUtilImpl
                                .getRegisteredEnchantments()
                                .stream()
                                .filter(
                                        Objects::nonNull
                                )
                                .filter(
                                        e ->
                                                stack.getItem()
                                                        == Items.ENCHANTED_BOOK
                                                        || e.canEnchant(
                                                        stack
                                                )
                                )
                                .filter(
                                        e ->
                                                !isBlacklisted(
                                                        e,
                                                        blacklist
                                                )
                                )
                                .toList();
            }

        } else {

            /*
             * -----------------------------------------------------
             * Explicit enchantment(s)
             * -----------------------------------------------------
             *
             * Explicit configuration continues to work exactly
             * as before.
             */
            pool =
                    keys.stream()
                            .filter(
                                    Objects::nonNull
                            )
                            .filter(
                                    key ->
                                            !key.isBlank()
                            )
                            .map(
                                    TradeUtilImpl
                                            ::getEnchantmentFromKey
                            )
                            .filter(
                                    Objects::nonNull
                            )
                            .filter(
                                    e ->
                                            !isBlacklisted(
                                                    e,
                                                    blacklist
                                            )
                            )
                            .filter(
                                    e ->
                                            stack.getItem()
                                                    == Items.ENCHANTED_BOOK
                                                    || e.canEnchant(
                                                    stack
                                            )
                            )
                            .toList();
        }

        if (pool.isEmpty()) {

            ModLogger.warn(
                    "No valid enchantments for item: "
                            + itemKey
            );

            return null;
        }

        /*
         * Pick the enchantment.
         */
        Enchantment enchantment =
                pool.get(
                        TradeUtil.random.nextInt(
                                pool.size()
                        )
                );

        /*
         * Convert to the registry-backed Holder required by
         * Minecraft 1.21.1's enchantment component system.
         */
        Holder<Enchantment> holder =
                registry.wrapAsHolder(
                        enchantment
                );

        /*
         * ---------------------------------------------------------
         * Level
         * ---------------------------------------------------------
         */
        int vanillaMaxLevel =
                enchantment.getMaxLevel();

        int min =
                entry.minEnchantmentLevel != null
                        ? entry.minEnchantmentLevel
                        : 1;

        int max =
                entry.maxEnchantmentLevel != null
                        ? entry.maxEnchantmentLevel
                        : vanillaMaxLevel;

        min =
                Math.max(
                        1,
                        Math.min(
                                min,
                                vanillaMaxLevel
                        )
                );

        max =
                Math.max(
                        1,
                        Math.min(
                                max,
                                vanillaMaxLevel
                        )
                );

        if (min > max) {

            int temp = min;
            min = max;
            max = temp;
        }

        int level =
                TradeUtil.random.nextInt(
                        min,
                        max + 1
                );

        return new EnchResult(
                holder,
                level
        );
    }

    private static boolean isBlacklisted(
            Enchantment enchantment,
            List<String> blacklist
    ) {

        if (blacklist == null
                || blacklist.isEmpty()) {

            return false;
        }

        ResourceLocation id =
                TradeUtilImpl
                        .getRegistryNameEnchantment(
                                enchantment
                        );

        if (id == null) {
            return false;
        }

        return blacklist.stream()
                .filter(
                        Objects::nonNull
                )
                .anyMatch(
                        value ->
                                value.equalsIgnoreCase(
                                        id.toString()
                                )
                );
    }
}
