package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import io.vertx.core.json.JsonObject;
import jinjava.de.odysseus.el.tree.impl.Builder;

public class JinjaTemplateProcessor implements TemplateProcessor {

    private final Jinjava jinjava;

    public JinjaTemplateProcessor() {
        JinjavaConfig.Builder builder = JinjavaConfig.newBuilder();
        builder.withFailOnUnknownTokens(true);
        JinjavaConfig config = builder.build();
        jinjava = new Jinjava(config);
    }

    @Override
    public String applyTemplate(JsonObject env, String str) {
        return jinjava.render(str,env.getMap());
    }
}
