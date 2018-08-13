/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.order;

import java.util.concurrent.ConcurrentSkipListMap;

import cointoss.util.Num;

/**
 * @version 2018/08/13 21:34:09
 */
public class OrderBook {

    private final ConcurrentSkipListMap<Num, Num> orders = new ConcurrentSkipListMap();
}
