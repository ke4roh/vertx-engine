package com.redhat.vertx.pipeline;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
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
    private Duration timeout;
    private String registerTo;
    private boolean initialized;

    @Override
    public Completable init(Engine engine, JsonObject config) {
        assert !initialized;
        this.engine = engine;
        name = config.getString("name");
        vars = config.getJsonObject("vars", new JsonObject());
        timeout = Duration.parse(config.getString("timeout", "PT5.000S"));
        registerTo = config.getString("register");
        initialized = true;
        return Completable.complete();
    }

    /**
     * Responsibilities:
     * <ul>
     *     <li>Call {@link #executeSlow(JsonObject)} with the appropriate environment</li>
     *     <li>Wrap the result from {@link #executeSlow(JsonObject)} in a JsonObject</li>
     * </ul>
     *
     * @param docId the key for the document being built (get it from the engine)
     * @return a Maybe containing a JsonObject with one entry, the key equal to the "register" config object,
     * or simply complete if the step has executed without returning a value.
     */
    @Override
    public final Maybe<JsonObject> execute(String docId) {
        assert initialized;
        JsonObject env = getEnvironment(docId);
        return Maybe.create(source -> addDisposable(docId,
                executeSlow(env).timeout(timeout.toMillis(),TimeUnit.MILLISECONDS)
                .subscribe(
                rval -> {
                    if (registerTo == null) {
                        source.onComplete();
                    } else {
                        source.onSuccess(new JsonObject().put(registerTo, rval));
                    }
                },
                source::onError,
                source::onComplete
        )));
    }

    protected JsonObject getEnvironment(String docId) {
         JsonObject vars = this.vars.copy();
         vars.put("doc",getDocument(docId));
         vars.put("system", engine.getSystemConfig());
         return new TemplatedJsonObject(vars,engine.getTemplateProcessor(),"doc", "system");
    }

    /**
     * @param docId The UUID of the document under construction
     * @return The document (without local step variables) from the engine, based on the given UUID
     */
    protected JsonObject getDocument(String docId) {
        return engine.getDocument(docId);
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
                    Maybe.empty() : Maybe.just(rval);
        } catch (MissingParameterException | StepDependencyNotMetException e) {
            return Maybe.error(e);
        }
    }

    protected void addDisposable(JsonObject env, Disposable disposable) {
        super.addDisposable(env.getJsonObject("doc").getString(Engine.DOC_UUID), disposable);
    }
}
