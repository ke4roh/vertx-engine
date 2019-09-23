package com.redhat.vertx.pipeline.step;

import java.util.Objects;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.Step;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import org.kohsuke.MetaInfServices;

/**
 * This implements a basic greeting in a minimal step.  Most steps should not start here, but
 * rather extend {@link com.redhat.vertx.pipeline.AbstractStep} which will provide context and,
 * for steps that aren't blocking, abstract away the reactive element.
 */
@MetaInfServices(Step.class)
public class HelloWorldStep implements Step {
    private String stepName;
    private String name;
    private String registerTo;
    private JsonObject stepConfig;

    @Override
    public Completable init(Engine engine, JsonObject config) {
        stepName = config.getString("name","HelloWorld");
        stepConfig = config.getJsonObject(getShortName(), new JsonObject());
        var vars = stepConfig.getJsonObject("vars", new JsonObject());

        // Just in case there isn't any additional config for a step
        if (Objects.isNull(stepConfig)) {
            stepConfig = new JsonObject();
        }

        name = vars.getString("name", "world");
        registerTo = config.getString("register","greeting");
        return Completable.complete();
    }

    @Override
    public Maybe<JsonObject> execute(String uuid) {
        return Maybe.just(new JsonObject().put(registerTo,"hello, " + name));
    }

    @Override
    public String getName() {
        return stepName;
    }

    @Override
    public JsonObject getStepConfig() {
        return this.stepConfig;
    }
}
