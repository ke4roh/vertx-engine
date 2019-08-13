package com.redhat.vertx.pipeline.templates;

import io.vertx.core.json.JsonObject;

public class NullTemplateProcessor implements TemplateProcessor {
    @Override
    public String applyTemplate(JsonObject env, String str) {
        return str;
    }
}
