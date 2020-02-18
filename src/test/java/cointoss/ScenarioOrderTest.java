/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import antibug.powerassert.PowerAssertOff;
import cointoss.trade.extension.ScenePart;
import cointoss.trade.extension.TradeTest;

@PowerAssertOff
class ScenarioOrderTest extends TraderTestSupport {

    @TradeTest
    void entries(ScenePart scene) {
        Scenario s = scenario(scene);

        switch (scene) {
        default:
            assert s.entries.size() == 1;
            break;
        }
    }

    @TradeTest
    void exits(ScenePart scene) {
        Scenario s = scenario(scene);

        switch (scene) {
        case Entry:
        case EntryCanceled:
            assert s.exits.size() == 0;
            break;

        case EntrySeparately:
            assert s.exits.size() == 2;
            break;

        default:
            assert s.exits.size() == 1;
            break;
        }
    }
}
