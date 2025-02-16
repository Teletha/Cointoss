/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.verify;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import cointoss.Currency;
import cointoss.Direction;
import cointoss.MarketService;
import cointoss.MarketSetting;
import cointoss.execution.Execution;
import cointoss.execution.LogHouse;
import cointoss.market.Exchange;
import cointoss.order.Order;
import cointoss.order.OrderManager;
import cointoss.order.OrderState;
import cointoss.order.OrderType;
import cointoss.order.QuantityCondition;
import cointoss.orderbook.OrderBookChanges;
import cointoss.util.Chrono;
import cointoss.util.EfficientWebSocket;
import cointoss.util.RetryPolicy;
import hypatia.Num;
import hypatia.Orientational;
import kiss.I;
import kiss.Signal;
import kiss.Signaling;

public class VerifiableMarketService extends MarketService {

    /** No OP */
    private static final Consumer<Execution> noop = e -> {
    };

    /** The managed id. */
    private int id = 0;

    /** The order manager. */
    private final List<BackendOrder> orderActive = new CopyOnWriteArrayList();

    /** The order manager. */
    final Signaling<Order> orderUpdateRealtimely = new Signaling();

    /** The initial base currency. */
    public Num baseCurrency = Num.HUNDRED;

    /** The initial target currency. */
    public Num targetCurrency = Num.ZERO;

    /** The latest price. */
    private Num latestPrice;

    /** The latest execution time. */
    private ZonedDateTime now = Chrono.MIN;

    /** The latest execution epoch mills. */
    private long nowMills = 0;

    /** The testable scheduler. */
    private final SchedulerEmulator scheduler = new SchedulerEmulator();

    /** The task queue. */
    private final PriorityQueue<Task> tasks = new PriorityQueue();

    /** The external repository. */
    public LogHouse external;

    /** The emulation for lag. */
    public Latency latency = Latency.zero();

    /**
     * 
     */
    public VerifiableMarketService() {
        this("TestableMarket");
    }

    /**
     * 
     */
    public VerifiableMarketService(String marketName) {
        this(marketName, MarketSetting.with.derivative()
                .target(Currency.UNKNOWN.minimumSize(0.0001))
                .base(Currency.UNKNOWN.minimumSize(0.01)));
    }

    /**
     * 
     */
    public VerifiableMarketService(String marketName, MarketSetting setting) {
        super(Exchange.TestNet, marketName, setting);
    }

    /**
     * @param setting
     */
    public VerifiableMarketService(MarketSetting setting) {
        super(Exchange.TestNet, "TestableMarket", setting);
    }

