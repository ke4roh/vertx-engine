package com.redhat.vertx.pool;

import io.reactivex.Single;

public class SimplePipelineResolver implements PipelineResolver {
    @Override
    public Single<String> getExecutablePipelineByName(String pipelineName) {
        return Single.just(pipelineName);
    }
}
