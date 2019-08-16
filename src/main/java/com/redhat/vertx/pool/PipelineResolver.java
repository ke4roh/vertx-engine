package com.redhat.vertx.pool;

import io.reactivex.Single;

/**
 * Take a pipeline name and turn it into the actual json representation for that pipeline.
 */
public interface PipelineResolver {
    Single<String> getExecutablePipelineByName(String pipelineName);
}
