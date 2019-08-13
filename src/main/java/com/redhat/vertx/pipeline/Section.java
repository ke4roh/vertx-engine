package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;

public class Section implements Step {
    private Engine engine;
    private String name;
    private List<Step> steps;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Step> klass = (Class<? extends Step>) Class.forName(def.getString("class"));
            return klass.getDeclaredConstructor((Class[]) null).newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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
        // Kick off every step.  If they need to wait, they are responsible for waiting without blocking.
        EventBus bus = engine.getEventBus();
        bus.publish(EventBusMessage.SECTION_STARTED, name, new DeliveryOptions().addHeader("uuid", uuid));

        // TODO is there a more succinct way to merge all this and fire a few events when it's done?
        // e.g. Observable.concat(steps.stream().map(s -> executeStep(s,uuid).toObservable()).iterator());
        return Single.create(emitter -> {
            Completable completable = Completable.complete();
            for (Step step: steps) {
                completable=executeStep(step,uuid).mergeWith(completable);
            }
            completable.subscribe(()-> {
                // this is a section
                emitter.onSuccess(name);
                bus.publish(EventBusMessage.SECTION_COMPLETED, name, new DeliveryOptions().addHeader("uuid", uuid));
            }, (err) -> {
                emitter.tryOnError(err);
                bus.publish(EventBusMessage.SECTION_ERRORED, new JsonArray(Arrays.asList(name, err.toString())), new DeliveryOptions().addHeader("uuid", uuid));
            });
        });
    }

    /**
     *
     * @return null, since there typically isn't content resulting from the execution of a section
     */
    @Override
    public String registerResultTo() {
        return null;
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
    private Completable executeStep(Step step, String uuid) {
        return Completable.create(source -> {
            Single<Object> single = step.execute(uuid);
            single.subscribe(onSuccess -> {
                if (step.registerResultTo() != null) {
                    // register to get the doc changed event (Engine fires that)
                    EventBus bus = engine.getEventBus();
                    final List<Disposable> consumer = new ArrayList<>(1);
                    consumer.add(bus.consumer(EventBusMessage.DOCUMENT_CHANGED)
                            .toObservable()
                            .filter(msg -> step.registerResultTo().equals(msg.body())) // identify the matching doc changed event (matching)
                            .subscribe(msg -> {
                                source.onComplete(); // this step is complete
                                consumer.stream().filter(d -> { d.dispose(); return false; });
                            } , err -> {
                                source.onError(err);
                                consumer.stream().filter(d -> { d.dispose(); return false; });
                            })
                    );

                    // fire event to change the doc (Engine listens)
                    JsonObject delta =  new JsonObject().put(step.registerResultTo(),onSuccess);
                    bus.publish(EventBusMessage.CHANGE_REQUEST, delta, new DeliveryOptions().addHeader("uuid", uuid));
                } else { // No result to store, step is completed
                    source.onComplete();
                }
            }, source::onError);
        });
    }
}
