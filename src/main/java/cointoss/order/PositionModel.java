/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.order;

import java.time.ZonedDateTime;

import org.apache.logging.log4j.util.PerformanceSensitive;

import cointoss.Direction;
import cointoss.Directional;
import cointoss.util.Num;
import icy.manipulator.Icy;

@Icy
abstract class PositionModel implements Directional {

    @Icy.Property
    @Override
    public abstract Direction direction();

    @Icy.Property
    public abstract Num price();

    @Icy.Property(mutable = true)
    public abstract Num size();

    @Icy.Property
    public abstract ZonedDateTime date();

    /**
     * Calculate total profit or loss on the current price.
     * 
     * @param currentPrice A current price.
     * @return A total profit or loss of this entry.
     */
    @PerformanceSensitive
    public final Num profit(Num currentPrice) {
        Num profit = isBuy() ? currentPrice.minus(price()) : price().minus(currentPrice);

        return profit.multiply(size()).scale(0);
    }
}
