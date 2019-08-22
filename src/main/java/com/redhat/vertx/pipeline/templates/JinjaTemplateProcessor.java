package com.redhat.vertx.pipeline.templates;

import java.util.Map;
import java.util.logging.Logger;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.interpret.TemplateError;

public class JinjaTemplateProcessor implements TemplateProcessor {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final Jinjava jinjava;

    public JinjaTemplateProcessor() {
        JinjavaConfig.Builder builder = JinjavaConfig.newBuilder();
        builder.withFailOnUnknownTokens(true);
        JinjavaConfig config = builder.build();
        jinjava = new Jinjava(config);
    }

    @Override
    public String applyTemplate(Map<String,Object> env, String str) {
        RenderResult rr = jinjava.renderForResult(str, env);
        if (rr.hasErrors()) {
            rr.getErrors().stream().filter(te -> te.getSeverity() == TemplateError.ErrorType.FATAL)
                    .iterator().forEachRemaining(te ->logger.severe(te.toString()));
            rr.getErrors().stream().filter(te -> te.getSeverity() == TemplateError.ErrorType.WARNING)
                    .iterator().forEachRemaining(te ->logger.warning(te.toString()));
            return null;
        }
        return rr.getOutput();
    }
}
