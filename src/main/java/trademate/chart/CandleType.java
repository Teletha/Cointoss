/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.chart;

import java.util.function.Function;

import javafx.scene.paint.Color;

import cointoss.ticker.Indicator;
import cointoss.ticker.Indicators;
import cointoss.ticker.Tick;
import cointoss.ticker.Ticker;
import cointoss.util.arithmetic.Num;
import kiss.Variable;
import trademate.Theme;
import viewtify.util.FXUtils;

public enum CandleType {

    /** Coordinate by price. */
    Price(CandleType::price, Indicators::ohclCandle),

    /** Coordinate by volume. */
    Volume(CandleType::volume, Indicators::ohclCandle),

    /** Coordinate by volume. */
    PriceVolume(CandleType::priceVolume, Indicators::ohclCandle),

    /** Coordinate by volume. */
    PriceVolumeWeight(CandleType::priceVolumeWeight, Indicators::ohclCandle),

    HeikinAshi(CandleType::price, Indicators::ohclHeikinAshi),

    HeikinAshiVolume(CandleType::volume, Indicators::ohclHeikinAshi);

    /** The candle color. */
    private static final Variable<Color> BuyT = Theme.$.buy.observing().map(color -> color.deriveColor(0, 1, 1, 0.4)).to();

    /** The candle color. */
    private static final Variable<Color> SellT = Theme.$.sell.observing().map(color -> color.deriveColor(0, 1, 1, 0.4)).to();

    /** The candle color. */
    private static final Color Same = FXUtils.color(ChartStyles.same);

    /** The color coordinator. */
    final Function<Tick, Color> coordinator;

    final Function<Ticker, Indicator<double[]>> candles;

    /**
     * @param coordinator
     */
    private CandleType(Function<Tick, Color> coordinator, Function<Ticker, Indicator<double[]>> candles) {
        this.coordinator = coordinator;
        this.candles = candles;
    }

    private static Color price(Tick tick) {
        Num open = tick.openPrice;
        Num close = tick.closePrice();

        if (open.isLessThan(close)) {
            return Theme.$.buy.v;
        } else if (open.isGreaterThan(close)) {
            return Theme.$.sell.v;
        } else {
            return CandleType.Same;
        }
    }

    private static Color volume(Tick tick) {
        double buy = tick.longVolume();
        double sell = tick.shortVolume();

        if (buy > sell) {
            return Theme.$.buy.v;
        } else if (buy < sell) {
            return Theme.$.sell.v;
        } else {
            return CandleType.Same;
        }
    }

    private static Color priceVolume(Tick tick) {
        Num open = tick.openPrice;
        Num close = tick.closePrice();
        double buy = tick.longVolume();
        double sell = tick.shortVolume();

        if (open.isLessThan(close)) {
            if (buy >= sell) {
                return Theme.$.buy.v;
            } else {
                return CandleType.BuyT.v;
            }
        } else if (open.isGreaterThan(close)) {
            if (buy >= sell) {
                return CandleType.SellT.v;
            } else {
                return Theme.$.sell.v;
            }
        } else {
            if (buy >= sell) {
                return Theme.$.buy.v;
            } else {
                return Theme.$.sell.v;
            }
        }
    }

    private static Color priceVolumeWeight(Tick tick) {
        Num open = tick.openPrice;
        Num close = tick.closePrice();
        double buy = tick.longVolume();
        double sell = tick.shortVolume();

        if (open.isLessThan(close)) {
            return Theme.$.buy.v.deriveColor(0, buy / sell, 1, 1);
        } else if (open.isGreaterThan(close)) {
            return Theme.$.sell.v.deriveColor(0, sell / buy, 1, 1);
        } else {
            return CandleType.Same;
        }
    }
}