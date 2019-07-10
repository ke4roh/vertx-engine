package com.redhat.vertx.engine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimplePipelineResolver implements PipelineResolver {
    @Override
    public CompletionStage<String> getExecutablePipelineByName(String pipelineName) {
        return CompletableFuture.completedFuture(pipelineName);
    }
}
