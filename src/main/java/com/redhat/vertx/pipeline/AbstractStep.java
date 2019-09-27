package com.redhat.vertx.pipeline;

import java.util.logging.Logger;

import com.redhat.vertx.Engine;
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
    private boolean initialized;

    @Override
    public Completable init(Engine engine, JsonObject config) {
        assert !initialized;
        this.engine = engine;
        name = config.getString("name");
        initialized = true;
        return Completable.complete();
    }


    /**
     * Override this if the work is non-blocking.
     *
     * @param env A {@link JsonObject} consisting of the variables for this step, plus a special one called "doc"
     *            containing the document being constructed.
     * @return a JSON-compatible object, JsonObject, JsonArray, or String
     */
    public Object executeFast(JsonObject env) {
        return null;
    }

    /**
     * Override this if the work is slow enough to need to return the result later.
     *
     * @param env A {@link JsonObject} consisting of the variables for this step, plus a special one called "doc"
     *            containing the document being constructed.
     * @return a JSON-compatible object, JsonObject, JsonArray, or String
     */
    @Override
    public Maybe<Object> execute(JsonObject env) {
        try {
            Object rval = executeFast(env);
            return (rval == null ) ? Maybe.empty() : Maybe.just(rval);
        } catch (Throwable e) {
            return Maybe.error(e);
        }
    }

    public String getName() {
        return name;
    }
}
