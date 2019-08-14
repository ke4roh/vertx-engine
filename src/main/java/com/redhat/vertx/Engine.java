package com.redhat.vertx;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.redhat.vertx.pipeline.EventBusMessage;
import com.redhat.vertx.pipeline.Section;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

/**
 * Entrypoint for execution of a particular pipeline, container for the entire execution system.
 * The execute method on an engine is thread-safe and can process multiple documents at once.
 * When each document is completed, it fires a documentCompleted event with the document.
 *
 */
public class Engine extends AbstractVerticle {
    public static final String DOC_UUID = "__uuid__";
    private Section pipeline;
    private Map<String,JsonObject> docCache = new ConcurrentHashMap<>();

    public Engine(String pipelineDef) {
        Object json = Json.decodeValue(pipelineDef);
        JsonObject jo;
        if (json instanceof JsonArray) {
            jo=new JsonObject();
            jo.put("steps",json);
            jo.put("name","default");
        } else {
            jo = (JsonObject)json;
        }
        this.pipeline = new Section();
        pipeline.init(this,jo);
    }

    public EventBus getEventBus() {
        return vertx.eventBus();
    }

    @Override
    public Completable rxStart() {
        return Completable.create(emitter -> {
            DocumentLogger documentLogger = new DocumentLogger();
            vertx.rxDeployVerticle(documentLogger, new DeploymentOptions().setWorker(true).setWorkerPoolName("document-logger")).subscribe((s, throwable) -> {
                if (throwable != null) {
                    emitter.tryOnError(throwable);
                } else {
                    emitter.onComplete();
                }
            }); // TODO dispose properly
        });
    }

    /**
     *
     * @param executionData The document to process
     * @return A single which will provide the document at the end of execution
     */
    public Single<JsonObject> execute(JsonObject executionData) {
        String uuid = UUID.randomUUID().toString();
        executionData.put(DOC_UUID, uuid);

        docCache.put(uuid, executionData);
        EventBus bus = getEventBus();
        bus.publish(EventBusMessage.DOCUMENT_STARTED, uuid);
        MessageConsumer<Object> changeWatcher = bus.consumer(EventBusMessage.CHANGE_REQUEST, delta -> {
            JsonObject body = (JsonObject) delta.body();
            assert body.size() == 1;
            docCache.get(uuid).mergeIn(body);
            final var deliveryOptions = new DeliveryOptions().addHeader("uuid", uuid);
            bus.publish(EventBusMessage.DOCUMENT_CHANGED, body.iterator().next().getKey(), deliveryOptions);
        });

        return Single.create(source ->
                pipeline.execute(uuid).subscribe((result, err) -> { // TODO dispose properly
                    bus.publish(EventBusMessage.DOCUMENT_COMPLETED, uuid);
                    JsonObject doc = docCache.remove(uuid);
                    changeWatcher.unregister();
                    if (err != null) {
                        source.onError(err);
                    } else {
                        source.onSuccess(doc);
                    }
                }));
    }

    public JsonObject getDocument(String uuid) {
        return docCache.get(uuid);
    }
}
