/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import kiss.Observer;
import kiss.Signal;

/**
 * @version 2017/08/24 23:09:32
 */
public class Order implements Directional {

    /** The date format. */
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSS");

    /** The ordered position. */
    private Side side;

    /** The ordered size. */
    private Amount size;

    /** The ordered price. */
    private Amount price;

    private Amount triggerPrice;

    private Quantity quantity;

    /** INTERNAL USAGE */
    Order entry;

    /** INTERNAL USAGE */
    Set<Order> exits = new LinkedHashSet();

    /** The description. */
    private String description;

    /**
     * <p>
     * Hide constructor.
     * </p>
     * 
     * @param position
     * @param price
     * @param size
     */
    protected Order(Side position, Amount size, Amount price, Amount priceLimit, Quantity quantity) {
        this.side = Objects.requireNonNull(position);
        this.size = Objects.requireNonNull(size);
        this.price = price == null ? Amount.ZERO : price;
        this.child_order_type = price == null ? OrderType.MARKET : OrderType.LIMIT;

        when(priceLimit);
        type(quantity);
    }

    /**
     * Aliase for {@link #child_order_acceptance_id}.
     * 
     * @return
     */
    public String id() {
        return child_order_acceptance_id;
    }

    /**
     * Get the side property of this {@link Order}.
     * 
     * @return The side property.
     */
    @Override
    public Side side() {
        return side;
    }

    /**
     * Get the side property of this {@link Order}.
     * 
     * @return The side property.
     */
    @SuppressWarnings("unused")
    private Side getSide() {
        return side;
    }

    /**
     * Set the side property of this {@link Order}.
     * 
     * @param side The side value to set.
     */
    @SuppressWarnings("unused")
    private void setSide(Side side) {
        this.side = side;
    }

    /**
     * Get the size property of this {@link Order}.
     * 
     * @return The size property.
     */
    public Amount size() {
        return size;
    }

    /**
     * Get the size property of this {@link Order}.
     * 
     * @return The size property.
     */
    @SuppressWarnings("unused")
    private Amount getSize() {
        return size;
    }

    /**
     * Set the size property of this {@link Order}.
     * 
     * @param size The size value to set.
     */
    @SuppressWarnings("unused")
    private void setSize(Amount size) {
        this.size = size;
    }

    /**
     * Get the price property of this {@link Order}.
     * 
     * @return The price property.
     */
    public Amount price() {
        return price;
    }

    /**
     * Get the price property of this {@link Order}.
     * 
     * @return The price property.
     */
    @SuppressWarnings("unused")
    private Amount getPrice() {
        return price;
    }

    /**
     * Set the price property of this {@link Order}.
     * 
     * @param price The price value to set.
     */
    @SuppressWarnings("unused")
    private void setPrice(Amount price) {
        this.price = price;
    }

    /**
     * Get the triggerPrice property of this {@link Order}.
     * 
     * @return The triggerPrice property.
     */
    public Amount triggerPrice() {
        return triggerPrice;
    }

    /**
     * Get the triggerPrice property of this {@link Order}.
     * 
     * @return The triggerPrice property.
     */
    @SuppressWarnings("unused")
    private Amount getTriggerPrice() {
        return triggerPrice;
    }

