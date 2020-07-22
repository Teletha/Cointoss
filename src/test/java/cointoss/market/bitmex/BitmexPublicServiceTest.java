/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.market.bitmex;

import java.util.List;

import org.junit.jupiter.api.Test;

import cointoss.Direction;
import cointoss.execution.Execution;
import cointoss.market.MarketServiceTestBase;
import cointoss.market.PublicServiceTemplate;
import cointoss.util.Chrono;

class BitmexPublicServiceTest extends MarketServiceTestBase<BitMexService> implements PublicServiceTemplate {

    /**
     * {@inheritDoc}
     */
    @Override
    protected BitMexService constructMarketService() {
        return construct(BitMexService::new, 88, BitMex.XBT_USD.marketName, BitMex.XBT_USD.setting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executions() {
        httpClient.onGet().doReturn("""
                [
                  {
                    "timestamp": "2015-09-30T08:57:15.828Z",
                    "symbol": "XBTUSD",
                    "side": "Sell",
                    "size": 10,
                    "price": 235.74,
                    "tickDirection": "MinusTick",
                    "trdMatchID": "85b088b4-80c5-95ca-7be8-f34cae7ef2b1",
                    "grossValue": 2357400,
                    "homeNotional": 0.023574,
                    "foreignNotional": 5.55733476
                  },
                  {
                    "timestamp": "2015-09-30T08:57:36.358Z",
                    "symbol": "XBTUSD",
                    "side": "Buy",
                    "size": 9,
                    "price": 235.9,
                    "tickDirection": "PlusTick",
                    "trdMatchID": "f157ae33-5c9d-1a28-e76c-d12f685d5843",
                    "grossValue": 2123100,
                    "homeNotional": 0.021231,
                    "foreignNotional": 5.0083929
                  }
                ]
                """);

        List<Execution> list = service.executions(1, 10).toList();
        Execution e = list.get(0);
        assert e.id == 144360343582800000L;
        assert e.direction == Direction.SELL;
        assert e.price.is(235.74);
        assert e.size.is(0.023574);
        assert e.date.isEqual(Chrono.utc(2015, 9, 30, 8, 57, 15, 828));
        assert e.consecutive == Execution.ConsecutiveDifference;

        e = list.get(1);
        assert e.id == 144360345635800000L;
        assert e.direction == Direction.BUY;
        assert e.price.is(235.9);
        assert e.size.is(0.021231);
        assert e.date.isEqual(Chrono.utc(2015, 9, 30, 8, 57, 36, 358));
        assert e.consecutive == Execution.ConsecutiveDifference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executionLatest() {
        PublicServiceTemplate.super.executionLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executionRealtimely() {
        PublicServiceTemplate.super.executionRealtimely();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executionRealtimelyConsecutiveBuy() {
        PublicServiceTemplate.super.executionRealtimelyConsecutiveBuy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executionRealtimelyConsecutiveSell() {
        PublicServiceTemplate.super.executionRealtimelyConsecutiveSell();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void executionRealtimelyWithMultipleChannels() {
        PublicServiceTemplate.super.executionRealtimelyWithMultipleChannels();
    }
}
