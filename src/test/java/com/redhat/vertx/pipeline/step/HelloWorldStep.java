package com.redhat.vertx.pipeline.step;

import com.redhat.vertx.pipeline.Step;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class HelloWorldStep implements Step {

    @Override
    public void init(JsonObject config, Future<Void> future) {
        // no-op
    }

    @Override
    public Single<Object> execute(String uuid) {
        return Single.just("hello, world");
    }
}
