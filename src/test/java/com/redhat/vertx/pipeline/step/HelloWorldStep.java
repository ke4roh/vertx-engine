package com.redhat.vertx.pipeline.step;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.Step;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public class HelloWorldStep implements Step {
    private String name;

    @Override
    public void init(Engine engine, JsonObject config) {
        name = config.getJsonObject("vars",new JsonObject()).getString("name","world");
    }

    @Override
    public Single<Object> execute(String uuid) {
        return Single.just("hello, " + name);
    }
}
