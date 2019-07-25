package com.redhat.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.redhat.vertx.pipeline.Section;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;

/**
 * Entrypoint for execution of a particular pipeline, container for the entire execution system.
 * The execute method on an engine is thread-safe and can process multiple documents at once.
 * When each document is completed,
 *
 */
public class Engine extends AbstractVerticle {
    public static final String DOC_UUID = "__uuid__";
    private Section pipeline;
    private Map<String,JsonObject> docCache = new HashMap<>();

    public Engine(String pipelineDef) {
        this.pipeline = new Section(this, new JsonArray(pipelineDef));
    }

    /**
     *
     * @param executionData
     * @return The document at the end of execution
     */
    public Single<JsonObject> execute(JsonObject executionData) {
        String uuid = UUID.randomUUID().toString();
        executionData.put(DOC_UUID, uuid);
        docCache.put(uuid, executionData);
        return Single.create(emitter ->
            pipeline.execute(uuid).subscribe(
                    o -> emitter.onSuccess(docCache.remove(uuid)),
                    exception -> {
                        exception.printStackTrace();
                        docCache.remove(uuid);
                    })
        );
    }

    public JsonObject getDoc(String uuid) {
        return docCache.get(uuid);
    }
}
