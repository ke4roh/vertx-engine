package com.redhat.vertx.pipeline.steps;

import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.hubspot.jinjava.interpret.UnknownTokenException;
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
        try {
            String s = env.getString("from");
            if (s == null) {
                throw new StepDependencyNotMetException("from");
            }
            try {
                return Json.decodeValue(s);
            } catch (DecodeException e) {
                return s;
            }
        } catch (UnknownTokenException e) {
            throw new StepDependencyNotMetException("from");
        } catch (FatalTemplateErrorsException e) {
            logger.warning(e.getMessage());
            throw new StepDependencyNotMetException("from");
        } catch (RuntimeException e) {
            logger.warning("Error finding var " + vars.getString("from") + ": " +  e.toString());
            throw e;
        }
    }
}
