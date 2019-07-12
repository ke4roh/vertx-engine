package com.redhat.vertx.pipeline.step;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.redhat.vertx.pipeline.ExecutionData;
import com.redhat.vertx.pipeline.Step;
import com.redhat.vertx.pipeline.StepReturnValue;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class HelloWorldStep implements Step {

    @Override
    public void init(JsonObject config, Future<Void> future) {
        // no-op
    }

    @Override
    public boolean isReady(ExecutionData doc) {
        return true;
    }

    @Override
    public CompletionStage<StepReturnValue> process(ExecutionData data) {
        return CompletableFuture.completedFuture(new StepReturnValue(data.getId(), "hello, " + data.getData()));
    }
}
