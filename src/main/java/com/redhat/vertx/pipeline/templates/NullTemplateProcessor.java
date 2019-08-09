package com.redhat.vertx.pipeline.templates;

import com.redhat.vertx.pipeline.Environment;

public class NullTemplateProcessor implements TemplateProcessor {
    @Override
    public String applyTemplate(Environment env, String str) {
        return str;
    }
}
