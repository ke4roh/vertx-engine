package com.redhat.vertx.pipeline.json;

import java.time.Instant;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.redhat.vertx.pipeline.templates.TemplateProcessor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TemplatedJsonArray extends JsonArray {
    private final JsonObject context;
    private final TemplateProcessor templateProcessor;
    private final JsonArray arr;

    TemplatedJsonArray(JsonArray arr, TemplateProcessor templateProcessor, JsonObject context) {
        this.arr = arr;
        this.templateProcessor = templateProcessor;
        this.context = context;
    }

    @Override
    public String getString(int index) {
        Object val = getValue(index);
        return val == null? null: val.toString();
    }

    @Override
    public Integer getInteger(int index) {
        Object o = getValue(index);
        return (o instanceof Integer)?(Integer)o:(o == null)?null:((Number)o).intValue();
    }

    @Override
    public Long getLong(int index) {
        Object o = getValue(index);
        return (o instanceof Long)?(Long)o:(o == null)?null:((Number)o).longValue();
    }

    @Override
    public Double getDouble(int index) {
        Object o = getValue(index);
        return (o instanceof Double)?(Double)o:(o == null)?null:((Number)o).doubleValue();
    }

    @Override
    public Float getFloat(int index) {
        Object o = getValue(index);
        return (o instanceof Float)?(Float)o:(o == null)?null:((Number)o).floatValue();
    }

    @Override
    public Boolean getBoolean(int index) {
        return (Boolean)getValue(index);
    }

    @Override
    public JsonObject getJsonObject(int index) {
        return (JsonObject)getValue(index);
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return (JsonArray)getValue(index);
    }

    @Override
    public byte[] getBinary(int index) {
        if (arr.hasNull(index)) {
            return (byte[]) getValue(index);
        }
        return arr.getBinary(index);
    }

    @Override
    public Instant getInstant(int index) {
        return Instant.parse(getValue(index).toString());
    }

    @Override
    public Object getValue(int index) {
        Object val = arr.getValue(index);
        if (val instanceof String) {
            val=templateProcessor.applyTemplate(new JsonObjectMapView(context),(String)val);
        }
        if (val instanceof JsonObject) {
            val = new TemplatedJsonObject((JsonObject)val,templateProcessor,context);
        } else if (val instanceof JsonArray) {
            val = new TemplatedJsonArray((JsonArray)val,templateProcessor,context);
        }
        return val;
    }

    @Override
    public boolean hasNull(int pos) {
        return getValue(pos) == null;
    }

    @Override
    public JsonArray add(Enum value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(CharSequence value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(String value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Integer value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Long value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Double value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Float value) {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Boolean value) {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray addNull() {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(JsonObject value) {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(JsonArray value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(byte[] value) { throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Instant value) {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray add(Object value) {  throw new UnsupportedOperationException(); }

    @Override
    public JsonArray addAll(JsonArray array) {  throw new UnsupportedOperationException(); }

    @Override
    public boolean contains(Object value) {
        for (Object o:this) {
            if (Objects.equals(o, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(Object value) { throw new UnsupportedOperationException(); }

    @Override
    public Object remove(int pos) { throw new UnsupportedOperationException(); }

    @Override
    public int size() {
        return arr.size();
    }

    @Override
    public boolean isEmpty() {
        return arr.isEmpty();
    }

    @Override
    public List<Object> getList() {
        return new AbstractList<>() {
            @Override
            public int size() {
                return TemplatedJsonArray.this.size();
            }

            @Override
            public Object get(int i) {
                return getValue(i);
            }
        };
    }

    @Override
    public JsonArray clear() { throw new UnsupportedOperationException(); }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<>() {
            int pos = 0;
            @Override
            public boolean hasNext() {
                return pos < size();
            }

            @Override
            public Object next() {
                return getValue(pos++);
            }
        };
    }

    @Override
    public String encode() {
        return new JsonArray(this.getList()).encode();
    }

    @Override
    public Buffer toBuffer() {
        return new JsonArray(getList()).toBuffer();
    }

    @Override
    public String encodePrettily() {
        return new JsonArray(getList()).encodePrettily();
    }

    @Override
    public JsonArray copy() {
        return new JsonArray(getList());
    }

    @Override
    public Stream<Object> stream() {
        return getList().stream();
    }

    @Override
    public String toString() {
        return new JsonArray(getList()).toString();
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        new JsonArray(getList()).writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) { throw new UnsupportedOperationException(); }
}
