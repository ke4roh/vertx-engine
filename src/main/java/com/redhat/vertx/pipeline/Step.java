package com.redhat.vertx.steps;


// Java Concurrent or vert.x rx?
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * A step is the abstract unit of work that can be performed within the Engine.
 * Steps are responsible for:
 * <ul>
 *     <li>taking in static configuration from their section of the pipeline specification</li>
 *     <li>knowing when they're ready to execute (i.e. all prerequisites, if any, are met)</li>
 *     <li>executing the work itself</li>
 * </ul>
 *
 * @param <ReturnType>
 */
public interface Step<ReturnType extends Object> {
    /**
     * Configure this step (static configuration upon parsing the pipeline the first time)
     *
     * @param config The configuration for this step
     * @param future The result of configuring this step
     */
    public void init(JsonObject config, Future<Void> future);

    /**
     * Return true if, upon examination of the environment, it is time to execute this step
     * @param doc
     * @return
     */
    public boolean isReady(JsonObject doc);

    /**
     *
     * @param document being built
     * @return The (Json-compatible) object to be persisted as a memento of this execution.  It may be a string, int,
     * a JsonArray, JsonObject, etc.
     */
    public void process(JsonObject document, Future<ReturnType> future);


}