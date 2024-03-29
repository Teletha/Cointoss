/*
 * Copyright (C) 2024 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package hypatia;

public interface Orientational {

    Orientational POSITIVE = () -> true;

    Orientational NEGATIVE = () -> false;

    boolean isPositive();

    default boolean isNegative() {
        return !isPositive();
    }

    default Orientational reverse() {
        boolean positive = !isPositive();
        return () -> positive;
    }
}
