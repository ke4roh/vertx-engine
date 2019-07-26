package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.vertx.DocumentUpdateEvent;
import com.redhat.vertx.Engine;
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
        // TODO register into the named place on the document (event for modify doc, event for doc modified, wait for
        // doc modified before marking step complete)
        return Single.create(emitter -> {
                Observable.fromIterable(steps)
                        .map(step ->
                                step.execute(uuid).subscribe((result, err) -> {
                                    if (err != null) {
                                        err.printStackTrace();
                                        emitter.tryOnError(err);
                                    } else {
                                        final var updateEvent = new DocumentUpdateEvent(uuid, getName(), result);
                                        engine.updateDocument(updateEvent);
                                        emitter.onSuccess("complete");
                                        // Doing below pulls us out of the thread or process or something and the flow
                                        // continues on like it never receives anything, seems like this all needs to be done
                                        // in the same processing scope.
//                                        final var deliveryOptions = new DeliveryOptions().setCodecName(DocumentUpdateEvent.CODEC.name());
//                                        bus.rxSend("updateDoc", updateEvent, deliveryOptions)
//                                                .subscribe((objectMessage, throwable) -> {
//                                                    if (throwable != null) {
//                                                        emitter.tryOnError(throwable);
//                                                        return;
//                                                    }
//                                                    emitter.onSuccess(objectMessage);
//                                                });
                                    }
                                })
                        )
                        // We're done
                        .doOnComplete(() -> emitter.onSuccess(engine.getDoc(uuid)))
                        .subscribe();
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
