package com.redhat.vertx.pipeline.templates;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface TemplateProcessor {
    public String applyTemplate(Map<String,Object> env, String str);
}
