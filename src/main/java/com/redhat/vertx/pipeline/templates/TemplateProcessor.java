package com.redhat.vertx.pipeline.templates;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface TemplateProcessor {
    /**
     * @param env The environment from which to look up variables
     * @param str The string to which to apply the template
     * @throws MissingParameterException if a variable mentioned in the string cannot be found in the environment.
     * @return An unmodified string if it contains no template markup, or the modified string with template applied
     */
    public String applyTemplate(Map<String,Object> env, String str);
}
