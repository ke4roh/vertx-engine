package com.redhat.vertx;

import java.util.concurrent.CompletionStage;

import com.redhat.vertx.pipeline.ExecutionData;
import com.redhat.vertx.pipeline.Pipeline;

/**
 * Entrypoint for execution of a particular pipeline.
 *
 */
public class Engine {
    private Pipeline pipeline;

    public Engine(String pipelineDef) {
        this.pipeline = new Pipeline(pipelineDef);
    }

    public CompletionStage<String> execute(String executionData) {
        return pipeline.execute(new ExecutionData(executionData));
    }
}
