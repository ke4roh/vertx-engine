package com.redhat.vertx.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class SimplePipelineResolver implements PipelineResolver {
    @Override
    public CompletionStage<String> getExecutablePipelineByName(String pipelineName) {
        return CompletableFuture.completedFuture(pipelineName);
    }
}
