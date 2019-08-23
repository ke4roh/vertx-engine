package com.redhat.vertx.pool;

import java.util.*;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * This class is responsible for instantiating enginePool and making them available for execution of their
 * individual pipelines as needed.
 */
public class EnginePool {
    JsonObject systemConfig;
    // make this expire things or something - fancier
    PipelineResolver resolver;
    Map<String, Engine> map;
    Vertx vertx;
    List<String> verticleIds;

    public EnginePool(PipelineResolver resolver, Vertx vertx) {
        this(resolver,vertx, Collections.emptyMap());
    }

    public EnginePool(PipelineResolver resolver, Vertx vertx, Map<String,Object> systemConfig) {
        this.resolver = resolver;
        this.map = new HashMap<>();
        this.vertx = vertx;
        this.verticleIds = new ArrayList<>();
        this.systemConfig = new JsonObject(systemConfig);
    }

    public Single<Engine> getEngineByPipelineName(String pipelineName) {
        return Single.create(emitter -> {
            resolver.getExecutablePipelineByName(pipelineName)
                    .subscribe(pipeline -> {
                                Engine e = map.get(pipeline);

                                if (e == null) {
                                    e = new Engine(pipeline, systemConfig);
                                    final Engine engine=e;
                                    map.put(pipeline, e);
                                    vertx.rxDeployVerticle(e).subscribe(s -> {
                                        emitter.onSuccess(engine);
                                        verticleIds.add(s);
                                    });
                                } else {
                                    emitter.onSuccess(e);
                                }
                            },
                            emitter::tryOnError);
        });
    }

    public Completable cleanup(String verticleId) {
        return this.vertx.rxUndeploy(verticleId);
    }

    public Completable cleanupAll() {
        return Observable.fromIterable(this.verticleIds).flatMapCompletable(this::cleanup);
    }
}
