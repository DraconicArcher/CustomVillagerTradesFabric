package uk.co.dotcode.customvillagertrades.trades;

public record VillagerTrade(

        TradeItem offer,
        TradeItem request,

        int tradeExp,
        int maxUses,
        float priceMultiplier,
        int demand,
        int tradeLevel,

        String UTID

) {}