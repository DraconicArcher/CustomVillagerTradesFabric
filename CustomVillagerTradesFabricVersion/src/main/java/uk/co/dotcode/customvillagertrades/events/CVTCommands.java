package uk.co.dotcode.customvillagertrades.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.trading.MerchantOffer;

import uk.co.dotcode.customvillagertrades.CVT;
import uk.co.dotcode.customvillagertrades.ConfigHandler;
import uk.co.dotcode.customvillagertrades.ModLogger;
import uk.co.dotcode.customvillagertrades.TradeUtil;

import uk.co.dotcode.customvillagertrades.trades.*;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CVTCommands {

	public static void register(
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context
	) {

		int permissionLevel =
				CVT.globalConfig.opLevel;

		dispatcher.register(
				Commands.literal("reloadCVT")
						.requires(
								src ->
										src.hasPermission(
												permissionLevel
										)
						)
						.executes(
								CVTCommands::refreshTrades
						)
		);

		dispatcher.register(
				Commands.literal("exportCVT")
						.requires(
								src ->
										src.hasPermission(
												permissionLevel
										)
						)
						.executes(
								CVTCommands::exportTrades
						)
		);

		dispatcher.register(
				Commands.literal("addCVT")
						.requires(
								src ->
										src.hasPermission(
												permissionLevel
										)
						)
						.then(
								Commands.argument(
												"profession",
												ResourceLocationArgument.id()
										)
										.suggests(
												CVTCommands::professionSuggestions
										)
										.then(
												Commands.argument(
																"offerItem",
																ItemArgument.item(
																		context
																)
														)
														.then(
																Commands.argument(
																				"offerAmount",
																				IntegerArgumentType.integer(
																						1,
																						64
																				)
																		)
																		.then(
																				Commands.argument(
																								"requestItem",
																								ItemArgument.item(
																										context
																								)
																						)
																						.then(
																								Commands.argument(
																												"requestAmount",
																												IntegerArgumentType.integer(
																														1,
																														64
																												)
																										)
																										.then(
																												Commands.argument(
																																"tradeExp",
																																IntegerArgumentType.integer(
																																		0
																																)
																														)
																														.then(
																																Commands.argument(
																																				"maxUses",
																																				IntegerArgumentType.integer(
																																						1
																																				)
																																		)
																																		.then(
																																				Commands.argument(
																																								"tradeLevel",
																																								IntegerArgumentType.integer(
																																										1,
																																										5
																																								)
																																						)
																																						.executes(
																																								CVTCommands::addTrade
																																						)
																																		)
																														)
																										)
																						)
																		)
														)
										)
						)
		);

		dispatcher.register(
				Commands.literal("removeCVT")
						.requires(
								src ->
										src.hasPermission(
												permissionLevel
										)
						)
						.then(
								Commands.argument(
												"UTID",
												StringArgumentType.greedyString()
										)
										.suggests(
												CVTCommands::utidSuggestions
										)
										.executes(
												CVTCommands::removeTrade
										)
						)
		);
	}

	static int refreshTrades(
			CommandContext<CommandSourceStack> ctx
	) {

		CVT.reload();

		broadcast(
				ctx,
				"Reloaded villager trades"
		);

		for (var player :
				ctx.getSource()
						.getServer()
						.getPlayerList()
						.getPlayers()) {

			CVT.sendConfigIssues(player);
		}

		return 1;
	}

	static int exportTrades(
			CommandContext<CommandSourceStack> ctx
	) {

		Entity entity =
				ctx.getSource().getEntity();

		if (entity == null) {
			return 0;
		}

		exportAllTrades(entity);

		exportWandererTrades(entity);

		broadcast(
				ctx,
				"Exported villager trades"
		);

		return 1;
	}

	static int addTrade(
			CommandContext<CommandSourceStack> ctx
	) {

		MyTrade trade =
				new MyTrade();

		TradeItem offer =
				new TradeItem();

		offer.itemKey =
				ItemArgument.getItem(
								ctx,
								"offerItem"
						)
						.getItem()
						.builtInRegistryHolder()
						.key()
						.location()
						.toString();

		offer.amount =
				IntegerArgumentType.getInteger(
						ctx,
						"offerAmount"
				);

		TradeItem request =
				new TradeItem();

		request.itemKey =
				ItemArgument.getItem(
								ctx,
								"requestItem"
						)
						.getItem()
						.builtInRegistryHolder()
						.key()
						.location()
						.toString();

		request.amount =
				IntegerArgumentType.getInteger(
						ctx,
						"requestAmount"
				);

		trade.offer = offer;
		trade.request = request;

		trade.tradeExp =
				IntegerArgumentType.getInteger(
						ctx,
						"tradeExp"
				);

		trade.maxUses =
				IntegerArgumentType.getInteger(
						ctx,
						"maxUses"
				);

		trade.tradeLevel =
				IntegerArgumentType.getInteger(
						ctx,
						"tradeLevel"
				);

		String utid =
				TradeRegistry.addNewTrade(
						ResourceLocationArgument
								.getId(
										ctx,
										"profession"
								)
								.toString(),
						trade
				);

		broadcast(
				ctx,
				"Added trade: " + utid
		);

		return 1;
	}

	static int removeTrade(
			CommandContext<CommandSourceStack> ctx
	) {

		String msg =
				TradeRegistry.removeTrade(
						StringArgumentType.getString(
								ctx,
								"UTID"
						)
				);

		broadcast(
				ctx,
				msg
		);

		return 1;
	}

	/*
	 * =============================================================
	 * VILLAGER TRADE EXPORT
	 * =============================================================
	 */

	private static void exportAllTrades(
			Entity entity
	) {

		for (VillagerProfession profession :
				TradeUtil.getAllProfessions()) {

			Int2ObjectMap<
					VillagerTrades.ItemListing[]
					> trades =
					VillagerTrades.TRADES.get(
							profession
					);

			if (trades == null) {
				continue;
			}

			TradeCollection collection =
					new TradeCollection();

			collection.profession =
					TradeUtil.getKeyFromProfession(
							profession
					);

			List<MyTrade> out =
					new ArrayList<>();

			for (int level = 1;
				 level <= 5;
				 level++) {

				VillagerTrades.ItemListing[] listings =
						trades.get(level);

				if (listings == null) {
					continue;
				}

				for (
						VillagerTrades.ItemListing listing :
						listings
				) {

					MerchantOffer offer;

					try {

						offer =
								listing.getOffer(
										entity,
										TradeUtil.randomSource
								);

					} catch (Exception e) {

						ModLogger.error(
								"Failed exporting trade from listing: "
										+ listing.getClass()
										.getName()
						);

						e.printStackTrace();

						continue;
					}

					if (offer == null) {
						continue;
					}

					MyTrade trade =
							new MyTrade();

					/*
					 * -------------------------------------------------
					 * Request
					 * -------------------------------------------------
					 */
					trade.request =
							createTradeItem(
									offer.getBaseCostA()
							);

					/*
					 * -------------------------------------------------
					 * Offer
					 * -------------------------------------------------
					 */
					trade.offer =
							createTradeItem(
									offer.getResult()
							);

					/*
					 * -------------------------------------------------
					 * Preserve vanilla enchantment RANDOMNESS.
					 * -------------------------------------------------
					 *
					 * We inspect the ItemListing, NOT the generated
					 * ItemStack.
					 *
					 * The generated ItemStack contains one particular
					 * random enchantment. Exporting that would freeze
					 * the random result.
					 */
					applyEnchantmentRules(
							listing,
							trade.offer
					);

					trade.tradeExp =
							offer.getXp();

					trade.maxUses =
							offer.getMaxUses();

					trade.priceMultiplier =
							offer.getPriceMultiplier();

					trade.demand =
							offer.getDemand();

					trade.tradeLevel =
							level;

					out.add(trade);
				}
			}

			collection.trades =
					out;

			ModLogger.info(
					"Exported "
							+ out.size()
							+ " trades for "
							+ collection.profession
			);

			ConfigHandler.exportTradeCollection(
					collection
			);
		}
	}

	/*
	 * =============================================================
	 * CREATE BASIC TRADE ITEM
	 * =============================================================
	 */

	private static TradeItem createTradeItem(
			ItemStack stack
	) {

		TradeItem item =
				new TradeItem();

		item.itemKey =
				stack.getItem()
						.builtInRegistryHolder()
						.key()
						.location()
						.toString();

		item.amount =
				stack.getCount();

		return item;
	}

	/*
	 * =============================================================
	 * ENCHANTMENT RULE EXPORT
	 * =============================================================
	 */

	private static void applyEnchantmentRules(
			VillagerTrades.ItemListing listing,
			TradeItem tradeItem
	) {

		/*
		 * Vanilla enchanted equipment:
		 *
		 *     emeralds -> enchanted sword/tool/etc.
		 *
		 * Vanilla uses the enchantments available from the
		 * on_traded_equipment tag and generates the enchantment
		 * dynamically.
		 *
		 * The vanilla level range is 5-19 inclusive.
		 *
		 * nextInt(5, 20) in the vanilla implementation gives
		 * the equivalent range.
		 */
		if (listing instanceof
				VillagerTrades.EnchantedItemForEmeralds) {

			EnchantmentEntry entry =
					new EnchantmentEntry(
							List.of("random"),
							5,
							19,
							"minecraft:on_traded_equipment"
					);

			tradeItem.setEnchantments(
					List.of(entry)
			);

			return;
		}

		/*
		 * Vanilla enchanted books.
		 *
		 * EnchantBookForEmeralds stores:
		 *
		 *     minLevel
		 *     maxLevel
		 *     tradeableEnchantments
		 *
		 * so preserve all three.
		 */
		if (listing instanceof
				VillagerTrades.EnchantBookForEmeralds bookListing) {

			int minLevel =
					getIntField(
							bookListing,
							"minLevel",
							1
					);

			int maxLevel =
					getIntField(
							bookListing,
							"maxLevel",
							minLevel
					);

			String tag =
					getTagField(
							bookListing,
							"tradeableEnchantments"
					);

			if (tag == null) {

				/*
				 * Fallback for mappings/implementations where
				 * the private field cannot be read.
				 */
				tag =
						"minecraft:tradeable";
			}

			EnchantmentEntry entry =
					new EnchantmentEntry(
							List.of("random"),
							minLevel,
							maxLevel,
							tag
					);

			tradeItem.setEnchantments(
					List.of(entry)
			);
		}
	}

	/*
	 * =============================================================
	 * REFLECTION HELPERS
	 * =============================================================
	 *
	 * EnchantBookForEmeralds exposes minLevel, maxLevel and
	 * tradeableEnchantments as private final fields in 1.21.1.
	 *
	 * We use reflection here so that we don't need a Mixin merely
	 * to expose three exporter-only fields.
	 */

	private static int getIntField(
			Object object,
			String fieldName,
			int fallback
	) {

		try {

			Field field =
					findField(
							object.getClass(),
							fieldName
					);

			if (field == null) {
				return fallback;
			}

			field.setAccessible(true);

			return field.getInt(object);

		} catch (Exception e) {

			ModLogger.warn(
					"Could not read integer field '"
							+ fieldName
							+ "' from "
							+ object.getClass().getName()
							+ ". Using "
							+ fallback
			);

			return fallback;
		}
	}

	private static String getTagField(
			Object object,
			String fieldName
	) {

		try {

			Field field =
					findField(
							object.getClass(),
							fieldName
					);

			if (field == null) {
				return null;
			}

			field.setAccessible(true);

			Object value =
					field.get(object);

			if (value == null) {
				return null;
			}

			/*
			 * TagKey#location() gives the namespaced tag ID.
			 */
			try {

				var method =
						value.getClass()
								.getMethod(
										"location"
								);

				Object location =
						method.invoke(value);

				return location.toString();

			} catch (Exception ignored) {
				/*
				 * Fall through to toString().
				 */
			}

			String string =
					value.toString();

			/*
			 * Some representations include a leading '#'.
			 * Our config stores the ResourceLocation without it.
			 */
			if (string.startsWith("#")) {
				string =
						string.substring(1);
			}

			return string;

		} catch (Exception e) {

			ModLogger.warn(
					"Could not read enchantment tag field '"
							+ fieldName
							+ "' from "
							+ object.getClass().getName()
			);

			return null;
		}
	}

	private static Field findField(
			Class<?> clazz,
			String fieldName
	) {

		Class<?> current =
				clazz;

		while (current != null) {

			try {

				return current.getDeclaredField(
						fieldName
				);

			} catch (NoSuchFieldException ignored) {

				current =
						current.getSuperclass();
			}
		}

		return null;
	}

	/*
	 * =============================================================
	 * WANDERING TRADER
	 * =============================================================
	 */

	private static void exportWandererTrades(
			Entity entity
	) {

		Int2ObjectMap<
				VillagerTrades.ItemListing[]
				> trades =
				VillagerTrades
						.WANDERING_TRADER_TRADES;

		WandererTradeCollection collection =
				new WandererTradeCollection();

		collection.profession =
				"wanderer";

		List<MyWandererTrade> out =
				new ArrayList<>();

		convertWandererLevel(
				entity,
				trades.get(1),
				false,
				out
		);

		convertWandererLevel(
				entity,
				trades.get(2),
				true,
				out
		);

		collection.trades =
				out.toArray(
						new MyWandererTrade[0]
				);

		ConfigHandler
				.exportWandererTradeCollection(
						collection
				);
	}

	private static void convertWandererLevel(
			Entity entity,
			VillagerTrades.ItemListing[] listings,
			boolean rare,
			List<MyWandererTrade> out
	) {

		if (listings == null) {
			return;
		}

		for (
				VillagerTrades.ItemListing listing :
				listings
		) {

			MerchantOffer offer;

			try {

				offer =
						listing.getOffer(
								entity,
								TradeUtil.randomSource
						);

			} catch (Exception e) {

				continue;
			}

			if (offer == null) {
				continue;
			}

			MyWandererTrade trade =
					new MyWandererTrade();

			trade.request =
					createTradeItem(
							offer.getBaseCostA()
					);

			trade.offer =
					createTradeItem(
							offer.getResult()
					);

			/*
			 * Wandering trader trades normally do not use
			 * enchanted equipment/book listings, but keeping this
			 * here makes the exporter future-proof.
			 */
			applyEnchantmentRules(
					listing,
					trade.offer
			);

			trade.tradeExp =
					offer.getXp();

			trade.maxUses =
					offer.getMaxUses();

			trade.priceMultiplier =
					offer.getPriceMultiplier();

			trade.demand =
					offer.getDemand();

			trade.tradeLevel =
					rare ? 2 : 1;

			out.add(trade);
		}
	}

	private static void broadcast(
			CommandContext<CommandSourceStack> ctx,
			String msg
	) {

		ctx.getSource()
				.getServer()
				.getPlayerList()
				.broadcastSystemMessage(
						Component.literal(msg),
						false
				);
	}

	private static CompletableFuture<Suggestions>
	professionSuggestions(
			CommandContext<CommandSourceStack> ctx,
			SuggestionsBuilder builder
	) {

		for (
				VillagerProfession profession :
				TradeUtil.getAllProfessions()
		) {

			builder.suggest(
					TradeUtil.getKeyFromProfession(
							profession
					)
			);
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions>
	utidSuggestions(
			CommandContext<CommandSourceStack> ctx,
			SuggestionsBuilder builder
	) {

		for (
				String key :
				TradeRegistry.usedUTIDs.keySet()
		) {

			builder.suggest(key);
		}

		return builder.buildFuture();
	}
}
