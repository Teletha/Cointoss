/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.volume;

import org.junit.jupiter.api.Test;

import cointoss.Direction;
import cointoss.MarketService;
import cointoss.verify.VerifiableMarketService;

class GlobalVolumeTest {

    final MarketService service1 = new VerifiableMarketService();

    final MarketService service2 = new VerifiableMarketService();

    @Test
    void add() {
        GlobalVolume volume = new GlobalVolume();
        volume.add(service1, Direction.BUY, 1);
        assert volume.longVolumeAt(service1) == 1;

        volume.add(service1, Direction.BUY, 2);
        assert volume.longVolumeAt(service1) == 3;

        volume.add(service1, Direction.BUY, 3);
        assert volume.longVolumeAt(service1) == 6;
    }

    @Test
    void longVolumeAt() {
        GlobalVolume volume = new GlobalVolume();
        volume.add(service1, Direction.BUY, 1);
        assert volume.longVolumeAt(service1) == 1;

        volume.add(service1, Direction.BUY, 2);
        assert volume.longVolumeAt(service1) == 3;

        // different side
        volume.add(service1, Direction.SELL, 3);
        assert volume.shortVolumeAt(service1) == 3;

        // different market
        volume.add(service2, Direction.BUY, 3);
        assert volume.shortVolumeAt(service1) == 3;
    }

    @Test
    void shortVolumeAt() {
        GlobalVolume volume = new GlobalVolume();
        volume.add(service1, Direction.SELL, 1);
        assert volume.shortVolumeAt(service1) == 1;

        volume.add(service1, Direction.SELL, 2);
        assert volume.shortVolumeAt(service1) == 3;

        // different side
        volume.add(service1, Direction.BUY, 3);
        assert volume.shortVolumeAt(service1) == 3;

        // different market
        volume.add(service2, Direction.SELL, 3);
        assert volume.shortVolumeAt(service1) == 3;
    }

    @Test
    void longVolume() {
        GlobalVolume volume = new GlobalVolume();
        volume.add(service1, Direction.BUY, 1);
        assert volume.longVolume() == 1;

        volume.add(service2, Direction.BUY, 2);
        assert volume.longVolume() == 3;

        // different side
        volume.add(service1, Direction.SELL, 3);
        assert volume.longVolume() == 3;
    }

    @Test
    void shortVolume() {
        GlobalVolume volume = new GlobalVolume();
        volume.add(service1, Direction.SELL, 1);
        assert volume.shortVolumeAt(service1) == 1;

        volume.add(service1, Direction.SELL, 2);
        assert volume.shortVolumeAt(service1) == 3;

        volume.add(service1, Direction.BUY, 3);
        assert volume.shortVolumeAt(service1) == 3;
    }
}
