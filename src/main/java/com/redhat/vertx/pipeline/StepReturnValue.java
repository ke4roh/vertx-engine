package com.redhat.vertx.pipeline;

import java.util.UUID;

// TODO assign the step run ID early, make sure the step knows what it is.
public class StepReturnValue {
    UUID stepRunId;
    UUID pipelineRunId;
    Object returnValue;

    public StepReturnValue(UUID pipelineRunId, Object returnValue) {
        this.pipelineRunId = pipelineRunId;
        this.returnValue = returnValue;
        this.stepRunId = UUID.randomUUID();
    }

    public UUID getStepRunId() {
        return stepRunId;
    }

    public UUID getPipelineRunId() {
        return pipelineRunId;
    }

    public Object getReturnValue() {
        return returnValue;
    }
}
