package com.redhat.vertx.pipeline;

import com.redhat.vertx.Engine;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

    @Override
    public void init(Engine engine, JsonObject config) {
        this.engine = engine;
        name = config.getString("name");
        vars = config.getJsonObject("vars",new JsonObject());
        registerTo = config.getString("register","greeting");
        timeout = config.getLong("timeout_ms",5000l);
    }

    /**
     * Responsibilities:
     * <ul>
     *     <li>Execute its step</li>
     *     <li>Defer the step whose dependencies are not met</li>
     *     <li>Retry the step whose dependencies are not met when a change happens</li>
     *     <li>Fetch the document from the engine</li>
     * </ul>
     * @param uuid the key for the document being built (get it from the engine)
     * @return
     */
    @Override
    public final Single<Object> execute(String uuid) {
        return Single.create(source -> {
            List<MessageConsumer<Object>> listener = new ArrayList<>();
            execute0(uuid,source, listener);
        });
    }

    /**
     *
     * @param uuid The UUID of the document to be operated on
     * @param source The SingleEmitter for the AbstractStep execution we're attmembempting to complete
     * @param listener (A container for) the listener to document change events so that it can be unsubscribed
     */
    private void execute0(String uuid, SingleEmitter<Object> source, Collection<MessageConsumer<Object>> listener) {
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
        try {
            Single<Object> result = executeSlow(new Environment(getDocument(uuid),vars));
            logger.finest(() -> "Step " + name + " gave a single.");

            bulkUnregister(listener);
            result.timeout(timeout, TimeUnit.MILLISECONDS).subscribe(
                    r -> {
                        bulkUnregister(listener);
                        logger.finest(() -> "Step " + name + " completed successfully, yielding a " + r.getClass().getName());
                        source.onSuccess(r);
                    },
                    err -> {
                        if (err instanceof StepDependencyNotMetException) {
                            logger.finest(() -> "Step " + name + " dependency not met (deferred). " + err.getMessage());
                            // If the initial invocation deferred the failure, the listener isn't registered yet
                            if (listener.isEmpty()) {
                                listener.add(engine.getEventBus().consumer("documentChanged." + uuid, delta ->
                                        execute0(uuid, source, listener)
                                ));
                            }
                        } else {
                            logger.finest(() -> "Step " + name + " threw exception. " + err.getMessage());
                            bulkUnregister(listener);
                            source.onError(err);
                        }
                    });
        } catch (StepDependencyNotMetException e) {
            logger.finest(() -> "Step " + name + " dependency not met (immediate). " + e.getMessage());
            if (listener.isEmpty()) {
                listener.add(engine.getEventBus().consumer("documentChanged." + uuid, delta ->
                        execute0(uuid, source, listener)
                ));
            }
            // we'll try again with the next change
        }
    }

    /**
     * Stop listening to everything in the list and clear the collection.
     *
     * @param listener
     */
    private static void bulkUnregister(Collection<MessageConsumer<Object>> listener) {
        listener.iterator().forEachRemaining(MessageConsumer::unregister);
        listener.clear();
    }

    /**
     *
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
    public Single<Object> executeSlow(JsonObject doc) throws StepDependencyNotMetException {
        return Single.just(execute(doc));
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