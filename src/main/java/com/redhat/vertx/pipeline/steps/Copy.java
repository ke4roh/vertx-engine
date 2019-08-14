package com.redhat.vertx.pipeline.steps;

import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.hubspot.jinjava.interpret.UnknownTokenException;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.StepDependencyNotMetException;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class Copy extends AbstractStep {

    @Override
    public Object execute(JsonObject env) throws StepDependencyNotMetException {
        String key = vars.getString("from");
        try {
            Object o = env.getValue(key);
            if (o == null) {
                throw new StepDependencyNotMetException(key);
            }
            // TODO: This may not be correct either, jinjava can't parse through JsonObject/JsonArray
            try {
                return Json.decodeValue(o.toString());
            } catch (DecodeException e) {
                return o;
            }
        } catch (UnknownTokenException e) {
            throw new StepDependencyNotMetException(key);
        } catch (FatalTemplateErrorsException e) {
            logger.warning(e.getMessage());
            throw new StepDependencyNotMetException(key);
        } catch (RuntimeException e) {
            logger.warning("Error finding var " + vars.getString("from") + ": " +  e.toString());
            throw e;
        }
    }
}
