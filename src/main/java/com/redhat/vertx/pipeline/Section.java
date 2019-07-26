package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.vertx.DocumentUpdateEvent;
import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;

// TODO make this a Step
public class Section implements Step {
    Engine engine;
    String name;
    List<Step> steps;
    EventBus bus;

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

        // TODO: Probably needs to be done somewhere else
        // Had to comment out the message, it broke stuff
//        bus = Vertx.vertx().eventBus();
//        bus.consumer("updateDoc", engine::updateDoc);
//        bus.registerCodec(DocumentUpdateEvent.CODEC);
    }

    public Single<Object> execute(String uuid) {
        // Run through every step finding ones that are ready
        // process the steps that are ready

        return Single.create(emitter -> {
            Observable observable = null;
            for (Step step: steps) {
                if (observable==null) {
                    observable=executeStep(step);
                } else {
                    observable.merge(executeStep(step,uuid));
                }
            }
            observable.subscribe((x)-> {
                // this is a section
                emitter.onSuccess(name);
            }, (err) -> {
                emitter.tryOnError(err);
            });
        });
    }

    /**
     * This is fundamentally the step executor.  When a step executes, these things happen:
     * 1. The step executes and produces some result
     * 2. The result gets put on an event to be added to the document
     * 3. The result is added to the document, and an event is fired for the document change
     * 4. The step is complete when its change is stored in the document
     *
     * If there is no "register" for a step, then the step is complete immediately after execution.
     */
    public Observable executeStep(Step step, String uuid) {
        return Observable.create(source -> {
            Single<Object> single = step.execute(uuid);
            single.subscribe(onSuccess -> {
                if (step.getRegisterTo()) {
                    // subscribe for doc changed event
                    // fire event to change the doc
                    // receive doc changed event (matching)
                }
                // complete "source"
            }, onError -> {
                // fail "source"
            });
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
