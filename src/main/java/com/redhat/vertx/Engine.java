package com.redhat.vertx;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.redhat.vertx.pipeline.EventBusMessage;
import com.redhat.vertx.pipeline.Section;
import com.redhat.vertx.pipeline.json.YamlParser;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import com.redhat.vertx.pipeline.templates.TemplateProcessor;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;

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
        DocumentLogger documentLogger = new DocumentLogger();
        return vertx.rxDeployVerticle(documentLogger, new DeploymentOptions().setWorker(true)
                .setWorkerPoolName("document-logger")).ignoreElement().mergeWith(initComplete);
    }

    /**
     *
     * @param executionData The document to process
     * @return A single which will provide the document at the end of execution
     */
    public Single<JsonObject> execute(JsonObject executionData) {
        String documentId = UUID.randomUUID().toString();
        executionData.put(DOC_UUID, documentId);

        docCache.put(documentId, executionData);
        EventBus bus = getEventBus();

        AtomicReference<Disposable> changeSub = new AtomicReference<>();

        return pipeline.execute(documentId)
                .doOnSubscribe(s-> bus.publish(EventBusMessage.DOCUMENT_STARTED, documentId))
                .doOnSubscribe(s -> DisposableHelper.set(changeSub,
                        bus.<JsonObject>consumer(EventBusMessage.CHANGE_REQUEST).toObservable()
                                .filter(delta -> documentId.equals(delta.headers().get("uuid")))
                                .doOnNext(this::processChangeRequest)
                                .subscribe()))
                .toSingle(docCache.get(documentId))
                .doOnSuccess(o -> bus.publish(EventBusMessage.DOCUMENT_COMPLETED, documentId))
                .doOnError(t -> bus.publish(EventBusMessage.DOCUMENT_COMPLETED, documentId))
                .doOnDispose(() -> { DisposableHelper.dispose(changeSub); docCache.remove(documentId); });
    }

    private void processChangeRequest(Message<JsonObject> delta) {
        assert delta.body().size() == 1;
        final var docUuid = delta.headers().get("uuid");
        final var body = delta.body();
        final var deliveryOptions = new DeliveryOptions().addHeader("uuid", docUuid);

        docCache.get(docUuid).mergeIn(body);
        getEventBus().publish(EventBusMessage.DOCUMENT_CHANGED, body.iterator().next().getKey(), deliveryOptions);
    }

    public JsonObject getDocument(String documentId) {
        return docCache.get(documentId);
    }
}
