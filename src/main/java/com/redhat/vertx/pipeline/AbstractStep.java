package com.redhat.vertx.pipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.redhat.vertx.Engine;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

/**
 * Abstract step offers code for managing steps that might execute longer
 */
public abstract class AbstractStep implements Step {
    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected String registerTo;
    protected JsonObject vars;
    protected Engine engine;
    protected String name;
    private long timeout;
    private Map<String, MessageConsumer<Object>> retryListeners;

    @Override
    public void init(Engine engine, JsonObject config) {
        this.engine = engine;
        name = config.getString("name");
        vars = config.getJsonObject("vars", new JsonObject());
        registerTo = config.getString("register", "greeting");
        timeout = config.getLong("timeout_ms", 5000l);
        retryListeners = new ConcurrentHashMap<>();
    }

    /**
     * Responsibilities:
     * <ul>
     *     <li>Execute its step</li>
     *     <li>Defer the step whose dependencies are not met</li>
     *     <li>Retry the step whose dependencies are not met when a change happens</li>
     *     <li>Fetch the document from the engine</li>
     * </ul>
     *
     * @param uuid the key for the document being built (get it from the engine)
     * @return
     */
    @Override
    public final Single<Object> execute(String uuid) {
        return Single.create(source -> {
            execute0(uuid, source);
        });
    }

    /**
     * @param uuid   The UUID of the document to be operated on
     * @param source The SingleEmitter for the AbstractStep execution we're attmembempting to complete
     */
    private void execute0(String uuid, SingleEmitter<Object> source) {
        /**
         * The gist of this function is:
         *
         * try {
         *    executeSlow(success -> pass it back,
         *    error -> if it was StepDependencyNotMetException and we don't already have a listener, register a listener
         *    and try again.)
         *
         * } catch (StepDependencyNotMetException e) {
         *    register a listener and defer execution until after a change
         * }
         */
        Single<Object> result = executeSlow(new Environment(getDocument(uuid), vars));
        result.subscribe(resultReturn -> {
                    logger.finest(() -> "Step " + name + " returned: " + resultReturn.toString());
                    if (this.retryListeners.containsKey(uuid)) {
                        this.retryListeners.remove(uuid).unregister();
                    }
                    source.onSuccess(resultReturn);
                },
                err -> {
                    if (err instanceof StepDependencyNotMetException) {
                        if (!this.retryListeners.containsKey(uuid)) {
                            MessageConsumer<Object> retryListener = engine.getEventBus()
                                    .consumer(EventBusMessage.DOCUMENT_CHANGED, msg -> {
                                        if (msg.headers().get("uuid") != null && uuid.equals(msg.headers().get("uuid"))) {
                                            execute0(uuid, source);
                                        }
                                    });

                            this.retryListeners.put(uuid, retryListener);
                        }
                    } else if (err != null) {
                        source.tryOnError(err);
                    }
                }).dispose();
    }

    /**
     * @param uuid
     * @return The document (without local step variables) from the engine, based on the given UUID
     */
    protected JsonObject getDocument(String uuid) {
        return engine.getDocument(uuid);
    }

    /**
     * Override this if the work is non-blocking.
     *
     * @param doc
     * @return a JSON-compatible object, JsonObject, JsonArray, or String
     * @throws StepDependencyNotMetException
     */
    public Object execute(JsonObject doc) throws StepDependencyNotMetException {
        return null;
    }

    /**
     * Override this if the work is slow enough to need to return the result later.
     *
     * @param doc An {@link Environment} consisting of the document with local step variables applied
     * @return a JSON-compatible object, JsonObject, JsonArray, or String
     * @throws StepDependencyNotMetException
     */
    public Single<Object> executeSlow(JsonObject doc) {
        try {
            return Single.just(execute(doc));
        } catch (StepDependencyNotMetException e) {
            return Single.error(e);
        }
    }

    /**
     * The json-compatible object will be stored at the key named by this field.  By default, this is
     * found in the value of "register" in the step config.
     *
     * @return
     */
    @Override
    public String registerResultTo() {
        return registerTo;
    }
}
