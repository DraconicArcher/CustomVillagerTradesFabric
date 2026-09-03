package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import uk.co.dotcode.customvillagertrades.ModLogger;
import uk.co.dotcode.customvillagertrades.TradeUtil;

public class MyTradeEffect {

	public String effectKey;
	public Integer duration; // ticks
	public Integer level;
	public Boolean isVisible;

	private int getDuration() {
		return duration == null ? 300 : duration;
	}

	private int getLevel() {
		int lvl = level == null ? 1 : level;
		return Math.max(lvl - 1, 0); // convert to amplifier
	}

	private boolean isVisible() {
		return isVisible == null || isVisible;
	}



	public MobEffectInstance getInstance() {
		return buildInstance(effectKey);
	}

	public MobEffectInstance getInstance(String chosenKey) {
		return buildInstance(chosenKey);
	}

	private MobEffectInstance buildInstance(String key) {

		ResourceLocation id = ResourceLocation.tryParse(key);

		if (id == null) {
			ModLogger.warn("Invalid effect key: " + key);
			return null;
		}

		Holder<MobEffect> holder =
				net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
						.getHolder(id)
						.orElse(null);

		if (holder == null) {
			ModLogger.warn("Invalid effect key: " + key);
			return null;
		}

		return new MobEffectInstance(
				holder,
				getDuration(),
				getLevel(),
				false,
				isVisible(),
				isVisible()
		);
	}

	public boolean validate(String profession, int tradeEntry, String itemKey, RegistryAccess access) {

		boolean isValid = true;

		if (effectKey == null || effectKey.isEmpty()) {

			ModLogger.warn(
					"Unable to add custom trade: effect key missing - profession="
							+ profession + ", entry=" + tradeEntry + ", item=" + itemKey
			);

			return false;
		}

		ResourceLocation id = ResourceLocation.tryParse(effectKey);

		if (id == null || !access.registryOrThrow(Registries.MOB_EFFECT).containsKey(id)) {

			ModLogger.warn(
					"Unable to add custom trade: invalid effect "
							+ effectKey + ", profession=" + profession
							+ ", entry=" + tradeEntry + ", item=" + itemKey
			);

			return false;
		}

		return true;
	}
}