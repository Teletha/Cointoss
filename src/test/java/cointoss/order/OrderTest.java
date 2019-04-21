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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import cointoss.Direction;

/**
 * @version 2018/07/08 11:44:58
 */
public class OrderTest {

    @Test
    void isCanceled() {
        Order order = Order.buy(1).price(10);
        assert order.isNotCanceled();

        order.state.set(OrderState.CANCELED);
        assert order.isCanceled();
    }

    @Test
    void isCompleted() {
        Order order = Order.buy(1).price(10);
        assert order.isNotCompleted();

        order.state.set(OrderState.COMPLETED);
        assert order.isCompleted();
    }

    @Test
    void isExpired() {
        Order order = Order.buy(1).price(10);
        assert order.isNotExpired();

        order.state.set(OrderState.EXPIRED);
        assert order.isExpired();
    }

    @Test
    void attribute() {
        Order order = Order.buy(1).price(10);
        Attribute attribute = order.attribute(Attribute.class);
        assert attribute != null;
        assert attribute.id == null;
    }

    @Test
    void attributeNull() {
        Order order = Order.buy(1).price(10);

        assertThrows(NullPointerException.class, () -> order.attribute(null));
    }

    private static class Attribute {
        String id;
    }

    @Test
    void limit() {
        Order order = Order.of(Direction.BUY, 1).price(20);
        assert order.side == Direction.BUY;
        assert order.size.is(1);
        assert order.price.is(20);
    }

    @Test
    void market() {
        Order order = Order.of(Direction.BUY, 1);
        assert order.side == Direction.BUY;
        assert order.size.is(1);
        assert order.price.is(0);
    }

    @Test
    void observeTerminatingByCompleted() {
        Order order = Order.of(Direction.BUY, 1);
        List<Order> result = order.observeTerminating().toList();
        assert result.isEmpty();
        order.state.set(OrderState.ACTIVE);
        assert result.isEmpty();
        order.state.set(OrderState.EXPIRED);
        assert result.isEmpty();
        order.state.set(OrderState.INIT);
        assert result.isEmpty();
        order.state.set(OrderState.REJECTED);
        assert result.isEmpty();
        order.state.set(OrderState.REQUESTING);
        assert result.isEmpty();

        order.state.set(OrderState.COMPLETED);
        assert result.size() == 1;
        order.state.set(OrderState.CANCELED);
        assert result.size() == 1;
        order.state.set(OrderState.COMPLETED);
        assert result.size() == 1;
    }

    @Test
    void observeTerminatingByCanceld() {
        Order order = Order.of(Direction.BUY, 1);
        List<Order> result = order.observeTerminating().toList();
        assert result.isEmpty();
        order.state.set(OrderState.ACTIVE);
        assert result.isEmpty();
        order.state.set(OrderState.EXPIRED);
        assert result.isEmpty();
        order.state.set(OrderState.INIT);
        assert result.isEmpty();
        order.state.set(OrderState.REJECTED);
        assert result.isEmpty();
        order.state.set(OrderState.REQUESTING);
        assert result.isEmpty();

        order.state.set(OrderState.CANCELED);
        assert result.size() == 1;
        order.state.set(OrderState.CANCELED);
        assert result.size() == 1;
        order.state.set(OrderState.COMPLETED);
        assert result.size() == 1;
    }

    @Test
    void condition() {
        assert Order.buy(1).condition == QuantityCondition.GoodTillCanceled;
        assert Order.buy(1).type(QuantityCondition.FillOrKill).condition == QuantityCondition.FillOrKill;
        assert Order.buy(1).type(null).condition == QuantityCondition.GoodTillCanceled;
    }
}
