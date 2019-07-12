package com.redhat.vertx;

import java.util.concurrent.CompletionStage;

import com.redhat.vertx.pipeline.ExecutionData;
import com.redhat.vertx.pipeline.Pipeline;
import com.redhat.vertx.pipeline.Section;
import io.vertx.core.json.JsonArray;

/**
 * Entrypoint for execution of a particular pipeline.
 *
 */
public class Engine {
    private Section pipeline;

    public Engine(String pipelineDef) {
        this.pipeline = new Section( new JsonArray(pipelineDef));
    }

    public CompletionStage<String> execute(String executionData) {
        return pipeline.execute(new ExecutionData(executionData));
    }
}
