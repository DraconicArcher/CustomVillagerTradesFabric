package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.network.chat.Component;

import net.minecraft.core.component.DataComponents;

import uk.co.dotcode.customvillagertrades.ModLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TradeItem {

    public String itemKey;

    public int amount = 1;

    public Integer priceModifier;

    public String name;

    public String advancedNBTData;

    private Holder<Item> itemHolder;

    private Integer amountRange;

    private Integer priceModifierAdditional;

    private List<EnchantmentEntry> enchantments =
            new ArrayList<>();

    private List<String> blacklist =
            new ArrayList<>();

    private Object metadata;

    private Integer r;

    private Integer g;

    private Integer b;

    private List<MyTradeEffect> effects =
            new ArrayList<>();

    private static final Random RANDOM =
            new Random();

    private record EffectData(
            Holder<MobEffect> effect,
            int duration,
            int amplifier,
            boolean visible
    ) {
    }

    /**
     * Resolves the configured item against the server registry.
     */
    public void resolve(RegistryAccess access) {

        if (itemHolder != null
                || itemKey == null) {
            return;
        }

        ResourceLocation id =
                ResourceLocation.tryParse(
                        itemKey
                );

        if (id == null) {

            ModLogger.error(
                    "Invalid item identifier: "
                            + itemKey
            );

            return;
        }

        Registry<Item> registry =
                access.registryOrThrow(
                        Registries.ITEM
                );

        itemHolder =
                registry.getHolder(id)
                        .orElse(null);

        if (itemHolder == null) {

            ModLogger.error(
                    "Failed to resolve item: "
                            + itemKey
            );
        }
    }

    /**
     * Creates the configured ItemStack.
     */
    public ItemStack createItemStack(
            Entity entity
    ) {

        RegistryAccess access =
                entity.level().registryAccess();

        if (itemHolder == null) {
            resolve(access);
        }

        if (itemHolder == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                new ItemStack(
                        itemHolder,
                        getAmount()
                );

        /*
         * ---------------------------------------------------------
         * Potion / stew effects.
         * ---------------------------------------------------------
         */
        List<MobEffectInstance> normalized =
                getEffects();

        if (!normalized.isEmpty()) {

            if (stack.is(Items.POTION)
                    || stack.is(Items.SPLASH_POTION)
                    || stack.is(Items.LINGERING_POTION)
                    || stack.is(Items.TIPPED_ARROW)) {

                applyPotion(
                        stack,
                        normalized
                );

            } else if (stack.is(
                    Items.SUSPICIOUS_STEW
            )) {

                applyStew(
                        stack,
                        normalized
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * Enchantments.
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * We process EVERY EnchantmentEntry in the configuration.
         *
         * Previously the code selected ONE random entry:
         *
         *     enchantments.get(RANDOM.nextInt(...))
         *
         * which meant a trade containing:
         *
         *     Knockback
         *     Sharpness
         *
         * could only ever receive one of them.
         *
         * EnchantmentHelper.updateEnchantments() handles the
         * correct component automatically:
         *
         *     normal item  -> ENCHANTMENTS
         *     enchanted book -> STORED_ENCHANTMENTS
         *
         * It also preserves enchantments already present on the
         * stack, allowing multiple configured enchantments.
         */
        if (enchantments != null
                && !enchantments.isEmpty()) {

            for (EnchantmentEntry entry :
                    enchantments) {

                if (entry == null) {
                    continue;
                }

                EnchantmentResolver.EnchResult result =
                        EnchantmentResolver.resolve(
                                entry,
                                itemKey,
                                blacklist,
                                access
                        );

                if (result == null) {
                    continue;
                }

                Holder<Enchantment> enchantment =
                        result.enchantment();

                int level =
                        result.level();

                /*
                 * EnchantmentHelper determines whether this stack
                 * should use ENCHANTMENTS or STORED_ENCHANTMENTS.
                 *
                 * This is what makes the same code work for:
                 *
                 *     diamond sword
                 *     netherite axe
                 *     wooden sword
                 *     enchanted book
                 *
                 * without manually manipulating the data
                 * components.
                 */
                EnchantmentHelper.updateEnchantments(
                        stack,
                        mutable ->
                                mutable.set(
                                        enchantment,
                                        level
                                )
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * Custom name.
         * ---------------------------------------------------------
         */
        if (name != null
                && !name.isBlank()) {

            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(name)
            );
        }

        /*
         * ---------------------------------------------------------
         * Dyed item color.
         * ---------------------------------------------------------
         */
        if (r != null
                && g != null
                && b != null) {

            int color =
                    (r << 16)
                            | (g << 8)
                            | b;

            stack.set(
                    DataComponents.DYED_COLOR,
                    new DyedItemColor(
                            color,
                            false
                    )
            );
        }

        return stack;
    }

    /**
     * Converts configured effects into
     * registry-backed effects.
     */
    private List<MobEffectInstance> getEffects() {

        if (effects == null
                || effects.isEmpty()) {

            return List.of();
        }

        List<MobEffectInstance> result =
                new ArrayList<>();

        for (MyTradeEffect effect :
                effects) {

            if (effect == null) {
                continue;
            }

            MobEffectInstance instance =
                    effect.getInstance();

            if (instance != null) {
                result.add(instance);
            }
        }

        return result;
    }

    /**
     * Applies effects to a potion using the
     * 1.21 component system.
     */
    private void applyPotion(
            ItemStack stack,
            List<MobEffectInstance> effects
    ) {

        PotionContents contents =
                PotionContents.EMPTY;

        for (MobEffectInstance effect :
                effects) {

            contents =
                    contents.withEffectAdded(
                            effect
                    );
        }

        stack.set(
                DataComponents.POTION_CONTENTS,
                contents
        );
    }

    /**
     * Applies effects to suspicious stew.
     */
    private void applyStew(
            ItemStack stack,
            List<MobEffectInstance> effects
    ) {

        List<SuspiciousStewEffects.Entry>
                stewEffects =
                new ArrayList<>();

        for (MobEffectInstance effect :
                effects) {

            stewEffects.add(
                    new SuspiciousStewEffects.Entry(
                            effect.getEffect(),
                            effect.getDuration()
                    )
            );
        }

        stack.set(
                DataComponents.SUSPICIOUS_STEW_EFFECTS,
                new SuspiciousStewEffects(
                        stewEffects
                )
        );
    }

    public void setItemKey(
            String itemKey
    ) {
        this.itemKey = itemKey;
    }

    public void setAmount(
            int amount
    ) {
        this.amount = amount;
    }

    public void setPriceModifier(
            Integer priceModifier
    ) {
        this.priceModifier =
                priceModifier;
    }

    public void setEnchantments(
            List<EnchantmentEntry> enchantments
    ) {
        this.enchantments =
                enchantments;
    }

    public void setBlacklist(
            List<String> blacklist
    ) {
        this.blacklist =
                blacklist;
    }

    public void setEffects(
            List<MyTradeEffect> effects
    ) {
        this.effects =
                effects;
    }

    public int getAmount() {

        if (amountRange != null
                && amountRange > 0) {

            return amount
                    + RANDOM.nextInt(
                    amountRange + 1
            );
        }

        return amount;
    }

    public boolean validate(
            String prof,
            int i
    ) {

        if (itemHolder == null
                && itemKey == null) {

            ModLogger.error(
                    "Missing item: "
                            + prof
                            + " index "
                            + i
            );

            return false;
        }

        return true;
    }
}
