/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package trademate.bot;

import cointoss.MarketBackend.Health;
import cointoss.Order;
import cointoss.Side;
import cointoss.Trading;
import cointoss.order.OrderBookList;
import cointoss.util.Num;

/**
 * @version 2018/01/20 22:37:53
 */
public class Spreader extends Trading {

    private boolean whileTrading = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        market.timeline.to(e -> {
            if (whileTrading == false && market.health.is(Health.Normal)) {
                Num volume = market.flow.volume();
                Num spread = market.orderBook.spread();

                if (volume.abs().isGreaterThanOrEqual(3) && spread.isGreaterThanOrEqual(200)) {
                    whileTrading = true;

                    Side entrySide = volume.isPositive() ? Side.BUY : Side.SELL;
                    Side exitSide = entrySide.inverse();
                    Num entrySize = Num.of(0.001);

                    OrderBookList entryBook = market.orderBook.bookFor(entrySide);
                    OrderBookList exitBook = market.orderBook.bookFor(exitSide);

                    market.request(Order.limit(entrySide, entrySize, entryBook.computeBestPrice(Num.ZERO, Num.TWO))).to(entry -> {
                        entry.execute.to(entryExe -> {
                            market.request(Order.limit(exitSide, entryExe.size, exitBook.computeBestPrice(Num.ZERO, Num.TWO))).to(exit -> {
                                exit.execute.to(exitExe -> {
                                    // if (entry.isCompleted() && exit.i) {
                                    //
                                    // }
                                });
                            });
                        });
                    });
                }
            }
        });
    }
}
