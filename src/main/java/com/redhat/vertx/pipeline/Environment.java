package com.redhat.vertx.pipeline;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * An unmodifiable view on the JsonObject "doc" passed in, with the JsonObject "env" overlaid in front of it.
 * The order of the resulting keys is all the keys from the document, in their order, followed by
 * all the keys only in the environment, in their order.
 *
 * This extends JsonObject for compatibility, but the functionality of JsonObject is delegated to the objects passed in.
 * The responsibilities of this class are:
 * <ul>
 *     <li>Handle requests for content from the environment</li>
 *     <li>Get data from variables if necessary</li>
 *     <li>Perform template substitution on environment contents</li>
 * </ul>
 */
public class Environment extends JsonObject {
    private final JsonObject env;
    private final JsonObject doc;

    public Environment(JsonObject doc, JsonObject env) {
        this.doc = doc;
        this.env = env;
    }

    @Override
    public <T> T mapTo(Class<T> type) {
        throw new UnsupportedOperationException();
        // return super.mapTo(type);
    }

    @Override
    public String getString(String key) {
        return getString(key, null);
    }

    @Override
    public Integer getInteger(String key) {
        return getInteger(key,null);
    }

    @Override
    public Long getLong(String key) {
        return getLong(key, null);
    }

    @Override
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    @Override
    public Float getFloat(String key) {
        return getFloat(key, null);
    }

    @Override
    public Boolean getBoolean(String key) {
        return (Boolean)getValue(key);
    }

    @Override
    public JsonObject getJsonObject(String key) {
        return getJsonObject(key, null);
    }

    @Override
    public JsonArray getJsonArray(String key) {
        return getJsonArray(key, null);
    }

    @Override
    public byte[] getBinary(String key) {
        return getBinary(key, null);
    }

    @Override
    public Instant getInstant(String key) {
        return getInstant(key, null);
    }

    @Override
    public Object getValue(String key) {
        return getValue(key, null);
    }

    @Override
    public String getString(String key, String def) {
        Object val = getValue(key,def);
        return val == null? null: val.toString();
    }

    @Override
    public Integer getInteger(String key, Integer def) {
        Object o = getValue(key, def);
        return (o instanceof Integer)?(Integer)o:(o == null)?null:((Number)o).intValue();
    }

    @Override
    public Long getLong(String key, Long def) {
        Object o = getValue(key, def);
        return (o instanceof Long)?(Long)o:(o == null)?null:((Number)o).longValue();
    }

    @Override
    public Double getDouble(String key, Double def) {
        Object o = getValue(key, def);
        return (o instanceof Double)?(Double)o:(o == null)?null:((Number)o).doubleValue();
    }

    @Override
    public Float getFloat(String key, Float def) {
        Object o = getValue(key, def);
        return (o instanceof Float)?(Float)o:(o == null)?null:((Number)o).floatValue();
    }

    @Override
    public Boolean getBoolean(String key, Boolean def) {
        return (Boolean)getValue(key, def);
    }

    @Override
    public JsonObject getJsonObject(String key, JsonObject def) {
        return (JsonObject)getValue(key, def);
    }

    @Override
    public JsonArray getJsonArray(String key, JsonArray def) {
        return (JsonArray)getValue(key, def);

    }

    @Override
    public byte[] getBinary(String key, byte[] def) {
        return (byte[])getValue(key, def);
    }

    @Override
    public Instant getInstant(String key, Instant def) {
        return (Instant)getValue(key, def);
    }

    @Override
    public Object getValue(String key, Object def) {
        if (!containsKey(key)) {
            return def;
        } else {
            if (env.containsKey(key)) {
                Object o = env.getValue(key);
                if (o instanceof String) {
                    o = applyTemplate((String)o);
                }
                return o;
            } else {
                return doc.getValue(key);
            }
        }
    }

    private String applyTemplate(String s) {
        // TODO make this look for variables and substitute
        return s;
    }

    @Override
    public boolean containsKey(String key) {
        return doc.containsKey(key) || env.containsKey(key);
    }

    @Override
    public Set<String> fieldNames() {
        return super.fieldNames();
    }

    @Override
    public JsonObject put(String key, Enum value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, CharSequence value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Integer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject putNull(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, JsonObject value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, JsonArray value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Instant value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject mergeIn(JsonObject other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject mergeIn(JsonObject other, boolean deep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject mergeIn(JsonObject other, int depth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encode() {
        return copy().encode();
    }

    @Override
    public String encodePrettily() {
        return copy().encodePrettily();
    }

    @Override
    public Buffer toBuffer() {
        return copy().toBuffer();
    }

    /**
     * @return A normal, modifiable JsonObject with the contents of this environment resolved.
     */
    @Override
    public JsonObject copy() {
        return new JsonObject(getMap());
    }

    @Override
    public Map<String, Object> getMap() {
        return new AbstractMap<>() {
            @Override
            public Set<String> keySet() {
                Set<String> keySet = new LinkedHashSet<>(doc.getMap().keySet());
                keySet.addAll(env.getMap().keySet());
                return Collections.unmodifiableSet(keySet);
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return new AbstractSet<Entry<String, Object>>() {
                    @Override
                    public Iterator<Entry<String, Object>> iterator() {
                        return new Iterator<Entry<String, Object>>() {
                            Iterator<String> keys = keySet().iterator();
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
                        return 0;
                    }
                };
            }

            @Override
            public Object get(Object key) {
                Object o = Environment.this.getValue((String)key);
                if (o instanceof JsonObject) {
                    return ((JsonObject) o).getMap();
                } else if (o instanceof JsonArray) {
                    return ((JsonArray) o).getList();
                } else {
                    return o;
                }
            }

            @Override
            public int size() {
                return keySet().size();
            }
        };
    }

    @Override
    public Stream<Map.Entry<String, Object>> stream() {
        return getMap().entrySet().stream();
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return getMap().entrySet().iterator();
    }

    @Override
    public int size() {
        return getMap().size();
    }

    @Override
    public JsonObject clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return doc.isEmpty() && env.isEmpty();
    }

    @Override
    public String toString() {
        return copy().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Environment entries = (Environment) o;
        return Objects.equals(env, entries.env) &&
                Objects.equals(doc, entries.doc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(env, doc);
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        copy().writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        throw new UnsupportedOperationException();
    }
}
