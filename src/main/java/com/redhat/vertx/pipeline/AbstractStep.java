package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;

/**
 * Abstract step offers code for managing steps that might execute longer
 */
public abstract class AbstractStep extends DocBasedDisposableManager implements Step {

    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected JsonObject vars;
    protected Engine engine;
    protected String name;
    private long timeout;
    private String registerTo;

    @Override
    public void init(Engine engine, JsonObject config) {
        this.engine = engine;
        name = config.getString("name");
        vars = config.getJsonObject("vars", new JsonObject());
        timeout = config.getLong("timeout_ms", 5000l);
        registerTo = config.getString("register");
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
    public final Maybe<Object> execute(String uuid) {
        return Maybe.create(source -> {
            execute0(uuid, source, new ArrayList<Disposable>(2));
        });
    }

    /**
     * @param uuid   The UUID of the document to be operated on
     * @param source The SingleEmitter for the AbstractStep execution we're attmembempting to complete
     */
    private void execute0(String uuid, MaybeEmitter<Object> source, List<Disposable> listener) {
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
        Maybe<Object> result = executeSlow(getEnvironment(uuid));
        addDisposable(uuid,result
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .subscribe(resultReturn -> {
                    logger.finest(() -> "Step " + name + " returned: " + resultReturn.toString());
                    listener.forEach(Disposable::dispose);
                    logger.finest(() -> "Removing from listener " + System.identityHashCode(listener) + " size=" +
                            listener.size() + " disposables for step " + name + ".");
                    listener.clear();
                    source.onSuccess(resultReturn);
                },
                err -> {
                    if (err instanceof StepDependencyNotMetException) {
                        if (listener.isEmpty()) {
                            logger.finest(() -> "Step " + name + " listening for a change.");
                            listener.add(engine.getEventBus()
                                    .consumer(EventBusMessage.DOCUMENT_CHANGED).toObservable()
                                    .filter(msg -> uuid.equals(msg.headers().get("uuid")))
                                    .subscribe(msg -> execute0(uuid, source, listener)));
                        }
                    } else if (err != null) {
                        source.tryOnError(err);
                    }
                }));
    }

    private JsonObject getEnvironment(String uuid) {
         JsonObject vars = this.vars.copy();
         vars.put("doc",getDocument(uuid));
         return new TemplatedJsonObject(vars,new JinjaTemplateProcessor(),"doc");
    }

    /**
     * @param uuid The UUID of the document under construction
     * @return The document (without local step variables) from the engine, based on the given UUID
     */
    protected JsonObject getDocument(String uuid) {
        return engine.getDocument(uuid);
    }

    /**
     * Override this if the work is non-blocking.
     *
     * @param env A {@link JsonObject} consisting of the variables for this step, plus a special one called "doc"
     *            containing the document being constructed.
     * @return a JSON-compatible object, JsonObject, JsonArray, or String
     * @throws StepDependencyNotMetException if this step should be retried later after the document has been changed
     */
    public Object execute(JsonObject env) throws StepDependencyNotMetException {
        return null;
    }

    /**
     * Override this if the work is slow enough to need to return the result later.
     *
     * @param env A {@link JsonObject} consisting of the variables for this step, plus a special one called "doc"
     *            containing the document being constructed.
     * @return a JSON-compatible object, JsonObject, JsonArray, or String

     */
    protected Maybe<Object> executeSlow(JsonObject env) {
        try {
            Object rval = execute(env);
            return (rval == null || registerTo == null) ?
                    Maybe.empty() :
                    Maybe.just(new JsonObject().put(registerTo,rval));
        } catch (StepDependencyNotMetException e) {
            return Maybe.error(e);
        }
    }
}
