/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.market.bitmex;

import cointoss.Currency;
import cointoss.MarketService;
import cointoss.MarketSetting;
import cointoss.market.Exchange;
import cointoss.market.MarketAccount;
import cointoss.market.MarketServiceProvider;
import kiss.I;

public final class BitMex extends MarketServiceProvider {

    /** Limitation */
    private static final int AcquirableSize = 1000;

    public static final MarketService XBT_USD = new BitMexService(88, "XBTUSD", MarketSetting.with.derivative()
            .target(Currency.BTC.minimumSize(0.00001))
            .base(Currency.USD.minimumSize(0.5))
            .acquirableExecutionSize(AcquirableSize));

    public static final MarketService ETH_USD = new BitMexService(297, "ETHUSD", MarketSetting.with.derivative()
            .target(Currency.ETH.minimumSize(0.00001))
            .base(Currency.USD.minimumSize(0.05))
            .priceRangeModifier(20)
            .acquirableExecutionSize(AcquirableSize));

    public static final MarketService XRP_USD = new BitMexService(377, "XRPUSD", MarketSetting.with.derivative()
            .target(Currency.XRP.minimumSize(0.00001))
            .base(Currency.USD.minimumSize(0.0001))
            .acquirableExecutionSize(AcquirableSize));

    /**
     * {@inheritDoc}
     */
    @Override
    public Exchange exchange() {
        return Exchange.BitMEX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MarketAccount account() {
        return I.make(BitMexAccount.class);
    }
}