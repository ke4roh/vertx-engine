package com.redhat.vertx.pipeline.templates;

import com.redhat.vertx.pipeline.Environment;

public interface TemplateProcessor {
    public String applyTemplate(Environment env, String str);
}
