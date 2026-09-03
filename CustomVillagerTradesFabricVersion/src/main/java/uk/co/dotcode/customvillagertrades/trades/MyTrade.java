package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import uk.co.dotcode.customvillagertrades.ModLogger;
import uk.co.dotcode.customvillagertrades.TradeUtil;

import java.util.Optional;

public class MyTrade {

	public TradeItem offer;
	public java.util.List<TradeItem> multiOffer;

	public TradeItem request;
	public java.util.List<TradeItem> multiRequest;

	public TradeItem additionalRequest;
	public java.util.List<TradeItem> additionalMultiRequest;

	public Integer tradeExp;
	public Integer maxUses;
	public Float priceMultiplier = 0.05f;
	public Integer demand = 0;
	public Integer tradeLevel;

	// =========================
	// ID
	// =========================

	public String UTID;

	public void assignUTID(String id) {
		this.UTID = id;
	}

	/**
	 * Creates a Minecraft 1.21.1 MerchantOffer from this configuration.
	 */
	public MerchantOffer createTrade(Entity entity) {

		TradeItem offerItem = pickOffer();
		TradeItem requestItem = pickRequest();
		TradeItem extraItem = pickExtraRequest();

		if (offerItem == null || requestItem == null) {
			return null;
		}

		ItemStack requestStack =
				requestItem.createItemStack(entity);

		ItemStack resultStack =
				offerItem.createItemStack(entity);

		ItemStack extraStack =
				extraItem != null
						? extraItem.createItemStack(entity)
						: ItemStack.EMPTY;

		if (requestStack.isEmpty() ||
				resultStack.isEmpty()) {

			return null;
		}

		/*
		 * Minecraft 1.21.1 uses ItemCost instead of the
		 * older Yarn TradedItem class.
		 */
		ItemCost firstBuyItem =
				new ItemCost(
						requestStack.getItem(),
						requestStack.getCount()
				);

		Optional<ItemCost> secondBuyItem =
				extraStack.isEmpty()
						? Optional.empty()
						: Optional.of(
						new ItemCost(
								extraStack.getItem(),
								extraStack.getCount()
						)
				);

		int uses =
				maxUses == null
						? 1
						: Math.max(1, maxUses);

		int xp =
				tradeExp == null
						? 1
						: Math.max(0, tradeExp);

		float multiplier =
				priceMultiplier == null
						? 0.05f
						: priceMultiplier;

		int tradeDemand =
				demand == null
						? 0
						: Math.max(0, demand);

		return new MerchantOffer(
				firstBuyItem,
				secondBuyItem,
				resultStack,
				0,
				uses,
				xp,
				multiplier,
				tradeDemand
		);
	}

	private TradeItem pickOffer() {

		if (multiOffer != null &&
				!multiOffer.isEmpty()) {

			return multiOffer.get(
					TradeUtil.random.nextInt(
							multiOffer.size()
					)
			);
		}

		return offer;
	}

	private TradeItem pickRequest() {

		if (multiRequest != null &&
				!multiRequest.isEmpty()) {

			return multiRequest.get(
					TradeUtil.random.nextInt(
							multiRequest.size()
					)
			);
		}

		return request;
	}

	private TradeItem pickExtraRequest() {

		if (additionalMultiRequest != null &&
				!additionalMultiRequest.isEmpty()) {

			return additionalMultiRequest.get(
					TradeUtil.random.nextInt(
							additionalMultiRequest.size()
					)
			);
		}

		return additionalRequest;
	}

	public boolean validate(
			String profession,
			int index
	) {

		if (pickOffer() == null) {

			ModLogger.warn(
					"Missing offer: "
							+ profession
							+ " entry "
							+ index
			);

			return false;
		}

		if (pickRequest() == null) {

			ModLogger.warn(
					"Missing request: "
							+ profession
							+ " entry "
							+ index
			);

			return false;
		}

		if (tradeLevel == null) {

			ModLogger.warn(
					"Missing tradeLevel: "
							+ profession
							+ " entry "
							+ index
			);

			return false;
		}

		return true;
	}
}
