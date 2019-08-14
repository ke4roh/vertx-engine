package com.redhat.vertx.pipeline.json;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * This is an unmodifiable JsonObject view onto another JsonObject.
 * The map belonging to <code>this</code> JsonObject is not used.
 *
 * Behavior can be changed by overriding the {@link #getValue(String)},
 * {@link #containsKey(String)}, and {@link #keySet()} methods.
 */
public abstract class AbstractJsonObjectView extends JsonObject {
    protected JsonObject obj;

    public AbstractJsonObjectView(JsonObject obj) {
        this.obj = obj;
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
        return obj.getValue(key);
    }

    @Override
    public String getString(String key, String def) {
        Object val = getValue(key,def);
        return val == null? def: val.toString();
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
        return containsKey(key) ? getValue(key) : def;
    }


    @Override
    public boolean containsKey(String key) {
        return obj.containsKey(key);
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
        return new JsonObjectMapView(this, obj.getMap().keySet());
    }

    @Override
    public Stream<Map.Entry<String, Object>> stream() {
        return getMap().entrySet().stream();
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, Object>> iterator() {
        return getMap().entrySet().iterator();
    }

    @Override
    public int size() { return obj.size(); }

    @Override
    public JsonObject clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return obj.isEmpty();
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
        AbstractJsonObjectView a = (AbstractJsonObjectView) o;
        return Objects.equals(obj, a.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
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
