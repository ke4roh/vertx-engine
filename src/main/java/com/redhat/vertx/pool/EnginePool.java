package com.redhat.vertx.pool;

import java.util.HashMap;
import java.util.Map;

import com.redhat.vertx.Engine;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;

/**
 * This class is responsible for instantiating enginePool and making them available for execution of their
 * individual pipelines as needed.
 */
public class EnginePool {
    // make this expire things or something - fancier
    PipelineResolver resolver;
    Map<String, Engine> map;
    Vertx vertx;

    public EnginePool(PipelineResolver resolver, Vertx vertx) {
        this.resolver = resolver;
        this.map = new HashMap<>();
        this.vertx = vertx;
    }

    public Single<Engine> getEngineByPipelineName(String pipelineName) {
        return Single.create(emitter -> {
            resolver.getExecutablePipelineByName(pipelineName)
                    .subscribe(pipeline -> {
                                Engine e = map.get(pipeline);

                                if (e == null) {
                                    e = new Engine(pipeline);
                                    final Engine engine=e;
                                    map.put(pipeline, e);
                                    vertx.rxDeployVerticle(e).subscribe(s -> emitter.onSuccess(engine));
                                    // TODO either make Engine a non-verticle, or find some way to un-register it later
                                } else {
                                    emitter.onSuccess(e);
                                }
                            },
                            emitter::tryOnError);
        });
    }
}
