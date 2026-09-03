package uk.co.dotcode.customvillagertrades.trades;

import java.util.List;

public record ProfessionTrades(

        String profession,
        boolean removeOtherTrades,
        int maxTrades,
        List<VillagerTrade> trades

) {}