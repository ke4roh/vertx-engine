package com.redhat.vertx.engine;

import java.util.concurrent.CompletionStage;

public interface PipelineResolver {
    CompletionStage<String> getExecutablePipelineByName(String pipelineName);
}
