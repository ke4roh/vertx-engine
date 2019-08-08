package com.redhat.vertx;


import io.reactivex.Completable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DocumentLogger watches events related to documents and logs them.
 */
public class DocumentLogger extends AbstractVerticle {
    private EventBus bus;
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Map<String, Set<MessageConsumer<Object>>> listeners = new HashMap<>();

    public DocumentLogger() {
    }


    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
    }

    @Override
    public Completable rxStart() {
        bus = this.vertx.eventBus();
        Set<MessageConsumer<Object>> listeners = new HashSet<>();
        this.listeners.put(null,listeners);
        listeners.add(bus.consumer("documentStarted", m -> onNewDocument(String.valueOf(m.body()))));
        listeners.add(bus.consumer("documentCompleted", m -> onCompletedDocument(String.valueOf(m.body()))));
        return Completable.complete();
    }

    @Override
    public Completable rxStop() {
        for (Set<MessageConsumer<Object>> set: listeners.values()) {
            for (MessageConsumer<Object> listener: set) {
                listener.unregister();
            }
        }
        return Completable.complete();
    }

    private void onNewDocument(String uuid) {
        logger.info("Starting document " + uuid);
        Set<MessageConsumer<Object>> listeners = new HashSet<>();
        this.listeners.put(uuid,listeners);

        listeners.add(bus.consumer("sectionCompleted." + uuid, key ->
            logger.info("Document " + uuid + " section " + key.body() + " completed.")
        ));
        listeners.add(bus.consumer("sectionStarted." + uuid, key ->
            logger.info("Document " + uuid + " section " + key.body() + " started.")
        ));
        listeners.add(bus.consumer("sectionErrored." + uuid, key ->
            logger.info("Document " + uuid + " section " + String.valueOf(key.body()) + " errored.")
        ));
        listeners.add(bus.consumer("documentChanged." + uuid, key ->
            logger.info("Document " + uuid + " field " + key.body() + " set.")
        ));
        listeners.add(bus.consumer("changeRequest." + uuid, delta -> {
            JsonObject body = (JsonObject) delta.body();
            Map.Entry<String, Object> entry = body.iterator().next();
            String field = entry.getKey();
            Object content = entry.getValue();
            logger.fine("Document " + uuid + " change requested, field " + field + ".");
            if (logger.getLevel() == Level.FINEST) {
                logger.finest("New content: " + String.valueOf(content));
            }
        }));
    }

    private void onCompletedDocument(String uuid) {
        logger.info("Completed document " + uuid);
        listeners.remove(uuid).iterator().forEachRemaining(MessageConsumer::unregister);
    }

}
