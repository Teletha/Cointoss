/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.market.bitfinex;

import cointoss.Currency;
import cointoss.MarketService;
import cointoss.MarketSetting;
import cointoss.market.Exchange;
import cointoss.market.MarketAccount;
import cointoss.market.MarketServiceProvider;
import kiss.I;

public final class Bitfinex extends MarketServiceProvider {

    public static final MarketService BTC_USD = new BitfinexService("BTCUSD", MarketSetting.with.spot()
            .target(Currency.BTC.minimumSize(0.0001))
            .base(Currency.USD.minimumSize(1)));

    public static final MarketService COMP_USD = new BitfinexService("COMP:USD", MarketSetting.with.spot()
            .target(Currency.COMP.minimumSize(0.0001))
            .base(Currency.USD.minimumSize(0.01)));

    public static final MarketService ETH_USD = new BitfinexService("ETHUSD", MarketSetting.with.spot()
            .target(Currency.ETH.minimumSize(0.0001))
            .base(Currency.USD.minimumSize(0.01).scale(8))
            .priceRangeModifier(100));

    public static final MarketService XRP_USD = new BitfinexService("XRPUSD", MarketSetting.with.spot()
            .target(Currency.XRP.minimumSize(0.0001))
            .base(Currency.USD.minimumSize(0.00001)));

    public static final MarketService BTC_PERP = new BitfinexService("BTCF0:USTF0", MarketSetting.with.derivative()
            .target(Currency.BTC.minimumSize(0.0001))
            .base(Currency.USDT.minimumSize(1)));

    public static final MarketService ETH_PERP = new BitfinexService("ETHF0:USTF0", MarketSetting.with.derivative()
            .target(Currency.ETH.minimumSize(0.0001))
            .base(Currency.USDT.minimumSize(0.01))
            .priceRangeModifier(100));

    /**
     * {@inheritDoc}
     */
    @Override
    public Exchange exchange() {
        return Exchange.Bitfinex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MarketAccount account() {
        return I.make(BitfinexAccount.class);
    }
}