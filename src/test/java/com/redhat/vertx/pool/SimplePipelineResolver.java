package com.redhat.vertx.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;

@Mock // Used for CDI injection in the tests
@ApplicationScoped
public class SimplePipelineResolver implements PipelineResolver {
    @Override
    public CompletionStage<String> getExecutablePipelineByName(String pipelineName) {
        return CompletableFuture.completedFuture(pipelineName);
    }
}
