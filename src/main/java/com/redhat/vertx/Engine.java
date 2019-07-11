package com.redhat.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Entrypoint for execution of a particular pipeline.
 *
 */
public class Engine {
    private String pipeline;

    public Engine(String pipeline) {
        this.pipeline = pipeline;
    }

    public CompletionStage<String> execute(String pipeline) {
        return CompletableFuture.completedFuture("Woohoo!" + pipeline);
    }
}
