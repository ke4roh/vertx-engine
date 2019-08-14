package com.redhat.vertx.pipeline.json;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.AbstractList;

public class JsonArrayListView extends AbstractList<Object> {

    private final JsonArray array;

    public JsonArrayListView(JsonArray array) {
        this.array=array;
    }

    @Override
    public Object get(int i) {
        Object o = array.getValue(i);
        if (o instanceof JsonObject) {
            JsonObject jo = (JsonObject)o;
            return new JsonObjectMapView(jo);
        } else if (o instanceof JsonArray) {
            return new JsonArrayListView((JsonArray)o);
        } else {
            return o;
        }
    }

    @Override
    public int size() {
        return array.size();
    }
}
