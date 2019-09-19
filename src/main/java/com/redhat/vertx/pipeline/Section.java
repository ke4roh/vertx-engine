package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
            IncompleteSectionMonitor sectionMonitor = new IncompleteSectionMonitor();
            Observable<Message<Object>> documentChanges =
                    engine.getEventBus().consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable()
                    .filter(delta -> delta.headers().get("uuid").equals(documentId))
                            .doOnNext(n -> logger.finest(() -> "Section " + name + " changes flowable next: " + n.body()))
                    .doAfterNext(sectionMonitor::doAfterNext);

            Observable<Message<Object>> sectionFinishesIncomplete = sectionMonitor.getSectionFinishesIncomplete();

            Observable<Message<Object>> documentChangesThisSection =
                    Observable.amb(Arrays.asList(documentChanges,sectionFinishesIncomplete)).publish().autoConnect();

            return Completable.mergeDelayError(
                    stepExecutors.stream().map(sx -> sx.executeStep(documentId,documentChangesThisSection))
                            .collect(Collectors.toList())
            )
                    .doOnSubscribe(s -> publishSectionEvent(EventBusMessage.SECTION_STARTED))
                    .doOnComplete(() -> publishSectionEvent(EventBusMessage.SECTION_COMPLETED))
                    .doOnError(t -> publishSectionEvent(EventBusMessage.SECTION_ERRORED)).toMaybe();

        }

        private class IncompleteSectionMonitor {
            private ObservableEmitter<Long> subscriber;

            private Observable<Message<Object>> getSectionFinishesIncomplete() {
                assert subscriber == null;
                AtomicLong working = new AtomicLong(stepExecutors.size());
                return Observable.<Long>create(subscriber -> this.subscriber = subscriber)
                        .filter(nowWorking -> {
                            long oldWorking = working.getAndSet(nowWorking);
                            return oldWorking >= nowWorking && nowWorking > 0;
                        })
                        .doOnNext(n -> logger.fine(() -> "Section " + name + " stopped processing with " + n + " steps not done."))
                        .ignoreElements()
                        .<Message<Object>>toObservable()
                        .doOnComplete(() -> logger.fine(() -> "Section " + name + " should close momentarily."))
                        .publish().autoConnect();
            }

            void doAfterNext(Object n) {
                subscriber.onNext(stepExecutors.stream().filter(x -> x.status.working).count());
            }
        }

        private void publishSectionEvent(String message) {
            logger.finest("Section " + message);
            EventBus bus = engine.getEventBus();
            DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
            bus.publish(message, name, documentIdHeader);
        }

    } // SectionExecutor

    enum StepStatus {
        NASCENT(true), RUNNING(true), RETRYING(false), WRITING(false), COMPLETE(false);

        final boolean working;

        StepStatus(boolean working) {
            this.working = working;
        }
    }

    /**
     * The StepExecutor is an adapter between the one-off Step and the stream of statuses it emits after
     * each attempt at execution.
     *
     * Responsibilities:
     * - Maintain the progression of each step's status throughout section execution
     * - Get results from executing steps into the document
     */
     private class StepExecutor {
        StepStatus status = StepStatus.NASCENT;
        final Step step;

        private StepExecutor(Step step) {
            this.step = step;
        }

        private void setStatus(StepStatus newStatus, Supplier<String> message) {
            finest(message);
            setStatus(newStatus);
        }

        private void setStatus(StepStatus newStatus) {
            this.status = newStatus;
        }

        private void finest(Supplier<String> message) {
            logger.finest(() -> "Step " + step.getName() + ": " + message.get());
        }

        Completable executeStep(String documentId, Observable<Message<Object>> changes) {

            return step.execute(documentId)
                    .doOnSubscribe(s -> setStatus(StepStatus.RUNNING, () -> " started execution"))
                    .retryWhen(errors -> errors.flatMap(err-> {
                            if (err instanceof PotentiallyRecoverableException) {
                                setStatus(StepStatus.RETRYING, () -> "failed, retrying " + err);
                                return Flowable.just(err);
                            } else {
                                setStatus(StepStatus.COMPLETE, (() -> "execution halted. Unrecoverable exception " + err));
                                return Flowable.error(err);
                            }
                        }) // flatMap
                            .zipWith(changes.toFlowable(BackpressureStrategy.LATEST),(err,change) -> change)
                            .doOnComplete(() -> finest(() -> "finished retries"))
                    ) // retry
                    .flatMap(returnValue -> {  // Convert the return value into a Maybe which completes when the change is recorded
                            setStatus(StepStatus.WRITING);
                            String register = returnValue.getMap().keySet().stream().findFirst().get();

                            Maybe<?> changeRecorded = changes
                                    .doOnNext(n -> finest(() -> "considering " + n.body()))
                                    .filter(msg -> register.equals(msg.body()))
                                    .doOnNext(n -> finest(() -> "Saw " + n.body() + " registered."))
                                    .firstElement().ignoreElement().toMaybe()
                                    .doOnComplete(() -> setStatus(StepStatus.COMPLETE, () -> "first complete"));

                            engine.getEventBus()
                                    .publish(EventBusMessage.CHANGE_REQUEST, returnValue,
                                            new DeliveryOptions().addHeader("uuid", documentId));

                            return changeRecorded;
                        }
                    )
                    .doOnComplete(() -> setStatus(StepStatus.COMPLETE, () -> "second complete"))
                    .ignoreElement();
        }

    } // StepExecutor
}
