package com.redhat.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.redhat.vertx.pipeline.Section;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

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

    /**
     *
     * @param executionData
     * @return The document at the end of execution
     */
    public Single<JsonObject> execute(JsonObject executionData) {
        String uuid = UUID.randomUUID().toString();
        executionData.put(DOC_UUID, uuid);
        docCache.put(uuid, executionData);
        EventBus bus = getEventBus();
        bus.publish("documentStarted", uuid);
        MessageConsumer<Object> changeWatcher = bus.consumer("changeRequest." + uuid, delta -> {
            JsonObject body = (JsonObject) delta.body();
            assert body.size() == 1;
            docCache.get(uuid).mergeIn(body);
            bus.publish("documentChanged." + uuid, body.iterator().next().getKey());
        });

        return Single.create(source ->
                pipeline.execute(uuid).subscribe((result, err) -> {
                    bus.publish("documentCompleted", uuid);
                    JsonObject doc = docCache.remove(uuid);
                    changeWatcher.unregister();
                    if (err != null) {
                        source.onError(err);
                    } else {
                        source.onSuccess(doc);
                    }
                }));
    }
}
