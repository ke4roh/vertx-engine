package com.redhat.vertx.pipeline.json;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

class JsonObjectMapView extends AbstractMap<String,Object> {
    private final Set<String> keySet;
    private final JsonObject obj;

    public JsonObjectMapView(JsonObject obj) {
        this(obj,obj.getMap().keySet());
    }

    public JsonObjectMapView(JsonObject obj, Set<String> keySet) {
        this.obj=obj;
        this.keySet=keySet;
    }

    @Override
    public Set<String> keySet() {
        return keySet;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<Entry<String, Object>>() {
                    Iterator<String> keys = keySet.iterator();
                    @Override
                    public boolean hasNext() {
                        return keys.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        return new Entry<String, Object>() {
                            String key = keys.next();
                            Object val = get(key);

                            @Override
                            public String getKey() {
                                return key;
                            }

                            @Override
                            public Object getValue() {
                                return val;
                            }

                            @Override
                            public Object setValue(Object o) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return keySet.size();
            }
        };
    }

    @Override
    public Object get(Object key) {
        Object o = obj.getValue((String)key);
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
        return keySet.size();
    }
}
