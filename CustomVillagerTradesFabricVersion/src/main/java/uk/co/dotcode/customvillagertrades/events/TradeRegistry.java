package uk.co.dotcode.customvillagertrades.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import uk.co.dotcode.customvillagertrades.ConfigHandler;
import uk.co.dotcode.customvillagertrades.TradeUtil;
import uk.co.dotcode.customvillagertrades.trades.MyTrade;
import uk.co.dotcode.customvillagertrades.trades.MyTradeConverted;
import uk.co.dotcode.customvillagertrades.trades.MyWandererTrade;
import uk.co.dotcode.customvillagertrades.trades.TradeCollection;

public final class TradeRegistry {

	private TradeRegistry() {
		// Utility class
	}

	/**
	 * Tracks UTIDs so trades can be removed later using /removeCVT.
	 */
	public static final Map<String, String> usedUTIDs = new HashMap<>();


	/**
	 * Converts the configured wandering-trader trades into ItemListings.
	 *
	 * Level 1 = common
	 * Level 2 = rare
	 */
	public static void registerWanderingTrades() {

		var data = ConfigHandler.loadWandererTrades("wanderer");

		if (data == null || !data.validate()) {
			return;
		}

		Map<Integer, List<VillagerTrades.ItemListing>> converted =
				new HashMap<>();

		if (data.trades != null) {

			for (MyWandererTrade trade : data.trades) {

				if (trade == null) {
					continue;
				}

				int level = trade.isRare ? 2 : 1;

				converted
						.computeIfAbsent(level, ignored -> new ArrayList<>())
						.add(new MyTradeConverted(trade));
			}
		}

		ConfigHandler.registeredCustomWandererTrades = converted;
	}


	/**
	 * Loads and registers the trades for a specific profession.
	 */
	public static void registerTrades(VillagerProfession profession) {

		if (profession == null) {
			return;
		}

		if (profession == VillagerProfession.NONE ||
				profession == VillagerProfession.NITWIT) {
			return;
		}

		String key = TradeUtil.getKeyFromProfession(profession);

		if (key == null || key.isEmpty()) {
			return;
		}

		TradeCollection data = ConfigHandler.loadTrades(key);

		registerCollection(key, data, false);
	}


	/**
	 * Converts a TradeCollection into Minecraft ItemListings and stores it.
	 */
	public static void registerCollection(
			String profession,
			TradeCollection tradeCollection,
			boolean reload
	) {

		if (profession == null || profession.isEmpty()) {
			return;
		}

		if (tradeCollection == null) {
			return;
		}

		/*
		 * Reload the configuration first when requested.
		 */
		if (reload) {
			ConfigHandler.init();

			tradeCollection =
					ConfigHandler.loadTrades(profession);

			if (tradeCollection == null) {
				return;
			}
		}

		/*
		 * Validate against the current server registry access.
		 */
		if (!tradeCollection.validate(ConfigHandler.ACCESS)) {
			return;
		}

		/*
		 * Generate missing UTIDs and clean null trades.
		 */
		tradeCollection =
				manageUTIDs(tradeCollection);

		/*
		 * Save generated UTIDs back to the configuration.
		 */
		if (tradeCollection.shouldUpdateFile) {
			ConfigHandler.overwriteTradeCollection(tradeCollection);
		}

		Map<Integer, List<VillagerTrades.ItemListing>> converted =
				new HashMap<>();

		if (tradeCollection.trades != null) {

			for (MyTrade trade : tradeCollection.trades) {

				if (trade == null || trade.tradeLevel == null) {
					continue;
				}

				converted
						.computeIfAbsent(
								trade.tradeLevel,
								ignored -> new ArrayList<>()
						)
						.add(new MyTradeConverted(trade));
			}
		}

		ConfigHandler.registeredCustomTrades.put(
				profession.toLowerCase(),
				converted
		);

		ConfigHandler.customTrades.put(
				profession.toLowerCase(),
				tradeCollection
		);
	}


	/**
	 * Registers trades from the special "all" category.
	 *
	 * These trades are subsequently added to every villager.
	 */
	public static void registerTradesAllCategory() {

		TradeCollection data =
				ConfigHandler.loadTrades("all");

		if (data == null) {
			return;
		}

		if (!data.validate(ConfigHandler.ACCESS)) {
			return;
		}

		data = manageUTIDs(data);

		if (data.shouldUpdateFile) {
			ConfigHandler.overwriteTradeCollection(data);
		}

		Map<Integer, List<VillagerTrades.ItemListing>> converted =
				new HashMap<>();

		if (data.trades != null) {

			for (MyTrade trade : data.trades) {

				if (trade == null || trade.tradeLevel == null) {
					continue;
				}

				converted
						.computeIfAbsent(
								trade.tradeLevel,
								ignored -> new ArrayList<>()
						)
						.add(new MyTradeConverted(trade));
			}
		}

		ConfigHandler.registeredAllCategoryTrades = converted;
	}


