/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.market.bitflyer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import cointoss.Execution;
import cointoss.MarketBuilder;
import cointoss.Side;
import cointoss.market.Span;
import eu.verdelhan.ta4j.Decimal;
import filer.Filer;
import kiss.I;
import kiss.Signal;

/**
 * @version 2017/08/16 8:13:06
 */
public class BitFlyerBuilder implements MarketBuilder {

    /** UTC */
    private static final ZoneId zone = ZoneId.of("UTC");

    /** file data format */
    private static final DateTimeFormatter fomatFile = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** realtime data format */
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.n'Z'");

    /** Sample trend */
    private static final Span SampleTrend = new Span(2017, 5, 29, 2017, 6, 5);

    /** Sample of range trend */
    private static final Span RangeTrend = new Span(2017, 5, 29, 2017, 7, 29);

    /** Sample of up trend */
    private static final Span UpTrend = new Span(2017, 7, 16, 2017, 8, 29);

    /** Sample of down trend */
    private static final Span DownTrend = new Span(2017, 6, 11, 2017, 7, 16);

    /** The log folder. */
    private static final Path root = Filer.locate(".log/bitflyer/" + BitFlyerType.FX_BTC_JPY.name());

    /** The current type. */
    private final BitFlyerType type;

    /** The latest execution id. */
    private long latestId;

    /** The latest cached id. */
    private long cacheId;

    /** The latest realtime id. */
    private long realtimeId;

    /** The first day. */
    private final LocalDate cacheFirst;

    /** The last day. */
    private LocalDate cacheLast;

    /** The current processing cache file. */
    private PrintWriter cache;

