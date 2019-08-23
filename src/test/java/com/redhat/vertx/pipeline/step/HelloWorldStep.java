package com.redhat.vertx.pipeline.step;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.Step;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class HelloWorldStep implements Step {
    private String name;
    private String registerTo;

    @Override
    public void init(Engine engine, JsonObject config) {
        name = config.getJsonObject("vars",new JsonObject()).getString("name","world");
        registerTo = config.getString("register","greeting");
    }

    @Override
    public Maybe<JsonObject> execute(String uuid) {
        return Maybe.just(new JsonObject().put(registerTo,"hello, " + name));
    }

    @Override
    public void finish(String uuid) {
    }

}
