/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.db;

import java.util.ArrayList;
import java.util.List;

import io.questdb.griffin.SqlException;

public class DBUser {
    public static class Bean {
        public int id;

        public long time;

        /**
         * @param id
         * @param value
         */
        public Bean(int id, long time) {
            this.id = id;
            this.time = time;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Bean [id=" + id + ", time=" + time + "]";
        }
    }

    public static void main(String[] args) throws SqlException {
        List<Bean> beans = new ArrayList();
        for (int i = 11; i < 100; i++) {
            Bean bean = new Bean(i, 1000000000L * i);
            beans.add(bean);
        }

        // TimeseriseDatabase.clearTable("bean");

        TimeseriseDatabase<Bean> db = TimeseriseDatabase.create("bean", Bean.class, "time");
        db.insert(beans);
        db.selectAll(i -> System.out.println(i));
        System.out.println(db.max("time") + "   " + db.min("time") + "   " + db.avg("time") + "   " + db.sum("time"));
    }
}
