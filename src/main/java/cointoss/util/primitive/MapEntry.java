/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util.primitive;

public class MapEntry {
    public static class IntKeyEntry<V> {
        int key;

        V val;

        public IntKeyEntry(int key, V val) {
            this.key = key;
            this.val = val;
        }

        public int getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }
    }

    public static class LongKeyEntry<V> {
        long key;

        V val;

        public LongKeyEntry(long key, V val) {
            this.key = key;
            this.val = val;
        }

        public long getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }
    }

    public static class FloatKeyEntry<V> {
        float key;

        V val;

        public FloatKeyEntry(float key, V val) {
            this.key = key;
            this.val = val;
        }

        public float getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }
    }

    public static class DoubleKeyEntry<V> {
        double key;

        V val;

        public DoubleKeyEntry(double key, V val) {
            this.key = key;
            this.val = val;
        }

        public double getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }
    }
}