    /**
     * @param type
     */
    public BitFlyerBuilder(BitFlyerType type) {
        this.type = type;

        List<Path> files = Filer.walk(root, "execution*.log");
        LocalDate start = null;
        LocalDate end = null;

        for (Path file : files) {
            String name = file.getFileName().toString();
            LocalDate date = LocalDate.parse(name.substring(9, 17), fomatFile);

            if (start == null || end == null) {
                start = date;
                end = date;
            } else {
                if (start.isAfter(date)) {
                    start = date;
                }

                if (end.isBefore(date)) {
                    end = date;
                }
            }
        }
        this.cacheFirst = start;
        this.cacheLast = end;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> initialize() {
        Span span = Span.random(cacheFirst, cacheLast, 5);
        LocalDate start = span.start;
        LocalDate end = span.end;

        List<LocalDate> period = new ArrayList();

        while (start.isBefore(end)) {
            period.add(start);
            start = start.plusDays(1);
        }

        return I.signal(period)
                .map(i -> Filer.locate(".log/bitflyer/" + BitFlyerType.FX_BTC_JPY.name() + "/execution" + fomatFile.format(i) + ".log"))
                .flatMap(Filer::read)
                .map(Execution::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> from(LocalDate start) {
        return new Signal<>((observer, disposer) -> {
            // read from cache
            LocalDate current = start.isBefore(cacheFirst) ? cacheFirst : start;

            while (disposer.isDisposed() == false && !current.isAfter(getCacheEnd())) {
                disposer.add(localCache(current).effect(e -> latestId = cacheId = e.id).to(observer::accept));
                current = current.plusDays(1);
            }

            AtomicBoolean completeREST = new AtomicBoolean();

            // read from realtime API
            if (disposer.isDisposed() == false) {
                disposer.add(realtime().skipUntil(e -> completeREST.get()).effect(this::cache).to(observer::accept));
            }

            // read from REST API
            if (disposer.isDisposed() == false) {
                disposer.add(rest().effect(this::cache).effectOnComplete((o, d) -> completeREST.set(true)).to(observer::accept));
            }

            return disposer;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDate getCacheStart() {
        return cacheFirst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDate getCacheEnd() {
        return cacheLast;
    }

    /**
     * Store cache data.
     * 
     * @param exe
     */
    private void cache(Execution exe) {
        if (cacheId < exe.id) {
            try {
                LocalDate date = exe.exec_date.toLocalDate();

                if (cache == null || cacheLast.isBefore(date)) {
                    I.quiet(cache);

                    File file = localCacheFile(date).toFile();
                    file.createNewFile();

                    cache = new PrintWriter(new FileWriter(file, true));
                    cacheLast = date;
                }
                cache.println(exe.toString());
                cache.flush();
                cacheId = exe.id;
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * Read date from local cache.
     * 
     * @param date
     * @return
     */
    private Signal<Execution> localCache(LocalDate date) {
        return Filer.read(localCacheFile(date)).map(Execution::new);
    }

    /**
     * Read date from local cache.
     * 
     * @param date
     * @return
     */
    private Path localCacheFile(LocalDate date) {
        return Filer.locate(".log/bitflyer/" + BitFlyerType.FX_BTC_JPY.name() + "/execution" + fomatFile.format(date) + ".log");
    }

    /**
     * Read data from REST API.
     */
    private Signal<Execution> rest() {
        return new Signal<Execution>((observer, disposer) -> {
            while (disposer.isDisposed() == false) {
                try {
                    URL url = new URL(BitFlyerBTCFX.api + "/v1/executions?product_code=" + type + "&count=500&before=" + (latestId + 500));
                    Executions executions = I.json(url).to(Executions.class);
                    System.out.println(url);
                    for (int j = executions.size() - 1; 0 <= j; j--) {
                        Execution exe = executions.get(j);

                        if (latestId < exe.id) {
                            observer.accept(exe);
                            latestId = exe.id;
                        }
                    }
                } catch (Exception e) {
                    observer.error(e);
                }

                if (realtimeId != 0 && realtimeId <= latestId) {
                    break;
                }

                try {
                    Thread.sleep(333);
                } catch (InterruptedException e) {
                    observer.error(e);
                }
            }
            System.out.println("追いついた！！！");
            observer.complete();

            return disposer;
        });
    }

    /**
     * Read data from realtime API.
     * 
     * @return
     */
    private Signal<Execution> realtime() {
        return new Signal<>((observer, disposer) -> {
            PNConfiguration config = new PNConfiguration();
            config.setSubscribeKey("sub-c-52a9ab50-291b-11e5-baaa-0619f8945a4f");

            PubNub pubNub = new PubNub(config);
            pubNub.addListener(new SubscribeCallback() {

                @Override
                public void status(PubNub pubnub, PNStatus status) {
                }

                @Override
                public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                }

                @Override
                public void message(PubNub pubnub, PNMessageResult message) {
                    if (message.getChannel() != null) {
                        Iterator<JsonNode> iterator = message.getMessage().iterator();

                        while (iterator.hasNext()) {
                            JsonNode node = iterator.next();

                            Execution exe = new Execution();
                            exe.id = node.get("id").asLong();
                            exe.side = Side.parse(node.get("side").asText());
                            exe.price = Decimal.valueOf(node.get("price").asText());
                            exe.size = Decimal.valueOf(node.get("size").asText());
                            exe.exec_date = LocalDateTime.parse(node.get("exec_date").asText(), format).atZone(zone);
                            exe.buy_child_order_acceptance_id = node.get("buy_child_order_acceptance_id").asText();
                            exe.sell_child_order_acceptance_id = node.get("sell_child_order_acceptance_id").asText();

                            if (exe.id == 0) {
                                exe.id = ++realtimeId;
                            }

                            observer.accept(exe);
                            realtimeId = exe.id;
                        }
                    }
                }
            });
            pubNub.subscribe().channels(I.list("lightning_executions_FX_BTC_JPY")).execute();

            return disposer.add(() -> {
                pubNub.unsubscribeAll();
                pubNub.destroy();
            });
        });
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        I.load(Decimal.Codec.class, false);

        new BitFlyerBuilder(BitFlyerType.FX_BTC_JPY).from(LocalDate.of(2017, 9, 8)).to(e -> {
            System.out.println(e);
        });
    }
}
