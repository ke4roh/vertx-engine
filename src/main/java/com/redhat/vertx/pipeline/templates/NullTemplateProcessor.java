package com.redhat.vertx.pipeline.templates;


import java.util.Map;

public class NullTemplateProcessor implements TemplateProcessor {
    @Override
    public String applyTemplate(Map<String,Object> env, String str) {
        return str;
    }
}
