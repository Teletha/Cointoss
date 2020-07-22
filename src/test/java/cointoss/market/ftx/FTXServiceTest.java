/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.market.ftx;

import org.junit.jupiter.api.Test;

import cointoss.market.MarketServiceTestTemplate;

class FTXServiceTest extends MarketServiceTestTemplate<FTXService> {

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected FTXService constructMarketService() {
        return construct(FTXService::new, FTX.BTC_USD.marketName, FTX.BTC_USD.setting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderActive() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderActiveEmpty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderCanceled() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderCanceledEmpty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderCompleted() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orderCompletedEmpty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void orders() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void ordersEmpty() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executions() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executionLatest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executionRealtimely() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executionRealtimelyConsecutiveBuy() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executionRealtimelyConsecutiveSell() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    protected void executionRealtimelyWithMultipleChannels() {
    }
}
