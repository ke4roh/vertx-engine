package com.redhat.vertx.engine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class Engine {
    private String pipeline;

    public Engine(String pipeline) {
        this.pipeline = pipeline;
    }

    public CompletionStage<String> execute(String pipeline) {
        return CompletableFuture.completedFuture("Woohoo!" + pipeline);
    }
}
