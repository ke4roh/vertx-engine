package com.redhat.vertx.pipeline.json;

import com.redhat.vertx.pipeline.templates.TemplateProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TemplatedJsonObject extends AbstractJsonObjectView {
    private final JsonObject context;
    private final TemplateProcessor templateProcessor;
    private final Set<String> protectedKeys;

    public TemplatedJsonObject(JsonObject obj, TemplateProcessor templateProcessor, String... protectedKeys) {
        super(obj);
        this.templateProcessor = templateProcessor;
        this.context = obj;
        this.protectedKeys = new HashSet<>(Arrays.asList(protectedKeys));
    }

    TemplatedJsonObject(JsonObject obj, TemplateProcessor templateProcessor, JsonObject context) {
        super(obj);
        this.templateProcessor = templateProcessor;
        this.context = context;
        this.protectedKeys = Collections.emptySet();
    }


    @Override
    public boolean containsKey(String key) {
        return super.containsKey(key);
    }

    @Override
    public Object getValue(String key) {
        Object val = super.containsKey(key) ? super.getValue(key) :
            templateProcessor.applyTemplate(context, key);

        if (!protectedKeys.contains(key)) {
            if (val instanceof JsonObject) {
                val = new TemplatedJsonObject((JsonObject) val, templateProcessor, context);
            } else if (val instanceof JsonArray) {
                val = new TemplatedJsonArray((JsonArray) val, templateProcessor, context);
            }
        }
        return val;
    }
}