    /**
     * 
     */
    public VerifiableMarketService(MarketService delegation) {
        super(delegation.exchange, delegation.marketName, delegation.setting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogHouse loghouse() {
        return external == null ? LogHouse.INVALID : external;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EfficientWebSocket clientRealtimely() {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * Clear all data.
     */
    public void clear() {
        id = 0;
        orderActive.clear();
        now = Chrono.MIN;
        nowMills = 0;
        latestPrice = null;
        tasks.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Order> connectOrdersRealtimely() {
        return orderUpdateRealtimely.expose;
    }

    /** The prepared execution store. */
    private final LinkedList<Execution> executionsBeforeOrderResponse = new LinkedList();

    /**
     * Prepare executions which are reveived before order response.
     * 
     * @param e
     */
    public void emulateExecutionBeforeOrderResponse(Execution e) {
        if (e != null) {
            executionsBeforeOrderResponse.add(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> request(Order order) {
        String identifier = "LOCAL-ACCEPTANCE-" + id++;

        return I.signal(order).map(o -> {
            BackendOrder child = new BackendOrder(order);
            child.id = identifier;
            child.state = OrderState.ACTIVE;
            child.createTimeMills = nowMills + latency.lag();
            child.remainingSize = order.size;

            // taker has high priority
            if (order.type.isMaker()) {
                orderActive.add(child); // last
            } else {
                int i = indexFirstMaker();
                if (i == -1) {
                    orderActive.add(child); // last
                } else {
                    orderActive.add(i, child); // before maker
                }
            }

            if (!executionsBeforeOrderResponse.isEmpty()) {
                for (Execution execution : executionsBeforeOrderResponse) {
                    emulate(execution, noop);
                }
                executionsBeforeOrderResponse.clear();
            }

            return child.id;
        }).effectOnComplete(() -> {
            if (order.type.isMaker()) {
                orderUpdateRealtimely.accept(OrderManager.Update.create(identifier, order.orientation, order.size, order.price));
            }
        });
    }

    private int indexFirstMaker() {
        for (int i = 0; i < orderActive.size(); i++) {
            BackendOrder b = orderActive.get(i);
            if (b.type.isMaker()) {
                return i;
            }
        }
        return -1;
    }

    /** The prepared execution store. */
    private final LinkedList<Execution> executionsAfterOrderCancelResponse = new LinkedList();

    /**
     * Prepare executions which are reveived after order cancel response.
     * 
     * @param e
     */
    public void emulateExecutionAfterOrderCancelResponse(Execution e) {
        if (e != null) {
            executionsAfterOrderCancelResponse.add(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> cancel(Order order) {
        BackendOrder backend = findBy(order);

        // associated backend order is not found, do nothing
        if (backend == null || backend.type.isTaker()) {
            return I.signal();
        }

        return cancel(backend, order);
    }

    private Signal<Order> cancel(BackendOrder backend, Order order) {
        // when latency is zero, cancel order immediately
        ZonedDateTime delay = latency.emulate(now);

        if (delay == now) {
            Signal<BackendOrder> response = I.signal(backend);

            if (executionsAfterOrderCancelResponse.isEmpty()) {
                response = response.effect(() -> {
                    backend.cancel();
                });
            } else {
                response = response.effectOnComplete(() -> {
                    executionsAfterOrderCancelResponse.forEach(e -> emulate(e, noop));
                    backend.cancel();
                });
            }
            return response.mapTo(order);
        }

        // backend order will be canceled in the specified delay
        backend.cancelTimeMills = Chrono.epochMills(delay);

        return backend.canceling.expose;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Boolean> cancelAll() {
        Signal<Order> composed = I.signal();

        for (BackendOrder back : orderActive) {
            composed = composed.merge(cancel(back, null));
        }
        return composed.mapTo(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Execution> connectExecutionRealtimely() {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatest() {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionsBefore(long id) {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /** The response store. */
    private LinkedList<List<Execution>> nextExecutions;

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionsAfter(long startId, long endId) {
        if (nextExecutions == null) {
            return I.signal();
        } else {
            return I.signal(nextExecutions.pollFirst());
        }
    }

    /**
     * Mock the next call of {@link #executionsAfter(long, long)}.
     * 
     * @param executions
     */
    public void executionsWillResponse(Execution... executions) {
        if (nextExecutions == null) {
            nextExecutions = new LinkedList();
        }
        nextExecutions.add(List.of(executions));
    }

    private Execution nextNearest;

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> searchNearestExecution(ZonedDateTime target) {
        try {
            return I.signal(nextNearest);
        } finally {
            nextNearest = null;
        }
    }

    /**
     * Mock the next call of
     */
    public void searchNearestIdWillResponse(Execution e) {
        this.nextNearest = e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders() {
        return I.signal(orderActive).map(o -> {
            Order order = Order.with.orientation(o.direction, o.size)
                    .price(o.price)
                    .quantityCondition(o.condition)
                    .executedSize(o.executedSize)
                    .id(o.id)
                    .state(o.state);

            return order;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders(OrderState state) {
        return I.signal(orderActive).take(o -> o.state == state).map(o -> {
            Order order = Order.with.orientation(o.direction, o.size)
                    .price(o.price)
                    .quantityCondition(o.condition)
                    .executedSize(o.executedSize)
                    .id(o.id)
                    .state(o.state);

            return order;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> baseCurrency() {
        return I.signal(baseCurrency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> targetCurrency() {
        return I.signal(targetCurrency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<OrderBookChanges> orderBook() {
        return Signal.never();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<OrderBookChanges> connectOrderBookRealtimely() {
        return Signal.never();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ZonedDateTime now() {
        return now;
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetryPolicy withPolicy(int max, String name) {
        return RetryPolicy.with.unlimit();
    }

    /**
     * Elapse market time.
     * 
     * @param time
     * @param unit
     */
    final void elapse(long time, TimeUnit unit) {
        long seconds = unit.toSeconds(time);

        for (long i = 0; i < seconds; i++) {
            now = now.plusSeconds(1);
            nowMills += 1000;

            while (!tasks.isEmpty() && tasks.peek().activeTime <= nowMills) {
                tasks.poll().run();
            }
        }
    }

    /**
     * Emulate {@link Execution}.
     */
    final void emulate(Execution e, Consumer<Execution> executor) {
        now = e.date;
        nowMills = e.mills;

        // emulate market execution
        Iterator<BackendOrder> iterator = orderActive.iterator();

        while (iterator.hasNext()) {
            BackendOrder order = iterator.next();

            // time base filter
            if (e.mills < order.createTimeMills) {
                continue;
            }

            // check canceling time
            if (order.cancelTimeMills != 0 && order.cancelTimeMills <= e.mills) {
                order.cancel();
                continue;
            }

            // check quantity condition
            if (order.condition == QuantityCondition.FillOrKill && !validateTradable(order, e)) {
                orderActive.remove(order);
                continue;
            }

            if (order.condition == QuantityCondition.ImmediateOrCancel) {
                if (validateTradableByPrice(order, e)) {
                    order.remainingSize = Num.min(e.size, order.remainingSize);
                } else {
                    orderActive.remove(order);
                    continue;
                }
            }

            if (validateTradableByPrice(order, e)) {
                Num executedSize = Num.min(e.size, order.remainingSize);
                if (order.type.isTaker() && executedSize.isNot(0)) {
                    order.marketMinPrice = order.isPositive() ? Num.max(order.marketMinPrice, e.price, latestPrice)
                            : Num.min(order.marketMinPrice, e.price, latestPrice);
                    order.price = order.price.multiply(order.executedSize)
                            .plus(order.marketMinPrice.multiply(executedSize))
                            .divide(executedSize.plus(order.executedSize));
                }
                order.executedSize = order.executedSize.plus(executedSize);
                order.remainingSize = order.remainingSize.minus(executedSize);

                if (order.remainingSize.isZero()) {
                    order.state = OrderState.COMPLETED;
                    orderActive.remove(order);
                }

                Num price = order.type.isTaker() ? order.marketMinPrice : order.price;
                orderUpdateRealtimely.accept(OrderManager.Update.executePartially(order.id, executedSize, price, Num.ZERO));

                while (!tasks.isEmpty() && tasks.peek().activeTime <= nowMills) {
                    tasks.poll().run();
                }

                executor.accept(Execution.with.orientation(e.orientation, executedSize)
                        .price(order.type.isTaker() ? order.marketMinPrice : order.price)
                        .date(e.date)
                        .id(e.id)
                        .consecutive(e.consecutive)
                        .delay(e.delay));

                if (executedSize.isNot(e.size)) {
                    emulate(Execution.with.orientation(e.orientation, e.size.minus(executedSize))
                            .price(e.price)
                            .date(e.date)
                            .id(e.id)
                            .consecutive(e.consecutive)
                            .delay(e.delay), executor);
                }
                return;
            }
        }

        latestPrice = e.price;

        while (!tasks.isEmpty() && tasks.peek().activeTime <= nowMills) {
            tasks.poll().run();
        }

        executor.accept(e);
    }

    /**
     * Test whether this order can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    private boolean validateTradable(BackendOrder order, Execution e) {
        return validateTradableBySize(order, e) && validateTradableByPrice(order, e);
    }

    /**
     * Test whether this order price can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    private boolean validateTradableByPrice(BackendOrder order, Execution e) {
        if (order.type == OrderType.Taker) {
            return true;
        }
        if (order.isPositive()) {
            Num price = order.price;
            return price.isGreaterThan(e.price) || price.is(setting.base.minimumSize);
        } else {
            return order.price.isLessThan(e.price);
        }
    }

    /**
     * Test whether this order size can trade with the specified {@link Execution}.
     * 
     * @param e A target {@link Execution}.
     * @return A result.
     */
    private boolean validateTradableBySize(BackendOrder order, Execution e) {
        return order.size.isLessThanOrEqual(e.size);
    }

    /**
     * Find {@link BackendOrder} by fronend {@link Order}.
     * 
     * @param order
     * @return
     */
    private BackendOrder findBy(Order order) {
        for (BackendOrder back : orderActive) {
            if (back.front.id.equals(order.id) || back.id.equals(order.id)) {
                return back;
            }
        }
        return null;
    }

    /**
     * For test.
     */
    private class BackendOrder implements Orientational<Direction> {

        /** The frontend order. */
        private final Order front;

        /** The order direction. */
        private final Direction direction;

        /** The order size. */
        private final Num size;

        /** The order id. */
        private String id;

        /** The order price. */
        private Num price;

        /** The order type. */
        private OrderType type;

        /** The order type. */
        private QuantityCondition condition;

        /** The order state. */
        private OrderState state;

        /** The order state. */
        private Num remainingSize;

        /** The order state. */
        private Num executedSize;

        /** The minimum price for market order. */
        private Num marketMinPrice;

        /**
         * The time which this order is created, Using epoch mills to make time-related calculation
         * faster.
         */
        private long createTimeMills;

        /**
         * The time which this order will be canceled completely. Using epoch mills to make
         * time-related calculation faster.
         */
        private long cancelTimeMills;

        /** The cancel event emitter. */
        private final Signaling<Order> canceling = new Signaling();

        /**
         * Create backend managed order.
         * 
         * @param o
         */
        private BackendOrder(Order o) {
            this.front = o;
            this.direction = o.orientation;
            this.size = o.size;
            this.executedSize = o.executedSize;
            this.price = o.price;
            this.type = o.type;
            this.condition = o.quantityCondition;
            this.createTimeMills = o.creationTime.toInstant().toEpochMilli();
            this.marketMinPrice = isPositive() ? Num.ZERO : Num.MAX;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Direction orientation() {
            return direction;
        }

        /**
         * Cancel this order actually.
         */
        private void cancel() {
            orderActive.remove(this);

            state = OrderState.CANCELED;
            orderUpdateRealtimely.accept(OrderManager.Update.cancel(id));

            canceling.accept(front);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "BackendOrder [direction=" + direction + ", size=" + size + ", id=" + id + ", price=" + price + ", type=" + type + ", condition=" + condition + ", state=" + state + ", remainingSize=" + remainingSize + ", executedSize=" + executedSize + ", marketMinPrice=" + marketMinPrice + ", createTimeMills=" + createTimeMills + ", cancelTimeMills=" + cancelTimeMills + "]";
        }
    }

    /**
     * 
     */
    private class SchedulerEmulator implements ScheduledExecutorService {

        /**
         * {@inheritDoc}
         */
        @Override
        public void shutdown() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isShutdown() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTerminated() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Future<?> submit(Runnable task) {
            return submit(task, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(Runnable command) {
            schedule(command, 0, TimeUnit.SECONDS);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            Task future = new Task(unit.toMillis(delay), command);

            if (delay <= 0) {
                future.run();
            } else {
                tasks.add(future);
            }
            return future;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            Task future = new Task(unit.toMillis(delay), callable);

            if (delay <= 0) {
                future.run();
            } else {
                tasks.add(future);
            }
            return future;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return null;
        }
    }

    /**
     * 
     */
    private class Task<V> implements ScheduledFuture<V>, Callable<V>, Runnable {

        /** The active time by epoch mills. */
        private final long activeTime;

        private final Callable<V> action;

        private boolean cancelled;

        private boolean done;

        /**
         * @param mills
         * @param action
         */
        private Task(long delay, Runnable action) {
            this(delay, () -> {
                action.run();
                return null;
            });
        }

        /**
         * @param delayMills
         * @param action
         */
        private Task(long delayMills, Callable<V> action) {
            this.activeTime = nowMills + delayMills;
            this.action = action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                call();
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V call() throws Exception {
            return action.call();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getDelay(TimeUnit unit) {
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Delayed o) {
            Task other = (Task) o;

            if (activeTime == other.activeTime) {
                return 0;
            }
            return activeTime < other.activeTime ? -1 : 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return tasks.remove(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDone() {
            return done;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() throws InterruptedException, ExecutionException {
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Task[" + activeTime + "]";
        }
    }
}