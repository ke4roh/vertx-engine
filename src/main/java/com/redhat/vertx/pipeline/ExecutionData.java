package com.redhat.vertx.pipeline;

import java.util.UUID;

// TODO Execution Data should be a vertx JsonObject
public class ExecutionData {
    UUID id;
    String data;

    public ExecutionData(String data) {
        this.data = data;
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public String getData() {
        return data;
    }
}
