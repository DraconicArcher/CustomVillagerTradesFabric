package uk.co.dotcode.customvillagertrades.trades;

import net.minecraft.core.RegistryAccess;
import uk.co.dotcode.customvillagertrades.ModLogger;

import java.util.ArrayList;
import java.util.List;

public class TradeCollection {

	public String profession = "unknown";
	public String source = "unknown";

	public List<MyTrade> trades = new ArrayList<>();

	public boolean removeOtherTrades = false;

	public int maxTrades = 2;

	public transient boolean shouldUpdateFile = false;


	/**
	 * Validates this trade collection using the server registry.
	 */
	public boolean validate(RegistryAccess access) {
		return validateInternal(access);
	}


	private boolean validateInternal(RegistryAccess access) {

		boolean valid = true;


		if (profession == null || profession.isEmpty()) {

			ModLogger.warn(
					"TradeCollection has missing profession (source="
							+ source + ")"
			);

			valid = false;
		}


		if (trades == null || trades.isEmpty()) {

			ModLogger.warn(
					"TradeCollection has no trades (profession="
							+ profession + ")"
			);

			return false;
		}


		for (int i = 0; i < trades.size(); i++) {

			MyTrade trade = trades.get(i);

			if (trade == null) {

				ModLogger.warn(
						"Null trade entry at index "
								+ i
								+ " (profession="
								+ profession
								+ ")"
				);

				valid = false;
				continue;
			}


			/*
			 * Default missing trade levels to level 1.
			 */
			if (trade.tradeLevel == null) {

				ModLogger.warn(
						"Missing tradeLevel → defaulting to 1 (profession="
								+ profession
								+ ", entry="
								+ i
								+ ")"
				);

				trade.tradeLevel = 1;
				shouldUpdateFile = true;
			}


			/*
			 * Resolve configured items against the Minecraft registry.
			 */
			if (access != null) {

				try {

					if (trade.offer != null) {
						trade.offer.resolve(access);
					}

					if (trade.request != null) {
						trade.request.resolve(access);
					}

					if (trade.additionalRequest != null) {
						trade.additionalRequest.resolve(access);
					}

					if (trade.multiOffer != null) {

						trade.multiOffer.forEach(item -> {

							if (item != null) {
								item.resolve(access);
							}
						});
					}

					if (trade.multiRequest != null) {

						trade.multiRequest.forEach(item -> {

							if (item != null) {
								item.resolve(access);
							}
						});
					}

					if (trade.additionalMultiRequest != null) {

						trade.additionalMultiRequest.forEach(item -> {

							if (item != null) {
								item.resolve(access);
							}
						});
					}

				} catch (Exception e) {

					ModLogger.error(
							"Failed to resolve trade (profession="
									+ profession
									+ ", entry="
									+ i
									+ "): "
									+ e.getMessage()
					);

					valid = false;
				}
			}


			/*
			 * Validate the actual trade configuration.
			 */
			if (!trade.validate(profession, i)) {
				valid = false;
			}
		}

		return valid;
	}
}
