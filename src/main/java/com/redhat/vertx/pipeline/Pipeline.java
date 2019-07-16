package com.redhat.vertx.pipeline;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Pipeline {
    Set<Section> sections;
    LocalDateTime createdAt;

    public Pipeline(String piplineDefinition) {
        this.sections = new HashSet<>();

        final JsonArray piplineJson = new JsonArray(piplineDefinition);
        this.sections = piplineJson.stream()
                .map(s -> new Section((JsonObject) s))
                .collect(Collectors.toSet());

        this.createdAt = LocalDateTime.now();
    }

    public Set<Section> getSections() {
        return Collections.unmodifiableSet(sections);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CompletionStage<String> execute(ExecutionData executionData) {
        CompletableFuture<?>[] completableSteps = this.sections.stream()
                        .map(section -> section.execute(executionData))
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(completableSteps)
                .thenComposeAsync(aVoid -> CompletableFuture.supplyAsync(() -> {
                    // TODO: Actually get the value based on the run id in executionData
                    return "Hello";
                }));
    }
}
