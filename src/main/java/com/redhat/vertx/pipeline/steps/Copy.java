package com.redhat.vertx.pipeline.steps;

import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.Step;
import com.redhat.vertx.pipeline.StepDependencyNotMetException;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Copy extends AbstractStep {

    @Override
    public Object execute(JsonObject env) throws StepDependencyNotMetException {
        String s = env.getString("from");
        try {
            return Json.decodeValue(s);
        } catch (DecodeException e) {
            return s;
        }
    }
}
