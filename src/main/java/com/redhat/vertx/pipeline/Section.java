package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// TODO make this a Step
public class Section {
    Engine engine;
    String name;
    List<Step> steps;

    public Section(Engine engine, JsonArray sectionDef) {
        // TODO this is a Section with a default name and default behavior
        this.engine = engine;
        this.steps = new ArrayList<>();

        // TODO: This will need to change
        final JsonObject stepDef = sectionDef.getJsonObject(0);
        this.name = stepDef.getString("name");
        steps.add(buildStep(stepDef));
    }

    public Section(Engine engine, JsonObject sectionDef) {
        this.engine = engine;

        this.name = sectionDef.getString("section");

        this.steps = sectionDef.getJsonArray("steps", new JsonArray()).stream()
                .map(s -> this.buildStep((JsonObject) s)).collect(Collectors.toList());
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

    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public Single<Object> execute(String uuid) {
        // Run through every step finding ones that are ready
        // process the steps that are ready
        // TODO figure out how to chain these events
        return Single.create(emitter -> {
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
                        nextSingle -> nextSingle.subscribe(o -> aggregator.addResult(o.toString())),
                        Throwable::printStackTrace,
                        () -> emitter.onSuccess(aggregator.getResult())
                    );
        });
//        Observable.fromIterable(steps).map( step -> {
//            // TODO this return is probably not right
//            return step.execute(uuid).subscribe(r -> {
//                JsonObject j = new JsonObject();
//                // TODO j.put(step.getRegisterKey(),r);
//                engine.getVertx().eventBus().publish("updateDoc:" + uuid, j);
//            }); // this has to store the result in te doc
//        }).subscribe(f -> {
//            // TODO done!
//        }, exception -> exception.printStackTrace()
//        );
//        // TODO finish the single only when all the steps have completed and updates are finished.
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
