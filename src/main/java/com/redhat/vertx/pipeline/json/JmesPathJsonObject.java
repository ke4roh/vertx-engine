package com.redhat.vertx.pipeline.json;

import io.burt.jmespath.parser.ParseException;
import io.burt.jmespath.vertx.VertxRuntime;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;

public class JmesPathJsonObject extends AbstractJsonObjectView {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private VertxRuntime jmespath = new VertxRuntime();

    public JmesPathJsonObject(JsonObject obj) {
        super(obj);
    }

    @Override
    public boolean containsKey(String key) {
        try {
            return super.containsKey(key) || (jmespath.compile(key).search(this) != null);
        } catch (ParseException pe) {
            return false;
        }
    }

    @Override
    public Object getValue(String key) {
        if (super.containsKey(key)) {
            return super.getValue(key);
        } else {
            try {
                return jmespath.compile(key).search(obj);
            } catch (ParseException e) {
                return null;
            }
        }
    }
}
