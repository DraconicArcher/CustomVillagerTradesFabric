package uk.co.dotcode.customvillagertrades.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import uk.co.dotcode.customvillagertrades.ConfigHandler;
import uk.co.dotcode.customvillagertrades.TradeUtil;
import uk.co.dotcode.customvillagertrades.trades.TradeCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(Villager.class)
public abstract class MixinVillagerTrades {

	/**
	 * Adds CVT trades after vanilla has generated the villager's trades.
	 */
	@Inject(
			method = "updateTrades",
			at = @At("TAIL")
	)
	private void customvillagertrades$addCustomTrades(CallbackInfo ci) {

		Villager villager = (Villager) (Object) this;

		VillagerData data = villager.getVillagerData();
		VillagerProfession profession = data.getProfession();

		String professionKey =
				TradeUtil.getKeyFromProfession(profession);

		/*
		 * Ignore villagers without a valid profession.
		 */
		if (profession == VillagerProfession.NONE ||
				profession == VillagerProfession.NITWIT) {
			return;
		}

		/*
		 * Get the original configuration for this profession.
		 */
		TradeCollection collection =
				ConfigHandler.customTrades.get(professionKey);

		/*
		 * Get converted profession-specific trades.
		 */
		Map<Integer, List<VillagerTrades.ItemListing>> professionTrades =
				ConfigHandler.registeredCustomTrades.get(professionKey);

		/*
		 * Get converted "all" trades.
		 */
		Map<Integer, List<VillagerTrades.ItemListing>> allTrades =
				ConfigHandler.registeredAllCategoryTrades;

		boolean hasProfessionTrades =
				professionTrades != null
						&& !professionTrades.isEmpty();

		boolean hasAllTrades =
				allTrades != null
						&& !allTrades.isEmpty();

		if (!hasProfessionTrades && !hasAllTrades) {
			return;
		}

		int level = data.getLevel();

		MerchantOffers offers = villager.getOffers();

		/*
		 * If removeOtherTrades is enabled, remove all vanilla
		 * offers before adding CVT trades.
		 */
		if (collection != null && collection.removeOtherTrades) {
			offers.clear();
		}

		/*
		 * Add profession-specific trades.
		 */
		if (hasProfessionTrades) {

			List<VillagerTrades.ItemListing> listings =
					professionTrades.get(level);

			addListings(
					villager,
					offers,
					listings,
					getTradeLimit(collection)
			);
		}

		/*
		 * Add trades from the "all" category.
		 *
		 * These are added regardless of whether the villager
		 * has profession-specific trades.
		 */
		if (hasAllTrades) {

			List<VillagerTrades.ItemListing> listings =
					allTrades.get(level);

			addListings(
					villager,
					offers,
					listings,
					Integer.MAX_VALUE
			);
		}
	}


	/**
	 * Gets the maximum number of profession-specific custom
	 * trades that should be added.
	 */
	@Unique
	private static int getTradeLimit(TradeCollection collection) {

		if (collection == null) {
			return Integer.MAX_VALUE;
		}

		return Math.max(1, collection.maxTrades);
	}


	/**
	 * Converts ItemListing objects into MerchantOffers and adds
	 * them to the villager.
	 */
	@Unique
	private static void addListings(
			Villager villager,
			MerchantOffers offers,
			List<VillagerTrades.ItemListing> listings,
			int limit
	) {

		if (listings == null || listings.isEmpty()) {
			return;
		}

		/*
		 * Copy the list so we don't modify the registry/config list
		 * when shuffling it.
		 */
		List<VillagerTrades.ItemListing> shuffled =
				new ArrayList<>(listings);

		Collections.shuffle(
				shuffled,
				TradeUtil.random
		);

		int amount = Math.min(limit, shuffled.size());

		for (int i = 0; i < amount; i++) {

			VillagerTrades.ItemListing listing =
					shuffled.get(i);

			if (listing == null) {
				continue;
			}

			try {

				MerchantOffer offer =
						listing.getOffer(
								villager,
								villager.getRandom()
						);

				if (offer != null) {
					offers.add(offer);
				}

			} catch (Exception e) {

				System.err.println(
						"[Custom Villager Trades] "
								+ "Failed to create custom trade: "
								+ e.getMessage()
				);

				e.printStackTrace();
			}
		}
	}
}
