package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.*;
import io.reactivex.Observable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
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
        List<StepExecutor> stepExecutors = steps.stream().map(step -> new StepExecutor(step,uuid)).collect(Collectors.toList());
        Observable<Message<Object>> changes = bus.consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable().publish().autoConnect();
        Observable<StepStatus> allSteps = Observable.mergeDelayError(
                stepExecutors.stream()
                        .map(sx -> sx.executeStep(changes)).collect(Collectors.toList()));
        return Maybe.create(source ->
                addDisposable(
                        uuid,
                        allSteps.subscribe(
                                next -> {
                                    if (stepExecutors.stream().allMatch(x->x.stepStatus.stopped)) {
                                        source.onComplete();
                                    }
                                },
                                source::onError,
                                source::onComplete)));
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

    /**
     * Responsibilities:
     * - Maintain the progression of each step's status throughout section execution
     * - Get results from executing steps into the document
     * - Mark steps {@link StepStatus#BLOCKED} if their dependencies weren't met
     */
    private class StepExecutor extends DocBasedDisposableManager {
        private Logger logger = Logger.getLogger(StepExecutor.class.getName());
        Step step;
        String documentId;
        StepStatus stepStatus;
        private ObservableEmitter<StepStatus> subscriber;


        StepExecutor(Step step, String documentId) {
            this.step=step;
            this.documentId = documentId;
            this.stepStatus = StepStatus.NASCENT;
        }

        Observable<StepStatus> executeStep(Observable<Message<Object>> events) {
            assert stepStatus.tryIt;
            addDisposable(documentId,events.subscribe(x->executeStep()));
            return Observable.<StepStatus>create(subscriber -> {this.subscriber = subscriber; this.executeStep(); } )
                    .publish().autoConnect().doOnDispose(this::finish);
        }

        /**
         * Execute a step.  These are the key events:
         * 1. The step executes and produces some result
         * 2. The result gets put on an event to be added to the document
         * 3. The result is added to the document, and an event is fired for the document change
         * 4. The step is complete when its change is stored in the document
         *
         * If there is no "register" for a step, then the step is complete immediately after execution.
         *
         * The stepStatus must be set to running before this call.
         *
         */
        private void executeStep() {
            if (stepStatus.tryIt && !subscriber.isDisposed()) {
                setStepStatus(StepStatus.RUNNING);
                Maybe<JsonObject> maybe = step.execute(documentId);
                addDisposable(documentId, maybe.subscribe(
                        this::processStepReturnValue,
                        this::errorToBlockedOrFailed,
                        () -> {
                            setStepStatus(StepStatus.COMPLETE);
                        }));
            }
        } // executeStep

        private void processStepReturnValue(JsonObject returnValue) {
            String register = returnValue.getMap().keySet().iterator().next();

            // register to get the doc changed event (Engine fires that)
            EventBus bus = engine.getEventBus();
            addDisposable(documentId,bus.consumer(EventBusMessage.DOCUMENT_CHANGED)
                    .toObservable()
                    .filter(msg -> register.equals(msg.body())) // identify the matching doc changed event (matching)
                    .subscribe(msg -> setStepStatus(StepStatus.COMPLETE), this::errorToBlockedOrFailed)
            );

            // fire event to change the doc (Engine listens)
            bus.publish(EventBusMessage.CHANGE_REQUEST, returnValue, new DeliveryOptions().addHeader("uuid", documentId));
        }

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
            subscriber.onNext(stepStatus);
            if (stepStatus==StepStatus.FAILED || stepStatus==StepStatus.COMPLETE) {
                subscriber.onComplete();
            }
        }

        private void errorToBlockedOrFailed(Throwable err) {
            if (err instanceof StepDependencyNotMetException || err instanceof MissingParameterException) {
                setStepStatus(StepStatus.BLOCKED);
            } else {
                subscriber.onError(err);
                setStepStatus(StepStatus.FAILED);
            }
        }

        private void finish() {
            subscriber.onComplete();
            step.finish(documentId);
            this.finish(documentId);
        }
    }
}
