package com.redhat.vertx;


import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.reactivex.Completable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;

/**
 * DocumentLogger watches events related to documents and logs them.
 */
public class DocumentLogger extends AbstractVerticle {
    private EventBus bus;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    public DocumentLogger() {
    }

    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
    }

    @Override
    public Completable rxStart() {
        bus = this.vertx.eventBus();
        bus.consumer("documentStarted", m -> onNewDocument(String.valueOf(m.body())));
        bus.consumer("documentCompleted", m -> onCompletedDocument(String.valueOf(m.body())));

        bus.consumer("sectionCompleted", msg ->
                logger.info("Thread: " + Thread.currentThread().getName() + " - Document " + msg.headers().get("uuid") + " section " + msg.body() + " completed.")
        );
        bus.consumer("sectionStarted", msg ->
                logger.info("Thread: " + Thread.currentThread().getName() + " - Document " + msg.headers().get("uuid") + " section " + msg.body() + " started.")
        );
        bus.consumer("sectionErrored", msg ->
                logger.info("Thread: " + Thread.currentThread().getName() + " - Document " + msg.headers().get("uuid") + " section " + String.valueOf(msg.body()) + " errored.")
        );
        bus.consumer("documentChanged", msg ->
                logger.info("Thread: " + Thread.currentThread().getName() + " - Document " + msg.headers().get("uuid") + " field " + msg.body() + " set.")
        );
        bus.consumer("changeRequest", msg -> {
            JsonObject body = (JsonObject) msg.body();
            Map.Entry<String, Object> entry = body.iterator().next();
            String field = entry.getKey();
            Object content = entry.getValue();
            logger.fine("Thread: " + Thread.currentThread().getName() + " - Document " + msg.headers().get("uuid") + " change requested, field " + field + ".");
            if (logger.getLevel() == Level.FINEST) {
                logger.finest("New content: " + String.valueOf(content));
            }
        });
        return Completable.complete();
    }

    private void onNewDocument(String uuid) {
        logger.info("Starting document " + uuid);
    }

    private void onCompletedDocument(String uuid) {
        logger.info("Thread: " + Thread.currentThread().getName() + " - Completed document " + uuid);
    }

}
