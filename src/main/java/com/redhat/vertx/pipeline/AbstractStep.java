package com.redhat.vertx.pipeline;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * Abstract step offers code for managing steps that might execute longer
 */
public abstract class AbstractStep implements Step {
    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected Engine engine;
    protected String name;
    protected Vertx vertx;
    private JsonObject stepConfig;
    private Duration timeout;
    private String registerTo;
    private boolean initialized;

    @Override
    public Completable init(Engine engine, JsonObject config) {
        assert !initialized;
        this.engine = engine;
        name = config.getString("name");
        stepConfig = config.getJsonObject(getShortName());

        // Just in case there isn't any additional config for a step
        stepConfig = Objects.isNull(stepConfig) ? new JsonObject() : stepConfig;

        vars = stepConfig; // This is typically correct

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
        this.vertx = engine.getRxVertx();
        try {
            return Maybe.defer(() -> executeSlow(getEnvironment(docId)))
                    .timeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .filter(x -> registerTo != null)
                    .map(x -> new JsonObject().put(registerTo, x));
        } catch (RuntimeException e) {
            return Maybe.error(e);
        }
    }

    protected JsonObject getEnvironment(String docId) {
        JsonObject vars = this.vars.copy();
        vars.put("doc", getDocument(docId));
        vars.put("system", engine.getSystemConfig());

        JsonObject stepAsJson = new JsonObject();
        stepAsJson.put("name", this.getName());
        stepAsJson.put("shortName", this.getShortName());
        stepAsJson.put("register", this.registerTo);
        stepAsJson.put("timeout", this.timeout.toString());
        stepAsJson.mergeIn(this.getVars());

        vars.put("step", stepAsJson);
        return new TemplatedJsonObject(vars, engine.getTemplateProcessor(), "doc", "system", "step");
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
     */
    public Object execute(JsonObject env) {
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
        } catch (Throwable e) {
            return Maybe.error(e);
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonObject getConfig() {
        return this.stepConfig;
    }

    @Override
    public JsonObject getVars() {
        return this.vars;
    }
}
