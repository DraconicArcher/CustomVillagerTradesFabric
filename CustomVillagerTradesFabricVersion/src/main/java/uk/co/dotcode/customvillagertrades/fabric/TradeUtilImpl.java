package uk.co.dotcode.customvillagertrades.fabric;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;


import uk.co.dotcode.customvillagertrades.CVT;

public class TradeUtilImpl {

	private final RegistryAccess registryAccess;

	public TradeUtilImpl(RegistryAccess registryAccess) {
		this.registryAccess = registryAccess;
	}

	public RegistryAccess getRegistryAccess() {
		return registryAccess;
	}

	public List<VillagerProfession> getAllProfessions() {
		return BuiltInRegistries.VILLAGER_PROFESSION
				.stream()
				.collect(Collectors.toList());
	}

	public VillagerProfession getProfessionFromKey(String professionKey) {

		String actualKey =
				professionKey.contains(":")
						? professionKey
						: "minecraft:" + professionKey;

		ResourceLocation id = ResourceLocation.tryParse(actualKey);

		if (id == null) {
			return null;
		}

		return BuiltInRegistries.VILLAGER_PROFESSION.get(id);
	}

	public String getKeyFromProfession(VillagerProfession profession) {

		ResourceLocation key =
				BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);

		return key == null ? "" : key.toString();
	}

	public Item getItemFromKey(String itemKey) {

		ResourceLocation id = ResourceLocation.tryParse(itemKey);

		if (id == null) {
			return null;
		}

		return BuiltInRegistries.ITEM.get(id);
	}

	public ResourceLocation getRegistryNameItem(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}

	public static Enchantment getEnchantmentFromKey(String key) {

		if (CVT.TRADE_UTIL == null) {
			return null;
		}

		ResourceLocation id = ResourceLocation.tryParse(key);

		if (id == null) {
			return null;
		}

		RegistryAccess access =
				CVT.TRADE_UTIL.getRegistryAccess();

		return access
				.registryOrThrow(Registries.ENCHANTMENT)
				.get(id);
	}

	public static List<Enchantment> getRegisteredEnchantments() {

		if (CVT.TRADE_UTIL == null) {
			return List.of();
		}

		RegistryAccess access =
				CVT.TRADE_UTIL.getRegistryAccess();

		return access
				.registryOrThrow(Registries.ENCHANTMENT)
				.holders()
				.map(Holder::value)
				.toList();
	}

	public static ResourceLocation getRegistryNameEnchantment(
			Enchantment enchantment) {

		if (CVT.TRADE_UTIL == null) {
			return null;
		}

		RegistryAccess access =
				CVT.TRADE_UTIL.getRegistryAccess();

		return access
				.registryOrThrow(Registries.ENCHANTMENT)
				.getKey(enchantment);
	}

	public static Potion getPotionFromKey(String key) {

		ResourceLocation id = ResourceLocation.tryParse(key);

		if (id == null) {
			return null;
		}

		return BuiltInRegistries.POTION.get(id);
	}

	public List<Potion> getRegisteredPotions() {

		return BuiltInRegistries.POTION
				.stream()
				.collect(Collectors.toList());
	}

	public ResourceLocation getPotionKey(Potion potion) {
		return BuiltInRegistries.POTION.getKey(potion);
	}

	public static MobEffect getEffectFromKey(String key) {

		ResourceLocation id = ResourceLocation.tryParse(key);

		if (id == null) {
			return null;
		}

		return BuiltInRegistries.MOB_EFFECT.get(id);
	}

	public List<MobEffect> getRegisteredMobEffects() {

		return BuiltInRegistries.MOB_EFFECT
				.stream()
				.collect(Collectors.toList());
	}

	public ResourceLocation getRegistryNameEffect(MobEffect effect) {

		return BuiltInRegistries.MOB_EFFECT.getKey(effect);
	}
}
