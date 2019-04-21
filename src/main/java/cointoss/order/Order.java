/*
 * Copyright (C) 2019 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.order;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cointoss.Direction;
import cointoss.Directional;
import cointoss.execution.Execution;
import cointoss.util.Chrono;
import cointoss.util.Num;
import kiss.I;
import kiss.Signal;
import kiss.Variable;

/**
 * @version 2018/07/09 10:33:17
 */
public class Order implements Directional {

    /** The updater. */
    private static final MethodHandle typeHandler;

    /** The updater. */
    private static final MethodHandle priceHandler;

    /** The updater. */
    private static final MethodHandle conditionHandler;

    static {
        typeHandler = find("type");
        priceHandler = find("price");
        conditionHandler = find("condition");
    }

    /**
     * Find field updater.
     * 
     * @param name A field name.
     * @return An updater.
     */
    private static MethodHandle find(String name) {
        try {
            Field field = Order.class.getField(name);
            field.setAccessible(true);

            return MethodHandles.lookup().unreflectSetter(field);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /** The order identifier for the specific market. */
    public final Variable<String> id = Variable.empty();

    /** The order type */
    public final OrderType type = OrderType.MARKET;

    /** The order state */
    public final Variable<OrderState> state = Variable.of(OrderState.INIT);

    /** The ordered position. */
    public final Direction side;

    /** The ordered price. */
    public final Num price = Num.ZERO;

    /** The order created date-time */
    public final Variable<ZonedDateTime> created = Variable.of(Chrono.utcNow());

    /** The ordered size. */
    public final Num size;

    /** The quantity conditions enforcement. */
    public final QuantityCondition condition = QuantityCondition.GoodTillCanceled;

    /** The executed size */
    public Num executedSize = Num.ZERO;

    /** The remaining size */
    public Num remainingSize;

    /** The attribute holder. */
    private final Map<Class, Object> attributes = new ConcurrentHashMap();

    /** The execution event. */
    public final Signal<Execution> executed = attribute(RecordedExecutions.class).additions.expose;

    /**
     * Hide constructor.
     * 
     * @param side A order direction.
     * @param size A order size.
     * @param price A order price.
     */
    protected Order(Direction side, Num size) {
        this.side = Objects.requireNonNull(side);
        this.size = this.remainingSize = Objects.requireNonNull(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Direction direction() {
        return side;
    }

    /**
     * Set your order price.
     * 
     * @param price A price to set.
     * @return Chainable API.
     */
    public final Order price(long price) {
        return price(Num.of(price));
    }

    /**
     * Set your order price.
     * 
     * @param price A price to set.
     * @return Chainable API.
     */
    public final Order price(double price) {
        return price(Num.of(price));
    }

    /**
     * Set your order price.
     * 
     * @param price A price to set.
     * @return Chainable API.
     */
    public final Order price(Num price) {
        if (price == null || price.isNegative()) {
            price = Num.ZERO;
        }

        try {
            priceHandler.invokeExact(this, price);

            if (state.is(OrderState.INIT)) {
                typeHandler.invoke(this, price.isZero() ? OrderType.MARKET : OrderType.LIMIT);
            }
        } catch (Throwable e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * For method reference.
     * 
     * @return
     */
    public final Variable<OrderState> state() {
        return state;
    }

    /**
     * Retrieve the {@link QuantityCondition} of this {@link Order}.
     * 
     * @return A {@link QuantityCondition}.
     */
    public final QuantityCondition quantityCondition() {
        return condition;
    }

    /**
     * Set the {@link QuantityCondition} of this {@link Order}.
     * 
     * @param quantityCondition A {@link QuantityCondition} to set.
     * @return Chainable API.
     */
    public final Order type(QuantityCondition quantityCondition) {
        if (quantityCondition == null) {
            quantityCondition = QuantityCondition.GoodTillCanceled;
        }

        try {
            conditionHandler.invoke(this, quantityCondition == null ? QuantityCondition.GoodTillCanceled : quantityCondition);
        } catch (Throwable e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * Observe when this {@link Order} will be canceled or completed.
     * 
     * @return A event {@link Signal}.
     */
    public final Signal<Order> observeTerminating() {
        return state.observe().take(OrderState.CANCELED, OrderState.COMPLETED).take(1).mapTo(this);
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isExpired() {
        return state.is(OrderState.EXPIRED);
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isNotExpired() {
        return isExpired() == false;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isCanceled() {
        return state.is(OrderState.CANCELED);
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isNotCanceled() {
        return isCanceled() == false;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isCompleted() {
        return state.is(OrderState.COMPLETED);
    }

    /**
     * Check the order {@link OrderState}.
     *
     * @return The result.
     */
    public final boolean isNotCompleted() {
        return isCompleted() == false;
    }

    /**
     * Retrieve the attribute by the specified type.
     * 
     * @param type A attribute type.
     */
    public final <T> T attribute(Class<T> type) {
        return (T) attributes.computeIfAbsent(type, key -> I.make(type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Order ? Objects.equals(id, ((Order) obj).id) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return direction().mark() + size + "@" + price + " 残" + remainingSize + " 済" + executedSize + " " + created;
    }

    /**
     * Build the new buying order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order buy(long size) {
        return buy(Num.of(size));
    }

    /**
     * Build the new buying order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order buy(double size) {
        return buy(Num.of(size));
    }

    /**
     * Build the new buying order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order buy(Num size) {
        return of(Direction.BUY, size);
    }

    /**
     * Build the new selling order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order sell(long size) {
        return sell(Num.of(size));
    }

    /**
     * Build the new selling order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order sell(double size) {
        return sell(Num.of(size));
    }

    /**
     * Build the new selling order.
     * 
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order sell(Num size) {
        return of(Direction.SELL, size);
    }

    /**
     * Build the new order.
     * 
     * @param direction A order direction.
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order of(Direction direction, long size) {
        return of(direction, Num.of(size));
    }

    /**
     * Build the new order.
     * 
     * @param direction A order direction.
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order of(Direction direction, double size) {
        return of(direction, Num.of(size));
    }

    /**
     * Build the new order.
     * 
     * @param direction A order direction.
     * @param size Your order size.
     * @return A new {@link Order}.
     */
    public static Order of(Direction direction, Num size) {
        return new Order(direction, size);
    }
}