    /**
     * Set the triggerPrice property of this {@link Order}.
     * 
     * @param triggerPrice The triggerPrice value to set.
     */
    @SuppressWarnings("unused")
    private void setTriggerPrice(Amount triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    /**
     * Get the quantity property of this {@link Order}.
     * 
     * @return The quantity property.
     */
    public Quantity quantity() {
        return quantity;
    }

    /**
     * Get the quantity property of this {@link Order}.
     * 
     * @return The quantity property.
     */
    @SuppressWarnings("unused")
    private Quantity getQuantity() {
        return quantity;
    }

    /**
     * Set the quantity property of this {@link Order}.
     * 
     * @param quantity The quantity value to set.
     */
    @SuppressWarnings("unused")
    private void setQuantity(Quantity quantity) {
        this.quantity = quantity;
    }

    /**
     * ストップ注文の値段を指定
     * 
     * @param priceLimit
     * @return
     */
    public Order when(int priceLimit) {
        return when(Amount.of(priceLimit));
    }

    /**
     * ストップ注文の値段を指定
     * 
     * @param triggerPrice
     * @return
     */
    public Order when(Amount triggerPrice) {
        if (triggerPrice != null) {
            this.triggerPrice = triggerPrice;
        }
        return this;
    }

    /**
     * Associate with entry order.
     * 
     * @param entry
     * @return
     */
    public Order with(OrderAndExecution entry) {
        return with(entry.o);
    }

    /**
     * Associate with entry order.
     * 
     * @param entry
     * @return
     */
    public Order with(Order entry) {
        if (entry != null) {
            this.entry = entry;
            this.entry.exits.add(this);
        }
        return this;
    }

    /**
     * 執行数量条件を指定
     * 
     * @param quantity
     * @return
     */
    public Order type(Quantity quantity) {
        this.quantity = quantity == null ? Quantity.GoodTillCnaceled : quantity;

        return this;
    }

    /**
     * Read order description.
     * 
     * @return
     */
    public String description() {
        return description;
    }

    /**
     * Write order description.
     * 
     * @param description
     * @return
     */
    public Order description(String description) {
        this.description = description;
        return this;
    }

    /**
     * 成り行き注文
     * 
     * @param position
     * @param size
     * @return
     */
    public static Order market(Side position, Amount size) {
        return new Order(position, size, null, null, null);
    }

    /**
     * 成り行き注文
     * 
     * @param size
     * @return
     */
    public static Order marketLong(int size) {
        return marketLong(Amount.of(size));
    }

    /**
     * 成り行き注文
     * 
     * @param size
     * @return
     */
    public static Order marketLong(Amount size) {
        return market(Side.BUY, size);
    }

    /**
     * 成り行き注文
     * 
     * @param size
     * @return
     */
    public static Order marketShort(int size) {
        return marketShort(Amount.of(size));
    }

    /**
     * 成り行き注文
     * 
     * @param size
     * @return
     */
    public static Order marketShort(Amount size) {
        return market(Side.SELL, size);
    }

    /**
     * 指値注文
     * 
     * @param position
     * @param size
     * @return
     */
    public static Order limit(Side position, Amount size, Amount price) {
        return new Order(position, size, price, null, null);
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitLong(Amount size, Amount price) {
        return limit(Side.BUY, size, price);
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitLong(String size, String price) {
        return limitLong(new Amount(size), new Amount(price));
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitLong(int size, int price) {
        return limitLong(Amount.of(size), Amount.of(price));
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitShort(Amount size, Amount price) {
        return limit(Side.SELL, size, price);
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitShort(String size, String price) {
        return limitShort(new Amount(size), new Amount(price));
    }

    /**
     * 指値注文
     * 
     * @param size
     * @return
     */
    public static Order limitShort(int size, int price) {
        return limitShort(Amount.of(size), Amount.of(price));
    }

    /**
     * @version 2017/07/22 18:02:37
     */
    public static enum Quantity {
        GoodTillCnaceled, ImmediateOrCancel, FillOrKill;
    }

    /**
     * Entry to the specified {@link Market}.
     * 
     * @param market A target market to order.
     */
    public Signal<OrderAndExecution> entryTo(Market market) {
        return new Signal<OrderAndExecution>((observer, disposer) -> {
            market.request(this).to(o -> {
                executionListeners.add(observer);
            });

            return disposer.add(() -> {
                executionListeners.remove(observer);
            });
        });
    }

    /** The server ID */
    public String child_order_acceptance_id;

    /** Order type */
    public OrderType child_order_type;

    /** Order state */
    public OrderState child_order_state;

    /** The order date */
    public LocalDateTime child_order_date;

    /** The expire date */
    public LocalDateTime expire_date;

    /** The remaining size */
    public Amount outstanding_size;

    /** The executed size */
    public Amount executed_size;

    /** The canceled size */
    public Amount cancel_size;

    /** The total commited size */
    public Amount total_commission;

    /** Order avarage price */
    public Amount average_price;

    /** INTERNAL USAGE */
    public List<Execution> executions = new ArrayList(4);

    /** INTERNAL USAGE */
    CopyOnWriteArrayList<Observer<? super OrderAndExecution>> executionListeners = new CopyOnWriteArrayList<>();

    /**
     * <p>
     * Utility.
     * </p>
     * 
     * @return
     */
    public final boolean isExpired() {
        return child_order_state == OrderState.EXPIRED;
    }

    /**
     * <p>
     * Utility.
     * </p>
     * 
     * @return
     */
    public boolean isCanceled() {
        return child_order_state == OrderState.CANCELED;
    }

    /**
     * <p>
     * Utility.
     * </p>
     * 
     * @return
     */
    public final boolean isCompleted() {
        return child_order_state == OrderState.COMPLETED;
    }

    /**
     * <p>
     * Utility.
     * </p>
     * 
     * @return
     */
    public final boolean isAllCompleted() {
        for (Order exit : exits) {
            if (exit.isCompleted() == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether this order can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    public final boolean isTradableWith(Execution e) {
        return isTradableSizeWith(e) && isTradablePriceWith(e);
    }

    /**
     * Test whether this order price can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    public final boolean isTradablePriceWith(Execution e) {
        if (child_order_type == OrderType.MARKET) {
            return true;
        }

        if (isBuy()) {
            return average_price.isEqualOrGreaterThan(e.price);
        } else {
            return average_price.isEqualOrLessThan(e.price);
        }
    }

    /**
     * Test whether this order size can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    public final boolean isTradableSizeWith(Execution e) {
        return size().isEqualOrLessThan(e.size);
    }

    /**
     * <p>
     * Calculate new price with the specified diff.
     * </p>
     * 
     * @param diff
     * @return
     */
    public final Amount calculatePrice(Amount diff) {
        return isBuy() ? average_price.plus(diff) : average_price.minus(diff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return side.mark() + size + "@" + average_price + " 残" + outstanding_size + " 済" + executed_size + " " + format
                .format(child_order_date);
    }
}
