package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.Jinjava;
import com.redhat.vertx.pipeline.Environment;

public class JinjaTemplateProcessor implements TemplateProcessor {
    private Jinjava jinjava = new Jinjava();

    @Override
    public String applyTemplate(Environment env, String str) {
        return jinjava.render(str,env.getMap());
    }
}
