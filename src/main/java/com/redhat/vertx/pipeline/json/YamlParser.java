package com.redhat.vertx.pipeline.json;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlParser {
    @SuppressWarnings("unchecked")
    public static Map<String,Object> parse(String doc) {
        Object root = new Yaml(new SafeConstructor()).load(doc);
        if (root instanceof List) {
            Map<String,Object> m = new HashMap<>();
            m.put("steps",root);
            root = m;
        }
        return (Map<String,Object>) root;
    }
}
