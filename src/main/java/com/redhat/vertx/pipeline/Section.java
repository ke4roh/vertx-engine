package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.vertx.Engine;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// TODO make this a Step
public class Section implements Step {
    Engine engine;
    String name;
    List<Step> steps;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        try {
            Class<Step> klass = (Class<Step>) Class.forName(def.getString("class"));
            return klass.getDeclaredConstructor((Class[]) null).newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public void init(Engine engine, JsonObject config) {
        this.engine = engine;
        this.name = config.getString("name","default");
        List<Step> steps = new ArrayList<Step>();
        for (Object stepConfig : config.getJsonArray("steps", new JsonArray())) {
            Step s = buildStep((JsonObject)stepConfig);
            s.init(engine,(JsonObject)stepConfig);
            steps.add(s);
        }
        this.steps=Collections.unmodifiableList(steps);
    }

    public Single<Object> execute(String uuid) {
        // Run through every step finding ones that are ready
        // process the steps that are ready
        // TODO register into the named place on the document (event for modify doc, event for doc modified, wait for
        // doc modified before marking step complete)
        return Single.create(emitter -> {
                    if (!emitter.isDisposed()) {
                        ExecuteAggregator aggregator = new ExecuteAggregator("");
                        Observable.fromIterable(steps)
                                .map(step ->
                                        step.execute(uuid)
                                                .doOnSuccess(result -> {
                                                    JsonObject j = new JsonObject();
                                                    // TODO j.put(step.getRegisterKey(),r);
                                                    engine.getDoc(uuid).put(getName(), result);
//                                    engine.getVertx().eventBus().publish("updateDoc:" + uuid, j);
                                                })
                                                .doOnError(err -> {
                                                    err.printStackTrace();
                                                    emitter.onError(err);
                                                }))
                                .subscribe(
                                        nextSingle -> nextSingle.subscribe(o -> aggregator.addResult(o.toString())).dispose(),
                                        err -> {
                                            err.printStackTrace();
                                            emitter.onError(err);
                                        },
                                        () -> emitter.onSuccess(aggregator.getResult())
                                );
                    }
                });
    }

    class ExecuteAggregator {
        private String result;

        public ExecuteAggregator(String startingResult) {
            this.result = startingResult;
        }

        public void addResult(String newResult) {
            this.result = result.concat(newResult);
        }

        public String getResult() {
            return result;
        }
    }
}
