package com.redhat.vertx.pool;

import java.util.HashMap;
import java.util.Map;

import com.redhat.vertx.Engine;
import io.reactivex.Single;

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

    public Single<Engine> getEngineByPipelineName(String pipelineName) {
        return Single.create(emitter -> {
            resolver.getExecutablePipelineByName(pipelineName)
                    .subscribe(pipeline -> {
                                if (!map.containsKey(pipeline)) {
                                    map.put(pipeline, new Engine(pipeline));
                                }

                                emitter.onSuccess(map.get(pipeline));
                            },
                            emitter::tryOnError);
        });
    }
}
