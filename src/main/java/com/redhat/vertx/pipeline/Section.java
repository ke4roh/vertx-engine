package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import io.reactivex.*;
import io.reactivex.Observable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Section implements Step {
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

    public Maybe<JsonObject> execute(String documentId) {
        return new SectionExecutor(documentId).execute();
    }

    private class SectionExecutor {
        private final String documentId;
        private List<StepExecutor> stepExecutors;

        private SectionExecutor(String documentId) {
            this.documentId = documentId;
        }

        public Maybe<JsonObject> execute() {
            if (steps.isEmpty()) {
                return Maybe.empty();
            }

            // Kick off every step, end when all steps are stopped.
            logger.fine(() -> Thread.currentThread().getName() + " Executing all steps for section " + getName());
            stepExecutors = steps.stream().map(StepExecutor::new).collect(Collectors.toList());
            Observable<Message<Object>> documentChanges =
                    engine.getEventBus().consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable()
                    .filter(delta -> delta.headers().get("uuid").equals(documentId))
                            .flatMap(objectMessage -> {
                                if (stepExecutors.stream().noneMatch(x -> x.trying)) {
                                    return Observable.empty();
                                } else {
                                    return Observable.just(objectMessage);
                                }
                            })
                            .doOnNext(n -> logger.finest(() -> "Section " + name + " changes flowable next: " + n.body()))
                            .doOnComplete(() -> logger.finest(() -> "Section " + name + " no executors working."))
                            .publish().autoConnect();

            return Completable.mergeDelayError(
                    stepExecutors.stream().map(sx -> sx.executeStep(documentId,documentChanges))
                            .collect(Collectors.toList())
            )
                    .doOnSubscribe(s -> publishSectionEvent(EventBusMessage.SECTION_STARTED))
                    .doOnComplete(() -> publishSectionEvent(EventBusMessage.SECTION_COMPLETED))
                    .doOnError(t -> publishSectionEvent(EventBusMessage.SECTION_ERRORED))
                    .toMaybe();
        }

        private void publishSectionEvent(String message) {
            EventBus bus = engine.getEventBus();
            DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
            bus.publish(message, name, documentIdHeader);
        }

    } // SectionExecutor


    /**
     * The StepExecutor is an adapter between the one-off Step and the stream of statuses it emits after
     * each attempt at execution.
     *
     * Responsibilities:
     * - Maintain the progression of each step's status throughout section execution
     * - Get results from executing steps into the document
     */
     private class StepExecutor {
        boolean trying = true;
        final Step step;

        private StepExecutor(Step step) {
            this.step = step;
        }

        private void setTrying(boolean iThinkICan, Supplier<String> message) {
            finest(message);
            setTrying(iThinkICan);
        }

        private void setTrying(boolean iThinkICan) {
            this.trying = iThinkICan;
        }

        private void finest(Supplier<String> message) {
            logger.finest(() -> "Step " + step.getName() + ": " + message.get());
        }

        Completable executeStep(String documentId, Observable<Message<Object>> changes) {
            return step.execute(documentId)
                    .retryWhen(errors -> errors.flatMap(err-> {
                            setTrying(false);
                            if (err instanceof PotentiallyRecoverableException) {
                                finest(() -> "failed, retrying " + err);
                                return Flowable.just(err);
                            } else {
                                finest(() -> "execution halted. Unrecoverable exception " + err);
                                return Flowable.error(err);
                            }
                        }) // flatMap
                            .zipWith(changes.toFlowable(BackpressureStrategy.LATEST),(err,change) -> change)
                            .doOnNext(n -> setTrying(true,() -> " proceeding after change " + n.body()))
                            .doOnComplete(() -> finest(() -> "finished retries"))
                    ) // retry
                    .flatMap(returnValue -> {  // Convert the return value into a Maybe which completes when the change is recorded
                                String register = returnValue.getMap().keySet().stream().findFirst().get();

                                Maybe<?> changeRecorded = changes
                                        .filter(msg -> register.equals(msg.body()))
                                        .doOnNext(m -> finest(() -> "completed change recording.")).firstElement()
                                        .doOnComplete(() -> setTrying(false, () -> "success"));

                                engine.getEventBus()
                                        .publish(EventBusMessage.CHANGE_REQUEST, returnValue,
                                                new DeliveryOptions().addHeader("uuid", documentId));

                                return changeRecorded;
                            }
                    )
                    .doOnComplete(() -> finest(() -> " completed"))
                    .ignoreElement();
        }

    } // StepExecutor
}
