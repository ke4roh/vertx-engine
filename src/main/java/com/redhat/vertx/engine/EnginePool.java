package com.redhat.vertx.engine;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This class is responsible for instantiating engines and making them available for execution of their
 * individual pipelines as needed.
 */
@ApplicationScoped
public class EnginePool {
    public interface PipelineResolver {
        public CompletionStage<String> getExecutablePipelineByName(String pipelineName);
    }
    public final PipelineResolver resolver;

    public EnginePool() {
        this(new PipelineResolver() {
            @Override
            public CompletableFuture<String> getExecutablePipelineByName(String pipelineName) {
                return CompletableFuture.completedFuture(pipelineName);
            }
        });
    }

    public EnginePool(PipelineResolver resolver) {
        this.resolver=resolver;
    }

    private Map<String,Engine> map=new HashMap();

    public CompletionStage<Engine> engine(String pipelineName) {
        final CompletableFuture<Engine> cf = new CompletableFuture<>();
        resolver.getExecutablePipelineByName(pipelineName).whenCompleteAsync((pipeline, throwable) ->
                {
                    if (!map.containsKey(pipeline)) {
                        map.put(pipeline, new Engine(pipeline));
                    }
                    cf.complete(map.get(pipeline));
                }
        );
        return cf;
    }
}
