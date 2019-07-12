package com.redhat.vertx.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<String> process(JsonObject document, Future<String> future) {
        return CompletableFuture.completedFuture("hello, Jason!");
    }
}
