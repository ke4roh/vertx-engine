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
    private String stepName;
    private String name;
    private String registerTo;
    private JsonObject stepConfig;

    @Override
    public Completable init(Engine engine, JsonObject config) {
        super.init(engine, config);
        name = vars.getString("name", "world");
        registerTo = config.getString("register","greeting");
        return Completable.complete();
    }

    @Override
    public String execute(JsonObject env) {
        return "hello, " + name;
    }
}
