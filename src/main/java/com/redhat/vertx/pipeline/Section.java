package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
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
    public Completable init(Engine engine, JsonObject config) {
        this.engine = engine;
        this.name = config.getString("name","default");
        List<Completable> stepCompletables = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        config.getJsonArray("steps", new JsonArray()).forEach( stepConfig -> {
            Step s = buildStep((JsonObject)stepConfig);
            stepCompletables.add(s.init(engine,(JsonObject)stepConfig));
            steps.add(s);
        });
        this.steps=Collections.unmodifiableList(steps);
        return Completable.merge(stepCompletables);
    }

    public Maybe<JsonObject> execute(String uuid) {
        // Kick off every step.  If they need to wait, they are responsible for waiting without blocking.
        EventBus bus = engine.getEventBus();
        bus.publish(EventBusMessage.SECTION_STARTED, name, new DeliveryOptions().addHeader("uuid", uuid));

        return Maybe.create(source ->
            executeExecutors(
                    source,
                    steps.stream().map(step -> new StepExecutor(step, uuid)).collect(Collectors.toList()),
                    0)
        );
    }

    private void executeExecutors(MaybeEmitter<JsonObject> source, List<StepExecutor> executors, long stepsCompleted) {
        addDisposable(executors.stream().findAny().get().documentId,Completable.concat(
                executors.stream()
                        .filter(x->x.stepStatus.tryIt)
                        .map(StepExecutor::executeStep)
                        .collect(Collectors.toList())
        ).subscribe(() -> {
            long newStepsCompleted = executors.stream().filter(x -> x.stepStatus.stopped).count();
            if ((newStepsCompleted > stepsCompleted) && (stepsCompleted < steps.size())) {
                executeExecutors(source,executors,newStepsCompleted);
            } else {
                executors.forEach(x->x.finish());
                source.onComplete();
            }
        }));
    }

    enum StepStatus {
        //       stopped, tryIt
        NASCENT (false  , true),
        RUNNING (false  , false),
        COMPLETE(true   , false),
        BLOCKED (true   , true),
        FAILED  (true   , false);

        final boolean stopped;
        final boolean tryIt;
        StepStatus(boolean stopped, boolean tryIt) {
            this.stopped = stopped;
            this.tryIt = tryIt;
        }
    }

    private class StepExecutor extends DocBasedDisposableManager {
        Step step;
        String documentId;
        StepStatus stepStatus;

        public StepExecutor(Step step, String documentId) {
            this.step=step;
            this.documentId = documentId;
            this.stepStatus = StepStatus.NASCENT;
        }

        /**
         * This is fundamentally the step executor.  When a step executes, these things happen:
         * 1. The step executes and produces some result
         * 2. The result gets put on an event to be added to the document
         * 3. The result is added to the document, and an event is fired for the document change
         * 4. The step is complete when its change is stored in the document
         *
         * If there is no "register" for a step, then the step is complete immediately after execution.
         *
         * @return a Completable which will indicate that the stepStatus should be re-evaluated, or an exception
         * if it has failed.
         */
        Completable executeStep() {
            return Completable.create(source -> {
                stepStatus = StepStatus.RUNNING;
                Maybe<JsonObject> maybe = step.execute(documentId);
                addDisposable(documentId,maybe.subscribe(onSuccess -> {
                    String register = onSuccess.getMap().keySet().iterator().next();

                    // register to get the doc changed event (Engine fires that)
                    EventBus bus = engine.getEventBus();
                    addDisposable(documentId,bus.consumer(EventBusMessage.DOCUMENT_CHANGED)
                            .toObservable()
                            .filter(msg -> register.equals(msg.body())) // identify the matching doc changed event (matching)
                            .subscribe(msg -> {
                                stepStatus = StepStatus.COMPLETE;
                                source.onComplete(); // this step is complete
                                step.finish(documentId);
                            } , err -> errorToBlockedOrFailed(source, err)
                            )
                    );

                    // fire event to change the doc (Engine listens)
                    bus.publish(EventBusMessage.CHANGE_REQUEST, onSuccess, new DeliveryOptions().addHeader("uuid", documentId));
                }, err-> errorToBlockedOrFailed(source, err),
                () -> {
                    stepStatus = StepStatus.COMPLETE;
                    source.onComplete();
                    step.finish(documentId);
                }));
            });
        } // executeStep

        private void errorToBlockedOrFailed(CompletableEmitter source, Throwable err) {
            if (err instanceof StepDependencyNotMetException || err instanceof MissingParameterException) {
                stepStatus = StepStatus.BLOCKED;
                source.onComplete();
            } else {
                stepStatus = StepStatus.FAILED;
                source.onError(err);
                step.finish(documentId);
            }
        }

        private void finish() {
            step.finish(documentId);
            this.finish(documentId);
        }
    }
}
