package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.redhat.vertx.pipeline.json.JmesPathJsonObject;
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
        try {
            return jinjava.render(str, env.getMap());
        } catch (FatalTemplateErrorsException e) {
            return null; // TODO make this smarter about genuine errors vs. missing variables
        }
    }
}
