/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.market;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import cointoss.market.bitfinex.Bitfinex;
import cointoss.market.bitmex.BitMex;
import cointoss.market.bybit.Bybit;
import cointoss.market.coinbase.Coinbase;

@Disabled
class SearchInitialExecutionTest {

    @Test
    @Timeout(value = 10)
    void bitmex() {
        assert BitMex.ETH_USD.searchInitialExecution().waitForTerminate().to().exact().id == 153320077068700000L;
    }

    @Test
    @Timeout(value = 10)
    void coinbase() {
        assert Coinbase.ETHUSD.searchInitialExecution().waitForTerminate().to().exact().id == 1;
    }

    @Test
    @Timeout(value = 10)
    void bybit() {
        assert Bybit.BTC_USD.searchInitialExecution().waitForTerminate().to().exact().id == 15698880007190000L;
    }

    @Test
    @Timeout(value = 10)
    void bitfinex() {
        assert Bitfinex.BTC_USD.searchInitialExecution().waitForTerminate().to().exact().id == 13581820430000000L;
    }
}