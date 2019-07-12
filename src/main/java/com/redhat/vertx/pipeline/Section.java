package com.redhat.vertx.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// TODO make this a Step
public class Section {
    String name;
    List<Step> steps;

    public Section(JsonArray sectionDef) {
        // TODO this is a Section with a default name and default behavior
    }

    public Section(JsonObject sectionDef) {
        this.name = sectionDef.getString("section");

        this.steps = sectionDef.getJsonArray("steps", new JsonArray()).stream()
                .map(s -> {
                    try {
                        JsonObject stepDef = (JsonObject) s;
                        Class<Step> klass = (Class<Step>) Class.forName(((JsonObject) stepDef).getString("class"));
                        return klass.getDeclaredConstructor((Class[]) null).newInstance();
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public CompletionStage<String> execute(ExecutionData executionData) {
        // Run through every step finding ones that are ready
        // process the steps that are ready
        CompletableFuture<?>[] stepCompletions = this.steps.stream()
                    .filter(step -> step.isReady(executionData))
                    .map(step -> step.process(executionData)
                                        .thenApplyAsync(returnValue -> {
                                            this.storeStepResult(returnValue);
                                            return returnValue.pipelineRunId.toString();
                                        }))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new);

        // Wait for all of them to finish
        return CompletableFuture.allOf(stepCompletions).thenCompose(aVoid -> CompletableFuture.completedStage("run-id"));
    }

    /**
     * Store values somewhere besides memory. Maybe this sends a vertx message.
     * @param result
     */
    private void storeStepResult(StepReturnValue result) {
        System.out.println("A Step completed with the result: " + result.returnValue.toString());
    }
}
