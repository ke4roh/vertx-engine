package com.redhat.vertx.pipeline.templates;

import io.vertx.core.json.JsonObject;

public interface TemplateProcessor {
    public String applyTemplate(JsonObject env, String str);
}
