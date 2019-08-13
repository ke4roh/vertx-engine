package com.redhat.vertx.pipeline.json;

import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestTemplatedJsonObject {
    @Test
    public void testToString() {
        JsonObject jo = new JsonObject();
        jo.put("foo","bar");

        JsonObject jo2 = new TemplatedJsonObject(jo,new JinjaTemplateProcessor());
        Map<String,Object> m = jo2.getMap();
        assertEquals(1,m.size());
        assertEquals("bar",m.get("foo"));
        Iterator mi = m.entrySet().iterator();
        assertTrue(mi.hasNext());
        Map.Entry<String,Object> entry = (Map.Entry<String, Object>) mi.next();
        assertEquals("foo",entry.getKey());
        assertEquals("bar", entry.getValue());
        assertFalse(mi.hasNext());
        try {
            mi.next();
            fail();
        } catch (NoSuchElementException e) {
            // pass
        }
    }
}
