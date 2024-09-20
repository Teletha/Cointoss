/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.ticker;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import cointoss.Direction;
import cointoss.Market;
import cointoss.MarketService;
import cointoss.execution.Execution;
import cointoss.execution.LogType;
import cointoss.util.Chrono;
import cointoss.util.feather.FeatherStore;
import hypatia.Num;
import kiss.Disposable;
import kiss.I;
import kiss.Signal;
import kiss.Variable;

public class TickerManager implements Disposable {

    /** The latest execution. */
    public final Variable<Execution> latest = Variable.of(Market.BASE);

    /** The associated service. */
    public final MarketService service;

    /** Total of long volume since application startup. */
    double longVolume = 0;

    /** Total of long losscut volume since application startup. */
    double longLosscutVolume = 0;

    /** Total of short volume since application startup. */
    double shortVolume = 0;

    /** Total of short losscut volume since application startup. */
    double shortLosscutVolume = 0;

    /** The number of tickers. */
    private final int size = Span.values().length;

    /** The managed tickers. */
    private final Ticker[] tickers = new Ticker[size];

    /** The initialization state. */
    private boolean initialized;

    public TickerManager() {
        this(null);
    }

    /**
     * Create {@link TickerManager}.
     */
    public TickerManager(MarketService service) {
        this.service = service;

        for (int i = size - 1; 0 <= i; i--) {
            Ticker ticker = tickers[i] = new Ticker(Span.values()[i], this);

            // cache associated upper tickers
            int index = 0;
            for (int upper : ticker.span.uppers) {
                ticker.uppers[index++] = this.tickers[upper];
            }
        }
    }

    /**
     * Retrieve the {@link Ticker} by {@link Span}.
     * 
     * @param span The target {@link Span}.
     */
    public Ticker on(Span span) {
        return tickers[span.ordinal()];
    }

    /**
     * Retrieve all {@link Ticker}s.
     * 
     * @return
     */
    public Signal<Ticker> tickers() {
        return I.signal(tickers);
    }

    /**
     * Update all {@link Ticker}s by {@link Execution}.
     * 
     * @param e The latest {@link Execution}.
     */
    public void update(Execution e) {
        if (initialized == false) {
            // initialize tickers once if needed
            initialized = true;
            // for the first time only, set LTP before updating tickers
            latest.set(e);

            for (Ticker ticker : tickers) {
                ticker.init(e);
            }
        } else {
            // update tickers
            update(tickers[0], e, e.price, e.price.compareTo(latest.v.price));
        }

        // update total related values
        if (e.orientation == Direction.BUY) {
            longVolume += e.size.doubleValue();
            if (e.delay == Execution.DelayHuge) {
                shortLosscutVolume += e.size.doubleValue();
            }
        } else {
            shortVolume += e.size.doubleValue();
            if (e.delay == Execution.DelayHuge) {
                longLosscutVolume += e.size.doubleValue();
            }
        }

        // update the latest execution at last
        latest.set(e);
    }

    /**
     * Update the specified {@link Ticker}.
     * 
     * @param ticker A target ticker to update.
     * @param execution A latest {@link Execution}.
     * @param price A latest price to cache.
     * @param comparisonResult The comparison result between previous price and current price.
     */
    private void update(Ticker ticker, Execution execution, Num price, int comparisonResult) {
        if (ticker.createTick(execution)) {
            for (Ticker upper : ticker.uppers) {
                update(upper, execution, price, comparisonResult);
            }
        } else {
            // If a new tick is not added, the maximum value and the minimum value will be updated.
            switch (comparisonResult) {
            case 1:
                // If it is higher than the previous price, since it is impossible to update the
                // minimum price in all upper tickers, only update the maximum price.
                updateHighPrice(ticker, price);
                break;
            case -1:
                // If it is lower than the previous price, since it is impossible to update the
                // maximum price in all upper tickers, only update the minimum price.
                updateLowPrice(ticker, price);
                break;
            }
        }
    }

