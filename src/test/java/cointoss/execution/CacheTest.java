/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.execution;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import cointoss.TestableMarket;
import cointoss.execution.ExecutionLog.Cache;
import cointoss.util.Chrono;
import kiss.I;
import kiss.Signal;

class CacheTest {

    TestableMarket market;

    @BeforeEach
    void setup() {
        market = new TestableMarket();
    }

    @Test
    void normalLog() {
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        assert cache.normal.name().equals("execution20201215.log");
    }

    @Test
    void compactLog() {
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        assert cache.compactLog().name().equals("execution20201215.clog");
    }

    @Test
    void fastLog() {
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        assert cache.fastLog().name().equals("execution20201215.flog");
    }

    @Test
    void writeNormal() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        assert cache.existNormal() == false;

        cache.writeNormal(e1, e2);
        assert cache.existNormal();
    }

    @Test
    void writeCompact() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        assert cache.existCompact() == false;

        cache.writeCompact(e1, e2);
        assert cache.existCompact();
    }

    @Test
    void readNormal() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        // write
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeNormal(e1, e2);

        // read
        List<Execution> executions = cache.readNormal().toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(e1);
        assert executions.get(1).equals(e2);
    }

    @Test
    void readCompact() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        // write
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeCompact(e1, e2);

        // read
        List<Execution> executions = cache.readCompact().toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(e1);
        assert executions.get(1).equals(e2);
    }

    @Test
    void readFromNotNormalButCompact() {
        Execution n1 = Execution.with.buy(1).price(10);
        Execution n2 = Execution.with.buy(1).price(12);
        Execution c1 = Execution.with.buy(2).price(10);
        Execution c2 = Execution.with.buy(2).price(12);

        // write
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeNormal(n1, n2);
        cache.writeCompact(c1, c2);

        // read
        List<Execution> executions = cache.read().toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(c1);
        assert executions.get(1).equals(c2);
    }

    @Test
    void readExternalRepository() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        ExecutionLogRepository external = new ExecutionLogRepository(market.service) {

            @Override
            public Signal<Execution> convert(ZonedDateTime date) {
                return I.signal(e1, e2);
            }

            @Override
            public Signal<ZonedDateTime> collect() {
                return I.signal(Chrono.utc(2020, 12, 15));
            }
        };

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        List<Execution> executions = cache.readExternalRepository(external).toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(e1);
        assert executions.get(1).equals(e2);
        assert cache.existNormal();
        assert cache.existCompact() == false;
    }

    @Test
    void convertNormalToCompact() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        // write
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeNormal(e1, e2);
        assert cache.existNormal();
        assert cache.existCompact() == false;

        // convert
        cache.convertNormalToCompact(false);
        assert cache.existNormal() == false;
        assert cache.existCompact();
        assert cache.existFast();

        // read from compact
        List<Execution> executions = cache.read().toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(e1);
        assert executions.get(1).equals(e2);
    }

    @Test
    void convertCompactToNormal() {
        Execution e1 = Execution.with.buy(1).price(10);
        Execution e2 = Execution.with.buy(1).price(12);

        // write
        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeCompact(e1, e2);
        assert cache.existNormal() == false;
        assert cache.existCompact();

        // convert
        cache.convertCompactToNormal();
        assert cache.existNormal();
        assert cache.existCompact();

        // read from normal
        List<Execution> executions = cache.readNormal().toList();
        assert executions.size() == 2;
        assert executions.get(0).equals(e1);
        assert executions.get(1).equals(e2);
    }

    @Test
    void existCompletedNormalOnCompleted() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(12).date(date.plusDays(1));
        market.service.executionsWillResponse(r1);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.existCompletedNormal();
    }

    @Test
    void existCompletedNormalOnImcompleted() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(14).date(date);
        market.service.executionsWillResponse(r1);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.existCompletedNormal() == false;
    }

    @Test
    void repairCompactLog() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Cache cache = market.log.cache(date);
        cache.writeCompact(e1, e2);
        assert cache.repair(false);
        assert cache.existCompact();
        assert cache.existNormal() == false;
    }

    @Test
    void repairComletedNormalLog() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(14).date(date.plusDays(1));
        market.service.executionsWillResponse(r1);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false);
        assert cache.existCompact();
        assert cache.existNormal() == false;
    }

    @Test
    void repairIgnoreToday() {
        ZonedDateTime date = Chrono.utcToday();
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(14).date(date.plusDays(1));
        market.service.executionsWillResponse(r1);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false) == false;
        assert cache.existCompact() == false;
        assert cache.existNormal();
    }

    @Test
    void repairIncomletedNormalLogAndExternalRepository() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(14).date(date);
        market.service.executionsWillResponse(r1);
        market.service.external = useExternalRepository(date);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false);
        assert cache.existCompact();
        assert cache.existNormal() == false;
    }

    @Test
    void repairNoNormalLogAndExternalRepository() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);

        Execution r1 = Execution.with.buy(1).price(14).date(date);
        market.service.executionsWillResponse(r1);
        market.service.external = useExternalRepository(date);

        Cache cache = market.log.cache(date);
        assert cache.repair(false);
        assert cache.existCompact();
        assert cache.existNormal() == false;
    }

    /**
     * Helper to create {@link ExecutionLogRepository} which has the completed log at the specified
     * date.
     * 
     * @return
     */
    private ExecutionLogRepository useExternalRepository(ZonedDateTime target) {
        Objects.requireNonNull(target);

        return new ExecutionLogRepository(market.service) {

            @Override
            public Signal<Execution> convert(ZonedDateTime date) {
                if (target.isEqual(date)) {
                    Execution e1 = Execution.with.buy(1).price(10).date(date);
                    Execution e2 = Execution.with.buy(1).price(12).date(date);
                    Execution e3 = Execution.with.sell(1).price(12).date(date);

                    return I.signal(e1, e2, e3);
                } else {
                    return I.signal();
                }
            }

            @Override
            public Signal<ZonedDateTime> collect() {
                return I.signal(target);
            }
        };
    }

    @Test
    void repairIncomletedNormalLogAndCompletableServerData() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(13).date(date);
        Execution r2 = Execution.with.sell(1).price(14).date(date);
        Execution r3 = Execution.with.buy(1).price(15).date(date.plusDays(1));
        market.service.executionsWillResponse(r1, r2, r3);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.existNormal() == true;

        assert cache.repair(false);
        assert checkCompact(cache, e1, e2, r1, r2);
        assert cache.existNormal() == false;
    }

    @Test
    void repairIncomletedNormalLogAndCompletableServerDataWithAlreadyWrittenExecutions() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(13).date(date);
        Execution r2 = Execution.with.sell(1).price(14).date(date);
        Execution r3 = Execution.with.buy(1).price(15).date(date.plusDays(1));
        market.service.executionsWillResponse(e1, e2, r1, r2, r3);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);

        assert cache.repair(false);
        assert checkCompact(cache, e1, e2, r1, r2);
        assert cache.existNormal() == false;
    }

    @Test
    void repairIncomletedNormalLogAndIncompletableServerData() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(13).date(date);
        Execution r2 = Execution.with.sell(1).price(14).date(date);
        Execution r3 = Execution.with.buy(1).price(15).date(date);
        market.service.executionsWillResponse(r1, r2, r3);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false) == false;
        assert checkNormal(cache, e1, e2, r1, r2, r3);
    }

    @Test
    void repairIncomletedNormalLogAndIncompletableServerDataWithAlreadyWrittenExecutions() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(13).date(date);
        Execution r2 = Execution.with.sell(1).price(14).date(date);
        Execution r3 = Execution.with.buy(1).price(15).date(date);
        market.service.executionsWillResponse(e1, e2, r1, r2, r3);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false) == false;
        assert checkNormal(cache, e1, e2, r1, r2, r3);
    }

    @Test
    void repairWithoutNormalLog() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution p0 = Execution.with.buy(1).price(10).date(date.minusDays(1));
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(10).date(date);
        market.service.executionsWillResponse(e1, e2);
        market.service.searchNearestIdWillResponse(p0);

        Cache cache = market.log.cache(date);
        assert cache.repair(false) == false;
        assert checkNormal(cache, e1, e2);
    }

    @Test
    void repairIncomletedNormalLogAndIncompletableServerDataWithExecutionsOnDifferentDay() {
        ZonedDateTime date = Chrono.utc(2020, 12, 15);
        Execution e1 = Execution.with.buy(1).price(10).date(date);
        Execution e2 = Execution.with.buy(1).price(12).date(date);

        Execution r1 = Execution.with.buy(1).price(13).date(date.minusDays(1));
        Execution r2 = Execution.with.sell(1).price(14).date(date.plusDays(1));
        market.service.executionsWillResponse(r1, r2);

        Cache cache = market.log.cache(date);
        cache.writeNormal(e1, e2);
        assert cache.repair(false);
        assert checkCompact(cache, e1, e2);
    }

    /**
     * Assert market.log.
     * 
     * @param cache
     * @param executions
     * @return
     */
    private boolean checkNormal(Cache cache, Execution... executions) {
        List<Execution> list = cache.readNormal().toList();
        assert list.size() == executions.length;
        for (int i = 0; i < executions.length; i++) {
            assert list.get(i).equals(executions[i]);
        }
        return true;
    }

    /**
     * Assert market.log.
     * 
     * @param cache
     * @param executions
     * @return
     */
    private boolean checkCompact(Cache cache, Execution... executions) {
        List<Execution> list = cache.readCompact().toList();
        assert list.size() == executions.length;
        for (int i = 0; i < executions.length; i++) {
            assert list.get(i).equals(executions[i]);
        }
        return true;
    }

    @Test
    void codec() throws IOException {
        int size = 1000;
        Execution[] executions = Executions.random(size, Duration.ofMillis(2000)).toArray(new Execution[size]);

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeNormal(executions);
        HashCode hashOriginal = Files.asByteSource(cache.normal.asJavaFile()).hash(Hashing.sha256());

        // encode and decode
        cache.convertNormalToCompact(false);
        cache.normal.delete();
        cache.convertCompactToNormal();

        // check hash
        HashCode hashRestored = Files.asByteSource(cache.normal.asJavaFile()).hash(Hashing.sha256());
        assert hashOriginal.equals(hashRestored);

        // check object
        List<Execution> restored = cache.readNormal().toList();
        assert restored.size() == executions.length;
        for (int i = 0; i < executions.length; i++) {
            assert executions[i].toString().equals(restored.get(i).toString());
        }
    }

    @Test
    void lastIdWithNormal() {
        Execution e1 = Execution.with.buy(1).price(10).id(1);
        Execution e2 = Execution.with.buy(1).price(10).id(2);
        Execution e3 = Execution.with.buy(1).price(10).id(3);

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeNormal(e1, e2, e3);
        assert cache.existNormal() == true;
        assert cache.normal.size() != 0;
        assert cache.estimateLastID() == 3;
    }

    @Test
    void lastIdWithCompact() {
        Execution e1 = Execution.with.buy(1).price(10).id(1);
        Execution e2 = Execution.with.buy(1).price(10).id(2);
        Execution e3 = Execution.with.buy(1).price(10).id(3);

        Cache cache = market.log.cache(Chrono.utc(2020, 12, 15));
        cache.writeCompact(e1, e2, e3);
        assert cache.estimateLastID() == 3;
    }
}