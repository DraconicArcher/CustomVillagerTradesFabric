package uk.co.dotcode.customvillagertrades.trades;


public class WandererTradeCollection {

	public transient String source = "Local Config file";

	public String profession;
	public boolean removeOtherTrades = false;
	public int maxCommonTrades = 5;
	public int maxRareTrades = 1;
	public MyWandererTrade[] trades;

	public int numberOfGenericTrades() {
		int count = 0;

		for (MyWandererTrade t : trades) {
			if (!t.isRare) {
				count++;
			}
		}
		return count;
	}

	public int numberOfRareTrades() {
		int count = 0;

		for (MyWandererTrade t : trades) {
			if (t.isRare) {
				count++;
			}
		}
		return count;
	}

	public WandererTradeCollection build() {
		return this;
	}

	public boolean validate() {

		if (trades == null || trades.length == 0) return false;

		for (int i = 0; i < trades.length; i++) {
			if (trades[i] == null) return false;

			if (!trades[i].validate(profession, i)) {
				return false;
			}
		}

		return true;
	}
}
