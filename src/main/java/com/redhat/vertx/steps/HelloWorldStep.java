package com.redhat.vertx.steps;

import com.redhat.vertx.steps.Step;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class HelloWorldStep implements Step<String> {

    @Override
    public void init(JsonObject config, Future<Void> future) {
        // no-op
    }

    @Override
    public boolean isReady(JsonObject doc) {
        return true;
    }

    @Override
    public void process(JsonObject document, Future<String> future) {
        return new Future("hello, Jason!");
    }
}
