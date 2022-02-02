/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

public class Project extends bee.api.Project {

    {
        product("cointoss", "cointoss", ref("version.txt"));

        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "viewtify");
        require("com.github.teletha", "icymanipulator").atAnnotation();
        require("com.github.teletha", "antibug").atTest();
        require("com.pgs-soft", "HttpClientMock").atTest();
        require("org.apache.commons", "commons-lang3");
        require("commons-net", "commons-net", "3.7.2");
        require("com.google.guava", "guava");
        unrequire("com.google.code.findbugs", "jsr305");
        unrequire("com.google.errorprone", "error_prone_annotations");
        unrequire("com.google.guava", "listenablefuture");
        unrequire("com.google.j2objc", "j2objc-annotations");
        unrequire("org.checkerframework", "checker-qual");
        require("com.univocity", "univocity-parsers");
        require("com.github.luben", "zstd-jni");
        require("org.decimal4j", "decimal4j").atTest();
        require("ai.djl", "api");
        require("ai.djl.mxnet", "mxnet-engine");
        require("edu.brown.cs.burlap", "burlap");
        require("org.nd4j", "nd4j-native-platform");
        require("org.deeplearning4j", "rl4j-core");

        versionControlSystem("https://github.com/teletha/cointoss");
    }
}