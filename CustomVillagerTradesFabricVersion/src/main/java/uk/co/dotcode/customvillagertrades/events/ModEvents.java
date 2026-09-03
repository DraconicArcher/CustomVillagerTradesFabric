package uk.co.dotcode.customvillagertrades.events;

import net.minecraft.world.entity.npc.VillagerTrades;

import uk.co.dotcode.customvillagertrades.ConfigHandler;
import uk.co.dotcode.customvillagertrades.ModLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModEvents {

	private ModEvents() {
		// Utility class
	}


	/**
	 * Adds custom villager trades to an existing trade map.
	 */
	public static void addVillagerTrades(
			Map<Integer, List<VillagerTrades.ItemListing>> target,
			Map<Integer, List<VillagerTrades.ItemListing>> custom
	) {

		if (target == null || custom == null) {
			return;
		}

		for (Map.Entry<Integer, List<VillagerTrades.ItemListing>> entry
				: custom.entrySet()) {

			Integer level = entry.getKey();

			List<VillagerTrades.ItemListing> listings =
					entry.getValue();

			if (level == null ||
					listings == null ||
					listings.isEmpty()) {

				continue;
			}

			target
					.computeIfAbsent(
							level,
							ignored -> new ArrayList<>()
					)
					.addAll(listings);
		}
	}


	/**
	 * Gets custom wandering-trader trades for a specific level.
	 */
	public static List<VillagerTrades.ItemListing> getWandererTrades(
			int level
	) {

		Map<Integer, List<VillagerTrades.ItemListing>> trades =
				ConfigHandler.registeredCustomWandererTrades;

		if (trades == null) {
			return List.of();
		}

		List<VillagerTrades.ItemListing> result =
				trades.get(level);

		return result == null
				? List.of()
				: result;
	}


	/**
	 * Logs the current custom trade state.
	 */
	public static void logTradeState() {

		ModLogger.info(
				"Registered custom villager trades: "
						+ ConfigHandler.registeredCustomTrades.keySet()
		);

		ModLogger.info(
				"Registered all-category trades: "
						+ (
						ConfigHandler.registeredAllCategoryTrades == null
								? 0
								: ConfigHandler.registeredAllCategoryTrades.size()
				)
		);

		ModLogger.info(
				"Registered wanderer trades: "
						+ (
						ConfigHandler.registeredCustomWandererTrades == null
								? 0
								: ConfigHandler.registeredCustomWandererTrades.size()
				)
		);
	}
}
