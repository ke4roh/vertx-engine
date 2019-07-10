package com.redhat.vertx.engine;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;


public class Engine {
    private String pipeline;

    public Engine(String pipeline) {
        this.pipeline = pipeline;
    }

    public CompletableFuture<String> execute(String pipeline) {
        return CompletableFuture.completedFuture("Woohoo!" + pipeline);
    }
}
