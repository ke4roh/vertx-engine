package com.redhat.vertx.pool;

import java.util.concurrent.CompletionStage;

/**
 * Take a pipeline name and turn it into the actual json representation for that pipeline.
 */
public interface PipelineResolver {
    CompletionStage<String> getExecutablePipelineByName(String pipelineName);
}
