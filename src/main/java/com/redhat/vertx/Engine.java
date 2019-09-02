package com.redhat.vertx;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.redhat.vertx.pipeline.EventBusMessage;
import com.redhat.vertx.pipeline.Section;
import com.redhat.vertx.pipeline.json.YamlParser;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import com.redhat.vertx.pipeline.templates.TemplateProcessor;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
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
    private JsonObject systemConfig;
    private Map<String, JsonObject> docCache = new ConcurrentHashMap<>();
    private JinjaTemplateProcessor templateProcessor;
    private Completable initComplete;

    public Engine(String pipelineDef) {
        this(pipelineDef, new JsonObject());
    }

    public Engine(String pipelineDef, JsonObject systemConfig) {
        this.systemConfig = systemConfig;
        JsonObject jo = new JsonObject(YamlParser.parse(pipelineDef));
        this.pipeline = new Section();
        initComplete = pipeline.init(this,jo);
    }

    public EventBus getEventBus() {
        return vertx.eventBus();
    }

    public Vertx getRxVertx() {
        return vertx;
    }

    public JsonObject getSystemConfig() {
        return systemConfig;
    }

    public TemplateProcessor getTemplateProcessor() {
        if (templateProcessor == null) {
            templateProcessor = new JinjaTemplateProcessor();
        }
        return templateProcessor;
    }

    @Override
    public Completable rxStart() {

        // TODO: This should probably be something else, or something configurable so we can configure it in tests
        // TODO: Also, we don't need multiple of these being deployed
        return Completable.create(emitter -> {
            DocumentLogger documentLogger = new DocumentLogger();
            vertx.rxDeployVerticle(documentLogger, new DeploymentOptions().setWorker(true).setWorkerPoolName("document-logger")).subscribe((s, throwable) -> {
                if (throwable != null) {
                    emitter.tryOnError(throwable);
                } else {
                    emitter.onComplete();
                }
            }); // TODO dispose properly
        }).mergeWith(initComplete);
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
                pipeline.execute(uuid).subscribe(
                        result -> finishDoc(uuid, bus, changeWatcher, source, null),
                        err -> finishDoc(uuid, bus, changeWatcher, source, err),
                        () -> finishDoc(uuid, bus, changeWatcher, source, null)
                ));
    }

    private void finishDoc(String uuid, EventBus bus, MessageConsumer<Object> changeWatcher, SingleEmitter<JsonObject> source, Throwable err) {
        bus.publish(EventBusMessage.DOCUMENT_COMPLETED, uuid);
        JsonObject doc = docCache.remove(uuid);
        changeWatcher.unregister();
        if (err == null) {
            source.onSuccess(doc);
        } else {
            source.onError(err);
        }
    }


    public JsonObject getDocument(String uuid) {
        return docCache.get(uuid);
    }
}
