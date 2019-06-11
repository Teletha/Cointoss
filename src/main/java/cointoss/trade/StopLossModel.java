/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.trade;

import java.util.function.BiConsumer;

import cointoss.order.Order;
import cointoss.order.OrderManager;
import icy.manipulator.Icy;
import kiss.Signal;

@Icy
public abstract class StopLossModel {

    @Icy.Property
    public abstract Signal<?> when();

    @Icy.Property
    public abstract BiConsumer<OrderManager, Order> how();
}
