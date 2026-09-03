package uk.co.dotcode.customvillagertrades.trades;


import net.minecraft.util.RandomSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

import uk.co.dotcode.customvillagertrades.ModLogger;

public class MyTradeConverted implements VillagerTrades.ItemListing {

	private final MyTrade villagerTrade;
	private final MyWandererTrade wandererTrade;

	public MyTradeConverted(MyTrade trade) {
		this.villagerTrade = trade;
		this.wandererTrade = null;
	}

	public MyTradeConverted(MyWandererTrade trade) {
		this.villagerTrade = null;
		this.wandererTrade = trade;
	}

	@Override
	public @Nullable MerchantOffer getOffer(Entity entity, RandomSource random) {

		try {

			MyTrade trade;

			if (villagerTrade != null) {
				trade = villagerTrade;
			}
			else if (wandererTrade != null) {
				trade = wandererTrade;
			}
			else {
				ModLogger.error("Trade was null");
				return null;
			}





		System.out.println("getOffer called");
			if (trade == null) {
				ModLogger.error("Trade was null");
				return null;
			}



			TradeItem request = trade.request;
			TradeItem extra = trade.additionalRequest;

			if (request == null) {
				ModLogger.error("Trade request was null");
				return null;
			}

			ItemStack costAStack = request.createItemStack(entity);

			if (costAStack == null || costAStack.isEmpty()) {
				ModLogger.error("Primary cost stack invalid");
				return null;
			}

			ItemStack costBStack = ItemStack.EMPTY;

			if (extra != null) {

				costBStack = extra.createItemStack(entity);

				if (costBStack == null) {
					costBStack = ItemStack.EMPTY;
				}
			}



			ItemStack resultStack;

			// NORMAL OFFER
			if (trade.offer != null) {

				resultStack = trade.offer.createItemStack(entity);

			}

			// MULTIOFFER
			else if (trade.multiOffer != null &&
					!trade.multiOffer.isEmpty()) {

				TradeItem chosen =
						trade.multiOffer.get(
								random.nextInt(trade.multiOffer.size())
						);

				if (chosen == null) {
					ModLogger.error("Chosen multiOffer was null");
					return null;
				}

				resultStack = chosen.createItemStack(entity);

			}

			else {

				ModLogger.error("Trade had no offer");
				return null;
			}

			if (resultStack == null || resultStack.isEmpty()) {

				ModLogger.error("Result stack invalid");
				return null;
			}



			ItemCost costA = new ItemCost(
					costAStack.getItem(),
					Math.max(1, costAStack.getCount())
			);

			var costB = costBStack.isEmpty()
					? java.util.Optional.<ItemCost>empty()
					: java.util.Optional.of(
					new ItemCost(
							costBStack.getItem(),
							Math.max(1, costBStack.getCount())
					)
			);



			int maxUses =
					trade.maxUses == null
							? 1
							: Math.max(1, trade.maxUses);

			int xp =
					trade.tradeExp == null
							? 1
							: Math.max(0, trade.tradeExp);

			float multiplier =
					trade.priceMultiplier == null
							? 0.05f
							: Math.max(0.0f, trade.priceMultiplier);

			int demand =
					trade.demand == null
							? 0
							: Math.max(0, trade.demand);



			System.out.println("Returning trade:");
			System.out.println("Result: " + resultStack);
			System.out.println("CostA: " + costAStack);
			System.out.println("CostB: " + costBStack);



			return new MerchantOffer(
					costA,
					costB,
					resultStack,
					0,
					maxUses,
					xp,
					multiplier,
					demand
			);

		} catch (Exception e) {

			ModLogger.error("Trade generation failed: " + e.getMessage());
			e.printStackTrace();

			return null;
		}
	}
}