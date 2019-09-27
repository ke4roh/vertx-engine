package com.redhat.vertx.pipeline.step;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.Step;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import org.kohsuke.MetaInfServices;

/**
 * This implements a basic greeting in a minimal step.  Most steps should not start here, but
 * rather extend {@link com.redhat.vertx.pipeline.AbstractStep} which will provide context and,
 * for steps that aren't blocking, abstract away the reactive element.
 */
@MetaInfServices(Step.class)
public class HelloWorldStep extends AbstractStep {

    @Override
    public Completable init(Engine engine, JsonObject config) {
        super.init(engine, config);
        return Completable.complete();
    }

    @Override
    public String executeFast(JsonObject env) {
        return "hello, " + env.getString("name","world");
    }
}
