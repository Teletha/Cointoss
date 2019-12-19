/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.google.common.math.DoubleMath;

public class Primitives {

    /** Fix decimal point(2). */
    public static final DecimalFormat DecimalScale2 = new DecimalFormat("#.#");

    static {
        DecimalScale2.setMinimumFractionDigits(2);
        DecimalScale2.setMaximumFractionDigits(2);
    }

    /**
     * Round to the specified decimal place.
     * 
     * @param value
     * @param scale
     * @return
     */
    public static double roundDecimal(double value, int scale) {
        double s = Math.pow(10, scale);
        return Math.round(value * s) / s;
    }

    /**
     * Round to the specified decimal place.
     * 
     * @param value
     * @param scale
     * @return
     */
    public static double roundDecimal(double value, int scale, RoundingMode mode) {
        double s = Math.pow(10, scale);
        return DoubleMath.roundToInt(value * s, mode) / s;
    }

    /**
     * @param min
     * @param value
     * @param max
     */
    public static double within(double min, double value, double max) {
        if (value < min) {
            value = min;
        }

        if (max < value) {
            value = max;
        }
        return value;
    }
}