    /**
     * Update high price of the specified {@link Ticker}.
     * 
     * @param ticker A target {@link Ticker} to update high price.
     * @param price A current price.
     */
    private void updateHighPrice(Ticker ticker, Num price) {
        if (price.isGreaterThan(ticker.current.highPrice)) {
            ticker.current.highPrice = price.doubleValue();

            for (Ticker upper : ticker.uppers) {
                updateHighPrice(upper, price);
            }
        }
    }

    /**
     * Update low price of the specified {@link Ticker}.
     * 
     * @param ticker A target {@link Ticker} to update low price.
     * @param price A current price.
     */
    private void updateLowPrice(Ticker ticker, Num price) {
        if (price.isLessThan(ticker.current.lowPrice)) {
            ticker.current.lowPrice = price.doubleValue();

            for (Ticker upper : ticker.uppers) {
                updateLowPrice(upper, price);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void vandalize() {
        for (Ticker ticker : tickers) {
            ticker.dispose();
        }
    }

    /**
     * Estimate the date range of full build.
     * 
     * @return
     */
    public ZonedDateTime[] estimateFullBuild() {
        Ticker ticker = on(Span.Day);

        // ideal cache's range
        long[] times = ticker.ticks.computeIdealSegmentTime();
        ZonedDateTime startCache = Chrono.utcBySeconds(times[0]);
        ZonedDateTime endCache = Chrono.utcBySeconds(times[1]);

        // log's range
        ZonedDateTime startLog = service.log.firstCacheDate();
        ZonedDateTime endLog = service.log.lastCacheDate();

        // compute the suitable log range
        ZonedDateTime start = Chrono.min(startLog, Chrono.between(startLog, startCache, endLog));
        ZonedDateTime end = Chrono.max(endLog, Chrono.between(startLog, endCache, endLog));

        return new ZonedDateTime[] {start, end};
    }

    /**
     * Build ticker data from execution log.
     * 
     * @param forceRebuild
     * @return
     */
    public Signal<ZonedDateTime> buildFully(boolean forceRebuild) {
        ZonedDateTime[] dates = estimateFullBuild();
        return build(dates[0], dates[1], forceRebuild);
    }

    /**
     * Build ticker data from execution log by your specified date-range.
     * 
     * @param start
     * @param end
     * @param forceRebuild
     * @return
     */
    public Signal<ZonedDateTime> build(ZonedDateTime start, ZonedDateTime end, boolean forceRebuild) {
        start = start.truncatedTo(ChronoUnit.DAYS);
        end = end.truncatedTo(ChronoUnit.DAYS);

        // correct order
        if (start.isAfter(end)) {
            ZonedDateTime temp = start;
            start = end;
            end = temp;
        }

        // check range of execution log
        start = Chrono.max(service.log.firstCacheDate(), start);
        end = Chrono.min(service.log.lastCacheDate(), end);

        Signal<ZonedDateTime> process = I.signal();
        if (forceRebuild) {
            process = buildCache(start, end);
        } else {
            FeatherStore<Tick> ticks = on(Span.Day).ticks;
            long firstTime = ticks.firstTime();
            long lastTime = ticks.lastTime();

            if (firstTime == -1 && lastTime == -1) {
                process = buildCache(start, end);
            } else {
                if (lastTime < end.toEpochSecond()) {
                    process = process.merge(buildCache(Chrono.utcBySeconds(lastTime), end));
                }

                if (start.toEpochSecond() < firstTime) {
                    process = process.merge(buildCache(start, Chrono.utcBySeconds(firstTime)));
                }
            }
        }

        return process;
    }

    private Signal<ZonedDateTime> buildCache(ZonedDateTime start, ZonedDateTime end) {
        return Chrono.range(end, start).effect(date -> {
            service.log.at(date, LogType.Fast).effectOnLifecycle(new TickerBuilder(service, this)).to(I.NoOP);
        });
    }

    /**
     * Expire all cache on memory.
     */
    public void expire() {
        for (Ticker ticker : tickers) {
            ticker.ticks.expire();
        }
    }

    /**
     * Clear all cache on memory.
     */
    public void clear() {
        for (Ticker ticker : tickers) {
            ticker.ticks.clear();
        }
    }
}