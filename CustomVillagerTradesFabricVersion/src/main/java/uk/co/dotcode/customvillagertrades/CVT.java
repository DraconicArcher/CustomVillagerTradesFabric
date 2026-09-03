package uk.co.dotcode.customvillagertrades;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import uk.co.dotcode.customvillagertrades.events.TradeRegistry;
import uk.co.dotcode.customvillagertrades.fabric.TradeUtilImpl;

public class CVT {

	public static final String MOD_ID = "customvillagertrades";

	public static GlobalConfig globalConfig = new GlobalConfig();

	public static TradeUtilImpl TRADE_UTIL;

	private static boolean initialized = false;


	public static void init() {

		ConfigHandler.registeredCustomTrades.clear();

		ConfigHandler.customTrades.forEach((key, collection) -> {
			TradeRegistry.registerCollection(key, collection, false);
		});

		TradeRegistry.registerTradesAllCategory();

		TradeRegistry.registerWanderingTrades();

		System.out.println("CVT INIT COMPLETE");
		System.out.println(
				"Loaded trades: "
						+ ConfigHandler.customTrades.keySet()
		);

		System.out.println(
				"Registered trades: "
						+ ConfigHandler.registeredCustomTrades.keySet()
		);

		initialized = true;
	}


	public static void reload() {

		ConfigHandler.init();

		if (ConfigHandler.ACCESS == null) {

			System.out.println(
					"CVT reload: config reloaded, "
							+ "server registry access is not available yet"
			);

			return;
		}

		ConfigHandler.finalizeTrades();

		init();

		System.out.println("CVT reload complete");
	}


	public static boolean isInitialized() {
		return initialized;
	}


	public static void sendConfigIssues(Player player) {

		if (!ModLogger.getConfigIssues().isEmpty()) {

			for (String s : ModLogger.getConfigIssues()) {

				player.sendSystemMessage(
						Component.literal(
								"[Custom Villager Trades] " + s
						)
				);
			}
		}
	}
}
