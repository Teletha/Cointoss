/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.ticker.data;

import java.time.ZonedDateTime;

import cointoss.Direction;
import cointoss.Directional;
import cointoss.util.arithmetic.Num;
import cointoss.util.feather.Timelinable;
import icy.manipulator.Icy;

@Icy
interface LiquidationModel extends Timelinable, Directional {

    /**
     * {@inheritDoc}
     */
    @Override
    @Icy.Property
    ZonedDateTime date();

    @Override
    @Icy.Property
    Direction direction();

    @Icy.Property
    double size();

    @Icy.Property
    Num price();
}