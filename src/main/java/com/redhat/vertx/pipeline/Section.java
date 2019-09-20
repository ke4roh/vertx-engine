package com.redhat.vertx.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Section implements Step {
    private static final Logger logger = Logger.getLogger(Section.class.getName());
    private static List<String> RESERVED_WORDS = Arrays.asList("name", "vars", "register", "steps", "class", "timeout");

    private Engine engine;
    private String name;
    private List<Step> steps;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        ServiceLoader<Step> serviceLoader = ServiceLoader.load(Step.class);
        final Optional<ServiceLoader.Provider<Step>> stepClass;

        // If the config is using the FQN for the class, load that one up
        if (def.containsKey("class")) {
            stepClass = serviceLoader.stream()
                    .filter(stepDef -> def.getString("class").equals(stepDef.type().getName()))
                    .findFirst();
        } else {
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
        }

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
        private Disposable statusChangeSubscription;

        private SectionExecutor(String documentId) {
            this.documentId = documentId;
        }

        public Maybe<JsonObject> execute() {
            if (steps.isEmpty()) {
                return Maybe.empty();
            }

            // Kick off every step.  If they need to wait, they are responsible for waiting without blocking.

            publishSectionEvent(EventBusMessage.SECTION_STARTED);
            logger.fine(() -> Thread.currentThread().getName() + " Executing all steps for section " + getName());
            stepExecutors = steps.stream().map(step -> new StepExecutor(step, documentId)).collect(Collectors.toList());
            Observable<Message<Object>> documentChanges =
                    engine.getEventBus().consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable()
                    .filter(delta -> delta.headers().get("uuid").equals(documentId)).publish().autoConnect();
            Observable<StepStatus> allStepsStatusChanges = Observable.mergeDelayError(
                    stepExecutors.stream().map(sx -> sx.executeStep(documentChanges)).collect(Collectors.toList()));
            return Maybe.create(source ->
                    statusChangeSubscription = allStepsStatusChanges.filter(stat -> stat.stopped)
                            .doOnComplete(() -> publishSectionEvent(EventBusMessage.SECTION_COMPLETED))
                            .doOnError(t -> publishSectionEvent(EventBusMessage.SECTION_ERRORED))
                            .doAfterTerminate(this::dispose)
                            .subscribe(
                                    next -> completeWhenAllStepsStopped(source),
                                    source::onError,
                                    source::onComplete));
        }

        private void publishSectionEvent(String message) {
            EventBus bus = engine.getEventBus();
            DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
            bus.publish(message, name, documentIdHeader);
        }

        private void completeWhenAllStepsStopped(MaybeEmitter<JsonObject> source) {
            if (stepExecutors.stream().allMatch(x -> x.stepStatus.stopped)) {
                logger.fine(() -> Thread.currentThread().getName() + " Completed section " + getName());
                source.onComplete();
            }
        }

        private void dispose() {
            statusChangeSubscription.dispose();
        }
    } // SectionExecutor


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
    } // StepStatus

    /**
     * The StepExecutor is an adapter between the one-off Step and the stream of statuses it emits after
     * each attempt at execution.
     *
     * Responsibilities:
     * - Maintain the progression of each step's status throughout section execution
     * - Get results from executing steps into the document
     * - Mark steps {@link StepStatus#BLOCKED} if their dependencies weren't met
     */
    private class StepExecutor {
        Collection<Disposable> disposables = new ArrayList<>();
        private Logger logger = Logger.getLogger(StepExecutor.class.getName());
        Step step;
        String documentId;
        StepStatus stepStatus;
        private ObservableEmitter<StepStatus> subscriber;
        private Observable<Message<Object>> documentChangedEventStream;
        private Disposable subscribedChangeStream;


        StepExecutor(Step step, String documentId) {
            this.step=step;
            this.documentId = documentId;
            this.stepStatus = StepStatus.NASCENT;
        }

        /**
         * Execute the step for this section.  This can only be called once for a single instance of step executor.
         * @param documentChangedEventStream A hot observable monitoring document changed events,
         *                                   which are cause to re-examine if this step might
         *                                   execute successfully
         * @return An observable which will indicate each status change of this step.
         */
        Observable<StepStatus> executeStep(Observable<Message<Object>> documentChangedEventStream) {
            assert subscriber == null;  // only once
            assert stepStatus.tryIt;
            this.documentChangedEventStream = documentChangedEventStream;
            subscribedChangeStream = addDisposable(documentChangedEventStream
                    .doOnComplete(this::noMoreChanges)
                    .subscribe(x->executeStep()));
            return Observable.<StepStatus>create(subscriber -> {this.subscriber = subscriber; this.executeStep(); } )
                    .publish().autoConnect().doAfterTerminate(this::finish);
        }

        /**
         * Execute a step.  These are the key events:
         * 1. The step executes and produces some result
         * 2. The result gets put on an event to be added to the document
         * 3. The result is added to the document, and an event is fired for the document change
         * 4. The step is complete when its change is stored in the document
         *
         * If there is no "register" for a step, then the step is complete immediately after execution.
         */
        private void executeStep() {
            if (stepStatus.tryIt && !subscriber.isDisposed()) {
                setStepStatus(StepStatus.RUNNING);
                Maybe<JsonObject> maybe = step.execute(documentId);
                addDisposable(maybe.subscribe(
                        this::processStepReturnValue,
                        this::errorToBlockedOrFailed,
                        () -> setStepStatus(StepStatus.COMPLETE)));
            }
        } // executeStep

        private void processStepReturnValue(JsonObject returnValue) {
            String register = returnValue.getMap().keySet().iterator().next();

            // register to get the doc changed event (Engine fires that)
            addDisposable(documentChangedEventStream.filter(msg -> register.equals(msg.body()))
                    .subscribe(msg -> setStepStatus(StepStatus.COMPLETE), this::errorToBlockedOrFailed));

            // fire event to change the doc (Engine listens)
            engine.getEventBus().publish(EventBusMessage.CHANGE_REQUEST, returnValue, new DeliveryOptions().addHeader("uuid", documentId));
        }

        private void setStepStatus(StepStatus newStatus) {
            logger.finest(() -> Thread.currentThread().getName() + " Step " + step.getName() + " from " + stepStatus + " to " + newStatus );
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
                finish();
            }
        }

        private void errorToBlockedOrFailed(Throwable err) {
            if (!subscribedChangeStream.isDisposed() && (err instanceof PotentiallyRecoverableException)) {
                setStepStatus(StepStatus.BLOCKED);
            } else {
                subscriber.onError(err);
                setStepStatus(StepStatus.FAILED);
            }
        }

        private Disposable addDisposable(Disposable d) {
            disposables.add(d);
            return d;
        }

        /**
         * This can happen when the document change stream closes.
         */
        private void noMoreChanges() {
            subscribedChangeStream.dispose();
        }

        private void finish() {
            subscriber.onComplete();
            disposables.forEach(Disposable::dispose);
        }
    } // StepExecutor
}
