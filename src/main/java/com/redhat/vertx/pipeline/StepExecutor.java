package com.redhat.vertx.pipeline;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class StepExecutor {
    final Engine engine;
    final Step step;
    final JsonObject stepConfig;

    public StepExecutor(Engine engine, Step step, JsonObject stepConfig) {
        this.engine = engine;
        this.step=step;
        this.stepConfig = stepConfig;
    }


    public Maybe<Object> executeStep(String docId) {
        return Maybe.defer(() -> this.execute0(docId));
    }

    private Maybe<Object> execute0(String docId) {
        JsonObject stepEnvironment = getEnvironment(docId);

        JsonObject stepdef = stepEnvironment.getJsonObject("stepdef");
        boolean when = Boolean.valueOf(stepdef.getString("when","true"));
        if (!when) {
            return Maybe.empty();
        }
        String register = stepdef.getString("register");

        Maybe<Object> result = step.execute(stepEnvironment)
                .filter(r -> register != null)
                .flatMap(r ->
                        engine
                                .updateDocument(docId, new JsonObject().put(register,r))
                                .toSingle(() -> r)
                                .toMaybe()
                );

        Duration timeout = parseDuration(stepdef.getString("timeout", null));
        if (timeout != null) {
            result = result.timeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return result;
    }

    private Duration parseDuration(String duration) {
        if (duration == null) {
            return null;
        }
        duration = duration.toUpperCase();
        if (!duration.startsWith("PT")) {
            duration = "PT" + duration;
        }
        return Duration.parse(duration);
    }


    protected JsonObject getEnvironment(String docId) {
        JsonObject vars = stepConfig.getJsonObject(step.getShortName());
        vars = (vars==null)?new JsonObject():vars.copy();
        vars.put("doc", engine.getDocument(docId));
        vars.put("system", engine.getSystemConfig());
        vars.put("stepdef", stepConfig);
        return new TemplatedJsonObject(vars, engine.getTemplateProcessor(), "doc", "system");
    }

}
