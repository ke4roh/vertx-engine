package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Section extends DocBasedDisposableManager implements Step {
    private Engine engine;
    private String name;
    private List<Step> steps;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        ServiceLoader<Step> serviceLoader = ServiceLoader.load(Step.class);
        final var stepClass = serviceLoader.stream()
                .filter(stepDef -> def.getString("class").equals(stepDef.type().getName()))
                .findFirst();

        if (stepClass.isPresent()) {
            return stepClass.get().get();
        } else {
            throw new RuntimeException("Error locating " + def.getString("class"));
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

    public Maybe<Object> execute(String uuid) {
        // Kick off every step.  If they need to wait, they are responsible for waiting without blocking.
        EventBus bus = engine.getEventBus();
        bus.publish(EventBusMessage.SECTION_STARTED, name, new DeliveryOptions().addHeader("uuid", uuid));

        return Maybe.create(emitter -> {
            addDisposable(uuid,
                    Completable.concat(
                            steps.stream().map(step -> executeStep(step,uuid)).collect(Collectors.toList())
                    ).subscribe(()-> {
                        // this is a section
                        emitter.onComplete();
                        bus.publish(EventBusMessage.SECTION_COMPLETED,
                                name,
                                new DeliveryOptions().addHeader("uuid", uuid)
                        );
                    }, (err) -> {
                        emitter.tryOnError(err);
                        bus.publish(EventBusMessage.SECTION_ERRORED,
                                new JsonArray(Arrays.asList(name, err.toString())),
                                new DeliveryOptions().addHeader("uuid", uuid)
                        );
                    }));
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
    private Completable executeStep(Step step, String uuid) {
        return Completable.create(source -> {
            Maybe<Object> maybe = step.execute(uuid);
            addDisposable(uuid,maybe.subscribe(onSuccess -> {
                JsonObject jo = (JsonObject)onSuccess;
                String register = jo.getMap().keySet().iterator().next();

                // register to get the doc changed event (Engine fires that)
                EventBus bus = engine.getEventBus();
                addDisposable(uuid,bus.consumer(EventBusMessage.DOCUMENT_CHANGED)
                        .toObservable()
                        .filter(msg -> register.equals(msg.body())) // identify the matching doc changed event (matching)
                        .subscribe(msg -> {
                            source.onComplete(); // this step is complete
                            step.finish(uuid);
                        } , err -> {
                            source.onError(err);
                            step.finish(uuid);
                        })
                );

                // fire event to change the doc (Engine listens)
                bus.publish(EventBusMessage.CHANGE_REQUEST, onSuccess, new DeliveryOptions().addHeader("uuid", uuid));
            }, source::onError, () -> {
                source.onComplete();
                step.finish(uuid);
            }));
        });
    }
}
