package uk.co.dotcode.customvillagertrades.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.commands.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CVTCommands {

	public static void register(CommandDispatcher<CommandSourceStack>
										dispatcher,
								CommandBuildContext context) {

		int permissionLevel = CVT.globalConfig.opLevel;

		dispatcher.register(Commands.literal("reloadCVT")
				.requires(src -> src.hasPermission(permissionLevel))
				.executes(CVTCommands::refreshTrades));

		dispatcher.register(Commands.literal("exportCVT")
				.requires(src -> src.hasPermission(permissionLevel))
				.executes(CVTCommands::exportTrades));

		dispatcher.register(Commands.literal("addCVT")
				.requires(src -> src.hasPermission(permissionLevel))
				.then(Commands.argument("profession", ResourceLocationArgument.id())
						.suggests(CVTCommands::professionSuggestions)
						.then(Commands.argument("offerItem", ItemArgument.item(context))
								.then(Commands.argument("offerAmount", IntegerArgumentType.integer(1, 64))
										.then(Commands.argument("requestItem", ItemArgument.item(context))
												.then(Commands.argument("requestAmount", IntegerArgumentType.integer(1, 64))
														.then(Commands.argument("tradeExp", IntegerArgumentType.integer(0))
																.then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
																		.then(Commands.argument("tradeLevel", IntegerArgumentType.integer(1, 5))
																				.executes(CVTCommands::addTrade))))))))));

		dispatcher.register(Commands.literal("removeCVT")
				.requires(src -> src.hasPermission(permissionLevel))
				.then(Commands.argument("UTID", StringArgumentType.greedyString())
						.suggests(CVTCommands::utidSuggestions)
						.executes(CVTCommands::removeTrade)));
	}



	static int refreshTrades(CommandContext<CommandSourceStack> ctx) {
		CVT.reload();

		broadcast(ctx, "Reloaded villager trades");

		for (var p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			CVT.sendConfigIssues(p);
		}

		return 1;
	}



	static int exportTrades(CommandContext<CommandSourceStack> ctx) {
		Entity entity = ctx.getSource().getEntity();

		exportAllTrades(entity);
		exportWandererTrades(entity);

		broadcast(ctx, "Exported villager trades");
		return 1;
	}



	static int addTrade(CommandContext<CommandSourceStack> ctx) {

		MyTrade trade = new MyTrade();

		TradeItem offer = new TradeItem();
		offer.itemKey = ItemArgument.getItem(ctx, "offerItem").getItem().builtInRegistryHolder().key().location().toString();
		offer.amount = IntegerArgumentType.getInteger(ctx, "offerAmount");

		TradeItem request = new TradeItem();
		request.itemKey = ItemArgument.getItem(ctx, "requestItem").getItem().builtInRegistryHolder().key().location().toString();
		request.amount = IntegerArgumentType.getInteger(ctx, "requestAmount");

		trade.offer = offer;
		trade.request = request;

		trade.tradeExp = IntegerArgumentType.getInteger(ctx, "tradeExp");
		trade.maxUses = IntegerArgumentType.getInteger(ctx, "maxUses");
		trade.tradeLevel = IntegerArgumentType.getInteger(ctx, "tradeLevel");

		String utid = TradeRegistry.addNewTrade(
				ResourceLocationArgument.getId(ctx, "profession").toString(),
				trade
		);

		broadcast(ctx, "Added trade: " + utid);
		return 1;
	}


	static int removeTrade(CommandContext<CommandSourceStack> ctx) {
		String msg = TradeRegistry.removeTrade(
				StringArgumentType.getString(ctx, "UTID")
		);

		broadcast(ctx, msg);
		return 1;
	}


	private static void exportAllTrades(Entity entity) {

		for (VillagerProfession profession : TradeUtil.getAllProfessions()) {

			Int2ObjectMap<VillagerTrades.ItemListing[]> trades =
					VillagerTrades.TRADES.get(profession);

			if (trades == null) continue;

			ModLogger.info("Trade levels for "
					+ TradeUtil.getKeyFromProfession(profession)
					+ ": "
					+ trades.keySet());

			TradeCollection collection = new TradeCollection();
			collection.profession = TradeUtil.getKeyFromProfession(profession);

			ModLogger.info("Exporting profession: " + collection.profession);

			List<MyTrade> out = new ArrayList<>();

			for (int level = 1; level <= 5; level++) {

				VillagerTrades.ItemListing[] listings = trades.get(level);
				if (listings == null) continue;

				for (VillagerTrades.ItemListing listing : listings) {

					MerchantOffer offer;
					try {
						offer = listing.getOffer(entity, TradeUtil.randomSource);
					} catch (Exception e) {

						ModLogger.error(
								"Failed exporting trade from listing: "
										+ listing.getClass().getName()
						);

						e.printStackTrace();

						continue;
					}

					if (offer == null) continue;

					MyTrade t = new MyTrade();

					t.request = new TradeItem();
					t.request.itemKey = offer.getBaseCostA().getItem().builtInRegistryHolder().key().location().toString();
					t.request.amount = offer.getBaseCostA().getCount();

					t.offer = new TradeItem();
					t.offer.itemKey = offer.getResult().getItem().builtInRegistryHolder().key().location().toString();
					t.offer.amount = offer.getResult().getCount();

					t.tradeExp = offer.getXp();
					t.maxUses = offer.getMaxUses();
					t.priceMultiplier = offer.getPriceMultiplier();
					t.demand = offer.getDemand();
					t.tradeLevel = level;

					out.add(t);
				}
			}

			collection.trades = out;

			ModLogger.info(
					"Exported "
							+ out.size()
							+ " trades for "
							+ collection.profession
			);

			ConfigHandler.exportTradeCollection(collection);
		}
	}


	private static void exportWandererTrades(Entity entity) {

		Int2ObjectMap<VillagerTrades.ItemListing[]> trades =
				VillagerTrades.WANDERING_TRADER_TRADES;

		WandererTradeCollection collection = new WandererTradeCollection();
		collection.profession = "wanderer";

		List<MyWandererTrade> out = new ArrayList<>();

		convertWandererLevel(entity, trades.get(1), false, out);
		convertWandererLevel(entity, trades.get(2), true, out);

		collection.trades = out.toArray(new MyWandererTrade[0]);
		ConfigHandler.exportWandererTradeCollection(collection);
	}

	private static void convertWandererLevel(Entity entity,
											 VillagerTrades.ItemListing[] listings,
											 boolean rare,
											 List<MyWandererTrade> out) {

		if (listings == null) return;

		for (VillagerTrades.ItemListing listing : listings) {

			MerchantOffer offer;
			try {
				offer = listing.getOffer(entity, TradeUtil.randomSource);
			} catch (Exception e) {
				continue;
			}

			if (offer == null) continue;

			MyWandererTrade t = new MyWandererTrade();

			t.request = new TradeItem();
			t.request.itemKey = offer.getBaseCostA().getItem().builtInRegistryHolder().key().location().toString();
			t.request.amount = offer.getBaseCostA().getCount();

			t.offer = new TradeItem();
			t.offer.itemKey = offer.getResult().getItem().builtInRegistryHolder().key().location().toString();
			t.offer.amount = offer.getResult().getCount();

			t.tradeExp = offer.getXp();
			t.maxUses = offer.getMaxUses();
			t.priceMultiplier = offer.getPriceMultiplier();
			t.demand = offer.getDemand();
			t.tradeLevel = rare ? 2 : 1;

			out.add(t);
		}
	}


	private static void broadcast(CommandContext<CommandSourceStack> ctx, String msg) {
		ctx.getSource().getServer().getPlayerList()
				.broadcastSystemMessage(Component.literal(msg), false);
	}

	private static CompletableFuture<Suggestions> professionSuggestions(
			CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {

		for (VillagerProfession profession : TradeUtil.getAllProfessions()) {
			builder.suggest(TradeUtil.getKeyFromProfession(profession));
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> utidSuggestions(
			CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {

		for (String key : TradeRegistry.usedUTIDs.keySet()) {
			builder.suggest(key);
		}

		return builder.buildFuture();
	}
}