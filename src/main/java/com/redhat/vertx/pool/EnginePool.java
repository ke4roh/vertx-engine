package com.redhat.vertx.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.redhat.vertx.Engine;

/**
 * This class is responsible for instantiating enginePool and making them available for execution of their
 * individual pipelines as needed.
 */
public class EnginePool {
    // make this expire things or something - fancier
    PipelineResolver resolver;
    Map<String, Engine> map;

    public EnginePool(PipelineResolver resolver) {
        this.resolver = resolver;
        this.map = new HashMap<>();
    }

    public CompletionStage<Engine> getEngineByPipelineName(String pipelineName) {
        return resolver.getExecutablePipelineByName(pipelineName)
                .thenComposeAsync(pipeline -> {
                    if (!map.containsKey(pipeline)) {
                        map.put(pipeline, new Engine(pipeline));
                    }

                    return CompletableFuture.completedFuture(map.get(pipeline));
                });
    }
}
