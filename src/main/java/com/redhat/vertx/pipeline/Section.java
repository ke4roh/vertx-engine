package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.logging.Logger;
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
    private static final Logger logger = Logger.getLogger(Section.class.getName());
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

        logger.fine(() -> Thread.currentThread().getName() + " Executing all steps for section " + getName());
        return Maybe.create(source ->
            executeExecutors(
                    source,
                    steps.stream().map(step -> new StepExecutor(step, uuid)).collect(Collectors.toList()))
        );
    }

    /**
     * Run the executors until they all complete or are stopped.
     * @param source  The MaybeEmitter for this Section's execution
     * @param executors The list of executors
     */
    private void executeExecutors(MaybeEmitter<JsonObject> source, List<StepExecutor> executors) {
        if (executors.isEmpty()) {
            return;
        }
        logger.fine(() ->Thread.currentThread().getName() + " executing pending steps in section " + getName());
        String docId = executors.stream().findAny().get().documentId;
        addDisposable(docId,Completable.concat(
                executors.stream()
                        .filter(x->x.stepStatus.tryIt)
                        .map(stepExecutor -> {
                            logger.fine(() -> Thread.currentThread().getName() + " Executing step "
                                    + stepExecutor.step.getName() + " (" + stepExecutor.stepStatus + ")");
                            stepExecutor.setStepStatus(StepStatus.RUNNING);
                            return Completable.create(stepCompleted -> {
                                addDisposable(docId, stepExecutor.executeStep().subscribe(() -> {

                                    switch (stepExecutor.stepStatus) {
                                        case COMPLETE:
                                            logger.fine(() -> Thread.currentThread().getName() + " Completed step " + stepExecutor.step.getName());
                                            if (executors.stream().anyMatch(x->x.stepStatus.tryIt)) {
                                                logger.fine(() -> Thread.currentThread().getName() + " Starting pending steps post " + stepExecutor.step.getName());
                                                executeExecutors(source, executors);
                                            }
                                            stepCompleted.onComplete();
                                            break;
                                        case FAILED:
                                            logger.fine(() -> Thread.currentThread().getName() + " Failed step " + stepExecutor.step.getName());
                                            stepCompleted.onComplete();
                                            break;
                                        case BLOCKED:
                                            logger.fine(() -> "Step " + stepExecutor.step.getName() + " blocked");
                                            if (executors.stream().allMatch(x-> x.stepStatus.stopped)) {
                                                logger.fine(() -> Thread.currentThread().getName() + " All other steps also blocked, ending.");
                                                stepCompleted.onComplete();
                                            }
                                            break;
                                        case NASCENT:
                                        case RUNNING:
                                            throw new IllegalStateException("How did we get here?");
                                    }
                                }, source::onError));
                            });
                        }).collect(Collectors.toList()))
        .subscribe(() -> {
            source.onComplete();
            executors.forEach(StepExecutor::finish);
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
        private Logger logger = Logger.getLogger(StepExecutor.class.getName());
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
                Maybe<JsonObject> maybe = step.execute(documentId);
                addDisposable(documentId,maybe.subscribe(onSuccess -> {
                    String register = onSuccess.getMap().keySet().iterator().next();

                    // register to get the doc changed event (Engine fires that)
                    EventBus bus = engine.getEventBus();
                    addDisposable(documentId,bus.consumer(EventBusMessage.DOCUMENT_CHANGED)
                            .toObservable()
                            .filter(msg -> register.equals(msg.body())) // identify the matching doc changed event (matching)
                            .subscribe(msg -> {
                                setStepStatus(StepStatus.COMPLETE);
                                source.onComplete(); // this step is complete
                            } , err -> errorToBlockedOrFailed(source, err)
                            )
                    );

                    // fire event to change the doc (Engine listens)
                    bus.publish(EventBusMessage.CHANGE_REQUEST, onSuccess, new DeliveryOptions().addHeader("uuid", documentId));
                }, err-> errorToBlockedOrFailed(source, err),
                () -> {
                    setStepStatus(StepStatus.COMPLETE);
                    source.onComplete();
                }));
            });
        } // executeStep

        private void setStepStatus(StepStatus newStatus) {
            logger.fine(() -> Thread.currentThread().getName() + " Step " + step.getName() + " from " + stepStatus + " to " + newStatus );
            try {
                switch (stepStatus) {
                    case NASCENT:
                        assert newStatus == StepStatus.RUNNING;
                        break;
                    case RUNNING:
                        assert newStatus.stopped;
                        break;
                    case BLOCKED:
                        assert newStatus == StepStatus.RUNNING;
                        break;
                    case FAILED:
                    case COMPLETE:
                        assert false;
                }
            } catch (AssertionError e) {
                String msg = "Illegal state transition for step " + step.getName() + " from " + stepStatus + " to " + newStatus;
                logger.warning(msg);
                throw new IllegalStateException(msg);
            }
            stepStatus = newStatus;
        }

        private void errorToBlockedOrFailed(CompletableEmitter source, Throwable err) {
            if (err instanceof StepDependencyNotMetException || err instanceof MissingParameterException) {
                stepStatus = StepStatus.BLOCKED;
                source.onComplete();
            } else {
                stepStatus = StepStatus.FAILED;
                source.onError(err);
            }
        }

        private void finish() {
            step.finish(documentId);
            this.finish(documentId);
        }
    }
}
