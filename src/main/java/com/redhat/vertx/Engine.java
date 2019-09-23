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
import io.reactivex.Observable;
import io.reactivex.Single;
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
    private Map<String, ManagedDocument> docCache = new ConcurrentHashMap<>();
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
        ManagedDocument managedDocument = new ManagedDocument(executionData);
        String documentId = managedDocument.documentId;
        docCache.put(documentId, managedDocument);

        return pipeline.execute(documentId)
                .doOnSubscribe(s-> getEventBus().publish(EventBusMessage.DOCUMENT_STARTED, documentId))
                .toSingle(docCache.get(documentId).document)
                .doOnSuccess(o -> getEventBus().publish(EventBusMessage.DOCUMENT_COMPLETED, documentId))
                .doOnError(t -> getEventBus().publish(EventBusMessage.DOCUMENT_COMPLETED, documentId))
                .doOnDispose(() -> docCache.remove(documentId));
    }

    public JsonObject getDocument(String documentId) {
        return docCache.get(documentId).document;
    }

    private class ManagedDocument {
        final JsonObject document;
        final String documentId;
        private final Observable<Message<Object>> completedChanges;

        ManagedDocument(JsonObject document) {
            this.document = document;
            documentId = UUID.randomUUID().toString();
            document.put(DOC_UUID, documentId);

            this.completedChanges = getEventBus().consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable()
                            .filter(delta -> delta.headers().get("uuid").equals(documentId)).publish().autoConnect();
        }


        Completable update(JsonObject entry) {
            final var key = entry.size() > 0 ? entry.iterator().next().getKey() : "null";
            final var deliveryOptions = new DeliveryOptions().addHeader("uuid", documentId);

            synchronized (document) {
                document.mergeIn(entry);
            }
            getEventBus().publish(EventBusMessage.DOCUMENT_CHANGED, key, deliveryOptions);
            return Completable.complete();
        }
    }
    public Completable updateDocument(String documentId, JsonObject entry) {
        return docCache.get(documentId).update(entry);
    }
}
