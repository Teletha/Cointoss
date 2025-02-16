/*
 * Copyright (C) 2024 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.util;

import static java.util.concurrent.TimeUnit.*;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import icy.manipulator.Icy;
import kiss.Disposable;
import kiss.I;
import kiss.JSON;
import kiss.Observer;
import kiss.Signal;
import kiss.Variable;

@Icy
public abstract class EfficientWebSocketModel {

    private static final Disposable Shutdown = Disposable.empty();

    /** The connection holder. */
    private final Variable<WebSocket> connection = Variable.empty();

    /** The signal tee. */
    private final Map<String, Supersonic> signals = new HashMap();

    /** The connecting state. */
    private final AtomicBoolean connecting = new AtomicBoolean();

    /** The current subscribing topics. */
    private final Set<IdentifiableTopic> subscribing = ConcurrentHashMap.newKeySet();

    /** The current subscribed topics. */
    private final Set<IdentifiableTopic> subscribed = ConcurrentHashMap.newKeySet();

    /** The limite rate. */
    private RateLimiter limit = RateLimiter.with.limit(1).refresh(250, MILLISECONDS);

    /** The server is responsible or not. */
    private boolean noReplyMode;

    /** The clean up point on disconnect. */
    private Disposable cleanup = Disposable.empty();

    private boolean debug;

    private boolean socketIO;

    /**
     * 
     */
    public EfficientWebSocketModel() {
        // At the end of the application, individually unsubscribing topics would take too much
        // time, so we just disconnect websocket.
        Shutdown.add(() -> disconnect("Shutdown Application", null));
    }

    @Icy.Property(copiable = true)
    public abstract String address();

    /**
     * Extract channel id from massage.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public abstract Function<JSON, String> extractId();

    /**
     * The subscription ID may be determined by the content of the response, so we must extract the
     * new ID from the response.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public Function<JSON, String> updateId() {
        return null;
    }

    /**
     * Sets the maximum number of subscriptions per connection. Default value is 25. A number less
     * than or equal to 0 is considered unlimited.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public int maximumSubscriptions() {
        return Integer.MAX_VALUE;
    }

    /**
     * Ignore JSON that match the specified criteria. This process is very efficient because it is
     * tried only once for each JSON data on the base stream.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public Predicate<JSON> ignoreMessageIf() {
        return null;
    }

    /**
     * Reconnect socket when some message match the specified criteria.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public Predicate<JSON> recconnectIf() {
        return null;
    }

    /**
     * Stop reconnecting socket when some message match the specified criteria.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public Predicate<JSON> stopRecconnectIf() {
        return null;
    }

    /**
     * Pong when some message match the specified criteria.
     * 
     * @return Chainable API.
     */
    @Icy.Property
    public Function<JSON, String> pongIf() {
        return null;
    }

    /**
     * Invoked when connection is established.
     * 
     * @return
     */
    @Icy.Property
    public Consumer<WebSocket> whenConnected() {
        return null;
    }

    /**
     * Outputs a detailed log.
     * 
     * @return Chainable API.
     */
    @Icy.Property(copiable = true)
    public HttpClient client() {
        return null;
    }

    /**
     * Outputs a detailed log.
     * 
     * @return Chainable API.
     */
    @Icy.Property(copiable = true)
    public ScheduledExecutorService scheduler() {
        return null;
    }

    /**
     * Outputs a detailed log.
     */
    public EfficientWebSocket enableDebug() {
        this.debug = true;
        return (EfficientWebSocket) this;
    }

    /**
     * Outputs a detailed log.
     */
    public EfficientWebSocket enableSocketIO() {
        this.socketIO = true;
        this.noReplyMode = true;
        return (EfficientWebSocket) this;
    }

    /**
     * Configure that this server never reply.
     * 
     * @return
     */
    public final EfficientWebSocket noServerReply() {
        noReplyMode = true;
        return (EfficientWebSocket) this;
    }

    /**
     * Configure the network access rate.
     * 
     * @return
     */
    public final EfficientWebSocket restrict(RateLimiter limiter) {
        this.limit = Objects.requireNonNull(limiter);
        return (EfficientWebSocket) this;
    }

    /**
     * Execute command on this connection.
     * 
     * @param topic A subscription command (i.e. bean-like object).
     * @return A shared connection.
     */
    public final synchronized Signal<JSON> subscribe(IdentifiableTopic topic) {
        return signals.computeIfAbsent(topic.id, id -> new Supersonic(topic)).expose;
    }

    /**
     * Subscribe the specified topic.
     * 
     * @param topic A topic to subscribe.
     */
    private synchronized void sendSubscribe(IdentifiableTopic topic) {
        if (connecting.compareAndSet(false, true)) {
            connect();
        }

        connection.observing().skipNull().first().to(ws -> {
            if (subscribing.add(topic)) {
                cleanup.add(I.schedule(0, 10, SECONDS, true, scheduler())
                        .takeWhile(count -> connection.isPresent())
                        .takeWhile(count -> !subscribed.contains(topic))
                        .to(count -> {
                            limit.acquire();

                            if (socketIO) {
                                // 42["join-room","transactions_btc_jpy"]
                                String command = "42[\"join-room\",\"" + topic.id + "\"]";
                                ws.sendText(command, true);
                                I.trace("Sent websocket command " + command + " to " + address() + ". @" + count);
                            } else {
                                if (debug) debugSendMessage("subscribe", I.write(topic));
                                ws.sendText(I.write(topic), true);
                                I.trace("Sent websocket command " + topic + " to " + address() + ". @" + count);
                            }

                            if (noReplyMode) {
                                subscribed.add(topic);
                            }
                        }));
            }
        });
    }

    /**
     * Unsubscribe the specified topic.
     * 
     * @param topic A topic to unsubscribe.
     */
    private synchronized void snedUnsubscribe(IdentifiableTopic topic) {
        connection.to(ws -> {
            if (subscribed.contains(topic)) {
                limit.acquire();

                try {
                    IdentifiableTopic unsubscribe = topic.unsubscribe();
                    ws.sendText(I.write(unsubscribe), true);
                    subscribed.remove(topic);
                    I.trace("Sent websocket command " + unsubscribe + " to " + address() + ".");
                } catch (Throwable e) {
                    // ignore
                } finally {
                    if (subscribed.isEmpty()) {
                        disconnect("No Subscriptions", null);
                    }
                }
            }
        });
    }

    /**
     * Connect to the server by websocket.
     */
    private synchronized void connect() {
        I.trace("Starting websocket [" + address() + "].");

        I.http(address() + (socketIO ? "?EIO=3&transport=websocket" : ""), ws -> {
            I.trace("Connected websocket [" + address() + "].");

            Consumer<WebSocket> connected = whenConnected();
            if (connected != null) {
                connected.accept(ws);
            }
            connection.set(ws);
        }, client()).to(debug ? I.bundle(this::outputTestCode, this::dispatch) : this::dispatch, e -> {
            error(e);
        }, () -> {
            disconnect("User Closed", null);
            signals.values().forEach(signal -> signal.complete());
        });
    }

    /**
     * Send close message to disconnect this websocket.
     */
    private synchronized void disconnect(String message, Throwable error) {
        connection.to(ws -> {
            // try to do disconnection
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
            } catch (Throwable ignore) {
                // ignore
            } finally {
                connection.set((WebSocket) null);
            }

            // reset
            cleanup.dispose();
            cleanup = Disposable.empty();
            subscribing.clear();
            subscribed.clear();

            if (error == null) {
                I.info("Disconnected websocket [" + address() + "] normally because " + message);
            } else {
                I.error("Disconnected websocket [" + address() + "]  unexpectedly because " + message);
                I.error(error);
            }
        });
        connecting.set(false);
    }

    /**
     * Disconnect websocket connection and send error message to all channels.
     */
    private void error(Throwable e) {
        disconnect(e.getMessage(), e);
        signals.values().forEach(signal -> signal.error(e));
    }

    /**
     * Dispatch websocket message.
     * 
     * @param text
     */
    private void dispatch(String text) {
        if (socketIO) {
            int start = text.indexOf('{');
            if (start == -1) return;
            int end = text.lastIndexOf('}') + 1;

            text = text.substring(start, end);
        }

        JSON json = I.json(text);

        Supersonic signaling = signals.get(extractId().apply(json));
        if (signaling != null) {
            signaling.accept(json);
        } else {
            Predicate<JSON> reject = ignoreMessageIf();
            if (reject != null && reject.test(json)) {
                return;
            }

            for (IdentifiableTopic topic : subscribing) {
                if (topic.verifySubscribedReply(json)) {
                    subscribed.add(topic);
                    subscribing.remove(topic);
                    I.trace("Accepted websocket subscription [" + address() + "] " + topic.id + ".");

                    Function<JSON, String> updater = updateId();
                    if (updater != null) {
                        topic.updatedId = updater.apply(json);
                        signals.put(topic.updatedId, signals.get(topic.id));
                        I.trace("Update websocket [" + address() + "] subscription id from '" + topic.id + "' to '" + topic.updatedId + "'.");
                    }
                    return;
                }
            }

            // ping - pong
            Function<JSON, String> pong = pongIf();
            if (pong != null) {
                String reply = pong.apply(json);
                if (reply != null) {
                    connection.to(v -> v.sendText(reply.replace('\'', '"'), true));
                    return;
                }
            }

            // ping -pong in Socket.IO
            if (socketIO) {
                String value = json.text("pingInterval");
                if (value != null) {
                    cleanup.add(I.schedule(10000, Long.parseLong(value) - 7000 /* Safety */, MILLISECONDS, true)
                            .to(() -> connection.v.sendText("2", true)));
                    return;
                }
            }

            // reconencting
            Predicate<JSON> recconnect = recconnectIf();
            if (recconnect != null && recconnect.test(json)) {
                error(new ConnectException("Server was terminated by some error, Try to reconnect. " + json));
                return;
            }

            // stop reconnecting
            Predicate<JSON> stopping = stopRecconnectIf();
            if (stopping != null && stopping.test(json)) {
                cleanup.dispose();
                cleanup = Disposable.empty();
                I.trace("Stop reconnecting because the channel was already subscribed. " + json);
                return;
            }

            // we can't handle message
            I.warn("Unknown message was recieved. [" + address() + "] " + text);
        }
    }

    /**
     * Output test code.
     * 
     * @param text
     */
    private void outputTestCode(String text) {
        System.out.println("server.sendJSON(\"" + text.replace('"', '\'') + "\");");
    }

    private void debugSendMessage(String type, String message) {
        System.out.println("user.sendJSON as " + type);
        System.out.println(message);
    }

    /**
     * Release all websocket related resources.
     */
    public static final void shutdownNow() {
        Shutdown.dispose();
    }

    /**
     * Identifiable topic which can represents subscribe and unsubscribe commands.
     */
    public static abstract class IdentifiableTopic<T extends IdentifiableTopic> implements Cloneable {

        /** The identifier. */
        protected final String id;

        /** The updated id holder. */
        protected String updatedId;

        /**
         * @param id
         */
        protected IdentifiableTopic(String id) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID must be non-empty value.");
            }
            this.id = id;
        }

        /**
         * Go to Unsubscribe mode.
         * 
         * @return
         */
        private IdentifiableTopic unsubscribe() {
            try {
                T cloned = (T) clone();
                buildUnsubscribeMessage(cloned);
                return cloned;
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }

        /**
         * Make sure your channel (un)subscription has been properly accepted.
         * 
         * @return
         */
        protected abstract boolean verifySubscribedReply(JSON reply);

        /**
         * Build the unsubscription message for this topic.
         * 
         * @param topic
         */
        protected abstract void buildUnsubscribeMessage(T topic);

        /**
         * {@inheritDoc}
         */
        @Override
        public final int hashCode() {
            return id.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof IdentifiableTopic) {
                IdentifiableTopic other = (IdentifiableTopic) obj;
                return id.equals(other.id);
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final String toString() {
            return I.write(this).replaceAll("\\s", "");
        }
    }

    /**
     * Supersonic {@link Signal} support subject.
     */
    private class Supersonic implements Observer<JSON> {

        /** The associated topic. */
        private IdentifiableTopic topic;

        /** The managed observers. */
        private final CopyOnWriteArrayList<Observer> managed = new CopyOnWriteArrayList();

        /** The exposed interface. */
        private final Signal<JSON> expose = new Signal<>((observer, disposer) -> {
            // First of all, you must register an observer. Because if you go ahead with the next
            // subscription request, you won't be able to read the message if the reply comes back
            // super fast (i.e local mocking server).
            managed.add(observer);
            if (managed.size() == 1) sendSubscribe(topic);

            return disposer.add(() -> {
                managed.remove(observer);
                if (managed.size() == 0) snedUnsubscribe(topic);
            });
        });

        /**
         * Binding topic.
         * 
         * @param topic
         */
        private Supersonic(IdentifiableTopic topic) {
            this.topic = topic;
        }

        @Override
        public void accept(JSON value) {
            managed.forEach(o -> o.accept(value));
        }

        @Override
        public void complete() {
            managed.forEach(Observer::complete);
        }

        @Override
        public void error(Throwable error) {
            managed.forEach(o -> o.error(error));
        }
    }
}