	/**
	 * Converts a wandering trader trade into a Minecraft ItemListing.
	 */
	public static VillagerTrades.ItemListing convert(
			MyWandererTrade trade
	) {

		if (trade == null) {
			return null;
		}

		return new MyTradeConverted(trade);
	}


	/**
	 * Generates missing UTIDs and removes null entries.
	 */
	public static TradeCollection manageUTIDs(
			TradeCollection collection
	) {

		if (collection == null || collection.trades == null) {
			return collection;
		}

		TradeCollection out =
				new TradeCollection();

		out.profession =
				collection.profession;

		out.removeOtherTrades =
				collection.removeOtherTrades;

		out.shouldUpdateFile =
				collection.shouldUpdateFile;

		List<MyTrade> safeList =
				new ArrayList<>();

		for (MyTrade trade : collection.trades) {

			if (trade == null) {
				continue;
			}

			/*
			 * Generate a UTID when the configuration doesn't have one.
			 */
			if (trade.UTID == null ||
					trade.UTID.isEmpty()) {

				trade =
						TradeUtil.generateUTID(
								collection.profession,
								trade
						);

				out.shouldUpdateFile = true;
			}

			usedUTIDs.put(
					trade.UTID,
					collection.profession
			);

			safeList.add(trade);
		}

		out.trades =
				List.of(safeList.toArray(new MyTrade[0]));

		return out;
	}


	/**
	 * Adds a new trade to a profession.
	 */
	public static String addNewTrade(
			String profession,
			MyTrade trade
	) {

		if (profession == null ||
				profession.isEmpty()) {

			return "Failed to add trade: invalid profession";
		}

		if (trade == null) {
			return "Failed to add trade: trade was null";
		}

		/*
		 * Generate a UTID if necessary.
		 */
		if (trade.UTID == null ||
				trade.UTID.isEmpty()) {

			trade =
					TradeUtil.generateUTID(
							profession,
							trade
					);
		}

		usedUTIDs.put(
				trade.UTID,
				profession
		);

		TradeCollection collection =
				ConfigHandler.customTrades.get(
						profession
				);

		if (collection == null) {

			collection =
					new TradeCollection();

			collection.profession =
					profession;

			collection.removeOtherTrades =
					false;

			collection.trades =
					List.of(new MyTrade[0]);
		}

		List<MyTrade> trades =
				new ArrayList<>();

		if (collection.trades != null) {
			for (MyTrade existing :
					collection.trades) {

				if (existing != null) {
					trades.add(existing);
				}
			}
		}

		trades.add(trade);

		collection.trades =
				List.of(trades.toArray(new MyTrade[0]));

		collection.shouldUpdateFile =
				true;

		/*
		 * Register the updated collection.
		 */
		registerCollection(
				profession,
				collection,
				false
		);

		return trade.UTID;
	}


	/**
	 * Removes a trade using its UTID.
	 */
	public static String removeTrade(
			String utid
	) {

		if (utid == null ||
				utid.isEmpty()) {

			return "Failed to remove trade " + utid;
		}

		String profession =
				usedUTIDs.get(utid);

		if (profession == null) {
			return "Failed to remove trade " + utid;
		}

		TradeCollection collection =
				ConfigHandler.customTrades.get(
						profession
				);

		if (collection == null ||
				collection.trades == null) {

			return "Failed to remove trade " + utid;
		}

		List<MyTrade> remaining =
				new ArrayList<>();

		boolean removed = false;

		for (MyTrade trade :
				collection.trades) {

			if (trade == null) {
				continue;
			}

			if (utid.equals(trade.UTID)) {
				removed = true;
				continue;
			}

			remaining.add(trade);
		}

		if (!removed) {
			return "Failed to remove trade " + utid;
		}

		collection.trades =
				List.of(remaining.toArray(new MyTrade[0]));

		collection.shouldUpdateFile =
				true;

		usedUTIDs.remove(utid);

		registerCollection(
				profession,
				collection,
				false
		);

		return "Removed trade " + utid;
	}
}
