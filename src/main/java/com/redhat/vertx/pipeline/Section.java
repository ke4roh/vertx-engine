package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;

import io.reactivex.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Section implements Step {
    private static final Logger logger = Logger.getLogger(Section.class.getName());
    private static List<String> RESERVED_WORDS = Arrays.asList("name", "vars", "register", "steps", "class", "timeout");

    private Engine engine;
    private String name;
    private List<Step> steps;
    private JsonObject stepConfig;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        ServiceLoader<Step> serviceLoader = ServiceLoader.load(Step.class);
        final Optional<ServiceLoader.Provider<Step>> stepClass;

        var allShortNames = serviceLoader.stream()
                .collect(Collectors.toMap(
                        provider -> provider.get().getShortName(),
                        Optional::of));

        // Get all the config keys, strip out reserved words
        final var defKeys = def.getMap().keySet();
        defKeys.removeAll(RESERVED_WORDS);

        // We had more than the short name of the step, error
        if (defKeys.size() > 1) {
            throw new RuntimeException("Unknown keys in configuration");
        }

        // We should only have one entry, use that for the sort name to class mapping
        stepClass = allShortNames.getOrDefault(defKeys.toArray()[0].toString(), Optional.empty());

        // Return the class found or error
        if (stepClass.isPresent()) {
            return stepClass.get().get();
        } else {
            throw new RuntimeException("Error locating step implementation");
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
        this.stepConfig = config.getJsonObject(getShortName(), config);

        stepConfig.getJsonArray("steps", new JsonArray()).forEach( stepConfig -> {
            Step s = buildStep((JsonObject)stepConfig);
            stepCompletables.add(s.init(engine,(JsonObject)stepConfig));
            steps.add(s);
        });
        this.steps=Collections.unmodifiableList(steps);
        return Completable.merge(stepCompletables);
    }

    public Maybe<JsonObject> execute(String documentId) {
        return executeSectionPass(steps.stream().map(s -> new StepExecutor(s,documentId)).collect(Collectors.toList()))
                .ignoreElements()
                .doOnSubscribe(s -> publishSectionEvent(documentId, EventBusMessage.SECTION_STARTED))
                .doOnComplete(() -> publishSectionEvent(documentId, EventBusMessage.SECTION_COMPLETED))
                .doOnError(t -> publishSectionEvent(documentId, EventBusMessage.SECTION_ERRORED))
                .toMaybe();
    }

    @Override
    public JsonObject getStepConfig() {
        return new JsonObject();
    }

    private void publishSectionEvent(String documentId, String message) {
        logger.finest("Section " + message);
        EventBus bus = engine.getEventBus();
        DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
        bus.publish(message, name, documentIdHeader);
    }

    private Flowable<List<Section.StepExecutor>> executeSectionPass(List<StepExecutor> steps) {
        return Single.mergeDelayError(steps.stream().map(StepExecutor::execute).collect(Collectors.toList()))
                .filter(s -> s == StepStatus.COMPLETE)
                .filter(s -> steps.size() > 1)
                .map(s -> steps.stream().filter(x -> x.state.get().tryIt).collect(Collectors.toList()))
                .filter(l -> !l.isEmpty() && l.size() < steps.size())
                .flatMap(this::executeSectionPass)
                .doOnSubscribe(s -> logger.fine(() -> "Starting section " + name +" execution pass with " + steps.size() + " steps. "
                 + steps.stream().map(step -> step.step.getName()).collect(Collectors.joining(","))))
                .doOnComplete(() -> logger.fine(() -> "Finished section " + name +" execution pass with " + steps.size() + " steps. "
                        + steps.stream().map(step -> step.step.getName()).collect(Collectors.joining(","))));
    }

    enum StepStatus {
        NASCENT(true), RUNNING(false), WAITING(true), COMPLETE(false), ERROR(false);

        private final boolean tryIt;

        StepStatus(boolean tryIt) {
            this.tryIt = tryIt;
        }
    } // StepStatus

    private class StepExecutor {
        private final Step step;
        private final AtomicReference<StepStatus> state;
        private final String documentId;

        StepExecutor(Step step, String documentId) {
            this.step = step;
            this.state = new AtomicReference<>(StepStatus.NASCENT);
            this.documentId = documentId;
        }

        /**
         * Run the step if it is not running already.
         * @return A Single indicating the status of the step at completion (if it was not already
         * running), or at the moment if it was already underway when called.
         */
        Single<StepStatus> execute() {
            StepStatus newStatus = state.updateAndGet(
                    startingState -> startingState.tryIt?StepStatus.RUNNING:startingState
            );
            if (newStatus != StepStatus.RUNNING) {
                // Only send COMPLETE from the one that actually completes the work.
                return Single.just(newStatus == StepStatus.COMPLETE?StepStatus.RUNNING:newStatus);
            }

            return step.execute(documentId)
                    .flatMap(
                            Maybe::just,
                            throwable -> {
                                if (throwable instanceof PotentiallyRecoverableException) {
                                    state.set(StepStatus.WAITING);
                                    return Maybe.empty();
                                } else {
                                    state.set(StepStatus.ERROR);
                                    return Maybe.error(throwable);
                                }
                            },
                            () -> { state.set(StepStatus.COMPLETE); return Maybe.empty(); }
                    )
                    .flatMapCompletable(jsonObject -> { // Register the change
                        assert jsonObject.size() == 1;
                        return engine.updateDocument(documentId,jsonObject)
                                .doOnComplete(() -> state.set(StepStatus.COMPLETE))
                                .timeout(50,TimeUnit.MILLISECONDS);
                    }).toSingle(state::get)
                    .doOnSubscribe(sub -> logger.finest(() -> "Step " + step.getName() + " starting"))
                    .doOnError(t -> logger.fine(() -> "Step " + step.getName() + " errored: " + t.toString()))
                    .doOnSuccess(stat -> logger.finest(() -> "Step " + step.getName() + " in state " + state));
        }
    } // StepExecutor
}
