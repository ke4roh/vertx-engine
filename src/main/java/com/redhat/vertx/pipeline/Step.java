package com.redhat.vertx.pipeline;

import com.redhat.vertx.Engine;
import io.reactivex.Completable;
import io.reactivex.Maybe;
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
 */
public interface Step {
    /**
     * Configure this step after parsing the json and before any execution.  This is for
     * configuration upon parsing the pipeline the first time to set things like the step name.
     *
     * @param engine The engine to which this step is bound.  The engine provides document cache and vertx.
     * @param config The configuration for this step
     */
    public Completable init(Engine engine, JsonObject config);

    /**
     *
     * @param uuid the key for the document being built (get it from the engine)
     * @return A Maybe containing a single entry with the key to which it is to be registered in the document,
     *    or simply complete if there is no artifact to persist as a result of this step.
     * @throws StepDependencyNotMetException (in the Maybe) if this step should be re-tried upon subsequent document
     *    updates pending population of necessary values.
     */
    public Maybe<JsonObject> execute(String uuid);

    /**
     * Called after step execution for a particular document.  This is an opportunity to dispose Disposables and
     * perform other cleanup.
     */
    public void finish(String uuid);

}
