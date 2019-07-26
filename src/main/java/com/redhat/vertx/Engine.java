package com.redhat.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.redhat.vertx.pipeline.Section;
import io.reactivex.Single;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Entrypoint for execution of a particular pipeline, container for the entire execution system.
 * The execute method on an engine is thread-safe and can process multiple documents at once.
 * When each document is completed,
 *
 */
public class Engine {
    public static final String DOC_UUID = "__uuid__";
    private Section pipeline;
    private Map<String,JsonObject> docCache = new HashMap<>();

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

    /**
     *
     * @param executionData
     * @return The document at the end of execution
     */
    public Single<JsonObject> execute(JsonObject executionData) {
        String uuid = UUID.randomUUID().toString();
        executionData.put(DOC_UUID, uuid);
        docCache.put(uuid, executionData);
        return Single.create(emitter -> {
            if (!emitter.isDisposed()) {
                pipeline.execute(uuid)
                        .subscribe(
                                (result, err) -> {
                                    if (err != null) {
                                        docCache.remove(uuid);
                                        emitter.onError(err);
                                    }

                                    emitter.onSuccess(docCache.remove(uuid));
                                });
            }
        });
    }

    public JsonObject getDoc(String uuid) {
        return docCache.get(uuid);
    }
}
