package com.redhat.vertx.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
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
        this.engine = engine;

        // TODO this is a Section with a default name and default behavior
    }

    public Section(Engine engine, JsonObject sectionDef) {
        this.engine = engine;

        this.name = sectionDef.getString("section");

        this.steps = sectionDef.getJsonArray("steps", new JsonArray()).stream()
                .map(s -> {
                    try {
                        JsonObject stepDef = (JsonObject) s;
                        Class<Step> klass = (Class<Step>) Class.forName(((JsonObject) stepDef).getString("class"));
                        return klass.getDeclaredConstructor((Class[]) null).newInstance();
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
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
        Observable.fromIterable(steps).map( step -> {
            // TODO this return is probably not right
            return step.execute(uuid).subscribe(r -> {
                JsonObject j = new JsonObject();
                // TODO j.put(step.getRegisterKey(),r);
                engine.getVertx().eventBus().publish("updateDoc:" + uuid, j);
            }); // this has to store the result in te doc
        }).subscribe(f -> {
            // TODO done!
        }, exception -> exception.printStackTrace()
        );
        // TODO finish the single only when all the steps have completed and updates are finished.
    }

    /**
     * Store values somewhere besides memory. Maybe this sends a vertx message.
     * @param result
     */
    private void storeStepResult(StepReturnValue result) {
        System.out.println("A Step completed with the result: " + result.returnValue.toString());
    }
}
